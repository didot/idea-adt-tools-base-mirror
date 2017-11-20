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

package com.android.tools.lint.checks;

import static com.android.tools.lint.checks.AnnotationDetectorTest.SUPPORT_ANNOTATIONS_JAR_BASE64_GZIP;

import com.android.tools.lint.detector.api.Detector;

/**
 * Unit tests for {@link VersionChecks}. This is written as a lint detector
 */
public class VersionChecksTest extends AbstractCheckTest {

    public void testConditionalApi0() {
        // See https://code.google.com/p/android/issues/detail?id=137195
        //noinspection all // Sample code

        lint().files(
                classpath(),
                manifest().minSdk(14),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.animation.RectEvaluator;\n"
                        + "import android.graphics.Rect;\n"
                        + "import android.os.Build;\n"
                        + "\n"
                        + "@SuppressWarnings(\"unused\")\n"
                        + "public class ConditionalApiTest {\n"
                        + "    private void test(Rect rect) {\n"
                        + "        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {\n"
                        + "            new RectEvaluator(rect); // OK\n"
                        + "        }\n"
                        + "        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {\n"
                        + "            if (rect != null) {\n"
                        + "                new RectEvaluator(rect); // OK\n"
                        + "            }\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void test2(Rect rect) {\n"
                        + "        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {\n"
                        + "            new RectEvaluator(rect); // OK\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void test3(Rect rect) {\n"
                        + "        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {\n"
                        + "            new RectEvaluator(); // ERROR\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void test4(Rect rect) {\n"
                        + "        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {\n"
                        + "            System.out.println(\"Something\");\n"
                        + "            new RectEvaluator(rect); // OK\n"
                        + "        } else {\n"
                        + "            new RectEvaluator(rect); // ERROR\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void test5(Rect rect) {\n"
                        + "        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {\n"
                        + "            new RectEvaluator(rect); // ERROR\n"
                        + "        } else {\n"
                        + "            new RectEvaluator(rect); // ERROR\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"),
                base64gzip("bin/classes/test/pkg/ConditionalApiTest.class", ""
                        + "H4sIAAAAAAAAAIWU208TQRTGv2lLty0LdMv9UhCEUqiwKngLxkQBk8YKhiUl"
                        + "PpGh3ZSB7W6z3ZLw5/DkszxIoomPPvhHGc8sy9Jy0TaZM3POzPf75pL9/ef7"
                        + "TwAreJVCNyaTmMIDBTMK5lKIIScbSs4jn8BCCosoKHikYIkh/lrYwnvDEM0v"
                        + "lBli607VZOgrCdvcatUPTHeXH1iUyZScCrfK3BVyHCRj3qFoMmRLntn09MZx"
                        + "TV937KrwhGNz621D7FJ6TU6jyDCWL3G76jqiqtdc3jgUlaa+Y1a8NR/sUo9h"
                        + "+J4pDD2GxyvHH3kjYHdJ0adBXAniahCfMaQMp+VWzPdCTh6+7Wv5iJ9wFSp6"
                        + "FCyreIwnCkhu8orvNPV3LWFVZ8ubO0Zxe2t/fXtj0yAfN8Zq0bZNd93izaZJ"
                        + "ZzF1tZ7bos4l0d/A5gm3WtxzXEkcULCiYhXkMmk4dZNO0a4peK7iBV4yjP/j"
                        + "NBnS0rducbumbx8c+Yc2cp9nBuW6Z2x82C9u7TKwIqncXNIhbJw2PbNOb8Jp"
                        + "kfxgya8IR//kCtszPNfkdbqR/jvSxGnIkWXTunypTdKjdI2uGtOQT1T+4mDy"
                        + "/KntpZFOkVHsWrwA+0qdCPqCSUACaWrVywnQkPHr/RgIFp8iSmNg/hyR9OAZ"
                        + "1B+IfS5coGsvSKQLv9qSUj7qy48RAOQoQZ408jJKclmq5AghkUOXsgFS9gYp"
                        + "Ry+K+jFEtAGqj4Q29gIbmVs2rom9FEEqCdLRaO01JRNSMm2UKJhG5dEQUqaR"
                        + "nK5JSPIM3RJygfgtxjgxJkh/so2hhQytk5Gm8ljI8IKN5IKNDJ9DySS+IRlu"
                        + "6Mudmxui2wFdcQIzBH1I+Dn6z7cZyIUGcp1HOZGg+njooBY4yJKD+Bn6/gPO"
                        + "kATow6agQM9iicB6GzQbQrOd0H4JnfAfUxazvk6EFKfJuvyGTpNK8i/Dv36D"
                        + "XAUAAA=="))
                .run().expect(""
                        + "src/test/pkg/ConditionalApiTest.java:28: Error: Call requires API level 18 (current min is 14): new android.animation.RectEvaluator [NewApi]\n"
                        + "            new RectEvaluator(); // ERROR\n"
                        + "            ~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/ConditionalApiTest.java:37: Error: Call requires API level 21 (current min is 14): new android.animation.RectEvaluator [NewApi]\n"
                        + "            new RectEvaluator(rect); // ERROR\n"
                        + "            ~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/ConditionalApiTest.java:43: Error: Call requires API level 21 (current min is 14): new android.animation.RectEvaluator [NewApi]\n"
                        + "            new RectEvaluator(rect); // ERROR\n"
                        + "            ~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/ConditionalApiTest.java:45: Error: Call requires API level 21 (current min is 14): new android.animation.RectEvaluator [NewApi]\n"
                        + "            new RectEvaluator(rect); // ERROR\n"
                        + "            ~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/ConditionalApiTest.java:27: Warning: Unnecessary; SDK_INT is always >= 14 [ObsoleteSdkInt]\n"
                        + "        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {\n"
                        + "            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/ConditionalApiTest.java:42: Warning: Unnecessary; SDK_INT is always >= 14 [ObsoleteSdkInt]\n"
                        + "        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {\n"
                        + "            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "4 errors, 2 warnings\n");
    }

