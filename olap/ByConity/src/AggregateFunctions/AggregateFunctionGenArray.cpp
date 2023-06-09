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
#include <AggregateFunctions/AggregateFunctionGenArray.h>
#include <AggregateFunctions/Helpers.h>

namespace DB
{

namespace
{

    AggregateFunctionPtr createAggregateFunctionGenArray(const std::string & name, const DataTypes & argument_types, const Array & parameters, const Settings *)
    {
        if (parameters.size() != 3 && parameters.size() != 4)
            throw Exception("Aggregate function " + name + " require at three or four parameters", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);
        if (argument_types.size() != 1 && argument_types.size() != 2)
            throw Exception("Incorrect number of arguments for aggregate function " + name, ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        if (!isNumber(*argument_types[0]))
            throw Exception("Illegal type " + argument_types[0]->getName() + " of argument for aggregate function " + name,
                            ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        AggregateFunctionPtr res = nullptr;
        UInt64 number_steps = parameters[0].safeGet<UInt64>();
        UInt64 start_time = parameters[1].safeGet<UInt64>();
        UInt64 ret_step = parameters[2].safeGet<UInt64>();

        if (argument_types.size() == 2)
        {
            res = AggregateFunctionPtr(createWithTwoTypes<AggregateFunctionAttrGenArray>(*argument_types[0], *argument_types[1], number_steps, start_time, ret_step, argument_types, parameters));
        }
        else
        {
            if (parameters.size() == 4)
            {
                UInt8 v_compress = parameters[3].safeGet<UInt64>();
                switch (static_cast<CompressEnum>(v_compress))
                {
                    case NO_COMPRESS_SORTED:
                        res = AggregateFunctionPtr(createWithIntegerType<AggregateFunctionGenArray2>(*argument_types[0], number_steps, start_time, ret_step, argument_types, parameters));
                        break;
                    case COMPRESS_BIT:
                        res = AggregateFunctionPtr(createWithIntegerType<AggregateFunctionGenArray>(*argument_types[0], number_steps, start_time, ret_step, argument_types, parameters));
                        break;
                    case NO_COMPRESS_NO_SORTED:
                        res = AggregateFunctionPtr(createWithIntegerType<AggregateFunctionGenArray3>(*argument_types[0], number_steps, start_time, ret_step, argument_types, parameters));
                        break;
                }
                if (!res)
                    throw Exception("Illegal type " + argument_types[0]->getName() + " of argument for aggregate function " + name, ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

                return res;
            }
            res = AggregateFunctionPtr(createWithIntegerType<AggregateFunctionGenArray>(*argument_types[0], number_steps, start_time, ret_step, argument_types, parameters));
        }

        if (!res)
            throw Exception("Illegal type " + argument_types[0]->getName() + " of argument for aggregate function " + name, ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        return res;
    }
}

void registerAggregateFunctionGenArray(AggregateFunctionFactory & factory)
{
    factory.registerFunction("genArray", createAggregateFunctionGenArray, AggregateFunctionFactory::CaseInsensitive);
}

}
