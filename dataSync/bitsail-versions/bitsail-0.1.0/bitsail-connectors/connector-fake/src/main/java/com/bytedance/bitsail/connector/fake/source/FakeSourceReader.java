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

package com.bytedance.bitsail.connector.fake.source;

import com.bytedance.bitsail.base.connector.reader.v1.SourcePipeline;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.row.Row;
import com.bytedance.bitsail.common.typeinfo.BasicArrayTypeInfo;
import com.bytedance.bitsail.common.typeinfo.ListTypeInfo;
import com.bytedance.bitsail.common.typeinfo.MapTypeInfo;
import com.bytedance.bitsail.common.typeinfo.TypeInfo;
import com.bytedance.bitsail.common.typeinfo.TypeInfos;
import com.bytedance.bitsail.common.typeinfo.TypeProperty;
import com.bytedance.bitsail.connector.base.source.SimpleSourceReaderBase;
import com.bytedance.bitsail.connector.fake.option.FakeReaderOptions;

import cn.ipokerface.snowflake.SnowflakeIdGenerator;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.RateLimiter;
import net.datafaker.Faker;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class FakeSourceReader extends SimpleSourceReaderBase<Row> {

  private BitSailConfiguration readerConfiguration;
  private TypeInfo<?>[] typeInfos;
  private long upper;
  private long lower;

  private final transient Faker faker;
  private final transient int totalCount;
  private final transient RateLimiter fakeGenerateRate;
  private final transient AtomicLong counter;
  private final transient SnowflakeIdGenerator snowflakeIdGenerator;
  private final transient Timestamp fromTimestamp;
  private final transient Timestamp toTimestamp;

  public FakeSourceReader(BitSailConfiguration readerConfiguration, Context context) {
    this.readerConfiguration = readerConfiguration;
    this.typeInfos = context.getTypeInfos();
    this.totalCount = readerConfiguration.get(FakeReaderOptions.TOTAL_COUNT);
    this.fakeGenerateRate = RateLimiter.create(readerConfiguration.get(FakeReaderOptions.RATE));
    this.faker = new Faker();
    this.counter = new AtomicLong();
    this.snowflakeIdGenerator = new SnowflakeIdGenerator(context.getIndexOfSubtask(),
        context.getIndexOfSubtask());
    this.upper = readerConfiguration.get(FakeReaderOptions.UPPER_LIMIT);
    this.lower = readerConfiguration.get(FakeReaderOptions.LOWER_LIMIT);
    this.fromTimestamp = Timestamp.valueOf(readerConfiguration.get(FakeReaderOptions.FROM_TIMESTAMP));
    this.toTimestamp = Timestamp.valueOf(readerConfiguration.get(FakeReaderOptions.TO_TIMESTAMP));
  }

  @Override
  public void pollNext(SourcePipeline<Row> pipeline) throws Exception {
    fakeGenerateRate.acquire();
    pipeline.output(fakeNextRecord());
  }

  @Override
  public boolean hasMoreElements() {
    return counter.incrementAndGet() <= totalCount;
  }

  private Row fakeNextRecord() {
    Row row = new Row(ArrayUtils.getLength(typeInfos));
    for (int index = 0; index < typeInfos.length; index++) {
      row.setField(index, fakeRawValue(typeInfos[index]));
    }
    return row;
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  private Object fakeRawValue(TypeInfo<?> typeInfo) {

    if (TypeInfos.LONG_TYPE_INFO.getTypeClass() == typeInfo.getTypeClass()) {
      if (CollectionUtils.isNotEmpty(typeInfo.getTypeProperties()) && typeInfo.getTypeProperties().contains(TypeProperty.UNIQUE)) {
        return snowflakeIdGenerator.nextId();
      } else {
        return faker.number().randomNumber();
      }
    } else if (TypeInfos.INT_TYPE_INFO.getTypeClass() == typeInfo.getTypeClass()) {
      return Long.valueOf(faker.number().randomNumber()).intValue();

    } else if (TypeInfos.SHORT_TYPE_INFO.getTypeClass() == typeInfo.getTypeClass()) {
      return Long.valueOf(faker.number().randomNumber()).shortValue();

    } else if (TypeInfos.STRING_TYPE_INFO.getTypeClass() == typeInfo.getTypeClass()) {
      return faker.name().fullName();

    } else if (TypeInfos.BOOLEAN_TYPE_INFO.getTypeClass() == typeInfo.getTypeClass()) {
      return faker.bool().bool();

    } else if (TypeInfos.DOUBLE_TYPE_INFO.getTypeClass() == typeInfo.getTypeClass()) {
      return faker.number().randomDouble(5, lower, upper);

    } else if (TypeInfos.FLOAT_TYPE_INFO.getTypeClass() == typeInfo.getTypeClass()) {
      return Double.valueOf(faker.number().randomDouble(5, lower, upper)).floatValue();

    } else if (TypeInfos.BIG_DECIMAL_TYPE_INFO.getTypeClass() == typeInfo.getTypeClass()) {
      return new BigDecimal(faker.number().randomDouble(5, lower, upper));

    } else if (TypeInfos.BIG_INTEGER_TYPE_INFO.getTypeClass() == typeInfo.getTypeClass()) {
      return new BigInteger(String.valueOf(faker.number().randomNumber()));

    } else if (BasicArrayTypeInfo.BINARY_TYPE_INFO.getTypeClass() == typeInfo.getTypeClass()) {
      return faker.name().fullName().getBytes();

    } else if (TypeInfos.SQL_DATE_TYPE_INFO.getTypeClass() == typeInfo.getTypeClass()) {
      return new java.sql.Date(faker.date().between(fromTimestamp, toTimestamp).getTime());

    } else if (TypeInfos.SQL_TIME_TYPE_INFO.getTypeClass() == typeInfo.getTypeClass()) {
      return new Time(faker.date().between(fromTimestamp, toTimestamp).getTime());

    } else if (TypeInfos.SQL_TIMESTAMP_TYPE_INFO.getTypeClass() == typeInfo.getTypeClass()) {
      return new Timestamp(faker.date().between(fromTimestamp, toTimestamp).getTime());

    } else if (TypeInfos.LOCAL_DATE_TYPE_INFO.getTypeClass() == typeInfo.getTypeClass()) {
      return faker.date().between(fromTimestamp, toTimestamp).toLocalDateTime().toLocalDate();

    } else if (TypeInfos.LOCAL_TIME_TYPE_INFO.getTypeClass() == typeInfo.getTypeClass()) {
      return faker.date().between(fromTimestamp, toTimestamp).toLocalDateTime().toLocalTime();

    } else if (TypeInfos.LOCAL_DATE_TIME_TYPE_INFO.getTypeClass() == typeInfo.getTypeClass()) {
      return faker.date().between(fromTimestamp, toTimestamp).toLocalDateTime();

    } else if (TypeInfos.VOID_TYPE_INFO.getTypeClass() == typeInfo.getTypeClass()) {
      return null;
    }

    if (typeInfo instanceof ListTypeInfo) {
      ListTypeInfo<?> listTypeInfo = (ListTypeInfo<?>) typeInfo;
      return Lists.newArrayList(fakeRawValue(listTypeInfo.getElementTypeInfo()));
    }

    if (typeInfo instanceof MapTypeInfo) {
      MapTypeInfo<?, ?> mapTypeInfo = (MapTypeInfo<?, ?>) typeInfo;
      Map<Object, Object> mapRawValue = Maps.newHashMap();
      mapRawValue.put(fakeRawValue(mapTypeInfo.getKeyTypeInfo()), fakeRawValue(mapTypeInfo.getValueTypeInfo()));
      return mapRawValue;
    }
    throw new RuntimeException("Unsupported type " + typeInfo);
  }
}
