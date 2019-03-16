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

package com.android.build.gradle.tasks;

import static com.android.build.gradle.internal.cxx.logging.LoggingEnvironmentKt.info;
import static com.android.build.gradle.internal.cxx.process.ProcessOutputJunctionKt.createProcessOutputJunction;
import static com.android.build.gradle.internal.publishing.AndroidArtifacts.ArtifactScope.ALL;
import static com.android.build.gradle.internal.publishing.AndroidArtifacts.ArtifactType.JNI;
import static com.android.build.gradle.internal.publishing.AndroidArtifacts.ConsumedConfigType.RUNTIME_CLASSPATH;
import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.build.gradle.internal.core.Abi;
import com.android.build.gradle.internal.cxx.json.AndroidBuildGradleJsons;
import com.android.build.gradle.internal.cxx.json.NativeBuildConfigValueMini;
import com.android.build.gradle.internal.cxx.json.NativeLibraryValueMini;
import com.android.build.gradle.internal.cxx.logging.GradleBuildLoggingEnvironment;
import com.android.build.gradle.internal.dsl.CoreExternalNativeBuildOptions;
import com.android.build.gradle.internal.dsl.CoreExternalNativeCmakeOptions;
import com.android.build.gradle.internal.dsl.CoreExternalNativeNdkBuildOptions;
import com.android.build.gradle.internal.scope.VariantScope;
import com.android.build.gradle.internal.tasks.AndroidBuilderTask;
import com.android.build.gradle.internal.tasks.factory.VariantTaskCreationAction;
import com.android.build.gradle.internal.variant.BaseVariantData;
import com.android.builder.core.AndroidBuilder;
import com.android.builder.errors.EvalIssueReporter;
import com.android.ide.common.process.BuildCommandException;
import com.android.ide.common.process.ProcessInfoBuilder;
import com.android.utils.FileUtils;
import com.android.utils.StringHelper;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.wireless.android.sdk.stats.GradleBuildVariant;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskProvider;

/**
 * Task that takes set of JSON files of type NativeBuildConfigValueMini and does build steps with
 * them.
 *
 * <p>It declares no inputs or outputs, as it's supposed to always run when invoked. Incrementality
 * is left to the underlying build system.
 */
public class ExternalNativeBuildTask extends AndroidBuilderTask {

    @Nullable private String buildTargetAbi;
    private Provider<ExternalNativeJsonGenerator> generator;
    private CoreExternalNativeBuildOptions nativeBuildOptions;

    // This placeholder is inserted into the buildTargetsCommand, and then later replaced by the
    // list of libraries that shall be built with a single build tool invocation.
    public static final String BUILD_TARGETS_PLACEHOLDER = "{LIST_OF_TARGETS_TO_BUILD}";

    /** Represents a single build step that, when executed, builds one or more libraries. */
    private static class BuildStep {
        @NonNull private String buildCommand;
        @NonNull private List<NativeLibraryValueMini> libraries;
        @NonNull private File outputFolder;

        // Defines a build step that builds one library with a single command.
        BuildStep(
                @NonNull String buildCommand,
                @NonNull NativeLibraryValueMini library,
                @NonNull File outputFolder) {
            this(buildCommand, Lists.newArrayList(library), outputFolder);
        }

        // Defines a build step that builds one or more libraries with a single command.
        BuildStep(
                @NonNull String buildCommand,
                @NonNull List<NativeLibraryValueMini> libraries,
                @NonNull File outputFolder) {
            this.buildCommand = buildCommand;
            this.libraries = libraries;
            this.outputFolder = outputFolder;
        }
    }

    @TaskAction
    void build() throws BuildCommandException, IOException {
        try (GradleBuildLoggingEnvironment ignore =
                new GradleBuildLoggingEnvironment(getLogger(), getVariantName())) {
            buildImpl();
        }
    }

