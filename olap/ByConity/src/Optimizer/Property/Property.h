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

#include <Core/Names.h>
#include <Core/SortDescription.h>
#include <Core/Types.h>
#include <Optimizer/Equivalences.h>

#include <map>
#include <utility>
#include <vector>
#include <Core/Names.h>
#include <Core/SortDescription.h>
#include <Core/Types.h>
#include <Functions/FunctionsHashing.h>
#include <Parsers/IAST_fwd.h>
#include <Analyzers/ASTEquals.h>

namespace DB
{
class Property;
using PropertySet = std::vector<Property>;
using PropertySets = std::vector<PropertySet>;
using SymbolEquivalences = Equivalences<String>;

using CTEId = UInt32;

/**
 * A partition operation divides a relation into disjoint subsets, called partitions.
 * A partition function defines which rows belong to which partitions. Partitioning
 * applies to the whole relation.
 */
class Partitioning
{
public:
    enum class Handle : UInt8
    {
        SINGLE = 0,
        COORDINATOR,
        FIXED_HASH,
        FIXED_ARBITRARY,
        FIXED_BROADCAST,
        SCALED_WRITER,
        BUCKET_TABLE,
        ARBITRARY,
        FIXED_PASSTHROUGH,
        UNKNOWN
    };

    enum class Type : UInt8
    {
        UNKNOWN = 0,
        LOCAL,
        DISTRIBUTED,
    };

    Partitioning(const Names & columns_) : Partitioning(Handle::FIXED_HASH, columns_) { }

    Partitioning(
        enum Handle handle_ = Handle::UNKNOWN,
        Names columns_ = {},
        bool require_handle_ = false,
        UInt64 buckets_ = 0,
        ASTPtr sharding_expr_ = nullptr,
        bool enforce_round_robin_ = true)
        : handle(handle_)
        , columns(std::move(columns_))
        , require_handle(require_handle_)
        , buckets(buckets_)
        , sharding_expr(sharding_expr_)
        , enforce_round_robin(enforce_round_robin_)
    {
    }
    void setHandle(Handle handle_) { handle = handle_; }
    enum Handle getPartitioningHandle() const { return handle; }
    const Names & getPartitioningColumns() const { return columns; }
    UInt64 getBuckets() const { return buckets; }
    ASTPtr getSharingExpr() const { return sharding_expr; }
    bool isEnforceRoundRobin() const { return enforce_round_robin; }
    void setEnforceRoundRobin(bool enforce_round_robin_) { enforce_round_robin = enforce_round_robin_; }
    bool isRequireHandle() const { return require_handle; }
    void setRequireHandle(bool require_handle_) { require_handle = require_handle_; }
    ASTPtr getSharingExpr() { return sharding_expr; }


    Partitioning translate(const std::unordered_map<String, String> & identities) const;
    Partitioning normalize(const SymbolEquivalences & symbol_equivalences) const;
    bool satisfy(const Partitioning &) const;
    bool isPartitionOn(const Partitioning &) const;

    size_t hash() const;
    bool operator==(const Partitioning & other) const
    {
        return handle == other.handle && columns == other.columns && require_handle == other.require_handle && buckets == other.buckets
            && enforce_round_robin == other.enforce_round_robin && ASTEquality::compareTree(sharding_expr, other.sharding_expr);
    }
    String toString() const;

private:
    enum Handle handle;
    Names columns;
    bool require_handle;
    UInt64 buckets;
    ASTPtr sharding_expr;
    bool enforce_round_robin;
};

class Grouping
{
public:
    explicit Grouping(Names columns_) : columns(std::move(columns_)) { }
    Names getColumns() { return columns; }

private:
    Names columns;
};


enum class SortOrder : UInt8
{
    ASC_NULLS_FIRST,
    ASC_NULLS_LAST,
    DESC_NULLS_FIRST,
    DESC_NULLS_LAST
};

class SortColumn
{
public:
    SortColumn(String name_, SortOrder order_) : name(std::move(name_)), order(order_) { }
    explicit SortColumn(const SortColumnDescription & sort_column_description) : name(sort_column_description.column_name)
    {
        if (sort_column_description.direction == 1)
        {
            if (sort_column_description.nulls_direction == 1)
                order = SortOrder::ASC_NULLS_LAST;
            else
                order = SortOrder::ASC_NULLS_FIRST;
        }
        else
        {
            if (sort_column_description.nulls_direction == 1)
                order = SortOrder::DESC_NULLS_FIRST;
            else
                order = SortOrder::DESC_NULLS_LAST;
        }
    }

    const String & getName() const { return name; }
    SortOrder getOrder() const { return order; }

    bool operator==(const SortColumn & other) const { return name == other.name && order == other.order; }
    size_t hash() const;
    String toString() const;

private:
    String name;
    SortOrder order;
};

class Sorting : public std::vector<SortColumn>
{
public:
    Sorting() = default;
    explicit Sorting(const SortDescription & sort_description)
    {
        for (const auto & item : sort_description)
            emplace_back(SortColumn(item));
    }

