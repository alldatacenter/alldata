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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;

/**
 * <p>
 * Utility to parse DDL scripts for verification and comparision. Parsing is not complete, only things relevant for
 * the unit tests are considered, e.g. the column data types are not captured as they may differ across different SQL
 * dialects.
 * </p>
 * <p>
 * Parsing is mostly done by regexp matches, so the parser has limited capabilities. The following known restrictions apply:
 * </p>
 * <ul>
 *   <li>Multiple statements in a single line are not supported, e.g: CREATE TABLE X(...); CREATE TABLE Y(...)</li>
 *   <li>Mutiple definitions in the same line within a create statement is supported though, e.g: name varchar(255), id bigint, ...</li>
 *   <li>Statements must be terminated by ;</li>
 * </ul>
 *
 */
public class DDLTestUtils {
  private static final Logger LOG = LoggerFactory.getLogger(DDLTestUtils.class);

  // These patterns are used during the initial line by line parsing of a DDL.
  // The patterns help
  // 1. filter out irrelevant lines (comment, empty line, go/commit),
  // 2. recognize statement starters (create table, alter table, create index)
  // 3. recognize statement terminators (;)
  private static final Pattern CommentLine =  Pattern.compile("^\\s*--.*");
  private static final Pattern EmptyLine =    Pattern.compile("^\\s*$");
  private static final Pattern CommitLine =   Pattern.compile("^\\s*(go|commit).*$");
  private static final Pattern CreateTable =  Pattern.compile("^\\s*create\\s+table.*$");
  private static final Pattern AlterTable =   Pattern.compile("^\\s*alter\\s+table.*$");
  private static final Pattern CreateIndex =  Pattern.compile("^\\s*create\\s+index.*$");
  private static final Pattern EndStatement = Pattern.compile("(.*\\;)\\s*$");

  // These patterns are used to match column/constraint definitons in a create table statement
  // to capture the table name
  private static final Pattern TableName =   Pattern.compile("^\\s*create table\\s+([\\w\\.\\_]+).*$");
  // to capture the name and columns in a primary key
  private static final Pattern PK =          Pattern.compile("^.*constraint\\s+([\\w\\.\\_]+)\\s+primary\\s+key\\s*\\(([^\\)]+)\\).*$");
  // to capture the name and columns in a clusterd primary key
  private static final Pattern PKClustered = Pattern.compile("^.*constraint\\s+([\\w\\.\\_]+)\\s+primary\\s+key\\s+clustered\\s*\\(([^\\)]+)\\).*$");
  // to capture the name and columns in a unique constraint
  private static final Pattern UQ =          Pattern.compile("^.*constraint\\s+([\\w\\.\\_]+)\\s+unique\\s*\\(([^\\)]+)\\).*$");
  // to capture the name and columns and the referred columnd in a foreign key
  private static final Pattern FK =          Pattern.compile("^.*constraint\\s+([\\w\\.\\_]+)\\s+foreign\\s+key\\s*\\(([^\\)]*)\\)\\s*references\\s+([\\w\\_\\.]+)\\s*\\(([^\\)]+)\\).*$");
  // to capture the name of a columns
  private static final Pattern Col =         Pattern.compile("^\\s*([\\`\\\"\\[\\]\\w\\.\\_]+)\\s+.*$");
  // to capture column lists within the create table statement, such as the column list in "primary key (name, id)"
  // in such lists commas are replaced with a | so that we can reliably use comma as a definition separator within the create table statement
  private static final Pattern InnerList =   Pattern.compile("(\\([^\\(^\\)]+\\))");

  // These patterns represent Unnamed constraints
  private static final Pattern UnnamedPK =          Pattern.compile("^\\s*primary\\s+key[\\sclustered]*\\(([^\\)]+)\\).*$"); // e.g: primary key [clustered] (name)
  private static final Pattern UnnamedUQ =          Pattern.compile("^\\s*unique\\s*\\(([^\\)]+)\\).*$"); // e.g: unique (name)
  private static final Pattern UnnamedFK =          Pattern.compile("^\\s*foreign\\s+key\\s*\\(([^\\)]+)\\)\\s*references\\s+([\\w\\_\\.]+)\\s*\\(([^\\)]+)\\).*$"); // e.g: foreign key (name) references other_table(name)
  private static final Pattern PKColumn =          Pattern.compile("^.*[\\w\\.\\_]+\\s.*primary\\s+key[\\sclustered\\,\\;\\)]*$"); // e.g: name varchar(255) not null primary key [clustered]
  private static final Pattern UQColumn =          Pattern.compile("^\\s*[\\w\\.\\_]+\\s.*unique[\\s\\;\\,\\)]*$"); // e.g: name varchar(255) not null unique
  private static final List<Pattern> CheckedUnnamedConstraints = ImmutableList.of(UnnamedPK);
  private static final List<Pattern> UncheckedUnnamedConstraints = ImmutableList.of(UnnamedUQ, UnnamedFK, PKColumn, UQColumn);

