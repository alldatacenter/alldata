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
package org.apache.drill.exec.store.parquet;

import java.math.BigDecimal;

import org.apache.drill.exec.util.JsonStringArrayList;
import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.test.TestBuilder;
import org.junit.Test;

public class TestParquetComplex extends BaseTestQuery {

  private static final String DATAFILE = "cp.`store/parquet/complex/complex.parquet`";

  @Test
  public void sort() throws Exception {
    String query = String.format("select * from %s order by amount", DATAFILE);
    testBuilder()
            .sqlQuery(query)
            .ordered()
            .jsonBaselineFile("store/parquet/complex/baseline_sorted.json")
            .build()
            .run();
  }

  @Test
  public void topN() throws Exception {
    String query = String.format("select * from %s order by amount limit 5", DATAFILE);
    testBuilder()
            .sqlQuery(query)
            .ordered()
            .jsonBaselineFile("store/parquet/complex/baseline_sorted.json")
            .build()
            .run();
  }

  @Test
  public void hashJoin() throws Exception{
    String query = String.format("select t1.amount, t1.`date`, t1.marketing_info, t1.`time`, t1.trans_id, t1.trans_info, t1.user_info " +
            "from %s t1, %s t2 where t1.amount = t2.amount", DATAFILE, DATAFILE);
    testBuilder()
            .sqlQuery(query)
            .unOrdered()
            .jsonBaselineFile("store/parquet/complex/baseline.json")
            .build()
            .run();
  }

  @Test
  public void mergeJoin() throws Exception{
    test("alter session set `planner.enable_hashjoin` = false");
    String query = String.format("select t1.amount, t1.`date`, t1.marketing_info, t1.`time`, t1.trans_id, t1.trans_info, t1.user_info " +
            "from %s t1, %s t2 where t1.amount = t2.amount", DATAFILE, DATAFILE);
    testBuilder()
            .sqlQuery(query)
            .unOrdered()
            .jsonBaselineFile("store/parquet/complex/baseline.json")
            .build()
            .run();
  }

  @Test
  public void selectAllColumns() throws Exception {
    String query = String.format("select amount, `date`, marketing_info, `time`, trans_id, trans_info, user_info from %s", DATAFILE);
    testBuilder()
            .sqlQuery(query)
            .ordered()
            .jsonBaselineFile("store/parquet/complex/baseline.json")
            .build()
            .run();
  }

  @Test
  public void selectMap() throws Exception {
    String query = "select marketing_info from cp.`store/parquet/complex/complex.parquet`";
    testBuilder()
            .sqlQuery(query)
            .ordered()
            .jsonBaselineFile("store/parquet/complex/baseline5.json")
            .build()
            .run();
  }

  @Test
  public void selectMapAndElements() throws Exception {
    String query = "select marketing_info, t.marketing_info.camp_id as camp_id, t.marketing_info.keywords[2] as keyword2 from cp.`store/parquet/complex/complex.parquet` t";
    testBuilder()
            .sqlQuery(query)
            .ordered()
            .jsonBaselineFile("store/parquet/complex/baseline6.json")
            .build()
            .run();
  }

  @Test
  public void selectMultiElements() throws Exception {
    String query = "select t.marketing_info.camp_id as camp_id, t.marketing_info.keywords as keywords from cp.`store/parquet/complex/complex.parquet` t";
    testBuilder()
            .sqlQuery(query)
            .ordered()
            .jsonBaselineFile("store/parquet/complex/baseline7.json")
            .build()
            .run();
  }

  @Test
  public void testStar() throws Exception {
    testBuilder()
            .sqlQuery("select * from cp.`store/parquet/complex/complex.parquet`")
            .ordered()
            .jsonBaselineFile("store/parquet/complex/baseline.json")
            .build()
            .run();
  }

  @Test
  public void missingColumnInMap() throws Exception {
    String query = "select t.trans_info.keywords as keywords from cp.`store/parquet/complex/complex.parquet` t";
    String[] columns = {"keywords"};
    testBuilder()
            .sqlQuery(query)
            .ordered()
            .jsonBaselineFile("store/parquet/complex/baseline2.json")
            .baselineColumns(columns)
            .build()
            .run();
  }

  @Test
  public void secondElementInMap() throws Exception {
    String query = String.format("select t.`marketing_info`.keywords as keywords from %s t", DATAFILE);
    String[] columns = {"keywords"};
    testBuilder()
            .sqlQuery(query)
            .ordered()
            .jsonBaselineFile("store/parquet/complex/baseline3.json")
            .baselineColumns(columns)
            .build()
            .run();
  }

