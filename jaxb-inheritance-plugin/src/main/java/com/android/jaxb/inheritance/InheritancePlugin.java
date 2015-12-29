/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.jaxb.inheritance;

import com.android.annotations.NonNull;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sun.codemodel.JAnnotationArrayMember;
import com.sun.codemodel.JArray;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JOp;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JType;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.model.CElementPropertyInfo;
import com.sun.tools.xjc.model.CPluginCustomization;
import com.sun.tools.xjc.model.CPropertyInfo;
import com.sun.tools.xjc.model.CTypeInfo;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;
import com.sun.xml.xsom.XSComponent;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSFacet;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSSimpleType;
import com.sun.xml.xsom.XSTerm;
import com.sun.xml.xsom.XSType;

import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.transform.dom.DOMSource;

/**
 * JAXB plugin to support specifying superclasses/interfaces for generated classes. Behavior is as
 * follows:<br> When a type declaration in an xsd is annotated as follows:
 * <pre>{@code
 * <xsd:schema
 *   ...
 *   xmlns:plugin="http://schemas.android.com/repository/android/plugin/1">
 *
 * <xsd:complexType name="myType">
 *     <xsd:annotation>
 *         <xsd:appinfo>
 *             <plugin:super name="my.package.ClassName$InnerClass"/>
 *         </xsd:appinfo>
 *     </xsd:annotation>
 * </xsd:complexType>
 * }</pre>
 *
 * The generated MyType class will have the given class (in this case {@code
 * my.package.ClassName$InnerClassName}) specified as the superclass. Method parameter and return
 * types will also be rewritten to reference supertypes.
 */
public class InheritancePlugin extends Plugin {

    // Namespace for tags handled by this plugin
    private static final String NS = "http://schemas.android.com/android/jaxb/plugin/1";

    // Tag specifying the superclass for a type
    private static final String SUPER_TAG = "super";

    // We generate our own header comment, since the default one contains a timestamp, which causes
    // diffs whenever the schema is recompiled (and would cause non-repeatable builds, if the
    // schema compilation were to be done automatically at build time). It also doesn't contain the
    // schema filename, which is nice to have for reference.
    // To suppress the default header, always run xjc with -no-header.
    private static final String HEADER_COMMENT =
            "DO NOT EDIT\nThis file was generated by xjc from %1$s. Any changes will be lost " +
                    "upon recompilation of the schema.\nSee the schema file for instructions on " +
                    "running xjc.%2$s";

    @Override
    public String getOptionName() {
        return "Xandroid-inheritance";
    }

    @Override
    public String getUsage() {
        return "  -Xandroid-inheritance: enable the android inheritance plugin";
    }

    @Override
    public boolean isCustomizationTagName(String nsUri, String localName) {
        return NS.equals(nsUri);
    }

    @Override
    public List<String> getCustomizationURIs() {
        return ImmutableList.of(NS);
    }

