package com.android.tools.pixelprobe.decoder;

import com.android.tools.chunkio.RangedInputStream;
import java.io.IOException;
import java.util.LinkedList;

final class PsdDecoder$TypeToolObject$$ChunkIO {
    static PsdDecoder.TypeToolObject read(RangedInputStream in, LinkedList<Object> stack) throws IOException {
        PsdDecoder.TypeToolObject typeToolObject = new PsdDecoder.TypeToolObject();
        stack.addFirst(typeToolObject);

        int size = 0;
        long byteCount = 0;

        typeToolObject.version = in.readShort();
        typeToolObject.xx = in.readDouble();
        typeToolObject.xy = in.readDouble();
        typeToolObject.yx = in.readDouble();
        typeToolObject.yy = in.readDouble();
        typeToolObject.tx = in.readDouble();
        typeToolObject.ty = in.readDouble();
        typeToolObject.textVersion = in.readShort();
        typeToolObject.testDescriptorVersion = in.readInt();
        typeToolObject.text = PsdDecoder$Descriptor$$ChunkIO.read(in, stack);
        typeToolObject.warpVersion = in.readShort();
        typeToolObject.warpDescriptorVersion = in.readInt();
        typeToolObject.warp = PsdDecoder$Descriptor$$ChunkIO.read(in, stack);
        typeToolObject.left = in.readInt();
        typeToolObject.top = in.readInt();
        typeToolObject.right = in.readInt();
        typeToolObject.bottom = in.readInt();

        stack.removeFirst();
        return typeToolObject;
    }
}
