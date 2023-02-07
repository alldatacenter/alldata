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
package org.apache.drill.exec.work.prepare;

import java.util.List;

import org.apache.drill.common.types.Types;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.junit.Test;

public class TestLimit0VsRegularQueriesMetadata extends PreparedStatementTestBase {

  @Test
  public void stringCasts() throws Exception {
    String query = "select\n" +
        "cast(col_int as varchar(30)) as col_int,\n" +
        "cast(col_vrchr as varchar(31)) as col_vrchr,\n" +
        "cast(col_dt as varchar(32)) as col_dt,\n" +
        "cast(col_tim as varchar(33)) as col_tim,\n" +
        "cast(col_tmstmp as varchar(34)) as col_tmstmp,\n" +
        "cast(col_flt as varchar(35)) as col_flt,\n" +
        "cast(col_intrvl_yr as varchar(36)) as col_intrvl_yr,\n" +
        "cast(col_bln as varchar(37)) as col_bln\n" +
        "from cp.`parquet/alltypes_optional.parquet`";

    List<ExpectedColumnResult> expectedMetadata = ImmutableList.of(
        new ExpectedColumnResult("col_int", "CHARACTER VARYING", true, 30, 30, 0, false, String.class.getName()),
        new ExpectedColumnResult("col_vrchr", "CHARACTER VARYING", true, 31, 31, 0, false, String.class.getName()),
        new ExpectedColumnResult("col_dt", "CHARACTER VARYING", true, 32, 32, 0, false, String.class.getName()),
        new ExpectedColumnResult("col_tim", "CHARACTER VARYING", true, 33, 33, 0, false, String.class.getName()),
        new ExpectedColumnResult("col_tmstmp", "CHARACTER VARYING", true, 34, 34, 0, false, String.class.getName()),
        new ExpectedColumnResult("col_flt", "CHARACTER VARYING", true, 35, 35, 0, false, String.class.getName()),
        new ExpectedColumnResult("col_intrvl_yr", "CHARACTER VARYING", true, 36, 36, 0, false, String.class.getName()),
        new ExpectedColumnResult("col_bln", "CHARACTER VARYING", true, 37, 37, 0, false, String.class.getName())
    );

    verifyResults(query, expectedMetadata);
  }

  @Test
  public void stringCastForDecimal() throws Exception {
    try {
      test("alter session set `planner.enable_decimal_data_type` = true");
      String query = "select cast(commission_pct as varchar(50)) as commission_pct from cp.`parquet/fixedlenDecimal.parquet`";
      List<ExpectedColumnResult> expectedMetadata = ImmutableList.of(
          new ExpectedColumnResult("commission_pct", "CHARACTER VARYING", true, 50, 50, 0, false, String.class.getName()));

      verifyResults(query, expectedMetadata);
    } finally {
      resetSessionOption("planner.enable_decimal_data_type");
    }
  }

  @Test
  public void constants() throws Exception {
    String query = "select\n" +
        "'aaa' as col_a,\n" +
        "10 as col_i\n," +
        "cast(null as varchar(5)) as col_n\n," +
        "cast('aaa' as varchar(5)) as col_a_short,\n" +
        "cast(10 as varchar(5)) as col_i_short,\n" +
        "cast('aaaaaaaaaaaaa' as varchar(5)) as col_a_long,\n" +
        "cast(1000000000 as varchar(5)) as col_i_long\n" +
        "from (values(1))";

    List<ExpectedColumnResult> expectedMetadata = ImmutableList.of(
        new ExpectedColumnResult("col_a", "CHARACTER VARYING", false, 3, 3, 0, false, String.class.getName()),
        new ExpectedColumnResult("col_i", "INTEGER", false, 11, 0, 0, true, Integer.class.getName()),
        new ExpectedColumnResult("col_n", "CHARACTER VARYING", true, 5, 5, 0, false, String.class.getName()),
        new ExpectedColumnResult("col_a_short", "CHARACTER VARYING", false, 5, 5, 0, false, String.class.getName()),
        new ExpectedColumnResult("col_i_short", "CHARACTER VARYING", false, 5, 5, 0, false, String.class.getName()),
        new ExpectedColumnResult("col_a_long", "CHARACTER VARYING", false, 5, 5, 0, false, String.class.getName()),
        new ExpectedColumnResult("col_i_long", "CHARACTER VARYING", false, 5, 5, 0, false, String.class.getName())
    );

    verifyResults(query, expectedMetadata);
  }

