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

#include <QueryPlan/ApplyStep.h>

#include <DataTypes/DataTypesNumber.h>
#include <DataTypes/DataTypeNullable.h>
#include <Parsers/ASTExpressionList.h>
#include <Parsers/ASTIdentifier.h>
#include <Core/Block.h>

namespace DB
{
ApplyStep::ApplyStep(
    DataStreams input_streams_, Names correlation_, ApplyType apply_type_, SubqueryType subquery_type_, Assignment assignment_)
    : correlation(std::move(correlation_)), apply_type(apply_type_), subquery_type(subquery_type_), assignment(std::move(assignment_))
{
    setInputStreams(input_streams_);
}

void ApplyStep::setInputStreams(const DataStreams & input_streams_)
{
    input_streams = std::move(input_streams_);
    Block output;
    output = input_streams[0].header;
    output.insert(ColumnWithTypeAndName{getAssignmentDataType(), assignment.first});
    output_stream = DataStream{output};
}

DataTypePtr ApplyStep::getAssignmentDataType() const
{
    switch (subquery_type)
    {
        case ApplyStep::SubqueryType::IN: {
            auto * arguments = assignment.second->children[0]->as<ASTExpressionList>();
            auto argument_name = arguments->children[0]->as<ASTIdentifier>()->name();
            for (const auto & column : input_streams[0].header)
                if (column.name == argument_name)
                    return column.type->isNullable() ? makeNullable(std::make_shared<DataTypeUInt8>()) : std::make_shared<DataTypeUInt8>();
            throw Exception("Unknown data type for column " + argument_name, ErrorCodes::LOGICAL_ERROR);
        }
        case ApplyStep::SubqueryType::EXISTS: {
            return std::make_shared<DataTypeUInt8>();
        }
        case ApplyStep::SubqueryType::SCALAR: {
            for (const auto & column : input_streams[1].header)
                if (column.name == assignment.first)
                    return column.type->canBeInsideNullable() ? makeNullable(column.type) : column.type;
            throw Exception("Unknown data type for column " + assignment.first, ErrorCodes::LOGICAL_ERROR);
        }
        case ApplyStep::SubqueryType::QUANTIFIED_COMPARISON: {
            auto argument_name = assignment.second->children[0]->as<ASTIdentifier>()->name();
            for (const auto & column : input_streams[0].header)
                if (column.name == argument_name)
                    return column.type->isNullable() ? makeNullable(std::make_shared<DataTypeUInt8>()) : std::make_shared<DataTypeUInt8>();
            throw Exception("Unknown data type for column " + argument_name, ErrorCodes::LOGICAL_ERROR);
        }
    }
}

void ApplyStep::serialize(WriteBuffer &) const
{
    throw Exception("ApplyStep should be rewritten into JoinStep", ErrorCodes::NOT_IMPLEMENTED);
}

QueryPlanStepPtr ApplyStep::deserialize(ReadBuffer &, ContextPtr)
{
    throw Exception("ApplyStep should be rewritten into JoinStep", ErrorCodes::NOT_IMPLEMENTED);
}

QueryPipelinePtr ApplyStep::updatePipeline(QueryPipelines, const BuildQueryPipelineSettings &)
{
    throw Exception("ApplyStep should be rewritten into JoinStep", ErrorCodes::NOT_IMPLEMENTED);
}

std::shared_ptr<IQueryPlanStep> ApplyStep::copy(ContextPtr) const
{
    return std::make_shared<ApplyStep>(input_streams, correlation, apply_type, subquery_type, assignment);
}
}
