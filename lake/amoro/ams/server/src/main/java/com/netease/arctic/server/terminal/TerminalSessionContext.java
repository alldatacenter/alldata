/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.server.terminal;

import com.clearspring.analytics.util.Lists;
import com.netease.arctic.server.utils.Configurations;
import com.netease.arctic.table.TableMetaStore;
import org.apache.commons.io.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class TerminalSessionContext {
  private static final Logger LOG = LoggerFactory.getLogger(TerminalSessionContext.class);

  private final String sessionId;
  private final TableMetaStore metaStore;

  private final AtomicReference<ExecutionStatus> status = new AtomicReference<>(ExecutionStatus.Created);
  private volatile ExecutionTask task = null;
  private final TerminalSessionFactory factory;
  private final Configurations sessionConfiguration;
  private volatile TerminalSession session;

  private volatile long lastExecutionTime = System.currentTimeMillis();

  final ThreadPoolExecutor threadPool;

  public TerminalSessionContext(
      String sessionId,
      TableMetaStore metaStore,
      ThreadPoolExecutor executor,
      TerminalSessionFactory factory,
      Configurations sessionConfiguration) {
    this.sessionId = sessionId;
    this.metaStore = metaStore;
    this.threadPool = executor;
    this.factory = factory;
    this.sessionConfiguration = sessionConfiguration;
  }

  public String getSessionId() {
    return this.sessionId;
  }

  public boolean isReadyToExecute() {
    return isStatusReadyToExecute(status.get());
  }

  public boolean isIdleStatus() {
    return isStatusReadyToExecute(status.get());
  }

  private boolean isStatusReadyToExecute(ExecutionStatus status) {
    return ExecutionStatus.Running != status;
  }

  public synchronized void submit(String catalog, String script, int fetchLimit, boolean stopOnError) {
    ExecutionTask task = new ExecutionTask(catalog, script, fetchLimit, stopOnError);
    if (!isReadyToExecute()) {
      throw new IllegalStateException("current session is not ready to execute. status: " + status.get().name());
    }
    status.set(ExecutionStatus.Running);

    CompletableFuture.supplyAsync(task, threadPool)
        .whenComplete((s, e) -> status.compareAndSet(ExecutionStatus.Running, s))
        .thenApply(s -> lastExecutionTime = System.currentTimeMillis());
    this.task = task;

    String poolInfo = "new sql script submit, current thread pool state. [Active: " +
        threadPool.getActiveCount() + ", PoolSize: " + threadPool.getPoolSize() + "]";
    LOG.info(poolInfo);
    task.executionResult.appendLog(poolInfo);
  }

  public synchronized void cancel() {
    if (this.task != null) {
      this.task.cancel();
    }
  }

  public void release() {
    if (this.session != null) {
      this.session.release();
    }
  }

  public ExecutionStatus getStatus() {
    return status.get();
  }

  public synchronized List<String> getLogs() {
    if (task != null) {
      return task.executionResult.getLogs();
    }
    return Lists.newArrayList();
  }

  public synchronized List<StatementResult> getStatementResults() {
    if (task != null) {
      return task.executionResult.getResults();
    }
    return Lists.newArrayList();
  }

  public long lastExecutionTime() {
    return lastExecutionTime;
  }

  public String lastScript() {
    if (this.task == null) {
      return "";
    }
    return this.task.script;
  }

  private synchronized TerminalSession lazyLoadSession(ExecutionTask task) {
    if (session != null && !session.active()) {
      task.executionResult.appendLog("terminal session is not active, release session");
      try {
        session.release();
      } catch (Throwable e) {
        LOG.error("error when release session.");
      } finally {
        session = null;
      }
    }

    if (session == null) {
      task.executionResult.appendLog("terminal session dose not exists. create session first");
      session = factory.create(metaStore, sessionConfiguration);
      task.executionResult.appendLog("create a new terminal session.");
    }
    return session;
  }

  private class ExecutionTask implements Supplier<ExecutionStatus> {

    final String script;

    final ExecutionResult executionResult = new ExecutionResult();

    private final AtomicBoolean canceled = new AtomicBoolean(false);
    private final int fetchLimits;
    private final boolean stopOnError;
    private final String catalog;

    public ExecutionTask(
        String catalog,
        String script,
        int fetchLimits,
        boolean stopOnError) {
      this.catalog = catalog;
      if (script.trim().endsWith(";")) {
        this.script = script;
      } else {
        this.script = script + ";";
      }
      this.fetchLimits = fetchLimits;
      this.stopOnError = stopOnError;
    }

    @Override
    public ExecutionStatus get() {
      try {
        return metaStore.doAs(() -> {
          TerminalSession session = lazyLoadSession(this);
          executionResult.appendLog("fetch terminal session: " + sessionId);
          executionResult.appendLogs(session.logs());
          for (String key : session.configs().keySet()) {
            executionResult.appendLog("session configuration: " + key + " => " + session.configs().get(key));
          }

          return execute(session);
        });
      } catch (Throwable t) {
        LOG.error("something error when execute script. ", t);
        executionResult.appendLog("something error when execute script.");
        executionResult.appendLog(getStackTraceAsString(t));
        return ExecutionStatus.Failed;
      }
    }

    public void cancel() {
      canceled.set(true);
    }

    ExecutionStatus execute(TerminalSession session) throws IOException {
      LineNumberReader reader = new LineNumberReader(new StringReader(script));
      StringBuilder statementBuilder = null;
      String line;
      int no = -1;

      while ((line = reader.readLine()) != null) {
        if (canceled.get()) {
          executionResult.appendLog("execution is canceled. ");
          return ExecutionStatus.Canceled;
        }
        if (statementBuilder == null) {
          statementBuilder = new StringBuilder();
        }
        line = line.trim();
        if (line.length() < 1 || line.startsWith("--")) {
          // ignore blank lines and comments.
          continue;
        } else if (line.endsWith(";")) {
          //TODO: sql split need do more to handle multi sql statement in one line.
          statementBuilder.append(line);
          no = lineNumber(reader, no);

          // drop the semicolon(;) character
          String statement = statementBuilder.substring(0, statementBuilder.length() - 1);
          boolean success = executeStatement(session, statement, no);
          if (!success) {
            if (stopOnError) {
              executionResult.appendLog("execution stopped for error happened and stop-when-error config.");
              return ExecutionStatus.Failed;
            }
          }

          statementBuilder = null;
          no = -1;
        } else {
          statementBuilder.append(line);
          statementBuilder.append(" ");
          no = lineNumber(reader, no);
        }
      }
      return ExecutionStatus.Finished;
    }

    int lineNumber(LineNumberReader reader, int no) {
      if (no < 0) {
        return reader.getLineNumber();
      }
      return no;
    }

    /**
     * @return - false if any exception happened.
     */
    boolean executeStatement(TerminalSession session, String statement, int lineNo) {
      executionResult.appendLog(" ");
      executionResult.appendLog("prepare execute statement, line:" + lineNo);
      executionResult.appendLog(statement);

      TerminalSession.ResultSet rs = null;
      long begin = System.currentTimeMillis();
      try {
        rs = session.executeStatement(catalog, statement);
        executionResult.appendLogs(session.logs());
      } catch (Throwable t) {
        executionResult.appendLogs(session.logs());
        executionResult.appendLog("meet exception during execution.");
        executionResult.appendLog(getStackTraceAsString(t));
        return false;
      }

      if (rs.empty()) {
        long cost = System.currentTimeMillis() - begin;
        executionResult.appendLog("statement execute down, result is empty, execution cost: " + cost + "ms");
        return true;
      } else {
        StatementResult sr = fetchResults(rs, statement, lineNo);
        long cost = System.currentTimeMillis() - begin;
        executionResult.appendResult(sr);
        executionResult.appendLog(
            "statement execute down, fetch rows:" + sr.getDatas().size() + ", execution cost: " + cost + "ms");
        return sr.isSuccess();
      }
    }

    StatementResult fetchResults(TerminalSession.ResultSet rs, String statement, int lineNo) {
      long count = 0;
      StatementResult sr = new StatementResult(statement, lineNo, rs.columns());
      try {
        while (rs.next()) {
          sr.appendRow(rs.rowData());
          count++;
          if (count >= fetchLimits) {
            executionResult.appendLog("meet result set limit " + count + ", ignore rows left.");
            break;
          }
        }
      } catch (Throwable t) {
        executionResult.appendLog("meet exception when fetch result data.");
        String log = getStackTraceAsString(t);
        sr.withExceptionLog(log);
        executionResult.appendLog(log);
      } finally {
        try {
          rs.close();
        } catch (Throwable t) {
          // ignore
        }
      }
      return sr;
    }

    String getStackTraceAsString(Throwable t) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(out);
      t.printStackTrace(ps);
      return new String(out.toByteArray(), Charsets.UTF_8);
    }
  }
}
