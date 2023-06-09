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

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;


/**
 * Drill-specific {@link DatabaseMetaData}.
 */
public interface DrillDatabaseMetaData extends DatabaseMetaData {

  // For matching order of java.sql.DatabaseMetaData:
  //
  //  allProceduresAreCallable()
  //  allTablesAreSelectable()
  //  getURL()
  //  getUserName()
  //  isReadOnly()

  /**
   * <strong>Drill</strong>:
   * Reports that NULL values are sorted high.
   * @return {@code true}
   */
  public boolean nullsAreSortedHigh() throws SQLException;

  /**
   * <strong>Drill</strong>:
   * Reports that NULL values are not sorted low.
   * @return {@code false}
   */
  public boolean nullsAreSortedLow() throws SQLException;

  /**
   * <strong>Drill</strong>:
   * Reports that NULL values are not sorted first.
   * @return {@code false}
   */
  public boolean nullsAreSortedAtStart() throws SQLException;

  /**
   * <strong>Drill</strong>:
   * Reports that NULL values are not sorted last.
   * @return {@code false}
   */
  public boolean nullsAreSortedAtEnd() throws SQLException;

  // For matching order of java.sql.DatabaseMetaData:
  //  getDatabaseProductName()
  //  getDatabaseProductVersion()
  //  getDriverName()
  //  getDriverVersion()
  //  getDriverMajorVersion()
  //  getDriverMinorVersion()
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

  /**
   * <strong>Drill</strong>:
   * Reports current SQL identifier quoting character.
   *  <li>{@link Quoting#BACK_TICK} - default back-quote character ("{@code `}"; Unicode U+0060; "GRAVE ACCENT") </li>
   *  <li>{@link Quoting#DOUBLE_QUOTE} - double quote character ("{@code "}"; Unicode U+0022; 'QUOTATION MARK')</li>
   *  <li>{@link Quoting#BRACKET} - brackets characters ("{@code [}"; Unicode U+005B; 'LEFT SQUARE BRACKET' and
   *  "{@code ]}"; Unicode U+005D; 'RIGHT SQUARE BRACKET')</li>
   *
   * @return current SQL identifier quoting character. Note: 'LEFT SQUARE BRACKET' is returned,
   *         when {@link Quoting#BRACKET} is set.
   */
  @Override
  String getIdentifierQuoteString() throws SQLException;


  // For matching order of java.sql.DatabaseMetaData:
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
  //  supportsConvert( int, int )
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
  //  supportsTransactionIsolationLevel( int )
  //  supportsDataDefinitionAndDataManipulationTransactions()
  //  supportsDataManipulationTransactionsOnly()
  //  dataDefinitionCausesTransactionCommit()
  //  dataDefinitionIgnoredInTransactions()


  /**
   * <strong>Drill</strong>:  Currently, returns an empty (zero-row) result set.
   * (Note:  Currently, result set might not have the expected columns.)
   */
  @Override
  ResultSet getProcedures( String catalog,
                           String schemaPattern,
                           String procedureNamePattern) throws SQLException;

  /**
   * <strong>Drill</strong>:  Currently, returns an empty (zero-row) result set.
   * (Note:  Currently, result set might not have the expected columns.)
   */
  @Override
  ResultSet getProcedureColumns( String catalog,
                                 String schemaPattern,
                                 String procedureNamePattern,
                                 String columnNamePattern ) throws SQLException;


  // For matching order of java.sql.DatabaseMetaData:
  //
  //  getTables( String, String, String, String[] )
  //  getSchemas()
  //  getCatalogs()


  /**
   * <strong>Drill</strong>:  Currently, returns an empty (zero-row) result set.
   * (Note:  Currently, result set might not have the expected columns.)
   */
  @Override
  ResultSet getTableTypes() throws SQLException;


  // For matching order of java.sql.DatabaseMetaData:
  //
  //  getColumns( String, String, String, String )


  /**
   * <strong>Drill</strong>:  Currently, returns an empty (zero-row) result set.
   * (Note:  Currently, result set might not have the expected columns.)
   */
  @Override
  ResultSet getColumnPrivileges( String catalog,
                                 String schema,
                                 String table,
                                 String columnNamePattern ) throws SQLException;

  /**
   * <strong>Drill</strong>:  Currently, returns an empty (zero-row) result set.
   * (Note:  Currently, result set might not have the expected columns.)
   */
  @Override
  ResultSet getTablePrivileges( String catalog,
                                String schemaPattern,
                                String tableNamePattern ) throws SQLException;

  /**
   * <strong>Drill</strong>:  Currently, returns an empty (zero-row) result set.
   * (Note:  Currently, result set might not have the expected columns.)
   */
  @Override
  ResultSet getBestRowIdentifier( String catalog,
                                  String schema,
                                  String table,
                                  int scope,
                                  boolean nullable ) throws SQLException;

  /**
   * <strong>Drill</strong>:  Currently, returns an empty (zero-row) result set.
   * (Note:  Currently, result set might not have the expected columns.)
   */
  @Override
  ResultSet getVersionColumns( String catalog, String schema, String table )
      throws SQLException;

  /**
   * <strong>Drill</strong>:  Currently, returns an empty (zero-row) result set.
   * (Note:  Currently, result set might not have the expected columns.)
   */
  @Override
  ResultSet getPrimaryKeys( String catalog, String schema, String table )
      throws SQLException;

  /**
   * <strong>Drill</strong>:  Currently, returns an empty (zero-row) result set.
   * (Note:  Currently, result set might not have the expected columns.)
   */
  @Override
  ResultSet getImportedKeys( String catalog, String schema, String table )
      throws SQLException;

