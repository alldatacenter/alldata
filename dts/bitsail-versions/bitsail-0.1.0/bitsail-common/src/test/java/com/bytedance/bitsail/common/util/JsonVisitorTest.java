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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

@Slf4j
public class JsonVisitorTest {

  @Test
  public void testGetPathValue_true() throws IOException {

    String x = "{\"A\":1}";
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode actualObj = objectMapper.readTree(x);
    JsonNode a =
        JsonVisitor.getPathValue(actualObj, FieldPathUtils.PathInfo.builder().nestPathInfo(null).pathType(FieldPathUtils.PathType.OTHER).name("a").build(), true);
    Assert.assertNotNull(a);
    Assert.assertEquals(1, a.asInt());

  }

  @Test
  public void testGetPathValueNested_true() throws IOException {

    String input1 = "{\"A\":{\"b\" : 1}}";
    String input2 = "{\"A\":{\"B\" : 1}}";
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode actualObj1 = objectMapper.readTree(input1);
    JsonNode actualObj2 = objectMapper.readTree(input2);
    JsonNode result1 = JsonVisitor.getPathValue(actualObj1,
        FieldPathUtils.PathInfo.builder().nestPathInfo(FieldPathUtils.PathInfo.builder().nestPathInfo(null).pathType(FieldPathUtils.PathType.OTHER).name("b").build())
            .pathType(FieldPathUtils.PathType.OTHER).name("a").build(), true);
    JsonNode result2 = JsonVisitor.getPathValue(actualObj2,
        FieldPathUtils.PathInfo.builder().nestPathInfo(FieldPathUtils.PathInfo.builder().nestPathInfo(null).pathType(FieldPathUtils.PathType.OTHER).name("b").build())
            .pathType(FieldPathUtils.PathType.OTHER).name("a").build(), true);
    Assert.assertNotNull(result1);
    Assert.assertNotNull(result2);
    Assert.assertEquals(1, result1.asInt());
    Assert.assertEquals(1, result2.asInt());

  }

  @Test
  public void testGetPathValue_false() throws IOException {

    String x = "{\"A\":1}";
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode actualObj = objectMapper.readTree(x);
    JsonNode a =
        JsonVisitor.getPathValue(actualObj, FieldPathUtils.PathInfo.builder().nestPathInfo(null).pathType(FieldPathUtils.PathType.OTHER).name("a").build(), false);
    Assert.assertNull(a);

  }

}
