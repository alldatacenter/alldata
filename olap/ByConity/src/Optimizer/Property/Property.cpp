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

#include <Optimizer/Property/Property.h>

#include <Functions/FunctionsHashing.h>
#include <Optimizer/SymbolEquivalencesDeriver.h>

namespace DB
{
size_t Partitioning::hash() const
{
    size_t hash = IntHash64Impl::apply(static_cast<UInt8>(handle));

    hash = MurmurHash3Impl64::combineHashes(hash, IntHash64Impl::apply(columns.size()));
    for (const auto & column : columns)
        hash = MurmurHash3Impl64::combineHashes(hash, MurmurHash3Impl64::apply(column.c_str(), column.size()));

    hash = MurmurHash3Impl64::combineHashes(hash, IntHash64Impl::apply(require_handle));
    hash = MurmurHash3Impl64::combineHashes(hash, IntHash64Impl::apply(buckets));
    hash = MurmurHash3Impl64::combineHashes(hash, IntHash64Impl::apply(enforce_round_robin));
    return hash;
}

bool Partitioning::satisfy(const Partitioning & requirement) const
{
    if (requirement.require_handle)
        return getPartitioningHandle() == requirement.getPartitioningHandle() && getBuckets() == requirement.getBuckets()
            && getPartitioningColumns() == requirement.getPartitioningColumns()
            && ASTEquality::compareTree(sharding_expr, requirement.sharding_expr);

    switch (requirement.getPartitioningHandle())
    {
        case Handle::FIXED_HASH:
            return getPartitioningColumns() == requirement.getPartitioningColumns() || this->isPartitionOn(requirement);
        default:
            return getPartitioningHandle() == requirement.getPartitioningHandle() && getBuckets() == requirement.getBuckets()
                && getPartitioningColumns() == requirement.getPartitioningColumns()
                && ASTEquality::compareTree(sharding_expr, requirement.sharding_expr);
    }
}

bool Partitioning::isPartitionOn(const Partitioning & requirement) const
{
    auto actual_columns = getPartitioningColumns();
    auto required_columns = requirement.getPartitioningColumns();
    std::unordered_set<std::string> required_columns_set;

    if (actual_columns.empty())
        return false;

    for (auto & required_column : required_columns)
    {
        required_columns_set.insert(required_column);
    }

    for (auto & actual_column : actual_columns)
    {
        if (!required_columns_set.count(actual_column))
        {
            return false;
        }
    }

    return true;
}

Partitioning Partitioning::normalize(const SymbolEquivalences & symbol_equivalences) const
{
    auto mapping = symbol_equivalences.representMap();
    for (const auto & item : columns)
    {
        if (!mapping.contains(item))
        {
            mapping[item] = item;
        }
    }
    return translate(mapping);
}

Partitioning Partitioning::translate(const std::unordered_map<String, String> & identities) const
{
    Names translate_columns;
    for (const auto & column : columns)
        if (identities.contains(column))
            translate_columns.emplace_back(identities.at(column));
        else // note: don't discard column
            translate_columns.emplace_back(column);
    return Partitioning{handle, translate_columns, require_handle, buckets, sharding_expr, enforce_round_robin};
}

String Partitioning::toString() const
{
    switch (handle)
    {
        case Handle::SINGLE:
            return "SINGLE";
        case Handle::COORDINATOR:
            return "COORDINATOR";
        case Handle::FIXED_HASH:
            if (columns.empty())
                return "[]";
            else
            {
                auto result = "["
                    + std::accumulate(
                                  std::next(columns.begin()),
                                  columns.end(),
                                  columns[0],
                                  [](String a, const String & b) { return std::move(a) + ", " + b; })
                    + "]";
                if (enforce_round_robin)
                    result += "RR";
                return result;
            }
        case Handle::FIXED_ARBITRARY:
            return "FIXED_ARBITRARY";
        case Handle::FIXED_BROADCAST:
            return "BROADCAST";
        case Handle::SCALED_WRITER:
            return "SCALED_WRITER";
        case Handle::BUCKET_TABLE:
            return "BUCKET_TABLE";
        case Handle::ARBITRARY:
            return "ARBITRARY";
        case Handle::FIXED_PASSTHROUGH:
            return "FIXED_PASSTHROUGH";
        default:
            return "UNKNOWN";
    }
}

size_t SortColumn::hash() const
{
    size_t hash = MurmurHash3Impl64::apply(name.c_str(), name.size());
    hash = MurmurHash3Impl64::combineHashes(hash, IntHash64Impl::apply(static_cast<UInt8>(order)));
    return hash;
}

String SortColumn::toString() const
{
    switch (order)
    {
        case SortOrder::ASC_NULLS_FIRST:
            return name + "↑↑";
        case SortOrder::ASC_NULLS_LAST:
            return name + "↑↓";
        case SortOrder::DESC_NULLS_FIRST:
            return name + "↓↑";
        case SortOrder::DESC_NULLS_LAST:
            return name + "↓↓";
    }
    return "unknown";
}

size_t Sorting::hash() const
{
    size_t hash = IntHash64Impl::apply(this->size());
    for (const auto & item : *this)
        hash = MurmurHash3Impl64::combineHashes(hash, item.hash());
    return hash;
}

Sorting Sorting::translate(const std::unordered_map<String, String> & identities) const
{
    Sorting result;
    result.reserve(this->size());
    for (auto & item : *this)
        if (identities.contains(item.getName()))
            result.emplace_back(SortColumn{identities.at(item.getName()), item.getOrder()});
        else
            result.emplace_back(item);
    return result;
}

String Sorting::toString() const
{
    return std::accumulate(
        std::next(begin()), end(), front().toString(), [](std::string a, const auto & b) { return std::move(a) + '-' + b.toString(); });
}


size_t CTEDescriptions::hash() const
{
    size_t hash = IntHash64Impl::apply(this->size());
    for (const auto & item : *this)
    {
        hash = MurmurHash3Impl64::combineHashes(hash, IntHash64Impl::apply(item.first));
        hash = MurmurHash3Impl64::combineHashes(hash, item.second.hash());
    }
    return hash;
}

String CTEDescription::toString() const
{
    std::stringstream output;
    output << node_partitioning.toString();
    if (stream_partitioning.getPartitioningHandle() != Partitioning::Handle::ARBITRARY)
        output << "/" << stream_partitioning.toString();
    if (!sorting.empty())
        output << " " << sorting.toString();
    return output.str();
}

String CTEDescriptions::toString() const
{
    auto it = begin();
    if (it == end())
        return "";
    std::stringstream output;
    output << "CTE(" << it->first << ")=" << it->second.toString();
    while (++it != end())
        output << ","
               << "CTE(" << it->first << ")=" << it->second.toString();
    return output.str();
}

Property Property::translate(const std::unordered_map<String, String> & identities) const
{
    Property result{node_partitioning.translate(identities), stream_partitioning.translate(identities), sorting.translate(identities)};
    result.setPreferred(preferred);
    result.setCTEDescriptions(cte_descriptions.translate(identities));
    return result;
}

Property Property::normalize(const SymbolEquivalences & symbol_equivalences) const
{
    Property result{node_partitioning.normalize(symbol_equivalences), stream_partitioning.normalize(symbol_equivalences), sorting};
    result.setPreferred(preferred);
    result.setCTEDescriptions(cte_descriptions);
    return result;
}

size_t Property::hash() const
{
    size_t hash = IntHash64Impl::apply(preferred);
    hash = MurmurHash3Impl64::combineHashes(hash, node_partitioning.hash());
    hash = MurmurHash3Impl64::combineHashes(hash, stream_partitioning.hash());
    hash = MurmurHash3Impl64::combineHashes(hash, sorting.hash());
    hash = MurmurHash3Impl64::combineHashes(hash, cte_descriptions.hash());
    return hash;
}

String Property::toString() const
{
    std::stringstream output;
    output << node_partitioning.toString();
    if (stream_partitioning.getPartitioningHandle() != Partitioning::Handle::ARBITRARY)
        output << "/" << stream_partitioning.toString();
    if (preferred)
        output << "?";
    if (!sorting.empty())
        output << " " << sorting.toString();
    if (!cte_descriptions.empty())
        output << " " << cte_descriptions.toString();
    return output.str();
}

CTEDescriptions CTEDescriptions::filter(const std::unordered_set<CTEId> & allowed) const
{
    CTEDescriptions res;
    for (const auto & item : *this)
        if (allowed.count(item.first))
            res.emplace(item);
    return res;
}

size_t CTEDescription::hash() const
{
    size_t hash = node_partitioning.hash();
    hash = MurmurHash3Impl64::combineHashes(hash, stream_partitioning.hash());
    hash = MurmurHash3Impl64::combineHashes(hash, sorting.hash());
    return hash;
}

CTEDescription::CTEDescription(const Property & property)
    : CTEDescription(property.getNodePartitioning(), property.getStreamPartitioning(), property.getSorting())
{
}

bool CTEDescription::operator==(const CTEDescription & other) const
{
    return node_partitioning == other.node_partitioning && stream_partitioning == other.stream_partitioning && sorting == other.sorting;
}

Property CTEDescription::createCTEDefGlobalProperty(const Property & property, CTEId cte_id)
{
    auto cte_description = property.getCTEDescriptions().at(cte_id);
    // no need to translate.
    Property res{cte_description.node_partitioning, cte_description.stream_partitioning, cte_description.sorting};
    // copy other cte descriptions.
    res.setCTEDescriptions(property.getCTEDescriptions());
    res.getCTEDescriptions().erase(cte_id);
    return res;
}

Property
CTEDescription::createCTEDefGlobalProperty(const Property & property, CTEId cte_id, const std::unordered_set<CTEId> & contains_cte_ids)
{
    auto cte_description = property.getCTEDescriptions().at(cte_id);
    // no need to translate.
    Property res{cte_description.node_partitioning, cte_description.stream_partitioning, cte_description.sorting};
    // copy other cte descriptions.
    res.setCTEDescriptions(property.getCTEDescriptions().filter(contains_cte_ids));
    res.getCTEDescriptions().erase(cte_id);
    return res;
}

Property CTEDescription::createCTEDefLocalProperty(
    const Property & property, CTEId cte_id, const std::unordered_map<String, String> & identities_mapping)
{
    auto res = property.translate(identities_mapping);
    res.getCTEDescriptions().erase(cte_id);
    return res;
}

CTEDescription CTEDescription::translate(const std::unordered_map<String, String> & identities) const
{
    return CTEDescription{
        node_partitioning.translate(identities), stream_partitioning.translate(identities), sorting.translate(identities)};
}

CTEDescriptions CTEDescriptions::translate(const std::unordered_map<String, String> & identities) const
{
    CTEDescriptions result;
    for (auto & item : *this)
        result.emplace(item.first, item.second.translate(identities));
    return result;
}

}
