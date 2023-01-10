/*
 * Copyright 2016-2023 ClickHouse, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#pragma once
#include <DataStreams/IBlockInputStream.h>
#include <MergeTreeCommon/MergeTreeMetaBase.h>
#include <Storages/MergeTree/IMergeTreeDataPart.h>
#include <Core/Block.h>
#include <Storages/MergeTree/MergeTreeDataPartTTLInfo.h>
#include <DataStreams/ITTLAlgorithm.h>
#include <DataStreams/TTLDeleteAlgorithm.h>

#include <common/DateLUT.h>

namespace DB
{

class TTLBlockInputStream : public IBlockInputStream
{
public:
    TTLBlockInputStream(
        const BlockInputStreamPtr & input_,
        const MergeTreeMetaBase & storage_,
        const StorageMetadataPtr & metadata_snapshot_,
        const MergeTreeMetaBase::MutableDataPartPtr & data_part_,
        time_t current_time,
        bool force_
    );

    String getName() const override { return "TTL"; }
    Block getHeader() const override { return header; }

protected:
    Block readImpl() override;

    /// Finalizes ttl infos and updates data part
    void readSuffixImpl() override;

private:
    std::vector<TTLAlgorithmPtr> algorithms;
    const TTLDeleteAlgorithm * delete_algorithm = nullptr;
    bool all_data_dropped = false;

    /// ttl_infos and empty_columns are updating while reading
    const MergeTreeMetaBase::MutableDataPartPtr & data_part;
    Poco::Logger * log;
    Block header;
};

}
