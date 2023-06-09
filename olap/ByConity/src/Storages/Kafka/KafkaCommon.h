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

#include <Common/StringUtils/StringUtils.h>
#include <Core/Names.h>
#include <Core/Types.h>
#include <Interpreters/Context.h>
#include <Storages/Kafka/KafkaSettings.h>
#include <cppkafka/configuration.h>
#include <cppkafka/topic_partition_list.h>

namespace DB::Kafka
{
    const String HDFS_PREFIX = "hdfs://";
    const String CFS_PREFIX = "cfs://";

    inline bool startsWithHDFSOrCFS(const String& name)
    {
        return startsWith(name, HDFS_PREFIX) || startsWith(name, CFS_PREFIX);
    }

cppkafka::Configuration createConsumerConfiguration(
        ContextPtr context, const StorageID & storage_id, const Names & topics, const KafkaSettings & settings);

String toString(const cppkafka::TopicPartitionList & tpl);
}
