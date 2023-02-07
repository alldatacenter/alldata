/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.phoenix;

import static org.apache.drill.test.rowSet.RowSetUtilities.boolArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.byteArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.doubleArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.intArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.longArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.shortArray;
import static org.apache.drill.test.rowSet.RowSetUtilities.strArray;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.QueryBuilder;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
@Category({ SlowTest.class, RowSetTests.class })
public class PhoenixDataTypeTest extends PhoenixBaseTest {

  @Test
  public void testDataType() throws Exception {
    String sql = "select * from phoenix123.v1.datatype";
    QueryBuilder builder = queryBuilder().sql(sql);
    RowSet sets = builder.rowSet();

    TupleMetadata schema = new SchemaBuilder()
        .addNullable("T_UUID", MinorType.VARCHAR)
        .addNullable("T_VARCHAR", MinorType.VARCHAR)
        .addNullable("T_CHAR", MinorType.VARCHAR)
        .addNullable("T_BIGINT", MinorType.BIGINT)
        .addNullable("T_INTEGER", MinorType.INT)
        .addNullable("T_SMALLINT", MinorType.INT)
        .addNullable("T_TINYINT", MinorType.INT)
        .addNullable("T_DOUBLE", MinorType.FLOAT8)
        .addNullable("T_FLOAT", MinorType.FLOAT4)
        .addNullable("T_DECIMAL", MinorType.VARDECIMAL)
        .addNullable("T_DATE", MinorType.DATE)
        .addNullable("T_TIME", MinorType.TIME)
        .addNullable("T_TIMESTAMP", MinorType.TIMESTAMP)
        .addNullable("T_BINARY", MinorType.VARBINARY)
        .addNullable("T_VARBINARY", MinorType.VARBINARY)
        .addNullable("T_BOOLEAN", MinorType.BIT)
        .build();

    RowSet expected = new RowSetBuilder(client.allocator(), schema)
        .addRow(U_U_I_D,
            "apache", "drill",
            Long.MAX_VALUE,
            Integer.MAX_VALUE,
            Short.MAX_VALUE,
            Byte.MAX_VALUE,
            Double.MAX_VALUE,
            Float.MAX_VALUE,
            BigDecimal.valueOf(10.11),
            LocalDate.parse("2021-12-12"),
            LocalTime.parse("12:12:12"),
            Instant.ofEpochMilli(1639311132000l),
            "a_b_c_d_e_".getBytes(), "12345".getBytes(),
            Boolean.TRUE)
        .build();

    new RowSetComparison(expected).verifyAndClearAll(sets);
  }

  @Test
  public void testArrayType() throws Exception {
    String sql = "select * from phoenix123.v1.arraytype";

    QueryBuilder builder = queryBuilder().sql(sql);
    RowSet sets = builder.rowSet();

    TupleMetadata schema = new SchemaBuilder()
        .addNullable("T_UUID", MinorType.VARCHAR)
        .addArray("T_VARCHAR", MinorType.VARCHAR)
        .addArray("T_CHAR", MinorType.VARCHAR)
        .addArray("T_BIGINT", MinorType.BIGINT)
        .addArray("T_INTEGER", MinorType.INT)
        .addArray("T_DOUBLE", MinorType.FLOAT8)
        .addArray("T_SMALLINT", MinorType.SMALLINT)
        .addArray("T_TINYINT", MinorType.TINYINT)
        .addArray("T_BOOLEAN", MinorType.BIT)
        .build();

    RowSet expected = new RowSetBuilder(client.allocator(), schema)
        .addRow(U_U_I_D,
            strArray("apache", "drill", "1.20"),
            strArray("a", "b", "c"),
            longArray(Long.MIN_VALUE, Long.MAX_VALUE),
            intArray(Integer.MIN_VALUE, Integer.MAX_VALUE),
            doubleArray(Double.MIN_VALUE, Double.MAX_VALUE),
            shortArray(Short.MIN_VALUE, Short.MAX_VALUE),
            byteArray((int) Byte.MIN_VALUE, (int) Byte.MAX_VALUE),
            boolArray(Boolean.TRUE, Boolean.FALSE))
        .build();

    new RowSetComparison(expected).verifyAndClearAll(sets);
  }
}
