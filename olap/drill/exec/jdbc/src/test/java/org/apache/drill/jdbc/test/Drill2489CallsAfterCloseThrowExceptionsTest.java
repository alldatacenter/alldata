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

import org.apache.drill.categories.JdbcTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.drill.jdbc.Driver;
import org.apache.drill.jdbc.JdbcTestBase;
import org.apache.drill.jdbc.AlreadyClosedSqlException;

/**
 * Test class for JDBC requirement that almost all methods throw
 * {@link SQLException} when called on a closed primary object (e.g.,
 * {@code Connection}, {@code ResultSet}, etc.).
 * <p>
 *   NOTE:  This test currently covers:
 *   {@link Connection},
 *   {@link Statement},
 *   {@link PreparedStatement},
 *   {@link ResultSet},
 *   {@link java.sql.ResultSetMetaData}, and
 *   {@link DatabaseMetaData}.
 * </p>
 * <p>
 *   It does not cover unimplemented {@link java.sql.CallableStatement} or any relevant
 *   secondary objects such as {@link java.sql.Array} or {@link java.sql.Struct}).
 * </p>
 */
@Category(JdbcTest.class)
public class Drill2489CallsAfterCloseThrowExceptionsTest extends JdbcTestBase {
  private static final Logger logger =
      getLogger(Drill2489CallsAfterCloseThrowExceptionsTest.class);

  private static Connection closedConn;
  private static Connection openConn;
  private static Statement closedPlainStmtOfOpenConn;
  private static PreparedStatement closedPreparedStmtOfOpenConn;
  // No CallableStatement.
  private static ResultSet closedResultSetOfClosedStmt;
  private static ResultSet closedResultSetOfOpenStmt;
  private static ResultSetMetaData resultSetMetaDataOfClosedResultSet;
  private static ResultSetMetaData resultSetMetaDataOfClosedStmt;
  private static DatabaseMetaData databaseMetaDataOfClosedConn;

  @BeforeClass
  public static void setUpClosedObjects() throws Exception {
    // (Note: Can't use JdbcTest's connect(...) for this test class.)

    final Connection connToClose =
        new Driver().connect("jdbc:drill:zk=local", getDefaultProperties());
    final Connection connToKeep =
        new Driver().connect("jdbc:drill:zk=local", getDefaultProperties());

    final Statement plainStmtToClose = connToKeep.createStatement();
    final Statement plainStmtToKeep = connToKeep.createStatement();
    final PreparedStatement preparedStmtToClose =
        connToKeep.prepareStatement("VALUES 'PreparedStatement query'");
    try {
      connToKeep.prepareCall("VALUES 'CallableStatement query'");
      fail("Test seems to be out of date.  Was prepareCall(...) implemented?");
    }
    catch (SQLException | UnsupportedOperationException e) {
      // Expected.
    }

    final ResultSet resultSetToCloseOnStmtToClose =
        plainStmtToClose.executeQuery("VALUES 'plain Statement query'");
    resultSetToCloseOnStmtToClose.next();
    final ResultSet resultSetToCloseOnStmtToKeep =
        plainStmtToKeep.executeQuery("VALUES 'plain Statement query'");
    resultSetToCloseOnStmtToKeep.next();

    final ResultSetMetaData rsmdForClosedStmt =
        resultSetToCloseOnStmtToKeep.getMetaData();
    final ResultSetMetaData rsmdForOpenStmt =
        resultSetToCloseOnStmtToClose.getMetaData();

    final DatabaseMetaData dbmd = connToClose.getMetaData();

    connToClose.close();
    plainStmtToClose.close();
    preparedStmtToClose.close();
    resultSetToCloseOnStmtToClose.close();
    resultSetToCloseOnStmtToKeep.close();

    closedConn = connToClose;
    openConn = connToKeep;
    closedPlainStmtOfOpenConn = plainStmtToClose;
    closedPreparedStmtOfOpenConn = preparedStmtToClose;
    closedResultSetOfClosedStmt = resultSetToCloseOnStmtToClose;
    closedResultSetOfOpenStmt = resultSetToCloseOnStmtToKeep;
    resultSetMetaDataOfClosedResultSet = rsmdForOpenStmt;
    resultSetMetaDataOfClosedStmt = rsmdForClosedStmt;
    databaseMetaDataOfClosedConn = dbmd;

    // Self-check that member variables are set (and objects are in right open
    // or closed state):
    assertTrue("Test setup error", closedConn.isClosed());
    assertFalse("Test setup error", openConn.isClosed());
    assertTrue("Test setup error", closedPlainStmtOfOpenConn.isClosed());
    assertTrue("Test setup error", closedPreparedStmtOfOpenConn.isClosed());
    assertTrue("Test setup error", closedResultSetOfClosedStmt.isClosed());
    assertTrue("Test setup error", closedResultSetOfOpenStmt.isClosed());
    // (No ResultSetMetaData.isClosed() or DatabaseMetaData.isClosed():)
    assertNotNull("Test setup error", resultSetMetaDataOfClosedResultSet);
    assertNotNull("Test setup error", resultSetMetaDataOfClosedStmt);
    assertNotNull("Test setup error", databaseMetaDataOfClosedConn);
  }

