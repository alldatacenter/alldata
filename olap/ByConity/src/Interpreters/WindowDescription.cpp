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

#include <Interpreters/WindowDescription.h>

#include <AggregateFunctions/AggregateFunctionFactory.h>
#include <Core/Field.h>
#include <DataTypes/DataTypeHelper.h>
#include <IO/Operators.h>
#include <Parsers/ASTFunction.h>
#include <QueryPlan/PlanSerDerHelper.h>
#include <Common/FieldVisitorToString.h>
#include <Common/FieldVisitorsAccurateComparison.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int BAD_ARGUMENTS;
}

std::string WindowFunctionDescription::dump() const
{
    WriteBufferFromOwnString ss;

    ss << "window function '" << column_name << "\n";
    ss << "function node " << (function_node != nullptr ? function_node->dumpTree() : "") << "\n";
    ss << "aggregate function '" << aggregate_function->getName() << "'\n";
    if (!function_parameters.empty())
    {
        ss << "parameters " << toString(function_parameters) << "\n";
    }

    return ss.str();
}

std::string WindowDescription::dump() const
{
    WriteBufferFromOwnString ss;

    ss << "window '" << window_name << "'\n";
    ss << "partition_by " << dumpSortDescription(partition_by) << "\n";
    ss << "order_by " << dumpSortDescription(order_by) << "\n";
    ss << "full_sort_description " << dumpSortDescription(full_sort_description) << "\n";

    return ss.str();
}

std::string WindowFrame::toString() const
{
    WriteBufferFromOwnString buf;
    toString(buf);
    return buf.str();
}

void WindowFrame::toString(WriteBuffer & buf) const
{
    buf << toString(type) << " BETWEEN ";
    if (begin_type == BoundaryType::Current)
    {
        buf << "CURRENT ROW";
    }
    else if (begin_type == BoundaryType::Unbounded)
    {
        buf << "UNBOUNDED";
        buf << " "
            << (begin_preceding ? "PRECEDING" : "FOLLOWING");
    }
    else
    {
        buf << applyVisitor(FieldVisitorToString(), begin_offset);
        buf << " "
            << (begin_preceding ? "PRECEDING" : "FOLLOWING");
    }
    buf << " AND ";
    if (end_type == BoundaryType::Current)
    {
        buf << "CURRENT ROW";
    }
    else if (end_type == BoundaryType::Unbounded)
    {
        buf << "UNBOUNDED";
        buf << " "
            << (end_preceding ? "PRECEDING" : "FOLLOWING");
    }
    else
    {
        buf << applyVisitor(FieldVisitorToString(), end_offset);
        buf << " "
            << (end_preceding ? "PRECEDING" : "FOLLOWING");
    }
}

void WindowFrame::checkValid() const
{
    // Check the validity of offsets.
    if (type == WindowFrame::FrameType::Rows
        || type == WindowFrame::FrameType::Groups)
    {
        if (begin_type == BoundaryType::Offset
            && !((begin_offset.getType() == Field::Types::UInt64
                    || begin_offset.getType() == Field::Types::Int64)
                && begin_offset.get<Int64>() >= 0
                && begin_offset.get<Int64>() < INT_MAX))
        {
            throw Exception(ErrorCodes::BAD_ARGUMENTS,
                "Frame start offset for '{}' frame must be a nonnegative 32-bit integer, '{}' of type '{}' given",
                toString(type),
                applyVisitor(FieldVisitorToString(), begin_offset),
                Field::Types::toString(begin_offset.getType()));
        }

        if (end_type == BoundaryType::Offset
            && !((end_offset.getType() == Field::Types::UInt64
                    || end_offset.getType() == Field::Types::Int64)
                && end_offset.get<Int64>() >= 0
                && end_offset.get<Int64>() < INT_MAX))
        {
            throw Exception(ErrorCodes::BAD_ARGUMENTS,
                "Frame end offset for '{}' frame must be a nonnegative 32-bit integer, '{}' of type '{}' given",
                toString(type),
                applyVisitor(FieldVisitorToString(), end_offset),
                Field::Types::toString(end_offset.getType()));
        }
    }

    // Check relative positioning of offsets.
    // UNBOUNDED PRECEDING end and UNBOUNDED FOLLOWING start should have been
    // forbidden at the parsing level.
    assert(!(begin_type == BoundaryType::Unbounded && !begin_preceding));
    assert(!(end_type == BoundaryType::Unbounded && end_preceding));

    if (begin_type == BoundaryType::Unbounded
        || end_type == BoundaryType::Unbounded)
    {
        return;
    }

    if (begin_type == BoundaryType::Current
        && end_type == BoundaryType::Offset
        && !end_preceding)
    {
        return;
    }

    if (end_type == BoundaryType::Current
        && begin_type == BoundaryType::Offset
        && begin_preceding)
    {
        return;
    }

    if (end_type == BoundaryType::Current
        && begin_type == BoundaryType::Current)
    {
        // BETWEEN CURRENT ROW AND CURRENT ROW makes some sense for RANGE or
        // GROUP frames, and is technically valid for ROWS frame.
        return;
    }

    if (end_type == BoundaryType::Offset
        && begin_type == BoundaryType::Offset)
    {
        // Frame start offset must be less or equal that the frame end offset.
        bool begin_less_equal_end;
        if (begin_preceding && end_preceding)
        {
            /// we can't compare Fields using operator<= if fields have different types
            begin_less_equal_end = applyVisitor(FieldVisitorAccurateLessOrEqual(), end_offset, begin_offset);
        }
        else if (begin_preceding && !end_preceding)
        {
            begin_less_equal_end = true;
        }
        else if (!begin_preceding && end_preceding)
        {
            begin_less_equal_end = false;
        }
        else /* if (!begin_preceding && !end_preceding) */
        {
            begin_less_equal_end = applyVisitor(FieldVisitorAccurateLessOrEqual(), begin_offset, end_offset);
        }

        if (!begin_less_equal_end)
        {
            throw Exception(ErrorCodes::BAD_ARGUMENTS,
                "Frame start offset {} {} does not precede the frame end offset {} {}",
                begin_offset, begin_preceding ? "PRECEDING" : "FOLLOWING",
                end_offset, end_preceding ? "PRECEDING" : "FOLLOWING");
        }
        return;
    }

    throw Exception(ErrorCodes::BAD_ARGUMENTS,
        "Window frame '{}' is invalid",
        toString());
}

