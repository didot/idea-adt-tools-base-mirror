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

import static com.android.SdkConstants.DOT_ANDROID_PACKAGE;
import static com.android.SdkConstants.FD_RES;
import static com.android.SdkConstants.FD_RES_RAW;
import static com.android.build.gradle.integration.common.truth.TruthHelper.assertThatApk;
import static com.android.builder.core.BuilderConstants.ANDROID_WEAR_MICRO_APK;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.android.build.gradle.integration.common.fixture.GradleBuildResult;
import com.android.build.gradle.integration.common.fixture.GradleTestProject;
import com.android.build.gradle.integration.common.utils.TestFileUtils;
import com.android.build.gradle.integration.common.utils.ZipHelper;
import com.android.ide.common.process.ProcessException;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
/**
 * Assemble tests for embedded wear app with a single app.
 */
public class WearSimpleTest {
    @ClassRule
    public static GradleTestProject project = GradleTestProject.builder()
            .fromTestProject("simpleMicroApp")
            .create();

    @BeforeClass
    public static void setUp() throws IOException {
        File mainAppBuildGradle = project.file("main/build.gradle");

        TestFileUtils.appendToFile(mainAppBuildGradle,
                "dependencies {\n"
                + "  wearApp project(':wear')\n"
                + "}\n");
    }

    @AfterClass
    public static void cleanUp() {
        project = null;
    }

    @Test
    public void checkDefaultEmbedding() throws IOException, ProcessException {
        project.execute("clean", ":main:assemble");

        String embeddedApkPath = FD_RES + '/' + FD_RES_RAW + '/' + ANDROID_WEAR_MICRO_APK +
                DOT_ANDROID_PACKAGE;

        // each micro app has a different version name to distinguish them from one another.
        // here we record what we expect from which.
        List<List<String>> variantData = Lists.newArrayList(
                //Output apk name     Version name
                //---------------     ------------
                Lists.newArrayList("release-unsigned", "default" ),
                Lists.newArrayList("debug",            null));

        for (List<String> data : variantData) {
            String apkName = data.get(0);
            String versionName = data.get(1);
            File fullApk = project.getSubproject("main").getApk(apkName);
            File embeddedApk = ZipHelper.extractFile(fullApk, embeddedApkPath);

            if (versionName == null) {
                assertNull("Expected no embedded app for " + apkName, embeddedApk);
                break;
            }

            assertNotNull("Failed to find embedded micro app for " + apkName, embeddedApk);

            // check for the versionName
            assertThatApk(embeddedApk).hasVersionName(versionName);
        }
    }

    @Test
    public void checkWearReleaseNotBuildWithMainDebug() {
        GradleBuildResult result = project.executor().run("clean", ":main:assembleDebug");
        assertFalse(result.getStdout().contains(":wear:packageRelease"));
    }
}
