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

package com.android.sdklib.devices;

public enum Multitouch {
    NONE("none"),
    BASIC("basic"),
    DISTINCT("distinct"),
    JAZZ_HANDS("jazz-hands");

    private final String mValue;

    private Multitouch(String value){
        mValue = value;
    }

    public static Multitouch getEnum(String val){
        for (Multitouch m : values()) {
            if (m.mValue.equals(val)) {
                return m;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return mValue;
    }
}
