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

#include <Core/Field.h>
#include <Common/Exception.h>
#include <DataTypes/IDataType.h>
#include <Optimizer/Utils.h>
#include <unordered_set>
#include <utility>
#include <variant>
#include <DataTypes/DataTypeNullable.h>
#include <Common/FieldVisitorHash.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int UNSUPPORTED_METHOD;
}

/**NOTE!!!
 * Before using the fields to represent values in a valueSet, you should first convert all fields to a uniform type !!!!!!!!!!!!
 */

namespace Predicate
{
    class AllOrNoneValueSet;
    class DiscreteValueSet;
    class SortedRangeSet;

    using ValueSet = std::variant<AllOrNoneValueSet, DiscreteValueSet, SortedRangeSet>;

    class AllOrNoneValueSet
    {
    public:
        AllOrNoneValueSet(DataTypePtr type_, bool all_) : type(std::move(type_)), all(all_)
        {}

        // ValueSet properties
        DataTypePtr getType() const { return type; }
        bool isNone() const { return !all; }
        bool isAll() const { return all; }
        bool isSingleValue() const { return false; } // NOLINT(readability-convert-member-functions-to-static)
        bool isDiscreteSet() const { return false; } // NOLINT(readability-convert-member-functions-to-static)

        // ValueSet operation
        const Field & getSingleValue() const { throw Exception("Unsupported method", DB::ErrorCodes::UNSUPPORTED_METHOD); } // NOLINT(readability-convert-member-functions-to-static)
        Array getDiscreteSet() const { throw Exception ("Unsupported method", DB::ErrorCodes::UNSUPPORTED_METHOD); } // NOLINT(readability-convert-member-functions-to-static)
        bool containsValue(const Field & value) const
        {
            if (value.isNull())
                throw Exception("value is null", DB::ErrorCodes::LOGICAL_ERROR);
            return all;
        }
        AllOrNoneValueSet intersect(const AllOrNoneValueSet & other) const
        {
            return {type, all && other.isAll()};
        }
        AllOrNoneValueSet unionn(const AllOrNoneValueSet & other) const
        {
            return {type, all || other.isAll()};
        }
        AllOrNoneValueSet subtract(const AllOrNoneValueSet & other) const
        {
            return intersect(other.complement());
        }
        AllOrNoneValueSet complement() const
        {
            return {type, !all};
        }
        bool contains(const AllOrNoneValueSet & other) const { return unionn(other) == *this; }
        bool overlaps(const AllOrNoneValueSet & other) const { return all && other.isAll(); }
        bool operator==(const AllOrNoneValueSet & other) const { return all == other.isAll(); }

        // ValueSet factory method
        static AllOrNoneValueSet createAll(const DataTypePtr & type) { return {type, true}; }
        static AllOrNoneValueSet createNone(const DataTypePtr & type) { return {type, false}; }

        // AllOrNoneValueSet properties

        // AllOrNoneValueSet operation
        String toString() { return all ? "[ALL]" : "[NONE]"; } // NOLINT(readability-make-member-function-const)

    private:
        DataTypePtr type;
        bool all;
    };

    struct FieldHashing {
    public:
        size_t operator()(const Field & f) const;
    };

    // TODO: test correctness for std::unordered_set<Field, FieldHash>
    using FieldSet = std::unordered_set<Field, FieldHashing>;

    class DiscreteValueSet
    {
    public:

        DiscreteValueSet() = default;
        DiscreteValueSet(DataTypePtr type_, bool inclusive_, FieldSet values_):
            type(std::move(type_)), inclusive(inclusive_), values(std::move(values_))
        {}

        // ValueSet properties
        DataTypePtr getType() const { return type; }
        bool isNone() const { return inclusive && values.empty(); }
        bool isAll() const { return !inclusive && values.empty(); }
        bool isSingleValue() const { return inclusive && values.size() == 1; }
        bool isDiscreteSet() const { return inclusive && !values.empty(); }

        // ValueSet operation
        const Field & getSingleValue() const;
        Array getDiscreteSet() const { return Array(values.begin(), values.end()); }
        bool containsValue(const Field & value) const { return inclusive == values.count(value); }
        DiscreteValueSet intersect(const DiscreteValueSet & other) const;
        DiscreteValueSet unionn(const DiscreteValueSet & other) const;
        DiscreteValueSet subtract(const DiscreteValueSet & other) const;
        DiscreteValueSet complement() const;
        bool contains(const DiscreteValueSet & other) const;
        bool overlaps(const DiscreteValueSet & other) const;
        bool operator==(const DiscreteValueSet & other) const
        {
            return inclusive == other.inclusive && values == other.getValues();
        }

        // ValueSet factory method
        static DiscreteValueSet createAll(const DataTypePtr & type) { return {type, false, {}}; }
        static DiscreteValueSet createNone(const DataTypePtr & type) { return {type, true, {}}; }

        // DiscreteValueSet properties
        bool isInclusive() const { return inclusive; }
        const FieldSet & getValues() const { return values; }
        size_t getValuesCount() const { return values.size(); }

        // DiscreteValueSet operation

        // DiscreteValueSet factory method
        static DiscreteValueSet createFromValues(const DataTypePtr & type, const Array & values);
    private:
        DataTypePtr type;
        bool inclusive;
        FieldSet values;
    };

    class Range
    {
    public:
        Range(const DataTypePtr & type_, bool low_inclusive, Field low_value, bool high_inclusive, Field high_value);