  private static final LoadingCache<String, DDL> ddlCache = CacheBuilder.newBuilder().build(
    new CacheLoader<String, DDL>() {
      @Override public DDL load(String key) throws Exception { return loadDdl(key); }
    }
  );

  /**
   * List of supported databases.
   */
  public static final List<String> DATABASES = ImmutableList.of(
    "Derby",
    "MySQL",
    "Oracle",
    "Postgres",
    "Postgres-EMBEDDED",
    "SQLAnywhere",
    "SQLServer");

  /**
   * Loads and parses the DDL for a specific database type (e.g. Postgres). Already loaded DDL's are cached.
   * @param dbType One of the supported databases @see #DATABASES
   * @return the parsed DDL
   * @throws Exception if exception occurs during loading/parsing
   */
  public static DDL getDdl(String dbType) throws Exception{
    return ddlCache.get(dbType);
  }

  private static URL getDdlUrl(String dbType) {
    return Resources.getResource("Ambari-DDL-" + dbType + "-CREATE.sql");
  }

  private static List<String> loadFile(String dbType) throws Exception {
    List<String> lines = Resources.readLines(getDdlUrl(dbType), Charsets.UTF_8);
    List<String> replaced = new ArrayList<>(lines.size());
    for (String line: lines) { replaced.add(line.toLowerCase()); }
    return replaced;
  }

  /**
   * Groups the load DDL file into statements. Currently CREATE TABLE's, ALTER TABLE's and CREATE INDEX-es
   * are considered statements. Multiple statements in a single line are not supported. Comments, empty lines and
   * GO / COMMIT commands are discarded.
   *
   * @param ddlFile the loaded DDL file (as list of strings)
   * @return a list of strings containing the statements
   */
  private static List<String> groupStatements(List<String> ddlFile) {
    List<String> statements = new ArrayList<>();
    Optional<ArrayList<String>> currentStmt = Optional.absent();
    for (String line: ddlFile) {
      // These lines should be skipped
      if (CommentLine.matcher(line).matches() ||
          EmptyLine.matcher(line).matches() ||
          CommitLine.matcher(line).matches());
      // These lines indicate the start of a CREATE TABLE / ALTER TABLE / CREATE INDEX statement
      else if (CreateTable.matcher(line).matches() ||
          AlterTable.matcher(line).matches() ||
          CreateIndex.matcher(line).matches()) {
        // Prepare to collect subsequent lines as part of the new statement
        if(currentStmt.isPresent()) throw new IllegalStateException(
            "Unfinished statement: " + currentStmt.get() + "\nnew statement: " +line);
        currentStmt = Optional.of(new ArrayList<String>());
        currentStmt.get().add(stripComment(line));
        // If the statement is a one liner, close it right away
        if (line.contains(";")) {
          statements.add(Joiner.on(' ').join(currentStmt.get()));
          currentStmt = Optional.absent();
        }
      }
      // Process terminating line (containing ;): add to the current statement and close current statement
      else if (currentStmt.isPresent() && EndStatement.matcher(line).matches()) {
        currentStmt.get().add(stripComment(line));
        statements.add(Joiner.on(' ').join(currentStmt.get()));
        currentStmt = Optional.absent();
      }
      // Collect all other lines as part of the current statement
      else if (currentStmt.isPresent()) {
        currentStmt.get().add(stripComment(line));
      }
    }
    return statements;
  }

  private static String stripComment(String line) {
    return line.contains("--") ? line.substring(0, line.indexOf("--")) : line;
  }

  private static Collection<String> toColumns(String cols) {
    List<String> columns = new ArrayList<>();
    for (String col: Splitter.on('|').split(cols)) {
      columns.add( stripPrefixQuotationAndBrackets(col.trim()));
    }
    return columns;
  }

