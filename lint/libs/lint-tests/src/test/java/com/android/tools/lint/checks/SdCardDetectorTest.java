/*
 * Copyright (C) 2011 The Android Open Source Project
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

@SuppressWarnings({"javadoc", "ClassNameDiffersFromFileName"})
public class SdCardDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new SdCardDetector();
    }

    public void test() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "src/test/pkg/SdCardTest.java:13: Warning: Do not hardcode \"/sdcard/\"; use Environment.getExternalStorageDirectory().getPath() instead [SdCardPath]\n"
                + " private static final String SDCARD_TEST_HTML = \"/sdcard/test.html\";\n"
                + "                                                ~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/SdCardTest.java:14: Warning: Do not hardcode \"/sdcard/\"; use Environment.getExternalStorageDirectory().getPath() instead [SdCardPath]\n"
                + " public static final String SDCARD_ROOT = \"/sdcard\";\n"
                + "                                          ~~~~~~~~~\n"
                + "src/test/pkg/SdCardTest.java:15: Warning: Do not hardcode \"/sdcard/\"; use Environment.getExternalStorageDirectory().getPath() instead [SdCardPath]\n"
                + " public static final String PACKAGES_PATH = \"/sdcard/o/packages/\";\n"
                + "                                            ~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/SdCardTest.java:16: Warning: Do not hardcode \"/sdcard/\"; use Environment.getExternalStorageDirectory().getPath() instead [SdCardPath]\n"
                + " File deviceDir = new File(\"/sdcard/vr\");\n"
                + "                           ~~~~~~~~~~~~\n"
                + "src/test/pkg/SdCardTest.java:20: Warning: Do not hardcode \"/sdcard/\"; use Environment.getExternalStorageDirectory().getPath() instead [SdCardPath]\n"
                + "   android.os.Debug.startMethodTracing(\"/sdcard/launcher\");\n"
                + "                                       ~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/SdCardTest.java:22: Warning: Do not hardcode \"/sdcard/\"; use Environment.getExternalStorageDirectory().getPath() instead [SdCardPath]\n"
                + "  if (new File(\"/sdcard\").exists()) {\n"
                + "               ~~~~~~~~~\n"
                + "src/test/pkg/SdCardTest.java:24: Warning: Do not hardcode \"/sdcard/\"; use Environment.getExternalStorageDirectory().getPath() instead [SdCardPath]\n"
                + "  String FilePath = \"/sdcard/\" + new File(\"test\");\n"
                + "                    ~~~~~~~~~~\n"
                + "src/test/pkg/SdCardTest.java:29: Warning: Do not hardcode \"/sdcard/\"; use Environment.getExternalStorageDirectory().getPath() instead [SdCardPath]\n"
                + "  intent.setDataAndType(Uri.parse(\"file://sdcard/foo.json\"), \"application/bar-json\");\n"
                + "                                  ~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/SdCardTest.java:30: Warning: Do not hardcode \"/sdcard/\"; use Environment.getExternalStorageDirectory().getPath() instead [SdCardPath]\n"
                + "  intent.putExtra(\"path-filter\", \"/sdcard(/.+)*\");\n"
                + "                                 ~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/SdCardTest.java:31: Warning: Do not hardcode \"/sdcard/\"; use Environment.getExternalStorageDirectory().getPath() instead [SdCardPath]\n"
                + "  intent.putExtra(\"start-dir\", \"/sdcard\");\n"
                + "                               ~~~~~~~~~\n"
                + "src/test/pkg/SdCardTest.java:32: Warning: Do not hardcode \"/data/\"; use Context.getFilesDir().getPath() instead [SdCardPath]\n"
                + "  String mypath = \"/data/data/foo\";\n"
                + "                  ~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/SdCardTest.java:33: Warning: Do not hardcode \"/data/\"; use Context.getFilesDir().getPath() instead [SdCardPath]\n"
                + "  String base = \"/data/data/foo.bar/test-profiling\";\n"
                + "                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                + "src/test/pkg/SdCardTest.java:34: Warning: Do not hardcode \"/sdcard/\"; use Environment.getExternalStorageDirectory().getPath() instead [SdCardPath]\n"
                + "  String s = \"file://sdcard/foo\";\n"
                + "             ~~~~~~~~~~~~~~~~~~~\n"
                + "0 errors, 13 warnings\n",

            lintProject(java(""
                            + "package test.pkg;\n"
                            + "\n"
                            + "import java.io.File;\n"
                            + "\n"
                            + "import android.content.Intent;\n"
                            + "import android.net.Uri;\n"
                            + "\n"
                            + "/**\n"
                            + " * Ignore comments - create(\"/sdcard/foo\")\n"
                            + " */\n"
                            + "public class SdCardTest {\n"
                            + "\tprivate static final boolean PROFILE_STARTUP = true;\n"
                            + "\tprivate static final String SDCARD_TEST_HTML = \"/sdcard/test.html\";\n"
                            + "\tpublic static final String SDCARD_ROOT = \"/sdcard\";\n"
                            + "\tpublic static final String PACKAGES_PATH = \"/sdcard/o/packages/\";\n"
                            + "\tFile deviceDir = new File(\"/sdcard/vr\");\n"
                            + "\n"
                            + "\tpublic SdCardTest() {\n"
                            + "\t\tif (PROFILE_STARTUP) {\n"
                            + "\t\t\tandroid.os.Debug.startMethodTracing(\"/sdcard/launcher\");\n"
                            + "\t\t}\n"
                            + "\t\tif (new File(\"/sdcard\").exists()) {\n"
                            + "\t\t}\n"
                            + "\t\tString FilePath = \"/sdcard/\" + new File(\"test\");\n"
                            + "\t\tSystem.setProperty(\"foo.bar\", \"file://sdcard\");\n"
                            + "\n"
                            + "\n"
                            + "\t\tIntent intent = new Intent(Intent.ACTION_PICK);\n"
                            + "\t\tintent.setDataAndType(Uri.parse(\"file://sdcard/foo.json\"), \"application/bar-json\");\n"
                            + "\t\tintent.putExtra(\"path-filter\", \"/sdcard(/.+)*\");\n"
                            + "\t\tintent.putExtra(\"start-dir\", \"/sdcard\");\n"
                            + "\t\tString mypath = \"/data/data/foo\";\n"
                            + "\t\tString base = \"/data/data/foo.bar/test-profiling\";\n"
                            + "\t\tString s = \"file://sdcard/foo\";\n"
                            + "\t}\n"
                            + "}\n")));
    }

    public void testSuppress() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "src/test/pkg/SuppressTest5.java:40: Warning: Do not hardcode \"/sdcard/\"; use Environment.getExternalStorageDirectory().getPath() instead [SdCardPath]\n"
                + "  String notAnnotated = \"/sdcard/mypath\";\n"
                + "                        ~~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n",

            // File with lots of /sdcard references, but with @SuppressLint warnings
            // on fields, methods, variable declarations etc
            lintProject(java(""
                            + "package test.pkg;\n"
                            + "\n"
                            + "import android.annotation.SuppressLint;\n"
                            + "\n"
                            + "@SuppressWarnings(\"unused\")\n"
                            + "public class SuppressTest5 {\n"
                            + "\tprivate String suppressVariable() {\n"
                            + "\t\t@SuppressLint(\"SdCardPath\")\n"
                            + "\t\tString string = \"/sdcard/mypath1\";\n"
                            + "\t\treturn string;\n"
                            + "\t}\n"
                            + "\n"
                            + "\t@SuppressLint(\"SdCardPath\")\n"
                            + "\tprivate String suppressMethod() {\n"
                            + "\t\tString string = \"/sdcard/mypath2\";\n"
                            + "\t\treturn string;\n"
                            + "\t}\n"
                            + "\n"
                            + "\t@SuppressLint(\"SdCardPath\")\n"
                            + "\tprivate static class SuppressClass {\n"
                            + "\t\tprivate String suppressMethod() {\n"
                            + "\t\t\tString string = \"/sdcard/mypath3\";\n"
                            + "\t\t\treturn string;\n"
                            + "\t\t}\n"
                            + "\t}\n"
                            + "\n"
                            + "\tprivate String suppressAll() {\n"
                            + "\t\t@SuppressLint(\"all\")\n"
                            + "\t\tString string = \"/sdcard/mypath4\";\n"
                            + "\t\treturn string;\n"
                            + "\t}\n"
                            + "\n"
                            + "\tprivate String suppressCombination() {\n"
                            + "\t\t@SuppressLint({\"foo1\", \"foo2\", \"SdCardPath\"})\n"
                            + "\t\tString string = \"/sdcard/mypath5\";\n"
                            + "\n"
                            + "\t\t// This is NOT annotated and *should* generate\n"
                            + "\t\t// a warning (here to make sure we don't just\n"
                            + "\t\t// suppress everything when we see an annotation\n"
                            + "\t\tString notAnnotated = \"/sdcard/mypath\";\n"
                            + "\n"
                            + "\t\treturn string;\n"
                            + "\t}\n"
                            + "\n"
                            + "\tprivate String suppressWarnings() {\n"
                            + "\t\t@SuppressWarnings(\"all\")\n"
                            + "\t\tString string = \"/sdcard/mypath6\";\n"
                            + "\n"
                            + "\t\t@SuppressWarnings(\"SdCardPath\")\n"
                            + "\t\tString string2 = \"/sdcard/mypath7\";\n"
                            + "\n"
                            + "\t\t@SuppressWarnings(\"AndroidLintSdCardPath\")\n"
                            + "\t\tString string3 = \"/sdcard/mypath9\";\n"
                            + "\n"
                            + "\t\t//noinspection AndroidLintSdCardPath\n"
                            + "\t\tString string4 = \"/sdcard/mypath9\";\n"
                            + "\n"
                            + "\t\treturn string;\n"
                            + "\t}\n"
                            + "\n"
                            + "\t@SuppressLint(\"SdCardPath\")\n"
                            + "\tprivate String supressField = \"/sdcard/mypath8\";\n"
                            + "}\n")));
    }

    public void testUtf8Bom() throws Exception {
        //noinspection all // Sample code
        assertEquals(""
                + "src/test/pkg/Utf8BomTest.java:4: Warning: Do not hardcode \"/sdcard/\"; use Environment.getExternalStorageDirectory().getPath() instead [SdCardPath]\n"
                + "    String s = \"/sdcard/mydir\";\n"
                + "               ~~~~~~~~~~~~~~~\n"
                + "0 errors, 1 warnings\n",

            lintProject(java(""
                            + "\ufeffpackage test.pkg;\n"
                            + "\n"
                            + "public class Utf8BomTest {\n"
                            + "    String s = \"/sdcard/mydir\";\n"
                            + "}\n")));
    }

    public void testSuppressInAnnotation() throws Exception {
        assertEquals("No warnings.",
                lintProject(java("src/test/pkg/MyInterface.java", ""
                        + "package test.pkg;\n"
                        + "import android.annotation.SuppressLint;\n"
                        + "public @interface MyInterface {\n"
                        + "    @SuppressLint(\"SdCardPath\")\n"
                        + "    String engineer() default \"/sdcard/this/is/wrong\";\n"
                        + "}\n")));
    }

    public void testMatchInTestIfEnabled() throws Exception {
        //noinspection all // Sample code
        lint().files(
                java("test/test/pkg/MyTest.java", ""
                        + "package test.pkg;\n"
                        + "\n"
                        + "public class MyTest {\n"
                        + "    String s = \"/sdcard/mydir\";\n"
                        + "}\n"))
                .configureOptions(flags -> flags.setCheckTestSources(true))
                .run()
                .expect(""
                        + "test/test/pkg/MyTest.java:4: Warning: Do not hardcode \"/sdcard/\"; use Environment.getExternalStorageDirectory().getPath() instead [SdCardPath]\n"
                        + "    String s = \"/sdcard/mydir\";\n"
                        + "               ~~~~~~~~~~~~~~~\n"
                        + "0 errors, 1 warnings\n");
    }

    public void testNothingInTests() throws Exception {
        //noinspection all // Sample code
        lint().files(
                java("test/test/pkg/MyTest.java", ""
                        + "package test.pkg;\n"
                        + "\n"
                        + "public class MyTest {\n"
                        + "    String s = \"/sdcard/mydir\";\n"
                        + "}\n"))
                .run()
                .expectClean();
    }
}
