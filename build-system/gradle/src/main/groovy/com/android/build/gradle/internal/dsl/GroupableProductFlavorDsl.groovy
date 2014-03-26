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

package com.android.build.gradle.internal.dsl

import com.android.annotations.NonNull
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.internal.reflect.Instantiator

/**
 * A version of ProductFlavorDsl that can receive a group name
 */
public class GroupableProductFlavorDsl extends ProductFlavorDsl {
    private static final long serialVersionUID = 1L

    String flavorDimension

    public GroupableProductFlavorDsl(
            @NonNull String name,
            @NonNull FileResolver fileResolver,
            @NonNull Instantiator instantiator,
            @NonNull Logger logger) {
        super(name, fileResolver, instantiator, logger)
    }

    // ---------------
    // TEMP for compatibility
    // STOPSHIP Remove in 1.0

    public void flavorGroup(String value) {
        logger.log(LogLevel.WARN,
                "WARNING: flavorGroup has been renamed flavorDimension. It will be removed in 1.0")
        flavorDimension = value
    }

    public void setFlavorGroup(String value) {
        flavorGroup(value)
    }
}
