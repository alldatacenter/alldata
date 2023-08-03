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

#include <Processors/Transforms/PartitionTopNTransform.h>

#include <AggregateFunctions/AggregateFunctionFactory.h>
#include <Common/Arena.h>
#include <Common/FieldVisitorsAccurateComparison.h>
#include <Columns/ColumnConst.h>
#include <Columns/ColumnsCommon.h>
#include <DataTypes/DataTypeNullable.h>
#include <Interpreters/ExpressionActions.h>
#include <Interpreters/convertFieldToType.h>


namespace DB
{

struct Settings;

namespace ErrorCodes
{
    extern const int BAD_ARGUMENTS;
    extern const int NOT_IMPLEMENTED;
}

PartitionTopNTransform::PartitionTopNTransform(Block header, size_t topN_, ColumnNumbers partition_by_columns_, ColumnNumbers order_by_columns_, PartitionTopNModel model_, bool reverse_)
    : IProcessor(InputPorts{header}, OutputPorts{header})
    , model(model_)
    , topN(topN_)
    , partition_by_columns(std::move(partition_by_columns_))
    , order_by_columns(std::move(order_by_columns_))
    , reverse(reverse_)
    , hash(0)
{
    if (order_by_columns.size() != 1)
    {
        throw Exception("currently order by columns size only support one", ErrorCodes::LOGICAL_ERROR);
    }
    if (model > PartitionTopNModel::DENSE_RANK)
    {
        throw Exception("invalid data", ErrorCodes::LOGICAL_ERROR);
    }
}

IProcessor::Status PartitionTopNTransform::prepare()
{
    auto & input = inputs.front();
    auto & output = outputs.front();

    if (receive_all_data)
    {
        if (start_output_chunk)
        {
            if (handle_all_data)
            {
                input.close();
                output.finish();
                return Status::Finished;
            }
            else
            {
                if (!output.canPush())
                    return Status::PortFull;
            }
        }
        return Status::Ready;
    }

    if (output.isFinished())
    {
        input.close();
        return Status::Finished;
    }

    if (!output.canPush())
        return Status::PortFull;

    if (input.isFinished())
    {
        receive_all_data = true;
        return Status::Ready;
    }

    input.setNeeded();
    if (!input.hasData())
        return Status::NeedData;

    chunk = input.pull();
    return Status::Ready;
}

void PartitionTopNTransform::work()
{
    if (start_output_chunk)
    {
        if (output_chunk_list.empty())
        {
            handle_all_data = true;
            return;
        }
        Chunk & output_first_chunk = output_chunk_list.front();
        auto & output = outputs.front();
        if (!output.canPush())
            return;
        output.push(std::move(output_first_chunk));
        output_chunk_list.pop_front();
        if (output_chunk_list.empty())
            handle_all_data = true;
        return;
    }

    if (chunk)
    {
        // receive chunk
        size_t chunk_list_size = chunk_list.size();
        auto num_rows = chunk.getNumRows();
        const auto & columns = chunk.getColumns();
        hash.reset(num_rows);
        for (const auto & column_number : partition_by_columns)
            columns[column_number]->updateWeakHash32(hash);

        const auto & hash_data = hash.getData();

        bool find_value = false;
        for (size_t row = 0; row < num_rows; ++row)
        {
            size_t hash_value = hash_data[row];
            // the partition key is not exists, just create one
            if (partition_to_heap.find(hash_value) == partition_to_heap.end())
            {
                if (!reverse)
                    partition_to_heap.emplace(std::make_pair(hash_value, std::priority_queue<Field>()));
                else
                    partition_to_heap_reverse.emplace(
                        std::make_pair(hash_value, std::priority_queue<Field, std::vector<Field>, std::greater<Field>>()));

                partition_to_map.emplace(std::make_pair(hash_value, std::map<Field, std::vector<RowNumber>>()));
            }
            Field order_by_field;
            chunk.getColumns()[order_by_columns[0]]->get(row, order_by_field);

            auto & field_map = partition_to_map[hash_value];
            // if order by key is not find, we need to check this key should be put into the head, otherwise, just add to the list
            if (field_map.find(order_by_field) == partition_to_map[hash_value].end())
            {
                if (!reverse)
                    partition_to_heap[hash_value].push(order_by_field);
                else
                    partition_to_heap_reverse[hash_value].push(order_by_field);

                if ((!reverse && partition_to_heap[hash_value].size() > topN)
                    || (reverse && partition_to_heap_reverse[hash_value].size() > topN))
                {
                    Field remove_field;
                    if (!reverse)
                        remove_field = partition_to_heap[hash_value].top();
                    else
                        remove_field = partition_to_heap_reverse[hash_value].top();
                    if (!reverse)
                        partition_to_heap[hash_value].pop();
                    else
                        partition_to_heap_reverse[hash_value].pop();

                    // if the new order by key is the top one, just do thins. otherwise, we need to delete the top key and put new key to the map
                    if (order_by_field == remove_field)
                    {
                        // do nothing
                    }
                    else
                    {
                        auto remote_itr = field_map.find(remove_field);
                        if (remote_itr != field_map.end())
                            field_map.erase(remote_itr);
                        field_map.emplace(std::make_pair(order_by_field, std::vector<RowNumber>()));
                        field_map[order_by_field].emplace_back(RowNumber{chunk_list_size, row});
                        find_value = true;
                    }
                }
                else
                {
                    field_map.emplace(std::make_pair(order_by_field, std::vector<RowNumber>()));
                    field_map[order_by_field].emplace_back(RowNumber{chunk_list_size, row});
                    find_value = true;
                }
            }
            else
            {
                field_map[order_by_field].emplace_back(RowNumber{chunk_list_size, row});
                find_value = true;
            }
        }

        if (find_value)
        {
            chunk_list.emplace_back(std::move(chunk));
        }
    }

   if (receive_all_data)
   {
       // generate filter_map, <key, value> => <block_index, filter>
       std::unordered_map<size_t, IColumn::Filter> filter_map;
       for (auto iter = partition_to_map.begin(); iter != partition_to_map.end(); ++iter)
       {
           for (auto field_iter = iter->second.begin(); field_iter != iter->second.end(); ++field_iter)
           {
               for (auto & row_iter: field_iter->second)
               {
                   size_t block_index = row_iter.block;
                   if (filter_map.find(block_index) == filter_map.end())
                   {
                       filter_map.emplace(std::make_pair(block_index, IColumn::Filter(chunk_list[block_index].getNumRows(), 0)));
                   }
                   filter_map[block_index][row_iter.row] = 1;
               }
           }
       }

       // put all filtered block to output_chunk_list
       for (auto iter = filter_map.begin(); iter != filter_map.end(); ++iter)
       {
           Chunk & source_chunk = chunk_list[iter->first];
           size_t num_filtered_rows = countBytesInFilter(iter->second);
           auto source_chunks_columns = source_chunk.detachColumns();
           for (size_t i = 0; i < source_chunks_columns.size(); i++)
           {
               auto & current_column = source_chunks_columns[i];
               if (isColumnConst(*current_column))
                   current_column = current_column->cut(0, num_filtered_rows);
               else
                   current_column = current_column->filter(iter->second, num_filtered_rows);
           }
           Chunk filter_chunk;
           filter_chunk.setColumns(std::move(source_chunks_columns), num_filtered_rows);
           output_chunk_list.emplace_back(std::move(filter_chunk));
       }

       start_output_chunk = true;
       chunk_list.clear();
   }
}
}
