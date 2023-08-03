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

#include <Optimizer/Rule/Rewrite/PushAggThroughJoinRules.h>

#include <AggregateFunctions/AggregateFunctionFactory.h>
#include <Optimizer/DistinctOutputUtil.h>
#include <Optimizer/PredicateUtils.h>
#include <Optimizer/Rule/Patterns.h>
#include <Parsers/ASTIdentifier.h>
#include <Interpreters/join_common.h>
#include <QueryPlan/AggregatingStep.h>
#include <QueryPlan/JoinStep.h>
#include <QueryPlan/ProjectionStep.h>
#include <QueryPlan/ValuesStep.h>

namespace DB
{
static bool groupsOnAllColumns(const AggregatingStep * agg, const std::set<String> & columns)
{
    std::set<String> group_by_keys{agg->getKeys().begin(), agg->getKeys().end()};

    return columns == group_by_keys;
}

static PlanNodePtr getInnerTable(PlanNodeBase & join)
{
    if (dynamic_cast<const JoinStep *>(join.getStep().get())->getKind() == ASTTableJoin::Kind::Left)
    {
        return join.getChildren()[1];
    }
    return join.getChildren()[0];
}

static PlanNodePtr getOuterTable(PlanNodeBase & join)
{
    if (dynamic_cast<const JoinStep *>(join.getStep().get())->getKind() == ASTTableJoin::Kind::Left)
    {
        return join.getChildren()[0];
    }
    return join.getChildren()[1];
}

static bool isAggregationOnSymbols(PlanNodeBase & agg, PlanNodeBase & source)
{
    std::unordered_set<String> source_symbols;
    for (const auto & col : source.getStep()->getOutputStream().header)
    {
        source_symbols.insert(col.name);
    }

    bool all_match = true;

    for (const auto & aggregate : dynamic_cast<const AggregatingStep *>(agg.getStep().get())->getAggregates())
    {
        for (const auto & col : aggregate.argument_names)
        {
            all_match &= source_symbols.contains(col);
        }
    }

    return all_match;
}

static MappedAggregationInfo createAggregationOverNull(const AggregatingStep * reference_aggregation, Context & context)
{
    // Create a values node that consists of a single row of nulls.
    // Map the output symbols from the reference_aggregation's source
    // to symbol references for the new values node.
    Names null_symbols;
    Fields null_literals;
    std::unordered_map<String, String> sources_symbol_mapping_builder;
    NamesAndTypes header;
    for (const auto & col : reference_aggregation->getInputStreams()[0].header)
    {
        auto type = col.type;
        if (JoinCommon::canBecomeNullable(type))
        {
            type = JoinCommon::convertTypeToNullable(type);
        }
        null_literals.emplace_back(Field());
        auto null_symbol = context.getSymbolAllocator()->newSymbol("null");
        null_symbols.emplace_back(null_symbol);
        sources_symbol_mapping_builder[col.name] = null_symbol;
        header.emplace_back(null_symbol, type);
    }
    auto null_row_step = std::make_shared<ValuesStep>(header, null_literals);
    PlanNodePtr null_row = PlanNodeBase::createPlanNode(context.nextNodeId(), std::move(null_row_step));

    // For each aggregation function in the reference node, create a corresponding aggregation function
    // that points to the nullRow. Map the symbols from the aggregations in reference_aggregation to the
    // symbols in these new aggregations.
    auto mapper = [sources_symbol_mapping_builder](const String & symbol) {
        return sources_symbol_mapping_builder.contains(symbol) ? sources_symbol_mapping_builder.at(symbol) : symbol;
    };
    std::unordered_map<String, String> aggregations_symbol_mapping;
    AggregateDescriptions aggregations_over_null;
    for (const auto & agg_desc : reference_aggregation->getAggregates())
    {
        auto aggregation_symbol = agg_desc.column_name;
        AggregateDescription over_null_aggregation;
        over_null_aggregation.function = agg_desc.function;
        over_null_aggregation.parameters = agg_desc.parameters;
        over_null_aggregation.arguments = agg_desc.arguments;
        over_null_aggregation.mask_column = mapper(agg_desc.mask_column);
        for (const auto & arg : agg_desc.argument_names)
        {
            over_null_aggregation.argument_names.emplace_back(mapper(arg));
        }

        auto over_null_symbol = context.getSymbolAllocator()->newSymbol(over_null_aggregation.function->getName());
        over_null_aggregation.column_name = over_null_symbol;
        aggregations_over_null.emplace_back(over_null_aggregation);
        aggregations_symbol_mapping[aggregation_symbol] = over_null_symbol;
    }

    // create an aggregation node whose source is the null row.
    auto aggregation_over_null_row_step
        = std::make_shared<AggregatingStep>(null_row->getStep()->getOutputStream(), Names{}, aggregations_over_null, GroupingSetsParamsList{}, true, GroupingDescriptions{}, false, false);
    auto aggregation_over_null_row = PlanNodeBase::createPlanNode(context.nextNodeId(), std::move(aggregation_over_null_row_step), {null_row});

    return MappedAggregationInfo{.aggregation_node = std::move(aggregation_over_null_row), .symbolMapping = std::move(aggregations_symbol_mapping)};
}

// When the aggregation is done after the join, there will be a null value that gets aggregated over
// where rows did not exist in the inner table.  For some aggregate functions, such as count, the result
// of an aggregation over a single null row is one or zero rather than null. In order to ensure correct results,
// we add a coalesce function with the output of the new outer join and the aggregation performed over a single
// null row.
static PlanNodePtr coalesceWithNullAggregation(const AggregatingStep * aggregation_step, const PlanNodePtr & outerJoin, Context & context)
{
    // Create an aggregation node over a row of nulls.
    MappedAggregationInfo aggregation_over_null_info = createAggregationOverNull(aggregation_step, context);

    auto & aggregation_over_null = aggregation_over_null_info.aggregation_node;
    auto & source_aggregation_to_over_null_mapping = aggregation_over_null_info.symbolMapping;

    // Do a cross join with the aggregation over null
    Block names_and_types;

    for (const auto & item : outerJoin->getStep()->getOutputStream().header)
    {
        names_and_types.insert(ColumnWithTypeAndName{item.type, item.name});
    }
    for (const auto & item : aggregation_over_null->getStep()->getOutputStream().header)
    {
        names_and_types.insert(ColumnWithTypeAndName{item.type, item.name});
    }


    auto cross_join_step = std::make_shared<JoinStep>(
        DataStreams{outerJoin->getStep()->getOutputStream(), aggregation_over_null->getStep()->getOutputStream()},
        DataStream{names_and_types},
        ASTTableJoin::Kind::Cross,
        ASTTableJoin::Strictness::All,
        Names{},
        Names{});

    PlanNodePtr cross_join = PlanNodeBase::createPlanNode(context.nextNodeId(), std::move(cross_join_step), {outerJoin, aggregation_over_null});

    // Add coalesce expressions for all aggregation functions
    Assignments assignments_builder;

    NameSet agg_func_outputs;
    for (const auto & agg_func : aggregation_step->getAggregates())
    {
        agg_func_outputs.insert(agg_func.column_name);
    }
    NameToType name_to_type;
    for (const auto & symbol : outerJoin->getStep()->getOutputStream().header)
    {
        if (agg_func_outputs.contains(symbol.name))
        {
            ASTPtr coalesce = makeASTFunction(
                "coalesce",
                ASTs{
                    std::make_shared<ASTIdentifier>(symbol.name),
                    std::make_shared<ASTIdentifier>(source_aggregation_to_over_null_mapping[symbol.name])});
            assignments_builder.emplace_back(symbol.name, coalesce);
        }
        else
        {
            assignments_builder.emplace_back(symbol.name, std::make_shared<ASTIdentifier>(symbol.name));
        }
        name_to_type[symbol.name] = symbol.type;
    }

    auto projection_step = std::make_shared<ProjectionStep>(cross_join->getStep()->getOutputStream(), std::move(assignments_builder), std::move(name_to_type));
    return PlanNodeBase::createPlanNode(context.nextNodeId(), std::move(projection_step), {cross_join});
}


PatternPtr PushAggThroughOuterJoin::getPattern() const
{
    return Patterns::aggregating()->withSingle(Patterns::join()->matchingStep<JoinStep>([](const JoinStep & s) {
        return (s.getKind() == ASTTableJoin::Kind::Left || s.getKind() == ASTTableJoin::Kind::Right)
            && PredicateUtils::isTruePredicate(s.getFilter());
    }));
}

/**
 * This optimizer pushes aggregations below outer joins when: the aggregation
 * is on top of the outer join, it groups by all columns in the outer table, and
 * the outer rows are guaranteed to be distinct.
 *
 * When the aggregation is pushed down, we still need to perform aggregations
 * on the null values that come out of the absent values in an outer
 * join. We add a cross join with a row of aggregations on null literals,
 * and coalesce the aggregation that results from the left outer join with
 * the result of the aggregation over nulls.
 *
 * Example:
 * - Filter ("nationkey" > "avg")
 *  - Aggregate(Group by: all columns from the left table, aggregation:
 *    avg("n2.nationkey"))
 *      - LeftJoin("regionkey" = "regionkey")
 *          - AssignUniqueId (nation)
 *              - Tablescan (nation)
 *          - Tablescan (nation)
 *
 * Is rewritten to:
 * - Filter ("nationkey" > "avg")
 *  - project(regionkey, coalesce("avg", "avg_over_null")
 *      - CrossJoin
 *          - LeftJoin("regionkey" = "regionkey")
 *              - AssignUniqueId (nation)
 *                  - Tablescan (nation)
 *              - Aggregate(Group by: regionkey, aggregation:
 *                avg(nationkey))
 *                  - Tablescan (nation)
 *          - Aggregate
 *            avg(null_literal)
 *              - Values (null_literal)
 */
 // @jingpeng TODO: wrong answer
 // select a, groupArray(s) from (select distinct 1 as a) left join (select 2 as b, 'foo' as s) on a=b group by a SETTINGS join_use_nulls=0;
TransformResult PushAggThroughOuterJoin::transformImpl(PlanNodePtr aggregation, const Captures &, RuleContext & context)
{
    const auto * agg_step = dynamic_cast<const AggregatingStep *>(aggregation->getStep().get());

    auto join = aggregation->getChildren()[0];
    const auto * join_step = dynamic_cast<const JoinStep *>(join->getStep().get());
    auto outer_table = getOuterTable(*join);
    auto inner_table = getInnerTable(*join);

    std::set<String> outer_output_symbols;
    for (const auto & col : outer_table->getStep()->getOutputStream().header)
    {
        outer_output_symbols.insert(col.name);
    }

    if (!agg_step->isNormal() || !groupsOnAllColumns(agg_step, outer_output_symbols) || !isAggregationOnSymbols(*aggregation, *inner_table)
        || !DistinctOutputQueryUtil::isDistinct(*outer_table))
    {
        return {};
    }

    auto grouping_keys = join_step->getKind() == ASTTableJoin::Kind::Right ? join_step->getLeftKeys() : join_step->getRightKeys();

    auto rewritten_aggregation = std::make_shared<AggregatingStep>(
        inner_table->getStep()->getOutputStream(), grouping_keys, agg_step->getAggregates(), agg_step->getGroupingSetsParams(), agg_step->isFinal(),
        GroupingDescriptions{}, false,  !(agg_step->isFinal()) && context.context->getSettingsRef().distributed_aggregation_memory_efficient
        );
    auto rewritten_agg_node = PlanNodeBase::createPlanNode(context.context->nextNodeId(), std::move(rewritten_aggregation), {inner_table});

    PlanNodePtr rewritten_join;
    const auto & rewritten_agg_output = rewritten_agg_node->getStep()->getOutputStream();
    if (join_step->getKind() == ASTTableJoin::Kind::Left)
    {
        const auto & join_left_output = join->getChildren()[0]->getStep()->getOutputStream();
        auto output_header = join_left_output.header;
        for (const auto & item : rewritten_agg_output.header)
        {
            output_header.insert(item);
        }
        auto rewritten_join_step = std::make_shared<JoinStep>(
            DataStreams{join_left_output, rewritten_agg_output},
            DataStream{output_header},
            join_step->getKind(),
            join_step->getStrictness(),
            join_step->getLeftKeys(),
            join_step->getRightKeys(),
            join_step->getFilter(),
            join_step->isHasUsing(),
            join_step->getRequireRightKeys(),
            join_step->getAsofInequality(),
            join_step->getDistributionType(),
            join_step->isMagic());
        rewritten_join = PlanNodeBase::createPlanNode(
            context.context->nextNodeId(), std::move(rewritten_join_step), {join->getChildren()[0], rewritten_agg_node});
    }
    else
    {
        const auto & join_right_output = join->getChildren()[1]->getStep()->getOutputStream();
        auto output_header = rewritten_agg_output.header;
        for (const auto & item : join_right_output.header)
        {
            output_header.insert(item);
        }
        auto rewritten_join_step = std::make_shared<JoinStep>(
            DataStreams{rewritten_agg_output, join_right_output},
            DataStream{output_header},
            join_step->getKind(),
            join_step->getStrictness(),
            join_step->getLeftKeys(),
            join_step->getRightKeys(),
            join_step->getFilter(),
            join_step->isHasUsing(),
            join_step->getRequireRightKeys(),
            join_step->getAsofInequality(),
            join_step->getDistributionType(),
            join_step->isMagic());
        rewritten_join = PlanNodeBase::createPlanNode(
            context.context->nextNodeId(), std::move(rewritten_join_step), {rewritten_agg_node, join->getChildren()[1]});
    }

    // if agg(null) == null, then no need to add a cross join.
    bool add_null = false;
    for (const auto & agg : agg_step->getAggregates())
    {
        // case 1 : agg.function->handleNullItSelf() return true, then agg(null) != null
        // case 2 : count(null) != null
        AggregateFunctionProperties properties;
        AggregateFunctionFactory::instance().get(
            agg.function->getName(), agg.function->getArgumentTypes(), agg.function->getParameters(), properties);

        if (properties.returns_default_when_only_null)
        {
            add_null = true;
        }
    }
    if (add_null)
    {
        auto result_node = coalesceWithNullAggregation(
            dynamic_cast<const AggregatingStep *>(rewritten_agg_node->getStep().get()), rewritten_join, *context.context);
        if (!result_node)
        {
            return {};
        }
        return result_node;
    }
    else
    {
        if (!rewritten_join)
        {
            return {};
        }
        return rewritten_join;
    }
}

}