        static Range allRange(const DataTypePtr & type_) { return {type_, false, Null(), false, Null()}; }
        static Range greaterThanRange(const DataTypePtr & type_, const Field & low_value_) { return {type_, false, low_value_, false, Null()}; }
        static Range greaterThanOrEqualRange(const DataTypePtr & type_, const Field & low_value_) { return {type_, true, low_value_, false, Null()}; }
        static Range lessThanRange(const DataTypePtr & type_, const Field & high_value_) { return {type_, false, Null(), false, high_value_}; }
        static Range lessThanOrEqualRange(const DataTypePtr & type_, const Field & high_value_) { return {type_, false, Null(), true, high_value_}; }
        static Range equalRange (const DataTypePtr & type_, const Field & value_) { return {type_, true, value_, true, value_}; }

        bool isLowInclusive() const { return low_inclusive; }
        bool isLowUnbounded() const { return low_value.isNull(); }
        bool isHighInclusive() const { return high_inclusive; }
        bool isHighUnbounded() const { return high_value.isNull(); }
        const Field & getLowValue() const { return low_value; }
        const Field & getHighValue() const { return high_value; }
        DataTypePtr getType() const { return type; }
        bool isAll() const { return low_value.isNull() && high_value.isNull(); }
        bool isSingleValue() const { return is_single_value; }
        const Field & getSingleValue() const;
        bool operator==(const Range & other) const;
        int compareLowBound(const Range & other) const;
        int compareHighBound(const Range & other) const;
        int compareTo(const Range & other) const;
        bool operator<(const Range & other) const { return compareTo(other) < 0; }
        bool contains(const Range & other) const;
        bool isFullyBefore(const Range & other) const;
        bool overlaps(const Range & other) const;
        Range span(const Range & other) const;
        std::optional<Range> intersect(const Range & other) const;
        /** Returns unioned range if {@code this} and {@code next} overlap or are adjacent.
     *  The {@code next} lower bound must not be before {@code this} lower bound.
     * */
        std::optional<Range> merge(const Range & next) const;

    private:
        bool low_inclusive;
        Field low_value;
        bool high_inclusive;
        Field high_value;
        bool is_single_value;
        DataTypePtr type;
    };

    using Ranges = std::vector<Range>;

    /**
 * A set containing zero or more Ranges of the same type over a continuous space of possible values.
 * Ranges are coalesced into the most compact representation of non-overlapping Ranges. This structure
 * allows iteration across these compacted Ranges in increasing order, as well as other common
 * set-related operation.
 */
    class SortedRangeSet
    {
    public:
        SortedRangeSet() = default;
        explicit SortedRangeSet(DataTypePtr type_, Ranges ranges_ = {}): type(std::move(type_)), ranges(std::move(ranges_))
        {}

        // ValueSet properties
        DataTypePtr getType() const { return type; }
        bool isNone() const { return ranges.empty(); }

        bool isAll() const;
        bool isSingleValue() const;
        bool isDiscreteSet() const;

        // ValueSet operation
        const Field & getSingleValue() const;
        Array getDiscreteSet() const;
        bool containsValue(const Field & value) const;
        SortedRangeSet intersect(const SortedRangeSet & other) const;
        SortedRangeSet unionn(const SortedRangeSet & other) const;
        SortedRangeSet subtract(const SortedRangeSet & other) const;
        SortedRangeSet complement() const;
        bool contains(const SortedRangeSet & other) const;
        bool overlaps(const SortedRangeSet & other) const;
        bool operator==(const SortedRangeSet & sorted_range_set) const
        {
            return ranges == sorted_range_set.getRanges();
        }

        // ValueSet factory method
        static SortedRangeSet createNone(const DataTypePtr & type) { return SortedRangeSet {type}; }
        static SortedRangeSet createAll(const DataTypePtr & type_) { return SortedRangeSet(type_, {Range::allRange(type_)}); }

        // SortedRangeSet properties
        const Ranges & getRanges() const { return ranges; }
        const Range & getRange(size_t index) const { return ranges.at(index); }
        size_t getRangesCount() const { return ranges.size(); }

        // SortedRangeSet operation
        Range getSpan() const;

        // SortedRangeSet factory method
        static SortedRangeSet createFromUnsortedRanges(Ranges ranges);
        static SortedRangeSet createFromUnsortedValues(const DataTypePtr & type, Array values);

    private:
        DataTypePtr type;
        Ranges ranges;
    };
    //TODO: static
    template<typename F> auto visitOnSameType(const F & visitor, const ValueSet & value_set_1, const ValueSet & value_set_2) -> decltype(visitor(value_set_1, value_set_1));
    ValueSet createNone(const DataTypePtr & type);
    ValueSet createAll(const DataTypePtr & type);
    ValueSet createValueSet(const DataTypePtr & type, const Array & values);
    ValueSet createValueSet(const Ranges & ranges);
    ValueSet createSingleValueSet(const DataTypePtr & type, const Field & value);
    ValueSet complementValueSet(const ValueSet & value_set);
    ValueSet subtractValueSet(const ValueSet & value_set1, const ValueSet & value_set2);
    ValueSet intersectValueSet(const ValueSet & value_set1, const ValueSet & value_set2);
    ValueSet unionValueSet(const ValueSet & value_set1, const ValueSet & value_set2);
    bool isAllValueSet(const ValueSet & value_set);
    bool isTypeOrderable(const DataTypePtr & type);
    bool isTypeComparable(const DataTypePtr & type);

}
}
