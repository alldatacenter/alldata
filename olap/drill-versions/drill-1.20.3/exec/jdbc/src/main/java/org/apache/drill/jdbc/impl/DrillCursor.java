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
package org.apache.drill.jdbc.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.drill.jdbc.DrillStatement;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.apache.calcite.avatica.AvaticaStatement;
import org.apache.calcite.avatica.ColumnMetaData;
import org.apache.calcite.avatica.Meta;
import org.apache.calcite.avatica.Meta.Signature;
import org.apache.calcite.avatica.util.ArrayImpl.Factory;
import org.apache.calcite.avatica.util.Cursor;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.client.DrillClient;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.proto.UserBitShared.QueryResult.QueryState;
import org.apache.drill.exec.proto.UserBitShared.QueryType;
import org.apache.drill.exec.proto.UserProtos.PreparedStatement;
import org.apache.drill.exec.proto.helper.QueryIdHelper;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.RecordBatchLoader;
import org.apache.drill.exec.rpc.ConnectionThrottle;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.exec.rpc.user.UserResultsListener;
import org.apache.drill.exec.store.ischema.InfoSchemaConstants;
import org.apache.drill.jdbc.SchemaChangeListener;
import org.apache.drill.jdbc.SqlTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.drill.shaded.guava.com.google.common.collect.Queues;

public class DrillCursor implements Cursor {

  ////////////////////////////////////////
  // ResultsListener:
  static class ResultsListener implements UserResultsListener {
    private static final Logger logger = LoggerFactory.getLogger(ResultsListener.class);

    private static volatile int nextInstanceId = 1;

    /** (Just for logging.) */
    private final int instanceId;

    private final int batchQueueThrottlingThreshold;

    /** (Just for logging.) */
    private volatile QueryId queryId;

    /** (Just for logging.) */
    private int lastReceivedBatchNumber;
    /** (Just for logging.) */
    private int lastDequeuedBatchNumber;

    private volatile UserException executionFailureException;

    // TODO:  Revisit "completed".  Determine and document exactly what it
    // means.  Some uses imply that it means that incoming messages indicate
    // that the _query_ has _terminated_ (not necessarily _completing_
    // normally), while some uses imply that it's some other state of the
    // ResultListener.  Some uses seem redundant.)
    volatile boolean completed;

    /** Whether throttling of incoming data is active. */
    private final AtomicBoolean throttled = new AtomicBoolean(false);
    private volatile ConnectionThrottle throttle;

    private volatile boolean closed;

    private final CountDownLatch firstMessageReceived = new CountDownLatch(1);

    final LinkedBlockingDeque<QueryDataBatch> batchQueue =
        Queues.newLinkedBlockingDeque();

    private final DrillCursor parent;
    Stopwatch elapsedTimer;

    /**
     * ...
     * @param parent
     *        reference to DrillCursor
     * @param batchQueueThrottlingThreshold
     *        queue size threshold for throttling server
     */
    ResultsListener(DrillCursor parent, int batchQueueThrottlingThreshold) {
      this.parent = parent;
      instanceId = nextInstanceId++;
      this.batchQueueThrottlingThreshold = batchQueueThrottlingThreshold;
      logger.debug("[#{}] Query listener created.", instanceId);
    }

    /**
     * Starts throttling if not currently throttling.
     * @param  throttle  the "throttlable" object to throttle
     * @return  true if actually started (wasn't throttling already)
     */
    private boolean startThrottlingIfNot(ConnectionThrottle throttle) {
      final boolean started = throttled.compareAndSet(false, true);
      if (started) {
        this.throttle = throttle;
        throttle.setAutoRead(false);
      }
      return started;
    }

    /**
     * Stops throttling if currently throttling.
     * @return  true if actually stopped (was throttling)
     */
    private boolean stopThrottlingIfSo() {
      final boolean stopped = throttled.compareAndSet(true, false);
      if (stopped) {
        throttle.setAutoRead(true);
        throttle = null;
      }
      return stopped;
    }