    @Override
    public boolean run(Outline outline, Options options, ErrorHandler errorHandler)
            throws SAXException {
        // TODO: would be nice to support enums
        assert outline.getEnums().isEmpty();

        // Get the schema filename from the first class (it will be the same for all generated
        // classes, since we always use episodic compilation.
        String filename = outline.getClasses().iterator().next().target.getLocator().getSystemId();
        filename = filename.substring(filename.lastIndexOf('/') + 1);

        // Add package-info header comment
        JPackage pack = outline.getCodeModel().packages().next();
        pack.javadoc().add(String.format(HEADER_COMMENT, filename, ""));

        JDefinedClass objFactory = handleObjectFactoryCustomization(outline, filename);

        Map<String, Class> supers = Maps.newHashMap();
        for (ClassOutline classOutline : outline.getClasses()) {
            classOutline.implClass.javadoc()
                    .add(0, String.format(HEADER_COMMENT, filename, "\n\n"));
            acknowledgeCustomizationsOnInherited(classOutline);
            // xjc has a problem if types referenced in imported schemas aren't referenced
            // (it seems that there's no good way to mark the customizations as accepted). So we
            // have to reference everything in a type called XjcWorkaround, which is then hidden
            // in the generated code.
            if (classOutline.implClass.name().equals("XjcWorkaround")) {
                classOutline.implClass.hide();
                continue;
            }
            addAndCollectParents(classOutline, supers);
            addValidityChecks(classOutline, outline.getCodeModel());
        }

        createGenerateElementMethod(outline, objFactory, supers);

        JCodeModel model = outline.getCodeModel();
        for (ClassOutline classOutline : outline.getClasses()) {
            JAnnotationArrayMember suppress =
                    classOutline.implClass.annotate(SuppressWarnings.class).paramArray("value");
            // The generated methods optionally have stubs in the superclass. In order to keep
            // the existance of stubs optional from version to version, and to not have warnings
            // in the generated code, we suppress "method is override" warnings.
            suppress.param("override");
            // Sometimes we generate unsafe casts to allow us to expose a consistent interface.
            // Suppress those warnings as well.
            suppress.param("unchecked");

            List<JMethod> orig = ImmutableList.copyOf(classOutline.implClass.methods());
            for (JMethod method : orig) {
                convertMethod(supers, model, classOutline, method);
            }

            // Add a method to each class to create the corresponding ObjectFactory (only if it's a
            // concrete class, since otherwise overriding methods in other modules will have
            // clashing return types).
            if (!classOutline.implClass.isAbstract()) {
                JMethod getOf = classOutline.implClass
                        .method(JMod.PUBLIC, objFactory, "createFactory");
                getOf.body()._return(JExpr._new(objFactory));
            }
        }

        return true;
    }

    /**
     * Converts a method to take superclass information into account. If it takes a parameter of a
     * type that has a superclass specified, the method is renamed, and then a new method with the
     * original name is created that takes the superclass type, then casts it and calls the original
     * method. If it returns a generic type with a parameter with a superclass specified, it is
     * changed to be of the parent type and casted appropriately before being returned.
     *
     * For example, Consider a schema with a type T1Sub which has an element of type T2Sub.
     * Without customization, xjc would generate a method like
     * {@code void setT2(T2Sub t)}. If the element is repeated, it would have a method like
     * {@code List<T2Sub> getT2()}.
     * Now, we declare T1Sub and T2Sub to be subclasses of T1Super and T2Super respectively, and
     * will interact with them solely through those classes. In order for this to be possible
     * we must (unsafely) cast in the generated classes. This method will modify the generated code
     * to look like:
     *
     * <pre><code>
     * void setT2Internal(T2Sub t) { ... }
     *
     * void setT2(T2Super t) {
     *   setT2Internal((T2Sub)t);
     * }
     * </code></pre>
     *
     * or in the repeated case,
     *
     * <pre><code>
     * List<T2Sub> getT2Internal() { ... }
     *
     * List<T2Super> getT2() {
     *   return (List)getT2Internal();
     * }
     * </code></pre>
     */
    private static void convertMethod(Map<String, Class> supers, JCodeModel model,
            ClassOutline classOutline, JMethod method) {
        if (method.listParamTypes().length > 0) {
            // Right now there's no way to get a method with more than one param, so that's all we
            // support.
            JType paramType = method.listParamTypes()[0];
            // TODO: better way to do android check
            if (isAndroid(paramType) && (supers.containsKey(paramType.fullName()) || (
                    paramType instanceof JClass && ((JClass) paramType)._extends() != null &&
                            !((JClass) paramType)._extends().name().equals("Object")))) {
                String name = method.name();
                method.name(name + "Internal");
                JMethod newMethod = classOutline.implClass.method(JMod.PUBLIC, model.VOID, name);
                newMethod.param(convertType(paramType, supers, model),
                        method.listParams()[0].name());
                JBlock body = newMethod.body();
                JInvocation call = body.invoke(method);
                call.arg(JExpr.cast(paramType, method.listParams()[0]));
            }
        }

        JType type = method.type();
        if (isAndroid(type) && type.erasure() != type && type instanceof JClass) {
            String name = method.name();
            method.name(name + "Internal");
            JType converted = convertType(type, supers, model);
            JMethod newMethod = classOutline.implClass.method(JMod.PUBLIC, converted, name);
            newMethod.body()._return(JExpr.cast(type.erasure(), JExpr.invoke(method)));
        }
    }