  @AfterClass
  public static void tearDownConnection() throws Exception {
    openConn.close();
  }

  ///////////////////////////////////////////////////////////////
  // 1.  Check that isClosed() and close() do not throw, and isClosed() returns
  //     true.

  @Test
  public void testClosedConnection_close_doesNotThrow() throws SQLException {
    closedConn.close();
  }

  @Test
  public void testClosedConnection_isClosed_returnsTrue() throws SQLException {
    assertThat(closedConn.isClosed(), equalTo(true));
  }

  @Test
  public void testClosedPlainStatement_close_doesNotThrow() throws SQLException {
    closedPlainStmtOfOpenConn.close();
  }

  @Test
  public void testClosedPlainStatement_isClosed_returnsTrue() throws SQLException {
    assertThat(closedPlainStmtOfOpenConn.isClosed(), equalTo(true));
  }

  @Test
  public void testClosedPreparedStatement_close_doesNotThrow() throws SQLException {
    closedPreparedStmtOfOpenConn.close();
  }

  @Test
  public void testClosedPreparedStatement_isClosed_returnsTrue() throws SQLException {
    assertThat(closedPreparedStmtOfOpenConn.isClosed(), equalTo(true));
  }

  @Test
  public void testClosedResultSet_close_doesNotThrow() throws SQLException {
    closedResultSetOfOpenStmt.close();
  }

  @Test
  public void testClosedResultSet_isClosed_returnsTrue() throws SQLException {
    assertThat(closedResultSetOfOpenStmt.isClosed(), equalTo(true));
  }

  ///////////////////////////////////////////////////////////////
  // 2.  Check that all methods throw or not appropriately (either as specified
  //     by JDBC or currently intended as partial Avatica workaround).

  /**
   * Reflection-based checker of throwing of "already closed" exception by JDBC
   * interfaces' implementation methods.
   *
   * @param  <INTF>  JDBC interface type
   */
  private static abstract class ThrowsClosedBulkChecker<INTF> {
    private final Class<INTF> jdbcIntf;
    private final INTF jdbcObject;
    protected final String normalClosedExceptionText;

    private String methodLabel;  // for inter-method multi-return passing
    private Object[] argsArray;  // for inter-method multi-return passing

    private final StringBuilder failureLinesBuf = new StringBuilder();
    private final StringBuilder successLinesBuf = new StringBuilder();


    ThrowsClosedBulkChecker(final Class<INTF> jdbcIntf,
                            final INTF jdbcObject,
                            final String normalClosedExceptionText) {
      this.jdbcIntf = jdbcIntf;
      this.jdbcObject = jdbcObject;
      this.normalClosedExceptionText = normalClosedExceptionText;
    }

    /**
     * Gets minimal value suitable for use as actual parameter value for given
     * formal parameter type.
     */
    private static Object getDummyValueForType(Class<?> type) {
      final Object result;
      if (! type.isPrimitive()) {
        result = null;
      }
      else {
        if (type == boolean.class) {
          result = false;
        }
        else if (type == byte.class) {
          result = (byte) 0;
        }
        else if (type == short.class) {
          result = (short) 0;
        }
        else if (type == char.class) {
          result = (char) 0;
        }
        else if (type == int.class) {
          result = 0;
        }
        else if (type == long.class) {
          result = (long) 0L;
        }
        else if (type == float.class) {
          result = 0F;
        }
        else if (type == double.class) {
          result = 0.0;
        }
        else {
          fail("Test needs to be updated to handle type " + type);
          result = null;  // Not executed; for "final".
        }
      }
      return result;
    }