    public void awaitFirstMessage() throws InterruptedException, SQLTimeoutException {
      //Check if a non-zero timeout has been set
      if (parent.timeoutInMilliseconds > 0) {
        //Identifying remaining in milliseconds to maintain a granularity close to integer value of timeout
        long timeToTimeout = parent.timeoutInMilliseconds - parent.elapsedTimer.elapsed(TimeUnit.MILLISECONDS);
        if (timeToTimeout <= 0 || !firstMessageReceived.await(timeToTimeout, TimeUnit.MILLISECONDS)) {
            throw new SqlTimeoutException(TimeUnit.MILLISECONDS.toSeconds(parent.timeoutInMilliseconds));
        }
      } else {
        firstMessageReceived.await();
      }
    }

    private void releaseIfFirst() {
      firstMessageReceived.countDown();
    }

    @Override
    public void queryIdArrived(QueryId queryId) {
      logger.debug("[#{}] Received query ID: {}.",
                    instanceId, QueryIdHelper.getQueryId(queryId));
      this.queryId = queryId;
    }

    @Override
    public void submissionFailed(UserException ex) {
      logger.debug("Received query failure: {} {}", instanceId, ex);
      this.executionFailureException = ex;
      completed = true;
      close();
      logger.info("[#{}] Query failed: ", instanceId, ex);
    }

    @Override
    public void dataArrived(QueryDataBatch result, ConnectionThrottle throttle) {
      lastReceivedBatchNumber++;
      logger.debug("[#{}] Received query data batch #{}: {}.",
                    instanceId, lastReceivedBatchNumber, result);

      // If we're in a closed state, just release the message.
      if (closed) {
        result.release();
        // TODO:  Revisit member completed:  Is ResultListener really completed
        // after only one data batch after being closed?
        completed = true;
        return;
      }

      // We're active; let's add to the queue.
      batchQueue.add(result);

      // Throttle server if queue size has exceed threshold.
      if (batchQueue.size() > batchQueueThrottlingThreshold) {
        if (startThrottlingIfNot(throttle)) {
          logger.debug("[#{}] Throttling started at queue size {}.",
                        instanceId, batchQueue.size());
        }
      }

      releaseIfFirst();
    }

    @Override
    public void queryCompleted(QueryState state) {
      logger.debug("[#{}] Received query completion: {}.", instanceId, state);
      releaseIfFirst();
      completed = true;
    }

    QueryId getQueryId() {
      return queryId;
    }

    /**
     * Gets the next batch of query results from the queue.
     * @return  the next batch, or {@code null} after last batch has been returned
     * @throws UserException
     *         if the query failed
     * @throws InterruptedException
     *         if waiting on the queue was interrupted
     */
    QueryDataBatch getNext() throws UserException, InterruptedException, SQLTimeoutException {
      while (true) {
        if (executionFailureException != null) {
          logger.debug("[#{}] Dequeued query failure exception: {}.",
                        instanceId, executionFailureException);
          throw executionFailureException;
        }
        if (completed && batchQueue.isEmpty()) {
          return null;
        } else {
          QueryDataBatch qdb = batchQueue.poll(50, TimeUnit.MILLISECONDS);
          if (qdb != null) {
            lastDequeuedBatchNumber++;
            logger.debug("[#{}] Dequeued query data batch #{}: {}.",
                          instanceId, lastDequeuedBatchNumber, qdb);

            // Unthrottle server if queue size has dropped enough below threshold:
            if (batchQueue.size() < batchQueueThrottlingThreshold / 2
                 || batchQueue.size() == 0  // (in case threshold < 2)
                ) {
              if (stopThrottlingIfSo()) {
                logger.debug("[#{}] Throttling stopped at queue size {}.",
                              instanceId, batchQueue.size());
              }
            }
            return qdb;
          }

          // Check and throw SQLTimeoutException
          if (parent.timeoutInMilliseconds > 0 && parent.elapsedTimer.elapsed(TimeUnit.MILLISECONDS) >= parent.timeoutInMilliseconds) {
            throw new SqlTimeoutException(TimeUnit.MILLISECONDS.toSeconds(parent.timeoutInMilliseconds));
          }
        }
      }
    }

