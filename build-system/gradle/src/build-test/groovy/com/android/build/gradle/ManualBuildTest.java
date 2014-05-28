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

package com.android.build.gradle;

import static com.android.build.gradle.BasePlugin.FD_INTERMEDIATES;
import static com.android.build.gradle.BasePlugin.FD_OUTPUTS;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import javax.imageio.ImageIO;

/**
 * Some manual tests for building projects.
 *
 * This requires an SDK, found through the ANDROID_HOME environment variable or present in the
 * Android Source tree under out/host/<platform>/sdk/... (result of 'make sdk')
 */
public class ManualBuildTest extends BuildTest {

    private static final int RED = 0xFFFF0000;
    private static final int GREEN = 0xFF00FF00;
    private static final int BLUE = 0xFF0000FF;

    public void testOverlay1Content() throws Exception {
        File project = buildProject("overlay1", BasePlugin.GRADLE_MIN_VERSION);
        File drawableOutput = new File(project, "build/" + FD_INTERMEDIATES + "/res/debug/drawable");

        checkImageColor(drawableOutput, "no_overlay.png", GREEN);
        checkImageColor(drawableOutput, "type_overlay.png", GREEN);
    }

    public void testOverlay2Content() throws Exception {
        File project = buildProject("overlay2", BasePlugin.GRADLE_MIN_VERSION);
        File drawableOutput = new File(project, "build/" + FD_INTERMEDIATES + "/res/one/debug/drawable");

        checkImageColor(drawableOutput, "no_overlay.png", GREEN);
        checkImageColor(drawableOutput, "type_overlay.png", GREEN);
        checkImageColor(drawableOutput, "flavor_overlay.png", GREEN);
        checkImageColor(drawableOutput, "type_flavor_overlay.png", GREEN);
        checkImageColor(drawableOutput, "variant_type_flavor_overlay.png", GREEN);
    }

    public void testOverlay3Content() throws Exception {
        File project = buildProject("overlay3", BasePlugin.GRADLE_MIN_VERSION);
        File drawableOutput = new File(project, "build/" + FD_INTERMEDIATES + "/res/freebeta/debug/drawable");

        checkImageColor(drawableOutput, "no_overlay.png", GREEN);
        checkImageColor(drawableOutput, "debug_overlay.png", GREEN);
        checkImageColor(drawableOutput, "beta_overlay.png", GREEN);
        checkImageColor(drawableOutput, "free_overlay.png", GREEN);
        checkImageColor(drawableOutput, "free_beta_overlay.png", GREEN);
        checkImageColor(drawableOutput, "free_beta_debug_overlay.png", GREEN);
        checkImageColor(drawableOutput, "free_normal_overlay.png", RED);

        drawableOutput = new File(project, "build/" + FD_INTERMEDIATES + "/res/freenormal/debug/drawable");

        checkImageColor(drawableOutput, "no_overlay.png", GREEN);
        checkImageColor(drawableOutput, "debug_overlay.png", GREEN);
        checkImageColor(drawableOutput, "beta_overlay.png", RED);
        checkImageColor(drawableOutput, "free_overlay.png", GREEN);
        checkImageColor(drawableOutput, "free_beta_overlay.png", RED);
        checkImageColor(drawableOutput, "free_beta_debug_overlay.png", RED);
        checkImageColor(drawableOutput, "free_normal_overlay.png", GREEN);

        drawableOutput = new File(project, "build/" + FD_INTERMEDIATES + "/res/paidbeta/debug/drawable");

        checkImageColor(drawableOutput, "no_overlay.png", GREEN);
        checkImageColor(drawableOutput, "debug_overlay.png", GREEN);
        checkImageColor(drawableOutput, "beta_overlay.png", GREEN);
        checkImageColor(drawableOutput, "free_overlay.png", RED);
        checkImageColor(drawableOutput, "free_beta_overlay.png", RED);
        checkImageColor(drawableOutput, "free_beta_debug_overlay.png", RED);
        checkImageColor(drawableOutput, "free_normal_overlay.png", RED);
    }

