/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.broker.stats;

import java.util.Map;

/**
 * TrafficService, incoming and outgoing traffic statistics service
 *
 * Supports adding new metric data one by one or in batches,
 * and outputting metric data to a file at specified intervals.
 */
public interface TrafficService {

    /**
     * Close service.
     *
     * @param waitTimeMs  the wait time
     */
    void close(long waitTimeMs);

    /**
     * Add traffic information in batches
     *
     * @param trafficInfos  the traffic information
     */
    void add(Map<String, TrafficInfo> trafficInfos);

    /**
     * Add a traffic information record
     *
     * @param statsKey  the statistical key
     * @param msgCnt    the total message count
     * @param msgSize   the total message size
     */
    void add(String statsKey, long msgCnt, long msgSize);
}
