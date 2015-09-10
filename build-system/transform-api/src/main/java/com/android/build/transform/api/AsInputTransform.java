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

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * A Transform that writes the input streams in matching output streams.
 */
public interface AsInputTransform extends Transform {

    /**
     * Perform the Transform.
     *
     * <p/>
     * The inputs/outputs are provided as a Map with the keys being the {@link TransformInput}
     * which are associated with a corresponding {@link TransformOutput}.
     *
     * <p/>
     * The Transform can require a non-incremental changes, either because {@link #isIncremental()}
     * returns false, or because there is a change in secondary files
     * (as returned by {@link #getSecondaryFileInputs()}), or a change to non input file parameters
     * (as returned by {@link #getParameterInputs()}), or an output was clobbered by something.
     *
     * <p/>
     * If this happens then <var>isIncremental</var> will be false, and
     * {@link TransformInput#getChangedFiles()} will return an empty map. In that case, the
     * transform should look directly at {@link TransformInput#getFiles()} to find the files. This
     * is different from Gradle's behavior where clean builds receive all files in the changed file
     * list.
     *
     * @param inputs the inputs/outputs of the transform
     * @param referencedInputs the referenced-only inputs
     * @param isIncremental whether the transform is incremental.
     * @throws IOException if an IO error occurs
     * @throws InterruptedException
     * @throws TransformException Generic exception encapsulating the cause.
     */
    void transform(
            @NonNull Map<TransformInput, TransformOutput> inputs,
            @NonNull Collection<TransformInput> referencedInputs,
            boolean isIncremental) throws IOException, TransformException, InterruptedException;
}
