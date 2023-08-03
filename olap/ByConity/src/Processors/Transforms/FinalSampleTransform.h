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
#include <Processors/ISimpleTransform.h>
#include <Common/HashTable/HashMap.h>


namespace DB
{
/// Executes SAMPLE for query.
class FinalSampleTransform : public IProcessor
{
public:
    FinalSampleTransform(const Block & header, size_t sample_size, size_t max_chunk_size, size_t num_streams = 1);

    String getName() const override { return "FinalSampleTransform"; }

    Status prepare() override;

    void work() override;

private:
    struct SampledChunk
    {
        SampledChunk(Chunk && chunk) : sampled_chunk(std::move(chunk)) { }
        Chunk sampled_chunk;
        size_t sample(const size_t sample_size, const size_t total_rows);
        size_t rows() { return sampled_chunk.getNumRows(); }
        void merge(const SampledChunk & rhs_sampled_chunk);
    };

    struct SampledChunks
    {
        SampledChunks(const size_t sample_size_, const size_t max_chunk_size_)
            : to_sample_size(sample_size_), max_chunk_size(max_chunk_size_)
        {
        }
        std::vector<SampledChunk> chunks;
        size_t to_sample_size;
        size_t max_chunk_size;
        size_t sampled_rows = 0;
        bool add(Chunk && chunk);
        SampledChunk getResult();
        void clear()
        {
            chunks.clear();
            sampled_rows = 0;
        }
        String toString()
        {
            String res = "\n";
            for (auto & c : chunks)
            {
                res += c.sampled_chunk.dumpStructure();
                res += "\n";
            }
            res = res + "to_sample_size: " + std::to_string(to_sample_size) + "\n";
            res = res + "sampled_rows: " + std::to_string(sampled_rows) + "\n";
            return res;
        }
    };

    size_t sample_size;
    size_t max_chunk_size;
    Chunk input_chunk;
    Chunk output_chunk;

    bool has_input = false;
    bool has_output = false;
    bool stop_reading = false;
    Block header;
    std::shared_ptr<SampledChunks> sampled_chunks;
};

}
