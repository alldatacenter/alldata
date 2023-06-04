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

package com.bytedance.bitsail.batch.file.parser;

import com.bytedance.bitsail.common.column.Column;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.util.FastJsonUtil;
import com.bytedance.bitsail.flink.core.typeinfo.PrimitiveColumnTypeInfo;

import com.alibaba.fastjson.JSONObject;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JsonBytesParserTest {

  @Test
  public void testConvertJsonKeyToLowerCase() throws Exception {
    JsonBytesParser jbp = new JsonBytesParser(BitSailConfiguration.from("{}"));

    String s = "{\"A\": [{\"E\": [{\"A\": 3, \"B\": 4}, {\"C\": 5, \"D\": 6}]}]}";
    String result = "{\"a\":[{\"e\":[{\"a\":3,\"b\":4},{\"c\":5,\"d\":6}]}]}";

    JSONObject jo = (JSONObject) FastJsonUtil.parse(s);
    Object obj = jbp.convertJsonKeyToLowerCase(jo);

    assertEquals(result, obj.toString());
  }

  @Test
  public void testWriteMapNullSerializeFeature() throws Exception {

    JsonBytesParser parser = new JsonBytesParser(BitSailConfiguration.from("\n" +
        "{\n" +
        "  \"job\" : {\n" +
        "      \"common\" : {\n" +
        "          \"json_serializer_features\" : \"WriteMapNullValue\"\n" +
        "      }\n" +
        "  }\n" +
        "}"));
    Row row = new Row(1);
    RowTypeInfo rowTypeInfo = new RowTypeInfo(new TypeInformation[] {PrimitiveColumnTypeInfo.STRING_COLUMN_TYPE_INFO}, new String[] {"a"});

    Row result = parser.parse(row, "{\"a\": { \"b\" : null, \"c\" : 1 }}".getBytes(),
        rowTypeInfo);

    Assert.assertEquals("{\"b\":null,\"c\":1}", ((Column) result.getField(0)).asString());
  }

  @Test
  public void testWithoutWriteMapNullSerializeFeature() throws Exception {

    JsonBytesParser parser = new JsonBytesParser(BitSailConfiguration.from("{}"));
    Row row = new Row(1);
    RowTypeInfo rowTypeInfo = new RowTypeInfo(new TypeInformation[] {PrimitiveColumnTypeInfo.STRING_COLUMN_TYPE_INFO}, new String[] {"a"});

    Row result = parser.parse(row, "{\"a\": { \"b\" : null, \"c\" : 1 }}".getBytes(),
        rowTypeInfo);

    Assert.assertEquals("{\"c\":1}", ((Column) result.getField(0)).asString());
  }
}
