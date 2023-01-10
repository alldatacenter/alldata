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

#pragma once

#include <Processors/IProcessor.h>
#include <IO/Operators.h>

namespace DB
{

/** Print pipeline in "dot" format for GraphViz.
  * You can render it with:
  *  dot -T png < pipeline.dot > pipeline.png
  */

template <typename Processors, typename Statuses>
void printPipeline(const Processors & processors, const Statuses & statuses, WriteBuffer & out, const String & direction = "LR")
{
    out << "digraph\n{\n";
    out << "  rankdir=\"" << direction << "\";\n";
    out << "  { node [shape = record]\n";

    auto get_proc_id = [](const IProcessor & proc) -> UInt64
    {
        return reinterpret_cast<std::uintptr_t>(&proc);
    };

    auto statuses_iter = statuses.begin();

    /// Nodes // TODO quoting and escaping
    for (const auto & processor : processors)
    {
        out << "    n" << get_proc_id(*processor) << "[label=\"{" << processor->getName() << processor->getDescription();

        if (statuses_iter != statuses.end())
        {
            out << " (" << IProcessor::statusToName(*statuses_iter) << ")";
            ++statuses_iter;
        }

        out << "}\"];\n";
    }

    out << "  }\n";

    /// Edges
    for (const auto & processor : processors)
    {
        for (const auto & port : processor->getOutputs())
        {
            if (!port.isConnected())
                continue;

            const IProcessor & curr = *processor;
            const IProcessor & next = port.getInputPort().getProcessor();

            out << "  n" << get_proc_id(curr) << " -> n" << get_proc_id(next) << ";\n";
        }
    }
    out << "}\n";
}

template <typename Processors>
void printPipeline(const Processors & processors, WriteBuffer & out)
{
    printPipeline(processors, std::vector<IProcessor::Status>(), out);
}

/// Prints pipeline in compact representation.
/// Group processors by it's name, QueryPlanStep and QueryPlanStepGroup.
/// If QueryPlanStep wasn't set for processor, representation may be not correct.
/// If with_header is set, prints block header for each edge.
void printPipelineCompact(const Processors & processors, WriteBuffer & out, bool with_header);

}
