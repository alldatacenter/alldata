/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.apache.inlong.sort.base;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;

/**
 * connector base option constant
 */
public final class Constants {

    /**
     * constants for metrics
     */
    public static final String DIRTY_BYTES = "dirtyBytes";

    public static final String DIRTY_RECORDS = "dirtyRecords";

    public static final String NUM_BYTES_OUT = "numBytesOut";

    public static final String NUM_RECORDS_OUT = "numRecordsOut";

    public static final String NUM_BYTES_OUT_PER_SECOND = "numBytesOutPerSecond";

    public static final String NUM_RECORDS_OUT_PER_SECOND = "numRecordsOutPerSecond";

    public static final String NUM_RECORDS_IN = "numRecordsIn";

    public static final String NUM_BYTES_IN = "numBytesIn";

    public static final String NUM_BYTES_IN_PER_SECOND = "numBytesInPerSecond";

    public static final String NUM_RECORDS_IN_PER_SECOND = "numRecordsInPerSecond";
    /**
     * Time span in seconds
     */
    public static final Integer TIME_SPAN_IN_SECONDS = 60;
    /**
     * Stream id used in inlong metric
     */
    public static final String STREAM_ID = "streamId";
    /**
     * Group id used in inlong metric
     */
    public static final String GROUP_ID = "groupId";
    /**
     * Node id used in inlong metric
     */
    public static final String NODE_ID = "nodeId";
    /**
     * It is used for inlong.metric
     */
    public static final String DELIMITER = "&";

    // sort received successfully
    public static final Integer AUDIT_SORT_INPUT = 7;

    // sort send successfully
    public static final Integer AUDIT_SORT_OUTPUT = 8;

    public static final ConfigOption<String> INLONG_METRIC =
        ConfigOptions.key("inlong.metric")
            .stringType()
            .noDefaultValue()
            .withDescription("INLONG GROUP ID + '&' + STREAM ID + '&' + NODE ID");


    public static final ConfigOption<String> INLONG_AUDIT =
        ConfigOptions.key("inlong.audit")
            .stringType()
            .noDefaultValue()
            .withDescription("INLONG AUDIT HOST + '&' + PORT");

    public static final ConfigOption<Boolean> IGNORE_ALL_CHANGELOG =
            ConfigOptions.key("sink.ignore.changelog")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Regard upsert delete as insert kind.");

}
