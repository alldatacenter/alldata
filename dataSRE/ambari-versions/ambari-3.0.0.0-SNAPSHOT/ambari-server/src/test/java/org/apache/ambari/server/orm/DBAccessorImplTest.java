/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.orm;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.reset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;

import java.io.ByteArrayInputStream;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.state.State;
import org.eclipse.persistence.platform.database.DatabasePlatform;
import org.eclipse.persistence.sessions.DatabaseSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.inject.Guice;
import com.google.inject.Injector;

import junit.framework.Assert;

public class DBAccessorImplTest {
  private Injector injector;
  private static final AtomicInteger tables_counter = new AtomicInteger(1);
  private static final AtomicInteger schemas_counter = new AtomicInteger(1);

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
  }

  @After
  public void tearDown() throws Exception {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  private static String getFreeTableName() {
    return "test_table_" + tables_counter.getAndIncrement();
  }

  private static String getFreeSchamaName() {
    return "test_schema_" + schemas_counter.getAndIncrement();
  }

  private void createMyTable(String tableName) throws Exception {
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    List<DBColumnInfo> columns = new ArrayList<>();
    columns.add(new DBColumnInfo("id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("name", String.class, 20000, null, true));
    columns.add(new DBColumnInfo("time", Long.class, null, null, true));

    dbAccessor.createTable(tableName, columns, "id");
  }

  private void createMyTable(String tableName, String...columnNames) throws Exception {
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    List<DBColumnInfo> columns = new ArrayList<>();
    columns.add(new DBColumnInfo("id", Long.class, null, null, false));
    for (String column: columnNames){
      columns.add(new DBColumnInfo(column, String.class, 20000, null, true));
    }

    dbAccessor.createTable(tableName, columns, "id");
  }

  @Test
  public void testDbType() throws Exception {
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);
    assertEquals(DBAccessor.DbType.H2, dbAccessor.getDbType());
  }

  @Test
  @Ignore
  public void testAlterColumn() throws Exception {
    String tableName = getFreeTableName();
    createMyTable(tableName);
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    ResultSet rs;
    DBColumnInfo fromColumn;
    DBColumnInfo toColumn;
    Statement statement = dbAccessor.getConnection().createStatement();
    final String dataString = "Data for inserting column.";

    // 1 - VARACHAR --> VARCHAR
    toColumn = new DBColumnInfo("name", String.class, 500, null, true);
    statement.execute(
        String.format("INSERT INTO %s(id, name) VALUES (1, '%s')", tableName,
            dataString));

    dbAccessor.alterColumn(tableName, toColumn);
    rs = statement.executeQuery(
        String.format("SELECT name FROM %s", tableName));
    while (rs.next()) {
      ResultSetMetaData rsm = rs.getMetaData();
      assertEquals(rs.getString(toColumn.getName()), dataString);
      assertEquals(rsm.getColumnTypeName(1), "VARCHAR");
      assertEquals(rsm.getColumnDisplaySize(1), 500);
    }
    rs.close();

    // 2 - VARACHAR --> CLOB
    toColumn = new DBColumnInfo("name", java.sql.Clob.class, 999, null, true);
    dbAccessor.alterColumn(tableName, toColumn);
    rs = statement.executeQuery(
        String.format("SELECT name FROM %s", tableName));
    while (rs.next()) {
      ResultSetMetaData rsm = rs.getMetaData();
      Clob clob = rs.getClob(toColumn.getName());
      assertEquals(dataString, clob.getSubString(1, (int) clob.length()));
      assertEquals("CLOB", rsm.getColumnTypeName(1));
      //size not supported for CLOB in H2
    }
    rs.close();

    // 3 - BLOB --> CLOB
    toColumn = new DBColumnInfo("name_blob_to_clob", java.sql.Clob.class, 567, null,
        true);
    fromColumn = new DBColumnInfo("name_blob_to_clob", byte[].class, 20000,
        null, true);
    dbAccessor.addColumn(tableName, fromColumn);

    String sql = String.format(
        "insert into %s(id, name_blob_to_clob) values (2, ?)", tableName);
    PreparedStatement preparedStatement = dbAccessor.getConnection().prepareStatement(
        sql);
    preparedStatement.setBinaryStream(1,
        new ByteArrayInputStream(dataString.getBytes()),
        dataString.getBytes().length);
    preparedStatement.executeUpdate();
    preparedStatement.close();

    dbAccessor.alterColumn(tableName, toColumn);
    rs = statement.executeQuery(
        String.format("SELECT name_blob_to_clob FROM %s WHERE id=2",
            tableName));
    while (rs.next()) {
      ResultSetMetaData rsm = rs.getMetaData();
      Clob clob = rs.getClob(toColumn.getName());
      assertEquals(clob.getSubString(1, (int) clob.length()), dataString);
      assertEquals(rsm.getColumnTypeName(1), "CLOB");
      assertEquals(rsm.getColumnDisplaySize(1), 567);
    }
    rs.close();

    // 4 - CLOB --> CLOB
    toColumn = new DBColumnInfo("name_blob_to_clob", char[].class, 1500, null,
        true);
    dbAccessor.alterColumn(tableName, toColumn);
    rs = statement.executeQuery(
        String.format("SELECT name_blob_to_clob FROM %s WHERE id=2",
            tableName));
    while (rs.next()) {
      ResultSetMetaData rsm = rs.getMetaData();
      Clob clob = rs.getClob(toColumn.getName());
      assertEquals(clob.getSubString(1, (int) clob.length()), dataString);
      assertEquals(rsm.getColumnTypeName(1), "CLOB");
      assertEquals(rsm.getColumnDisplaySize(1), 1500);
    }
    rs.close();

    dbAccessor.dropTable(tableName);

  }


  @Test
  public void testCreateTable() throws Exception {
    String tableName = getFreeTableName();
    createMyTable(tableName);
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    Statement statement = dbAccessor.getConnection().createStatement();
    statement.execute(String.format("insert into %s(id, name) values(1,'hello')", tableName));

    ResultSet resultSet = statement.executeQuery(String.format("select * from %s", tableName));

    int count = 0;
    while (resultSet.next()) {
      assertEquals(resultSet.getString("name"), "hello");
      count++;
    }

    assertEquals(count, 1);
  }

  @Test
  public void testAddFKConstraint() throws Exception {
    String tableName = getFreeTableName();
    createMyTable(tableName);
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    List<DBColumnInfo> columns = new ArrayList<>();
    columns.add(new DBColumnInfo("fid", Long.class, null, null, false));
    columns.add(new DBColumnInfo("fname", String.class, null, null, false));

    String foreignTableName = getFreeTableName();
    dbAccessor.createTable(foreignTableName, columns, "fid");

    dbAccessor.addFKConstraint(foreignTableName, "MYFKCONSTRAINT", "fid",
      tableName, "id", false);

    Statement statement = dbAccessor.getConnection().createStatement();
    statement.execute("insert into " + tableName + "(id, name) values(1,'hello')");
    statement.execute("insert into " + foreignTableName + "(fid, fname) values(1,'howdy')");

    ResultSet resultSet = statement.executeQuery("select * from " + foreignTableName);

    int count = 0;
    while (resultSet.next()) {
      assertEquals(resultSet.getString("fname"), "howdy");
      count++;
    }
    resultSet.close();
    assertEquals(count, 1);

    exception.expect(SQLException.class);
    exception.expectMessage(containsString("MYFKCONSTRAINT"));
    dbAccessor.executeQuery("DELETE FROM " + tableName);
  }

  @Test
  public void testAddPKConstraint() throws Exception{
    String tableName = getFreeTableName();

    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    List<DBColumnInfo> columns = new ArrayList<>();
    columns.add(new DBColumnInfo("id", Long.class, null, null, false));
    columns.add(new DBColumnInfo("sid", Long.class, null, null, false));
    columns.add(new DBColumnInfo("data", char[].class, null, null, true));

    dbAccessor.createTable(tableName, columns);

    dbAccessor.addPKConstraint(tableName, "PK_sid", "sid");
    try {
      //List<String> indexes = dbAccessor.getIndexesList(tableName, false);
      //assertTrue(CustomStringUtils.containsCaseInsensitive("pk_sid", indexes));
    } finally {
      dbAccessor.dropTable(tableName);
    }
  }

  @Test
  public void testAddColumn() throws Exception {
    String tableName = getFreeTableName();
    createMyTable(tableName);
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    DBColumnInfo dbColumnInfo = new DBColumnInfo("description", String.class,  null, null, true);

    dbAccessor.addColumn(tableName, dbColumnInfo);

    Statement statement = dbAccessor.getConnection().createStatement();
    statement.execute("update " + tableName + " set description = 'blah' where id = 1");

    ResultSet resultSet = statement.executeQuery("select description from " + tableName);

    while (resultSet.next()) {
      assertEquals(resultSet.getString("description"), "blah");
    }
    resultSet.close();
  }

  @Test
  public void testUpdateTable() throws Exception {
    String tableName = getFreeTableName();
    createMyTable(tableName);
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    dbAccessor.updateTable(tableName, "name", "blah", "where id = 1");

    Statement statement = dbAccessor.getConnection().createStatement();
    ResultSet resultSet = statement.executeQuery("select name from " + tableName);

    while (resultSet.next()) {
      assertEquals(resultSet.getString("name"), "blah");
    }
    resultSet.close();
  }


  @Test
  public void testTableHasFKConstraint() throws Exception {
    String tableName = getFreeTableName();
    createMyTable(tableName);

    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    List<DBColumnInfo> columns = new ArrayList<>();
    columns.add(new DBColumnInfo("fid", Long.class, null, null, false));
    columns.add(new DBColumnInfo("fname", String.class, null, null, false));

    String foreignTableName = getFreeTableName();
    dbAccessor.createTable(foreignTableName, columns, "fid");

    Statement statement = dbAccessor.getConnection().createStatement();
    statement.execute("ALTER TABLE " + foreignTableName + " ADD CONSTRAINT FK_test FOREIGN KEY (fid) REFERENCES " +
      tableName + " (id)");

    Assert.assertTrue(dbAccessor.tableHasForeignKey(foreignTableName,
      tableName, "fid", "id"));
  }

  @Test
  public void testGetCheckedForeignKey() throws Exception {
    String tableName = getFreeTableName();
    createMyTable(tableName);

    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    List<DBColumnInfo> columns = new ArrayList<>();
    columns.add(new DBColumnInfo("fid", Long.class, null, null, false));
    columns.add(new DBColumnInfo("fname", String.class, null, null, false));

    String foreignTableName = getFreeTableName();
    dbAccessor.createTable(foreignTableName, columns, "fid");

    Statement statement = dbAccessor.getConnection().createStatement();
    statement.execute("ALTER TABLE " + foreignTableName + " ADD CONSTRAINT FK_test1 FOREIGN KEY (fid) REFERENCES " +
            tableName + " (id)");

    Assert.assertEquals("FK_TEST1", dbAccessor.getCheckedForeignKey(foreignTableName, "fk_test1"));
  }

  @Test
  public void getCheckedForeignKeyReferencingUniqueKey() throws Exception {
    String tableName = getFreeTableName();
    createMyTable(tableName);

    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);
    Statement statement = dbAccessor.getConnection().createStatement();
    statement.execute(String.format("ALTER TABLE %s ADD CONSTRAINT UC_name UNIQUE (%s)", tableName, "name"));

    List<DBColumnInfo> columns = new ArrayList<>();
    columns.add(new DBColumnInfo("fid", Long.class, null, null, false));
    columns.add(new DBColumnInfo("fname", String.class, null, null, false));

    String foreignTableName = getFreeTableName();
    dbAccessor.createTable(foreignTableName, columns);

    statement = dbAccessor.getConnection().createStatement();
    statement.execute(String.format("ALTER TABLE %s ADD CONSTRAINT FK_name FOREIGN KEY (%s) REFERENCES %s (%s)", foreignTableName, "fname", tableName, "name"));

    Assert.assertEquals("FK_NAME", dbAccessor.getCheckedForeignKey(foreignTableName, "fk_name"));
  }

  @Test
  public void testTableExists() throws Exception {
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    Statement statement = dbAccessor.getConnection().createStatement();
    String tableName = getFreeTableName();
    statement.execute("Create table " + tableName + " (id VARCHAR(255))");

    Assert.assertTrue(dbAccessor.tableExists(tableName));
  }

  @Test
  public void testTableExistsMultipleSchemas() throws Exception {
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    String tableName = getFreeTableName();
    createMyTable(tableName);

    // create table with the same name but in custom schema
    createTableUnderNewSchema(dbAccessor, tableName);

    Assert.assertTrue(dbAccessor.tableExists(tableName));
  }

  @Test
  public void testColumnExists() throws Exception {
    String tableName = getFreeTableName();
    createMyTable(tableName);

    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    Assert.assertTrue(dbAccessor.tableHasColumn(tableName, "time"));
  }

  @Test
  public void testColumnExistsMultipleSchemas() throws Exception {
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    String tableName = getFreeTableName();
    createMyTable(tableName);

    // create table with the same name and same field (id) but in custom schema
    createTableUnderNewSchema(dbAccessor, tableName);

    Assert.assertTrue(dbAccessor.tableHasColumn(tableName, "id"));
  }

  @Test
  public void testColumnsExistsMultipleSchemas() throws Exception {
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    String tableName = getFreeTableName();
    createMyTable(tableName);

    // create table with the same name and same field (id) but in custom schema
    createTableUnderNewSchema(dbAccessor, tableName);

    Assert.assertTrue(dbAccessor.tableHasColumn(tableName, "id", "time"));
  }

  @Test
  public void testRenameColumn() throws Exception {
    String tableName = getFreeTableName();
    createMyTable(tableName);
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    dbAccessor.executeQuery("insert into " + tableName + "(id, name, time) values(1, 'Bob', 1234567)");

    dbAccessor.renameColumn(tableName, "time", new DBColumnInfo("new_time", Long.class, 0, null, true));

    Statement statement = dbAccessor.getConnection().createStatement();
    ResultSet resultSet = statement.executeQuery("select new_time from " + tableName + " where id=1");
    int count = 0;
    while (resultSet.next()) {
      count++;
      long newTime = resultSet.getLong("new_time");
      assertEquals(newTime, 1234567L);
    }

    assertEquals(count, 1);
  }

  @Test
  public void testModifyColumn() throws Exception {
    String tableName = getFreeTableName();
    createMyTable(tableName);
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    dbAccessor.executeQuery("insert into " + tableName + "(id, name, time) values(1, 'Bob', 1234567)");

    dbAccessor.alterColumn(tableName, new DBColumnInfo("name", String.class, 25000));

  }

  @Test
  public void testAddColumnWithDefault() throws Exception {
    String tableName = getFreeTableName();
    createMyTable(tableName);
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    dbAccessor.executeQuery("insert into " + tableName + "(id, name, time) values(1, 'Bob', 1234567)");

    dbAccessor.addColumn(tableName, new DBColumnInfo("test", String.class, 1000, "test", false));

    Statement statement = dbAccessor.getConnection().createStatement();
    ResultSet resultSet = statement.executeQuery("select * from " + tableName);
    int count = 0;
    while (resultSet.next()) {
      assertEquals(resultSet.getString("test"), "test");
      count++;
    }

    assertEquals(count, 1);

  }

  @Test
  public void testDBSession() throws Exception {
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);
    String tableName = getFreeTableName();
    createMyTable(tableName);
    dbAccessor.executeQuery("insert into " + tableName + "(id, name, time) values(1, 'Bob', 1234567)");

    DatabaseSession databaseSession = dbAccessor.getNewDatabaseSession();
    databaseSession.login();
    Vector vector = databaseSession.executeSQL("select * from " + tableName + " where id=1");
    assertEquals(vector.size(), 1);
    Map map = (Map) vector.get(0);
    //all names seem to be converted to upper case
    assertEquals("Bob", map.get("name".toUpperCase()));

    databaseSession.logout();
  }

  @Test
  public void testGetColumnType() throws Exception {
    String tableName = getFreeTableName();
    createMyTable(tableName);
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);
    assertEquals(Types.BIGINT, dbAccessor.getColumnType(tableName, "id"));
    assertEquals(Types.VARCHAR, dbAccessor.getColumnType(tableName, "name"));
  }

  @Test
  public void testSetNullable() throws Exception {
    String tableName = getFreeTableName();
    createMyTable(tableName);
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    // create a column with a non-NULL constraint
    DBColumnInfo dbColumnInfo = new DBColumnInfo("isNullable", String.class, 1000, "test", false);
    dbAccessor.addColumn(tableName, dbColumnInfo);

    Statement statement = dbAccessor.getConnection().createStatement();
    ResultSet resultSet = statement.executeQuery("SELECT isNullable FROM " + tableName);
    ResultSetMetaData rsmd = resultSet.getMetaData();
    assertEquals(ResultSetMetaData.columnNoNulls, rsmd.isNullable(1));

    statement.close();

    // set it to nullable
    dbAccessor.setColumnNullable(tableName, dbColumnInfo, true);
    statement = dbAccessor.getConnection().createStatement();
    resultSet = statement.executeQuery("SELECT isNullable FROM " + tableName);
    rsmd = resultSet.getMetaData();
    assertEquals(ResultSetMetaData.columnNullable, rsmd.isNullable(1));

    statement.close();

    // set it back to non-NULL
    dbAccessor.setColumnNullable(tableName, dbColumnInfo, false);
    statement = dbAccessor.getConnection().createStatement();
    resultSet = statement.executeQuery("SELECT isNullable FROM " + tableName);
    rsmd = resultSet.getMetaData();
    assertEquals(ResultSetMetaData.columnNoNulls, rsmd.isNullable(1));

    statement.close();
  }

  private void createTableUnderNewSchema(DBAccessorImpl dbAccessor, String tableName) throws SQLException {
    Statement schemaCreation = dbAccessor.getConnection().createStatement();
    String schemaName = getFreeSchamaName();
    schemaCreation.execute("create schema " + schemaName);

    Statement customSchemaTableCreation = dbAccessor.getConnection().createStatement();
    customSchemaTableCreation.execute(toString().format("Create table %s.%s (id int, time int)", schemaName, tableName));
  }

  /**
   * Checks to ensure that columns created with a default have the correct
   * DEFAULT constraint value set in.
   *
   * @throws Exception
   */
  @Test
  public void testDefaultColumnConstraintOnAddColumn() throws Exception {
    String tableName = getFreeTableName().toUpperCase();
    String columnName = "COLUMN_WITH_DEFAULT_VALUE";

    createMyTable(tableName);
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    // create a column with a non-NULL constraint that has a default value
    DBColumnInfo dbColumnInfo = new DBColumnInfo(columnName, String.class, 32, "foo", false);
    dbAccessor.addColumn(tableName, dbColumnInfo);

    String schema = null;
    Connection connection = dbAccessor.getConnection();
    DatabaseMetaData databaseMetadata = connection.getMetaData();
    ResultSet schemaResultSet = databaseMetadata.getSchemas();
    if (schemaResultSet.next()) {
      schema = schemaResultSet.getString(1);
    }

    schemaResultSet.close();

    String columnDefaultVal = null;
    ResultSet rs = databaseMetadata.getColumns(null, schema, tableName, columnName);

    if (rs.next()) {
      columnDefaultVal = rs.getString("COLUMN_DEF");
    }

    rs.close();

    assertEquals("'foo'", columnDefaultVal);
   }

  @Test
  public void testMoveColumnToAnotherTable() throws Exception {
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);
    String sourceTableName = getFreeTableName();
    String targetTableName = getFreeTableName();
    int testRowAmount = 10;

    createMyTable(sourceTableName, "col1", "col2");
    createMyTable(targetTableName, "col1");

    for (Integer i=0; i < testRowAmount; i++){
      dbAccessor.insertRow(sourceTableName,
        new String[] {"id", "col1", "col2"},
        new String[]{i.toString(), String.format("'source,1,%s'", i), String.format("'source,2,%s'", i)}, false);

      dbAccessor.insertRow(targetTableName,
        new String[] {"id", "col1"},
        new String[]{i.toString(), String.format("'target,1,%s'", i)}, false);
    }

    DBColumnInfo sourceColumn = new DBColumnInfo("col2", String.class, null, null, false);
    DBColumnInfo targetColumn = new DBColumnInfo("col2", String.class, null, null, false);

    dbAccessor.moveColumnToAnotherTable(sourceTableName, sourceColumn, "id",
      targetTableName, targetColumn, "id", "initial");

    Statement statement = dbAccessor.getConnection().createStatement();
    ResultSet resultSet =  statement.executeQuery("SELECT col2 FROM " + targetTableName + " ORDER BY col2");

    assertNotNull(resultSet);

    List<String> response = new LinkedList<>();

    while (resultSet.next()){
      response.add(resultSet.getString(1));
    }

    assertEquals(testRowAmount, response.toArray().length);

    int i = 0;
    for(String row: response){
      assertEquals(String.format("source,2,%s", i), row);
      i++;
    }

   }

  @Test
  public void testCopyColumnToAnotherTable() throws Exception {
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);
    String sourceTableName = getFreeTableName();
    String targetTableName = getFreeTableName();
    int testRowAmount = 10;

    createMyTable(sourceTableName, "col1", "col2", "col3", "col4", "col5");
    createMyTable(targetTableName, "col1", "col2", "col3");

    for (Integer i = 0; i < testRowAmount; i++) {
      dbAccessor.insertRow(sourceTableName,
          new String[]{"id", "col1", "col2", "col3", "col4", "col5"},
          new String[]{i.toString(), String.format("'1,%s'", i), String.format("'2,%s'", i * 2), String.format("'3,%s'", i * 3), String.format("'4,%s'", i * 4), String.format("'%s'", (i * 5) % 2)}, false);

      dbAccessor.insertRow(targetTableName,
          new String[]{"id", "col1", "col2", "col3"},
          new String[]{i.toString(), String.format("'1,%s'", i), String.format("'2,%s'", i * 2), String.format("'3,%s'", i * 3)}, false);
    }

    DBColumnInfo sourceColumn = new DBColumnInfo("col4", String.class, null, null, false);
    DBColumnInfo targetColumn = new DBColumnInfo("col4", String.class, null, null, false);

    dbAccessor.copyColumnToAnotherTable(sourceTableName, sourceColumn, "id", "col1", "col2",
        targetTableName, targetColumn, "id", "col1", "col2", "col5", "0", "initial");

    Statement statement = dbAccessor.getConnection().createStatement();
    ResultSet resultSet = statement.executeQuery("SELECT col4 FROM " + targetTableName + " ORDER BY id");

    assertNotNull(resultSet);

    List<String> response = new LinkedList<>();

    while (resultSet.next()) {
      response.add(resultSet.getString(1));
    }

    assertEquals(testRowAmount, response.toArray().length);
    for (String row : response) {
      System.out.println(row);
    }


    int i = 0;
    for (String row : response) {
      if (i % 2 == 0) {
        assertEquals(String.format("4,%s", i * 4), row);
      } else {
        assertEquals("initial", row);
      }
      i++;
    }

  }

  @Test
  public void testGetIntColumnValues() throws Exception {
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);
    String sourceTableName = getFreeTableName();
    int testRowAmount = 10;

    createMyTable(sourceTableName, "col1", "col2", "col3", "col4", "col5");

    for (Integer i = 0; i < testRowAmount; i++) {
      dbAccessor.insertRow(sourceTableName,
          new String[]{"id", "col1", "col2", "col3", "col4", "col5"},
          new String[]{i.toString(), String.format("'1,%s'", i), String.format("'2,%s'", i * 2), String.format("'3,%s'", i * 3), String.format("'4,%s'", i * 4), String.format("'%s'", (i * 5) % 2)}, false);
    }

    List<Integer> idList = dbAccessor.getIntColumnValues(sourceTableName, "id",
        new String[]{"col1", "col5"}, new String[]{"1,0", "0"}, false);

    assertEquals(idList.size(), 1);
    assertEquals(idList.get(0), Integer.valueOf(0));

    idList = dbAccessor.getIntColumnValues(sourceTableName, "id",
        new String[]{"col5"}, new String[]{"0"}, false);

    assertEquals(idList.size(), 5);

    int i = 0;
    for (Integer id : idList) {
      assertEquals(id, Integer.valueOf(i * 2));
      i++;
    }

  }

  @Test
  public void testMoveNonexistentColumnIsNoop() throws Exception {
    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);
    String sourceTableName = getFreeTableName();
    String targetTableName = getFreeTableName();
    int testRowAmount = 10;

    createMyTable(sourceTableName, "col1");
    createMyTable(targetTableName, "col1", "col2");

    for (Integer i=0; i < testRowAmount; i++){
      dbAccessor.insertRow(sourceTableName,
        new String[] {"id", "col1"},
        new String[]{i.toString(), String.format("'source,1,%s'", i)}, false);

      dbAccessor.insertRow(targetTableName,
        new String[] {"id", "col1", "col2"},
        new String[]{i.toString(), String.format("'target,1,%s'", i), String.format("'target,2,%s'", i)}, false);
    }

    DBColumnInfo sourceColumn = new DBColumnInfo("col2", String.class, null, null, false);
    DBColumnInfo targetColumn = new DBColumnInfo("col2", String.class, null, null, false);

    dbAccessor.moveColumnToAnotherTable(sourceTableName, sourceColumn, "id",
      targetTableName, targetColumn, "id", "initial");

    // should not result in exception due to unknown column in source table
  }

  @Test
  public void testDbColumnInfoEqualsAndHash() {
    DBColumnInfo column1 = new DBColumnInfo("col", String.class, null, null, false);
    DBColumnInfo equalsColumn1 = new DBColumnInfo("col", String.class, null, null, false);
    DBColumnInfo notEqualsColumn1Name = new DBColumnInfo("col1", String.class, null, null, false);
    DBColumnInfo notEqualsColumn1Type = new DBColumnInfo("col", Integer.class, null, null, false);
    DBColumnInfo notEqualsColumn1Length = new DBColumnInfo("col", String.class, 10, null, false);
    DBColumnInfo notEqualsColumn1DefaultValue = new DBColumnInfo("col", String.class, null, "default", false);
    DBColumnInfo notEqualsColumn1DefaultValueEmptyString = new DBColumnInfo("col", String.class, null, "", false);
    DBColumnInfo notEqualsColumn1Nullable = new DBColumnInfo("col", String.class, null, null, true);

    assertTrue(column1.hashCode() == equalsColumn1.hashCode());
    assertFalse(column1.hashCode() == notEqualsColumn1Name.hashCode());
    assertFalse(column1.hashCode() == notEqualsColumn1Type.hashCode());
    assertFalse(column1.hashCode() == notEqualsColumn1Length.hashCode());
    assertFalse(column1.hashCode() == notEqualsColumn1DefaultValue.hashCode());
    assertTrue(column1.hashCode() == notEqualsColumn1DefaultValueEmptyString.hashCode()); // null and "" yield the same hashcode
    assertFalse(column1.hashCode() == notEqualsColumn1Nullable.hashCode());

    assertTrue(column1.equals(equalsColumn1));
    assertFalse(column1.equals(notEqualsColumn1Name));
    assertFalse(column1.equals(notEqualsColumn1Type));
    assertFalse(column1.equals(notEqualsColumn1Length));
    assertFalse(column1.equals(notEqualsColumn1DefaultValue));
    assertFalse(column1.equals(notEqualsColumn1DefaultValueEmptyString));
    assertFalse(column1.equals(notEqualsColumn1Nullable));
  }

  @Test
  public void testBuildQuery() throws Exception {
    String tableName = getFreeTableName();
    createMyTable(tableName);

    DBAccessorImpl dbAccessor = injector.getInstance(DBAccessorImpl.class);

    assertEquals(String.format("SELECT id FROM %s WHERE name='value1'", tableName),
    dbAccessor.buildQuery(tableName, new String[] {"id"}, new String[] {"name"}, new String[] {"value1"}));

    assertEquals(String.format("SELECT id FROM %s WHERE name='value1' AND time='100'", tableName),
    dbAccessor.buildQuery(tableName, new String[] {"id"}, new String[] {"name", "time"}, new String[] {"value1", "100"}));

    assertEquals(String.format("SELECT id, name FROM %s WHERE time='100'", tableName),
    dbAccessor.buildQuery(tableName, new String[] {"id", "name"}, new String[] {"time"}, new String[] {"100"}));

    assertEquals(String.format("SELECT id, name, time FROM %s", tableName),
    dbAccessor.buildQuery(tableName, new String[] {"id", "name", "time"}, null, null));

    try {
      dbAccessor.buildQuery("invalid_table_name", new String[] {"id", "name"}, new String[] {"time"}, new String[] {"100"});
      fail("Expected IllegalArgumentException due to bad table name");
    }
    catch (IllegalArgumentException e) {
      // This is expected
    }

    try {
      dbAccessor.buildQuery(tableName, new String[] {"invalid_column_name"}, new String[] {"time"}, new String[] {"100"});
      fail("Expected IllegalArgumentException due to bad column name");
    }
    catch (IllegalArgumentException e) {
      // This is expected
    }

    try {
      dbAccessor.buildQuery(tableName, new String[] {"id"}, new String[] {"invalid_column_name"}, new String[] {"100"});
      fail("Expected IllegalArgumentException due to bad column name");
    }
    catch (IllegalArgumentException e) {
      // This is expected
    }

    try {
      dbAccessor.buildQuery(tableName, new String[] {}, new String[] {"name"}, new String[] {"100"});
      fail("Expected IllegalArgumentException due missing select columns");
    }
    catch (IllegalArgumentException e) {
      // This is expected
    }

    try {
      dbAccessor.buildQuery(tableName, null, new String[] {"name"}, new String[] {"100"});
      fail("Expected IllegalArgumentException due missing select columns");
    }
    catch (IllegalArgumentException e) {
      // This is expected
    }

    try {
      dbAccessor.buildQuery(tableName, new String[] {"id"}, new String[] {"name", "time"}, new String[] {"100"});
      fail("Expected IllegalArgumentException due mismatch condition column and value arrays");
    }
    catch (IllegalArgumentException e) {
      // This is expected
    }
  }

  @Test
  public void escapesEnumValue() {
    DatabasePlatform platform = createNiceMock(DatabasePlatform.class);
    Object value = State.UNKNOWN;
    expect(platform.convertToDatabaseType(value)).andReturn(value).anyTimes();
    reset(platform);
    assertEquals("'" + value + "'", DBAccessorImpl.escapeParameter(value, platform));
  }

  @Test
  public void escapesString() {
    DatabasePlatform platform = createNiceMock(DatabasePlatform.class);
    Object value = "hello, world";
    expect(platform.convertToDatabaseType(value)).andReturn(value).anyTimes();
    reset(platform);
    assertEquals("'" + value + "'", DBAccessorImpl.escapeParameter(value, platform));
  }

  @Test
  public void doesNotEscapeNumbers() {
    DatabasePlatform platform = createNiceMock(DatabasePlatform.class);
    Object value = 123;
    expect(platform.convertToDatabaseType(value)).andReturn(value).anyTimes();
    reset(platform);
    assertEquals("123", DBAccessorImpl.escapeParameter(value, platform));
  }

}
