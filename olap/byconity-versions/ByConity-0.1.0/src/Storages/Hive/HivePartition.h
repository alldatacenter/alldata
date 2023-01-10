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

#include <Core/Types.h>
#include <hivemetastore/hive_metastore_types.h>

namespace DB
{
using namespace Apache::Hadoop::Hive;

struct HivePartitionInfo
{
    String db_name;
    String table_name;
    String partition_path;
    String table_path;
    int32_t create_time;
    int32_t last_access_time;
    std::vector<String> values;
    String input_format;
    String output_format;
    std::vector<FieldSchema> cols;
    std::vector<String> parts_name;

    HivePartitionInfo(
        const String & dbName_,
        const String & tableName_,
        const String & partition_path_,
        const String & table_path_,
        int32_t & createTime_,
        int32_t & lastAccessTime_,
        std::vector<String> & values_,
        const String & inputFormat_,
        const String & outputFormat_,
        std::vector<FieldSchema> & cols_,
        std::vector<String> & parts_name_)
        : db_name(dbName_)
        , table_name(tableName_)
        , partition_path(partition_path_)
        , table_path(table_path_)
        , create_time(createTime_)
        , last_access_time(lastAccessTime_)
        , values(values_)
        , input_format(inputFormat_)
        , output_format(outputFormat_)
        , cols(cols_)
        , parts_name(parts_name_)
    {
    }
    HivePartitionInfo() = default;

    const std::vector<String> & getPartsName() const { return parts_name; }
    const String & getLocation() const { return partition_path; }
};

class HivePartition
{
public:
    HivePartition(const String & partition_id, HivePartitionInfo & info_);
    ~HivePartition();

    const String & getID();
    const String & getTablePath() const;
    const String & getPartitionPath();
    const String & getTableName() const;
    const String & getDBName() const;
    int32_t getCreateTime() const;
    int32_t getLastAccessTime() const;
    const std::vector<String> & getValues() const;
    const String & getInputFormat() const;
    const String & getOutputFromat() const;
    const std::vector<String> & getPartsName() const;


private:
    String partition_id;
    HivePartitionInfo info;
};

}
