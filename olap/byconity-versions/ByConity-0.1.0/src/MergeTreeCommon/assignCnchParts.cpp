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

#include <MergeTreeCommon/assignCnchParts.h>
#include <Catalog/Catalog.h>
#include <Catalog/DataModelPartWrapper.h>
#include <common/logger_useful.h>

#include <sstream>

namespace DB
{

template<typename M>
inline void reportStats(const M & map, const String & name, size_t num_workers)
{
    std::stringstream ss;
    double sum = 0;
    double max_load = 0;
    for (const auto & it : map)
    {
        sum += it.second.size();
        max_load = std::max(max_load, static_cast<double>(it.second.size()));
        ss << it.first << " -> " << it.second.size() << "; ";
    }
    /// calculate coefficient of variance of the distribution
    double mean = sum / num_workers;
    double sum_of_squares = 0;
    for (const auto & it : map)
    {
        double diff = it.second.size() - mean;
        sum_of_squares += diff * diff;
    }
    /// coefficient of variance, lower is better
    double co_var = std::sqrt(sum_of_squares / num_workers) / mean;
    /// peak over average ratio, lower is better and must not exceed the load factor if use bounded load balancing
    double peak_over_avg = max_load / mean;
    ss << "stats: " << co_var << " " << peak_over_avg << std::endl;
    LOG_DEBUG(&Poco::Logger::get(name), ss.str());
}

/// explicit instantiation for server part and cnch data part.
template ServerAssignmentMap assignCnchParts<ServerDataPartsVector>(const WorkerGroupHandle & worker_group, const ServerDataPartsVector & parts);
template AssignmentMap assignCnchParts<MergeTreeDataPartsCNCHVector>(const WorkerGroupHandle & worker_group, const MergeTreeDataPartsCNCHVector & parts);

template <typename DataPartsCnchVector>
std::unordered_map<String, DataPartsCnchVector> assignCnchParts(const WorkerGroupHandle & worker_group, const DataPartsCnchVector & parts)
{
    switch (worker_group->getContext()->getPartAllocationAlgo())
    {
        case Context::PartAllocator::JUMP_CONSISTENT_HASH:
        {
            auto ret = assignCnchPartsWithJump(worker_group->getWorkerIDVec(), parts);
            reportStats(ret, "Jump Consistent Hash", worker_group->getWorkerIDVec().size());
            return ret;
        }
        case Context::PartAllocator::RING_CONSISTENT_HASH:
        {
            if (!worker_group->hasRing())
            {
                LOG_WARNING(&Poco::Logger::get("Consistent Hash"), "Attempt to use ring-base consistent hash, but ring is empty; fall back to jump");
                return assignCnchPartsWithJump(worker_group->getWorkerIDVec(), parts);
            }
            auto ret = assignCnchPartsWithRingAndBalance(worker_group->getRing(), parts);
            reportStats(ret, "Bounded-load Consistent Hash", worker_group->getRing().size());
            return ret;
        }
        case Context::PartAllocator::STRICT_RING_CONSISTENT_HASH:
        {
            if (!worker_group->hasRing())
            {
                LOG_WARNING(&Poco::Logger::get("Strict Consistent Hash"), "Attempt to use ring-base consistent hash, but ring is empty; fall back to jump");
                return assignCnchPartsWithJump(worker_group->getWorkerIDVec(), parts);
            }
            auto ret = assignCnchPartsWithStrictBoundedHash(worker_group->getRing(), parts, true);
            reportStats(ret, "Strict Consistent Hash", worker_group->getRing().size());
            return ret;
        }
    }
}

template <typename DataPartsCnchVector>
std::unordered_map<String, DataPartsCnchVector> assignCnchPartsWithJump(WorkerList workers, const DataPartsCnchVector & parts)
{
    std::unordered_map<String, DataPartsCnchVector> ret;
    /// we don't know the order of workers returned from consul so sort then explicitly now
    sort(workers.begin(), workers.end());
    auto num_workers = workers.size();
    for (const auto & part : parts)
    {
        /// use crc64 as original implementation, may change to other hash later
        auto part_name = part->get_info().getBasicPartName();
        auto hash_val = fio_crc64(reinterpret_cast<const unsigned char *>(part_name.c_str()), part_name.length());
        auto index = JumpConsistentHash(hash_val, num_workers);
        ret[workers[index]].emplace_back(part);
    }
    return ret;
}

/// 2 round apporach
template <typename DataPartsCnchVector>
std::unordered_map<String, DataPartsCnchVector> assignCnchPartsWithRingAndBalance(const ConsistentHashRing & ring, const DataPartsCnchVector & parts)
{
    LOG_INFO(&Poco::Logger::get("Consistent Hash"), "Start to allocate part with bounded ring based hash policy.");
    std::unordered_map<String, DataPartsCnchVector> ret;
    size_t num_parts = parts.size();
    auto cap_limit = ring.getCapLimit(num_parts);
    DataPartsCnchVector exceed_parts;
    std::unordered_map<String, UInt64> stats;

    // first round, try respect original hash mapping as much as possible
    for (auto & part : parts)
    {
        if (auto hostname = ring.tryFind(part->get_info().getBasicPartName(), cap_limit, stats); !hostname.empty())
            ret[hostname].emplace_back(part);
        else
            exceed_parts.emplace_back(part);
    }

    // second round to assign the overloaded parts, reuse the one round apporach `findAndRebalance`.
    for (auto & part: exceed_parts)
    {
        auto hostname = ring.findAndRebalance(part->get_info().getBasicPartName(), cap_limit, stats);
        ret[hostname].emplace_back(part);
    }


    LOG_INFO(&Poco::Logger::get("Consistent Hash"),
             "Finish allocate part with bounded ring based hash policy, # of overloaded parts {}.", exceed_parts.size());
    return ret;
}

// 1 round approach
template <typename DataPartsCnchVector>
std::unordered_map<String, DataPartsCnchVector> assignCnchPartsWithStrictBoundedHash(const ConsistentHashRing & ring, const DataPartsCnchVector & parts, bool strict)
{
    LOG_INFO(&Poco::Logger::get("Strict Bounded Consistent Hash"), "Start to allocate part with bounded ring based hash policy under strict mode " + std::to_string(strict) + ".");
    std::unordered_map<String, DataPartsCnchVector> ret;
    size_t cap_limit = 0;
    std::unordered_map<String, UInt64> stats;

    for (size_t i = 0; i < parts.size(); ++i)
    {
        auto & part = parts[i];
        cap_limit = ring.getCapLimit(i + 1, strict);
        auto hostname = ring.findAndRebalance(part->get_info().getBasicPartName(), cap_limit, stats);
        ret[hostname].emplace_back(part);
    }

    LOG_INFO(&Poco::Logger::get("Strict Bounded Consistent Hash"), "Finish allocate part with strict bounded ring based hash policy under strict mode " + std::to_string(strict) + ".");
    return ret;
}

HivePartsAssignMap assignCnchHiveParts(const WorkerGroupHandle & worker_group, const HiveDataPartsCNCHVector & parts)
{
    auto workers = worker_group->getWorkerIDVec();
    auto num_workers = workers.size();
    HivePartsAssignMap ret;
    for (size_t i = 0 ; i < parts.size(); i++)
    {
        auto index = i % num_workers;
        ret[workers[index]].emplace_back(parts[i]);
    }
    return ret;
}


bool isCnchBucketTable(const ContextPtr & context, const IStorage & storage, const ServerDataPartsVector & parts)
{
    if (!storage.isBucketTable())
        return false;
    if (context->getCnchCatalog()->isTableClustered(storage.getStorageUUID()))
        return true;

    return std::all_of(parts.begin(), parts.end(), [&](auto part)
        { return part->part_model().table_definition_hash() == storage.getTableHashForClusterBy() && part->part_model().bucket_number() != -1; });
}


BucketNumberAndServerPartsAssignment assignCnchPartsForBucketTable(const ServerDataPartsVector & parts, WorkerList workers, std::set<Int64> required_bucket_numbers)
{
    std::sort(workers.begin(), workers.end());
    BucketNumberAndServerPartsAssignment assignment;

    for (const auto & part : parts)
    {
        // For bucket tables, the parts with the same bucket number is assigned to the same worker.
        Int64 bucket_number = part->part_model().bucket_number();
        // if required_bucket_numbers is empty, assign parts as per normal
        if (required_bucket_numbers.size() == 0 || required_bucket_numbers.find(bucket_number) != required_bucket_numbers.end())
        {
            auto index = bucket_number % workers.size();
            assignment.parts_assignment_map[workers[index]].emplace_back(part);
            assignment.bucket_number_assignment_map[workers[index]].insert(bucket_number);
        }
    }
    return assignment;
}

}