    /**
     * By default xjc creates a method to return a JAXBElement for top-level elements declared in
     * the schema. This creates a method called generateElement that calls the default element
     * creation method, casts the type parameter to its superclass, and returns that. This standard
     * method name can then be specified in a superclass of ObjectFactory itself, making it
     * accessible without having to know the specific type of the ObjectFactory.
     *
     * For example, suppose your schema contains a top-level element of type {@code MyElementType}
     * which is declared to be a subclass of {@code MyElementSuper}. By default ObjectFactory would
     * contain
     * {@code public JAXBElement<MyElementType> createMyElementType(MyElementType value)}
     * This method generates a method
     * {@code public JAXBElement<MyElementSuper> generateElement(MyElementSuper value)} that
     * calls the default method with the appropriate casts.
     *
     * Note that jaxb gets confused if this method is called "createElement", due to the "create"
     * prefix.
     */
    private static void createGenerateElementMethod(Outline outline, JDefinedClass objFactory,
            Map<String, Class> supers) throws SAXException {
        List<JMethod> origMethods = Lists.newArrayList(objFactory.methods());
        for (JMethod m : origMethods) {
            Class superType = supers.get(m.type().fullName());
            if (superType != null) {
                try {
                    m.type(outline.getCodeModel().parseType(superType.getName()));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    throw new SAXException(e);
                }
            }
            if (isAndroid(m.type()) && m.type().erasure() != m.type()) {
                JType converted = convertType(m.type(), supers, outline.getCodeModel());
                if (converted == m.type()) {
                    continue;
                }
                JType paramType = m.listParamTypes()[0];
                JType convertedParam = convertType(paramType, supers, outline.getCodeModel());
                JMethod newMethod = objFactory.method(JMod.PUBLIC, converted, "generateElement");
                newMethod.param(convertedParam, m.listParams()[0].name());
                JInvocation call = JExpr.invoke(m);
                call.arg(JExpr.cast(paramType, m.listParams()[0]));
                newMethod.body()._return(JExpr.cast(m.type().erasure(), call));
            }
        }
    }

    /**
     * For the given class, get the superclass customization specified in the xsd. Adds the
     * specified class/interface as a superclass/implemented interface of this class, and adds
     * the child to parent mapping to superCollector.
     */
    private static void addAndCollectParents(ClassOutline classOutline,
            Map<String, Class> superCollector)
            throws SAXException {
        for (CPluginCustomization c : classOutline.target.getCustomizations()) {
            Element customizationElement = c.element;
            if (customizationElement.getNamespaceURI().equals(NS) && customizationElement
                    .getLocalName().equals(SUPER_TAG)) {
                c.markAsAcknowledged();

                Class superclass = getSuperclass(c);

                if (classOutline.implClass._extends().name().equals("Object") &&
                        !superclass.isInterface()) {
                    classOutline.implClass._extends(superclass);
                    superCollector.put(classOutline.implClass.fullName(), superclass);
                } else {
                    classOutline.implClass._implements(superclass);
                }
            }
        }
    }

    /**
     * Parses the superclass customization and returns a {@link Class} instance for the specified
     * type.
     */
    @NonNull
    private static Class getSuperclass(CPluginCustomization c) throws SAXException {
        JAXBContext context;
        Unmarshaller unmarshaller;
        try {
            context = JAXBContext.newInstance(SuperClass.class);
            unmarshaller = context.createUnmarshaller();
        } catch (Exception e) {
            // shouldn't ever happen
            throw new SAXException(e);
        }
        Object result;
        SuperClass superClass;
        try {
            result = unmarshaller.unmarshal(new DOMSource(c.element));
        } catch (JAXBException e) {
            System.err.println("Couldn't parse superclass element");
            throw new SAXException(e);
        }
        superClass = (SuperClass) JAXBIntrospector.getValue(result);
        return superClass.getClassInstance();
    }

