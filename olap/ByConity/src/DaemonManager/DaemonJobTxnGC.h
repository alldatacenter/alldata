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

#include <DaemonManager/DaemonJob.h>
#include <Transaction/TxnTimestamp.h>
#include <Transaction/TransactionCommon.h>
#include <Core/Types.h>
#include <common/logger_useful.h>

namespace DB::DaemonManager
{
class TxnGCLog
{
public:
    TxnGCLog(Poco::Logger * lg) : log(lg) { }
    TxnGCLog(const TxnGCLog &) = delete;
    TxnGCLog & operator=(const TxnGCLog &) = delete;
    ~TxnGCLog()
    {
        try
        {
            logSummary();
        }
        catch (...)
        {
        }
    }

    void logSummary()
    {
        std::stringstream msg;
        msg << "Summary of TxnGC execution, total txn_records: " << total << ", committed: " << committed << ", aborted: " << aborted
            << ", running: " << running << ", cleaned: " << cleaned << ", reschedule: " << reschedule << ", inactive: " << inactive;
        LOG_INFO(log, msg.str());
    }

public:
    std::atomic<UInt32> total{0};
    std::atomic<UInt32> committed{0};
    std::atomic<UInt32> aborted{0};
    std::atomic<UInt32> running{0};

    std::atomic<UInt32> cleaned{0};
    std::atomic<UInt32> reschedule{0};
    std::atomic<UInt32> inactive{0};

private:
    Poco::Logger * log;
};
class DaemonJobTxnGC : public DaemonJob
{
public:
    DaemonJobTxnGC(ContextMutablePtr global_context_) : DaemonJob(global_context_, CnchBGThreadType::TxnGC) { }
    bool executeImpl() override;
    using TransactionRecords = std::vector<TransactionRecord>;

private:
    void cleanTxnRecords(const TransactionRecords & records);
    void cleanUndoBuffers(const TransactionRecords & records);
    void cleanTxnRecord(const TransactionRecord & record, TxnTimestamp current_time, std::vector<TxnTimestamp> & cleanTxnIds, TxnGCLog & summary);
    bool triggerCleanUndoBuffers();
private:
    std::chrono::time_point<std::chrono::system_clock> lastCleanUBtime;
};

}
