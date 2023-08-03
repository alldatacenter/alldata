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

#include <Processors/printPipeline.h>
#include <QueryPlan/IQueryPlanStep.h>
#include <set>
#include <map>

namespace DB
{

void printPipelineCompact(const Processors & processors, WriteBuffer & out, bool with_header)
{
    struct Node;

    /// Group by processors name, QueryPlanStep and group in this step.
    struct Key
    {
        size_t group;
        IQueryPlanStep * step;
        std::string name;

        auto getTuple() const { return std::forward_as_tuple(group, step, name); }

        bool operator<(const Key & other) const
        {
            return getTuple() < other.getTuple();
        }
    };

    /// Group ports by header.
    struct EdgeData
    {
        Block header;
        size_t count;
    };

    using Edge = std::vector<EdgeData>;

    struct Node
    {
        size_t id = 0;
        std::map<Node *, Edge> edges = {};
        std::vector<const IProcessor *> agents = {};
    };

    std::map<Key, Node> graph;

    auto get_key = [](const IProcessor & processor)
    {
        return Key{processor.getQueryPlanStepGroup(), processor.getQueryPlanStep(), processor.getName()};
    };

    /// Fill nodes.
    for (const auto & processor : processors)
    {
        auto res = graph.emplace(get_key(*processor), Node());
        auto & node = res.first->second;
        node.agents.emplace_back(processor.get());

        if (res.second)
            node.id = graph.size();
    }

    Block empty_header;

    /// Fill edges.
    for (const auto & processor : processors)
    {
        auto & from =  graph[get_key(*processor)];

        for (auto & port : processor->getOutputs())
        {
            if (!port.isConnected())
                continue;

            auto & to = graph[get_key(port.getInputPort().getProcessor())];
            auto & edge = from.edges[&to];

            /// Use empty header for each edge if with_header is false.
            const auto & header = with_header ? port.getHeader()
                                              : empty_header;

            /// Group by header.
            bool found = false;
            for (auto & item : edge)
            {
                if (blocksHaveEqualStructure(header, item.header))
                {
                    found = true;
                    ++item.count;
                    break;
                }
            }

            if (!found)
                edge.emplace_back(EdgeData{header, 1});
        }
    }

    /// Group processors by it's QueryPlanStep.
    std::map<IQueryPlanStep *, std::vector<const Node *>> steps_map;

    for (const auto & item : graph)
        steps_map[item.first.step].emplace_back(&item.second);

    out << "digraph\n{\n";
    out << "  rankdir=\"LR\";\n";
    out << "  { node [shape = rect]\n";

    /// Nodes // TODO quoting and escaping
    size_t next_step = 0;
    for (const auto & item : steps_map)
    {
        /// Use separate clusters for each step.
        if (item.first != nullptr)
        {
            out << "    subgraph cluster_" << next_step << " {\n";
            out << "      label =\"" << item.first->getName() << "\";\n";
            out << "      style=filled;\n";
            out << "      color=lightgrey;\n";
            out << "      node [style=filled,color=white];\n";
            out << "      { rank = same;\n";

            ++next_step;
        }

        for (const auto & node : item.second)
        {
            const auto & processor = node->agents.front();
            out << "        n" << node->id << " [label=\"" << processor->getName();

            if (node->agents.size() > 1)
                out << " × " << node->agents.size();

            const auto & description = processor->getDescription();
            if (!description.empty())
                out << ' ' << description;

            out << "\"];\n";
        }

        if (item.first != nullptr)
        {
            out << "      }\n";
            out << "    }\n";
        }
    }

    out << "  }\n";

    /// Edges
    for (const auto & item : graph)
    {
        for (const auto & edge : item.second.edges)
        {
            for (const auto & data : edge.second)
            {
                out << "  n" << item.second.id << " -> " << "n" << edge.first->id << " [label=\"";

                if (data.count > 1)
                    out << "× " << data.count;

                if (with_header)
                {
                    for (const auto & elem : data.header)
                    {
                        out << "\n";
                        elem.dumpStructure(out);
                    }
                }

                out << "\"];\n";
            }
        }
    }
    out << "}\n";
}

}
