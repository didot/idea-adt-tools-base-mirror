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

package com.android.build.gradle.integration.component

import com.android.build.gradle.integration.common.fixture.GradleTestProject
import com.android.build.gradle.integration.common.fixture.app.EmptyAndroidTestApp
import com.android.builder.model.NativeAndroidProject
import com.android.builder.model.NativeArtifact
import com.android.builder.model.NativeFolder
import com.android.builder.model.NativeSettings
import com.android.utils.FileUtils
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Sets
import groovy.transform.CompileStatic
import org.junit.Before
import org.junit.Rule
import org.junit.Test

import static com.android.build.gradle.integration.common.truth.TruthHelper.assertThat

/**
 * Test NativeAndroidProject model generated by StandaloneNdkPlugin.
 */
@CompileStatic
class StandaloneNdkModelTest {

    @Rule
    public GradleTestProject project = GradleTestProject.builder()
            .fromTestApp(new EmptyAndroidTestApp())
            .forExperimentalPlugin(true)
            .create()

    @Before
    public void setUp() {
        FileUtils.createFile(project.file("src/main/jni/empty.c"), "")
    }

    private final Set<String> ABIS = ImmutableSet.of(
            "armeabi",
            "armeabi-v7a",
            "arm64-v8a",
            "x86",
            "x86_64",
            "mips",
            "mips64")

    @Test
    public void "check simple model"() {
        project.buildFile << """
apply plugin: "com.android.model.native"

model {
    android {
        compileSdkVersion = $GradleTestProject.DEFAULT_COMPILE_SDK_VERSION
    }
    android.ndk {
        moduleName = "hello-jni"
    }
}
"""
        NativeAndroidProject model =
                project.executeAndReturnModel(NativeAndroidProject.class, "clean", "assemble")
        assertThat(model.buildFiles).isEmpty()
        checkModel(model, ImmutableSet.of("debug", "release"), ImmutableSet.of());
    }

    @Test
    public void "check model with variants"() {
        project.buildFile << """
apply plugin: "com.android.model.native"

model {
    android {
        compileSdkVersion = $GradleTestProject.DEFAULT_COMPILE_SDK_VERSION
    }
    android.ndk {
        moduleName = "hello-jni"
    }
    android.buildTypes {
        debug {
            ndk.with {
                CFlags.add("-DDEBUG_C")
                cppFlags.add("-DDEBUG_CPP")
            }
        }
        release {
            ndk.with {
                CFlags.add("-DRELEASE_C")
                cppFlags.add("-DRELEASE_CPP")
            }
        }
        create("optimized") {
            ndk.with {
                CFlags.add("-DOPTIMIZED_C")
                cppFlags.add("-DOPTIMIZED_CPP")
            }
        }
    }
    android.productFlavors {
        create("free") {
            ndk.with {
                CFlags.add("-DFREE_C")
                cppFlags.add("-DFREE_CPP")
            }
        }
        create("premium") {
            ndk.with {
                CFlags.add("-DPREMIUM_C")
                cppFlags.add("-DPREMIUM_CPP")
            }
        }
    }
}
"""
        NativeAndroidProject model =
                project.executeAndReturnModel(NativeAndroidProject.class, "clean", "assemble")
        assertThat(model.buildFiles).isEmpty()
        checkModel(
                model,
                ImmutableSet.of("debug", "release", "optimized"),
                ImmutableSet.of("free", "premium"));

        // Check all setting contains the dimension specific flags.
        List<String> allDimensions = [ "debug", "release", "optimized", "free", "premium" ];
        for (NativeArtifact artifact : model.getArtifacts()) {
            for (NativeFolder folder : artifact.sourceFolders) {
                String cSettingName = folder.perLanguageSettings.get("c")
                NativeSettings cSettings = model.getSettings().find { it.name == cSettingName }
                assertThat(cSettings).isNotNull()

                String cppSettingName = folder.perLanguageSettings.get("c++")
                NativeSettings cppSettings = model.getSettings().find { it.name == cppSettingName }
                assertThat(cppSettings).isNotNull()

                for (String dimension : allDimensions) {
                    String expectedCFlag = "-D" + dimension.toUpperCase() + "_C"
                    String expectedCppFlag = "-D" + dimension.toUpperCase() + "_CPP"

                    if (artifact.name.toLowerCase().contains(dimension)) {
                        assertThat(cSettings.compilerFlags).contains(expectedCFlag)
                        assertThat(cppSettings.compilerFlags).contains(expectedCppFlag)
                    } else {
                        assertThat(cSettings.compilerFlags).doesNotContain(expectedCFlag)
                        assertThat(cppSettings.compilerFlags).doesNotContain(expectedCppFlag)
                    }
                }
            }
        }
    }

    private void checkModel(
            NativeAndroidProject model,
            Set<String> buildTypes,
            Set<String> productFlavors) {
        Collection<NativeArtifact> artifacts = model.getArtifacts()
        productFlavors = productFlavors.isEmpty() ? ImmutableSet.of("") : productFlavors
        for(List<String> combo : Sets.cartesianProduct(buildTypes, productFlavors, ABIS)) {
            String buildType = combo.get(0)
            String flavor = combo.get(1)
            String abi = combo.get(2);
            String variant = flavor.isEmpty() ? buildType : flavor + buildType.capitalize()
            String name = variant + '-' + abi;

            assertThat(artifacts.collect { it.getName() }).contains(name)
            NativeArtifact artifact = null;
            for (NativeArtifact a : artifacts) {
                if (a.getName().equals(name)) {
                    artifact = a;
                    break;
                }
            }

            assertThat(artifact.outputFile).hasName("libhello-jni.so")
            assertThat(artifact.sourceFiles).isEmpty()
            assertThat(artifact.toolChain).endsWith(abi)

            List<File> expectedSrc =
                    ["main", buildType, flavor, variant]
                            .findAll { !it.isEmpty() }
                            .unique()
                            .collect { project.file("src/" + it + "/jni") }

            assertThat(artifact.sourceFolders.collect { it.getFolderPath() }).containsAllIn(expectedSrc)
        }
    }
}
