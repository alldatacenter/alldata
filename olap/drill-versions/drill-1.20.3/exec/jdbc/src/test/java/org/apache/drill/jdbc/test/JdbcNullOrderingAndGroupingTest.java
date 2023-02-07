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
package org.apache.drill.jdbc.test;

import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.categories.JdbcTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.junit.experimental.categories.Category;

@Category(JdbcTest.class)
public class JdbcNullOrderingAndGroupingTest extends JdbcTestQueryBase {
  static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(JdbcNullOrderingAndGroupingTest.class);

  // TODO:  Move this to where is covers more tests:  HACK: Disable Jetty
  // status(?) server so unit tests run (without Maven setup).
  @BeforeClass
  public static void setUpClass() throws Exception {
    testQuery(String.format("alter session set `%s` = true", PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY));
    System.setProperty( "drill.exec.http.enabled", "false" );
  }

  @AfterClass
  public static void resetDefaults() throws Exception {
    testQuery(String.format("alter session set `%s` = false", PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY));
  }


  ////////////////////
  // DonutsTopping3:

  @Test
  public void testOrderDonutsTopping3AscNullsFirst() throws Exception {
    withNoDefaultSchema()
        .sql( "SELECT tbl.id, tbl.topping[3].type AS topping3 \n" +
              "FROM cp.`donuts.json` AS tbl \n" +
              "ORDER BY topping3 ASC NULLS FIRST" )
        .returns( "id=0005; topping3=null\n" +
                  "id=0002; topping3=Chocolate\n" +
                  "id=0003; topping3=Maple\n" +
                  "id=0001; topping3=Powdered Sugar\n" +
                  "id=0004; topping3=Powdered Sugar" );
  }

  @Test
  public void testOrderDonutsTopping3AscNullsLast() throws Exception {
     withNoDefaultSchema()
         .sql( "SELECT tbl.id, tbl.topping[3].type AS topping3 \n" +
               "FROM cp.`donuts.json` AS tbl \n" +
               "ORDER BY topping3 ASC NULLS LAST" )
         .returns( "id=0002; topping3=Chocolate\n" +
                   "id=0003; topping3=Maple\n" +
                   "id=0001; topping3=Powdered Sugar\n" +
                   "id=0004; topping3=Powdered Sugar\n" +
                   "id=0005; topping3=null"
                   );
  }

  @Test
  public void testOrderDonutsTopping3AscNullsDefaultLast() throws Exception {
    withNoDefaultSchema()
        .sql( "SELECT tbl.id, tbl.topping[3].type AS topping3 \n" +
              "FROM cp.`donuts.json` AS tbl \n" +
              "ORDER BY topping3 ASC" )
        .returns( "id=0002; topping3=Chocolate\n" +
                  "id=0003; topping3=Maple\n" +
                  "id=0001; topping3=Powdered Sugar\n" +
                  "id=0004; topping3=Powdered Sugar\n" +
                  "id=0005; topping3=null" );
  }

  @Test
  public void testOrderDonutsTopping3DescNullsFirst() throws Exception {
    withNoDefaultSchema()
        .sql( "SELECT tbl.id, tbl.topping[3].type AS topping3 \n" +
              "FROM cp.`donuts.json` AS tbl \n" +
              "ORDER BY topping3 DESC NULLS FIRST" )
        .returns( "id=0005; topping3=null\n" +
                  "id=0001; topping3=Powdered Sugar\n" +
                  "id=0004; topping3=Powdered Sugar\n" +
                  "id=0003; topping3=Maple\n" +
                  "id=0002; topping3=Chocolate" );
  }

  @Test
  public void testOrderDonutsTopping3DescNullsLast() throws Exception {
    withNoDefaultSchema()
        .sql( "SELECT tbl.id, tbl.topping[3].type AS topping3 \n" +
              "FROM cp.`donuts.json` AS tbl \n" +
              "ORDER BY topping3 DESC NULLS LAST" )
        .returns( "id=0001; topping3=Powdered Sugar\n" +
                  "id=0004; topping3=Powdered Sugar\n" +
                  "id=0003; topping3=Maple\n" +
                  "id=0002; topping3=Chocolate\n" +
                  "id=0005; topping3=null" );
  }

  @Test
  public void testOrderDonutsTopping3DescNullsDefaultFirst() throws Exception {
    withNoDefaultSchema()
        .sql( "SELECT tbl.id, tbl.topping[3].type AS topping3 \n" +
              "FROM cp.`donuts.json` AS tbl \n" +
              "ORDER BY topping3 DESC" )
        .returns( "id=0005; topping3=null\n" +
                  "id=0001; topping3=Powdered Sugar\n" +
                  "id=0004; topping3=Powdered Sugar\n" +
                  "id=0003; topping3=Maple\n" +
                  "id=0002; topping3=Chocolate" );
  }

