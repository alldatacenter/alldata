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

#include <Common/PODArray.h>
#include <DataStreams/IBlockStream_fwd.h>
#include <Processors/Pipe.h>
#include <Storages/MergeTree/IMergeTreeDataPart_fwd.h>
#include <Storages/MergeTree/MergeTreeDataPartChecksum.h>
#include <Storages/StorageInMemoryMetadata.h>
#include <WorkerTasks/ManipulationList.h>
#include <WorkerTasks/ManipulationProgress.h>

#include <common/logger_useful.h>

namespace Poco
{
class TemporaryFile;
}

namespace DB
{

struct ManipulationTaskParams;
struct ManipulationListElement;
struct MergeTreeWriterSettings;

class CompressedReadBufferFromFile;
class CnchMergePrefetcher;
class MergedBlockOutputStream;
class MergeTreeMetaBase;

class MergeTreeDataMerger
{
public:
    using MutableDataPartPtr = std::shared_ptr<IMergeTreeDataPart>;
    using CheckCancelCallback = std::function<bool()>;

    MergeTreeDataMerger(
        MergeTreeMetaBase & data_,
        const ManipulationTaskParams & params_,
        ContextPtr context_,
        ManipulationListElement * manipulation_entry_,
        CheckCancelCallback check_cancel_,
        bool build_rowid_mappings = false);

    ~MergeTreeDataMerger();

    MutableDataPartPtr mergePartsToTemporaryPart();

    /// The i-th input row is mapped to the RowidMapping[i]-th output row in the merged part
    /// TODO: the elements are sorted integers, could apply encoding to reduce memory footprint
    using RowidMapping = PODArray<UInt32, /*INITIAL_SIZE*/ 1024>;

    /// REQUIRES:
    /// 1. when constructing the merger, build_rowid_mappings is true
    /// 2. mergePartsToTemporaryPart() has been called
    const RowidMapping & getRowidMapping(size_t part_index) const { return rowid_mappings[part_index]; }

private:
    void prepareColumnNamesAndTypes();
    void prepareNewParts();

    void chooseMergeAlgorithm();
    void prepareVerticalMerge();
    void resetMergingColumns();

    void chooseCompressionCodec();

    void prepareForProgress();

    void createSources();
    void createMergedStream();

    void copyMergedData();

    void gatherColumn(const String & column_name);
    void gatherColumns();

    void finalizePart();

private:
    MergeTreeMetaBase & data;
    MergeTreeSettingsPtr data_settings;
    StorageMetadataPtr metadata_snapshot;

    const ManipulationTaskParams & params;
    ContextPtr context;
    ManipulationListElement * manipulation_entry;
    CheckCancelCallback check_cancel;
    bool build_rowid_mappings;
    std::vector<RowidMapping> rowid_mappings; /// rowid mapping for each input part
    Poco::Logger * log = nullptr;

    /// Some parameters
    size_t sum_input_rows_upper_bound = 0;
    MergeAlgorithm merge_alg = MergeAlgorithm::Horizontal;
    CompressionCodecPtr compression_codec;

    /// All about new parts
    ReservationPtr space_reservation;
    MutableDataPartPtr new_data_part;

    /// Define prefetcher before streams
    /// std::unique_ptr<CnchMergePrefetcher> prefetcher;

    /// All about source parts
    Names all_column_names;
    Names gathering_column_names;
    Names merging_column_names;

    NamesAndTypesList storage_columns;
    NamesAndTypesList gathering_columns;
    NamesAndTypesList merging_columns;

    /// Streams
    Pipes pipes;
    BlockInputStreamPtr merged_stream;
    std::unique_ptr<MergedBlockOutputStream> to;

    /// For vertical merge
    DiskPtr tmp_disk;
    std::unique_ptr<Poco::TemporaryFile> rows_sources_file;
    std::unique_ptr<WriteBuffer> rows_sources_uncompressed_write_buf;
    std::unique_ptr<WriteBuffer> rows_sources_write_buf;
    std::unique_ptr<CompressedReadBufferFromFile> rows_sources_read_buf;
    std::unique_ptr<std::set<std::string>> written_offset_columns;
    std::unique_ptr<MergeTreeWriterSettings> writer_settings;
    MergeTreeDataPartChecksums additional_column_checksums;

    /// For progress
    ColumnSizeEstimator::ColumnToSize merged_column_to_size;
    std::optional<ColumnSizeEstimator> column_sizes;
    std::optional<MergeStageProgress> horizontal_stage_progress;

    /// statistics
    UInt64 watch_prev_elapsed = 0;
    size_t rows_written = 0;
    size_t normal_columns_gathered = 0;

    /// Used for building rowid mappings
    size_t output_rowid = 0;
};

} /// EOF
