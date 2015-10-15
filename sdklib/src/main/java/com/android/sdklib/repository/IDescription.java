/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.sdklib.repository;

/**
 * Interface for elements that can provide a description of themselves.
 */
public interface IDescription {

    /**
     * Returns a description of the given element. Cannot be null.
     * <p/>
     * A description is a multi-line of text, typically much more
     * elaborate than what {@link Object#toString()} would provide.
     */
    String getShortDescription();

    /**
     * Returns a description of the given element. Cannot be null.
     * <p/>
     * A description is a multi-line of text, typically much more
     * elaborate than what {@link Object#toString()} would provide.
     */
    String getLongDescription();

}
