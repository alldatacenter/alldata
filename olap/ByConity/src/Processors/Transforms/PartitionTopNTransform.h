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

#include <Common/WeakHash.h>
#include <Processors/IProcessor.h>
#include <Core/ColumnNumbers.h>
#include <Common/AlignedBuffer.h>
#include <deque>
#include <queue>
#include <type_traits>

namespace DB
{

enum PartitionTopNModel
{
    RowNumber       = 0,
    RANKER          = 1,
    DENSE_RANK      = 2
};

struct PartitionTopNTransform : IProcessor
{
    PartitionTopNTransform(Block header, size_t topN, ColumnNumbers partition_by_column_, ColumnNumbers order_by_columns_, PartitionTopNModel model, bool reverse = false);
    String getName() const override { return "PartitionTopNTransform"; }

    Status prepare() override;
    void work() override;

private:
    struct RowNumber
    {
        uint64_t block = 0;
        uint64_t row = 0;

        bool operator < (const RowNumber & other) const
        {
            return block < other.block
                || (block == other.block && row < other.row);
        }

        bool operator == (const RowNumber & other) const
        {
            return block == other.block && row == other.row;
        }

        bool operator <= (const RowNumber & other) const
        {
            return *this < other || *this == other;
        }
    };

    PartitionTopNModel model;

    size_t topN;
    ColumnNumbers partition_by_columns;
    ColumnNumbers order_by_columns;
    bool reverse;

    bool receive_all_data = false;
    bool start_output_chunk = false;
    bool handle_all_data = false;
    Chunk chunk;
    std::vector<Chunk> chunk_list;

    std::unordered_map<size_t, std::priority_queue<Field>> partition_to_heap;
    std::unordered_map<size_t, std::priority_queue<Field, std::vector<Field>, std::greater<Field>>> partition_to_heap_reverse;
    std::unordered_map<size_t, std::map<Field, std::vector<RowNumber>>> partition_to_map;

    std::list<Chunk> output_chunk_list;

    WeakHash32 hash;
// For tests
public:
    void setChunk(Chunk chunk_) {std::swap(chunk, chunk_);}
    void setReceiveAllData(bool receive_all_data_) {receive_all_data = receive_all_data_;}
    void setStartOutputChunk(bool start_output_chunk_) {start_output_chunk = start_output_chunk_;}
    void printOutputChunk()
    {
        while (!output_chunk_list.empty())
        {
            Chunk & output_first_chunk = output_chunk_list.front();
            output_first_chunk.dumpStructure();
            for (size_t i = 0; i < output_first_chunk.getNumRows(); i++)
            {
                for (size_t j = 0; j < output_first_chunk.getNumColumns(); j++)
                {
                    std::cerr << output_first_chunk.getColumns()[j]->get64(i) << "\t";
                }
                std::cerr<<std::endl;
            }
            output_chunk_list.pop_front();
        }
    }
};


}
