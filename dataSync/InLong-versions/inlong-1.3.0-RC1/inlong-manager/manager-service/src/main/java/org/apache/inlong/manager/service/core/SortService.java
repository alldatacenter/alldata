/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.service.core;

import org.apache.inlong.common.pojo.sortstandalone.SortClusterResponse;
import org.apache.inlong.common.pojo.sdk.SortSourceConfigResponse;

/**
 * Sort Service
 */
public interface SortService {
    /**
     * Get sort cluster config.
     *
     * <p>For a specific sort cluster, there are a series of tasks that defined how dataflow into and
     * out from sort.</p>
     *
     * <p>The param of md5 represents the md5 value of last update response.
     * if the md5 is same with the newest one, which means all configs are not updated,
     * the detailed config in response will be <b>NULL</b>.</p>
     *
     * @param clusterName Name of sort cluster.
     * @param md5 Last update md5.
     * @return Response of sort cluster config {@link SortClusterResponse}
     */
    SortClusterResponse getClusterConfig(String clusterName, String md5);

    /**
     * Get sort source config.
     *
     * <p>Interface that acquires the config source SDK for a specific task.
     *
     * <p>The param of md5 represents the md5 value of last update response. * if the md5 is same
     * with the newest one, which means all configs are not updated, * the detailed config in
     * response will be <b>NULL</b>.
     *
     * @param clusterName Name of sort cluster.
     * @param sortTaskId Task id.
     * @param md5 Last update md5.
     * @return Response of sort cluster config
     */
    SortSourceConfigResponse getSourceConfig(String clusterName, String sortTaskId, String md5);
}