  /**
   * Strips out quotation characters ('"[]) and schema prefixes from identifiers
   * (table / columns / constraint names)
   * @param input an identifier
   * @return the important part of the identifier
   */
  private static String stripPrefixQuotationAndBrackets(String input) {
    String output = input.replaceAll("[\\`\\\"\\[\\]]", "").replaceAll("[^\\.]*\\.", "");
    return output;
  }

  private static Optional<String> firstMatchingGroup(Pattern p, String s) {
    Matcher m = p.matcher(s);
    if (m.matches()) {
      return Optional.of(m.group(1));
    }
    else {
      return Optional.absent();
    }
  }

  private static Map<String, Table> parseTableDefs(List<String> statements) {
    // Find all CREATE TABLE statements
    List<String> createTables = new ArrayList<>();
    for (String stmt: statements) {
      if (stmt.matches(".*create\\s+table.*")) {
        String content = stmt.substring(stmt.indexOf('(') + 1, stmt.lastIndexOf(')'));
        // Replace , with | within PK/FK/UQ definitions so that we will be able to partition column/constraint definitions by ,
        Matcher m = InnerList.matcher(content);
        while (m.find()) {
          String innerList = m.group();
          stmt = stmt.replace(innerList, innerList.replaceAll("\\,", "|"));
        }
        createTables.add(stmt);
      }
    }
    List<Table> tables = new ArrayList<>();
    // Parse CREATE TABLE statements
    for(String ct: createTables) {
      String tableName = stripPrefixQuotationAndBrackets(firstMatchingGroup(TableName, ct).get());
      List<String> columns = new ArrayList<>();
      Optional<SimpleConstraint> pk = Optional.absent();
      List<FKConstraint> fks = new ArrayList<>();
      List<SimpleConstraint> uqs = new ArrayList<>();
      final String innerPart = ct.substring(ct.indexOf('(') + 1, ct.lastIndexOf(')'));
      for (String definition: Splitter.on(',').split(innerPart)) {
        definition = definition.trim();
        assertNounnamedConstraint(tableName, definition);
        Matcher pkMatcher = PK.matcher(definition);
        Matcher pkClustMatcher = PKClustered.matcher(definition);
        Matcher unnamedPkMatcher = UnnamedPK.matcher(definition);
        Matcher pkColumnMatcher = PKColumn.matcher(definition);
        Matcher fkMatcher = FK.matcher(definition);
        Matcher uqMatcher = UQ.matcher(definition);
        Matcher unnamedFkMatcher = UnnamedFK.matcher(definition);
        Matcher unnamedUqMatcher = UnnamedUQ.matcher(definition);
        Matcher uqColumnMatcher = UQColumn.matcher(definition);
        Matcher colMatcher = Col.matcher(definition);
        if (pkMatcher.matches()) {
          pk = Optional.of(Constraint.pk(pkMatcher.group(1),toColumns(pkMatcher.group(2))));
        } else if (pkMatcher.matches()) {
          pk = Optional.of(Constraint.pk(stripPrefixQuotationAndBrackets(pkMatcher.group(1)),toColumns(pkMatcher.group(2))));
        } else if (pkClustMatcher.matches()) {
          pk = Optional.of(Constraint.pk(stripPrefixQuotationAndBrackets(pkClustMatcher.group(1)),toColumns(pkClustMatcher.group(2))));
        } else if (unnamedPkMatcher.matches()) {
          pk = Optional.of(Constraint.pk("<default>",toColumns(unnamedPkMatcher.group(1))));
        } else if (fkMatcher.matches()) {
          fks.add(Constraint.fk(fkMatcher.group(1), toColumns(fkMatcher.group(2)), stripPrefixQuotationAndBrackets(fkMatcher.group(3)), toColumns(fkMatcher.group(4))));
        } else if (unnamedFkMatcher.matches()) {
          fks.add(Constraint.fk("<default>", toColumns(unnamedFkMatcher.group(1)), stripPrefixQuotationAndBrackets(unnamedFkMatcher.group(2)), toColumns(unnamedFkMatcher.group(3))));
        } else if (uqMatcher.matches()) {
          uqs.add(Constraint.uq(stripPrefixQuotationAndBrackets(uqMatcher.group(1)),toColumns(uqMatcher.group(2))));
        } else if (unnamedUqMatcher.matches()) {
          uqs.add(Constraint.uq("<default>", toColumns(unnamedUqMatcher.group(1))));
        } else if (colMatcher.matches()) {
          String colName = stripPrefixQuotationAndBrackets(colMatcher.group(1));
          columns.add(colName);
          // column definitions can include PK/UQ declaration, e.g: x integer not null primary key
          if (pkColumnMatcher.matches()) {
            pk = Optional.of(Constraint.pk("<default>", Collections.singleton(colName)));
          } else if (uqColumnMatcher.matches()) {
            uqs.add(Constraint.uq("<default>", Collections.singleton(colName)));
          }
        } else {
          LOG.warn("Unexpected definition: {}, context: {}", definition, ct);
        }
      }
      if (columns.isEmpty()) {
        throw new IllegalStateException("No columns found in table " + tableName);
      }
      checkDupes("columns of table " + tableName, columns);
      tables.add(new Table(tableName,
          ImmutableSet.copyOf(columns),
          pk,
          ImmutableSet.copyOf(fks),
          ImmutableSet.copyOf(uqs)));
    }
    Map<String, Table> tableMap = Maps.newHashMap();
    for(Table t: tables) {
      if (tableMap.containsKey(t.name)) throw new IllegalStateException("Duplicate table definition: " + t.name);
      tableMap.put(t.name, t);
    }
    return tableMap;
  }

