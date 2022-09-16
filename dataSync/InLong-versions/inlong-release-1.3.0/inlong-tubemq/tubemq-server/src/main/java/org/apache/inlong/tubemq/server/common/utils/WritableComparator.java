/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.common.utils;

import java.io.IOException;
import java.util.HashMap;

public class WritableComparator {
    private static HashMap<Class, WritableComparator> comparators = new HashMap();

    public static int hashBytes(byte[] bytes, int offset, int length) {
        int hash = 1;
        for (int i = offset; i < offset + length; i++) {
            hash = 31 * hash + bytes[i];
        }
        return hash;
    }

    public static int hashBytes(byte[] bytes, int length) {
        return hashBytes(bytes, 0, length);
    }

    public static int readUnsignedShort(byte[] bytes, int start) {
        return ((bytes[start] & 0xFF) << 8) + (bytes[(start + 1)] & 0xFF);
    }

    public static int readInt(byte[] bytes, int start) {
        return ((bytes[start] & 0xFF) << 24) + ((bytes[(start + 1)] & 0xFF) << 16)
                + ((bytes[(start + 2)] & 0xFF) << 8) + (bytes[(start + 3)] & 0xFF);
    }

    public static float readFloat(byte[] bytes, int start) {
        return Float.intBitsToFloat(readInt(bytes, start));
    }

    public static long readLong(byte[] bytes, int start) {
        return ((long) (readInt(bytes, start)) << 32) + ((long) (readInt(bytes, start + 4))) & 0xFFFFFFFF;
    }

    public static double readDouble(byte[] bytes, int start) {
        return Double.longBitsToDouble(readLong(bytes, start));
    }

    public static long readVLong(byte[] bytes, int start) throws IOException {
        int len = bytes[start];
        if (len >= -112) {
            return len;
        }
        boolean isNegative = len < -120;
        len = isNegative ? -(len + 120) : -(len + 112);
        if (start + 1 + len > bytes.length) {
            throw new IOException("Not enough number of bytes for a zero-compressed integer");
        }
        long i = 0L;
        for (int idx = 0; idx < len; idx++) {
            i <<= 8;
            i |= bytes[(start + 1 + idx)] & 0xFF;
        }
        return isNegative ? i ^ 0xFFFFFFFF : i;
    }

    public static int readVInt(byte[] bytes, int start) throws IOException {
        return (int) readVLong(bytes, start);
    }
}