  @Test
  public void elementsOfArray() throws Exception {
    String query = String.format("select t.`marketing_info`.keywords[0] as keyword0, t.`marketing_info`.keywords[2] as keyword2 from %s t", DATAFILE);
    String[] columns = {"keyword0", "keyword2"};
    testBuilder()
            .sqlQuery(query)
            .unOrdered()
            .jsonBaselineFile("store/parquet/complex/baseline4.json")
            .baselineColumns(columns)
            .build()
            .run();
  }

  @Test
  public void elementsOfArrayCaseInsensitive() throws Exception {
    String query = String.format("select t.`MARKETING_INFO`.keywords[0] as keyword0, t.`Marketing_Info`.Keywords[2] as keyword2 from %s t", DATAFILE);
    String[] columns = {"keyword0", "keyword2"};
    testBuilder()
            .sqlQuery(query)
            .unOrdered()
            .jsonBaselineFile("store/parquet/complex/baseline4.json")
            .baselineColumns(columns)
            .build()
            .run();
  }

  @Test //DRILL-3533
  public void notxistsField() throws Exception {
    String query = String.format("select t.`marketing_info`.notexists as notexists1,\n" +
                                        "t.`marketing_info`.camp_id as id,\n" +
                                        "t.`marketing_info.camp_id` as notexists2\n" +
                                  "from %s t", DATAFILE);
    String[] columns = {"notexists1", "id", "notexists2"};
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .jsonBaselineFile("store/parquet/complex/baseline8.json")
        .baselineColumns(columns)
        .build()
        .run();
  }

  @Test
  public void testReadRepeatedDecimals() throws Exception {

    JsonStringArrayList<BigDecimal> ints = new JsonStringArrayList<>();
    ints.add(new BigDecimal("999999.999"));
    ints.add(new BigDecimal("-999999.999"));
    ints.add(new BigDecimal("0.000"));

    JsonStringArrayList<BigDecimal> longs = new JsonStringArrayList<>();
    longs.add(new BigDecimal("999999999.999999999"));
    longs.add(new BigDecimal("-999999999.999999999"));
    longs.add(new BigDecimal("0.000000000"));

    JsonStringArrayList<BigDecimal> fixedLen = new JsonStringArrayList<>();
    fixedLen.add(new BigDecimal("999999999999.999999"));
    fixedLen.add(new BigDecimal("-999999999999.999999"));
    fixedLen.add(new BigDecimal("0.000000"));

    testBuilder()
        .sqlQuery("select * from cp.`parquet/repeatedIntLondFixedLenBinaryDecimal.parquet`")
        .unOrdered()
        .baselineColumns("decimal_int32", "decimal_int64", "decimal_fixedLen", "decimal_binary")
        .baselineValues(ints, longs, fixedLen, fixedLen)
        .go();
  }

  @Test
  public void selectDictBigIntValue() throws Exception {
    String query = "select order_items from cp.`store/parquet/complex/simple_map.parquet`";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("order_items")
        .baselineValues(TestBuilder.mapOfObject("Pencils", 1L))
        .go();
  }

  @Test
  public void selectDictStructValue() throws Exception {
    String query = "select id, mapcol4 from cp.`store/parquet/complex/map/parquet/000000_0.parquet`";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("id", "mapcol4")
        .baselineValues(1,
            TestBuilder.mapOfObject(
                101L,
                TestBuilder.mapOfObject(false, "item_amount", 1L, "item_type", "pencil"),
                102L,
                TestBuilder.mapOfObject(false, "item_amount", 2L, "item_type", "eraser")
            )
        )
        .baselineValues(2,
            TestBuilder.mapOfObject(
                102L,
                TestBuilder.mapOfObject(false, "item_amount", 3L, "item_type", "pen"),
                103L,
                TestBuilder.mapOfObject(false, "item_amount", 4L, "item_type", "scissors")
            )
        )
        .baselineValues(3,
            TestBuilder.mapOfObject(
                110L,
                TestBuilder.mapOfObject(false, "item_amount", 5L, "item_type", "glue"),
                113L,
                TestBuilder.mapOfObject(false, "item_amount", 6L, "item_type", "pencil")
            )
        )
        .baselineValues(4,
            TestBuilder.mapOfObject(
                238L,
                TestBuilder.mapOfObject(false, "item_amount", 7L, "item_type", "pen"),
                239L,
                TestBuilder.mapOfObject(false, "item_amount", 8L, "item_type", "eraser"),
                240L,
                TestBuilder.mapOfObject(false, "item_amount", 9L, "item_type", "scissors"),
                241L,
                TestBuilder.mapOfObject(false, "item_amount", 10L, "item_type", "glue")
            )
        )
        .baselineValues(5,
            TestBuilder.mapOfObject(
                242L,
                TestBuilder.mapOfObject(false, "item_amount", 11L, "item_type", "paper"),
                243L,
                TestBuilder.mapOfObject(false, "item_amount", 13L, "item_type", "ink")
            )
        )
        .go();
  }

