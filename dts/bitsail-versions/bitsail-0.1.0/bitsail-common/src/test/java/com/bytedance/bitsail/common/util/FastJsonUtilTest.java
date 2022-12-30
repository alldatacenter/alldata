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

package com.bytedance.bitsail.common.util;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.google.common.collect.ImmutableList;
import lombok.AllArgsConstructor;
import lombok.val;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class FastJsonUtilTest {
  @Test
  public void testParse() {

    String s = "{\"@type\":[{\"E\":[{\"@type\":3, \"B\":4}, {\"C\":5,\"D\":6}]}]}";
    String result = "{\"@type\":[{\"E\":[{\"B\":4,\"@type\":3},{\"C\":5,\"D\":6}]}]}";

    JSONObject jo = (JSONObject) FastJsonUtil.parse(s);
    assertEquals(result, jo.toString());
  }

  @Test
  public void testParseObject() {
    String s = "{\"@type\":[{\"E\":[{\"@type\":3, \"B\":4}, {\"C\":5,\"D\":6}]}]}";
    String result = "{\"@type\":[{\"E\":[{\"B\":4,\"@type\":3},{\"C\":5,\"D\":6}]}]}";

    JSONObject jo = FastJsonUtil.parseObject(s);
    assertEquals(result, jo.toString());
  }

  @Test
  public void testParseObjectClazz() {

    String s = "{\"@type\":\"test\", \"name\": \"col\", \"type\": \"string\"}";
    TestClass test = FastJsonUtil.parseObject(s, TestClass.class);
    assertEquals(test.name, "col");
    assertEquals(test.type, "string");
  }

  @Test
  public void testParseSerializerFeaturesFromConfig() {
    String noConfig = "";
    val noFeatures = FastJsonUtil.parseSerializerFeaturesFromConfig(noConfig).toArray(new SerializerFeature[0]);
    assertEquals("[]", Arrays.toString(noFeatures));

    String rightConfig = "WriteMapNullValue ,  WriteNullNumberAsZero";
    val rightFeatures = FastJsonUtil.parseSerializerFeaturesFromConfig(rightConfig).toArray(new SerializerFeature[0]);
    assertEquals("[WriteMapNullValue, WriteNullNumberAsZero]", Arrays.toString(rightFeatures));

    String wrongConfig = "writemapnullvalue ,  writenullnumberaszero";
    try {
      FastJsonUtil.parseSerializerFeaturesFromConfig(wrongConfig);
    } catch (Exception e) {
      assertEquals("No enum constant com.alibaba.fastjson.serializer.SerializerFeature.writemapnullvalue", e.getMessage());
    }
  }

  @Test
  public void testToStringUsingSerializer() {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("a", "aaa");
    jsonObject.put("b", null);
    val list = ImmutableList.of("bb", "cc");
    jsonObject.put("c", list);

    val noFeatures = new SerializerFeature[] {};
    assertEquals("{\"a\":\"aaa\",\"c\":[\"bb\",\"cc\"]}",
        JSONObject.toJSONString(jsonObject, noFeatures));

    val singleFeature = new SerializerFeature[] {SerializerFeature.WriteMapNullValue};
    assertEquals("{\"a\":\"aaa\",\"b\":null,\"c\":[\"bb\",\"cc\"]}",
        JSONObject.toJSONString(jsonObject, singleFeature));
  }

  @AllArgsConstructor
  public static class TestClass {
    String name;
    String type;
  }
}
