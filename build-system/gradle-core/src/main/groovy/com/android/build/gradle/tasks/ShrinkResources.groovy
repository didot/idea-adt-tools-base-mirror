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

package com.android.build.gradle.tasks
import com.android.annotations.NonNull
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.build.gradle.internal.pipeline.TransformStream
import com.android.build.gradle.internal.scope.ConventionMappingHelper
import com.android.build.gradle.internal.scope.TaskConfigAction
import com.android.build.gradle.internal.scope.VariantOutputScope
import com.android.build.gradle.internal.scope.VariantScope
import com.android.build.gradle.internal.tasks.BaseTask
import com.android.build.gradle.internal.variant.BaseVariantOutputData
import com.android.build.transform.api.ScopedContent
import com.android.builder.core.AaptPackageProcessBuilder
import com.android.ide.common.process.LoggedProcessOutputHandler
import com.google.common.collect.ImmutableList
import com.google.common.collect.Iterables
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.ParallelizableTask
import org.gradle.api.tasks.TaskAction

import java.util.concurrent.Callable
/**
 * Task which strips out unused resources
 * <p>
 * The process works as follows:
 * <ul>
 * <li> Collect R id <b>values</b> from the final merged R class, which incorporates
 *      the final id's of all the libraries (if ProGuard hasn't inlined these,
 *      we don't need to do this; we can look for actual R.id's instead!)
 * <li> Collect <b>used<b> R values from all the .class files, and R.x.y references too!
 * <li> Compute the set of remaining/used id’s
 * <li> Add in any found in the manifest
 * <li> Look through all resources and produce a graph of reachable resources
 * <li> Compute unused resources by visiting all resources and ignoring those that
 *      were reachable
 * <li> In addition, if we find a call to Resources#getIdentifier(), we collect all
 *      strings in the class files, and also mark as used any resources that match
 *      potential string lookups
 * </ul>
 */
@ParallelizableTask
public class ShrinkResources extends BaseTask {
    /**
     * Associated variant data that the strip task will be run against. Used to locate
     * not only locations the task needs (e.g. for resources and generated R classes)
     * but also to obtain the resource merging task, since we will run it a second time
     * here to generate a new .ap_ file with fewer resources
     */
    public BaseVariantOutputData variantOutputData

    protected File minifiedOutFolder

    @InputDirectory
    File getInputFolder() {
        return minifiedOutFolder;
    }

    @InputFile
    File getManifestInput() {
        return variantOutputData.manifestProcessorTask.manifestOutputFile
    }

    @InputDirectory
    File getSourceDir() {
        return variantOutputData.variantData.generateRClassTask.sourceOutputDir
    }

    @InputDirectory
    File getResourceDir() {
        return variantOutputData.variantData.getScope().getFinalResourcesDir()
    }

    @InputFile
    File uncompressedResources

    @OutputFile
    File compressedResources

    /** Whether we've already warned about how to turn off shrinking. Used to avoid
     * repeating the same multi-line message for every repeated abi split. */
    private static ourWarned;

