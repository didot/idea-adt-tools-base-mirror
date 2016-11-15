/*
 * Copyright (C) 2015 The Android Open Source Project
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

import static com.android.build.gradle.integration.common.utils.AndroidVersionMatcher.thatUsesArt;

import com.android.build.gradle.integration.common.category.DeviceTests;
import com.android.build.gradle.integration.common.fixture.Adb;
import com.android.build.gradle.integration.common.fixture.GradleTestProject;
import com.android.build.gradle.integration.common.utils.AndroidVersionMatcher;
import com.android.ddmlib.IDevice;
import com.google.common.collect.ImmutableList;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Ignore
public class InstantUnitTest {

    @Rule
    public final Adb adb = new Adb();

    @ClassRule
    public static GradleTestProject sProject = GradleTestProject.builder()
            .fromTestProject("instant-unit-tests")
            .create();

    @Test
    public void checkInstantUnitTestsBuild() {
        sProject.execute("clean", "assembleDebugAndroidTest");
    }

    @Test
    @Category(DeviceTests.class)
    public void runTestsOnDevice() {
        sProject.execute("clean");
        IDevice device = adb.getDevice(thatUsesArt());
        sProject.executeConnectedCheck(
                ImmutableList.of(Adb.getInjectToDeviceProviderProperty(device)));
    }
}
