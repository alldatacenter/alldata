/*
 * Copyright 2016-2023 ClickHouse, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#pragma once

#include <Core/BaseSettings.h>
#include <Core/Settings.h>


namespace DB
{
class ASTStorage;


#define KAFKA_RELATED_SETTINGS(M) \
    M(String, broker_list, "", "A comma-separated list of brokers for Kafka engine.", 0) \
    M(String, topic_list, "", "A list of Kafka topics.", 0) \
    M(String, group_name, "", "Client group id string. All Kafka consumers sharing the same group.id belong to the same group.", 0) \
    M(String, client_id, "", "Client identifier.", 0) \
    M(UInt64, num_consumers, 1, "The number of consumers per table for Kafka engine.", 0) \
    M(Bool, commit_every_batch, false, "Commit every consumed and handled batch instead of a single commit after writing a whole block", 0) \
    /* default is stream_poll_timeout_ms */ \
    M(Milliseconds, poll_timeout_ms, 0, "Timeout for single poll from Kafka.", 0) \
    /* default is min(max_block_size, kafka_max_block_size)*/ \
    M(UInt64, poll_max_batch_size, 0, "Maximum amount of messages to be polled in a single Kafka poll.", 0) \
    /* default is = max_insert_block_size / kafka_num_consumers  */ \
    M(UInt64, max_block_size, 65536, "Number of row collected by poll(s) for flushing data from Kafka.", 0) \
    /* default is stream_flush_interval_ms */ \
    M(Milliseconds, max_poll_interval_ms, 8000, "Timeout for flushing data from Kafka.", 0) \
    /* those are mapped to format factory settings */ \
    M(String, format, "", "The message format for Kafka engine.", 0) \
    M(Char, row_delimiter, '\0', "The character to be considered as a delimiter in Kafka message.", 0) \
    M(String, schema, "", "Schema identifier (used by schema-based formats) for Kafka engine", 0) \
    M(String, format_schema_path, "", "Path for schema (used by schema-based formats) and usually be hdfs path", 0) \
    M(UInt64, skip_broken_messages, 0, "Skip at least this number of broken messages from Kafka topic per block", 0) \
    M(Bool, thread_per_consumer, false, "Provide independent thread for each consumer", 0) \
    M(HandleKafkaErrorMode, kafka_handle_error_mode, HandleKafkaErrorMode::DEFAULT, "How to handle errors for Kafka engine. Passible values: default, stream.", 0) \
    \
    /* Settings added for Bytedance kafka */ \
    M(String, cluster, "", "Kafka cluster name required by bytedance kafka client", 0) \
    M(String, bytedance_owner, "", "Owner(user) of bytedance Kafka/BMQ", 0) \
    M(UInt64, max_block_bytes_size, 20ull * 1024 * 1024 * 1024, "The maximum block bytes size per consumer for Kafka engine.", 0) \
    M(UInt64, max_partition_fetch_bytes, 10485760, "Max bytes of each partition read from kafka", 0) \
    M(String, unique_group_prefix, "", "Only used as prefix for storing offsets in bytekv to ensure uniqueness for tob", 0) \
    M(String, leader_priority, "0", "The priority in leader election", 0) \
    M(Int64, max_delay_to_yield_leadership, 600, "Minimal absolute delay to yield leadership.", 0) \
    M(String, partition_num, "-1", "Kafka partition number", 0) \
    M(String, shard_count, "1", "The number of shards in ClickHouse cluster", 0) \
    M(Bool, enable_memory_tracker, false, "Enable memory tracker while consuming", 0) \
    M(Bool, enable_transaction, false, "Enable transaction while consuming", 0) \
    M(Bool, enable_memory_table, false, "Enable memory table", 0) \
    M(UInt64, memory_table_min_time, 60, "Memory table minimum time", 0) \
    M(UInt64, memory_table_max_time, 300,"Memory table maximum time", 0) \
    M(UInt64, memory_table_min_rows, 200000,"Memory table minimum rows", 0) \
    M(UInt64, memory_table_max_rows, 10000000,"Memory table maximum rows", 0) \
    M(UInt64, memory_table_min_bytes, 209715200,"Memory table minimum bytes default value 200M", 0) \
    M(UInt64, memory_table_max_bytes, 838860800,"Memory table maximum bytes default value 800M", 0) \
    M(String, memory_table_read_mode, "ALL", "Memory table read mode valid values: ALL, PART, SKIP", 0) \
    M(UInt64, memory_table_queue_size, 2, "Memory table write block queue size", 0) \
    M(Bool, json_aggregate_function_type_base64_encode, false, "Indicate whether the json data of aggregate function type is encoded by base64.", 0) \
    M(Bool, protobuf_enable_multiple_message, true, "Same as 'format_protobuf_enable_multiple_message' in settings", 0) \
    M(Bool, protobuf_default_length_parser, false, "Same as 'format_protobuf_default_length_parser' in settings", 0) \
    M(String, api_version_request, "true", "Librdkafka config: request broker's supported API versions to adjust functionality to available protocol features", 0) \
    M(String, broker_version_fallback, "", "Librdkafka config: older broker versions", 0) \
    M(String, auto_offset_reset, "", "Librdkafka config: action to take when there is no initial offset in offset store or the desired offset is out of range", 0) \
    M(String, extra_librdkafka_config, "", "Extra configuration for librdkafka, in JSON format", 0) \
    M(Bool, librdkafka_enable_debug_log, false, "Enable librdkafka debug level logs", 0) \
    M(String, cnch_vw_write, "vw_write", "VW group name for Kafka consumer task", 0) \
    M(String, cnch_schedule_mode, "random", "Schedule mode for Kafka comsume manager", 0) \
    /** Settings for Unique Table */ \
    M(Bool, enable_unique_partial_update, true, "Whether to use partial column update for INSERT", 0) \



    /** TODO: */
    /* https://github.com/edenhill/librdkafka/blob/master/CONFIGURATION.md */
    /* https://github.com/edenhill/librdkafka/blob/v1.4.2/src/rdkafka_conf.c */

#define LIST_OF_KAFKA_SETTINGS(M) \
    KAFKA_RELATED_SETTINGS(M) \
    FORMAT_FACTORY_SETTINGS(M)

DECLARE_SETTINGS_TRAITS(KafkaSettingsTraits, LIST_OF_KAFKA_SETTINGS)


/** Settings for the Kafka engine.
  * Could be loaded from a CREATE TABLE query (SETTINGS clause).
  */
struct KafkaSettings : public BaseSettings<KafkaSettingsTraits>
{
    void applyKafkaSettingChanges(const SettingsChanges & changes);
    void loadFromQuery(ASTStorage & storage_def);
};

class IAST;
void sortKafkaSettings(IAST & settings_ast);

}
