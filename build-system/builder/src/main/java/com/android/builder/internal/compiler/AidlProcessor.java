/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.builder.internal.compiler;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.compiling.DependencyFileProcessor;
import com.android.builder.internal.incremental.DependencyData;
import com.android.ide.common.internal.CommandLineRunner;
import com.android.ide.common.internal.LoggedErrorException;
import com.android.sdklib.io.FileOp;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A Source File processor for AIDL files. This compiles each aidl file found by the SourceSearcher.
 */
public class AidlProcessor implements SourceSearcher.SourceFileProcessor {

    @NonNull
    private final String mAidlExecutable;
    @NonNull
    private final String mFrameworkLocation;
    @NonNull
    private final List<File> mImportFolders;
    @NonNull
    private final File mSourceOutputDir;
    @Nullable
    private final File mParcelableOutputDir;
    @NonNull
    private final DependencyFileProcessor mDependencyFileProcessor;
    @NonNull
    private final CommandLineRunner mRunner;

    public AidlProcessor(@NonNull String aidlExecutable,
                         @NonNull String frameworkLocation,
                         @NonNull List<File> importFolders,
                         @NonNull File sourceOutputDir,
                         @Nullable File parcelableOutputDir,
                         @NonNull DependencyFileProcessor dependencyFileProcessor,
                         @NonNull CommandLineRunner runner) {
        mAidlExecutable = aidlExecutable;
        mFrameworkLocation = frameworkLocation;
        mImportFolders = importFolders;
        mSourceOutputDir = sourceOutputDir;
        mParcelableOutputDir = parcelableOutputDir;
        mDependencyFileProcessor = dependencyFileProcessor;
        mRunner = runner;
    }

    @Override
    public void processFile(@NonNull File sourceFolder, @NonNull File sourceFile)
            throws IOException, InterruptedException, LoggedErrorException {
        ArrayList<String> command = Lists.newArrayList();

        command.add(mAidlExecutable);

        command.add("-p" + mFrameworkLocation);
        command.add("-o" + mSourceOutputDir.getAbsolutePath());

        // add all the library aidl folders to access parcelables that are in libraries
        for (File f : mImportFolders) {
            command.add("-I" + f.getAbsolutePath());
        }

        // create a temp file for the dependency
        File depFile = File.createTempFile("aidl", ".d");
        command.add("-d" + depFile.getAbsolutePath());

        command.add(sourceFile.getAbsolutePath());

        mRunner.runCmdLine(command, null);

        // send the dependency file to the processor.
        DependencyData data = mDependencyFileProcessor.processFile(depFile);

        if (mParcelableOutputDir != null && data != null && data.getOutputFiles().isEmpty()) {
            // looks like a parcelable. Store it in the 2ndary output of the DependencyData object.

            String relative = FileOp.makeRelative(sourceFolder, sourceFile);

            File destFile = new File(mParcelableOutputDir, relative);
            destFile.getParentFile().mkdirs();
            Files.copy(sourceFile, destFile);
            data.addSecondaryOutputFile(destFile.getPath());
        }

        depFile.delete();
    }
}
