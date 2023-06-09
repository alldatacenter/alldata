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

#include <DaemonManager/DaemonJobTxnGC.h>
#include <Catalog/Catalog.h>
#include <DaemonManager/DaemonFactory.h>
#include <CloudServices/CnchServerClient.h>
#include <CloudServices/CnchServerClientPool.h>
#include <MergeTreeCommon/CnchTopologyMaster.h>
#include <Transaction/TransactionCommon.h>
#include <Common/Exception.h>
#include <Common/ThreadPool.h>

namespace DB::DaemonManager
{
bool DaemonJobTxnGC::executeImpl()
{
    const Context & context = *getContext();
    auto txn_records
        = context.getCnchCatalog()->getTransactionRecordsForGC(context.getConfigRef().getInt("cnch_txn_clean_batch_size", 200000));
    if (txn_records.empty())
        return true;

    cleanTxnRecords(txn_records);

    if (triggerCleanUndoBuffers())
    {
        cleanUndoBuffers(txn_records);
        lastCleanUBtime = std::chrono::system_clock::now();
    }

    return true;
}

void DaemonJobTxnGC::cleanTxnRecords(const TransactionRecords & txn_records)
{
    const Context & context = *getContext();
    LOG_DEBUG(log, "txn_id from {} to {}",
        txn_records.front().txnID().toUInt64(),
        txn_records.back().txnID().toUInt64());

    TxnGCLog summary(log);
    summary.total = txn_records.size();
    TxnTimestamp current_time = context.getTimestamp();

    size_t num_threads = std::min(txn_records.size(), size_t(context.getConfigRef().getUInt("cnch_txn_gc_parallel", 16)));
    ThreadPool thread_pool(num_threads);

    size_t chunk_size = txn_records.size() / num_threads;

    for (size_t chunk_begin = 0; chunk_begin < txn_records.size(); chunk_begin += chunk_size)
    {
        size_t chunk_end = std::min(txn_records.size(), chunk_begin + chunk_size);
        thread_pool.scheduleOrThrow(
            [this, &txn_records, current_time, &summary, chunk_begin, chunk_end, & context] {
                std::vector<TxnTimestamp> cleanTxnIds;
                cleanTxnIds.reserve(chunk_end - chunk_begin);

                for (size_t i = chunk_begin; i < chunk_end; i++)
                {
                    cleanTxnRecord(txn_records[i], current_time, cleanTxnIds, summary);
                }

                context.getCnchCatalog()->removeTransactionRecords(cleanTxnIds);
                summary.cleaned += cleanTxnIds.size();
            }
            );
    }

    thread_pool.wait();
}

void DaemonJobTxnGC::cleanTxnRecord(
    const TransactionRecord & txn_record, TxnTimestamp current_time, std::vector<TxnTimestamp> & cleanTxnIds, TxnGCLog & summary)
{
    const Context & context = *getContext();

    try
    {
        auto & server_pool = context.getCnchServerClientPool();
        auto client = server_pool.tryGetByRPCAddress(txn_record.location());
        const UInt64 safe_remove_interval = context.getConfigRef().getInt("cnch_txn_safe_remove_seconds", 5 * 60); // default 5 min

        bool server_exists = static_cast<bool>(client);
        if (!client)
            client = server_pool.get();

        switch (txn_record.status())
        {
            case CnchTransactionStatus::Aborted: {
                summary.aborted++;
                client->cleanTransaction(txn_record);
                break;
            }
            case CnchTransactionStatus::Finished: {
                summary.committed++;
                if (!txn_record.cleanTs())
                {
                    if (txn_record.hasMainTableUUID())
                    {
                        auto host_port = context.getCnchTopologyMaster()->getTargetServer(
                            UUIDHelpers::UUIDToString(txn_record.mainTableUUID()), false);
                        client = server_pool.get(host_port);
                    }
                    client->cleanTransaction(txn_record);
                }
                else if (current_time.toSecond() - txn_record.cleanTs().toSecond() > safe_remove_interval)
                {
                    cleanTxnIds.push_back(txn_record.txnID());
                }
                break;
            }
            case CnchTransactionStatus::Running: {
                summary.running++;
                if (!server_exists)
                {
                    summary.reschedule++;
                    client->cleanTransaction(txn_record);
                }
                else if (client->getTransactionStatus(txn_record.txnID()) == CnchTransactionStatus::Inactive)
                {
                    summary.inactive++;
                    client->cleanTransaction(txn_record);
                }
                break;
            }
            default:
                throw Exception("Invalid status received", ErrorCodes::LOGICAL_ERROR);
        }
    }
    catch (...)
    {
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
    }
}

bool DaemonJobTxnGC::triggerCleanUndoBuffers()
{
    const int clean_undobuffer_interval = getContext()->getConfigRef().getInt("clean_undobuffer_interval_minutes", 6 * 60); // default 6 hour
    std::chrono::system_clock::time_point now = std::chrono::system_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::minutes>(now - lastCleanUBtime).count();
    return duration >= clean_undobuffer_interval;
}

void DaemonJobTxnGC::cleanUndoBuffers(const TransactionRecords & txn_records)
{
    const Context & context = *getContext();
    LOG_DEBUG(log, "cleanUndoBuffer starts");

    auto & server_pool = context.getCnchServerClientPool();
    auto catalog = context.getCnchCatalog();
    std::unordered_set<UInt64> txn_id_set;
    std::for_each(txn_records.begin(), txn_records.end(), [&txn_id_set](const auto & txn_record) {
        txn_id_set.insert(txn_record.txnID());
    });

    auto txn_undobuffers = catalog->getAllUndoBuffer();
    std::vector<TxnTimestamp> missing_ids;
    missing_ids.reserve(txn_undobuffers.size());
    for (const auto & elem : txn_undobuffers)
    {
        const auto & txn_id = elem.first;
        if (txn_id_set.find(txn_id) == txn_id_set.end())
            missing_ids.push_back(txn_id);
    }

    auto missing_records = catalog->getTransactionRecords(missing_ids);
    size_t count = 0;
    for (auto & record : missing_records)
    {
        if (record.status() == CnchTransactionStatus::Unknown)
        {
            // clean process for UNKNOWN is the same as ABORTED
            count++;
            record.setStatus(CnchTransactionStatus::Aborted);

            try
            {
                auto client = server_pool.get();
                client->cleanTransaction(record);
            }
            catch (...)
            {
                tryLogCurrentException(log, __PRETTY_FUNCTION__);
            }
        }
    }

    LOG_DEBUG(log, "Clean undo buffer for {} deleted txn_records", count);
}

void registerTxnGCDaemon(DaemonFactory & factory)
{
    factory.registerLocalDaemonJob<DaemonJobTxnGC>("TXN_GC");
}

}
