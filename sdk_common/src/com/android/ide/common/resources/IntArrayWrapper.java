/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.ide.common.resources;

import java.util.Arrays;


/**
 * Wrapper around a int[] to provide hashCode/equals support.
 */
public final class IntArrayWrapper {

    private int[] mData;

    public IntArrayWrapper(int[] data) {
        mData = data;
    }

    public void set(int[] data) {
        mData = data;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(mData);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass().equals(obj.getClass())) {
            return Arrays.equals(mData, ((IntArrayWrapper)obj).mData);
        }

        return super.equals(obj);
    }
}
