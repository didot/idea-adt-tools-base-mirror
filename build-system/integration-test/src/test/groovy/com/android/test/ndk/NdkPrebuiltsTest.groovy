/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.test.ndk

import com.android.test.common.fixture.GradleTestProject
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test

/**
 * Assemble tests for ndkPrebuilts.
 */
class NdkPrebuiltsTest {
    @ClassRule
    static public GradleTestProject project = GradleTestProject.builder()
            .fromSample("regular/ndkPrebuilts")
            .create()

    @BeforeClass
    static void setup() {
        project.execute("clean", "assembleDebug");
    }

    @Test
    void assembleDebug() {
    }
}
