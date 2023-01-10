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

#include <Columns/IColumn.h>
#include <Core/Block.h>
#include <IO/ReadHelpers.h>
#include <IO/WriteHelpers.h>
#include <Interpreters/Aggregator.h>
#include <Interpreters/BloomFilterV2.h>
#include <Interpreters/Context.h>
#include <Interpreters/DistributedStages/AddressInfo.h>
#include <Parsers/IAST.h>
#include <common/logger_useful.h>

namespace DB
{
class BloomFilterV2;
using BloomFilterV2Ptr = std::shared_ptr<BloomFilterV2>;

class RuntimeFilter
{
public:
    RuntimeFilter();

    /// initialize runtime filter in build stream to build filter
    void init(
        ContextPtr context_,
        const Block & build_header_,
        const Block & stream_header,
        std::unordered_map<String, String> & join_key_map_,
        bool enable_bloom_filter_,
        bool enable_range_filter_,
        std::unordered_map<String, std::vector<String>> & multiple_alias_);

    /// initialize runtime filter in coordinator side to merge filter
    void init(const Settings & context_settings);

    void add(Block & block);

    void merge(RuntimeFilter & other_filter);

    void mergeBatchFilers(std::vector<std::shared_ptr<RuntimeFilter>> filters);

    void finalize();

    void serialize(WriteBuffer & buf, bool transform);

    void deserialize(ReadBuffer & buf, bool transform);

    ASTs getPredicate(const String & seg_key);

    size_t size();

    size_t getBuildTime() const { return build_time; }

    size_t getBuildBytes() const { return build_bytes; }

    void setRangeValid(bool valid) { range_valid = valid; }

    bool getRangeValid() const { return range_valid; }

    BloomFilterV2Ptr getBloomFilterByColumn(const String & col_name);

    std::unordered_map<std::string, std::pair<ASTPtr, ASTPtr>> getMinMax();

private:
    void initAggregator(const Block & stream_header);

    ASTPtr parseJoinKey(const String & bloom_key);
    ASTPtr parseRangeKey(const ColumnWithTypeAndName & range_key, size_t row);
    std::unordered_map<std::string, std::shared_ptr<BloomFilterV2>> col_to_bf;
    Settings settings;
    Block build_join_keys;
    std::unordered_map<String, String> join_column_map;
    std::shared_ptr<Aggregator> aggregator;
    AggregatedDataVariantsPtr data_variants;
    std::vector<ColumnRawPtrs> aggregate_columns;
    StringRefs key;
    ColumnRawPtrs key_columns;
    Poco::Logger * log;
    size_t build_time{0};
    size_t build_bytes{0};
    bool enable_bloom_filter{false};
    bool enable_range_filter{false};
    bool range_valid {false};
    std::unordered_map<String, std::vector<String>> multiple_alias;
};

using RuntimeFilterPtr = std::shared_ptr<RuntimeFilter>;
using RuntimeFilterPtrs = std::vector<RuntimeFilterPtr>;

}
