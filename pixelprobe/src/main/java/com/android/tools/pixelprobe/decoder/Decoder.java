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

package com.android.tools.pixelprobe.decoder;

import com.android.tools.pixelprobe.ColorMode;
import com.android.tools.pixelprobe.Image;
import com.android.tools.pixelprobe.util.Strings;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

/**
 * Abstract image decoder interface. A decoder can be used to generate
 * an Image from an input stream. They also provide the ability to
 * indicate whether they are able to decode a specific stream using
 * the accept() method. A decoder can also be selected by checking for
 * supported formats.
 */
public abstract class Decoder {
    private final Set<String> formats = new HashSet<>();

    /**
     * Creates a new decoder that supports the specified list of formats.
     *
     * @param formats A list of formats
     */
    public Decoder(String... formats) {
        for (String format : formats) {
            this.formats.add(format.toLowerCase(Locale.ROOT));
        }
    }

    /**
     * Returns true if the specified format is supported by this decoder.
     * The format comparison is case insensitive.
     *
     * @param format A format string
     */
    public boolean accept(String format) {
        return formats.contains(format.toLowerCase(Locale.ROOT));
    }

    /**
     * Returns true if the specified input stream contains data that
     * can be decoded with this decoder. It is the caller's responsibility
     * to rewind the stream after calling this method.
     *
     * @param in An input stream
     */
    public abstract boolean accept(InputStream in);

    /**
     * Decodes the specified input stream into an Image.
     *
     * @param in Input stream to decode
     *
     * @return An Image instance, never null. Might be marked invalid if
     * an error occurred during the decoding process.
     */
    public Image decode(InputStream in) throws IOException {
        ImageInputStream stream = ImageIO.createImageInputStream(in);

        ImageReader reader = getImageReader(stream);
        ImageReadParam parameters = reader.getDefaultReadParam();
        reader.setInput(stream, true, false);

        BufferedImage image;
        try {
            image = reader.read(0, parameters);
        } catch (IIOException e) {
            throw new IOException(e);
        }

        ColorModel colorModel = image.getColorModel();
        ColorSpace colorSpace = colorModel.getColorSpace();

        Image.Builder builder = new Image.Builder()
                .format(reader.getFormatName())
                .dimensions(image.getWidth(), image.getHeight())
                .mergedImage(image)
                .colorMode(getColorMode(colorSpace))
                .colorSpace(colorSpace)
                .depth(colorModel.getComponentSize(0));

        IIOMetadata metadata = reader.getImageMetadata(0);
        if (metadata != null) {
            decodeMetadata(builder, metadata);
        }

        reader.dispose();
        stream.close();

        return builder.build();
    }

    /**
     * Invoked by the default {@link #decode(InputStream)} implementation
     * to let the decoder extract metadata from the image stream.
     *
     * @param builder The image builder where to stored decoded metadata
     * @param metadata The image stream metadata
     */
    public void decodeMetadata(Image.Builder builder, IIOMetadata metadata) {
    }

    private static ImageReader getImageReader(ImageInputStream stream) throws IOException {
        Iterator readerIterator = ImageIO.getImageReaders(stream);
        if (!readerIterator.hasNext()) {
            throw new IOException("Unknown image format");
        }
        return (ImageReader) readerIterator.next();
    }

    private static ColorMode getColorMode(ColorSpace colorSpace) {
        switch (colorSpace.getType()) {
            case ColorSpace.TYPE_CMYK:
                return ColorMode.CMYK;
            case ColorSpace.TYPE_GRAY:
                return ColorMode.GRAYSCALE;
            case ColorSpace.TYPE_Lab:
                return ColorMode.LAB;
            case ColorSpace.TYPE_RGB:
                return ColorMode.RGB;
        }
        return ColorMode.UNKNOWN;
    }

    @Override
    public String toString() {
        return "Decoder{" +
                "formats={" + Strings.join(formats, ",") + '}' +
                '}';
    }
}
