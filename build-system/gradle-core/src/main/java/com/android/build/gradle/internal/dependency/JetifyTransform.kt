/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.build.gradle.internal.dependency

import com.android.build.gradle.options.BooleanOption
import com.android.builder.model.Version
import com.android.tools.build.jetifier.core.config.ConfigParser
import com.android.tools.build.jetifier.processor.FileMapping
import com.android.tools.build.jetifier.processor.Processor
import com.google.common.base.Preconditions
import com.google.common.base.Verify
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencySubstitution
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.transform.ArtifactTransform
import java.io.File
import javax.inject.Inject

/**
 * [ArtifactTransform] to convert a third-party library that uses old support libraries into an
 * equivalent library that uses new support libraries.
 */
class JetifyTransform @Inject constructor() : ArtifactTransform() {

    companion object {

        @JvmStatic
        val jetifyProcessor: Processor by lazy {
            Processor.createProcessor(ConfigParser.loadDefaultConfig()!!)
        }

        /**
         * Replaces old support libraries with new ones.
         */
        @JvmStatic
        fun replaceOldSupportLibraries(project: Project) {
            project.configurations.all { config ->
                // Only consider resolvable configurations
                if (config.isCanBeResolved) {
                    config.resolutionStrategy.dependencySubstitution.all { it ->
                        JetifyTransform.maybeSubstituteDependency(it)
                    }
                }
            }
        }

        /**
         * Replaces the given dependency with the new support library if the given dependency is an
         * old support library.
         */
        private fun maybeSubstituteDependency(dependencySubstitution: DependencySubstitution) {
            // Only consider Gradle module dependencies (in the form of group:module:version)
            if (dependencySubstitution.requested !is ModuleComponentSelector) {
                return
            }

            val requestedDependency = dependencySubstitution.requested as ModuleComponentSelector
            val newSupportLibrary = getNewSupportLibrary(requestedDependency)

            // If the returned value is not null, it means that the dependency is an old support
            // library and should be replaced
            val targetDependency = newSupportLibrary ?: requestedDependency.displayName

            val effectiveTargetDependency = getEffectiveTargetDependency(targetDependency)
            if (effectiveTargetDependency != requestedDependency.displayName) {
                dependencySubstitution
                    .useTarget(
                        effectiveTargetDependency,
                        BooleanOption.ENABLE_JETIFIER.name + " is enabled"
                    )
            }
        }

        /**
         * Returns the new support library that replaces the old one if the given dependency is an
         * old support library, otherwise returns null.
         */
        private fun getNewSupportLibrary(dependency: ModuleComponentSelector): String? {
            if (isOldSupportLibrary(dependency)) {
                val newSupportLibraries = jetifyProcessor.mapDependency(dependency.displayName)
                if (newSupportLibraries == null || newSupportLibraries.isEmpty()) {
                    throw IllegalStateException(
                        "Can't find substitution for ${dependency.displayName}"
                    )
                }
                if (newSupportLibraries.size > 1) {
                    throw IllegalStateException(
                        "Multiple substitutions exist for ${dependency.displayName}.\n"
                                + "They are $newSupportLibraries."
                    )
                }
                return newSupportLibraries.single()
            } else {
                return null
            }
        }

        private fun getEffectiveTargetDependency(targetDependency: String): String {
            val parts = targetDependency.split(':')
            val group = parts[0]
            val module = parts[1]
            val version = parts[2]

            // TODO (jetifier-core): Need to map databinding to Android Gradle plugin version. Right
            // now jetifier-core is mapping it to 1.0.0, which is incorrect.
            if (group == "androidx.databinding") {
                return "$group:$module:${Version.ANDROID_GRADLE_PLUGIN_VERSION}"
            }

            // TODO (AGP): The stable versions of Android X are not available yet, only preview
            // versions. Therefore, here we replace the dependencies with their preview versions.
            // Eventually, when the stable versions are all published, we should remove this method.
            if (group.startsWith("androidx")) {
                if (version == "1.0.0") {
                    return "$group:$module:1.0.0-alpha1"
                } else if (version == "2.0.0-SNAPSHOT") {
                    return "$group:$module:2.0.0-alpha1"
                }
            }
            return targetDependency
        }

        private fun isOldSupportLibrary(dependency: ModuleComponentSelector): Boolean {
            // TODO (jetifier-core): Need a method to tell whether the given dependency is an
            // old support library
            return dependency.group.startsWith("com.android.support")
                    || dependency.group.startsWith("android.arch")
                    || dependency.group == "com.android.databinding"
        }

        private fun isOldSupportLibrary(aarOrJarFile: File): Boolean {
            // TODO (jetifier-core): Need a method to tell whether the given aarOrJarFile is an
            // old support library
            return aarOrJarFile.absolutePath.matches(Regex(".*com.android.support.*"))
                    || aarOrJarFile.absolutePath.matches(Regex(".*android.arch.*"))
                    || aarOrJarFile.absolutePath.matches(Regex(".*com.android.databinding.*"))
        }

        private fun isNewSupportLibrary(aarOrJarFile: File): Boolean {
            // TODO (jetifier-core): Need a method to tell whether the given aarOrJarFile is a
            // new support library
            return aarOrJarFile.absolutePath.contains("androidx")
                    || aarOrJarFile.absolutePath.matches(Regex(".*com.google.android.material.*"))
        }
    }

    override fun transform(aarOrJarFile: File): List<File> {
        Preconditions.checkArgument(
            aarOrJarFile.name.toLowerCase().endsWith(".aar")
                    || aarOrJarFile.name.toLowerCase().endsWith(".jar")
        )

        /*
         * The aars or jars can be categorized into 3 types:
         *  - New support libraries
         *  - Old support libraries
         *  - Others
         * In the following, we handle these cases accordingly.
         */
        // Case 1: If this is a new support library, no need to transform it
        if (isNewSupportLibrary(aarOrJarFile)) {
            return listOf(aarOrJarFile)
        }

        // Case 2: If this is an old support library, there was probably some bug because it should
        // have been replaced with a new support library earlier via dependency substitution.
        if (isOldSupportLibrary(aarOrJarFile)) {
            throw IllegalStateException(
                "Dependency was not replaced with AndroidX: ${aarOrJarFile.absolutePath}")
        }

        // Case 3: For the remaining, let's jetify them.
        val outputFile = File(outputDirectory, "jetified-" + aarOrJarFile.name)
        val maybeTransformedFile: File
        try {
            maybeTransformedFile = jetifyProcessor.transform(
                setOf(FileMapping(aarOrJarFile, outputFile)), false
            )
                .single()
        } catch (exception: Exception) {
            throw RuntimeException(
                "Failed to transform '$aarOrJarFile' using Jetifier. To disable Jetifier,"
                        + " set ${BooleanOption.ENABLE_JETIFIER.propertyName}=false in your"
                        + " gradle.properties file.",
                exception
            )
        }

        // If the aar/jar was transformed, the returned file would be the output file. Otherwise, it
        // would be the original file.
        Preconditions.checkState(
            maybeTransformedFile == aarOrJarFile || maybeTransformedFile == outputFile)

        // If the file wasn't transformed, returning the original file here also tells Gradle that
        // the file wasn't transformed. In either case (whether the file was transformed or not), we
        // can just return to Gradle the file that was returned from Jetifier.
        Verify.verify(maybeTransformedFile.exists(), "$outputFile does not exist")
        return listOf(maybeTransformedFile)
    }
}

