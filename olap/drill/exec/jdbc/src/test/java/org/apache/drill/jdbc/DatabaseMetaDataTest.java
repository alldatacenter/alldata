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
package org.apache.drill.jdbc;

import static java.sql.Connection.TRANSACTION_NONE;
import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import static java.sql.Connection.TRANSACTION_READ_UNCOMMITTED;
import static java.sql.Connection.TRANSACTION_REPEATABLE_READ;
import static java.sql.Connection.TRANSACTION_SERIALIZABLE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import org.apache.calcite.avatica.util.Quoting;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.apache.drill.categories.JdbcTest;
import org.apache.drill.test.BaseTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Test for Drill's implementation of DatabaseMetaData's methods (other than
 * those tested separately, e.g., {@code getColumn(...)}, tested in
 * {@link DatabaseMetaDataGetColumnsTest})).
 */
@Category(JdbcTest.class)
public class DatabaseMetaDataTest extends BaseTest {

  protected static Connection connection;
  protected static DatabaseMetaData dbmd;

  @BeforeClass
  public static void setUpConnection() throws SQLException {
    // (Note: Can't use JdbcTest's connect(...) because JdbcTest closes
    // Connection--and other JDBC objects--on test method failure, but this test
    // class uses some objects across methods.)
    connection = new Driver().connect( "jdbc:drill:zk=local", null );
    dbmd = connection.getMetaData();
  }

  @AfterClass
  public static void tearDownConnection() throws SQLException {
    connection.close();
  }


  // For matching order of java.sql.DatabaseMetaData:
  //
  //  allProceduresAreCallable()
  //  allTablesAreSelectable()
  //  getURL()
  //  getUserName()
  //  isReadOnly()


  @Test
  public void testNullsAreSortedMethodsSaySortedHigh() throws SQLException {
    assertThat( "DatabaseMetadata.nullsAreSortedHigh()",
                dbmd.nullsAreSortedHigh(), equalTo( true ) );
    assertThat( "DatabaseMetadata.nullsAreSortedLow()",
                dbmd.nullsAreSortedLow(), equalTo( false ) );
    assertThat( "DatabaseMetadata.nullsAreSortedAtEnd()",
                dbmd.nullsAreSortedAtEnd(), equalTo( false ) );
    assertThat( "DatabaseMetadata.nullsAreSortedAtStart()",
                dbmd.nullsAreSortedAtStart(), equalTo( false ) );
  }


  // For matching order of java.sql.DatabaseMetaData:
  //
  //  getDatabaseProductName()
  //  getDatabaseProductVersion()
  //  getDriverName()
  //  getDriverVersion()
  //  getDriverMajorVersion();
  //  getDriverMinorVersion();
  //  usesLocalFiles()
  //  usesLocalFilePerTable()
  //  supportsMixedCaseIdentifiers()
  //  storesUpperCaseIdentifiers()
  //  storesLowerCaseIdentifiers()
  //  storesMixedCaseIdentifiers()
  //  supportsMixedCaseQuotedIdentifiers()
  //  storesUpperCaseQuotedIdentifiers()
  //  storesLowerCaseQuotedIdentifiers()
  //  storesMixedCaseQuotedIdentifiers()


  // TODO(DRILL-5402): Update when server meta information will be updated during one session.
  @Test
  public void testGetIdentifierQuoteString() throws SQLException {
    // If connection string hasn't "quoting_identifiers" property, this method will return current system
    // "planner.parser.quoting_identifiers" option (back tick by default)
    assertThat(dbmd.getIdentifierQuoteString(), equalTo(Quoting.BACK_TICK.string));
  }


