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

package com.android.builder.shrinker;

import static com.android.utils.FileUtils.getAllFiles;
import static com.android.utils.FileUtils.withExtension;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.build.transform.api.ScopedContent;
import com.android.build.transform.api.TransformInput;
import com.android.build.transform.api.TransformOutput;
import com.android.ide.common.internal.LoggedErrorException;
import com.android.ide.common.internal.WaitableExecutor;
import com.android.utils.FileUtils;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Code shrinker. It analyzes the input classes and the SDK jar and outputs minified classes. Uses
 * the given implementation of {@link ShrinkerGraph} to keep state and persist it for later
 * incremental runs.
 */
public class Shrinker<T> {

    private final WaitableExecutor<Void> mExecutor;
    private final ShrinkerGraph<T> mGraph;
    private final File mAndroidJar;

    public Shrinker(WaitableExecutor<Void> executor, ShrinkerGraph<T> graph, File androidJar) {
        mExecutor = executor;
        mGraph = graph;
        mAndroidJar = androidJar;
    }

    @NonNull
    private static Optional<File> chooseOutputFile(
            @NonNull File classFile,
            @NonNull Map<TransformInput, TransformOutput> inputOutputs) {
        String absolutePath = classFile.getAbsolutePath();

        for (Map.Entry<TransformInput, TransformOutput> entry : inputOutputs.entrySet()) {
            TransformInput input = entry.getKey();
            TransformOutput output = entry.getValue();
            for (File inputDir : getAllDirectories(input)) {
                if (absolutePath.startsWith(inputDir.getAbsolutePath())) {
                    File outputDir;
                    if (input.getFormat() == ScopedContent.Format.MULTI_FOLDER) {
                        outputDir = new File(output.getOutFile(), inputDir.getName());
                    } else {
                        outputDir = output.getOutFile();
                    }

                    String relativePath = FileUtils.relativePath(classFile, inputDir);
                    return Optional.of(new File(outputDir, relativePath));
                }
            }
        }

        return Optional.absent();
    }

    @NonNull
    private static ClassNode readClassNode(File classFile) throws IOException {
        ClassReader classReader = new ClassReader(Files.toByteArray(classFile));
        ClassNode classNode = new ClassNode(Opcodes.ASM5);
        classReader.accept(classNode, 0);
        return classNode;
    }

    private static byte[] rewrite(File classFile, Set<String> membersToKeep, Predicate<String> keepClass) throws IOException {
        ClassReader classReader = new ClassReader(Files.toByteArray(classFile));
        ClassWriter classWriter = new ClassWriter(classReader, 0);
        ClassVisitor filter = new FilterMembersVisitor(membersToKeep, keepClass, classWriter);
        classReader.accept(filter, 0);
        return classWriter.toByteArray();
    }

    @NonNull
    public static UnsupportedOperationException todo(String message) {
        return new UnsupportedOperationException("TODO: " + message);
    }

    private ImmutableMap<ShrinkType, Set<T>> buildMapPerShrinkType(
            ImmutableMap<ShrinkType, KeepRules> keepRules) {
        ImmutableMap.Builder<ShrinkType, Set<T>> builder = ImmutableMap.builder();
        for (ShrinkType shrinkType : keepRules.keySet()) {
            builder.put(shrinkType, Sets.<T>newConcurrentHashSet());
        }

        return builder.build();
    }

