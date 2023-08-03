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

#include <Columns/ColumnsCommon.h>
#include <Columns/ColumnsNumber.h>
#include <Columns/FilterDescription.h>
#include <Processors/Transforms/FinalSampleTransform.h>
#include <Common/PODArray.h>
#include <Common/SipHash.h>

namespace DB
{
FinalSampleTransform::FinalSampleTransform(const Block & header_, size_t sample_size_, size_t max_chunk_size_, size_t num_streams_)
    : IProcessor({num_streams_, header_}, {1, header_})
    , sample_size(sample_size_)
    , max_chunk_size(max_chunk_size_)
{
    sampled_chunks = std::make_shared<SampledChunks>(sample_size, max_chunk_size);
}

IProcessor::Status FinalSampleTransform::prepare()
{
    auto & output = outputs.front();

    /// Check can output.
    if (output.isFinished() || stop_reading)
    {
        output.finish();
        for (auto & input : inputs)
            input.close();

        if (has_output && output_chunk)
        {
            output.push(std::move(output_chunk));
            has_output = false;
        }

        return Status::Finished;
    }

    if (!output.canPush())
    {
        for (auto & input : inputs)
            input.setNotNeeded();
        return Status::PortFull;
    }

    /// Output if has data.
    if (has_output)
    {
        output.push(std::move(output_chunk));
        has_output = false;

        return Status::PortFull;
    }

    if (has_input)
        return Status::Ready;

    bool all_inputs_finished = true;
    for (auto & input: inputs)
    {
        if (!input.isFinished())
        {
            all_inputs_finished = false;
            break;
        }
    }
    if (all_inputs_finished)
    {
        output.finish();
        return Status::Finished;
    }

    for (auto & input: inputs)
    {
        input.setNeeded();
        if (input.hasData())
        {
            input_chunk = input.pull(true);
            has_input = true;
            return Status::Ready;
        }
    }

    return Status::NeedData;
}

void FinalSampleTransform::work()
{
    if (!sampled_chunks->add(std::move(input_chunk)))
        stop_reading = true;

    has_input = false;
    output_chunk = std::move(sampled_chunks->getResult().sampled_chunk);
    has_output = true;
}

size_t FinalSampleTransform::SampledChunk::sample(const size_t sample_size_, const size_t total_rows)
{
    size_t rows = sampled_chunk.getNumRows();

    if (sample_size_ == 0 || total_rows == 0)
        return rows;

    if (!rows)
        return 0;

    auto filter = ColumnUInt8::create();
    ColumnUInt8::Container & filter_vec = filter->getData();
    filter_vec.resize_fill(rows);

    Float64 sample_level_size = static_cast<Float64>(rows) / total_rows * sample_size_;
    Float64 step = static_cast<Float64>(rows) / sample_level_size;

    if (step <= 1)
        return rows;

    if (step > 1 && step < 2)
    {
        size_t base = 10;
        size_t window = step * base;
        for (size_t idx = 0; idx < rows; idx += window)
        {
            memset(filter_vec.data() + idx, 1, base);
        }
    }
    else
    {
        size_t window = step * 10 / 10;
        for (size_t idx = 0; idx < rows; idx += window)
            filter_vec[idx] = 1;
    }

    ConstantFilterDescription constant_filter_description = ConstantFilterDescription(*filter);

    if (constant_filter_description.always_false)
    {
        sampled_chunk.clear();
        return 0;
    }

    if (constant_filter_description.always_true)
        return rows;

    FilterDescription filter_and_holder(*filter);
    size_t columns = sampled_chunk.getNumColumns();

    size_t first_non_constant_column = 0;
    for (size_t i = 0; i < columns; ++i)
    {
        if (!isColumnConst(*(sampled_chunk.getColumns()[i])))
        {
            first_non_constant_column = i;
            break;
        }
    }

    Columns result_columns(sampled_chunk.getNumColumns());
    size_t filtered_rows = 0;
    auto first_column = sampled_chunk.getColumns()[first_non_constant_column];
    first_column = first_column->filter(*filter_and_holder.data, -1);
    filtered_rows = first_column->size();
    result_columns[first_non_constant_column] = first_column;

    if (filtered_rows == 0)
    {
        sampled_chunk.clear();
        return 0;
    }

    if (filtered_rows == filter_and_holder.data->size())
        return filtered_rows;

    /// Filter the rest of the columns.
    for (size_t i = 0; i < columns; ++i)
    {
        const auto & current_column = sampled_chunk.getColumns()[i];

        if (i == first_non_constant_column)
            continue;

        if (isColumnConst(*current_column))
            result_columns[i] = current_column->cut(0, filtered_rows);
        else
            result_columns[i] = current_column->filter(*filter_and_holder.data, -1);
    }
    sampled_chunk.setColumns(std::move(result_columns), filtered_rows);
    return filtered_rows;
}

void FinalSampleTransform::SampledChunk::merge(const SampledChunk & rhs_sampled_chunk)
{
    const Chunk & chunk = rhs_sampled_chunk.sampled_chunk;
    MutableColumns res_columns = sampled_chunk.mutateColumns();
    Columns add_columns = chunk.getColumns();
    size_t column_size = res_columns.size();
    size_t size = 0;
    for (size_t i = 0; i < column_size; ++i)
    {
        size_t length = add_columns[i]->size();
        size = length;
        res_columns[i]->insertRangeFrom(*add_columns[i], 0, length);
    }
    sampled_chunk.setColumns(std::move(res_columns), size);
}


bool FinalSampleTransform::SampledChunks::add(Chunk && chunk)
{
    if (!chunk)
        return false;

    if (to_sample_size == 0)
        return false;

    chunks.emplace_back(std::move(chunk));
    size_t total_rows = sampled_rows + chunks.back().rows();
    size_t rows = 0;

    for (auto it = chunks.begin(); it != chunks.end();)
    {
        auto res_rows = it->sample(to_sample_size, total_rows);
        if (res_rows == 0)
            it = chunks.erase(it);
        else
        {
            rows += res_rows;
            ++it;
        }
    }

    if (rows >= max_chunk_size || rows >= to_sample_size)
        return false;

    sampled_rows = rows;

    return true;
}

FinalSampleTransform::SampledChunk FinalSampleTransform::SampledChunks::getResult()
{
    if (chunks.empty())
    {
        Chunk empty;
        FinalSampleTransform::SampledChunk empty_sample(std::move(empty));
        return empty_sample;
    }

    SampledChunk res = std::move(chunks.front());

    for (size_t idx = 1; idx < chunks.size(); ++idx)
    {
        res.merge(chunks[idx]);
    }

    clear();
    to_sample_size = to_sample_size > res.rows() ? to_sample_size - res.rows() : 0;

    return res;
}
}
