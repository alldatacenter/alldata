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
package org.apache.ambari.server.orm.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;


/**
 * Test to check the sanity and consistence of DDL scripts for different SQL dialects.
 * (e.g. no unnamed constraints, the same tables with the same columns and constraints must exist)
 */
public class DDLTests {
  private static final Logger LOG = LoggerFactory.getLogger(DDLTestUtils.class);

  private static final int EXPECTED_ALTER_TABLE_COUNT = 1;

  @Test
  public void testVerifyDerby() throws Exception {
    verifyDDL("Derby");
  }

  @Test
  public void testVerifyPostgres() throws Exception {
    verifyDDL("Postgres");
  }

  @Test
  public void testVerifyMySQL() throws Exception {
    verifyDDL("MySQL");
  }

  @Test
  public void testVerifyOracle() throws Exception {
    verifyDDL("Oracle");
  }

  @Test
  public void testVerifySqlAnywhere() throws Exception {
    verifyDDL("SQLAnywhere");
  }

  @Test
  public void testVerifyMsSqlServer() throws Exception {
    verifyDDL("SQLServer");
  }

  /**
   * To verify if DDL have certain characteristics:
   * <ul>
   *   <li>There are no unnamed constraints</li>
   *   <li>Alter tables are only used in exceptional cases</li>
   *   <li>Table and constraint name lenghts doesn't exceed 30 (a restriction imposed by Oracle)</li>
   * </ul>
   *
   */
  private void verifyDDL(String dbType) throws Exception {
    LOG.info("Checking DDL for {}", dbType);
    DDL ddl = DDLTestUtils.getDdl(dbType);
    printDDLMetrics(ddl);

    // check for unwanted alter tables
    Assert.assertEquals("Expected count of alter tables mismatch. Please include all constraint definitions in " +
            "the create table statement, only use alter table in exceptional cases, such as to work around a circular " +
            "FK dependency. Would another such case occur, please document it in the DDL's and adjust the " +
            "EXPECTED_ALTER_TABLE_COUNT in this test.",
        EXPECTED_ALTER_TABLE_COUNT,
        ddl.alterTables.size());

    // check for too long table/constraint names
    for (String tableName: ddl.tableNames()) {
      Assert.assertTrue("Table name exceeds the 30 character limit: " + tableName, tableName.length() <= 30);
    }
    for (Table table: ddl.tables.values()) {
      Assert.assertTrue("PK name exceeds the 30 character limit: " + table.primaryKey,
          !table.primaryKey.isPresent() || table.primaryKey.get().name().length() <= 30);
      for (Constraint constr: Sets.union(table.foreignKeys, table.uniqueConstraints)) {
        Assert.assertTrue("Constraint name exceeds the 30 character limit: " + constr, constr.name().length() <= 30);
      }
    }
    // check for unnamed PK's (skip quartz tables)
    for (Table table: ddl.tables.values()) {
      Assert.assertFalse("Unnamed PK exists for table: " + table.name,
          !table.name.startsWith("qrtz") && table.primaryKey.isPresent() && table.primaryKey.get().name().equals("<default>"));
      for (Constraint constr: Sets.union(table.foreignKeys, table.uniqueConstraints)) {
        Assert.assertTrue("Constraint name exceeds the 30 character limit: " + constr, constr.name().length() <= 30);
      }
    }

  }

  @Test
  public void testCompareDerby() throws Exception {
    compareAgainstPostgres("Derby");
  }

  @Test
  public void testCompareOracle() throws Exception {
    compareAgainstPostgres("Oracle");
  }

  @Test
  public void testCompareMySQL() throws Exception {
    compareAgainstPostgres("MySQL");
  }

  @Test
  public void testCompareSQLAnywhere() throws Exception {
    compareAgainstPostgres("SQLAnywhere");
  }

  @Test
  public void testCompareSQLServer() throws Exception  {
    compareAgainstPostgres("SQLServer");
  }

  static void compareAgainstPostgres(String dbType) throws Exception {
    LOG.info("Comparing {} against Postgres", dbType);
    DDL postgres = DDLTestUtils.getDdl("Postgres");
    DDL other = DDLTestUtils.getDdl(dbType);
    List<String> diffs = compareDdls(postgres, other);
    if (diffs.isEmpty()) {
      LOG.info("Compare OK.");
    }
    else {
      LOG.info("{} differences found:", diffs.size());
      for (String diff: diffs) { LOG.info(diff); }
      StringBuilder buffer = new StringBuilder();
      buffer.append("Found ").append(diffs.size()).append(" differences when comparing ").append(
          other).append(" against Postgres:").append(System.lineSeparator()).append(
              StringUtils.join(diffs, System.lineSeparator()));

      Assert.fail(buffer.toString());
    }
  }

