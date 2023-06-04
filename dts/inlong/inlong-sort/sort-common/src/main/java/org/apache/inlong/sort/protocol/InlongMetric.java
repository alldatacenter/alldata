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
 * We agree that the key of the inlong metric report is
 * {@link org.apache.inlong.sort.configuration.Constants#METRICS_GROUP_STREAM_NODE},
 * and its value is format by `groupId&streamId&nodeId`.
 * If node implements this interface, we will inject the key and value into the corresponding Sort-Connectors
 * during flink sql parser
 */
public interface InlongMetric {
}
