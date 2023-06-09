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

#include <AggregateFunctions/AggregateFunctionFactory.h>
#include <AggregateFunctions/AggregateFunctionRetention2.h>
#include <AggregateFunctions/AggregateRetentionCommon.h>
#include <AggregateFunctions/Helpers.h>
#include <DataTypes/DataTypeString.h>


namespace DB
{

namespace
{

    AggregateFunctionPtr createAggregateFunctionRetention2(const std::string & name, const DataTypes & argument_types, const Array & parameters, const Settings *)
    {
        if (argument_types.size() != 2)
            throw Exception("Incorrect number of arguments for aggregate function " + name, ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        const auto * array_type = checkAndGetDataType<DataTypeArray>(argument_types[0].get());
        const auto *array_type2 = checkAndGetDataType<DataTypeArray>(argument_types[1].get());
        if (!array_type || !array_type2)
            throw Exception("arguments for function " + name + " must be array.",
                            ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        size_t params_size = parameters.size();

        if (params_size != 1 && params_size != 2 && (params_size - 1) % 2 != 0)
            throw Exception("This instantiation of " + name + "aggregate function doesn't accept " + toString(params_size) + " parameters.",
                            ErrorCodes::LOGICAL_ERROR);
        UInt64 ret_window = parameters[0].safeGet<UInt64>();

        AggregateFunctionPtr res = nullptr;
        if (params_size == 2)
        {
            UInt8 v_compress = parameters[1].safeGet<UInt64>();
            switch(static_cast<CompressEnum>(v_compress))
            {
                case NO_COMPRESS_SORTED:
                    res = AggregateFunctionPtr(createWithIntegerTypeLastKnown<AggregateFunctionRetention2, compress_trait<NO_COMPRESS_SORTED>>(*array_type->getNestedType(), ret_window, argument_types, parameters));
                    break;
                case COMPRESS_BIT:
                    res = AggregateFunctionPtr(createWithIntegerTypeLastKnown<AggregateFunctionRetention2, compress_trait<COMPRESS_BIT>>(*array_type->getNestedType(), ret_window, argument_types, parameters));
                    break;
                case NO_COMPRESS_NO_SORTED:
                    res = AggregateFunctionPtr(createWithIntegerTypeLastKnown<AggregateFunctionRetention2, compress_trait<NO_COMPRESS_NO_SORTED>>(*array_type->getNestedType(), ret_window, argument_types, parameters));
                    break;
            }

            if (!res)
                throw Exception("Illegal type " + argument_types[0]->getName() + " of argument for aggregate function " + name, ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
            return res;
        }
        else if (params_size > 2)
        {
            if ((params_size - 1) % 2 != 0)
                throw Exception("Illegal size of params, it should be 1, 2, or an odd number", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

            if (argument_types[0]->getName() == "Array(Array(String))")
                res = std::make_shared<AggregateFunctionAttrRetention2<compress_trait<COMPRESS_BIT>>>(ret_window, argument_types, parameters);
            else
                res = AggregateFunctionPtr(createWithIntegerTypeLastKnown<AggregateFunctionRetention2, compress_trait<COMPRESS_BIT>>(*array_type->getNestedType(), ret_window, argument_types, parameters));

            if (!res)
                throw Exception("Illegal type " + argument_types[0]->getName() + " of argument for aggregate function " + name, ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
            return res;
        }

        if (argument_types[0]->getName() == "Array(Array(String))") {
            return std::make_shared<AggregateFunctionAttrRetention2<compress_trait<COMPRESS_BIT>>>(ret_window, argument_types, parameters);
        }

        res = AggregateFunctionPtr(createWithIntegerTypeLastKnown<AggregateFunctionRetention2, compress_trait<COMPRESS_BIT>>(*array_type->getNestedType(), ret_window, argument_types, parameters));

        if (!res)
            throw Exception("Illegal type " + argument_types[0]->getName() + " of argument for aggregate function " + name, ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
        return res;
    }
}

void registerAggregateFunctionRetention2(AggregateFunctionFactory & factory)
{
    factory.registerFunction("retention2", createAggregateFunctionRetention2, AggregateFunctionFactory::CaseInsensitive);
}

}
