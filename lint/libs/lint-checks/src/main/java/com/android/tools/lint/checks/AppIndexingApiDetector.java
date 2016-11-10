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
package com.android.tools.lint.checks;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.res2.AbstractResourceRepository;
import com.android.ide.common.res2.ResourceItem;
import com.android.ide.common.resources.ResourceUrl;
import com.android.resources.ResourceType;
import com.android.tools.lint.client.api.JavaEvaluator;
import com.android.tools.lint.client.api.LintClient;
import com.android.tools.lint.client.api.XmlParser;
import com.android.tools.lint.detector.api.*;
import com.android.tools.lint.detector.api.Detector.JavaPsiScanner;
import com.android.tools.lint.detector.api.Detector.XmlScanner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.psi.*;
import org.w3c.dom.*;

import java.io.File;
import java.util.*;

import static com.android.SdkConstants.*;
import static com.android.xml.AndroidManifest.*;


/**
 * Check if the usage of App Indexing is correct.
 */
public class AppIndexingApiDetector extends Detector implements XmlScanner, JavaPsiScanner {

    private static final Implementation URL_IMPLEMENTATION = new Implementation(
            AppIndexingApiDetector.class, Scope.MANIFEST_SCOPE);

    @SuppressWarnings("unchecked")
    private static final Implementation APP_INDEXING_API_IMPLEMENTATION =
            new Implementation(
                    AppIndexingApiDetector.class,
                    EnumSet.of(Scope.JAVA_FILE, Scope.MANIFEST),
                    Scope.JAVA_FILE_SCOPE, Scope.MANIFEST_SCOPE);

    public static final Issue ISSUE_URL_ERROR = Issue.create(
            "GoogleAppIndexingUrlError", //$NON-NLS-1$
            "URL not supported by app for Firebase App Indexing",
            "Ensure the URL is supported by your app, to get installs and traffic to your"
                    + " app from Google Search.",
            Category.USABILITY, 5, Severity.ERROR, URL_IMPLEMENTATION)
            .addMoreInfo("https://g.co/AppIndexing/AndroidStudio");

    public static final Issue ISSUE_APP_INDEXING =
      Issue.create(
        "GoogleAppIndexingWarning", //$NON-NLS-1$
        "Missing support for Firebase App Indexing",
        "Adds URLs to get your app into the Google index, to get installs"
          + " and traffic to your app from Google Search.",
        Category.USABILITY, 5, Severity.WARNING, URL_IMPLEMENTATION)
        .addMoreInfo("https://g.co/AppIndexing/AndroidStudio");

    public static final Issue ISSUE_APP_INDEXING_API =
            Issue.create(
                    "GoogleAppIndexingApiWarning", //$NON-NLS-1$
                    "Missing support for Firebase App Indexing Api",
                    "Adds URLs to get your app into the Google index, to get installs"
                            + " and traffic to your app from Google Search.",
                    Category.USABILITY, 5, Severity.WARNING, APP_INDEXING_API_IMPLEMENTATION)
                    .addMoreInfo("https://g.co/AppIndexing/AndroidStudio")
                    .setEnabledByDefault(false);

    private static final String[] PATH_ATTR_LIST = new String[]{ATTR_PATH_PREFIX, ATTR_PATH};
    private static final String SCHEME_MISSING = "android:scheme is missing";
    private static final String HOST_MISSING = "android:host is missing";
    private static final String DATA_MISSING = "Missing data element";
    private static final String URL_MISSING = "Missing URL for the intent filter";
    private static final String NOT_BROWSABLE
            = "Activity supporting ACTION_VIEW is not set as BROWSABLE";
    private static final String ILLEGAL_NUMBER = "android:port is not a legal number";

    private static final String APP_INDEX_START = "start"; //$NON-NLS-1$
    private static final String APP_INDEX_END = "end"; //$NON-NLS-1$
    private static final String APP_INDEX_VIEW = "view"; //$NON-NLS-1$
    private static final String APP_INDEX_VIEW_END = "viewEnd"; //$NON-NLS-1$
    private static final String CLIENT_CONNECT = "connect"; //$NON-NLS-1$
    private static final String CLIENT_DISCONNECT = "disconnect"; //$NON-NLS-1$
    private static final String ADD_API = "addApi"; //$NON-NLS-1$