  @Test
  public void testOrderDonutsTopping3DefaultedAscNullsFirst() throws Exception {
    withNoDefaultSchema()
        .sql( "SELECT tbl.id, tbl.topping[3].type AS topping3 \n" +
              "FROM cp.`donuts.json` AS tbl \n" +
              "ORDER BY topping3 NULLS FIRST" )
        .returns( "id=0005; topping3=null\n" +
                  "id=0002; topping3=Chocolate\n" +
                  "id=0003; topping3=Maple\n" +
                  "id=0001; topping3=Powdered Sugar\n" +
                  "id=0004; topping3=Powdered Sugar" );
  }

  @Test
  public void testOrderDonutsTopping3DefaultedAscNullsLast() throws Exception {
    withNoDefaultSchema()
        .sql( "SELECT tbl.id, tbl.topping[3].type AS topping3 \n" +
              "FROM cp.`donuts.json` AS tbl \n" +
              "ORDER BY topping3 NULLS LAST" )
        .returns( "id=0002; topping3=Chocolate\n" +
                  "id=0003; topping3=Maple\n" +
                  "id=0001; topping3=Powdered Sugar\n" +
                  "id=0004; topping3=Powdered Sugar\n" +
                  "id=0005; topping3=null" );
  }

  @Test
  public void testOrderDonutsTopping3DefaultedAscNullsDefaultLast() throws Exception {
    withNoDefaultSchema()
        .sql( "SELECT tbl.id, tbl.topping[3].type AS topping3 \n" +
              "FROM cp.`donuts.json` AS tbl \n" +
              "ORDER BY topping3" )
        .returns( "id=0002; topping3=Chocolate\n" +
                  "id=0003; topping3=Maple\n" +
                  "id=0001; topping3=Powdered Sugar\n" +
                  "id=0004; topping3=Powdered Sugar\n" +
                  "id=0005; topping3=null" );
  }


  ////////////////////
  // type VARCHAR? / VarChar:

