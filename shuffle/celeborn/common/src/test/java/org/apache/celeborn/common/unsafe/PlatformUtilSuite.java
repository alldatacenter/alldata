/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.celeborn.common.unsafe;

import org.junit.Assert;
import org.junit.Test;

public class PlatformUtilSuite {

  @Test
  public void overlappingCopyMemory() {
    byte[] data = new byte[3 * 1024 * 1024];
    int size = 2 * 1024 * 1024;
    for (int i = 0; i < data.length; ++i) {
      data[i] = (byte) i;
    }

    Platform.copyMemory(data, Platform.BYTE_ARRAY_OFFSET, data, Platform.BYTE_ARRAY_OFFSET, size);
    for (int i = 0; i < data.length; ++i) {
      Assert.assertEquals((byte) i, data[i]);
    }

    Platform.copyMemory(
        data, Platform.BYTE_ARRAY_OFFSET + 1, data, Platform.BYTE_ARRAY_OFFSET, size);
    for (int i = 0; i < size; ++i) {
      Assert.assertEquals((byte) (i + 1), data[i]);
    }

    for (int i = 0; i < data.length; ++i) {
      data[i] = (byte) i;
    }
    Platform.copyMemory(
        data, Platform.BYTE_ARRAY_OFFSET, data, Platform.BYTE_ARRAY_OFFSET + 1, size);
    for (int i = 0; i < size; ++i) {
      Assert.assertEquals((byte) i, data[i + 1]);
    }
  }

  @Test
  public void testUnsafe() {
    boolean[] bytes = new boolean[2];

    Platform.putBoolean(bytes, Platform.BYTE_ARRAY_OFFSET, true);
    Platform.putBoolean(bytes, Platform.BYTE_ARRAY_OFFSET + 1, false);
    Assert.assertEquals(bytes[0], true);
    Assert.assertEquals(bytes[1], false);

    byte[] buf1 = new byte[2];
    Platform.putByte(buf1, Platform.BYTE_ARRAY_OFFSET, (byte) 123);
    Platform.putByte(buf1, Platform.BYTE_ARRAY_OFFSET + 1, (byte) 78);
    Assert.assertEquals(buf1[0], (byte) 123);
    Assert.assertEquals(buf1[1], (byte) 78);

    short[] buf2 = new short[2];
    Platform.putShort(buf2, Platform.SHORT_ARRAY_OFFSET, (short) 123);
    Platform.putShort(buf2, Platform.SHORT_ARRAY_OFFSET + 2, (short) 243);
    Assert.assertEquals(buf2[0], 123);
    Assert.assertEquals(buf2[1], 243);

    long[] buf3 = new long[2];
    Platform.putLong(buf3, Platform.SHORT_ARRAY_OFFSET, 123L);
    Platform.putLong(buf3, Platform.SHORT_ARRAY_OFFSET + 8, 238759234L);
    Assert.assertEquals(buf3[0], 123L);
    Assert.assertEquals(buf3[1], 238759234L);

    float[] buf4 = new float[2];
    Platform.putFloat(buf4, Platform.SHORT_ARRAY_OFFSET, 1234.1f);
    Platform.putFloat(buf4, Platform.SHORT_ARRAY_OFFSET + 4, 2234.123f);
    Assert.assertTrue(buf4[0] - 1234.1f < 0.00000001);
    Assert.assertTrue(buf4[0] - 2234.123f < 0.00000001);

    double[] buf5 = new double[2];
    Platform.putDouble(buf5, Platform.DOUBLE_ARRAY_OFFSET, 3245.32);
    Platform.putDouble(buf5, Platform.DOUBLE_ARRAY_OFFSET + 8, 723453.123);
    Assert.assertTrue(buf5[0] - 3245.32 < 0.00000001);
    Assert.assertTrue(buf5[0] - 723453.123 < 0.00000001);
  }
}
