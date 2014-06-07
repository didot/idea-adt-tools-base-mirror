/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.build.gradle.model;

import static com.android.builder.core.BuilderConstants.ANDROID_TEST;
import static com.android.builder.model.AndroidProject.ARTIFACT_ANDROID_TEST;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.internal.StringHelper;
import com.android.builder.model.AndroidArtifact;
import com.android.builder.model.AndroidLibrary;
import com.android.builder.model.AndroidProject;
import com.android.builder.model.ApiVersion;
import com.android.builder.model.ArtifactMetaData;
import com.android.builder.model.BuildTypeContainer;
import com.android.builder.model.Dependencies;
import com.android.builder.model.JavaArtifact;
import com.android.builder.model.JavaCompileOptions;
import com.android.builder.model.JavaLibrary;
import com.android.builder.model.ProductFlavor;
import com.android.builder.model.ProductFlavorContainer;
import com.android.builder.model.SigningConfig;
import com.android.builder.model.SourceProvider;
import com.android.builder.model.SourceProviderContainer;
import com.android.builder.model.Variant;
import com.android.ide.common.signing.KeystoreHelper;
import com.android.prefs.AndroidLocation;
import com.google.common.collect.Maps;

import junit.framework.TestCase;

import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.UnknownModelException;
import org.gradle.tooling.model.GradleProject;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.KeyStore;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AndroidProjectTest extends TestCase {

    private static final String MODEL_VERSION = "0.11.0";

    private static final Map<String, ProjectData> sProjectModelMap = Maps.newHashMap();

    private static final class ProjectData {
        AndroidProject model;
        File projectDir;

        static ProjectData create(File projectDir, AndroidProject model) {
            ProjectData projectData = new ProjectData();
            projectData.model = model;
            projectData.projectDir = projectDir;

            return projectData;
        }
    }

    private static final class DefaultApiVersion implements ApiVersion {
        private final int mApiLevel;

        @Nullable
        private final String mCodename;

        public DefaultApiVersion(int apiLevel, @Nullable String codename) {
            mApiLevel = apiLevel;
            mCodename = codename;
        }

        public DefaultApiVersion(int apiLevel) {
            this(apiLevel, null);
        }

        public DefaultApiVersion(@NonNull String codename) {
            this(1, codename);
        }

        public static ApiVersion create(@NonNull Object value) {
            if (value instanceof Integer) {
                return new DefaultApiVersion((Integer) value, null);
            } else if (value instanceof String) {
                return new DefaultApiVersion(1, (String) value);
            }

            return null;
        }

        @Override
        public int getApiLevel() {
            return mApiLevel;
        }

        @Nullable
        @Override
        public String getCodename() {
            return mCodename;
        }

        @NonNull
        @Override
        public String getApiString() {
            return mCodename != null ? mCodename : Integer.toString(mApiLevel);
        }

        @Override
        public boolean equals(Object o) {
            /**
             * Normally equals only test for the same exact class, but here me make it accept
             * ApiVersion since we're comparing it against implementations that are serialized
             * across Gradle's tooling api.
             */
            if (this == o) {
                return true;
            }
            if (!(o instanceof ApiVersion)) {
                return false;
            }

            ApiVersion that = (ApiVersion) o;

            if (mApiLevel != that.getApiLevel()) {
                return false;
            }
            if (mCodename != null ? !mCodename.equals(that.getCodename()) : that.getCodename() != null) {
                return false;
            }

            return true;
        }

    }

    private ProjectData getModelForProject(String projectName) {
        ProjectData projectData = sProjectModelMap.get(projectName);

        if (projectData == null) {
            // Configure the connector and create the connection
            GradleConnector connector = GradleConnector.newConnector();

            File projectDir = new File(getTestDir(), projectName);
            connector.forProjectDirectory(projectDir);

            ProjectConnection connection = connector.connect();
            try {
                // Load the custom model for the project
                AndroidProject model = connection.getModel(AndroidProject.class);
                assertNotNull("Model Object null-check", model);
                assertEquals("Model Name", projectName, model.getName());
                assertEquals("Model version", MODEL_VERSION, model.getModelVersion());

                projectData = ProjectData.create(projectDir, model);

                sProjectModelMap.put(projectName, projectData);

                return projectData;
            } finally {
                connection.close();
            }
        }

        return projectData;
    }

    private Map<String, ProjectData> getModelForMultiProject(String projectName) throws Exception {
        // Configure the connector and create the connection
        GradleConnector connector = GradleConnector.newConnector();

        File projectDir = new File(getTestDir(), projectName);
        connector.forProjectDirectory(projectDir);

        Map<String, ProjectData> map = Maps.newHashMap();

        ProjectConnection connection = connector.connect();

        try {
            // Query the default Gradle Model.
            GradleProject model = connection.getModel(GradleProject.class);
            assertNotNull("Model Object null-check", model);

            // Now get the children projects, recursively.
            for (GradleProject child : model.getChildren()) {
                String path = child.getPath();
                String name = path.substring(1);
                File childDir = new File(projectDir, name);

                GradleConnector childConnector = GradleConnector.newConnector();

                childConnector.forProjectDirectory(childDir);

                ProjectConnection childConnection = childConnector.connect();
                try {
                    AndroidProject androidProject = childConnection.getModel(AndroidProject.class);

                    assertNotNull("Model Object null-check for " + path, androidProject);
                    assertEquals("Model Name for " + path, name, androidProject.getName());
                    assertEquals("Model version", MODEL_VERSION, androidProject.getModelVersion());

                    map.put(path, ProjectData.create(childDir, androidProject));

                } catch (UnknownModelException e) {
                    // probably a Java-only project, ignore.
                } finally {
                    childConnection.close();
                }
            }
        } finally {
            connection.close();
        }

        return map;
    }

    public void testBasic() {
        // Load the custom model for the project
        ProjectData projectData = getModelForProject("basic");

        AndroidProject model = projectData.model;

        assertFalse("Library Project", model.isLibrary());
        assertEquals("Compile Target", "android-19", model.getCompileTarget());
        assertFalse("Non empty bootclasspath", model.getBootClasspath().isEmpty());

        JavaCompileOptions javaCompileOptions = model.getJavaCompileOptions();
        assertEquals("1.6", javaCompileOptions.getSourceCompatibility());
        assertEquals("1.6", javaCompileOptions.getTargetCompatibility());
    }

    public void testBasicSourceProviders() throws Exception {
        // Load the custom model for the project
        ProjectData projectData = getModelForProject("basic");

        AndroidProject model = projectData.model;
        File projectDir = projectData.projectDir;

        testDefaultSourceSets(model, projectDir);

        // test the source provider for the artifacts
        for (Variant variant : model.getVariants()) {
            AndroidArtifact artifact = variant.getMainArtifact();
            assertNull(artifact.getVariantSourceProvider());
            assertNull(artifact.getMultiFlavorSourceProvider());
        }
    }

    public void testBasicMultiFlavorsSourceProviders() throws Exception {
        // Load the custom model for the project
        ProjectData projectData = getModelForProject("basicMultiFlavors");

        AndroidProject model = projectData.model;
        File projectDir = projectData.projectDir;

        testDefaultSourceSets(model, projectDir);

        // test the source provider for the flavor
        Collection<ProductFlavorContainer> productFlavors = model.getProductFlavors();
        assertEquals("Product Flavor Count", 4, productFlavors.size());

        for (ProductFlavorContainer pfContainer : productFlavors) {
            String name = pfContainer.getProductFlavor().getName();
            new SourceProviderTester(
                    model.getName(),
                    projectDir,
                    name,
                    pfContainer.getSourceProvider())
                .test();

            assertEquals(1, pfContainer.getExtraSourceProviders().size());
            SourceProviderContainer container = getSourceProviderContainer(
                    pfContainer.getExtraSourceProviders(), ARTIFACT_ANDROID_TEST);
            assertNotNull(container);

            new SourceProviderTester(
                    model.getName(),
                    projectDir,
                    ANDROID_TEST + StringHelper.capitalize(name),
                    container.getSourceProvider())
                .test();
        }

        // test the source provider for the artifacts
        for (Variant variant : model.getVariants()) {
            AndroidArtifact artifact = variant.getMainArtifact();
            assertNotNull(artifact.getVariantSourceProvider());
            assertNotNull(artifact.getMultiFlavorSourceProvider());
        }
    }

    private static void testDefaultSourceSets(@NonNull AndroidProject model,
            @NonNull File projectDir) {
        ProductFlavorContainer defaultConfig = model.getDefaultConfig();

        // test the main source provider
        new SourceProviderTester(model.getName(), projectDir,
                "main", defaultConfig.getSourceProvider())
                .test();

        // test the main instrumentTest source provider
        SourceProviderContainer testSourceProviders = getSourceProviderContainer(
                defaultConfig.getExtraSourceProviders(), ARTIFACT_ANDROID_TEST);
        assertNotNull("InstrumentTest source Providers null-check", testSourceProviders);

        new SourceProviderTester(model.getName(), projectDir,
                ANDROID_TEST, testSourceProviders.getSourceProvider())
            .test();

        // test the source provider for the build types
        Collection<BuildTypeContainer> buildTypes = model.getBuildTypes();
        assertEquals("Build Type Count", 2, buildTypes.size());

        for (BuildTypeContainer btContainer : model.getBuildTypes()) {
            new SourceProviderTester(
                    model.getName(),
                    projectDir,
                    btContainer.getBuildType().getName(),
                    btContainer.getSourceProvider())
                .test();

            assertEquals(0, btContainer.getExtraSourceProviders().size());
        }
    }

    public void testBasicVariantDetails() throws Exception {
        // Load the custom model for the project
        ProjectData projectData = getModelForProject("basic");

        AndroidProject model = projectData.model;

        Collection<Variant> variants = model.getVariants();
        assertEquals("Variant Count", 2 , variants.size());

        // debug variant
        Variant debugVariant = getVariant(variants, "debug");
        assertNotNull("debug Variant null-check", debugVariant);
        new ProductFlavorTester(debugVariant.getMergedFlavor(), "Debug Merged Flavor")
                .setVersionCode(12)
                .setVersionName("2.0")
                .setMinSdkVersion(16)
                .setTargetSdkVersion(16)
                .setTestInstrumentationRunner("android.test.InstrumentationTestRunner")
                .setTestHandleProfiling(Boolean.FALSE)
                .setTestFunctionalTest(null)
            .test();

        // debug variant, tested.
        AndroidArtifact debugMainInfo = debugVariant.getMainArtifact();
        assertNotNull("Debug main info null-check", debugMainInfo);
        assertEquals("Debug package name", "com.android.tests.basic.debug",
                debugMainInfo.getApplicationId());
        assertTrue("Debug signed check", debugMainInfo.isSigned());
        assertEquals("Debug signingConfig name", "myConfig", debugMainInfo.getSigningConfigName());
        assertEquals("Debug sourceGenTask", "generateDebugSources", debugMainInfo.getSourceGenTaskName());
        assertEquals("Debug javaCompileTask", "compileDebugJava", debugMainInfo.getJavaCompileTaskName());

        // this variant is tested.
        Collection<AndroidArtifact> debugExtraAndroidArtifacts = debugVariant.getExtraAndroidArtifacts();
        AndroidArtifact debugTestInfo = getAndroidArtifact(debugExtraAndroidArtifacts,
                ARTIFACT_ANDROID_TEST);
        assertNotNull("Test info null-check", debugTestInfo);
        assertEquals("Test package name", "com.android.tests.basic.debug.test",
                debugTestInfo.getApplicationId());
        assertNotNull("Test output file null-check", debugTestInfo.getOutputFile());
        assertTrue("Test signed check", debugTestInfo.isSigned());
        assertEquals("Test signingConfig name", "myConfig", debugTestInfo.getSigningConfigName());
        assertEquals("Test sourceGenTask", "generateDebugTestSources", debugTestInfo.getSourceGenTaskName());
        assertEquals("Test javaCompileTask", "compileDebugTestJava", debugTestInfo.getJavaCompileTaskName());

        // release variant, not tested.
        Variant releaseVariant = getVariant(variants, "release");
        assertNotNull("release Variant null-check", releaseVariant);

        AndroidArtifact relMainInfo = releaseVariant.getMainArtifact();
        assertNotNull("Release main info null-check", relMainInfo);
        assertEquals("Release package name", "com.android.tests.basic",
                relMainInfo.getApplicationId());
        assertFalse("Release signed check", relMainInfo.isSigned());
        assertNull("Release signingConfig name", relMainInfo.getSigningConfigName());
        assertEquals("Release sourceGenTask", "generateReleaseSources", relMainInfo.getSourceGenTaskName());
        assertEquals("Release javaCompileTask", "compileReleaseJava", relMainInfo.getJavaCompileTaskName());

        Collection<AndroidArtifact> releaseExtraAndroidArtifacts = releaseVariant.getExtraAndroidArtifacts();
        AndroidArtifact relTestInfo = getAndroidArtifact(releaseExtraAndroidArtifacts, ARTIFACT_ANDROID_TEST);
        assertNull("Release test info null-check", relTestInfo);

        // check debug dependencies
        Dependencies dependencies = debugMainInfo.getDependencies();
        assertNotNull(dependencies);
        assertEquals(2, dependencies.getJavaLibraries().size());
        assertEquals(1, dependencies.getLibraries().size());

        AndroidLibrary lib = dependencies.getLibraries().iterator().next();
        assertNotNull(lib);
        assertNotNull(lib.getBundle());
        assertNotNull(lib.getFolder());

        assertTrue(dependencies.getProjects().isEmpty());
    }

    public void testBasicSigningConfigs() throws Exception {
        // Load the custom model for the project
        ProjectData projectData = getModelForProject("basic");

        AndroidProject model = projectData.model;

        Collection<SigningConfig> signingConfigs = model.getSigningConfigs();
        assertNotNull("SigningConfigs null-check", signingConfigs);
        assertEquals("Number of signingConfig", 2, signingConfigs.size());

        SigningConfig debugSigningConfig = getSigningConfig(signingConfigs, "debug");
        assertNotNull("debug signing config null-check", debugSigningConfig);
        new SigningConfigTester(debugSigningConfig, "debug", true).test();

        SigningConfig mySigningConfig = getSigningConfig(signingConfigs, "myConfig");
        assertNotNull("myConfig signing config null-check", mySigningConfig);
        new SigningConfigTester(mySigningConfig, "myConfig", true)
                .setStoreFile(new File(projectData.projectDir, "debug.keystore"))
                .test();
    }

    public void testMigrated() throws Exception {
        // Load the custom model for the project
        ProjectData projectData = getModelForProject("migrated");

        AndroidProject model = projectData.model;
        File projectDir = projectData.projectDir;

        assertNotNull("Model Object null-check", model);
        assertEquals("Model Name", "migrated", model.getName());
        assertFalse("Library Project", model.isLibrary());

        ProductFlavorContainer defaultConfig = model.getDefaultConfig();

        new SourceProviderTester(model.getName(), projectDir,
                "main", defaultConfig.getSourceProvider())
                .setJavaDir("src")
                .setResourcesDir("src")
                .setAidlDir("src")
                .setRenderscriptDir("src")
                .setResDir("res")
                .setAssetsDir("assets")
                .setManifestFile("AndroidManifest.xml")
                .test();

        SourceProviderContainer testSourceProviderContainer = getSourceProviderContainer(
                defaultConfig.getExtraSourceProviders(), ARTIFACT_ANDROID_TEST);
        assertNotNull("InstrumentTest source Providers null-check", testSourceProviderContainer);

        new SourceProviderTester(model.getName(), projectDir,
                ANDROID_TEST, testSourceProviderContainer.getSourceProvider())
                .setJavaDir("tests/java")
                .setResourcesDir("tests/resources")
                .setAidlDir("tests/aidl")
                .setJniDir("tests/jni")
                .setRenderscriptDir("tests/rs")
                .setResDir("tests/res")
                .setAssetsDir("tests/assets")
                .setManifestFile("tests/AndroidManifest.xml")
                .test();
    }

    public void testRenamedApk() throws Exception {
        // Load the custom model for the project
        ProjectData projectData = getModelForProject("renamedApk");

        AndroidProject model = projectData.model;
        File projectDir = projectData.projectDir;

        assertNotNull("Model Object null-check", model);
        assertEquals("Model Name", "renamedApk", model.getName());

        Collection<Variant> variants = model.getVariants();
        assertEquals("Variant Count", 2 , variants.size());

        File buildDir = new File(projectDir, "build");

        for (Variant variant : variants) {
            AndroidArtifact mainInfo = variant.getMainArtifact();
            assertNotNull(
                    "Null-check on mainArtifactInfo for " + variant.getDisplayName(),
                    mainInfo);

            assertEquals("Output file for " + variant.getName(),
                    new File(buildDir, variant.getName() + ".apk"),
                    mainInfo.getOutputFile());
        }
    }

    public void testFilteredOutBuildType() {
        // Load the custom model for the project
        ProjectData projectData = getModelForProject("filteredOutBuildType");

        AndroidProject model = projectData.model;

        assertEquals("Variant Count", 1, model.getVariants().size());
        Variant variant = model.getVariants().iterator().next();
        assertEquals("Variant name", "release", variant.getBuildType());
    }

    public void testFilteredOutVariants() {
        // Load the custom model for the project
        ProjectData projectData = getModelForProject("filteredOutVariants");

        AndroidProject model = projectData.model;

        Collection<Variant> variants = model.getVariants();
        // check we have the right number of variants:
        // arm/cupcake, arm/gingerbread, x86/gingerbread, mips/gingerbread
        // all 4 in release and debug
        assertEquals("Variant Count", 8, variants.size());

        for (Variant variant : variants) {
            List<String> flavors = variant.getProductFlavors();
            assertFalse("check ignored x86/cupcake", flavors.contains("x68") && flavors.contains("cupcake"));
            assertFalse("check ignored mips/cupcake", flavors.contains("mips") && flavors.contains("cupcake"));
        }
    }

    public void testFlavors() {
        // Load the custom model for the project
        ProjectData projectData = getModelForProject("flavors");

        AndroidProject model = projectData.model;
        File projectDir = projectData.projectDir;

        assertNotNull("Model Object null-check", model);
        assertEquals("Model Name", "flavors", model.getName());
        assertFalse("Library Project", model.isLibrary());

        ProductFlavorContainer defaultConfig = model.getDefaultConfig();

        new SourceProviderTester(model.getName(), projectDir,
                "main", defaultConfig.getSourceProvider())
                .test();

        SourceProviderContainer testSourceProviderContainer = getSourceProviderContainer(
                defaultConfig.getExtraSourceProviders(), ARTIFACT_ANDROID_TEST);
        assertNotNull("InstrumentTest source Providers null-check", testSourceProviderContainer);

        new SourceProviderTester(model.getName(), projectDir,
                ANDROID_TEST, testSourceProviderContainer.getSourceProvider())
                .test();

        Collection<BuildTypeContainer> buildTypes = model.getBuildTypes();
        assertEquals("Build Type Count", 2, buildTypes.size());

        Collection<Variant> variants = model.getVariants();
        assertEquals("Variant Count", 8, variants.size());

        Variant f1faDebugVariant = getVariant(variants, "f1FaDebug");
        assertNotNull("f1faDebug Variant null-check", f1faDebugVariant);
        new ProductFlavorTester(f1faDebugVariant.getMergedFlavor(), "F1faDebug Merged Flavor")
                .test();
        new VariantTester(f1faDebugVariant, projectDir, "flavors-f1-fa-debug.apk").test();
    }

    public void testTicTacToe() throws Exception {
        Map<String, ProjectData> map = getModelForMultiProject("tictactoe");

        ProjectData libModelData = map.get(":lib");
        assertNotNull("lib module model null-check", libModelData);
        assertTrue("lib module library flag", libModelData.model.isLibrary());

        ProjectData appModelData = map.get(":app");
        assertNotNull("app module model null-check", appModelData);

        Collection<Variant> variants = appModelData.model.getVariants();
        Variant debugVariant = getVariant(variants, "debug");
        assertNotNull("debug variant null-check", debugVariant);

        Dependencies dependencies = debugVariant.getMainArtifact().getDependencies();
        assertNotNull(dependencies);

        Collection<AndroidLibrary> libs = dependencies.getLibraries();
        assertNotNull(libs);
        assertEquals(1, libs.size());

        AndroidLibrary androidLibrary = libs.iterator().next();
        assertNotNull(androidLibrary);

        assertEquals("Dependency project path", ":lib", androidLibrary.getProject());

        // TODO: right now we can only test the folder name efficiently
        assertTrue(androidLibrary.getFolder().getPath().endsWith("/tictactoe/lib/unspecified"));
    }

    public void testFlavorLib() throws Exception {
        Map<String, ProjectData> map = getModelForMultiProject("flavorlib");

        ProjectData appModelData = map.get(":app");
        assertNotNull("Module app null-check", appModelData);
        AndroidProject model = appModelData.model;

        assertFalse("Library Project", model.isLibrary());

        Collection<Variant> variants = model.getVariants();
        Collection<ProductFlavorContainer> productFlavors = model.getProductFlavors();

        ProductFlavorContainer flavor1 = getProductFlavor(productFlavors, "flavor1");
        assertNotNull(flavor1);

        Variant flavor1Debug = getVariant(variants, "flavor1Debug");
        assertNotNull(flavor1Debug);

        Dependencies dependencies = flavor1Debug.getMainArtifact().getDependencies();
        assertNotNull(dependencies);
        Collection<AndroidLibrary> libs = dependencies.getLibraries();
        assertNotNull(libs);
        assertEquals(1, libs.size());
        AndroidLibrary androidLibrary = libs.iterator().next();
        assertNotNull(androidLibrary);
        assertEquals(":lib1", androidLibrary.getProject());
        // TODO: right now we can only test the folder name efficiently
        assertTrue(androidLibrary.getFolder().getPath(), androidLibrary.getFolder().getPath().endsWith("/flavorlib/lib1/unspecified"));

        ProductFlavorContainer flavor2 = getProductFlavor(productFlavors, "flavor2");
        assertNotNull(flavor2);

        Variant flavor2Debug = getVariant(variants, "flavor2Debug");
        assertNotNull(flavor2Debug);

        dependencies = flavor2Debug.getMainArtifact().getDependencies();
        assertNotNull(dependencies);
        libs = dependencies.getLibraries();
        assertNotNull(libs);
        assertEquals(1, libs.size());
        androidLibrary = libs.iterator().next();
        assertNotNull(androidLibrary);
        assertEquals(":lib2", androidLibrary.getProject());
        // TODO: right now we can only test the folder name efficiently
        assertTrue(androidLibrary.getFolder().getPath(), androidLibrary.getFolder().getPath().endsWith("/flavorlib/lib2/unspecified"));
    }

    public void testFlavoredLib() throws Exception {
        Map<String, ProjectData> map = getModelForMultiProject("flavoredlib");

        ProjectData appModelData = map.get(":app");
        assertNotNull("Module app null-check", appModelData);
        AndroidProject model = appModelData.model;

        assertFalse("Library Project", model.isLibrary());

        Collection<Variant> variants = model.getVariants();
        Collection<ProductFlavorContainer> productFlavors = model.getProductFlavors();

        ProductFlavorContainer flavor1 = getProductFlavor(productFlavors, "flavor1");
        assertNotNull(flavor1);

        Variant flavor1Debug = getVariant(variants, "flavor1Debug");
        assertNotNull(flavor1Debug);

        Dependencies dependencies = flavor1Debug.getMainArtifact().getDependencies();
        assertNotNull(dependencies);
        Collection<AndroidLibrary> libs = dependencies.getLibraries();
        assertNotNull(libs);
        assertEquals(1, libs.size());
        AndroidLibrary androidLibrary = libs.iterator().next();
        assertNotNull(androidLibrary);
        assertEquals(":lib", androidLibrary.getProject());
        assertEquals("flavor1Release", androidLibrary.getProjectVariant());
        // TODO: right now we can only test the folder name efficiently
        assertTrue(androidLibrary.getFolder().getPath(), androidLibrary.getFolder().getPath().endsWith("/flavoredlib/lib/unspecified/flavor1Release"));

        ProductFlavorContainer flavor2 = getProductFlavor(productFlavors, "flavor2");
        assertNotNull(flavor2);

        Variant flavor2Debug = getVariant(variants, "flavor2Debug");
        assertNotNull(flavor2Debug);

        dependencies = flavor2Debug.getMainArtifact().getDependencies();
        assertNotNull(dependencies);
        libs = dependencies.getLibraries();
        assertNotNull(libs);
        assertEquals(1, libs.size());
        androidLibrary = libs.iterator().next();
        assertNotNull(androidLibrary);
        assertEquals(":lib", androidLibrary.getProject());
        assertEquals("flavor2Release", androidLibrary.getProjectVariant());
        // TODO: right now we can only test the folder name efficiently
        assertTrue(androidLibrary.getFolder().getPath(), androidLibrary.getFolder().getPath().endsWith("/flavoredlib/lib/unspecified/flavor2Release"));
    }

    public void testMultiproject() throws Exception {
        Map<String, ProjectData> map = getModelForMultiProject("multiproject");

        ProjectData baseLibModelData = map.get(":baseLibrary");
        assertNotNull("Module app null-check", baseLibModelData);
        AndroidProject model = baseLibModelData.model;

        Collection<Variant> variants = model.getVariants();
        assertEquals("Variant count", 2, variants.size());

        Variant variant = getVariant(variants, "release");
        assertNotNull("release variant null-check", variant);

        AndroidArtifact mainInfo = variant.getMainArtifact();
        assertNotNull("Main Artifact null-check", mainInfo);

        Dependencies dependencies = mainInfo.getDependencies();
        assertNotNull("Dependencies null-check", dependencies);

        Collection<String> projects = dependencies.getProjects();
        assertNotNull("project dep list null-check", projects);
        assertEquals("project dep count", 1, projects.size());
        assertEquals("dep on :util check", ":util", projects.iterator().next());

        Collection<JavaLibrary> javaLibraries = dependencies.getJavaLibraries();
        assertNotNull("jar dep list null-check", javaLibraries);
        // TODO these are jars coming from ':util' They shouldn't be there.
        assertEquals("jar dep count", 2, javaLibraries.size());
    }

    public void testTestWithDep() {
        // Load the custom model for the project
        ProjectData projectData = getModelForProject("testWithDep");

        AndroidProject model = projectData.model;

        Collection<Variant> variants = model.getVariants();
        Variant debugVariant = getVariant(variants, "debug");
        assertNotNull(debugVariant);

        Collection<AndroidArtifact> extraAndroidArtifact = debugVariant.getExtraAndroidArtifacts();
        AndroidArtifact testArtifact = getAndroidArtifact(extraAndroidArtifact,
                ARTIFACT_ANDROID_TEST);
        assertNotNull(testArtifact);

        Dependencies testDependencies = testArtifact.getDependencies();
        assertEquals(1, testDependencies.getJavaLibraries().size());
    }

    public void testLibTestDep() {
        // Load the custom model for the project
        ProjectData projectData = getModelForProject("libTestDep");

        AndroidProject model = projectData.model;

        Collection<Variant> variants = model.getVariants();
        Variant debugVariant = getVariant(variants, "debug");
        assertNotNull(debugVariant);

        Collection<AndroidArtifact> extraAndroidArtifact = debugVariant.getExtraAndroidArtifacts();
        AndroidArtifact testArtifact = getAndroidArtifact(extraAndroidArtifact,
                ARTIFACT_ANDROID_TEST);
        assertNotNull(testArtifact);

        Dependencies testDependencies = testArtifact.getDependencies();
        Collection<JavaLibrary> javaLibraries = testDependencies.getJavaLibraries();
        assertEquals(2, javaLibraries.size());
        for (JavaLibrary lib : javaLibraries) {
            File f = lib.getJarFile();
            assertTrue(f.getName().equals("guava-11.0.2.jar") || f.getName().equals("jsr305-1.3.9.jar"));
        }
    }

    public void testRsSupportMode() throws Exception {
        // Load the custom model for the project
        ProjectData projectData = getModelForProject("rsSupportMode");

        AndroidProject model = projectData.model;
        File projectDir = projectData.projectDir;

        Variant debugVariant = getVariant(model.getVariants(), "x86Debug");
        assertNotNull("x86Debug variant null-check", debugVariant);

        AndroidArtifact mainArtifact = debugVariant.getMainArtifact();
        Dependencies dependencies = mainArtifact.getDependencies();

        assertFalse(dependencies.getJavaLibraries().isEmpty());

        boolean foundSupportJar = false;
        for (JavaLibrary lib : dependencies.getJavaLibraries()) {
            File file = lib.getJarFile();
            if (SdkConstants.FN_RENDERSCRIPT_V8_JAR.equals(file.getName())) {
                foundSupportJar = true;
                break;
            }
        }

        assertTrue("Found suppport jar check", foundSupportJar);
    }


    public void testGenFolderApi() throws Exception {
        // Load the custom model for the project
        ProjectData projectData = getModelForProject("genFolderApi");

        AndroidProject model = projectData.model;
        File projectDir = projectData.projectDir;

        File buildDir = new File(projectDir, "build");

        for (Variant variant : model.getVariants()) {

            AndroidArtifact mainInfo = variant.getMainArtifact();
            assertNotNull(
                    "Null-check on mainArtifactInfo for " + variant.getDisplayName(),
                    mainInfo);

            // get the generated source folders.
            Collection<File> genFolder = mainInfo.getGeneratedSourceFolders();

            // We're looking for a custom folder
            String folderStart = new File(buildDir, "customCode").getAbsolutePath() + File.separatorChar;
            boolean found = false;
            for (File f : genFolder) {
                if (f.getAbsolutePath().startsWith(folderStart)) {
                    found = true;
                    break;
                }
            }

            assertTrue("custom generated source folder check", found);
        }
    }

    public void testGenFolderApi2() throws Exception {
        // Load the custom model for the project
        ProjectData projectData = getModelForProject("genFolderApi2");

        AndroidProject model = projectData.model;
        File projectDir = projectData.projectDir;

        File buildDir = new File(projectDir, "build");

        for (Variant variant : model.getVariants()) {

            AndroidArtifact mainInfo = variant.getMainArtifact();
            assertNotNull(
                    "Null-check on mainArtifactInfo for " + variant.getDisplayName(),
                    mainInfo);

            // get the generated source folders.
            Collection<File> genFolder = mainInfo.getGeneratedSourceFolders();

            // We're looking for a custom folder
            String folderStart = new File(buildDir, "customCode").getAbsolutePath() + File.separatorChar;
            boolean found = false;
            for (File f : genFolder) {
                if (f.getAbsolutePath().startsWith(folderStart)) {
                    found = true;
                    break;
                }
            }

            assertTrue("custom generated source folder check", found);
        }
    }

    public void testArtifactApi() throws Exception {
        // Load the custom model for the project
        ProjectData projectData = getModelForProject("artifactApi");

        AndroidProject model = projectData.model;

        // check the Artifact Meta Data
        Collection<ArtifactMetaData> extraArtifacts = model.getExtraArtifacts();
        assertNotNull("Extra artifact collection null-check", extraArtifacts);
        assertEquals("Extra artifact size check", 2, extraArtifacts.size());

        assertNotNull("instrument test metadata null-check",
                getArtifactMetaData(extraArtifacts, ARTIFACT_ANDROID_TEST));

        // get the custom one.
        ArtifactMetaData extraArtifactMetaData = getArtifactMetaData(extraArtifacts, "__test__");
        assertNotNull("custom extra metadata null-check", extraArtifactMetaData);
        assertFalse("custom extra meta data is Test check", extraArtifactMetaData.isTest());
        assertEquals("custom extra meta data type check", ArtifactMetaData.TYPE_JAVA, extraArtifactMetaData.getType());

        // check the extra source provider on the build Types.
        for (BuildTypeContainer btContainer : model.getBuildTypes()) {
            String name = btContainer.getBuildType().getName();
            Collection<SourceProviderContainer> extraSourceProviderContainers = btContainer.getExtraSourceProviders();
            assertNotNull(
                    "Extra source provider containers for build type '" + name + "' null-check",
                    extraSourceProviderContainers);
            assertEquals(
                    "Extra source provider containers for build type size '" + name + "' check",
                    1,
                    extraSourceProviderContainers.size());

            SourceProviderContainer sourceProviderContainer = extraSourceProviderContainers.iterator().next();
            assertNotNull(
                    "Extra artifact source provider for " + name + " null check",
                    sourceProviderContainer);

            assertEquals(
                    "Extra artifact source provider for " + name + " name check",
                    "__test__",
                    sourceProviderContainer.getArtifactName());

            assertEquals(
                    "Extra artifact source provider for " + name + " value check",
                    "buildType:" + name,
                    sourceProviderContainer.getSourceProvider().getManifestFile().getPath());
        }

        // check the extra source provider on the product flavors.
        for (ProductFlavorContainer pfContainer : model.getProductFlavors()) {
            String name = pfContainer.getProductFlavor().getName();
            Collection<SourceProviderContainer> extraSourceProviderContainers = pfContainer.getExtraSourceProviders();
            assertNotNull(
                    "Extra source provider container for product flavor '" + name + "' null-check",
                    extraSourceProviderContainers);
            assertEquals(
                    "Extra artifact source provider container for product flavor size '" + name + "' check",
                    2,
                    extraSourceProviderContainers.size());

            assertNotNull(
                    "Extra source provider container for product flavor '" + name + "': instTest check",
                    getSourceProviderContainer(extraSourceProviderContainers, ARTIFACT_ANDROID_TEST));


            SourceProviderContainer sourceProviderContainer = getSourceProviderContainer(
                    extraSourceProviderContainers, "__test__");
            assertNotNull(
                    "Custom source provider container for " + name + " null check",
                    sourceProviderContainer);

            assertEquals(
                    "Custom artifact source provider for " + name + " name check",
                    "__test__",
                    sourceProviderContainer.getArtifactName());

            assertEquals(
                    "Extra artifact source provider for " + name + " value check",
                    "productFlavor:" + name,
                    sourceProviderContainer.getSourceProvider().getManifestFile().getPath());
        }

        // check the extra artifacts on the variants
        for (Variant variant : model.getVariants()) {
            String name = variant.getName();
            Collection<JavaArtifact> javaArtifacts = variant.getExtraJavaArtifacts();
            assertEquals(1, javaArtifacts.size());
            JavaArtifact javaArtifact = javaArtifacts.iterator().next();
            assertEquals("__test__", javaArtifact.getName());
            assertEquals("assemble:" + name, javaArtifact.getAssembleTaskName());
            assertEquals("compile:" + name, javaArtifact.getJavaCompileTaskName());
            assertEquals(new File("classesFolder:" + name), javaArtifact.getClassesFolder());

            SourceProvider variantSourceProvider = javaArtifact.getVariantSourceProvider();
            assertNotNull(variantSourceProvider);
            assertEquals("provider:" + name, variantSourceProvider.getManifestFile().getPath());

            Dependencies deps = javaArtifact.getDependencies();
            assertNotNull("java artifact deps null-check", deps);
            assertFalse(deps.getJavaLibraries().isEmpty());
        }
    }

    public void testCustomArtifact() throws Exception {
        // Load the custom model for the projects
        Map<String, ProjectData> map = getModelForMultiProject("customArtifactDep");

        ProjectData appModelData = map.get(":app");
        assertNotNull("Module app null-check", appModelData);
        AndroidProject model = appModelData.model;

        Collection<Variant> variants = model.getVariants();
        assertEquals("Variant count", 2, variants.size());

        Variant variant = getVariant(variants, "release");
        assertNotNull("release variant null-check", variant);

        AndroidArtifact mainInfo = variant.getMainArtifact();
        assertNotNull("Main Artifact null-check", mainInfo);

        Dependencies dependencies = mainInfo.getDependencies();
        assertNotNull("Dependencies null-check", dependencies);

        Collection<String> projects = dependencies.getProjects();
        assertNotNull("project dep list null-check", projects);
        assertTrue("project dep empty check", projects.isEmpty());

        Collection<JavaLibrary> javaLibraries = dependencies.getJavaLibraries();
        assertNotNull("jar dep list null-check", javaLibraries);
        assertEquals("jar dep count", 1, javaLibraries.size());
    }

    /**
     * Returns the SDK folder as built from the Android source tree.
     * @return the SDK
     */
    protected File getSdkDir() {
        String androidHome = System.getenv("ANDROID_HOME");
        if (androidHome != null) {
            File f = new File(androidHome);
            if (f.isDirectory()) {
                return f;
            }
        }

        throw new IllegalStateException("SDK not defined with ANDROID_HOME");
    }

    /**
     * Returns the root dir for the gradle plugin project
     */
    private File getRootDir() {
        CodeSource source = getClass().getProtectionDomain().getCodeSource();
        if (source != null) {
            URL location = source.getLocation();
            try {
                File dir = new File(location.toURI());
                assertTrue(dir.getPath(), dir.exists());

                File f;
                if (System.getenv("IDE_MODE") != null) {
                    f = dir.getParentFile().getParentFile().getParentFile();
                } else {
                    f = dir.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();
                    f = new File(f, "tools" + File.separator + "base" + File.separator + "build-system");
                }
                return f;
            } catch (URISyntaxException e) {
                fail(e.getLocalizedMessage());
            }
        }

        fail("Fail to get the tools/build folder");
        return null;
    }

    /**
     * Returns the root folder for the tests projects.
     */
    private File getTestDir() {
        File rootDir = getRootDir();
        return new File(rootDir, "tests");
    }

    @Nullable
    private static Variant getVariant(
            @NonNull Collection<Variant> items,
            @NonNull String name) {
        for (Variant item : items) {
            if (name.equals(item.getName())) {
                return item;
            }
        }

        return null;
    }

    @Nullable
    private static ProductFlavorContainer getProductFlavor(
            @NonNull Collection<ProductFlavorContainer> items,
            @NonNull String name) {
        for (ProductFlavorContainer item : items) {
            assertNotNull("ProductFlavorContainer list item null-check:" + name, item);
            assertNotNull("ProductFlavorContainer.getProductFlavor() list item null-check: " + name, item.getProductFlavor());
            assertNotNull("ProductFlavorContainer.getProductFlavor().getName() list item null-check: " + name, item.getProductFlavor().getName());
            if (name.equals(item.getProductFlavor().getName())) {
                return item;
            }
        }

        return null;
    }

    @Nullable
    private static ArtifactMetaData getArtifactMetaData(
            @NonNull Collection<ArtifactMetaData> items,
            @NonNull String name) {
        for (ArtifactMetaData item : items) {
            assertNotNull("ArtifactMetaData list item null-check:" + name, item);
            assertNotNull("ArtifactMetaData.getName() list item null-check: " + name, item.getName());
            if (name.equals(item.getName())) {
                return item;
            }
        }

        return null;
    }

    @Nullable
    private static AndroidArtifact getAndroidArtifact(
            @NonNull Collection<AndroidArtifact> items,
            @NonNull String name) {
        for (AndroidArtifact item : items) {
            assertNotNull("AndroidArtifact list item null-check:" + name, item);
            assertNotNull("AndroidArtifact.getName() list item null-check: " + name, item.getName());
            if (name.equals(item.getName())) {
                return item;
            }
        }

        return null;
    }

    @Nullable
    private static SigningConfig getSigningConfig(
            @NonNull Collection<SigningConfig> items,
            @NonNull String name) {
        for (SigningConfig item : items) {
            assertNotNull("SigningConfig list item null-check:" + name, item);
            assertNotNull("SigningConfig.getName() list item null-check: " + name, item.getName());
            if (name.equals(item.getName())) {
                return item;
            }
        }

        return null;
    }

    @Nullable
    private static SourceProviderContainer getSourceProviderContainer(
            @NonNull Collection<SourceProviderContainer> items,
            @NonNull String name) {
        for (SourceProviderContainer item : items) {
            assertNotNull("SourceProviderContainer list item null-check:" + name, item);
            assertNotNull("SourceProviderContainer.getName() list item null-check: " + name, item.getArtifactName());
            if (name.equals(item.getArtifactName())) {
                return item;
            }
        }

        return null;
    }

    private static final class ProductFlavorTester {
        @NonNull private final ProductFlavor productFlavor;
        @NonNull private final String name;

        private String applicationId = null;
        private int versionCode = -1;
        private String versionName = null;
        private ApiVersion minSdkVersion = null;
        private ApiVersion targetSdkVersion = null;
        private int renderscriptTargetApi = -1;
        private String testApplicationId = null;
        private String testInstrumentationRunner = null;
        private Boolean testHandleProfiling = null;
        private Boolean testFunctionalTest = null;

        ProductFlavorTester(@NonNull ProductFlavor productFlavor, @NonNull String name) {
            this.productFlavor = productFlavor;
            this.name = name;
        }

        ProductFlavorTester setApplicationId(String applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        ProductFlavorTester setVersionCode(int versionCode) {
            this.versionCode = versionCode;
            return this;
        }

        ProductFlavorTester setVersionName(String versionName) {
            this.versionName = versionName;
            return this;
        }

        ProductFlavorTester setMinSdkVersion(int minSdkVersion) {
            this.minSdkVersion = new DefaultApiVersion(minSdkVersion);
            return this;
        }

        ProductFlavorTester setTargetSdkVersion(int targetSdkVersion) {
            this.targetSdkVersion = new DefaultApiVersion(targetSdkVersion);
            return this;
        }

        ProductFlavorTester setRenderscriptTargetApi(int renderscriptTargetApi) {
            this.renderscriptTargetApi = renderscriptTargetApi;
            return this;
        }

        ProductFlavorTester setTestApplicationId(String testApplicationId) {
            this.testApplicationId = testApplicationId;
            return this;
        }

        ProductFlavorTester setTestInstrumentationRunner(String testInstrumentationRunner) {
            this.testInstrumentationRunner = testInstrumentationRunner;
            return this;
        }

        ProductFlavorTester setTestHandleProfiling(Boolean testHandleProfiling) {
            this.testHandleProfiling = testHandleProfiling;
            return this;
        }

        ProductFlavorTester setTestFunctionalTest(Boolean testFunctionalTest) {
            this.testFunctionalTest = testFunctionalTest;
            return this;
        }

        void test() {
            assertEquals(name + ":applicationId", applicationId, productFlavor.getApplicationId());
            assertEquals(name + ":VersionCode", versionCode, productFlavor.getVersionCode());
            assertEquals(name + ":VersionName", versionName, productFlavor.getVersionName());
            assertEquals(name + ":minSdkVersion", minSdkVersion, productFlavor.getMinSdkVersion());
            assertEquals(name + ":targetSdkVersion",
                    targetSdkVersion, productFlavor.getTargetSdkVersion());
            assertEquals(name + ":renderscriptTargetApi",
                    renderscriptTargetApi, productFlavor.getRenderscriptTargetApi());
            assertEquals(name + ":testApplicationId",
                    testApplicationId, productFlavor.getTestApplicationId());
            assertEquals(name + ":testInstrumentationRunner",
                    testInstrumentationRunner, productFlavor.getTestInstrumentationRunner());
            assertEquals(name + ":testHandleProfiling",
                    testHandleProfiling, productFlavor.getTestHandleProfiling());
            assertEquals(name + ":testFunctionalTest",
                    testFunctionalTest, productFlavor.getTestFunctionalTest());
        }
    }

    private static final class SourceProviderTester {

        @NonNull private final String projectName;
        @NonNull private final String configName;
        @NonNull private final SourceProvider sourceProvider;
        @NonNull private final File projectDir;
        private String javaDir;
        private String resourcesDir;
        private String manifestFile;
        private String resDir;
        private String assetsDir;
        private String aidlDir;
        private String renderscriptDir;
        private String jniDir;

        SourceProviderTester(@NonNull String projectName, @NonNull File projectDir,
                             @NonNull String configName, @NonNull SourceProvider sourceProvider) {
            this.projectName = projectName;
            this.projectDir = projectDir;
            this.configName = configName;
            this.sourceProvider = sourceProvider;
            // configure tester with default relative paths
            setJavaDir("src/" + configName + "/java");
            setResourcesDir("src/" + configName + "/resources");
            setManifestFile("src/" + configName + "/AndroidManifest.xml");
            setResDir("src/" + configName + "/res");
            setAssetsDir("src/" + configName + "/assets");
            setAidlDir("src/" + configName + "/aidl");
            setRenderscriptDir("src/" + configName + "/rs");
            setJniDir("src/" + configName + "/jni");
        }

        SourceProviderTester setJavaDir(String javaDir) {
            this.javaDir = javaDir;
            return this;
        }

        SourceProviderTester setResourcesDir(String resourcesDir) {
            this.resourcesDir = resourcesDir;
            return this;
        }

        SourceProviderTester setManifestFile(String manifestFile) {
            this.manifestFile = manifestFile;
            return this;
        }

        SourceProviderTester setResDir(String resDir) {
            this.resDir = resDir;
            return this;
        }

        SourceProviderTester setAssetsDir(String assetsDir) {
            this.assetsDir = assetsDir;
            return this;
        }

        SourceProviderTester setAidlDir(String aidlDir) {
            this.aidlDir = aidlDir;
            return this;
        }

        SourceProviderTester setRenderscriptDir(String renderscriptDir) {
            this.renderscriptDir = renderscriptDir;
            return this;
        }

        SourceProviderTester setJniDir(String jniDir) {
            this.jniDir = jniDir;
            return this;
        }

        void test() {
            testSinglePathCollection("java", javaDir, sourceProvider.getJavaDirectories());
            testSinglePathCollection("resources", resourcesDir, sourceProvider.getResourcesDirectories());
            testSinglePathCollection("res", resDir, sourceProvider.getResDirectories());
            testSinglePathCollection("assets", assetsDir, sourceProvider.getAssetsDirectories());
            testSinglePathCollection("aidl", aidlDir, sourceProvider.getAidlDirectories());
            testSinglePathCollection("rs", renderscriptDir, sourceProvider.getRenderscriptDirectories());
            testSinglePathCollection("jni", jniDir, sourceProvider.getJniDirectories());

            assertEquals("AndroidManifest",
                    new File(projectDir, manifestFile).getAbsolutePath(),
                    sourceProvider.getManifestFile().getAbsolutePath());
        }

        private void testSinglePathCollection(
                @NonNull String setName,
                @NonNull String referencePath,
                @NonNull Collection<File> pathSet) {
            assertEquals(1, pathSet.size());
            assertEquals(projectName + ": " + configName + "/" + setName,
                    new File(projectDir, referencePath).getAbsolutePath(),
                    pathSet.iterator().next().getAbsolutePath());
        }

    }

    private static final class VariantTester {

        private final Variant variant;
        private final File projectDir;
        private final String outputFileName;

        VariantTester(Variant variant, File projectDir, String outputFileName) {
            this.variant = variant;
            this.projectDir = projectDir;
            this.outputFileName = outputFileName;
        }

        void test() {
            AndroidArtifact artifact = variant.getMainArtifact();
            assertNotNull("Main Artifact null-check", artifact);

            String variantName = variant.getName();
            File build = new File(projectDir,  "build");
            File apk = new File(build, "outputs/apk/" + outputFileName);
            assertEquals(variantName + " output", apk, artifact.getOutputFile());

            Collection<File> sourceFolders = artifact.getGeneratedSourceFolders();
            assertEquals("Gen src Folder count", 4, sourceFolders.size());

            File manifest = artifact.getGeneratedManifest();
            assertNotNull(manifest);
        }
    }

    private static final class SigningConfigTester {

        public static final String DEFAULT_PASSWORD = "android";
        public static final String DEFAULT_ALIAS = "AndroidDebugKey";

        @NonNull private final SigningConfig signingConfig;
        @NonNull private final String name;
        private File storeFile = null;
        private String storePassword = null;
        private String keyAlias = null;
        private String keyPassword = null;
        private String storeType = KeyStore.getDefaultType();
        private boolean isSigningReady = false;

        SigningConfigTester(@NonNull SigningConfig signingConfig, @NonNull String name,
                            boolean isDebug) throws AndroidLocation.AndroidLocationException {
            assertNotNull(String.format("SigningConfig '%s' null-check", name), signingConfig);
            this.signingConfig = signingConfig;
            this.name = name;

            if (isDebug) {
                storeFile =  new File(KeystoreHelper.defaultDebugKeystoreLocation());
                storePassword = DEFAULT_PASSWORD;
                keyAlias = DEFAULT_ALIAS;
                keyPassword = DEFAULT_PASSWORD;
                isSigningReady = true;
            }
        }

        SigningConfigTester setStoreFile(File storeFile) {
            this.storeFile = storeFile;
            return this;
        }

        SigningConfigTester setStorePassword(String storePassword) {
            this.storePassword = storePassword;
            return this;
        }

        SigningConfigTester setKeyAlias(String keyAlias) {
            this.keyAlias = keyAlias;
            return this;
        }

        SigningConfigTester setKeyPassword(String keyPassword) {
            this.keyPassword = keyPassword;
            return this;
        }

        SigningConfigTester setStoreType(String storeType) {
            this.storeType = storeType;
            return this;
        }

        SigningConfigTester setSigningReady(boolean isSigningReady) {
            this.isSigningReady = isSigningReady;
            return this;
        }

        void test() {
            assertEquals("SigningConfig name", name, signingConfig.getName());

            assertEquals(String.format("SigningConfig '%s' storeFile", name),
                    storeFile, signingConfig.getStoreFile());

            assertEquals(String.format("SigningConfig '%s' storePassword", name),
                    storePassword, signingConfig.getStorePassword());

            String scAlias = signingConfig.getKeyAlias();
            assertEquals(String.format("SigningConfig '%s' keyAlias", name),
                    keyAlias != null ? keyAlias.toLowerCase(Locale.getDefault()) : keyAlias,
                    scAlias != null ? scAlias.toLowerCase(Locale.getDefault()) : scAlias);

            assertEquals(String.format("SigningConfig '%s' keyPassword", name),
                    keyPassword, signingConfig.getKeyPassword());

            assertEquals(String.format("SigningConfig '%s' storeType", name),
                    storeType, signingConfig.getStoreType());

            assertEquals(String.format("SigningConfig '%s' isSigningReady", name),
                    isSigningReady, signingConfig.isSigningReady());
        }
    }
}
