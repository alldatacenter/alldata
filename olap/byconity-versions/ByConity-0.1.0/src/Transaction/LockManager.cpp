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

#include <Transaction/LockManager.h>
#include <Catalog/Catalog.h>
#include <IO/WriteHelpers.h>
#include <Interpreters/Context.h>
#include <Transaction/LockDefines.h>
#include <Transaction/LockRequest.h>
#include <Transaction/TransactionCommon.h>
#include <common/logger_useful.h>
#include <Common/Exception.h>
#include <Common/Stopwatch.h>

#include <chrono>
#include <cassert>


namespace DB
{


namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
}

LockContext::LockContext() : grantedCounts{}, grantedModes(0), conflictedCounts{}, conflictedModes(0)
{}

LockStatus LockContext::lock(LockRequest * request)
{
    bool granted = lockedBySameTxn(request->getTransactionID()) || (!conflicts(request->getMode(), grantedModes) && waitingList.empty());

    if (granted)
    {
        incGrantedModeCount(request->getMode());
        incGrantedTxnCounts(request->getTransactionID());
        auto pos = grantedList.insert(grantedList.end(), request);
        request->setLockResult(LockStatus::LOCK_OK, pos);
        return LockStatus::LOCK_OK;
    }
    else
    {
        if (request->noWait())
        {
            request->setLockResult(LockStatus::LOCK_TIMEOUT, waitingList.end());
            return LockStatus::LOCK_TIMEOUT;
        }
        else
        {
            incConflictedModeCount(request->getMode());
            auto pos = waitingList.insert(waitingList.end(), request);
            request->setLockResult(LockStatus::LOCK_WAITING, pos);
            return LockStatus::LOCK_WAITING;
        }
    }
}

void LockContext::unlock(LockRequest * request)
{
    LockStatus status = request->getStatus();
    if (status == LockStatus::LOCK_OK)
    {
        grantedList.erase(request->getRequestItor());
        decGrantedModeCount(request->getMode());
        decGrantedTxnCounts(request->getTransactionID());
        scheduleWaitingRequests();
        request->setLockResult(LockStatus::LOCK_INIT, grantedList.end());
    }
    else if (status == LockStatus::LOCK_WAITING)
    {
        waitingList.erase(request->getRequestItor());
        decConflictedModeCount(request->getMode());
        request->setLockResult(LockStatus::LOCK_INIT, waitingList.end());
    }
    else
        throw Exception("Unlock the lock with undefined status " + toString(to_underlying(status)), ErrorCodes::LOGICAL_ERROR);
}

std::vector<TxnTimestamp> LockContext::getGrantedTxnList() const
{
    std::vector<TxnTimestamp> res;
    res.reserve(grantedTxnCounts.size());
    for (const auto & it : grantedTxnCounts)
    {
        res.push_back(it.first);
    }

    return res;
}

void LockContext::scheduleWaitingRequests()
{
    auto it = waitingList.begin();
    while (it != waitingList.end())
    {
        LockRequest * request = *it;
        if (conflicts(request->getMode(), grantedModes))
        {
            ++it;
            continue;
        }

        it = waitingList.erase(it);
        decConflictedModeCount(request->getMode());
        incGrantedModeCount(request->getMode());
        incGrantedTxnCounts(request->getTransactionID());
        auto pos = grantedList.insert(grantedList.end(), request);
        request->setLockResult(LockStatus::LOCK_OK, pos);
        request->notify();
    }
}

void LockContext::incGrantedModeCount(LockMode mode)
{
    ++grantedCounts[to_underlying(mode)];
    if (grantedCounts[to_underlying(mode)] == 1)
    {
        assert((grantedModes & modeMask(mode)) == 0);
        grantedModes |= modeMask(mode);
    }
}

void LockContext::decGrantedModeCount(LockMode mode)
{
    assert(grantedCounts[to_underlying(mode)] > 0);
    --grantedCounts[to_underlying(mode)];
    if (grantedCounts[to_underlying(mode)] == 0)
    {
        assert((grantedModes & modeMask(mode)) == modeMask(mode));
        grantedModes &= ~(modeMask(mode));
    }
}

void LockContext::incConflictedModeCount(LockMode mode)
{
    ++conflictedCounts[to_underlying(mode)];
    if (conflictedCounts[to_underlying(mode)] == 1)
    {
        assert((conflictedModes & modeMask(mode)) == 0);
        conflictedModes |= modeMask(mode);
    }
}

