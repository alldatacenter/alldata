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

package org.apache.uniffle.client.api;

import org.apache.uniffle.client.request.RssAccessClusterRequest;
import org.apache.uniffle.client.request.RssAppHeartBeatRequest;
import org.apache.uniffle.client.request.RssApplicationInfoRequest;
import org.apache.uniffle.client.request.RssFetchClientConfRequest;
import org.apache.uniffle.client.request.RssFetchRemoteStorageRequest;
import org.apache.uniffle.client.request.RssGetShuffleAssignmentsRequest;
import org.apache.uniffle.client.request.RssSendHeartBeatRequest;
import org.apache.uniffle.client.response.RssAccessClusterResponse;
import org.apache.uniffle.client.response.RssAppHeartBeatResponse;
import org.apache.uniffle.client.response.RssApplicationInfoResponse;
import org.apache.uniffle.client.response.RssFetchClientConfResponse;
import org.apache.uniffle.client.response.RssFetchRemoteStorageResponse;
import org.apache.uniffle.client.response.RssGetShuffleAssignmentsResponse;
import org.apache.uniffle.client.response.RssSendHeartBeatResponse;

public interface CoordinatorClient {

  RssAppHeartBeatResponse sendAppHeartBeat(RssAppHeartBeatRequest request);

  RssApplicationInfoResponse registerApplicationInfo(RssApplicationInfoRequest request);

  RssSendHeartBeatResponse sendHeartBeat(RssSendHeartBeatRequest request);

  RssGetShuffleAssignmentsResponse getShuffleAssignments(RssGetShuffleAssignmentsRequest request);

  RssAccessClusterResponse accessCluster(RssAccessClusterRequest request);

  RssFetchClientConfResponse fetchClientConf(RssFetchClientConfRequest request);

  RssFetchRemoteStorageResponse fetchRemoteStorage(RssFetchRemoteStorageRequest request);

  String getDesc();

  void close();
}
