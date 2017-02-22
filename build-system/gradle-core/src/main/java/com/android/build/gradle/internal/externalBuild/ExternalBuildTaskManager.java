/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.build.gradle.internal.externalBuild;

import static com.google.common.base.Preconditions.checkState;

import com.android.annotations.NonNull;
import com.android.build.VariantOutput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.gradle.internal.ExtraModelInfo;
import com.android.build.gradle.internal.InstantRunTaskManager;
import com.android.build.gradle.internal.TaskContainerAdaptor;
import com.android.build.gradle.internal.aapt.AaptGeneration;
import com.android.build.gradle.internal.dsl.DexOptions;
import com.android.build.gradle.internal.dsl.SigningConfig;
import com.android.build.gradle.internal.ide.AaptOptionsImpl;
import com.android.build.gradle.internal.incremental.BuildInfoLoaderTask;
import com.android.build.gradle.internal.incremental.BuildInfoWriterTask;
import com.android.build.gradle.internal.incremental.InstantRunPatchingPolicy;
import com.android.build.gradle.internal.pipeline.ExtendedContentType;
import com.android.build.gradle.internal.pipeline.OriginalStream;
import com.android.build.gradle.internal.pipeline.TransformManager;
import com.android.build.gradle.internal.pipeline.TransformTask;
import com.android.build.gradle.internal.scope.AndroidTask;
import com.android.build.gradle.internal.scope.AndroidTaskRegistry;
import com.android.build.gradle.internal.scope.PackagingScope;
import com.android.build.gradle.internal.scope.SplitFactory;
import com.android.build.gradle.internal.scope.SplitScope;
import com.android.build.gradle.internal.scope.TaskOutputHolder;
import com.android.build.gradle.internal.scope.VariantScope;
import com.android.build.gradle.internal.transforms.ExtractJarsTransform;
import com.android.build.gradle.internal.transforms.InstantRunSliceSplitApkBuilder;
import com.android.build.gradle.internal.transforms.PreDexTransform;
import com.android.build.gradle.options.ProjectOptions;
import com.android.build.gradle.tasks.PackageApplication;
import com.android.build.gradle.tasks.PreColdSwapTask;
import com.android.builder.core.AndroidBuilder;
import com.android.builder.core.BuilderConstants;
import com.android.builder.core.DefaultDexOptions;
import com.android.builder.core.DefaultManifestParser;
import com.android.builder.dexing.DexingMode;
import com.android.builder.profile.Recorder;
import com.android.builder.signing.DefaultSigningConfig;
import com.android.ide.common.build.ApkData;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.util.EnumSet;
import java.util.Optional;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

/**
 * Task Manager for External Build system integration.
 */
class ExternalBuildTaskManager {

    private final Project project;
    @NonNull private final ProjectOptions projectOptions;
    private final AndroidTaskRegistry androidTasks = new AndroidTaskRegistry();
    private final TaskContainerAdaptor tasks;
    private final Recorder recorder;

    ExternalBuildTaskManager(
            @NonNull Project project,
            @NonNull ProjectOptions projectOptions,
            @NonNull Recorder recorder) {
        this.project = project;
        this.projectOptions = projectOptions;
        this.tasks = new TaskContainerAdaptor(project.getTasks());
        this.recorder = recorder;
    }

