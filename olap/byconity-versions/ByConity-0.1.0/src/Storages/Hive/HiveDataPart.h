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

#include <Storages/Hive/HiveDataPart_fwd.h>
#include <Storages/IStorage.h>
#include <Disks/IDisk.h>
#include <IO/ReadBuffer.h>
#include <IO/WriteBuffer.h>
#include <Storages/HDFS/ReadBufferFromByteHDFS.h>
#include <boost/algorithm/string.hpp>
#include <Common/StringUtils/StringUtils.h>
#include <Storages/HDFS/HDFSCommon.h>
#include <Storages/MergeTree/IMergeTreeDataPart.h>


namespace parquet { namespace arrow { class FileReader; } }
namespace arrow { class Buffer; }

namespace DB
{

struct HivePartInfo
{
    String name;
    String partition_id;

    HivePartInfo(const String & name_, const String & partition_id_)
        : name(name_)
        , partition_id(partition_id_)
    {
    }

    const std::map<String, String> getPartition() const
    {
        std::map<String, String> partition;
        String temp = partition_id;
        if(startsWith(temp, "/"))
        {
            temp = temp.substr(1, temp.size());
        }

        if(endsWith(temp,"/"))
        {
            temp  = temp.substr(0, temp.size() - 1);
        }

        std::vector<String> values;
        boost::split(values, temp, boost::is_any_of("/"), boost::token_compress_on);

        for(auto elem : values)
        {
            std::vector<String> key_value;
            boost::split(key_value, elem, boost::is_any_of("="), boost::token_compress_on);
            partition.insert({key_value[0], key_value[1]});
        }

        return partition;
    }

    String getBasicPartName() const
    {
        return name;
    }
};

class HiveDataPart : public std::enable_shared_from_this<HiveDataPart>
{
public:
    using MinMaxIndex = IMergeTreeDataPart::MinMaxIndex;
    using MinMaxIndexPtr = std::shared_ptr<MinMaxIndex>;

public:
    HiveDataPart(
        const String & name_,
        const String & relative_path_,
        const DiskPtr & disk_,
        const HivePartInfo & info_,
        HDFSConnectionParams hdfs_params_ = HDFSConnectionParams(),
        std::unordered_set<Int64> skip_splits_ = {},
        NamesAndTypesList index_names_and_types_ = {});

    String getFullDataPartPath() const;
    String getFullTablePath() const;

    const std::unordered_set<Int64> & getSkipSplits() const { return skip_splits; }
    void setSkipSplits(const std::unordered_set<Int64> & skip_splits_) { skip_splits = skip_splits_; }

    String describeMinMaxIndex(const MinMaxIndexPtr & idx) const;
    const std::vector<MinMaxIndexPtr> & getSubMinMaxIndexes() const { return split_minmax_idxes; }
    void loadSplitMinMaxIndexes();
    void loadSplitMinMaxIndexesImpl();
    void prepareReader() const;
    size_t getTotalRowGroups() const;

    ~HiveDataPart();

public:

    String name;
    String relative_path;
    DiskPtr disk;
    HivePartInfo info;
    const HDFSConnectionParams hdfs_params;

    std::unordered_set<Int64> skip_splits = {};
    NamesAndTypesList index_names_and_types = {};

    mutable std::vector<MinMaxIndexPtr> split_minmax_idxes;
    std::atomic<bool> split_minmax_idxes_loaded{false};

    mutable std::atomic<bool> initialized{false};

    mutable std::mutex mutex;

    mutable std::unique_ptr<ReadBufferFromByteHDFS> in;
    mutable std::unique_ptr<parquet::arrow::FileReader> reader;
    mutable std::map<String, size_t> parquet_column_positions;
};

}
