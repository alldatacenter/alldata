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

#include <Optimizer/CardinalityEstimate/SymbolStatistics.h>

#include <Statistics/TypeUtils.h>
#include <Common/FieldVisitorConvertToNumber.h>

namespace DB
{
SymbolStatisticsPtr SymbolStatistics::UNKNOWN = std::make_shared<SymbolStatistics>();

SymbolStatistics::SymbolStatistics(
    UInt64 ndv_,
    Float64 min_,
    Float64 max_,
    UInt64 null_counts_,
    UInt64 avg_len_,
    Histogram histogram_,
    DataTypePtr type_,
    String db_table_column_,
    bool unknown_)
    : ndv(ndv_)
    , min(min_)
    , max(max_)
    , null_counts(null_counts_)
    , avg_len(avg_len_)
    , histogram(std::move(histogram_))
    , type(std::move(type_))
    , db_table_column(std::move(db_table_column_))
    , unknown(unknown_)
{
}

size_t SymbolStatistics::getOutputSizeInBytes()
{
    if (type->haveMaximumSizeOfValue())
    {
        return type->getMaximumSizeOfValueInMemory();
    }
    // set default size to 8
    return avg_len;
}

bool SymbolStatistics::isNumber() const
{
    auto tmp_type = Statistics::decayDataType(type);
    return tmp_type->isValueRepresentedByNumber();
}

bool SymbolStatistics::isString() const
{
    auto tmp_type = Statistics::decayDataType(type);
    return tmp_type->getTypeId() == TypeIndex::String || tmp_type->getTypeId() == TypeIndex::FixedString;
}

bool SymbolStatistics::isImplicitConvertableFromString()
{
    auto tmp_type = Statistics::decayDataType(type);
    // currently support date, date32, datetime32/64
    return isDateOrDateTime(tmp_type) || isTime(tmp_type);
}

double SymbolStatistics::toDouble(const Field & literal)
{
    return applyVisitor(FieldVisitorConvertToNumber<Float64>(), literal);
}

String SymbolStatistics::toString(const Field & literal)
{
    return literal.safeGet<String>();
}

bool SymbolStatistics::contains(double value)
{
    return value >= min && value <= max;
}

double SymbolStatistics::estimateEqualFilter(double value)
{
    double selectivity = 1.0;
    if (histogram.getBuckets().empty())
    {
        if (ndv > 0)
        {
            selectivity = (1.0 / ndv);
        }
    }
    else
    {
        selectivity = histogram.estimateEqual(value);
    }
    return selectivity;
}

double SymbolStatistics::estimateNotEqualFilter(double value)
{
    double selectivity = estimateEqualFilter(value);
    return 1.0 - selectivity;
}

double SymbolStatistics::estimateLessThanOrLessThanEqualFilter(double upper_bound, bool upper_bound_inclusive, double lower_bound, bool)
{
    double selectivity;
    if (histogram.getBuckets().empty())
    {
        selectivity = (upper_bound - lower_bound) / (max - min);
    }
    else
    {
        selectivity = histogram.estimateLessThanOrLessThanEqualFilter(upper_bound, upper_bound_inclusive);
    }
    return selectivity > 1 ? 1 : selectivity;
}

double
SymbolStatistics::estimateGreaterThanOrGreaterThanEqualFilter(double upper_bound, bool, double lower_bound, bool lower_bound_inclusive)
{
    double selectivity;
    if (histogram.getBuckets().empty())
    {
        selectivity = (max - min <= 0) ? 1 : (upper_bound - lower_bound) / (max - min);
    }
    else
    {
        selectivity = histogram.estimateGreaterThanOrGreaterThanEqualFilter(lower_bound, lower_bound_inclusive);
    }
    return selectivity > 1 ? 1 : selectivity;
}

double SymbolStatistics::estimateInFilter(std::set<double> & values, bool has_null_value, UInt64 count)
{
    double in_values_selectivity = 0.0;
    for (const auto & value : values)
    {
        if (contains(value))
        {
            in_values_selectivity += estimateEqualFilter(value);
        }
    }
    if (has_null_value)
    {
        in_values_selectivity += static_cast<double>(getNullsCount()) / count;
    }
    return in_values_selectivity;
}

double SymbolStatistics::estimateInFilter(std::set<String> & values, bool has_null_value, UInt64 count) const
{
    double in_values_selectivity = getNdv() == 0 ? 1.0 : static_cast<double>(values.size()) / getNdv();
    if (has_null_value)
    {
        in_values_selectivity += static_cast<double>(getNullsCount()) / count;
    }
    return in_values_selectivity;
}

double SymbolStatistics::estimateNotInFilter(std::set<double> & values, bool has_null_value, UInt64 count)
{
    double in_values_selectivity = estimateInFilter(values, has_null_value, count);
    return 1.0 - in_values_selectivity;
}

double SymbolStatistics::estimateNotInFilter(std::set<String> & values, bool has_null_value, UInt64 count)
{
    double in_values_selectivity = estimateInFilter(values, has_null_value, count);
    return 1.0 - in_values_selectivity;
}

double SymbolStatistics::estimateNullFilter(UInt64 count) const
{
    if (count == 0)
    {
        return 1.0;
    }
    UInt64 nulls_count = getNullsCount();
    return static_cast<double>(nulls_count) / count;
}

double SymbolStatistics::estimateNotNullFilter(UInt64 count) const
{
    if (count == 0)
    {
        return 1.0;
    }
    return 1.0 - static_cast<double>(getNullsCount()) / count;
}

SymbolStatisticsPtr SymbolStatistics::createEmpty()
{
    return std::make_shared<SymbolStatistics>(0, 0, 0, 0, avg_len, Histogram{}, type, db_table_column, unknown);
}

SymbolStatisticsPtr SymbolStatistics::createEqualFilter(double value)
{
    return std::make_shared<SymbolStatistics>(
        1, value, value, 0, avg_len, histogram.createEqualFilter(value), type, db_table_column, unknown);
}

SymbolStatisticsPtr SymbolStatistics::createNotEqualFilter(double value)
{
    return std::make_shared<SymbolStatistics>(
        ndv - 1, min, max, null_counts, avg_len, histogram.createNotEqualFilter(value), type, db_table_column, unknown);
}

SymbolStatisticsPtr
SymbolStatistics::createLessThanOrLessThanEqualFilter(double selectivity, double min_value, double max_value, bool equal)
{
    return std::make_shared<SymbolStatistics>(
        static_cast<UInt64>(getNdv() * selectivity),
        min_value,
        max_value,
        static_cast<UInt64>(getNullsCount() * selectivity),
        avg_len,
        histogram.createLessThanOrLessThanEqualFilter(max_value, equal),
        type,
        db_table_column,
        unknown);
}

SymbolStatisticsPtr
SymbolStatistics::createGreaterThanOrGreaterThanEqualFilter(double selectivity, double min_value, double max_value, bool equal)
{
    return std::make_shared<SymbolStatistics>(
        static_cast<UInt64>(getNdv() * selectivity),
        min_value,
        max_value,
        static_cast<UInt64>(getNullsCount() * selectivity),
        avg_len,
        histogram.createGreaterThanOrGreaterThanEqualFilter(min_value, equal),
        type,
        db_table_column,
        unknown);
}

SymbolStatisticsPtr SymbolStatistics::createInFilter(std::set<double> & values, bool has_null_value)
{
    double min_ = getMin();
    double max_ = getMax();
    for (auto value : values)
    {
        if (value < min_)
        {
            min_ = value;
        }
        if (value > max_)
        {
            max_ = value;
        }
    }

    return std::make_shared<SymbolStatistics>(
        values.size(), min_, max_, has_null_value ? 1 : 0, avg_len, histogram.createInFilter(values), type, db_table_column, unknown);
}

SymbolStatisticsPtr SymbolStatistics::createInFilter(std::set<String> & literals, bool has_null_value)
{
    return std::make_shared<SymbolStatistics>(
        literals.size(), getMin(), getMax(), has_null_value ? 1 : 0, avg_len, Histogram{}, type, db_table_column, unknown);
}

SymbolStatisticsPtr SymbolStatistics::createNotInFilter(std::set<double> & values, bool has_null_value)
{
    return std::make_shared<SymbolStatistics>(
        getNdv() - values.size(),
        getMin(),
        getMax(),
        has_null_value ? 0 : getNullsCount(),
        avg_len,
        histogram.createNotInFilter(values),
        type,
        db_table_column,
        unknown);
}

SymbolStatisticsPtr SymbolStatistics::createNotInFilter(std::set<String> & literals, bool has_null_value)
{
    return std::make_shared<SymbolStatistics>(
        getNdv() - literals.size(),
        getMin(),
        getMax(),
        has_null_value ? 0 : getNullsCount(),
        getAvg(),
        Histogram{},
        type,
        db_table_column,
        unknown);
}

SymbolStatisticsPtr SymbolStatistics::createNullFilter()
{
    return std::make_shared<SymbolStatistics>(1, 0, 0, 1, getAvg(), Histogram{}, type, db_table_column, unknown);
}

SymbolStatisticsPtr SymbolStatistics::createNotNullFilter()
{
    return std::make_shared<SymbolStatistics>(getNdv(), getMin(), getMax(), 0, getAvg(), histogram.copy(), type, db_table_column, unknown);
}

SymbolStatisticsPtr SymbolStatistics::createJoin(Buckets & buckets)
{
    Histogram join_histogram{buckets};
    return std::make_shared<SymbolStatistics>(getNdv(), getMin(), getMax(), 0, getAvg(), join_histogram, type, db_table_column, unknown);
}

SymbolStatisticsPtr SymbolStatistics::createUnion(SymbolStatisticsPtr & other)
{
    return std::make_shared<SymbolStatistics>(
        ndv += other->ndv,
        min = min < other->min ? min : other->min,
        max = max > other->max ? max : other->max,
        null_counts += other->null_counts,
        avg_len,
        histogram = histogram.createUnion(other->histogram),
        type,
        db_table_column,
        unknown);
}

SymbolStatisticsPtr SymbolStatistics::createNot(SymbolStatisticsPtr & origin)
{
    Histogram not_histogram = histogram.createNot(origin->histogram);
    return std::make_shared<SymbolStatistics>(
        not_histogram.getTotalNdv(),
        not_histogram.getMin(),
        not_histogram.getMax(),
        null_counts, // TODO nulls count intersect
        avg_len,
        not_histogram,
        type,
        db_table_column,
        unknown);
}

// TODO Poisson distribution
SymbolStatisticsPtr SymbolStatistics::applySelectivity(double selectivity)
{
    return applySelectivity(selectivity, selectivity);
}

SymbolStatisticsPtr SymbolStatistics::applySelectivity(double rowcount_selectivity, double ndv_selectivity)
{
    UInt64 new_ndv = static_cast<UInt64>(getNdv() * ndv_selectivity);
    return std::make_shared<SymbolStatistics>(
        new_ndv == 0 ? 1 : new_ndv,
        getMin(),
        getMax(),
        0,
        avg_len,
        histogram.applySelectivity(rowcount_selectivity, ndv_selectivity),
        type,
        db_table_column,
        unknown);
}

Poco::JSON::Object::Ptr SymbolStatistics::toJson() const
{
    Poco::JSON::Object::Ptr json = new Poco::JSON::Object;
    if (!unknown)
    {
        json->set("ndv", ndv);
        json->set("min", min);
        json->set("max", max);
        json->set("null_counts", null_counts);
        json->set("avg_len", avg_len);
        json->set("histogram", !histogram.empty());
        json->set("db_table_column", db_table_column);
    }
    return json;
}
}
