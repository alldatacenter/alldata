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
package org.apache.ambari.spi.net;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

/**
 * The {@link HttpURLConnectionProvider} is used as a way to provide
 * {@link HttpURLConnection} instances which are backed by Ambari's truststore,
 * cookie store, and timeout configurations.
 */
public interface HttpURLConnectionProvider {

  /**
   * Gets a {@link HttpURLConnection} which is initialized and ready to read.
   *
   * @param url
   *          the URL to retrieve information from.
   * @param headers
   *          the HTTP headers to use in the request.
   *
   * @return an iniitalized HTTP connection which is ready to read.
   * @throws IOException
   *           if the URL could not be opened.
   */
  HttpURLConnection getConnection(String url, Map<String, List<String>> headers) throws IOException;

}
