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
package com.android.ide.common.gradle.model;

import com.android.annotations.NonNull;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ModelCache {
    @NonNull private final Map<Object, Object> myData = new HashMap<>();

    @SuppressWarnings("unchecked")
    @NonNull
    public <K, V> V computeIfAbsent(@NonNull K key, @NonNull Function<K, V> mappingFunction) {
        Object result = myData.computeIfAbsent(key, o -> mappingFunction.apply((K) o));
        return (V) result;
    }

    @NonNull
    Map<Object, Object> getData() {
        return myData;
    }
}
