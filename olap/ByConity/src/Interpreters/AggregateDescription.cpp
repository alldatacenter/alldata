/*
 * Copyright 2016-2023 ClickHouse, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#include <Interpreters/AggregateDescription.h>
#include <Common/FieldVisitorToString.h>
#include <Common/JSONBuilder.h>
#include <IO/Operators.h>
#include <IO/ReadHelpers.h>
#include <IO/WriteHelpers.h>
#include <DataTypes/DataTypeHelper.h>
#include <AggregateFunctions/AggregateFunctionFactory.h>


namespace DB
{

void AggregateDescription::explain(WriteBuffer & out, size_t indent) const
{
    String prefix(indent, ' ');

    out << prefix << column_name << '\n';

    auto dump_params = [&](const Array & arr)
    {
        bool first = true;
        for (const auto & param : arr)
        {
            if (!first)
                out << ", ";

            first = false;

            out << applyVisitor(FieldVisitorToString(), param);
        }
    };

    if (function)
    {
        /// Double whitespace is intentional.
        out << prefix << "  Function: " << function->getName();

        const auto & params = function->getParameters();
        if (!params.empty())
        {
            out << "(";
            dump_params(params);
            out << ")";
        }

        out << "(";

        bool first = true;
        for (const auto & type : function->getArgumentTypes())
        {
            if (!first)
                out << ", ";
            first = false;

            out << type->getName();
        }

        out << ") → " << function->getReturnType()->getName() << "\n";
    }
    else
        out << prefix << "  Function: nullptr\n";

    if (!parameters.empty())
    {
        out << prefix << "  Parameters: ";
        dump_params(parameters);
        out << '\n';
    }

    out << prefix << "  Arguments: ";

    if (argument_names.empty())
        out << "none\n";
    else
    {
        bool first = true;
        for (const auto & arg : argument_names)
        {
            if (!first)
                out << ", ";
            first = false;

            out << arg;
        }
        out << "\n";
    }

    out << prefix << "  Argument positions: ";

    if (arguments.empty())
        out << "none\n";
    else
    {
        bool first = true;
        for (auto arg : arguments)
        {
            if (!first)
                out << ", ";
            first = false;

            out << arg;
        }
        out << '\n';
    }
}

void AggregateDescription::explain(JSONBuilder::JSONMap & map) const
{
    map.add("Name", column_name);

    if (function)
    {
        auto function_map = std::make_unique<JSONBuilder::JSONMap>();

        function_map->add("Name", function->getName());

        const auto & params = function->getParameters();
        if (!params.empty())
        {
            auto params_array = std::make_unique<JSONBuilder::JSONArray>();
            for (const auto & param : params)
                params_array->add(applyVisitor(FieldVisitorToString(), param));

            function_map->add("Parameters", std::move(params_array));
        }

        auto args_array = std::make_unique<JSONBuilder::JSONArray>();
        for (const auto & type : function->getArgumentTypes())
            args_array->add(type->getName());

        function_map->add("Argument Types", std::move(args_array));
        function_map->add("Result Type", function->getReturnType()->getName());

        map.add("Function", std::move(function_map));
    }

    auto args_array = std::make_unique<JSONBuilder::JSONArray>();
    for (const auto & name : argument_names)
        args_array->add(name);

    map.add("Arguments", std::move(args_array));

    if (!arguments.empty())
    {
        auto args_pos_array = std::make_unique<JSONBuilder::JSONArray>();
        for (auto pos : arguments)
            args_pos_array->add(pos);

        map.add("Argument Positions", std::move(args_pos_array));
    }
}

void AggregateDescription::serialize(WriteBuffer & buf) const
{
    writeBinary(function->getName(), buf);
    writeBinary(function->getArgumentTypes().size(), buf);
    for (const auto & type : function->getArgumentTypes())
    {
        serializeDataType(type, buf);
    }

    writeBinary(parameters, buf);
    writeBinary(arguments, buf);
    writeBinary(argument_names, buf);
    writeBinary(column_name, buf);
    writeBinary(mask_column, buf);
}

void AggregateDescription::deserialize(ReadBuffer & buf)
{
    String func_name;
    readBinary(func_name, buf);
    DataTypes data_types;
    size_t type_size;
    readBinary(type_size, buf);
    data_types.resize(type_size);

    for (size_t i = 0; i < type_size; ++i)
        data_types[i] = deserializeDataType(buf);

    readBinary(parameters, buf);
    readBinary(arguments, buf);
    readBinary(argument_names, buf);
    readBinary(column_name, buf);
    readBinary(mask_column, buf);

    AggregateFunctionProperties properties;
    function = AggregateFunctionFactory::instance().get(func_name, data_types, parameters, properties);
}

}
