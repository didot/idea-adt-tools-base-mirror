/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.build.gradle.tasks;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.build.api.artifact.BuildableArtifact;
import com.android.build.gradle.AndroidGradleOptions;
import com.android.build.gradle.internal.core.VariantConfiguration;
import com.android.build.gradle.internal.packaging.IncrementalPackagerBuilder;
import com.android.build.gradle.internal.scope.BuildElementsTransformParams;
import com.android.build.gradle.internal.scope.BuildElementsTransformRunnable;
import com.android.build.gradle.internal.scope.ExistingBuildElements;
import com.android.build.gradle.internal.scope.InternalArtifactType;
import com.android.build.gradle.internal.scope.VariantScope;
import com.android.build.gradle.internal.tasks.AndroidBuilderTask;
import com.android.build.gradle.internal.tasks.Workers;
import com.android.build.gradle.internal.tasks.factory.VariantTaskCreationAction;
import com.android.build.gradle.internal.variant.BaseVariantData;
import com.android.builder.files.IncrementalRelativeFileSets;
import com.android.builder.internal.packaging.IncrementalPackager;
import com.android.builder.model.SigningConfig;
import com.android.ide.common.build.ApkInfo;
import com.android.ide.common.workers.WorkerExecutorFacade;
import com.android.utils.FileUtils;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import javax.inject.Inject;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.workers.WorkerExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Package each split resources into a specific signed apk file. */
public class PackageSplitRes extends AndroidBuilderTask {

    private SigningConfig signingConfig;
    private File incrementalDir;
    public BuildableArtifact processedResources;
    public File splitResApkOutputDirectory;

    @InputFiles
    public BuildableArtifact getProcessedResources() {
        return processedResources;
    }

    @OutputDirectory
    public File getSplitResApkOutputDirectory() {
        return splitResApkOutputDirectory;
    }

    @Nested
    @Optional
    public SigningConfig getSigningConfig() {
        return signingConfig;
    }

    private final WorkerExecutorFacade workers;

    @Inject
    public PackageSplitRes(WorkerExecutor workerExecutor) {
        this.workers = Workers.INSTANCE.getWorker(workerExecutor);
    }

    @TaskAction
    protected void doFullTaskAction() {
        ExistingBuildElements.from(
                        InternalArtifactType.DENSITY_OR_LANGUAGE_SPLIT_PROCESSED_RES,
                        processedResources)
                .transform(
                        workers,
                        PackageSplitResTransformRunnable.class,
                        ((apkInfo, file) ->
                                new PackageSplitResTransformParams(apkInfo, file, this)))
                .into(
                        InternalArtifactType.DENSITY_OR_LANGUAGE_PACKAGED_SPLIT,
                        splitResApkOutputDirectory);
    }

    private static class PackageSplitResTransformRunnable extends BuildElementsTransformRunnable {

        @Inject
        public PackageSplitResTransformRunnable(@NotNull PackageSplitResTransformParams params) {
            super(params);
        }

        @Override
        public void run() {
            PackageSplitResTransformParams params = (PackageSplitResTransformParams) getParams();
            File intDir =
                    new File(
                            params.incrementalDir,
                            FileUtils.join(params.apkInfo.getFilterName(), "tmp"));
            try {
                FileUtils.cleanOutputDir(intDir);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            try (IncrementalPackager pkg =
                    new IncrementalPackagerBuilder(IncrementalPackagerBuilder.ApkFormat.FILE)
                            .withSigning(params.signingConfig)
                            .withOutputFile(params.output)
                            .withKeepTimestampsInApk(params.keepTimestampsInApk)
                            .withIntermediateDir(intDir)
                            .build()) {
                pkg.updateAndroidResources(IncrementalRelativeFileSets.fromZip(params.input));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static class PackageSplitResTransformParams extends BuildElementsTransformParams {
        private final ApkInfo apkInfo;
        private final File input;
        private final File output;
        private final File incrementalDir;
        private final SigningConfig signingConfig;
        private final boolean keepTimestampsInApk;

        PackageSplitResTransformParams(ApkInfo apkInfo, File input, PackageSplitRes task) {
            if (input == null) {
                throw new RuntimeException("Cannot find processed resources for " + apkInfo);
            }
            this.apkInfo = apkInfo;
            this.input = input;
            output =
                    new File(
                            task.splitResApkOutputDirectory,
                            getOutputFileNameForSplit(
                                    apkInfo,
                                    (String)
                                            task.getProject()
                                                    .getProperties()
                                                    .get("archivesBaseName"),
                                    task.signingConfig != null));
            incrementalDir = task.incrementalDir;
            signingConfig = task.signingConfig;
            keepTimestampsInApk = AndroidGradleOptions.keepTimestampsInApk(task.getProject());
        }

        @Nullable
        @Override
        public File getOutput() {
            return output;
        }
    }

    public static String getOutputFileNameForSplit(
            final ApkInfo apkData, String archivesBaseName, boolean isSigned) {
        String apkName = archivesBaseName + "-" + apkData.getBaseName();
        return apkName + (isSigned ? "" : "-unsigned") + SdkConstants.DOT_ANDROID_PACKAGE;
    }

    // ----- CreationAction -----

    public static class CreationAction extends VariantTaskCreationAction<PackageSplitRes> {

        private File splitResApkOutputDirectory;

        public CreationAction(VariantScope scope) {
            super(scope);
        }

        @Override
        @NonNull
        public String getName() {
            return getVariantScope().getTaskName("package", "SplitResources");
        }

        @Override
        @NonNull
        public Class<PackageSplitRes> getType() {
            return PackageSplitRes.class;
        }

        @Override
        public void preConfigure(@NonNull String taskName) {
            super.preConfigure(taskName);
            splitResApkOutputDirectory =
                    getVariantScope()
                            .getArtifacts()
                            .appendArtifact(
                                    InternalArtifactType.DENSITY_OR_LANGUAGE_PACKAGED_SPLIT,
                                    taskName,
                                    "out");
        }

        @Override
        public void handleProvider(@NonNull TaskProvider<? extends PackageSplitRes> taskProvider) {
            super.handleProvider(taskProvider);
            getVariantScope().getTaskContainer().setPackageSplitResourcesTask(taskProvider);
        }

        @Override
        public void configure(@NonNull PackageSplitRes task) {
            super.configure(task);
            VariantScope scope = getVariantScope();

            BaseVariantData variantData = scope.getVariantData();
            final VariantConfiguration config = variantData.getVariantConfiguration();

            task.processedResources =
                    scope.getArtifacts().getFinalArtifactFiles(InternalArtifactType.PROCESSED_RES);
            task.signingConfig = config.getSigningConfig();
            task.splitResApkOutputDirectory = splitResApkOutputDirectory;
            task.incrementalDir = scope.getIncrementalDir(getName());
        }
    }
}