  @Test
  public void selectDictIntArrayValue() throws Exception {
    String query = "select id, mapcol5 from cp.`store/parquet/complex/map/parquet/000000_0.parquet` order by id asc";
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("id", "mapcol5")
        .baselineValues(
            1, TestBuilder.mapOfObject(
                3, TestBuilder.listOf(3, 4, 5),
                5, TestBuilder.listOf(5, 3)
            )
        )
        .baselineValues(
            2, TestBuilder.mapOfObject(
                1, TestBuilder.listOf(1, 2, 3, 4, 5)
            )
        )
        .baselineValues(
            3, TestBuilder.mapOfObject(
                1, TestBuilder.listOf(1, 2, 3, 4, 5),
                2, TestBuilder.listOf(2, 3)
            )
        )
        .baselineValues(
            4, TestBuilder.mapOfObject(
                1, TestBuilder.listOf(3, 4, 5, 10, -2, -4),
                5, TestBuilder.listOf(), // this actually contains a null element
                -2, TestBuilder.listOf(2, 2, 2, 2),
                8, TestBuilder.listOf(2, 2, 3, 4)
            )
        )
        .baselineValues(
            5, TestBuilder.mapOfObject(
                2, TestBuilder.listOf(5),
                3, TestBuilder.listOf(8, -5, 3, 4)
            )
        )
        .go();
  }

  @Test
  public void selectDictIntArrayValueGetByKey() throws Exception {
    String query = "select id, mapcol5[1] as val from cp.`store/parquet/complex/map/parquet/000000_0.parquet` order by id asc";
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("id", "val")
        .baselineValues(1, TestBuilder.listOf())
        .baselineValues(2, TestBuilder.listOf(1, 2, 3, 4, 5))
        .baselineValues(3, TestBuilder.listOf(1, 2, 3, 4, 5))
        .baselineValues(4, TestBuilder.listOf(3, 4, 5, 10, -2, -4))
        .baselineValues(5, TestBuilder.listOf())
        .go();
  }

  @Test
  public void selectDictDictValue() throws Exception {
    String query = "select id, mapcol3 from cp.`store/parquet/complex/map/parquet/000000_0.parquet` order by id asc";
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("id", "mapcol3")
        .baselineValues(1, TestBuilder.mapOfObject(
            3, TestBuilder.mapOfObject("a", 1L, "b", 2L),
            4, TestBuilder.mapOfObject("c", 3L),
            5, TestBuilder.mapOfObject("d", 4L, "e", 5L)
            )
        )
        .baselineValues(2, TestBuilder.mapOfObject(
            1, TestBuilder.mapOfObject("a", 1L, "b", 2L)
            )
        )
        .baselineValues(3, TestBuilder.mapOfObject(
            2, TestBuilder.mapOfObject("a", 1L, "b", 2L),
            3, TestBuilder.mapOfObject("C", 3L)
            )
        )
        .baselineValues(4, TestBuilder.mapOfObject(
            2, TestBuilder.mapOfObject("abc", 1L, "bce", 2L),
            4, TestBuilder.mapOfObject("c", 3L, "cf", 6L),
            5, TestBuilder.mapOfObject("d", 4L, "eh", 5L),
            8, TestBuilder.mapOfObject("d", 32L, "e", -17L)
            )
        )
        .baselineValues(5, TestBuilder.mapOfObject(
            1, TestBuilder.mapOfObject("bee", -2L, "awg", 1L),
            2, TestBuilder.mapOfObject("cddd", 3L),
            4, TestBuilder.mapOfObject("deea", 4L, "eerie", 99L)
            )
        )
        .go();
  }

  @Test
  public void selectDictGetByIntKeyComplexValue() throws Exception {
    String query = "select id, mapcol3[3] as val from cp.`store/parquet/complex/map/parquet/000000_0.parquet`";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("id", "val")
        .baselineValues(4, TestBuilder.mapOfObject())
        .baselineValues(1, TestBuilder.mapOfObject("a", 1L, "b", 2L))
        .baselineValues(3, TestBuilder.mapOfObject("C", 3L))
        .baselineValues(2, TestBuilder.mapOfObject())
        .baselineValues(5, TestBuilder.mapOfObject())
        .go();
  }

  @Test
  public void selectDictGetByStringKey() throws Exception {
    String query = "select mapcol['a'] val from cp.`store/parquet/complex/map/parquet/000000_0.parquet` order by id asc";
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("val")
        .baselineValuesForSingleColumn(null, 1, null, 3, 8)
        .go();
  }

  @Test
  public void selectDictGetByStringKey2() throws Exception {
    String query = "select id, mapcol['b'] val from cp.`store/parquet/complex/map/parquet/000000_0.parquet`";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("id", "val")
        .baselineValues(4, 4)
        .baselineValues(1, 6)
        .baselineValues(3, null)
        .baselineValues(2, 2)
        .baselineValues(5, 6)
        .go();
  }

