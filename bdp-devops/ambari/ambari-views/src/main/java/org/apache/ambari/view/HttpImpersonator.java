/**
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
package org.apache.ambari.view;

import java.net.HttpURLConnection;

/**
 * Interface for views to impersonate users over HTTP request.
 *
 * @deprecated  As of release 2.0, replaced by
 *              {@link URLStreamProvider#readAs(String, String, String, java.util.Map, String)}
 */
@Deprecated
public interface HttpImpersonator {

  /**
   * @param conn HTTP connection that will be modified and returned
   * @param type HTTP Request type: GET, PUT, POST, DELETE, etc.
   * @return HTTP Connection object with the "doAs" query param set to the currently logged on user.
   */
  public HttpURLConnection doAs(HttpURLConnection conn, String type);

  /**
   * @param conn HTTP connection that will be modified and returned
   * @param type HTTP Request type: GET, PUT, POST, DELETE, etc.
   * @param username Username to impersonate
   * @param doAsParamName Query param, typically "doAs"
   * @return HTTP Connection object with the doAs query param set to the provider username.
   */
  public HttpURLConnection doAs(HttpURLConnection conn, String type, String username, String doAsParamName);

  /**
   * Returns the result of the HTTP request by setting the "doAs" impersonation for the query param and username
   * in @param impersonatorSetting.
   * @param urlToRead URL to request
   * @param requestType HTTP Request type: GET, PUT, POST, DELETE, etc.
   * @param impersonatorSetting Setting class with default values for username and doAs param name.
   *                           To use different values, call the setters of the object.
   * @return Return a response as a String
   */
  public String requestURL(String urlToRead, String requestType, ImpersonatorSetting impersonatorSetting);
}