    public void testRepo() {
        File repo = new File(testDir, "repo");

        try {
            runGradleTasks(sdkDir, ndkDir, BasePlugin.GRADLE_MIN_VERSION,
                    new File(repo, "util"), "clean", "uploadArchives");
            runGradleTasks(sdkDir, ndkDir, BasePlugin.GRADLE_MIN_VERSION,
                    new File(repo, "baseLibrary"), "clean", "uploadArchives");
            runGradleTasks(sdkDir, ndkDir, BasePlugin.GRADLE_MIN_VERSION,
                    new File(repo, "library"), "clean", "uploadArchives");
            runGradleTasks(sdkDir, ndkDir, BasePlugin.GRADLE_MIN_VERSION,
                    new File(repo, "app"), "clean", "assemble");
        } finally {
            // clean up the test repository.
            File testRepo = new File(repo, "testrepo");
            deleteFolder(testRepo);
        }
    }

    public void testLibsManifestMerging() throws Exception {
        File project = new File(testDir, "libsTest");
        File fileOutput = new File(project, "libapp/build/" + FD_INTERMEDIATES + "/bundles/release/AndroidManifest.xml");

        runGradleTasks(sdkDir, ndkDir, BasePlugin.GRADLE_MIN_VERSION,
                project, "clean", "build");
        assertTrue(fileOutput.exists());
    }

    // test whether a library project has its fields ProGuarded
    public void testLibProguard() throws Exception {
        File project = new File(testDir, "libProguard");
        File fileOutput = new File(project, "build/" + FD_OUTPUTS + "/proguard/release");

        runGradleTasks(sdkDir, ndkDir, BasePlugin.GRADLE_MIN_VERSION,
          project, "clean", "build");
        checkFile(fileOutput, "mapping.txt", new String[]{"int proguardInt -> a"});

    }

    // test whether proguard.txt has been correctly merged
    public void testLibProguardConsumerFile() throws Exception {
        File project = new File(testDir, "libProguardConsumerFiles");
        File debugFileOutput = new File(project, "build/" + FD_INTERMEDIATES + "/bundles/debug");
        File releaseFileOutput = new File(project, "build/" + FD_INTERMEDIATES + "/bundles/release");

        runGradleTasks(sdkDir, ndkDir, BasePlugin.GRADLE_MIN_VERSION,
            project, "clean", "build");
        checkFile(debugFileOutput, "proguard.txt", new String[]{"A"});
        checkFile(releaseFileOutput, "proguard.txt", new String[]{"A", "B", "C"});
    }

