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
package org.apache.drill.exec.fn.impl;

import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.TestBuilder;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.math.BigDecimal;

import static org.apache.drill.test.TestBuilder.listOf;
import static org.hamcrest.CoreMatchers.containsString;

public class TestVarArgFunctions extends ClusterTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @BeforeClass
  public static void setUp() throws Exception {
    startCluster(ClusterFixture.builder(dirTestWatcher));
  }

  @Test
  public void testVarargUdfWithValueHolder() throws Exception {
    testBuilder()
        .sqlQuery("SELECT concat_varchar('a', 'b', 'c', 'd') as c, concat_varchar(first_name, case when true then ' ' else null end, last_name) as c1 " +
            "from cp.`employee.json` limit 10")
        .unOrdered()
        .baselineColumns("c", "c1")
        .baselineValues("abcd", "Sheri Nowmer")
        .baselineValues("abcd", "Derrick Whelply")
        .baselineValues("abcd", "Michael Spence")
        .baselineValues("abcd", "Maya Gutierrez")
        .baselineValues("abcd", "Roberta Damstra")
        .baselineValues("abcd", "Rebecca Kanagaki")
        .baselineValues("abcd", "Kim Brunner")
        .baselineValues("abcd", "Brenda Blumberg")
        .baselineValues("abcd", "Darren Stanz")
        .baselineValues("abcd", "Jonathan Murraiin")
        .go();
  }

  @Test
  public void testVarargUdfWithSeveralArgs() throws Exception {
    testBuilder()
        .sqlQuery("SELECT concat_delim('o', 'Hell', 'W', 'rld!') as c," +
            "concat_delim(' ', first_name, last_name) as c1 " +
            "from cp.`employee.json` limit 10")
        .unOrdered()
        .baselineColumns("c", "c1")
        .baselineValues("HelloWorld!", "Sheri Nowmer")
        .baselineValues("HelloWorld!", "Derrick Whelply")
        .baselineValues("HelloWorld!", "Michael Spence")
        .baselineValues("HelloWorld!", "Maya Gutierrez")
        .baselineValues("HelloWorld!", "Roberta Damstra")
        .baselineValues("HelloWorld!", "Rebecca Kanagaki")
        .baselineValues("HelloWorld!", "Kim Brunner")
        .baselineValues("HelloWorld!", "Brenda Blumberg")
        .baselineValues("HelloWorld!", "Darren Stanz")
        .baselineValues("HelloWorld!", "Jonathan Murraiin")
        .go();
  }

  @Test
  public void testConcatWsWithNoArgs() throws Exception {
    testBuilder()
        .sqlQuery("SELECT concat_delim('delim') as c," +
            "concat_delim(2, 'one ', ' three ', ' one') as c1," +
            "concat_delim(cast(null as varchar), 'one ', ' three ', ' one') as c2," +
            "concat_delim('2', cast(null as varchar)) as c3")
        .unOrdered()
        .baselineColumns("c", "c1", "c2", "c3")
        .baselineValues("", "one 2 three 2 one", null, "")
        .go();
  }

  @Test
  public void testVarargUdfWithDifferentDataModes() throws Exception {
    testBuilder()
        .sqlQuery("SELECT concat_varchar(first_name, ' ', last_name) as c1\n" +
          "from cp.`employee.json` limit 10")
        .unOrdered()
        .baselineColumns("c1")
        .baselineValues("Sheri Nowmer")
        .baselineValues("Derrick Whelply")
        .baselineValues("Michael Spence")
        .baselineValues("Maya Gutierrez")
        .baselineValues("Roberta Damstra")
        .baselineValues("Rebecca Kanagaki")
        .baselineValues("Kim Brunner")
        .baselineValues("Brenda Blumberg")
        .baselineValues("Darren Stanz")
        .baselineValues("Jonathan Murraiin")
        .go();
  }

  @Test
  public void testRepeatedVarargUdf() throws Exception {
    testBuilder()
        .sqlQuery("SELECT `test_count`(str_list, str_list) as c, `test_count`() as c1 " +
            "from cp.`store/json/json_basic_repeated_varchar.json` t")
        .unOrdered()
        .baselineColumns("c", "c1")
        .baselineValues(2L, 0L)
        .baselineValues(2L, 0L)
        .baselineValues(2L, 0L)
        .baselineValues(2L, 0L)
        .go();
  }

  @Test
  public void testRepeatedMapVarargUdf() throws Exception {
    testBuilder()
        .sqlQuery("SELECT `test_count`(t.map.rm[0].rptd, t.map.rm[0].rptd, t.map.rm[0].rptd) as c, `test_count`() as c1 " +
            "from cp.`store/json/nested_repeated_map.json` t")
        .unOrdered()
        .baselineColumns("c", "c1")
        .baselineValues(3L, 0L)
        .go();
  }

  @Test
  public void testListVarargUdf() throws Exception {
    testBuilder()
        .sqlQuery("SELECT `test_count`(t.rl, t.rl) as c, `test_count`() as c1 " +
            "from cp.`store/json/input2.json` t")
        .unOrdered()
        .baselineColumns("c", "c1")
        .baselineValues(2L, 0L)
        .baselineValues(2L, 0L)
        .baselineValues(2L, 0L)
        .baselineValues(2L, 0L)
        .go();
  }

  @Test
  public void testListVarargUdfFilter() throws Exception {
    testBuilder()
        .sqlQuery("SELECT `test_count`() as c1 " +
            "from cp.`store/json/input2.json` t where `test_count`(t.rl, t.rl) = 2")
        .unOrdered()
        .baselineColumns("c1")
        .baselineValues(0L)
        .baselineValues(0L)
        .baselineValues(0L)
        .baselineValues(0L)
        .go();
  }

  @Test
  public void testCollectToListFunctionWithLiterals() throws Exception {
    testBuilder()
        .sqlQuery("SELECT collect_to_list(1, 2, 3) as ints, collect_to_list('a', 'b', 'c', 'd') as varchars")
        .unOrdered()
        .baselineColumns("ints", "varchars")
        .baselineValues(listOf(1, 2, 3), listOf("a", "b", "c", "d"))
        .go();
  }

  @Test
  public void testCollectToListFunction() throws Exception {
    testBuilder()
        .sqlQuery("SELECT collect_to_list(employee_id, position_id, department_id) as ids," +
            "collect_to_list(first_name, last_name) as names," +
            "collect_to_list(cast(employee_id as decimal(28,0)), cast(position_id as decimal(28,0))) as decimals " +
            "from cp.`employee.json` limit 10")
        .unOrdered()
        .baselineColumns("ids", "names", "decimals")
        .baselineValues(listOf(1L, 1L, 1L), listOf("Sheri", "Nowmer"), listOf(BigDecimal.valueOf(1), BigDecimal.valueOf(1)))
        .baselineValues(listOf(2L, 2L, 1L), listOf("Derrick", "Whelply"), listOf(BigDecimal.valueOf(2), BigDecimal.valueOf(1)))
        .baselineValues(listOf(4L, 2L, 1L), listOf("Michael", "Spence"), listOf(BigDecimal.valueOf(4), BigDecimal.valueOf(2)))
        .baselineValues(listOf(5L, 2L, 1L), listOf("Maya", "Gutierrez"), listOf(BigDecimal.valueOf(5), BigDecimal.valueOf(2)))
        .baselineValues(listOf(6L, 3L, 2L), listOf("Roberta", "Damstra"), listOf(BigDecimal.valueOf(6), BigDecimal.valueOf(3)))
        .baselineValues(listOf(7L, 4L, 3L), listOf("Rebecca", "Kanagaki"), listOf(BigDecimal.valueOf(7), BigDecimal.valueOf(4)))
        .baselineValues(listOf(8L, 11L, 11L), listOf("Kim", "Brunner"), listOf(BigDecimal.valueOf(8), BigDecimal.valueOf(11)))
        .baselineValues(listOf(9L, 11L, 11L), listOf("Brenda", "Blumberg"), listOf(BigDecimal.valueOf(9), BigDecimal.valueOf(11)))
        .baselineValues(listOf(10L, 5L, 5L), listOf("Darren", "Stanz"), listOf(BigDecimal.valueOf(10), BigDecimal.valueOf(5)))
        .baselineValues(listOf(11L, 11L, 11L), listOf("Jonathan", "Murraiin"), listOf(BigDecimal.valueOf(11), BigDecimal.valueOf(11)))
        .go();
  }

  @Test
  public void testCollectToListFunctionWithNoArgs() throws Exception {
    testBuilder()
        .sqlQuery("SELECT collect_to_list() as emptyList " +
            "from cp.`employee.json` limit 5")
        .unOrdered()
        .baselineColumns("emptyList")
        .baselineValues(listOf())
        .baselineValues(listOf())
        .baselineValues(listOf())
        .baselineValues(listOf())
        .baselineValues(listOf())
        .go();
  }

  @Test
  public void testAggregateFunctions() throws Exception {
    testBuilder()
        .sqlQuery("SELECT test_count_agg(employee_id, position_id, department_id) as ids " +
            "from cp.`employee.json`")
        .unOrdered()
        .baselineColumns("ids")
        .baselineValues(3465L)
        .go();
  }

  @Test
  public void testAggregateFunctionWithGroupBy() throws Exception {
    testBuilder()
        .sqlQuery("SELECT test_count_agg(employee_id, position_id, department_id) as ids " +
            "from cp.`employee.json` group by department_id")
        .unOrdered()
        .baselineColumns("ids")
        .baselineValues(21L)
        .baselineValues(15L)
        .baselineValues(12L)
        .baselineValues(300L)
        .baselineValues(27L)
        .baselineValues(6L)
        .baselineValues(96L)
        .baselineValues(48L)
        .baselineValues(678L)
        .baselineValues(804L)
        .baselineValues(666L)
        .baselineValues(792L)
        .go();
  }

  @Test
  public void testAggregateFunctionWithGroupByWithLiterals() throws Exception {
    testBuilder()
        .sqlQuery("SELECT test_count_agg(1, 2, 3) as ids " +
            "from cp.`employee.json` group by department_id")
        .unOrdered()
        .baselineColumns("ids")
        .baselineValues(21L)
        .baselineValues(15L)
        .baselineValues(12L)
        .baselineValues(300L)
        .baselineValues(27L)
        .baselineValues(6L)
        .baselineValues(96L)
        .baselineValues(48L)
        .baselineValues(678L)
        .baselineValues(804L)
        .baselineValues(666L)
        .baselineValues(792L)
        .go();
  }

  @Test
  public void testEmptyAggregateFunctions() throws Exception {
    testBuilder()
        .sqlQuery("SELECT test_count_agg() as ids " +
            "from cp.`employee.json` group by department_id")
        .unOrdered()
        .baselineColumns("ids")
        .baselineValues(0L)
        .baselineValues(0L)
        .baselineValues(0L)
        .baselineValues(0L)
        .baselineValues(0L)
        .baselineValues(0L)
        .baselineValues(0L)
        .baselineValues(0L)
        .baselineValues(0L)
        .baselineValues(0L)
        .baselineValues(0L)
        .baselineValues(0L)
        .go();
  }

  @Test
  public void testRegularFunctionPriority() throws Exception {
    // checks that non-vararg function is used instead of vararg one
    testBuilder()
        .sqlQuery("SELECT add(1, 2) as ids")
        .unOrdered()
        .baselineColumns("ids")
        .baselineValues(3)
        .go();

    // checks that vararg function is present
    thrown.expect(DrillRuntimeException.class);
    thrown.expectMessage(containsString("This function should not be used!"));
    run("SELECT add(1, 2, 3) as ids");
  }

  @Test
  public void testFunctionWithNonLastVarargValidation() throws Exception {
    // checks that function wasn't registered, so it is cannot be found
    thrown.expect(UserRemoteException.class);
    thrown.expectMessage(containsString("No match found for function signature non_last_vararg"));
    queryBuilder().sql("SELECT non_last_vararg(1, 2, '3') as ids").run();
  }

  @Test
  public void testFunctionWithSeveralVarargsValidation() throws Exception {
    // checks that function wasn't registered, so it is cannot be found
    thrown.expect(UserRemoteException.class);
    thrown.expectMessage(containsString("No match found for function signature several_varargs"));
    queryBuilder().sql("SELECT several_varargs('0', 1, 2) as ids").run();
  }

  @Test
  public void testVarArgFunctionInSysTable() throws Exception {
    testBuilder()
        .sqlQuery("select * from sys.functions where name = 'collect_to_list'")
        .unOrdered()
        .baselineColumns("name", "signature", "returnType", "source", "internal")
        .baselineValues("collect_to_list", "LATE-REQUIRED...", "LATE", "built-in", false)
        .go();
  }

  @Test
  public void testSimpleRowFunction() throws Exception {
    testBuilder()
        .sqlQuery("SELECT ROW(1,2) as abc")
        .unOrdered()
        .baselineColumns("abc")
        .baselineValues(TestBuilder.mapOf("EXPR$0", 1, "EXPR$1", 2))
        .go();
  }

}