    private static final String APP_INDEXING_API_CLASS
            = "com.google.android.gms.appindexing.AppIndexApi";
    private static final String GOOGLE_API_CLIENT_CLASS
            = "com.google.android.gms.common.api.GoogleApiClient";
    private static final String GOOGLE_API_CLIENT_BUILDER_CLASS
            = "com.google.android.gms.common.api.GoogleApiClient.Builder";
    private static final String API_CLASS = "com.google.android.gms.appindexing.AppIndex";

    public enum IssueType {
        SCHEME_MISSING(AppIndexingApiDetector.SCHEME_MISSING),
        HOST_MISSING(AppIndexingApiDetector.HOST_MISSING),
        DATA_MISSING(AppIndexingApiDetector.DATA_MISSING),
        URL_MISSING(AppIndexingApiDetector.URL_MISSING),
        NOT_BROWSABLE(AppIndexingApiDetector.NOT_BROWSABLE),
        ILLEGAL_NUMBER(AppIndexingApiDetector.ILLEGAL_NUMBER),
        EMPTY_FIELD("cannot be empty"),
        MISSING_SLASH("attribute should start with '/'"),
        UNKNOWN("unknown error type");

        private final String message;

        IssueType(String str) {
            this.message = str;
        }

        public static IssueType parse(String str) {
            for (IssueType type : IssueType.values()) {
                if (str.contains(type.message)) {
                    return type;
                }
            }
            return UNKNOWN;
        }
    }

    // ---- Implements XmlScanner ----
    @Override
    @Nullable
    public Collection<String> getApplicableElements() {
        return Collections.singletonList(NODE_APPLICATION);
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element application) {
        List<Element> activities = extractChildrenByName(application, NODE_ACTIVITY);
        boolean applicationHasActionView = false;
        for (Element activity : activities) {
            List<Element> intents = extractChildrenByName(activity, NODE_INTENT);
            boolean activityHasActionView = false;
            for (Element intent : intents) {
                boolean actionView = hasActionView(intent);
                if (actionView) {
                    activityHasActionView = true;
                }
                visitIntent(context, intent);
            }
            if (activityHasActionView) {
                applicationHasActionView = true;
                if (activity.hasAttributeNS(ANDROID_URI, ATTR_EXPORTED)) {
                    Attr exported = activity.getAttributeNodeNS(ANDROID_URI, ATTR_EXPORTED);
                    if (!exported.getValue().equals("true")) {
                        // Report error if the activity supporting action view is not exported.
                        context.report(ISSUE_URL_ERROR, activity,
                                       context.getLocation(activity),
                                       "Activity supporting ACTION_VIEW is not exported");
                    }
                }
            }
        }
        if (!applicationHasActionView && !context.getProject().isLibrary()) {
            // Report warning if there is no activity that supports action view.
            context.report(ISSUE_APP_INDEXING, application, context.getLocation(application),
                           // This error message is more verbose than the other app indexing lint warnings, because it
                           // shows up on a blank project, and we want to make it obvious by just looking at the error
                           // message what this is
                           "App is not indexable by Google Search; consider adding at least one Activity with an ACTION-VIEW " +
                           "intent filter. See issue explanation for more details.");
        }
    }

    @Nullable
    @Override
    public List<String> applicableSuperClasses() {
        return Collections.singletonList(CLASS_ACTIVITY);
    }

    @Override
    public void checkClass(@NonNull JavaContext context, @NonNull PsiClass declaration) {
        if (declaration.getName() == null) {
            return;
        }

        // In case linting the base class itself.
        if (!context.getEvaluator().extendsClass(declaration, CLASS_ACTIVITY, true)) {
            return;
        }

        declaration.accept(new MethodVisitor(context, declaration));
    }

    static class MethodVisitor extends JavaRecursiveElementVisitor {
        private final JavaContext mContext;
        private final PsiClass mCls;

        private final List<PsiMethodCallExpression> mStartMethods;
        private final List<PsiMethodCallExpression> mEndMethods;
        private final List<PsiMethodCallExpression> mConnectMethods;
        private final List<PsiMethodCallExpression> mDisconnectMethods;
        private boolean mHasAddAppIndexApi;

        MethodVisitor(JavaContext context, PsiClass cls) {
            mCls = cls;
            mContext = context;
            mStartMethods = Lists.newArrayListWithExpectedSize(2);
            mEndMethods = Lists.newArrayListWithExpectedSize(2);
            mConnectMethods = Lists.newArrayListWithExpectedSize(2);
            mDisconnectMethods = Lists.newArrayListWithExpectedSize(2);
        }