    void close() {
      logger.debug("[#{}] Query listener closing.", instanceId);
      closed = true;
      if (stopThrottlingIfSo()) {
        logger.debug("[#{}] Throttling stopped at close() (at queue size {}).",
                      instanceId, batchQueue.size());
      }
      while (!batchQueue.isEmpty()) {
        // Don't bother with query timeout, we're closing the cursor
        QueryDataBatch qdb = batchQueue.poll();
        if (qdb != null && qdb.getData() != null) {
          qdb.getData().release();
        }
      }
      // Close may be called before the first result is received and therefore
      // when the main thread is blocked waiting for the result.  In that case
      // we want to unblock the main thread.
      firstMessageReceived.countDown(); // TODO:  Why not call releaseIfFirst as used elsewhere?
      completed = true;
    }
  }

  private static final Logger logger = getLogger(DrillCursor.class);

  /** JDBC-specified string for unknown catalog, schema, and table names. */
  private static final String UNKNOWN_NAME_STRING = "";

  private final DrillConnectionImpl connection;
  private final AvaticaStatement statement;
  private final Meta.Signature signature;

  /** Holds current batch of records (none before first load). */
  private final RecordBatchLoader currentBatchHolder;

  private final ResultsListener resultsListener;
  private SchemaChangeListener changeListener;

  private final DrillAccessorList accessors = new DrillAccessorList();

  /** Schema of current batch (null before first load). */
  private BatchSchema schema;

  /** ... corresponds to current schema. */
  private DrillColumnMetaDataList columnMetaDataList;

  /** Whether loadInitialSchema() has been called. */
  private boolean initialSchemaLoaded;

  /** Whether after first batch.  (Re skipping spurious empty batches.) */
  private boolean afterFirstBatch;

  /**
   * Whether the next call to {@code this.}{@link #next()} should just return
   * {@code true} rather than calling {@link #nextRowInternally()} to try to
   * advance to the next record.
   * <p>
   *   Currently, can be true only for first call to {@link #next()}.
   * </p>
   * <p>
   *   (Relates to {@link #loadInitialSchema()}'s calling
   *   {@link #nextRowInternally()} one "extra" time (extra relative to number
   *   of {@link java.sql.ResultSet#next()} calls) at the beginning to get first batch
   *   and schema before {@code Statement.execute...(...)} even returns.)
   * </p>
   */
  private boolean returnTrueForNextCallToNext;

  /** Whether cursor is after the end of the sequence of records/rows. */
  private boolean afterLastRow;

  private int currentRowNumber = -1;
  /** Zero-based offset of current record in record batch.
   * (Not <i>row</i> number.) */
  private int currentRecordNumber = -1;

  //Track timeout period
  private long timeoutInMilliseconds;
  private Stopwatch elapsedTimer;

  /**
   *
   * @param statement
   * @param signature
   * @throws SQLException
   */
  DrillCursor(DrillConnectionImpl connection, AvaticaStatement statement, Signature signature) throws SQLException {
    this.connection = connection;
    this.statement = statement;
    this.signature = signature;

    DrillClient client = connection.getClient();
    final int batchQueueThrottlingThreshold =
        client.getConfig().getInt(
            ExecConstants.JDBC_BATCH_QUEUE_THROTTLING_THRESHOLD);
    resultsListener = new ResultsListener(this, batchQueueThrottlingThreshold);
    currentBatchHolder = new RecordBatchLoader(client.getAllocator());

    // Set Query Timeout
    logger.debug("Setting timeout as {}", this.statement.getQueryTimeout());
    setTimeout(this.statement.getQueryTimeout());
  }

  protected int getCurrentRecordNumber() {
    return currentRecordNumber;
  }

  public String getQueryId() {
    if (resultsListener.getQueryId() != null) {
      return QueryIdHelper.getQueryId(resultsListener.getQueryId());
    } else {
      return null;
    }
  }

  public boolean isBeforeFirst() {
    return currentRowNumber < 0;
  }

  public boolean isAfterLast() {
    return afterLastRow;
  }

  // (Overly restrictive Avatica uses List<Accessor> instead of List<? extends
  // Accessor>, so accessors/DrillAccessorList can't be of type
  // List<AvaticaDrillSqlAccessor>, and we have to cast from Accessor to
  // AvaticaDrillSqlAccessor in updateColumns().)
  @Override
  public List<Accessor> createAccessors(List<ColumnMetaData> types,
                                        Calendar localCalendar, Factory factory) {
    columnMetaDataList = (DrillColumnMetaDataList) types;
    return accessors;
  }

