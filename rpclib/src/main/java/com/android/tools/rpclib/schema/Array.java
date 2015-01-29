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
package com.android.tools.rpclib.schema;

import com.android.tools.rpclib.rpc.ArrayInfo;

/**
 * An array of elements unpacked using a schema description.
 */
public class Array {
  public final ArrayInfo info;
  public final Object[] elements;

  public Array(ArrayInfo info, Object[] elements) {
    this.info = info;
    this.elements = elements;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (int i = 0; i < elements.length; i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(elements[i].toString());
    }
    sb.append("]");
    return sb.toString();
  }
}
