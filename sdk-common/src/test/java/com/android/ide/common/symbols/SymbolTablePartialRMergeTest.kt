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

package com.android.ide.common.symbols

import com.android.resources.ResourceAccessibility
import com.android.resources.ResourceType
import com.android.testutils.truth.MoreTruth.assertThat
import com.android.utils.FileUtils
import com.google.common.collect.ImmutableList.of as list
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files

/*
 * Tests for [SymbolTable.mergePartialTables]
 */
class SymbolTablePartialRMergeTest {

    @get:Rule
    val mTemporaryFolder = TemporaryFolder()

    @Test
    fun testEmpty() {
        val result = SymbolTable.mergePartialTables(list(), "com.foo.bar")
        assertThat(result.symbols.values()).isEmpty()
    }

    @Test
    fun testValuesOverrides() {
        val f1 = mTemporaryFolder.newFile("values_strings-R.txt")
        Files.write(
                f1.toPath(),
                list(
                        "default int string s1",
                        "default int string s2",
                        "public int string s3"))

        val f2 = mTemporaryFolder.newFile("values-en_strings-R.txt")
        Files.write(
                f2.toPath(),
                list(
                        "default int string s1",
                        "private int string s2",
                        "default int string s3"))

        val result = SymbolTable.mergePartialTables(list(f1, f2), "com.foo.bar")

        assertThat(result.symbols.values()).hasSize(3)
        assertThat(result.symbols.values()).containsExactly(
                Symbol.createSymbol(
                        ResourceAccessibility.DEFAULT,
                        ResourceType.STRING,
                        "s1",
                        SymbolJavaType.INT,
                        "0",
                        Symbol.NO_CHILDREN),
                Symbol.createSymbol(
                        ResourceAccessibility.PRIVATE,
                        ResourceType.STRING,
                        "s2",
                        SymbolJavaType.INT,
                        "0",
                        Symbol.NO_CHILDREN),
                Symbol.createSymbol(
                        ResourceAccessibility.PUBLIC,
                        ResourceType.STRING,
                        "s3",
                        SymbolJavaType.INT,
                        "0",
                        Symbol.NO_CHILDREN))
    }

    @Test
    fun testIncorrectValuesOverrides() {
        val f1 = mTemporaryFolder.newFile("values_strings-R.txt")
        Files.write(f1.toPath(), list("private int string s1"))
        val f2 = mTemporaryFolder.newFile("values-en_strings-R.txt")
        Files.write(f2.toPath(), list("public int string s1"))

        try {
            SymbolTable.mergePartialTables(list(f1, f2), "com.foo.bar")
            fail()
        } catch (e: PartialRMergingException) {
            assertThat(e.message)
                    .contains("An error occurred during merging of the partial R files")
            assertThat(e.cause!!.message).contains(
                    "Symbol with resource type string and name s1 defined both as private and " +
                            "public.")
        }

        try {
            SymbolTable.mergePartialTables(list(f2, f1), "com.foo.bar")
            fail()
        } catch (e: PartialRMergingException) {
            assertThat(e.message)
                    .contains("An error occurred during merging of the partial R files")
            assertThat(e.cause!!.message).contains(
                    "Symbol with resource type string and name s1 defined both as private and " +
                            "public.")
        }
    }

    @Test
    fun testLayoutOverrides() {
        //Trickier because of "@+id/name" structures
        val sourceSetA  = mTemporaryFolder.newFolder("A")
        val layoutA = File(sourceSetA, "layout_layout-R.txt")
        Files.write(layoutA.toPath(), list("default int id idA", "default int layout layout"))
        val sourceSetB = mTemporaryFolder.newFolder("B")
        val layoutB = File(sourceSetB, "layout_layout-R.txt")
        Files.write(layoutB.toPath(), list("default int id idB", "default int layout layout"))

        var result = SymbolTable.mergePartialTables(list(layoutA, layoutB), null)
        assertThat(result.symbols.values()).hasSize(2)
        assertThat(result.symbols.values()).containsExactly(
                Symbol.createSymbol(
                        ResourceAccessibility.DEFAULT,
                        ResourceType.ID,
                        "idB",
                        SymbolJavaType.INT,
                        "0",
                        Symbol.NO_CHILDREN),
                Symbol.createSymbol(
                        ResourceAccessibility.DEFAULT,
                        ResourceType.LAYOUT,
                        "layout",
                        SymbolJavaType.INT,
                        "0",
                        Symbol.NO_CHILDREN))

        result = SymbolTable.mergePartialTables(list(layoutB, layoutA), null)
        assertThat(result.symbols.values()).hasSize(2)
        assertThat(result.symbols.values()).containsExactly(
                Symbol.createSymbol(
                        ResourceAccessibility.DEFAULT,
                        ResourceType.ID,
                        "idA",
                        SymbolJavaType.INT,
                        "0",
                        Symbol.NO_CHILDREN),
                Symbol.createSymbol(
                        ResourceAccessibility.DEFAULT,
                        ResourceType.LAYOUT,
                        "layout",
                        SymbolJavaType.INT,
                        "0",
                        Symbol.NO_CHILDREN))
    }