    void createTasks(@NonNull ExternalBuildExtension externalBuildExtension) throws Exception {

        // anchor task
        AndroidTask<ExternalBuildAnchorTask> externalBuildAnchorTask =
                androidTasks.create(tasks, new ExternalBuildAnchorTask.ConfigAction());

        ExternalBuildContext externalBuildContext = new ExternalBuildContext(
                externalBuildExtension);

        File file = project.file(externalBuildExtension.buildManifestPath);
        ExternalBuildManifestLoader.loadAndPopulateContext(
                new File(externalBuildExtension.getExecutionRoot()),
                file, project, externalBuildContext);

        ExtraModelInfo modelInfo = new ExtraModelInfo(project);
        TransformManager transformManager = new TransformManager(
                project, androidTasks, modelInfo, recorder);

        transformManager.addStream(OriginalStream.builder(project)
                .addContentType(QualifiedContent.DefaultContentType.CLASSES)
                .addScope(QualifiedContent.Scope.PROJECT)
                .setJars(externalBuildContext::getInputJarFiles)
                .build());

        // add an empty java resources directory for now.
        // the folder itself doesn't actually matter, but it has to be consistent
        // for gradle's up-to-date check
        transformManager.addStream(OriginalStream.builder(project)
                .addContentType(QualifiedContent.DefaultContentType.RESOURCES)
                .addScope(QualifiedContent.Scope.PROJECT)
                .setFolder(new File(project.getBuildDir(), "temp/streams/resources"))
                .build());

        // add an empty native libraries resources directory for now.
        // the folder itself doesn't actually matter, but it has to be consistent
        // for gradle's up-to-date check
        transformManager.addStream(OriginalStream.builder(project)
                .addContentType(ExtendedContentType.NATIVE_LIBS)
                .addScope(QualifiedContent.Scope.PROJECT)
                .setFolder(new File(project.getBuildDir(), "temp/streams/native_libs"))
                .build());

        ExternalBuildGlobalScope globalScope =
                new ExternalBuildGlobalScope(project, projectOptions);
        File androidManifestFile =
                new File(externalBuildContext.getExecutionRoot(),
                        externalBuildContext
                                .getBuildManifest()
                                .getAndroidManifest()
                                .getExecRootPath());

        File processedAndroidResourcesFile =
                new File(externalBuildContext.getExecutionRoot(),
                        externalBuildContext.getBuildManifest().getResourceApk().getExecRootPath());

        ApkData mainApkData =
                new SplitFactory.DefaultApkData(
                        VariantOutput.OutputType.MAIN,
                        "",
                        "main",
                        "main",
                        "main",
                        ImmutableList.of());

        ExternalBuildVariantScope variantScope =
                new ExternalBuildVariantScope(
                        globalScope,
                        project.getBuildDir(),
                        externalBuildContext,
                        new AaptOptionsImpl(null, null, false, null),
                        new DefaultManifestParser(androidManifestFile),
                        ImmutableList.of(mainApkData));

        // massage the manifest file.

        // Extract the passed jars into folders as the InstantRun transforms can only handle folders.
        ExtractJarsTransform extractJarsTransform = new ExtractJarsTransform(
                ImmutableSet.of(QualifiedContent.DefaultContentType.CLASSES),
                ImmutableSet.of(QualifiedContent.Scope.PROJECT));
        Optional<AndroidTask<TransformTask>> extractJarsTask =
                transformManager.addTransform(tasks, variantScope, extractJarsTransform);

        InstantRunTaskManager instantRunTaskManager =
                new InstantRunTaskManager(
                        project.getLogger(),
                        variantScope,
                        transformManager,
                        androidTasks,
                        tasks,
                        recorder);

        AndroidTask<BuildInfoLoaderTask> buildInfoLoaderTask =
                instantRunTaskManager.createInstantRunAllTasks(
                        new DexOptions(modelInfo),
                        externalBuildContext.getAndroidBuilder()::getDexByteCodeConverter,
                        extractJarsTask.orElse(null),
                        externalBuildAnchorTask,
                        EnumSet.of(QualifiedContent.Scope.PROJECT),
                        project.files(androidManifestFile),
                        project.files(processedAndroidResourcesFile),
                        false /* addResourceVerifier */);

        extractJarsTask.ifPresent(t -> t.dependsOn(tasks, buildInfoLoaderTask));

        AndroidTask<PreColdSwapTask> preColdswapTask = instantRunTaskManager
                .createPreColdswapTask(project);

        if (variantScope.getBuildContext().getPatchingPolicy()
                != InstantRunPatchingPolicy.PRE_LOLLIPOP) {
            instantRunTaskManager.createSlicerTask();
        }

        createDexTasks(externalBuildContext, transformManager, variantScope);

        SigningConfig manifestSigningConfig = createManifestSigningConfig(externalBuildContext);

        PackagingScope packagingScope =
                new ExternalBuildPackagingScope(
                        project, externalBuildContext, variantScope, transformManager,
                        manifestSigningConfig);

        SplitScope splitScope = packagingScope.getSplitScope();
        splitScope.addOutputForSplit(
                TaskOutputHolder.TaskOutputType.MERGED_MANIFESTS, mainApkData, androidManifestFile);
        splitScope.addOutputForSplit(
                TaskOutputHolder.TaskOutputType.INSTANT_RUN_MERGED_MANIFESTS,
                mainApkData,
                androidManifestFile);
        splitScope.addOutputForSplit(
                TaskOutputHolder.TaskOutputType.PROCESSED_RES,
                mainApkData,
                processedAndroidResourcesFile);

        packagingScope.addTaskOutput(
                TaskOutputHolder.TaskOutputType.MERGED_MANIFESTS,
                project.files(androidManifestFile));

        // TODO: Where should assets come from?
        packagingScope.addTaskOutput(
                TaskOutputHolder.TaskOutputType.MERGED_ASSETS, project.files());

        Logger logger = Logging.getLogger(ExternalBuildTaskManager.class);

        InstantRunSliceSplitApkBuilder slicesApkBuilder =
                new InstantRunSliceSplitApkBuilder(
                        logger,
                        project,
                        variantScope.getBuildContext(),
                        externalBuildContext.getAndroidBuilder(),
                        packagingScope,
                        packagingScope.getSigningConfig(),
                        AaptGeneration.fromProjectOptions(globalScope.getProjectOptions()),
                        packagingScope.getAaptOptions(),
                        packagingScope.getInstantRunSplitApkOutputFolder(),
                        packagingScope.getInstantRunSupportDir());

        Optional<AndroidTask<TransformTask>> transformTaskAndroidTask =
                transformManager.addTransform(tasks, variantScope, slicesApkBuilder);

        AndroidTask<PackageApplication> packageApp =
                androidTasks.create(
                        tasks,
                        new PackageApplication.StandardConfigAction(
                                packagingScope,
                                project.getBuildDir(),
                                variantScope.getBuildContext().getPatchingPolicy(),
                                project.files(processedAndroidResourcesFile),
                                project.files(androidManifestFile),
                                VariantScope.TaskOutputType.INSTANT_RUN_MERGED_MANIFESTS,
                                variantScope.getSplitScope(),
                                TaskOutputHolder.TaskOutputType.APK));

        transformTaskAndroidTask.ifPresent(
                transformTaskAndroidTask1 ->
                        packageApp.dependsOn(tasks, transformTaskAndroidTask1));

        variantScope.setPackageApplicationTask(packageApp);

        AndroidTask<BuildInfoWriterTask> buildInfoWriterTask = androidTasks.create(tasks,
                new BuildInfoWriterTask.ConfigAction(variantScope, logger));

        // finally, generate the build-info.xml
        instantRunTaskManager.configureBuildInfoWriterTask(buildInfoWriterTask, packageApp);

        externalBuildAnchorTask.dependsOn(tasks, packageApp);
        externalBuildAnchorTask.dependsOn(tasks, buildInfoWriterTask);

        for (AndroidTask<? extends DefaultTask> task : variantScope.getColdSwapBuildTasks()) {
            task.dependsOn(tasks, preColdswapTask);
        }
    }

