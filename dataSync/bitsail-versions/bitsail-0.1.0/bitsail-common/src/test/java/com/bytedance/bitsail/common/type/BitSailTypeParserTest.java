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

package com.bytedance.bitsail.common.type;

import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.typeinfo.TypeInfo;
import com.bytedance.bitsail.common.typeinfo.TypeInfoUtils;
import com.bytedance.bitsail.common.typeinfo.TypeProperty;

import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class BitSailTypeParserTest {

  @Test
  public void fromTypePropertyString() {
    String uniqTypeProperty = "unique";

    List<TypeProperty> typeProperties = BitSailTypeParser.fromTypePropertyString(uniqTypeProperty);
    Assert.assertEquals(TypeProperty.UNIQUE, typeProperties.get(0));

    String uniqWithNotNullTypeProperty = "unique,not_null";

    List<TypeProperty> uniqWithNotNullTypePropertyProperties = BitSailTypeParser
        .fromTypePropertyString(uniqWithNotNullTypeProperty);
    Assert.assertEquals(TypeProperty.UNIQUE, uniqWithNotNullTypePropertyProperties.get(0));
    Assert.assertEquals(TypeProperty.NOT_NULL, uniqWithNotNullTypePropertyProperties.get(1));
  }

  @Test
  public void fromTypePropertyForSameTypeString() {
    BitSailTypeInfoConverter bitSailTypeInfoConverter = new BitSailTypeInfoConverter();
    List<ColumnInfo> columnInfos = Lists.newArrayList();
    ColumnInfo columnInfo1 = ColumnInfo
        .builder()
        .type("bigint")
        .name("bigint_1")
        .build();
    columnInfo1.setProperties("unique");
    ColumnInfo columnInfo2 = ColumnInfo
        .builder()
        .type("bigint")
        .name("bigint_2")
        .build();
    columnInfos.add(columnInfo1);
    columnInfos.add(columnInfo2);

    TypeInfo<?>[] typeInfos = TypeInfoUtils.getTypeInfos(bitSailTypeInfoConverter, columnInfos);
    Assert.assertEquals(CollectionUtils.size(typeInfos[0].getTypeProperties()), 1);
    Assert.assertEquals(typeInfos[0].getTypeProperties().get(0), TypeProperty.UNIQUE);
    Assert.assertEquals(CollectionUtils.size(typeInfos[1].getTypeProperties()), 0);
  }
}