        @Override
        public void visitClass(PsiClass aClass) {
            if (aClass == mCls) {
                super.visitClass(aClass);
                report();
            } // else: don't go into inner classes
        }

        @Override
        public void visitMethodCallExpression(PsiMethodCallExpression node) {
            super.visitMethodCallExpression(node);

            String methodName = node.getMethodExpression().getReferenceName();
            if (methodName == null) {
                return;
            }

            JavaEvaluator evaluator = mContext.getEvaluator();
            if (methodName.equals(APP_INDEX_START)) {
                if (evaluator.isMemberInClass(node.resolveMethod(), APP_INDEXING_API_CLASS)) {
                    mStartMethods.add(node);
                }
            } else if (methodName.equals(APP_INDEX_END)) {
                if (evaluator.isMemberInClass(node.resolveMethod(), APP_INDEXING_API_CLASS)) {
                    mEndMethods.add(node);
                }
            } else if (methodName.equals(APP_INDEX_VIEW)) {
                if (evaluator.isMemberInClass(node.resolveMethod(), APP_INDEXING_API_CLASS)) {
                    mStartMethods.add(node);
                }
            } else if (methodName.equals(APP_INDEX_VIEW_END)) {
                if (evaluator.isMemberInClass(node.resolveMethod(), APP_INDEXING_API_CLASS)) {
                    mEndMethods.add(node);
                }
            } else if (methodName.equals(CLIENT_CONNECT)) {
                if (evaluator.isMemberInClass(node.resolveMethod(), GOOGLE_API_CLIENT_CLASS)) {
                    mConnectMethods.add(node);
                }
            } else if (methodName.equals(CLIENT_DISCONNECT)) {
                if (evaluator.isMemberInClass(node.resolveMethod(), GOOGLE_API_CLIENT_CLASS)) {
                    mDisconnectMethods.add(node);
                }
            } else if (methodName.equals(ADD_API)) {
                if (evaluator.isMemberInClass(node.resolveMethod(), GOOGLE_API_CLIENT_BUILDER_CLASS)) {
                    PsiExpression[] args = node.getArgumentList().getExpressions();
                    if (args.length > 0) {
                        PsiElement resolved = evaluator.resolve(args[0]);
                        if (resolved instanceof PsiField &&
                                evaluator.isMemberInClass((PsiField) resolved, API_CLASS)) {
                            mHasAddAppIndexApi = true;
                        }
                    }
                }
            }
        }

        @Override
        public void visitAnonymousClass(PsiAnonymousClass aClass) {
            // Don't jump into inner classes
        }

