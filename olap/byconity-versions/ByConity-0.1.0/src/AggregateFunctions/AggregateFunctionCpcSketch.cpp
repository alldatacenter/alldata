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
#include <Statistics/StatsCpcSketch.h>
// TODO: use datasketches
namespace DB
{
struct CpcData
{
    Statistics::StatsCpcSketch data_;

    template <typename T>
    void add(T value)
    {
        data_.update(value);
    }

    void merge(const CpcData & rhs) { return data_.merge(rhs.data_); }

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

    String getText() const { return data_.to_string(); }

    void insertResultInto(IColumn & to) const
    {
        BlobType blob_raw = data_.serialize();
        auto blob_b64 = base64Encode(blob_raw);
        static_cast<ColumnString &>(to).insertData(blob_b64.c_str(), blob_b64.size());
    }

    static String getName() { return "cpc"; }
};

template <typename T>
using CpcDataAdaptor = CpcData;

template <template <typename> class Function>
AggregateFunctionPtr
createAggregateFunctionCpcSketch(const std::string & name, const DataTypes & argument_types, const Array & parameters, const Settings *)
{
    assertNoParameters(name, parameters);
    assertUnary(name, argument_types);

    AggregateFunctionPtr res;
    DataTypePtr data_type = argument_types[0];
    WhichDataType which(data_type);

    // TODO: support most data_type
    if (DB::isColumnedAsNumber(data_type))
    {
        res.reset(createWithNumericBasedType<Function>(*data_type, argument_types));
    }
    else if (which.isStringOrFixedString())
    {
        res = std::make_shared<AggregateFunctionCboFamilyForString<CpcDataAdaptor<String>>>(argument_types);
    }

    if (!res)
    {
        throw Exception(
            "Illegal type " + argument_types[0]->getName() + " of argument for aggregate function " + name,
            ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
    }
    return res;
}

template <typename T>
struct FuncImpl
{
    using Func = AggregateFunctionCboFamily<CpcDataAdaptor, T>;
};
template <typename T>
using Func = typename FuncImpl<T>::Func;


void registerAggregateFunctionCpcSketch(AggregateFunctionFactory & factory)
{
    AggregateFunctionWithProperties functor;
    functor.creator = createAggregateFunctionCpcSketch<Func>;
    factory.registerFunction("cpc", functor);
}

}