    private void buildImpl() throws BuildCommandException, IOException {
        info("starting build");
        checkNotNull(getVariantName());
        info("reading expected JSONs");
        List<NativeBuildConfigValueMini> miniConfigs = getNativeBuildConfigValueMinis();
        info("done reading expected JSONs");

        if (getTargets().isEmpty()) {
            info("executing build commands for targets that produce .so files or executables");
        } else {
            verifyTargetsExist(miniConfigs);
        }

        List<BuildStep> buildSteps = Lists.newArrayList();

        for (int miniConfigIndex = 0; miniConfigIndex < miniConfigs.size(); ++miniConfigIndex) {
            NativeBuildConfigValueMini config = miniConfigs.get(miniConfigIndex);
            info("evaluate miniconfig");
            if (config.libraries.isEmpty()) {
                info("no libraries");
                continue;
            }

            List<NativeLibraryValueMini> librariesToBuild = findLibrariesToBuild(config);
            if (librariesToBuild.isEmpty()) {
                info("no libraries to build");
                continue;
            }

            if (!Strings.isNullOrEmpty(config.buildTargetsCommand)) {
                // Build all libraries together in one step, using the names of the artifacts.
                List<String> artifactNames =
                        librariesToBuild
                                .stream()
                                .filter(library -> library.artifactName != null)
                                .map(library -> library.artifactName)
                                .sorted()
                                .distinct()
                                .collect(Collectors.toList());
                String buildTargetsCommand =
                        substituteBuildTargetsCommand(config.buildTargetsCommand, artifactNames);
                buildSteps.add(
                        new BuildStep(
                                buildTargetsCommand,
                                librariesToBuild,
                                getNativeBuildConfigurationsJsons()
                                        .get(miniConfigIndex)
                                        .getParentFile()));
                info("about to build targets " + String.join(", ", artifactNames));
            } else {
                // Build each library separately using multiple steps.
                for (NativeLibraryValueMini libraryValue : librariesToBuild) {
                    assert libraryValue.buildCommand != null;
                    buildSteps.add(
                            new BuildStep(
                                    libraryValue.buildCommand,
                                    libraryValue,
                                    getNativeBuildConfigurationsJsons()
                                            .get(miniConfigIndex)
                                            .getParentFile()));
                    info("about to build %s", libraryValue.buildCommand);
                }
            }
        }

        executeProcessBatch(buildSteps);

        info("check expected build outputs");
        for (NativeBuildConfigValueMini config : miniConfigs) {
            for (String library : config.libraries.keySet()) {
                NativeLibraryValueMini libraryValue = config.libraries.get(library);
                checkNotNull(libraryValue);
                checkNotNull(libraryValue.output);
                checkState(!Strings.isNullOrEmpty(libraryValue.artifactName));
                if (!getTargets().isEmpty() && !getTargets().contains(libraryValue.artifactName)) {
                    continue;
                }
                if (buildSteps.stream().noneMatch(step -> step.libraries.contains(libraryValue))) {
                    // Only need to check existence of output files we expect to create
                    continue;
                }
                if (!libraryValue.output.exists()) {
                    throw new GradleException(
                            String.format(
                                    "Expected output file at %s for target %s"
                                            + " but there was none",
                                    libraryValue.output, libraryValue.artifactName));
                }
                if (libraryValue.abi == null) {
                    throw new GradleException("Expected NativeLibraryValue to have non-null abi");
                }

                // If the build chose to write the library output somewhere besides objFolder
                // then copy to objFolder (reference b.android.com/256515)
                //
                // Since there is now a .so file outside of the standard build/ folder we have to
                // consider clean. Here's how the two files are covered.
                // (1) Gradle plugin deletes the build/ folder. This covers the destination of the
                //     copy.
                // (2) ExternalNativeCleanTask calls the individual clean targets for everything
                //     that was built. This should cover the source of the copy but it is up to the
                //     CMakeLists.txt or Android.mk author to ensure this.
                Abi abi = Abi.getByName(libraryValue.abi);
                if (abi == null) {
                    throw new RuntimeException(
                            String.format("Unknown ABI seen %s", libraryValue.abi));
                }
                File expectedOutputFile =
                        FileUtils.join(
                                getObjFolder(), abi.getName(), libraryValue.output.getName());
                if (!FileUtils.isSameFile(libraryValue.output, expectedOutputFile)) {
                    info(
                            "external build set its own library output location for '%s', "
                                    + "copy to expected location",
                            libraryValue.output.getName());

                    if (expectedOutputFile.getParentFile().mkdirs()) {
                        info("created folder %s", expectedOutputFile.getParentFile());
                    }
                    info("copy file %s to %s", libraryValue.output, expectedOutputFile);
                    Files.copy(libraryValue.output, expectedOutputFile);
                }
            }
        }

        if (!getStlSharedObjectFiles().isEmpty()) {
            info("copy STL shared object files");
            for (Abi abi : getStlSharedObjectFiles().keySet()) {
                File stlSharedObjectFile = checkNotNull(getStlSharedObjectFiles().get(abi));
                File objAbi =
                        FileUtils.join(
                                getObjFolder(), abi.getName(), stlSharedObjectFile.getName());
                if (!objAbi.getParentFile().isDirectory()) {
                    // A build failure can leave the obj/abi folder missing. Just note that case
                    // and continue without copying STL.
                    info(
                            "didn't copy STL file to %s because that folder wasn't created "
                                    + "by the build ",
                            objAbi.getParentFile());
                } else {
                    info("copy file %s to %s", stlSharedObjectFile, objAbi);
                    Files.copy(stlSharedObjectFile, objAbi);
                }
            }
        }

        info("build complete");
    }

