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

#include <Optimizer/Property/PropertyMatcher.h>

#include <Interpreters/Context.h>

namespace DB
{
bool PropertyMatcher::matchNodePartitioning(
    const Context & context, Partitioning & required, const Partitioning & actual, const SymbolEquivalences & equivalences)
{
    if (required.getPartitioningHandle() == Partitioning::Handle::ARBITRARY)
        return true;

    if (required.getPartitioningHandle() == Partitioning::Handle::FIXED_HASH && context.getSettingsRef().enforce_round_robin
        && required.isEnforceRoundRobin() && actual.normalize(equivalences).satisfy(required.normalize(equivalences)))
    {
        required.setHandle(Partitioning::Handle::FIXED_ARBITRARY);
        return false;
    }

    return actual.normalize(equivalences).satisfy(required.normalize(equivalences));
}

bool PropertyMatcher::matchStreamPartitioning(
    const Context &, const Partitioning & required, const Partitioning & actual, const SymbolEquivalences & equivalences)
{
    if (required.getPartitioningHandle() == Partitioning::Handle::ARBITRARY)
        return true;
    return required.normalize(equivalences) == actual.normalize(equivalences);
}

Property PropertyMatcher::compatibleCommonRequiredProperty(const std::unordered_set<Property, PropertyHash> & required_properties)
{
    if (required_properties.empty())
        return Property{};
    if (required_properties.size() == 1)
        return Property{required_properties.begin()->getNodePartitioning()};

    Property res;
    bool is_all_broadcast = !required_properties.empty();
    auto it = required_properties.begin();
    for (; it != required_properties.end(); ++it)
    {
        auto partition_handle = it->getNodePartitioning().getPartitioningHandle();
        is_all_broadcast &= partition_handle == Partitioning::Handle::FIXED_BROADCAST;
        if (partition_handle == Partitioning::Handle::ARBITRARY || partition_handle == Partitioning::Handle::FIXED_ARBITRARY
            || partition_handle == Partitioning::Handle::FIXED_BROADCAST)
            continue;
        res = *it;
        break;
    }


    const auto & node_partition = res.getNodePartitioning();
    const auto handle = node_partition.getPartitioningHandle();
    std::unordered_set<String> columns_set;
    for (const auto & item : node_partition.getPartitioningColumns())
        columns_set.emplace(item);

    for (; it != required_properties.end(); ++it)
    {
        auto partition_handle = it->getNodePartitioning().getPartitioningHandle();
        is_all_broadcast &= partition_handle == Partitioning::Handle::FIXED_BROADCAST;
        if (partition_handle == Partitioning::Handle::ARBITRARY || partition_handle == Partitioning::Handle::FIXED_ARBITRARY
            || partition_handle == Partitioning::Handle::FIXED_BROADCAST)
            continue;

        if (partition_handle != handle)
            return Property{};

        if (partition_handle == Partitioning::Handle::FIXED_HASH)
        {
            std::unordered_set<String> intersection;
            const auto & partition_columns = it->getNodePartitioning().getPartitioningColumns();
            std::copy_if(
                partition_columns.begin(),
                partition_columns.end(),
                std::inserter(intersection, intersection.begin()),
                [&columns_set](const auto & e) { return columns_set.contains(e); });
            if (intersection.empty())
                return Property{}; // no common property
            columns_set.swap(intersection);
        }
    }

    if (is_all_broadcast)
        return Property{Partitioning{Partitioning::Handle::FIXED_BROADCAST}};

    Names partition_columns{columns_set.begin(), columns_set.end()};

    // no need to consider require_handle / buckets / enforce_round_robin for required property
    return Property{Partitioning{handle, std::move(partition_columns)}};
}
}