  /**
   * <strong>Drill</strong>:  Currently, returns an empty (zero-row) result set.
   * (Note:  Currently, result set might not have the expected columns.)
   */
  @Override
  ResultSet getExportedKeys( String catalog, String schema, String table )
      throws SQLException;

  /**
   * <strong>Drill</strong>:  Currently, returns an empty (zero-row) result set.
   * (Note:  Currently, result set might not have the expected columns.)
   */
  @Override
  ResultSet getCrossReference( String parentCatalog,
                               String parentSchema,
                               String parentTable,
                               String foreignCatalog,
                               String foreignSchema,
                               String foreignTable ) throws SQLException;

  /**
   * <strong>Drill</strong>:  Currently, returns an empty (zero-row) result set.
   * (Note:  Currently, result set might not have the expected columns.)
   */
  @Override
  ResultSet getTypeInfo() throws SQLException;

  /**
   * <strong>Drill</strong>:  Currently, returns an empty (zero-row) result set.
   * (Note:  Currently, result set might not have the expected columns.)
   */
  @Override
  ResultSet getIndexInfo( String catalog,
                          String schema,
                          String table,
                          boolean unique,
                          boolean approximate ) throws SQLException;


  // For matching order of java.sql.DatabaseMetaData:
  //
  // --------------------------JDBC 2.0-----------------------------
  //  supportsResultSetType( int )
  //  supportsResultSetConcurrency( int, int )
  //  ownUpdatesAreVisible( int )
  //  ownDeletesAreVisible( int )
  //  ownInsertsAreVisible( int )
  //  othersUpdatesAreVisible( int )
  //  othersDeletesAreVisible( int )
  //  othersInsertsAreVisible( int )
  //  updatesAreDetected( int )
  //  deletesAreDetected( int )
  //  insertsAreDetected( int )
  //  supportsBatchUpdates()


  /**
   * <strong>Drill</strong>:  Currently, returns an empty (zero-row) result set.
   * (Note:  Currently, result set might not have the expected columns.)
   */
  @Override
  ResultSet getUDTs( String catalog,
                     String schemaPattern,
                     String typeNamePattern,
                     int[] types ) throws SQLException;


  // For matching order of java.sql.DatabaseMetaData:
  //
  //  getConnection()
  // ------------------- JDBC 3.0 -------------------------
  //  supportsSavepoints()
  //  supportsNamedParameters()
  //  supportsMultipleOpenResults()
  //  supportsGetGeneratedKeys()


  /**
   * <strong>Drill</strong>:  Currently, returns an empty (zero-row) result set.
   * (Note:  Currently, result set might not have the expected columns.)
   */
  @Override
  ResultSet getSuperTypes( String catalog,
                           String schemaPattern,
                           String typeNamePattern ) throws SQLException;

  /**
   * <strong>Drill</strong>:  Currently, returns an empty (zero-row) result set.
   * (Note:  Currently, result set might not have the expected columns.)
   */
  @Override
  ResultSet getSuperTables( String catalog,
                            String schemaPattern,
                            String tableNamePattern ) throws SQLException;

  /**
   * <strong>Drill</strong>:  Currently, returns an empty (zero-row) result set.
   * (Note:  Currently, result set might not have the expected columns.)
   */
  @Override
  ResultSet getAttributes( String catalog,
                           String schemaPattern,
                           String typeNamePattern,
                           String attributeNamePattern ) throws SQLException;


  // For matching order of java.sql.DatabaseMetaData:
  //
  //  supportsResultSetHoldability( int )
  //  getResultSetHoldability()
  //  getDatabaseMajorVersion()
  //  getDatabaseMinorVersion()
  //  getJDBCMajorVersion()
  //  getJDBCMinorVersion()
  //  getSQLStateType()
  //  locatorsUpdateCopy()
  //  supportsStatementPooling()
  // ------------------------- JDBC 4.0 -----------------------------------
  //  getRowIdLifetime()
  //  getSchemas( String, String )
  //  supportsStoredFunctionsUsingCallSyntax()
  //  autoCommitFailureClosesAllResultSets()


  /**
   * <strong>Drill</strong>:  Currently, returns an empty (zero-row) result set.
   * (Note:  Currently, result set might not have the expected columns.)
   */
  @Override
  ResultSet getClientInfoProperties() throws SQLException;

  /**
   * <strong>Drill</strong>:  Currently, returns an empty (zero-row) result set.
   * (Note:  Currently, result set might not have the expected columns.)
   */
  @Override
  ResultSet getFunctions( String catalog,
                          String schemaPattern,
                          String functionNamePattern ) throws SQLException;

  /**
   * <strong>Drill</strong>:  Currently, returns an empty (zero-row) result set.
   * (Note:  Currently, result set might not have the expected columns.)
   */
  @Override
  ResultSet getFunctionColumns( String catalog,
                                String schemaPattern,
                                String functionNamePattern,
                                String columnNamePattern ) throws SQLException;


  // --------------------------JDBC 4.1 -----------------------------


  /**
   * <strong>Drill</strong>:  Currently, returns an empty (zero-row) result set.
   * (Note:  Currently, result set might not have the expected columns.)
   */
  @Override
  ResultSet getPseudoColumns( String catalog,
                              String schemaPattern,
                              String tableNamePattern,
                              String columnNamePattern ) throws SQLException;


  // For matching order of java.sql.DatabaseMetaData:
  //
  //  generatedKeyAlwaysReturned();

}