    private void buildGraph(
            Iterable<TransformInput> programInputs,
            Iterable<TransformInput> libraryInputs,
            final Map<ShrinkType, KeepRules> keepRules,
            final ImmutableMap<ShrinkType, Set<T>> toIncrement) throws IOException {
        final Set<T> virtualMethods = Sets.newConcurrentHashSet();
        final Set<UnresolvedReference<T>> unresolvedReferences = Sets.newConcurrentHashSet();

        readPlatformJar();

        for (TransformInput input : libraryInputs) {
            for (File folder : getAllDirectories(input)) {
                for (final File classFile : getClassFiles(folder)) {
                    mExecutor.execute(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            processLibraryClass(Files.toByteArray(classFile));
                            return null;
                        }
                    });
                }
            }
        }

        for (TransformInput input : programInputs) {
            for (File folder : getAllDirectories(input)) {
                for (final File classFile : getClassFiles(folder)) {
                    mExecutor.execute(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            processNewClassFile(
                                    classFile,
                                    keepRules,
                                    toIncrement,
                                    virtualMethods,
                                    unresolvedReferences);
                            return null;
                        }
                    });
                }
            }
        }

        waitForAllTasks();

        handleOverrides(virtualMethods);
        resolveReferences(unresolvedReferences);

        waitForAllTasks();

        // TODO - remove in production.
        mGraph.checkDependencies();
    }

    private static Collection<File> getAllDirectories(TransformInput input) {
        switch (input.getFormat()) {
            case SINGLE_FOLDER:
                return input.getFiles();
            case MULTI_FOLDER:
                List<File> folders = Lists.newArrayList();
                for (File topLevel : input.getFiles()) {
                    File[] files = topLevel.listFiles();
                    Preconditions.checkArgument(files != null, "Invalid input %s", topLevel);
                    Collections.addAll(folders, files);
                }
                return folders;
            default:
                throw new IllegalStateException("Unsupported input type " + input.getFormat());
        }
    }

    private static FluentIterable<File> getClassFiles(File dir) {
        return getAllFiles(dir).filter(withExtension("class"));
    }

    private void resolveReferences(Set<UnresolvedReference<T>> unresolvedReferences) {
        for (final UnresolvedReference<T> unresolvedReference : unresolvedReferences) {
            mExecutor.execute(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    T currentClass = mGraph.getClassForMember(unresolvedReference.target);

                    if (unresolvedReference.opcode == Opcodes.INVOKESPECIAL) {
                        // With invokespecial we disregard the class in target and start walking up
                        // the type hierarchy, starting from the superclass of the caller.
                        currentClass =
                                mGraph.getSuperclass(
                                        mGraph.getClassForMember(unresolvedReference.method));
                    }

                    while (currentClass != null) {
                        T target = mGraph.findMatchingMethod(currentClass, unresolvedReference.target);
                        if (target != null) {
                            if (!mGraph.isLibraryMember(target)) {
                                mGraph.addDependency(unresolvedReference.method, target, DependencyType.REQUIRED);
                            }
                            break;
                        }

                        currentClass = mGraph.getSuperclass(currentClass);
                    }

                    return null;
                }
            });
        }
    }

    private void handleOverrides(Set<T> virtualMethods) {
        // TODO: Handle Object class specially.
        //final T javaLangObject = mGraph.getClassReference("java/lang/Object");
        for (final T method : virtualMethods) {
            mExecutor.execute(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    walkTypeHierarchy(mGraph.getClassForMember(method));
                    return null;
                }

                private void walkTypeHierarchy(@Nullable T klass) {
                    if (klass == null) {
                        return;
                    }

                    T superMethod = mGraph.findMatchingMethod(klass, method);
                    if (superMethod != null && !superMethod.equals(method)) {
                        if (mGraph.isLibraryMember(superMethod)) {
                            // If we override an SDK method, it just has to be there at runtime
                            // (if the class itself is kept).
                            mGraph.addDependency(
                                    mGraph.getClassForMember(method),
                                    method,
                                    DependencyType.REQUIRED);
                        } else {
                            // If we override a program method, there's a chance this method is
                            // never called and we will get rid of it. Set up the dependencies
                            // appropriately.
                            mGraph.addDependency(
                                    mGraph.getClassForMember(method),
                                    method,
                                    DependencyType.NEEDED_FOR_INHERITANCE);
                            mGraph.addDependency(
                                    superMethod,
                                    method,
                                    DependencyType.IS_OVERRIDDEN);
                        }
                    } else {
                        walkTypeHierarchy(mGraph.getSuperclass(klass));
                        for (T iface : mGraph.getInterfaces(klass)) {
                            walkTypeHierarchy(iface);
                        }
                    }
                }
            });
        }
    }

    private void readPlatformJar() throws IOException {
        JarFile jarFile = new JarFile(mAndroidJar);
        try {
            for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements();) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) {
                    continue;
                }
                final InputStream inputStream = jarFile.getInputStream(entry);
                try {
                    final byte[] source = ByteStreams.toByteArray(inputStream);
                    // TODO: See if doing this in parallel actually improves performance.
                    mExecutor.execute(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            processLibraryClass(source);
                            return null;
                        }
                    });
                } finally {
                    inputStream.close();
                }
            }
        } finally {
            jarFile.close();
        }
    }

    private void processLibraryClass(byte[] source) throws IOException {
        ClassReader classReader = new ClassReader(source);
        classReader.accept(
                new ClassStructureVisitor<T>(mGraph, null, null),
                ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
    }

    private void waitForAllTasks() {
        try {
            mExecutor.waitForTasksWithQuickFail(true);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (LoggedErrorException e) {
            throw new RuntimeException(e);
        }
    }

    private void decrementCounter(
            T member,
            DependencyType dependencyType,
            ShrinkType shrinkType,
            @Nullable ImmutableMap<ShrinkType, Set<T>> modifiedClasses) {
        if (mGraph.decrementAndCheck(member, dependencyType, shrinkType)) {
            if (modifiedClasses != null) {
                modifiedClasses.get(shrinkType).add(mGraph.getClassForMember(member));
            }
            for (Dependency<T> dependency : mGraph.getDependencies(member)) {
                decrementCounter(dependency.target, dependency.type, shrinkType, modifiedClasses);
            }
        }
    }

    @NonNull
    private Set<Dependency<T>> getDependencies(MethodNode methodNode) {
        final Set<Dependency<T>> deps = Sets.newHashSet();
        methodNode.accept(new DependencyFinderVisitor<T>(mGraph, null, null, null) {
            @Override
            protected void handleDependency(T source, T target, DependencyType type) {
                deps.add(new Dependency<T>(target, type));
            }
        });
        return deps;
    }

    public void handleFileChanges(
            Map<TransformInput, TransformOutput> inputOutputs,
            final ImmutableMap<ShrinkType, KeepRules> keepRules) throws IOException {
        mGraph.loadState();

        final ImmutableMap<ShrinkType, Set<T>> modifiedClasses = buildMapPerShrinkType(keepRules);

        for (final Map.Entry<TransformInput, TransformOutput> entry : inputOutputs.entrySet()) {
            TransformInput input = entry.getKey();
            Set<Map.Entry<File, TransformInput.FileStatus>> changedFiles =
                    input.getChangedFiles().entrySet();
            for (final Map.Entry<File, TransformInput.FileStatus> changedFile : changedFiles) {
                mExecutor.execute(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        switch (changedFile.getValue()) {
                            case ADDED:
                                throw todo("new file added");
                            case REMOVED:
                                throw todo("removed file");
                            case CHANGED:
                                processChangedClassFile(
                                        changedFile.getKey(),
                                        keepRules,
                                        modifiedClasses);
                                break;
                        }
                        return null;
                    }
                });
            }
        }

        waitForAllTasks();

        for (ShrinkType shrinkType : keepRules.keySet()) {
            updateClassFiles(modifiedClasses.get(shrinkType), inputOutputs);
        }

        waitForAllTasks();
    }

    private void incrementCounter(
            T member,
            DependencyType dependencyType,
            ShrinkType shrinkType,
            @Nullable ImmutableMap<ShrinkType, Set<T>> modifiedClasses) {
        if (mGraph.incrementAndCheck(member, dependencyType, shrinkType)) {
            if (modifiedClasses != null) {
                modifiedClasses.get(shrinkType).add(mGraph.getClassForMember(member));
            }
            for (Dependency<T> dependency : mGraph.getDependencies(member)) {
                incrementCounter(dependency.target, dependency.type, shrinkType, modifiedClasses);
            }
        }
    }

    private void processChangedClassFile(
            File classFile,
            Map<ShrinkType, KeepRules> keepRules,
            ImmutableMap<ShrinkType, Set<T>> modifiedClasses) throws IOException {
        // TODO: Use the visitor API to save memory.
        ClassNode classNode = readClassNode(classFile);
        T klass = mGraph.getClassReference(classNode.name);

        for (ShrinkType shrinkType : keepRules.keySet()) {
            // If the class is in the output, it needs to be rewritten, to reflect changes.
            if (mGraph.isReachable(klass, shrinkType)) {
                modifiedClasses.get(shrinkType).add(klass);
            }
        }

        Set<T> oldMembers = mGraph.getMembers(klass);

        //noinspection unchecked - ASM doesn't use generics.
        for (MethodNode methodNode : (List<MethodNode>) classNode.methods) {
            T method = mGraph.getMemberReference(classNode.name, methodNode.name, methodNode.desc);

            if (!oldMembers.contains(method)) {
                throw todo("added method");
            } else {
                Set<Dependency<T>> oldDeps =  mGraph.getDependencies(method);
                Set<Dependency<T>> currentDeps = getDependencies(methodNode);

                for (Dependency<T> addedDep : Sets.difference(currentDeps, oldDeps)) {
                    mGraph.addDependency(method, addedDep.target, addedDep.type);
                    for (ShrinkType shrinkType : keepRules.keySet()) {
                        if (mGraph.isReachable(method, shrinkType)) {
                            incrementCounter(addedDep.target, addedDep.type, shrinkType, modifiedClasses);
                        }
                    }
                }

                for (Dependency<T> removedDep : Sets.difference(oldDeps, currentDeps)) {
                    mGraph.removeDependency(method, removedDep);
                    for (ShrinkType shrinkType : keepRules.keySet()) {
                        if (mGraph.isReachable(method, shrinkType)) {
                            decrementCounter(removedDep.target, removedDep.type, shrinkType, modifiedClasses);
                        }
                    }
                }

                // Keep only unprocessed members, so we know which ones were deleted.
                oldMembers.remove(method);
            }
        }

        // TODO: process fields

        if (!oldMembers.isEmpty()) {
            throw todo("deleted member");
        }
    }

    private void processNewClassFile(
            File classFile,
            Map<ShrinkType, KeepRules> keepRules,
            ImmutableMap<ShrinkType, Set<T>> toIncrement,
            Set<T> virtualMethods,
            Set<UnresolvedReference<T>> unresolvedReferences) throws IOException {
        // TODO: Can we run keep rules in a visitor?
        // TODO: See if computing all these things on the class nodes is faster (on big projects).
        ClassNode classNode = new ClassNode(Opcodes.ASM5);
        ClassVisitor depsFinder =
                new DependencyFinderVisitor<T>(mGraph, classNode, virtualMethods,
                        unresolvedReferences) {
            @Override
            protected void handleDependency(T source, T target, DependencyType type) {
                mGraph.addDependency(source, target, type);
            }
        };
        ClassVisitor structureVisitor =
                new ClassStructureVisitor<T>(mGraph, classFile, depsFinder);
        ClassReader classReader = new ClassReader(Files.toByteArray(classFile));
        classReader.accept(structureVisitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        T klass = mGraph.getClassReference(classNode.name);
        //noinspection unchecked - ASM doesn't use generics.
        for (MethodNode methodNode : (List<MethodNode>) classNode.methods) {
            T method = mGraph.getMemberReference(classNode.name, methodNode.name, methodNode.desc);

            for (Map.Entry<ShrinkType, KeepRules> entry : keepRules.entrySet()) {
                if (entry.getValue().keep(classNode, methodNode)) {
                    toIncrement.get(entry.getKey()).add(method);
                    toIncrement.get(entry.getKey()).add(klass);
                }
            }
        }
    }

    public void run(
            @NonNull Map<TransformInput, TransformOutput> inputOutputs,
            @NonNull Collection<TransformInput> referencedClasses,
            @NonNull ImmutableMap<ShrinkType, KeepRules> keepRules,
            boolean saveState) throws IOException {
        mGraph.removeStoredState();

        for (TransformOutput output : inputOutputs.values()) {
            FileUtils.emptyFolder(output.getOutFile());
        }

        ImmutableMap<ShrinkType, Set<T>> toIncrement = buildMapPerShrinkType(keepRules);

        buildGraph(inputOutputs.keySet(), referencedClasses, keepRules, toIncrement);
        setCounters(keepRules, toIncrement);
        writeOutput(inputOutputs);

        if (saveState) {
            mGraph.saveState();
        }
    }

    private void writeOutput(
            Map<TransformInput, TransformOutput> inputOutputs) throws IOException {
        updateClassFiles(mGraph.getClassesToKeep(ShrinkType.SHRINK), inputOutputs);
    }

    private void setCounters(ImmutableMap<ShrinkType, KeepRules> keepRules,
            ImmutableMap<ShrinkType, Set<T>> toIncrement) {
        // TODO: Parallelize.
        for (ShrinkType shrinkType : keepRules.keySet()) {
            for (T member : toIncrement.get(shrinkType)) {
                incrementCounter(member, DependencyType.REQUIRED, shrinkType, null);
            }

        }
    }

    private void updateClassFiles(
            Iterable<T> classesToWrite,
            Map<TransformInput, TransformOutput> inputOutputs) throws IOException {
        for (T klass : classesToWrite) {
            File classFile = mGraph.getClassFile(klass);
            Optional<File> outputFile = chooseOutputFile(classFile, inputOutputs);
            if (!outputFile.isPresent()) {
                // The class is from code we don't control.
                continue;
            }
            Files.createParentDirs(outputFile.get());
            Files.write(
                    rewrite(classFile,
                            mGraph.getMembersToKeep(klass, ShrinkType.SHRINK),
                            new Predicate<String>() {
                                @Override
                                public boolean apply(String input) {
                                    return mGraph.keepClass(input, ShrinkType.SHRINK);
                                }
                            }),
                    outputFile.get());
        }
    }

    public enum ShrinkType {
        SHRINK, LEGACY_MULTIDEX
    }

    static class UnresolvedReference<T> {
        final T method;
        final T target;
        final int opcode;

        UnresolvedReference(T method, T target, int opcode) {
            this.method = method;
            this.target = target;
            this.opcode = opcode;
        }
    }
}
