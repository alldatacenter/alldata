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


namespace orc
{
class Statistics;
class ColumnStatistics;
}
namespace parquet
{
class Statistics;
class ColumnStatistics;
}
namespace arrow
{
    class FileReader;
    class Buffer;
    class Status;
}
namespace arrow::adapters::orc
{
class ORCFileReader;
class Statistics;
class ColumnStatistics;
}

namespace DB
{
namespace ErrorCodes
{
    extern const int NOT_IMPLEMENTED;
}

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

    enum class FileFormat
    {
        PARQUET,
        ORC,
    };

    inline static const String PARQUET_INPUT_FORMAT = "com.cloudera.impala.hive.serde.ParquetInputFormat";
    inline static const String MR_PARQUET_INPUT_FORMAT = "org.apache.hadoop.hive.ql.io.parquet.MapredParquetInputFormat";
    inline static const String ORC_INPUT_FORMAT = "org.apache.hadoop.hive.ql.io.orc.OrcInputFormat";
    inline static const std::map<String, FileFormat> VALID_HDFS_FORMATS = {
        {PARQUET_INPUT_FORMAT, FileFormat::PARQUET},
        {MR_PARQUET_INPUT_FORMAT, FileFormat::PARQUET},
        {ORC_INPUT_FORMAT, FileFormat::ORC},
    };

    static inline bool isFormatClass(const String & format_class) { return VALID_HDFS_FORMATS.count(format_class) > 0; }
    static inline FileFormat toFileFormat(const String & format_class)
    {
        if (isFormatClass(format_class))
        {
            return VALID_HDFS_FORMATS.find(format_class)->second;
        }
        throw Exception("Unsupported hdfs file format " + format_class, ErrorCodes::NOT_IMPLEMENTED);
    }

public:
    HiveDataPart(
        const String & name_,
        const String & hdfs_uri_,
        const String & relative_path_,
        const String & format_name_,
        const DiskPtr & disk_,
        const HivePartInfo & info_,
        std::unordered_set<Int64> skip_splits_ = {},
        NamesAndTypesList index_names_and_types_ = {});

    virtual ~HiveDataPart() = default;

    String getFullDataPartPath() const;
    String getFullTablePath() const;
    String getHDFSUri() const;
    String getFormatName() const;
    String getName() const;
    HivePartInfo getInfo() const;
    String getRelativePath() const;
    size_t getTotalBlockNumber() const;

    const std::unordered_set<Int64> & getSkipSplits() const { return skip_splits; }
    void setSkipSplits(const std::unordered_set<Int64> & skip_splits_) { skip_splits = skip_splits_; }
    const std::vector<MinMaxIndexPtr> & getSubMinMaxIndexes() const { return split_minmax_idxes; }

    String describeMinMaxIndex(const MinMaxIndexPtr & idx) const
    {
        if (!idx)
            return "";
        std::vector<String> strs;
        strs.reserve(index_names_and_types.size());
        size_t i = 0;
        for (const auto & name_type : index_names_and_types)
            strs.push_back(name_type.name + ":" + name_type.type->getName() + idx->hyperrectangle[i++].toString());
        return boost::algorithm::join(strs, "|");
    }

    virtual FileFormat getFormat() const = 0;

    void loadFileMinMaxIndex();
    void loadSplitMinMaxIndexes();

protected:
    virtual void loadFileMinMaxIndexImpl()
    {
        throw Exception(ErrorCodes::NOT_IMPLEMENTED, "Method loadFileMinMaxIndexImpl is not supported by hive file:{}", getFormatName());
    }

    virtual void loadSplitMinMaxIndexesImpl()
    {
        throw Exception(ErrorCodes::NOT_IMPLEMENTED, "Method loadSplitMinMaxIndexesImpl is not supported by hive file:{}", getFormatName());
    }

    String name;
    String hdfs_uri;
    String relative_path;
    String format_name;
    DiskPtr disk;
    HivePartInfo info;

    std::unordered_set<Int64> skip_splits = {};
    NamesAndTypesList index_names_and_types = {};

    mutable std::vector<MinMaxIndexPtr> split_minmax_idxes;
    std::atomic<bool> split_minmax_idxes_loaded{false};

    mutable std::atomic<bool> initialized{false};
};

class HiveORCFile : public HiveDataPart
{
public:
    HiveORCFile(
        const String & name_,
        const String & hdfs_uri_,
        const String & relative_path_,
        const String & format_name_,
        const DiskPtr & disk_,
        const HivePartInfo & info_,
        std::unordered_set<Int64> skip_splits_ = {},
        NamesAndTypesList index_names_and_types_ = {})
        : HiveDataPart(name_, hdfs_uri_, relative_path_, format_name_, disk_, info_, skip_splits_, index_names_and_types_)
    {
    }

    FileFormat getFormat() const override { return FileFormat::ORC; }
    arrow::Status tryGetTotalStripes(size_t & res) const;
    size_t getTotalStripes() const;

private:
    void loadFileMinMaxIndexImpl() override;
    void loadSplitMinMaxIndexesImpl() override;

    mutable size_t total_stripes = 0;
};

class HiveParquetFile : public HiveDataPart
{
public:
    HiveParquetFile(
        const String & name_,
        const String & hdfs_uri_,
        const String & relative_path_,
        const String & format_name_,
        const DiskPtr & disk_,
        const HivePartInfo & info_,
        std::unordered_set<Int64> skip_splits_ = {},
        NamesAndTypesList index_names_and_types_ = {})
        : HiveDataPart(name_, hdfs_uri_, relative_path_, format_name_, disk_, info_, skip_splits_, index_names_and_types_)
    {
    }

    FileFormat getFormat() const override { return FileFormat::PARQUET; }
    size_t getTotalRowGroups() const;
    arrow::Status tryGetTotalRowGroups(size_t & num_row_groups) const;

private:
    void loadSplitMinMaxIndexesImpl() override;

    mutable size_t total_row_groups = 0;
    std::map<String, size_t> parquet_column_positions;
};

}