  @Test
  public void windowFunctions() throws Exception {
    String query = "select\n" +
        "lead(sales_country) over (partition by sales_country order by region_id) as col_lead,\n" +
        "lag(sales_country) over (partition by sales_country order by region_id) as col_lag,\n" +
        "first_value(sales_country) over (partition by sales_country order by region_id) as col_first_value,\n" +
        "last_value(sales_country) over (partition by sales_country order by region_id) as col_last_value\n" +
        "from (select cast(sales_country as varchar(30)) as sales_country, region_id from cp.`region.json`)";

    List<ExpectedColumnResult> expectedMetadata = ImmutableList.of(
        new ExpectedColumnResult("col_lead", "CHARACTER VARYING", true, 30, 30, 0, false, String.class.getName()),
        new ExpectedColumnResult("col_lag", "CHARACTER VARYING", true, 30, 30, 0, false, String.class.getName()),
        new ExpectedColumnResult("col_first_value", "CHARACTER VARYING", true, 30, 30, 0, false, String.class.getName()),
        new ExpectedColumnResult("col_last_value", "CHARACTER VARYING", true, 30, 30, 0, false, String.class.getName())
    );

    verifyResults(query, expectedMetadata);
  }

  @Test
  public void functionsWithSameInOutLength() throws Exception {
    String query = "select\n" +
        "lower(sales_city) as lower_col,\n" +
        "upper(sales_city) as upper_col,\n" +
        "initcap(sales_city) as initcap_col,\n" +
        "reverse(sales_city) as reverse_col,\n" +
        "lower(cast(sales_city as varchar(30))) as lower_cast_col,\n" +
        "upper(cast(sales_city as varchar(30))) as upper_cast_col,\n" +
        "initcap(cast(sales_city as varchar(30))) as initcap_cast_col,\n" +
        "reverse(cast(sales_city as varchar(30))) as reverse_cast_col\n" +
        "from cp.`region.json`";

    List<ExpectedColumnResult> expectedMetadata = ImmutableList.of(
        new ExpectedColumnResult("lower_col", "CHARACTER VARYING", true, Types.MAX_VARCHAR_LENGTH, Types.MAX_VARCHAR_LENGTH, 0, false, String.class.getName()),
        new ExpectedColumnResult("upper_col", "CHARACTER VARYING", true, Types.MAX_VARCHAR_LENGTH, Types.MAX_VARCHAR_LENGTH, 0, false, String.class.getName()),
        new ExpectedColumnResult("initcap_col", "CHARACTER VARYING", true, Types.MAX_VARCHAR_LENGTH, Types.MAX_VARCHAR_LENGTH, 0, false, String.class.getName()),
        new ExpectedColumnResult("reverse_col", "CHARACTER VARYING", true, Types.MAX_VARCHAR_LENGTH, Types.MAX_VARCHAR_LENGTH, 0, false, String.class.getName()),
        new ExpectedColumnResult("lower_cast_col", "CHARACTER VARYING", true, 30, 30, 0, false, String.class.getName()),
        new ExpectedColumnResult("upper_cast_col", "CHARACTER VARYING", true, 30, 30, 0, false, String.class.getName()),
        new ExpectedColumnResult("initcap_cast_col", "CHARACTER VARYING", true, 30, 30, 0, false, String.class.getName()),
        new ExpectedColumnResult("reverse_cast_col", "CHARACTER VARYING", true, 30, 30, 0, false, String.class.getName())
    );

    verifyResults(query, expectedMetadata);
  }

