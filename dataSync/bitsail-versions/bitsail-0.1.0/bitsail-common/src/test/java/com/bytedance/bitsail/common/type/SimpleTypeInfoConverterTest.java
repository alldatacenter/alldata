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

package com.bytedance.bitsail.common.type;

import com.bytedance.bitsail.common.type.filemapping.FileMappingTypeInfoConverter;
import com.bytedance.bitsail.common.type.filemapping.FileMappingTypeInfoReader;
import com.bytedance.bitsail.common.typeinfo.TypeInfo;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

/**
 * Created 2022/8/23
 */
public class SimpleTypeInfoConverterTest {

  @Test
  public void testSimpleTypeInfoConverter() {
    FileMappingTypeInfoConverter fileMappingTypeInfoConverter = new FileMappingTypeInfoConverter("fake");
    FileMappingTypeInfoReader reader = fileMappingTypeInfoConverter.getReader();
    Map<String, TypeInfo<?>> toTypeInformation = reader.getToTypeInformation();
    Map<TypeInfo<?>, String> fromTypeInformation = reader.getFromTypeInformation();

    Assert.assertEquals(toTypeInformation.size(), 8);
    Assert.assertEquals(fromTypeInformation.size(), 6);
  }

}