    /**
     * Assembles arguments array and method signature text for given method.
     * Updates members args and methodLabel.
     */
    private void makeArgsAndLabel(Method method) {
      final List<Object> argsList = new ArrayList<>();
      methodLabel = jdbcIntf.getSimpleName() + "." + method.getName() + "(";
      boolean first = true;
      for (Class<?> paramType : method.getParameterTypes()) {
        if (! first) {
          methodLabel += ", ";
        }
        first = false;
        methodLabel += paramType.getSimpleName();
        argsList.add(getDummyValueForType(paramType));
      }
      methodLabel += ")";
      argsArray = argsList.toArray();
    }

    /**
     * Reports whether it's okay if given method didn't throw any exception.
     */
    protected boolean isOkayNonthrowingMethod(Method method) {
       switch (method.getName()) {
         case "isClosed":
         case "close":
         case "isValid":
           return true;
         default:
           return false;
       }
    }

    /**
     * Reports whether it's okay if given method throw given exception (that is
     * not preferred AlreadyClosedException with regular message).
     */
    protected boolean isOkaySpecialCaseException(Method method,
                                                 Throwable cause) {
      return false;
    }

    /**
     * Tests one method.
     * (Disturbs members set by makeArgsAndLabel, but those shouldn't be used
     * except by this method.)
     */
    private void testOneMethod(Method method) {
      makeArgsAndLabel(method);
      logger.debug("Testing method " + methodLabel);

      try {
        // See if method throws exception:
        method.invoke(jdbcObject, argsArray);

        // If here, method didn't throw--check if it's an expected non-throwing
        // method (e.g., an isClosed).  (If not, report error.)
        final String resultLine = "- " + methodLabel + " didn't throw\n";

        if (isOkayNonthrowingMethod(method)) {
          successLinesBuf.append(resultLine);
        }
        else {
          logger.trace("Failure: " + resultLine);
          failureLinesBuf.append(resultLine);
        }
      }
      catch (InvocationTargetException e) {
        final Throwable cause = e.getCause();
        final String resultLine = "- " + methodLabel + " threw <" + cause + ">\n";

        if (AlreadyClosedSqlException.class == cause.getClass()
             && normalClosedExceptionText.equals(cause.getMessage())) {
          // Common good case--our preferred exception class with our message.
          successLinesBuf.append(resultLine);
        }
        else if (NullPointerException.class == cause.getClass()
                  && (method.getName().equals("isWrapperFor")
                      || method.getName().equals("unwrap"))) {
          // Known good-enough case--these methods don't throw already-closed
          // exception, but do throw NullPointerException because of the way
          // we call them (with null) and the way Avatica code implements them.
          successLinesBuf.append(resultLine);
        }
        else {
          // Not a case that base-class code here recognizes, but subclass may
          // know that it's okay.
          if (isOkaySpecialCaseException(method, cause)) {
            successLinesBuf.append(resultLine);
          }
          else {
            final String badResultLine =
                "- " + methodLabel + " threw <" + cause + "> instead"
                + " of " + AlreadyClosedSqlException.class.getSimpleName()
                + " with \""
                + normalClosedExceptionText.replaceAll("\"", "\"\"")
                + "\"" + "\n";
            logger.trace("Failure: " + resultLine);
            failureLinesBuf.append(badResultLine);
          }
        }
      }
      catch (IllegalAccessException | IllegalArgumentException e) {
        fail("Unexpected exception: " + e + ", cause = " + e.getCause()
              + "  from " + method);
      }
    }

    public void testAllMethods() {
      for (Method method : jdbcIntf.getMethods()) {
        testOneMethod(method);
      }
    }

    public boolean hadAnyFailures() {
      return 0 != failureLinesBuf.length();
    }

    public String getFailureLines() {
      return failureLinesBuf.toString();
    }

    public String getSuccessLines() {
      return successLinesBuf.toString();
    }

    public String getReport() {
      final String report =
          "Failures:\n"
          + getFailureLines()
          + "(Successes:\n"
          + getSuccessLines()
          + ")";
      return report;
    }
  } // class ThrowsClosedChecker<INTF>

