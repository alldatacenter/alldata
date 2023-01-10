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

#include <Common/ActionBlocker.h>
#include <MergeTreeCommon/MergeTreeMetaBase.h>
#include <Storages/MutationCommands.h>
#include <Storages/MergeTree/IMergeTreeDataPart_fwd.h>
#include <Storages/MergeTree/IMergedBlockOutputStream.h>
#include <WorkerTasks/ManipulationTask.h>

#include <atomic>
#include <functional>


namespace DB
{

/** Can select parts for background processes and do them.
 * Currently helps with cnch mutations
 */
class MergeTreeDataMutator
{
public:
    MergeTreeDataMutator(MergeTreeMetaBase & data_, size_t background_pool_size);

    /** Get maximum total size of parts to do mutation, at current moment of time.
      * It depends only on amount of free space in disk.
      */
    UInt64 getMaxSourcePartSizeForMutation() const;

    IMutableMergeTreeDataPartsVector mutatePartsToTemporaryParts(
        const ManipulationTaskParams & params,
        ManipulationListEntry & manipulation_entry,
        ContextPtr context,
        TableLockHolder & holder);

    /// Mutate a single data part with the specified commands. Will create and return a temporary part.
    IMutableMergeTreeDataPartPtr mutatePartToTemporaryPart(
        const IMergeTreeDataPartPtr & source_part,
        const StorageMetadataPtr & metadata_snapshot,
        const MutationCommands & commands,
        ManipulationListEntry & manipulation_entry,
        time_t time_of_mutation,
        ContextPtr context,
        const ReservationPtr & space_reservation,
        TableLockHolder & table_lock_holder);

private:
    /** Split mutation commands into two parts:
      * First part should be executed by mutations interpreter.
      * Other is just simple drop/renames, so they can be executed without interpreter.
      */
    static void splitMutationCommands(
        MergeTreeMetaBase::DataPartPtr part,
        const MutationCommands & commands,
        MutationCommands & for_interpreter,
        MutationCommands & for_file_renames);

    /// Apply commands to source_part i.e. remove and rename some columns in
    /// source_part and return set of files, that have to be removed or renamed
    /// from filesystem and in-memory checksums. Ordered result is important,
    /// because we can apply renames that affects each other: x -> z, y -> x.
    static NameToNameVector collectFilesForRenames(MergeTreeMetaBase::DataPartPtr source_part, const MutationCommands & commands_for_removes, const String & mrk_extension);

    /// Collect necessary implicit files for clear map key commands.
    /// If the part enables compact map data and all implicit keys of the map column has been removed, the compacted file need to remove too.
    static NameSet collectFilesForClearMapKey(MergeTreeMetaBase::DataPartPtr source_part, const MutationCommands & commands);

    /// Get the columns list of the resulting part in the same order as storage_columns.
    static NamesAndTypesList getColumnsForNewDataPart(
        MergeTreeMetaBase::DataPartPtr source_part,
        const Block & updated_header,
        NamesAndTypesList storage_columns,
        const MutationCommands & commands_for_removes);

    /// Get skip indices, that should exists in the resulting data part.
    static MergeTreeIndices getIndicesForNewDataPart(
        const IndicesDescription & all_indices,
        const MutationCommands & commands_for_removes);

    static MergeTreeProjections getProjectionsForNewDataPart(
        const ProjectionsDescription & all_projections,
        const MutationCommands & commands_for_removes);

    /// Return set of indices which should be recalculated during mutation also
    /// wraps input stream into additional expression stream
    static std::set<MergeTreeIndexPtr> getIndicesToRecalculate(
        BlockInputStreamPtr & input_stream,
        const NameSet & updated_columns,
        const StorageMetadataPtr & metadata_snapshot,
        ContextPtr context,
        const NameSet & materialized_indices,
        const MergeTreeMetaBase::DataPartPtr & source_part);

    static std::set<MergeTreeProjectionPtr> getProjectionsToRecalculate(
        const NameSet & updated_columns,
        const StorageMetadataPtr & metadata_snapshot,
        const NameSet & materialized_projections,
        const MergeTreeMetaBase::DataPartPtr & source_part);

    void writeWithProjections(
        MergeTreeMetaBase::MutableDataPartPtr new_data_part,
        const StorageMetadataPtr & metadata_snapshot,
        const MergeTreeProjections & projections_to_build,
        BlockInputStreamPtr mutating_stream,
        IMergedBlockOutputStream & out,
        time_t time_of_mutation,
        ManipulationListEntry & manipulation_entry,
        const ReservationPtr & space_reservation,
        TableLockHolder & holder,
        ContextPtr context,
        IMergeTreeDataPart::MinMaxIndex * minmax_idx = nullptr);

    /// Override all columns of new part using mutating_stream
    void mutateAllPartColumns(
        MergeTreeMetaBase::MutableDataPartPtr new_data_part,
        const StorageMetadataPtr & metadata_snapshot,
        const MergeTreeIndices & skip_indices,
        const MergeTreeProjections & projections_to_build,
        const MutationCommands & commands_for_removes,
        BlockInputStreamPtr mutating_stream,
        time_t time_of_mutation,
        const CompressionCodecPtr & compression_codec,
        ManipulationListEntry & manipulation_entry,
        bool need_sync,
        const ReservationPtr & space_reservation,
        TableLockHolder & holder,
        ContextPtr context);

    /// Mutate some columns of source part with mutation_stream
    void mutateSomePartColumns(
        const MergeTreeDataPartPtr & source_part,
        const StorageMetadataPtr & metadata_snapshot,
        const std::set<MergeTreeIndexPtr> & indices_to_recalc,
        const std::set<MergeTreeProjectionPtr> & projections_to_recalc,
        const Block & mutation_header,
        MergeTreeMetaBase::MutableDataPartPtr new_data_part,
        BlockInputStreamPtr mutating_stream,
        time_t time_of_mutation,
        const CompressionCodecPtr & compression_codec,
        ManipulationListEntry & manipulation_entry,
        bool need_sync,
        const ReservationPtr & space_reservation,
        TableLockHolder & holder,
        ContextPtr context);

public :
    /// Initialize and write to disk new part fields like checksums, columns, etc.
    static void finalizeMutatedPart(
        const MergeTreeDataPartPtr & source_part,
        MergeTreeMetaBase::MutableDataPartPtr new_data_part,
        const CompressionCodecPtr & codec);

    /** Is used to cancel all merges and mutations. On cancel() call all currently running actions will throw exception soon.
      * All new attempts to start a merge or mutation will throw an exception until all 'LockHolder' objects will be destroyed.
      */
    ActionBlocker mutate_blocker;

private:
    bool checkOperationIsNotCanceled(const ManipulationListEntry & manipulation_entry) const;

    MergeTreeMetaBase & data;
    const size_t background_pool_size;

    Poco::Logger * log;
};


}
