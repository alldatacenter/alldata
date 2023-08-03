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

#include <DaemonManager/DaemonJob.h>
#include <Common/Exception.h>
#include <common/logger_useful.h>
#include <DaemonManager/Metrics.h>
#include <Interpreters/Context.h>

#include <time.h>
#include <sstream>

namespace DB
{
namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
}

namespace DaemonManager
{

bvar::Adder<int> & getExecuteMetric(CnchBGThreadType type)
{
    using namespace DB::DaemonManager::BRPCMetrics;
    switch (type)
    {
        case CnchBGThreadType::PartGC:
            return g_executeImpl_PartGC;
        case CnchBGThreadType::MergeMutate:
            return g_executeImpl_MergeMutate;
        case CnchBGThreadType::Consumer:
            return g_executeImpl_Consumer;
        case CnchBGThreadType::DedupWorker:
            return g_executeImpl_DedupWorker;
        case CnchBGThreadType::GlobalGC:
            return g_executeImpl_GlobalGC;
        case CnchBGThreadType::TxnGC:
            return g_executeImpl_TxnGC;
        case CnchBGThreadType::Clustering:
            return g_executeImpl_Clustering;
        default:
            throw Exception(String{"No metric add for daemon job type "} + toString(type) + ", this is coding mistake", ErrorCodes::LOGICAL_ERROR);
    }
}

bvar::Adder<int> & getExecuteErrorMetric(CnchBGThreadType type)
{
    using namespace DB::DaemonManager::BRPCMetrics;
    switch (type)
    {
        case CnchBGThreadType::PartGC:
            return g_executeImpl_PartGC_error;
        case CnchBGThreadType::MergeMutate:
            return g_executeImpl_MergeMutate_error;
        case CnchBGThreadType::Consumer:
            return g_executeImpl_Consumer_error;
        case CnchBGThreadType::DedupWorker:
            return g_executeImpl_DedupWorker_error;
        case CnchBGThreadType::GlobalGC:
            return g_executeImpl_GlobalGC_error;
        case CnchBGThreadType::TxnGC:
            return g_executeImpl_TxnGC_error;
        case CnchBGThreadType::Clustering:
            return g_executeImpl_Clustering_error;
        default:
            throw Exception(String{"No error metric add for daemon job type "} + toString(type) + ", this is coding mistake", ErrorCodes::LOGICAL_ERROR);
    }
}

void DaemonJob::init()
{
    task = getContext()->getSchedulePool().createTask(toString(type), [this]() { execute(); });
}

void DaemonJob::start()
{
    try
    {
        if (task)
            task->activateAndSchedule();
    }
    catch (...)
    {
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
    }
}

void DaemonJob::stop()
{
    try
    {
        if (task)
            task->deactivate();
    }
    catch (...)
    {
        tryLogCurrentException(log, __PRETTY_FUNCTION__);
    }
}

void DaemonJob::execute()
{
    try
    {
        LOG_TRACE(log, __PRETTY_FUNCTION__);
        getExecuteMetric(getType()) << 1;
        bool ret = executeImpl();
        if (!ret)
            getExecuteErrorMetric(getType()) << 1;
        task->scheduleAfter(interval_ms);
        LOG_TRACE(log, "finish execute {}, try again after {}", toString(getType()), interval_ms);
    }
    catch (...)
    {
        tryLogCurrentException(log, String("Error occurs during daemon ") + toString(getType()) + " execution");
        getExecuteErrorMetric(getType()) << 1;
        task->scheduleAfter(interval_ms);
    }
}

} // end namespace DaemonManager
} // end namespace DB