  private static void checkDupes(String objectName, List<? extends Object> items) {
    Set<Object> set = Sets.newHashSet(items);
    if (set.size() < items.size()) {
      throw new IllegalStateException(String.format("Duplicates found in %s: %s", objectName, Iterables.toString(items)));
    }
  }

  /**
   * Currently we only fail on unnamed primary keys.
   * @param tableName
   * @param definition
   */
  private static void assertNounnamedConstraint(String tableName, String definition) {
    if (tableName.contains("qrtz")) {
      LOG.debug("Skipp checking quartz table: {}", tableName);
    }
    else {
      for (Pattern unnamedConstraint: CheckedUnnamedConstraints) {
        if (unnamedConstraint.matcher(definition).matches()) {
          throw new IllegalStateException(
              String.format("Found invalid (unnamed) constraint in table %s: %s", tableName, definition));
        }
      }
      for (Pattern unnamedConstraint: UncheckedUnnamedConstraints) {
        if (unnamedConstraint.matcher(definition).matches()) {
          LOG.info("Found unnamed constraint in table {}: {}", tableName, definition);
        }
      }
    }
  }

  private static DDL loadDdl(String dbType) throws Exception {
    List<String> lines = loadFile(dbType);
    List<String> statements = groupStatements(lines);
    Map<String, Table> tables = parseTableDefs(statements);
    List<String> alterTables = new ArrayList<>();
    for (String stmt: statements) {
      if (stmt.matches(".*alter\\s+table.*")) alterTables.add(stmt);
    }
    return new DDL(dbType, tables, alterTables);
  }

}

/**
 * Represents a DDL
 */
class DDL {
  final String dbType;
  final Map<String, Table> tables;
  final List<String> alterTables;

  Set<String> tableNames() { return tables.keySet(); }

  DDL(String dbType, Map<String, Table> tables, List<String> alterTables) {
    this.dbType = dbType;
    this.tables = tables;
    this.alterTables = alterTables;
  }
}

/**
 * Represents a datbase table
 */
class Table {
  final String name;
  final ImmutableSet<String> columns;
  final Optional<SimpleConstraint> primaryKey;
  final ImmutableSet<FKConstraint> foreignKeys;
  final ImmutableSet<SimpleConstraint> uniqueConstraints;

  Table(String name, Set<String> columns, Optional<SimpleConstraint> primaryKey, Set<FKConstraint> foreignKeys, Set<SimpleConstraint> uniqueConstraints) {
    this.name = name;
    this.columns =
        (columns instanceof ImmutableSet) ? (ImmutableSet<String>)columns : ImmutableSet.copyOf(columns);
    this.primaryKey = primaryKey;
    this.foreignKeys =
        (foreignKeys instanceof ImmutableSet) ? (ImmutableSet<FKConstraint>)foreignKeys : ImmutableSet.copyOf(foreignKeys);
    this.uniqueConstraints =
        (uniqueConstraints instanceof ImmutableSet) ? (ImmutableSet<SimpleConstraint>) uniqueConstraints : ImmutableSet.copyOf(uniqueConstraints);
  }

