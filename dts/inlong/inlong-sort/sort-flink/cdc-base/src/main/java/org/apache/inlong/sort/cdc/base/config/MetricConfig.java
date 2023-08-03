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

package org.apache.inlong.sort.cdc.base.config;

import java.io.Serializable;
import java.util.List;

/** The mertic configuration which offers basic metric configuration. **/
public interface MetricConfig extends Serializable {

    /**
     * getInlongMetric
     *
     * @return a label of inlong metric
     */
    String getInlongMetric();

    /**
     * getInlongAudit
     *
     * @return an address of inlong audit
     */
    String getInlongAudit();

    /**
     * getMetricLabelList
     *
     * @return metric label list of each connector.
     * eg: oracle metric label list is [DATABASE, SCHEMA, TABLE]
     */
    List<String> getMetricLabelList();

}
