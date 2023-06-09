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
package org.apache.drill.exec.work.foreman;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.drill.exec.work.filter.RuntimeFilterRouter;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.logical.LogicalPlan;
import org.apache.drill.common.logical.PlanProperties.Generator.ResultMode;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.exception.OptimizerException;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.opt.BasicOptimizer;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.physical.base.FragmentRoot;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.planner.fragment.Fragment;
import org.apache.drill.exec.planner.fragment.MakeFragmentsVisitor;
import org.apache.drill.exec.planner.sql.DirectPlan;
import org.apache.drill.exec.planner.sql.DrillSqlWorker;
import org.apache.drill.exec.proto.BitControl.PlanFragment;
import org.apache.drill.exec.proto.ExecProtos.FragmentHandle;
import org.apache.drill.exec.proto.ExecProtos.ServerPreparedStatementState;
import org.apache.drill.exec.proto.GeneralRPCProtos.Ack;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.proto.UserBitShared.QueryResult;
import org.apache.drill.exec.proto.UserBitShared.QueryResult.QueryState;
import org.apache.drill.exec.proto.UserProtos.PreparedStatementHandle;
import org.apache.drill.exec.proto.UserProtos.RunQuery;
import org.apache.drill.exec.proto.helper.QueryIdHelper;
import org.apache.drill.exec.rpc.BaseRpcOutcomeListener;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.exec.rpc.UserClientConnection;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.server.FailureUtils;
import org.apache.drill.exec.server.options.OptionSet;
import org.apache.drill.exec.testing.ControlsInjector;
import org.apache.drill.exec.testing.ControlsInjectorFactory;
import org.apache.drill.exec.util.Pointer;
import org.apache.drill.exec.work.QueryWorkUnit;
import org.apache.drill.exec.work.WorkManager.WorkerBee;
import org.apache.drill.exec.work.foreman.rm.QueryQueue.QueueTimeoutException;
import org.apache.drill.exec.work.foreman.rm.QueryQueue.QueryQueueException;
import org.apache.drill.exec.work.foreman.rm.QueryResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import static org.apache.drill.exec.server.FailureUtils.EXIT_CODE_HEAP_OOM;

/**
 * Foreman manages all the fragments (local and remote) for a single query where this
 * is the driving/root node.
 *
 * The flow is as follows:
 * <ul>
 * <li>While Foreman is initialized query is in preparing state.</li>
 * <li>Foreman is submitted as a runnable.</li>
 * <li>Runnable does query planning.</li>
 * <li>Runnable submits query to be enqueued.</li>
 * <li>The Runnable's run() completes, but the Foreman stays around to listen to state changes.</li>
 * <li>Once query is enqueued, starting fragments are sent out.</li>
 * <li>Status listener are activated</li>
 * <li>Foreman listens for state change messages.</li>
 * <li>State change messages can drive the state to FAILED or CANCELED, in which case
 *   messages are sent to running fragments to terminate.</li>
 * <li>When all fragments is completed, state change messages drive the state to COMPLETED.</li>
 * </ul>
 */

