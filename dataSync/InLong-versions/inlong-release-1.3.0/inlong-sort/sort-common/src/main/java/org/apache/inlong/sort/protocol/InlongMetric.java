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

package org.apache.inlong.sort.protocol;

/**
 * The class is the abstract of Inlong Metric.
 * We agree that the key of the inlong metric report is `inlong.metric`,
 * and its value is format by `groupId&streamId&nodeId`.
 * If node implements this interface, we will inject the key and value into the corresponding Sort-Connectors
 * during flink sql parser
 */
public interface InlongMetric {

    /**
     * The key of metric, it must be `inlong.metric` here.
     */
    String METRIC_KEY = "inlong.metric";

    /**
     * The value format, it must be `groupId&streamId&nodeId` here.
     * The groupId is the id of Inlong Group
     * The streamId is the id of Inlong Stream
     * The nodeId is the id of Inlong Source or Sink
     */
    String METRIC_VALUE_FORMAT = "%s&%s&%s";

    /**
     * The key of InLong audit, the value should be ip:port&ip:port
     */
    String AUDIT_KEY = "inlong.audit";

}
