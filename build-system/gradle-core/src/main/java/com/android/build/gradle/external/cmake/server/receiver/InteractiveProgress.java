/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.build.gradle.external.cmake.server.receiver;

/**
 * Progress information sent by Cmake server when it is busy processing a given request. More info:
 * https://cmake.org/cmake/help/v3.7/manual/cmake-server.7.html#type-progress
 */
public class InteractiveProgress {
    public final String type;
    public final String cookie;
    public final String inReplyTo;
    public final Integer progressCurrent;
    public final Integer progressMaximum;
    public final String progressMessage;
    public final Integer progressMinimum;

    private InteractiveProgress() {
        this.type = null;
        this.cookie = null;
        this.inReplyTo = null;
        this.progressCurrent = null;
        this.progressMaximum = null;
        this.progressMessage = null;
        this.progressMinimum = null;
    }
}
