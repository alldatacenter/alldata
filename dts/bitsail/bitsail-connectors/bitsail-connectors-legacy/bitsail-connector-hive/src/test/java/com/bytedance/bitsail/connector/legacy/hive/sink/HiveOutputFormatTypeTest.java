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

package com.bytedance.bitsail.connector.legacy.hive.sink;

import com.bytedance.bitsail.common.column.Column;
import com.bytedance.bitsail.common.column.DateColumn;
import com.bytedance.bitsail.conversion.hive.BitSailColumnConversion;
import com.bytedance.bitsail.conversion.hive.ConvertToHiveObjectOptions;
import com.bytedance.bitsail.conversion.hive.extractor.GeneralWritableExtractor;

import org.apache.hadoop.io.BytesWritable;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created 2022/7/22
 */
public class HiveOutputFormatTypeTest {

  @Test
  public void dateTypeToBigIntTypeCreateIntOrBigIntWritableTestGeneralExtractor() {
    GeneralWritableExtractor generalWritableExtractor = new GeneralWritableExtractor();
    /* to BigInt case */
    boolean intOrBigint = false; // true:IntWritable; false:LongWritable

    ConvertToHiveObjectOptions options = ConvertToHiveObjectOptions.builder()
        .convertErrorColumnAsNull(false)
        .dateTypeToStringAsLong(false)
        .nullStringAsNull(false)
        .useStringText(false)
        .datePrecision(ConvertToHiveObjectOptions.DatePrecision.SECOND)
        .build();
    generalWritableExtractor.initConvertOptions(options);
    Column column = new DateColumn();
    Object object = BitSailColumnConversion.toIntOrBigintHiveObject(column, intOrBigint);
    assertNull(object);

    column = new DateColumn(1539492540123L);
    object = BitSailColumnConversion.toIntOrBigintHiveObject(column, intOrBigint);
    assertEquals(object, 1539492540L);

    options.setDatePrecision(ConvertToHiveObjectOptions.DatePrecision.MILLISECOND);
    generalWritableExtractor.initConvertOptions(options);
    column = new DateColumn();
    object = BitSailColumnConversion.toIntOrBigintHiveObject(column, intOrBigint);
    assertNull(object);

    column = new DateColumn(1539492540123L);
    object = BitSailColumnConversion.toIntOrBigintHiveObject(column, intOrBigint);
    assertEquals(object, 1539492540123L);
  }

  private String getStringFromByteWritable(BytesWritable writable) {
    byte[] value = writable.getBytes();
    return new String(value);
  }
}