    public void testConditionalApi1() {
        // See https://code.google.com/p/android/issues/detail?id=137195
        //noinspection all // Sample code

        lint().files(
                classpath(),
                manifest().minSdk(4),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.os.Build;\n"
                        + "import android.widget.GridLayout;\n"
                        + "\n"
                        + "import static android.os.Build.VERSION;\n"
                        + "import static android.os.Build.VERSION.SDK_INT;\n"
                        + "import static android.os.Build.VERSION_CODES;\n"
                        + "import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;\n"
                        + "import static android.os.Build.VERSION_CODES.JELLY_BEAN;\n"
                        + "\n"
                        + "@SuppressWarnings({\"UnusedDeclaration\", \"ConstantConditions\"})\n"
                        + "public class VersionConditional1 {\n"
                        + "    public void test(boolean priority) {\n"
                        + "        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {\n"
                        + "            new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "        } else {\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {\n"
                        + "            new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "        } else {\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (SDK_INT >= ICE_CREAM_SANDWICH) {\n"
                        + "            new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "        } else {\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {\n"
                        + "            new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "        } else {\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "        } else {\n"
                        + "            new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (Build.VERSION.SDK_INT >= 14) {\n"
                        + "            new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "        } else {\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (VERSION.SDK_INT >= VERSION_CODES.ICE_CREAM_SANDWICH) {\n"
                        + "            new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "        } else {\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        // Nested conditionals\n"
                        + "        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {\n"
                        + "            if (priority) {\n"
                        + "                new GridLayout(null).getOrientation(); // Flagged\n"
                        + "            } else {\n"
                        + "                new GridLayout(null).getOrientation(); // Flagged\n"
                        + "            }\n"
                        + "        } else {\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        // Nested conditionals 2\n"
                        + "        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {\n"
                        + "            if (priority) {\n"
                        + "                new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "            } else {\n"
                        + "                new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "            }\n"
                        + "        } else {\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    public void test2(boolean priority) {\n"
                        + "        if (android.os.Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {\n"
                        + "            new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "        } else {\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (android.os.Build.VERSION.SDK_INT >= 16) {\n"
                        + "            new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "        } else {\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (android.os.Build.VERSION.SDK_INT >= 13) {\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "        } else {\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {\n"
                        + "            new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "        } else {\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (SDK_INT >= JELLY_BEAN) {\n"
                        + "            new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "        } else {\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {\n"
                        + "            new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "        } else {\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "        } else {\n"
                        + "            new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (Build.VERSION.SDK_INT >= 16) {\n"
                        + "            new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "        } else {\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {\n"
                        + "            new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "        } else {\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"), base64gzip("bin/classes/test/pkg/VersionConditional1.class", ""
                        + "H4sIAAAAAAAAAL2Vz28TVxDHP8/5YSdxEmOCHUgIMYTWkJYtUBWpQahgErAw"
                        + "iYrTpI1U0o29DS+4u+l6zQ+1QoAQ6oEjEuIG9NDeKjgQCSTECSGOiBNcemz/"
                        + "AMQJxDzjkDQitCoS+1Yzb36+mfcdrx+8uHUH2M5HYVY3E6G7ibX0hFnXTD29"
                        + "hqTCrA+zQdG4U7s62KWoS28aU9RnvKKjaM9p1xmu/DDl+KP2VEk08ZxXsEtj"
                        + "tq+NXFPWB0d0WdGTC5xyYM0enbbGHL+sPTfjuUUdyMYubR0wfmIXlp4wZ0Rm"
                        + "fe35OjipUBOK1nxgF44etGdrSRuM8zZFc96r+AVnSBtl5xsSb5mxj9lh+qQA"
                        + "2y36ni5aXtnaU9GlYt/Y4KF8dmR4MjOydzAvZyyRo1nXdfxMyS6XnXKUJprD"
                        + "bIzyIWnF6vlkx3Vx2gmsfb4u5uyTXiUwjpuibKZf0f22lhUxU5tVst1pa2Rq"
                        + "xilI97GlRUpXy9WtCC/s8nsPTGaHR+W2snJuOjcfVPDcwHEDK2P4iWDA3G2b"
                        + "VDzia1HbppYqrFlShGUKzFOHMt0KbRHJEq6EN2yeI3RdNiGiQhurylZZiFx1"
                        + "oI124dIFK14Fh/olWUhUkzeoi7VdJX6b+m/UHA03aRz/jfbF4vt1ufaeDmq5"
                        + "ysauy0vdev4lKvbGqK5/itF5cfx6FTUDy1l6hbYLkjGSxNnAKvpJ8DmdDNHF"
                        + "MN18Sw9a8P6R9Zymjwsy0xdJ86tM7R98zBxbuMcnPJSvw2M+5W8+46lkeM6A"
                        + "UuxUMXapdexW/Qyq7QypHexTQ2TVV+TUYTnBjETiFey1kTC7OCtlKDqqto5k"
                        + "S2LRWtMmb0Ssq+YHRz2pDU6qdhvx5Vr/L/bWd4x/Z/u1BYSXQft/51+A3hdo"
                        + "4UuB/pBAPyqDMC6gfs0OJvhCQN/PYdF8J6vAEYoc53uBX/MLM1yixBU8fmdW"
                        + "4Pe5S4X7HOMRJ/iTn/iLn3nGKfmZn1ERzqok51Qv5xdBnXoNdWoR1B3Ek5HE"
                        + "67WyxVDxT1S/I0kZSMQ7JN6drMH8A3XyAU0vAYbI8ZqdBgAA"))
                .run().expect(""
                        + "src/test/pkg/VersionConditional1.java:18: Error: Call requires API level 14 (current min is 4): android.widget.GridLayout#getOrientation [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "                                 ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1.java:18: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1.java:24: Error: Call requires API level 14 (current min is 4): android.widget.GridLayout#getOrientation [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "                                 ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1.java:24: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1.java:30: Error: Call requires API level 14 (current min is 4): android.widget.GridLayout#getOrientation [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "                                 ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1.java:30: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1.java:36: Error: Call requires API level 14 (current min is 4): android.widget.GridLayout#getOrientation [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "                                 ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1.java:36: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1.java:40: Error: Call requires API level 14 (current min is 4): android.widget.GridLayout#getOrientation [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "                                 ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1.java:40: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1.java:48: Error: Call requires API level 14 (current min is 4): android.widget.GridLayout#getOrientation [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "                                 ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1.java:48: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1.java:54: Error: Call requires API level 14 (current min is 4): android.widget.GridLayout#getOrientation [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "                                 ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1.java:54: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1.java:60: Error: Call requires API level 14 (current min is 4): android.widget.GridLayout#getOrientation [NewApi]\n"
                        + "                new GridLayout(null).getOrientation(); // Flagged\n"
                        + "                                     ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1.java:60: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "                new GridLayout(null).getOrientation(); // Flagged\n"
                        + "                ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1.java:62: Error: Call requires API level 14 (current min is 4): android.widget.GridLayout#getOrientation [NewApi]\n"
                        + "                new GridLayout(null).getOrientation(); // Flagged\n"
                        + "                                     ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1.java:62: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "                new GridLayout(null).getOrientation(); // Flagged\n"
                        + "                ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1.java:65: Error: Call requires API level 14 (current min is 4): android.widget.GridLayout#getOrientation [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "                                 ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1.java:65: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1.java:76: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1.java:84: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1.java:90: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1.java:94: Error: Call requires API level 14 (current min is 4): android.widget.GridLayout#getOrientation [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "                                 ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1.java:94: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1.java:96: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1.java:102: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1.java:108: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1.java:114: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1.java:118: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1.java:126: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1.java:132: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "32 errors, 0 warnings\n");
    }

    public void testConditionalApi1b() {
        // See https://code.google.com/p/android/issues/detail?id=137195
        // This is like testConditionalApi1, but with each logical lookup call extracted into
        // a single method. This makes debugging through the control flow graph a lot easier.
        //noinspection all // Sample code

        lint().files(
                classpath()
                , manifest().minSdk(4),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.os.Build;\n"
                        + "import android.widget.GridLayout;\n"
                        + "\n"
                        + "import static android.os.Build.VERSION;\n"
                        + "import static android.os.Build.VERSION.SDK_INT;\n"
                        + "import static android.os.Build.VERSION_CODES;\n"
                        + "import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;\n"
                        + "import static android.os.Build.VERSION_CODES.JELLY_BEAN;\n"
                        + "\n"
                        + "@SuppressWarnings({\"UnusedDeclaration\", \"ConstantConditions\"})\n"
                        + "public class VersionConditional1b {\n"
                        + "    private void m9(boolean priority) {\n"
                        + "        // Nested conditionals 2\n"
                        + "        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {\n"
                        + "            if (priority) {\n"
                        + "                new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "            } else {\n"
                        + "                new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "            }\n"
                        + "        } else {\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m8(boolean priority) {\n"
                        + "        // Nested conditionals\n"
                        + "        if (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB) {\n"
                        + "            if (priority) {\n"
                        + "                new GridLayout(null).getOrientation(); // Flagged\n"
                        + "            } else {\n"
                        + "                new GridLayout(null).getOrientation(); // Flagged\n"
                        + "            }\n"
                        + "        } else {\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m7() {\n"
                        + "        if (VERSION.SDK_INT >= VERSION_CODES.ICE_CREAM_SANDWICH) {\n"
                        + "            new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "        } else {\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m6() {\n"
                        + "        if (VERSION.SDK_INT >= 14) {\n"
                        + "            new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "        } else {\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m5() {\n"
                        + "        if (VERSION.SDK_INT < VERSION_CODES.ICE_CREAM_SANDWICH) {\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "        } else {\n"
                        + "            new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m4() {\n"
                        + "        if (VERSION.SDK_INT >= VERSION_CODES.ICE_CREAM_SANDWICH) {\n"
                        + "            new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "        } else {\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m3() {\n"
                        + "        if (SDK_INT >= ICE_CREAM_SANDWICH) {\n"
                        + "            new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "        } else {\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m2() {\n"
                        + "        if (VERSION.SDK_INT >= VERSION_CODES.ICE_CREAM_SANDWICH) {\n"
                        + "            new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "        } else {\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m1() {\n"
                        + "        if (VERSION.SDK_INT >= VERSION_CODES.ICE_CREAM_SANDWICH) {\n"
                        + "            new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "        } else {\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    public void test2(boolean priority) {\n"
                        + "        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {\n"
                        + "            new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "        } else {\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (VERSION.SDK_INT >= 16) {\n"
                        + "            new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "        } else {\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (VERSION.SDK_INT >= 13) {\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "        } else {\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {\n"
                        + "            new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "        } else {\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (SDK_INT >= JELLY_BEAN) {\n"
                        + "            new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "        } else {\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {\n"
                        + "            new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "        } else {\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN) {\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "        } else {\n"
                        + "            new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (VERSION.SDK_INT >= 16) {\n"
                        + "            new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "        } else {\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {\n"
                        + "            new GridLayout(null).getOrientation(); // Not flagged\n"
                        + "        } else {\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"),
                base64gzip("bin/classes/test/pkg/VersionConditional1b.class", ""
                        + "H4sIAAAAAAAAAK2WzW8TRxjGn7Gd2ImdxJg0TjD5MBhwAmXJB0lTIlTi8GFw"
                        + "E4EjR0QIWHtXYYOzG603pKkqgSq1IKgqFSFQb4VLe4UDkVoJcax67q0XjvwH"
                        + "vVV9xthxsAgGxV7p3Xln3pnfs+/Mvuu//vv9BYBhjHgRbYYPsSbsw34vDjTD"
                        + "g7g0/V4MeHFQoHHCMA3nuIA73p8R8CQsTRdoSxmmPr2ylNXtWTWbZ08oZeXU"
                        + "fEa1DemXOj3ONaMg0Jty9IKjLF9fUDK6XTAsM2GZmuGwoeYHs8cEXEvjjI7P"
                        + "S4Rv2TYs23DWBMS8QEvaUXPXv1SXS2u6lj6TZkyaUWmOSjMizbA0Q9IMCjRI"
                        + "Jp3mtLVi5/RThpzc9S7+4UX1hurFIYEe1dRsy9AUq6BMrhh5LZY5eSGdnJm+"
                        + "kpiZOpmmmCo/kDRN3U7k1UJBLwTQhGYvPg1AwRGyyoutGtqC7iinbUNLqWvW"
                        + "iiMDBwMYAgV3vzc1AkEpTsmr5oIyk13Ucw67qlUKdG4lXMBbaaWnzl1JTs8y"
                        + "r0mB3fFUeVLOMh3ddJSEvH/lHJO70ErJM7bBblWKKe5/ElF4eVzkzw8hH5fW"
                        + "T0/hXfDeMLAO11M2XAjQNhY7W3iBfjEArWgrjgexozT5Ntz0gZFncAeDj7Ev"
                        + "8jNCf8BzUayj4Tka535F5G03UHbnJMtdZO0iDVzVx3UDCCGCdgwgzFPeVeR3"
                        + "vGGU+LIVwk4qaC+qckdafQz4ZEPU3ZKoMSnK/05RPW+7bZvdal3d1NVDv5e6"
                        + "otQVwyj2b9I1tqFrrFqXnwEdG5nWuKxMZEzqan1crWkLESG+0kA/RciUHMJe"
                        + "HN60KbESXCI9cIUlMVwX4iCJQ1xrhMTRGsTOLYhPPoo4TuLnJE6QeLwGsasu"
                        + "z3iCxEkSp0g8VYO4qy7EJIlnSUyROF2DGKkL8TyJF0icJTFTg7i7LsSLJM6T"
                        + "eInEy+8lsoaW31rxz5u3VkRLpSS0Vd34kPGWbc7f9viTSoqq07Xd9Svptrlh"
                        + "gMp0Z5luDX1YQBzXWIcW8QWWcAYm5rCMqyiw18EqbuAW1nAHX+MRvsEvuInf"
                        + "2LOOb/ES3+FPfI+/OfoK9/AaP+Bf/Mh9+0n4cF+E8UD04WGl7oloue6xVal7"
                        + "7QiFfR0b106/tIzvKX45enkk5DFxMboPeyD/vPSxoDX9DyGCeCXYCAAA"))
                .run().expect(""
                        + "src/test/pkg/VersionConditional1b.java:23: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1b.java:31: Error: Call requires API level 14 (current min is 4): android.widget.GridLayout#getOrientation [NewApi]\n"
                        + "                new GridLayout(null).getOrientation(); // Flagged\n"
                        + "                                     ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1b.java:31: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "                new GridLayout(null).getOrientation(); // Flagged\n"
                        + "                ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1b.java:33: Error: Call requires API level 14 (current min is 4): android.widget.GridLayout#getOrientation [NewApi]\n"
                        + "                new GridLayout(null).getOrientation(); // Flagged\n"
                        + "                                     ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1b.java:33: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "                new GridLayout(null).getOrientation(); // Flagged\n"
                        + "                ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1b.java:36: Error: Call requires API level 14 (current min is 4): android.widget.GridLayout#getOrientation [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "                                 ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1b.java:36: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1b.java:44: Error: Call requires API level 14 (current min is 4): android.widget.GridLayout#getOrientation [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "                                 ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1b.java:44: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1b.java:52: Error: Call requires API level 14 (current min is 4): android.widget.GridLayout#getOrientation [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "                                 ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1b.java:52: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1b.java:58: Error: Call requires API level 14 (current min is 4): android.widget.GridLayout#getOrientation [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "                                 ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1b.java:58: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1b.java:68: Error: Call requires API level 14 (current min is 4): android.widget.GridLayout#getOrientation [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "                                 ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1b.java:68: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1b.java:76: Error: Call requires API level 14 (current min is 4): android.widget.GridLayout#getOrientation [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "                                 ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1b.java:76: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1b.java:84: Error: Call requires API level 14 (current min is 4): android.widget.GridLayout#getOrientation [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "                                 ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1b.java:84: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1b.java:92: Error: Call requires API level 14 (current min is 4): android.widget.GridLayout#getOrientation [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "                                 ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1b.java:92: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1b.java:100: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1b.java:106: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1b.java:110: Error: Call requires API level 14 (current min is 4): android.widget.GridLayout#getOrientation [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "                                 ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1b.java:110: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null).getOrientation(); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1b.java:112: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1b.java:118: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1b.java:124: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1b.java:130: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1b.java:134: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1b.java:142: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional1b.java:148: Error: Call requires API level 14 (current min is 4): new android.widget.GridLayout [NewApi]\n"
                        + "            new GridLayout(null); // Flagged\n"
                        + "            ~~~~~~~~~~~~~~\n"
                        + "32 errors, 0 warnings\n");
    }

    public void testConditionalApi2() {
        // See https://code.google.com/p/android/issues/detail?id=137195
        //noinspection all // Sample code

        lint().files(
                classpath(),
                manifest().minSdk(4),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.graphics.drawable.Drawable;\n"
                        + "import android.view.View;\n"
                        + "\n"
                        + "import static android.os.Build.VERSION.SDK_INT;\n"
                        + "import static android.os.Build.VERSION_CODES;\n"
                        + "import static android.os.Build.VERSION_CODES.GINGERBREAD;\n"
                        + "import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;\n"
                        + "import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1;\n"
                        + "import static android.os.Build.VERSION_CODES.JELLY_BEAN;\n"
                        + "\n"
                        + "@SuppressWarnings({\"ConstantConditions\", \"StatementWithEmptyBody\"})\n"
                        + "public class VersionConditional2 {\n"
                        + "    // Requires API 16 (JELLY_BEAN)\n"
                        + "    // root.setBackground(background);\n"
                        + "\n"
                        + "    private void testGreaterThan(View root, Drawable background) {\n"
                        + "        if (SDK_INT > GINGERBREAD) {\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (SDK_INT > ICE_CREAM_SANDWICH) {\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (SDK_INT > ICE_CREAM_SANDWICH_MR1) { // => SDK_INT >= JELLY_BEAN\n"
                        + "            root.setBackground(background); // Not flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (SDK_INT > JELLY_BEAN) {\n"
                        + "            root.setBackground(background); // Not flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (SDK_INT > VERSION_CODES.JELLY_BEAN_MR1) {\n"
                        + "            root.setBackground(background); // Not flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void testGreaterThanOrEquals(View root, Drawable background) {\n"
                        + "        if (SDK_INT >= GINGERBREAD) {\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (SDK_INT >= ICE_CREAM_SANDWICH) {\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (SDK_INT >= ICE_CREAM_SANDWICH_MR1) {\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (SDK_INT >= JELLY_BEAN) {\n"
                        + "            root.setBackground(background); // Not flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {\n"
                        + "            root.setBackground(background); // Not flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void testLessThan(View root, Drawable background) {\n"
                        + "        if (SDK_INT < GINGERBREAD) {\n"
                        + "            // Other\n"
                        + "        } else {\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (SDK_INT < ICE_CREAM_SANDWICH) {\n"
                        + "            // Other\n"
                        + "        } else {\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (SDK_INT < ICE_CREAM_SANDWICH_MR1) {\n"
                        + "            // Other\n"
                        + "        } else {\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (SDK_INT < JELLY_BEAN) {\n"
                        + "            // Other\n"
                        + "        } else {\n"
                        + "            root.setBackground(background); // Not flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (SDK_INT < VERSION_CODES.JELLY_BEAN_MR1) {\n"
                        + "            // Other\n"
                        + "        } else {\n"
                        + "            root.setBackground(background); // Not flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void testLessThanOrEqual(View root, Drawable background) {\n"
                        + "        if (SDK_INT <= GINGERBREAD) {\n"
                        + "            // Other\n"
                        + "        } else {\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (SDK_INT <= ICE_CREAM_SANDWICH) {\n"
                        + "            // Other\n"
                        + "        } else {\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (SDK_INT <= ICE_CREAM_SANDWICH_MR1) {\n"
                        + "            // Other\n"
                        + "        } else { // => SDK_INT >= JELLY_BEAN\n"
                        + "            root.setBackground(background); // Not flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (SDK_INT <= JELLY_BEAN) {\n"
                        + "            // Other\n"
                        + "        } else {\n"
                        + "            root.setBackground(background); // Not flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (SDK_INT <= VERSION_CODES.JELLY_BEAN_MR1) {\n"
                        + "            // Other\n"
                        + "        } else {\n"
                        + "            root.setBackground(background); // Not flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void testEquals(View root, Drawable background) {\n"
                        + "        if (SDK_INT == GINGERBREAD) {\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (SDK_INT == ICE_CREAM_SANDWICH) {\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (SDK_INT == ICE_CREAM_SANDWICH_MR1) {\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (SDK_INT == JELLY_BEAN) {\n"
                        + "            root.setBackground(background); // Not flagged\n"
                        + "        }\n"
                        + "\n"
                        + "        if (SDK_INT == VERSION_CODES.JELLY_BEAN_MR1) {\n"
                        + "            root.setBackground(background); // Not flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"), base64gzip("bin/classes/test/pkg/VersionConditional2.class", ""
                        + "H4sIAAAAAAAAAKWVa08TQRiFz0ChUAvdgggqKghyp+s9UYgXLiqxgrWkJsaE"
                        + "bNtNWai7sLuVxJhoookmmmj8BaAmftZETfzgd/1RxrNlqZOVGoLbZGbOzOz7"
                        + "nmfm3fTnr2/fAZxCIozOCOpxpBFd6I7gKHrC6A3jmED9uGEa7gWB2oHBjEBo"
                        + "0srrArGkYeqzpXtZ3Z7XskXOtCStnFbMaLbhaX8y5C4ajsDhpKs7rrqyXFAz"
                        + "uu0YljlpmXnD5UArnhxjOG/9qq1rLuMtaqbA+YGkZuZty8ir9w19Tc2wGatM"
                        + "FWxtZdHIOWre1ta8VOqUPxgrm7QtyxVo3SaEQCSr5ZYLtlUy8wK9Owkp0JR2"
                        + "+dINbcXnag/4nbOnV0takahRbyWpO84mRqss/V204M1uvRFJWyU7p18xvMAd"
                        + "25xPYkm7r4XRx3PcMms56kTJKOZ7M9O30jNzswuTc1PTafoM6OiMaer2ZFFz"
                        + "HN2JIoyGMPqjGMRQGMNRjGBUoPNfdyOgeNnVomYW1Lnskp7juSpBG/RdzZlA"
                        + "+M8oPXV9YWZ2XkDMCMT/uhz6d3R3Qrqe/oGdXTm6Ucfy9Z56CI+TbSOVyl6w"
                        + "rxv6ipqPHNQg4m8CmrGHbXRzA/um8nozYv7Ln6lq2U98Qq3S+B4NwyNfEPJE"
                        + "syxislBkEd8SXubacuZEOWcrDe5lvna0oQMHcJBfXCf6cYSGu3AaPRhHLy6j"
                        + "r7y7a9OF79AbKYiXXU+ghbFqGAuErmuK8qFsq4KwISPIIiYLRRbxjSoIg0QY"
                        + "oqFRZksQ4QQRThLhDBHOEuEcEc4TYXx3CPsqCD98hFQZ4S3qP8gUAR0LaCWg"
                        + "45IOEl1k6/ma5nVco+PrGMYsjiOFMczjEm5z5g5u4q5ElKoQpSpEKYkoisie"
                        + "UMT/ca69Cta7AFZAxwJaCej4u+pYC2yzNKcTa5G2l4llEmuVWC6x1oj1gFgP"
                        + "/wOro0rBrctMsojJQpFFfL0KxyMW3GO6esKCe8qCe8aCe86Ce8GCe8mCe8VS"
                        + "e82Ce7O7gttf/vgP4DD7Fo4Ej+sQvP/DgxhA42+M2tIyKwcAAA=="))
                .run().expect(""
                        + "src/test/pkg/VersionConditional2.java:20: Error: Call requires API level 16 (current min is 4): android.view.View#setBackground [NewApi]\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "                 ~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional2.java:24: Error: Call requires API level 16 (current min is 4): android.view.View#setBackground [NewApi]\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "                 ~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional2.java:42: Error: Call requires API level 16 (current min is 4): android.view.View#setBackground [NewApi]\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "                 ~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional2.java:46: Error: Call requires API level 16 (current min is 4): android.view.View#setBackground [NewApi]\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "                 ~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional2.java:50: Error: Call requires API level 16 (current min is 4): android.view.View#setBackground [NewApi]\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "                 ~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional2.java:66: Error: Call requires API level 16 (current min is 4): android.view.View#setBackground [NewApi]\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "                 ~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional2.java:72: Error: Call requires API level 16 (current min is 4): android.view.View#setBackground [NewApi]\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "                 ~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional2.java:78: Error: Call requires API level 16 (current min is 4): android.view.View#setBackground [NewApi]\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "                 ~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional2.java:98: Error: Call requires API level 16 (current min is 4): android.view.View#setBackground [NewApi]\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "                 ~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional2.java:104: Error: Call requires API level 16 (current min is 4): android.view.View#setBackground [NewApi]\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "                 ~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional2.java:128: Error: Call requires API level 16 (current min is 4): android.view.View#setBackground [NewApi]\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "                 ~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional2.java:132: Error: Call requires API level 16 (current min is 4): android.view.View#setBackground [NewApi]\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "                 ~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional2.java:136: Error: Call requires API level 16 (current min is 4): android.view.View#setBackground [NewApi]\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "                 ~~~~~~~~~~~~~\n"
                        + "13 errors, 0 warnings\n");
    }

    public void testConditionalApi2b() {
        // See https://code.google.com/p/android/issues/detail?id=137195
        // This is like testConditionalApi2, but with each logical lookup call extracted into
        // a single method. This makes debugging through the control flow graph a lot easier.
        //noinspection all // Sample code

        lint().files(
                classpath(),
                manifest().minSdk(4),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.graphics.drawable.Drawable;\n"
                        + "import android.view.View;\n"
                        + "\n"
                        + "import static android.os.Build.VERSION.SDK_INT;\n"
                        + "import static android.os.Build.VERSION_CODES;\n"
                        + "import static android.os.Build.VERSION_CODES.GINGERBREAD;\n"
                        + "import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH;\n"
                        + "import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1;\n"
                        + "import static android.os.Build.VERSION_CODES.JELLY_BEAN;\n"
                        + "\n"
                        + "@SuppressWarnings({\"ConstantConditions\", \"StatementWithEmptyBody\"})\n"
                        + "public class VersionConditional2b {\n"
                        + "    private void gt5(View root, Drawable background) {\n"
                        + "        if (SDK_INT > GINGERBREAD) {\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void gt4(View root, Drawable background) {\n"
                        + "        if (SDK_INT > ICE_CREAM_SANDWICH) {\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void gt3(View root, Drawable background) {\n"
                        + "        if (SDK_INT > ICE_CREAM_SANDWICH_MR1) { // => SDK_INT >= JELLY_BEAN\n"
                        + "            root.setBackground(background); // Not flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void gt2(View root, Drawable background) {\n"
                        + "        if (SDK_INT > JELLY_BEAN) {\n"
                        + "            root.setBackground(background); // Not flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void gt1(View root, Drawable background) {\n"
                        + "        if (SDK_INT > VERSION_CODES.JELLY_BEAN_MR1) {\n"
                        + "            root.setBackground(background); // Not flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void gte5(View root, Drawable background) {\n"
                        + "        if (SDK_INT >= GINGERBREAD) {\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void gte4(View root, Drawable background) {\n"
                        + "        if (SDK_INT >= ICE_CREAM_SANDWICH) {\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void gte3(View root, Drawable background) {\n"
                        + "        if (SDK_INT >= ICE_CREAM_SANDWICH_MR1) {\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void gte2(View root, Drawable background) {\n"
                        + "        if (SDK_INT >= JELLY_BEAN) {\n"
                        + "            root.setBackground(background); // Not flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void gte1(View root, Drawable background) {\n"
                        + "        if (SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {\n"
                        + "            root.setBackground(background); // Not flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void lt5(View root, Drawable background) {\n"
                        + "        if (SDK_INT < GINGERBREAD) {\n"
                        + "            // Other\n"
                        + "        } else {\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void lt4(View root, Drawable background) {\n"
                        + "        if (SDK_INT < ICE_CREAM_SANDWICH) {\n"
                        + "            // Other\n"
                        + "        } else {\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void lt3(View root, Drawable background) {\n"
                        + "        if (SDK_INT < ICE_CREAM_SANDWICH_MR1) {\n"
                        + "            // Other\n"
                        + "        } else {\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void lt2(View root, Drawable background) {\n"
                        + "        if (SDK_INT < JELLY_BEAN) {\n"
                        + "            // Other\n"
                        + "        } else {\n"
                        + "            root.setBackground(background); // Not flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void lt1(View root, Drawable background) {\n"
                        + "        if (SDK_INT < VERSION_CODES.JELLY_BEAN_MR1) {\n"
                        + "            // Other\n"
                        + "        } else {\n"
                        + "            root.setBackground(background); // Not flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void lte5(View root, Drawable background) {\n"
                        + "        if (SDK_INT <= GINGERBREAD) {\n"
                        + "            // Other\n"
                        + "        } else {\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void lte4(View root, Drawable background) {\n"
                        + "        if (SDK_INT <= ICE_CREAM_SANDWICH) {\n"
                        + "            // Other\n"
                        + "        } else {\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void lte3(View root, Drawable background) {\n"
                        + "        if (SDK_INT <= ICE_CREAM_SANDWICH_MR1) {\n"
                        + "            // Other\n"
                        + "        } else { // => SDK_INT >= JELLY_BEAN\n"
                        + "            root.setBackground(background); // Not flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void lte2(View root, Drawable background) {\n"
                        + "        if (SDK_INT <= JELLY_BEAN) {\n"
                        + "            // Other\n"
                        + "        } else {\n"
                        + "            root.setBackground(background); // Not flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void lte1(View root, Drawable background) {\n"
                        + "        if (SDK_INT <= VERSION_CODES.JELLY_BEAN_MR1) {\n"
                        + "            // Other\n"
                        + "        } else {\n"
                        + "            root.setBackground(background); // Not flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void eq5(View root, Drawable background) {\n"
                        + "        if (SDK_INT == GINGERBREAD) {\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void eq4(View root, Drawable background) {\n"
                        + "        if (SDK_INT == ICE_CREAM_SANDWICH) {\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void eq3(View root, Drawable background) {\n"
                        + "        if (SDK_INT == ICE_CREAM_SANDWICH_MR1) {\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void eq2(View root, Drawable background) {\n"
                        + "        if (SDK_INT == JELLY_BEAN) {\n"
                        + "            root.setBackground(background); // Not flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void eq1(View root, Drawable background) {\n"
                        + "        if (SDK_INT == VERSION_CODES.JELLY_BEAN_MR1) {\n"
                        + "            root.setBackground(background); // Not flagged\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"))
                .run().expect(""
                        + "src/test/pkg/VersionConditional2b.java:17: Error: Call requires API level 16 (current min is 4): android.view.View#setBackground [NewApi]\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "                 ~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional2b.java:23: Error: Call requires API level 16 (current min is 4): android.view.View#setBackground [NewApi]\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "                 ~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional2b.java:47: Error: Call requires API level 16 (current min is 4): android.view.View#setBackground [NewApi]\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "                 ~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional2b.java:53: Error: Call requires API level 16 (current min is 4): android.view.View#setBackground [NewApi]\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "                 ~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional2b.java:59: Error: Call requires API level 16 (current min is 4): android.view.View#setBackground [NewApi]\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "                 ~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional2b.java:79: Error: Call requires API level 16 (current min is 4): android.view.View#setBackground [NewApi]\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "                 ~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional2b.java:87: Error: Call requires API level 16 (current min is 4): android.view.View#setBackground [NewApi]\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "                 ~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional2b.java:95: Error: Call requires API level 16 (current min is 4): android.view.View#setBackground [NewApi]\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "                 ~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional2b.java:119: Error: Call requires API level 16 (current min is 4): android.view.View#setBackground [NewApi]\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "                 ~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional2b.java:127: Error: Call requires API level 16 (current min is 4): android.view.View#setBackground [NewApi]\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "                 ~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional2b.java:157: Error: Call requires API level 16 (current min is 4): android.view.View#setBackground [NewApi]\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "                 ~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional2b.java:163: Error: Call requires API level 16 (current min is 4): android.view.View#setBackground [NewApi]\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "                 ~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional2b.java:169: Error: Call requires API level 16 (current min is 4): android.view.View#setBackground [NewApi]\n"
                        + "            root.setBackground(background); // Flagged\n"
                        + "                 ~~~~~~~~~~~~~\n"
                        + "13 errors, 0 warnings\n");
    }

    public void testConditionalApi3() {
        // See https://code.google.com/p/android/issues/detail?id=137195
        //noinspection all // Sample code

        lint().files(
                classpath(),
                manifest().minSdk(4),
                java(""
                        + "package test.pkg;\n"
                        + "import android.os.Build;\n"
                        + "import android.os.Build.VERSION_CODES;\n"
                        + "import android.view.ViewDebug;\n"
                        + "\n"
                        + "import static android.os.Build.VERSION_CODES.KITKAT_WATCH;\n"
                        + "import static android.os.Build.VERSION_CODES.LOLLIPOP;\n"
                        + "\n"
                        + "@SuppressWarnings({\"unused\", \"StatementWithEmptyBody\"})\n"
                        + "public class VersionConditional3 {\n"
                        + "    public void test(ViewDebug.ExportedProperty property) {\n"
                        + "        // Test short circuit evaluation\n"
                        + "        if (Build.VERSION.SDK_INT > 18 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "        }\n"
                        + "        if (Build.VERSION.SDK_INT > 19 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "        }\n"
                        + "        if (Build.VERSION.SDK_INT > 20 && property.hasAdjacentMapping()) { // OK\n"
                        + "        }\n"
                        + "        if (Build.VERSION.SDK_INT > 21 && property.hasAdjacentMapping()) { // OK\n"
                        + "        }\n"
                        + "        if (Build.VERSION.SDK_INT > 22 && property.hasAdjacentMapping()) { // OK\n"
                        + "        }\n"
                        + "\n"
                        + "        if (Build.VERSION.SDK_INT >= 18 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "        }\n"
                        + "        if (Build.VERSION.SDK_INT >= 19 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "        }\n"
                        + "        if (Build.VERSION.SDK_INT >= 20 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "        }\n"
                        + "        if (Build.VERSION.SDK_INT >= 21 && property.hasAdjacentMapping()) { // OK\n"
                        + "        }\n"
                        + "        if (Build.VERSION.SDK_INT >= 22 && property.hasAdjacentMapping()) { // OK\n"
                        + "        }\n"
                        + "\n"
                        + "        if (Build.VERSION.SDK_INT == 18 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "        }\n"
                        + "        if (Build.VERSION.SDK_INT == 19 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "        }\n"
                        + "        if (Build.VERSION.SDK_INT == 20 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "        }\n"
                        + "        if (Build.VERSION.SDK_INT == 21 && property.hasAdjacentMapping()) { // OK\n"
                        + "        }\n"
                        + "        if (Build.VERSION.SDK_INT == 22 && property.hasAdjacentMapping()) { // OK\n"
                        + "        }\n"
                        + "\n"
                        + "        if (Build.VERSION.SDK_INT < 18 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "        }\n"
                        + "        if (Build.VERSION.SDK_INT < 22 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "        }\n"
                        + "        if (Build.VERSION.SDK_INT <= 18 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "        }\n"
                        + "        if (Build.VERSION.SDK_INT <= 22 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "        }\n"
                        + "\n"
                        + "        // Symbolic names instead\n"
                        + "        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD && property.hasAdjacentMapping()) { // ERROR\n"
                        + "        }\n"
                        + "        if (Build.VERSION.SDK_INT > VERSION_CODES.KITKAT && property.hasAdjacentMapping()) { // ERROR\n"
                        + "        }\n"
                        + "        if (Build.VERSION.SDK_INT > KITKAT_WATCH && property.hasAdjacentMapping()) { // OK\n"
                        + "        }\n"
                        + "        if (Build.VERSION.SDK_INT > LOLLIPOP && property.hasAdjacentMapping()) { // OK\n"
                        + "        }\n"
                        + "\n"
                        + "        // Wrong operator\n"
                        + "        if (Build.VERSION.SDK_INT > 21 || property.hasAdjacentMapping()) { // ERROR\n"
                        + "        }\n"
                        + "\n"
                        + "        // Test multiple conditions in short circuit evaluation\n"
                        + "        if (Build.VERSION.SDK_INT > 21 &&\n"
                        + "                System.getProperty(\"something\") != null &&\n"
                        + "                property.hasAdjacentMapping()) { // OK\n"
                        + "        }\n"
                        + "\n"
                        + "        // Test order (still before call)\n"
                        + "        if (System.getProperty(\"something\") != null &&\n"
                        + "                Build.VERSION.SDK_INT > 21 &&\n"
                        + "                property.hasAdjacentMapping()) { // OK\n"
                        + "        }\n"
                        + "\n"
                        + "        // Test order (after call)\n"
                        + "        if (System.getProperty(\"something\") != null &&\n"
                        + "                property.hasAdjacentMapping() && // ERROR\n"
                        + "                Build.VERSION.SDK_INT > 21) {\n"
                        + "        }\n"
                        + "\n"
                        + "        if (Build.VERSION.SDK_INT > 21 && System.getProperty(\"something\") == null) { // OK\n"
                        + "            boolean p = property.hasAdjacentMapping(); // OK\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"))
                .run().expect(""
                        + "src/test/pkg/VersionConditional3.java:13: Error: Call requires API level 21 (current min is 4): android.view.ViewDebug.ExportedProperty#hasAdjacentMapping [NewApi]\n"
                        + "        if (Build.VERSION.SDK_INT > 18 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "                                                   ~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional3.java:15: Error: Call requires API level 21 (current min is 4): android.view.ViewDebug.ExportedProperty#hasAdjacentMapping [NewApi]\n"
                        + "        if (Build.VERSION.SDK_INT > 19 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "                                                   ~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional3.java:24: Error: Call requires API level 21 (current min is 4): android.view.ViewDebug.ExportedProperty#hasAdjacentMapping [NewApi]\n"
                        + "        if (Build.VERSION.SDK_INT >= 18 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "                                                    ~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional3.java:26: Error: Call requires API level 21 (current min is 4): android.view.ViewDebug.ExportedProperty#hasAdjacentMapping [NewApi]\n"
                        + "        if (Build.VERSION.SDK_INT >= 19 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "                                                    ~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional3.java:28: Error: Call requires API level 21 (current min is 4): android.view.ViewDebug.ExportedProperty#hasAdjacentMapping [NewApi]\n"
                        + "        if (Build.VERSION.SDK_INT >= 20 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "                                                    ~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional3.java:35: Error: Call requires API level 21 (current min is 4): android.view.ViewDebug.ExportedProperty#hasAdjacentMapping [NewApi]\n"
                        + "        if (Build.VERSION.SDK_INT == 18 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "                                                    ~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional3.java:37: Error: Call requires API level 21 (current min is 4): android.view.ViewDebug.ExportedProperty#hasAdjacentMapping [NewApi]\n"
                        + "        if (Build.VERSION.SDK_INT == 19 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "                                                    ~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional3.java:39: Error: Call requires API level 21 (current min is 4): android.view.ViewDebug.ExportedProperty#hasAdjacentMapping [NewApi]\n"
                        + "        if (Build.VERSION.SDK_INT == 20 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "                                                    ~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional3.java:46: Error: Call requires API level 21 (current min is 4): android.view.ViewDebug.ExportedProperty#hasAdjacentMapping [NewApi]\n"
                        + "        if (Build.VERSION.SDK_INT < 18 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "                                                   ~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional3.java:48: Error: Call requires API level 21 (current min is 4): android.view.ViewDebug.ExportedProperty#hasAdjacentMapping [NewApi]\n"
                        + "        if (Build.VERSION.SDK_INT < 22 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "                                                   ~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional3.java:50: Error: Call requires API level 21 (current min is 4): android.view.ViewDebug.ExportedProperty#hasAdjacentMapping [NewApi]\n"
                        + "        if (Build.VERSION.SDK_INT <= 18 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "                                                    ~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional3.java:52: Error: Call requires API level 21 (current min is 4): android.view.ViewDebug.ExportedProperty#hasAdjacentMapping [NewApi]\n"
                        + "        if (Build.VERSION.SDK_INT <= 22 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "                                                    ~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional3.java:56: Error: Call requires API level 21 (current min is 4): android.view.ViewDebug.ExportedProperty#hasAdjacentMapping [NewApi]\n"
                        + "        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD && property.hasAdjacentMapping()) { // ERROR\n"
                        + "                                                                                ~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional3.java:58: Error: Call requires API level 21 (current min is 4): android.view.ViewDebug.ExportedProperty#hasAdjacentMapping [NewApi]\n"
                        + "        if (Build.VERSION.SDK_INT > VERSION_CODES.KITKAT && property.hasAdjacentMapping()) { // ERROR\n"
                        + "                                                                     ~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional3.java:66: Error: Call requires API level 21 (current min is 4): android.view.ViewDebug.ExportedProperty#hasAdjacentMapping [NewApi]\n"
                        + "        if (Build.VERSION.SDK_INT > 21 || property.hasAdjacentMapping()) { // ERROR\n"
                        + "                                                   ~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional3.java:83: Error: Call requires API level 21 (current min is 4): android.view.ViewDebug.ExportedProperty#hasAdjacentMapping [NewApi]\n"
                        + "                property.hasAdjacentMapping() && // ERROR\n"
                        + "                         ~~~~~~~~~~~~~~~~~~\n"
                        + "16 errors, 0 warnings\n");
    }

    public void testConditionalApi3b() {
        // See https://code.google.com/p/android/issues/detail?id=137195
        // This is like testConditionalApi3, but with each logical lookup call extracted into
        // a single method. This makes debugging through the control flow graph a lot easier.
        //noinspection all // Sample code

        lint().files(
                classpath(),
                manifest().minSdk(4),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.os.Build;\n"
                        + "import android.os.Build.VERSION_CODES;\n"
                        + "import android.view.ViewDebug;\n"
                        + "\n"
                        + "import static android.os.Build.VERSION_CODES.KITKAT_WATCH;\n"
                        + "import static android.os.Build.VERSION_CODES.LOLLIPOP;\n"
                        + "\n"
                        + "@SuppressWarnings({\"unused\", \"StatementWithEmptyBody\"})\n"
                        + "public class VersionConditional3b {\n"
                        + "    private void m28(ViewDebug.ExportedProperty property) {\n"
                        + "        if (Build.VERSION.SDK_INT > 21 && System.getProperty(\"something\") == null) { // OK\n"
                        + "            boolean p = property.hasAdjacentMapping(); // OK\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m27(ViewDebug.ExportedProperty property) {\n"
                        + "        // Test order (after call)\n"
                        + "        if (System.getProperty(\"something\") != null &&\n"
                        + "                property.hasAdjacentMapping() && // ERROR\n"
                        + "                Build.VERSION.SDK_INT > 21) {\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m26(ViewDebug.ExportedProperty property) {\n"
                        + "        // Test order (still before call)\n"
                        + "        if (System.getProperty(\"something\") != null &&\n"
                        + "                Build.VERSION.SDK_INT > 21 &&\n"
                        + "                property.hasAdjacentMapping()) { // OK\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m25(ViewDebug.ExportedProperty property) {\n"
                        + "        // Test multiple conditions in short circuit evaluation\n"
                        + "        if (Build.VERSION.SDK_INT > 21 &&\n"
                        + "                System.getProperty(\"something\") != null &&\n"
                        + "                property.hasAdjacentMapping()) { // OK\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m24(ViewDebug.ExportedProperty property) {\n"
                        + "        // Wrong operator\n"
                        + "        if (Build.VERSION.SDK_INT > 21 || property.hasAdjacentMapping()) { // ERROR\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m23(ViewDebug.ExportedProperty property) {\n"
                        + "        if (Build.VERSION.SDK_INT > LOLLIPOP && property.hasAdjacentMapping()) { // OK\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m22(ViewDebug.ExportedProperty property) {\n"
                        + "        if (Build.VERSION.SDK_INT > KITKAT_WATCH && property.hasAdjacentMapping()) { // OK\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m21(ViewDebug.ExportedProperty property) {\n"
                        + "        if (Build.VERSION.SDK_INT > VERSION_CODES.KITKAT && property.hasAdjacentMapping()) { // ERROR\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m20(ViewDebug.ExportedProperty property) {\n"
                        + "        if (Build.VERSION.SDK_INT > VERSION_CODES.GINGERBREAD && property.hasAdjacentMapping()) { // ERROR\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m19(ViewDebug.ExportedProperty property) {\n"
                        + "        if (Build.VERSION.SDK_INT <= 22 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m18(ViewDebug.ExportedProperty property) {\n"
                        + "        if (Build.VERSION.SDK_INT <= 18 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m17(ViewDebug.ExportedProperty property) {\n"
                        + "        if (Build.VERSION.SDK_INT < 22 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m16(ViewDebug.ExportedProperty property) {\n"
                        + "        if (Build.VERSION.SDK_INT < 18 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m15(ViewDebug.ExportedProperty property) {\n"
                        + "        if (Build.VERSION.SDK_INT == 22 && property.hasAdjacentMapping()) { // OK\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m14(ViewDebug.ExportedProperty property) {\n"
                        + "        if (Build.VERSION.SDK_INT == 21 && property.hasAdjacentMapping()) { // OK\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m13(ViewDebug.ExportedProperty property) {\n"
                        + "        if (Build.VERSION.SDK_INT == 20 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m12(ViewDebug.ExportedProperty property) {\n"
                        + "        if (Build.VERSION.SDK_INT == 19 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m11(ViewDebug.ExportedProperty property) {\n"
                        + "        if (Build.VERSION.SDK_INT == 18 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m10(ViewDebug.ExportedProperty property) {\n"
                        + "        if (Build.VERSION.SDK_INT >= 22 && property.hasAdjacentMapping()) { // OK\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m9(ViewDebug.ExportedProperty property) {\n"
                        + "        if (Build.VERSION.SDK_INT >= 21 && property.hasAdjacentMapping()) { // OK\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m8(ViewDebug.ExportedProperty property) {\n"
                        + "        if (Build.VERSION.SDK_INT >= 20 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m7(ViewDebug.ExportedProperty property) {\n"
                        + "        if (Build.VERSION.SDK_INT >= 19 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m6(ViewDebug.ExportedProperty property) {\n"
                        + "        if (Build.VERSION.SDK_INT >= 18 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m5(ViewDebug.ExportedProperty property) {\n"
                        + "        if (Build.VERSION.SDK_INT > 22 && property.hasAdjacentMapping()) { // OK\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m4(ViewDebug.ExportedProperty property) {\n"
                        + "        if (Build.VERSION.SDK_INT > 21 && property.hasAdjacentMapping()) { // OK\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m3(ViewDebug.ExportedProperty property) {\n"
                        + "        if (Build.VERSION.SDK_INT > 20 && property.hasAdjacentMapping()) { // OK\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m2(ViewDebug.ExportedProperty property) {\n"
                        + "        if (Build.VERSION.SDK_INT > 19 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private void m1(ViewDebug.ExportedProperty property) {\n"
                        + "        if (Build.VERSION.SDK_INT > 18 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"))
                .run().expect(""
                        + "src/test/pkg/VersionConditional3b.java:21: Error: Call requires API level 21 (current min is 4): android.view.ViewDebug.ExportedProperty#hasAdjacentMapping [NewApi]\n"
                        + "                property.hasAdjacentMapping() && // ERROR\n"
                        + "                         ~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional3b.java:44: Error: Call requires API level 21 (current min is 4): android.view.ViewDebug.ExportedProperty#hasAdjacentMapping [NewApi]\n"
                        + "        if (Build.VERSION.SDK_INT > 21 || property.hasAdjacentMapping()) { // ERROR\n"
                        + "                                                   ~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional3b.java:59: Error: Call requires API level 21 (current min is 4): android.view.ViewDebug.ExportedProperty#hasAdjacentMapping [NewApi]\n"
                        + "        if (Build.VERSION.SDK_INT > VERSION_CODES.KITKAT && property.hasAdjacentMapping()) { // ERROR\n"
                        + "                                                                     ~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional3b.java:64: Error: Call requires API level 21 (current min is 4): android.view.ViewDebug.ExportedProperty#hasAdjacentMapping [NewApi]\n"
                        + "        if (Build.VERSION.SDK_INT > VERSION_CODES.GINGERBREAD && property.hasAdjacentMapping()) { // ERROR\n"
                        + "                                                                          ~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional3b.java:69: Error: Call requires API level 21 (current min is 4): android.view.ViewDebug.ExportedProperty#hasAdjacentMapping [NewApi]\n"
                        + "        if (Build.VERSION.SDK_INT <= 22 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "                                                    ~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional3b.java:74: Error: Call requires API level 21 (current min is 4): android.view.ViewDebug.ExportedProperty#hasAdjacentMapping [NewApi]\n"
                        + "        if (Build.VERSION.SDK_INT <= 18 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "                                                    ~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional3b.java:79: Error: Call requires API level 21 (current min is 4): android.view.ViewDebug.ExportedProperty#hasAdjacentMapping [NewApi]\n"
                        + "        if (Build.VERSION.SDK_INT < 22 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "                                                   ~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional3b.java:84: Error: Call requires API level 21 (current min is 4): android.view.ViewDebug.ExportedProperty#hasAdjacentMapping [NewApi]\n"
                        + "        if (Build.VERSION.SDK_INT < 18 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "                                                   ~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional3b.java:99: Error: Call requires API level 21 (current min is 4): android.view.ViewDebug.ExportedProperty#hasAdjacentMapping [NewApi]\n"
                        + "        if (Build.VERSION.SDK_INT == 20 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "                                                    ~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional3b.java:104: Error: Call requires API level 21 (current min is 4): android.view.ViewDebug.ExportedProperty#hasAdjacentMapping [NewApi]\n"
                        + "        if (Build.VERSION.SDK_INT == 19 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "                                                    ~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional3b.java:109: Error: Call requires API level 21 (current min is 4): android.view.ViewDebug.ExportedProperty#hasAdjacentMapping [NewApi]\n"
                        + "        if (Build.VERSION.SDK_INT == 18 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "                                                    ~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional3b.java:124: Error: Call requires API level 21 (current min is 4): android.view.ViewDebug.ExportedProperty#hasAdjacentMapping [NewApi]\n"
                        + "        if (Build.VERSION.SDK_INT >= 20 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "                                                    ~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional3b.java:129: Error: Call requires API level 21 (current min is 4): android.view.ViewDebug.ExportedProperty#hasAdjacentMapping [NewApi]\n"
                        + "        if (Build.VERSION.SDK_INT >= 19 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "                                                    ~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional3b.java:134: Error: Call requires API level 21 (current min is 4): android.view.ViewDebug.ExportedProperty#hasAdjacentMapping [NewApi]\n"
                        + "        if (Build.VERSION.SDK_INT >= 18 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "                                                    ~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional3b.java:154: Error: Call requires API level 21 (current min is 4): android.view.ViewDebug.ExportedProperty#hasAdjacentMapping [NewApi]\n"
                        + "        if (Build.VERSION.SDK_INT > 19 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "                                                   ~~~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/VersionConditional3b.java:159: Error: Call requires API level 21 (current min is 4): android.view.ViewDebug.ExportedProperty#hasAdjacentMapping [NewApi]\n"
                        + "        if (Build.VERSION.SDK_INT > 18 && property.hasAdjacentMapping()) { // ERROR\n"
                        + "                                                   ~~~~~~~~~~~~~~~~~~\n"
                        + "16 errors, 0 warnings\n");
    }

    public void testConditionalApi4() {
        //noinspection all // Sample code

        lint().files(
                manifest().minSdk(4),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.support.annotation.RequiresApi;\n"
                        + "import android.support.v4.os.BuildCompat;\n"
                        + "\n"
                        + "import static android.os.Build.VERSION.SDK_INT;\n"
                        + "import static android.os.Build.VERSION_CODES.M;\n"
                        + "import static android.os.Build.VERSION_CODES.N;\n"
                        + "import static android.os.Build.VERSION_CODES.N_MR1;\n"
                        + "\n"
                        + "@SuppressWarnings({\"unused\", \"WeakerAccess\", \"StatementWithEmptyBody\"})\n"
                        + "public class VersionConditionals4 {\n"
                        + "    public void testOrConditionals(int x) {\n"
                        + "        if (SDK_INT < N || x < 5 || methodN()) { } // OK\n"
                        + "        if (SDK_INT < N || methodN()) { } // OK\n"
                        + "        if (methodN() || SDK_INT < N) { } // ERROR\n"
                        + "    }\n"
                        + "\n"
                        + "    public void testVersionCheckMethods() {\n"
                        + "        if (SDK_INT >= N) { methodN(); } // OK\n"
                        + "        if (getBuildSdkInt() >= N) {  methodN();  }// OK\n"
                        + "        if (isNougat()) {  methodN(); } // OK\n"
                        + "        if (isAtLeast(N)) { methodN(); } // OK\n"
                        + "        if (isAtLeast(10)) { methodN(); } // ERROR\n"
                        + "        if (isAtLeast(23)) { methodN(); } // ERROR\n"
                        + "        if (isAtLeast(24)) { methodN(); } // OK\n"
                        + "        if (isAtLeast(25)) { methodN(); } // OK\n"
                        + "        if (BuildCompat.isAtLeastN()) { methodM(); } // OK\n"
                        + "        if (BuildCompat.isAtLeastN()) { methodN(); } // OK\n"
                        + "        if (BuildCompat.isAtLeastN()) { methodN_MR1(); } // ERROR\n"
                        + "        if (BuildCompat.isAtLeastNMR1()) { methodN_MR1(); } // OK\n"
                        + "        if (isAtLeastN()) { methodN(); } // OK\n"
                        + "        if (BuildCompat.isAtLeastNMR1()) { methodN(); } // OK\n"
                        + "    }\n"
                        + "\n"
                        + "    // Data-binding adds this method\n"
                        + "    public static int getBuildSdkInt() {\n"
                        + "        return SDK_INT;\n"
                        + "    }\n"
                        + "\n"
                        + "    public static boolean isNougat() {\n"
                        + "        return SDK_INT >= N;\n"
                        + "    }\n"
                        + "\n"
                        + "    public static boolean isAtLeast(int api) {\n"
                        + "        return SDK_INT >= api;\n"
                        + "    }\n"
                        + "\n"
                        + "    public static boolean isAtLeastN() {\n"
                        + "        return BuildCompat.isAtLeastN();\n"
                        + "    }\n"
                        + "\n"
                        + "\n"
                        + "    @RequiresApi(M)\n"
                        + "    public boolean methodM() {\n"
                        + "        return true;\n"
                        + "    }\n"
                        + "\n"
                        + "    @RequiresApi(N)\n"
                        + "    public boolean methodN() {\n"
                        + "        return true;\n"
                        + "    }\n"
                        + "\n"
                        + "    @RequiresApi(N_MR1)\n"
                        + "    public boolean methodN_MR1() {\n"
                        + "        return true;\n"
                        + "    }\n"
                        + "}\n"),
                jar("libs/build-compat.jar",
                        base64gzip("android/support/v4/os/BuildCompat.class", ""
                                + "H4sIAAAAAAAAAIWSy07CQBSG/ymFCpaLeANvCdGFurAhutMYFTUhcknAsHBj"
                                + "Bmh0sLRNL7yPKzdudGPiwgfwoYynFZAYjbPoucz//T3tzPvH6xuAXawlICMb"
                                + "xyzmFMwrWGCIHQhTeIcMkc2tFoNcsro6Q7oiTL3m99u6c8nbBnWyFavDjRZ3"
                                + "RFAPm7J3K1yG9Qo3u44luprr27bleNpgT7Nc7cQXRrdk9W3u7TMkhHvsVXTu"
                                + "erXwbVcMyabHO3dVbg/9kt+SaqNISNPynY5+LoLNzITdTo8PuIooYgpyKpax"
                                + "wlD4dwjyCDjN4OaNVm/39I6nYJUhNyJH8o3WWaNZrtOcyjhTy6apOyWDu65O"
                                + "36w0Ty+uy7VLBlYm358OKCBC/zpYEUjBoCAmPAQiKEa3X8CeKJEwRc8ERZBI"
                                + "JlGcMvVLRP1pinGqk0ODYqgEUs+QMrl7KPID5Mjj2CkWkgmkQl5Sjxjh6d/x"
                                + "/F94ahKXkAm3Z7AUOjO6P4vII/4Jinenz1gCAAA=")),
                mSupportJar)
                .run().expect(""
                        + "src/test/pkg/VersionConditionals4.java:16: Error: Call requires API level 24 (current min is 4): methodN [NewApi]\n"
                        + "        if (methodN() || SDK_INT < N) { } // ERROR\n"
                        + "            ~~~~~~~\n"
                        + "src/test/pkg/VersionConditionals4.java:24: Error: Call requires API level 24 (current min is 4): methodN [NewApi]\n"
                        + "        if (isAtLeast(10)) { methodN(); } // ERROR\n"
                        + "                             ~~~~~~~\n"
                        + "src/test/pkg/VersionConditionals4.java:25: Error: Call requires API level 24 (current min is 4): methodN [NewApi]\n"
                        + "        if (isAtLeast(23)) { methodN(); } // ERROR\n"
                        + "                             ~~~~~~~\n"
                        + "src/test/pkg/VersionConditionals4.java:30: Error: Call requires API level 25 (current min is 4): methodN_MR1 [NewApi]\n"
                        + "        if (BuildCompat.isAtLeastN()) { methodN_MR1(); } // ERROR\n"
                        + "                                        ~~~~~~~~~~~\n"
                        + "4 errors, 0 warnings\n");
    }

    public void testConditionalApi5() {
        // Regression test for
        //   -- https://code.google.com/p/android/issues/detail?id=212170
        //   -- https://code.google.com/p/android/issues/detail?id=199041
        // Handle version checks in conditionals.
        //noinspection all // Sample code

        lint().files(
                manifest().minSdk(4),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.Manifest;\n"
                        + "import android.app.Activity;\n"
                        + "import android.app.ActivityOptions;\n"
                        + "import android.content.Intent;\n"
                        + "import android.content.pm.PackageManager;\n"
                        + "import android.os.Build.VERSION;\n"
                        + "import android.os.Build.VERSION_CODES;\n"
                        + "import android.view.View;\n"
                        + "\n"
                        + "public class VersionConditionals5 extends Activity {\n"
                        + "    public boolean test() {\n"
                        + "        return VERSION.SDK_INT < 23\n"
                        + "                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;\n"
                        + "    }\n"
                        + "\n"
                        + "    public static void startActivity(final Activity activity, View searchCardView) {\n"
                        + "        final Intent intent = new Intent(activity, VersionConditionals5.class);\n"
                        + "        if (VERSION.SDK_INT < VERSION_CODES.LOLLIPOP || searchCardView == null)\n"
                        + "            activity.startActivity(intent);\n"
                        + "        else {\n"
                        + "            final String transitionName = activity.getString(android.R.string.ok);\n"
                        + "            searchCardView.setTransitionName(transitionName);\n"
                        + "            final ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(activity,\n"
                        + "                    searchCardView, transitionName);\n"
                        + "            activity.startActivity(intent, options.toBundle());\n"
                        + "            activity.getWindow().getSharedElementExitTransition().setDuration(100);\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"))
                .run().expect("No warnings.");
    }

