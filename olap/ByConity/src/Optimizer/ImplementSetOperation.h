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

#include <DataTypes/DataTypesNumber.h>
#include <Interpreters/Context.h>
#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTLiteral.h>
#include <QueryPlan/PlanNode.h>
#include <QueryPlan/ProjectionStep.h>

namespace DB
{
struct TranslationResult
{
    PlanNodePtr plan_node;
    Names count_symbols;
};

class SetOperationNodeTranslator
{
public:
    explicit SetOperationNodeTranslator(Context & context_) : context(context_) { }

    TranslationResult makeSetContainmentPlanForDistinct(PlanNodeBase & node);

private:
    Names allocateSymbols(size_t count, const String & name)
    {
        Names symbols;
        for (size_t i = 0; i < count; i++)
        {
            symbols.emplace_back(context.getSymbolAllocator()->newSymbol(name));
        }
        return symbols;
    }

    PlanNodes appendMarkers(Names & markers, PlanNodes & nodes, const PlanNodePtr & node)
    {
        PlanNodes result;
        for (size_t i = 0; i < nodes.size(); i++)
        {
            result.emplace_back(appendMarkers(context, nodes[i], i, markers, sourceSymbolMapping(node, i)));
        }
        return result;
    }

    /// output symbol to child symbol mapping
    static Names sourceSymbolMapping(const PlanNodePtr & node, size_t child_index)
    {
        std::vector<String> result;
        const auto & child_header = node->getChildren()[child_index]->getStep()->getOutputStream().header;

        const auto & node_header = node->getStep()->getOutputStream().header;
        node_header.getNames();
        for (size_t index = 0; index < node_header.columns(); ++index)
        {
            result.emplace_back(child_header.getByPosition(index).name);
        }

        return result;
    }

    static PlanNodePtr
    appendMarkers(Context & context, const PlanNodePtr & source, size_t markerIndex, Names markers, const Names & projections)
    {
        Assignments assignments;
        const auto & output_stream = source->getStep()->getOutputStream();
        NameToType name_to_type;

        for (const auto & item : output_stream.header)
        {
            name_to_type[item.name] = item.type;
        }

        NameToType new_name_to_type;
        // add existing intersect symbols to projection
        for (const auto & item : projections)
        {
            assignments.emplace_back(item, std::make_shared<ASTIdentifier>(item));
            new_name_to_type[item] = name_to_type[item];
        }

        /// add extra marker fields to the projection
        for (size_t i = 0; i < markers.size(); ++i)
        {
            auto expression = (i == markerIndex) ? std::make_shared<ASTLiteral>(1u) : std::make_shared<ASTLiteral>(0u);
            assignments.emplace_back(markers[i], expression);
            new_name_to_type[markers[i]] = std::make_shared<DataTypeUInt8>();
        }

        auto expression_step = std::make_shared<ProjectionStep>(output_stream, assignments, new_name_to_type);
        PlanNodes children{source};
        PlanNodePtr expr_node = std::make_shared<ProjectionNode>(context.nextNodeId(), std::move(expression_step), children);
        return expr_node;
    }

    PlanNodePtr unionNodes(const PlanNodes & children, NamesAndTypes cols);

    static String MARKER;
    Context & context;
};


}
