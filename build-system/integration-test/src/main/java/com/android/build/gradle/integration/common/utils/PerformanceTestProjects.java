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

package com.android.build.gradle.integration.common.utils;

import com.android.SdkConstants;
import com.android.build.gradle.integration.common.fixture.GradleTestProject;
import com.android.builder.core.AndroidBuilder;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PerformanceTestProjects {

    private static String generateLocalRepositoriesSnippet() {
        StringBuilder localRepositoriesSnippet = new StringBuilder();
        for (Path repo : GradleTestProject.getLocalRepositories()) {
            localRepositoriesSnippet.append(GradleTestProject.mavenSnippet(repo));
        }
        return localRepositoriesSnippet.toString();
    }

    public static void initializeAntennaPod(GradleTestProject mainProject) throws IOException {
        GradleTestProject project = mainProject.getSubproject("AntennaPod");

        Files.move(
                mainProject.file(SdkConstants.FN_LOCAL_PROPERTIES).toPath(),
                project.file(SdkConstants.FN_LOCAL_PROPERTIES).toPath(),
                StandardCopyOption.REPLACE_EXISTING);

        TestFileUtils.searchAndReplace(
                project.getBuildFile(),
                "classpath \"com.android.tools.build:gradle:\\d+.\\d+.\\d+\"",
                "classpath \"com.android.tools.build:gradle:"
                        + GradleTestProject.ANDROID_GRADLE_PLUGIN_VERSION
                        + '"');



        TestFileUtils.searchAndReplace(
                project.getBuildFile(), "jcenter\\(\\)", generateLocalRepositoriesSnippet());

        TestFileUtils.searchAndReplace(
                project.getBuildFile(),
                "buildToolsVersion = \".*\"",
                "buildToolsVersion = \"" + GradleTestProject.DEFAULT_BUILD_TOOL_VERSION
                        + "\" // Updated by test");

        List<String> subprojects =
                ImmutableList.of("AudioPlayer/library", "afollestad/commons", "afollestad/core");

        for (String subproject: subprojects) {
            TestFileUtils.searchAndReplace(
                    mainProject.getSubproject(subproject).getBuildFile(),
                    "buildToolsVersion \".*\"",
                    "buildToolsVersion \"" + GradleTestProject.DEFAULT_BUILD_TOOL_VERSION
                            + "\" // Updated by test");
        }

        // Update the support lib and fix resulting issue:
        List<File> filesWithSupportLibVersion =
                ImmutableList.of(
                        project.getBuildFile(),
                        mainProject.file("afollestad/core/build.gradle"),
                        mainProject.file("afollestad/commons/build.gradle"));

        for (File buildFile : filesWithSupportLibVersion) {
            TestFileUtils.searchAndReplace(
                    buildFile,
                    " 23",
                    " " + GradleTestProject.DEFAULT_COMPILE_SDK_VERSION);

            TestFileUtils.searchAndReplace(
                    buildFile,
                    "23.1.1",
                    GradleTestProject.SUPPORT_LIB_VERSION);
        }

        TestFileUtils.searchAndReplace(
                mainProject.file("afollestad/core/src/main/res/values-v11/styles.xml"),
                "abc_ic_ab_back_mtrl_am_alpha",
                "abc_ic_ab_back_material");
    }


    public static void initializeWordpress(GradleTestProject project) throws IOException {

        Files.copy(
                project.file("WordPress/gradle.properties-example").toPath(),
                project.file("WordPress/gradle.properties").toPath());


        String localRepositoriesSnippet = generateLocalRepositoriesSnippet();

        TestFileUtils.appendToFile(
                project.getBuildFile(),
                String.format(
                        "buildscript {\n"
                                + "    repositories {\n"
                                + "        %1$s\n"
                                + "    }\n"
                                + "    dependencies {\n"
                                + "        classpath 'com.android.tools.build:gradle:%2$s'\n"
                                + "    }\n"
                                + "}\n",
                        localRepositoriesSnippet, GradleTestProject.ANDROID_GRADLE_PLUGIN_VERSION));

        List<Path> buildGradleFiles =
                Stream.of(
                        "WordPress/build.gradle",
                        "libs/utils/WordPressUtils/build.gradle",
                        "libs/editor/example/build.gradle",
                        "libs/editor/WordPressEditor/build.gradle",
                        "libs/networking/WordPressNetworking/build.gradle",
                        "libs/analytics/WordPressAnalytics/build.gradle")
                        .map(name -> project.file(name).toPath())
                        .collect(Collectors.toList());

        TestFileUtils.searchAndReplace(
                project.file("WordPress/build.gradle"),
                "maven \\{ url (('.*')|(\".*\")) \\}",
                "");

        for (Path file : buildGradleFiles) {
            TestFileUtils.searchAndReplace(
                    file, "classpath 'com\\.android\\.tools\\.build:gradle:\\d+.\\d+.\\d+'", "");

            TestFileUtils.searchAndReplace(
                    file,
                    "jcenter\\(\\)",
                    localRepositoriesSnippet);

            TestFileUtils.searchAndReplace(
                    file,
                    "buildToolsVersion \"[^\"]+\"",
                    String.format("buildToolsVersion \"%s\"", AndroidBuilder.MIN_BUILD_TOOLS_REV));
        }


        // Remove crashlytics
        TestFileUtils.searchAndReplace(
                project.file("WordPress/build.gradle"),
                "classpath 'io.fabric.tools:gradle:1.+'",
                "");
        TestFileUtils.searchAndReplace(
                project.file("WordPress/build.gradle"),
                "apply plugin: 'io.fabric'",
                "");
        TestFileUtils.searchAndReplace(
                project.file("WordPress/build.gradle"),
                "compile\\('com.crashlytics.sdk.android:crashlytics:2.5.5\\@aar'\\) \\{\n"
                        + "        transitive = true;\n"
                        + "    \\}",
                "");

        TestFileUtils.searchAndReplace(
                project.file("WordPress/src/main/java/org/wordpress/android/util/CrashlyticsUtils.java"),
                "\n     ",
                "\n     //"
        );

        TestFileUtils.searchAndReplace(
                project.file("WordPress/src/main/java/org/wordpress/android/util/CrashlyticsUtils.java"),
                "\nimport",
                "\n// import"
        );

        TestFileUtils.searchAndReplace(
                project.file("WordPress/src/main/java/org/wordpress/android/WordPress.java"),
                "\n(.*Fabric)",
                "\n// $1");
        TestFileUtils.searchAndReplace(
                project.file("WordPress/src/main/java/org/wordpress/android/WordPress.java"),
                "import com\\.crashlytics\\.android\\.Crashlytics;",
                "//import com.crashlytics.android.Crashlytics;");

        //TODO: Upstream some of this?

        // added to force version upgrade.
        TestFileUtils.searchAndReplace(
                project.file("WordPress/build.gradle"),
                "\ndependencies \\{\n",
                "dependencies {\n"
                        + "    compile 'org.jetbrains.kotlin:kotlin-stdlib:1.0.5'\n"
                        + "    compile 'com.android.support:support-v4:" + GradleTestProject.SUPPORT_LIB_VERSION + "'\n");

        TestFileUtils.searchAndReplace(
                project.file("WordPress/build.gradle"),
                "compile 'org.wordpress:drag-sort-listview:0.6.1'",
                "compile ('org.wordpress:drag-sort-listview:0.6.1') {\n"
                        + "    exclude group:'com.android.support'\n"
                        + "}");
        TestFileUtils.searchAndReplace(
                project.file("WordPress/build.gradle"),
                "compile 'org.wordpress:slidinguppanel:1.0.0'",
                "compile ('org.wordpress:slidinguppanel:1.0.0') {\n"
                        + "    exclude group:'com.android.support'\n"
                        + "}");

        TestFileUtils.searchAndReplace(
                project.file("libs/utils/WordPressUtils/build.gradle"),
                "classpath 'com\\.novoda:bintray-release:0\\.3\\.4'",
                "");
        TestFileUtils.searchAndReplace(
                project.file("libs/utils/WordPressUtils/build.gradle"),
                "apply plugin: 'com\\.novoda\\.bintray-release'",
                "");
        TestFileUtils.searchAndReplace(
                project.file("libs/utils/WordPressUtils/build.gradle"),
                "publish \\{[\\s\\S]*\\}",
                "");

        TestFileUtils.searchAndReplace(
                project.file("libs/editor/WordPressEditor/build.gradle"),
                "\ndependencies \\{\n",
                "dependencies {\n"
                        + "    compile 'com.android.support:support-v13:" + GradleTestProject.SUPPORT_LIB_VERSION + "'\n");

        TestFileUtils.searchAndReplace(
                project.file("libs/networking/WordPressNetworking/build.gradle"),
                "maven \\{ url 'http://wordpress-mobile\\.github\\.io/WordPress-Android' \\}",
                "");

        List<Path> useEditor =
                Stream.of(
                        "libs/editor/WordPressEditor/build.gradle",
                        "libs/networking/WordPressNetworking/build.gradle",
                        "libs/analytics/WordPressAnalytics/build.gradle")
                        .map(name -> project.file(name).toPath())
                        .collect(Collectors.toList());
        for (Path path: useEditor) {
            TestFileUtils.searchAndReplace(
                    path,
                    "compile 'org\\.wordpress:utils:1\\.11\\.0'",
                    "releaseCompile "
                            + "project(path:':libs:utils:WordPressUtils', configuration: 'release')\n"
                            + "    debugCompile "
                            + "project(path:':libs:utils:WordPressUtils', configuration: 'debug')\n");
        }

        // There is an extraneous BOM in the values-ja/strings.xml
        Files.copy(
                project.file("WordPress/src/main/res/values-en-rCA/strings.xml").toPath(),
                project.file("WordPress/src/main/res/values-ja/strings.xml").toPath(),
                StandardCopyOption.REPLACE_EXISTING);

        List<File> filesWithSupportLibVersion =
                ImmutableList.of(
                        project.file("WordPress/build.gradle"),
                        project.file("libs/editor/WordPressEditor/build.gradle"),
                        project.file("libs/utils/WordPressUtils/build.gradle"));

        // Replace support lib version
        for (File buildFile : filesWithSupportLibVersion) {
            TestFileUtils.searchAndReplace(
                    buildFile,
                    "24.2.1",
                    GradleTestProject.SUPPORT_LIB_VERSION);
        }

        TestFileUtils.searchAndReplace(
                project.file("WordPress/build.gradle"),
                "9.0.2",
                GradleTestProject.PLAY_SERVICES_VERSION);


    }
}
