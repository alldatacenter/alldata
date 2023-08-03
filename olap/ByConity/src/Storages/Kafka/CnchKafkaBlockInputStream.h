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

#include <DataStreams/IBlockInputStream.h>
#include <Interpreters/Context.h>
#include <Interpreters/KafkaLog.h>

#include <Storages/Kafka/StorageCloudKafka.h>

namespace DB
{

class StorageCloudKafka;

class CnchKafkaBlockInputStream : public IBlockInputStream
{
public:
    CnchKafkaBlockInputStream(StorageCloudKafka & storage_, const StorageMetadataPtr & metadata_snapshot_, const std::shared_ptr<Context> & context_,
                            const Names & column_names_, size_t max_block_size_, size_t consumer_index_, bool need_add_defaults = false);

    ~CnchKafkaBlockInputStream() override;

    String getName() const override { return storage.getName(); }
    Block getHeader() const override;

    void readPrefixImpl() override;
    Block readImpl() override;
    void readSuffixImpl() override;

    void forceCommit();

private:
    StorageCloudKafka & storage;
    StorageMetadataPtr metadata_snapshot;
    ContextPtr context;
    Names column_names;
    UInt64 max_block_size;
    size_t consumer_index;
    [[maybe_unused]] bool need_add_defaults;

    static Names default_virtual_column_names;
    Names virtual_column_names;
    std::vector<int> used_column;
    MutableColumns virtual_columns;

    BufferPtr delimited_buffer;
    bool broken = true, claimed = false;

    const Block non_virtual_header, virtual_header;
};

} // namespace DB

#endif
