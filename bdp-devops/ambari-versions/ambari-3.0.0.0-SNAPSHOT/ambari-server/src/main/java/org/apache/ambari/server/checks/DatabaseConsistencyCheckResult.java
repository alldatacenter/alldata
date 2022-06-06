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
package org.apache.ambari.server.checks;

/**
 * Enum representing the possible outcomes of the on-start database consistency check.
 *
 * <p><b>IMPORTANT:</b></p>
 * <ul>
 *   <li>Outcomes are ordered by severity, the program relies on this.</li>
 *   <li>The check result is logged to the standard output and the server startup python script relies
 *       on them. When changing the values, please make sure the startup scripts are changed accordingly!</li>
 * </ul>

 */
public enum DatabaseConsistencyCheckResult {
  DB_CHECK_SUCCESS,
  DB_CHECK_WARNING,
  DB_CHECK_ERROR;

  /**
   * @return a boolean indicating that the result is has least warning severity
   */
  public boolean isErrorOrWarning() {
    return this == DB_CHECK_WARNING || this == DB_CHECK_ERROR;
  }

  /**
   * @return a boolean indicating that the result is error
   */
  public boolean isError() {
    return this == DB_CHECK_ERROR;
  }

}
