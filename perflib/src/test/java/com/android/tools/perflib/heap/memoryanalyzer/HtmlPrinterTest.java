// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.android.tools.perflib.heap.memoryanalyzer;

import com.android.ddmlib.BitmapDecoder;
import com.android.tools.perflib.heap.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link HtmlPrinter}.
 */
@RunWith(JUnit4.class)
public final class HtmlPrinterTest {

    @Mock
    private Instance mInstanceMock;
    @Mock
    private ClassObj mClassObjMock;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ClassInstance mBitmapClassInstanceMock;
    @Mock
    private ArrayInstance mBufferMock;

    private ByteArrayOutputStream mByteStream;
    private HtmlPrinter mPrinter;

    @Before
    public void setUp() {
        mByteStream = new ByteArrayOutputStream();
        mPrinter = new HtmlPrinter(new PrintStream((mByteStream)));

        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testAddHeading() throws Exception {
        // act
        mPrinter.addHeading(1, "test heading");

        // assert
        String out = new String(mByteStream.toByteArray(), StandardCharsets.UTF_8);
        assertEquals(out, "<h1>test heading</h1>\n");
    }

    @Test
    public void testAddParagraph() throws Exception {
        // act
        mPrinter.addParagraph("test paragraph");

        // assert
        String out = new String(mByteStream.toByteArray(), StandardCharsets.UTF_8);
        assertEquals(out, "<p>test paragraph</p>\n");
    }

    @Test
    public void testTable() throws Exception {
        // act
        mPrinter.startTable("test row heading");
        mPrinter.addRow("test data");
        mPrinter.endTable();

        // assert
        String out = new String(mByteStream.toByteArray(), StandardCharsets.UTF_8);
        assertEquals(out,
                "<table>\n"
                        + "<tr style='border: 1px solid black;'>\n"
                        + "<th style='border: 1px solid black;'>test row heading</th>\n"
                        + "</tr>\n"
                        + "<tr>\n"
                        + "<td>test data</td>\n"
                        + "</tr>\n"
                        + "</table>\n");
    }

    @Test
    public void testTable_noHeadings() throws Exception {
        // act
        mPrinter.startTable();
        mPrinter.addRow("test data");
        mPrinter.endTable();

        // assert
        String out = new String(mByteStream.toByteArray(), StandardCharsets.UTF_8);
        assertEquals(out,
                "<table>\n"
                        + "<tr>\n"
                        + "<td>test data</td>\n"
                        + "</tr>\n"
                        + "</table>\n");
    }

    @Test
    public void testAddImage() throws Exception {
        // arrange
        Mockito.when(mBufferMock.getArrayType()).thenReturn(Type.BYTE);
        Mockito.when(mBufferMock.asRawByteArray(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt()))
                .thenReturn(new byte[]{0, 0, 0, 0});
        Mockito.when(mBufferMock.getLength()).thenReturn(4);
        List<ClassInstance.FieldValue> fields = new ArrayList<>();
        fields.add(new ClassInstance.FieldValue(new Field(Type.OBJECT, "mBuffer"), mBufferMock));
        fields.add(new ClassInstance.FieldValue(new Field(Type.BOOLEAN, "mIsMutable"),
                new Boolean(true)));
        fields.add(new ClassInstance.FieldValue(new Field(Type.INT, "mWidth"), new Integer(1)));
        fields.add(new ClassInstance.FieldValue(new Field(Type.INT, "mHeight"), new Integer(1)));
        ClassObj bitmapClassObj = new ClassObj(0L, new StackTrace(0, 0, new StackFrame[0]),
                BitmapDecoder.BITMAP_FQCN, 0L);
        Mockito.when(mBitmapClassInstanceMock.getClassObj()).thenReturn(bitmapClassObj);
        Mockito.when(mBitmapClassInstanceMock.getValues()).thenReturn(fields);

        // act
        mPrinter.addImage(mBitmapClassInstanceMock);

        // assert
        String out = new String(mByteStream.toByteArray(), StandardCharsets.UTF_8);
        System.err.println(out);
        assertEquals(out,
                "<img src='data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAA"
                        + "C0lEQVR42mNgAAIAAAUAAen63NgAAAAASUVORK5CYII=' \\>\n");
    }

    @Test
    public void testFormatInstance() throws Exception {
        // arrange
        Mockito.when(mInstanceMock.toString()).thenReturn("mock instance");

        // act
        String out = mPrinter.formatInstance(mInstanceMock);

        // assert
        assertEquals(out, "mock instance");
    }
}
