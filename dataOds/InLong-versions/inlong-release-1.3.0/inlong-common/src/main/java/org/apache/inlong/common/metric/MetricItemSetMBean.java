/*
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

package org.apache.inlong.common.metric;

import javax.management.MXBean;
import java.util.List;

/**
 * MetricItemSetMBean<br>
 * Provide access interface of metric items with JMX.<br>
 * Decouple between metric item and monitor system, in particular scene, <br>
 * inlong can depend on user-defined monitor system.
 */
@MXBean
public interface MetricItemSetMBean {

    String METHOD_SNAPSHOT = "snapshot";

    /**
     * snapshot
     *
     * @return get snapshot all metric of item, CountMetric will get metric value and set 0 to value, <br>
     *         GaugeMetric will only get metric value.
     */
    List<MetricItem> snapshot();

}