  // For matching order of java.sql.DatabaseMetaData:
  //
  //  getSQLKeywords()
  //  getNumericFunctions()
  //  getStringFunctions()
  //  getSystemFunctions()
  //  getTimeDateFunctions()
  //  getSearchStringEscape()
  //  getExtraNameCharacters()
  //  supportsAlterTableWithAddColumn()
  //  supportsAlterTableWithDropColumn()
  //  supportsColumnAliasing()
  //  nullPlusNonNullIsNull()
  //  supportsConvert()
  //  supportsConvert(int fromType, int toType)
  //  supportsTableCorrelationNames()
  //  supportsDifferentTableCorrelationNames()
  //  supportsExpressionsInOrderBy()
  //  supportsOrderByUnrelated()
  //  supportsGroupBy()
  //  supportsGroupByUnrelated()
  //  supportsGroupByBeyondSelect()
  //  supportsLikeEscapeClause()
  //  supportsMultipleResultSets()
  //  supportsMultipleTransactions()
  //  supportsNonNullableColumns()
  //  supportsMinimumSQLGrammar()
  //  supportsCoreSQLGrammar()
  //  supportsExtendedSQLGrammar()
  //  supportsANSI92EntryLevelSQL()
  //  supportsANSI92IntermediateSQL()
  //  supportsANSI92FullSQL()
  //  supportsIntegrityEnhancementFacility()
  //  supportsOuterJoins()
  //  supportsFullOuterJoins()
  //  supportsLimitedOuterJoins()
  //  getSchemaTerm()
  //  getProcedureTerm()
  //  getCatalogTerm()
  //  isCatalogAtStart()
  //  getCatalogSeparator()
  //  supportsSchemasInDataManipulation()
  //  supportsSchemasInProcedureCalls()
  //  supportsSchemasInTableDefinitions()
  //  supportsSchemasInIndexDefinitions()
  //  supportsSchemasInPrivilegeDefinitions()
  //  supportsCatalogsInDataManipulation()
  //  supportsCatalogsInProcedureCalls()
  //  supportsCatalogsInTableDefinitions()
  //  supportsCatalogsInIndexDefinitions()
  //  supportsCatalogsInPrivilegeDefinitions()
  //  supportsPositionedDelete()
  //  supportsPositionedUpdate()
  //  supportsSelectForUpdate()
  //  supportsStoredProcedures()
  //  supportsSubqueriesInComparisons()
  //  supportsSubqueriesInExists()
  //  supportsSubqueriesInIns()
  //  supportsSubqueriesInQuantifieds()
  //  supportsCorrelatedSubqueries()
  //  supportsUnion()
  //  supportsUnionAll()
  //  supportsOpenCursorsAcrossCommit()
  //  supportsOpenCursorsAcrossRollback()
  //  supportsOpenStatementsAcrossCommit()
  //  supportsOpenStatementsAcrossRollback()
  //  getMaxBinaryLiteralLength()
  //  getMaxCharLiteralLength()
  //  getMaxColumnNameLength()
  //  getMaxColumnsInGroupBy()
  //  getMaxColumnsInIndex()
  //  getMaxColumnsInOrderBy()
  //  getMaxColumnsInSelect()
  //  getMaxColumnsInTable()
  //  getMaxConnections()
  //  getMaxCursorNameLength()
  //  getMaxIndexLength()
  //  getMaxSchemaNameLength()
  //  getMaxProcedureNameLength()
  //  getMaxCatalogNameLength()
  //  getMaxRowSize()
  //  doesMaxRowSizeIncludeBlobs()
  //  getMaxStatementLength()
  //  getMaxStatements()
  //  getMaxTableNameLength()
  //  getMaxTablesInSelect()
  //  getMaxUserNameLength()
  //  getDefaultTransactionIsolation()
  //  supportsTransactions()
  //  supportsTransactionIsolationLevel(int level)
  //  supportsDataDefinitionAndDataManipulationTransactions()
  //  supportsDataManipulationTransactionsOnly()
  //  dataDefinitionCausesTransactionCommit()
  //  dataDefinitionIgnoredInTransactions()


  @Test
  public void testGetDefaultTransactionIsolationSaysNone() throws SQLException {
    assertThat( dbmd.getDefaultTransactionIsolation(), equalTo( TRANSACTION_NONE ) );
  }

  @Test
  public void testSupportsTransactionsSaysNo() throws SQLException {
    assertThat( dbmd.supportsTransactions(), equalTo( false ) );
  }

  @Test
  public void testSupportsTransactionIsolationLevelNoneSaysYes()
      throws SQLException {
    assertTrue( dbmd.supportsTransactionIsolationLevel( TRANSACTION_NONE ) );
  }

