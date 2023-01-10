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

namespace DB
{
/**
 *  Use enum in nested namespace instead enum class.
 *  Because we want to pass it to protos easily, while it offers weaker compile-time check.
 *  So be careful with those enums !
 */
namespace CnchBGThread
{
    enum Type : unsigned int
    {
        Empty = 0,

        PartGC = 1,
        MergeMutate = 2,
        Consumer = 3,
        DedupWorker = 4,
        Clustering = 5,
        ServerMinType = PartGC,
        ServerMaxType = Clustering,

        GlobalGC = 20, /// reserve several entries
        TxnGC = 21,
        DaemonMinType = GlobalGC,
        DaemonMaxType = TxnGC,

        ResourceReport = 30, /// worker
        WorkerMinType = ResourceReport, /// Enum to mark start of worker types
        WorkerMaxType = ResourceReport, /// Enum to mark end of worker types
    };

    constexpr unsigned int NumType = WorkerMaxType + 1;

    enum Action : unsigned int
    {
        Start = 0,
        Stop = 1, /// Just stop scheduling but not remove
        Remove = 2, /// Remove from memory
        Drop = 3, /// Used when DROP TABLE
        Wakeup = 4,
    };

    enum Status : unsigned int
    {
        Running = 0,
        Stopped = 1,
        Removed = 2,
    };
}
using CnchBGThreadType = CnchBGThread::Type;
using CnchBGThreadAction = CnchBGThread::Action;
using CnchBGThreadStatus = CnchBGThread::Status;

constexpr auto toString(CnchBGThreadType type)
{
    switch (type)
    {
        case CnchBGThreadType::Empty:
            return "Empty";
        case CnchBGThreadType::PartGC:
            return "PartGCThread";
        case CnchBGThreadType::MergeMutate:
            return "MergeMutateThread";
        case CnchBGThreadType::Clustering:
            return "ClusteringThread";
        case CnchBGThreadType::Consumer:
            return "ConsumerManager";
        case CnchBGThreadType::DedupWorker:
            return "DedupWorkerManager";
        case CnchBGThreadType::GlobalGC:
            return "GlobalGCThread";
        case CnchBGThreadType::TxnGC:
            return "TxnGCThread";
        case CnchBGThreadType::ResourceReport:
            return "ResourceReport";
    }
    __builtin_unreachable();
}

constexpr auto isServerBGThreadType(CnchBGThreadType t)
{
    return CnchBGThreadType::ServerMinType <= t && t <= CnchBGThreadType::ServerMaxType;
}

constexpr auto iDaemonBGThreadType(CnchBGThreadType t)
{
    return CnchBGThreadType::DaemonMinType <= t && t <= CnchBGThreadType::DaemonMaxType;
}

constexpr auto toString(CnchBGThreadAction action)
{
    switch (action)
    {
        case CnchBGThreadAction::Start:
            return "Start";
        case CnchBGThreadAction::Stop:
            return "Stop";
        case CnchBGThreadAction::Remove:
            return "Remove";
        case CnchBGThreadAction::Drop:
            return "Drop";
        case CnchBGThreadAction::Wakeup:
            return "Wakeup";
    }
    __builtin_unreachable();
}

constexpr auto toString(CnchBGThreadStatus status)
{
    switch (status)
    {
        case CnchBGThreadStatus::Running:
            return "Running";
        case CnchBGThreadStatus::Stopped:
            return "Stopped";
        case CnchBGThreadStatus::Removed:
            return "Removed";
    }
    __builtin_unreachable();
}

}
