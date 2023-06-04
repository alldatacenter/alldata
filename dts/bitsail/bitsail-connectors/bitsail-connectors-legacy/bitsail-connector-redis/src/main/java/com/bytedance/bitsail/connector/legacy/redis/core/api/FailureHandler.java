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

/**
 * Failure response handler.
 * Stateless, can use singleton mode.
 **/
public interface FailureHandler {

  /**
   * resolve the JedisDataException, to determine handle policy
   * @param command   the failed command
   * @param failure   the JedisDataException in pipeline
   * @param processor pipeline processor, to store resolve result
   */
  default void handle(Command command, Throwable failure, PipelineProcessor processor) {
    if (failure instanceof JedisDataException) {
      resolve(command, (JedisDataException) failure, processor);
    } else {
      caughtUnexpectedError(command, failure, processor);
    }
  }

  /**
   * resolve the JedisDataException, to determine handle policy
   * @param command   the failed command
   * @param exception the JedisDataException in pipeline
   * @param processor pipeline processor, to store resolve result
   */
  void resolve(Command command, JedisDataException exception, PipelineProcessor processor);


  /**
   * caught unexpected jedis data exception, which mean should fail this job immediately
   * @param command the failed command
   * @param failure the JedisDataException in pipeline
   * @param processor pipeline processor, to store resolve result
   */
  void caughtUnexpectedError(Command command, Throwable failure, PipelineProcessor processor);

}

