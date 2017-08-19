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

package com.android.tools.lint

import com.android.SdkConstants.ANDROID_URI
import com.android.SdkConstants.ATTR_NAME
import com.android.tools.lint.LintCliFlags.ERRNO_SUCCESS
import com.android.tools.lint.checks.infrastructure.ProjectDescription
import com.android.tools.lint.checks.infrastructure.ProjectDescription.Type.LIBRARY
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.java
import com.android.tools.lint.checks.infrastructure.TestFiles.xml
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.android.tools.lint.client.api.LintDriver
import com.android.tools.lint.client.api.LintListener
import com.android.tools.lint.client.api.LintListener.EventType.REGISTERED_PROJECT
import com.android.tools.lint.client.api.LintListener.EventType.STARTING
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Project
import com.android.utils.XmlUtils.getFirstSubTagByName
import com.google.common.base.Charsets
import com.google.common.io.Files
import com.google.common.truth.Truth.assertThat
import org.intellij.lang.annotations.Language
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ProjectInitializerTest {
    @Test
    fun testManualProject() {

        val library = project(
                xml("AndroidManifest.xml", """
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="foo.bar2"
    android:versionCode="1"
    android:versionName="1.0" >

    <uses-sdk android:minSdkVersion="14" />

    <permission android:name="bar.permission.SEND_SMS"
        android:label="@string/foo"
        android:description="@string/foo" />

    <application
        android:icon="@drawable/ic_launcher"
        android:label="@string/app_name" >
    </application>

</manifest>"""),
                java("src/test/pkg/Loader.java", """
package test.pkg;

@SuppressWarnings("ClassNameDiffersFromFileName")
public abstract class Loader<P> {
    private P mParam;

    public abstract void loadInBackground(P val);

    public void load() {
        // Invoke a method that takes a generic type.
        loadInBackground(mParam);
    }
}"""),
                java("src/test/pkg/NotInProject.java", """
package test.pkg;

@SuppressWarnings("ClassNameDiffersFromFileName")
public class Foo {
    private String foo = "/sdcard/foo";
}
""")

        ).type(LIBRARY).name("Library")

        val main = project(
                xml("AndroidManifest.xml", """
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="foo.bar2"
    android:versionCode="1"
    android:versionName="1.0" >

    <uses-sdk android:minSdkVersion="14" />

    <permission android:name="foo.permission.SEND_SMS"
        android:label="@string/foo"
        android:description="@string/foo" />

    <application
        android:icon="@drawable/ic_launcher"
        android:label="@string/app_name" >
    </application>

</manifest>
"""),
                xml("res/values/strings.xml", """
<resources>
    <string name="string1">String 1</string>
    <string name="string1">String 2</string>
    <string name="string3">String 3</string>
    <string name="string3">String 4</string>
</resources>
"""),
                xml("res/values/not_in_project.xml", """
<resources>
    <string name="string2">String 1</string>
    <string name="string2">String 2</string>
</resources>
""")
        ).name("App").dependsOn(library)

        val root = temp.newFolder()

        val projects = lint().projects(main, library).createProjects(root)
        val appProjectPath = projects[0].path

        val sdk = temp.newFolder("fake-sdk")
        val cacheDir = temp.newFolder("cache")
        @Language("XML")
        val mergedManifestXml = """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="foo.bar2"
    android:versionCode="1"
    android:versionName="1.0" >

    <uses-sdk android:minSdkVersion="14" />

    <permission
        android:name="foo.permission.SEND_SMS"
        android:description="@string/foo"
        android:label="@string/foo" />
    <permission
        android:name="bar.permission.SEND_SMS"
        android:description="@string/foo"
        android:label="@string/foo" />

    <application
        android:icon="@drawable/ic_launcher"
        android:label="@string/app_name" >
    </application>

</manifest>"""

        val mergedManifest = temp.newFile("merged-manifest")
        Files.asCharSink(mergedManifest, Charsets.UTF_8).write(mergedManifestXml)

        @Language("XML")
        val baselineXml = """
<issues format="4" by="lint unknown">
    <issue
        id="DuplicateDefinition"
        message="`string3` has already been defined in this folder"
        errorLine1="    &lt;string name=&quot;string3&quot;>String 4&lt;/string>"
        errorLine2="            ~~~~~~~~~~~~~~">
        <location
            file="res/values/strings.xml"
            line="8"
            column="13"/>
        <location
            file="res/values/strings.xml"
            line="5"
            column="13"/>
    </issue>
</issues>"""
        val baseline = temp.newFile("baseline")
        Files.asCharSink(baseline, Charsets.UTF_8).write(baselineXml)

        @Language("XML")
        val descriptor = """
            <project>
            <root dir="$root" />
            <sdk dir='$sdk'/>
            <cache dir='$cacheDir'/>
            <classpath jar="test.jar" />
            <baseline file='$baseline' />
            <module name="$appProjectPath:App" android="true" library="false" compile-sdk-version='18'>
              <manifest file="AndroidManifest.xml" />
              <resource file="res/values/strings.xml" />
              <dep module="Library" />
            </module>
            <module name="Library" android="true" library="true" compile-sdk-version='android-M'>
              <manifest file="Library/AndroidManifest.xml" />
              <merged-manifest file='$mergedManifest'/>
              <src file="Library/src/test/pkg/Loader.java" />
            </module>
            </project>""".trimIndent()
        Files.asCharSink(File(root, "project.xml"), Charsets.UTF_8).write(descriptor)

        var assertionsChecked = 0
        val listener: LintListener = object : LintListener {
            override fun update(
                    driver: LintDriver,
                    type: LintListener.EventType,
                    project: Project?,
                    context: Context?) {
                val client = driver.client
                when (type) {
                    REGISTERED_PROJECT -> {
                        assertThat(project).isNotNull()
                        project!!
                        if (project.name == "Library") {
                            assertThat(project).isNotNull()
                            val manifest = client.getMergedManifest(project)
                            assertThat(manifest).isNotNull()
                            manifest!!
                            val permission = getFirstSubTagByName(manifest.documentElement,
                                    "permission")!!
                            assertThat(permission.getAttributeNS(ANDROID_URI,
                                    ATTR_NAME)).isEqualTo("foo.permission.SEND_SMS")
                            assertionsChecked++

                            // compileSdkVersion=android-M -> build API=23
                            assertThat(project.buildSdk).isEqualTo(23)
                            assertionsChecked++
                        } else {
                            // App project
                            assertThat(project.buildSdk).isEqualTo(18)
                            assertionsChecked++
                        }
                    }
                    STARTING -> {
                        // Check extra metadata is handled right
                        assertThat(client.getSdkHome()).isEqualTo(sdk)
                        assertThat(client.getCacheDir(null, false)).isEqualTo(cacheDir)
                        assertionsChecked += 2
                    }
                    else -> {
                        // Ignored
                    }
                }
            }
        }

        MainTest.checkDriver(

"""../baseline: Information: 1 error were filtered out because they were listed in the baseline file, TESTROOT/baseline [LintBaseline]
res/values/strings.xml:4: Error: string1 has already been defined in this folder [DuplicateDefinition]
    <string name="string1">String 2</string>
            ~~~~~~~~~~~~~~
    res/values/strings.xml:3: Previously defined here
../Library/AndroidManifest.xml:9: Error: Permission name SEND_SMS is not unique (appears in both foo.permission.SEND_SMS and bar.permission.SEND_SMS) [UniquePermission]
    <permission android:name="bar.permission.SEND_SMS"
                ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    AndroidManifest.xml:9: Previous permission here
2 errors, 1 warnings (1 error filtered by baseline baseline)
""",
                "",

                // Expected exit code
                ERRNO_SUCCESS,

                // Args
                arrayOf("--quiet",
                        "--check",
                        "UniquePermission,DuplicateDefinition,SdCardPath",
                        "--project",
                        File(root, "project.xml").path),

                { it.replace(baseline.parentFile.path, "TESTROOT") },
                listener)


        // Make sure we hit all our checks with the listener
        assertThat(assertionsChecked).isEqualTo(5)
    }

    @Test
    fun testManualProjectErrorHandling() {

        @Language("XML")
        val descriptor = """
            <project>
            <classpath jar="test.jar" />
            <module name="Foo:App" android="true" library="true">
              <unknown file="foo.Bar" />
              <resource file="res/values/strings.xml" />
              <dep module="NonExistent" />
            </module>
            </project>""".trimIndent()
        val projectXml = File(temp.root, "project.xml")
        Files.asCharSink(projectXml, Charsets.UTF_8).write(descriptor)

        MainTest.checkDriver(
                "" +
                        "project.xml:4: Error: Unexpected tag unknown [LintError]\n" +
                        "  <unknown file=\"foo.Bar\" />\n" +
                        "  ~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                        "1 errors, 0 warnings\n",
                "",

                ERRNO_SUCCESS,

                arrayOf("--quiet",
                        "--project",
                        projectXml.path), null, null)
    }

    companion object {
        @ClassRule
        @JvmField
        var temp = TemporaryFolder()

        fun project(vararg files: TestFile): ProjectDescription = ProjectDescription(*files)
    }
}