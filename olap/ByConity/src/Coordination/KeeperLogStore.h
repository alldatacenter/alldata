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
#include <libnuraft/log_store.hxx> // Y_IGNORE
#include <map>
#include <mutex>
#include <Core/Types.h>
#include <Coordination/Changelog.h>
#include <common/logger_useful.h>

namespace DB
{

/// Wrapper around Changelog class. Implements RAFT log storage.
class KeeperLogStore : public nuraft::log_store
{
public:
    KeeperLogStore(const std::string & changelogs_path, uint64_t rotate_interval_, bool force_sync_, bool compress_logs_);

    /// Read log storage from filesystem starting from last_commited_log_index
    void init(uint64_t last_commited_log_index, uint64_t logs_to_keep);

    uint64_t start_index() const override;

    uint64_t next_slot() const override;

    /// return last entry from log
    nuraft::ptr<nuraft::log_entry> last_entry() const override;

    /// Append new entry to log
    uint64_t append(nuraft::ptr<nuraft::log_entry> & entry) override;

    /// Remove all entries starting from index and write entry into index position
    void write_at(uint64_t index, nuraft::ptr<nuraft::log_entry> & entry) override;

    /// Return entries between [start, end)
    nuraft::ptr<std::vector<nuraft::ptr<nuraft::log_entry>>> log_entries(uint64_t start, uint64_t end) override;

    /// Return entry at index
    nuraft::ptr<nuraft::log_entry> entry_at(uint64_t index) override;

    /// Term if the index
    uint64_t term_at(uint64_t index) override;

    /// Serialize entries in interval [index, index + cnt)
    nuraft::ptr<nuraft::buffer> pack(uint64_t index, int32_t cnt) override;

    /// Apply serialized entries starting from index
    void apply_pack(uint64_t index, nuraft::buffer & pack) override;

    /// Entries from last_log_index can be removed from memory and from disk
    bool compact(uint64_t last_log_index) override;

    /// Call fsync to the stored data
    bool flush() override;

    /// Current log storage size
    uint64_t size() const;

    /// Flush batch of appended entries
    void end_of_append_batch(uint64_t start_index, uint64_t count) override;

    /// Get entry with latest config in logstore
    nuraft::ptr<nuraft::log_entry> getLatestConfigChange() const;

private:
    mutable std::mutex changelog_lock;
    Poco::Logger * log;
    Changelog changelog;
};

}