    public void testAnnotations() throws Exception {
        File project = new File(testDir, "extractAnnotations");
        File debugFileOutput = new File(project, "build/" + FD_INTERMEDIATES + "/bundles/debug");

        runGradleTasks(sdkDir, ndkDir, BasePlugin.GRADLE_MIN_VERSION,
                project, "clean", "extractDebugAnnotations");
        File file = new File(debugFileOutput, "annotations.zip");

        Map<String,String> map = Maps.newHashMap();
        //noinspection SpellCheckingInspection
        map.put("com/android/tests/extractannotations/annotations.xml", ""
                + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<root>\n"
                + "  <item name=\"com.android.tests.extractannotations.ExtractTest ExtractTest(int, java.lang.String) 0\">\n"
                + "    <annotation name=\"android.support.annotation.IdRes\" />\n"
                + "  </item>\n"
                // This item should be removed when I start supporting @hide
                + "  <item name=\"com.android.tests.extractannotations.ExtractTest int getHiddenMethod()\">\n"
                + "    <annotation name=\"android.support.annotation.IdRes\" />\n"
                + "  </item>\n"
                // This item should be removed when I start supporting @hide
                + "  <item name=\"com.android.tests.extractannotations.ExtractTest int getPrivate()\">\n"
                + "    <annotation name=\"android.support.annotation.IdRes\" />\n"
                + "  </item>\n"
                + "  <item name=\"com.android.tests.extractannotations.ExtractTest int getVisibility()\">\n"
                + "    <annotation name=\"android.support.annotation.IntDef\">\n"
                + "      <val name=\"value\" val=\"{com.android.tests.extractannotations.ExtractTest.VISIBLE, com.android.tests.extractannotations.ExtractTest.INVISIBLE, com.android.tests.extractannotations.ExtractTest.GONE, 5, 17, com.android.tests.extractannotations.Constants.CONSTANT_1}\" />\n"
                + "    </annotation>\n"
                + "  </item>\n"
                + "  <item name=\"com.android.tests.extractannotations.ExtractTest int resourceTypeMethod(int, int)\">\n"
                + "    <annotation name=\"android.support.annotation.StringRes\" />\n"
                + "    <annotation name=\"android.support.annotation.IdRes\" />\n"
                + "  </item>\n"
                + "  <item name=\"com.android.tests.extractannotations.ExtractTest int resourceTypeMethod(int, int) 0\">\n"
                + "    <annotation name=\"android.support.annotation.DrawableRes\" />\n"
                + "  </item>\n"
                + "  <item name=\"com.android.tests.extractannotations.ExtractTest int resourceTypeMethod(int, int) 1\">\n"
                + "    <annotation name=\"android.support.annotation.IdRes\" />\n"
                + "    <annotation name=\"android.support.annotation.ColorRes\" />\n"
                + "  </item>\n"
                // This item should be removed when I start supporting @hide
                + "  <item name=\"com.android.tests.extractannotations.ExtractTest java.lang.Object getPackagePrivate()\">\n"
                + "    <annotation name=\"android.support.annotation.IdRes\" />\n"
                + "  </item>\n"
                + "  <item name=\"com.android.tests.extractannotations.ExtractTest java.lang.String getStringMode(int)\">\n"
                + "    <annotation name=\"android.support.annotation.StringDef\">\n"
                + "      <val name=\"value\" val=\"{com.android.tests.extractannotations.ExtractTest.STRING_1, com.android.tests.extractannotations.ExtractTest.STRING_2, &quot;literalValue&quot;, &quot;concatenated&quot;}\" />\n"
                + "    </annotation>\n"
                + "  </item>\n"
                + "  <item name=\"com.android.tests.extractannotations.ExtractTest java.lang.String getStringMode(int) 0\">\n"
                + "    <annotation name=\"android.support.annotation.IntDef\">\n"
                + "      <val name=\"value\" val=\"{com.android.tests.extractannotations.ExtractTest.VISIBLE, com.android.tests.extractannotations.ExtractTest.INVISIBLE, com.android.tests.extractannotations.ExtractTest.GONE, 5, 17, com.android.tests.extractannotations.Constants.CONSTANT_1}\" />\n"
                + "    </annotation>\n"
                + "  </item>\n"
                + "  <item name=\"com.android.tests.extractannotations.ExtractTest void checkForeignTypeDef(int) 0\">\n"
                + "    <annotation name=\"android.support.annotation.IntDef\">\n"
                + "      <val name=\"value\" val=\"{com.android.tests.extractannotations.Constants.CONSTANT_1, com.android.tests.extractannotations.Constants.CONSTANT_2}\" />\n"
                + "      <val name=\"flag\" val=\"true\" />\n"
                + "    </annotation>\n"
                + "  </item>\n"
                + "  <item name=\"com.android.tests.extractannotations.ExtractTest void resourceTypeMethodWithTypeArgs(java.util.Map&lt;java.lang.String,? extends java.lang.Number&gt;, T, int) 0\">\n"
                + "    <annotation name=\"android.support.annotation.StringRes\" />\n"
                + "  </item>\n"
                + "  <item name=\"com.android.tests.extractannotations.ExtractTest void resourceTypeMethodWithTypeArgs(java.util.Map&lt;java.lang.String,? extends java.lang.Number&gt;, T, int) 1\">\n"
                + "    <annotation name=\"android.support.annotation.DrawableRes\" />\n"
                + "  </item>\n"
                + "  <item name=\"com.android.tests.extractannotations.ExtractTest void resourceTypeMethodWithTypeArgs(java.util.Map&lt;java.lang.String,? extends java.lang.Number&gt;, T, int) 2\">\n"
                + "    <annotation name=\"android.support.annotation.IdRes\" />\n"
                + "  </item>\n"
                + "  <item name=\"com.android.tests.extractannotations.ExtractTest void testMask(int) 0\">\n"
                + "    <annotation name=\"android.support.annotation.IntDef\">\n"
                + "      <val name=\"value\" val=\"{0, com.android.tests.extractannotations.Constants.FLAG_VALUE_1, com.android.tests.extractannotations.Constants.FLAG_VALUE_2}\" />\n"
                + "      <val name=\"flag\" val=\"true\" />\n"
                + "    </annotation>\n"
                + "  </item>\n"
                + "  <item name=\"com.android.tests.extractannotations.ExtractTest void testNonMask(int) 0\">\n"
                + "    <annotation name=\"android.support.annotation.IntDef\">\n"
                + "      <val name=\"value\" val=\"{0, com.android.tests.extractannotations.Constants.CONSTANT_1, com.android.tests.extractannotations.Constants.CONSTANT_3}\" />\n"
                + "    </annotation>\n"
                + "  </item>\n"
                // This should be hidden when we start filtering out hidden classes on @hide!
                + "  <item name=\"com.android.tests.extractannotations.ExtractTest.HiddenClass int getHiddenMember()\">\n"
                + "    <annotation name=\"android.support.annotation.IdRes\" />\n"
                + "  </item>\n"
                + "</root>");

        checkJar(file, map);
    }