    /**
     * @param buildTargetsCommand The build command that can build multiple targets in parallel.
     * @param artifactNames The names of artifacts the build command will build in parallel.
     * @return Replaces the placeholder in the input command with the given artifacts and returns a
     *     command that can be executed directly.
     */
    private static String substituteBuildTargetsCommand(
            @NonNull String buildTargetsCommand, @NonNull List<String> artifactNames) {
        return buildTargetsCommand.replace(
                BUILD_TARGETS_PLACEHOLDER, String.join(" ", artifactNames));
    }

    /**
     * Verifies that all targets provided by the user will be built. Throws GradleException if it
     * detects an unexpected target.
     */
    private void verifyTargetsExist(@NonNull List<NativeBuildConfigValueMini> miniConfigs) {
        // Check the resulting JSON targets against the targets specified in ndkBuild.targets or
        // cmake.targets. If a target name specified by the user isn't present then provide an
        // error to the user that lists the valid target names.
        info("executing build commands for targets: '%s'", Joiner.on(", ").join(getTargets()));

        // Search libraries for matching targets.
        Set<String> matchingTargets = Sets.newHashSet();
        Set<String> unmatchedTargets = Sets.newHashSet();
        for (NativeBuildConfigValueMini config : miniConfigs) {
            for (NativeLibraryValueMini libraryValue : config.libraries.values()) {
                if (getTargets().contains(libraryValue.artifactName)) {
                    matchingTargets.add(libraryValue.artifactName);
                } else {
                    unmatchedTargets.add(libraryValue.artifactName);
                }
            }
        }

        // All targets must be found or it's a build error
        for (String target : getTargets()) {
            if (!matchingTargets.contains(target)) {
                // TODO(emrekultursay): Convert this into a warning.
                throw new GradleException(
                        String.format(
                                "Unexpected native build target %s. Valid values are: %s",
                                target, Joiner.on(", ").join(unmatchedTargets)));
            }
        }
    }

    /**
     * @return List of libraries defined in the input config file, filtered based on the targets
     *     field optionally provided by the user, and other criteria.
     */
    @NonNull
    private List<NativeLibraryValueMini> findLibrariesToBuild(
            @NonNull NativeBuildConfigValueMini config) {
        List<NativeLibraryValueMini> librariesToBuild = Lists.newArrayList();

        for (NativeLibraryValueMini libraryValue : config.libraries.values()) {
            info("evaluate library %s (%s)", libraryValue.artifactName, libraryValue.abi);
            if (!getTargets().isEmpty() && !getTargets().contains(libraryValue.artifactName)) {
                info(
                        "not building target %s because it isn't in targets set",
                        libraryValue.artifactName);
                continue;
            }

            if (Strings.isNullOrEmpty(config.buildTargetsCommand)
                    && Strings.isNullOrEmpty(libraryValue.buildCommand)) {
                // This can happen when there's an externally referenced library.
                info(
                        "not building target %s because there was no buildCommand for the target, "
                                + "nor a buildTargetsCommand for the config",
                        libraryValue.artifactName);
                continue;
            }

            if (getTargets().isEmpty()) {
                if (libraryValue.output == null) {
                    info(
                            "not building target %s because no targets are specified and "
                                    + "library build output file is null",
                            libraryValue.artifactName);
                    continue;
                }

                String extension = Files.getFileExtension(libraryValue.output.getName());
                switch (extension) {
                    case "so":
                        info(
                                "building target library %s because no targets are " + "specified.",
                                libraryValue.artifactName);
                        break;
                    case "":
                        info(
                                "building target executable %s because no targets are "
                                        + "specified.",
                                libraryValue.artifactName);
                        break;
                    default:
                        info(
                                "not building target %s because the type cannot be "
                                        + "determined.",
                                libraryValue.artifactName);
                        continue;
                }
            }

            librariesToBuild.add(libraryValue);
        }

        return librariesToBuild;
    }

