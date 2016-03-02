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

package com.android.build.gradle.integration.instant;

import static com.android.build.gradle.integration.common.truth.TruthHelper.assertThat;
import static com.android.build.gradle.integration.common.truth.TruthHelper.assertThatDex;

import com.android.annotations.NonNull;
import com.android.build.gradle.integration.common.category.DeviceTests;
import com.android.build.gradle.integration.common.fixture.GradleTestProject;
import com.android.build.gradle.integration.common.fixture.Logcat;
import com.android.build.gradle.integration.common.utils.TestFileUtils;
import com.android.build.gradle.internal.incremental.ColdswapMode;
import com.android.build.gradle.internal.incremental.InstantRunVerifierStatus;
import com.android.builder.model.InstantRun;
import com.android.ddmlib.IDevice;
import com.android.tools.fd.client.InstantRunArtifact;
import com.android.tools.fd.client.InstantRunArtifactType;
import com.android.tools.fd.client.InstantRunBuildInfo;
import com.android.tools.fd.client.InstantRunClient;
import com.google.common.collect.Iterables;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.io.IOException;

public class ButterKnifeTest {
    private static final ColdswapMode COLDSWAP_MODE = ColdswapMode.MULTIDEX;
    private static final String ORIGINAL_MESSAGE = "foo";
    private static final String HOTSWAP_MESSAGE = "clicked";

    @Rule
    public GradleTestProject project =
            GradleTestProject.builder().fromTestProject("butterknife").create();

    private File mActiv;

    @Rule
    public Logcat logcat = Logcat.create();

    @Before
    public void setUp() throws IOException {
        Assume.assumeFalse("Disabled until instant run supports Jack", GradleTestProject.USE_JACK);
        mActiv = project.file("src/main/java/com/example/bk/Activ.java");
    }

    @Test
    public void normalBuild() throws Exception {
        project.execute("assembleDebug");
    }

    @Test
    public void coldSwapBuild() throws Exception {
        InstantRun instantRunModel = InstantRunTestUtils.doInitialBuild(project, 23, COLDSWAP_MODE);

        InstantRunBuildInfo initialContext = InstantRunTestUtils.loadContext(instantRunModel);
        String startBuildId = initialContext.getTimeStamp();

        makeColdSwapChange();

        project.execute(
                InstantRunTestUtils.getInstantRunArgs(23, COLDSWAP_MODE),
                instantRunModel.getIncrementalAssembleTaskName());

        InstantRunBuildInfo coldSwapContext = InstantRunTestUtils.loadContext(instantRunModel);

        assertThat(coldSwapContext.getVerifierStatus()).named("verifier status")
                .isEqualTo(InstantRunVerifierStatus.METHOD_ADDED.toString());
        assertThat(coldSwapContext.getTimeStamp()).named("build id").isNotEqualTo(startBuildId);

        assertThat(coldSwapContext.getArtifacts()).hasSize(1);
        InstantRunArtifact artifact = Iterables.getOnlyElement(coldSwapContext.getArtifacts());

        assertThat(artifact.type).isEqualTo(InstantRunArtifactType.DEX);
        assertThatDex(artifact.file)
                .hasClass("Lcom/example/bk/Activ;")
                .that().hasMethod("getMessage");
    }

    @Test
    public void hotSwap() throws Exception {
        InstantRun instantRunModel = InstantRunTestUtils.doInitialBuild(project, 23, COLDSWAP_MODE);

        makeHotSwapChange();

        project.execute(InstantRunTestUtils.getInstantRunArgs(23, COLDSWAP_MODE),
                instantRunModel.getIncrementalAssembleTaskName());

        InstantRunArtifact artifact =
                InstantRunTestUtils.getCompiledHotSwapCompatibleChange(instantRunModel);

        assertThatDex(artifact.file).hasClass("Lcom/example/bk/Activ$override;");
    }

    private void makeColdSwapChange() throws Exception {
        TestFileUtils.searchAndReplace(mActiv.getAbsoluteFile(),
                "text\\.getText\\(\\)\\.toString\\(\\)", "getMessage()");
        TestFileUtils.addMethod(
                mActiv,
                "public String getMessage() { return text.getText().toString(); }");
    }

    private void makeHotSwapChange() throws Exception {
        TestFileUtils.searchAndReplace(mActiv, "text\\.getText\\(\\)\\.toString\\(\\)",
                "\"clicked!\"");
    }

    @Test
    @Category(DeviceTests.class)
    public void hotSwap_device() throws Exception {
        HotSwapTester.run(
                project,
                "com.example.bk",
                "Activ",
                "butterknife",
                logcat,
                new HotSwapTester.Steps() {
                    @Override
                    public void verifyOriginalCode(
                            @NonNull InstantRunClient client,
                            @NonNull Logcat logcat,
                            @NonNull IDevice device) throws Exception {
                        assertThat(logcat).containsMessageWithText(ORIGINAL_MESSAGE);
                        assertThat(logcat).doesNotContainMessageWithText(HOTSWAP_MESSAGE);
                    }

                    @Override
                    public void makeChange() throws Exception {
                        makeHotSwapChange();
                    }

                    @Override
                    public void verifyNewCode(@NonNull InstantRunClient client,
                            @NonNull Logcat logcat,
                            @NonNull IDevice device) throws Exception {
                        // Should not have restarted activity
                        assertThat(logcat).doesNotContainMessageWithText(ORIGINAL_MESSAGE);
                        assertThat(logcat).doesNotContainMessageWithText(HOTSWAP_MESSAGE);

                        client.restartActivity(device);
                        Thread.sleep(500); // TODO: blocking logcat assertions with timeouts.

                        assertThat(logcat).doesNotContainMessageWithText(ORIGINAL_MESSAGE);
                        assertThat(logcat).containsMessageWithText(HOTSWAP_MESSAGE);
                    }
                });
    }
}