    @SuppressWarnings("GroovyUnusedDeclaration")
    @TaskAction
    void shrink() {
        def variantData = variantOutputData.variantData
        try {
            def processResourcesTask = variantData.generateRClassTask
            File sourceDir = processResourcesTask.sourceOutputDir
            File resourceDir = variantData.getScope().getFinalResourcesDir()
            File mergedManifest = variantOutputData.manifestProcessorTask.manifestOutputFile

            // Analyze resources and usages and strip out unused
            def analyzer = new ResourceUsageAnalyzer(
                    sourceDir,
                    minifiedOutFolder,
                    mergedManifest,
                    variantData.getMappingFile(),
                    resourceDir)
            analyzer.verbose = project.logger.isEnabled(LogLevel.INFO)
            analyzer.debug = project.logger.isEnabled(LogLevel.DEBUG)
            analyzer.analyze();

            //noinspection GroovyConstantIfStatement
            if (ResourceUsageAnalyzer.TWO_PASS_AAPT) {
                // This is currently not working; we need support from aapt to be able
                // to assign a stable set of resources that it should use.
                def destination = new File(resourceDir.parentFile, resourceDir.name + "-stripped")
                analyzer.removeUnused(destination)

                File sourceOutputs = processResourcesTask.getSourceOutputDir();
                sourceOutputs = new File(sourceOutputs.getParentFile(),
                        sourceOutputs.getName() + "-stripped")
                sourceOutputs.mkdirs()

                // We don't need to emit R files again, but we can do this here such that
                // we can *verify* that the R classes generated in the second aapt pass
                // matches those we saw the first time around.
                //String sourceOutputPath = sourceOutputs?.getAbsolutePath();
                String sourceOutputPath = null

                // Repackage the resources:
                AaptPackageProcessBuilder aaptPackageCommandBuilder =
                        new AaptPackageProcessBuilder(processResourcesTask.getManifestFile(),
                                processResourcesTask.getAaptOptions())
                                .setAssetsFolder(processResourcesTask.getAssetsDir())
                                .setResFolder(destination)
                                .setLibraries(processResourcesTask.getLibraries())
                                .setPackageForR(processResourcesTask.getPackageForR())
                                .setSourceOutputDir(sourceOutputPath)
                                .setResPackageOutput(getCompressedResources().absolutePath)
                                .setType(processResourcesTask.getType())
                                .setDebuggable(processResourcesTask.getDebuggable())
                                .setResourceConfigs(processResourcesTask.getResourceConfigs())
                                .setSplits(processResourcesTask.getSplits())

                getBuilder().processResources(
                        aaptPackageCommandBuilder,
                        processResourcesTask.getEnforceUniquePackageName(),
                        new LoggedProcessOutputHandler(getBuilder().getLogger())
                )
            } else {
                // Just rewrite the .ap_ file to strip out the res/ files for unused resources
                analyzer.rewriteResourceZip(getUncompressedResources(), getCompressedResources())
            }

            // Dump some stats
            int unused = analyzer.getUnusedResourceCount()
            if (unused > 0) {
                StringBuilder sb = new StringBuilder(200);
                sb.append("Removed unused resources")

                // This is a bit misleading until we can strip out all resource types:
                //int total = analyzer.getTotalResourceCount()
                //sb.append("(" + unused + "/" + total + ")")

                int before = getUncompressedResources().length()
                int after = getCompressedResources().length()
                int percent = (before - after) * 100 / before
                sb.append(": Binary resource data reduced from ").
                        append(toKbString(before)).
                        append("KB to ").
                        append(toKbString(after)).
                        append("KB: Removed " + percent + "%");
                if (!ourWarned) {
                    ourWarned = true;
                    sb.append(
                        "\nNote: If necessary, you can disable resource shrinking by adding\n" +
                        "android {\n" +
                        "    buildTypes {\n" +
                        "        " + variantData.variantConfiguration.buildType.name + " {\n" +
                        "            shrinkResources false\n" +
                        "        }\n" +
                        "    }\n" +
                        "}")
                }

                println sb.toString();
            }

        } catch (Exception e) {
            println 'Failed to shrink resources: ' + e.toString() + '; ignoring'
            logger.quiet("Failed to shrink resources: ignoring", e)
        }
    }

    private static String toKbString(long size) {
        return Integer.toString((int)size/1024);
    }

    public static class ConfigAction implements TaskConfigAction<ShrinkResources> {

        private VariantOutputScope scope;

        public ConfigAction(VariantOutputScope scope) {
            this.scope = scope;
        }

        @Override
        String getName() {
            return scope.getTaskName("shrink", "Resources");
        }

        @Override
        Class<ShrinkResources> getType() {
            return ShrinkResources.class
        }

        @Override
        void execute(ShrinkResources task) {
            VariantScope variantScope = scope.getVariantScope()
            task.setAndroidBuilder(scope.getGlobalScope().getAndroidBuilder());

            task.setVariantName(variantScope.getVariantConfiguration().getFullName());
            task.variantOutputData = scope.variantOutputData;

            ImmutableList<TransformStream> streams = variantScope.getTransformManager().getStreams(
                    new TransformManager.StreamFilter() {
                            @Override
                            boolean accept(@NonNull Set<ScopedContent.ContentType> types,
                                    @NonNull Set<ScopedContent.Scope> scopes) {
                                return types.contains(ScopedContent.ContentType.CLASSES) &&
                                        !scopes.contains(ScopedContent.Scope.PROVIDED_ONLY) &&
                                        !scopes.contains(ScopedContent.Scope.TESTED_CODE)
                            }
                        })
            // there should be only one stream if we're running minification
            TransformStream stream = Iterables.getOnlyElement(streams);
            // there should be a single folder too since streams downstream from the original
            // ones are single folders.
            task.minifiedOutFolder = Iterables.getOnlyElement(stream.getFiles().get())

            final String outputBaseName = scope.variantOutputData.getBaseName();
            task.setCompressedResources(scope.getCompressedResourceFile());

            ConventionMappingHelper.map(task, "uncompressedResources", new Callable<File>() {
                @Override
                public File call() {
                    return scope.variantOutputData.processResourcesTask.getPackageOutputFile();
                }
            });

        }
    }
}
