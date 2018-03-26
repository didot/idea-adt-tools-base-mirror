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

package com.android.build.gradle.internal.scope;

import com.android.build.api.artifact.ArtifactType;

/**
 * a Type of output that serves as an anchor for multiple tasks.
 *
 * <p>This is used when a single task consumes outputs (of the same type) coming from different
 * tasks, especially if the number of tasks generating this is be dynamic (either because some
 * tasks are optional based on some parameters or if the API allows for user-added tasks
 * generating the same content.)
 *
 * <p>This allows the consuming task to simply consume a single file collection rather than have
 * to deal with all the different tasks generating the content.
 */
public enum AnchorOutputType implements ArtifactType {
    GENERATED_RES,
    GENERATED_SRC,
    // anchor for a collection grouping all the generated bytecode
    ALL_CLASSES,
}