  synchronized void cleanup() {
    if (resultsListener.getQueryId() != null && ! resultsListener.completed) {
      connection.getClient().cancelQuery(resultsListener.getQueryId());
    }
    resultsListener.close();
    currentBatchHolder.clear();
  }

  long getTimeoutInMilliseconds() {
    return timeoutInMilliseconds;
  }

  //Set the cursor's timeout in seconds
  void setTimeout(int timeoutDurationInSeconds) {
    this.timeoutInMilliseconds = TimeUnit.SECONDS.toMillis(timeoutDurationInSeconds);
    //Starting the timer, since this is invoked via the ResultSet.execute() call
    if (timeoutInMilliseconds > 0) {
      elapsedTimer = Stopwatch.createStarted();
    }
  }

  /**
   * Updates column accessors and metadata from current record batch.
   */
  private void updateColumns() {
    // First update accessors and schema from batch:
    accessors.generateAccessors(this, currentBatchHolder);

    // Extract Java types from accessors for metadata's getColumnClassName:
    final List<Class<?>> getObjectClasses = new ArrayList<>();
    // (Can't use modern for loop because, for some incompletely clear reason,
    // DrillAccessorList blocks iterator() (throwing exception).)
    for (int ax = 0; ax < accessors.size(); ax++) {
      final AvaticaDrillSqlAccessor accessor =
          accessors.get(ax);
      getObjectClasses.add(accessor.getObjectClass());
    }

    // Update metadata for result set.
    columnMetaDataList.updateColumnMetaData(
        InfoSchemaConstants.IS_CATALOG_NAME,
        UNKNOWN_NAME_STRING,  // schema name
        UNKNOWN_NAME_STRING,  // table name
        schema,
        getObjectClasses);

    if (changeListener != null) {
      changeListener.schemaChanged(schema);
    }
  }

  /**
   * ...
   * <p>
   *   Is to be called (once) from {@link #loadInitialSchema} for
   *   {@link DrillResultSetImpl#execute()}, and then (repeatedly) from
   *   {@link #next()} for {@link org.apache.calcite.avatica.AvaticaResultSet#next()}.
   * </p>
   *
   * @return  whether cursor is positioned at a row (false when after end of
   *   results)
   */
  private boolean nextRowInternally() throws SQLException {
    if (currentRecordNumber + 1 < currentBatchHolder.getRecordCount()) {
      // Have next row in current batch--just advance index and report "at a row."
      currentRecordNumber++;
      return true;
    } else {
      // No (more) records in any current batch--try to get first or next batch.
      // (First call always takes this branch.)

      try {
        QueryDataBatch qrb = resultsListener.getNext();

        // (Apparently:)  Skip any spurious empty batches (batches that have
        // zero rows and null data, other than the first batch (which carries
        // the (initial) schema but no rows)).
        if (afterFirstBatch) {
          while (qrb != null
              && (qrb.getHeader().getRowCount() == 0 && qrb.getData() == null)) {
            // Empty message--dispose of and try to get another.
            logger.warn("Spurious batch read: {}", qrb);

            qrb.release();

            qrb = resultsListener.getNext();
          }
        }

        afterFirstBatch = true;

        if (qrb == null) {
          // End of batches--clean up, set state to done, report after last row.

          currentBatchHolder.clear();  // (We load it so we clear it.)
          afterLastRow = true;
          return false;
        } else {
          // Got next (or first) batch--reset record offset to beginning;
          // assimilate schema if changed; set up return value for first call
          // to next().

          currentRecordNumber = 0;

          if (qrb.getHeader().hasAffectedRowsCount()) {
            int updateCount = qrb.getHeader().getAffectedRowsCount();
            int currentUpdateCount = statement.getUpdateCount() == -1 ? 0 : statement.getUpdateCount();
            ((DrillStatement) statement).setUpdateCount(updateCount + currentUpdateCount);
            ((DrillStatement) statement).setResultSet(null);
          }

          final boolean schemaChanged;
          try {
            schemaChanged = currentBatchHolder.load(qrb.getHeader().getDef(), qrb.getData());
          }
          finally {
            qrb.release();
          }
          schema = currentBatchHolder.getSchema();
          if (schemaChanged) {
            updateColumns();
          }

          if (returnTrueForNextCallToNext
              && currentBatchHolder.getRecordCount() == 0) {
            returnTrueForNextCallToNext = false;
          }
          return true;
        }
      }
      catch (UserException e) {
        // A normally expected case--for any server-side error (e.g., syntax
        // error in SQL statement).
        // Construct SQLException with message text from the UserException.
        // TODO:  Map UserException error type to SQLException subclass (once
        // error type is accessible, of course. :-()
        throw new SQLException(e.getMessage(), e);
      }
      catch (InterruptedException e) {
        // Not normally expected--Drill doesn't interrupt in this area (right?)--
        // but JDBC client certainly could.
        throw new SQLException("Interrupted.", e);
      }
      catch (RuntimeException e) {
        throw new SQLException("Unexpected RuntimeException: " + e.toString(), e);
      }
    }
  }