  @Test
  public void selectDictByKeyComplexValue2() throws Exception {
    String query = "select id, mapcol3[4]['c'] val from cp.`store/parquet/complex/map/parquet/000000_0.parquet`";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("id", "val")
        .baselineValues(4, 3L)
        .baselineValues(1, 3L)
        .baselineValues(3, null)
        .baselineValues(2, null)
        .baselineValues(5, null)
        .go();
  }

  @Test
  public void selectDictGetByKeyComplexValue3() throws Exception {
    String query = "select id, mapcol3[3]['b'] val from cp.`store/parquet/complex/map/parquet/000000_0.parquet`";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("id", "val")
        .baselineValues(4, null)
        .baselineValues(1, 2L)
        .baselineValues(3, null)
        .baselineValues(2, null)
        .baselineValues(5, null)
        .go();
  }

  @Test
  public void testDictOrderByAnotherField() throws Exception {
    String query = "select id, mapcol from cp.`store/parquet/complex/map/parquet/000000_0.parquet` order by id desc";
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("id", "mapcol")
        .baselineValues(5, TestBuilder.mapOfObject("b", 6, "c", 7, "a", 8, "abc4", 9, "bde", 10))
        .baselineValues(4, TestBuilder.mapOfObject("a", 3, "b", 4, "c", 5))
        .baselineValues(3, TestBuilder.mapOfObject("b", null, "c", 8, "d", 9, "e", 10))
        .baselineValues(2, TestBuilder.mapOfObject("a", 1, "b", 2, "c", 3))
        .baselineValues(1, TestBuilder.mapOfObject("b", 6, "c", 7))
        .go();
  }

  @Test
  public void testDictWithLimitAndOffset() throws Exception {
    String query = "select id, mapcol from cp.`store/parquet/complex/map/parquet/000000_0.parquet` order by id desc limit 2 offset 2";
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("id", "mapcol")
        .baselineValues(3, TestBuilder.mapOfObject("b", null, "c", 8, "d", 9, "e", 10))
        .baselineValues(2, TestBuilder.mapOfObject("a", 1, "b", 2, "c", 3))
        .go();
  }

  @Test
  public void testDictDictArrayValue() throws Exception {
    String query = "select id, map_array from cp.`store/parquet/complex/map/parquet/000000_0.parquet`";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("id", "map_array")
        .baselineValues(
            4,
            TestBuilder.listOf(
                TestBuilder.mapOfObject(1L, 2, 10L, 1, 42L, 3, 31L, 4),
                TestBuilder.mapOfObject(-1L, 2, 3L, 1, 5L, 3, 54L, 4, 55L, 589, -78L, 2),
                TestBuilder.mapOfObject(1L, 124, 3L, 1, -4L, 2, 19L, 3, 5L, 3, 9L, 1),
                TestBuilder.mapOfObject(1L, 89, 2L, 1, 3L, 3, 4L, 21, 5L, 12, 6L, 34),
                TestBuilder.mapOfObject(1L, -25, 3L, 1, 5L, 3, 6L, 2, 9L, 333, 10L, 344),
                TestBuilder.mapOfObject(3L, 222, 4L, 1, 5L, 3, 6L, 2, 7L, 1, 8L, 3),
                TestBuilder.mapOfObject(1L, 11, 3L, 12, 5L, 13)
            )
        )
        .baselineValues(
            1,
            TestBuilder.listOf(
                TestBuilder.mapOfObject(8L, 1, 9L, 2, 523L, 4, 31L, 3),
                TestBuilder.mapOfObject(1L, 2, 3L, 1, 5L, 3)
            )
        )
        .baselineValues(
            3,
            TestBuilder.listOf(
                TestBuilder.mapOfObject(3L, 1),
                TestBuilder.mapOfObject(1L, 2)
            )
        )
        .baselineValues(
            2,
            TestBuilder.listOf(
                TestBuilder.mapOfObject(1L, 1, 2L, 2)
            )
        )
        .baselineValues(
            5,
            TestBuilder.listOf(
                TestBuilder.mapOfObject(1L, 1, 2L, 2, 3L, 3, 4L, 4),
                TestBuilder.mapOfObject(1L, -1, 2L, -2),
                TestBuilder.mapOfObject(1L, 4, 2L, 5, 3L, 7)
            )
        )
        .go();
  }

