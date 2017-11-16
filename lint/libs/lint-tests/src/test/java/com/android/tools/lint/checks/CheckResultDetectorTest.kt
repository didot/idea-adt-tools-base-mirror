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

package com.android.tools.lint.checks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import junit.framework.TestCase
import java.io.File

class aCheckResultDetectorTest : AbstractCheckTest() {
    override fun getDetector(): Detector = CheckResultDetector()

    fun testCheckResult() {
        val expected =
                """
src/test/pkg/CheckPermissions.java:22: Warning: The result of extractAlpha is not used [CheckResult]
        bitmap.extractAlpha(); // WARNING
        ~~~~~~~~~~~~~~~~~~~~~
src/test/pkg/Intersect.java:7: Warning: The result of intersect is not used. If the rectangles do not intersect, no change is made and the original rectangle is not modified. These methods return false to indicate that this has happened. [CheckResult]
    rect.intersect(aLeft, aTop, aRight, aBottom);
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/test/pkg/CheckPermissions.java:10: Warning: The result of checkCallingOrSelfPermission is not used; did you mean to call #enforceCallingOrSelfPermission(String,String)? [UseCheckPermission]
        context.checkCallingOrSelfPermission(Manifest.permission.INTERNET); // WRONG
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/test/pkg/CheckPermissions.java:11: Warning: The result of checkPermission is not used; did you mean to call #enforcePermission(String,int,int,String)? [UseCheckPermission]
        context.checkPermission(Manifest.permission.INTERNET, 1, 1);
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 4 warnings
"""
        lint().files(
                java("""
                package test.pkg;

                import android.Manifest;
                import android.content.Context;
                import android.content.pm.PackageManager;
                import android.graphics.Bitmap;

                public class CheckPermissions {
                    private void foo(Context context) {
                        context.checkCallingOrSelfPermission(Manifest.permission.INTERNET); // WRONG
                        context.checkPermission(Manifest.permission.INTERNET, 1, 1);
                        check(context.checkCallingOrSelfPermission(Manifest.permission.INTERNET)); // OK
                        int check = context.checkCallingOrSelfPermission(Manifest.permission.INTERNET); // OK
                        if (context.checkCallingOrSelfPermission(Manifest.permission.INTERNET) // OK
                                != PackageManager.PERMISSION_GRANTED) {
                            showAlert(context, "Error",
                                    "Application requires permission to access the Internet");
                        }
                    }

                    private Bitmap checkResult(Bitmap bitmap) {
                        bitmap.extractAlpha(); // WARNING
                        Bitmap bitmap2 = bitmap.extractAlpha(); // OK
                        call(bitmap.extractAlpha()); // OK
                        return bitmap.extractAlpha(); // OK
                    }

                    private void showAlert(Context context, String error, String s) {
                    }

                    private void check(int i) {
                    }
                    private void call(Bitmap bitmap) {
                    }
                }""").indented(),

                java("src/test/pkg/Intersect.java",
                        """
                package test.pkg;

                import android.graphics.Rect;

                public class Intersect {
                  void check(Rect rect, int aLeft, int aTop, int aRight, int aBottom) {
                    rect.intersect(aLeft, aTop, aRight, aBottom);
                  }
                }""").indented(),
                SUPPORT_ANNOTATIONS_CLASS_PATH,
                SUPPORT_ANNOTATIONS_JAR)
                .issues(CheckResultDetector.CHECK_RESULT, PermissionDetector.CHECK_PERMISSION)
                .run()
                .expect(expected)
    }

