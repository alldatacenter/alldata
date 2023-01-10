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

#include <Interpreters/Context.h>
#include <Interpreters/WorkerGroupHandle.h>
#include <Storages/IStorage.h>
#include <Storages/MergeTree/MergeTreeDataPartCNCH_fwd.h>
#include <Storages/Hive/HiveDataPart_fwd.h>
#include <Catalog/DataModelPartWrapper_fwd.h>
#include <Common/ConsistentHashUtils/ConsistentHashRing.h>

namespace DB
{

using WorkerList = std::vector<String>;
using ServerAssignmentMap = std::unordered_map<String, ServerDataPartsVector>;
using HivePartsAssignMap = std::unordered_map<String, HiveDataPartsCNCHVector>;
using AssignmentMap = std::unordered_map<String, MergeTreeDataPartsCNCHVector>;
using BucketNumbersAssignmentMap = std::unordered_map<String, std::set<Int64>>;

struct BucketNumberAndServerPartsAssignment
{
    ServerAssignmentMap parts_assignment_map;
    BucketNumbersAssignmentMap bucket_number_assignment_map;
};

// the hive has different allocate logic, thus separate it.
HivePartsAssignMap assignCnchHiveParts(const WorkerGroupHandle & worker_group, const HiveDataPartsCNCHVector & parts);

template <typename DataPartsCnchVector>
std::unordered_map<String, DataPartsCnchVector> assignCnchParts(const WorkerGroupHandle & worker_group, const DataPartsCnchVector & parts);

bool isCnchBucketTable(const ContextPtr & context, const IStorage & storage, const ServerDataPartsVector & parts);
BucketNumberAndServerPartsAssignment assignCnchPartsForBucketTable(const ServerDataPartsVector & parts, WorkerList workers, std::set<Int64> required_bucket_numbers = {});

}
