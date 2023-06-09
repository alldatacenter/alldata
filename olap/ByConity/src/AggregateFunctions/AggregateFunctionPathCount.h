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

#include <pdqsort.h>
#include <AggregateFunctions/IAggregateFunction.h>
#include <Columns/ColumnArray.h>
#include <Columns/ColumnsNumber.h>
#include <Columns/ColumnTuple.h>
#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypeTuple.h>
#include <DataTypes/DataTypesNumber.h>
#include <Common/HashTable/HashMap.h>
#include <Common/SpaceSaving.h>

#include <IO/ReadHelpers.h>
#include <IO/WriteHelpers.h>

namespace DB
{

struct AggregateFunctionPathCountData
{
    using Node = UInt64;
    static const Node LOSS_NODE = std::numeric_limits<UInt16>::max();
    static const Node OTHER_NODE = std::numeric_limits<UInt16>::max() - 1;
    using Edge = PairInt64;   // low:from,high:to
    using Count = PairInt64;
    using NodeMap = HashMap<StringRef, Node, StringRefHash>;
    using NodeCount = HashMap<Node, Count>;
    using EdgeCount = HashMap<Edge, Count>;
    using NodeSet = SpaceSaving<Node, HashCRC32<Node>>;
    using EdgeSet = SpaceSaving<Edge>;
    struct Step
    {
        NodeCount node_counts;
        EdgeCount edge_counts;
    };
    NodeMap nodes;
    Step steps[1];


    size_t addNode(StringRef s, Arena * arena = nullptr)
    {
        typename NodeMap::LookupResult it;
        bool inserted;
        nodes.emplace(s, it, inserted);
        // TODO limit insert
        if (inserted)
        {
            it->getMapped() = Node(nodes.size() - 1);
        }
        else if (arena)
        {
            arena->rollback(s.size);
        }
        return it->getMapped();
    }

    void addNodeCount(size_t level, Node node, Count count)
    {
        typename NodeCount::LookupResult it;
        bool inserted;
        steps[level].node_counts.emplace(node, it, inserted);
        if (inserted)
        {
            it->getMapped() = count;
        }
        else
        {
            it->getMapped().low += count.low;
            it->getMapped().high += count.high;
        }
    }

    void addEdgeCount(size_t level, Edge edge, Count count)
    {
        addEdgeCountImpl(steps[level].edge_counts, edge, count);
    }

    static void addEdgeCountImpl(EdgeCount& edge_counts, Edge edge, Count count)
    {
        typename EdgeCount::LookupResult it;
        bool inserted;
        edge_counts.emplace(edge, it, inserted);
        if (inserted)
        {
            it->getMapped() = count;
        }
        else
        {
            it->getMapped().low += count.low;
            it->getMapped().high += count.high;
        }
    }

    void merge(const AggregateFunctionPathCountData & other, Arena * arena, size_t max_step)
    {
        std::unordered_map<Node, Node> mapping;
        for (const auto & node : other.nodes)
        {
            auto s = StringRef(arena->insert(node.getKey().data, node.getKey().size), node.getKey().size);
            Node old_node = node.getMapped(), new_node = addNode(s, arena);
            mapping[old_node] = new_node;
        }
        for (size_t level = 0; level < max_step; ++level)
        {
            for (const auto & node_count : other.steps[level].node_counts)
                addNodeCount(level, mapping[node_count.getKey()], node_count.getMapped());

            for (const auto & edge_count : other.steps[level].edge_counts)
            {
                Edge edge = edge_count.getKey();
                edge.low = mapping[edge.low];
                edge.high = mapping[edge.high];
                addEdgeCount(level, edge, edge_count.getMapped());
            }
        }
    }

    void serialize(WriteBuffer & buf, size_t max_step) const
    {
        nodes.write(buf);
        for (size_t level = 0; level < max_step; ++level)
        {
            steps[level].node_counts.write(buf);
            steps[level].edge_counts.write(buf);
        }
    }