    /**
     * For types used in subclasses, we need to get the type declaration in the imported schema
     * and mark any customization as accepted. This seems to be because of a bug/design problem
     * in xjc.
     */
    private static void acknowledgeCustomizationsOnInherited(ClassOutline classOutline) {
        for (FieldOutline field : classOutline.getDeclaredFields()) {
            for (CTypeInfo ref : field.getPropertyInfo().ref()) {
                CPluginCustomization c = ref.getCustomizations().find(NS, SUPER_TAG);
                if (c != null) {
                    c.markAsAcknowledged();
                }
            }
        }
    }

    /**
     * Sets the superclass of the generated ObjectFactory to the type specified in the top-level
     * customization.
     */
    @NonNull
    private static JDefinedClass handleObjectFactoryCustomization(Outline outline,
            String schemaFileName)
            throws SAXException {
        JAXBContext context;
        Unmarshaller unmarshaller;
        try {
            context = JAXBContext.newInstance(SuperClass.class);
            unmarshaller = context.createUnmarshaller();
        } catch (Exception e) {
            // shouldn't ever happen
            throw new SAXException(e);
        }

        String factoryName = outline.getAllPackageContexts().iterator().next()._package().name()
                + ".ObjectFactory";
        JDefinedClass objFactory = outline.getCodeModel()._getClass(factoryName);
        objFactory.annotate(SuppressWarnings.class).param("value", "override");
        objFactory.javadoc().add(0, String.format(HEADER_COMMENT, schemaFileName, "\n\n"));


        // Can't find() the customization, since we'll pick up imported ones as well. So need to
        // iterate and check the locators to see if the customization came from the same file.
        for (CPluginCustomization ofCustomization : outline.getModel().getCustomizations()) {
            Element customizationElement = ofCustomization.element;
            if (customizationElement.getNamespaceURI().equals(NS) && customizationElement
                    .getLocalName().equals(SUPER_TAG)) {
                ofCustomization.markAsAcknowledged();
                ClassOutline aClass = outline.getClasses().iterator().next();
                if (ofCustomization.locator.getSystemId()
                        .equals(aClass.target.getLocator().getSystemId())) {
                    Object result;
                    SuperClass ofSuperClass;
                    try {
                        result = unmarshaller.unmarshal(new DOMSource(ofCustomization.element));
                    } catch (JAXBException e) {
                        System.err.println("Couldn't parse superclass element");
                        throw new SAXException(e);
                    }
                    ofSuperClass = (SuperClass) JAXBIntrospector.getValue(result);
                    objFactory._extends(ofSuperClass.getClassInstance());
                }
            }
        }

        Iterator<JMethod> methodIterator = objFactory.methods().iterator();
        while (methodIterator.hasNext()) {
            JMethod method = methodIterator.next();
            if (method.name().endsWith("XjcWorkaround")) {
                methodIterator.remove();
                break;
            }
        }
        return objFactory;
    }

    /**
     * If we have a method taking e.g. Integer, we don't want to convert to the super type.
     * For now we just check like this; ideally we would check whether it was created in this or
     * and imported schema.
     */
    private static boolean isAndroid(JType type) {
        return type.fullName().contains("android");
    }

    /**
     * Given a type, return either the supertype, or (if it's a generic type), the same generic
     * with the parameter being the supertype of the original parameter.
     */
    private static JType convertType(JType type, Map<String, Class> supers, JCodeModel model) {
        if (type.erasure() != type && type instanceof JClass) {
            boolean found = false;
            List<JClass> newParams = Lists.newArrayList();
            for (JClass param : ((JClass) type).getTypeParameters()) {
                JType newType = convertBasicType(param, supers, model);
                if (!param.equals(newType)) {
                    found = true;
                }
                // It must be a class if it's a superclass of type, which is also a class.
                newParams.add((JClass)newType);
            }
            if (found) {
                return ((JClass) type.erasure()).narrow(newParams);
            }
        }
        return convertBasicType(type, supers, model);
    }

