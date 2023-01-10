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

#include <DataTypes/DataTypeNullable.h>
#include <DataTypes/IDataType.h>
#include <Parsers/ASTLiteral.h>
#include <Statistics/Histogram.h>

#include <Poco/JSON/Object.h>

namespace DB
{
class SymbolStatistics;
using SymbolStatisticsPtr = std::shared_ptr<SymbolStatistics>;

// export symbols
using Statistics::OverlappedRange;
using Statistics::OverlappedRanges;
using Statistics::Bucket;
using Statistics::Buckets;
using Statistics::BucketPtr;
using Statistics::Histogram;

namespace Statistics
{
    class StatisticsCollector;
    template <typename T>
    class StatsNdvBucketsImpl;
}

/**
 * Statistics for a single column or symbol.
 */
class SymbolStatistics
{
public:
    static SymbolStatisticsPtr UNKNOWN;

    friend class Statistics::StatisticsCollector;
    template <typename T>
    friend class Statistics::StatsNdvBucketsImpl;

    explicit SymbolStatistics(
        UInt64 ndv = 0,
        Float64 min = 0,
        Float64 max = 0,
        UInt64 null_counts = 0,
        UInt64 avg_len = 8,
        Histogram histogram = {},
        DataTypePtr type = {},
        String db_table_column = "unknown",
        bool unknown = true);

    SymbolStatisticsPtr copy() const
    {
        return std::make_shared<SymbolStatistics>(ndv, min, max, null_counts, avg_len, histogram, type, db_table_column, unknown);
    }

    SymbolStatistics & operator+(const SymbolStatistics & other)
    {
        ndv += other.ndv;
        min = min < other.min ? min : other.min;
        max = max > other.max ? max : other.max;
        null_counts += other.null_counts;
        histogram = histogram.createUnion(other.histogram);
        return *this;
    }

    UInt64 getNdv() const { return ndv; }
    Float64 getMin() const { return min; }
    Float64 getMax() const { return max; }
    UInt64 getNullsCount() const { return null_counts; }
    UInt64 getAvg() const { return avg_len; }
    const Histogram & getHistogram() const { return histogram; }
    Histogram & getHistogram() { return histogram; }
    DataTypePtr getType() const { return type; }
    String getDbTableColumn() { return db_table_column; }
    bool isUnknown() const { return unknown; }

    // TODO@lichengxian use data size for shuffle
    size_t getOutputSizeInBytes();

    void setNdv(UInt64 ndv_) { ndv = ndv_; }
    void setType(const DataTypePtr & type_) { type = type_; }
    void setDbTableColumn(String db_table_column_) { db_table_column = db_table_column_; }

    bool isNullable() const { return type->isNullable(); }
    bool isNumber() const;
    bool isString() const;

    bool isImplicitConvertableFromString();

    double toDouble(const Field & literal);
    String toString(const Field & literal);
    bool contains(double value);

    // estimate selectivity
    double estimateEqualFilter(double value);
    double estimateNotEqualFilter(double value);
    double
    estimateLessThanOrLessThanEqualFilter(double upper_bound, bool upper_bound_inclusive, double lower_bound, bool lower_bound_inclusive);
    double estimateGreaterThanOrGreaterThanEqualFilter(
        double upper_bound, bool upper_bound_inclusive, double lower_bound, bool lower_bound_inclusive);
    double estimateInFilter(std::set<double> & values, bool has_null_value, UInt64 count);
    double estimateInFilter(std::set<String> & values, bool has_null_value, UInt64 count) const;
    double estimateNotInFilter(std::set<double> & values, bool has_null_value, UInt64 count);
    double estimateNotInFilter(std::set<String> & values, bool has_null_value, UInt64 count);
    double estimateNullFilter(UInt64 count) const;
    double estimateNotNullFilter(UInt64 count) const;

    // create new SymbolStatistics after symbol apply filter, union, group by, join ...
    SymbolStatisticsPtr createEmpty();
    SymbolStatisticsPtr createEqualFilter(double value);
    SymbolStatisticsPtr createNotEqualFilter(double value);
    SymbolStatisticsPtr createLessThanOrLessThanEqualFilter(double selectivity, double min_value, double max_value, bool equal);
    SymbolStatisticsPtr createGreaterThanOrGreaterThanEqualFilter(double selectivity, double min_value, double max_value, bool equal);
    SymbolStatisticsPtr createInFilter(std::set<double> & values, bool has_null_value);
    SymbolStatisticsPtr createInFilter(std::set<String> & literals, bool has_null_value);
    SymbolStatisticsPtr createNotInFilter(std::set<double> & values, bool has_null_value);
    SymbolStatisticsPtr createNotInFilter(std::set<String> & literals, bool has_null_value);
    SymbolStatisticsPtr createNullFilter();
    SymbolStatisticsPtr createNotNullFilter();
    SymbolStatisticsPtr createJoin(Buckets & buckets);
    SymbolStatisticsPtr createUnion(SymbolStatisticsPtr & other);
    SymbolStatisticsPtr createNot(SymbolStatisticsPtr & origin);

    // adjust other symbols statistics while they don't participation in filter/join/aggregate.
    SymbolStatisticsPtr applySelectivity(double selectivity);
    SymbolStatisticsPtr applySelectivity(double rowcount_selectivity, double ndv_selectivity);

    void emplaceBackBucket(BucketPtr bucket) { histogram.emplaceBackBucket(std::move(bucket)); }

    Poco::JSON::Object::Ptr toJson() const;

private:
    // number of distinct values
    UInt64 ndv;

    // minimum value
    double min;

    // maximum value
    double max;

    // number of nulls
    UInt64 null_counts;

    // TODO @guoguiling collect avg_len later.
    // average length of the values. For fixed-length types, this should be a constant.
    UInt64 avg_len;

    // data distribution
    Histogram histogram;

    // data type
    DataTypePtr type;

    // column description, combine database, table, column name.
    String db_table_column;

    // is the statistics available for estimation.
    bool unknown;
};

}