  @Test
  public void testDictArrayGetElementByIndex() throws Exception {
    String query = "select id, map_array[0] as element from cp.`store/parquet/complex/map/parquet/000000_0.parquet`";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("id", "element")
        .baselineValues(4, TestBuilder.mapOfObject(1L, 2, 10L, 1, 42L, 3, 31L, 4))
        .baselineValues(1, TestBuilder.mapOfObject(8L, 1, 9L, 2, 523L, 4, 31L, 3))
        .baselineValues(3, TestBuilder.mapOfObject(3L, 1))
        .baselineValues(2, TestBuilder.mapOfObject(1L, 1, 2L, 2))
        .baselineValues(5, TestBuilder.mapOfObject(1L, 1, 2L, 2, 3L, 3, 4L, 4))
        .go();
  }

  @Test
  public void testDictGetByLongKey() throws Exception {
    String query = "select id, mapcol4[102] as val from cp.`store/parquet/complex/map/parquet/000000_0.parquet`";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("id", "val")
        .baselineValues(1, TestBuilder.mapOfObject(false, "item_amount", 2L, "item_type", "eraser"))
        .baselineValues(2, TestBuilder.mapOfObject(false, "item_amount", 3L, "item_type", "pen"))
        .baselineValues(3, TestBuilder.mapOfObject())
        .baselineValues(4, TestBuilder.mapOfObject())
        .baselineValues(5, TestBuilder.mapOfObject())
        .go();
  }

  @Test
  public void testSelectDictFloatToFloat() throws Exception {
    String query = "select id, mapcol2 as val from cp.`store/parquet/complex/map/parquet/000000_0.parquet`";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("id", "val")
        .baselineValues(4, TestBuilder.mapOfObject(-9.01f, 2.0f, 0.43f, 4.3f))
        .baselineValues(1, TestBuilder.mapOfObject(1.1f, -1.0f, 2.3f, 2.1f, 3.45f, 3.5f, 4.47f, 4.43f))
        .baselineValues(3, TestBuilder.mapOfObject(7.9f, 0.43f, 3.1f, 21.1f, 1.1f, 3.53f))
        .baselineValues(2, TestBuilder.mapOfObject(0.9f, 0.43f, 1.1f, 2.1f, 2.0f, 3.3f))
        .baselineValues(5, TestBuilder.mapOfObject(1.1f, 255.34f, -2.0f, 24.0f, 45.53f, 78.22f))
        .go();
  }

  @Test
  public void testSelectDictGetByFloatKey() throws Exception {
    String query = "select id, mapcol2['1.1'] as val from cp.`store/parquet/complex/map/parquet/000000_0.parquet`";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("id", "val")
        .baselineValues(4, null)
        .baselineValues(1, -1.0f)
        .baselineValues(3, 3.53f)
        .baselineValues(2, 2.1f)
        .baselineValues(5, 255.34f)
        .go();
  }

  @Test
  public void testSelectDictGetByNegativeFloatKey() throws Exception {
    String query = "select id, mapcol2['-9.01'] as val from cp.`store/parquet/complex/map/parquet/000000_0.parquet`";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("id", "val")
        .baselineValues(4, 2.0f)
        .baselineValues(1, null)
        .baselineValues(3, null)
        .baselineValues(2, null)
        .baselineValues(5, null)
        .go();
  }

  @Test
  public void testDictOrderByValue() throws Exception {
    String query = "select id, mapcol as val from cp.`store/parquet/complex/map/parquet/000000_0.parquet` order by mapcol['a'] desc";
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("id", "val")
        .baselineValues(1, TestBuilder.mapOfObject("b", 6, "c", 7))
        .baselineValues(3, TestBuilder.mapOfObject("b", null, "c", 8, "d", 9, "e", 10))
        .baselineValues(5, TestBuilder.mapOfObject("b", 6, "c", 7, "a", 8, "abc4", 9, "bde", 10))
        .baselineValues(4, TestBuilder.mapOfObject("a", 3, "b", 4, "c", 5))
        .baselineValues(2, TestBuilder.mapOfObject("a", 1, "b", 2, "c", 3))
        .go();
  }

  @Test
  public void testDictArrayElementGetByKey() throws Exception {
    String query = "select map_array[1][5] as val from cp.`store/parquet/complex/map/parquet/000000_0.parquet` order by map_array[1][5] desc";
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("val")
        .baselineValuesForSingleColumn(null, null, null, 3, 3)
        .go();
  }

  @Test
  public void testDictArrayElementGetByStringKey() throws Exception {
    String query = "select map_array[1]['1'] as val from cp.`store/parquet/complex/map/parquet/000000_0.parquet`";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("val")
        .baselineValuesForSingleColumn(null, 2, 2, null, -1)
        .go();
  }

  @Test
  public void testDictArrayTypeOf() throws Exception {
    String query = "select typeof(map_array) as type from cp.`store/parquet/complex/map/parquet/000000_0.parquet` limit 1";
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("type")
        .baselineValuesForSingleColumn("ARRAY<DICT<BIGINT,INT>>")
        .go();
  }

