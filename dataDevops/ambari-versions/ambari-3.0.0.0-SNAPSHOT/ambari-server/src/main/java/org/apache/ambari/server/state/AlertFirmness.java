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
package org.apache.ambari.server.state;

/**
 * The {@link AlertFirmness} enum is used to represent whether an alert should
 * be considered as a real alert or whether it could still potentially be a
 * false positive. Alerts which are {@link #SOFT} must have more occurrences in
 * order to rule out the possibility of a false positive.
 */
public enum AlertFirmness {
  /**
   * The alert is a potential false positive and needs more instances to be
   * confirmed.
   */
  SOFT,

  /**
   * The alert is not a potential false-positive.
   */
  HARD;
}
