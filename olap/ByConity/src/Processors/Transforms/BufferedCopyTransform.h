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

#pragma once
#include <list>
#include <vector>
#include <Processors/IProcessor.h>

namespace DB
{
/// Transform which has single input and num_outputs outputs.
/// Read chunk from input and copy it to all output queues.
class BufferedCopyTransform : public IProcessor
{
public:
    BufferedCopyTransform(const Block & header, size_t num_outputs, size_t max_queue_size_);

    String getName() const override { return "BufferedCopy"; }
    Status prepare(const PortNumbers & updated_input_ports, const PortNumbers & updated_output_ports) override;
    InputPort & getInputPort() { return inputs.front(); }

private:
    Chunk chunk;
    bool has_data = false;
    bool pushed = false;
    size_t max_queue_size;
    std::vector<char> was_output_processed;
    std::vector<std::list<Chunk>> output_queues;
    std::vector<OutputPort *> output_vec;
    Status prepareGenerate();
    Status prepareConsume(const PortNumbers & updated_output_ports);
    void tryFlush(const PortNumbers & updated_output_ports);
};

}
