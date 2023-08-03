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

#include <Processors/Transforms/EnforceSingleRowTransform.h>
namespace DB
{
namespace ErrorCodes
{
    extern const int INCORRECT_RESULT_OF_SCALAR_SUBQUERY;
}

EnforceSingleRowTransform::EnforceSingleRowTransform(const Block & header_)
    : IProcessor({header_}, {header_}), input(inputs.front()), output(outputs.front())
{
}

IProcessor::Status EnforceSingleRowTransform::prepare()
{
    // continuous pull data from input until we get an exception or input is finished
    if (!input.isFinished())
    {
        input.setNeeded();
        if (!input.hasData())
            return Status::NeedData;

        auto input_data = input.pullData();
        size_t rows = input_data.chunk.getNumRows();
        if (rows > 1 || (rows == 1 && has_input))
            input_data.exception = std::make_exception_ptr(
                Exception("Scalar sub-query has returned multiple rows", ErrorCodes::INCORRECT_RESULT_OF_SCALAR_SUBQUERY));

        if (!has_input || input_data.exception)
        {
            single_row = std::move(input_data);
            has_input = true;
        }
    }
    else
    {
        if (!has_input)
        {
            single_row = createNullSingleRow();
            has_input = true;
        }

        if (output_finished)
        {
            output.finish();
            return Status::Finished;
        }
    }

    // if we have an exception, push into output and close input.
    if (single_row.exception)
    {
        // An exception occurred during processing.
        output.pushData(std::move(single_row));
        output.finish();
        input.close();
        output_finished = true;
        return Status::Finished;
    }

    // push into output if we have data
    if (!output_finished)
    {
        if (output.canPush())
        {
            output.pushData(std::move(single_row));
            output_finished = true;
        }
        return Status::PortFull;
    }

    return Status::NeedData;
}

Port::Data EnforceSingleRowTransform::createNullSingleRow() const
{
    Port::Data data;

    Block null_block;
    for (const auto & col : input.getHeader().getColumnsWithTypeAndName())
    {
        if (!col.type->isNullable())
        {
            data.exception = std::make_exception_ptr(
                Exception("Non-nullable type scalar sub-query return empty", ErrorCodes::INCORRECT_RESULT_OF_SCALAR_SUBQUERY));
            return data;
        }
        auto column = col.type->createColumn();
        column->insertDefault();
        null_block.insert({std::move(column), col.type, col.name});
    }
    data.chunk.setColumns(null_block.getColumns(), 1);
    return data;
}
}