  @Test
  public void testDictTypeOf() throws Exception {
    String query = "select typeof(map_array[0]) as type from cp.`store/parquet/complex/map/parquet/000000_0.parquet` limit 1";
    testBuilder()
        .sqlQuery(query)
        .ordered()
        .baselineColumns("type")
        .baselineValuesForSingleColumn("DICT<BIGINT,INT>")
        .go();
  }

  @Test
  public void testDictFlatten() throws Exception {
    String query = "select id, flatten(mapcol) as flat from cp.`store/parquet/complex/map/parquet/000000_0.parquet` order by id";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("id", "flat")
        .baselineValues(1, TestBuilder.mapOfObject(false, "key", "b", "value", 6))
        .baselineValues(1, TestBuilder.mapOfObject(false, "key", "c", "value", 7))
        .baselineValues(3, TestBuilder.mapOfObject(false, "key", "b")) // "value" == null
        .baselineValues(3, TestBuilder.mapOfObject(false, "key", "c", "value", 8))
        .baselineValues(3, TestBuilder.mapOfObject(false, "key", "d", "value", 9))
        .baselineValues(3, TestBuilder.mapOfObject(false, "key", "e", "value", 10))
        .baselineValues(5, TestBuilder.mapOfObject(false, "key", "b", "value", 6))
        .baselineValues(5, TestBuilder.mapOfObject(false, "key", "c", "value", 7))
        .baselineValues(5, TestBuilder.mapOfObject(false, "key", "a", "value", 8))
        .baselineValues(5, TestBuilder.mapOfObject(false, "key", "abc4", "value", 9))
        .baselineValues(5, TestBuilder.mapOfObject(false, "key", "bde", "value", 10))
        .baselineValues(4, TestBuilder.mapOfObject(false, "key", "a", "value", 3))
        .baselineValues(4, TestBuilder.mapOfObject(false, "key", "b", "value", 4))
        .baselineValues(4, TestBuilder.mapOfObject(false, "key", "c", "value", 5))
        .baselineValues(2, TestBuilder.mapOfObject(false, "key", "a", "value", 1))
        .baselineValues(2, TestBuilder.mapOfObject(false, "key", "b", "value", 2))
        .baselineValues(2, TestBuilder.mapOfObject(false, "key", "c", "value", 3))
        .go();
  }

  @Test
  public void testDictArrayFlatten() throws Exception {
    String query = "select id, flatten(map_array) flat from cp.`store/parquet/complex/map/parquet/000000_0.parquet`";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("id", "flat")
        .baselineValues(4, TestBuilder.mapOfObject(1L, 2, 10L, 1, 42L, 3, 31L, 4))
        .baselineValues(4, TestBuilder.mapOfObject(-1L, 2, 3L, 1, 5L, 3, 54L, 4, 55L, 589, -78L, 2))
        .baselineValues(4, TestBuilder.mapOfObject(1L, 124, 3L, 1, -4L, 2, 19L, 3, 5L, 3, 9L, 1))
        .baselineValues(4, TestBuilder.mapOfObject(1L, 89, 2L, 1, 3L, 3, 4L, 21, 5L, 12, 6L, 34))
        .baselineValues(4, TestBuilder.mapOfObject(1L, -25, 3L, 1, 5L, 3, 6L, 2, 9L, 333, 10L, 344))
        .baselineValues(4, TestBuilder.mapOfObject(3L, 222, 4L, 1, 5L, 3, 6L, 2, 7L, 1, 8L, 3))
        .baselineValues(4, TestBuilder.mapOfObject(1L, 11, 3L, 12, 5L, 13))
        .baselineValues(1, TestBuilder.mapOfObject(8L, 1, 9L, 2, 523L, 4, 31L, 3))
        .baselineValues(1, TestBuilder.mapOfObject(1L, 2, 3L, 1, 5L, 3))
        .baselineValues(3, TestBuilder.mapOfObject(3L, 1))
        .baselineValues(3, TestBuilder.mapOfObject(1L, 2))
        .baselineValues(2, TestBuilder.mapOfObject(1L, 1, 2L, 2))
        .baselineValues(5, TestBuilder.mapOfObject(1L, 1, 2L, 2, 3L, 3, 4L, 4))
        .baselineValues(5, TestBuilder.mapOfObject(1L, -1, 2L, -2))
        .baselineValues(5, TestBuilder.mapOfObject(1L, 4, 2L, 5, 3L, 7))
        .go();
  }

