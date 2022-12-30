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

package com.bytedance.bitsail.connector.elasticsearch;

import com.bytedance.bitsail.common.type.TypeInfoConverter;
import com.bytedance.bitsail.common.typeinfo.BasicArrayTypeInfo;
import com.bytedance.bitsail.common.typeinfo.TypeInfo;
import com.bytedance.bitsail.common.typeinfo.TypeInfos;
import com.bytedance.bitsail.common.util.Pair;
import com.bytedance.bitsail.connector.elasticsearch.sink.ElasticsearchSink;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class ElasticsearchTypeConverterTest {

  private final List<Pair<TypeInfo<?>, String>> toBitSailTypePairs = Arrays.asList(
      Pair.newPair(TypeInfos.LONG_TYPE_INFO, "long"),
      Pair.newPair(TypeInfos.DOUBLE_TYPE_INFO, "double"),
      Pair.newPair(TypeInfos.INT_TYPE_INFO, "timestamp"),
      Pair.newPair(TypeInfos.SQL_DATE_TYPE_INFO, "date"),
      Pair.newPair(TypeInfos.STRING_TYPE_INFO, "varchar"),
      Pair.newPair(TypeInfos.BOOLEAN_TYPE_INFO, "boolean"),
      Pair.newPair(BasicArrayTypeInfo.BINARY_TYPE_INFO, "binary"),
      Pair.newPair(TypeInfos.STRING_TYPE_INFO, "text"),
      Pair.newPair(TypeInfos.STRING_TYPE_INFO, "keyword"),
      Pair.newPair(TypeInfos.INT_TYPE_INFO, "integer"),
      Pair.newPair(TypeInfos.INT_TYPE_INFO, "short"),
      Pair.newPair(TypeInfos.INT_TYPE_INFO, "long"),
      Pair.newPair(TypeInfos.INT_TYPE_INFO, "byte"),
      Pair.newPair(TypeInfos.DOUBLE_TYPE_INFO, "half_float"),
      Pair.newPair(TypeInfos.DOUBLE_TYPE_INFO, "scaled_float")
  );

  @Test
  public void testTypeConverter() {
    TypeInfoConverter typeInfoConverter = new ElasticsearchSink<>().createTypeInfoConverter();

    toBitSailTypePairs.forEach(pair -> {
      TypeInfo<?> typeInfo = typeInfoConverter.fromTypeString(pair.getSecond());
      Assert.assertEquals(pair.getFirst().getClass(), typeInfo.getClass());
    });
  }
}