  @Test
  public void ifExpression() throws Exception {
    String query = "select\n" +
        "case when sales_state_province = 'CA' then 'a' when sales_state_province = 'DB' then 'aa' else 'aaa' end as col_123,\n" +
        "case when sales_state_province = 'CA' then 'aa' when sales_state_province = 'DB' then 'a' else 'aaa' end as col_213,\n" +
        "case when sales_state_province = 'CA' then 'a' when sales_state_province = 'DB' then 'aaa' else 'aa' end as col_132,\n" +
        "case when sales_state_province = 'CA' then 'aa' when sales_state_province = 'DB' then 'aaa' else 'a' end as col_231,\n" +
        "case when sales_state_province = 'CA' then 'aaa' when sales_state_province = 'DB' then 'aa' else 'a' end as col_321,\n" +
        "case when sales_state_province = 'CA' then 'aaa' when sales_state_province = 'DB' then 'a' else 'aa' end as col_312,\n" +
        "case when sales_state_province = 'CA' then sales_state_province when sales_state_province = 'DB' then 'a' else 'aa' end as col_unk1,\n" +
        "case when sales_state_province = 'CA' then 'aaa' when sales_state_province = 'DB' then sales_state_province else 'aa' end as col_unk2,\n" +
        "case when sales_state_province = 'CA' then 'aaa' when sales_state_province = 'DB' then 'a' else sales_state_province end as col_unk3\n" +
        "from cp.`region.json`";

    List<ExpectedColumnResult> expectedMetadata = ImmutableList.of(
        new ExpectedColumnResult("col_123", "CHARACTER VARYING", false, 3, 3, 0, false, String.class.getName()),
        new ExpectedColumnResult("col_213", "CHARACTER VARYING", false, 3, 3, 0, false, String.class.getName()),
        new ExpectedColumnResult("col_132", "CHARACTER VARYING", false, 3, 3, 0, false, String.class.getName()),
        new ExpectedColumnResult("col_231", "CHARACTER VARYING", false, 3, 3, 0, false, String.class.getName()),
        new ExpectedColumnResult("col_321", "CHARACTER VARYING", false, 3, 3, 0, false, String.class.getName()),
        new ExpectedColumnResult("col_312", "CHARACTER VARYING", false, 3, 3, 0, false, String.class.getName()),
        new ExpectedColumnResult("col_unk1", "CHARACTER VARYING", true, Types.MAX_VARCHAR_LENGTH, Types.MAX_VARCHAR_LENGTH, 0, false, String.class.getName()),
        new ExpectedColumnResult("col_unk2", "CHARACTER VARYING", true, Types.MAX_VARCHAR_LENGTH, Types.MAX_VARCHAR_LENGTH, 0, false, String.class.getName()),
        new ExpectedColumnResult("col_unk3", "CHARACTER VARYING", true, Types.MAX_VARCHAR_LENGTH, Types.MAX_VARCHAR_LENGTH, 0, false, String.class.getName())
    );

    verifyResults(query, expectedMetadata);
  }

  @Test
  public void coalesce() throws Exception {
    String query = "select\n" +
        "coalesce(cast(sales_city as varchar(10)), 'unknown') as col_first_cond,\n" +
        "coalesce(cast(sales_city as varchar(10)), cast('unknown' as varchar(20))) as col_second_cond,\n" +
        "coalesce(cast(null as varchar(10)), 'unknown') as col_null,\n" +
        "coalesce(sales_city, sales_country) as col_unk\n" +
        "from cp.`region.json`";

    List<ExpectedColumnResult> expectedMetadata = ImmutableList.of(
        new ExpectedColumnResult("col_first_cond", "CHARACTER VARYING", true, 10, 10, 0, false, String.class.getName()),
        new ExpectedColumnResult("col_second_cond", "CHARACTER VARYING", true, 20, 20, 0, false, String.class.getName()),
        new ExpectedColumnResult("col_null", "CHARACTER VARYING", true, 10, 10, 0, false, String.class.getName()),
        new ExpectedColumnResult("col_unk", "CHARACTER VARYING", true, Types.MAX_VARCHAR_LENGTH, Types.MAX_VARCHAR_LENGTH, 0, false, String.class.getName())
    );

    verifyResults(query, expectedMetadata);
  }

