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
#include <Statistics/StatisticsBaseImpl.h>
#include <Common/Exception.h>

#include <Statistics/Base64.h>
#include <Statistics/DataSketchesHelper.h>

namespace DB::Statistics
{
class StatsCpcSketch : public StatisticsBase
{
public:
    static constexpr auto default_lg_k = 12;
    static constexpr auto tag = StatisticsTag::CpcSketch;
    StatsCpcSketch() : data(default_lg_k) { }

    String serialize() const override;
    void deserialize(std::string_view blob) override;
    StatisticsTag getTag() const override { return tag; }

    double get_estimate() const { return data.get_estimate(); }

    template <typename T>
    void update(const T & value)
    {
        if constexpr (std::is_arithmetic_v<T> || std::is_same_v<T, String>)
        {
            data.update(value);
        }
        else
        {
            static_assert(std::is_trivial_v<T> || std::is_same_v<UUID, T>);
            T v = value;
            data.update(&v, sizeof(v));
        }
    }

    void merge(const StatsCpcSketch & rhs)
    {
        datasketches::cpc_union un(default_lg_k);
        un.update(data);
        un.update(rhs.data);
        data = un.get_result();
    }

    // To human-readable text
    String to_string() const { return data.to_string(); }

private:
    datasketches::cpc_sketch data;
};

// transform ndv to integer, and make it no greater than count
inline UInt64 AdjustNdvWithCount(double ndv_estimate, UInt64 count)
{
    UInt64 int_ndv = std::llround(ndv_estimate);
    return std::min(int_ndv, count);
}

}
