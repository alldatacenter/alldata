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
package org.apache.drill.exec.store.cassandra;

import org.apache.drill.common.exceptions.UserRemoteException;
import org.joda.time.Period;
import org.junit.Test;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class CassandraQueryTest extends BaseCassandraTest {

  @Test
  public void testSelectAll() throws Exception {
    testBuilder()
        .sqlQuery("select * from cassandra.test_keyspace.`employee` order by employee_id")
        .ordered()
        .baselineColumns("employee_id", "full_name", "first_name", "last_name", "position_id",
            "position_title", "store_id", "department_id", "birth_date", "hire_date", "salary",
            "supervisor_id", "education_level", "marital_status", "gender", "management_role",
            "ascii_field", "blob_field", "boolean_field", "date_field", "decimal_field", "double_field",
            "duration_field", "inet_field", "time_field", "timestamp_field", "timeuuid_field",
            "uuid_field", "varchar_field", "varint_field")
        .baselineValues(1L, "Sheri Nowmer", "Sheri", "Nowmer", 1, "President", 0, 1, "1961-08-26", "1994-12-01 00:00:00.0", 80000.0f, 0, "Graduate Degree", "S", "F", "Senior Management", "abc", "0000000000000003", true, 15008L, BigDecimal.valueOf(123), 321.123, new Period(0, 0, 0, 3, 0, 0, 0, 320688000), InetAddress.getByName("8.8.8.8").getAddress(), 14700000000000L, 1296705900000L, getUuidBytes("50554d6e-29bb-11e5-b345-feff819cdc9f"), getUuidBytes("50554d6e-29bb-11e5-b345-feff819cdc9f"), "abc", 123L)
        .baselineValues(2L, "Derrick Whelply", "Derrick", "Whelply", 2, "VP Country Manager", 0, 1, "1915-07-03", "1994-12-01 00:00:00.0", 40000.0f, 1, "Graduate Degree", "M", "M", "Senior Management", null, null, null, null, null, null, null, null, null, null, null, null, null, null)
        .baselineValues(4L, "Michael Spence", "Michael", "Spence", 2, "VP Country Manager", 0, 1, "1969-06-20", "1998-01-01 00:00:00.0", 40000.0f, 1, "Graduate Degree", "S", "M", "Senior Management", null, null, null, null, null, null, null, null, null, null, null, null, null, null)
        .baselineValues(5L, "Maya Gutierrez", "Maya", "Gutierrez", 2, "VP Country Manager", 0, 1, "1951-05-10", "1998-01-01 00:00:00.0", 35000.0f, 1, "Bachelors Degree", "M", "F", "Senior Management", null, null, null, null, null, null, null, null, null, null, null, null, null, null)
        .baselineValues(6L, "Roberta Damstra", "Roberta", "Damstra", 3, "VP Information Systems", 0, 2, "1942-10-08", "1994-12-01 00:00:00.0", 25000.0f, 1, "Bachelors Degree", "M", "F", "Senior Management", null, null, null, null, null, null, null, null, null, null, null, null, null, null)
        .baselineValues(7L, "Rebecca Kanagaki", "Rebecca", "Kanagaki", 4, "VP Human Resources", 0, 3, "1949-03-27", "1994-12-01 00:00:00.0", 15000.0f, 1, "Bachelors Degree", "M", "F", "Senior Management", null, null, null, null, null, null, null, null, null, null, null, null, null, null)
        .baselineValues(8L, "Kim Brunner", "Kim", "Brunner", 11, "Store Manager", 9, 11, "1922-08-10", "1998-01-01 00:00:00.0", 10000.0f, 5, "Bachelors Degree", "S", "F", "Store Management", null, null, null, null, null, null, null, null, null, null, null, null, null, null)
        .baselineValues(9L, "Brenda Blumberg", "Brenda", "Blumberg", 11, "Store Manager", 21, 11, "1979-06-23", "1998-01-01 00:00:00.0", 17000.0f, 5, "Graduate Degree", "M", "F", "Store Management", null, null, null, null, null, null, null, null, null, null, null, null, null, null)
        .baselineValues(10L, "Darren Stanz", "Darren", "Stanz", 5, "VP Finance", 0, 5, "1949-08-26", "1994-12-01 00:00:00.0", 50000.0f, 1, "Partial College", "M", "M", "Senior Management", null, null, null, null, null, null, null, null, null, null, null, null, null, null)
        .baselineValues(11L, "Jonathan Murraiin", "Jonathan", "Murraiin", 11, "Store Manager", 1, 11, "1967-06-20", "1998-01-01 00:00:00.0", 15000.0f, 5, "Graduate Degree", "S", "M", "Store Management", null, null, null, null, null, null, null, null, null, null, null, null, null, null)
        .go();
  }

  @Test
  public void testSelectColumns() throws Exception {
    testBuilder()
        .sqlQuery("select full_name, birth_date from cassandra.test_keyspace.`employee`")
        .unOrdered()
        .baselineColumns("full_name", "birth_date")
        .baselineValues("Sheri Nowmer", "1961-08-26")
        .baselineValues("Derrick Whelply", "1915-07-03")
        .baselineValues("Michael Spence", "1969-06-20")
        .baselineValues("Maya Gutierrez", "1951-05-10")
        .baselineValues("Roberta Damstra", "1942-10-08")
        .baselineValues("Rebecca Kanagaki", "1949-03-27")
        .baselineValues("Kim Brunner", "1922-08-10")
        .baselineValues("Brenda Blumberg", "1979-06-23")
        .baselineValues("Darren Stanz", "1949-08-26")
        .baselineValues("Jonathan Murraiin", "1967-06-20")
        .go();
  }

  @Test
  public void testSelectAllFiltered() throws Exception {
    testBuilder()
        .sqlQuery("select * from cassandra.test_keyspace.`employee` where employee_id = 1")
        .unOrdered()
        .baselineColumns("employee_id", "full_name", "first_name", "last_name", "position_id",
            "position_title", "store_id", "department_id", "birth_date", "hire_date", "salary",
            "supervisor_id", "education_level", "marital_status", "gender", "management_role",
            "ascii_field", "blob_field", "boolean_field", "date_field", "decimal_field", "double_field",
            "duration_field", "inet_field", "time_field", "timestamp_field", "timeuuid_field",
            "uuid_field", "varchar_field", "varint_field")
        .baselineValues(1L, "Sheri Nowmer", "Sheri", "Nowmer", 1, "President", 0, 1, "1961-08-26",
            "1994-12-01 00:00:00.0", 80000.0f, 0, "Graduate Degree", "S", "F", "Senior Management",
            "abc", "0000000000000003", true, 15008L, BigDecimal.valueOf(123), 321.123,
            new Period(0, 0, 0, 3, 0, 0, 0, 320688000),
            InetAddress.getByName("8.8.8.8").getAddress(), 14700000000000L, 1296705900000L,
          getUuidBytes("50554d6e-29bb-11e5-b345-feff819cdc9f"),
          getUuidBytes("50554d6e-29bb-11e5-b345-feff819cdc9f"),
            "abc", 123L)
        .go();
  }

  @Test
  public void testSelectColumnsFiltered() throws Exception {
    testBuilder()
        .sqlQuery("select full_name, birth_date from cassandra.test_keyspace.`employee` where employee_id = 1 and first_name = 'Sheri'")
        .unOrdered()
        .baselineColumns("full_name", "birth_date")
        .baselineValues("Sheri Nowmer", "1961-08-26")
        .go();
  }

  @Test
  public void testSelectColumnsUnsupportedFilter() throws Exception {
    // Calcite doesn't support LIKE and other functions yet, so ensure Drill filter is used there
    testBuilder()
        .sqlQuery("select full_name, birth_date from cassandra.test_keyspace.`employee` where first_name like 'Sh%'")
        .unOrdered()
        .baselineColumns("full_name", "birth_date")
        .baselineValues("Sheri Nowmer", "1961-08-26")
        .go();
  }

  @Test
  public void testSelectColumnsUnsupportedProject() throws Exception {
    // Calcite doesn't support LIKE and other functions yet, so ensure Drill project is used there
    testBuilder()
        .sqlQuery("select first_name like 'Sh%' as c, birth_date from cassandra.test_keyspace.`employee` where first_name like 'Sh%'")
        .unOrdered()
        .baselineColumns("c", "birth_date")
        .baselineValues(true, "1961-08-26")
        .go();
  }

  @Test
  public void testSelectColumnsUnsupportedAggregate() throws Exception {
    // Calcite doesn't support stddev_samp and other functions yet, so ensure Drill agg is used there
    testBuilder()
        .sqlQuery("select stddev_samp(salary) as standard_deviation from cassandra.test_keyspace.`employee`")
        .unOrdered()
        .baselineColumns("standard_deviation")
        .baselineValues(21333.593748410563)
        .go();
  }

  @Test
  public void testLimitWithSort() throws Exception {
    testBuilder()
        .sqlQuery("select full_name, birth_date from cassandra.test_keyspace.`employee` order by employee_id limit 3")
        .ordered()
        .baselineColumns("full_name", "birth_date")
        .baselineValues("Sheri Nowmer", "1961-08-26")
        .baselineValues("Derrick Whelply", "1915-07-03")
        .baselineValues("Michael Spence", "1969-06-20")
        .go();
  }

  @Test
  public void testSelectAllWithLimitAndSort() throws Exception {
    testBuilder()
        .sqlQuery("select * from cassandra.test_keyspace.`employee` order by employee_id limit 3")
        .ordered()
        .baselineColumns("employee_id", "full_name", "first_name", "last_name", "position_id",
            "position_title", "store_id", "department_id", "birth_date", "hire_date", "salary",
            "supervisor_id", "education_level", "marital_status", "gender", "management_role",
            "ascii_field", "blob_field", "boolean_field", "date_field", "decimal_field", "double_field",
            "duration_field", "inet_field", "time_field", "timestamp_field", "timeuuid_field",
            "uuid_field", "varchar_field", "varint_field")
        .baselineValues(1L, "Sheri Nowmer", "Sheri", "Nowmer", 1, "President", 0, 1, "1961-08-26", "1994-12-01 00:00:00.0", 80000.0f, 0, "Graduate Degree", "S", "F", "Senior Management", "abc", "0000000000000003", true, 15008L, BigDecimal.valueOf(123), 321.123, new Period(0, 0, 0, 3, 0, 0, 0, 320688000), InetAddress.getByName("8.8.8.8").getAddress(), 14700000000000L, 1296705900000L, getUuidBytes("50554d6e-29bb-11e5-b345-feff819cdc9f"), getUuidBytes("50554d6e-29bb-11e5-b345-feff819cdc9f"), "abc", 123L)
        .baselineValues(2L, "Derrick Whelply", "Derrick", "Whelply", 2, "VP Country Manager", 0, 1, "1915-07-03", "1994-12-01 00:00:00.0", 40000.0f, 1, "Graduate Degree", "M", "M", "Senior Management", null, null, null, null, null, null, null, null, null, null, null, null, null, null)
        .baselineValues(4L, "Michael Spence", "Michael", "Spence", 2, "VP Country Manager", 0, 1, "1969-06-20", "1998-01-01 00:00:00.0", 40000.0f, 1, "Graduate Degree", "S", "M", "Senior Management", null, null, null, null, null, null, null, null, null, null, null, null, null, null)
        .go();
  }

  @Test
  public void testSingleColumn() throws Exception {
    testBuilder()
        .sqlQuery("select full_name from cassandra.test_keyspace.`employee`")
        .unOrdered()
        .baselineColumns("full_name")
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
  public void testJoin() throws Exception {
    testBuilder()
        .sqlQuery("select t1.full_name, t2.birth_date from cassandra.test_keyspace.`employee` t1 join cassandra.test_keyspace.`employee` t2 on t1.employee_id = t2.employee_id")
        .unOrdered()
        .baselineColumns("full_name", "birth_date")
        .baselineValues("Sheri Nowmer", "1961-08-26")
        .baselineValues("Derrick Whelply", "1915-07-03")
        .baselineValues("Michael Spence", "1969-06-20")
        .baselineValues("Maya Gutierrez", "1951-05-10")
        .baselineValues("Roberta Damstra", "1942-10-08")
        .baselineValues("Rebecca Kanagaki", "1949-03-27")
        .baselineValues("Kim Brunner", "1922-08-10")
        .baselineValues("Brenda Blumberg", "1979-06-23")
        .baselineValues("Darren Stanz", "1949-08-26")
        .baselineValues("Jonathan Murraiin", "1967-06-20")
        .go();
  }

  @Test
  public void testJoinWithFileTable() throws Exception {
    testBuilder()
        .sqlQuery("select t1.full_name, t2.birth_date from cassandra.test_keyspace.`employee` t1 join cp.`employee.json` t2 on t1.employee_id = t2.employee_id")
        .unOrdered()
        .baselineColumns("full_name", "birth_date")
        .baselineValues("Sheri Nowmer", "1961-08-26")
        .baselineValues("Derrick Whelply", "1915-07-03")
        .baselineValues("Michael Spence", "1969-06-20")
        .baselineValues("Maya Gutierrez", "1951-05-10")
        .baselineValues("Roberta Damstra", "1942-10-08")
        .baselineValues("Rebecca Kanagaki", "1949-03-27")
        .baselineValues("Kim Brunner", "1922-08-10")
        .baselineValues("Brenda Blumberg", "1979-06-23")
        .baselineValues("Darren Stanz", "1949-08-26")
        .baselineValues("Jonathan Murraiin", "1967-06-20")
        .go();
  }

  @Test
  public void testAggregate() throws Exception {
    testBuilder()
        .sqlQuery("select count(*) c from cassandra.test_keyspace.`employee`")
        .ordered()
        .baselineColumns("c")
        .baselineValues(10L)
        .go();
  }

  @Test
  public void testAggregateWithGroupBy() throws Exception {
    testBuilder()
        .sqlQuery("select sum(`salary`) sal, department_id from cassandra.test_keyspace.`employee` e group by e.`department_id`")
        .unOrdered()
        .baselineColumns("sal", "department_id")
        .baselineValues(195000.0, 1)
        .baselineValues(42000.0, 11)
        .baselineValues(25000.0, 2)
        .baselineValues(15000.0, 3)
        .baselineValues(50000.0, 5)
        .go();
  }

   @Test
    public void testSelectNonExistingColumn() throws Exception {
      try {
        queryBuilder().sql("select full_name123 from cassandra.test_keyspace.`employee` order by employee_id limit 3").run();
        fail("Query didn't fail");
      } catch (UserRemoteException e) {
        assertThat(e.getMessage(), containsString("VALIDATION ERROR"));
        assertThat(e.getMessage(), containsString("Column 'full_name123' not found in any table"));
      }
    }

  @Test
  public void testSelectLiterals() throws Exception {
    testBuilder()
        .sqlQuery("select 'abc' as full_name, 123 as id from cassandra.test_keyspace.`employee` limit 3")
        .unOrdered()
        .baselineColumns("full_name", "id")
        .baselineValues("abc", 123)
        .baselineValues("abc", 123)
        .baselineValues("abc", 123)
        .go();
  }

  @Test
  public void testSelectIntLiterals() throws Exception {
    testBuilder()
        .sqlQuery("select 333 as full_name, 123 as id from cassandra.test_keyspace.`employee` limit 3")
        .unOrdered()
        .baselineColumns("full_name", "id")
        .baselineValues(333, 123)
        .baselineValues(333, 123)
        .baselineValues(333, 123)
        .go();
  }

  @Test
  public void testSelectLiteralWithStar() throws Exception {
    testBuilder()
        .sqlQuery("select *, 123 as full_name from cassandra.test_keyspace.`employee` order by employee_id limit 3")
        .unOrdered()
        .baselineColumns("full_name")
        .baselineColumns("employee_id", "full_name", "first_name", "last_name", "position_id",
            "position_title", "store_id", "department_id", "birth_date", "hire_date", "salary",
            "supervisor_id", "education_level", "marital_status", "gender", "management_role",
            "ascii_field", "blob_field", "boolean_field", "date_field", "decimal_field", "double_field",
            "duration_field", "inet_field", "time_field", "timestamp_field", "timeuuid_field",
            "uuid_field", "varchar_field", "varint_field", "full_name0")
        .baselineValues(1L, "Sheri Nowmer", "Sheri", "Nowmer", 1, "President", 0, 1, "1961-08-26", "1994-12-01 00:00:00.0", 80000.0f, 0, "Graduate Degree", "S", "F", "Senior Management", "abc", "0000000000000003", true, 15008L, BigDecimal.valueOf(123), 321.123, new Period(0, 0, 0, 3, 0, 0, 0, 320688000), InetAddress.getByName("8.8.8.8").getAddress(), 14700000000000L, 1296705900000L, getUuidBytes("50554d6e-29bb-11e5-b345-feff819cdc9f"), getUuidBytes("50554d6e-29bb-11e5-b345-feff819cdc9f"), "abc", 123L, 123)
        .baselineValues(2L, "Derrick Whelply", "Derrick", "Whelply", 2, "VP Country Manager", 0, 1, "1915-07-03", "1994-12-01 00:00:00.0", 40000.0f, 1, "Graduate Degree", "M", "M", "Senior Management", null, null, null, null, null, null, null, null, null, null, null, null, null, null, 123)
        .baselineValues(4L, "Michael Spence", "Michael", "Spence", 2, "VP Country Manager", 0, 1, "1969-06-20", "1998-01-01 00:00:00.0", 40000.0f, 1, "Graduate Degree", "S", "M", "Senior Management", null, null, null, null, null, null, null, null, null, null, null, null, null, null, 123)
        .go();
  }

  @Test
  public void testLiteralWithAggregateAndGroupBy() throws Exception {
    testBuilder()
        .sqlQuery("select sum(`salary`) sal, 1 as department_id from cassandra.test_keyspace.`employee` e group by e.`department_id`")
        .unOrdered()
        .baselineColumns("sal", "department_id")
        .baselineValues(195000.0, 1)
        .baselineValues(42000.0, 1)
        .baselineValues(25000.0, 1)
        .baselineValues(15000.0, 1)
        .baselineValues(50000.0, 1)
        .go();
  }

  @Test
  public void testSelectNonExistingTable() throws Exception {
    try {
      queryBuilder().sql("select full_name from cassandra.test_keyspace.`non-existing`").run();
      fail("Query didn't fail");
    } catch (UserRemoteException e) {
      assertThat(e.getMessage(), containsString("VALIDATION ERROR"));
      assertThat(e.getMessage(), containsString("Object 'non-existing' not found within 'cassandra.test_keyspace'"));
    }
  }

  @Test
  public void testSelectNonExistingSubSchema() throws Exception {
    try {
      queryBuilder().sql("select full_name from cassandra.test_keyspace.`non-existing`.employee").run();
      fail("Query didn't fail");
    } catch (UserRemoteException e) {
      assertThat(e.getMessage(), containsString("VALIDATION ERROR"));
      assertThat(e.getMessage(), containsString("Schema [[cassandra, test_keyspace, non-existing]] is not valid with respect to either root schema or current default schema"));
    }
  }

  @Test
  public void testWithProvidedSchema() throws Exception {
    testBuilder()
        .sqlQuery("select * from " +
                "table(cassandra.test_keyspace.`employee`(schema=>'inline=(birth_date date not null, salary decimal(10, 2))')) " +
                "where first_name = 'Sheri'")
        .ordered()
        .baselineColumns("employee_id", "full_name", "first_name", "last_name", "position_id",
            "position_title", "store_id", "department_id", "birth_date", "hire_date", "salary",
            "supervisor_id", "education_level", "marital_status", "gender", "management_role",
            "ascii_field", "blob_field", "boolean_field", "date_field", "decimal_field", "double_field",
            "duration_field", "inet_field", "time_field", "timestamp_field", "timeuuid_field",
            "uuid_field", "varchar_field", "varint_field")
        .baselineValues(1L, "Sheri Nowmer", "Sheri", "Nowmer", 1, "President", 0, 1, LocalDate.parse("1961-08-26"),
            "1994-12-01 00:00:00.0", new BigDecimal("80000.00"), 0, "Graduate Degree", "S", "F", "Senior Management",
            "abc", "0000000000000003", true, 15008L, BigDecimal.valueOf(123), 321.123,
            new Period(0, 0, 0, 3, 0, 0, 0, 320688000),
            InetAddress.getByName("8.8.8.8").getAddress(), 14700000000000L, 1296705900000L,
          getUuidBytes("50554d6e-29bb-11e5-b345-feff819cdc9f"),
          getUuidBytes("50554d6e-29bb-11e5-b345-feff819cdc9f"),
            "abc", 123L)
        .go();
  }

  private static byte[] getUuidBytes(String name) {
    UUID uuid = UUID.fromString(name);
    return ByteBuffer.wrap(new byte[16])
      .order(ByteOrder.BIG_ENDIAN)
      .putLong(uuid.getMostSignificantBits())
      .putLong(uuid.getLeastSignificantBits())
      .array();
  }
}
