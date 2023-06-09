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

#include <DaemonManager/Metrics.h>

namespace DB::DaemonManager::BRPCMetrics
{
    bvar::Adder< int > g_executeImpl_PartGC_error;
    bvar::Adder< int > g_executeImpl_PartGC;
    bvar::Window<bvar::Adder<int>> g_executeImpl_PartGC_error_minute("DaemonManager_Internal", "executeImpl_PartGC_error", & g_executeImpl_PartGC_error, 60);
    bvar::Window<bvar::Adder<int>> g_executeImpl_PartGC_minute("DaemonManager_Internal", "executeImpl_PartGC", & g_executeImpl_PartGC, 60);

    bvar::Adder< int > g_executeImpl_MergeMutate_error;
    bvar::Adder< int > g_executeImpl_MergeMutate;
    bvar::Window<bvar::Adder<int>> g_executeImpl_MergeMutate_error_minute("DaemonManager_Internal", "executeImpl_MergeMutate_error", & g_executeImpl_MergeMutate_error, 60);
    bvar::Window<bvar::Adder<int>> g_executeImpl_MergeMutate_minute("DaemonManager_Internal", "executeImpl_MergeMutate", & g_executeImpl_MergeMutate, 60);


    bvar::Adder< int > g_executeImpl_Consumer_error;
    bvar::Adder< int > g_executeImpl_Consumer;
    bvar::Window<bvar::Adder<int>> g_executeImpl_Consumer_error_minute("DaemonManager_Internal", "executeImpl_Consumer_error", & g_executeImpl_Consumer_error, 60);
    bvar::Window<bvar::Adder<int>> g_executeImpl_Consumer_minute("DaemonManager_Internal", "executeImpl_Consumer", & g_executeImpl_Consumer, 60);

    bvar::Adder< int > g_executeImpl_MemoryBuffer_error;
    bvar::Adder< int > g_executeImpl_MemoryBuffer;
    bvar::Window<bvar::Adder<int>> g_executeImpl_MemoryBuffer_error_minute("DaemonManager_Internal", "executeImpl_MemoryBuffer_error", & g_executeImpl_MemoryBuffer_error, 60);
    bvar::Window<bvar::Adder<int>> g_executeImpl_MemoryBuffer_minute("DaemonManager_Internal", "executeImpl_MemoryBuffer", & g_executeImpl_MemoryBuffer, 60);

    bvar::Adder< int > g_executeImpl_DedupWorker_error;
    bvar::Adder< int > g_executeImpl_DedupWorker;
    bvar::Window<bvar::Adder<int>> g_executeImpl_DedupWorker_error_minute("DaemonManager_Internal", "executeImpl_DedupWorker_error", & g_executeImpl_DedupWorker_error, 60);
    bvar::Window<bvar::Adder<int>> g_executeImpl_DedupWorker_minute("DaemonManager_Internal", "executeImpl_DedupWorker", & g_executeImpl_DedupWorker, 60);

    bvar::Adder< int > g_executeImpl_GlobalGC_error;
    bvar::Adder< int > g_executeImpl_GlobalGC;
    bvar::Window<bvar::Adder<int>> g_executeImpl_GlobalGC_error_minute("DaemonManager_Internal", "executeImpl_GlobalGC_error", & g_executeImpl_GlobalGC_error, 60);
    bvar::Window<bvar::Adder<int>> g_executeImpl_GlobalGC_minute("DaemonManager_Internal", "executeImpl_GlobalGC", & g_executeImpl_GlobalGC, 60);

    bvar::Adder< int > g_executeImpl_TxnGC_error;
    bvar::Adder< int > g_executeImpl_TxnGC;
    bvar::Window<bvar::Adder<int>> g_executeImpl_TxnGC_error_minute("DaemonManager_Internal", "executeImpl_TxnGC_error", & g_executeImpl_TxnGC_error, 60);
    bvar::Window<bvar::Adder<int>> g_executeImpl_TxnGC_minute("DaemonManager_Internal", "executeImpl_TxnGC", & g_executeImpl_TxnGC, 60);

    bvar::Adder< int > g_executeImpl_Clustering_error;
    bvar::Adder< int > g_executeImpl_Clustering;
    bvar::Window<bvar::Adder<int>> g_executeImpl_Clustering_error_minute("DaemonManager_Internal", "executeImpl_Clustering_error", & g_executeImpl_Clustering_error, 60);
    bvar::Window<bvar::Adder<int>> g_executeImpl_Clustering_minute("DaemonManager_Internal", "executeImpl_Clustering", & g_executeImpl_Clustering, 60);
}/// end namespace

