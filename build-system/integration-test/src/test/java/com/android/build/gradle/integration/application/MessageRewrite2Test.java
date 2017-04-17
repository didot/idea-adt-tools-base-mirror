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

package com.android.build.gradle.integration.application;

import static com.android.build.gradle.integration.common.truth.TruthHelper.assertThat;

import com.android.build.gradle.integration.common.fixture.GradleBuildResult;
import com.android.build.gradle.integration.common.fixture.GradleTestProject;
import com.android.build.gradle.integration.common.fixture.TemporaryProjectModification;
import com.android.build.gradle.options.BooleanOption;
import com.android.utils.FileUtils;
import com.android.utils.SdkUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class MessageRewrite2Test {

    @ClassRule
    public static GradleTestProject project =
            GradleTestProject.builder().fromTestProject("flavored").create();

    @BeforeClass
    public static void setUp() throws Exception {
        project.execute("assembleDebug");
    }

    @AfterClass
    public static void cleanUp() {
        project = null;
    }

    @Test
    public void testErrorInStrings() throws Exception {
        TemporaryProjectModification.doTest(
                project,
                it -> {
                    it.replaceInFile(
                            "src/main/res/values/strings.xml", "default text", "don't work");

                    assertThat(project.file("src/main/res/values/strings.xml"))
                            .contains(">don't work<");

                    GradleBuildResult result =
                            project.executor()
                                    .with(BooleanOption.IDE_INVOKED_FROM_IDE, true)
                                    .expectFailure()
                                    .run("assembleDebug");
                    assertThat(result.getStderr())
                            .contains(
                                    SdkUtils.escapePropertyValue(
                                            FileUtils.join(
                                                    "src",
                                                    "main",
                                                    "res",
                                                    "values",
                                                    "strings.xml")));
                });

        project.execute("assembleDebug");
    }
}