void LockContext::decConflictedModeCount(LockMode mode)
{
    assert(conflictedCounts[to_underlying(mode)] > 0);
    --conflictedCounts[to_underlying(mode)];
    if (conflictedCounts[to_underlying(mode)] == 0)
    {
        assert((conflictedModes & modeMask(mode)) == modeMask(mode));
        conflictedModes &= ~(modeMask(mode));
    }
}

void LockContext::incGrantedTxnCounts(const TxnTimestamp & txn_id)
{
    grantedTxnCounts[txn_id.toUInt64()]++;
}

void LockContext::decGrantedTxnCounts(const TxnTimestamp & txn_id)
{
    if (auto it = grantedTxnCounts.find(txn_id.toUInt64()); it != grantedTxnCounts.end())
    {
        if ((--it->second) == 0)
            grantedTxnCounts.erase(it);
    }
    else
        assert(0);
}

bool LockContext::lockedBySameTxn(const TxnTimestamp & txn_id)
{
    return grantedTxnCounts.find(txn_id.toUInt64()) != grantedTxnCounts.end();
}

LockStatus LockManager::lock(LockRequest * request, const Context & context)
{
    LOG_DEBUG(log, "lock request: " + request->toDebugString());

    auto level = to_underlying(request->getLevel());
    std::vector<TxnTimestamp> current_granted_txns;
    LockMapStripe & stripe = lock_maps[level].getStripe(request->getEntity());
    {
        std::lock_guard lock(stripe.mutex);
        // construct lock context if not exists
        auto it = stripe.map.find(request->getEntity());
        if (it == stripe.map.end())
        {
            it = stripe.map.try_emplace(request->getEntity()).first;
        }
        LockContext & lock_context = it->second;
        lock_context.lock(request);

        // If lock request needs to wait, check if there are expired txn
        if (evict_expired_locks && request->getStatus() != LockStatus::LOCK_OK)
        {
            current_granted_txns = lock_context.getGrantedTxnList();
        }
    }

    // clear expired txn and retry lock request
    if (evict_expired_locks && !current_granted_txns.empty())
    {
        for (const auto & txn_id : current_granted_txns)
            isTxnExpired(txn_id, context);
    }

    return request->getStatus();
}

void LockManager::unlock(LockRequest * request)
{
    LOG_DEBUG(log, "unlock request: " + request->toDebugString());
    auto level = to_underlying(request->getLevel());
    LockMapStripe & stripe = lock_maps[level].getStripe(request->getEntity());
    {
        std::lock_guard lock(stripe.mutex);
        if (auto it = stripe.map.find(request->getEntity()); it != stripe.map.end())
        {
            it->second.unlock(request);

            // remove LockContext if it's empty
            if (it->second.empty())
                stripe.map.erase(it);
        }
    }
}

void LockManager::lock(const LockInfoPtr & info, const Context & context)
{
    LOG_DEBUG(log, "try lock: " + info->toDebugString());
    assert(info->lock_id != 0);
    // register transaction in LockManager
    UInt64 txn_id = UInt64(info->txn_id);
    TxnLockMapStripe & stripe = txn_locks_map.getStripe(txn_id);
    {
        // update txn_locks_map, which is a map of txn_id -> lock_ids;
        std::lock_guard lock(stripe.mutex);
        // auto expire_tp = Clock::now() + ICnchTransaction::default_lock_expire_duration;
        auto expire_tp = Clock::now() + std::chrono::milliseconds(30000);
        if (auto it = stripe.map.find(txn_id); it != stripe.map.end())
        {
            auto & txn_locks_info = it->second;
            txn_locks_info.expire_time = expire_tp;
            txn_locks_info.lock_ids.emplace(info->lock_id, info);
        }
        else
        {
            stripe.map.emplace(txn_id, TxnLockInfo{txn_id, expire_tp, LockIdMap{{info->lock_id, info}}});
        }
    }

    const auto & requests = info->getLockRequests();
    // TODO: fix here type conversion
    Int64 remain_wait_time = info->timeout;
    Stopwatch watch;
    for (const auto & request : requests)
    {
        request->setTimeout(remain_wait_time);
        bool lock_ok = request->lock(context);
        remain_wait_time -= watch.elapsedMilliseconds();
        if (!lock_ok || remain_wait_time < 0)
        {
            info->status = LockStatus::LOCK_TIMEOUT;
            // unlock all previous acquired lock requests
            unlock(info);
            return;
        }
    }

    info->status = LockStatus::LOCK_OK;
}

