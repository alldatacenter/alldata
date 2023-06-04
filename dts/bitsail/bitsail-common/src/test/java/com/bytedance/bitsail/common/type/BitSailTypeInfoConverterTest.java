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

import com.bytedance.bitsail.common.typeinfo.TypeInfo;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public class BitSailTypeInfoConverterTest {

  private BitSailTypeInfoConverter converter;

  @Before
  public void before() {
    this.converter = new BitSailTypeInfoConverter();
  }

  @Test
  public void testMapType() {
    TypeInfo<?> typeInfo = converter.fromTypeString("map<string,map<string,string>>");
    Assert.assertEquals(typeInfo.getTypeClass(), Map.class);
  }

  @Test
  public void testListType() {
    TypeInfo<?> typeInfo = converter.fromTypeString("list<string>");
    Assert.assertEquals(typeInfo.getTypeClass(), List.class);
  }

  @Test
  public void testBooleanType() {
    TypeInfo<?> typeInfo = converter.fromTypeString("boolean");
    Assert.assertEquals(typeInfo.getTypeClass(), Boolean.class);
  }

  @Test
  public void testLocalTimeType() {
    TypeInfo<?> typeInfo = converter.fromTypeString("time");
    Assert.assertEquals(typeInfo.getTypeClass(), LocalTime.class);
  }

  @Test
  public void testLocalDateTimeType() {
    TypeInfo<?> typeInfo = converter.fromTypeString("timestamp");
    Assert.assertEquals(typeInfo.getTypeClass(), LocalDateTime.class);
  }

  @Test
  public void testLocalDateType() {
    TypeInfo<?> typeInfo = converter.fromTypeString("date");
    Assert.assertEquals(typeInfo.getTypeClass(), LocalDate.class);
  }

  @Test
  public void testSqlDateType() {
    TypeInfo<?> typeInfo = converter.fromTypeString("date.date");
    Assert.assertEquals(typeInfo.getTypeClass(), Date.class);
  }

  @Test
  public void testSqlTimeType() {
    TypeInfo<?> typeInfo = converter.fromTypeString("date.time");
    Assert.assertEquals(typeInfo.getTypeClass(), Time.class);
  }

  @Test
  public void testSqlTimestampType() {
    TypeInfo<?> typeInfo = converter.fromTypeString("date.datetime");
    Assert.assertEquals(typeInfo.getTypeClass(), Timestamp.class);
  }

  @Test
  public void testStringType() {
    TypeInfo<?> typeInfo = converter.fromTypeString("string");
    Assert.assertEquals(typeInfo.getTypeClass(), String.class);
  }

  @Test
  public void testNumberType() {
    TypeInfo<?> longTypeInfo = converter.fromTypeString("long");
    Assert.assertEquals(longTypeInfo.getTypeClass(), Long.class);

    TypeInfo<?> intTypeInfo = converter.fromTypeString("int");
    Assert.assertEquals(intTypeInfo.getTypeClass(), Integer.class);

    TypeInfo<?> shortTypeInfo = converter.fromTypeString("short");
    Assert.assertEquals(shortTypeInfo.getTypeClass(), Short.class);

    TypeInfo<?> floatTypeInfo = converter.fromTypeString("float");
    Assert.assertEquals(floatTypeInfo.getTypeClass(), Float.class);

    TypeInfo<?> doubleTypeInfo = converter.fromTypeString("double");
    Assert.assertEquals(doubleTypeInfo.getTypeClass(), Double.class);

    TypeInfo<?> bigDecimalTypeInfo = converter.fromTypeString("bigdecimal");
    Assert.assertEquals(bigDecimalTypeInfo.getTypeClass(), BigDecimal.class);

    TypeInfo<?> bigIntegerTypeInfo = converter.fromTypeString("biginteger");
    Assert.assertEquals(bigIntegerTypeInfo.getTypeClass(), BigInteger.class);
  }

  @Test
  public void testByteType() {
    TypeInfo<?> typeInfo = converter.fromTypeString("byte");
    Assert.assertEquals(typeInfo.getTypeClass(), Byte.class);
  }

  @Test
  public void testBinaryType() {
    TypeInfo<?> binaryTypeInfo = converter.fromTypeString("binary");
    Assert.assertEquals(binaryTypeInfo.getTypeClass(), byte[].class);

    TypeInfo<?> bytesTypeInfo = converter.fromTypeString("bytes");
    Assert.assertEquals(bytesTypeInfo.getTypeClass(), byte[].class);
  }

  @Test
  public void testVoidType() {
    TypeInfo<?> voidTypeInfo = converter.fromTypeString("void");
    Assert.assertEquals(voidTypeInfo.getTypeClass(), Void.class);
  }
}