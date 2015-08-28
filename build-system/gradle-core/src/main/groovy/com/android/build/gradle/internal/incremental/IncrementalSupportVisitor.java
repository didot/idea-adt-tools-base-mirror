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

package com.android.build.gradle.internal.incremental;

import com.android.annotations.NonNull;
import com.android.utils.FileUtils;
import com.google.common.io.Files;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * Visitor for classes that will eventually be replaceable at runtime.
 *
 * Since classes cannot be replaced in an existing class loader, we use a delegation model to
 * redirect any method implementation to the AndroidInstantRuntime.
 *
 * This redirection happens only when a new class implementation is available, so far we do a
 * hashtable lookup for updated implementation. In the future, we could generate a static field
 * during the class visit with this visitor and have that boolean field indicate the presence of an
 * updated version or not.
 */
public class IncrementalSupportVisitor extends IncrementalVisitor {

    public IncrementalSupportVisitor(ClassNode classNode, List<ClassNode> parentNodes, ClassVisitor classVisitor) {
        super(classNode, parentNodes, classVisitor);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName,
            String[] interfaces) {
        visitedClassName = name;
        visitedSuperName = superName;

        super.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "$change", getRuntimeTypeName(CHANGE_TYPE), null, null);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature,
            String[] exceptions) {
        MethodVisitor defaultVisitor = super.visitMethod(access, name, desc, signature, exceptions);
        if (name.equals("<clinit>")) {
            return defaultVisitor;
        } else {
            return new ISMethodVisitor(Opcodes.ASM5, defaultVisitor, access, name, desc);
        }
    }

    private class ISMethodVisitor extends GeneratorAdapter {

        private final String name;
        private final String desc;
        private final int access;
        private final boolean isConstructor;

        public ISMethodVisitor(int api, MethodVisitor mv, int access,  String name, String desc) {
            super(api, mv, access, name, desc);
            this.name = name;
            this.desc = desc;
            this.access = access;
            this.isConstructor = name.equals("<init>");
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf)  {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            if (isConstructor && opcode == Opcodes.INVOKESPECIAL && name.equals("<init>") && owner.equals(visitedSuperName)) {
                addRedirection();
            }
        }

        @Override
        public void visitCode() {
            if (!isConstructor) {
                addRedirection();
            }
            super.visitCode();
        }

        private void addRedirection() {
            // code to check if a new implementation of the current class is available.
            visitFieldInsn(Opcodes.GETSTATIC, visitedClassName, "$change",
                    getRuntimeTypeName(CHANGE_TYPE));
            Label l0 = new Label();
            super.visitJumpInsn(Opcodes.IFNULL, l0);
            visitFieldInsn(Opcodes.GETSTATIC, visitedClassName, "$change",
                    getRuntimeTypeName(CHANGE_TYPE));
            push(name + "." + desc);

            List<Type> args = new ArrayList<Type>(Arrays.asList(Type.getArgumentTypes(desc)));
            boolean isStatic = (access & Opcodes.ACC_STATIC) != 0;
            // if this is not a static, we add a fictional first parameter what will contain the "this"
            // reference which can be loaded with ILOAD_0 bytecode.
            if (!isStatic) {
                args.add(0, Type.getType(Object.class));
            }

            // create an array of objects capable of containing all the parameters and optional the "this"
            push(args.size());
            newArray(Type.getType(Object.class));

            // we need to maintain the stack index when loading parameters from, as for long and double
            // values, it uses 2 stack elements, all others use only 1 stack element.
            int stackIndex = 0;
            for (int arrayIndex = 0; arrayIndex < args.size(); arrayIndex++) {
                Type arg = args.get(arrayIndex);
                // duplicate the array of objects reference, it will be used to store the value in.
                dup();
                // index in the array of objects to store the boxed parameter.
                push(arrayIndex);
                // This will load "this" when it's not static function as the first element
                visitVarInsn(arg.getOpcode(Opcodes.ILOAD), stackIndex);

                // potentially box up intrinsic types.
                box(arg);
                arrayStore(Type.getType(Object.class));
                // stack index must progress according to the parameter type we just processed.
                stackIndex += arg.getSize();
            }

            // now invoke the generic dispatch method.
            invokeInterface(CHANGE_TYPE, Method.getMethod("Object access$dispatch(String, Object[])"));
            Type ret = Type.getReturnType(desc);
            if (ret == Type.VOID_TYPE) {
                pop();
            } else {
                unbox(ret);
            }
            returnValue();

            // jump label for classes without any new implementation, just invoke the original
            // method implementation.
            visitLabel(l0);
        }
    }

    @Override
    public void visitEnd() {
        int access = Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC | Opcodes.ACC_VARARGS;
        Method m = new Method("access$super", "(L" + visitedClassName + ";Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;");
        MethodVisitor visitor = super.visitMethod(access,
                        m.getName(),
                        m.getDescriptor(),
                        null, null);

        GeneratorAdapter mv = new GeneratorAdapter(access, m, visitor);

        List<MethodNode> methods = classNode.methods;
        for (MethodNode methodNode : methods) {
            if (methodNode.name.equals("<init>") || methodNode.name.equals("<clinit>")) {
                continue;
            }
            if (TRACING_ENABLED) {
                trace(mv, "testing super for ", methodNode.name);
            }
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitLdcInsn(methodNode.name + "." + methodNode.desc);
            if (TRACING_ENABLED) {
                mv.push(methodNode.name + "." + methodNode.desc);
                mv.push("==");
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                trace(mv, 3);
            }
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals",
                    "(Ljava/lang/Object;)Z", false);
            Label l0 = new Label();
            mv.visitJumpInsn(Opcodes.IFEQ, l0);
            mv.visitVarInsn(Opcodes.ALOAD, 0);

            Type[] args = Type.getArgumentTypes(methodNode.desc);
            int argc = 0;
            for (Type t : args) {
                mv.visitVarInsn(Opcodes.ALOAD, 2);
                mv.push(argc);
                mv.visitInsn(Opcodes.AALOAD);
                mv.unbox(t);
                argc++;
            }

            if (TRACING_ENABLED) {
                trace(mv, "super selected ", methodNode.name, methodNode.desc);
            }
            // Call super on the other object, yup this works cos we are on the right place to call from.
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, visitedSuperName, methodNode.name,
                    methodNode.desc, false);

            Type ret = Type.getReturnType(methodNode.desc);
            if (ret.getSort() == Type.VOID) {
                mv.visitInsn(Opcodes.ACONST_NULL);
            } else {
                mv.box(ret);
            }
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitLabel(l0);
        }


        // we could not find the method to invoke, prepare an exception to be thrown.
        mv.newInstance(Type.getType(StringBuilder.class));
        mv.dup();
        mv.invokeConstructor(Type.getType(StringBuilder.class), Method.getMethod("void <init>()V"));

        // create a meaningful message
        mv.push("Method not found ");
        mv.invokeVirtual(Type.getType(StringBuilder.class),
                Method.getMethod("StringBuilder append (String)"));
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.invokeVirtual(Type.getType(StringBuilder.class),
                Method.getMethod("StringBuilder append (String)"));
        mv.push("in " + visitedClassName + "$super implementation");
        mv.invokeVirtual(Type.getType(StringBuilder.class),
                Method.getMethod("StringBuilder append (String)"));

        mv.invokeVirtual(Type.getType(StringBuilder.class),
                Method.getMethod("String toString()"));

        // create the exception with the message
        mv.newInstance(INSTANT_RELOAD_EXCEPTION);
        mv.dupX1();
        mv.swap();
        mv.invokeConstructor(INSTANT_RELOAD_EXCEPTION,
                Method.getMethod("void <init> (String)"));
        // and throw.
        mv.throwException();

        mv.visitMaxs(0, 0);
        mv.visitEnd();

        super.visitEnd();
    }

    /**
     * Command line invocation entry point. Expects 2 parameters, first is the source directory
     * with .class files as produced by the Java compiler, second is the output directory where to
     * store the bytecode enhanced version.
     * @param args the command line arguments.
     * @throws IOException if some files cannot be read or written.
     */
    public static void main(String[] args) throws IOException {

        IncrementalVisitor.main(args, new VisitorBuilder() {
            @Override
            public IncrementalVisitor build(@NonNull ClassNode classNode,
                    List<ClassNode> parentNodes,
                    ClassVisitor classVisitor) {
                return new IncrementalSupportVisitor(classNode, parentNodes, classVisitor);
            }

            @Override
            public boolean processParents() {
                return false;
            }
        });
    }
}
