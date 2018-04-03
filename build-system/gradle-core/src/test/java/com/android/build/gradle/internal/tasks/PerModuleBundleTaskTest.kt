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

package com.android.build.gradle.internal.tasks

import com.android.build.api.artifact.BuildableArtifact
import com.android.build.gradle.internal.core.GradleVariantConfiguration
import com.android.build.gradle.internal.pipeline.StreamFilter
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.build.gradle.internal.publishing.AndroidArtifacts
import com.android.build.gradle.internal.scope.BuildArtifactsHolder
import com.android.build.gradle.internal.scope.InternalArtifactType
import com.android.build.gradle.internal.scope.VariantScope
import com.android.builder.core.VariantTypeImpl
import com.android.testutils.truth.ZipFileSubject
import com.android.utils.FileUtils
import com.google.common.truth.Truth.assertThat
import org.gradle.api.file.FileCollection
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.io.File

class PerModuleBundleTaskTest {

    @Mock private lateinit var assetsFiles: BuildableArtifact
    @Mock private lateinit var resFiles: BuildableArtifact
    @Mock private lateinit var dexFiles: FileCollection
    @Mock private lateinit var javaResFiles: FileCollection
    @Mock private lateinit var nativeLibsFiles: FileCollection
    @Mock private lateinit var variantScope: VariantScope
    @Mock private lateinit var artifacts: BuildArtifactsHolder
    @Mock private lateinit var featureSetMetadata: FileCollection
    @Mock private lateinit var transformManager: TransformManager
    @Mock private lateinit var variantConfiguration: GradleVariantConfiguration

    @get:Rule
    val testFolder = TemporaryFolder()

    lateinit var task: PerModuleBundleTask

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        Mockito.`when`(variantScope.fullVariantName).thenReturn("variant")
        Mockito.`when`(variantScope.artifacts).thenReturn(artifacts)
        Mockito.`when`(variantScope.type).thenReturn(VariantTypeImpl.FEATURE)

        Mockito.`when`(variantScope.variantConfiguration).thenReturn(variantConfiguration)
        Mockito.`when`(variantConfiguration.supportedAbis).thenReturn(setOf())

        Mockito.`when`(artifacts.getFinalArtifactFiles(InternalArtifactType.MERGED_ASSETS))
            .thenReturn(assetsFiles)
        Mockito.`when`(assetsFiles.iterator()).thenReturn(
            listOf(testFolder.newFolder("assets")).iterator())
        Mockito.`when`(artifacts.getFinalArtifactFiles(InternalArtifactType.LINKED_RES_FOR_BUNDLE))
            .thenReturn(resFiles)
        Mockito.`when`(resFiles.iterator()).thenReturn(
            listOf(testFolder.newFile("res")).iterator())

        Mockito.`when`(variantScope.transformManager).thenReturn(transformManager)
        Mockito.`when`(transformManager.getPipelineOutputAsFileCollection(StreamFilter.DEX))
            .thenReturn(dexFiles)
        Mockito.`when`(transformManager.getPipelineOutputAsFileCollection(StreamFilter.RESOURCES))
            .thenReturn(javaResFiles)
        Mockito.`when`(transformManager.getPipelineOutputAsFileCollection(StreamFilter.NATIVE_LIBS))
            .thenReturn(nativeLibsFiles)

        Mockito.`when`(variantScope.getArtifactFileCollection(
            AndroidArtifacts.ConsumedConfigType.COMPILE_CLASSPATH,
            AndroidArtifacts.ArtifactScope.MODULE,
            AndroidArtifacts.ArtifactType.FEATURE_SET_METADATA))
            .thenReturn(featureSetMetadata)


        val testFiles = testFolder.newFolder("test_files")
        val featureMetadata = File(testFiles, "feature-metadata.json")
        FileUtils.writeToFile(
            featureMetadata,
            "[{\"modulePath\":\":feature2\",\"featureName\":\"feature2\",\"resOffset\":129}," +
                    "{\"modulePath\":\":feature1\",\"featureName\":\"feature1\",\"resOffset\":128}]")

        val project = ProjectBuilder.builder().withProjectDir(testFolder.newFolder()).build()
        task = project.tasks.create("test", PerModuleBundleTask::class.java)

        Mockito.`when`(featureSetMetadata.singleFile).thenReturn(featureMetadata)
        Mockito.`when`(artifacts.appendArtifact(InternalArtifactType.MODULE_BUNDLE, task))
            .thenReturn(testFolder.newFolder("out"))


        val configAction = PerModuleBundleTask.ConfigAction(variantScope)
        configAction.execute(task)
    }

    @Test
    fun testSingleDexFiles() {
        val dexFolder = testFolder.newFolder("dex_files")
        Mockito.`when`(dexFiles.files).thenReturn(
            setOf(createDex(dexFolder, "classes.dex")))
        task.zip()
        verifyOutputZip(task.outputDir.listFiles().single(), 1)
    }

    @Test
    fun testNoDuplicateDexFiles() {
        val dexFolder = testFolder.newFolder("dex_files")
        Mockito.`when`(dexFiles.files).thenReturn(
            setOf(
                createDex(dexFolder, "classes.dex"),
                createDex(dexFolder, "classes1.dex"),
                createDex(dexFolder, "classes2.dex")))
        task.zip()
        verifyOutputZip(task.outputDir.listFiles().single(), 3)
    }

    @Test
    fun testDuplicateDexFiles() {
        val dexFolder0 = testFolder.newFolder("0")
        val dexFolder1 = testFolder.newFolder("1")
        Mockito.`when`(dexFiles.files).thenReturn(
            setOf(
                createDex(dexFolder0, "classes.dex"),
                createDex(dexFolder0, "classes1.dex"),
                createDex(dexFolder0, "classes2.dex"),
                createDex(dexFolder1, "classes0.dex"),
                createDex(dexFolder1, "classes1.dex")))
        task.zip()

        // verify naming and shuffling of names.
        verifyOutputZip(task.outputDir.listFiles().single(), 5)

    }

    private fun verifyOutputZip(zipFile: File, expectedNumberOfDexFiles: Int) {
        assertThat(expectedNumberOfDexFiles).isGreaterThan(0)
        assertThat(zipFile.exists())
        val thatZip = ZipFileSubject.assertThatZip(zipFile)
        thatZip.contains("dex/classes.dex")
        for (index in 1..(expectedNumberOfDexFiles-1)) {
            thatZip.contains("dex/classes$index.dex")
        }
        thatZip.doesNotContain("dex/classes$expectedNumberOfDexFiles.dex")
    }

    private fun createDex(folder: File, id: String): File {
        val dexFile = File(folder, id)
        FileUtils.createFile(dexFile, "Dex $id")
        return dexFile
    }
}