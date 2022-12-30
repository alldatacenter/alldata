/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.connector.legacy.redis.core.api;

import com.bytedance.bitsail.connector.legacy.redis.core.Command;

import redis.clients.jedis.exceptions.JedisDataException;

import java.io.Closeable;
import java.util.List;

/**
 * Jedis command pipeline processor
 **/
public interface PipelineProcessor extends Closeable {

  /**
   * @return whether the pipeline is first run or in a retry context
   */
  boolean isFirstRun();

  /**
   * Callback before the bulk is executed.
   */
  void preExecute() throws Exception;

  /**
   * the main process, will submit commands to server
   *
   * @return whether need to retry again, call {{@link #needRetry()}}, can be used by guava-retryer
   */
  boolean run();

  /**
   * Callback after a successful execution of pipeline request.
   * responses contains only successful response or JedisDataException
   */
  void postExecute(final List<Command> requests, final List<Object> responses);

  /**
   * Callback after a failed execution of pipeline request.
   */
  void postExecute(final List<Command> requests, Throwable failure);

  /**
   * Callback after a failed execution of pipeline request. this pipeline will be retried later
   * for example if connection is reset or read timeout, this pipeline need to be retried
   */
  void postExecuteWithRetry(final List<Command> requests, Throwable failure);

  /**
   * @return whether need to retry again
   */
  boolean needRetry();

  /**
   * add initial command to processor, only invoke it in JedisOutputFormat
   * @param command     initial command
   */
  void addInitialCommand(Command command);

  /**
   * add unexpected failed record, which means the job will failed later
   * current this function will handle InterruptException only, any other exceptions will be
   * handled by handleNeedRetriedRecords
   * @param command     failed record
   * @param throwable   the cause
   */
  void handleUnexpectedFailedRecord(Command command, Throwable throwable);

  /**
   * add dirty record, which means the record will not retry later, and will be treated as a dirty record
   * @param command     failed record
   * @param exception   the cause
   */
  @Deprecated
  void handleDirtyRecords(Command command, JedisDataException exception);

  /**
   * add need retried record, will retry later if attempt num not reach MAX_ATTEMPT_NUM
   * @param command     failed record
   * @param exception   the cause
   */
  void handleNeedRetriedRecords(Command command, Throwable exception);

  /**
   * when the process is normally running, there is no log printed. so if the hive data is quite large,
   * our task manager is muted, this will confuse us, whether this job is running or not.
   * So we use this method to log sample.
   *
   * @return
   */
  boolean hitLogSampling();

}