  /**
   * Advances to first batch to load schema data into result set metadata.
   * <p>
   *   To be called once from {@link DrillResultSetImpl#execute()} before
   *   {@link #next()} is called from {@link org.apache.calcite.avatica.AvaticaResultSet#next()}.
   * <p>
   */
  void loadInitialSchema() throws SQLException {
    if (initialSchemaLoaded) {
      throw new IllegalStateException(
          "loadInitialSchema() called a second time");
    }

    assert ! afterLastRow : "afterLastRow already true in loadInitialSchema()";
    assert ! afterFirstBatch : "afterLastRow already true in loadInitialSchema()";
    assert -1 == currentRecordNumber
        : "currentRecordNumber not -1 (is " + currentRecordNumber
          + ") in loadInitialSchema()";
    assert 0 == currentBatchHolder.getRecordCount()
        : "currentBatchHolder.getRecordCount() not 0 (is "
          + currentBatchHolder.getRecordCount() + " in loadInitialSchema()";

    final PreparedStatement preparedStatement;
    if (statement instanceof DrillPreparedStatementImpl) {
      DrillPreparedStatementImpl drillPreparedStatement = (DrillPreparedStatementImpl) statement;
      preparedStatement = drillPreparedStatement.getPreparedStatementHandle();
    } else {
      preparedStatement = null;
    }

    if (preparedStatement != null) {
      connection.getClient().executePreparedStatement(preparedStatement.getServerHandle(), resultsListener);
    }
    else {
      connection.getClient().runQuery(QueryType.SQL, signature.sql, resultsListener);
    }

    try {
      resultsListener.awaitFirstMessage();
    } catch (InterruptedException e) {
      // Preserve evidence that the interruption occurred so that code higher up
      // on the call stack can learn of the interruption and respond to it if it
      // wants to.
      Thread.currentThread().interrupt();

      // Not normally expected--Drill doesn't interrupt in this area (right?)--
      // but JDBC client certainly could.
      throw new SQLException("Interrupted", e);
    }

    returnTrueForNextCallToNext = true;

    nextRowInternally();

    initialSchemaLoaded = true;
  }

  /**
   * Advances this cursor to the next row, if any, or to after the sequence of
   * rows if no next row.
   *
   * @return  whether cursor is positioned at a row (false when after end of
   *   results)
   */
  @Override
  public boolean next() throws SQLException {
    if (! initialSchemaLoaded) {
      throw new IllegalStateException(
          "next() called but loadInitialSchema() was not called");
    }
    assert afterFirstBatch : "afterFirstBatch still false in next()";

    if (afterLastRow) {
      // We're already after end of rows/records--just report that after end.
      return false;
    }
    else if (returnTrueForNextCallToNext) {
      ++currentRowNumber;
      // We have a deferred "not after end" to report--reset and report that.
      returnTrueForNextCallToNext = false;
      return true;
    }
    else {
      accessors.clearLastColumnIndexedInRow();
      boolean res = nextRowInternally();
      if (res) { ++ currentRowNumber; }

      return res;
    }
  }

  public void cancel() {
    close();
  }

  @Override
  public void close() {
    // Clean up result set (to deallocate any buffers).
    cleanup();
    // TODO:  CHECK:  Something might need to set statement.openResultSet to
    // null.  Also, AvaticaResultSet.close() doesn't check whether already
    // closed and skip calls to cursor.close(), statement.onResultSetClose()
  }

  @Override
  public boolean wasNull() throws SQLException {
    return accessors.wasNull();
  }

  public Stopwatch getElapsedTimer() {
    return elapsedTimer;
  }
}
