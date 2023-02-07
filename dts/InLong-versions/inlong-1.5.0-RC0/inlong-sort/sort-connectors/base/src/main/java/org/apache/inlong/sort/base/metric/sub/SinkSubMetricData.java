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

package org.apache.inlong.sort.base.metric.sub;

import java.util.Map;
import org.apache.inlong.sort.base.metric.SinkMetricData;

/**
 * A collection class for handling sub metrics
 */
public interface SinkSubMetricData {

    /**
     * Get sub sink metric map
     *
     * @return The sub sink metric map
     */
    Map<String, SinkMetricData> getSubSinkMetricMap();
}