  @Test
  public void pad() throws Exception {
    String query = "SELECT\n" +
        "%1$s(cast(sales_city as varchar(10)), 10, 'A') as col_same_pad,\n" +
        "%1$s(cast(sales_city as varchar(10)), 0, 'A') as col_zero_pad,\n" +
        "%1$s(cast(sales_city as varchar(10)), -1, 'A') as col_negative_pad,\n" +
        "%1$s(cast(sales_city as varchar(10)), 9, 'A') as col_lower_pad,\n" +
        "%1$s(cast(sales_city as varchar(10)), 20, 'A') as col_greater_pad,\n" +
        "%1$s(sales_city, 10, 'A') as col_unk_source_length,\n" +
        "%1$s(cast(sales_city as varchar(10)), '10', 'A') as col_length_char\n" +
        "from cp.`region.json`";

    List<ExpectedColumnResult> expectedMetadata = ImmutableList.of(
        new ExpectedColumnResult("col_same_pad", "CHARACTER VARYING", true, 10, 10, 0, false, String.class.getName()),
        new ExpectedColumnResult("col_zero_pad", "CHARACTER VARYING", true, 0, 0, 0, false, String.class.getName()),
        new ExpectedColumnResult("col_negative_pad", "CHARACTER VARYING", true, 0, 0, 0, false, String.class.getName()),
        new ExpectedColumnResult("col_lower_pad", "CHARACTER VARYING", true, 9, 9, 0, false, String.class.getName()),
        new ExpectedColumnResult("col_greater_pad", "CHARACTER VARYING", true, 20, 20, 0, false, String.class.getName()),
        new ExpectedColumnResult("col_unk_source_length", "CHARACTER VARYING", true, 10, 10, 0, false, String.class.getName()),
        new ExpectedColumnResult("col_length_char", "CHARACTER VARYING", true, Types.MAX_VARCHAR_LENGTH, Types.MAX_VARCHAR_LENGTH, 0, false, String.class.getName())
    );

    List<String> padFunctions = Lists.newArrayList("rpad", "lpad");
    for (String function : padFunctions) {
      verifyResults(String.format(query, function), expectedMetadata);
    }
  }

  @Test
  public void concat() throws Exception {
    String query = "select\n" +
        "concat(cast(sales_city as varchar(10)), cast(sales_city as varchar(10))) as concat_two_casts,\n" +
        "concat(cast(sales_city as varchar(60000)), cast(sales_city as varchar(60000))) as concat_max_length,\n" +
        "concat(cast(sales_city as varchar(10)), sales_city) as concat_one_unknown,\n" +
        "concat(sales_city, sales_city) as concat_two_unknown,\n" +
        "concat(cast(sales_city as varchar(10)), 'a') as concat_one_constant,\n" +
        "concat('a', 'a') as concat_two_constants,\n" +
        "concat(cast(sales_city as varchar(10)), cast(null as varchar(10))) as concat_right_null,\n" +
        "concat(cast(null as varchar(10)), cast(sales_city as varchar(10))) as concat_left_null,\n" +
        "concat(cast(null as varchar(10)), cast(null as varchar(10))) as concat_both_null,\n" +
        "concat(cast(sales_district_id as integer), '_D') as concat_with_int,\n" +

        "cast(sales_city as varchar(10)) || cast(sales_city as varchar(10)) as concat_op_two_casts,\n" +
        "cast(sales_city as varchar(60000)) || cast(sales_city as varchar(60000)) as concat_op_max_length,\n" +
        "cast(sales_city as varchar(10)) || sales_city as concat_op_one_unknown,\n" +
        "sales_city || sales_city as concat_op_two_unknown,\n" +
        "cast(sales_city as varchar(10)) || 'a' as concat_op_one_constant,\n" +
        "'a' || 'a' as concat_op_two_constants,\n" +
        "cast(sales_city as varchar(10)) || cast(null as varchar(10)) as concat_op_right_null,\n" +
        "cast(null as varchar(10)) || cast(sales_city as varchar(10)) as concat_op_left_null,\n" +
        "cast(null as varchar(10)) || cast(null as varchar(10)) as concat_op_both_null,\n" +
        "cast(sales_district_id as integer) || '_D' as concat_op_with_int\n" +
        "from cp.`region.json`";

    List<ExpectedColumnResult> expectedMetadata = ImmutableList.of(
        new ExpectedColumnResult("concat_two_casts", "CHARACTER VARYING", false, 20, 20, 0, false, String.class.getName()),
        new ExpectedColumnResult("concat_max_length", "CHARACTER VARYING", false, Types.MAX_VARCHAR_LENGTH, Types.MAX_VARCHAR_LENGTH, 0, false, String.class.getName()),
        new ExpectedColumnResult("concat_one_unknown", "CHARACTER VARYING", false, Types.MAX_VARCHAR_LENGTH, Types.MAX_VARCHAR_LENGTH, 0, false, String.class.getName()),
        new ExpectedColumnResult("concat_two_unknown", "CHARACTER VARYING", false, Types.MAX_VARCHAR_LENGTH, Types.MAX_VARCHAR_LENGTH, 0, false, String.class.getName()),
        new ExpectedColumnResult("concat_one_constant", "CHARACTER VARYING", false, 11, 11, 0, false, String.class.getName()),
        new ExpectedColumnResult("concat_two_constants", "CHARACTER VARYING", false, 2, 2, 0, false, String.class.getName()),
        new ExpectedColumnResult("concat_right_null", "CHARACTER VARYING", false, 20, 20, 0, false, String.class.getName()),
        new ExpectedColumnResult("concat_left_null", "CHARACTER VARYING", false, 20, 20, 0, false, String.class.getName()),
        new ExpectedColumnResult("concat_both_null", "CHARACTER VARYING", false, 20, 20, 0, false, String.class.getName()),
        new ExpectedColumnResult("concat_with_int", "CHARACTER VARYING", false, Types.MAX_VARCHAR_LENGTH, Types.MAX_VARCHAR_LENGTH, 0, false, String.class.getName()),

        new ExpectedColumnResult("concat_op_two_casts", "CHARACTER VARYING", true, 20, 20, 0, false, String.class.getName()),
        new ExpectedColumnResult("concat_op_max_length", "CHARACTER VARYING", true, Types.MAX_VARCHAR_LENGTH, Types.MAX_VARCHAR_LENGTH, 0, false, String.class.getName()),
        new ExpectedColumnResult("concat_op_one_unknown", "CHARACTER VARYING", true, Types.MAX_VARCHAR_LENGTH, Types.MAX_VARCHAR_LENGTH, 0, false, String.class.getName()),
        new ExpectedColumnResult("concat_op_two_unknown", "CHARACTER VARYING", true, Types.MAX_VARCHAR_LENGTH, Types.MAX_VARCHAR_LENGTH, 0, false, String.class.getName()),
        new ExpectedColumnResult("concat_op_one_constant", "CHARACTER VARYING", true, 11, 11, 0, false, String.class.getName()),
        new ExpectedColumnResult("concat_op_two_constants", "CHARACTER VARYING", false, 2, 2, 0, false, String.class.getName()),
        new ExpectedColumnResult("concat_op_right_null", "CHARACTER VARYING", true, 20, 20, 0, false, String.class.getName()),
        new ExpectedColumnResult("concat_op_left_null", "CHARACTER VARYING", true, 20, 20, 0, false, String.class.getName()),
        new ExpectedColumnResult("concat_op_both_null", "CHARACTER VARYING", true, 20, 20, 0, false, String.class.getName()),
        new ExpectedColumnResult("concat_op_with_int", "CHARACTER VARYING", true, Types.MAX_VARCHAR_LENGTH, Types.MAX_VARCHAR_LENGTH, 0, false, String.class.getName())
        );

    verifyResults(query, expectedMetadata);
  }

