/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.inlong.sdk.sort.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.xerial.snappy.Snappy;

public class Utils {

    public static final long MINUTE_IN_MILLIS = 60 * 1000;

    public static final long HOUR_IN_MILLIS = 60 * 60 * 1000;

    public static final long DAY_IN_MILLIS = 24 * 60 * 60 * 1000;


    private static ConcurrentHashMap<String, AtomicLong> timeMap = new ConcurrentHashMap<String, AtomicLong>();

    public static double toKB(long bytes) {
        return bytes / 1024d;
    }

    public static double toMB(long bytes) {
        return bytes / 1024d / 1024d;
    }

    public static double toGB(long bytes) {
        return bytes / 1024d / 1024d / 1024d;
    }

    public static String ipInt2String(int ipInt) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) ((ipInt >>> 24) & 0xFF);
        bytes[1] = (byte) ((ipInt >>> 16) & 0xFF);
        bytes[2] = (byte) ((ipInt >>> 8) & 0xFF);
        bytes[3] = (byte) (ipInt & 0xFF);
        return "" + (bytes[0] & 0x00FF) + '.' + (bytes[1] & 0x00FF) + '.' + (bytes[2] & 0x00FF)
                + '.' + (bytes[3] & 0x00FF);
    }

    public static String bytesToStr(byte[] value, String charsetName, String defaultValue) {
        if (value == null || value.length == 0) {
            return defaultValue;
        }
        try {
            String str = new String(value, charsetName);
            return str;
        } catch (UnsupportedEncodingException e) {
            return defaultValue;
        }
    }

    public static <T> Set<T> toSet(T... values) {
        Set<T> result = new HashSet<T>();
        for (T v : values) {
            result.add(v);
        }
        return result;
    }

    public static <T> List<T> toList(T... values) {
        List<T> result = new ArrayList<T>();
        for (T v : values) {
            result.add(v);
        }
        return result;
    }

    public static String exceptionMsg(Throwable exception) {
        if (exception == null) {
            return "";
        }
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(exception.getClass().getSimpleName() + ",");
            sb.append(exception.getMessage());

            Throwable cause = exception.getCause();
            if (cause != null) {
                sb.append("," + cause.getMessage());
            }
            return sb.toString();
        } catch (Throwable e) {
            return "";
        }
    }

    public static String doubleToStr(double value, int fractionDigits) {
        NumberFormat format = NumberFormat.getInstance();
        format.setMinimumFractionDigits(fractionDigits);
        String str = format.format(value);
        return str;
    }

    public static String getUUID() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString().replaceAll("-", "");

    }

    public static String toStr(String splitChar, Object... objs) {
        StringBuilder sb = new StringBuilder();
        int size = objs.length;
        int lastIndex = size - 1;
        for (int i = 0; i < size; i++) {
            sb.append(objs[i]);
            if (i < lastIndex) {
                sb.append(splitChar);
            }
        }
        return sb.toString();
    }

    public static <E> List<List<E>> splitList(List<E> list, int splitSize) {
        List<List<E>> result = new ArrayList<List<E>>();
        if (list == null || list.size() <= splitSize) {
            return result;
        }

        int size = 0;
        List<E> subList = new ArrayList<E>();
        for (E e : list) {
            if ((size++) > splitSize) {
                result.add(subList);

                size = 0;
                subList = new ArrayList<E>();
            } else {
                subList.add(e);
            }
        }

        return result;
    }

    public static byte[] compressGZip(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip;
        try {
            gzip = new GZIPOutputStream(out);
            gzip.write(data);
            gzip.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    public static byte[] gzipDecompress(byte[] data, int startOffset, int dataLength)
            throws IOException {
        GZIPInputStream gzip = null;
        int initBufSize = 8192;
        try {
            //int inputLength=(data.length-(startOffset==0? 0:startOffset));
            ByteArrayInputStream in = new ByteArrayInputStream(data, startOffset, dataLength);
            gzip = new GZIPInputStream(in, 8192);

            byte[] buf = new byte[initBufSize];
            int offset = 0;
            do {
                int readNum = gzip.read(buf, offset, 8192);
                if (readNum == -1) {
                    break;
                } else {
                    offset += readNum;
                    if (offset + 8192 > (buf.length - 1)) {
                        buf = Arrays.copyOf(buf, buf.length * 2);
                    }
                }
            } while (true);

            return Arrays.copyOf(buf, offset);
        } finally {
            if (gzip != null) {
                gzip.close();
            }
        }
    }

    public static byte[] snappyCompress(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            return null;
        }

        return Snappy.compress(data);
    }

    public static byte[] snappyDecompress(byte[] data, int startOffset, int dataLength)
            throws IOException {
        int needSize = Snappy.uncompressedLength(data, startOffset, dataLength);
        byte[] result = new byte[needSize];

        Snappy.uncompress(data, startOffset, dataLength, result, 0);
        return result;
    }

    public static byte[] toBytes(ByteBuffer buffer) {
        if (buffer == null || buffer.remaining() == 0) {
            return new byte[0];
        }
        int remainBytes = buffer.remaining();
        byte[] value = new byte[buffer.remaining()];

        if (buffer.hasArray()) {
            System.arraycopy(buffer.array(), buffer.arrayOffset(), value, 0, remainBytes);
        } else {
            buffer.get(value, 0, remainBytes);
        }
        return value;
    }

    public static double div(double d1, double d2) {
        return (d2 != 0 ? (d1 / d2) : 0);
    }

    public static String nowToStr() {
        return StringUtil.formatDate(new Date(), "yyyy-MM-dd HH:mm:ss.SSS");
    }

    public static <K, V> V getMapValue(Map<K, V> map, K k, V defaultV) {
        if (map == null) {
            return defaultV;
        }
        V v = map.get(k);
        return (v != null ? v : defaultV);
    }

}
