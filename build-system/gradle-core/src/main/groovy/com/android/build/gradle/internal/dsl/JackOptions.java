/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.build.gradle.internal.dsl;

import com.android.annotations.Nullable;
import com.google.common.base.Objects;

/**
 * DSL object for configuring Jack options.
 */
public class JackOptions implements CoreJackOptions {
    @Nullable
    private Boolean isEnabledFlag;
    @Nullable
    private Boolean isJackInProcessFlag;

    public void _initWith(CoreJackOptions that) {
        isEnabledFlag = that.isEnabled();
        isJackInProcessFlag = that.isJackInProcess();
    }

    @Override
    @Nullable
    public Boolean isEnabled() {
        return isEnabledFlag;
    }

    public void setEnabled(@Nullable Boolean enabled) {
        isEnabledFlag = enabled;
    }

    @Override
    @Nullable
    public Boolean isJackInProcess() {
        return isJackInProcessFlag;
    }

    public void setJackInProcess(Boolean jackInProcess) {
        isJackInProcessFlag = jackInProcess;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JackOptions that = (JackOptions) o;
        return Objects.equal(isEnabledFlag, that.isEnabledFlag) &&
                Objects.equal(isJackInProcessFlag, that.isJackInProcessFlag);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(isEnabledFlag, isJackInProcessFlag);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("isEnabled", isEnabledFlag)
                .add("isJackInProcess", isJackInProcessFlag)
                .toString();
    }
}