  private static class ClosedConnectionChecker
      extends ThrowsClosedBulkChecker<Connection> {

    private static final String STATEMENT_CLOSED_MESSAGE =
        "Connection is already closed.";

    ClosedConnectionChecker(Class<Connection> intf, Connection jdbcObject) {
      super(intf, jdbcObject, STATEMENT_CLOSED_MESSAGE);
    }

    @Override
    protected boolean isOkayNonthrowingMethod(Method method) {
      return
          super.isOkayNonthrowingMethod(method)
          // New Java 9 methods not implemented in Avatica.
          || method.getName().equals("beginRequest")
          || method.getName().equals("endRequest");
    }

    @Override
    protected boolean isOkaySpecialCaseException(Method method, Throwable cause) {
      final boolean result;
      if (super.isOkaySpecialCaseException(method, cause)) {
        result = true;
      }
      else if (SQLClientInfoException.class == cause.getClass()
                && normalClosedExceptionText.equals(cause.getMessage())
                && (method.getName().equals("setClientInfo")
                    || method.getName().equals("getClientInfo"))) {
        // Special good case--we had to use SQLClientInfoException from those.
        result = true;
      }
      else if (SQLFeatureNotSupportedException.class == cause.getClass()
                && (method.getName().equals("setShardingKeyIfValid")
                    || method.getName().equals("setShardingKey"))) {
        // New Java 9 methods not implemented in Avatica.
        result = true;
      } else {
        result = false;
      }
      return result;
    }
  } // class ClosedConnectionChecker

  @Test
  public void testClosedConnectionMethodsThrowRight() {
    ThrowsClosedBulkChecker<Connection> checker =
        new ClosedConnectionChecker(Connection.class, closedConn);

    checker.testAllMethods();

    if (checker.hadAnyFailures()) {
      System.err.println(checker.getReport());
      fail("Already-closed exception error(s): \n" + checker.getReport());
    }
  }

  private static class ClosedPlainStatementChecker
      extends ThrowsClosedBulkChecker<Statement> {

    private static final String PLAIN_STATEMENT_CLOSED_MESSAGE =
        "Statement is already closed.";

    ClosedPlainStatementChecker(Class<Statement> intf, Statement jdbcObject) {
      super(intf, jdbcObject, PLAIN_STATEMENT_CLOSED_MESSAGE);
    }

    @Override
    protected boolean isOkaySpecialCaseException(Method method, Throwable cause) {
      final boolean result;
      if (super.isOkaySpecialCaseException(method, cause)) {
        result = true;
      } else if (NullPointerException.class == cause.getClass()
              && (method.getName().equals("enquoteIdentifier")
                  || method.getName().equals("enquoteLiteral")
                  || method.getName().equals("enquoteNCharLiteral")
                  || method.getName().equals("isSimpleIdentifier"))) {
        result = true;
      } else {
        result = false;
      }

      return result;
    }
  } // class ClosedPlainStatementChecker

  @Test
  public void testClosedPlainStatementMethodsThrowRight() {
    ThrowsClosedBulkChecker<Statement> checker =
        new ClosedPlainStatementChecker(Statement.class, closedPlainStmtOfOpenConn);

    checker.testAllMethods();

    if (checker.hadAnyFailures()) {
      fail("Already-closed exception error(s): \n" + checker.getReport());
    }
  }

  private static class ClosedPreparedStatementChecker
      extends ThrowsClosedBulkChecker<PreparedStatement> {

    private static final String PREPAREDSTATEMENT_CLOSED_MESSAGE =
        "PreparedStatement is already closed.";

    ClosedPreparedStatementChecker(Class<PreparedStatement> intf,
                                   PreparedStatement jdbcObject) {
      super(intf, jdbcObject, PREPAREDSTATEMENT_CLOSED_MESSAGE);
    }

    @Override
    protected boolean isOkaySpecialCaseException(Method method, Throwable cause) {
      final boolean result;
      if (super.isOkaySpecialCaseException(method, cause)) {
        result = true;
      } else if (NullPointerException.class == cause.getClass()
                 && (method.getName().equals("enquoteIdentifier")
                     || method.getName().equals("enquoteLiteral")
                     || method.getName().equals("enquoteNCharLiteral")
                     || method.getName().equals("isSimpleIdentifier"))) {
        result = true;
      } else {
        result = false;
      }

      return result;
    }
  } // class closedPreparedStmtOfOpenConnChecker

  @Test
  public void testclosedPreparedStmtOfOpenConnMethodsThrowRight() {
    ThrowsClosedBulkChecker<PreparedStatement> checker =
        new ClosedPreparedStatementChecker(PreparedStatement.class,
                                           closedPreparedStmtOfOpenConn);

    checker.testAllMethods();

    if (checker.hadAnyFailures()) {
      fail("Already-closed exception error(s): \n" + checker.getReport());
    }
  }

  private static class ClosedResultSetChecker
      extends ThrowsClosedBulkChecker<ResultSet> {

    private static final String RESULTSET_CLOSED_MESSAGE =
        "ResultSet is already closed.";

    ClosedResultSetChecker(Class<ResultSet> intf, ResultSet jdbcObject) {
      super(intf, jdbcObject, RESULTSET_CLOSED_MESSAGE);
    }
  } // class ClosedResultSetChecker

