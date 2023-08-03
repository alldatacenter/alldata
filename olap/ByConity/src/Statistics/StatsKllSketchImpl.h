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
#include <DataTypes/IDataType.h>
#include <Statistics/SerdeUtils.h>
#include <Statistics/StatisticsBaseImpl.h>
#include <Statistics/StatsKllSketch.h>
#include <Statistics/VectorSerde.h>
#include <boost/lexical_cast.hpp>
#include <Common/Exception.h>

#include <cmath>
#include <string_view>
#include <city.h>
#include <Statistics/Base64.h>
#include <Statistics/BucketBoundsImpl.h>
#include <Statistics/DataSketchesHelper.h>
#include <Statistics/SerdeUtils.h>
#include <Statistics/StatsNdvBucketsResultImpl.h>
#include <Statistics/serde_extend.hpp>

namespace DB::Statistics
{
namespace impl
{
    /// @description trim array of data to at most 2 duplicated
    /// @example [1, 1, 2, 3, 4, 4, 4, 5, 6, 6, 6, 6] => [1, 1, 2, 3, 4, 4, 5, 6, 6]
    /// @param src sorted array of data
    /// @return trimmed array of data
    template <typename T>
    std::vector<T> trimBucketBounds(const std::vector<T> & src)
    {
        if (src.empty())
        {
            return {};
        }
        std::vector<T> result;
        auto last_count = 0;
        T last = src[0];
        for (auto & x : src)
        {
            if (x == last)
            {
                if (last_count < 2)
                {
                    result.push_back(x);
                }
                ++last_count;
            }
            else
            {
                last = x;
                last_count = 1;
                result.push_back(x);
            }
        }
        return result;
    }

    // algorithm to generate bucket bounds from kll_sketch
    template <typename T>
    std::vector<T> generateBoundsFromKll(const datasketches::kll_sketch<T> & kll)
    {
        // dump internal data from kll_sketch
        std::vector<T> internal_bounds;
        for (auto [k, v] : kll)
        {
            // internal data is of format <data, node_freq>
            // dump data only since we just need it
            internal_bounds.emplace_back(k);
        }
        std::sort(internal_bounds.begin(), internal_bounds.end());
        internal_bounds = trimBucketBounds(internal_bounds);

        // 350 ~= 248 + 50 * 2
        if (internal_bounds.size() < 350)
        {
            if (internal_bounds.size() < 125)
            {
                // construct top K like bounds
                // i.e. [1, 1, 3, 3, 5, 5, ...]
                auto end = std::unique(internal_bounds.begin(), internal_bounds.end());
                internal_bounds.resize(std::distance(internal_bounds.begin(), end));
                std::vector<T> result;
                for (auto & x : internal_bounds)
                {
                    result.push_back(x);
                    result.push_back(x);
                }
                return result;
            }
            // in case data points are not sufficient
            // use internal bounds directly
            return internal_bounds;
        }
        else
        {
            // normally
            // just use get_quantiles api
            auto quantiles = kll.get_quantiles(350);
            return trimBucketBounds(quantiles);
        }
    }
}
template <typename T>
class StatsKllSketchImpl : public StatsKllSketch
{
public:
    using Self = StatsKllSketchImpl;
    static constexpr bool is_string = std::is_same_v<T, String>;
    using EmbeddedType = std::conditional_t<is_string, UInt64, T>;
    using ViewType = std::conditional_t<is_string, std::string_view, T>;

    template <typename U = T>
    static inline EmbeddedType hash(std::string_view str)
    {
        // enable only when T is string
        // using sfinae
        static_assert(is_string);
        return CityHash_v1_0_2::CityHash64(str.data(), str.size());
    }

    StatsKllSketchImpl() = default;

    String serialize() const override;
    void deserialize(std::string_view blob) override;

    void update(ViewType value)
    {
        if constexpr (std::is_same_v<T, String>)
        {
            data.update(hash(value));
        }
        else
        {
            data.update(value);
        }
    }

    void merge(const StatsKllSketchImpl & rhs) { data.merge(rhs.data); }

    SerdeDataType getSerdeDataType() const override { return SerdeDataTypeFrom<T>; }

    bool isEmpty() const override { return data.is_empty(); }

    T get_min_value() { return data.get_min_value(); }
    T get_max_value() { return data.get_max_value(); }

    std::shared_ptr<BucketBounds> getBucketBounds() const override;

    int64_t getCount() const override { return data.get_n(); }

