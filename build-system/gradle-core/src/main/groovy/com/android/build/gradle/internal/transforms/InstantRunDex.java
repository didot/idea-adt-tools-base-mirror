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

package com.android.build.gradle.internal.transforms;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.build.api.transform.Context;
import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformOutputProvider;
import com.android.build.gradle.AndroidGradleOptions;
import com.android.build.gradle.OptionalCompilationStep;
import com.android.build.gradle.internal.LoggerWrapper;
import com.android.build.gradle.internal.incremental.BuildInfoGeneratorTask;
import com.android.build.gradle.internal.incremental.InstantRunBuildContext;
import com.android.build.gradle.internal.scope.VariantScope;
import com.android.build.gradle.internal.transforms.InstantRunVerifierTransform.VerificationResult;
import com.android.builder.core.AndroidBuilder;
import com.android.builder.core.DexOptions;
import com.android.ide.common.process.LoggedProcessOutputHandler;
import com.android.ide.common.process.ProcessException;
import com.android.sdklib.AndroidVersion;
import com.android.utils.FileUtils;
import com.android.utils.ILogger;
import com.google.common.base.CaseFormat;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import org.gradle.api.logging.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Non incremental transform that looks for a file named "incrementalChanges.txt" in its input
 * and will use the file's content to get a list of .class files to dex in order to produce an
 * incremental dex file that can contains the delta changes from the last invocation.
 */
public class InstantRunDex extends Transform {

    @NonNull
    private final AndroidBuilder androidBuilder;

    @NonNull
    private final DexOptions dexOptions;

    @NonNull
    private final ILogger logger;

    @NonNull
    private final Set<QualifiedContent.ContentType> inputTypes;

    @NonNull
    private final InstantRunBuildType buildType;

    @NonNull
    private final VariantScope variantScope;

    public InstantRunDex(
            @NonNull VariantScope variantScope,
            @NonNull InstantRunBuildType buildType,
            @NonNull AndroidBuilder androidBuilder,
            @NonNull DexOptions dexOptions,
            @NonNull Logger logger,
            @NonNull Set<QualifiedContent.ContentType> inputTypes) {
        this.variantScope = variantScope;
        this.buildType = buildType;
        this.androidBuilder = androidBuilder;
        this.dexOptions = dexOptions;
        this.logger = new LoggerWrapper(logger);
        this.inputTypes = inputTypes;
    }

    @Override
    public void transform(@NonNull Context context, @NonNull Collection<TransformInput> inputs,
            @NonNull Collection<TransformInput> referencedInputs,
            @Nullable TransformOutputProvider output,
            boolean isIncremental) throws IOException, TransformException, InterruptedException {

        File outputFolder = buildType.getOutputFolder(variantScope);

        boolean changesAreCompatible =
                variantScope.getInstantRunBuildContext().hasPassedVerification();
        boolean restartDexRequested =
                variantScope.getGlobalScope().isActive(OptionalCompilationStep.RESTART_DEX_ONLY);

        switch(buildType) {
            case RELOAD:
                if (!changesAreCompatible || restartDexRequested) {
                    FileUtils.emptyFolder(outputFolder);
                    return;
                }
                break;
            case RESTART:
                if (changesAreCompatible && !restartDexRequested) {
                    // do nothing, let the incrementalChanges.txt accumulate all changes until
                    // we are asked to produce a restart.dex or the verifier flagged the changes.
                    if (outputFolder.exists()) {
                        for (File file : outputFolder.listFiles()) {
                            if (!file.getName().equals("build-info.xml") &&
                                    !file.delete()) {
                                logger.warning("Cannot delete " + file);
                            }
                        }
                    }
                    return;
                }
                break;
            default:
                throw new RuntimeException("Unhandled build type " + buildType);
        }

        // create a tmp jar file.
        File classesJar = new File(outputFolder, "classes.jar");
        if (classesJar.exists()) {
            classesJar.delete();
        }
        Files.createParentDirs(classesJar);
        JarOutputStream jarOutputStream = null;

        try {

            for (TransformInput input : referencedInputs) {
                for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                    if (!directoryInput.getContentTypes().containsAll(inputTypes)) {
                        continue;
                    }
                    File folder = directoryInput.getFile();
                    File incremental = buildType.getIncrementalChangesFile(variantScope);
                    if (!incremental.exists()) {
                        // done
                        continue;
                    }
                    List<String> filesToProcess = Files.readLines(incremental, Charsets.UTF_8);
                    // delete the incremental changes file to reset the list of changes
                    if (!incremental.delete()) {
                        throw new RuntimeException("Cannot delete " + incremental);
                    }
                    if (filesToProcess == null || filesToProcess.isEmpty()) {
                        return;
                    }

                    if (jarOutputStream == null) {
                        jarOutputStream = new JarOutputStream(new FileOutputStream(classesJar));
                    }

                    for (String fileToProcess : filesToProcess) {
                        // todo: check that file to process belongs to folder.
                        copyFileInJar(folder, new File(fileToProcess), jarOutputStream);
                    }
                }
            }
        } finally {
            if (jarOutputStream != null) {
                jarOutputStream.close();
            }
        }
        final ImmutableList.Builder<File> inputFiles = ImmutableList.builder();
        inputFiles.add(classesJar);

