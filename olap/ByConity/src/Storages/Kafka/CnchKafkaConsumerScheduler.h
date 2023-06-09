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
#include <Common/config.h>
#if USE_RDKAFKA

#include <CloudServices/CnchWorkerClientPools.h>
#include <Interpreters/Context.h>
#include <Interpreters/VirtualWarehousePool.h>

namespace DB
{
enum KafkaConsumerScheduleMode : uint32_t
{
    Unknown = 0,
    Random = 1,
    Hash = 2,
    LeastConsumers = 3,
    ResourceManager = 4  /// TODO: introduce ResourceManager to Kafka scheduler
};

constexpr auto toString(KafkaConsumerScheduleMode mode)
{
    switch (mode)
    {
        case KafkaConsumerScheduleMode::Random:
            return "Random";
        case KafkaConsumerScheduleMode::Hash:
            return "Hash";
        case KafkaConsumerScheduleMode::LeastConsumers:
            return "LeastConsumers";
        case KafkaConsumerScheduleMode::ResourceManager:
            return "ResourceManager";
        default:
            return "Unknown mode";
    }
}

class IKafkaConsumerScheduler
{
public:
    auto getScheduleMode() { return schedule_mode; }
    auto getScheduleModeName() { return toString(schedule_mode); }

    virtual CnchWorkerClientPtr selectWorkerNode(const String & key, size_t index) = 0;

    virtual bool shouldReschedule(CnchWorkerClientPtr, const String &, size_t)
    {
        return false;
    }

    virtual void resetWorkerClient(CnchWorkerClientPtr)
    {
        LOG_TRACE(log, "Need do nothing for resetting worker client");
    }

protected:
    IKafkaConsumerScheduler(const String & vw_name_, KafkaConsumerScheduleMode schedule_mode_, ContextPtr context_);
    virtual ~IKafkaConsumerScheduler() = default;

    virtual void initOrUpdateWorkerPool();

    String vw_name;
    VirtualWarehouseHandle vw_handle;
    KafkaConsumerScheduleMode schedule_mode;
    ContextPtr global_context;
    Poco::Logger * log;

    const time_t min_running_time_for_reschedule{60};
};

using KafkaConsumerSchedulerPtr = std::shared_ptr<IKafkaConsumerScheduler>;

/** @Name: Random mode class
 * @Description:   Just select worker client in random mode each time
 * @Reschdule:     It is not able to trigger rescheduling for this mode
 * @Useage:        It can be used for global cluster, but it may not able to ensure exactly load balance
 * */
class KafkaConsumerSchedulerRandom : public IKafkaConsumerScheduler
{
public:
    KafkaConsumerSchedulerRandom(const String & vw_name_, KafkaConsumerScheduleMode schedule_mode_, ContextPtr global_context_);
    ~KafkaConsumerSchedulerRandom() override = default;

    CnchWorkerClientPtr selectWorkerNode(const String & key, size_t index) override;
};

/** @Name: Hash mode class
 * @Description:   Create a consistent-hash ring for vw, and the table suffix of consumer will be taken as key to select worker
 * @Reschdule:     If the scheduler chooses a new worker for the running consumer, and the worker has run for a period time
 *                 then the consumer will be rescheduled to new worker client
 * @Useage:        It can be used for global cluster, but it may not able to ensure exactly load balance
 * */
class KafkaConsumerSchedulerHash : public IKafkaConsumerScheduler
{
public:
    KafkaConsumerSchedulerHash(const String & vw_name_, KafkaConsumerScheduleMode schedule_mode_, ContextPtr global_context_);
    ~KafkaConsumerSchedulerHash() override = default;

    CnchWorkerClientPtr selectWorkerNode(const String & key, size_t index) override;

    bool shouldReschedule(CnchWorkerClientPtr current_worker, const String & key, size_t index) override;
};

/** @Name: LeastConsumers mode class
 * @Description:    Select the worker with the least consumers to schedule a new consumer
 * @Reschdule:      If the number of consumers on the worker has more than `avg_cnt * (1 + ration)`,
 *                  the extra consumers will be rescheduled to other workers
 * @Limitation:     Only suitable for exclusive vw with multi consumers, not for global use
 * */
class KafkaConsumerSchedulerLeastConsumers : public IKafkaConsumerScheduler
{
public:
    KafkaConsumerSchedulerLeastConsumers(const String & vw_name_, KafkaConsumerScheduleMode schedule_mode_, ContextPtr global_context_);
    ~KafkaConsumerSchedulerLeastConsumers() override = default;

    void initOrUpdateWorkerPool() override;

    CnchWorkerClientPtr selectWorkerNode(const String & key, size_t index) override;

    bool shouldReschedule(CnchWorkerClientPtr current_worker, const String & key, size_t index) override;
    void resetWorkerClient(CnchWorkerClientPtr) override;

private:
    struct client_status {
        CnchWorkerClientPtr client_ptr;
        size_t consumers_cnt;

        client_status(const CnchWorkerClientPtr client, size_t cnt)
            : client_ptr(client), consumers_cnt(cnt)
        {}

        bool operator<(const client_status & lhs) const {
            return consumers_cnt > lhs.consumers_cnt;
        }
    };
    std::priority_queue<client_status> clients_queue;
    std::unordered_map<CnchWorkerClientPtr, size_t> clients_status_map;

    size_t max_consumers_trigger_reschedule{0};
    const float min_ration_trigger_reschedule{0.2};
};

bool checkHostPortsVecChanged(const HostWithPortsVec & lhs, const HostWithPortsVec & rhs);

}

#endif