    /**
     * Get native build config minis. Also gather stats if they haven't already been gathered for
     * this variant
     *
     * @return the mini configs
     */
    private List<NativeBuildConfigValueMini> getNativeBuildConfigValueMinis() throws IOException {
        // Gather stats only if they haven't been gathered during model build
        if (getStats().getNativeBuildConfigCount() == 0) {
            return AndroidBuildGradleJsons.getNativeBuildMiniConfigs(
                    getNativeBuildConfigurationsJsons(), getStats());
        }
        return AndroidBuildGradleJsons.getNativeBuildMiniConfigs(
                getNativeBuildConfigurationsJsons(), null);
    }

    /**
     * Given a list of build steps, execute each. If there is a failure, processing is stopped at
     * that point.
     */
    private void executeProcessBatch(@NonNull List<BuildStep> buildSteps)
            throws BuildCommandException, IOException {
        for (BuildStep buildStep : buildSteps) {
            List<String> tokens = StringHelper.tokenizeCommandLineToEscaped(buildStep.buildCommand);
            ProcessInfoBuilder processBuilder = new ProcessInfoBuilder();
            processBuilder.setExecutable(tokens.get(0));
            for (int i = 1; i < tokens.size(); ++i) {
                processBuilder.addArgs(tokens.get(i));
            }
            info("%s", processBuilder);

            String logFileSuffix;
            if (buildStep.libraries.size() > 1) {
                logFileSuffix = "targets";
                List<String> targetNames =
                        buildStep
                                .libraries
                                .stream()
                                .map(library -> library.artifactName + "_" + library.abi)
                                .collect(Collectors.toList());
                getLogger()
                        .lifecycle(
                                String.format(
                                        "Build multiple targets %s",
                                        String.join(" ", targetNames)));
            } else {
                checkElementIndex(0, buildStep.libraries.size());
                logFileSuffix =
                        buildStep.libraries.get(0).artifactName
                                + "_"
                                + buildStep.libraries.get(0).abi;
                getLogger().lifecycle(String.format("Build %s", logFileSuffix));
            }

            createProcessOutputJunction(
                            buildStep.outputFolder,
                            "android_gradle_build_" + logFileSuffix,
                            processBuilder,
                            getBuilder(),
                            "")
                    .logStderrToInfo()
                    .logStdoutToInfo()
                    .execute();
        }
    }

    @NonNull
    private Set<String> getTargets() {
        ExternalNativeJsonGenerator jsonGenerator = generator.get();
        switch (jsonGenerator.getNativeBuildSystem()) {
            case CMAKE:
                {
                    CoreExternalNativeCmakeOptions options =
                            checkNotNull(nativeBuildOptions.getExternalNativeCmakeOptions());
                    return options.getTargets();
                }
            case NDK_BUILD:
                {
                    CoreExternalNativeNdkBuildOptions options =
                            checkNotNull(nativeBuildOptions.getExternalNativeNdkBuildOptions());
                    return options.getTargets();
                }
            default:
                throw new RuntimeException(
                        "Unexpected native build system "
                                + jsonGenerator.getNativeBuildSystem().getName());
        }
    }

    @NonNull
    private File getObjFolder() {
        return generator.get().getObjFolder();
    }

