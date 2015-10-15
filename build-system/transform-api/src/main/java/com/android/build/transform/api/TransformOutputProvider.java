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

package com.android.build.transform.api;

import com.android.annotations.NonNull;
import com.google.common.annotations.Beta;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * The output of a transform.
 * <p/>
 * There is no direct access to a location to write. Instead, Transforms can ask to create
 * new content location for given scopes/content-types and a format.
 */
@Beta
public interface TransformOutputProvider {

    /**
     * Delete all content. This is useful when running in non-incremental mode
     * @throws IOException
     */
    void deleteAll() throws IOException;

    /**
     * Returns the location of content for a given set of Scopes, Content Types, and format.
     *
     * If the format is {@link Format#DIRECTORY} then the result is the file location of the
     * directory.
     * If the format is {@link Format#JAR} then the result is a file representing the jar to create.
     *
     * @param name a unique name for the content. For a given set of scopes/types/format it must
     *             be unique.
     * @param types the content types associated with this content.
     * @param scopes the scopes associated with this content.
     * @param format the format of the content.
     * @return the location of the content.
     */
    @NonNull
    File getContentLocation(@NonNull String name,
            @NonNull Set<QualifiedContent.ContentType> types,
            @NonNull Set<QualifiedContent.Scope> scopes,
            @NonNull Format format);
}
