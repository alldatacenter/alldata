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

#include <Storages/Hive/HivePartition.h>

namespace DB
{
HivePartition::HivePartition(const String & partition_id_, HivePartitionInfo & info_) : partition_id(partition_id_), info(info_)
{
}

HivePartition::~HivePartition() = default;

const String & HivePartition::getID()
{
    return partition_id;
}

const String & HivePartition::getTablePath() const
{
    return info.table_path;
}

const String & HivePartition::getPartitionPath()
{
    return info.getLocation();
}

const String & HivePartition::getTableName() const
{
    return info.table_name;
}

const String & HivePartition::getDBName() const
{
    return info.db_name;
}

int32_t HivePartition::getCreateTime() const
{
    return info.create_time;
}

int32_t HivePartition::getLastAccessTime() const
{
    return info.last_access_time;
}

const std::vector<String> & HivePartition::getValues() const
{
    return info.values;
}

const String & HivePartition::getInputFormat() const
{
    return info.input_format;
}

const String & HivePartition::getOutputFromat() const
{
    return info.output_format;
}

const std::vector<String> & HivePartition::getPartsName() const
{
    return info.parts_name;
}


}