    private static JType convertBasicType(JType type, Map<String, Class> supers, JCodeModel model) {
        Class superClass = supers.get(type.fullName());
        if (superClass != null) {
            return model.ref(superClass);
        } else if (type instanceof JClass) {
            JClass sup = ((JClass)type)._extends();
            if (sup != null && !sup.fullName().equals(Object.class.getName())) {
                return sup;
            }
        }
        return type;
    }

    /**
     * Adds a method to a class allowing the caller to check whether a string value is valid with
     * respect to the facets of a field.<br/>
     * Currently only pattern facets are supported.
     */
    private static void addValidityChecks(ClassOutline classOutline, JCodeModel codeModel) {
        for (FieldOutline field : classOutline.getDeclaredFields()) {
            CPropertyInfo info = field.getPropertyInfo();
            XSComponent component = info.getSchemaComponent();
            List<JExpression> conditions = Lists.newArrayList();
            if (component instanceof XSParticle) {
                XSTerm term = ((XSParticle)component).getTerm();
                if (term instanceof XSElementDecl) {
                    if (((XSElementDecl) term).getDefaultValue() != null) {
                        // If the a default value is specified in the schema, it won't actually be
                        // applied if the element is missing altogether. We must apply it ourselves.
                        JFieldVar fieldRef = classOutline.implClass.fields()
                                .get(info.getName(false));
                        fieldRef.init(JExpr.lit(((XSElementDecl) term).getDefaultValue().value));
                    }

                    XSType type = ((XSElementDecl)term).getType();
                    if (type instanceof XSSimpleType) {
                        // TODO: support other facets
                        XSFacet patternFacet = ((XSSimpleType)type).getFacet("pattern");
                        if (patternFacet != null) {
                            String pattern = patternFacet.getValue().toString();
                            conditions.add(JExpr.direct("value.matches(\"^" + pattern + "$\")"));
                        }
                        List<XSFacet> enums = ((XSSimpleType)type).getFacets("enumeration");
                        if (!enums.isEmpty()) {
                            JExpression enumExpr = JExpr.FALSE;
                            for (XSFacet enumVal : enums) {
                                enumExpr = enumExpr.cor(JExpr.direct(
                                        "value.equals(\"" + enumVal.getValue().toString() + "\")"));
                            }
                            conditions.add(enumExpr);

                            // Also create a "get valid options" method. This allows e.g. a choice
                            // between the valid options to be presented in the UI.
                            JMethod method = classOutline.implClass
                                    .method(JMod.PUBLIC, codeModel.ref(String.class).array(),
                                            "getValid" + info.getName(true) + "s");
                            JArray arr = JExpr.newArray(codeModel.ref(String.class));
                            for (XSFacet enumVal : enums) {
                                arr.add(JExpr.lit(enumVal.getValue().toString()));
                            }
                            method.body()._return(arr);
                        }
                    }
                }
            }
            if (!conditions.isEmpty()) {
                JMethod method = classOutline.implClass
                        .method(JMod.PUBLIC, codeModel.BOOLEAN,
                                "isValid" + info.getName(true));
                method.param(String.class, "value");
                JBlock body = method.body();
                JExpression combined;
                Iterator<JExpression> conditionIter = conditions.iterator();
                combined = conditionIter.next();
                while (conditionIter.hasNext()) {
                    combined = JOp.cand(combined, conditionIter.next());
                }

                if (combined != null) {
                    if (info instanceof CElementPropertyInfo) {
                        if (((CElementPropertyInfo) info).isRequired()) {
                            combined = JOp.cand(JExpr.direct("value != null"), combined);
                        } else {
                            combined = JOp.cor(JExpr.direct("value == null"), combined);
                        }
                    }
                }
                body._return(combined);
            }
        }
    }

    /**
     * Class used for unmarshalling the customizations.
     */
    @XmlRootElement(namespace = NS, name = SUPER_TAG)
    public static class SuperClass {

        @XmlAttribute(required = true, name = "name")
        public String mName;

        @NonNull
        public Class getClassInstance() throws SAXException {
            try {
                return Thread.currentThread().getContextClassLoader().loadClass(mName);
            } catch (ClassNotFoundException e) {
                System.err.println("Superclass " + mName + " not found");
                throw new SAXException(e);
            }
        }
    }
}