    @NonNull
    private List<File> getNativeBuildConfigurationsJsons() {
        ExternalNativeJsonGenerator jsonGenerator = generator.get();

        if (Strings.isNullOrEmpty(buildTargetAbi)) {
            return jsonGenerator.getNativeBuildConfigurationsJsons();
        } else {
            // Android Studio has requested a particular ABI to build or the user has specified
            // one from the command-line like with: -Pandroid.injected.build.abi=x86
            //
            // In this case, the requested ABI overrides and abiFilters in the variantConfig.
            // So this can build ABIs that aren't specified in any variant.
            //
            // It is possible for multiple ABIs to be passed through buildTargetAbi. In this
            // case, take the first. It is preferred.
            List<File> expectedJson =
                    ExternalNativeBuildTaskUtils.getOutputJsons(
                            jsonGenerator.getJsonFolder(),
                            Arrays.asList(buildTargetAbi.split(",")));
            // Remove JSONs that won't be created by the generator.
            expectedJson.retainAll(jsonGenerator.getNativeBuildConfigurationsJsons());
            // If no JSONs remain then issue a warning and proceed with no-op build.
            if (expectedJson.isEmpty()) {
                getBuilder()
                        .getIssueReporter()
                        .reportWarning(
                                EvalIssueReporter.Type.EXTERNAL_NATIVE_BUILD_CONFIGURATION,
                                String.format(
                                        "Targeted device ABI or comma-delimited ABIs [%s] is not"
                                                + " one of [%s]. Nothing to build.",
                                        buildTargetAbi,
                                        Joiner.on(", ")
                                                .join(
                                                        jsonGenerator
                                                                .getAbis()
                                                                .stream()
                                                                .map(Abi::getName)
                                                                .collect(Collectors.toList()))),
                                this.getName());
                return ImmutableList.of();
            } else {
                // Take the first JSON that matched the build configuration
                return Lists.newArrayList(expectedJson.iterator().next());
            }
        }
    }

    @NonNull
    private Map<Abi, File> getStlSharedObjectFiles() {
        return generator.get().getStlSharedObjectFiles();
    }

    @NonNull
    private GradleBuildVariant.Builder getStats() {
        return generator.get().stats;
    }

    public static class CreationAction extends VariantTaskCreationAction<ExternalNativeBuildTask> {
        @Nullable private final String buildTargetAbi;
        @NonNull private final Provider<ExternalNativeJsonGenerator> generator;
        @NonNull private final TaskProvider<? extends Task> generateTask;
        @NonNull private final AndroidBuilder androidBuilder;

        public CreationAction(
                @Nullable String buildTargetAbi,
                @NonNull Provider<ExternalNativeJsonGenerator> generator,
                @NonNull TaskProvider<? extends Task> generateTask,
                @NonNull VariantScope scope,
                @NonNull AndroidBuilder androidBuilder) {
            super(scope);
            this.buildTargetAbi = buildTargetAbi;
            this.generator = generator;
            this.generateTask = generateTask;
            this.androidBuilder = androidBuilder;
        }

        @NonNull
        @Override
        public String getName() {
            return getVariantScope().getTaskName("externalNativeBuild");
        }

        @NonNull
        @Override
        public Class<ExternalNativeBuildTask> getType() {
            return ExternalNativeBuildTask.class;
        }

        @Override
        public void handleProvider(
                @NonNull TaskProvider<? extends ExternalNativeBuildTask> taskProvider) {
            super.handleProvider(taskProvider);
            getVariantScope().getTaskContainer().getExternalNativeBuildTasks().add(taskProvider);
            getVariantScope().getTaskContainer().setExternalNativeBuildTask(taskProvider);
        }

        @Override
        public void configure(@NonNull ExternalNativeBuildTask task) {
            super.configure(task);

            VariantScope scope = getVariantScope();
            final BaseVariantData variantData = scope.getVariantData();
            task.nativeBuildOptions =
                    variantData.getVariantConfiguration().getExternalNativeBuildOptions();

            task.setAndroidBuilder(androidBuilder);
            task.dependsOn(
                    generateTask, scope.getArtifactFileCollection(RUNTIME_CLASSPATH, ALL, JNI));

            task.generator = generator;
            task.buildTargetAbi = buildTargetAbi;
        }
    }
}