void WindowDescription::checkValid() const
{
    frame.checkValid();

    // RANGE OFFSET requires exactly one ORDER BY column.
    if (frame.type == WindowFrame::FrameType::Range
        && (frame.begin_type == WindowFrame::BoundaryType::Offset
            || frame.end_type == WindowFrame::BoundaryType::Offset)
        && order_by.size() != 1)
    {
        throw Exception(ErrorCodes::BAD_ARGUMENTS,
            "The RANGE OFFSET window frame requires exactly one ORDER BY column, {} given",
           order_by.size());
    }
}

void WindowFrame::serialize(WriteBuffer & buffer) const
{
    writeBinary(is_default, buffer);
    serializeEnum(type, buffer);

    serializeEnum(begin_type, buffer);
    writeFieldBinary(begin_offset, buffer);
    writeBinary(begin_preceding, buffer);

    serializeEnum(end_type, buffer);
    writeFieldBinary(end_offset, buffer);
    writeBinary(end_preceding, buffer);
}

void WindowFrame::deserialize(ReadBuffer & buffer)
{
    readBinary(is_default, buffer);
    deserializeEnum(type, buffer);

    deserializeEnum(begin_type, buffer);
    readFieldBinary(begin_offset, buffer);
    readBinary(begin_preceding, buffer);

    deserializeEnum(end_type, buffer);
    readFieldBinary(end_offset, buffer);
    readBinary(end_preceding, buffer);
}

void WindowFunctionDescription::serialize(WriteBuffer & buffer) const
{
    writeBinary(column_name, buffer);

    writeBinary(aggregate_function->getName(), buffer);
    writeBinary(function_parameters, buffer);
    serializeDataTypes(argument_types, buffer);
    serializeStrings(argument_names, buffer);
}

void WindowFunctionDescription::deserialize(ReadBuffer & buffer)
{
    readBinary(column_name, buffer);

    String func_name;
    readBinary(func_name, buffer);
    readBinary(function_parameters, buffer);
    argument_types = deserializeDataTypes(buffer);
    argument_names = deserializeStrings(buffer);
    AggregateFunctionProperties properties;
    aggregate_function = AggregateFunctionFactory::instance().get(func_name, argument_types, function_parameters, properties);
}

void WindowDescription::serialize(WriteBuffer & buffer) const
{
    writeBinary(window_name, buffer);
    serializeSortDescription(partition_by, buffer);
    serializeSortDescription(order_by, buffer);
    serializeSortDescription(full_sort_description, buffer);

    frame.serialize(buffer);

    writeBinary(window_functions.size(), buffer);
    for (const auto & item : window_functions)
        item.serialize(buffer);
}

void WindowDescription::deserialize(ReadBuffer & buffer)
{
    readBinary(window_name, buffer);
    deserializeSortDescription(partition_by, buffer);
    deserializeSortDescription(order_by, buffer);
    deserializeSortDescription(full_sort_description, buffer);

    frame.deserialize(buffer);

    size_t size;
    readBinary(size, buffer);
    for (size_t index = 0; index < size; ++index)
    {
        WindowFunctionDescription desc;
        desc.deserialize(buffer);
        window_functions.emplace_back(desc);
    }
}

}
