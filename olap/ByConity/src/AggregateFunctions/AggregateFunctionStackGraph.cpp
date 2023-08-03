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

#include <AggregateFunctions/AggregateFunctionStackGraph.h>
#include <AggregateFunctions/AggregateFunctionCombinatorFactory.h>
#include <AggregateFunctions/AggregateFunctionFactory.h>
#include <AggregateFunctions/FactoryHelpers.h>
#include <Functions/FunctionHelpers.h>
#include <AggregateFunctions/Helpers.h>


namespace DB
{

namespace ErrorCodes
{
    extern const int ILLEGAL_TYPE_OF_ARGUMENT;
    extern const int NUMBER_OF_ARGUMENTS_DOESNT_MATCH;
}

class AggregateFunctionCombinatorStack final : public IAggregateFunctionCombinator
{
public:
    static constexpr size_t params_size = 3;

    String getName() const override { return "Stack"; }

    DataTypes transformArguments(const DataTypes & arguments) const override
    {
        if (arguments.empty())
            throw Exception("Incorrect number of arguments for aggregate function with " + getName() + " suffix",
                            ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        return DataTypes(arguments.begin(), arguments.end() - 1);
    }

    Array transformParameters(const Array & params) const override
    {
        if (params.size() < params_size)
            throw Exception("Incorrect number of parameters for aggregate function with " + getName() + " suffix",
                            ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        return Array(params.begin(), params.end() - params_size);
    }

    AggregateFunctionPtr transformAggregateFunction(
        const AggregateFunctionPtr & nested_function,
        const AggregateFunctionProperties &,
        const DataTypes & arguments,
        const Array & params) const override
    {
        WhichDataType which {arguments.back()};

        if (which.isNativeUInt() || which.isDate() || which.isDateTime())
        {
            UInt64 begin = params[params.size() - 3].safeGet<UInt64>();
            UInt64 end = params[params.size() - 2].safeGet<UInt64>();
            UInt64 step = params[params.size() - 1].safeGet<UInt64>();
            return std::make_shared<AggregateFunctionStack<UInt64>>(nested_function, begin, end, step, arguments, params);
        }

        if (which.isNativeInt() || which.isEnum() || which.isInterval())
        {
            Int64 begin, end;

            // notice: UInt64 -> Int64 may lead to overflow
            if (!params[params.size() - 3].tryGet<Int64>(begin))
                begin = params[params.size() - 3].safeGet<UInt64>();
            if (!params[params.size() - 2].tryGet<Int64>(end))
                end = params[params.size() - 2].safeGet<UInt64>();
            UInt64 step = params[params.size() - 1].safeGet<UInt64>();

            return std::make_shared<AggregateFunctionStack<Int64>>(nested_function, begin, end, step, arguments, params);
        }

        throw Exception(
            ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT,
            "Illegal types of argument for aggregate function {}, the type of the last argument should be native integer or integer-like",
            getName());
    }
};


template <template <typename, typename> class FunctionTemplate>
AggregateFunctionPtr createAggregateFunctionMergeStreamStack(const std::string & name, const DataTypes & argument_types, const Array & parameters, const Settings *)
{
    assertNoParameters(name, parameters);
    assertUnary(name, argument_types);

    if (const auto * array = checkAndGetDataType<DataTypeArray>(argument_types[0].get()))
    {
        const auto *tuple = checkAndGetDataType<DataTypeTuple>(array->getNestedType().get());

        if (!tuple || tuple->getElements().size() != 2)
            throw Exception("Function MergeStreamStack argument type must be Array(Tuple(Number, Number))", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        DataTypePtr key_type = tuple->getElements()[0];
        DataTypePtr value_type = tuple->getElements()[1];

        AggregateFunctionPtr res(createWithTwoBasicNumericTypes<FunctionTemplate>(*key_type, *value_type, argument_types));
        if (!res)
            throw Exception("Illegal types " + key_type->getName() + " and " + value_type->getName()
                            + " of arguments for aggregate function " + name, ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        return res;
    }
    else
        throw  Exception("Function MergeStreamStack argument type must be Array(Tuple(Number, Number))", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);
}

void registerAggregateFunctionCombinatorStack(AggregateFunctionCombinatorFactory & factory)
{
    factory.registerCombinator(std::make_shared<AggregateFunctionCombinatorStack>());
}

void registerAggregateFunctionMergeStreamStack(AggregateFunctionFactory & factory)
{
    factory.registerFunction("MergeStreamStack", createAggregateFunctionMergeStreamStack<AggregateFunctionMergeStreamStack>);
}

}
