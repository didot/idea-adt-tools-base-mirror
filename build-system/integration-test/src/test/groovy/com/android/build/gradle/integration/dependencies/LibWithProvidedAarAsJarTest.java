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

package com.android.build.gradle.integration.dependencies;

import static com.android.build.gradle.integration.common.utils.TestFileUtils.appendToFile;
import static com.android.build.gradle.integration.common.truth.TruthHelper.assertThatAar;

import com.android.build.gradle.integration.common.fixture.GradleTestProject;
import com.android.build.gradle.integration.common.truth.TruthHelper;
import com.android.build.gradle.integration.common.utils.ModelHelper;
import com.android.builder.model.AndroidProject;
import com.android.builder.model.Dependencies;
import com.android.builder.model.Variant;
import com.android.ide.common.process.ProcessException;
import com.google.common.truth.Truth;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * test for provided jar in library where the jar comes from a library project.
 */
public class LibWithProvidedAarAsJarTest {

    @ClassRule
    public static GradleTestProject project = GradleTestProject.builder()
            .fromTestProject("projectWithModules")
            .create();
    static Map<String, AndroidProject> models;

    @BeforeClass
    public static void setUp() throws IOException {
        appendToFile(project.getSubproject("library").getBuildFile(),
                "\n" +
                "dependencies {\n" +
                "    provided project(path: \":library2\", configuration: \"fakeJar\")\n" +
                "}\n");

        appendToFile(project.getSubproject("library2").getBuildFile(),
                "\n" +
                "configurations {\n" +
                "    fakeJar\n" +
                "}\n" +
                "\n" +
                "task makeFakeJar(type: Jar) {\n" +
                "    from \"src/main/java\"\n" +
                "}\n" +
                "\n" +
                "artifacts {\n" +
                "    fakeJar makeFakeJar\n" +
                "}\n");

        models = project.executeAndReturnMultiModel("clean", ":library:assembleDebug");
    }

    @AfterClass
    public static void cleanUp() {
        project = null;
        models = null;
    }

    @Test
    public void checkProjectJarIsNotPackaged() throws IOException, ProcessException {
        assertThatAar(project.getSubproject("library").getAar("debug"))
                .doesNotContainClass("Lcom/example/android/multiproject/library2/PersionView2;");
    }

    @Test
    public void checkProvidedJarIsInTheMainArtifactDependency() {
        Variant variant = ModelHelper.getVariant(models.get(":library").getVariants(), "debug");
        Truth.assertThat(variant).isNotNull();

        Dependencies deps = variant.getMainArtifact().getDependencies();
        Collection<String> projectDeps = deps.getProjects();
        TruthHelper.assertThat(projectDeps).containsExactly(":library2");
    }

    @Test
    public void checkProvidedJarIsInTheAndroidTestDeps() {
        // TODO
    }

    @Test
    public void checkProvidedJarIsInTheUnitTestDeps() {
        // TODO
    }
}
