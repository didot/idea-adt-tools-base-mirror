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
 *
 * THIS FILE WAS GENERATED BY codergen. EDIT WITH CARE.
 */
package com.android.tools.rpclib.rpccore;

import org.jetbrains.annotations.NotNull;

import com.android.tools.rpclib.binary.*;
import com.android.tools.rpclib.schema.*;

import java.io.IOException;
import java.util.Arrays;

public final class ErrInvalidHeader extends RpcException implements BinaryObject {
    //<<<Start:Java.ClassBody:1>>>
    private byte[] mHeader;

    // Constructs a default-initialized {@link ErrInvalidHeader}.
    public ErrInvalidHeader() {}


    public byte[] getHeader() {
        return mHeader;
    }

    public ErrInvalidHeader setHeader(byte[] v) {
        mHeader = v;
        return this;
    }

    @Override @NotNull
    public BinaryClass klass() { return Klass.INSTANCE; }


    private static final Entity ENTITY = new Entity("rpc", "ErrInvalidHeader", "", "");

    static {
        ENTITY.setFields(new Field[]{
            new Field("Header", new Array("", new Primitive("byte", Method.Uint8), 4)),
        });
        Namespace.register(Klass.INSTANCE);
    }
    public static void register() {}
    //<<<End:Java.ClassBody:1>>>

    @Override
    public String getMessage() {
        return "Invalid RPC header: " + Arrays.toString(mHeader);
    }

    public enum Klass implements BinaryClass {
        //<<<Start:Java.KlassBody:2>>>
        INSTANCE;

        @Override @NotNull
        public Entity entity() { return ENTITY; }

        @Override @NotNull
        public BinaryObject create() { return new ErrInvalidHeader(); }

        @Override
        public void encode(@NotNull Encoder e, BinaryObject obj) throws IOException {
            ErrInvalidHeader o = (ErrInvalidHeader)obj;
            e.write(o.mHeader, 4);

        }

        @Override
        public void decode(@NotNull Decoder d, BinaryObject obj) throws IOException {
            ErrInvalidHeader o = (ErrInvalidHeader)obj;
            d.read(o.mHeader, o.mHeader.length);
        }
        //<<<End:Java.KlassBody:2>>>
    }
}
