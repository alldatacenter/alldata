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

#include "IntentLock.h"

#include <Catalog/Catalog.h>
#include <CloudServices/CnchServerClientPool.h>
#include <Transaction/TimestampCacheManager.h>
#include <Transaction/TransactionCoordinatorRcCnch.h>
#include <Common/ProfileEvents.h>
#include <Common/Stopwatch.h>
#include <Common/randomSeed.h>
#include <common/scope_guard.h>

namespace ProfileEvents
{
    extern const Event IntentLockPreemptionElapsedMilliseconds;
    extern const Event IntentLockWriteIntentElapsedMilliseconds;
    extern const Event IntentLockElapsedMilliseconds;
}

namespace DB
{
namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int CNCH_TRANSACTION_TSCACHE_CHECK_FAILED;
    extern const int CONCURRENCY_NOT_ALLOWED_FOR_DDL;
}

static void doRetry(std::function<void()> action, size_t retry)
{
    pcg64 rng(randomSeed());

    while (retry--)
    {
        try
        {
            action();
            return;
        }
        catch (...)
        {
            if (retry)
            {
                auto duration = std::chrono::milliseconds(std::uniform_int_distribution<Int64>(0, 1000)(rng));
                std::this_thread::sleep_for(duration);
            }
            else throw;
        }
    }
}

bool IntentLock::tryLock()
{
    try
    {
        doRetry([this] { lockImpl(); }, lock_retry);
    }
    catch (...)
    {
        DB::tryLogCurrentException(__PRETTY_FUNCTION__);
    }

    return locked;
}

void IntentLock::lock()
{
    doRetry([this] { lockImpl(); }, lock_retry);
}

void IntentLock::writeIntents()
{
    if (locked)
        throw Exception("IntentLock is already locked", ErrorCodes::LOGICAL_ERROR);

    Stopwatch watch;
    auto catalog = context.getCnchCatalog();
    auto intents = createWriteIntents();
    std::map<std::pair<TxnTimestamp, String>, std::vector<String>> conflict_parts;
    bool lock_success = catalog->writeIntents(lock_prefix, intents, conflict_parts);
    ProfileEvents::increment(ProfileEvents::IntentLockWriteIntentElapsedMilliseconds, watch.elapsedMilliseconds());

    if (lock_success)
    {
        locked = true;
    }
    else
    {
        Stopwatch watch_for_preemption;
        SCOPE_EXIT({ProfileEvents::increment(ProfileEvents::IntentLockPreemptionElapsedMilliseconds, watch.elapsedMilliseconds());});
        for (const auto & txn : conflict_parts)
        {
            const auto & [txn_id, location] = txn.first;

            // Locked by current transaction
            if (txn_id == txn_record.txnID())
            {
                Strings locked_intents = txn.second;
                std::sort(locked_intents.begin(), locked_intents.end());
                intent_names.erase(
                    std::remove_if(
                        intent_names.begin(),
                        intent_names.end(),
                        [&](String intent_name) { return std::binary_search(locked_intents.begin(), locked_intents.end(), intent_name); }),
                    intent_names.end());

                if (intent_names.empty())
                {
                    valid = false;
                    locked = true;
                    return;
                }

                continue;
            }

            auto record = catalog->tryGetTransactionRecord(txn_id);
            if (!record.has_value() || record->status() == CnchTransactionStatus::Running)
            {
                CnchTransactionStatus txn_status{};

                if (location == txn_record.location())
                {
                    // get transaction status from local server
                    auto & coordinator = context.getCnchTransactionCoordinator();
                    txn_status = coordinator.getTransactionStatus(txn_id);
                }
                else
                {
                    // get transaction status from remote server
                    try
                    {
                        // auto server_client = context.getCnchServerClientPool().get(location);
                        // txn_status = server_client->getTransactionStatus(txn_id);
                    }
                    catch (...)
                    {
                        LOG_WARNING(log, "Unable to get transaction status from " + location);
                        txn_status = CnchTransactionStatus::Aborted;
                    }
                }

                if (txn_status == CnchTransactionStatus::Running)
                {
                    // preempt low prioity txn
                    if (record && record->priority() < txn_record.priority())
                    {
                        TransactionRecord target_record;
                        target_record.setStatus(CnchTransactionStatus::Aborted);
                        target_record.setCommitTs(context.getTimestamp());

                        bool success = catalog->setTransactionRecord(*record, target_record);
                        if (success)
                        {
                            LOG_DEBUG(log, "Will preempt a low-priority transaction (txn_id: " + txn_id.toString() + ")");
                        }
                        else
                            throw Exception(
                                "Fail to preempt the transaction (txn_id: " + txn_id.toString() + ")",
                                ErrorCodes::CONCURRENCY_NOT_ALLOWED_FOR_DDL);
                    }
                    else
                    {
                        throw Exception(
                            "Current transaction is conflicted with other txn (txn_id: " + txn_id.toString() + ")",
                            ErrorCodes::CONCURRENCY_NOT_ALLOWED_FOR_DDL);
                    }
                }
                else if (record && txn_status == CnchTransactionStatus::Inactive)
                {
                    try
                    {
                        record->setCommitTs(context.getTimestamp());
                        catalog->rollbackTransaction(*record);
                    }
                    catch (...)
                    {
                        tryLogCurrentException(log, "Failed to set txn status for inactive transaction " + txn_id.toString());
                    }
                }
            }

            // reset conflicted intents
            std::vector<WriteIntent> old_intents;
            for (const auto & old_part : txn.second)
            {
                old_intents.emplace_back(txn_id, location, old_part);
                LOG_DEBUG(log, "Will reset old intent: {} {} , txn id {}\n", lock_prefix, old_part, txn_id);
            }

            if (!catalog->tryResetIntents(lock_prefix, old_intents, txn_record.txnID(), txn_record.location()))
                throw Exception(
                    "Cannot reset intents for conflicted txn. The lock might be acquired by another transaction now. conflicted txn_id: " + txn_id.toString()
                        + ". Current txn_id: " + txn_record.txnID().toString(),
                    ErrorCodes::CONCURRENCY_NOT_ALLOWED_FOR_DDL);
        }

        locked = true;
    }
}

void IntentLock::removeIntents()
{
    if (locked && valid)
    {
        auto catalog = context.getCnchCatalog();
        auto intents = createWriteIntents();
        catalog->clearIntents(lock_prefix, intents);
        locked = false;
    }
}

void IntentLock::lockImpl()
{
    Stopwatch watch;
    SCOPE_EXIT({ProfileEvents::increment(ProfileEvents::IntentLockElapsedMilliseconds, watch.elapsedMilliseconds());});
    writeIntents();
}

void IntentLock::unlock()
{
    removeIntents();
}

std::vector<WriteIntent> IntentLock::createWriteIntents()
{
    std::vector<WriteIntent> intents;
    intents.reserve(intent_names.size());
    for (const auto & intent_name : intent_names)
        intents.emplace_back(txn_record.txnID(), txn_record.location(), intent_name);
    return intents;
}

}
