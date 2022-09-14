/*
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

package org.apache.flink.connectors.tubemq;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;

/**
 * The configuration options for tubemq sources and sink.
 */
public class TubemqOptions {

    public static final ConfigOption<String> SESSION_KEY =
        ConfigOptions.key("session.key")
            .noDefaultValue()
            .withDescription("The session key for this consumer group at startup.");

    public static final ConfigOption<String> TID =
        ConfigOptions.key("topic.tid")
            .noDefaultValue()
            .withDescription("The tid owned this topic.");

    public static final ConfigOption<Integer> MAX_RETRIES =
        ConfigOptions.key("max.retries")
            .defaultValue(5)
            .withDescription("The maximum number of retries when an "
                    + "exception is caught.");

    public static final ConfigOption<Boolean> BOOTSTRAP_FROM_MAX =
        ConfigOptions.key("bootstrap.from.max")
            .defaultValue(false)
            .withDescription("True if consuming from the most recent "
                    + "position when the tubemq source starts.. It only takes "
                    + "effect when the tubemq source does not recover from "
                    + "checkpoints.");

    public static final ConfigOption<String> SOURCE_MAX_IDLE_TIME =
        ConfigOptions.key("source.task.max.idle.time")
            .defaultValue("5min")
            .withDescription("The max time of the source marked as temporarily idle.");

    public static final ConfigOption<String> MESSAGE_NOT_FOUND_WAIT_PERIOD =
        ConfigOptions.key("message.not.found.wait.period")
            .defaultValue("350ms")
            .withDescription("The time of waiting period if tubemq broker return message not found.");
}
