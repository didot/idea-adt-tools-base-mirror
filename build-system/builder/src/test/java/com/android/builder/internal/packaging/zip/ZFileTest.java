/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.builder.internal.packaging.zip;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.android.annotations.NonNull;
import com.android.builder.internal.packaging.zip.compress.DeflateExecutionCompressor;
import com.android.builder.internal.packaging.zip.utils.CloseableByteSource;
import com.android.builder.internal.packaging.zip.utils.RandomAccessFileUtils;
import com.android.utils.FileUtils;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closer;
import com.google.common.io.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZFileTest {
    @Rule
    public TemporaryFolder mTemporaryFolder = new TemporaryFolder();

    @Test
    public void getZipPath() throws Exception {
        File temporaryDir = mTemporaryFolder.getRoot();
        File zpath = new File(temporaryDir, "a");
        ZFile zf = new ZFile(zpath);
        assertEquals(zpath, zf.getFile());
        zf.close();
    }

    @Test
    public void readNonExistingFile() throws Exception {
        File temporaryDir = mTemporaryFolder.getRoot();
        File zf = new File(temporaryDir, "a");
        ZFile azf = new ZFile(zf);
        azf.touch();
        azf.close();
        assertTrue(zf.exists());
    }

    @Test(expected = IOException.class)
    public void readExistingEmptyFile() throws Exception {
        File temporaryDir = mTemporaryFolder.getRoot();
        File zf = new File(temporaryDir, "a");
        Files.write(new byte[0], zf);
        @SuppressWarnings("unused")
        ZFile azf = new ZFile(zf);

        azf.close();
    }

    @Test
    public void readAlmostEmptyZip() throws Exception {
        File zf = ZipTestUtils.cloneRsrc("empty-zip.zip", mTemporaryFolder);

        ZFile azf = new ZFile(zf);
        assertEquals(1, azf.entries().size());

        StoredEntry z = azf.get("z/");
        assertNotNull(z);
        assertSame(StoredEntryType.DIRECTORY, z.getType());

        azf.close();
    }

    @Test
    public void readZipWithTwoFilesOneDirectory() throws Exception {
        File zf = ZipTestUtils.cloneRsrc("simple-zip.zip", mTemporaryFolder);
        ZFile azf = new ZFile(zf);
        assertEquals(3, azf.entries().size());

        StoredEntry e0 = azf.get("dir/");
        assertNotNull(e0);
        assertSame(StoredEntryType.DIRECTORY, e0.getType());

        StoredEntry e1 = azf.get("dir/inside");
        assertNotNull(e1);
        assertSame(StoredEntryType.FILE, e1.getType());
        ByteArrayOutputStream e1BytesOut = new ByteArrayOutputStream();
        ByteStreams.copy(e1.open(), e1BytesOut);
        byte e1Bytes[] = e1BytesOut.toByteArray();
        String e1Txt = new String(e1Bytes, Charsets.US_ASCII);
        assertEquals("inside", e1Txt);

        StoredEntry e2 = azf.get("file.txt");
        assertNotNull(e2);
        assertSame(StoredEntryType.FILE, e2.getType());
        ByteArrayOutputStream e2BytesOut = new ByteArrayOutputStream();
        ByteStreams.copy(e2.open(), e2BytesOut);
        byte e2Bytes[] = e2BytesOut.toByteArray();
        String e2Txt = new String(e2Bytes, Charsets.US_ASCII);
        assertEquals("file with more text to allow deflating to be useful", e2Txt);

        azf.close();
    }

    @Test
    public void readOnlyZipSupport() throws Exception {
        File testZip = ZipTestUtils.cloneRsrc("empty-zip.zip", mTemporaryFolder);

        assertTrue(testZip.setWritable(false));

        ZFile zf = new ZFile(testZip);
        assertEquals(1, zf.entries().size());
        zf.close();
    }

    @Test
    public void compressedFilesReadableByJavaZip() throws Exception {
        File testZip = new File(mTemporaryFolder.getRoot(), "t.zip");
        ZFile zf = new ZFile(testZip);

        File wiki = ZipTestUtils.cloneRsrc("text-files/wikipedia.html", mTemporaryFolder, "wiki");
        File rfc = ZipTestUtils.cloneRsrc("text-files/rfc2460.txt", mTemporaryFolder, "rfc");
        File lena = ZipTestUtils.cloneRsrc("images/lena.png", mTemporaryFolder, "lena");
        byte[] wikiData = Files.toByteArray(wiki);
        byte[] rfcData = Files.toByteArray(rfc);
        byte[] lenaData = Files.toByteArray(lena);
        zf.add("wiki", new ByteArrayInputStream(wikiData));
        zf.add("rfc", new ByteArrayInputStream(rfcData));
        zf.add("lena", new ByteArrayInputStream(lenaData));
        zf.close();

        ZipFile jz = new ZipFile(testZip);
        try {
            ZipEntry ze = jz.getEntry("wiki");
            assertNotNull(ze);
            assertEquals(ZipEntry.DEFLATED, ze.getMethod());
            assertTrue(ze.getCompressedSize() < wikiData.length);
            InputStream zeis = jz.getInputStream(ze);
            assertArrayEquals(wikiData, ByteStreams.toByteArray(zeis));
            zeis.close();

            ze = jz.getEntry("rfc");
            assertNotNull(ze);
            assertEquals(ZipEntry.DEFLATED, ze.getMethod());
            assertTrue(ze.getCompressedSize() < rfcData.length);
            zeis = jz.getInputStream(ze);
            assertArrayEquals(rfcData, ByteStreams.toByteArray(zeis));
            zeis.close();

            ze = jz.getEntry("lena");
            assertNotNull(ze);
            assertEquals(ZipEntry.STORED, ze.getMethod());
            assertTrue(ze.getCompressedSize() == lenaData.length);
            zeis = jz.getInputStream(ze);
            assertArrayEquals(lenaData, ByteStreams.toByteArray(zeis));
            zeis.close();
        } finally {
            jz.close();
        }
    }

    @Test
    public void removeFileFromZip() throws Exception {
        File zipFile = mTemporaryFolder.newFile("test.zip");

        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
        try {
            ZipEntry entry = new ZipEntry("foo/");
            entry.setMethod(ZipEntry.STORED);
            entry.setSize(0);
            entry.setCompressedSize(0);
            entry.setCrc(0);
            zos.putNextEntry(entry);
            zos.putNextEntry(new ZipEntry("foo/bar"));
            zos.write(new byte[]{1, 2, 3, 4});
            zos.closeEntry();
        } finally {
            zos.close();
        }

        ZFile zf = new ZFile(zipFile);
        assertEquals(2, zf.entries().size());
        for (StoredEntry e : zf.entries()) {
            if (e.getType() == StoredEntryType.FILE) {
                e.delete();
            }
        }

        zf.update();

        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
        try {
            ZipEntry e1 = zis.getNextEntry();
            assertNotNull(e1);

            assertEquals("foo/", e1.getName());

            ZipEntry e2 = zis.getNextEntry();
            assertNull(e2);
        } finally {
            zis.close();
        }
    }

    @Test
    public void addFileToZip() throws Exception {
        File zipFile = mTemporaryFolder.newFile("test.zip");

        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
        try {
            ZipEntry fooDir = new ZipEntry("foo/");
            fooDir.setCrc(0);
            fooDir.setCompressedSize(0);
            fooDir.setSize(0);
            fooDir.setMethod(ZipEntry.STORED);
            zos.putNextEntry(fooDir);
            zos.closeEntry();
        } finally {
            zos.close();
        }

        ZFile zf = new ZFile(zipFile);
        assertEquals(1, zf.entries().size());


        zf.update();

        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
        try {
            ZipEntry e1 = zis.getNextEntry();
            assertNotNull(e1);

            assertEquals("foo/", e1.getName());

            ZipEntry e2 = zis.getNextEntry();
            assertNull(e2);
        } finally {
            zis.close();
        }
    }

    @Test
    public void createNewZip() throws Exception {
        File zipFile = new File(mTemporaryFolder.getRoot(), "test.zip");

        ZFile zf = new ZFile(zipFile);
        zf.add("foo", new ByteArrayInputStream(new byte[] { 0, 1 }));
        zf.close();

        ZipFile jzf = new ZipFile(zipFile);
        try {
            assertEquals(1, jzf.size());

            ZipEntry fooEntry = jzf.getEntry("foo");
            assertNotNull(fooEntry);
            assertEquals("foo", fooEntry.getName());
            assertEquals(2, fooEntry.getSize());

            InputStream is = jzf.getInputStream(fooEntry);
            assertEquals(0, is.read());
            assertEquals(1, is.read());
            assertEquals(-1, is.read());

            is.close();
        } finally {
            jzf.close();
        }
    }

    @Test
    public void replaceFileWithSmallerInMiddle() throws Exception {
        File zipFile = new File(mTemporaryFolder.getRoot(), "test.zip");

        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
        try {
            zos.putNextEntry(new ZipEntry("file1"));
            zos.write(new byte[]{1, 2, 3, 4, 5});
            zos.putNextEntry(new ZipEntry("file2"));
            zos.write(new byte[]{6, 7, 8});
            zos.putNextEntry(new ZipEntry("file3"));
            zos.write(new byte[]{9, 0, 1, 2, 3, 4});
        } finally {
            zos.close();
        }

        int totalSize = (int) zipFile.length();

        ZFile zf = new ZFile(zipFile);
        assertEquals(3, zf.entries().size());

        StoredEntry file2 = zf.get("file2");
        assertNotNull(file2);
        assertEquals(3, file2.getCentralDirectoryHeader().getUncompressedSize());

        assertArrayEquals(new byte[] { 6, 7, 8 }, file2.read());

        zf.add("file2", new ByteArrayInputStream(new byte[] { 11, 12 }));
        zf.close();

        int newTotalSize = (int) zipFile.length();
        assertTrue(newTotalSize + " == " + totalSize, newTotalSize == totalSize);

        file2 = zf.get("file2");
        assertNotNull(file2);
        assertArrayEquals(new byte[] { 11, 12, }, file2.read());

        ZFile zf2 = new ZFile(zipFile);
        file2 = zf2.get("file2");
        assertNotNull(file2);
        assertArrayEquals(new byte[] { 11, 12, }, file2.read());
    }

    @Test
    public void replaceFileWithSmallerAtEnd() throws Exception {
        File zipFile = new File(mTemporaryFolder.getRoot(), "test.zip");

        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
        try {
            zos.putNextEntry(new ZipEntry("file1"));
            zos.write(new byte[]{1, 2, 3, 4, 5});
            zos.putNextEntry(new ZipEntry("file2"));
            zos.write(new byte[]{6, 7, 8});
            zos.putNextEntry(new ZipEntry("file3"));
            zos.write(new byte[]{9, 0, 1, 2, 3, 4});
        } finally {
            zos.close();
        }

        int totalSize = (int) zipFile.length();

        ZFile zf = new ZFile(zipFile);
        assertEquals(3, zf.entries().size());

        StoredEntry file3 = zf.get("file3");
        assertNotNull(file3);
        assertEquals(6, file3.getCentralDirectoryHeader().getUncompressedSize());

        assertArrayEquals(new byte[] { 9, 0, 1, 2, 3, 4 }, file3.read());

        zf.add("file3", new ByteArrayInputStream(new byte[] { 11, 12 }));
        zf.close();

        int newTotalSize = (int) zipFile.length();
        assertTrue(newTotalSize + " < " + totalSize, newTotalSize < totalSize);

        file3 = zf.get("file3");
        assertNotNull(file3);
        assertArrayEquals(new byte[] { 11, 12, }, file3.read());

        ZFile zf2 = new ZFile(zipFile);
        file3 = zf2.get("file3");
        assertNotNull(file3);
        assertArrayEquals(new byte[] { 11, 12, }, file3.read());
    }

    @Test
    public void replaceFileWithBiggerAtBegin() throws Exception {
        File zipFile = new File(mTemporaryFolder.getRoot(), "test.zip");

        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
        try {
            zos.putNextEntry(new ZipEntry("file1"));
            zos.write(new byte[]{1, 2, 3, 4, 5});
            zos.putNextEntry(new ZipEntry("file2"));
            zos.write(new byte[]{6, 7, 8});
            zos.putNextEntry(new ZipEntry("file3"));
            zos.write(new byte[]{9, 0, 1, 2, 3, 4});
        } finally {
            zos.close();
        }

        int totalSize = (int) zipFile.length();

        ZFile zf = new ZFile(zipFile);
        assertEquals(3, zf.entries().size());

        StoredEntry file1 = zf.get("file1");
        assertNotNull(file1);
        assertEquals(5, file1.getCentralDirectoryHeader().getUncompressedSize());

        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, file1.read());

        /*
         * Need some data because java zip API uses data descriptors which we don't and makes the
         * entries bigger (meaning just adding a couple of bytes would still fit in the same
         * place).
         */
        byte[] newData = new byte[100];
        Random r = new Random();
        r.nextBytes(newData);

        zf.add("file1", new ByteArrayInputStream(newData));
        zf.close();

        int newTotalSize = (int) zipFile.length();
        assertTrue(newTotalSize + " > " + totalSize, newTotalSize > totalSize);

        file1 = zf.get("file1");
        assertNotNull(file1);
        assertArrayEquals(newData, file1.read());

        ZFile zf2 = new ZFile(zipFile);
        file1 = zf2.get("file1");
        assertNotNull(file1);
        assertArrayEquals(newData, file1.read());
    }

    @Test
    public void replaceFileWithBiggerAtEnd() throws Exception {
        File zipFile = new File(mTemporaryFolder.getRoot(), "test.zip");

        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
        try {
            zos.putNextEntry(new ZipEntry("file1"));
            zos.write(new byte[]{1, 2, 3, 4, 5});
            zos.putNextEntry(new ZipEntry("file2"));
            zos.write(new byte[]{6, 7, 8});
            zos.putNextEntry(new ZipEntry("file3"));
            zos.write(new byte[]{9, 0, 1, 2, 3, 4});
        } finally {
            zos.close();
        }

        int totalSize = (int) zipFile.length();

        ZFile zf = new ZFile(zipFile);
        assertEquals(3, zf.entries().size());

        StoredEntry file3 = zf.get("file3");
        assertNotNull(file3);
        assertEquals(6, file3.getCentralDirectoryHeader().getUncompressedSize());

        assertArrayEquals(new byte[] { 9, 0, 1, 2, 3, 4 }, file3.read());

        /*
         * Need some data because java zip API uses data descriptors which we don't and makes the
         * entries bigger (meaning just adding a couple of bytes would still fit in the same
         * place).
         */
        byte[] newData = new byte[100];
        Random r = new Random();
        r.nextBytes(newData);

        zf.add("file3", new ByteArrayInputStream(newData));
        zf.close();

        int newTotalSize = (int) zipFile.length();
        assertTrue(newTotalSize + " > " + totalSize, newTotalSize > totalSize);

        file3 = zf.get("file3");
        assertNotNull(file3);
        assertArrayEquals(newData, file3.read());

        ZFile zf2 = new ZFile(zipFile);
        file3 = zf2.get("file3");
        assertNotNull(file3);
        assertArrayEquals(newData, file3.read());
    }

    @Test
    public void ignoredFilesDuringMerge() throws Exception {
        File zip1 = mTemporaryFolder.newFile("t1.zip");
        ZipOutputStream zos1 = new ZipOutputStream(new FileOutputStream(zip1));
        try {
            zos1.putNextEntry(new ZipEntry("only_in_1"));
            zos1.write(new byte[] { 1, 2 });
            zos1.putNextEntry(new ZipEntry("overridden_by_2"));
            zos1.write(new byte[] { 2, 3 });
            zos1.putNextEntry(new ZipEntry("not_overridden_by_2"));
            zos1.write(new byte[] { 3, 4 });
        } finally {
            zos1.close();
        }

        File zip2 = mTemporaryFolder.newFile("t2.zip");
        ZipOutputStream zos2 = new ZipOutputStream(new FileOutputStream(zip2));
        try {
            zos2.putNextEntry(new ZipEntry("only_in_2"));
            zos2.write(new byte[] { 4, 5 });
            zos2.putNextEntry(new ZipEntry("overridden_by_2"));
            zos2.write(new byte[] { 5, 6 });
            zos2.putNextEntry(new ZipEntry("not_overridden_by_2"));
            zos2.write(new byte[] { 6, 7 });
            zos2.putNextEntry(new ZipEntry("ignored_in_2"));
            zos2.write(new byte[] { 7, 8 });
        } finally {
            zos2.close();
        }

        Predicate<String> ignorePredicate = Predicates.or(new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                return input.matches("not.*");
            }
        }, new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                return input.matches(".*gnored.*");
            }
        });

        ZFile zf1 = new ZFile(zip1);
        ZFile zf2 = new ZFile(zip2);
        zf1.mergeFrom(zf2, ignorePredicate);

        StoredEntry only_in_1 = zf1.get("only_in_1");
        assertNotNull(only_in_1);
        assertArrayEquals(new byte[] { 1, 2 }, only_in_1.read());

        StoredEntry overridden_by_2 = zf1.get("overridden_by_2");
        assertNotNull(overridden_by_2);
        assertArrayEquals(new byte[] { 5, 6 }, overridden_by_2.read());

        StoredEntry not_overridden_by_2 = zf1.get("not_overridden_by_2");
        assertNotNull(not_overridden_by_2);
        assertArrayEquals(new byte[] { 3, 4 }, not_overridden_by_2.read());

        StoredEntry only_in_2 = zf1.get("only_in_2");
        assertNotNull(only_in_2);
        assertArrayEquals(new byte[] { 4, 5 }, only_in_2.read());

        StoredEntry ignored_in_2 = zf1.get("ignored_in_2");
        assertNull(ignored_in_2);
    }

    @Test
    public void addingFileDoesNotAddDirectoriesAutomatically() throws Exception {
        File zip = new File(mTemporaryFolder.getRoot(), "z.zip");
        ZFile zf = new ZFile(zip);
        zf.add("a/b/c", new ByteArrayInputStream(new byte[] { 1, 2, 3 }));
        zf.update();
        assertEquals(1, zf.entries().size());

        StoredEntry c = zf.get("a/b/c");
        assertNotNull(c);
        assertEquals(3, c.read().length);

        zf.close();
    }

    @Test
    public void zipFileWithEocdSignatureInComment() throws Exception {
        File zip = mTemporaryFolder.newFile("f.zip");
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip));
        try {
            zos.putNextEntry(new ZipEntry("a"));
            zos.write(new byte[] { 1, 2, 3 });
            zos.setComment("Random comment with XXXX weird characters. There must be enough "
                    + "characters to survive skipping back the EOCD size.");
        } finally {
            zos.close();
        }

        byte zipBytes[] = Files.toByteArray(zip);
        boolean didX4 = false;
        for (int i = 0; i < zipBytes.length - 3; i++) {
            boolean x4 = true;
            for (int j = 0; j < 4; j++) {
                if (zipBytes[i + j] != 'X') {
                    x4 = false;
                    break;
                }
            }

            if (x4) {
                zipBytes[i] = (byte) 0x50;
                zipBytes[i + 1] = (byte) 0x4b;
                zipBytes[i + 2] = (byte) 0x05;
                zipBytes[i + 3] = (byte) 0x06;
                didX4 = true;
                break;
            }
        }

        assertTrue(didX4);

        Files.write(zipBytes, zip);

        ZFile zf = new ZFile(zip);
        assertEquals(1, zf.entries().size());
        StoredEntry a = zf.get("a");
        assertNotNull(a);
        assertArrayEquals(new byte[] { 1, 2, 3 }, a.read());

    }

    @Test
    public void addFileRecursively() throws Exception {
        File tdir = mTemporaryFolder.newFolder();
        File tfile = new File(tdir, "blah-blah");
        Files.write("blah", tfile, Charsets.US_ASCII);

        File zip = new File(tdir, "f.zip");
        ZFile zf = new ZFile(zip);
        zf.addAllRecursively(tfile);

        StoredEntry blahEntry = zf.get("blah-blah");
        assertNotNull(blahEntry);
        String contents = new String(blahEntry.read(), Charsets.US_ASCII);
        assertEquals("blah", contents);
        zf.close();
    }

    @Test
    public void addDirectoryRecursively() throws Exception {
        File tdir = mTemporaryFolder.newFolder();

        String boom = Strings.repeat("BOOM!", 100);
        String kaboom = Strings.repeat("KABOOM!", 100);

        Files.write(boom, new File(tdir, "danger"), Charsets.US_ASCII);
        Files.write(kaboom, new File(tdir, "do not touch"), Charsets.US_ASCII);
        File safeDir = new File(tdir, "safe");
        assertTrue(safeDir.mkdir());

        String iLoveChocolate = Strings.repeat("I love chocolate! ", 200);
        String iLoveOrange = Strings.repeat("I love orange! ", 50);
        String loremIpsum = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aenean vitae "
                + "turpis quis justo scelerisque vulputate in et magna. Suspendisse eleifend "
                + "ultricies nisi, placerat consequat risus accumsan et. Pellentesque habitant "
                + "morbi tristique senectus et netus et malesuada fames ac turpis egestas. "
                + "Integer vitae leo purus. Nulla facilisi. Duis ligula libero, lacinia a "
                + "malesuada a, viverra tempor sapien. Donec eget consequat sapien, ultrices"
                + "interdum diam. Maecenas ipsum erat, suscipit at iaculis a, mollis nec risus. "
                + "Quisque tristique ac velit sed auctor. Nulla lacus diam, tristique id sem non, "
                + "pellentesque commodo mauris.";

        Files.write(iLoveChocolate, new File(safeDir, "eat.sweet"), Charsets.US_ASCII);
        Files.write(iLoveOrange, new File(safeDir, "eat.fruit"), Charsets.US_ASCII);
        Files.write(loremIpsum, new File(safeDir, "bedtime.reading.txt"), Charsets.US_ASCII);

        File zip = new File(tdir, "f.zip");
        ZFile zf = new ZFile(zip);
        zf.addAllRecursively(tdir, new Function<File, Boolean>() {
            @Override
            public Boolean apply(File input) {
                if (input.getName().startsWith("eat.")) {
                    return false;
                } else {
                    return true;
                }
            }
        });

        assertEquals(6, zf.entries().size());

        StoredEntry boomEntry = zf.get("danger");
        assertNotNull(boomEntry);
        assertEquals(CompressionMethod.DEFLATE,
                boomEntry.getCentralDirectoryHeader().getCompressionInfoWithWait().getMethod());
        assertEquals(boom, new String(boomEntry.read(), Charsets.US_ASCII));

        StoredEntry kaboomEntry = zf.get("do not touch");
        assertNotNull(kaboomEntry);
        assertEquals(CompressionMethod.DEFLATE,
                kaboomEntry.getCentralDirectoryHeader().getCompressionInfoWithWait().getMethod());
        assertEquals(kaboom, new String(kaboomEntry.read(), Charsets.US_ASCII));

        StoredEntry safeEntry = zf.get("safe/");
        assertNotNull(safeEntry);
        assertEquals(0, safeEntry.read().length);

        StoredEntry choc = zf.get("safe/eat.sweet");
        assertNotNull(choc);
        assertEquals(CompressionMethod.STORE,
                choc.getCentralDirectoryHeader().getCompressionInfoWithWait().getMethod());
        assertEquals(iLoveChocolate, new String(choc.read(), Charsets.US_ASCII));

        StoredEntry orangeEntry = zf.get("safe/eat.fruit");
        assertNotNull(orangeEntry);
        assertEquals(CompressionMethod.STORE,
                orangeEntry.getCentralDirectoryHeader().getCompressionInfoWithWait().getMethod());
        assertEquals(iLoveOrange, new String(orangeEntry.read(), Charsets.US_ASCII));

        StoredEntry loremEntry = zf.get("safe/bedtime.reading.txt");
        assertNotNull(loremEntry);
        assertEquals(CompressionMethod.DEFLATE,
                loremEntry.getCentralDirectoryHeader().getCompressionInfoWithWait().getMethod());
        assertEquals(loremIpsum, new String(loremEntry.read(), Charsets.US_ASCII));

        zf.close();
    }

    @Test
    public void extraDirectoryOffsetEmptyFile() throws Exception {
        File zipNoOffsetFile = new File(mTemporaryFolder.getRoot(), "a.zip");
        File zipWithOffsetFile = new File(mTemporaryFolder.getRoot(), "b.zip");

        ZFile zipNoOffset = new ZFile(zipNoOffsetFile);
        ZFile zipWithOffset = new ZFile(zipWithOffsetFile);
        zipWithOffset.setExtraDirectoryOffset(31);

        zipNoOffset.close();
        zipWithOffset.close();

        long zipNoOffsetSize = zipNoOffsetFile.length();
        long zipWithOffsetSize = zipWithOffsetFile.length();

        assertEquals(zipNoOffsetSize + 31, zipWithOffsetSize);

        /*
         * EOCD with no comment has 22 bytes.
         */
        assertEquals(0, zipNoOffset.getCentralDirectoryOffset());
        assertEquals(0, zipNoOffset.getCentralDirectorySize());
        assertEquals(0, zipNoOffset.getEocdOffset());
        assertEquals(22, zipNoOffset.getEocdSize());
        assertEquals(31, zipWithOffset.getCentralDirectoryOffset());
        assertEquals(0, zipWithOffset.getCentralDirectorySize());
        assertEquals(31, zipWithOffset.getEocdOffset());
        assertEquals(22, zipWithOffset.getEocdSize());

        /*
         * The EOCDs should not differ up until the end of the Central Directory size and should
         * not differ after the offset
         */
        int p1Start = 0;
        int p1Size = Eocd.F_CD_SIZE.endOffset();
        int p2Start = Eocd.F_CD_OFFSET.endOffset();
        int p2Size = (int) zipNoOffsetSize - p2Start;

        byte[] noOffsetData1 = FileUtils.readSegment(zipNoOffsetFile, p1Start, p1Size);
        byte[] noOffsetData2 = FileUtils.readSegment(zipNoOffsetFile, p2Start, p2Size);
        byte[] withOffsetData1 = FileUtils.readSegment(zipWithOffsetFile, 31, p1Size);
        byte[] withOffsetData2 = FileUtils.readSegment(zipWithOffsetFile, 31 + p2Start, p2Size);

        assertArrayEquals(noOffsetData1, withOffsetData1);
        assertArrayEquals(noOffsetData2, withOffsetData2);

        ZFile readWithOffset = new ZFile(zipWithOffsetFile);
        assertEquals(0, readWithOffset.entries().size());
    }

    @Test
    public void extraDirectoryOffsetNonEmptyFile() throws Exception {
        File zipNoOffsetFile = new File(mTemporaryFolder.getRoot(), "a.zip");
        File zipWithOffsetFile = new File(mTemporaryFolder.getRoot(), "b.zip");

        ZFile zipNoOffset = new ZFile(zipNoOffsetFile);
        ZFile zipWithOffset = new ZFile(zipWithOffsetFile);
        zipWithOffset.setExtraDirectoryOffset(37);

        zipNoOffset.add("x", new ByteArrayInputStream(new byte[] { 1, 2 }));
        zipWithOffset.add("x", new ByteArrayInputStream(new byte[] { 1, 2 }));

        zipNoOffset.close();
        zipWithOffset.close();

        long zipNoOffsetSize = zipNoOffsetFile.length();
        long zipWithOffsetSize = zipWithOffsetFile.length();

        assertEquals(zipNoOffsetSize + 37, zipWithOffsetSize);

        /*
         * Local file header has 30 bytes + name.
         * Central directory entry has 46 bytes + name
         * EOCD with no comment has 22 bytes.
         */
        assertEquals(30 + 1 + 2, zipNoOffset.getCentralDirectoryOffset());
        int cdSize = (int) zipNoOffset.getCentralDirectorySize();
        assertEquals(46 + 1, cdSize);
        assertEquals(30 + 1 + 2 + cdSize, zipNoOffset.getEocdOffset());
        assertEquals(22, zipNoOffset.getEocdSize());
        assertEquals(30 + 1 + 2 + 37, zipWithOffset.getCentralDirectoryOffset());
        assertEquals(cdSize, zipWithOffset.getCentralDirectorySize());
        assertEquals(30 + 1 + 2 + 37 + cdSize, zipWithOffset.getEocdOffset());
        assertEquals(22, zipWithOffset.getEocdSize());

        /*
         * The files should be equal: until the end of the first entry, from the beginning of the
         * central directory until the offset field in the EOCD and after the offset field.
         */
        int p1Start = 0;
        int p1Size = 30 + 1 + 2;
        int p2Start = 30 + 1 + 2;
        int p2Size = cdSize + Eocd.F_CD_SIZE.endOffset();
        int p3Start = p2Start + cdSize + Eocd.F_CD_OFFSET.endOffset();
        int p3Size = 22 - Eocd.F_CD_OFFSET.endOffset();

        byte[] noOffsetData1 = FileUtils.readSegment(zipNoOffsetFile, p1Start, p1Size);
        byte[] noOffsetData2 = FileUtils.readSegment(zipNoOffsetFile, p2Start, p2Size);
        byte[] noOffsetData3 = FileUtils.readSegment(zipNoOffsetFile, p3Start, p3Size);
        byte[] withOffsetData1 = FileUtils.readSegment(zipWithOffsetFile, p1Start, p1Size);
        byte[] withOffsetData2 = FileUtils.readSegment(zipWithOffsetFile, 37 + p2Start, p2Size);
        byte[] withOffsetData3 = FileUtils.readSegment(zipWithOffsetFile, 37 + p3Start, p3Size);

        assertArrayEquals(noOffsetData1, withOffsetData1);
        assertArrayEquals(noOffsetData2, withOffsetData2);
        assertArrayEquals(noOffsetData3, withOffsetData3);

        ZFile readWithOffset = new ZFile(zipWithOffsetFile);
        assertEquals(1, readWithOffset.entries().size());
    }

    @Test
    public void changeExtraDirectoryOffset() throws Exception {
        File zipFile = new File(mTemporaryFolder.getRoot(), "a.zip");

        ZFile zip = new ZFile(zipFile);
        zip.add("x", new ByteArrayInputStream(new byte[] { 1, 2 }));
        zip.close();

        long noOffsetSize = zipFile.length();

        zip.setExtraDirectoryOffset(177);
        zip.close();

        long withOffsetSize = zipFile.length();

        assertEquals(noOffsetSize + 177, withOffsetSize);
    }

    @Test
    public void computeOffsetWhenReadingEmptyFile() throws Exception {
        File zipFile = new File(mTemporaryFolder.getRoot(), "a.zip");

        ZFile zip = new ZFile(zipFile);
        zip.setExtraDirectoryOffset(18);
        zip.close();

        zip = new ZFile(zipFile);
        assertEquals(18, zip.getExtraDirectoryOffset());

        zip.close();
    }

    @Test
    public void computeOffsetWhenReadingNonEmptyFile() throws Exception {
        File zipFile = new File(mTemporaryFolder.getRoot(), "a.zip");

        ZFile zip = new ZFile(zipFile);
        zip.setExtraDirectoryOffset(287);
        zip.add("x", new ByteArrayInputStream(new byte[] { 1, 2 }));
        zip.close();

        zip = new ZFile(zipFile);
        assertEquals(287, zip.getExtraDirectoryOffset());

        zip.close();
    }

    @Test
    public void obtainingCDAndEocdWhenEntriesWrittenOnEmptyZip() throws Exception {
        File zipFile = new File(mTemporaryFolder.getRoot(), "a.zip");

        final byte[][] cd = new byte[1][];
        final byte[][] eocd = new byte[1][];

        final ZFile zip = new ZFile(zipFile);
        zip.addZFileExtension(new ZFileExtension() {
            @Override
            public void entriesWritten() throws IOException {
                cd[0] = zip.getCentralDirectoryBytes();
                eocd[0] = zip.getEocdBytes();
            }
        });

        zip.close();

        assertNotNull(cd[0]);
        assertEquals(0, cd[0].length);
        assertNotNull(eocd[0]);
        assertEquals(22, eocd[0].length);
    }

    @Test
    public void obtainingCDAndEocdWhenEntriesWrittenOnNonEmptyZip() throws Exception {
        File zipFile = new File(mTemporaryFolder.getRoot(), "a.zip");

        final byte[][] cd = new byte[1][];
        final byte[][] eocd = new byte[1][];

        final ZFile zip = new ZFile(zipFile);
        zip.add("foo", new ByteArrayInputStream(new byte[0]));
        zip.addZFileExtension(new ZFileExtension() {
            @Override
            public void entriesWritten() throws IOException {
                cd[0] = zip.getCentralDirectoryBytes();
                eocd[0] = zip.getEocdBytes();
            }
        });

        zip.close();

        /*
         * Central directory entry has 46 bytes + name
         * EOCD with no comment has 22 bytes.
         */
        assertNotNull(cd[0]);
        assertEquals(46 + 3, cd[0].length);
        assertNotNull(eocd[0]);
        assertEquals(22, eocd[0].length);
    }

    @Test
    public void java7JarSupported() throws Exception {
        File jar = ZipTestUtils.cloneRsrc("j7.jar", mTemporaryFolder);

        ZFile j = new ZFile(jar);
        assertEquals(8, j.entries().size());
        j.close();
    }

    @Test
    public void java8JarSupported() throws Exception {
        File jar = ZipTestUtils.cloneRsrc("j8.jar", mTemporaryFolder);

        ZFile j = new ZFile(jar);
        assertEquals(8, j.entries().size());
        j.close();
    }

    @Test
    public void utf8NamesSupportedOnReading() throws Exception {
        File zip = ZipTestUtils.cloneRsrc("zip-with-utf8-filename.zip", mTemporaryFolder);

        ZFile f = new ZFile(zip);
        assertEquals(1, f.entries().size());

        StoredEntry entry = f.entries().iterator().next();
        String filetMignonKorean = "\uc548\uc2eC \uc694\ub9ac";
        String isGoodJapanese = "\u3068\u3066\u3082\u826f\u3044";

        assertEquals(filetMignonKorean + " " + isGoodJapanese,
                entry.getCentralDirectoryHeader().getName());
        assertArrayEquals("Stuff about food is good.\n".getBytes(Charsets.US_ASCII), entry.read());

        f.close();
    }

    @Test
    public void utf8NamesSupportedOnWriting() throws Exception {
        File zipFile = new File(mTemporaryFolder.getRoot(), "a.zip");
        ZFile zip = new ZFile(zipFile);

        String lettuceIsHealthyArmenian = "\u0533\u0561\u0566\u0561\u0580\u0020\u0561\u057C"
                + "\u0578\u0572\u057B";
        zip.add(lettuceIsHealthyArmenian, new ByteArrayInputStream(new byte[]{0}));
        zip.close();

        ZFile zip2 = new ZFile(zipFile);
        assertEquals(1, zip2.entries().size());
        StoredEntry entry = zip2.entries().iterator().next();
        assertEquals(lettuceIsHealthyArmenian, entry.getCentralDirectoryHeader().getName());

        zip2.close();
    }

    @Test
    public void zipMemoryUsageIsZeroAfterClose() throws Exception {
        File zipFile = new File(mTemporaryFolder.getRoot(), "a.zip");

        ZFileOptions options = new ZFileOptions();
        ZFile zip = new ZFile(zipFile, options);

        assertEquals(0, options.getTracker().getBytesUsed());
        assertEquals(0, options.getTracker().getMaxBytesUsed());

        zip.add("Blah", new ByteArrayInputStream(new byte[500]));
        long used = options.getTracker().getBytesUsed();
        assertTrue(used > 500);
        assertEquals(used, options.getTracker().getMaxBytesUsed());

        zip.close();
        assertEquals(0, options.getTracker().getBytesUsed());
        assertEquals(used, options.getTracker().getMaxBytesUsed());
    }

    @Test
    public void unusedZipAreasAreClearedOnWrite() throws Exception {
        File zipFile = new File(mTemporaryFolder.getRoot(), "a.zip");
        ZFile zf = new ZFile(zipFile);
        zf.getAlignmentRules().add(new AlignmentRule(Pattern.compile(".*\\.txt"), 1000));
        zf.add("test1.txt", new ByteArrayInputStream(new byte[]{1}), false);
        zf.close();

        /*
         * Write dummy data in some unused portion of the file.
         */
        Closer closer = Closer.create();
        try {
            RandomAccessFile raf = closer.register(new RandomAccessFile(zipFile, "rw"));

            raf.seek(500);
            byte[] dummyData = "Dummy".getBytes(Charsets.US_ASCII);
            raf.write(dummyData);
        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }

        zf = new ZFile(zipFile);
        zf.touch();
        zf.close();

        closer = Closer.create();
        try {
            RandomAccessFile raf = closer.register(new RandomAccessFile(zipFile, "r"));

            /*
             * test1.txt won't take more than 200 bytes. Additionally, the header for
             */
            byte[] data = new byte[900];
            RandomAccessFileUtils.fullyRead(raf, data);

            byte[] zeroData = new byte[data.length];
            assertArrayEquals(zeroData, data);
        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }
    }

    @Test
    public void deferredCompression() throws Exception {
        File zipFile = new File(mTemporaryFolder.getRoot(), "a.zip");

        ExecutorService executor = Executors.newSingleThreadExecutor();

        ZFileOptions options = new ZFileOptions();
        final boolean[] done = new boolean[1];
        options.setCompressor(new DeflateExecutionCompressor(executor, options.getTracker(),
                Deflater.BEST_COMPRESSION) {
            @NonNull
            @Override
            protected CompressionResult immediateCompress(@NonNull CloseableByteSource source)
                    throws Exception {
                Thread.sleep(500);
                CompressionResult cr = super.immediateCompress(source);
                done[0] = true;
                return cr;
            }
        });

        ZFile zip = new ZFile(zipFile, options);
        byte sequences = 100;
        int seqCount = 1000;
        final byte[] compressableData = new byte[sequences * seqCount];
        for (byte i = 0; i < sequences; i++) {
            for (int j = 0; j < seqCount; j++) {
                compressableData[i * seqCount + j] = i;
            }
        }

        zip.add("compressedFile", new ByteArrayInputStream(compressableData));
        assertFalse(done[0]);

        /*
         * Even before closing, eventually all the stream will be read.
         */
        long tooLong = System.currentTimeMillis() + 10000;
        while (!done[0] && System.currentTimeMillis() < tooLong) {
            Thread.sleep(10);
        }

        assertTrue(done[0]);

        zip.close();
        executor.shutdownNow();
    }
}
