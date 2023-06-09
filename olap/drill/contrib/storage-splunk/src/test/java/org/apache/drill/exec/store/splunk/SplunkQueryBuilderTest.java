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

package org.apache.drill.exec.store.splunk;

import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.store.base.filter.ConstantHolder;
import org.apache.drill.exec.store.base.filter.ExprNode;
import org.apache.drill.exec.store.base.filter.RelOp;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class SplunkQueryBuilderTest {

  @Test
  public void testSimpleQuery() {
    SplunkQueryBuilder builder = new SplunkQueryBuilder("main");
    String query = builder.build();
    assertEquals("search index=main | table *", query);
  }

  @Test
  public void testAddSingleFieldQuery() {
    SplunkQueryBuilder builder = new SplunkQueryBuilder("main");
    builder.addField("field1");
    String query = builder.build();
    assertEquals("search index=main | fields field1 | table field1", query);
  }

  @Test
  public void testAddMultipleFieldQuery() {
    SplunkQueryBuilder builder = new SplunkQueryBuilder("main");
    builder.addField("field1");
    builder.addField("field2");
    builder.addField("field3");
    String query = builder.build();
    assertEquals("search index=main | fields field1,field2,field3 | table field1,field2,field3", query);
  }

  @Test
  public void testFieldsAndFiltersQuery() {
    SplunkQueryBuilder builder = new SplunkQueryBuilder("main");

    Map<String, ExprNode.ColRelOpConstNode> filters = new HashMap<>();
    filters.put("field1", new ExprNode.ColRelOpConstNode("field1", RelOp.EQ, new ConstantHolder(TypeProtos.MinorType.VARCHAR, "foo")));

    builder.addField("field1");
    builder.addField("field2");
    builder.addField("field3");

    builder.addFilters(filters);

    String query = builder.build();
    assertEquals("search index=main field1=\"foo\" | fields field1,field2,field3 | table field1,field2,field3", query);
  }

  @Test
  public void testFieldsAndSourcetypeQuery() {
    SplunkQueryBuilder builder = new SplunkQueryBuilder("main");

    Map<String, ExprNode.ColRelOpConstNode> filters = new HashMap<>();
    filters.put("field1", new ExprNode.ColRelOpConstNode("field1", RelOp.EQ, new ConstantHolder(TypeProtos.MinorType.VARCHAR, "foo")));
    filters.put("sourcetype", new ExprNode.ColRelOpConstNode("sourcetype", RelOp.EQ, new ConstantHolder(TypeProtos.MinorType.VARCHAR, "st")));

    builder.addField("field1");
    builder.addField("field2");
    builder.addField("field3");

    builder.addFilters(filters);

    String query = builder.build();
    assertEquals("search index=main sourcetype=\"st\" field1=\"foo\" | fields field1,field2,field3 | table field1,field2,field3", query);
  }

  @Test
  public void testGTQuery() {
    SplunkQueryBuilder builder = new SplunkQueryBuilder("main");

    Map<String, ExprNode.ColRelOpConstNode> filters = new HashMap<>();
    filters.put("field1", new ExprNode.ColRelOpConstNode("field1", RelOp.GT, new ConstantHolder(TypeProtos.MinorType.INT, 5)));

    builder.addField("field1");
    builder.addFilters(filters);

    String query = builder.build();
    assertEquals("search index=main field1>5 | fields field1 | table field1", query);
  }

  @Test
  public void testGEQuery() {
    SplunkQueryBuilder builder = new SplunkQueryBuilder("main");

    Map<String, ExprNode.ColRelOpConstNode> filters = new HashMap<>();
    filters.put("field1", new ExprNode.ColRelOpConstNode("field1", RelOp.GE, new ConstantHolder(TypeProtos.MinorType.INT, 5)));

    builder.addField("field1");
    builder.addFilters(filters);

    String query = builder.build();
    assertEquals("search index=main field1>=5 | fields field1 | table field1", query);
  }

  @Test
  public void testLEQuery() {
    SplunkQueryBuilder builder = new SplunkQueryBuilder("main");

    Map<String, ExprNode.ColRelOpConstNode> filters = new HashMap<>();
    filters.put("field1", new ExprNode.ColRelOpConstNode("field1", RelOp.LE, new ConstantHolder(TypeProtos.MinorType.INT, 5)));

    builder.addField("field1");
    builder.addFilters(filters);

    String query = builder.build();
    assertEquals("search index=main field1<=5 | fields field1 | table field1", query);
  }

  @Test
  public void testLTQuery() {
    SplunkQueryBuilder builder = new SplunkQueryBuilder("main");

    Map<String, ExprNode.ColRelOpConstNode> filters = new HashMap<>();
    filters.put("field1", new ExprNode.ColRelOpConstNode("field1", RelOp.LT, new ConstantHolder(TypeProtos.MinorType.INT, 5)));

    builder.addField("field1");
    builder.addFilters(filters);

    String query = builder.build();
    assertEquals("search index=main field1<5 | fields field1 | table field1", query);
  }

  @Test
  public void testStarAndSourcetypeQuery() {
    SplunkQueryBuilder builder = new SplunkQueryBuilder("main");

    Map<String, ExprNode.ColRelOpConstNode> filters = new HashMap<>();
    filters.put("field1", new ExprNode.ColRelOpConstNode("field1", RelOp.EQ, new ConstantHolder(TypeProtos.MinorType.VARCHAR, "foo")));
    filters.put("sourcetype", new ExprNode.ColRelOpConstNode("sourcetype", RelOp.EQ, new ConstantHolder(TypeProtos.MinorType.VARCHAR, "st")));

    builder.addField("field1");
    builder.addField("field2");
    builder.addField("field3");

    builder.addFilters(filters);

    String query = builder.build();
    assertEquals("search index=main sourcetype=\"st\" field1=\"foo\" | fields field1,field2,field3 | table field1,field2,field3", query);
  }

  @Test
  public void testLimitQuery() {
    SplunkQueryBuilder builder = new SplunkQueryBuilder("main");
    builder.addLimit(5);
    String query = builder.build();
    assertEquals("search index=main | head 5 | table *", query);
  }

  @Test
  public void testAddSingleSourcetypeQuery() {
    SplunkQueryBuilder builder = new SplunkQueryBuilder("main");
    builder.addSourceType("access_combined_wcookie");
    String query = builder.build();
    assertEquals("search index=main sourcetype=\"access_combined_wcookie\" | table *", query);
  }
  @Test
  public void testSingleFilterQuery() {
    SplunkQueryBuilder builder = new SplunkQueryBuilder("main");
    builder.addFilter("field1", "value1", SplunkQueryBuilder.EQUAL_OPERATOR);
    String query = builder.build();
    assertEquals("search index=main field1=\"value1\" | table *", query);
  }

  @Test
  public void testMultipleFilterQuery() {
    SplunkQueryBuilder builder = new SplunkQueryBuilder("main");
    builder.addFilter("field1", "value1", SplunkQueryBuilder.EQUAL_OPERATOR);
    builder.addFilter("field2", "value2", SplunkQueryBuilder.EQUAL_OPERATOR);
    builder.addFilter("field3", "value3", SplunkQueryBuilder.EQUAL_OPERATOR);
    String query = builder.build();
    assertEquals("search index=main field1=\"value1\" field2=\"value2\" field3=\"value3\" | table *", query);
  }

  @Test
  public void testAddMultipleSourcetypeQuery() {
    SplunkQueryBuilder builder = new SplunkQueryBuilder("main");
    builder.addSourceType("access_combined_wcookie");
    builder.addSourceType("sourcetype2");
    builder.addSourceType("sourcetype3");

    String query = builder.build();
    assertEquals("search index=main (sourcetype=\"access_combined_wcookie\" OR sourcetype=\"sourcetype2\" OR sourcetype=\"sourcetype3\") | table *", query);
  }
}