  @Test
  public void unionWithConstants() throws Exception {
    String query = "select * from (\n" +
        "select cast('AAA' as varchar(3)) as col_const from (values(1))\n" +
        "union all\n" +
        "select cast('AAA' as varchar(5)) as col_const from (values(1))\n" +
        ")";

    List<ExpectedColumnResult> expectedMetadata = ImmutableList.of(
        new ExpectedColumnResult("col_const", "CHARACTER VARYING", false, 5, 5, 0, false, String.class.getName())
    );

    verifyResults(query, expectedMetadata);
  }

  @Test
  public void unionWithOptionalRequired() throws Exception {
    String query = "select * from (\n" +
        "select cast('AAA' as varchar(10)) as col_const from (values(1))\n" +
        "union all\n" +
        "select cast(sales_city as varchar(10)) as col_const from cp.`region.json`\n" +
        ")";

    List<ExpectedColumnResult> expectedMetadata = ImmutableList.of(
        new ExpectedColumnResult("col_const", "CHARACTER VARYING", true, 10, 10, 0, false, String.class.getName())
    );

    verifyResults(query, expectedMetadata);
  }

  private void verifyResults(String query, List<ExpectedColumnResult> expectedMetadata) throws Exception {
    // regular query
    verifyMetadata(expectedMetadata, createPrepareStmt(query, false, null).getColumnsList());

    // limit 0 query
    try {
      test("alter session set `%s` = true", ExecConstants.EARLY_LIMIT0_OPT_KEY);
      verifyMetadata(expectedMetadata, createPrepareStmt(String.format("select * from (%s) t limit 0", query), false, null)
          .getColumnsList());
    } finally {
      resetSessionOption(ExecConstants.EARLY_LIMIT0_OPT_KEY);
    }
  }


}
