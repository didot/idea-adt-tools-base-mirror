/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.ddmlib.logcat;

import com.android.annotations.NonNull;
import com.android.ddmlib.Log.LogLevel;

/**
 * Model a single log message output from {@code logcat -v long}.
 *
 * Every message is furthermore associated with a {@link LogCatHeader} which contains additionally
 * meta information about the message.
 */
public final class LogCatMessage {

    @NonNull
    private final LogCatHeader mHeader;

    @NonNull
    private final String mMessage;

    /**
     * @deprecated Create a {@link LogCatHeader} separately and call {@link
     * #LogCatMessage(LogCatHeader, String)} instead. This approach shares the same header data
     * across multiple messages.
     */
    public LogCatMessage(@NonNull LogLevel logLevel, @NonNull String pid, @NonNull String tid,
            @NonNull String appName, @NonNull String tag,
            @NonNull String time, @NonNull String msg) {
        this(new LogCatHeader(logLevel, pid, tid, appName, tag, time), msg);
    }

    /**
     * Construct an immutable log message object.
     */
    public LogCatMessage(@NonNull LogCatHeader header, @NonNull String msg) {
        mHeader = header;
        mMessage = msg;
    }

    @NonNull
    public LogCatHeader getHeader() {
        return mHeader;
    }

    @NonNull
    public String getMessage() {
        return mMessage;
    }

    @NonNull
    public LogLevel getLogLevel() {
        return mHeader.getLogLevel();
    }

    @NonNull
    public String getPid() {
        return mHeader.getPid();
    }

    @NonNull
    public String getTid() {
        return mHeader.getTid();
    }

    @NonNull
    public String getAppName() {
        return mHeader.getAppName();
    }

    @NonNull
    public String getTag() {
        return mHeader.getTag();
    }

    @NonNull
    public String getTime() {
        return mHeader.getTime();
    }

    @Override
    public String toString() {
        return mHeader.toString() + ": " + mMessage;
    }
}
