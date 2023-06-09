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

import org.junit.Test;

import static org.apache.drill.test.TestBuilder.listOf;
import static org.apache.drill.test.TestBuilder.mapOf;

public class CassandraComplexTypesTest extends BaseCassandraTest {

  @Test
  public void testSelectStarWithArray() throws Exception {
    testBuilder()
        .sqlQuery("select * from cassandra.test_keyspace.arr")
        .unOrdered()
        .baselineColumns("f_int", "string_arr", "int_arr", "int_set")
        .baselineValues(0, listOf("a", "b", "c", "d"), listOf(1, 2, 3, 4, 0),
            listOf(9, 8, 7, 6, 5))
        .go();
  }

  @Test
  public void testSelectArrayElem() throws Exception {
    testBuilder()
        .sqlQuery("select string_arr[0] c1, int_arr[1] c2 from cassandra.test_keyspace.arr")
        .unOrdered()
        .baselineColumns("c1", "c2")
        .baselineValues("a", 2)
        .go();
  }

  @Test
  public void testSelectStarWithJson() throws Exception {
    testBuilder()
        .sqlQuery("select * from cassandra.test_keyspace.map")
        .unOrdered()
        .baselineColumns("prim_field", "nest_field", "more_nest_field", "map_arr")
        .baselineValues(0, mapOf("a", "123", "b", "abc"),
            mapOf("a", mapOf("b", "abc")),
            listOf(mapOf("a", 123, "b", 321), mapOf("c", 456, "d", 789)))
        .go();
  }

  @Test
  public void testSelectNestedFields() throws Exception {
    testBuilder()
        .sqlQuery("select m.nest_field.a a, m.nest_field.b b from cassandra.test_keyspace.map m")
        .unOrdered()
        .baselineColumns("a", "b")
        .baselineValues("123", "abc")
        .go();
  }
}
