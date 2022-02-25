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
package org.apache.ambari.server.state.alert;

/**
 * Source type refers to how the alert is to be collected.
 */
public enum SourceType {
  /**
   * Source is from ams metric data.
   */
  AMS,

  /**
   * Source is from metric data.
   */
  METRIC,

  /**
   * Source is generated using of a script
   */
  SCRIPT,

  /**
   * Source is a simple port check
   */
  PORT,

  /**
   * Source is an aggregate of a collection of other alert states
   */
  AGGREGATE,

  /**
   * Source is a ratio of two {@link #METRIC} values.
   */
  PERCENT,

  /**
   * Source is an http(s)-style request.
   */
  WEB,

  /**
   * Source is a component state recovery results
   */
  RECOVERY,

  /**
   * A server-side alert.
   */
  SERVER;
}