  @Test
  public void testSupportsTransactionIsolationLevelOthersSayNo()
      throws SQLException {
    assertFalse( dbmd.supportsTransactionIsolationLevel( TRANSACTION_READ_UNCOMMITTED ) );
    assertFalse( dbmd.supportsTransactionIsolationLevel( TRANSACTION_READ_COMMITTED ) );
    assertFalse( dbmd.supportsTransactionIsolationLevel( TRANSACTION_REPEATABLE_READ ) );
    assertFalse( dbmd.supportsTransactionIsolationLevel( TRANSACTION_SERIALIZABLE ) );
  }

  @Test
  public void testGetProceduresReturnsNonNull() throws SQLException {
    assertThat( dbmd.getProcedures( null, null, "%" ), notNullValue() );
  }
  // TODO:  Later, test more (e.g., right columns (even if/though zero rows)).

  @Test
  public void testGetProcedureColumnsReturnsNonNull() throws SQLException {
    assertThat( dbmd.getProcedureColumns( null, null, "%", "%" ), notNullValue() );
  }
  // TODO:  Later, test more (e.g., right columns (even if/though zero rows)).


  // For matching order of java.sql.DatabaseMetaData:
  //
  //  getTables(String catalog, String schemaPattern, String tableNamePattern, String types[])
  //  getSchemas()
  //  getCatalogs()


  @Test
  public void testGetTableTypesReturnsNonNull() throws SQLException {
    assertThat( dbmd.getTableTypes(), notNullValue() );
  }
  // TODO:  Later, test more (e.g., right columns (even if/though zero rows)).


  // For matching order of java.sql.DatabaseMetaData:
  //
  //  getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern)


  @Test
  public void testGetColumnPrivilegesReturnsNonNull() throws SQLException {
    assertThat( dbmd.getColumnPrivileges( null, null, "%", "%" ), notNullValue() );
  }
  // TODO:  Later, test more (e.g., right columns (even if/though zero rows)).

  @Test
  public void testGetTablePrivilegesReturnsNonNull() throws SQLException {
    assertThat( dbmd.getTablePrivileges( null, null, "%" ), notNullValue() );
  }
  // TODO:  Later, test more (e.g., right columns (even if/though zero rows)).

  @Test
  public void testGetBestRowIdentifierReturnsNonNull() throws SQLException {
    assertThat( dbmd.getBestRowIdentifier( null, null, "%", DatabaseMetaData.bestRowTemporary, true ), notNullValue() );
  }
  // TODO:  Later, test more (e.g., right columns (even if/though zero rows)).

  @Test
  public void testGetVersionColumnsReturnsNonNull() throws SQLException {
    assertThat( dbmd.getVersionColumns( null, null, "%" ), notNullValue() );
  }
  // TODO:  Later, test more (e.g., right columns (even if/though zero rows)).

  @Test
  public void testGetPrimaryKeysReturnsNonNull() throws SQLException {
    assertThat( dbmd.getPrimaryKeys( null, null, "%" ), notNullValue() );
  }
  // TODO:  Later, test more (e.g., right columns (even if/though zero rows)).

  @Test
  public void testGetImportedKeysReturnsNonNull() throws SQLException {
    assertThat( dbmd.getImportedKeys( null, null, "%" ), notNullValue() );
  }
  // TODO:  Later, test more (e.g., right columns (even if/though zero rows)).

  @Test
  public void testGetExportedKeysReturnsNonNull() throws SQLException {
    assertThat( dbmd.getExportedKeys( null, null, "%" ), notNullValue() );
  }
  // TODO:  Later, test more (e.g., right columns (even if/though zero rows)).

  @Test
  public void testGetCrossReferenceReturnsNonNull() throws SQLException {
    assertThat( dbmd.getCrossReference( null, null, "%", null, null, "%" ), notNullValue() );
  }
  // TODO:  Later, test more (e.g., right columns (even if/though zero rows)).

  @Test
  public void testGetTypeInfoReturnsNonNull() throws SQLException {
    assertThat( dbmd.getTypeInfo(), notNullValue() );
  }
  // TODO:  Later, test more (e.g., right columns (even if/though zero rows)).

  @Test
  public void testGetIndexInfoReturnsNonNull() throws SQLException {
    assertThat( dbmd.getIndexInfo( null, null, "%", false, true ), notNullValue() );
  }
  // TODO:  Later, test more (e.g., right columns (even if/though zero rows)).


