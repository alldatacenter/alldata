/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.netease.arctic.spark;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import scala.Tuple2$;
import scala.collection.immutable.Map$;
import scala.collection.mutable.WrappedArray;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;


public class TestComplexType extends SparkTestBase {

  private final String database = "db_complex";
  private final String tableA = "testa";

  @Before
  public void prepare() {
    sql("create database if not exists " + database);
  }

  @After
  public void clean() {
    sql("drop database if exists " + database + " cascade");
  }

  @Test
  public void complexKeyedTable() {
    sql("create table {0}.{1} ( \n" +
        " id bigint , \n" +
        " tiny_id tinyint , \n" +
        " small_id smallint , \n" +
        " user_id int , \n" +
        " is_true boolean , \n" +
        " decimal decimal(4,2) , \n" +
        " salary double , \n" +
        " money float , \n" +
        " ts timestamp , \n " +
        " structdata01 map<string,string> , \n " +
        " structdata02 array<string> , \n " +
        " name string , \n" +
        " name2 string , \n" +
        " primary key (id,user_id) \n" +
        ") using arctic partitioned by (name, name2) \n" +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, tableA);
    sql("insert overwrite table {0}.{1} values  \n" +
            "(11,1,110,200012,true,88.88,12345.123,12.11,timestamp(''2022-09-05 10:51:34''), \n" +
            "map(''test_key_01'',''test_value_02''),array(''array01'',''array02'',''array03''), \n" +
            "''aaa'',''aaa2'')"
        , database, tableA);
    rows = sql("select * from {0}.{1}", database, tableA);
    Assert.assertEquals(1, rows.size());
    List<Object[]> expected = new ArrayList<>();
    expected.add(new Object[]{
        new Long(11), 1, 110, 200012, true, new BigDecimal("88.88"), new Double(12345.123),
        new Float(12.11), Timestamp.valueOf("2022-09-05 10:51:34"),
        Map$.MODULE$.empty().$plus(Tuple2$.MODULE$.apply("test_key_01", "test_value_02")),
        WrappedArray.make(new String[]{"array01", "array02", "array03"}), "aaa", "aaa2"
    });
    assertEquals("Should have rows matching the expected rows", expected, rows);

    sql("drop table {0}.{1}", database, tableA);
  }

  @Test
  public void complexUnkeyedTable() {
    sql("create table {0}.{1} ( \n" +
        " id bigint , \n" +
        " tiny_id tinyint , \n" +
        " small_id smallint , \n" +
        " user_id int , \n" +
        " is_true boolean , \n" +
        " decimal decimal(4,2) , \n" +
        " salary double , \n" +
        " money float , \n" +
        " ts timestamp , \n " +
        " structdata01 map<string,string> , \n " +
        " structdata02 array<string> , \n " +
        " name string , \n" +
        " name2 string \n" +
        ") using arctic partitioned by (name, name2) \n" +
        " tblproperties ( \n" +
        " ''props.test1'' = ''val1'', \n" +
        " ''props.test2'' = ''val2'' ) ", database, tableA);
    sql("insert overwrite table {0}.{1} values  \n" +
            "(11,1,110,200012,true,88.88,12345.123,12.11,timestamp(''2022-09-05 10:51:34''), \n" +
            "map(''test_key_01'',''test_value_02''),array(''array01'',''array02'',''array03''), \n" +
            "''aaa'',''aaa2'')"
        , database, tableA);
    rows = sql("select * from {0}.{1}", database, tableA);
    Assert.assertEquals(1, rows.size());
    List<Object[]> expected = new ArrayList<>();
    expected.add(new Object[]{
        new Long(11), 1, 110, 200012, true, new BigDecimal("88.88"), new Double(12345.123),
        new Float(12.11), Timestamp.valueOf("2022-09-05 10:51:34"),
        Map$.MODULE$.empty().$plus(Tuple2$.MODULE$.apply("test_key_01", "test_value_02")),
        WrappedArray.make(new String[]{"array01", "array02", "array03"}), "aaa", "aaa2"
    });
    assertEquals("Should have rows matching the expected rows", expected, rows);

    sql("drop table {0}.{1}", database, tableA);
  }

}