  @Test
  public void testDictArrayAndElementFlatten() throws Exception {
    String query = "select id, flatten(flatten(map_array)) flat from cp.`store/parquet/complex/map/parquet/000000_0.parquet`";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("id", "flat")
        .baselineValues(4, TestBuilder.mapOf("key", 1L, "value", 2))
        .baselineValues(4, TestBuilder.mapOf("key", 10L, "value", 1))
        .baselineValues(4, TestBuilder.mapOf("key", 42L, "value", 3))
        .baselineValues(4, TestBuilder.mapOf("key", 31L, "value", 4))
        .baselineValues(4, TestBuilder.mapOf("key", -1L, "value", 2))
        .baselineValues(4, TestBuilder.mapOf("key", 3L, "value", 1))
        .baselineValues(4, TestBuilder.mapOf("key", 5L, "value", 3))
        .baselineValues(4, TestBuilder.mapOf("key", 54L, "value", 4))
        .baselineValues(4, TestBuilder.mapOf("key", 55L, "value", 589))
        .baselineValues(4, TestBuilder.mapOf("key", -78L, "value", 2))
        .baselineValues(4, TestBuilder.mapOf("key", 1L, "value", 124))
        .baselineValues(4, TestBuilder.mapOf("key", 3L, "value", 1))
        .baselineValues(4, TestBuilder.mapOf("key", -4L, "value", 2))
        .baselineValues(4, TestBuilder.mapOf("key", 19L, "value", 3))
        .baselineValues(4, TestBuilder.mapOf("key", 5L, "value", 3))
        .baselineValues(4, TestBuilder.mapOf("key", 9L, "value", 1))
        .baselineValues(4, TestBuilder.mapOf("key", 1L, "value", 89))
        .baselineValues(4, TestBuilder.mapOf("key", 2L, "value", 1))
        .baselineValues(4, TestBuilder.mapOf("key", 3L, "value", 3))
        .baselineValues(4, TestBuilder.mapOf("key", 4L, "value", 21))
        .baselineValues(4, TestBuilder.mapOf("key", 5L, "value", 12))
        .baselineValues(4, TestBuilder.mapOf("key", 6L, "value", 34))
        .baselineValues(4, TestBuilder.mapOf("key", 1L, "value", -25))
        .baselineValues(4, TestBuilder.mapOf("key", 3L, "value", 1))
        .baselineValues(4, TestBuilder.mapOf("key", 5L, "value", 3))
        .baselineValues(4, TestBuilder.mapOf("key", 6L, "value", 2))
        .baselineValues(4, TestBuilder.mapOf("key", 9L, "value", 333))
        .baselineValues(4, TestBuilder.mapOf("key", 10L, "value", 344))
        .baselineValues(4, TestBuilder.mapOf("key", 3L, "value", 222))
        .baselineValues(4, TestBuilder.mapOf("key", 4L, "value", 1))
        .baselineValues(4, TestBuilder.mapOf("key", 5L, "value", 3))
        .baselineValues(4, TestBuilder.mapOf("key", 6L, "value", 2))
        .baselineValues(4, TestBuilder.mapOf("key", 7L, "value", 1))
        .baselineValues(4, TestBuilder.mapOf("key", 8L, "value", 3))
        .baselineValues(4, TestBuilder.mapOf("key", 1L, "value", 11))
        .baselineValues(4, TestBuilder.mapOf("key", 3L, "value", 12))
        .baselineValues(4, TestBuilder.mapOf("key", 5L, "value", 13))
        .baselineValues(1, TestBuilder.mapOf("key", 8L, "value", 1))
        .baselineValues(1, TestBuilder.mapOf("key", 9L, "value", 2))
        .baselineValues(1, TestBuilder.mapOf("key", 523L, "value", 4))
        .baselineValues(1, TestBuilder.mapOf("key", 31L, "value", 3))
        .baselineValues(1, TestBuilder.mapOf("key", 1L, "value", 2))
        .baselineValues(1, TestBuilder.mapOf("key", 3L, "value", 1))
        .baselineValues(1, TestBuilder.mapOf("key", 5L, "value", 3))
        .baselineValues(3, TestBuilder.mapOf("key", 3L, "value", 1))
        .baselineValues(3, TestBuilder.mapOf("key", 1L, "value", 2))
        .baselineValues(2, TestBuilder.mapOf("key", 1L, "value", 1))
        .baselineValues(2, TestBuilder.mapOf("key", 2L, "value", 2))
        .baselineValues(5, TestBuilder.mapOf("key", 1L, "value", 1))
        .baselineValues(5, TestBuilder.mapOf("key", 2L, "value", 2))
        .baselineValues(5, TestBuilder.mapOf("key", 3L, "value", 3))
        .baselineValues(5, TestBuilder.mapOf("key", 4L, "value", 4))
        .baselineValues(5, TestBuilder.mapOf("key", 1L, "value", -1))
        .baselineValues(5, TestBuilder.mapOf("key", 2L, "value", -2))
        .baselineValues(5, TestBuilder.mapOf("key", 1L, "value", 4))
        .baselineValues(5, TestBuilder.mapOf("key", 2L, "value", 5))
        .baselineValues(5, TestBuilder.mapOf("key", 3L, "value", 7))
        .go();
  }