    void deserialize(ReadBuffer & buf, Arena * arena, size_t max_step)
    {
        // nodes.read(buf);
        {
            // NodeMap::cell_type::State::read(buf); //do nothing

            nodes.clearAndShrink();

            size_t new_size = 0;
            DB::readVarUInt(new_size, buf);
            if (new_size != 0)
                nodes.resize(new_size);

            for (size_t i = 0; i < new_size; ++i)
            {
                StringRef k = readStringBinaryInto(*arena, buf);
                Node v;
                DB::readBinary(v, buf);
                nodes.insert({k, v});
            }
        }
        for (size_t level = 0; level < max_step; ++level)
        {
            steps[level].node_counts.read(buf);
            steps[level].edge_counts.read(buf);
        }
    }
};

const AggregateFunctionPathCountData::Node AggregateFunctionPathCountData::LOSS_NODE;
const AggregateFunctionPathCountData::Node AggregateFunctionPathCountData::OTHER_NODE;

template <typename EventIndex, bool with_user_count = true>
class AggregateFunctionPathCount final
    : public IAggregateFunctionDataHelper<AggregateFunctionPathCountData, AggregateFunctionPathCount<EventIndex, with_user_count>>
{
private:
    UInt64 max_node_size;
    UInt64 max_step_size;

    using Data = AggregateFunctionPathCountData;

public:
    AggregateFunctionPathCount(UInt64 max_node_size_, UInt64 max_step_size_, const DataTypes & arguments, const Array & params)
        : IAggregateFunctionDataHelper<AggregateFunctionPathCountData, AggregateFunctionPathCount<EventIndex, with_user_count>>(
            arguments, params)
        , max_node_size(max_node_size_)
        , max_step_size(max_step_size_)
    {
    }

    String getName() const override { return "pathCount"; }

    void create(const AggregateDataPtr __restrict place) const override
    {
        new (place) Data::NodeMap;
        new (place + sizeof(Data::NodeMap)) Data::Step[max_step_size];
    }

    void destroy(AggregateDataPtr __restrict place) const noexcept override
    {
        this->data(place).nodes.clearAndShrink();
        for (size_t level = 0; level < max_step_size; ++level)
        {
            this->data(place).steps[level].node_counts.clearAndShrink();
            this->data(place).steps[level].edge_counts.clearAndShrink();
        }
    }

    size_t sizeOfData() const override { return sizeof(Data::NodeMap) + sizeof(Data::Step) * max_step_size; }

    //([(event_index, param)...], [(level,event,param, countDistinct ,count)...], [(level,event, countDistinct, count)])
    DataTypePtr getReturnType() const override
    {
        DataTypes types;
        types.emplace_back(std::make_shared<DataTypeNumber<EventIndex>>()); // event index
        types.emplace_back(std::make_shared<DataTypeString>()); // param
        DataTypePtr node_index = std::make_shared<DataTypeArray>(std::make_shared<DataTypeTuple>(types));
        DataTypePtr node = std::make_shared<DataTypeArray>(std::make_shared<DataTypeArray>(std::make_shared<DataTypeNumber<UInt64>>()));
        DataTypePtr edge = std::make_shared<DataTypeArray>(std::make_shared<DataTypeArray>(std::make_shared<DataTypeNumber<UInt64>>()));
        types.clear();
        types.emplace_back(node_index);
        types.emplace_back(node);
        types.emplace_back(edge);
        return std::make_shared<DataTypeTuple>(types);
    }

    void add(AggregateDataPtr place, const IColumn ** columns, size_t row_num, Arena * arena) const override
    {
        if constexpr (with_user_count)
            addImplWithUserCount(place, columns, row_num, arena);
        else
            addImplWithoutUserCount(place, columns, row_num, arena);
    }

    //[[()...]]
    void addImplWithUserCount(AggregateDataPtr place, const IColumn ** columns, size_t row_num, Arena * arena) const
    {
        const auto & arr_col = static_cast<const ColumnArray &>(*columns[0]);
        const auto & arr_data = static_cast<const ColumnArray &>(arr_col.getData()); //Array
        const auto & arr_data_data = arr_data.getData();    // Tuple

        auto & data = this->data(place);

        size_t offset = arr_col.offsetAt(row_num);
        size_t size = arr_col.sizeAt(row_num);

        auto * nodes = new std::vector<Data::Node>[size];
        auto * node_sets = new std::set<Data::Node>[max_step_size];
        auto * edge_sets = new std::set<Data::Edge>[max_step_size];
        for (size_t i = 0; i < size; ++i)
        {
            size_t node_offset = arr_data.offsetAt(offset + i);
            size_t node_size = std::min<size_t>(arr_data.sizeAt(offset + i), max_step_size);

            nodes[i].reserve(node_size);
            for (size_t j = 0; j < node_size; ++j)
            {
                const char * begin = nullptr;
                StringRef value = arr_data_data.serializeValueIntoArena(node_offset + j, *arena, begin);
                auto node = data.addNode(value, arena);
                nodes[i].emplace_back(node);
                data.addNodeCount(j, node, Data::Count(node_sets[j].insert(node).second, 1));
            }

            if (node_size != max_step_size)
                data.addNodeCount(node_size, Data::LOSS_NODE, Data::Count(node_sets[node_size].insert(Data::LOSS_NODE).second, 1));
        }

        for (size_t i = 0; i < size; ++i)
        {
            size_t node_size = std::min<size_t>(arr_data.sizeAt(offset + i), max_step_size);
            if (unlikely(!node_size))
                continue;

            for (size_t j = 0, edge_size = node_size - 1; j < edge_size; ++j)
            {
                Data::Edge edge(nodes[i][j], nodes[i][j + 1]);
                data.addEdgeCount(j, edge, Data::Count(edge_sets[j].insert(edge).second, 1));
            }

            if (node_size != max_step_size)
            {
                Data::Edge edge(nodes[i].back(), Data::LOSS_NODE);
                data.addEdgeCount(node_size - 1, edge, Data::Count(edge_sets[node_size - 1].insert(edge).second, 1));
            }
        }

        delete[] nodes;
        delete[] node_sets;
        delete[] edge_sets;
    }

    //([()...], count1, count2)
    void addImplWithoutUserCount(AggregateDataPtr place, const IColumn ** columns, size_t row_num, Arena * arena) const
    {
        const auto & arr_col = static_cast<const ColumnArray &>(*columns[0]);
        const auto & arr_data = arr_col.getData();

        auto & data = this->data(place);

        // // path node
        size_t offset = arr_col.offsetAt(row_num);
        size_t size = std::min<size_t>(max_step_size, arr_col.sizeAt(row_num));

        if (unlikely(!size))
            return;

        Data::Count count(columns[1]->getUInt(row_num), columns[2]->getUInt(row_num));

        std::vector<Data::Node> nodes;
        nodes.reserve(size);
        for (size_t i = 0; i < size; ++i)
        {
            const char * begin = nullptr;
            StringRef value = arr_data.serializeValueIntoArena(offset + i, *arena, begin);
            nodes.emplace_back(data.addNode(value, arena));
        }

        for (size_t i = 0; i < size - 1; ++i)
        {
            data.addNodeCount(i, nodes[i], count);
            Data::Edge edge(nodes[i], nodes[i + 1]);
            data.addEdgeCount(i, edge, count);
        }
        //last node
        data.addNodeCount(size - 1, nodes.back(), count);
        if (size != max_step_size)
        {
            Data::Edge edge(nodes.back(), Data::LOSS_NODE);
            data.addEdgeCount(size - 1, edge, count);
        }
    }

    void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, Arena * arena) const override
    {
        this->data(place).merge(this->data(rhs), arena, max_step_size);
    }

