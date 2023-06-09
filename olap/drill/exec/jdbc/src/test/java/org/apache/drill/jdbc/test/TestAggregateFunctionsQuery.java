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

import java.sql.Date;

import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.jdbc.Driver;
import org.apache.drill.categories.JdbcTest;
import org.joda.time.chrono.ISOChronology;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(JdbcTest.class)
public class TestAggregateFunctionsQuery extends JdbcTestQueryBase {

  // enable decimal data type
  @BeforeClass
  public static void enableDecimalDataType() throws Exception {
    testQuery(String.format("alter session set `%s` = true", PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY));
  }

  @AfterClass
  public static void disableDecimalDataType() throws Exception {
    testQuery(String.format("alter session set `%s` = false", PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY));
  }

  static{
    Driver.load();
  }

  @Test
  public void testDateAggFunction() throws Exception{
    String query = "SELECT max(cast(HIRE_DATE as date)) as MAX_DATE, min(cast(HIRE_DATE as date)) as MIN_DATE" +
        " FROM `employee.json`";

    String t  = new Date(ISOChronology.getInstance().getDateTimeMillis(1998, 1, 1, 0)).toString();
    String t1 = new Date(ISOChronology.getInstance().getDateTimeMillis(1993, 5, 1, 0)).toString();

    String result = "MAX_DATE="+ t + "; " + "MIN_DATE=" + t1 + "\n";

    withFull("cp")
        .sql(query)
        .returns(result);
  }

  @Test
  public void testIntervalAggFunction() throws Exception{
    String query = "select max(date_diff(date'2014-5-2', cast(HIRE_DATE as date))) as MAX_DAYS, min(date_diff(date'2014-5-2', cast(HIRE_DATE as date))) MIN_DAYS" +
        " FROM `employee.json`";

    withFull("cp")
        .sql(query)
        .returns(
            "MAX_DAYS=P7671D; " +
                "MIN_DAYS=P5965D\n"
        );
  }

  @Test
  public void testDecimalAggFunction() throws Exception{
    String query = "SELECT " +
        "max(cast(EMPLOYEE_ID as decimal(9, 2))) as MAX_DEC9, min(cast(EMPLOYEE_ID as decimal(9, 2))) as MIN_DEC9," +
        "max(cast(EMPLOYEE_ID as decimal(18, 4))) as MAX_DEC18, min(cast(EMPLOYEE_ID as decimal(18, 4))) as MIN_DEC18," +
        "max(cast(EMPLOYEE_ID as decimal(28, 9))) as MAX_DEC28, min(cast(EMPLOYEE_ID as decimal(28, 9))) as MIN_DEC28," +
        "max(cast(EMPLOYEE_ID as decimal(38, 11))) as MAX_DEC38, min(cast(EMPLOYEE_ID as decimal(38, 11))) as MIN_DEC38" +
        " FROM `employee.json`";

    withFull("cp")
        .sql(query)
        .returns(
            "MAX_DEC9=1156.00; " +
                "MIN_DEC9=1.00; " +
                "MAX_DEC18=1156.0000; " +
                "MIN_DEC18=1.0000; " +
                "MAX_DEC28=1156.000000000; " +
                "MIN_DEC28=1.000000000; " +
                "MAX_DEC38=1156.00000000000; " +
                "MIN_DEC38=1.00000000000\n "
        );
  }

  @Test
  public void testVarCharAggFunction() throws Exception{
    String query = "select max(full_name) as MAX_NAME, min(full_name) as MIN_NAME FROM `employee.json`";

    withFull("cp")
        .sql(query)
        .returns(
            "MAX_NAME=Zach Lovell; " +
                "MIN_NAME=A. Joyce Jarvis\n"
        );
  }
}
