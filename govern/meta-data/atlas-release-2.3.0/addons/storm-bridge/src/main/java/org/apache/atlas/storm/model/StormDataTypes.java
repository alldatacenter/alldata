/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.storm.model;


/**
 * Storm Data Types for model and hook.
 */
public enum StormDataTypes {

    // Topology Classes
    STORM_TOPOLOGY,  // represents the topology containing the DAG

    STORM_NODE,  // base abstraction for producer and processor
    STORM_SPOUT, // data producer node having only outputs
    STORM_BOLT,  // data processing node having both inputs and outputs

    // Data Sets
    KAFKA_TOPIC,  // kafka data set
    JMS_TOPIC,  // jms data set
    HBASE_TABLE,  // hbase table data set
    ;

    public String getName() {
        return name().toLowerCase();
    }
}
