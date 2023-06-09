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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.drill.test.TestTools;
import org.apache.drill.categories.JdbcTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestRule;

@Category(JdbcTest.class)
public class TestJdbcMetadata extends JdbcTestActionBase {
  @Rule
  public TestRule TIMEOUT = TestTools.getTimeoutRule( 120_000 /* ms */ );

  @Test
  public void catalogs() throws Exception{
    this.testAction(new JdbcAction(){
      @Override
      public ResultSet getResult(Connection c) throws SQLException {
        return c.getMetaData().getCatalogs();
      }
    }, 1);
  }

  @Test
  public void allSchemas() throws Exception{
    this.testAction(new JdbcAction(){
      @Override
      public ResultSet getResult(Connection c) throws SQLException {
        return c.getMetaData().getSchemas();
      }
    });
  }

  @Test
  public void schemasWithConditions() throws Exception{
    this.testAction(new JdbcAction(){
      @Override
      public ResultSet getResult(Connection c) throws SQLException {
        return c.getMetaData().getSchemas("DRILL", "%fs%");
      }
    }, 3);
  }

  @Test
  public void allTables() throws Exception{
    this.testAction(new JdbcAction(){
      @Override
      public ResultSet getResult(Connection c) throws SQLException {
        return c.getMetaData().getTables(null, null, null, null);
      }
    });
  }

  @Test
  public void tablesWithConditions() throws Exception{
    this.testAction(new JdbcAction(){
      @Override
      public ResultSet getResult(Connection c) throws SQLException {
        return c.getMetaData().getTables("DRILL", "sys", "opt%", new String[]{"SYSTEM TABLE", "SYSTEM_VIEW"});
      }
    }, 2);
  }

  @Test
  public void allColumns() throws Exception{
    this.testAction(new JdbcAction(){
      @Override
      public ResultSet getResult(Connection c) throws SQLException {
        return c.getMetaData().getColumns(null, null, null, null);
      }
    });
  }

  @Test
  public void columnsWithConditions() throws Exception{
    this.testAction(new JdbcAction(){
      @Override
      public ResultSet getResult(Connection c) throws SQLException {
        return c.getMetaData().getColumns("DRILL", "sys", "opt%", "%ame");
      }
    }, 2);
  }
}
