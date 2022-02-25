package org.apache.ambari.server.controller.logging;

import java.util.Map;

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public interface LoggingRequestHelper {

  /**
   * Sends a search query request to the LogSearch server
   *
   * @param queryParameters the query parameters to pass to LogSearch
   *
   * @return a LogQueryResponse, containing the results of the search
   */
  LogQueryResponse sendQueryRequest(Map<String, String> queryParameters);

  /**
   * Sends a request to obtain the log file names for a given host
   *
   * @param hostName the host name
   *
   * @return a HostLogFilesResponse, containing include the log file names for components associated
   *         with a hostname
   */
  HostLogFilesResponse sendGetLogFileNamesRequest(String hostName);

  /**
   * Sends a request to obtain the log level counts for a given component on
   *   a given host
   *
   * @param componentName the component name
   * @param hostName the host name
   *
   * @return a LogLevelQueryResponse, containing the log level counts for this
   *         component/host combination
   */
  LogLevelQueryResponse sendLogLevelQueryRequest(String componentName, String hostName);

  /**
   * Appends the required LogSearch query parameters to a base URI
   *
   * @param baseURI the base URI for this request, typically the URI to the
   *                Ambari Integration searchEngine component
   *
   * @param componentName the component name
   * @param hostName the host name
   *
   * @return a URI String that refers to the tail results of
   *         the log file associated with this component/host
   *         combination
   */
  String createLogFileTailURI(String baseURI, String componentName, String hostName);

}
