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

#include <Storages/MergeTree/MergeTreeIndexGranularity.h>
#include <MergeTreeCommon/MergeTreeMetaBase.h>
#include <DataStreams/IBlockOutputStream.h>
#include <Storages/MergeTree/IMergeTreeDataPart.h>
#include <Storages/MergeTree/IMergeTreeDataPartWriter.h>

namespace DB
{

class IMergedBlockOutputStream : public IBlockOutputStream
{
public:
    IMergedBlockOutputStream(
        const MergeTreeDataPartPtr & data_part,
        const StorageMetadataPtr & metadata_snapshot_);

    using WrittenOffsetColumns = std::set<std::string>;

    const MergeTreeIndexGranularity & getIndexGranularity()
    {
        return writer->getIndexGranularity();
    }

    virtual void updateWriterStream(const NameAndTypePair &pair);

protected:
    // using SerializationState = ISerialization::SerializeBinaryBulkStatePtr;

    // ISerialization::OutputStreamGetter createStreamGetter(const String & name, WrittenOffsetColumns & offset_columns);

    /// Remove all columns marked expired in data_part. Also, clears checksums
    /// and columns array. Return set of removed files names.
    static NameSet removeEmptyColumnsFromPart(
        const MergeTreeDataPartPtr & data_part,
        NamesAndTypesList & columns,
        MergeTreeMetaBase::DataPart::Checksums & checksums);

protected:
    const MergeTreeMetaBase & storage;
    StorageMetadataPtr metadata_snapshot;

    VolumePtr volume;
    String part_path;

    IMergeTreeDataPart::MergeTreeWriterPtr writer;
};

}
