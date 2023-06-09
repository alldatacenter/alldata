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
#include <Transaction/LockDefines.h>
#include <Transaction/LockRequest.h>
#include <common/logger_useful.h>

#include <array>
#include <chrono>
#include <unordered_map>
#include <common/singleton.h>
namespace DB
{
class Context;

class LockContext
{
public:
    LockContext();
    LockStatus lock(LockRequest * request);
    void unlock(LockRequest * request);
    const LockRequestList & getGrantedList() const { return grantedList; }
    const LockRequestList & getWaitingList() const { return waitingList; }
    std::vector<TxnTimestamp> getGrantedTxnList() const;
    std::pair<UInt32, UInt32> getModes() const { return {grantedModes, conflictedModes}; }
    bool empty() const { return !grantedModes && !conflictedModes; }

private:
    void scheduleWaitingRequests();

    void incGrantedModeCount(LockMode mode);
    void decGrantedModeCount(LockMode mode);
    void incConflictedModeCount(LockMode mode);
    void decConflictedModeCount(LockMode mode);

    void incGrantedTxnCounts(const TxnTimestamp & txn_id);
    void decGrantedTxnCounts(const TxnTimestamp & txn_id);
    bool lockedBySameTxn(const TxnTimestamp & txn_id);

private:
    // Counts the granted lock requests for each lock modes
    std::array<UInt32, LockModeSize> grantedCounts;

    // Bit-mask of current granted lock modes
    UInt32 grantedModes;

    // List of granted lock requests
    LockRequestList grantedList;

    std::array<UInt32, LockModeSize> conflictedCounts;
    UInt32 conflictedModes;
    LockRequestList waitingList;

    //  Counts the num of granted lock requests for each txn_id
    using TxnID = UInt64;
    std::unordered_map<TxnID, UInt32> grantedTxnCounts;
};

template <typename Key, typename T>
struct MapStripe
{
    std::mutex mutex;
    std::unordered_map<Key, T> map;
};

template <typename Key, typename T, typename Hash = std::hash<Key>>
struct StripedMap
{
    static constexpr size_t num_stripes = 16;
    Hash hash_fn;
    std::vector<MapStripe<Key, T>> map_stripes{num_stripes};

    MapStripe<Key, T> & getStripe(const Key & key) { return map_stripes.at(hash_fn(key) % num_stripes); }
};

using LockMapStripe = MapStripe<String, LockContext>;
using LockMap = StripedMap<String, LockContext>;
using LockMaps = std::vector<LockMap>;

class LockManager : public ext::singleton<LockManager>
{
public:
    LockManager() = default;
    LockManager(const LockManager &) = delete;
    LockManager & operator=(const LockManager &) = delete;

    LockStatus lock(LockRequest *, const Context & context);
    void unlock(LockRequest *);

    void lock(const LockInfoPtr &, const Context & context);
    void unlock(const LockInfoPtr &);
    void unlock(const TxnTimestamp & txn_id);

    using Clock = std::chrono::steady_clock;
    void updateExpireTime(const TxnTimestamp & txn_id_, Clock::time_point expire_tp);

    // for unit test
    void initialize() { lock_maps = LockMaps(LockLevelSize); }
    void setEvictExpiredLocks(bool enable) { evict_expired_locks = enable; }
    LockMaps & getLockMaps() { return lock_maps; }

private:
    LockInfoPtr getLockInfoPtr(const TxnTimestamp & txn_id, LockID lock_id);
    bool isTxnExpired(const TxnTimestamp & txn_id, const Context & context);

private:
    bool evict_expired_locks{true};
    LockMaps lock_maps{LockLevelSize};

    using LockIdMap = std::unordered_map<LockID, LockInfoPtr>;

    struct TxnLockInfo
    {
        UInt64 txn_id;
        Clock::time_point expire_time;
        LockIdMap lock_ids;
    };

    using TxnLockMapStripe = MapStripe<UInt64, TxnLockInfo>;
    using TxnLockMap = StripedMap<UInt64, TxnLockInfo>;
    // Maps from txn_id -> TxnLockInfo
    TxnLockMap txn_locks_map;

    Poco::Logger * log{&Poco::Logger::get("CnchLockManager")};
};

}
