/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.ide.common.rendering.api;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceType;
import java.util.ArrayList;
import java.util.List;

/** Represents an Android plurals resource. */
public class PluralsResourceValueImpl extends ResourceValueImpl implements PluralsResourceValue {
    private final List<String> mQuantities = new ArrayList<>();
    private final List<String> mValues = new ArrayList<>();

    public PluralsResourceValueImpl(
            @NonNull ResourceReference reference,
            @Nullable String value,
            @Nullable String libraryName) {
        super(reference, value, libraryName);
        assert reference.getResourceType() == ResourceType.PLURALS;
    }

    public PluralsResourceValueImpl(
            @NonNull ResourceNamespace namespace,
            @NonNull ResourceType type,
            @NonNull String name,
            @Nullable String value,
            @Nullable String libraryName) {
        super(namespace, type, name, value, libraryName);
        assert type == ResourceType.PLURALS;
    }

    /** Adds an element into the array */
    public void addPlural(String quantity, String value) {
        mQuantities.add(quantity);
        mValues.add(value);
    }

    @Override
    public int getPluralsCount() {
        return mQuantities.size();
    }

    @NonNull
    @Override
    public String getQuantity(int index) {
        return mQuantities.get(index);
    }

    @Override
    @NonNull
    public String getValue(int index) {
        return mValues.get(index);
    }

    @Override
    @Nullable
    public String getValue(@NonNull String quantity) {
        assert mQuantities.size() == mValues.size();
        for (int i = 0, n = mQuantities.size(); i < n; i++) {
            if (quantity.equals(mQuantities.get(i))) {
                return mValues.get(i);
            }
        }

        return null;
    }

    @Override
    @Nullable
    public String getValue() {
        // Clients should normally not call this method on PluralsResourceValues; they should
        // pick the specific quantity element they want. However, for compatibility with older
        // layout libs, return the first plurals element's value instead.

        //noinspection VariableNotUsedInsideIf
        if (super.getValue() == null) {
            if (!mValues.isEmpty()) {
                return getValue(0);
            }
        }

        return super.getValue();
    }
}