    public void testConditionalApi6() {
        // Regression test for https://code.google.com/p/android/issues/detail?id=207289

        //noinspection all // Sample code

        lint().files(
                manifest().minSdk(4),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.animation.*;\n"
                        + "import android.os.Build;\n"
                        + "import android.view.View;\n"
                        + "\n"
                        + "class Test {\n"
                        + "    View mSelection;\n"
                        + "    void f() {\n"
                        + "        final View flashView = mSelection;\n"
                        + "        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {\n"
                        + "            ObjectAnimator whiteFlashIn = ObjectAnimator.ofObject(flashView,\n"
                        + "                    \"backgroundColor\", new ArgbEvaluator(), 0x00FFFFFF, 0xAAFFFFFF);\n"
                        + "            ObjectAnimator whiteFlashOut = ObjectAnimator.ofObject(flashView,\n"
                        + "                    \"backgroundColor\", new ArgbEvaluator(), 0xAAFFFFFF, 0x00000000);\n"
                        + "            whiteFlashIn.setDuration(200);\n"
                        + "            whiteFlashOut.setDuration(300);\n"
                        + "            AnimatorSet whiteFlash = new AnimatorSet();\n"
                        + "            whiteFlash.playSequentially(whiteFlashIn, whiteFlashOut);\n"
                        + "            whiteFlash.addListener(new AnimatorListenerAdapter() {\n"
                        + "                @SuppressWarnings(\"deprecation\")\n"
                        + "                @Override public void onAnimationEnd(Animator animation) {\n"
                        + "                    flashView.setBackgroundDrawable(null);\n"
                        + "                }\n"
                        + "            });\n"
                        + "            whiteFlash.start();\n"
                        + "        }\n"
                        + "    }\n"
                        + "}"))
                .run().expectClean();
    }

    public void testConditionalOnConstant() {
        // Regression test for https://code.google.com/p/android/issues/detail?id=221586

        //noinspection all // Sample code

        lint().files(
                manifest().minSdk(4),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.os.Build;\n"
                        + "import android.widget.TextView;\n"
                        + "\n"
                        + "public class VersionConditionals6 extends Activity {\n"
                        + "    public static final boolean SUPPORTS_LETTER_SPACING = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;\n"
                        + "\n"
                        + "    public void test(TextView textView) {\n"
                        + "        if (SUPPORTS_LETTER_SPACING) {\n"
                        + "            textView.setLetterSpacing(1f); // OK\n"
                        + "        }\n"
                        + "        textView.setLetterSpacing(1f); // ERROR\n"
                        + "    }\n"
                        + "}\n"))
                .run().expect(""
                        + "src/test/pkg/VersionConditionals6.java:14: Error: Call requires API level 21 (current min is 4): android.widget.TextView#setLetterSpacing [NewApi]\n"
                        + "        textView.setLetterSpacing(1f); // ERROR\n"
                        + "                 ~~~~~~~~~~~~~~~~\n"
                        + "1 errors, 0 warnings\n");
    }

    public void testVersionCheckMethodsInBinaryOperator() {
        // Regression test for https://code.google.com/p/android/issues/detail?id=199572
        //noinspection all // Sample code

        lint().files(
                manifest().minSdk(10),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.content.Context;\n"
                        + "import android.hardware.camera2.CameraAccessException;\n"
                        + "import android.hardware.camera2.CameraManager;\n"
                        + "import android.os.Build;\n"
                        + "\n"
                        + "public class VersionConditionals8 extends Activity {\n"
                        + "    private boolean mDebug;\n"
                        + "    \n"
                        + "    public void testCamera() {\n"
                        + "        if (isLollipop() && mDebug) {\n"
                        + "            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);\n"
                        + "            try {\n"
                        + "                int length = manager.getCameraIdList().length;\n"
                        + "            } catch (Throwable ignore) {\n"
                        + "            }\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private boolean isLollipop() {\n"
                        + "        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;\n"
                        + "    }\n"
                        + "}\n"))
                .run().expectClean();
    }

    public void testTernaryOperator() {
        //noinspection all // Sample code

        lint().files(
                manifest().minSdk(10),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.os.Build;\n"
                        + "import android.view.View;\n"
                        + "import android.widget.GridLayout;\n"
                        + "\n"
                        + "public class TestTernaryOperator {\n"
                        + "    public View getLayout1() {\n"
                        + "        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH \n"
                        + "                ? new GridLayout(null) : null;\n"
                        + "    }\n"
                        + "\n"
                        + "    public View getLayout2() {\n"
                        + "        return Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH\n"
                        + "                ? null : new GridLayout(null);\n"
                        + "    }\n"
                        + "}\n"))
                .run().expectClean();
    }

    public void testVersionInVariable() {
        // Regression test for b/35116007:
        // Allow the SDK version to be extracted into a variable or field
        //noinspection all // Sample code

        lint().files(
                manifest().minSdk(10),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.os.Build;\n"
                        + "import android.view.View;\n"
                        + "import android.widget.GridLayout;\n"
                        + "\n"
                        + "public class TestVersionInVariable {\n"
                        + "    private static final int STASHED_VERSION = Build.VERSION.SDK_INT;\n"
                        + "    public void getLayout1() {\n"
                        + "        final int version = Build.VERSION.SDK_INT;\n"
                        + "        if (version >= 14) {"
                        + "            new GridLayout(null);\n"
                        + "        }\n"
                        + "        if (STASHED_VERSION >= 14) {\n"
                        + "            new GridLayout(null);\n"
                        + "        }\n"
                        + "    }\n"
                        + "}\n"))
                .run().expectClean();
    }

    public void testNegative() {
        //noinspection all // Sample code

        lint().files(
                manifest().minSdk(10),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.app.Activity;\n"
                        + "import android.content.Context;\n"
                        + "import android.hardware.camera2.CameraAccessException;\n"
                        + "import android.hardware.camera2.CameraManager;\n"
                        + "import android.os.Build;\n"
                        + "\n"
                        + "public class Negative extends Activity {\n"
                        + "    public void testNegative1() throws CameraAccessException {\n"
                        + "        if (!isLollipop()) {\n"
                        + "        } else {\n"
                        + "            ((CameraManager) getSystemService(Context.CAMERA_SERVICE)).getCameraIdList();\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    public void testReversedOperator() throws CameraAccessException {\n"
                        + "        if (Build.VERSION_CODES.LOLLIPOP <= Build.VERSION.SDK_INT) {\n"
                        + "            ((CameraManager) getSystemService(Context.CAMERA_SERVICE)).getCameraIdList();\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private boolean isLollipop() {\n"
                        + "        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;\n"
                        + "    }\n"
                        + "}\n"))
                .run().expectClean();
    }

    public void testPrecededBy() {
        //noinspection all // Sample code

        lint().files(
                manifest().minSdk(10),
                java(""
                        + "package test.pkg;\n"
                        + "\n"
                        + "import android.os.Build;\n"
                        + "import android.support.annotation.RequiresApi;\n"
                        + "\n"
                        + "@SuppressWarnings({\"WeakerAccess\", \"unused\"})\n"
                        + "public class TestPrecededByVersionCheck {\n"
                        + "    @RequiresApi(22)\n"
                        + "    public boolean requiresLollipop() {\n"
                        + "        return true;\n"
                        + "    }\n"
                        + "\n"
                        + "    public void test1() {\n"
                        + "        if (Build.VERSION.SDK_INT < 22) {\n"
                        + "            return;\n"
                        + "        }\n"
                        + "        requiresLollipop(); // OK\n"
                        + "    }\n"
                        + "\n"
                        + "    public void test2() {\n"
                        + "        if (Build.VERSION.SDK_INT < 18) {\n"
                        + "            return;\n"
                        + "        }\n"
                        + "        requiresLollipop(); // ERROR: API level could be 18-21\n"
                        + "    }\n"
                        + "\n"
                        + "    public void test3() {\n"
                        + "        requiresLollipop(); // ERROR: Version check is after\n"
                        + "        if (Build.VERSION.SDK_INT < 22) {\n"
                        + "            return;\n"
                        + "        }\n"
                        + "        requiresLollipop(); // OK\n"
                        + "    }\n"
                        + "\n"
                        + "    public void test4() {\n"
                        + "        if (Build.VERSION.SDK_INT > 22) {\n"
                        + "            return;\n"
                        + "        }\n"
                        + "        requiresLollipop(); // ERROR: Version check is going in the wrong direction: API can be 1\n"
                        + "    }\n"
                        + "\n"
                        + "    public void test5() {\n"
                        + "        if (Build.VERSION.SDK_INT > 22) {\n"
                        + "            // Something\n"
                        + "        } else {\n"
                        + "            return;\n"
                        + "        }\n"
                        + "        requiresLollipop(); // OK\n"
                        + "    }\n"
                        + "\n"
                        + "    public void test6() {\n"
                        + "        if (Build.VERSION.SDK_INT > 18) {\n"
                        + "            // Something\n"
                        + "        } else {\n"
                        + "            return;\n"
                        + "        }\n"
                        + "        requiresLollipop(); // ERROR: API level can be less than 22\n"
                        + "    }\n"
                        + "\n"
                        + "    public void test7() {\n"
                        + "        if (Build.VERSION.SDK_INT <= 22) {\n"
                        + "            // Something\n"
                        + "        } else {\n"
                        + "            return;\n"
                        + "        }\n"
                        + "        requiresLollipop(); // ERROR: API level can be less than 22\n"
                        + "    }\n"
                        + "}\n"),
                mSupportJar)
                .run().expect(""
                        + "src/test/pkg/TestPrecededByVersionCheck.java:24: Error: Call requires API level 22 (current min is 10): requiresLollipop [NewApi]\n"
                        + "        requiresLollipop(); // ERROR: API level could be 18-21\n"
                        + "        ~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/TestPrecededByVersionCheck.java:28: Error: Call requires API level 22 (current min is 10): requiresLollipop [NewApi]\n"
                        + "        requiresLollipop(); // ERROR: Version check is after\n"
                        + "        ~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/TestPrecededByVersionCheck.java:39: Error: Call requires API level 22 (current min is 10): requiresLollipop [NewApi]\n"
                        + "        requiresLollipop(); // ERROR: Version check is going in the wrong direction: API can be 1\n"
                        + "        ~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/TestPrecededByVersionCheck.java:57: Error: Call requires API level 22 (current min is 10): requiresLollipop [NewApi]\n"
                        + "        requiresLollipop(); // ERROR: API level can be less than 22\n"
                        + "        ~~~~~~~~~~~~~~~~\n"
                        + "src/test/pkg/TestPrecededByVersionCheck.java:66: Error: Call requires API level 22 (current min is 10): requiresLollipop [NewApi]\n"
                        + "        requiresLollipop(); // ERROR: API level can be less than 22\n"
                        + "        ~~~~~~~~~~~~~~~~\n"
                        + "5 errors, 0 warnings\n");
    }

    public void testNestedChecks() {
        //noinspection all // Sample code

        lint().files(
                manifest().minSdk(11),
                java(""
                        + "package p1.p2;\n"
                        + "\n"
                        + "import android.os.Build;\n"
                        + "import android.widget.GridLayout;\n"
                        + "\n"
                        + "public class Class {\n"
                        + "    public void testEarlyExit1() {\n"
                        + "        // https://code.google.com/p/android/issues/detail?id=37728\n"
                        + "        if (Build.VERSION.SDK_INT < 14) return;\n"
                        + "\n"
                        + "        new GridLayout(null); // OK\n"
                        + "    }\n"
                        + "\n"
                        + "    public void testEarlyExit2() {\n"
                        + "        if (!Utils.isIcs()) {\n"
                        + "            return;\n"
                        + "        }\n"
                        + "\n"
                        + "        new GridLayout(null); // OK\n"
                        + "    }\n"
                        + "\n"
                        + "    public void testEarlyExit3(boolean nested) {\n"
                        + "        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {\n"
                        + "            return;\n"
                        + "        }\n"
                        + "\n"
                        + "        if (nested) {\n"
                        + "            new GridLayout(null); // OK\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    public void testEarlyExit4(boolean nested) {\n"
                        + "        if (nested) {\n"
                        + "            if (Utils.isIcs()) {\n"
                        + "                return;\n"
                        + "            }\n"
                        + "        }\n"
                        + "\n"
                        + "        new GridLayout(null); // ERROR\n"
                        + "\n"
                        + "        if (Utils.isIcs()) { // too late\n"
                        + "            //noinspection UnnecessaryReturnStatement\n"
                        + "            return;\n"
                        + "        }\n"
                        + "    }\n"
                        + "\n"
                        + "    private static class Utils {\n"
                        + "        public static boolean isIcs() {\n"
                        + "            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;\n"
                        + "        }\n"
                        + "        public static boolean isGingerbread() {\n"
                        + "            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;\n"
                        + "        }\n"
                        + "    }\n"
                        + "}"))
                .run().expect(""
                        + "src/p1/p2/Class.java:39: Error: Call requires API level 14 (current min is 11): new android.widget.GridLayout [NewApi]\n"
                        + "        new GridLayout(null); // ERROR\n"
                        + "        ~~~~~~~~~~~~~~\n"
                        + "src/p1/p2/Class.java:52: Warning: Unnecessary; SDK_INT is always >= 11 [ObsoleteSdkInt]\n"
                        + "            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;\n"
                        + "                   ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n"
                        + "1 errors, 1 warnings\n");
    }

    public void testNestedChecksKotlin() {
        // Kotlin version of testNestedChecks. There are several important changes here:
        // The version check utility method is now defined as an expression body, so there
        // is no explicit "return" keyword (which the code used to look for).
        // Second, we're accessing the version check using property syntax, not a call, which
        // also required changes to the AST analysis.

        //noinspection all // Sample code
        lint().files(
                manifest().minSdk(11),
                kotlin("" +
                        "package p1.p2\n" +
                        "\n" +
                        "import android.os.Build\n" +
                        "import android.widget.GridLayout\n" +
                        "\n" +
                        "class NestedChecks {\n" +
                        "    fun testEarlyExit1() {\n" +
                        "        // https://code.google.com/p/android/issues/detail?id=37728\n" +
                        "        if (Build.VERSION.SDK_INT < 14) return\n" +
                        "\n" +
                        "        GridLayout(null) // OK\n" +
                        "    }\n" +
                        "\n" +
                        "    fun testEarlyExit2() {\n" +
                        "        if (!Utils.isIcs) {\n" +
                        "            return\n" +
                        "        }\n" +
                        "\n" +
                        "        GridLayout(null) // OK\n" +
                        "    }\n" +
                        "\n" +
                        "    fun testEarlyExit3(nested: Boolean) {\n" +
                        "        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {\n" +
                        "            return\n" +
                        "        }\n" +
                        "\n" +
                        "        if (nested) {\n" +
                        "            GridLayout(null) // OK\n" +
                        "        }\n" +
                        "    }\n" +
                        "\n" +
                        "    fun testEarlyExit4(nested: Boolean) {\n" +
                        "        if (nested) {\n" +
                        "            if (Utils.isIcs) {\n" +
                        "                return\n" +
                        "            }\n" +
                        "        }\n" +
                        "\n" +
                        "        GridLayout(null) // ERROR\n" +
                        "\n" +
                        "        if (Utils.isIcs) { // too late\n" +
                        "\n" +
                        "            return\n" +
                        "        }\n" +
                        "    }\n" +
                        "\n" +
                        "    private object Utils {\n" +
                        "        val isIcs: Boolean\n" +
                        "            get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH\n" +
                        "        val isGingerbread: Boolean\n" +
                        "            get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD\n" +
                        "    }\n" +
                        "}"))
                .run().expect("" +
                "src/p1/p2/NestedChecks.kt:39: Error: Call requires API level 14 (current min is 11): new android.widget.GridLayout [NewApi]\n" +
                "        GridLayout(null) // ERROR\n" +
                "        ~~~~~~~~~~~~~~~~\n" +
                "src/p1/p2/NestedChecks.kt:51: Warning: Unnecessary; SDK_INT is always >= 11 [ObsoleteSdkInt]\n" +
                "            get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD\n" +
                "                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                "1 errors, 1 warnings\n");
    }

    public void testGetMinSdkVersionFromMethodName() {
        assertEquals(19, VersionChecks.getMinSdkVersionFromMethodName("isAtLeastKitKat"));
        assertEquals(19, VersionChecks.getMinSdkVersionFromMethodName("isKitKatSdk"));
        assertEquals(19, VersionChecks.getMinSdkVersionFromMethodName("isKitKatSDK"));
        assertEquals(19, VersionChecks.getMinSdkVersionFromMethodName("isRunningKitkatOrLater"));
        assertEquals(19, VersionChecks.getMinSdkVersionFromMethodName("isKeyLimePieOrLater"));
        assertEquals(19, VersionChecks.getMinSdkVersionFromMethodName("isKitKatOrHigher"));
        assertEquals(19, VersionChecks.getMinSdkVersionFromMethodName("isKitKatOrNewer"));

        assertEquals(17, VersionChecks.getMinSdkVersionFromMethodName("isRunningJellyBeanMR1OrLater"));
        assertEquals(20, VersionChecks.getMinSdkVersionFromMethodName("isAtLeastKitKatWatch"));
    }

    public void testVersionNameFromMethodName() {
        //noinspection all // Sample code
        lint().files(
                java("" +
                        "package test.pkg;\n" +
                        "\n" +
                        "import android.content.pm.ShortcutManager;\n" +
                        "\n" +
                        "public abstract class VersionCheck {\n" +
                        "    public void test(ShortcutManager shortcutManager) {\n" +
                        "        // this requires API 26\n" +
                        "        if (isAtLeastOreo()) {\n" +
                        "            shortcutManager.removeAllDynamicShortcuts();\n" +
                        "        }\n" +
                        "        if (isOreoOrLater()) {\n" +
                        "            shortcutManager.removeAllDynamicShortcuts();\n" +
                        "        }\n" +
                        "        if (isOreoOrAbove()) {\n" +
                        "            shortcutManager.removeAllDynamicShortcuts();\n" +
                        "        }\n" +
                        "    }\n" +
                        "\n" +
                        "    public abstract boolean isAtLeastOreo();\n" +
                        "    public abstract boolean isOreoOrLater();\n" +
                        "    public abstract boolean isOreoOrAbove();\n" +
                        "}\n"))
                .run()
                .expectClean();
    }

    public void testKotlinWhenStatement() {
        // Regression test for
        //   67712955: Kotlin when statement fails if subject is Build.VERSION.SDK_INT

        //noinspection all // Sample code
        lint().files(
                manifest().minSdk(4),
                kotlin("" +
                        "import android.os.Build.VERSION.SDK_INT\n" +
                        "import android.os.Build.VERSION_CODES.N\n" +
                        "import android.text.Html\n" +
                        "\n" +
                        "fun String.fromHtm() : String\n" +
                        "{\n" +
                        "    return when {\n" +
                        "        false, SDK_INT >= N -> Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY)\n" +
                        "        else -> Html.fromHtml(this)\n" +
                        "    }.toString()\n" +
                        "}"))
                .run()
                .expectClean();
    }

    public void testKotlinWhenStatement2() {
        // Regression test for issue 69661204
        lint().files(
                kotlin("" +
                        "package test.pkg\n" +
                        "\n" +
                        "import android.os.Build\n" +
                        "import android.support.annotation.RequiresApi\n" +
                        "\n" +
                        "@RequiresApi(21)\n" +
                        "fun requires21() { }\n" +
                        "\n" +
                        "@RequiresApi(23)\n" +
                        "fun requires23() { }\n" +
                        "\n" +
                        "fun requiresNothing() { }\n" +
                        "\n" +
                        "fun test() {\n" +
                        "    when {\n" +
                        "        Build.VERSION.SDK_INT >= 21 -> requires21()\n" +
                        "        Build.VERSION.SDK_INT >= 23 -> requires23()\n" +
                        "        else -> requiresNothing()\n" +
                        "    }\n" +
                        "}\n"),
                mSupportJar)
                .run()
                .expectClean();
    }

    public void testKotlinHelper() {
        // Regression test for issue 64550633
        lint().files(
                kotlin("" +
                        "package test.pkg\n" +
                        "\n" +
                        "import android.os.Build\n" +
                        "import android.os.Build.VERSION_CODES.KITKAT\n" +
                        "\n" +
                        "inline fun fromApi(value: Int, action: () -> Unit) {\n" +
                        "    if (Build.VERSION.SDK_INT >= value) {\n" +
                        "        action()\n" +
                        "    }\n" +
                        "}\n" +
                        "\n" +
                        "fun fromApiNonInline(value: Int, action: () -> Unit) {\n" +
                        "    if (Build.VERSION.SDK_INT >= value) {\n" +
                        "        action()\n" +
                        "    }\n" +
                        "}\n" +
                        "\n" +
                        "inline fun notFromApi(value: Int, action: () -> Unit) {\n" +
                        "    if (Build.VERSION.SDK_INT < value) {\n" +
                        "        action()\n" +
                        "    }\n" +
                        "}\n" +
                        "\n" +
                        "fun test1() {\n" +
                        "    fromApi(KITKAT) {\n" +
                        "        // Example of a Java 7+ field\n" +
                        "        val cjkExtensionC = Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C // OK\n" +
                        "    }\n" +
                        "}\n" +
                        "\n" +
                        "fun test2() {\n" +
                        "    fromApiNonInline(KITKAT) {\n" +
                        "        val cjkExtensionC = Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C // OK\n" +
                        "    }\n" +
                        "}\n" +
                        "\n" +
                        "fun test3() {\n" +
                        "    notFromApi(KITKAT) {\n" +
                        "        val cjkExtensionC = Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C // ERROR\n" +
                        "    }\n" +
                        "}\n" +
                        "\n"))
                .run()
                .expect("src/test/pkg/test.kt:39: Error: Field requires API level 19 (current min is 1): java.lang.Character.UnicodeBlock#CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C [NewApi]\n" +
                        "        val cjkExtensionC = Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C // ERROR\n" +
                        "                            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                        "1 errors, 0 warnings");
    }

    @Override
    protected Detector getDetector() {
        return new ApiDetector();
    }

    private TestFile mSupportJar = base64gzip(ApiDetectorTest.SUPPORT_JAR_PATH,
            SUPPORT_ANNOTATIONS_JAR_BASE64_GZIP);
}