  @Test
  public void selectDictFlattenListValue() throws Exception {
    String query = "select id, flatten(mapcol5[1]) as flat from cp.`store/parquet/complex/map/parquet/000000_0.parquet`";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("id", "flat")
        .baselineValues(2, 1)
        .baselineValues(2, 2)
        .baselineValues(2, 3)
        .baselineValues(2, 4)
        .baselineValues(2, 5)
        .baselineValues(3, 1)
        .baselineValues(3, 2)
        .baselineValues(3, 3)
        .baselineValues(3, 4)
        .baselineValues(3, 5)
        .baselineValues(4, 3)
        .baselineValues(4, 4)
        .baselineValues(4, 5)
        .baselineValues(4, 10)
        .baselineValues(4, -2)
        .baselineValues(4, -4)
        .go();
  }

  @Test
  public void testDictValueInFilter() throws Exception {
    String query = "select id, mapcol from cp.`store/parquet/complex/map/parquet/000000_0.parquet` where mapcol['c'] > 5";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("id", "mapcol")
        .baselineValues(1, TestBuilder.mapOfObject("b", 6, "c", 7))
        .baselineValues(3, TestBuilder.mapOfObject("b", null, "c", 8, "d", 9, "e", 10))
        .baselineValues(5, TestBuilder.mapOfObject("b", 6, "c", 7, "a", 8, "abc4", 9, "bde", 10))
        .go();
  }

  @Test
  public void testDictValueInFilter2() throws Exception {
    String query = "select id, mapcol from cp.`store/parquet/complex/map/parquet/000000_0.parquet` where mapcol['a'] is null";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("id", "mapcol")
        .baselineValues(1, TestBuilder.mapOfObject("b", 6, "c", 7))
        .baselineValues(3, TestBuilder.mapOfObject("b", null, "c", 8, "d", 9, "e", 10))
        .go();
  }

  @Test
  public void testDictValueInFilter3() throws Exception {
    String query = "select id, mapcol from cp.`store/parquet/complex/map/parquet/000000_0.parquet` where mapcol['b'] is not null";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("id", "mapcol")
        .baselineValues(1, TestBuilder.mapOfObject("b", 6, "c", 7))
        .baselineValues(5, TestBuilder.mapOfObject("b", 6, "c", 7, "a", 8, "abc4", 9, "bde", 10))
        .baselineValues(4, TestBuilder.mapOfObject("a", 3, "b", 4, "c", 5))
        .baselineValues(2, TestBuilder.mapOfObject("a", 1, "b", 2, "c", 3))
        .go();
  }

  @Test // DRILL-7473
  public void testDictInRepeatedMap() throws Exception {
    String query = "select struct_array[1].d as d from cp.`store/parquet/complex/map/parquet/repeated_struct_with_dict.parquet`";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("d")
        .baselineValuesForSingleColumn(
            TestBuilder.mapOfObject(1, "a", 2, "b", 3, "c"),
            TestBuilder.mapOfObject(),
            TestBuilder.mapOfObject(1, "a", 2, "b")
        )
        .go();
  }

  @Test // DRILL-7491
  public void testCountOnComplexTypes() throws Exception {
    String query = "SELECT " +
        "COUNT(c13) cnt13, COUNT(c14) cnt14, " +
        "COUNT(c15) cnt15, COUNT(c16) cnt16 " +
        "FROM cp.`parquet/hive_all/hive_alltypes.parquet`";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("cnt13", "cnt14", "cnt15", "cnt16")
        .baselineValues(3L, 0L, 3L, 3L)
        .go();
  }

  @Test // DRILL-7509
  public void selectRepeatedMapWithFilter() throws Exception {
    String query = "select id, struct_array[1].b as b from cp.`store/parquet/complex/repeated_struct.parquet` where struct_array[1].b is null";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("id", "b")
        .baselineValues(2, null)
        .go();
  }

  @Test
  public void testNewComplexParquetReaderUUID() throws Exception {
    String query = "select `uuid_req1`, `uuid_opt1`, `uuid_req2` from cp.`store/parquet/complex/uuid.parquet` order by `uuid_req1`, `uuid_opt1`, `uuid_req2` limit 1";
    byte[] firstValue = {0, 39, -125, -76, -113, 95, 73, -68, -68, 61, -89, -24, 123, -40, 94, -6};
    byte[] secondValue = {74, -38, 0, -43, -73, 101, 67, -11, -68, -17, -63, 111, -20, 70, -93, -76};
    testBuilder()
            .optionSettingQueriesForTestQuery("alter session set `store.parquet.use_new_reader` = false")
            .sqlQuery(query)
            .unOrdered()
            .baselineColumns("uuid_req1", "uuid_opt1", "uuid_req2")
            .baselineValues(firstValue, null, secondValue)
            .go();
  }
}
