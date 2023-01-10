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

#include <Functions/IFunction.h>
#include <Functions/FunctionFactory.h>
#include <DataTypes/DataTypeString.h>
#include <Interpreters/Context.h>


namespace DB
{

/// Get the host. Is is constant on single server, but is not constant in distributed queries.
class FunctionGetHostWithPorts final: public IFunction
{
public:
    FunctionGetHostWithPorts(ContextPtr c) : host_ports(c->getHostWithPorts().toDebugString()) { }

    static constexpr auto name = "getHostWithPorts";
    static FunctionPtr create(ContextPtr context)
    {
        return std::make_shared<FunctionGetHostWithPorts>(context);
    }

    String getName() const override
    {
        return name;
    }

    bool isDeterministic() const override { return false; }

    bool isDeterministicInScopeOfQuery() const override
    {
        return false;
    }

    size_t getNumberOfArguments() const override
    {
        return 0;
    }

    DataTypePtr getReturnTypeImpl(const DataTypes & /*arguments*/) const override
    {
        return std::make_shared<DataTypeString>();
    }

    /** convertToFullColumn needed because in distributed query processing,
      *    each server returns its own value.
      */

    ColumnPtr executeImpl(const ColumnsWithTypeAndName & arguments, const DataTypePtr & result_type, size_t input_rows_count) const override
    {
        (void)arguments;
        return result_type->createColumnConst(input_rows_count, host_ports);
    }

private:
    String host_ports;
};


void registerFunctionGetHostWithPorts(FunctionFactory & factory)
{
    factory.registerFunction<FunctionGetHostWithPorts>();
}

}
