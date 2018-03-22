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
import com.android.build.gradle.internal.api.artifact.singleFile
import com.android.build.gradle.internal.scope.InternalArtifactType
import com.android.build.gradle.internal.scope.TaskConfigAction
import com.android.build.gradle.internal.scope.VariantScope
import com.android.build.gradle.options.StringOption
import com.android.build.gradle.tasks.WorkerExecutorAdapter
import com.android.bundle.Devices.DeviceSpec
import com.android.tools.build.bundletool.commands.SelectApksCommand
import com.android.utils.FileUtils
import com.google.protobuf.util.JsonFormat
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.io.Serializable
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Inject

/**
 * Task that extract APKs from the apk zip (created with [BundleToApkTask] into a folder. a Device
 * info file indicate which APKs to extract. Only APKs for that particular device are extracted.
 */
open class SelectApksTask @Inject constructor(private val workerExecutor: WorkerExecutor) : AndroidVariantTask() {

    companion object {
        fun getTaskName(scope: VariantScope) = scope.getTaskName("selectApksFor")
    }

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    lateinit var apkFolders: BuildableArtifact
        private set

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    var deviceConfig: File? = null
        private set

    @get:OutputDirectory
    lateinit var outputDir: File
        private set

    @TaskAction
    fun generateApk() {
        val adapter = WorkerExecutorAdapter<Params>(workerExecutor, BundleToolRunnable::class.java)

        adapter.submit(
            Params(
                apkFolders.singleFile(),
                deviceConfig ?: throw RuntimeException("Calling ApkSelect with no device config"),
                outputDir
            )
        )

        adapter.taskActionDone()
    }

    private data class Params(
        val apkFolder: File,
        val deviceConfig: File,
        val outputDir: File
    ) : Serializable

    private class BundleToolRunnable @Inject constructor(private val params: Params): Runnable {
        override fun run() {
            FileUtils.cleanOutputDir(params.outputDir)

            val path: Path = Files.createTempFile(null, "spec.proto")

            val builder: DeviceSpec.Builder = DeviceSpec.newBuilder()

            Files.newBufferedReader(params.deviceConfig.toPath(), Charsets.UTF_8).use {
                JsonFormat.parser().merge(it, builder)
            }

            val command = SelectApksCommand
                .builder()
                .setApksArchivePath(File(params.apkFolder, "bundle.apks").toPath())
                .setDeviceSpec(builder.build())
                .setOutputDirectory(params.outputDir.toPath())

            command.build().execute()
        }
    }

    class ConfigAction(private val scope: VariantScope) : TaskConfigAction<SelectApksTask> {

        override fun getName() = getTaskName(scope)
        override fun getType() = SelectApksTask::class.java

        override fun execute(task: SelectApksTask) {
            task.variantName = scope.fullVariantName
            task.outputDir = scope.artifacts.appendArtifact(InternalArtifactType.SELECTED_APKS, task)
            task.apkFolders = scope.artifacts.getFinalArtifactFiles(InternalArtifactType.APKS_FROM_BUNDLE)

            val devicePath = scope.globalScope.projectOptions.get(StringOption.IDE_APK_SELECT_CONFIG)
            if (devicePath != null) {
                task.deviceConfig = File(devicePath)
            }
        }
    }
}
