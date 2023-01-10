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
#include <Optimizer/value_sets.h>
#include <Optimizer/FunctionInvoker.h>
#include <utility>

namespace DB::Predicate
{
class Domain;
class TupleDomain;

using Domains = std::vector<Domain>;
using TupleDomains = std::vector<TupleDomain>;
using FieldWithTypeMap = std::unordered_map<String, FieldWithType>;
class Domain
{
private:
    ValueSet value_set;
    bool null_allowed;

public:
    Domain(ValueSet value_set_, bool null_allowed_) : value_set(std::move(value_set_)), null_allowed(null_allowed_) { }

    const ValueSet & getValueSet() const { return value_set; }
    bool isNullAllowed() const { return null_allowed; }
    DataTypePtr getType() const
    {
        return std::visit([](auto & v) { return v.getType(); }, value_set);
    }
    bool valueSetIsNone() const
    {
        return std::visit([](auto & v) { return v.isNone(); }, value_set);
    }
    bool valueSetIsAll() const
    {
        return std::visit([](auto & v) { return v.isAll(); }, value_set);
    }
    bool valueSetIsSingleValue() const
    {
        return std::visit([](auto & v) { return v.isSingleValue(); }, value_set);
    }
    bool isNone() const { return valueSetIsNone() && !null_allowed; }
    bool isAll() const { return valueSetIsAll() && null_allowed; }
    bool isSingleValue() const { return valueSetIsSingleValue() && !null_allowed; }
    bool isNullableSingleValue() const //null or a singleValue
    {
        return null_allowed ? valueSetIsNone() : valueSetIsSingleValue();
    }
    bool isOnlyNull() const { return valueSetIsNone() && null_allowed; }
    const Field & getSingleValue() const;
    Field getNullableSingleValue() const;
    bool includesNullableValue(const Field & value) const;
    bool isNullableDiscreteSet() const; // If there are only a 'null' value or there are discrete values;
    Array getNullableDiscreteSet() const;
    Domain intersect(const Domain & other) const;
    Domain unionn(const Domain & other) const;
    Domain complement();
    Domain subtract(const Domain & other);
    bool overlaps(const Domain & other) const;
    bool contains(const Domain & other) const;
    bool operator==(const Domain & other) const;

    static Domain none(const DataTypePtr & type) { return {createNone(type), false}; }
    static Domain all(const DataTypePtr & type) { return {createAll(type), true}; }
    static Domain onlyNull(const DataTypePtr & type) { return {createNone(type), true}; }
    static Domain notNull(const DataTypePtr & type) { return {createAll(type), false}; }
    static Domain singleValue(const DataTypePtr & type, const Field & value) { return singleValue(type, value, false); }
    static Domain singleValue(const DataTypePtr & type, const Field & value, bool null_allowed)
    {
        return {createSingleValueSet(type, value), null_allowed};
    }
    static Domain multipleValues(const DataTypePtr & type, const Array & values) { return multipleValues(type, values, false); }
    static Domain multipleValues(const DataTypePtr & type, const Array & values, bool null_allowed);
    static Domain unionDomains(const Domains & domains);

private:
    //TODO: common method
    template<typename F>
    auto visitOnSameType(const F & visitor, const ValueSet & other_value_set) const -> decltype(visitor(value_set, other_value_set))
    {
        using RetType = decltype(visitor(value_set, other_value_set));

        return std::visit([&](const auto & a, const auto & b) -> RetType
                          {
                              using TA = std::decay_t<decltype(a)>;
                              using TB = std::decay_t<decltype(b)>;

                              if constexpr (std::is_same_v<TA, TB>)
                                  return visitor(a, b);
                              else
                                  throw Exception("Incompatible value set types", ErrorCodes::LOGICAL_ERROR);
                          }, value_set, other_value_set);
    }
};

using DomainMap = std::unordered_map<String, Domain>;

/** TupleDomain defines a set of valid tuples according to the constraints on each of its constituent columns
    * TupleDomain is internally represented as a normalized map of each column to its
    * respective allowable value Domain. Conceptually, these Domains can be thought of
    * as being AND'ed together to form the representative predicate.
    * <p>
    * This map is normalized in the following ways:
    * 1) The map will not contain Domain.none() as any of its values. If any of the Domain
    * values are Domain.none(), then the whole map will instead be null. This enforces the fact that
    * any single Domain.none() value effectively turns this TupleDomain into "none" as well.
    * 2) The map will not contain Domain.all() as any of its values. Our convention here is that
    * any unmentioned column is equivalent to having Domain.all(). To normalize this structure,
    * we remove any Domain.all() values from the map.
    */
class TupleDomain
{
private:
    bool is_none;
    DomainMap domains;

public:
    explicit TupleDomain(bool is_none_): is_none(is_none_) {}
    explicit TupleDomain(DomainMap domains_);

    DomainMap getDomains() const { return domains; }
    size_t getDomainCount() const { return domains.size(); }
    const Domain & getOnlyElement() const { return domains.begin()->second; }
    bool domainsIsEmpty() const { return domains.empty(); }
    bool isNone() const { return is_none; }
    bool isAll() const { return !is_none && domains.empty(); }
    bool haveSpecificDomain(const String & column) const { return domains.count(column); }
    TupleDomain intersect(const TupleDomain & other) { return intersect(std::vector<TupleDomain>{other, *this}); }
    bool contains(const TupleDomain & other) const;
    bool overlaps(const TupleDomain & other) const;
    bool operator==(const TupleDomain & other) const
    {
        return is_none == other.isNone() && domains == other.getDomains();
    }
    std::optional<FieldWithTypeMap> extractFixedValues() const;
    std::optional<std::unordered_map<String, Array>> extractDiscreteValues() const;

    static TupleDomain none() { return TupleDomain{true}; }
    static TupleDomain all() { return TupleDomain{false}; }

    static TupleDomain fromFixedValues(const FieldWithTypeMap & fixed_values);
    static TupleDomain intersect(const std::vector<TupleDomain> & others);
    static std::optional<TupleDomain> maximal(const std::vector<TupleDomain> & domains);
    static TupleDomain columnWiseUnion(const std::vector<TupleDomain> & tuple_domains);
};

}
