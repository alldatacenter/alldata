/*
 * Copyright (2022) Bytedance Ltd. and/or its affiliates
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#pragma once

#include <Interpreters/SystemLog.h>
#include <Interpreters/KafkaLog.h>


namespace DB
{

class QueryMetricLog;
class QueryWorkerMetricLog;

// Query metrics definitions
constexpr auto CNCH_SYSTEM_LOG_QUERY_METRICS_TABLE_NAME = "query_metrics";
constexpr auto CNCH_SYSTEM_LOG_QUERY_WORKER_METRICS_TABLE_NAME = "query_worker_metrics";
constexpr auto CNCH_SYSTEM_LOG_KAFKA_LOG_TABLE_NAME = "cnch_kafka_log";

static inline bool isQueryMetricsTable(const String & database, const String & table)
{
    return (database == CNCH_SYSTEM_LOG_DB_NAME || database == DatabaseCatalog::SYSTEM_DATABASE) &&
            (table == CNCH_SYSTEM_LOG_QUERY_METRICS_TABLE_NAME ||
            table == CNCH_SYSTEM_LOG_QUERY_WORKER_METRICS_TABLE_NAME);
}

/** Modified version of SystemLog that flushes data to a CnchMergeTree table.
  * Altering of schema is also possible for columns that are not part of primary/partition keys.
  * Reordering of columns are not supported.
  */
class CnchSystemLogs
{
public:
    CnchSystemLogs(ContextPtr global_context);
    ~CnchSystemLogs();

    std::shared_ptr<CloudKafkaLog> getKafkaLog() const
    {
        std::lock_guard<std::mutex> g(mutex);
        return cloud_kafka_log;
    }

    std::shared_ptr<QueryMetricLog> getQueryMetricLog() const
    {
        std::lock_guard<std::mutex> g(mutex);
        return query_metrics;
    }

    std::shared_ptr<QueryWorkerMetricLog> getQueryWorkerMetricLog() const
    {
        std::lock_guard<std::mutex> g(mutex);
        return query_worker_metrics;
    }

    void shutdown();

private:
    std::shared_ptr<CloudKafkaLog> cloud_kafka_log;
    std::shared_ptr<QueryMetricLog> query_metrics;                /// Used to log query metrics.
    std::shared_ptr<QueryWorkerMetricLog> query_worker_metrics;   /// Used to log query worker metrics.

    int init_time_in_worker{};
    int init_time_in_server{};
    mutable std::mutex mutex;
    template<typename CloudLog>
    bool initInServerForSingleLog(ContextPtr & global_context,
        const String & db,
        const String & tb,
        const String & config_prefix,
        const Poco::Util::AbstractConfiguration & config,
        std::shared_ptr<CloudLog> & cloud_log);

    bool initInServer(ContextPtr global_context);
    bool initInWorker(ContextPtr global_context);

    BackgroundSchedulePool::TaskHolder init_task;
    Poco::Logger * log;

    std::vector<ISystemLog *> logs;
};

constexpr auto QUERY_METRICS_CONFIG_PREFIX = "query_metrics";
constexpr auto QUERY_WORKER_METRICS_CONFIG_PREFIX = "query_worker_metrics";
constexpr auto CNCH_KAFKA_LOG_CONFIG_PREFIX = "cnch_kafka_log";

/// Instead of typedef - to allow forward declaration.
class CloudKafkaLog : public CnchSystemLog<KafkaLogElement>
{
public:
    using CnchSystemLog<KafkaLogElement>::CnchSystemLog;
    void logException(const StorageID & storage_id, String msg, String consumer_id = "");
};

} // end namespace
