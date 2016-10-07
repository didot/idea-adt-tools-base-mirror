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

package com.android.build.gradle.external.gson;

import com.android.annotations.Nullable;

import java.io.File;
import java.util.Collection;

/**
 * Value type to be used with Gson.
 */
public class NativeLibraryValue {
    @Nullable
    public String buildCommand;
    @Nullable
    public String buildType;
    @Nullable
    public String toolchain;
    @Nullable
    public String groupName;
    @Nullable
    public String abi;
    @Nullable
    public String artifactName;
    @Nullable
    public Collection<NativeSourceFolderValue> folders;
    @Nullable
    public Collection<NativeSourceFileValue> files;
    @Nullable
    public Collection<File> exportedHeaders;
    @Nullable
    public File output;
}
