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

#include <Common/FieldVisitors.h>
#include <Common/SipHash.h>
#include <Optimizer/value_sets.h>
#include <algorithm>

namespace DB::Predicate
{

inline int compareBool(bool first, bool second)
{
    return static_cast<int>(first) - static_cast<int>(second);
}

size_t FieldHashing::operator()(const Field & f) const
{
    SipHash hash;
    applyVisitor(FieldVisitorHash(hash), f);
    return static_cast<size_t>(hash.get64());
}

FieldSet setUnion(const FieldSet & lhs, const FieldSet & rhs)
{
    FieldSet res_set(lhs);
    res_set.insert(rhs.begin(), rhs.end());
    return res_set;
}

FieldSet setIntersection(const FieldSet & lhs, const FieldSet & rhs) // NOLINT(misc-no-recursion)
{
    if (lhs.size() > rhs.size())
        return setIntersection(rhs, lhs);

    FieldSet res_set;
    for (const Field & key : lhs)
        if (rhs.count(key))
            res_set.insert(key);

    return res_set;
}


FieldSet setSubtract(const FieldSet & lhs, const FieldSet & rhs)
{
    FieldSet res_set;
    for (const Field & key : lhs)
        if (!rhs.count(key))
            res_set.insert(key);

    return res_set;
}

bool setOverlap(const FieldSet & lhs, const FieldSet & rhs) // NOLINT(misc-no-recursion)
{
    if (lhs.size() > rhs.size())
        return setOverlap(rhs, lhs);

    return std::any_of(lhs.begin(), lhs.end(), [&](auto & key) -> bool { return rhs.count(key); });
}

//if lhs contains all elements of rhs , then return true, else return false;
bool setContainAll(const FieldSet & lhs, const FieldSet & rhs)
{
    if (lhs.size() < rhs.size())
        return false;

    return std::all_of(rhs.begin(), rhs.end(), [&](auto & key) -> bool { return lhs.count(key); });
}

const Field & DiscreteValueSet::getSingleValue() const
{
    if (!isSingleValue())
    {
        throw Exception("DiscreteValueSet does not have just a single value", DB::ErrorCodes::LOGICAL_ERROR);
    }
    return *(values.begin());
}

DiscreteValueSet DiscreteValueSet::intersect(const DiscreteValueSet & other) const
{
    if (inclusive && other.isInclusive())
        return {type, true, setIntersection(values, other.getValues())};
    else if (inclusive)
        return {type, true, setSubtract(values, other.getValues())};
    else if (other.isInclusive())
        return {type, true, setSubtract(other.getValues(), values)};
    else
        return {type, false, setUnion(values, other.getValues())};
}


DiscreteValueSet DiscreteValueSet::unionn(const DiscreteValueSet & other) const
{
    if (inclusive && other.isInclusive())
        return {type, true, setUnion(values, other.getValues())};
    else if (inclusive)
        return {type, false, setSubtract(other.getValues(), values)};
    else if (other.isInclusive())
        return {type, false, setSubtract(values, other.getValues())};
    else
        return {type, false, setIntersection(values, other.getValues())};
}

DiscreteValueSet DiscreteValueSet::subtract(const DiscreteValueSet & other) const
{
    return intersect(other.complement());
}

DiscreteValueSet DiscreteValueSet::complement() const
{
    return {type, !inclusive, values};
}

bool DiscreteValueSet::contains(const DiscreteValueSet & other) const
{
    if (inclusive && other.isInclusive())
        return setContainAll(values, other.getValues());
    else if (inclusive)
        /// return false is correct for most cases, unless when `this.values` UNION `other.values` = all elements of universe
        return false;
    else if (other.isInclusive())
        return !setOverlap(values, other.getValues());
    else
        return setContainAll(other.getValues(), values);
}

bool DiscreteValueSet::overlaps(const DiscreteValueSet & other) const
{
    if (inclusive && other.isInclusive())
        return setOverlap(values, other.getValues());
    else if (inclusive)
        return !setContainAll(other.getValues(), values);
    else if (other.isInclusive())
        return !setContainAll(values, other.getValues());
    else
        /// return true is correct for most cases, unless when `this.values` UNION `other.values` = all elements of universe
        return true;
}

DiscreteValueSet DiscreteValueSet::createFromValues(const DataTypePtr & type, const Array & values)
{
    FieldSet set;
    for (const auto & value : values)
        set.insert(value);
    return {type, true, set};
}

Range::Range(const DataTypePtr & type_, bool low_inclusive_, Field low_value_, bool high_inclusive_, Field high_value_)
    : low_inclusive(low_inclusive_)
    , low_value(std::move(low_value_))
    , high_inclusive(high_inclusive_)
    , high_value(std::move(high_value_))
    , type(type_)
{
    if (low_value.isNull() && low_inclusive)
        throw Exception("low bound must be exclusive for low unbounded range", ErrorCodes::LOGICAL_ERROR);

    if (high_value.isNull() && high_inclusive)
        throw Exception("high bound must be exclusive for high unbounded range", ErrorCodes::LOGICAL_ERROR);

    is_single_value = false;
    if (!low_value.isNull() && !high_value.isNull())
    {
        if (low_value > high_value)
            throw Exception("high bound must be exclusive for low unbounded range", ErrorCodes::LOGICAL_ERROR);

        if (low_value == high_value)
        {
            if (!high_inclusive || !low_inclusive)
            {
                throw Exception("invalid bounds for single value range", ErrorCodes::LOGICAL_ERROR);
            }
            is_single_value = true;
        }
    }
    if ((!low_value.isNull() && DB::Utils::isFloatingPointNaN(type, low_value))
        || (!high_value.isNull() && DB::Utils::isFloatingPointNaN(type, high_value)))
        throw Exception("invalid bounds", ErrorCodes::LOGICAL_ERROR);
}

bool Range::operator==(const Range & other) const
{
    if (type->getTypeId() != other.getType()->getTypeId())
        throw Exception("types not match", DB::ErrorCodes::LOGICAL_ERROR);
    return low_inclusive == other.isLowInclusive() && low_value == other.getLowValue() && high_inclusive == other.isHighInclusive()
        && high_value == other.getHighValue();
}

int Range::compareLowBound(const Range & other) const
{
    bool first = isLowUnbounded();
    bool second = other.isLowUnbounded();
    if (first || second)
    {
        return compareBool(!first, !second);
    }

    if (low_value > other.getLowValue())
        return 1;
    if (low_value < other.getLowValue())
        return -1;

    return compareBool(!isLowInclusive(), !other.isLowInclusive());
}

int Range::compareHighBound(const Range & other) const
{
    bool first = isHighUnbounded();
    bool second = other.isHighUnbounded();
    if (first || second)
    {
        return compareBool(first, second);
    }

    if (high_value > other.getHighValue())
        return 1;
    if (high_value < other.getHighValue())
        return -1;

    return compareBool(isHighInclusive(), other.isHighInclusive());
}

int Range::compareTo(const Range & other) const
{
    if (auto ret = compareLowBound(other))
        return ret;
    else
        return compareHighBound(other);
}

bool Range::contains(const Range & other) const
{
    return compareLowBound(other) <= 0 && compareHighBound(other) >= 0;
}

Range Range::span(const Range & other) const
{
    int compare_low_bound = compareLowBound(other);
    int compare_high_bound = compareHighBound(other);
    return {
        type,
        compare_low_bound <= 0 ? low_inclusive : other.isLowInclusive(),
        compare_low_bound <= 0 ? low_value : other.getLowValue(),
        compare_high_bound >= 0 ? high_inclusive : other.isHighInclusive(),
        compare_high_bound >= 0 ? high_value : other.getHighValue()};
}

bool Range::isFullyBefore(const Range & other) const
{
    if (isHighUnbounded())
        return false;
    if (other.isLowUnbounded())
        return false;
    if (high_value < other.getLowValue())
        return true;
    if (high_value == other.getLowValue())
        return !high_inclusive || !other.isLowInclusive();
    return false;
}

bool Range::overlaps(const Range & other) const
{
    return !isFullyBefore(other) && !other.isFullyBefore(*this);
}

std::optional<Range> Range::intersect(const Range & other) const
{
    if (!overlaps(other))
    {
        return std::nullopt;
    }

    int compare_low_bound = compareLowBound(other);
    int compare_high_bound = compareHighBound(other);
    return Range{
        type,
        compare_low_bound >= 0 ? low_inclusive : other.isLowInclusive(),
        compare_low_bound >= 0 ? low_value : other.getLowValue(),
        compare_high_bound <= 0 ? high_inclusive : other.isHighInclusive(),
        compare_high_bound <= 0 ? high_value : other.getHighValue()};
}

std::optional<Range> Range::merge(const Range & next) const
{
    if (compareLowBound(next) > 0)
        throw Exception("next before this", ErrorCodes::LOGICAL_ERROR);

    if (isHighUnbounded())
        return *this;

    bool merge;
    if (next.isLowUnbounded())
    {
        // both are low-unbounded
        merge = true;
    }
    else
    {
        merge = high_value > next.getLowValue() // overlap
            || (high_value == next.getLowValue() && (high_inclusive || next.isLowInclusive())); // adjacent
    }

    if (merge)
    {
        int compare_high_bound = compareHighBound(next);
        return Range(
            type,
            low_inclusive,
            low_value,
            compare_high_bound <= 0 ? next.isHighInclusive() : high_inclusive,
            compare_high_bound <= 0 ? next.getHighValue() : high_value);
    }
    return std::nullopt;
}

const Field & Range::getSingleValue() const
{
    if (!isSingleValue())
        throw Exception("Range is not a single value", DB::ErrorCodes::LOGICAL_ERROR);
    return low_value;
}

bool SortedRangeSet::isAll() const
{
    if (ranges.size() != 1)
        return false;
    return getRange(0).isLowUnbounded() && getRange(0).isHighUnbounded();
}

bool SortedRangeSet::isSingleValue() const
{
    return ranges.size() == 1 && getRange(0).isSingleValue();
}

const Field & SortedRangeSet::getSingleValue() const
{
    if (!isSingleValue())
        throw Exception("SortedRangeSet does not have just a single value", DB::ErrorCodes::LOGICAL_ERROR);

    return getRange(0).getSingleValue();
}

bool SortedRangeSet::isDiscreteSet() const
{
    if (ranges.empty())
        return false;

    return std::all_of(ranges.begin(), ranges.end(), [](auto & range) { return range.isSingleValue(); });
}

Array SortedRangeSet::getDiscreteSet() const
{
    Array res;

    for (auto & range : ranges)
        res.emplace_back(range.getSingleValue());

    return res;
}
//TODO: should we convertFieldToType for value?
bool SortedRangeSet::containsValue(const Field & value) const
{
    if (value.isNull())
        throw Exception("value is null", DB::ErrorCodes::LOGICAL_ERROR);

    if (DB::Utils::isFloatingPointNaN(type, value))
        return isAll();

    if (isNone())
        return false;

    if (isAll())
        return true;

    Range value_range = Range::equalRange(type, value);

    size_t low_range_index = 0;
    size_t high_range_index = ranges.size();
    size_t mid_range_index;

    while (low_range_index + 1 < high_range_index)
    {
        mid_range_index = (low_range_index + high_range_index) >> 1;
        if (getRange(mid_range_index).compareLowBound(value_range) <= 0)
            low_range_index = mid_range_index;
        else
            high_range_index = mid_range_index;
    }

    return getRange(low_range_index).overlaps(value_range);
}

SortedRangeSet SortedRangeSet::intersect(const SortedRangeSet & other) const
{
    if (isNone() || other.isNone())
        return createNone(type);

    size_t this_range_count = getRangesCount();
    size_t other_range_count = other.getRangesCount();
    size_t this_index = 0, other_index = 0;
    Ranges res;

    while (this_index < this_range_count && other_index < other_range_count)
    {
        std::optional<Range> intersect = getRange(this_index).intersect(other.getRange(other_index));

        if (intersect)
            res.emplace_back(*intersect);

        int compare = getRange(this_index).compareHighBound(other.getRange(other_index));
        if (compare == 0)
        {
            this_index++;
            other_index++;
        }
        else if (compare < 0)
        {
            this_index++;
        }
        else
        {
            other_index++;
        }
    }
    return SortedRangeSet(type, res);
}

bool SortedRangeSet::overlaps(const SortedRangeSet & other) const
{
    if (isNone() || other.isNone())
        return false;

    size_t this_range_count = ranges.size();
    size_t other_range_count = other.getRangesCount();
    size_t this_index = 0, other_index = 0;

    while (this_index < this_range_count && other_index < other_range_count)
    {
        if (getRange(this_index).overlaps(other.getRange(other_index)))
            return true;

        if (getRange(this_index).compareTo(other.getRange(other_index)) < 0)
            this_index++;
        else
            other_index++;
    }
    return false;
}

SortedRangeSet SortedRangeSet::unionn(const SortedRangeSet & other) const
{
    if (isAll() || other.isAll())
        return createAll(type);

    size_t this_range_count = ranges.size();
    size_t other_range_count = other.getRangesCount();
    size_t this_index = 0, other_index = 0;
    Ranges res;
    std::optional<Range> pre = std::nullopt;

    while (this_index < this_range_count || other_index < other_range_count)
    {
        Range cur = [&]() {
            if (this_index == this_range_count)
                return other.getRange(other_index++);
            else if (other_index == other_range_count)
                return getRange(this_index++);
            else
            {
                // both are not exhausted yet
                if (getRange(this_index).compareTo(other.getRange(other_index)) <= 0)
                    return getRange(this_index++);
                else
                    return other.getRange(other_index++);
            }
        }();

        if (!pre)
            pre = cur;
        else
        {
            std::optional<Range> merged = pre->merge(cur);
            if (merged)
            {
                pre = *merged;
            }
            else
            {
                res.emplace_back(*pre);
                pre = cur;
            }
        }
    }

    if (pre)
        res.emplace_back(*pre);

    return SortedRangeSet(type, res);
}

SortedRangeSet SortedRangeSet::subtract(const SortedRangeSet & other) const
{
    return intersect(other.complement());
}

bool SortedRangeSet::contains(const SortedRangeSet & other) const
{
    if (isAll())
        return true;

    if (other.isAll())
        return false;

    //none contains none?->return true;
    if (other.isNone())
        return true;

    if (isNone())
        return false;

    size_t this_range_count = ranges.size();
    size_t other_range_count = other.getRangesCount();
    size_t this_index = 0;

    for (size_t other_index = 0; other_index < other_range_count; other_index++)
    {
        while (this_index < this_range_count && getRange(this_index).isFullyBefore(other.getRange(other_index)))
            this_index++;

        if (this_index == this_range_count || !getRange(this_index).contains(other.getRange(other_index)))
        {
            // thisRange partially overlaps with thatRange, or it's fully after thatRange
            return false;
        }
    }

    return true;
}

SortedRangeSet SortedRangeSet::complement() const
{
    if (isNone())
        return createAll(type);

    if (isAll())
        return createNone(type);

    Ranges result;

    const auto & first = ranges.front();
    if (!first.isLowUnbounded())
        result.emplace_back(type, false, Null(), !first.isLowInclusive(), first.getLowValue());

    for (size_t cur_index = 1; cur_index < ranges.size(); cur_index++)
    {
        size_t pre_index = cur_index - 1;
        result.emplace_back(
            type,
            !getRange(pre_index).isHighInclusive(),
            getRange(pre_index).getHighValue(),
            !getRange(cur_index).isLowInclusive(),
            getRange(cur_index).getLowValue());
    }

    const auto & last = ranges.back();
    if (!last.isHighUnbounded())
        result.emplace_back(type, !last.isHighInclusive(), last.getHighValue(), false, Null());

    return SortedRangeSet(type, result);
}

Range SortedRangeSet::getSpan() const
{
    if (isNone())
        throw Exception("Cannot get span if no ranges exist", DB::ErrorCodes::LOGICAL_ERROR);

    return {
        type, ranges.front().isLowInclusive(), ranges.front().getLowValue(), ranges.back().isHighInclusive(), ranges.back().getHighValue()};
}
//TODO:delete type
SortedRangeSet SortedRangeSet::createFromUnsortedRanges(Ranges ranges)
{
    if (ranges.empty())
        throw Exception("ranges can not be empty, please use the methode 'createNone'", DB::ErrorCodes::LOGICAL_ERROR);
    // 1. sort range
    std::sort(ranges.begin(), ranges.end());

    // 2. merge ranges which overlap or are adjacent
    Ranges normalized;
    std::optional<Range> pre = std::nullopt;

    for (const auto & current : ranges)
    {
        if (!pre)
        {
            pre = current;
            continue;
        }
        std::optional<Range> merged_range = pre->merge(current);
        if (merged_range)
        {
            pre = *merged_range;
        }
        else
        {
            normalized.emplace_back(*pre);
            pre = current;
        }
    }

    // TODO: local variable may point to memory which is out of scope
    if (pre)
        normalized.emplace_back(*pre);

    return SortedRangeSet{ranges[0].getType(), std::move(normalized)};
}

SortedRangeSet SortedRangeSet::createFromUnsortedValues(const DataTypePtr & type, Array values)
{
    std::sort(values.begin(), values.end());
    Ranges ranges;

    for (size_t i = 0; i < values.size(); i++)
    {
        assert(!values[i].isNull());
        if (i && values[i] == values[i - 1])
            continue;
        ranges.emplace_back(Range::equalRange(type, values[i]));
    }

    return SortedRangeSet{type, std::move(ranges)};
}

template <typename F>
auto visitOnSameType(const F & visitor, const ValueSet & value_set_1, const ValueSet & value_set_2)
    -> decltype(visitor(value_set_1, value_set_1))
{
    using RetType = decltype(visitor(value_set_1, value_set_2));

    return std::visit(
        [&](const auto & a, const auto & b) -> RetType {
            using TA = std::decay_t<decltype(a)>;
            using TB = std::decay_t<decltype(b)>;

            if constexpr (std::is_same_v<TA, TB>)
                return visitor(a, b);
            else
                throw Exception("Incompatible value set types", ErrorCodes::LOGICAL_ERROR);
        },
        value_set_1,
        value_set_2);
}

ValueSet createNone(const DataTypePtr & type)
{
    if (isTypeOrderable(type))
        return SortedRangeSet::createNone(type);

    if (isTypeComparable(type))
        return DiscreteValueSet::createNone(type);

    return AllOrNoneValueSet::createNone(type);
}

ValueSet createAll(const DataTypePtr & type)
{
    if (isTypeOrderable(type))
        return SortedRangeSet::createAll(type);

    if (isTypeComparable(type))
        return DiscreteValueSet::createAll(type);

    return AllOrNoneValueSet::createAll(type);
}

ValueSet createValueSet(const DataTypePtr & type, const Array & values)
{
    if (isTypeOrderable(type))
        return SortedRangeSet::createFromUnsortedValues(type, values);

    if (isTypeComparable(type))
        return DiscreteValueSet::createFromValues(type, values);

    throw Exception("Cannot create discrete ValueSet with non-comparable type", DB::ErrorCodes::LOGICAL_ERROR);
}

ValueSet createValueSet(const Ranges & ranges)
{
    return SortedRangeSet::createFromUnsortedRanges(ranges);
}

ValueSet createSingleValueSet(const DataTypePtr & type, const Field & value)
{
    Array array;
    array.emplace_back(value);
    return createValueSet(type, array);
}

bool isTypeOrderable(const DataTypePtr & type)
{
    auto t = removeNullable(type);
    return isNumber(t) || isDecimal(t) || isString(t) || isDateOrDateTime(t);
}

bool isTypeComparable(const DataTypePtr &)
{
    return false;
}

ValueSet complementValueSet(const ValueSet & value_set)
{
    auto caller = [](const auto & v) -> ValueSet { return v.complement(); };
    return std::visit(caller, value_set);
}

ValueSet subtractValueSet(const ValueSet & value_set1, const ValueSet & value_set2)
{
    return visitOnSameType([](auto & v1, auto & v2) -> ValueSet { return v1.subtract(v2); }, value_set1, value_set2);
}

ValueSet intersectValueSet(const ValueSet & value_set1, const ValueSet & value_set2)
{
    return visitOnSameType([](auto & v1, auto & v2) -> ValueSet { return v1.intersect(v2); }, value_set1, value_set2);
}

ValueSet unionValueSet(const ValueSet & value_set1, const ValueSet & value_set2)
{
    return visitOnSameType([](auto & v1, auto & v2) -> ValueSet { return v1.unionn(v2); }, value_set1, value_set2);
}

bool isAllValueSet(const ValueSet & value_set)
{
    auto caller = [](const auto & v) -> bool { return v.isAll(); };
    return std::visit(caller, value_set);
}

}
