/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.projectmodel

import com.android.ide.common.util.PathString
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Test cases for [ExternalLibrary]
 */
class ExternalLibraryTest {
    @Test
    fun toStringDefaultTest() {
        val cfg = ExternalLibrary("foo")
        assertThat(cfg.toString()).isEqualTo("ExternalLibrary(address=foo)")
    }

    @Test
    fun toStringOverrideTest() {
        val cfg = ExternalLibrary(
            address = "foo",
            classesJar = PathString("/bar/baz")
        )
        assertThat(cfg.toString()).isEqualTo("ExternalLibrary(address=foo,classesJar=file:///bar/baz)")
    }

    @Test
    fun simpleConstructorTest() {
        assertThat(ExternalLibrary("foo")).isEqualTo(
            ExternalLibrary(
                address = "foo",
                manifestFile = null
            )
        )
    }

    private val barPath = PathString("bar")

    @Test
    fun withManifestFileTest() {
        val manifest = barPath
        val representativeManifest = PathString("representative")
        val address = "foo"
        val lib = ExternalLibrary(address)
        val withManifest = ExternalLibrary(
            address = address,
            manifestFile = manifest,
            representativeManifestFile = representativeManifest
        )

        assertThat(lib.withManifestFile(manifest)).isEqualTo(
            ExternalLibrary(
                address = address,
                manifestFile = manifest
            )
        )
        assertThat(
            lib.withRepresentativeManifestFile(representativeManifest).withManifestFile(
                manifest
            )
        )
            .isEqualTo(withManifest)
        assertThat(
            lib.withManifestFile(manifest).withRepresentativeManifestFile(
                representativeManifest
            )
        )
            .isEqualTo(withManifest)
        assertThat(lib.withRepresentativeManifestFile(representativeManifest))
            .isEqualTo(
                ExternalLibrary(
                    address = address,
                    representativeManifestFile = representativeManifest
                )
            )
    }

    @Test
    fun withClassesJarTest() {
        assertThat(ExternalLibrary("foo").withClassesJar(barPath))
            .isEqualTo(ExternalLibrary(address = "foo", classesJar = barPath))
    }

    @Test
    fun withResFolderTest() {
        assertThat(ExternalLibrary("foo").withResFolder(barPath))
            .isEqualTo(ExternalLibrary(address = "foo", resFolder = barPath))
    }

    @Test
    fun withLocationTest() {
        assertThat(ExternalLibrary("foo").withLocation(barPath))
            .isEqualTo(ExternalLibrary(address = "foo", location = barPath))
    }

    @Test
    fun withSymbolFileTest() {
        assertThat(ExternalLibrary("foo").withSymbolFile(barPath))
            .isEqualTo(ExternalLibrary(address = "foo", symbolFile = barPath))
    }
}