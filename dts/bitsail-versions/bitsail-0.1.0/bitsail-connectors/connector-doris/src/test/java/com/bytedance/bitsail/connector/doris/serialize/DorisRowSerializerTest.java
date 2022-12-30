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

package com.bytedance.bitsail.connector.doris.serialize;

import com.bytedance.bitsail.common.row.Row;
import com.bytedance.bitsail.common.row.RowKind;
import com.bytedance.bitsail.common.util.FastJsonUtil;
import com.bytedance.bitsail.connector.doris.config.DorisOptions;
import com.bytedance.bitsail.connector.doris.converter.DorisRowConverter;
import com.bytedance.bitsail.connector.doris.typeinfo.DorisDataType;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

@SuppressWarnings("checkstyle:LineLength")
public class DorisRowSerializerTest extends DorisRowSerializer {

  private final int columnSize = 16;
  private final String[] fields = {"f_bigint", "f_varchar", "f_null", "f_boolean", "f_decimal", "f_interval_year_mon",
      "f_interval_day_time", "f_float", "f_double", "f_date", "f_int"};
  private final DorisDataType[] types = {
      DorisDataType.BIGINT,
      DorisDataType.VARCHAR,
      DorisDataType.NULL,
      DorisDataType.BOOLEAN,
      DorisDataType.DECIMAL,
      DorisDataType.INTERVAL_YEAR_MONTH,
      DorisDataType.INTERVAL_DAY_TIME,
      DorisDataType.FLOAT,
      DorisDataType.DOUBLE,
      DorisDataType.DATE,
      DorisDataType.INTEGER
  };

  public Row getDtsRow() {
    Row dtsRow = new Row(columnSize);
    dtsRow.setKind(RowKind.INSERT);
    dtsRow.setField(0, new BigInteger("1"));
    dtsRow.setField(1, "dts");
    dtsRow.setField(2, null);
    dtsRow.setField(3, true);
    dtsRow.setField(4, new BigDecimal("102921.2312314"));
    dtsRow.setField(5, 10);
    dtsRow.setField(6, 5);
    dtsRow.setField(7, 1.23123f);
    dtsRow.setField(8, 12.412123d);
    dtsRow.setField(9, 20221203);
    dtsRow.setField(10, 1);
    return dtsRow;
  }

  public DorisRowSerializerTest() {
    super();
  }

  @Before
  public void init() {
    this.fieldNames = fields;
    this.dataTypes = types;
    this.type = DorisOptions.LOAD_CONTENT_TYPE.JSON;
    this.objectMapper = new ObjectMapper();
    this.fieldDelimiter = ",";
    this.enableDelete = true;
    this.rowConverter = new DorisRowConverter(dataTypes);
  }

  @Test
  public void testEnableDeletedJsonSerialize() throws IOException {
    String expectedSerializedStr = "{\"f_boolean\":\"true\",\"f_interval_year_mon\":\"10\",\"f_varchar\":\"dts\",\"f_float\":\"1.23123\",\"__DORIS_DELETE_SIGN__\":\"0\",\"f_date\":\"\\u0000333-10-10\",\"f_bigint\":\"1\",\"f_double\":\"12.412123\",\"f_decimal\":\"102921.2312314\",\"f_null\":null,\"f_interval_day_time\":\"5\",\"f_int\":\"1\"}";
    Assert.assertTrue(checkJsonStringEqual(expectedSerializedStr, this.serialize(getDtsRow())));
  }

  @Test
  public void testDisableDeletedJsonSerialize() throws IOException {
    this.enableDelete = false;
    String expectedSerializedStr = "{\"f_boolean\":\"true\",\"f_interval_year_mon\":\"10\",\"f_varchar\":\"dts\",\"f_float\":\"1.23123\"," +
        "\"f_date\":\"\\u0000333-10-10\",\"f_bigint\":\"1\",\"f_double\":\"12.412123\",\"f_decimal\":\"102921.2312314\",\"f_null\":null,\"f_interval_day_time\":\"5\",\"f_int\":\"1\"}";
    Assert.assertTrue(checkJsonStringEqual(expectedSerializedStr, this.serialize(getDtsRow())));
  }

  @Test
  public void testEnableDeletedCsvSerialize() throws IOException {
    this.type = DorisOptions.LOAD_CONTENT_TYPE.CSV;
    String expectedSerializedStr = "1,dts,\\N,true,102921.2312314,10,5,1.23123,12.412123,\u0000333-10-10,1,0";
    Assert.assertEquals(expectedSerializedStr, this.serialize(getDtsRow()));
  }

  @Test
  public void testDisableDeletedCsvSerialize() throws IOException {
    this.type = DorisOptions.LOAD_CONTENT_TYPE.CSV;
    this.enableDelete = false;
    String expectedSerializedStr = "1,dts,\\N,true,102921.2312314,10,5,1.23123,12.412123,\u0000333-10-10,1";
    Assert.assertEquals(expectedSerializedStr, this.serialize(getDtsRow()));
  }

  private boolean checkJsonStringEqual(String jsonStr1, String jsonStr2) {
    JSONObject jsonObject1 = FastJsonUtil.parseObject(jsonStr1);
    JSONObject jsonObject2 = FastJsonUtil.parseObject(jsonStr2);
    return jsonObject1.equals(jsonObject2);
  }
}