    Sorting translate(const std::unordered_map<String, String> & identities) const;
    size_t hash() const;
    String toString() const;
};

class CTEDescription
{
public:
    explicit CTEDescription()
        : CTEDescription(Partitioning(Partitioning::Handle::ARBITRARY), Partitioning(Partitioning::Handle::ARBITRARY), {})
    {
    }

    explicit CTEDescription(const Property &);

    bool operator==(const CTEDescription & other) const;
    size_t hash() const;
    String toString() const;

    CTEDescription translate(const std::unordered_map<String, String> & identities) const;
    const Partitioning & getNodePartitioning() const { return node_partitioning; }
    Partitioning & getNodePartitioningRef() { return node_partitioning; }

    static Property createCTEDefGlobalProperty(const Property & property, CTEId cte_id);
    static Property createCTEDefGlobalProperty(const Property & property, CTEId cte_id, const std::unordered_set<CTEId> & contains_cte_ids);
    static Property
    createCTEDefLocalProperty(const Property & property, CTEId cte_id, const std::unordered_map<String, String> & identities_mapping);

private:
    explicit CTEDescription(Partitioning node_partitioning_, Partitioning stream_partitioning_, Sorting sorting_)
        : node_partitioning(std::move(node_partitioning_))
        , stream_partitioning(std::move(stream_partitioning_))
        , sorting(std::move(sorting_))
    {
    }

    // Description of the partitioning of the data across nodes
    Partitioning node_partitioning;
    // Description of the partitioning of the data across streams
    Partitioning stream_partitioning;
    // Description of the sort order of the columns
    Sorting sorting;
};

class CTEDescriptions : public std::map<CTEId, CTEDescription>
{
public:
    using std::map<CTEId, CTEDescription>::map;
    size_t hash() const;
    CTEDescriptions translate(const std::unordered_map<String, String> & identities) const;
    CTEDescriptions filter(const std::unordered_set<CTEId> & allowed) const;
    String toString() const;
};


class Constants
{
public:
private:
    std::map<String, String> values;
};

class FunctionalDependency
{
};

class Property
{
public:
    explicit Property(
        Partitioning node_partitioning_ = Partitioning(Partitioning::Handle::ARBITRARY),
        Partitioning stream_partitioning_ = Partitioning(Partitioning::Handle::ARBITRARY),
        Sorting sorting_ = {})
        : node_partitioning(std::move(node_partitioning_))
        , stream_partitioning(std::move(stream_partitioning_))
        , sorting(std::move(sorting_))
    {
    }

    bool isPreferred() const { return preferred; }
    const Partitioning & getNodePartitioning() const { return node_partitioning; }
    Partitioning & getNodePartitioningRef() { return node_partitioning; }
    const Partitioning & getStreamPartitioning() const { return stream_partitioning; }
    const Sorting & getSorting() const { return sorting; }
    const CTEDescriptions & getCTEDescriptions() const { return cte_descriptions; }
    CTEDescriptions & getCTEDescriptions() { return cte_descriptions; }

    void setPreferred(bool preferred_) { preferred = preferred_; }
    void setNodePartitioning(Partitioning node_partitioning_) { node_partitioning = std::move(node_partitioning_); }
    void setStreamPartitioning(Partitioning stream_partitioning_) { stream_partitioning = std::move(stream_partitioning_); }
    void setCTEDescriptions(CTEDescriptions descriptions) { cte_descriptions = std::move(descriptions); }

    Property translate(const std::unordered_map<String, String> & identities) const;
    Property normalize(const SymbolEquivalences & symbol_equivalences) const;

    bool operator==(const Property & other) const
    {
        return preferred == other.preferred && node_partitioning == other.node_partitioning
            && stream_partitioning == other.stream_partitioning && sorting == other.sorting
            && cte_descriptions == other.cte_descriptions;
    }

    bool operator!=(const Property & other) const { return !(*this == other); }

    size_t hash() const;
    String toString() const;

private:
    // Description whether the property is required or preferred.
    bool preferred = false;
    // Description of the partitioning of the data across nodes
    Partitioning node_partitioning;
    // Description of the partitioning of the data across streams
    Partitioning stream_partitioning;
    // Description of the sort order of the columns
    Sorting sorting;
    // Description of the requirements of the common table expressions.
    CTEDescriptions cte_descriptions;
    // Grouping grouping;
    // Constants constants;
    // FunctionalDependency fd;
};

/**
 * Defines struct for hashing a property set
 */
struct PropertyHash
{
    /**
     * Hashes a Property
     * @param property Property to hash
     * @returns hash code
     */
    std::size_t operator()(const Property & property) const { return property.hash(); }
};

/**
 * Defines struct for hashing CTEDescription
 */
struct CTEDescriptionHash
{

    /**
     * Hashes a CTEDescription
     * @param cte_description CTEDescription to hash
     * @returns hash code
     */
    std::size_t operator()(const CTEDescription & cte_description) const { return cte_description.hash(); }
};

}