void LockManager::unlock(const LockInfoPtr & info)
{
    LOG_DEBUG(log, "unlock: " + info->toDebugString());

    const UInt64 txn_id = info->txn_id.toUInt64();
    const LockID lock_id = info->lock_id;

    LockInfoPtr stored_info = getLockInfoPtr(info->txn_id, info->lock_id);
    if (!stored_info)
    {
        LOG_WARNING(log, "Unlock a nonexistent lock. lock id: {}, txn_id: {}\n", toString(info->lock_id),toString(txn_id));
        return;
    }
    else
    {
        TxnLockMapStripe & stripe = txn_locks_map.getStripe(txn_id);
        std::lock_guard lock(stripe.mutex);
        if (auto it = stripe.map.find(txn_id); it != stripe.map.end())
        {
            // erase lock id
            auto & txn_locks_info = it->second;
            txn_locks_info.lock_ids.erase(lock_id);

            if (txn_locks_info.lock_ids.empty())
            {
                stripe.map.erase(it);
            }
        }
    }

    const auto & requests = stored_info->getLockRequests();
    for (const auto & request : requests)
    {
        request->unlock();
    }
}

void LockManager::unlock(const TxnTimestamp & txn_id_)
{
    UInt64 txn_id = UInt64(txn_id_);
    LockIdMap lock_infos;

    // deregister transaction
    TxnLockMapStripe & stripe = txn_locks_map.getStripe(txn_id);
    {
        std::lock_guard lock(stripe.mutex);
        if (auto it = stripe.map.find(txn_id); it != stripe.map.end())
        {
            lock_infos = std::move(it->second.lock_ids);
            stripe.map.erase(it);
        }
    }

    for (const auto & [lock_id, info] : lock_infos)
    {
        const auto & requests = info->getLockRequests();
        for (auto & request : requests)
        {
            request->unlock();
        }
    }
}

void LockManager::updateExpireTime(const TxnTimestamp & txn_id_, Clock::time_point tp)
{
    UInt64 txn_id = UInt64(txn_id_);
    TxnLockMapStripe & stripe = txn_locks_map.getStripe(txn_id);
    {
        std::lock_guard lock(stripe.mutex);
        if (auto it = stripe.map.find(txn_id); it != stripe.map.end())
        {
            LOG_DEBUG(log, "Update txn expire time {}", txn_id);
            it->second.expire_time = tp;
        }
    }
}

LockInfoPtr LockManager::getLockInfoPtr(const TxnTimestamp & txn_id, LockID lock_id)
{
    TxnLockMapStripe & stripe = txn_locks_map.getStripe(txn_id);
    // lookup stored lock info by (txn_id, lock_id)
    {
        std::lock_guard lock(stripe.mutex);
        if (auto it = stripe.map.find(txn_id); it != stripe.map.end())
        {
            auto & lock_id_map = it->second.lock_ids;
            if (auto it_inner = lock_id_map.find(lock_id); it_inner != lock_id_map.end())
                return it_inner->second;
        }
    }

    return {};
}

bool LockManager::isTxnExpired(const TxnTimestamp & txn_id, const Context & context)
{
    bool expired = false;
    TxnLockMapStripe & stripe = txn_locks_map.getStripe(UInt64(txn_id));
    {
        std::lock_guard lock(stripe.mutex);
        if (auto it = stripe.map.find(txn_id); it != stripe.map.end())
        {
            expired = (it->second.expire_time < Clock::now());
        }
        else
        {
            expired = true;
        }
    }

    if (expired)
    {
        LOG_DEBUG(log, "Txn {} is expired. Will abort it and evict its locks from lock manager", txn_id);
        // abort transaction and steal transaction's lock
        if (auto catalog = context.tryGetCnchCatalog(); catalog)
        {
            auto txn_record = catalog->tryGetTransactionRecord(txn_id);
            if (txn_record)
            {
                TransactionRecord target_record = txn_record.value();
                target_record.setStatus(CnchTransactionStatus::Aborted).setCommitTs(context.getTimestamp());
                bool success = catalog->setTransactionRecord(txn_record.value(), target_record);
                // If abort txn successfully, clear locks belonging to this txn
                if (success)
                {
                    unlock(txn_id);
                }
                else
                {
                    LOG_WARNING(log, "Fail to evict txn {}", txn_id);
                    return false;
                }
            }
            else
            {
                // txn record not exists
                unlock(txn_id);
            }
        }
        else
        {
            // Catalog is not initialized, for unit test
            unlock(txn_id);
        }
    }
    return expired;
}

}
