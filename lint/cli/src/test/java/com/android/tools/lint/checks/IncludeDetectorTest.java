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

package com.android.tools.lint.checks;

import com.android.tools.lint.detector.api.Detector;

public class IncludeDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new IncludeDetector();
    }

    public void test() throws Exception {
        assertEquals(""
            + "res/layout/include_params.xml:43: Error: Layout parameter layout_margin ignored unless both layout_width and layout_height are also specified on <include> tag [IncludeLayoutParam]\n"
            + "        android:layout_margin=\"20dp\"\n"
            + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
            + "res/layout/include_params.xml:44: Error: Layout parameter layout_weight ignored unless both layout_width and layout_height are also specified on <include> tag [IncludeLayoutParam]\n"
            + "        android:layout_weight=\"1.5\"\n"
            + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
            + "res/layout/include_params.xml:51: Error: Layout parameter layout_weight ignored unless layout_width is also specified on <include> tag [IncludeLayoutParam]\n"
            + "        android:layout_weight=\"1.5\"\n"
            + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
            + "res/layout/include_params.xml:58: Error: Layout parameter layout_weight ignored unless layout_height is also specified on <include> tag [IncludeLayoutParam]\n"
            + "        android:layout_weight=\"1.5\"\n"
            + "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
            + "4 errors, 0 warnings\n",

            lintProject(
                "res/layout/include_params.xml"));
    }
}