    public void test3rdPartyTests() throws Exception {
        // custom because we want to run deviceCheck even without devices, since we use
        // a fake DeviceProvider that doesn't use a device, but only record the calls made
        // to the DeviceProvider and the DeviceConnector.
        runGradleTasks(sdkDir, ndkDir, BasePlugin.GRADLE_MIN_VERSION,
                new File(testDir, "3rdPartyTests"), "clean", "deviceCheck");
    }

    private static void checkImageColor(File folder, String fileName, int expectedColor)
            throws IOException {
        File f = new File(folder, fileName);
        assertTrue("File '" + f.getAbsolutePath() + "' does not exist.", f.isFile());

        BufferedImage image = ImageIO.read(f);
        int rgb = image.getRGB(0, 0);
        assertEquals(String.format("Expected: 0x%08X, actual: 0x%08X for file %s",
                expectedColor, rgb, f),
                expectedColor, rgb);
    }

    private static void checkFile(File folder, String fileName, String[] expectedContents)
            throws IOException {
        File f = new File(folder, fileName);
        assertTrue("File '" + f.getAbsolutePath() + "' does not exist.", f.isFile());

        String contents = Files.toString(f, Charsets.UTF_8);
        for (String expectedContent : expectedContents) {
            assertTrue("File '" + f.getAbsolutePath() + "' does not contain: " + expectedContent,
                contents.contains(expectedContent));
        }
    }

    private static void checkJar(File jar, Map<String, String> pathToContents)
            throws IOException {
        assertTrue("File '" + jar.getPath() + "' does not exist.", jar.isFile());
        JarInputStream zis = null;
        FileInputStream fis;
        Set<String> notFound = Sets.newHashSet();
        notFound.addAll(pathToContents.keySet());
        fis = new FileInputStream(jar);
        try {
            zis = new JarInputStream(fis);

            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                String name = entry.getName();
                String expected = pathToContents.get(name);
                if (expected != null) {
                    notFound.remove(name);
                    if (!entry.isDirectory()) {
                        byte[] bytes = ByteStreams.toByteArray(zis);
                        if (bytes != null) {
                            String contents = new String(bytes, Charsets.UTF_8).trim();
                            assertEquals("Contents in " + name + " did not match",
                                    expected, contents);
                        }
                    }
                }
                entry = zis.getNextEntry();
            }
        } finally {
            fis.close();
            if (zis != null) {
                zis.close();
            }
        }

        assertTrue("Did not find the following paths in the " + jar.getPath() + " file: " +
            notFound, notFound.isEmpty());
    }
}