        try {
            variantScope.getInstantRunBuildContext().startRecording(
                    InstantRunBuildContext.TaskType.INSTANT_RUN_DEX);
            androidBuilder.convertByteCode(inputFiles.build(),
                    outputFolder,
                    false /* multiDexEnabled */,
                    null /*getMainDexListFile */,
                    new OverridingDexOptions(dexOptions),
                    ImmutableList.<String>of() /* getAdditionalParameters */,
                    false /* incremental */,
                    true /* optimize */,
                    new LoggedProcessOutputHandler(logger),
                    true);
            variantScope.getInstantRunBuildContext().addChangedFile(
                    buildType == InstantRunBuildType.RELOAD
                            ? InstantRunBuildContext.FileType.RELOAD_DEX
                            : InstantRunBuildContext.FileType.RESTART_DEX,
                    new File(outputFolder, "classes.dex"));
        } catch (ProcessException e) {
            throw new TransformException(e);
        } finally {
            variantScope.getInstantRunBuildContext().stopRecording(
                    InstantRunBuildContext.TaskType.INSTANT_RUN_DEX);
        }
    }

    private static void copyFileInJar(File inputDir, File inputFile, JarOutputStream jarOutputStream)
            throws IOException {

        String entryName = inputFile.getPath().substring(inputDir.getPath().length() + 1);
        JarEntry jarEntry = new JarEntry(entryName);
        jarOutputStream.putNextEntry(jarEntry);
        Files.copy(inputFile, jarOutputStream);
        jarOutputStream.closeEntry();
    }

    @NonNull
    @Override
    public String getName() {
        return "instant+" + CaseFormat.UPPER_UNDERSCORE.to(
                CaseFormat.LOWER_CAMEL, buildType.name()) + "Dex";
    }

    @NonNull
    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return inputTypes;
    }

    @NonNull
    @Override
    public Set<QualifiedContent.Scope> getScopes() {
        return ImmutableSet.of();
    }

    @NonNull
    @Override
    public Set<QualifiedContent.Scope> getReferencedScopes() {
        return Sets.immutableEnumSet(QualifiedContent.Scope.PROJECT);
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    /**
     * DexOptions overriding values for the incremental InstantRun modes.
     * TODO: remove once dex compilation is in memory or using Jack exclusively.
     */
    private static class OverridingDexOptions implements DexOptions {
        private final DexOptions delegate;

        private OverridingDexOptions(DexOptions delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean getIncremental() {
            return delegate.getIncremental();
        }

        @Override
        public boolean getPreDexLibraries() {
            return delegate.getPreDexLibraries();
        }

        @Override
        public boolean getJumboMode() {
            return delegate.getJumboMode();
        }

        @Override
        public boolean getDexInProcess() {
            // back door system property to force using the out of process dexer.
            return !Boolean.getBoolean("instant-run.force.dex.oop");
        }

        @Nullable
        @Override
        public String getJavaMaxHeapSize() {
            return delegate.getJavaMaxHeapSize();
        }

        @Nullable
        @Override
        public Integer getThreadCount() {
            return delegate.getThreadCount();
        }
    }
}