    // assuming ndv==count for each bucket
    // generate ndvBucketsResultImpl
    std::shared_ptr<StatsNdvBucketsResultImpl<T>> generateNdvBucketsResultImpl(double total_ndv) const;

    std::shared_ptr<StatsNdvBucketsResult> generateNdvBucketsResult(double total_ndv) const override
    {
        return generateNdvBucketsResultImpl(total_ndv);
    }

protected:
    // hide since these function won't be used in derived class
    std::optional<double> minAsDouble() const override
    {
        if (data.is_empty())
        {
            return std::nullopt;
        }
        else
        {
            return Statistics::toDouble(data.get_min_value());
        }
    }
    std::optional<double> maxAsDouble() const override
    {
        if (data.is_empty())
        {
            return std::nullopt;
        }
        else
        {
            return Statistics::toDouble(data.get_max_value());
        }
    }

private:
    datasketches::kll_sketch<EmbeddedType> data;
};

template <typename T>
inline std::shared_ptr<BucketBounds> StatsKllSketchImpl<T>::getBucketBounds() const
{
    auto bounds = std::make_shared<BucketBoundsImpl<T>>();
    auto vec = impl::generateBoundsFromKll(data);
    bounds->setBounds(std::move(vec));
    return bounds;
}

template <typename T>
String StatsKllSketchImpl<T>::serialize() const
{
    std::ostringstream ss;
    auto serde_data_type = SerdeDataTypeFrom<T>;
    ss.write(reinterpret_cast<const char *>(&serde_data_type), sizeof(serde_data_type));
    data.serialize(ss);
    return ss.str();
}

template <typename T>
void StatsKllSketchImpl<T>::deserialize(std::string_view raw_blob)
{
    auto [serde_data_type, blob] = parseBlobWithHeader(raw_blob);
    checkSerdeDataType<T>(serde_data_type);
    data = decltype(data)::deserialize(blob.data(), blob.size());
}

namespace impl
{
    template <typename T>
    inline T nextAfter(const T x)
    {
        if constexpr (std::is_floating_point_v<T>)
        {
            return std::nextafter(x, std::numeric_limits<T>::max());
        }
        else
        {
            if (x < std::numeric_limits<T>::max())
            {
                return x + 1;
            }
            else
            {
                return x;
            }
        }
    }
}

// return null when ndv < 2
template <typename T>
std::shared_ptr<StatsNdvBucketsResultImpl<T>> StatsKllSketchImpl<T>::generateNdvBucketsResultImpl(double total_ndv) const
{
    auto bounds = impl::generateBoundsFromKll(data);
    auto output_bounds = bounds;

    auto row_count = data.get_n();
    auto num_bucket = bounds.size() - 1;

    if (num_bucket <= 1)
    {
        return nullptr;
    }

    for (size_t i = 0; i < num_bucket; ++i)
    {
        if (bounds[i] == bounds[i + 1])
        {
            auto & x = bounds[i + 1];
            x = impl::nextAfter(x);
            // to avoid mistakenly change
            ++i;
        }
    }

    std::vector<EmbeddedType> splitters;
    std::unique_copy(bounds.begin(), bounds.end(), std::back_inserter(splitters));
    auto cdfs = data.get_CDF(splitters.data(), splitters.size());
    std::vector<uint64_t> counts;
    int splitter_id = 0;
    UInt64 last_sum = 0;
    for (UInt64 i = 0; i < num_bucket - 1; ++i)
    {
        auto x = bounds[i + 1];
        if (x != splitters[splitter_id])
        {
            ++splitter_id;
        }
        assert(splitters[splitter_id] == x);
        UInt64 ncdf = std::round(cdfs[splitter_id] * row_count);
        counts.push_back(ncdf - last_sum);
        last_sum = ncdf;
    }
    counts.push_back(row_count - last_sum);
    assert(num_bucket == counts.size());

    BucketBoundsImpl<T> bounds_obj;
    bounds_obj.setBounds(std::move(output_bounds));
    std::vector<double> ndvs;
    for (auto cnt : counts)
    {
        // TODO scale this
        double ndv = std::llround(total_ndv * cnt / row_count);
        if (cnt != 0)
        {
            ndv = std::max(1.0, ndv);
        }
        ndvs.push_back(ndv);
    }
    return StatsNdvBucketsResultImpl<T>::createImpl(bounds_obj, std::move(counts), std::move(ndvs));
}

}