    void serialize(ConstAggregateDataPtr place, WriteBuffer & buf) const override
    {
        this->data(place).serialize(buf, max_step_size);
    }

    void deserialize(AggregateDataPtr place, ReadBuffer & buf, Arena * arena) const override
    {
        this->data(place).deserialize(buf, arena, max_step_size);
    }

    void insertResultInto(AggregateDataPtr __restrict place, IColumn & to, Arena *) const override
    {
        auto & tuple_col = static_cast<ColumnTuple &>(to);
        auto & node_index_col = static_cast<ColumnArray &>(tuple_col.getColumn(0));
        auto & node_index_data = static_cast<ColumnTuple &>(node_index_col.getData());
        auto & node_index_offsets = node_index_col.getOffsets();

        auto & node_col = static_cast<ColumnArray &>(tuple_col.getColumn(1));
        auto & node_data = static_cast<ColumnArray &>(node_col.getData());
        auto & node_offsets = node_col.getOffsets();
        auto & node_data_data = static_cast<ColumnUInt64 &>(node_data.getData());
        auto & node_data_offsets = node_data.getOffsets();

        auto & edge_col = static_cast<ColumnArray &>(tuple_col.getColumn(2));
        auto & edge_data = static_cast<ColumnArray &>(edge_col.getData());
        auto & edge_offsets = edge_col.getOffsets();
        auto & edge_data_data = static_cast<ColumnUInt64 &> (edge_data.getData());
        auto & edge_data_offsets = edge_data.getOffsets();

        auto & nodes = this->data(place).nodes;
        auto * steps = this->data(place).steps;

        // insert node index
        StringRef * node_refs = new StringRef[nodes.size()];
        for (const auto & node : nodes)
            node_refs[node.getMapped()] = node.getKey();

        for (size_t i = 0, size = nodes.size(); i < size; ++i)
            node_index_data.deserializeAndInsertFromArena(node_refs[i].data);

        node_index_offsets.push_back(nodes.size() + (!node_index_offsets.empty() ? node_index_offsets.back() : 0));

        //prue
        auto * node_sets = new std::set<Data::Node>[max_step_size];
        for (size_t i = 0; i < max_step_size; ++i)
        {
            Data::NodeSet topk_set;
            for (auto & node_count : steps[i].node_counts)
            {
                if (node_count.getKey() == Data::LOSS_NODE)
                    continue;

                topk_set.insert(node_count.getKey(), node_count.getMapped().high); // TODO check count
            }

            for (auto & node_count : topk_set.topK(max_node_size))
                node_sets[i].insert(node_count.key);

            node_sets[i].insert(Data::LOSS_NODE);
        }

        //insert node
        size_t node_num = 0;
        for (size_t i = 0; i < max_step_size; ++i)
        {
            Data::Count other_node_count(0, 0);
            size_t offset = !node_data_offsets.empty() ? node_data_offsets.back() : 0;
            for (auto & node_count : steps[i].node_counts)
            {
                auto node = node_count.getKey();
                if (node_sets[i].count(node))
                {
                    node_data_data.insertValue(i);
                    node_data_data.insertValue(node);
                    node_data_data.insertValue(node_count.getMapped().low);
                    node_data_data.insertValue(node_count.getMapped().high);
                    offset += 4;
                    node_data_offsets.push_back(offset);
                    node_num += 1;
                }
                else
                {
                    other_node_count.low += node_count.getMapped().low;
                    other_node_count.high += node_count.getMapped().high;
                }
            }
            if (other_node_count.high)
            {
                node_data_data.insertValue(i);
                node_data_data.insertValue(Data::OTHER_NODE);
                node_data_data.insertValue(other_node_count.low); // inaccurate
                node_data_data.insertValue(other_node_count.high);
                offset += 4;
                node_data_offsets.push_back(offset);
                node_num += 1;
            }
        }
        node_offsets.push_back(node_num + (!node_offsets.empty() ? node_offsets.back() : 0));

        //insert edge
        size_t edge_num = 0;
        for (size_t i = 0; i < max_step_size; ++i)
        {
            Data::EdgeCount tmp;
            for (auto & edge_count : steps[i].edge_counts)
            {
                auto edge = edge_count.getKey();

                if (!node_sets[i].count(edge.low))
                    edge.low = Data::OTHER_NODE;

                if (!node_sets[i + 1].count(edge.high))
                    edge.high = Data::OTHER_NODE;

                Data::addEdgeCountImpl(tmp, edge, edge_count.getMapped());
            }

            size_t offset = !edge_data_offsets.empty() ? edge_data_offsets.back() : 0;
            for (auto & edge_count : tmp)
            {
                edge_data_data.insertValue(i);
                edge_data_data.insertValue(edge_count.getKey().low);
                edge_data_data.insertValue(edge_count.getKey().high);
                edge_data_data.insertValue(edge_count.getMapped().low);
                edge_data_data.insertValue(edge_count.getMapped().high);
                offset += 5;
                edge_data_offsets.push_back(offset);
            }
            edge_num += tmp.size();
        }
        edge_offsets.push_back(edge_num + (!edge_offsets.empty() ? edge_offsets.back() : 0));

        delete[] node_refs;
        delete[] node_sets;
    }

    bool allocatesMemoryInArena() const override { return true; }

    bool handleNullItSelf() const override { return true; }

};
}