  static void printDDLMetrics(DDL ddl) {
    LOG.info("DDL metrics for {}", ddl.dbType);
    int colCount = 0;
    int pkCount = 0;
    int fkCount = 0;
    int uqCount = 0;
    for (Table t: ddl.tables.values()) {
      colCount += t.columns.size();
      if (t.primaryKey.isPresent()) {
        pkCount ++;
      }
      fkCount += t.foreignKeys.size();
      uqCount += t.uniqueConstraints.size();
    }
    LOG.info("Found {} tables", ddl.tables.size());
    List<String> tableNames = new ArrayList<>();
    tableNames.addAll(ddl.tableNames());
    Collections.sort(tableNames);
    LOG.info("Table names: {}", Joiner.on(',').join(tableNames));
    LOG.info("Total number of Columns: {}", colCount);
    LOG.info("Total number of PK's: {}", pkCount);
    LOG.info("Total number of FK's: {}", fkCount);
    LOG.info("Total number of UQ's: {}", uqCount);
    LOG.info("Number of Alter table statements: {}", ddl.alterTables.size());

  }

  static List<String> compareDdls(DDL base, DDL other) {
    List<String> diffs = new ArrayList<>();

    if (!base.tableNames().equals(other.tableNames())) {
      Set<String> missingTables = Sets.difference(base.tableNames(), other.tableNames());
      if (!missingTables.isEmpty()) {
        diffs.add("Missing tables: " + Joiner.on(", ").join(missingTables));
      }

      Set<String> extraTables = Sets.difference(other.tableNames(), base.tableNames());
      if (!extraTables.isEmpty()) {
        diffs.add("Extra tables: " + Joiner.on(", ").join(extraTables));
      }
    }

    Set<String> commonTables = Sets.intersection(base.tableNames(), other.tableNames());
    for (String tableName: commonTables) {
      Table baseTable = base.tables.get(tableName);
      Table otherTable = other.tables.get(tableName);
      diffs.addAll(
          compareSets(String.format("Comparing columns of table %s.", tableName), baseTable.columns, otherTable.columns));
      diffs.addAll(
          DDLTests.compareConstraints(tableName, "FK", baseTable.foreignKeys, otherTable.foreignKeys, false));
      diffs.addAll(
          DDLTests.compareConstraints(tableName, "UQ", baseTable.uniqueConstraints, otherTable.uniqueConstraints, false));
      boolean comparePKName = !tableName.contains("qrtz"); // we are more lenient with quartz tables
      diffs.addAll(
          DDLTests.compareConstraints(tableName, "PK", toSet(baseTable.primaryKey), toSet(otherTable.primaryKey), comparePKName));
    }

    return diffs;
  }

  static <T> Set<T> toSet(Optional<T> arg) {
    return arg.isPresent() ? ImmutableSet.of(arg.get()) : ImmutableSet.of();
  }

  static <ContentType> List<String> compareSets(String message, Set<ContentType> base, Set<ContentType> other) {
    List<String> diffs = new ArrayList<>(2);
    Set<ContentType> missingItems = Sets.difference(base, other);
    if (!missingItems.isEmpty()) {
      diffs.add(message + " Missing items: " + Joiner.on(", ").join(missingItems));
    }
    Set<ContentType> extraItems = Sets.difference(other, base);
    if (!extraItems.isEmpty()) {
      diffs.add(message + " Extra items: " + Joiner.on(", ").join(extraItems));
    }
    return diffs;
  }


  static <ContentType> List<String> compareConstraints(String tableName,
                                             String constraintType,
                                             Set<? extends Constraint<ContentType>> base,
                                             Set<? extends Constraint<ContentType>> other,
                                             boolean compareConstraintNames) {
    List<String> diffs = new ArrayList<>();
    Map<ContentType, Constraint<ContentType>> baseByContent = Maps.newHashMap();
    Map<ContentType, Constraint<ContentType>> otherByContent = Maps.newHashMap();
    for (Constraint<ContentType> c: base) {
      baseByContent.put(c.content(), c);
    }
    for (Constraint<ContentType> c: other) {
      otherByContent.put(c.content(), c);
    }
    diffs.addAll(compareSets(String.format("Comparing %ss of table %s.", constraintType, tableName),
        baseByContent.keySet(),
        otherByContent.keySet()));

    Set<ContentType> common = Sets.intersection(baseByContent.keySet(), otherByContent.keySet());
    for (ContentType constrContent : common) {
      Constraint b = baseByContent.get(constrContent);
      Constraint o = otherByContent.get(constrContent);
      if (!b.name().equals(o.name())) {
        if (compareConstraintNames) {
          diffs.add(String.format("Constraint name mismatch for table %s: %s vs. %s", tableName, b, o));
        }
        else {
          LOG.info("Ignoring constraint name mismatch for table {}: {} vs. {}", tableName, b, o);
        }
      }
    }

    return diffs;
  }

}
