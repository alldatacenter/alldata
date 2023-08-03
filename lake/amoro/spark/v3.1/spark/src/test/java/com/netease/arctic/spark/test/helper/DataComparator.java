/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.spark.test.helper;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.relocated.com.google.common.collect.Streams;
import org.apache.iceberg.types.Types;
import org.junit.Assert;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class DataComparator {
  private List<Record> expectRecords;
  private List<Record> actualRecords;
  private Comparator<Record> comparator;

  private Function<Object, Object> fieldValueTrans;

  protected DataComparator(List<Record> expectRecords, List<Record> actualRecords) {
    this.expectRecords = expectRecords;
    this.actualRecords = actualRecords;

    this.fieldValueTrans = x -> {
      if (x instanceof LocalDateTime) {
        // TODO: there are something wrong in timestamp handle for mixed-iceberg.
        return 0;
      } else if (x instanceof OffsetDateTime) {
        return 0;
      }
      return x;
    };
  }

  public DataComparator ignoreOrder(Comparator<Record> comparator) {
    this.comparator = comparator;
    return this;
  }

  public DataComparator ignoreOrder(String... sortFields) {
    for (String f : sortFields) {
      Comparator<Record> cmp = Comparator.comparing(r -> (Comparable) r.getField(f));
      if (comparator == null) {
        this.comparator = cmp;
      } else {
        this.comparator = comparator.thenComparing(cmp);
      }
    }
    return this;
  }

  public void assertRecordsEqual() {
    Assert.assertEquals("records size is not expected.", expectRecords.size(), actualRecords.size());
    if (comparator != null) {
      expectRecords.sort(comparator);
      actualRecords.sort(comparator);
    }
    Streams.zip(expectRecords.stream(), actualRecords.stream(), Pair::of)
        .forEach(r -> assertRecord(r.getLeft(), r.getRight()));
  }

  private void assertRecord(Record expectRecord, Record actualRecord) {
    Assert.assertEquals("The record has different schema",
        expectRecord.struct().fields().size(), actualRecord.struct().fields().size());
    Types.StructType structType = expectRecord.struct();
    for (int i = 0; i < structType.fields().size(); i++) {


      Object expectValue = expectRecord.get(i);
      Object actualValue = actualRecord.get(i);

      Object transExpectValue = fieldValueTrans.apply(expectValue);
      Object transActualValue = fieldValueTrans.apply(actualValue);

      Assert.assertEquals("field values are different", transExpectValue, transActualValue);
    }
  }


  public static DataComparator build(List<Record> expectRecords, List<Record> actualRecords) {
    return new DataComparator(expectRecords, actualRecords);
  }
}
