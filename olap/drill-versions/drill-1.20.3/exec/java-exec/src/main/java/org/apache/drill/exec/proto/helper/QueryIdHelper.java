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
package org.apache.drill.exec.proto.helper;

import java.util.List;
import java.util.UUID;

import org.apache.drill.exec.proto.ExecProtos.FragmentHandle;
import org.apache.drill.exec.proto.UserBitShared.QueryId;

/* Helper class around the QueryId protobuf */
public class QueryIdHelper {

  /* Generate a UUID from the two parts of the queryid */
  public static String getQueryId(final QueryId queryId) {
    return (new UUID(queryId.getPart1(), queryId.getPart2())).toString();
  }

  public static QueryId getQueryIdFromString(final String queryId) {
    final UUID uuid = UUID.fromString(queryId);
    return QueryId.newBuilder().setPart1(uuid.getMostSignificantBits()).setPart2(uuid.getLeastSignificantBits()).build();
  }

  public static String getQueryIdentifier(final FragmentHandle h) {
    return getQueryId(h.getQueryId()) + ":" + h.getMajorFragmentId() + ":" + h.getMinorFragmentId();
  }

  public static String getExecutorThreadName(final FragmentHandle fragmentHandle) {
    return String.format("%s:frag:%s:%s",
        getQueryId(fragmentHandle.getQueryId()),
        fragmentHandle.getMajorFragmentId(), fragmentHandle.getMinorFragmentId());
  }

  public static String getQueryIdentifiers(final QueryId queryId, final int majorFragmentId, final List<Integer> minorFragmentIds) {
    final String fragmentIds = minorFragmentIds.size() == 1 ? minorFragmentIds.get(0).toString() : minorFragmentIds.toString();
    return getQueryId(queryId) + ":" + majorFragmentId + ":" + fragmentIds;
  }

  public static String getFragmentId(final FragmentHandle fragmentHandle) {
    return fragmentHandle.getMajorFragmentId() + ":" + fragmentHandle.getMinorFragmentId();
  }

}