        private void report() {
            // finds the activity classes that need app activity annotation
            Set<String> activitiesToCheck = getActivitiesToCheck(mContext);

            // app indexing API used but no support in manifest
            boolean hasIntent = activitiesToCheck.contains(mCls.getQualifiedName());
            if (!hasIntent) {
                for (PsiMethodCallExpression call : mStartMethods) {
                    mContext.report(ISSUE_APP_INDEXING_API, call,
                            mContext.getNameLocation(call),
                            "Missing support for Firebase App Indexing in the manifest");
                }
                for (PsiMethodCallExpression call : mEndMethods) {
                    mContext.report(ISSUE_APP_INDEXING_API, call,
                            mContext.getNameLocation(call),
                            "Missing support for Firebase App Indexing in the manifest");
                }
                return;
            }

            // `AppIndex.AppIndexApi.start / end / view / viewEnd` should exist
            if (mStartMethods.isEmpty() && mEndMethods.isEmpty()) {
                mContext.report(ISSUE_APP_INDEXING_API, mCls,
                        mContext.getNameLocation(mCls),
                        "Missing support for Firebase App Indexing API");
                return;
            }

            for (PsiMethodCallExpression startNode : mStartMethods) {
                PsiExpression[] expressions = startNode.getArgumentList().getExpressions();
                if (expressions.length == 0) {
                    continue;
                }
                PsiExpression startClient = expressions[0];

                // GoogleApiClient should `addApi(AppIndex.APP_INDEX_API)`
                if (!mHasAddAppIndexApi) {
                    String message = String.format(
                            "GoogleApiClient `%1$s` has not added support for App Indexing API",
                            startClient.getText());
                    mContext.report(ISSUE_APP_INDEXING_API, startClient,
                            mContext.getLocation(startClient), message);
                }

                // GoogleApiClient `connect` should exist
                if (!hasOperand(startClient, mConnectMethods)) {
                    String message = String.format("GoogleApiClient `%1$s` is not connected",
                                    startClient.getText());
                    mContext.report(ISSUE_APP_INDEXING_API, startClient,
                            mContext.getLocation(startClient), message);
                }

                // `AppIndex.AppIndexApi.end` should pair with `AppIndex.AppIndexApi.start`
                if (!hasFirstArgument(startClient, mEndMethods)) {
                    mContext.report(ISSUE_APP_INDEXING_API, startNode,
                            mContext.getNameLocation(startNode),
                            "Missing corresponding `AppIndex.AppIndexApi.end` method");
                }
            }

            for (PsiMethodCallExpression endNode : mEndMethods) {
                PsiExpression[] expressions = endNode.getArgumentList().getExpressions();
                if (expressions.length == 0) {
                    continue;
                }
                PsiExpression endClient = expressions[0];

                // GoogleApiClient should `addApi(AppIndex.APP_INDEX_API)`
                if (!mHasAddAppIndexApi) {
                    String message = String.format(
                            "GoogleApiClient `%1$s` has not added support for App Indexing API",
                            endClient.getText());
                    mContext.report(ISSUE_APP_INDEXING_API, endClient,
                            mContext.getLocation(endClient), message);
                }

                // GoogleApiClient `disconnect` should exist
                if (!hasOperand(endClient, mDisconnectMethods)) {
                    String message = String.format("GoogleApiClient `%1$s`"
                            + " is not disconnected", endClient.getText());
                    mContext.report(ISSUE_APP_INDEXING_API, endClient,
                            mContext.getLocation(endClient), message);
                }

                // `AppIndex.AppIndexApi.start` should pair with `AppIndex.AppIndexApi.end`
                if (!hasFirstArgument(endClient, mStartMethods)) {
                    mContext.report(ISSUE_APP_INDEXING_API, endNode,
                            mContext.getNameLocation(endNode),
                            "Missing corresponding `AppIndex.AppIndexApi.start` method");
                }
            }
        }
    }

