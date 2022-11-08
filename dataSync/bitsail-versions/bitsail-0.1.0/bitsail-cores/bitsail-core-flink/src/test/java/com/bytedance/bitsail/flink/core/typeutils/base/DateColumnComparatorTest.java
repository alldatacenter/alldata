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

package com.bytedance.bitsail.flink.core.typeutils.base;

import com.bytedance.bitsail.common.column.DateColumn;

import org.apache.flink.api.common.typeutils.TypeComparator;
import org.apache.flink.api.common.typeutils.TypeSerializer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class DateColumnComparatorTest extends ColumnComparatorTestBase<DateColumn> {

  @Override
  public TypeComparator<DateColumn> getComparator(boolean ascending) {
    return new DateColumnComparator(ascending);
  }

  @Override
  public TypeSerializer<DateColumn> createSerializer() {
    return new DateColumnSerializer();
  }

  @Override
  public DateColumn[] getSortedData() {
    return new DateColumn[] {
        new DateColumn(System.currentTimeMillis()),
        new DateColumn(LocalTime.of(10, 0, 0)),
        new DateColumn(LocalDate.of(2022, 1, 1)),
        new DateColumn(LocalDate.of(2022, 1, 2)),
        new DateColumn(LocalDateTime.of(LocalDate.of(2022, 1, 3), LocalTime
            .of(12, 0, 0)))
    };
  }
}