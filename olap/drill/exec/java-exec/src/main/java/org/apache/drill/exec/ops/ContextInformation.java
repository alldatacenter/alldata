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
package org.apache.drill.exec.ops;

import org.apache.drill.exec.proto.BitControl.QueryContextInformation;
import org.apache.drill.exec.proto.UserBitShared.UserCredentials;
import java.time.Instant;
import java.time.ZoneId;
import org.apache.drill.exec.expr.fn.impl.DateUtility;

/**
 * Provides query context information (such as query start time, query user, default schema etc.) for UDFs.
 */
public class ContextInformation {
  private final String queryUser;
  private final String currentDefaultSchema;
  private final long queryStartTime;
  private final int rootFragmentTimeZone;
  private final String sessionId;

  public ContextInformation(final UserCredentials userCredentials, final QueryContextInformation queryContextInfo) {
    this.queryUser = userCredentials.getUserName();
    this.currentDefaultSchema = queryContextInfo.getDefaultSchemaName();
    this.queryStartTime = queryContextInfo.getQueryStartTime();
    this.rootFragmentTimeZone = queryContextInfo.getTimeZone();
    this.sessionId = queryContextInfo.getSessionId();
  }

  /**
   * @return userName of the user who issued the current query.
   */
  public String getQueryUser() {
    return queryUser;
  }

  /**
   * @return Get the current default schema in user session at the time of this particular query submission.
   */
  public String getCurrentDefaultSchema() {
    return currentDefaultSchema;
  }

  /**
   * @return Query start time in Unix time (ms)
   */
  public long getQueryStartTime() {
    return queryStartTime;
  }

  /**
   * @return Query start time as an Instant
   */
  public Instant getQueryStartInstant() {
    return Instant.ofEpochMilli(queryStartTime);
  }

  /**
   * @return Query time zone as a ZoneId
   */
  public ZoneId getRootFragmentTimeZone() {
    String zoneId = DateUtility.getTimeZone(rootFragmentTimeZone);
    return ZoneId.of(zoneId);
  }

  /**
   * @return Unique id of the user session
   */
  public String getSessionId() {
    return sessionId;
  }

}