public class Foreman implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(Foreman.class);
  private static final Logger queryLogger = LoggerFactory.getLogger("query.logger");
  private static final ControlsInjector injector = ControlsInjectorFactory.getInjector(Foreman.class);

  public enum ProfileOption { SYNC, ASYNC, NONE }

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final QueryId queryId;
  private final String queryIdString;
  private final RunQuery queryRequest;
  private final QueryContext queryContext;
  private final QueryManager queryManager; // handles lower-level details of query execution
  private final DrillbitContext drillbitContext;
  private final UserClientConnection initiatingClient; // used to send responses
  private boolean resume;

  private final QueryResourceManager queryRM;

  private final ResponseSendListener responseListener = new ResponseSendListener();
  private final GenericFutureListener<Future<Void>> closeListener = future -> cancel();
  private final Future<Void> closeFuture;
  private final FragmentsRunner fragmentsRunner;
  private final QueryStateProcessor queryStateProcessor;

  private String queryText;

  private RuntimeFilterRouter runtimeFilterRouter;
  private final boolean enableRuntimeFilter;

  /**
   * Constructor. Sets up the Foreman, but does not initiate any execution.
   *
   * @param bee work manager (runs fragments)
   * @param drillbitContext drillbit context
   * @param connection connection
   * @param queryId the id for the query
   * @param queryRequest the query to execute
   */
  public Foreman(final WorkerBee bee, final DrillbitContext drillbitContext,
      final UserClientConnection connection, final QueryId queryId, final RunQuery queryRequest) {
    this.queryId = queryId;
    this.queryIdString = QueryIdHelper.getQueryId(queryId);
    this.queryRequest = queryRequest;
    this.drillbitContext = drillbitContext;
    this.initiatingClient = connection;
    this.closeFuture = initiatingClient.getClosureFuture();
    closeFuture.addListener(closeListener);

    // Apply AutoLimit on resultSet (Usually received via REST APIs)
    final int autoLimit = queryRequest.getAutolimitRowcount();
    if (autoLimit > 0) {
      connection.getSession().getOptions().setLocalOption(ExecConstants.QUERY_MAX_ROWS, autoLimit);
    }
    this.queryContext = new QueryContext(connection.getSession(), drillbitContext, queryId);
    this.queryManager = new QueryManager(queryId, queryRequest, drillbitContext.getStoreProvider(),
        drillbitContext.getClusterCoordinator(), this);
    this.queryRM = drillbitContext.getResourceManager().newQueryRM(this);
    this.fragmentsRunner = new FragmentsRunner(bee, initiatingClient, drillbitContext, this);
    this.queryStateProcessor = new QueryStateProcessor(queryIdString, queryManager, drillbitContext, new ForemanResult());
    this.enableRuntimeFilter = queryContext.getOptions().getOption(ExecConstants.HASHJOIN_ENABLE_RUNTIME_FILTER_KEY).bool_val;
  }

  /**
   * @return query id
   */
  public QueryId getQueryId() {
    return queryId;
  }

  /**
   * @return current query state
   */
  public QueryState getState() {
    return queryStateProcessor.getState();
  }

  /**
   * @return sql query text of the query request
   */
  public String getQueryText() {
    return queryText;
  }

  /**
   * Get the QueryContext created for the query.
   *
   * @return the QueryContext
   */
  public QueryContext getQueryContext() {
    return queryContext;
  }

  /**
   * Get the QueryManager created for the query.
   *
   * @return the QueryManager
   */
  public QueryManager getQueryManager() {
    return queryManager;
  }

  /**
   * Cancel the query (move query in cancellation requested state).
   * Query execution will be canceled once possible.
   */
  public void cancel() {
    logger.debug("Cancel Foreman");
    queryStateProcessor.cancel();
  }

  /**
   * Adds query status in the event queue to process it when foreman is ready.
   *
   * @param state new query state
   * @param exception exception if failure has occurred
   */
  public void addToEventQueue(QueryState state, Exception exception) {
    queryStateProcessor.addToEventQueue(state, exception);
  }

  /**
   * Resume the query. Regardless of the current state, this method sends a resume signal to all fragments.
   * This method can be called multiple times.
   */
  public void resume() {
    resume = true;
    // resume all pauses through query context
    queryContext.getExecutionControls().unpauseAll();
    // resume all pauses through all fragment contexts
    queryManager.unpauseExecutingFragments(drillbitContext);
  }

  /**
   * Called by execution pool to do query setup, and kick off remote execution.
   *
   * <p>Note that completion of this function is not the end of the Foreman's role
   * in the query's lifecycle.
   */
  @Override
  public void run() {
    // rename the thread we're using for debugging purposes
    final Thread currentThread = Thread.currentThread();
    final String originalName = currentThread.getName();
    currentThread.setName(queryIdString + ":foreman");
    try {
      /*
       Check if the foreman is ONLINE. If not don't accept any new queries.
       */
      if (!drillbitContext.isForemanOnline()) {
        throw new ForemanException("Query submission failed since Foreman is shutting down.");
      }
    } catch (ForemanException e) {
      logger.debug("Failure while submitting query", e);
      queryStateProcessor.addToEventQueue(QueryState.FAILED, e);
    }

    queryText = queryRequest.getPlan();
    queryStateProcessor.moveToState(QueryState.PLANNING, null);

    try {
      injector.injectChecked(queryContext.getExecutionControls(), "run-try-beginning", ForemanException.class);

      // convert a run query request into action
      switch (queryRequest.getType()) {
      case LOGICAL:
        parseAndRunLogicalPlan(queryRequest.getPlan());
        break;
      case PHYSICAL:
        parseAndRunPhysicalPlan(queryRequest.getPlan());
        break;
      case SQL:
        final String sql = queryRequest.getPlan();
        // log query id, username and query text before starting any real work. Also, put
        // them together such that it is easy to search based on query id
        logger.info("Query text for query with id {} issued by {}: {}", queryIdString,
            queryContext.getQueryUserName(), sql);
        runSQL(sql);
        break;
      case EXECUTION:
        runFragment(queryRequest.getFragmentsList());
        break;
      case PREPARED_STATEMENT:
        runPreparedStatement(queryRequest.getPreparedStatementHandle());
        break;
      default:
        throw new IllegalStateException();
      }
      injector.injectChecked(queryContext.getExecutionControls(), "run-try-end", ForemanException.class);
    } catch (ForemanException | UserException e) {
      queryStateProcessor.moveToState(QueryState.FAILED, e);
    } catch (OutOfMemoryError | OutOfMemoryException e) {
      if (FailureUtils.isDirectMemoryOOM(e)) {
        queryStateProcessor.moveToState(QueryState.FAILED, UserException.memoryError(e).build(logger));
      } else {
        /*
         * FragmentExecutors use a DrillbitStatusListener to watch out for the death of their query's Foreman. So, if we
         * die here, they should get notified about that, and cancel themselves; we don't have to attempt to notify
         * them, which might not work under these conditions.
         */
        FailureUtils.unrecoverableFailure(e, "Unable to handle out of memory condition in Foreman.", EXIT_CODE_HEAP_OOM);
      }
    } catch (Throwable ex) {
      queryStateProcessor.moveToState(QueryState.FAILED,
          new ForemanException("Unexpected exception during fragment initialization: " + ex.getMessage(), ex));
    } finally {
      // restore the thread's original name
      currentThread.setName(originalName);
    }

    /*
     * Note that despite the run() completing, the Foreman continues to exist, and receives
     * events (indirectly, through the QueryManager's use of stateListener), about fragment
     * completions. It won't go away until everything is completed, failed, or cancelled.
     */
  }

  /**
   * While one fragments where sanding out, other might have been completed. We don't want to process completed / failed
   * events until all fragments are sent out. This method triggers events processing when all fragments were sent out.
   */
  public void startProcessingEvents() {
    queryStateProcessor.startProcessingEvents();

    // If we received the resume signal before fragments are setup, the first call does not actually resume the
    // fragments. Since setup is done, all fragments must have been delivered to remote nodes. Now we can resume.
    if (resume) {
      resume();
    }
  }

  private ProfileOption getProfileOption(QueryContext queryContext) {
    if (queryContext.isSkipProfileWrite()) {
      return ProfileOption.NONE;
    }
    OptionSet options = queryContext.getOptions();
    if (!options.getBoolean(ExecConstants.ENABLE_QUERY_PROFILE_OPTION)) {
      return ProfileOption.NONE;
    }
    if (options.getBoolean(ExecConstants.QUERY_PROFILE_DEBUG_OPTION)) {
      return ProfileOption.SYNC;
    } else {
      return ProfileOption.ASYNC;
    }
  }

  private void parseAndRunLogicalPlan(final String json) throws ExecutionSetupException {
    LogicalPlan logicalPlan;
    try {
      logicalPlan = drillbitContext.getPlanReader().readLogicalPlan(json);
    } catch (final IOException e) {
      throw new ForemanException("Failure parsing logical plan.", e);
    }

    if (logicalPlan.getProperties().resultMode == ResultMode.LOGICAL) {
      throw new ForemanException(
          "Failure running plan.  You requested a result mode of LOGICAL and submitted a logical plan.  In this case you're output mode must be PHYSICAL or EXEC.");
    }

    log(logicalPlan);

    final PhysicalPlan physicalPlan = convert(logicalPlan);

    if (logicalPlan.getProperties().resultMode == ResultMode.PHYSICAL) {
      returnPhysical(physicalPlan);
      return;
    }

    log(physicalPlan);
    runPhysicalPlan(physicalPlan);
  }

  private void log(final LogicalPlan plan) {
    if (logger.isDebugEnabled()) {
      logger.debug("Logical {}", plan.unparse(queryContext.getLpPersistence()));
    }
  }

  private void log(final PhysicalPlan plan) {
    if (logger.isDebugEnabled()) {
      try {
        final String planText = queryContext.getLpPersistence().getMapper().writeValueAsString(plan);
        logger.debug("Physical {}", planText);
      } catch (final IOException e) {
        logger.warn("Error while attempting to log physical plan.", e);
      }
    }
  }

  private void returnPhysical(final PhysicalPlan plan) throws ExecutionSetupException {
    final String jsonPlan = plan.unparse(queryContext.getLpPersistence().getMapper().writer());
    runPhysicalPlan(DirectPlan.createDirectPlan(queryContext, new PhysicalFromLogicalExplain(jsonPlan)));
  }

  public static class PhysicalFromLogicalExplain {
    public final String json;

    public PhysicalFromLogicalExplain(final String json) {
      this.json = json;
    }
  }

  private void parseAndRunPhysicalPlan(final String json) throws ExecutionSetupException {
    try {
      final PhysicalPlan plan = drillbitContext.getPlanReader().readPhysicalPlan(json);
      runPhysicalPlan(plan);
    } catch (final IOException e) {
      throw new ForemanSetupException("Failure while parsing physical plan.", e);
    }
  }

  private void runPhysicalPlan(final PhysicalPlan plan) throws ExecutionSetupException {
    runPhysicalPlan(plan, null);
  }

  private void runPhysicalPlan(final PhysicalPlan plan, Pointer<String> textPlan) throws ExecutionSetupException {
    validatePlan(plan);

    queryRM.visitAbstractPlan(plan);
    final QueryWorkUnit work = getQueryWorkUnit(plan, queryRM);
    if (enableRuntimeFilter) {
      runtimeFilterRouter = new RuntimeFilterRouter(work, drillbitContext);
      runtimeFilterRouter.collectRuntimeFilterParallelAndControlInfo();
    }
    if (textPlan != null) {
      queryManager.setPlanText(textPlan.value);
    }
    queryRM.visitPhysicalPlan(work);
    queryRM.setCost(plan.totalCost());
    queryManager.setTotalCost(plan.totalCost());
    work.applyPlan(drillbitContext.getPlanReader());
    logWorkUnit(work);

    fragmentsRunner.setFragmentsInfo(work.getFragments(), work.getRootFragment(), work.getRootOperator());

    startQueryProcessing();
  }

  /**
   * This is a helper method to run query based on the list of PlanFragment that were planned
   * at some point of time
   * @param fragmentsList fragment list
   * @throws ExecutionSetupException
   */
  private void runFragment(List<PlanFragment> fragmentsList) throws ExecutionSetupException {
    // need to set QueryId, MinorFragment for incoming Fragments
    PlanFragment rootFragment = null;
    boolean isFirst = true;
    final List<PlanFragment> planFragments = Lists.newArrayList();
    for (PlanFragment myFragment : fragmentsList) {
      final FragmentHandle handle = myFragment.getHandle();
      // though we have new field in the FragmentHandle - parentQueryId
      // it can not be used until every piece of code that creates handle is using it, as otherwise
      // comparisons on that handle fail that causes fragment runtime failure
      final FragmentHandle newFragmentHandle = FragmentHandle.newBuilder().setMajorFragmentId(handle.getMajorFragmentId())
          .setMinorFragmentId(handle.getMinorFragmentId()).setQueryId(queryId)
          .build();
      final PlanFragment newFragment = PlanFragment.newBuilder(myFragment).setHandle(newFragmentHandle).build();
      if (isFirst) {
        rootFragment = newFragment;
        isFirst = false;
      } else {
        planFragments.add(newFragment);
      }
    }

    assert rootFragment != null;

    final FragmentRoot rootOperator;
    try {
      rootOperator = drillbitContext.getPlanReader().readFragmentRoot(rootFragment.getFragmentJson());
    } catch (IOException e) {
      throw new ExecutionSetupException(String.format("Unable to parse FragmentRoot from fragment: %s", rootFragment.getFragmentJson()));
    }
    queryRM.setCost(rootOperator.getCost().getOutputRowCount());

    fragmentsRunner.setFragmentsInfo(planFragments, rootFragment, rootOperator);

    startQueryProcessing();
  }

  /**
   * Enqueues the query and once enqueued, starts sending out query fragments for further execution.
   * Moves query to RUNNING state.
   */
  private void startQueryProcessing() {
    enqueue();
    runFragments();
    queryStateProcessor.moveToState(QueryState.RUNNING, null);
  }

  /**
   * Move query to ENQUEUED state. Enqueues query if queueing is enabled.
   * Foreman run will be blocked until query is enqueued.
   * In case of failures (ex: queue timeout exception) will move query to FAILED state.
   */
  private void enqueue() {
    queryStateProcessor.moveToState(QueryState.ENQUEUED, null);

    try {
      queryRM.admit();
      queryStateProcessor.moveToState(QueryState.STARTING, null);
    } catch (QueueTimeoutException | QueryQueueException e) {
      queryStateProcessor.moveToState(QueryState.FAILED, e);
    } finally {
      String queueName = queryRM.queueName();
      queryManager.setQueueName(queueName == null ? "Unknown" : queueName);
    }
  }

  private void runFragments() {
    try {
      fragmentsRunner.submit();
    } catch (Exception e) {
      queryStateProcessor.moveToState(QueryState.FAILED, e);
    } finally {
      /*
       * Begin accepting external events.
       *
       * Doing this here in the finally clause will guarantee that it occurs. Otherwise, if there
       * is an exception anywhere during setup, it wouldn't occur, and any events that are generated
       * as a result of any partial setup that was done (such as the FragmentSubmitListener,
       * the ResponseSendListener, or an external call to cancel()), will hang the thread that makes the
       * event delivery call.
       *
       * If we do throw an exception during setup, and have already moved to QueryState.FAILED, we just need to
       * make sure that we can't make things any worse as those events are delivered, but allow
       * any necessary remaining cleanup to proceed.
       *
       * Note that cancellations cannot be simulated before this point, i.e. pauses can be injected, because Foreman
       * would wait on the cancelling thread to signal a resume and the cancelling thread would wait on the Foreman
       * to accept events.
       */
      startProcessingEvents();
    }
  }

  /**
   * Helper method to execute the query in prepared statement. Current implementation takes the query from opaque
   * object of the <code>preparedStatement</code> and submits as a new query.
   *
   * @param preparedStatementHandle prepared statement handle
   * @throws ExecutionSetupException
   */
  private void runPreparedStatement(final PreparedStatementHandle preparedStatementHandle)
      throws ExecutionSetupException {
    final ServerPreparedStatementState serverState;

    try {
      serverState =
          ServerPreparedStatementState.PARSER.parseFrom(preparedStatementHandle.getServerInfo());
    } catch (final InvalidProtocolBufferException ex) {
      throw UserException.parseError(ex)
          .message("Failed to parse the prepared statement handle. " +
              "Make sure the handle is same as one returned from create prepared statement call.")
          .build(logger);
    }

    queryText = serverState.getSqlQuery();
    logger.info("Prepared statement query for QueryId {} : {}", queryId, queryText);
    runSQL(queryText);

  }

  private static void validatePlan(final PhysicalPlan plan) throws ForemanSetupException {
    if (plan.getProperties().resultMode != ResultMode.EXEC) {
      throw new ForemanSetupException(String.format(
          "Failure running plan.  You requested a result mode of %s and a physical plan can only be output as EXEC",
          plan.getProperties().resultMode));
    }
  }

  private QueryWorkUnit getQueryWorkUnit(final PhysicalPlan plan,
                                         final QueryResourceManager rm) throws ExecutionSetupException {

    final PhysicalOperator rootOperator = plan.getSortedOperators(false).iterator().next();

    final Fragment rootFragment = rootOperator.accept(MakeFragmentsVisitor.INSTANCE, null);

    return rm.getParallelizer(plan.getProperties().hasResourcePlan).generateWorkUnit(queryContext.getOptions().getOptionList(),
                                                                        queryContext.getCurrentEndpoint(),
                                                                        queryId, queryContext.getOnlineEndpoints(),
                                                                        rootFragment, initiatingClient.getSession(),
                                                                        queryContext.getQueryContextInfo());
  }

  private void logWorkUnit(QueryWorkUnit queryWorkUnit) {
    if (! logger.isTraceEnabled()) {
      return;
    }
    logger.trace(String.format("PlanFragments for query %s \n%s",
        queryId, queryWorkUnit.stringifyFragments()));
  }

  private void runSQL(final String sql) throws ExecutionSetupException {
    final Pointer<String> textPlan = new Pointer<>();
    final PhysicalPlan plan = DrillSqlWorker.getPlan(queryContext, sql, textPlan);
    runPhysicalPlan(plan, textPlan);
  }

  private PhysicalPlan convert(final LogicalPlan plan) throws OptimizerException {
    if (logger.isDebugEnabled()) {
      logger.debug("Converting logical plan {}.", plan.toJsonStringSafe(queryContext.getLpPersistence()));
    }
    return new BasicOptimizer(queryContext, initiatingClient).optimize(
        new BasicOptimizer.BasicOptimizationContext(queryContext), plan);
  }

  public RuntimeFilterRouter getRuntimeFilterRouter() {
    return runtimeFilterRouter;
  }

  /**
   * Manages the end-state processing for Foreman.
   *
   * End-state processing is tricky, because even if a query appears to succeed, but
   * we then encounter a problem during cleanup, we still want to mark the query as
   * failed. So we have to construct the successful result we would send, and then
   * clean up before we send that result, possibly changing that result if we encounter
   * a problem during cleanup. We only send the result when there is nothing left to
   * do, so it will account for any possible problems.
   *
   * The idea here is to make close()ing the ForemanResult do the final cleanup and
   * sending. Closing the result must be the last thing that is done by Foreman.
   */
  public class ForemanResult implements AutoCloseable {
    private QueryState resultState = null;
    private volatile Exception resultException = null;
    private boolean isClosed = false;

    /**
     * Set up the result for a COMPLETED or CANCELED state.
     *
     * <p>Note that before sending this result, we execute cleanup steps that could
     * result in this result still being changed to a FAILED state.
     *
     * @param queryState one of COMPLETED or CANCELED
     */
    public void setCompleted(final QueryState queryState) {
      Preconditions.checkArgument((queryState == QueryState.COMPLETED) || (queryState == QueryState.CANCELED));
      Preconditions.checkState(!isClosed);
      Preconditions.checkState(resultState == null);

      resultState = queryState;
    }

    /**
     * Set up the result for a FAILED state.
     *
     * <p>Failures that occur during cleanup processing will be added as suppressed
     * exceptions.
     *
     * @param exception the exception that led to the FAILED state
     */
    public void setFailed(final Exception exception) {
      Preconditions.checkArgument(exception != null);
      Preconditions.checkState(!isClosed);
      Preconditions.checkState(resultState == null);

      resultState = QueryState.FAILED;
      resultException = exception;
    }

    /**
     * Ignore the current status and force the given failure as current status.
     * NOTE: Used only for testing purposes. Shouldn't be used in production.
     */
    public void setForceFailure(final Exception exception) {
      Preconditions.checkArgument(exception != null);
      Preconditions.checkState(!isClosed);

      resultState = QueryState.FAILED;
      resultException = exception;
    }

    /**
     * Add an exception to the result. All exceptions after the first become suppressed
     * exceptions hanging off the first.
     *
     * @param exception the exception to add
     */
    private void addException(final Exception exception) {
      assert exception != null;

      if (resultException == null) {
        resultException = exception;
      } else {
        resultException.addSuppressed(exception);
      }
    }

    /**
     * Expose the current exception (if it exists). This is useful for secondary reporting to the query profile.
     *
     * @return the current Foreman result exception or null.
     */
    public Exception getException() {
      return resultException;
    }

    /**
     * Close the given resource, catching and adding any caught exceptions via {@link #addException(Exception)}. If an
     * exception is caught, it will change the result state to FAILED, regardless of what its current value.
     *
     * @param autoCloseable
     *          the resource to close
     */
    private void suppressingClose(final AutoCloseable autoCloseable) {
      Preconditions.checkState(!isClosed);
      Preconditions.checkState(resultState != null);

      if (autoCloseable == null) {
        return;
      }

      try {
        autoCloseable.close();
      } catch(final Exception e) {
        /*
         * Even if the query completed successfully, we'll still report failure if we have
         * problems cleaning up.
         */
        resultState = QueryState.FAILED;
        addException(e);
      }
    }

    private void logQuerySummary() {
      try {
        LoggedQuery q = new LoggedQuery(
            queryIdString,
            queryContext.getQueryContextInfo().getDefaultSchemaName(),
            queryText,
            new Date(queryContext.getQueryContextInfo().getQueryStartTime()),
            new Date(System.currentTimeMillis()),
            queryStateProcessor.getState(),
            queryContext.getSession().getCredentials().getUserName(),
            initiatingClient.getRemoteAddress());
        queryLogger.info(MAPPER.writeValueAsString(q));
      } catch (Exception e) {
        logger.error("Failure while recording query information to query log.", e);
      }
    }

    @Override
    public void close() {
      Preconditions.checkState(!isClosed);
      Preconditions.checkState(resultState != null);

      logger.debug(queryIdString + ": cleaning up.");
      injector.injectPause(queryContext.getExecutionControls(), "foreman-cleanup", logger);
      if (enableRuntimeFilter && runtimeFilterRouter != null) {
        runtimeFilterRouter.waitForComplete();
      }
      // remove the channel disconnected listener (doesn't throw)
      closeFuture.removeListener(closeListener);

      // log the query summary
      logQuerySummary();

      // These are straight forward removals from maps, so they won't throw.
      drillbitContext.getWorkBus().removeFragmentStatusListener(queryId);
      drillbitContext.getClusterCoordinator().removeDrillbitStatusListener(queryManager.getDrillbitStatusListener());

      suppressingClose(queryContext);

      /*
       * We do our best to write the latest state, but even that could fail. If it does, we can't write
       * the (possibly newly failing) state, so we continue on anyway.
       *
       * We only need to do this if the resultState differs from the last recorded state
       */
      if (resultState != queryStateProcessor.getState()) {
        suppressingClose(() -> queryStateProcessor.recordNewState(resultState));
      }

      // set query end time before writing final profile
      queryStateProcessor.close();

      /*
       * Construct the response based on the latest resultState. The builder shouldn't fail.
       */
      final QueryResult.Builder resultBuilder = QueryResult.newBuilder()
          .setQueryId(queryId)
          .setQueryState(resultState);
      final UserException uex;
      if (resultException != null) {
        final boolean verbose = queryContext.getOptions().getOption(ExecConstants.ENABLE_VERBOSE_ERRORS_KEY).bool_val;
        if (resultException instanceof UserException) {
          uex = (UserException) resultException;
        } else {
          uex = UserException.systemError(resultException).addIdentity(queryContext.getCurrentEndpoint()).build(logger);
        }
        resultBuilder.addError(uex.getOrCreatePBError(verbose));
      } else {
        uex = null;
      }

      // Debug option: write query profile before sending final results so that
      // the client can be certain the profile exists.
      ProfileOption profileOption = getProfileOption(queryContext);
      if (profileOption == ProfileOption.SYNC) {
        queryManager.writeFinalProfile(uex);
      }

      /*
       * If sending the result fails, we don't really have any way to modify the result we tried to send;
       * it is possible it got sent but the result came from a later part of the code path. It is also
       * possible the connection has gone away, so this is irrelevant because there's nowhere to
       * send anything to.
       */
      try {
        // send whatever result we ended up with
        initiatingClient.sendResult(responseListener, resultBuilder.build());
      } catch(final Exception e) {
        addException(e);
        logger.warn("Exception sending result to client", resultException);
      }

      // Store the final result here so we can capture any error/errorId in the
      // profile for later debugging.
      // Normal behavior is to write the query profile AFTER sending results to the user.
      // The observed
      // user behavior is a possible time-lag between query return and appearance
      // of the query profile in persistent storage. Also, the query might
      // succeed, but the profile never appear if the profile write fails. This
      // behavior is acceptable for an eventually-consistent distributed system.
      // The key benefit is that the client does not wait for the persistent
      // storage write; query completion occurs in parallel with profile
      // persistence.

      if (profileOption == ProfileOption.ASYNC) {
        queryManager.writeFinalProfile(uex);
      }

      // Remove the Foreman from the running query list.
      fragmentsRunner.getBee().retireForeman(Foreman.this);

      try {
        queryContext.close();
      } catch (Exception e) {
        logger.error("Unable to close query context for query {}", QueryIdHelper.getQueryId(queryId), e);
      }

      try {
        queryManager.close();
      } catch (final Exception e) {
        logger.warn("unable to close query manager", e);
      }

      try {
        queryRM.exit();
      } finally {
        isClosed = true;
      }
    }
  }

  /**
   * Listens for the status of the RPC response sent to the user for the query.
   */
  private static class ResponseSendListener extends BaseRpcOutcomeListener<Ack> {
    @Override
    public void failed(final RpcException ex) {
      logger.info("Failure while trying communicate query result to initiating client. " +
          "This would happen if a client is disconnected before response notice can be sent.", ex);
    }

    @Override
    public void interrupted(final InterruptedException e) {
      logger.warn("Interrupted while waiting for RPC outcome of sending final query result to initiating client.");
    }
  }

}
