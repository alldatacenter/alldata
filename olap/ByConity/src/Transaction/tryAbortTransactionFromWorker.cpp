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

#include <Catalog/Catalog.h>
#include <Common/Exception.h>
#include <Interpreters/Context.h>
#include <Poco/Logger.h>
#include <Transaction/tryAbortTransactionFromWorker.h>

namespace DB
{

TransactionRecord tryAbortTransactionFromWorker(const Context & context, const TransactionCnchPtr & txn)
{
    static constexpr size_t MAX_ABORT_RETRY = 3;
    Poco::Logger * log = &Poco::Logger::get(__func__);

    TransactionRecord cur_txn_record = txn->getTransactionRecord();
    cur_txn_record.setStatus(CnchTransactionStatus::Running);
    auto txn_id = cur_txn_record.txnID();

    auto unknown_txn_record = cur_txn_record;
    unknown_txn_record.setStatus(CnchTransactionStatus::Unknown);

    try
    {
        auto catalog = context.getCnchCatalog();
        auto commit_ts = context.getTimestamp();
        int retry = MAX_ABORT_RETRY;
        do
        {
            TransactionRecord target_record = cur_txn_record;
            target_record.setStatus(CnchTransactionStatus::Aborted).setCommitTs(commit_ts).setMainTableUUID(txn->getMainTableUUID());

            bool success = catalog->setTransactionRecord(cur_txn_record, target_record);
            cur_txn_record = std::move(target_record);
            bool could_return = true;

            if (success)
                LOG_INFO(log, "Worker aborts txn {} successfully\n", txn_id.toUInt64());
            else if (cur_txn_record.status() == CnchTransactionStatus::Finished)
                LOG_INFO(log, "Txn {} is already finished, can't abort\n", txn_id.toUInt64());
            else if (cur_txn_record.status() == CnchTransactionStatus::Aborted)
                LOG_INFO(log, "Txn {} is already aborted\n", txn_id.toUInt64());
            else if (cur_txn_record.status() == CnchTransactionStatus::Unknown)
                LOG_WARNING(log, "Txn {} is deleted from KV, can't abort\n", txn_id.toUInt64());
            else
                could_return = false;

            if (could_return)
                return cur_txn_record;
            else
                LOG_WARNING(log, "Failed to abort merge txn, current record is {}. Will retry {} times\n", cur_txn_record.toString(), retry);
            std::this_thread::sleep_for(std::chrono::milliseconds(200 * (MAX_ABORT_RETRY - retry)));
        }
        while (retry-- > 0);
        /// can't get transaction's final status from kv, return unknown
    }
    catch (...)
    {
        tryLogCurrentException(log, "Failed to abort merge txn " + txn_id.toString());
    }
    return unknown_txn_record;
}
}
