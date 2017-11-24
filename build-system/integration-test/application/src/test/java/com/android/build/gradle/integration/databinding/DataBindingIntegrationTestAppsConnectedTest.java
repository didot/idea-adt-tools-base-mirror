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

package com.android.build.gradle.integration.databinding;

import com.android.build.gradle.integration.common.category.DeviceTests;
import com.android.build.gradle.integration.common.fixture.Adb;
import com.android.build.gradle.integration.common.fixture.GradleTestProject;
import com.android.build.gradle.integration.common.runner.FilterableParameterized;
import com.google.common.collect.ImmutableList;
import org.junit.AssumptionViolatedException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(FilterableParameterized.class)
public class DataBindingIntegrationTestAppsConnectedTest {
    @Rule public GradleTestProject project;

    public DataBindingIntegrationTestAppsConnectedTest(String projectName) {
        project = GradleTestProject.builder().fromDataBindingIntegrationTest(projectName).create();
    }

    @Parameterized.Parameters(name = "_{0}")
    public static Iterable<String> classNames() {
        // "App With Spaces", not supported by bazel :/
        return ImmutableList.of(
                "IndependentLibrary",
                "TestApp",
                "ProguardedAppWithTest",
                "AppWithDataBindingInTests");
    }

    @Rule public Adb adb = new Adb();

    @Test
    @Category(DeviceTests.class)
    public void connectedCheck() throws Exception {
        String projectName = project.getName();
        if (projectName.equals("TestApp") || projectName.equals("ProguardedAppWithTest")) {
            // Disabled due to b/69446221
            throw new AssumptionViolatedException(
                    String.format(
                            "Project %s disabled for connected tests due to missing prebuilts.",
                            projectName));
        }

        project.executeConnectedCheck();
    }
}
