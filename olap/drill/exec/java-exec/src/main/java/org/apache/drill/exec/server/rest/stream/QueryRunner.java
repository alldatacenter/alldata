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
package org.apache.drill.exec.server.rest.stream;

import java.io.OutputStream;

import org.apache.calcite.tools.ValidationException;
import org.apache.drill.exec.proto.UserBitShared.QueryType;
import org.apache.drill.exec.server.rest.BaseQueryRunner;
import org.apache.drill.exec.server.rest.QueryWrapper;
import org.apache.drill.exec.server.rest.WebUserConnection;
import org.apache.drill.exec.work.WorkManager;

/**
 * Query runner for streaming JSON results. This version is backward-compatible
 * with pre-Drill 1.19 JSON queries.
 */
public class QueryRunner extends BaseQueryRunner {

  private StreamingHttpConnection userConn;

  public QueryRunner(final WorkManager workManager, final WebUserConnection webUserConnection) {
    super(workManager, webUserConnection);
  }

  public void start(QueryWrapper query) throws ValidationException {
    applyUserName(query.getUserName());
    applyOptions(query.getOptions());
    applyDefaultSchema(query.getDefaultSchema());
    applyRowLimit(query.getAutoLimitRowCount());

    userConn = new StreamingHttpConnection(webUserConnection.resources());
    startQuery(QueryType.valueOf(query.getQueryType()),
        query.getQuery(),
        userConn);
    userConn.onStart(queryId, maxRows);

    // Query is now running in a separate thread, possibly sending
    // the first batch to the user connection.
  }

  public void sendResults(OutputStream output) throws Exception {
    try {
      // Hand output stream to the user connection which will now
      // stream batches
      userConn.outputAvailable(output);

      // Wait for the query to succeed or fail
      try {
        userConn.await();
        userConn.finish();
      } catch (InterruptedException e) {
        // No-op
      }
    } finally {
      // no-op for authenticated user
      webUserConnection.cleanupSession();
    }
  }
}