  // For matching order of java.sql.DatabaseMetaData:
  //
  // --------------------------JDBC 2.0-----------------------------
  //  supportsResultSetType(int type)
  //  supportsResultSetConcurrency(int type, int concurrency)
  //  ownUpdatesAreVisible(int type)
  //  ownDeletesAreVisible(int type)
  //  ownInsertsAreVisible(int type)
  //  othersUpdatesAreVisible(int type)
  //  othersDeletesAreVisible(int type)
  //  othersInsertsAreVisible(int type)
  //  updatesAreDetected(int type)
  //  deletesAreDetected(int type)
  //  insertsAreDetected(int type)
  //  supportsBatchUpdates()


  @Test
  public void testGetUDTsReturnsNonNull() throws SQLException {
    assertThat( dbmd.getUDTs( null, null, "%", null ), notNullValue() );
  }
  // TODO:  Later, test more (e.g., right columns (even if/though zero rows)).


  // For matching order of java.sql.DatabaseMetaData:
  // getConnection()
  // ------------------- JDBC 3.0 -------------------------
  //  supportsSavepoints()
  //  supportsNamedParameters()
  //  supportsMultipleOpenResults()
  //  supportsGetGeneratedKeys()


  @Test
  public void testGetSuperTypesReturnsNonNull() throws SQLException {
    assertThat( dbmd.getSuperTypes( null, "%", "%" ), notNullValue() );
  }
  // TODO:  Later, test more (e.g., right columns (even if/though zero rows)).

  @Test
  public void testGetSuperTablesReturnsNonNull() throws SQLException {
    assertThat( dbmd.getSuperTables( null, "%", "%" ), notNullValue() );
  }
  // TODO:  Later, test more (e.g., right columns (even if/though zero rows)).

  @Test
  public void testGetAttributesReturnsNonNull() throws SQLException {
    assertThat( dbmd.getAttributes( null, null, "%", "%" ), notNullValue() );
  }
  // TODO:  Later, test more (e.g., right columns (even if/though zero rows)).


  // For matching order of java.sql.DatabaseMetaData:
  //
  //  supportsResultSetHoldability(int holdability)
  //  getResultSetHoldability()
  //  getDatabaseMajorVersion()
  //  getDatabaseMinorVersion()
  //  getJDBCMajorVersion()
  //  getJDBCMinorVersion()
  //  getSQLStateType()
  //  locatorsUpdateCopy()
  //  supportsStatementPooling()
  //- ------------------------ JDBC 4.0 -----------------------------------
  //  getRowIdLifetime()
  //  getSchemas(String catalog, String schemaPattern)
  //  getSchemas(String, String)


  @Test
  public void testGetClientInfoPropertiesReturnsNonNull() throws SQLException {
    assertThat( dbmd.getClientInfoProperties(), notNullValue() );
  }
  // TODO:  Later, test more (e.g., right columns (even if/though zero rows)).


  @Test
  public void testGetFunctionsReturnsNonNull() throws SQLException {
    assertThat( dbmd.getFunctions( null, "%", "%" ), notNullValue() );
  }
  // TODO:  Later, test more (e.g., right columns (even if/though zero rows)).

  @Test
  public void testGetFunctionColumnsReturnsNonNull() throws SQLException {
    assertThat( dbmd.getFunctionColumns( null, null, "%", null ), notNullValue() );
  }
  // TODO:  Later, test more (e.g., right columns (even if/though zero rows)).


  // For matching order of java.sql.DatabaseMetaData:
  //
  //  supportsStoredFunctionsUsingCallSyntax()
  //  autoCommitFailureClosesAllResultSets()
  //??--------------------------JDBC 4.1 -----------------------------


  @Test
  public void testGetPseudoColumnsReturnsNonNull() throws SQLException {
    assertThat( dbmd.getPseudoColumns( null, null, "%", "%" ), notNullValue() );
  }
  // TODO:  Later, test more (e.g., right columns (even if/though zero rows)).


  // For matching order of java.sql.DatabaseMetaData:
  //
  //   generatedKeyAlwaysReturned()


}
