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

#include <AggregateFunctions/AggregateFunctionCboFamily.h>
#include <AggregateFunctions/AggregateFunctionFactory.h>
#include <AggregateFunctions/FactoryHelpers.h>
#include <AggregateFunctions/Helpers.h>
#include <Statistics/Base64.h>
#include <Statistics/StatisticsBaseImpl.h>
#include <Statistics/StatsNdvBucketsExtendImpl.h>
#include <Common/FieldVisitors.h>
#include <Common/FieldVisitorToString.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int NUMBER_OF_ARGUMENTS_DOESNT_MATCH;
}

// This agg fucntion is to calculate Ndvs in Histogram
// it takes BucketBuckets as a paratmeter
// and build histogram using StatsNdvBuckets

// extend version will receive two arguments
// first is <col>
// second is hash(<col>, _mark_id)
template <typename T>
struct NdvBucketsExtendData
{
    // UUID is in fact UInt128, use UInt128 for calculation
    using EmbeddedType = std::conditional_t<std::is_same_v<T, UUID>, UInt128, T>;
    // Extend version: add block_ndvs
    Statistics::StatsNdvBucketsExtendImpl<EmbeddedType> data_;

    NdvBucketsExtendData() = default;

    NdvBucketsExtendData(std::string_view blob)
    {
        Statistics::BucketBoundsImpl<EmbeddedType> bounds;
        bounds.deserialize(blob);
        data_.initialize(std::move(bounds));
    }

    void add(T value, UInt64 block_value) { data_.update(value, block_value); }

    void merge(const NdvBucketsExtendData & rhs) { data_.merge(rhs.data_); }

    using BlobType = String;
    void write(WriteBuffer & buf) const
    {
        BlobType blob = data_.serialize();
        writeBinary(blob, buf);
    }

    void read(ReadBuffer & buf)
    {
        BlobType blob;
        readBinary(blob, buf);
        data_.deserialize(blob);
    }

    std::string getText() const { return data_.toString(); }

    void insertResultInto(IColumn & to) const
    {
        auto blob_raw = data_.serialize();
        auto blob_b64 = base64Encode(blob_raw);
        static_cast<ColumnString &>(to).insertData(blob_b64.c_str(), blob_b64.size());
    }

    static String getName() { return "ndv_buckets_extend"; }
};


template <template <typename> class Function>
AggregateFunctionPtr
createAggregateFunctionNdvBucketsExtend(const std::string & name, const DataTypes & argument_types, const Array & parameters, const Settings*)
{
    if (parameters.empty())
    {
        throw Exception("params mismatch", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);
    }

    assertBinary(name, argument_types);
    if (argument_types[1]->getTypeId() != TypeIndex::UInt64)
    {
        throw Exception("The second type is required to be UInt64", ErrorCodes::TYPE_MISMATCH);
    }

    auto blob_b64 = applyVisitor(FieldVisitorToString(), parameters[0]);
    blob_b64 = [](std::string_view view) -> std::string_view {
        // trim '\'
        if (view.size() < 2)
            return {};
        else if (view.front() == '\'' && view.back() == '\'')
        {
            return view.substr(1, view.size() - 2);
        }
        else
        {
            return view;
        }
    }(blob_b64);
    auto blob = Statistics::base64Decode(blob_b64);

    AggregateFunctionPtr res;
    DataTypePtr data_type = argument_types[0];

    // TODO: support most data_type
    if (isColumnedAsNumber(data_type))
    {
        // TODO: add bounds
        res.reset(createWithNumericBasedType<Function>(*data_type, argument_types, blob));
    }
    else if (isStringOrFixedString(data_type))
    {
        res = std::make_shared<AggregateFunctionCboFamilyForString<NdvBucketsExtendData<String>, true>>(argument_types, blob);
    }

    if (!res)
        throw Exception(
            "Illegal type " + argument_types[0]->getName() + " of argument for aggregate function " + name,
            ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
    return res;
}

template <typename T>
struct FuncImpl
{
    using Func = AggregateFunctionCboFamily<NdvBucketsExtendData, T, true>;
};
template <typename T>
using Func = typename FuncImpl<T>::Func;


void registerAggregateFunctionNdvBucketsExtend(AggregateFunctionFactory & factory)
{
    AggregateFunctionWithProperties functor;
    functor.creator = createAggregateFunctionNdvBucketsExtend<Func>;
    factory.registerFunction("ndv_buckets_extend", functor);
}

}