  @Test
  public void testOrderVarCharAscNullsFirst() throws Exception {
    withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_VarChar AS VARCHAR ) AS as_VARCHAR \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_VARCHAR ASC NULLS FIRST" )
        .returns( "id=2; as_VARCHAR=null\n" +
                  "id=3; as_VARCHAR=A\n" +
                  "id=1; as_VARCHAR=B" );  // TODO: Revisit VARCHAR's acting as VARCHAR(1) in cast
  }

  @Test
  public void testOrderVarCharAscNullsLast() throws Exception {
     withNoDefaultSchema()
         .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_VarChar AS VARCHAR ) AS as_VARCHAR \n" +
               "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
               "ORDER BY as_VARCHAR ASC NULLS LAST" )
         .returns( "id=3; as_VARCHAR=A\n" +
                   "id=1; as_VARCHAR=B\n" +  // TODO: Revisit VARCHAR's acting as VARCHAR(1)
                   "id=2; as_VARCHAR=null"
                   );
  }

  @Test
  public void testOrderVarCharAscNullsDefaultLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_VarChar AS VARCHAR ) AS as_VARCHAR \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_VARCHAR ASC" )
        .returns( "id=3; as_VARCHAR=A\n" +
                  "id=1; as_VARCHAR=B\n" +  // TODO: Revisit VARCHAR's acting as VARCHAR(1)
                  "id=2; as_VARCHAR=null" );
  }

  @Test
  public void testOrderVarCharDescNullsFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_VarChar AS VARCHAR ) AS as_VARCHAR \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_VARCHAR DESC NULLS FIRST" )
        .returns( "id=2; as_VARCHAR=null\n" +
                  "id=1; as_VARCHAR=B\n" +  // TODO: Revisit VARCHAR's acting as VARCHAR(1)
                  "id=3; as_VARCHAR=A" );
  }

  @Test
  public void testOrderVarCharDescNullsLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_VarChar AS VARCHAR ) AS as_VARCHAR \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_VARCHAR DESC NULLS LAST" )
        .returns( "id=1; as_VARCHAR=B\n" +  // TODO: Revisit VARCHAR's acting as VARCHAR(1)
                  "id=3; as_VARCHAR=A\n" +
                  "id=2; as_VARCHAR=null" );
  }

  @Test
  public void testOrderVarCharDescNullsDefaultFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_VarChar AS VARCHAR ) AS as_VARCHAR \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_VARCHAR DESC" )
        .returns( "id=2; as_VARCHAR=null\n" +
                  "id=1; as_VARCHAR=B\n" +  // TODO: Revisit VARCHAR's acting as VARCHAR(1)
                  "id=3; as_VARCHAR=A" );
  }

  @Test
  public void testOrderVarCharDefaultedAscNullsFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_VarChar AS VARCHAR ) AS as_VARCHAR \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_VARCHAR NULLS FIRST" )
        .returns( "id=2; as_VARCHAR=null\n" +
                  "id=3; as_VARCHAR=A\n" +
                  "id=1; as_VARCHAR=B" );  // TODO: Revisit VARCHAR's acting as VARCHAR(1)
  }

  @Test
  public void testOrderVarCharDefaultedAscNullsLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_VarChar AS VARCHAR ) AS as_VARCHAR \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_VARCHAR NULLS LAST" )
        .returns( "id=3; as_VARCHAR=A\n" +
                  "id=1; as_VARCHAR=B\n" +  // TODO: Revisit VARCHAR's acting as VARCHAR(1)
                  "id=2; as_VARCHAR=null" );
  }

  @Test
  public void testOrderVarCharDefaultedAscNullsDefaultLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_VarChar AS VARCHAR ) AS as_VARCHAR \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_VARCHAR" )
        .returns( "id=3; as_VARCHAR=A\n" +
                  "id=1; as_VARCHAR=B\n" +  // TODO: Revisit VARCHAR's acting as VARCHAR(1)
                  "id=2; as_VARCHAR=null" );
  }


  ////////////////////
  // type INTEGER / Int:

  @Test
  public void testOrderIntAscNullsFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Int AS INT ) AS as_INT \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_INT ASC NULLS FIRST" )
        .returns( "id=2; as_INT=null\n" +
                  "id=3; as_INT=19\n" +
                  "id=1; as_INT=180" );
  }

  @Test
  public void testOrderIntAscNullsLast() throws Exception {
      withNoDefaultSchema()
         .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Int AS INT ) AS as_INT \n" +
               "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
               "ORDER BY as_INT ASC NULLS LAST" )
         .returns( "id=3; as_INT=19\n" +
                   "id=1; as_INT=180\n" +
                   "id=2; as_INT=null"
                   );
  }

  @Test
  public void testOrderIntAscNullsDefaultLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Int AS INT ) AS as_INT \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_INT ASC" )
        .returns( "id=3; as_INT=19\n" +
                  "id=1; as_INT=180\n" +
                  "id=2; as_INT=null" );
  }

  @Test
  public void testOrderIntDescNullsFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Int AS INT ) AS as_INT \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_INT DESC NULLS FIRST" )
        .returns( "id=2; as_INT=null\n" +
                  "id=1; as_INT=180\n" +
                  "id=3; as_INT=19" );
  }

  @Test
  public void testOrderIntDescNullsLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Int AS INT ) AS as_INT \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_INT DESC NULLS LAST" )
        .returns( "id=1; as_INT=180\n" +
                  "id=3; as_INT=19\n" +
                  "id=2; as_INT=null" );
  }

  @Test
  public void testOrderIntDescNullsDefaultFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Int AS INT ) AS as_INT \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_INT DESC" )
        .returns( "id=2; as_INT=null\n" +
                  "id=1; as_INT=180\n" +
                  "id=3; as_INT=19" );
  }

  @Test
  public void testOrderIntDefaultedAscNullsFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Int AS INT ) AS as_INT \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_INT NULLS FIRST" )
        .returns( "id=2; as_INT=null\n" +
                  "id=3; as_INT=19\n" +
                  "id=1; as_INT=180" );
  }

  @Test
  public void testOrderIntDefaultedAscNullsLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Int AS INT ) AS as_INT \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_INT NULLS LAST" )
        .returns( "id=3; as_INT=19\n" +
                  "id=1; as_INT=180\n" +
                  "id=2; as_INT=null" );
  }

  @Test
  public void testOrderIntDefaultedAscNullsDefaultLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Int AS INT ) AS as_INT \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_INT" )
        .returns( "id=3; as_INT=19\n" +
                  "id=1; as_INT=180\n" +
                  "id=2; as_INT=null" );
  }


  ////////////////////
  // type FLOAT? / Float?:

  @Test
  public void testOrderFloatAscNullsFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Float AS FLOAT ) AS as_FLOAT \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_FLOAT ASC NULLS FIRST" )
        .returns( "id=2; as_FLOAT=null\n" +
                  "id=3; as_FLOAT=19.0\n" +
                  "id=1; as_FLOAT=180.0" );
  }

  @Test
  public void testOrderFloatAscNullsLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Float AS FLOAT ) AS as_FLOAT \n" +
               "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
               "ORDER BY as_FLOAT ASC NULLS LAST" )
        .returns( "id=3; as_FLOAT=19.0\n" +
                   "id=1; as_FLOAT=180.0\n" +
                   "id=2; as_FLOAT=null"
                   );
  }

  @Test
  public void testOrderFloatAscNullsDefaultLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Float AS FLOAT ) AS as_FLOAT \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_FLOAT ASC" )
        .returns( "id=3; as_FLOAT=19.0\n" +
                  "id=1; as_FLOAT=180.0\n" +
                  "id=2; as_FLOAT=null" );
  }

  @Test
  public void testOrderFloatDescNullsFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Float AS FLOAT ) AS as_FLOAT \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_FLOAT DESC NULLS FIRST" )
        .returns( "id=2; as_FLOAT=null\n" +
                  "id=1; as_FLOAT=180.0\n" +
                  "id=3; as_FLOAT=19.0" );
  }

  @Test
  public void testOrderFloatDescNullsLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Float AS FLOAT ) AS as_FLOAT \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_FLOAT DESC NULLS LAST" )
        .returns( "id=1; as_FLOAT=180.0\n" +
                  "id=3; as_FLOAT=19.0\n" +
                  "id=2; as_FLOAT=null" );
  }

  @Test
  public void testOrderFloatDescNullsDefaultFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Float AS FLOAT ) AS as_FLOAT \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_FLOAT DESC" )
        .returns( "id=2; as_FLOAT=null\n" +
                  "id=1; as_FLOAT=180.0\n" +
                  "id=3; as_FLOAT=19.0" );
  }

  @Test
  public void testOrderFloatDefaultedAscNullsFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Float AS FLOAT ) AS as_FLOAT \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_FLOAT NULLS FIRST" )
        .returns( "id=2; as_FLOAT=null\n" +
                  "id=3; as_FLOAT=19.0\n" +
                  "id=1; as_FLOAT=180.0" );
  }

  @Test
  public void testOrderFloatDefaultedAscNullsLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Float AS FLOAT ) AS as_FLOAT \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_FLOAT NULLS LAST" )
        .returns( "id=3; as_FLOAT=19.0\n" +
                  "id=1; as_FLOAT=180.0\n" +
                  "id=2; as_FLOAT=null" );
  }

  @Test
  public void testOrderFloatDefaultedAscNullsDefaultLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Float AS FLOAT ) AS as_FLOAT \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_FLOAT" )
        .returns( "id=3; as_FLOAT=19.0\n" +
                  "id=1; as_FLOAT=180.0\n" +
                  "id=2; as_FLOAT=null" );
  }


  ////////////////////
  // type BIGINT / BigInt:

  @Test
  public void testOrderBigIntAscNullsFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_BigInt AS BIGINT ) AS as_BIGINT \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_BIGINT ASC NULLS FIRST" )
        .returns( "id=2; as_BIGINT=null\n" +
                  "id=3; as_BIGINT=19\n" +
                  "id=1; as_BIGINT=180" );
  }

  @Test
  public void testOrderBigIntAscNullsLast() throws Exception {
     withNoDefaultSchema()
         .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_BigInt AS BIGINT ) AS as_BIGINT \n" +
               "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
               "ORDER BY as_BIGINT ASC NULLS LAST" )
         .returns( "id=3; as_BIGINT=19\n" +
                   "id=1; as_BIGINT=180\n" +
                   "id=2; as_BIGINT=null"
                   );
  }

  @Test
  public void testOrderBigIntAscNullsDefaultLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_BigInt AS BIGINT ) AS as_BIGINT \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_BIGINT ASC" )
        .returns( "id=3; as_BIGINT=19\n" +
                  "id=1; as_BIGINT=180\n" +
                  "id=2; as_BIGINT=null" );
  }

  @Test
  public void testOrderBigIntDescNullsFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_BigInt AS BIGINT ) AS as_BIGINT \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_BIGINT DESC NULLS FIRST" )
        .returns( "id=2; as_BIGINT=null\n" +
                  "id=1; as_BIGINT=180\n" +
                  "id=3; as_BIGINT=19" );
  }

  @Test
  public void testOrderBigIntDescNullsLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_BigInt AS BIGINT ) AS as_BIGINT \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_BIGINT DESC NULLS LAST" )
        .returns( "id=1; as_BIGINT=180\n" +
                  "id=3; as_BIGINT=19\n" +
                  "id=2; as_BIGINT=null" );
  }

  @Test
  public void testOrderBigIntDescNullsDefaultFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_BigInt AS BIGINT ) AS as_BIGINT \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_BIGINT DESC" )
        .returns( "id=2; as_BIGINT=null\n" +
                  "id=1; as_BIGINT=180\n" +
                  "id=3; as_BIGINT=19" );
  }

  @Test
  public void testOrderBigIntDefaultedAscNullsFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_BigInt AS BIGINT ) AS as_BIGINT \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_BIGINT NULLS FIRST" )
        .returns( "id=2; as_BIGINT=null\n" +
                  "id=3; as_BIGINT=19\n" +
                  "id=1; as_BIGINT=180" );
  }

  @Test
  public void testOrderBigIntDefaultedAscNullsLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_BigInt AS BIGINT ) AS as_BIGINT \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_BIGINT NULLS LAST" )
        .returns( "id=3; as_BIGINT=19\n" +
                  "id=1; as_BIGINT=180\n" +
                  "id=2; as_BIGINT=null" );
  }

  @Test
  public void testOrderBigIntDefaultedAscNullsDefaultLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_BigInt AS BIGINT ) AS as_BIGINT \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_BIGINT" )
        .returns( "id=3; as_BIGINT=19\n" +
                  "id=1; as_BIGINT=180\n" +
                  "id=2; as_BIGINT=null" );
  }


  ////////////////////
  // Type DATE? / Date?:

  @Test
  public void testOrderDateAscNullsFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Date AS DATE ) AS as_DATE \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_DATE ASC NULLS FIRST" )
        .returns( "id=2; as_DATE=null\n" +
                  "id=3; as_DATE=2014-01-01\n" +
                  "id=1; as_DATE=2014-12-31" );
  }

  @Test
  public void testOrderDateAscNullsLast() throws Exception {
      withNoDefaultSchema()
         .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Date AS DATE ) AS as_DATE \n" +
               "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
               "ORDER BY as_DATE ASC NULLS LAST" )
         .returns( "id=3; as_DATE=2014-01-01\n" +
                   "id=1; as_DATE=2014-12-31\n" +
                   "id=2; as_DATE=null"
                   );
  }

  @Test
  public void testOrderDateAscNullsDefaultLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Date AS DATE ) AS as_DATE \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_DATE ASC" )
        .returns( "id=3; as_DATE=2014-01-01\n" +
                  "id=1; as_DATE=2014-12-31\n" +
                  "id=2; as_DATE=null" );
  }

  @Test
  public void testOrderDateDescNullsFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Date AS DATE ) AS as_DATE \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_DATE DESC NULLS FIRST" )
        .returns( "id=2; as_DATE=null\n" +
                  "id=1; as_DATE=2014-12-31\n" +
                  "id=3; as_DATE=2014-01-01" );
  }

  @Test
  public void testOrderDateDescNullsLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Date AS DATE ) AS as_DATE \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_DATE DESC NULLS LAST" )
        .returns( "id=1; as_DATE=2014-12-31\n" +
                  "id=3; as_DATE=2014-01-01\n" +
                  "id=2; as_DATE=null" );
  }

  @Test
  public void testOrderDateDescNullsDefaultFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Date AS DATE ) AS as_DATE \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_DATE DESC" )
        .returns( "id=2; as_DATE=null\n" +
                  "id=1; as_DATE=2014-12-31\n" +
                  "id=3; as_DATE=2014-01-01" );
  }

  @Test
  public void testOrderDateDefaultedAscNullsFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Date AS DATE ) AS as_DATE \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_DATE NULLS FIRST" )
        .returns( "id=2; as_DATE=null\n" +
                  "id=3; as_DATE=2014-01-01\n" +
                  "id=1; as_DATE=2014-12-31" );
  }

  @Test
  public void testOrderDateDefaultedAscNullsLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Date AS DATE ) AS as_DATE \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_DATE NULLS LAST" )
        .returns( "id=3; as_DATE=2014-01-01\n" +
                  "id=1; as_DATE=2014-12-31\n" +
                  "id=2; as_DATE=null" );
  }

  @Test
  public void testOrderDateDefaultedAscNullsDefaultLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Date AS DATE ) AS as_DATE \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_DATE" )
        .returns( "id=3; as_DATE=2014-01-01\n" +
                  "id=1; as_DATE=2014-12-31\n" +
                  "id=2; as_DATE=null" );
  }


  ////////////////////
  // Type INTERVAL? / Interval?:

  @Test
  public void testOrderIntervalAscNullsFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Interval AS INTERVAL HOUR ) AS as_INTERVAL \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_INTERVAL ASC NULLS FIRST" )
        .returns( "id=2; as_INTERVAL=null\n" +
                  "id=3; as_INTERVAL=PT3600S\n" +
                  "id=1; as_INTERVAL=PT7200S" );
  }

  @Test
  public void testOrderIntervalAscNullsLast() throws Exception {
     withNoDefaultSchema()
         .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Interval AS INTERVAL HOUR ) AS as_INTERVAL \n" +
               "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
               "ORDER BY as_INTERVAL ASC NULLS LAST" )
         .returns( "id=3; as_INTERVAL=PT3600S\n" +
                   "id=1; as_INTERVAL=PT7200S\n" +
                   "id=2; as_INTERVAL=null"
                   );
  }

  @Test
  public void testOrderIntervalAscNullsDefaultLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Interval AS INTERVAL HOUR ) AS as_INTERVAL \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_INTERVAL ASC" )
        .returns( "id=3; as_INTERVAL=PT3600S\n" +
                  "id=1; as_INTERVAL=PT7200S\n" +
                  "id=2; as_INTERVAL=null" );
  }

  @Test
  public void testOrderIntervalDescNullsFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Interval AS INTERVAL HOUR ) AS as_INTERVAL \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_INTERVAL DESC NULLS FIRST" )
        .returns( "id=2; as_INTERVAL=null\n" +
                  "id=1; as_INTERVAL=PT7200S\n" +
                  "id=3; as_INTERVAL=PT3600S" );
  }

  @Test
  public void testOrderIntervalDescNullsLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Interval AS INTERVAL HOUR ) AS as_INTERVAL \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_INTERVAL DESC NULLS LAST" )
        .returns( "id=1; as_INTERVAL=PT7200S\n" +
                  "id=3; as_INTERVAL=PT3600S\n" +
                  "id=2; as_INTERVAL=null" );
  }

  @Test
  public void testOrderIntervalDescNullsDefaultFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Interval AS INTERVAL HOUR ) AS as_INTERVAL \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_INTERVAL DESC" )
        .returns( "id=2; as_INTERVAL=null\n" +
                  "id=1; as_INTERVAL=PT7200S\n" +
                  "id=3; as_INTERVAL=PT3600S" );
  }

  @Test
  public void testOrderIntervalDefaultedAscNullsFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Interval AS INTERVAL HOUR ) AS as_INTERVAL \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_INTERVAL NULLS FIRST" )
        .returns( "id=2; as_INTERVAL=null\n" +
                  "id=3; as_INTERVAL=PT3600S\n" +
                  "id=1; as_INTERVAL=PT7200S" );
  }

  @Test
  public void testOrderIntervalDefaultedAscNullsLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Interval AS INTERVAL HOUR ) AS as_INTERVAL \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_INTERVAL NULLS LAST" )
        .returns( "id=3; as_INTERVAL=PT3600S\n" +
                  "id=1; as_INTERVAL=PT7200S\n" +
                  "id=2; as_INTERVAL=null" );
  }

  @Test
  public void testOrderIntervalDefaultedAscNullsDefaultLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Interval AS INTERVAL HOUR ) AS as_INTERVAL \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_INTERVAL" )
        .returns( "id=3; as_INTERVAL=PT3600S\n" +
                  "id=1; as_INTERVAL=PT7200S\n" +
                  "id=2; as_INTERVAL=null" );
  }


  ////////////////////
  // type DECIMAL / Decimal9?:

  @Test
  public void testOrderDecimalAscNullsFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Decimal AS DECIMAL ) AS as_DECIMAL \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_DECIMAL ASC NULLS FIRST" )
        .returns( "id=2; as_DECIMAL=null\n" +
                  "id=3; as_DECIMAL=19\n" +
                  "id=1; as_DECIMAL=180" );
  }

  @Test
  public void testOrderDecimalAscNullsLast() throws Exception {
     withNoDefaultSchema()
         .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Decimal AS DECIMAL ) AS as_DECIMAL \n" +
               "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
               "ORDER BY as_DECIMAL ASC NULLS LAST" )
         .returns( "id=3; as_DECIMAL=19\n" +
                   "id=1; as_DECIMAL=180\n" +
                   "id=2; as_DECIMAL=null"
                   );
  }

  @Test
  public void testOrderDecimalAscNullsDefaultLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Decimal AS DECIMAL ) AS as_DECIMAL \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_DECIMAL ASC" )
        .returns( "id=3; as_DECIMAL=19\n" +
                  "id=1; as_DECIMAL=180\n" +
                  "id=2; as_DECIMAL=null" );
  }

  @Test
  public void testOrderDecimalDescNullsFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Decimal AS DECIMAL ) AS as_DECIMAL \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_DECIMAL DESC NULLS FIRST" )
        .returns( "id=2; as_DECIMAL=null\n" +
                  "id=1; as_DECIMAL=180\n" +
                  "id=3; as_DECIMAL=19" );
  }

  @Test
  public void testOrderDecimalDescNullsLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Decimal AS DECIMAL ) AS as_DECIMAL \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_DECIMAL DESC NULLS LAST" )
        .returns( "id=1; as_DECIMAL=180\n" +
                  "id=3; as_DECIMAL=19\n" +
                  "id=2; as_DECIMAL=null" );
  }

  @Test
  public void testOrderDecimalDescNullsDefaultFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Decimal AS DECIMAL ) AS as_DECIMAL \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_DECIMAL DESC" )
        .returns( "id=2; as_DECIMAL=null\n" +
                  "id=1; as_DECIMAL=180\n" +
                  "id=3; as_DECIMAL=19" );
  }

  @Test
  public void testOrderDecimalDefaultedAscNullsFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Decimal AS DECIMAL ) AS as_DECIMAL \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_DECIMAL NULLS FIRST" )
        .returns( "id=2; as_DECIMAL=null\n" +
                  "id=3; as_DECIMAL=19\n" +
                  "id=1; as_DECIMAL=180" );
  }

  @Test
  public void testOrderDecimalDefaultedAscNullsLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Decimal AS DECIMAL ) AS as_DECIMAL \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_DECIMAL NULLS LAST" )
        .returns( "id=3; as_DECIMAL=19\n" +
                  "id=1; as_DECIMAL=180\n" +
                  "id=2; as_DECIMAL=null" );
  }

  @Test
  public void testOrderDecimalDefaultedAscNullsDefaultLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Decimal AS DECIMAL ) AS as_DECIMAL \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_DECIMAL" )
        .returns( "id=3; as_DECIMAL=19\n" +
                  "id=1; as_DECIMAL=180\n" +
                  "id=2; as_DECIMAL=null" );
  }

  ////////////////////
  // type DECIMAL(5) / Decimal9(?):
  // Note: DECIMAL(5) and DECIMAL(35) are both tested because Decimal9 (and
  // Decimal18) and Decimal38Sparse (and Decimal28Sparse) involve different
  // code paths for NULLS FIRST/LAST handling.

  @Test
  public void testOrderDecimal5AscNullsFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Decimal5 AS DECIMAL(5) ) AS as_DECIMAL5 \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_DECIMAL5 ASC NULLS FIRST" )
        .returns( "id=2; as_DECIMAL5=null\n" +
                  "id=3; as_DECIMAL5=1235\n" +
                  "id=1; as_DECIMAL5=9877" );
  }

  @Test
  public void testOrderDecimal5AscNullsLast() throws Exception {
     withNoDefaultSchema()
         .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Decimal5 AS DECIMAL(5) ) AS as_DECIMAL5 \n" +
               "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
               "ORDER BY as_DECIMAL5 ASC NULLS LAST" )
         .returns( "id=3; as_DECIMAL5=1235\n" +
                   "id=1; as_DECIMAL5=9877\n" +
                   "id=2; as_DECIMAL5=null"
                   );
  }

  @Test
  public void testOrderDecimal5AscNullsDefaultLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Decimal5 AS DECIMAL(5) ) AS as_DECIMAL5 \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_DECIMAL5 ASC" )
        .returns( "id=3; as_DECIMAL5=1235\n" +
                  "id=1; as_DECIMAL5=9877\n" +
                  "id=2; as_DECIMAL5=null" );
  }

  @Test
  public void testOrderDecimal5DescNullsFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Decimal5 AS DECIMAL(5) ) AS as_DECIMAL5 \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_DECIMAL5 DESC NULLS FIRST" )
        .returns( "id=2; as_DECIMAL5=null\n" +
                  "id=1; as_DECIMAL5=9877\n" +
                  "id=3; as_DECIMAL5=1235" );
  }

  @Test
  public void testOrderDecimal5DescNullsLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Decimal5 AS DECIMAL(5) ) AS as_DECIMAL5 \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_DECIMAL5 DESC NULLS LAST" )
        .returns( "id=1; as_DECIMAL5=9877\n" +
                  "id=3; as_DECIMAL5=1235\n" +
                  "id=2; as_DECIMAL5=null" );
  }

  @Test
  public void testOrderDecimal5DescNullsDefaultFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Decimal5 AS DECIMAL(5) ) AS as_DECIMAL5 \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_DECIMAL5 DESC" )
        .returns( "id=2; as_DECIMAL5=null\n" +
                  "id=1; as_DECIMAL5=9877\n" +
                  "id=3; as_DECIMAL5=1235" );
  }

  @Test
  public void testOrderDecimal5DefaultedAscNullsFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Decimal5 AS DECIMAL(5) ) AS as_DECIMAL5 \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_DECIMAL5 NULLS FIRST" )
        .returns( "id=2; as_DECIMAL5=null\n" +
                  "id=3; as_DECIMAL5=1235\n" +
                  "id=1; as_DECIMAL5=9877" );
  }

  @Test
  public void testOrderDecimal5DefaultedAscNullsLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Decimal5 AS DECIMAL(5) ) AS as_DECIMAL5 \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_DECIMAL5 NULLS LAST" )
        .returns( "id=3; as_DECIMAL5=1235\n" +
                  "id=1; as_DECIMAL5=9877\n" +
                  "id=2; as_DECIMAL5=null" );
  }

  @Test
  public void testOrderDecimal5DefaultedAscNullsDefaultLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Decimal5 AS DECIMAL(5) ) AS as_DECIMAL5 \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_DECIMAL5" )
        .returns( "id=3; as_DECIMAL5=1235\n" +
                  "id=1; as_DECIMAL5=9877\n" +
                  "id=2; as_DECIMAL5=null" );
  }

  ////////////////////
  // type DECIMAL(35) / Decimal39Sparse(?):
  // Note: DECIMAL(5) and DECIMAL(35) are both tested because Decimal9 (and
  // Decimal18) and Decimal38Sparse (and Decimal28Sparse) involve different
  // code paths for NULLS FIRST/LAST handling.

  @Test
  public void testOrderDecimal35AscNullsFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Decimal35 AS DECIMAL(35) ) AS as_DECIMAL35 \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_DECIMAL35 ASC NULLS FIRST" )
        .returns( "id=2; as_DECIMAL35=null\n" +
                  "id=3; as_DECIMAL35=12345678901234567890123456789012345\n" +
                  "id=1; as_DECIMAL35=43210987654321098765432109876543210" );
  }

  @Test
  public void testOrderDecimal35AscNullsLast() throws Exception {
     withNoDefaultSchema()
         .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Decimal35 AS DECIMAL(35) ) AS as_DECIMAL35 \n" +
               "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
               "ORDER BY as_DECIMAL35 ASC NULLS LAST" )
         .returns( "id=3; as_DECIMAL35=12345678901234567890123456789012345\n" +
                   "id=1; as_DECIMAL35=43210987654321098765432109876543210\n" +
                   "id=2; as_DECIMAL35=null"
                   );
  }

  @Test
  public void testOrderDecimal35AscNullsDefaultLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Decimal35 AS DECIMAL(35) ) AS as_DECIMAL35 \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_DECIMAL35 ASC" )
        .returns( "id=3; as_DECIMAL35=12345678901234567890123456789012345\n" +
                  "id=1; as_DECIMAL35=43210987654321098765432109876543210\n" +
                  "id=2; as_DECIMAL35=null" );
  }

  @Test
  public void testOrderDecimal35DescNullsFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Decimal35 AS DECIMAL(35) ) AS as_DECIMAL35 \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_DECIMAL35 DESC NULLS FIRST" )
        .returns( "id=2; as_DECIMAL35=null\n" +
                  "id=1; as_DECIMAL35=43210987654321098765432109876543210\n" +
                  "id=3; as_DECIMAL35=12345678901234567890123456789012345" );
  }

  @Test
  public void testOrderDecimal35DescNullsLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Decimal35 AS DECIMAL(35) ) AS as_DECIMAL35 \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_DECIMAL35 DESC NULLS LAST" )
        .returns( "id=1; as_DECIMAL35=43210987654321098765432109876543210\n" +
                  "id=3; as_DECIMAL35=12345678901234567890123456789012345\n" +
                  "id=2; as_DECIMAL35=null" );
  }

  @Test
  public void testOrderDecimal35DescNullsDefaultFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Decimal35 AS DECIMAL(35) ) AS as_DECIMAL35 \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_DECIMAL35 DESC" )
        .returns( "id=2; as_DECIMAL35=null\n" +
                  "id=1; as_DECIMAL35=43210987654321098765432109876543210\n" +
                  "id=3; as_DECIMAL35=12345678901234567890123456789012345" );
  }

  @Test
  public void testOrderDecimal35DefaultedAscNullsFirst() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Decimal35 AS DECIMAL(35) ) AS as_DECIMAL35 \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_DECIMAL35 NULLS FIRST" )
        .returns( "id=2; as_DECIMAL35=null\n" +
                  "id=3; as_DECIMAL35=12345678901234567890123456789012345\n" +
                  "id=1; as_DECIMAL35=43210987654321098765432109876543210" );
  }

  @Test
  public void testOrderDecimal35DefaultedAscNullsLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Decimal35 AS DECIMAL(35) ) AS as_DECIMAL35 \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_DECIMAL35 NULLS LAST" )
        .returns( "id=3; as_DECIMAL35=12345678901234567890123456789012345\n" +
                  "id=1; as_DECIMAL35=43210987654321098765432109876543210\n" +
                  "id=2; as_DECIMAL35=null" );
  }

  @Test
  public void testOrderDecimal35DefaultedAscNullsDefaultLast() throws Exception {
     withNoDefaultSchema()
        .sql( "SELECT tbl.id, \n" +
              "       CAST( tbl.for_Decimal35 AS DECIMAL(35) ) AS as_DECIMAL35 \n" +
              "FROM cp.`null_ordering_and_grouping_data.json` AS tbl \n" +
              "ORDER BY as_DECIMAL35" )
        .returns( "id=3; as_DECIMAL35=12345678901234567890123456789012345\n" +
                  "id=1; as_DECIMAL35=43210987654321098765432109876543210\n" +
                  "id=2; as_DECIMAL35=null" );
  }
}