    @Test
    fun testMultipleSourceSets() {
        // Source-sets in this examples are A, B and C; C overrides B which overrides A (base).

        val files = ArrayList<File>()

        // source-set A
        val sourceSetA = mTemporaryFolder.newFolder("A")
        val nonXmlA = File(sourceSetA, "non-xml-R.txt")
        Files.write(
                nonXmlA.toPath(),
                list("default int drawable image", "default int drawable picture"))
        files.add(nonXmlA)

        // Should get overridden by C
        val layout1A = File(sourceSetA, "layout_layout1-R.txt")
        Files.write(layout1A.toPath(), list("default int id id1", "default int layout layout1"))
        files.add(layout1A)

        // Should get overridden by B, and so idA should not be present in the merged table.
        val layout2A = File(sourceSetA, "layout_layout2-R.txt")
        Files.write(layout2A.toPath(), list("default int id idA", "default int layout layout2"))
        files.add(layout2A)

        // 'foo' declared as public in A, 'bar' is public in B, 'beep' is private in A
        val stringsA = File(sourceSetA, "values_strings-R.txt")
        Files.write(
                stringsA.toPath(),
                list("default int string foo", "default int string bar", "default int string beep"))
        files.add(stringsA)

        val colorsA = File(sourceSetA, "values_colors-R.txt")
        Files.write(colorsA.toPath(), list("default int color main", "default int color accent"))
        files.add(colorsA)

        // dsA overridden in C, dsA_attr1 and dsA_attr2 should not be in the merged table.
        val stylesA = File(sourceSetA, "values_styles-R.txt")
        Files.write(
                stylesA.toPath(),
                list(
                        "default int[] styleable dsA",
                        "default int styleable dsA_attr1",
                        "default int styleable dsA_attr2"))
        files.add(stylesA)

        // all should be kept as 'public'
        val publicA = File(sourceSetA, "values_public-R.txt")
        Files.write(
                publicA.toPath(),
                list("public int string foo", "public int layout layout1", "public int id id1"))
        files.add(publicA)

        // should carry as 'private'
        val symbolsA = File(sourceSetA, "values_symbols-R.txt")
        Files.write(symbolsA.toPath(), list("private int string beep"))
        files.add(symbolsA)


        // source-set B
        val sourceSetB = mTemporaryFolder.newFolder("B")
        val nonXmlB = File(sourceSetB, "non-xml-R.txt")
        Files.write(nonXmlB.toPath(), list("default int drawable imageB"))
        files.add(nonXmlB)

        // Overriddes layout2 from A, id1 and idB should be kept.
        val layout2B = File(sourceSetB, "layout_layout2-R.txt")
        Files.write(
                layout2B.toPath(),
                list("default int id id1", "default int id idB", "default int layout layout2"))
        files.add(layout2B)

        // 'foo' is default in B, but was declared as 'public' in A. In the merged table it should
        // be 'public'.
        val valuesB = File(sourceSetB, "values_values-R.txt")
        Files.write(valuesB.toPath(), list("default int string foo"))
        files.add(valuesB)

        // 'bar' defined in A, marked as public in B
        val publicB = File(sourceSetB, "values_public-R.txt")
        Files.write(publicB.toPath(), list("public int string bar"))
        files.add(publicB)


        // source-set C
        val sourceSetC = mTemporaryFolder.newFolder("C")

        // 'image' from C overrides the 'image' from A
        val nonXmlC = File(sourceSetC, "non-xml-R.txt")
        Files.write(nonXmlC.toPath(), list("default int drawable image"))
        files.add(nonXmlC)

        // overrides the 'layout1' in A
        val layout1C = File(sourceSetC, "layout_layout1-R.txt")
        Files.write(layout1C.toPath(), list("default int id id1", "default int layout layout1"))
        files.add(layout1C)

        // 'boop' marked as public in C/values/public.txt and 'biip' marked as private in
        // C/values/private.txt
        val stringsC = File(sourceSetC, "values_strings-R.txt")
        Files.write(stringsC.toPath(), list("default int string boop", "default int string biip"))
        files.add(stringsC)

        // dsA overrides the one from A, dsA_attr1 and dsA_attr2 should not be in the merged table,
        // dsA_attr3 should be kept.
        val styleablesC = File(sourceSetC, "values_styleables-R.txt")
        Files.write(
                styleablesC.toPath(),
                list("default int[] styleable dsA", "default int styleable dsA_attr3"))
        files.add(styleablesC)

        val publicC = File(sourceSetC, "values_public-R.txt")
        Files.write(publicC.toPath(), list("public int string boop"))
        files.add(publicC)

        val privateC = File(sourceSetC, "values_private-R.txt")
        Files.write(privateC.toPath(), list("private int string biip"))
        files.add(privateC)

        // Files given in order of A files, B files, C files.
        val result = SymbolTable.mergePartialTables(files, "com.boop.beep")

        // Write to a file so it's easier to check contents.
        val writtenResources = mTemporaryFolder.newFile("all-resources.txt")
        SymbolIo.writePartialR(result, writtenResources.toPath())
        val lines = Files.readAllLines(writtenResources.toPath())

        assertThat(lines).containsExactly(
                "default int color accent",
                "default int color main",
                "default int drawable image",
                "default int drawable imageB",
                "default int drawable picture",
                "public int id id1", // explicitly defined as public in A
                "default int id idB",
                "public int layout layout1", // public in A, overridden in C, no idA in result
                "default int layout layout2",
                "public int string bar",
                "private int string beep",
                "private int string biip",
                "public int string boop",
                "public int string foo",
                "default int[] styleable dsA", // overridden in C, should only have attr3 child
                "default int styleable dsA_attr3")
    }
}