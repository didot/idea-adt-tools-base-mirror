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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;

public class Storage {
    private long mNoBytes;

    public Storage(long amount, Unit unit) {
        mNoBytes = amount * unit.getNumberOfBytes();
    }

    public Storage(long amount) {
        this(amount, Unit.B);
    }

    /** Returns the amount of storage represented, in Bytes */
    public long getSize() {
        return getSizeAsUnit(Unit.B);
    }

    @NonNull
    public Storage deepCopy() {
        return new Storage(mNoBytes);
    }

    /**
     * Return the amount of storage represented by the instance in the given unit
     * @param unit The unit of the result.
     * @return The size of the storage in the given unit.
     */
    public long getSizeAsUnit(@NonNull Unit unit) {
        return mNoBytes / unit.getNumberOfBytes();
    }

  /**
   * Returns the amount of storage represented by the instance in the given unit
   * as a double to get a more precise result
   * @param unit The unit of the result.
   * @return The size of the storage in the given unit.
   */
    public double getPreciseSizeAsUnit(@NonNull Unit unit) {
        return ((double)mNoBytes) / unit.getNumberOfBytes();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Storage)) {
            return false;
        }
        return this.getSize() == ((Storage)other).getSize();
    }

    public boolean lessThan(Object other) {
        if (!(other instanceof Storage)) {
            return false;
        }
        return this.getSize() < ((Storage)other).getSize();
    }

    @Override
    public int hashCode() {
        int result = 17;
        return 31 * result + (int)(mNoBytes^(mNoBytes>>>32));
    }

    public enum Unit{
        B("B", "B", 1),
        KiB("KiB", "KB", 1024),
        MiB("MiB", "MB", 1024 * 1024),
        GiB("GiB", "GB", 1024 * 1024 * 1024),
        TiB("TiB", "TB", 1024L * 1024L * 1024L * 1024L);

        @NonNull
        private String mValue;

        @NonNull
        private String mDisplayValue;

        /** The number of bytes needed to have one of the given unit */
        private long mNoBytes;

        Unit(@NonNull String val, @NonNull String displayVal, long noBytes) {
            mValue = val;
            mDisplayValue = displayVal;
            mNoBytes = noBytes;
        }

        @Nullable
        public static Unit getEnum(@NonNull String val) {
            for (Unit unit : values()) {
                if (unit.mValue.equals(val)) {
                    return unit;
                }
            }
            return null;
        }

        public long getNumberOfBytes() {
            return mNoBytes;
        }

        @Override
        @NonNull
        public String toString() {
            return mValue;
        }

        @NonNull
        public String getDisplayValue() {
          return mDisplayValue;
        }
    }

    /**
     * Finds the largest {@link Unit} which can display the storage value as a positive integer
     * with no loss of accuracy.
     * @return The most appropriate {@link Unit}.
     */
    @NonNull
    public Unit getAppropriateUnits() {
        Unit optimalUnit = Unit.B;
        for (Unit unit : Unit.values()) {
            if (mNoBytes % unit.getNumberOfBytes() == 0) {
                optimalUnit = unit;
            } else {
                break;
            }
        }
        return optimalUnit;
    }

    @Override
    @NonNull
    public String toString() {
        Unit unit = getAppropriateUnits();
        return String.format("%d %s", getSizeAsUnit(unit), unit.getDisplayValue());
    }
}
