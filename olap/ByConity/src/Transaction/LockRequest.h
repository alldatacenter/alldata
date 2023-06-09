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
#include <Core/UUID.h>
#include <Transaction/LockDefines.h>
#include <Transaction/TxnTimestamp.h>

#include <condition_variable>
#include <list>
#include <mutex>

namespace DB
{
class Context;

class LockRequest
{
public:
    using LockRequestItor = std::list<LockRequest *>::iterator;

    LockRequest(TxnTimestamp txn_id, String lock_entity, LockLevel level, LockMode mode, UInt64 timeout);
    LockRequest() = delete;
    LockRequest(const LockRequest &) = delete;
    LockRequest & operator=(const LockRequest &) = delete;
    ~LockRequest() = default;

    bool lock(const Context & context);
    void unlock();

    TxnTimestamp getTransactionID() const { return txn_id; }
    bool noWait() const { return !timeout; }
    LockMode getMode() const { return mode; }
    LockLevel getLevel() const { return level; }
    const String & getEntity() const { return entity; }
    LockRequestItor getRequestItor() const;
    LockStatus getStatus() const;
    String toDebugString() const;

    void setLockResult(LockStatus status, LockRequestItor it);
    void setStatus(LockStatus status_);
    void setTimeout(UInt64 t) { timeout = t; }
    void notify();

private:
    bool wait();

private:
    TxnTimestamp txn_id;
    String entity;
    LockLevel level;
    LockMode mode;

    // TODO: use Int64 for timeout
    UInt64 timeout;

    LockRequestItor request_itor;
    LockStatus status{LockStatus::LOCK_INIT};

    mutable std::mutex mutex;
    std::condition_variable cv;
};

using LockRequestPtr = std::unique_ptr<LockRequest>;
using LockRequestPtrs = std::vector<LockRequestPtr>;
using LockRequestList = std::list<LockRequest *>;

using LockID = UInt64;

struct LockInfo
{
public:
    TxnTimestamp txn_id;
    LockMode lock_mode;
    UInt64 timeout{0};
    UUID table_uuid;
    Int64 bucket{-1};
    String partition;

    LockID lock_id;
    LockStatus status{LockStatus::LOCK_INIT};
    LockRequestPtrs requests;

public:
    LockInfo(TxnTimestamp txn_id_) : txn_id(txn_id_) { }

    LockLevel getLockLevel() const;

    const LockRequestPtrs & getLockRequests();

    bool hasBucket() const { return bucket >= 0; }
    bool hasPartition() const { return !partition.empty(); }
    String toDebugString() const;

    inline LockInfo & setTxnID(TxnTimestamp txn_id_)
    {
        txn_id = txn_id_;
        return *this;
    }

    inline LockInfo & setMode(LockMode mode)
    {
        lock_mode = mode;
        return *this;
    }

    inline LockInfo & setTimeout(UInt64 timeout_)
    {
        timeout = timeout_;
        return *this;
    }

    inline LockInfo & setUUID(UUID uuid)
    {
        table_uuid = uuid;
        return *this;
    }

    inline LockInfo & setBucket(Int64 bucket_id)
    {
        bucket = bucket_id;
        return *this;
    }

    inline LockInfo & setPartition(String partition_)
    {
        partition = std::move(partition_);
        return *this;
    }

    // Don't need to manually set
    inline LockInfo & setLockID(LockID id)
    {
        lock_id = id;
        return *this;
    }
};

using LockInfoPtr = std::shared_ptr<LockInfo>;

}