    fun testSubtract() {
        // Regression test for https://issuetracker.google.com/69344103:
        // @CanIgnoreReturnValue should let you *undo* a @CheckReturnValue on a class/package
        lint().files(
                java("""
                    package test.pkg;
                    import com.google.errorprone.annotations.CanIgnoreReturnValue;
                    import javax.annotation.CheckReturnValue;

                    @SuppressWarnings({"ClassNameDiffersFromFileName", "MethodMayBeStatic"})
                    @CheckReturnValue
                    public class IgnoreTest {
                        public String method1() {
                            return "";
                        }

                        public void method2() {
                        }

                        @CanIgnoreReturnValue
                        public String method3() {
                            return "";
                        }

                        public void test() {
                            method1(); // ERROR: should check
                            method2(); // OK: void return value
                            method3(); // OK: Specifically allowed
                        }
                    }
                """).indented(),
                java("""
                    package com.google.errorprone.annotations;
                    import java.lang.annotation.Retention;
                    import static java.lang.annotation.RetentionPolicy.CLASS;
                    @SuppressWarnings("ClassNameDiffersFromFileName")
                    @Retention(CLASS)
                    public @interface CanIgnoreReturnValue {}""").indented(),
                java("""
                    package javax.annotation;
                    import static java.lang.annotation.RetentionPolicy.CLASS;
                    import java.lang.annotation.Retention;
                    import javax.annotation.meta.When;
                    @SuppressWarnings("ClassNameDiffersFromFileName")
                    @Retention(CLASS)
                    public @interface CheckReturnValue {
                    }
                    """).indented(),
                SUPPORT_ANNOTATIONS_CLASS_PATH,
                SUPPORT_ANNOTATIONS_JAR)
                .issues(CheckResultDetector.CHECK_RESULT, PermissionDetector.CHECK_PERMISSION)
                .run()
                .expect("src/test/pkg/IgnoreTest.java:21: Warning: The result of method1 is not used [CheckResult]\n" +
                        "        method1(); // ERROR: should check\n" +
                        "        ~~~~~~~~~\n" +
                        "src/test/pkg/IgnoreTest.java:22: Warning: The result of method2 is not used [CheckResult]\n" +
                        "        method2(); // OK: void return value\n" +
                        "        ~~~~~~~~~\n" +
                        "0 errors, 2 warnings")
    }

    fun testSubtract2() {
        // Regression test for https://issuetracker.google.com/69344103
        // Make sure we don't inherit @CheckReturn value from packages
        lint().files(
                java("""
                    package test.pkg;

                    @SuppressWarnings({"ClassNameDiffersFromFileName", "MethodMayBeStatic"})
                    public class IgnoreTest {
                        public String method() {
                            return "";
                        }

                        public void test() {
                            method(); // OK: not inheriting from packages
                        }
                    }
                """).indented(),
                java("" +
                        "@CheckReturnValue\n" +
                        "package test.pkg;\n" +
                        "import javax.annotation.CheckReturnValue;\n"),
                // Also register the compiled version of the above package-info jar file;
                // without this we don't resolve package annotations
                base64gzip("libs/packageinfoclass.jar", "" +
                        "H4sIAAAAAAAAAAvwZmYRYeDg4GDInpfvzYAEOBlYGHxdQxx1Pf3c9P+dYmBg" +
                        "ZgjwZucASTFBlQTg1CwCxHDNvo5+nm6uwSF6vm6ffc+c9vHW1bvI662rde7M" +
                        "+c1BBleMHzwt0vPy1fH0vVi6ioUz4oXkEelZUi/Flj5boia2XCujYuk0C1HV" +
                        "tGei2iKvRV8+zf5U9LGIEeyWtpXBql5Am7xQ3GKK5hZpIC5JLS7RL8hO1y9I" +
                        "TM5OTE/VzcxLy9dLzkksLvb12ct1yEBiT0juVU921vT0sw9eqDRNVgh5Ma/t" +
                        "/CwztaW+R9KLPzDWaGwMFcjf0fy7et87fgZtHiUXwV88hxd/nbpk7qUjBnqt" +
                        "Oi5u3Kl+ZTO7VyfMOHrPonShy1Wtq1sMj1k9nGJqerjmfllezB+ffbaTdZKK" +
                        "9hl1Xph3jUfYfevj1Ikf/1e3BVo/i5rRI39pzpLZTDyM+zgu/CwQ3+t2WI2z" +
                        "0Rzky33aDlPmAv1wAOxLRiYRBtQwh8UGKMJQAUr0oWtFDjwRFG22OCIP2QRQ" +
                        "ICM7TBrFhJP4gzzAm5UNpIwZCI8B6fWMIB4A/Y4BiosCAAA="),
                SUPPORT_ANNOTATIONS_CLASS_PATH,
                SUPPORT_ANNOTATIONS_JAR)
                .issues(CheckResultDetector.CHECK_RESULT, PermissionDetector.CHECK_PERMISSION)
                .run()
                .expectClean()
    }
}