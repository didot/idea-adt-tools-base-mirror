/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.build.gradle.integration.common.utils

import com.android.utils.PathUtils
import com.google.common.io.Files
import com.google.common.truth.Truth
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Path
import java.util.Base64

class FileBuilderTest {

    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = Files.createTempDir()
    }

    @After
    fun tearDown() {
        PathUtils.deleteRecursivelyIfExists(tempDir.toPath())
    }

    @Test
    fun withFiles() {
        tempDir {
            (File("dir1")) {
                (File("dir2")) {
                    File("file2") - "content 2"
                }
                File("file1") - "content 1"
            }
            (File("dir3/file3")) - "content 3"
        }
        val (dir1, dir3) = tempDir.listFiles().sorted()
        val (dir2, file1) = dir1.listFiles().sorted()
        val (file2) = dir2.listFiles()
        val (file3) = dir3.listFiles()

        Truth.assertThat(file1.readText()).isEqualTo("content 1")
        Truth.assertThat(file2.readText()).isEqualTo("content 2")
        Truth.assertThat(file3.readText()).isEqualTo("content 3")
    }

    @Test
    fun withStrings() {
        tempDir.absolutePath {
            "dir1" {
                "dir2" {
                    "file2" - "content 2"
                }
                "file1" - "content 1"
            }
            "dir3/file3" - "content 3"
        }

        val (dir1, dir3) = tempDir.listFiles().sorted()
        val (dir2, file1) = dir1.listFiles().sorted()
        val (file2) = dir2.listFiles()
        val (file3) = dir3.listFiles()

        Truth.assertThat(file1.readText()).isEqualTo("content 1")
        Truth.assertThat(file2.readText()).isEqualTo("content 2")
        Truth.assertThat(file3.readText()).isEqualTo("content 3")
    }

    @Test
    fun links() {
        var link1: Path? = null
        var link2: Path? = null
        tempDir {
            "dir" {
                val file1 = "file1" - "content 1"
                link1 = "file1-link1" linkTo file1
                link2 = "file1-link2" linkTo path("file1")
            }
        }

        Truth.assertThat(link1!!.toFile().readText()).isEqualTo("content 1")
        Truth.assertThat(link2!!.toFile().readText()).isEqualTo("content 1")
    }

    @Test
    fun resolveBySlash() {
        tempDir {
            "file1" - "content1"
            "file2" - "content2"
        }
        Truth.assertThat((tempDir / "file1").readText()).isEqualTo("content1")
        Truth.assertThat((tempDir.toPath() / "file1").toFile().readText()).isEqualTo("content1")
    }

    @Test
    fun appendStrings() {
        tempDir {
            "file1" - "hello"
        }
        tempDir {
            "file1" - +" world"
        }

        val (file1) = tempDir.listFiles()
        Truth.assertThat(file1.readText()).isEqualTo("hello world")
    }

    @Test
    fun inflatedBy() {
        // Base64 encoding for a zip file containing the following files
        //
        //     dir1
        //     +-- dir2
        //     |   +-- file2 - "content 2"
        //     +-- file1 - "content 1"
        val zipFileContent = Base64.getDecoder().decode(
            """
            UEsDBAoAAAAAACNxa04AAAAAAAAAAAAAAAAFABwAZGlyMS9VVAkAA3HOhlxyzoZcdXgLAAEEzEgE
            AARTXwEAUEsDBAoAAAAAACdxa04AAAAAAAAAAAAAAAAKABwAZGlyMS9kaXIyL1VUCQADec6GXIbO
            hlx1eAsAAQTMSAQABFNfAQBQSwMECgAAAAAAJ3FrTkBESCgKAAAACgAAAA8AHABkaXIxL2RpcjIv
            ZmlsZTJVVAkAA3nOhlx5zoZcdXgLAAEEzEgEAARTXwEAY29udGVudCAyClBLAwQKAAAAAAAgcWtO
            gxdlAwoAAAAKAAAACgAcAGRpcjEvZmlsZTFVVAkAA2vOhlxrzoZcdXgLAAEEzEgEAARTXwEAY29u
            dGVudCAxClBLAQIeAwoAAAAAACNxa04AAAAAAAAAAAAAAAAFABgAAAAAAAAAEADoQQAAAABkaXIx
            L1VUBQADcc6GXHV4CwABBMxIBAAEU18BAFBLAQIeAwoAAAAAACdxa04AAAAAAAAAAAAAAAAKABgA
            AAAAAAAAEADoQT8AAABkaXIxL2RpcjIvVVQFAAN5zoZcdXgLAAEEzEgEAARTXwEAUEsBAh4DCgAA
            AAAAJ3FrTkBESCgKAAAACgAAAA8AGAAAAAAAAQAAAKCBgwAAAGRpcjEvZGlyMi9maWxlMlVUBQAD
            ec6GXHV4CwABBMxIBAAEU18BAFBLAQIeAwoAAAAAACBxa06DF2UDCgAAAAoAAAAKABgAAAAAAAEA
            AACggdYAAABkaXIxL2ZpbGUxVVQFAANrzoZcdXgLAAEEzEgEAARTXwEAUEsFBgAAAAAEAAQAQAEA
            ACQBAAAAAA==
            """.trimIndent().replace("\n", "")
        )

        tempDir {
            val zipFile = "dir1.zip" - zipFileContent
            ("unzipped" inflatedBy zipFile) {
                Truth.assertThat(file("dir1/dir2/file2").readText()).isEqualTo("content 2\n")
                Truth.assertThat(file("dir1/file1").readText()).isEqualTo("content 1\n")
            }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `failure - start with a relative path is not allowed`() {
        (File("relative/path")) {} // start file builder with relative path
    }

    @Test(expected = IllegalArgumentException::class)
    fun `failure - cannot write to a directory`() {
        tempDir {
            "dir" {}
            "dir" - "content" // write to directory here
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `failure - cannot create files inside an existing file`() {
        tempDir {
            "file1" - "content"
            "file1" {} // trying to use "file1" as a directory
        }
    }
}
