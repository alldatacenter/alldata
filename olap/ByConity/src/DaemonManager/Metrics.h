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

#include <bvar/bvar.h>

namespace DB::DaemonManager::BRPCMetrics
{
    extern bvar::Adder< int > g_executeImpl_PartGC_error;
    extern bvar::Adder< int > g_executeImpl_PartGC;
    extern bvar::Adder< int > g_executeImpl_MergeMutate_error;
    extern bvar::Adder< int > g_executeImpl_MergeMutate;
    extern bvar::Adder< int > g_executeImpl_Consumer_error;
    extern bvar::Adder< int > g_executeImpl_Consumer;
    extern bvar::Adder< int > g_executeImpl_MemoryBuffer_error;
    extern bvar::Adder< int > g_executeImpl_MemoryBuffer;
    extern bvar::Adder< int > g_executeImpl_DedupWorker_error;
    extern bvar::Adder< int > g_executeImpl_DedupWorker;
    extern bvar::Adder< int > g_executeImpl_GlobalGC_error;
    extern bvar::Adder< int > g_executeImpl_GlobalGC;
    extern bvar::Adder< int > g_executeImpl_TxnGC_error;
    extern bvar::Adder< int > g_executeImpl_TxnGC;
    extern bvar::Adder< int > g_executeImpl_Clustering_error;
    extern bvar::Adder< int > g_executeImpl_Clustering;
}/// end namespace