  @Override
  public String toString() {
    return String.format("TABLE name: %s, columns: %s, pk: %s, fks: %s, uqs: %s",
        name, Iterables.toString(columns), primaryKey, Iterables.toString(foreignKeys), Iterables.toString(uniqueConstraints));
  }
}

/**
 * Represents a constraint.
 */
abstract class Constraint<ContentType> {
  abstract String name();
  abstract ContentType content();

  static SimpleConstraint pk(String name, Collection<String> columns) {
    Preconditions.checkArgument(!columns.isEmpty(), "Columns must not be empty.");
    return new SimpleConstraint(name, "PK", columns);
  }

  static SimpleConstraint uq(String name, Collection<String> columns) {
    Preconditions.checkArgument(!columns.isEmpty(), "Columns must not be empty.");
    return new SimpleConstraint(name, "PK", columns);
  }

  static FKConstraint fk(String name, Collection<String> columns, String referredTableName, Collection<String> referredColumns) {
    Preconditions.checkArgument(!columns.isEmpty(), "Columns must not be empty.");
    Preconditions.checkArgument(!referredColumns.isEmpty(), "Referred columns must not be empty.");
    return new FKConstraint(name, columns, referredTableName, referredColumns);
  }

}

/**
 * Represents a simple constraint (PK/UQ)
 */
class SimpleConstraint extends Constraint<Set<String>> {
  final String name;
  final String type;
  final ImmutableSet<String> columns; // These have favorable equals/hashcode semantics

  SimpleConstraint(String name, String type, Collection<String> columns) {
    this.name = name;
    this.type = type;
    this.columns = (columns instanceof ImmutableSet) ? (ImmutableSet<String>) columns : ImmutableSet.copyOf(columns);
  }

  public String name() {
    return name;
  }

  public ImmutableSet<String> content() { return columns; }

  @Override public String toString() {
    return String.format("%s %s [%s]", type, name, Joiner.on(',').join(columns));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SimpleConstraint that = (SimpleConstraint) o;
    if (!name.equals(that.name)) return false;
    if (!type.equals(that.type)) return false;
    return columns.equals(that.columns);
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + type.hashCode();
    result = 31 * result + columns.hashCode();
    return result;
  }

}

class FKConstraintContent {
  final ImmutableSet<String> columns; // These have favorable equals/hashcode semantics
  final String referredTable;
  final ImmutableSet<String> referredColumns; // These have favorable equals/hashcode semantics

  public FKConstraintContent(Collection<String> columns, String referredTable, Collection<String> referredColumns) {
    this.columns = columns instanceof ImmutableSet ? (ImmutableSet<String>)columns : ImmutableSet.copyOf(columns);
    this.referredTable = referredTable;
    this.referredColumns = referredColumns instanceof ImmutableSet ? (ImmutableSet<String>)referredColumns :
        ImmutableSet.copyOf(referredColumns);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FKConstraintContent that = (FKConstraintContent) o;
    if (!columns.equals(that.columns)) return false;
    if (!referredTable.equals(that.referredTable)) return false;
    return referredColumns.equals(that.referredColumns);

  }

  @Override
  public int hashCode() {
    int result = columns.hashCode();
    result = 31 * result + referredTable.hashCode();
    result = 31 * result + referredColumns.hashCode();
    return result;
  }

  @Override public String toString() {
    return String.format("[%s] --> %s [%s]", Joiner.on(',').join(columns), referredTable, Joiner.on(',').join(referredColumns));
  }

}

class FKConstraint extends Constraint<FKConstraintContent> {
  final String name;
  final FKConstraintContent content;

  FKConstraint(String name, Collection<String> columns, String referredTable, Collection<String> referredColumns) {
    this.name = name;
    this.content = new FKConstraintContent(columns, referredTable, referredColumns);
  }

  public String name() {
    return name;
  }

  public FKConstraintContent content() {
    return content;
  }

  @Override public String toString() {
    return String.format("FK name:%s content: %s", name, content);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FKConstraint that = (FKConstraint) o;
    if (!name.equals(that.name)) return false;
    return content.equals(that.content);
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + content.hashCode();
    return result;
  }
}
