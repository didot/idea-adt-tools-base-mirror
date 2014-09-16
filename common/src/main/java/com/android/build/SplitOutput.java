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

package com.android.build;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;

import java.io.File;

/**
 * An output with an associated set of filters.
 */
public interface SplitOutput {

    /**
     * Returns the output file for this artifact's output.
     * Depending on whether the project is an app or a library project, this could be an apk or
     * an aar file.
     *
     * For test artifact for a library project, this would also be an apk.
     *
     * @return the output file.
     */
    @NonNull
    File getOutputFile();

    /**
     * The density filter if applicable.
     * @return the density filter or null if not applicable.
     */
    @Nullable
    String getDensityFilter();

    /**
     * The ABI filter if applicable.
     * @return the ABI filter or null if not applicable.
     */
    @Nullable
    String getAbiFilter();

    /**
     * The output versionCode.
     *
     * In case of multi-apk, the version code of each apk is different.
     *
     * @return the versionCode
     */
    int getVersionCode();
}