    private void createDexTasks(
            @NonNull ExternalBuildContext externalBuildContext,
            @NonNull TransformManager transformManager,
            @NonNull ExternalBuildVariantScope variantScope) {
        AndroidBuilder androidBuilder = externalBuildContext.getAndroidBuilder();
        InstantRunPatchingPolicy patchingPolicy =
                variantScope.getBuildContext().getPatchingPolicy();
        final DexingMode dexingMode = DexingMode.NATIVE_MULTIDEX;
        //if (patchingPolicy != null && patchingPolicy.useMultiDex()) {
        //    dexingMode = DexingMode.NATIVE_MULTIDEX;
        //} else {
        //    dexingMode = DexingMode.MONO_DEX;
        //}

        PreDexTransform preDexTransform =
                new PreDexTransform(
                        new DefaultDexOptions(),
                        androidBuilder,
                        variantScope.getGlobalScope().getBuildCache(),
                        dexingMode,
                        variantScope.getBuildContext().isInInstantRunMode());
        transformManager.addTransform(tasks, variantScope, preDexTransform);

        //if (dexingMode != DexingMode.NATIVE_MULTIDEX) {
        //    DexTransform dexTransform =
        //            new DexTransform(
        //                    new DefaultDexOptions(),
        //                    dexingMode,
        //                    true,
        //                    null,
        //                    verifyNotNull(androidBuilder.getTargetInfo(), "Target Info not set."),
        //                    androidBuilder.getDexByteCodeConverter(),
        //                    androidBuilder.getErrorReporter());
        //
        //    transformManager.addTransform(tasks, variantScope, dexTransform);
        //}
    }

    private static SigningConfig createManifestSigningConfig(
            ExternalBuildContext externalBuildContext) {
        SigningConfig config = new SigningConfig(BuilderConstants.EXTERNAL_BUILD);
        config.setStorePassword(DefaultSigningConfig.DEFAULT_PASSWORD);
        config.setKeyAlias(DefaultSigningConfig.DEFAULT_ALIAS);
        config.setKeyPassword(DefaultSigningConfig.DEFAULT_PASSWORD);

        File keystore =
                new File(
                        externalBuildContext.getExecutionRoot(),
                        externalBuildContext
                                .getBuildManifest()
                                .getDebugKeystore()
                                .getExecRootPath());
        checkState(
                keystore.isFile(),
                "Keystore file from the manifest (%s) does not exist.",
                keystore.getAbsolutePath());
        config.setStoreFile(keystore);

        return config;
    }
}