    /**
     * Gets names of activities which needs app indexing. i.e. the activities have data tag in their
     * intent filters.
     * TODO: Cache the activities to speed up batch lint.
     *
     * @param context The context to check in.
     */
    private static Set<String> getActivitiesToCheck(Context context) {
        Set<String> activitiesToCheck = Sets.newHashSet();
        List<File> manifestFiles = context.getProject().getManifestFiles();
        XmlParser xmlParser = context.getDriver().getClient().getXmlParser();
        if (xmlParser != null) {
            // TODO: Avoid visit all manifest files before enable this check by default.
            for (File manifest : manifestFiles) {
                XmlContext xmlContext =
                        new XmlContext(context.getDriver(), context.getProject(),
                                null, manifest, null, xmlParser);
                Document doc = xmlParser.parseXml(xmlContext);
                if (doc != null) {
                    List<Element> children = LintUtils.getChildren(doc);
                    for (Element child : children) {
                        if (child.getNodeName().equals(NODE_MANIFEST)) {
                            List<Element> apps = extractChildrenByName(child, NODE_APPLICATION);
                            for (Element app : apps) {
                                List<Element> acts = extractChildrenByName(app, NODE_ACTIVITY);
                                for (Element act : acts) {
                                    List<Element> intents = extractChildrenByName(act, NODE_INTENT);
                                    for (Element intent : intents) {
                                        List<Element> data = extractChildrenByName(intent,
                                                NODE_DATA);
                                        if (!data.isEmpty() && act.hasAttributeNS(
                                                ANDROID_URI, ATTRIBUTE_NAME)) {
                                            Attr attr = act.getAttributeNodeNS(
                                                    ANDROID_URI, ATTRIBUTE_NAME);
                                            String activityName = attr.getValue();
                                            int dotIndex = activityName.indexOf('.');
                                            if (dotIndex <= 0) {
                                                String pkg = context.getMainProject().getPackage();
                                                if (pkg != null) {
                                                    if (dotIndex == 0) {
                                                        activityName = pkg + activityName;
                                                    }
                                                    else {
                                                        activityName = pkg + '.' + activityName;
                                                    }
                                                }
                                            }
                                            activitiesToCheck.add(activityName);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return activitiesToCheck;
    }

    private static void visitIntent(@NonNull XmlContext context, @NonNull Element intent) {
        boolean actionView = hasActionView(intent);
        boolean browsable = isBrowsable(intent);
        boolean isHttp = false;
        boolean hasScheme = false;
        boolean hasHost = false;
        boolean hasPort = false;
        boolean hasPath = false;
        boolean hasMimeType = false;
        Element firstData = null;
        List<Element> children = extractChildrenByName(intent, NODE_DATA);
        for (Element data : children) {
            if (firstData == null) {
                firstData = data;
            }
            if (isHttpSchema(data)) {
                isHttp = true;
            }
            checkSingleData(context, data);

            for (String name : PATH_ATTR_LIST) {
                if (data.hasAttributeNS(ANDROID_URI, name)) {
                    hasPath = true;
                }
            }

            if (data.hasAttributeNS(ANDROID_URI, ATTR_SCHEME)) {
                hasScheme = true;
            }

            if (data.hasAttributeNS(ANDROID_URI, ATTR_HOST)) {
                hasHost = true;
            }

            if (data.hasAttributeNS(ANDROID_URI, ATTRIBUTE_PORT)) {
                hasPort = true;
            }

            if (data.hasAttributeNS(ANDROID_URI, ATTRIBUTE_MIME_TYPE)) {
                hasMimeType = true;
            }
        }

        // In data field, a URL is consisted by
        // <scheme>://<host>:<port>[<path>|<pathPrefix>|<pathPattern>]
        // Each part of the URL should not have illegal character.
        if ((hasPath || hasHost || hasPort) && !hasScheme) {
            context.report(ISSUE_URL_ERROR, firstData, context.getLocation(firstData),
                    SCHEME_MISSING);
        }

        if ((hasPath || hasPort) && !hasHost) {
            context.report(ISSUE_URL_ERROR, firstData, context.getLocation(firstData),
                    HOST_MISSING);
        }

        if (actionView && browsable) {
            if (firstData == null) {
                // If this activity is an ACTION_VIEW action with category BROWSABLE, but doesn't
                // have data node, it may be a mistake and we will report error.
                context.report(ISSUE_URL_ERROR, intent, context.getLocation(intent),
                        DATA_MISSING);
            } else if (!hasScheme && !hasMimeType) {
                // If this activity is an action view, is browsable, but has neither a
                // URL nor mimeType, it may be a mistake and we will report error.
                context.report(ISSUE_URL_ERROR, firstData, context.getLocation(firstData),
                        URL_MISSING);
            }
        }

        // If this activity is an ACTION_VIEW action, has a http URL but doesn't have
        // BROWSABLE, it may be a mistake and and we will report warning.
        if (actionView && isHttp && !browsable) {
            context.report(ISSUE_APP_INDEXING, intent, context.getLocation(intent),
                    NOT_BROWSABLE);
        }

        if (actionView && !hasScheme) {
            context.report(ISSUE_APP_INDEXING, intent, context.getLocation(intent),
                    "Missing URL");
        }
    }

    /**
     * Check if the intent filter supports action view.
     *
     * @param intent the intent filter
     * @return true if it does
     */
    private static boolean hasActionView(@NonNull Element intent) {
        List<Element> children = extractChildrenByName(intent, NODE_ACTION);
        for (Element action : children) {
            if (action.hasAttributeNS(ANDROID_URI, ATTRIBUTE_NAME)) {
                Attr attr = action.getAttributeNodeNS(ANDROID_URI, ATTRIBUTE_NAME);
                if (attr.getValue().equals("android.intent.action.VIEW")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if the intent filter is browsable.
     *
     * @param intent the intent filter
     * @return true if it does
     */
    private static boolean isBrowsable(@NonNull Element intent) {
        List<Element> children = extractChildrenByName(intent, NODE_CATEGORY);
        for (Element e : children) {
            if (e.hasAttributeNS(ANDROID_URI, ATTRIBUTE_NAME)) {
                Attr attr = e.getAttributeNodeNS(ANDROID_URI, ATTRIBUTE_NAME);
                if (attr.getNodeValue().equals("android.intent.category.BROWSABLE")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if the data node contains http schema
     *
     * @param data the data node
     * @return true if it does
     */
    private static boolean isHttpSchema(@NonNull Element data) {
        if (data.hasAttributeNS(ANDROID_URI, ATTR_SCHEME)) {
            String value = data.getAttributeNodeNS(ANDROID_URI, ATTR_SCHEME).getValue();
            if (value.equalsIgnoreCase("http") || value.equalsIgnoreCase("https")) {
                return true;
            }
        }
        return false;
    }

    private static void checkSingleData(@NonNull XmlContext context, @NonNull Element data) {
        // path, pathPrefix and pathPattern should starts with /.
        for (String name : PATH_ATTR_LIST) {
            if (data.hasAttributeNS(ANDROID_URI, name)) {
                Attr attr = data.getAttributeNodeNS(ANDROID_URI, name);
                String path = replaceUrlWithValue(context, attr.getValue());
                if (!path.startsWith("/") && !path.startsWith(SdkConstants.PREFIX_RESOURCE_REF)) {
                    context.report(ISSUE_URL_ERROR, attr, context.getLocation(attr),
                            "android:" + name + " attribute should start with '/', but it is : "
                                    + path);
                }
            }
        }

        // port should be a legal number.
        if (data.hasAttributeNS(ANDROID_URI, ATTRIBUTE_PORT)) {
            Attr attr = data.getAttributeNodeNS(ANDROID_URI, ATTRIBUTE_PORT);
            try {
                String port = replaceUrlWithValue(context, attr.getValue());
                Integer.parseInt(port);
            } catch (NumberFormatException e) {
                context.report(ISSUE_URL_ERROR, attr, context.getLocation(attr),
                        ILLEGAL_NUMBER);
            }
        }

        // Each field should be non empty.
        NamedNodeMap attrs = data.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node item = attrs.item(i);
            if (item.getNodeType() == Node.ATTRIBUTE_NODE) {
                Attr attr = (Attr) attrs.item(i);
                if (attr.getValue().isEmpty()) {
                    context.report(ISSUE_URL_ERROR, attr, context.getLocation(attr),
                            attr.getName() + " cannot be empty");
                }
            }
        }
    }

    private static String replaceUrlWithValue(@NonNull XmlContext context,
            @NonNull String str) {
        Project project = context.getProject();
        LintClient client = context.getClient();
        if (!client.supportsProjectResources()) {
            return str;
        }
        ResourceUrl style = ResourceUrl.parse(str);
        if (style == null || style.type != ResourceType.STRING || style.framework) {
            return str;
        }
        AbstractResourceRepository resources = client.getResourceRepository(project, true, true);
        if (resources == null) {
            return str;
        }
        List<ResourceItem> items = resources.getResourceItem(ResourceType.STRING, style.name);
        if (items == null || items.isEmpty()) {
            return str;
        }
        ResourceValue resourceValue = items.get(0).getResourceValue(false);
        if (resourceValue == null) {
            return str;
        }
        return resourceValue.getValue() == null ? str : resourceValue.getValue();
    }

    /**
     * If a method with a certain argument exists in the list of methods.
     *
     * @param argument The first argument of the method.
     * @param list     The methods list.
     * @return If such a method exists in the list.
     */
    private static boolean hasFirstArgument(PsiExpression argument, List<PsiMethodCallExpression> list) {
        for (PsiMethodCallExpression call : list) {
            PsiExpression[] expressions = call.getArgumentList().getExpressions();
            if (expressions.length > 0) {
                PsiExpression argument2 = expressions[0];
                if (argument.getText().equals(argument2.getText())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * If a method with a certain operand exists in the list of methods.
     *
     * @param operand The operand of the method.
     * @param list    The methods list.
     * @return If such a method exists in the list.
     */
    private static boolean hasOperand(PsiExpression operand, List<PsiMethodCallExpression> list) {
        for (PsiMethodCallExpression method : list) {
            PsiElement operand2 = method.getMethodExpression().getQualifier();
            if (operand2 != null && operand.getText().equals(operand2.getText())) {
                return true;
            }
        }
        return false;
    }

    private static List<Element> extractChildrenByName(@NonNull Element node,
            @NonNull String name) {
        List<Element> result = Lists.newArrayList();
        List<Element> children = LintUtils.getChildren(node);
        for (Element child : children) {
            if (child.getNodeName().equals(name)) {
                result.add(child);
            }
        }
        return result;
    }
}
