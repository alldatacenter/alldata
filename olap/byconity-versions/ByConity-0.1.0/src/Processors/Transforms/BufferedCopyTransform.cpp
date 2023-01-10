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

#include <Processors/Port.h>
#include <Processors/Transforms/BufferedCopyTransform.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
}

BufferedCopyTransform::BufferedCopyTransform(const Block & header, size_t num_outputs, size_t max_queue_size_)
    : IProcessor(InputPorts(1, header), OutputPorts(num_outputs, header)), max_queue_size(max_queue_size_), output_queues(num_outputs)
{
    if (num_outputs <= 1)
        throw Exception("BufferedCopyTransform expects more than 1 outputs, got " + std::to_string(num_outputs), ErrorCodes::LOGICAL_ERROR);

    output_vec.resize(num_outputs);
    auto output_it = outputs.begin();
    for (size_t i = 0; i < num_outputs; ++i)
    {
        output_vec[i] = &*output_it;
        ++output_it;
    }
}

IProcessor::Status BufferedCopyTransform::prepare(const PortNumbers &, const PortNumbers & updated_output_ports)
{
    pushed = false;

    Status status = Status::Ready;

    while (status == Status::Ready)
    {
        status = !has_data ? prepareConsume(updated_output_ports) : prepareGenerate();
    }

    return status;
}


IProcessor::Status BufferedCopyTransform::prepareConsume(const PortNumbers & updated_output_ports)
{
    auto & input = getInputPort();

    /// Check all outputs are finished or ready to get data.

    bool all_finished = true;
    for (auto & output : outputs)
    {
        if (output.isFinished())
            continue;

        all_finished = false;
    }

    if (all_finished)
    {
        input.close();
        return Status::Finished;
    }

    /// Try get chunk from input.

    if (input.isFinished())
    {
        for (auto & queue : output_queues)
        {
            if (!queue.empty())
            {
                tryFlush(updated_output_ports);
                return Status::NeedData;
            }
        }
        for (auto & output : outputs)
            output.finish();

        return Status::Finished;
    }

    input.setNeeded();
    if (!input.hasData())
    {
        tryFlush(updated_output_ports);
        return Status::NeedData;
    }
    chunk = input.pull();
    has_data = true;
    was_output_processed.assign(outputs.size(), false);

    return Status::Ready;
}


IProcessor::Status BufferedCopyTransform::prepareGenerate()
{
    if(pushed)
        return Status::PortFull;

    pushed = true;
    bool all_outputs_processed = true;

    size_t chunk_number = 0;
    for (auto & output : outputs)
    {
        auto & was_processed = was_output_processed[chunk_number];
        std::list<Chunk> & queue = output_queues[chunk_number];
        ++chunk_number;

        if (was_processed)
            continue;

        if (output.isFinished())
            continue;

        if (output.canPush())
        {
            if (queue.empty())
            {
                output.push(chunk.clone());
            }
            else
            {
                output.push(std::move(queue.front()));
                queue.pop_front();
                queue.emplace_back(chunk.clone());
            }
            was_processed = true;
            continue;
        }

        if (queue.size() < max_queue_size)
        {
            queue.emplace_back(chunk.clone());
            was_processed = true;
            continue;
        }
        all_outputs_processed = false;
    }

    if (all_outputs_processed)
    {
        has_data = false;
        return Status::Ready;
    }

    return Status::PortFull;
}

void BufferedCopyTransform::tryFlush(const PortNumbers & updated_output_ports)
{
    if(pushed)
        return;

    for (UInt64 output_id : updated_output_ports)
    {
        OutputPort * output_ptr = output_vec[output_id];
        std::list<Chunk> & queue = output_queues[output_id];
        if (output_ptr->canPush() && !queue.empty())
        {
            output_ptr->push(std::move(queue.front()));
            queue.pop_front();
        }
    }
}

}
