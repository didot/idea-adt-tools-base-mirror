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

package com.android.build.gradle.internal.transforms;

import static com.android.build.api.transform.QualifiedContent.DefaultContentType.CLASSES;
import static com.android.build.api.transform.QualifiedContent.DefaultContentType.RESOURCES;
import static com.android.build.api.transform.QualifiedContent.Scope.PROJECT;
import static com.android.build.api.transform.QualifiedContent.Scope.PROJECT_LOCAL_DEPS;
import static com.android.build.gradle.internal.transforms.TransformTestHelper.invocationBuilder;
import static com.android.build.gradle.internal.transforms.TransformTestHelper.singleJarBuilder;
import static com.android.testutils.truth.MoreTruth.assertThat;

import com.android.annotations.NonNull;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInvocation;
import com.android.testutils.apk.Zip;
import com.android.utils.FileUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LibraryIntermediateJarsTransformTest {

    private File mainClassLocation;
    private File localJarsLocation;
    private File resJarLocation;
    private File typedefRecipe;
    private String packageName;

    private LibraryIntermediateJarsTransform transform;

    @Before
    public void setUp() throws Exception {
        packageName = "com.example.android.multiproject.person";
        typedefRecipe = null;

        mainClassLocation = Files.createTempFile(null, null).toFile();
        resJarLocation = Files.createTempFile(null, null).toFile();
        localJarsLocation = Files.createTempDirectory(null).toFile();

        transform = new LibraryIntermediateJarsTransform(
                mainClassLocation,
                localJarsLocation,
                resJarLocation,
                typedefRecipe,
                packageName,
                true);
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteIfExists(mainClassLocation);
        FileUtils.deleteIfExists(resJarLocation);
        FileUtils.deleteDirectoryContents(localJarsLocation);
        FileUtils.deleteIfExists(localJarsLocation);

        mainClassLocation = resJarLocation = localJarsLocation = null;
        packageName = null;
        transform = null;
    }

    @Test
    public void testSimpleMainInput() throws TransformException, InterruptedException, IOException {
        // get a simple input
        TransformInvocation invocation = invocationBuilder()
                .setIncremental(false)
                .addReferenceInput(singleJarBuilder(getInputFile("test-jar1.jar"))
                        .setContentTypes(CLASSES, RESOURCES)
                        .setScopes(PROJECT)
                        .build())
                .build();

        transform.transform(invocation);

        // check the output
        final Zip classZip = new Zip(mainClassLocation);
        final Zip resZip = new Zip(resJarLocation);

        // source code in classZip only
        assertThat(classZip).contains("com/example/android/multiproject/person/People.class");
        assertThat(resZip).doesNotContain("com/example/android/multiproject/person/People.class");

        // resources in resZip only
        assertThat(classZip).doesNotContain("file1.txt");
        assertThat(resZip).contains("file1.txt");

        // R class nowhere
        assertThat(classZip).doesNotContain("com/example/android/multiproject/person/R.class");
        assertThat(resZip).doesNotContain("com/example/android/multiproject/person/R.class");

        // no folder either. Can't check this yet.
        //assertThat(classZip).doesNotContain("com/example/");
        //assertThat(resZip).doesNotContain("com/example/");
    }

    @Test
    public void testSimpleMainInputWithLocal()
            throws TransformException, InterruptedException, IOException {
        // 2 inputs.
        final File localJarSource = getInputFile("test-jar2.jar");
        TransformInvocation invocation = invocationBuilder()
                .setIncremental(false)
                .addReferenceInput(singleJarBuilder(getInputFile("test-jar1.jar"))
                        .setContentTypes(CLASSES, RESOURCES)
                        .setScopes(PROJECT)
                        .build())
                .addReferenceInput(singleJarBuilder(localJarSource)
                        .setContentTypes(CLASSES)
                        .setScopes(PROJECT_LOCAL_DEPS)
                        .build())
                .build();

        transform.transform(invocation);

        // check the output
        File localJar = new File(localJarsLocation, localJarSource.getName());
        assertThat(localJar).named("generated local jar").isFile();

        final Zip classZip = new Zip(mainClassLocation);
        final Zip resZip = new Zip(resJarLocation);
        final Zip localzip = new Zip(localJar);

        // source code in classZip only
        assertThat(classZip).contains("com/example/android/multiproject/person/People.class");
        assertThat(resZip).doesNotContain("com/example/android/multiproject/person/People.class");
        assertThat(localzip).doesNotContain("com/example/android/multiproject/person/People.class");

        // resources in resZip only
        assertThat(classZip).doesNotContain("file1.txt");
        assertThat(resZip).contains("file1.txt");
        assertThat(localzip).doesNotContain("file1.txt");
        assertThat(classZip).doesNotContain("file2.txt");
        assertThat(resZip).doesNotContain("file2.txt");
        assertThat(localzip).doesNotContain("file2.txt");

        // R class nowhere
        assertThat(classZip).doesNotContain("com/example/android/multiproject/person/R.class");
        assertThat(resZip).doesNotContain("com/example/android/multiproject/person/R.class");
        assertThat(localzip).doesNotContain("com/example/android/multiproject/person/R.class");

        // class from local jar in local jar only.
        assertThat(classZip).doesNotContain("com/example/android/multiproject/person/Foo.class");
        assertThat(resZip).doesNotContain("com/example/android/multiproject/person/Foo.class");
        assertThat(localzip).contains("com/example/android/multiproject/person/Foo.class");
    }

    private File getInputFile(@NonNull String name) throws IOException {
        InputStream stream = this.getClass().getResourceAsStream(name);
        final Path inputPath = Files.createTempFile(null, null);
        File inputFile = inputPath.toFile();
        inputFile.deleteOnExit();
        FileUtils.deleteIfExists(inputFile);

        Files.copy(stream, inputPath);

        return inputFile;
    }
}