  @Test
  public void testClosedResultSetMethodsThrowRight1() {
    ThrowsClosedBulkChecker<ResultSet> checker =
        new ClosedResultSetChecker(ResultSet.class, closedResultSetOfClosedStmt);

    checker.testAllMethods();

    if (checker.hadAnyFailures()) {
      fail("Already-closed exception error(s): \n" + checker.getReport());
    }
  }

  @Test
  public void testClosedResultSetMethodsThrowRight2() {
    ThrowsClosedBulkChecker<ResultSet> checker =
        new ClosedResultSetChecker(ResultSet.class, closedResultSetOfOpenStmt);

    checker.testAllMethods();

    if (checker.hadAnyFailures()) {
      fail("Already-closed exception error(s): \n" + checker.getReport());
    }
  }


  private static class ClosedResultSetMetaDataChecker
      extends ThrowsClosedBulkChecker<ResultSetMetaData> {

    private static final String RESULTSETMETADATA_CLOSED_MESSAGE =
        "ResultSetMetaData's ResultSet is already closed.";

    ClosedResultSetMetaDataChecker(Class<ResultSetMetaData> intf,
                                   ResultSetMetaData jdbcObject) {
      super(intf, jdbcObject, RESULTSETMETADATA_CLOSED_MESSAGE);
    }
  } // class ClosedResultSetMetaDataChecker

  @Test
  public void testClosedResultSetMetaDataMethodsThrowRight1() {
    ThrowsClosedBulkChecker<ResultSetMetaData> checker =
        new ClosedResultSetMetaDataChecker(ResultSetMetaData.class,
                                           resultSetMetaDataOfClosedResultSet);

    checker.testAllMethods();

    if (checker.hadAnyFailures()) {
      fail("Already-closed exception error(s): \n" + checker.getReport());
    }
  }

  @Test
  public void testClosedResultSetMetaDataMethodsThrowRight2() {
    ThrowsClosedBulkChecker<ResultSetMetaData> checker =
        new ClosedResultSetMetaDataChecker(ResultSetMetaData.class,
                                           resultSetMetaDataOfClosedStmt);

    checker.testAllMethods();

    if (checker.hadAnyFailures()) {
      fail("Already-closed exception error(s): \n" + checker.getReport());
    }
  }


  private static class ClosedDatabaseMetaDataChecker
      extends ThrowsClosedBulkChecker<DatabaseMetaData> {

    private static final String DATABASEMETADATA_CLOSED_MESSAGE =
        "DatabaseMetaData's Connection is already closed.";

    ClosedDatabaseMetaDataChecker(Class<DatabaseMetaData> intf,
                                  DatabaseMetaData jdbcObject) {
      super(intf, jdbcObject, DATABASEMETADATA_CLOSED_MESSAGE);
    }

    @Override
    protected boolean isOkayNonthrowingMethod(Method method) {
      return
          super.isOkayNonthrowingMethod(method)
          || method.getName().equals("getDriverMajorVersion")
          || method.getName().equals("getDriverMinorVersion")
          || method.getName().equals("getConnection")
          // TODO: New Java 8 methods not implemented in Avatica.
          || method.getName().equals("getMaxLogicalLobSize")
          || method.getName().equals("supportsRefCursors")
          // New Java 9 methods not implemented in Avatica.
          || method.getName().equals("supportsSharding");
    }

    @Override
    protected boolean isOkaySpecialCaseException(Method method, Throwable cause) {
      final boolean result;
      if (super.isOkaySpecialCaseException(method, cause)) {
        result = true;
      }
      else if (RuntimeException.class == cause.getClass()
               && normalClosedExceptionText.equals(cause.getMessage())
               && method.getName().equals("getResultSetHoldability")) {
        // Special good-enough case--we had to use RuntimeException for now.
        result = true;
      }
      else {
        result = false;
      }
      return result;
    }
  } // class ClosedDatabaseMetaDataChecker


  @Test
  public void testClosedDatabaseMetaDataMethodsThrowRight() {
    ThrowsClosedBulkChecker<DatabaseMetaData> checker =
        new ClosedDatabaseMetaDataChecker(DatabaseMetaData.class,
                                          databaseMetaDataOfClosedConn);

    checker.testAllMethods();

    if (checker.hadAnyFailures()) {
      fail("Already-closed exception error(s): \n" + checker.getReport());
    }
  }
}
