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

#include <Interpreters/Context.h>
#include <Storages/IStorage.h>
#include <Storages/Kafka/KafkaSettings.h>
#include <Storages/Kafka/KafkaTaskCommand.h>
#include <Storages/Kafka/CnchReadBufferFromKafkaConsumer.h>

namespace DB
{
    using OffsetsMap = std::unordered_map<
            std::pair<std::string, std::uint64_t>,
            std::int64_t,
            PairHash>;


 /*** This class should be the base class for CnchKafka, and mustn't be used as object */
class IStorageCnchKafka : public IStorage, public WithMutableContext
{
public:
    ~IStorageCnchKafka() override = default;

    /// Keep the 'getName' to be pure virtual one
    /// String getName() const override {return "IStorageCnch";}

    const auto &getSettings() const { return settings; }
    const auto &getCluster() const { return useBytedanceKafka() ? settings.cluster.value : settings.broker_list.value; }
    const auto &getTopics() const { return topics; }
    const auto &getGroup() const { return settings.group_name.value; }
    const auto &getFormatName() const { return settings.format.value; }
    const auto &getSchemaName() const { return settings.schema.value; }
    const auto &getSchemaPath() const { return settings.format_schema_path.value; }
    const auto &getConsumersNum() const { return settings.num_consumers.value; }
    void getKafkaTableInfo(KafkaTableInfo & table_info);

    bool useBytedanceKafka() const { return !settings.cluster.value.empty(); }
    String getGroupForBytekv() const;

    void checkAndLoadingSettings(KafkaSettings & kafka_settings);

    NamesAndTypesList getVirtuals() const override;
    /// void setColumns(ColumnsDescription columns_) override;

protected:
    KafkaSettings settings;

    Names topics;
    Int64 shard_count = 1;

    IStorageCnchKafka(
        const StorageID &table_id_,
        ContextMutablePtr context_,
        const ASTPtr setting_changes_,
        const KafkaSettings &settings,
        const ColumnsDescription & columns_,
        const ConstraintsDescription & constraints_
    );
};

}/// namespace DB

#endif
