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

#include <QueryPlan/QueryPlanner.h>

#include <Analyzers/analyze_common.h>
#include <Analyzers/ExpressionVisitor.h>
#include <QueryPlan/planning_common.h>
#include <QueryPlan/PlanBuilder.h>
#include <Interpreters/AggregateDescription.h>
#include <Interpreters/WindowDescription.h>
#include <Optimizer/PredicateUtils.h>
#include <Optimizer/makeCastFunction.h>
#include <Optimizer/Utils.h>
#include <QueryPlan/AggregatingStep.h>
#include <QueryPlan/ApplyStep.h>
#include <QueryPlan/DistinctStep.h>
#include <QueryPlan/EnforceSingleRowStep.h>
#include <QueryPlan/ExceptStep.h>
#include <QueryPlan/ExtremesStep.h>
#include <QueryPlan/FilterStep.h>
#include <QueryPlan/GraphvizPrinter.h>
#include <QueryPlan/IntersectStep.h>
#include <QueryPlan/JoinStep.h>
#include <QueryPlan/LimitByStep.h>
#include <QueryPlan/LimitStep.h>
#include <QueryPlan/SortingStep.h>
#include <QueryPlan/MergingSortedStep.h>
#include <QueryPlan/MergeSortingStep.h>
#include <QueryPlan/PartialSortingStep.h>
#include <QueryPlan/ProjectionStep.h>
#include <QueryPlan/UnionStep.h>
#include <QueryPlan/TableScanStep.h>
#include <QueryPlan/WindowStep.h>
#include <Common/FieldVisitors.h>
#include <Columns/Collator.h>
#include <Interpreters/getTableExpressions.h>

#include <unordered_set>
#include <algorithm>

namespace DB
{

namespace ErrorCodes
{
    extern const int PARAMETER_OUT_OF_BOUND;
    extern const int PLAN_BUILD_ERROR;
    extern const int EXPECTED_ALL_OR_ANY;
    extern const int INVALID_LIMIT_EXPRESSION;
    extern const int BAD_ARGUMENTS;
}

static thread_local UInt32 step_id;

#define PRINT_PLAN(plan, NAME) \
    do {                          \
        GraphvizPrinter::printLogicalPlan(*(plan), context, std::to_string(step_id++) + "_" + #NAME); \
    } while (false) \

struct PlanWithSymbolMappings
{
    PlanNodePtr plan;
    NameToNameMap mappings;
};

using ExpressionsAndTypes = std::vector<std::pair<ASTPtr, DataTypePtr>>;

struct PlanWithSymbols
{
    PlanNodePtr plan;
    Names symbols;
};

class QueryPlannerVisitor : public ASTVisitor<RelationPlan, const Void>
{
public:
    QueryPlannerVisitor(ContextMutablePtr context_, CTERelationPlans & cte_plans_, Analysis & analysis_, TranslationMapPtr outer_context_)
        : context(std::move(context_))
        , cte_plans(cte_plans_)
        , analysis(analysis_)
        , outer_context(std::move(outer_context_))
        , use_ansi_semantic(context->getSettingsRef().dialect_type == DialectType::ANSI)
        , enable_shared_cte(context->getSettingsRef().cte_mode != CTEMode::INLINED)
        , enable_implicit_type_conversion(context->getSettingsRef().enable_implicit_type_conversion)
    {
    }

    RelationPlan visitASTSelectIntersectExceptQuery(ASTPtr & node, const Void &) override;
    RelationPlan visitASTSelectWithUnionQuery(ASTPtr & node, const Void &) override;
    RelationPlan visitASTSelectQuery(ASTPtr & node, const Void &) override;
    RelationPlan visitASTSubquery(ASTPtr & node, const Void &) override;

    RelationPlan process(ASTPtr & node) { return ASTVisitorUtil::accept(node, *this, {}); }

private:
    ContextMutablePtr context;
    CTERelationPlans & cte_plans;
    Analysis & analysis;
    TranslationMapPtr outer_context;
    const bool use_ansi_semantic;
    const bool enable_shared_cte;
    const bool enable_implicit_type_conversion;

    /// plan FROM
    PlanBuilder planFrom(ASTSelectQuery &);
    PlanBuilder planWithoutTables(ASTSelectQuery & select_query);
    PlanBuilder planTables(ASTTablesInSelectQuery & tables_in_select, ASTSelectQuery & select_query);
    PlanBuilder planTableExpression(ASTTableExpression & table_expression, ASTSelectQuery & select_query);
    PlanBuilder planTable(ASTTableIdentifier & db_and_table, ASTSelectQuery & select_query);
    PlanBuilder planTableFunction(ASTFunction & table_function, ASTSelectQuery & select_query);
    PlanBuilder planTableSubquery(ASTSubquery & subquery, ASTPtr & node);

    /// plan join
    /// 1. join node will be planned in the left builder, so no return value is needed
    /// 2. join prepare methods are used to retrieve join keys, and do necessary projection for join keys
    void planJoin(ASTTableJoin & table_join, PlanBuilder & left_builder, PlanBuilder & right_builder);
    void planCrossJoin(ASTTableJoin & table_join, PlanBuilder & left_builder, PlanBuilder & right_builder);
    void planJoinUsing(ASTTableJoin & table_join, PlanBuilder & left_builder, PlanBuilder & right_builder);
    void planJoinOn(ASTTableJoin & table_join, PlanBuilder & left_builder, PlanBuilder & right_builder);
    std::pair<Names, Names> prepareJoinUsingKeys(ASTTableJoin & table_join, PlanBuilder & left_builder, PlanBuilder & right_builder);
    std::pair<Names, Names> prepareJoinOnKeys(ASTTableJoin & table_join, PlanBuilder & left_builder, PlanBuilder & right_builder);
    static DataStream getJoinOutputStream(ASTTableJoin & table_join, PlanBuilder & left_builder, PlanBuilder & right_builder);

    RelationPlan planReadFromStorage(const IAST & table_ast, ScopePtr table_scope, ASTSelectQuery & origin_query);
    // static MergeTreeReadPlannerPtr getReadPlanner(ASTSelectQuery & select_query);
    // static MergeTreeBitMapSchedulerPtr getBitMapScheduler(ASTSelectQuery & select_query, const StoragePtr & storage, size_t max_streams);

    void planFilter(PlanBuilder & builder, ASTSelectQuery & select_query, const ASTPtr & filter);
    void planAggregate(PlanBuilder & builder, ASTSelectQuery & select_query);
    void planWindow(PlanBuilder & builder, ASTSelectQuery & select_query);
    void planSelect(PlanBuilder & builder, ASTSelectQuery & select_query);
    void planDistinct(PlanBuilder & builder, ASTSelectQuery & select_query);
    void planOrderBy(PlanBuilder & builder, ASTSelectQuery & select_query);
    void planLimitBy(PlanBuilder & builder, ASTSelectQuery & select_query);
    void planLimit(PlanBuilder & builder, ASTSelectQuery & select_query);
    // void planSampling(PlanBuilder & builder, ASTSelectQuery & select_query);
    RelationPlan planFinalSelect(PlanBuilder & builder, ASTSelectQuery & select_query);

    /// plan subquery expressions, only subquery expressions under parent_expression will be planned
    void planSubqueryExpression(PlanBuilder & builder, ASTSelectQuery & select_query, ASTs & parent_expressions)
    {
        for (auto & expr : parent_expressions)
            planSubqueryExpression(builder, select_query, expr);
    }

    void planSubqueryExpression(PlanBuilder & builder, ASTSelectQuery & select_query, ASTPtr root);
    void planScalarSubquery(PlanBuilder & builder, const ASTPtr & scalar_subquery);
    void planInSubquery(PlanBuilder & builder, const ASTPtr & node);
    void planExistsSubquery(PlanBuilder & builder, const ASTPtr & node);
    void planQuantifiedComparisonSubquery(PlanBuilder & builder, const ASTPtr & node);
    RelationPlan combineSubqueryOutputsToTuple(const RelationPlan & plan, const ASTPtr & subquery);

    /// plan UNION/INTERSECT/EXCEPT
    RelationPlan projectFieldSymbols(const RelationPlan & plan);
    RelationPlan planSetOperation(ASTs & selects, ASTSelectWithUnionQuery::Mode union_mode);

    /// type coercion
    // coerce types for a set of symbols
    PlanWithSymbolMappings coerceTypesForSymbols(const PlanNodePtr & node, const NameToType & symbol_and_types, bool replace_symbol);
    NameToNameMap coerceTypesForSymbols(PlanBuilder & builder, const NameToType & symbol_and_types, bool replace_symbol);
    // coerce types for the first output column of a subquery plan
    RelationPlan coerceTypeForSubquery(const RelationPlan & plan, const DataTypePtr & type);
    // project expressions and cast their types
    PlanWithSymbols projectExpressionsWithCoercion(const PlanNodePtr & node, const TranslationMapPtr & translation,
                                                   const ExpressionsAndTypes & expression_and_types);
    Names projectExpressionsWithCoercion(PlanBuilder & builder, const ExpressionsAndTypes & expression_and_types);

    /// utils
    SizeLimits extractDistinctSizeLimits();
    std::pair<UInt64, UInt64> getLimitLengthAndOffset(ASTSelectQuery & query);
    PlanBuilder toPlanBuilder(const RelationPlan & plan, ScopePtr scope);

    void processSubqueryArgs(PlanBuilder & builder, ASTs & children, String & rhs_symbol, String & lhs_symbol, RelationPlan & rhs_plan);
};

namespace
{
PlanNodePtr planOutput(const RelationPlan & plan, ASTPtr & query, Analysis & analysis, ContextMutablePtr context)
{
    const auto & output_desc = analysis.getOutputDescription(*query);
    const auto & field_symbol_infos = plan.getFieldSymbolInfos();
    const auto old_root = plan.getRoot();

    Assignments assignments;
    NameToType input_types = old_root->getOutputNamesToTypes();
    NameToType output_types;
    NameSet output_names;
    std::unordered_map<String, UInt64> output_name_counter;

    auto get_uniq_output_name = [&](const auto & output_name)
    {
        String uniq_name = output_name;
        while(true) {
            auto cur_id = output_name_counter[output_name]++;
            // when current_id == 0, hide it
            uniq_name = cur_id == 0 ? output_name: fmt::format("{}_{}", output_name, cur_id);
            // it is possible to have conflicts so do check
            // merely redo is ok to avoid O(N^2) total complexity
            // since conflicting names won't be retried with the same cur_id
            // with the help of output_name_counter
            if (!output_names.count(uniq_name))
            {
                output_names.insert(uniq_name);
                return uniq_name;
            }
        }
    };

    assert(output_desc.size() == field_symbol_infos.size());

    for (size_t i = 0; i < output_desc.size(); ++i)
    {
        String input_column = field_symbol_infos[i].getPrimarySymbol();
        String output_name = get_uniq_output_name(output_desc[i].name);
        assignments.emplace_back(output_name, toSymbolRef(input_column));
        output_types[output_name] = input_types[input_column];
    }

    auto output_step = std::make_shared<ProjectionStep>(old_root->getCurrentDataStream(), assignments, output_types, true);
    auto new_root = old_root->addStep(context->nextNodeId(), std::move(output_step));

    PRINT_PLAN(new_root, plan_output);
    return new_root;
}

RelationPlan planExtremes(const RelationPlan & plan, ContextMutablePtr context)
{
    if (context->getSettingsRef().extremes)
    {
        auto extremes_step = std::make_shared<ExtremesStep>(plan.getRoot()->getCurrentDataStream());
        auto extremes = plan.getRoot()->addStep(context->nextNodeId(), std::move(extremes_step));
        return plan.withNewRoot(extremes);
    }
    return plan;
}
}

QueryPlanPtr QueryPlanner::plan(ASTPtr & query, Analysis & analysis, ContextMutablePtr context)
{
    step_id = GraphvizPrinter::PRINT_PLAN_BUILD_INDEX;
    CTERelationPlans cte_plans;
    RelationPlan relation_plan = planQuery(query, nullptr, analysis, context, cte_plans);
    relation_plan = planExtremes(relation_plan, context);
    PlanNodePtr plan_root = planOutput(relation_plan, query, analysis, context);
    CTEInfo cte_info;
    for (const auto & cte_plan : cte_plans)
        cte_info.add(cte_plan.first, cte_plan.second.getRoot());
    return std::make_unique<QueryPlan>(plan_root, cte_info, context->getPlanNodeIdAllocator());
}

RelationPlan QueryPlanner::planQuery(ASTPtr query, TranslationMapPtr outer_query_context, Analysis & analysis,
                                    ContextMutablePtr context, CTERelationPlans & cte_plans)
{
    QueryPlannerVisitor visitor {context, cte_plans, analysis, outer_query_context};
    return visitor.process(query);
}

RelationPlan QueryPlannerVisitor::visitASTSelectIntersectExceptQuery(ASTPtr & node, const Void &)
{
    auto & intersect_or_except = node->as<ASTSelectIntersectExceptQuery &>();
    auto selects = intersect_or_except.getListOfSelects();
    auto operator_to_union_mode = [](ASTSelectIntersectExceptQuery::Operator op) -> ASTSelectWithUnionQuery::Mode
    {
        switch (op)
        {
            case ASTSelectIntersectExceptQuery::Operator::INTERSECT_ALL:
                return ASTSelectWithUnionQuery::Mode::INTERSECT_ALL;
            case ASTSelectIntersectExceptQuery::Operator::INTERSECT_DISTINCT:
                return ASTSelectWithUnionQuery::Mode::INTERSECT_DISTINCT;
            case ASTSelectIntersectExceptQuery::Operator::EXCEPT_ALL:
                return ASTSelectWithUnionQuery::Mode::EXCEPT_ALL;
            case ASTSelectIntersectExceptQuery::Operator::EXCEPT_DISTINCT:
                return ASTSelectWithUnionQuery::Mode::EXCEPT_DISTINCT;
            default:
                throw Exception("Unrecognized set operator found", ErrorCodes::LOGICAL_ERROR);
        }
    };

    return planSetOperation(selects, operator_to_union_mode(intersect_or_except.final_operator));
}

RelationPlan QueryPlannerVisitor::visitASTSelectWithUnionQuery(ASTPtr & node, const Void &)
{
    auto & select_with_union = node->as<ASTSelectWithUnionQuery &>();
    return planSetOperation(select_with_union.list_of_selects->children, select_with_union.union_mode);
}

RelationPlan QueryPlannerVisitor::visitASTSelectQuery(ASTPtr & node, const Void &)
{
    auto & select_query = node->as<ASTSelectQuery &>();

    PlanBuilder builder = planFrom(select_query);
    PRINT_PLAN(builder.plan, plan_from);

    planFilter(builder, select_query, select_query.where());
    PRINT_PLAN(builder.plan, plan_where);

    planAggregate(builder, select_query);

    planFilter(builder, select_query, select_query.having());
    PRINT_PLAN(builder.plan, plan_having);

    planWindow(builder, select_query);

    planSelect(builder, select_query);
    PRINT_PLAN(builder.plan, plan_select);

    planDistinct(builder, select_query);

    planOrderBy(builder, select_query);

    planLimitBy(builder, select_query);

    planLimit(builder, select_query);

    // planSampling(builder, select_query);

    return planFinalSelect(builder, select_query);
}

RelationPlan QueryPlannerVisitor::visitASTSubquery(ASTPtr & node, const Void &)
{
    auto subquery = node->as<ASTSubquery &>();
    if (enable_shared_cte && analysis.isSharableCTE(subquery))
    {
        auto & cte_analysis = analysis.getCTEAnalysis(subquery);
        auto cte_id = cte_analysis.id;
        RelationPlan cte_ref;
        if (!cte_plans.contains(cte_id))
        {
            cte_ref = process(node->children.front());
            cte_plans.emplace(cte_id, cte_ref);
        }
        else
            cte_ref = cte_plans.at(cte_id);

        std::unordered_map<String, String> output_columns;
        NamesAndTypes mapped_name_and_types;
        FieldSymbolInfos field_symbol_infos;
        for (const auto & name_and_type : cte_ref.getRoot()->getCurrentDataStream().header.getNamesAndTypes())
        {
            auto new_name = context->getSymbolAllocator()->newSymbol(name_and_type.name);
            output_columns.emplace(new_name, name_and_type.name);
            mapped_name_and_types.emplace_back(new_name, name_and_type.type);
            field_symbol_infos.emplace_back(FieldSymbolInfo{new_name});
        }

        PlanNodePtr plan = PlanNodeBase::createPlanNode(
            context->nextNodeId(), std::make_shared<CTERefStep>(DataStream{mapped_name_and_types}, cte_id, output_columns, PredicateConst::TRUE_VALUE));
        PRINT_PLAN(plan, plan_cte);
        return RelationPlan{plan, field_symbol_infos};
    }

    return process(node->children.front());
}

PlanBuilder QueryPlannerVisitor::planWithoutTables(ASTSelectQuery & select_query)
{
    PlanNodePtr node;
    auto symbol = context->getSymbolAllocator()->newSymbol("dummy");

    {
        NamesAndTypes header;
        Fields data;

        header.emplace_back(symbol, std::make_shared<DataTypeUInt8>());
        data.emplace_back(0U);

        auto values_step = std::make_shared<ValuesStep>(header, data);
        node = PlanNodeBase::createPlanNode(context->nextNodeId(), values_step);
    }

    RelationPlan plan {node, FieldSymbolInfos {{symbol}}};
    return toPlanBuilder(plan, analysis.getQueryWithoutFromScope(select_query));
}

PlanBuilder QueryPlannerVisitor::planTables(ASTTablesInSelectQuery & tables_in_select, ASTSelectQuery & select_query)
{
    auto & first_table_elem = tables_in_select.children[0]->as<ASTTablesInSelectQueryElement &>();
    auto builder = planTableExpression(first_table_elem.table_expression->as<ASTTableExpression &>(), select_query);

    for (size_t idx = 1; idx < tables_in_select.children.size(); ++idx)
    {
        auto & table_element = tables_in_select.children[idx]->as<ASTTablesInSelectQueryElement &>();

        if (auto * table_expression = table_element.table_expression->as<ASTTableExpression>())
        {
            auto right_builder = planTableExpression(*table_expression, select_query);
            planJoin(table_element.table_join->as<ASTTableJoin &>(), builder, right_builder);
        }
    }

    return builder;
}

PlanBuilder QueryPlannerVisitor::planTableExpression(ASTTableExpression & table_expression, ASTSelectQuery & select_query)
{
    if (table_expression.database_and_table_name)
        return planTable(table_expression.database_and_table_name->as<ASTTableIdentifier &>(), select_query);
    if (table_expression.subquery)
        return planTableSubquery(table_expression.subquery->as<ASTSubquery &>(), table_expression.subquery);
    if (table_expression.table_function)
        return planTableFunction(table_expression.table_function->as<ASTFunction &>(), select_query);

    __builtin_unreachable();
}

PlanBuilder QueryPlannerVisitor::planTable(ASTTableIdentifier & db_and_table, ASTSelectQuery & select_query)
{
    // Reading a table consists of 3 steps:
    //  1. Read ordinary columns from storage
    //  2. Calculate alias columns
    //  3. Add mask for sensitive columns(FGAC)

    // read ordinary columns
    const auto *storage_scope = analysis.getTableStorageScope(db_and_table);
    auto relation_plan = planReadFromStorage(db_and_table, storage_scope, select_query);
    auto builder = toPlanBuilder(relation_plan, storage_scope);
    PRINT_PLAN(builder.plan, plan_table);

    // append alias columns
    {
        auto & alias_columns = analysis.getTableAliasColumns(db_and_table);

        Assignments assignments;
        NameToType types;
        putIdentities(builder.getOutputNamesAndTypes(), assignments, types);
        FieldSymbolInfos field_symbol_infos = builder.getFieldSymbolInfos();

        for (auto & alias_column: alias_columns)
        {
            auto alias_symbol = context->getSymbolAllocator()->newSymbol(alias_column->tryGetAlias());
            assignments.emplace_back(alias_symbol, builder.translate(alias_column));
            types[alias_symbol] = analysis.getExpressionType(alias_column);
            field_symbol_infos.emplace_back(alias_symbol);
        }

        auto project_alias_columns = std::make_shared<ProjectionStep>(builder.getCurrentDataStream(), assignments, types);
        builder.addStep(std::move(project_alias_columns));
        builder.withScope(analysis.getScope(db_and_table), field_symbol_infos);
        PRINT_PLAN(builder.plan, plan_add_alias);
    }

    return builder;
}

PlanBuilder QueryPlannerVisitor::planTableFunction(ASTFunction & table_function, ASTSelectQuery & select_query)
{
    const auto * scope = analysis.getScope(table_function);
    auto relation_plan = planReadFromStorage(table_function, scope, select_query);
    auto builder = toPlanBuilder(relation_plan, scope);
    PRINT_PLAN(builder.plan, plan_table_function);
    return builder;
}

PlanBuilder QueryPlannerVisitor::planTableSubquery(ASTSubquery & subquery, ASTPtr & node)
{
    auto plan = process(node);
    auto builder = toPlanBuilder(plan, analysis.getScope(subquery));
    PRINT_PLAN(builder.plan, plan_table_subquery);
    return builder;
}

void QueryPlannerVisitor::planJoin(ASTTableJoin & table_join, PlanBuilder & left_builder, PlanBuilder & right_builder)
{
    if (isCrossJoin(table_join))
        planCrossJoin(table_join, left_builder, right_builder);
    else if (table_join.using_expression_list)
        planJoinUsing(table_join, left_builder, right_builder);
    else if (table_join.on_expression)
        planJoinOn(table_join, left_builder, right_builder);
    else
        throw Exception("Unrecognized join criteria found", ErrorCodes::PLAN_BUILD_ERROR);

    PRINT_PLAN(left_builder.plan, plan_join);
}

void QueryPlannerVisitor::planCrossJoin(ASTTableJoin & table_join, PlanBuilder & left_builder, PlanBuilder & right_builder)
{
    // build join node
    {
        NamesAndTypes output_header;
        append(output_header, left_builder.getOutputNamesAndTypes());
        append(output_header, right_builder.getOutputNamesAndTypes());

        auto join_step = std::make_shared<JoinStep>(
            DataStreams{left_builder.getCurrentDataStream(), right_builder.getCurrentDataStream()},
            DataStream{.header = output_header},
            ASTTableJoin::Kind::Cross,
            ASTTableJoin::Strictness::Unspecified);

        left_builder.addStep(std::move(join_step), {left_builder.getRoot(), right_builder.getRoot()});
    }

    // update scope
    {
        FieldSymbolInfos field_symbol_infos;
        append(field_symbol_infos, left_builder.getFieldSymbolInfos());
        append(field_symbol_infos, right_builder.getFieldSymbolInfos());
        left_builder.withScope(analysis.getScope(table_join), field_symbol_infos);
    }
}

void QueryPlannerVisitor::planJoinUsing(ASTTableJoin & table_join, PlanBuilder & left_builder, PlanBuilder & right_builder)
{
    auto & join_analysis = analysis.getJoinUsingAnalysis(table_join);

    // 1. prepare join keys
    auto [left_keys, right_keys] = prepareJoinUsingKeys(table_join, left_builder, right_builder);

    // 2. build join node
    auto join_step = std::make_shared<JoinStep>(
        DataStreams {left_builder.getCurrentDataStream(), right_builder.getCurrentDataStream()},
        getJoinOutputStream(table_join, left_builder, right_builder),
        table_join.kind,
        table_join.strictness,
        left_keys,
        right_keys,
        PredicateConst::TRUE_VALUE,
        true,
        use_ansi_semantic ? std::nullopt : std::make_optional(join_analysis.require_right_keys));
    left_builder.addStep(std::move(join_step), {left_builder.getRoot(), right_builder.getRoot()});
    // left_builder.setWithNonJoinStreamIfNecessary(table_join);

    // 3. update translation map
    if (isSemiOrAntiJoin(table_join))
    {
        // in case of Semi/Anti join, we don't have to change scope, but we remove any calculated expressions
        left_builder.removeMappings();
    }
    else
    {
        FieldSymbolInfos output_symbols;

        if (use_ansi_semantic)
        {
            for (auto & field_id: join_analysis.left_join_fields)
            {
                output_symbols.emplace_back(left_builder.getFieldSymbol(field_id));
            }

            auto add_non_join_fields = [&] (const FieldSymbolInfos & source_fields, std::vector<size_t> & join_fields_list)
            {
                std::unordered_set<size_t> join_fields {join_fields_list.begin(), join_fields_list.end()};

                for (size_t i = 0; i < source_fields.size(); ++i)
                {
                    if (join_fields.find(i) == join_fields.end())
                        output_symbols.push_back(source_fields[i]);
                }
            };

            add_non_join_fields(left_builder.getFieldSymbolInfos(), join_analysis.left_join_fields);
            add_non_join_fields(right_builder.getFieldSymbolInfos(), join_analysis.right_join_fields);
        }
        else
        {
            // process left
            auto & left_join_field_reverse_map = join_analysis.left_join_field_reverse_map;
            const auto & left_symbols = left_builder.getFieldSymbolInfos();

            for (size_t i = 0; i < left_symbols.size(); ++i)
            {
                if (left_join_field_reverse_map.count(i))
                {
                    output_symbols.emplace_back(left_keys[left_join_field_reverse_map[i]]);
                }
                else
                {
                    output_symbols.push_back(left_symbols[i]);
                }
            }

            // process right
            auto & require_right_keys = join_analysis.require_right_keys;
            auto & right_join_field_reverse_map = join_analysis.right_join_field_reverse_map;
            const auto & right_field_symbols = right_builder.getFieldSymbolInfos();

            for (size_t i = 0; i < right_field_symbols.size(); ++i)
            {
                if (!right_join_field_reverse_map.count(i))
                {
                    output_symbols.push_back(right_field_symbols[i]);
                }
                else if (require_right_keys[right_join_field_reverse_map[i]])
                {
                    output_symbols.emplace_back(right_field_symbols[i].getPrimarySymbol());
                }
            }
        }
        left_builder.withScope(analysis.getScope(table_join), output_symbols);
    }
}

void QueryPlannerVisitor::planJoinOn(ASTTableJoin & table_join, PlanBuilder & left_builder, PlanBuilder & right_builder)
{
    // 1. update translation map(for Semi/Anti join, we need to keep the left side's scope & symbols, as they will be used for output)
    ScopePtr left_scope = left_builder.getScope();
    FieldSymbolInfos left_field_symbols = left_builder.getFieldSymbolInfos();
    ScopePtr joined_scope = analysis.getScope(table_join);
    FieldSymbolInfos joined_field_symbols;
    append(joined_field_symbols, left_builder.getFieldSymbolInfos());
    append(joined_field_symbols, right_builder.getFieldSymbolInfos());

    left_builder.withScope(joined_scope, joined_field_symbols);
    right_builder.withScope(joined_scope, joined_field_symbols);

    // 2. prepare join keys
    auto [left_keys, right_keys] = prepareJoinOnKeys(table_join, left_builder, right_builder);

    // 3. build join filter
    ASTPtr join_filter = PredicateConst::TRUE_VALUE;
    {
        auto & join_analysis = analysis.getJoinOnAnalysis(table_join);
        // use a new translation map to avoid any calculated expression
        auto translation = std::make_shared<TranslationMap>(outer_context, joined_scope, joined_field_symbols, analysis, context);
        ASTs conjuncts;

        if (!isAsofJoin(table_join))
        {
            for (auto & cond: join_analysis.inequality_conditions)
            {
                auto left_side = translation->translate(cond.left_ast);
                auto right_side = translation->translate(cond.right_ast);
                conjuncts.push_back(makeASTFunction(getFunctionForInequality(cond.inequality), left_side, right_side));
            }
        }

        append(conjuncts, join_analysis.complex_expressions,
               [&](auto & expr) {return translation->translate(expr);});

        join_filter = cnfToExpression(conjuncts);
    }

    // 4. join inequality
    ASOF::Inequality asof_inequality = ASOF::Inequality::GreaterOrEquals;

    if (isAsofJoin(table_join))
        asof_inequality = analysis.getJoinOnAnalysis(table_join).getAsofInequality();

    // 5. build join node
    auto join_step = std::make_shared<JoinStep>(
        DataStreams {left_builder.getCurrentDataStream(), right_builder.getCurrentDataStream()},
        getJoinOutputStream(table_join, left_builder, right_builder),
        table_join.kind,
        table_join.strictness,
        left_keys,
        right_keys,
        isNormalInnerJoin(table_join) ? PredicateConst::TRUE_VALUE : join_filter,
        false,
        std::nullopt,
        asof_inequality);
    left_builder.addStep(std::move(join_step), {left_builder.getRoot(), right_builder.getRoot()});
    // left_builder.setWithNonJoinStreamIfNecessary(table_join);

    // 6. for inner join, build a FilterNode for join filter
    if (isNormalInnerJoin(table_join) && !ASTEquality::compareTree(join_filter, PredicateConst::TRUE_VALUE))
    {
        auto filter_step = std::make_shared<FilterStep>(left_builder.getCurrentDataStream(), join_filter);
        left_builder.addStep(filter_step);
    }

    // 7. update translation map for Semi/Anti join
    if (isSemiOrAntiJoin(table_join))
        left_builder.withScope(left_scope, left_field_symbols);
    else
        left_builder.removeMappings();
}

std::pair<Names, Names> QueryPlannerVisitor::prepareJoinUsingKeys(ASTTableJoin & table_join, PlanBuilder & left_builder,
                                                                  PlanBuilder & right_builder)
{
    auto & join_using_analysis = analysis.getJoinUsingAnalysis(table_join);

    auto prepare_join_keys = [&](std::vector<size_t> & join_key_indices, DataTypes & coercion, PlanBuilder & builder)
    {
        Names join_key_symbols;
        join_key_symbols.reserve(join_key_indices.size());

        for (auto & join_key_index: join_key_indices)
            join_key_symbols.push_back(builder.getFieldSymbol(join_key_index));

        assert(join_key_symbols.size() == coercion.size());
        NameToType name_to_type;

        for (size_t i = 0; i < join_key_symbols.size(); ++i)
            name_to_type.emplace(join_key_symbols[i], coercion[i]);

        coerceTypesForSymbols(builder, name_to_type, true);
        return join_key_symbols;
    };


    Names left_keys, right_keys;

    if (use_ansi_semantic)
    {
        left_keys = prepare_join_keys(join_using_analysis.left_join_fields, join_using_analysis.left_coercions, left_builder);
        right_keys = prepare_join_keys(join_using_analysis.right_join_fields, join_using_analysis.right_coercions, right_builder);
    }
    else
    {
        ExpressionsAndTypes expressions_and_types;
        assert(join_using_analysis.join_key_asts.size() == join_using_analysis.left_coercions.size());

        for (size_t i = 0; i < join_using_analysis.join_key_asts.size(); ++i)
            expressions_and_types.emplace_back(join_using_analysis.join_key_asts[i], join_using_analysis.left_coercions[i]);
        left_keys = projectExpressionsWithCoercion(left_builder, expressions_and_types);
        right_keys = prepare_join_keys(join_using_analysis.right_join_fields, join_using_analysis.right_coercions, right_builder);
    }

    return {left_keys, right_keys};
}

std::pair<Names, Names> QueryPlannerVisitor::prepareJoinOnKeys(ASTTableJoin & table_join, PlanBuilder & left_builder,
                                                               PlanBuilder & right_builder)
{
    auto & join_analysis = analysis.getJoinOnAnalysis(table_join);

    ExpressionsAndTypes left_conditions;
    ExpressionsAndTypes right_conditions;

    // for asof join, equality exprs & inequality exprs forms the join keys
    // for other joins, equality exprs forms the join keys, inequality exprs & complex exprs forms the join filter
    for (const auto & condition: join_analysis.equality_conditions)
    {
        left_conditions.emplace_back(condition.left_ast, condition.left_coercion);
        right_conditions.emplace_back(condition.right_ast, condition.right_coercion);
    }

    if (isAsofJoin(table_join))
    {
        for (const auto & condition: join_analysis.inequality_conditions)
        {
            left_conditions.emplace_back(condition.left_ast, condition.left_coercion);
            right_conditions.emplace_back(condition.right_ast, condition.right_coercion);
        }
    }

    // translate join keys to symbols
    Names left_symbols = projectExpressionsWithCoercion(left_builder, left_conditions);
    Names right_symbols = projectExpressionsWithCoercion(right_builder, right_conditions);

    return {left_symbols, right_symbols};
}

DataStream QueryPlannerVisitor::getJoinOutputStream(ASTTableJoin & table_join, PlanBuilder & left_builder, PlanBuilder & right_builder)
{
    if (isSemiOrAntiJoin(table_join))
    {
        return left_builder.getCurrentDataStream();
    }
    else
    {
        DataStream output_stream;

        // columns will be pruned further in optimizing phase
        for (auto & x: left_builder.getOutputNamesAndTypes())
            output_stream.header.insert({x.type, x.name});

        for (auto & x: right_builder.getOutputNamesAndTypes())
            output_stream.header.insert({x.type, x.name});

        return output_stream;
    }
}

RelationPlan QueryPlannerVisitor::planReadFromStorage(const IAST & table_ast, ScopePtr table_scope, ASTSelectQuery & origin_query)
{
    const auto & storage_analysis = analysis.getStorageAnalysis(table_ast);
    const auto & storage = storage_analysis.storage;

    auto required_columns = table_scope->getOriginColumns();
    const auto & used_sub_columns = analysis.getUsedSubColumns(table_ast);
    NamesWithAliases columns_with_aliases;
    FieldSymbolInfos field_symbols;

    field_symbols.reserve(required_columns.size());
    columns_with_aliases.reserve(required_columns.size()); // may be larger

    assert(used_sub_columns.size() <= required_columns.size());

    for (size_t i = 0; i < required_columns.size(); ++i)
    {
        // assign primary column symbol
        const auto & primary_column_name = required_columns[i];
        auto primary_column_symbol = context->getSymbolAllocator()->newSymbol(primary_column_name);
        columns_with_aliases.emplace_back(primary_column_name, primary_column_symbol);

        // assign sub column symbols
        FieldSymbolInfo::SubColumnToSymbol sub_column_symbols;

        if (i < used_sub_columns.size())
        {
            for (const auto & sub_column_id : used_sub_columns[i])
            {
                auto sub_column_name = sub_column_id.getSubColumnName(primary_column_name);
                auto sub_column_symbol = context->getSymbolAllocator()->newSymbol(sub_column_name);
                columns_with_aliases.emplace_back(sub_column_name, sub_column_symbol);
                sub_column_symbols.emplace(sub_column_id, sub_column_symbol);
            }
        }

        field_symbols.emplace_back(primary_column_symbol, sub_column_symbols);
    }

    /// create ASTSelectQuery for "SELECT * FROM table" as if written by hand
    const auto generated_query = std::make_shared<ASTSelectQuery>();
    generated_query->setExpression(ASTSelectQuery::Expression::SELECT, std::make_shared<ASTExpressionList>());
    const auto select_expression_list = generated_query->select();
    if (origin_query.prewhere())
        generated_query->setExpression(ASTSelectQuery::Expression::PREWHERE, origin_query.prewhere()->clone());
    /*
    if (origin_query.implicitWhere())
        generated_query->setExpression(ASTSelectQuery::Expression::IMPLICITWHERE, origin_query.implicitWhere()->clone());
    if (origin_query.encodedWhere())
        generated_query->setExpression(ASTSelectQuery::Expression::ENCODEDWHERE, origin_query.encodedWhere()->clone());
    */

    NamesAndTypesList columns;
    columns = storage->getInMemoryMetadataPtr()->getColumns().getOrdinary();
    auto storage_id = storage->getStorageID();
    generated_query->replaceDatabaseAndTable(storage_id.getDatabaseName(), storage_id.getTableName());

    // set sampling size
    if (origin_query.sampleSize())
    {
        auto & new_tables_in_select_query = generated_query->tables()->as<ASTTablesInSelectQuery &>();
        auto & new_tables_element = new_tables_in_select_query.children[0]->as<ASTTablesInSelectQueryElement &>();
        auto & new_table_expr = new_tables_element.table_expression->as<ASTTableExpression &>();
        new_table_expr.sample_size = origin_query.sampleSize();

        if (origin_query.sampleOffset())
            new_table_expr.sample_offset = origin_query.sampleOffset();
    }

    select_expression_list->children.reserve(columns.size());
    /// manually substitute column names in place of asterisk
    for (const auto & column : columns)
        select_expression_list->children.emplace_back(std::make_shared<ASTIdentifier>(column.name));

    SelectQueryInfo query_info;
    query_info.query = generated_query;

    const Settings & settings = context->getSettingsRef();
    UInt64 max_block_size = settings.max_block_size;
    if (!max_block_size)
        throw Exception("Setting 'max_block_size' cannot be zero", ErrorCodes::PARAMETER_OUT_OF_BOUND);

    /*
    bool is_remote = false;
    if (storage && storage->isRemote())
    {
        is_remote = true;
    }

    /// How many streams we ask for storage to produce, and in how many threads we will do further processing.
    size_t max_streams = 1;
    if (max_streams == 0)
        throw Exception("Logical error: zero number of streams requested", ErrorCodes::LOGICAL_ERROR);

    /// If necessary, we request more sources than the number of threads - to distribute the work evenly over the threads.
    if (max_streams > 1 && !is_remote)
        max_streams *= settings.max_streams_to_max_threads_ratio;

    MergeTreeReadPlannerPtr read_planner = getReadPlanner(origin_query);
    MergeTreeBitMapSchedulerPtr bitmap_scheduler = getBitMapScheduler(origin_query, storage, max_streams);

    if (read_planner || bitmap_scheduler)
    {
        auto * merge_tree_data = dynamic_cast<MergeTreeData *>(storage.get());
        auto * cloud_merge_tree = dynamic_cast<StorageCloudMergeTree *>(storage.get());
        BlockInputStreams streams;

        if (merge_tree_data)
            streams = merge_tree_data->read(
                required_columns, query_info, context, processing_stage, max_block_size, max_streams, read_planner, bitmap_scheduler);
        else if (cloud_merge_tree)
            streams = cloud_merge_tree->read(
                required_columns, query_info, context, processing_stage, max_block_size, max_streams, nullptr, bitmap_scheduler);
        else
            //throw Exception("Read order not supported by non MergeTree engine", ErrorCodes::NOT_IMPLEMENTED);
            streams = storage->read(required_columns, query_info, context, processing_stage, max_block_size, max_streams);

        if (!streams.empty())
        {
            auto table_step = std::make_shared<ReadFromStorageStep>(
                context,
                storage,
                database,
                table,
                columns_with_aliases,
                origin_query.prewhere(),
                query_info,
                processing_stage,
                max_block_size);
            return table_step;
        }
    }
    else
    {
        auto * cloud_merge_tree = dynamic_cast<StorageCloudMergeTree *>(storage.get());
        if (cloud_merge_tree)
        {
            const_cast<Context *>(&context)->setNameNode();
        }

        auto table_step = std::make_unique<TableScanStep>(
            context,
            storage->getStorageID(),
            columns_with_aliases,
            query_info,
            QueryProcessingStage::Enum::FetchColumns,
            max_block_size);
        return table_step;
    }
    */

    auto table_step = std::make_shared<TableScanStep>(
        context,
        storage->getStorageID(),
        columns_with_aliases,
        query_info,
        QueryProcessingStage::Enum::FetchColumns,
        max_block_size);

    auto plan_node = PlanNodeBase::createPlanNode(context->nextNodeId(), table_step);
    return {plan_node, field_symbols};
}

/*
MergeTreeReadPlannerPtr QueryPlannerVisitor::getReadPlanner(ASTSelectQuery & query)
{
    if (!query.stepExecute())
        return nullptr;

    ASTStepExecute & step_execute = typeid_cast<ASTStepExecute &>(*(query.refStepExecute()));

    if (!step_execute.order_expression_list || !step_execute.step_number || !step_execute.batch_size)
        return nullptr;

    SortDescription order_desc;
    order_desc.reserve(step_execute.order_expression_list->children.size());
    for (const auto & elem : step_execute.order_expression_list->children)
    {
        String name = elem->children.front()->getColumnName();
        const ASTOrderByElement & order_by_elem = typeid_cast<const ASTOrderByElement &>(*elem);

        std::shared_ptr<Collator> collator;
        if (order_by_elem.collation)
            collator = std::make_shared<Collator>(typeid_cast<const ASTLiteral &>(*order_by_elem.collation).value.get<String>());

        order_desc.emplace_back(name, order_by_elem.direction, order_by_elem.nulls_direction, collator);
    }
    if (order_desc.empty())
        return nullptr;

    size_t step_number = 0;
    size_t step_size = 1;
    size_t batch_size = 0;
    step_number = safeGet<UInt64>(typeid_cast<ASTLiteral &>(*(step_execute.step_number)).value);
    if (step_execute.step_size)
        step_size = safeGet<UInt64>(typeid_cast<ASTLiteral &>(*(step_execute.step_size)).value);
    batch_size = safeGet<UInt64>(typeid_cast<ASTLiteral &>(*(step_execute.batch_size)).value);

    return std::make_shared<MergeTreeReadPlanner>(std::move(order_desc), step_number, step_size, batch_size);
}

MergeTreeBitMapSchedulerPtr QueryPlannerVisitor::getBitMapScheduler(ASTSelectQuery & query, const StoragePtr & storage, size_t max_streams)
{
    if (!query.bitmapExecute())
        return nullptr;

    ASTBitMapExecute & bitmap_execute = typeid_cast<ASTBitMapExecute &>(*(query.refBitmapExecute()));

    if (!bitmap_execute.split_key)
        return nullptr;

    const auto * merge_tree = dynamic_cast<const MergeTreeData *>(storage.get());

    if (!merge_tree)
        return nullptr;

    auto is_partition_key = [&](auto & data) -> size_t {
        ASTPtr partition_expr_ast = data.getPartitionKeyList();
        if (!partition_expr_ast)
            return 0;

        ASTs children = partition_expr_ast->children;

        size_t res_idx = 0;

        for (size_t idx = 0; idx < children.size(); ++idx)
        {
            ASTPtr child = children.at(idx);
            if (child->getAliasOrColumnName() == bitmap_execute.split_key->getAliasOrColumnName())
            {
                res_idx = idx;
                break;
            }
        }

        NamesAndTypesList partition_keys = data.partition_key_sample.getNamesAndTypesList();
        DataTypePtr split_type = partition_keys.getTypes().at(res_idx);
        WhichDataType to_type(split_type);
        if (to_type.isUInt() || to_type.isInt())
            return res_idx;
        else
            throw Exception("Only numeric type can be used to be BITMAPEXECUTE by key", ErrorCodes::LOGICAL_ERROR);

        return 0;
    };

    size_t valid_split_by = is_partition_key(*merge_tree);

    // return nullptr if we want split by the first partition key
    if (valid_split_by == 0)
        return nullptr;

    return std::make_shared<MergeTreeBitMapScheduler>(max_streams, valid_split_by);
}
*/

PlanBuilder QueryPlannerVisitor::planFrom(ASTSelectQuery & select_query)
{
    if (select_query.tables())
        return planTables(select_query.refTables()->as<ASTTablesInSelectQuery &>(), select_query);
    else
        return planWithoutTables(select_query);
}

void QueryPlannerVisitor::planFilter(PlanBuilder & builder, ASTSelectQuery & select_query, const ASTPtr & filter)
{
    if (!filter || ASTEquality::compareTree(filter, PredicateConst::TRUE_VALUE))
        return;

    /// In clickhouse, if filter contains non-determinism functions reference to select, these functions may compute twice.
    /// Extract these functions into projection, use translation map to replace upstream expressions.
    /// Eg, select arrayJoin([1,2,3]) a from system.one where a = 1
    auto nondeterministic_expressions = extractExpressions(context, analysis, filter, false,
                                                           [&](const ASTPtr & expr)
                                                           {
                                                               if (auto * func = expr->as<ASTFunction>())
                                                                   return !context->isFunctionDeterministic(func->name);
                                                               return false;
                                                           });

    builder.appendProjection(nondeterministic_expressions);
    planSubqueryExpression(builder, select_query, filter);
    auto filter_step = std::make_shared<FilterStep>(builder.getCurrentDataStream(), builder.translate(filter));
    builder.addStep(std::move(filter_step));
}

void QueryPlannerVisitor::planAggregate(PlanBuilder & builder, ASTSelectQuery & select_query)
{
    if (!analysis.needAggregate(select_query))
    {
        if (select_query.group_by_with_totals || select_query.group_by_with_rollup || select_query.group_by_with_cube)
            throw Exception("WITH TOTALS, ROLLUP or CUBE are not supported without aggregation", ErrorCodes::NOT_IMPLEMENTED);
        return;
    }

    auto & group_by_analysis = analysis.getGroupByAnalysis(select_query);
    auto & aggregate_analysis = analysis.getAggregateAnalysis(select_query);

    // add projections for aggregation function params, group by keys
    {
        ASTs aggregate_inputs;

        append(aggregate_inputs, group_by_analysis.grouping_expressions);

        for (auto & agg_item: aggregate_analysis)
            append(aggregate_inputs, agg_item.expression->arguments->children);

        planSubqueryExpression(builder, select_query, aggregate_inputs);
        builder.appendProjection(aggregate_inputs);
        PRINT_PLAN(builder.plan, plan_prepare_aggregate);
    }

    // build aggregation descriptions
    AstToSymbol mappings_for_aggregate = createScopeAwaredASTMap<String>(analysis);
    AggregateDescriptions aggregate_descriptions;

    auto uniq_aggs = deduplicateByAst(aggregate_analysis, analysis, std::mem_fn(&AggregateAnalysis::expression));
    for (auto & agg_item: uniq_aggs)
    {
        AggregateDescription agg_desc;

        agg_desc.function = agg_item.function;
        agg_desc.parameters = agg_item.parameters;
        agg_desc.argument_names = builder.translateToSymbols(agg_item.expression->arguments->children);
        agg_desc.column_name = context->getSymbolAllocator()->newSymbol(agg_item.expression);

        aggregate_descriptions.push_back(agg_desc);
        mappings_for_aggregate.emplace(agg_item.expression, agg_desc.column_name);
    }

    // build grouping operations
    // TODO: grouping function only work, when group by with rollup?
    GroupingDescriptions grouping_operations_descs;
    for (auto & grouping_op : analysis.getGroupingOperations(select_query))
        if (!mappings_for_aggregate.count(grouping_op))
        {
            GroupingDescription description;

            for (const auto & argument: grouping_op->arguments->children)
                description.argument_names.emplace_back(builder.translateToSymbol(argument));

            description.output_name = context->getSymbolAllocator()->newSymbol(grouping_op);

            mappings_for_aggregate.emplace(grouping_op, description.output_name);
            grouping_operations_descs.emplace_back(std::move(description));
        }

    // collect group by keys & prune invisible columns
    Names keys_for_all_group;
    NameSet key_set_for_all_group;
    GroupingSetsParamsList grouping_sets_params;
    FieldSymbolInfos visible_fields(builder.getFieldSymbolInfos().size());
    AstToSymbol complex_expressions = createScopeAwaredASTMap<String>(analysis);

    auto process_grouping_set = [&](const ASTs & grouping_set) {
        Names keys_for_this_group;
        NameSet key_set_for_this_group;

        for (const auto & grouping_expr: grouping_set)
        {
            auto symbol = builder.translateToSymbol(grouping_expr);
            bool new_global_key = false;

            if (!key_set_for_all_group.count(symbol))
            {
                keys_for_all_group.push_back(symbol);
                key_set_for_all_group.insert(symbol);
                new_global_key = true;
            }

            if (!key_set_for_this_group.count(symbol))
            {
                keys_for_this_group.push_back(symbol);
                key_set_for_this_group.insert(symbol);
            }

            if (new_global_key)
            {
                if (auto col_ref = analysis.tryGetColumnReference(grouping_expr);
                    col_ref && builder.isLocalScope(col_ref->scope))
                {
                    assert(symbol == builder.getFieldSymbolInfo(col_ref->hierarchy_index).getPrimarySymbol());
                    visible_fields[col_ref->hierarchy_index].primary_symbol = symbol;
                }
                else if (auto sub_col_ref = analysis.tryGetSubColumnReference(grouping_expr);
                         sub_col_ref && builder.isLocalScope(sub_col_ref->getScope())
                         && builder.getFieldSymbolInfo(sub_col_ref->getFieldHierarchyIndex()).tryGetSubColumnSymbol(sub_col_ref->getColumnID())) // Note: sub column symbol may be invalidated
                {
                    auto sub_column_symbol = *builder.getFieldSymbolInfo(sub_col_ref->getFieldHierarchyIndex()).tryGetSubColumnSymbol(sub_col_ref->getColumnID());
                    assert(symbol == sub_column_symbol);
                    visible_fields[sub_col_ref->getFieldHierarchyIndex()].sub_column_symbols.emplace(sub_col_ref->getColumnID(), symbol);
                }
                else if (builder.isCalculatedExpression(grouping_expr))
                {
                    complex_expressions.emplace(grouping_expr, symbol);
                }
            }
        }

        grouping_sets_params.emplace_back(std::move(keys_for_this_group));
    };

    for (const auto & grouping_set : group_by_analysis.grouping_sets)
        process_grouping_set(grouping_set);

    builder.withNewMappings(visible_fields, complex_expressions);

    if (select_query.group_by_with_rollup)
    {
        grouping_sets_params.clear();
        auto key_size = keys_for_all_group.size();
        for (size_t set_size = 0; set_size <= key_size; set_size++)
        {
            auto end = keys_for_all_group.begin();
            std::advance(end, set_size);
            Names keys_for_this_group{keys_for_all_group.begin(), end};
            grouping_sets_params.emplace_back(std::move(keys_for_this_group));
        }
    }

    if (select_query.group_by_with_cube)
    {
        grouping_sets_params.clear();
        for (auto keys_for_this_group : Utils::powerSet(keys_for_all_group))
        {
            grouping_sets_params.emplace_back(std::move(keys_for_this_group));
        }
        grouping_sets_params.emplace_back(GroupingSetsParams{});
    }

    auto agg_step = std::make_shared<AggregatingStep>(
        builder.getCurrentDataStream(),
        std::move(keys_for_all_group),
        std::move(aggregate_descriptions),
        select_query.group_by_with_grouping_sets || grouping_sets_params.size() > 1 ? std::move(grouping_sets_params) : GroupingSetsParamsList{},
        true,
        grouping_operations_descs,
        select_query.group_by_with_totals);

    builder.addStep(std::move(agg_step));
    builder.withAdditionalMappings(mappings_for_aggregate);
    PRINT_PLAN(builder.plan, plan_aggregate);
}

void QueryPlannerVisitor::planWindow(PlanBuilder & builder, ASTSelectQuery & select_query)
{
    if (analysis.getWindowAnalysisOfSelectQuery(select_query).empty())
        return;

    auto & window_analysis = analysis.getWindowAnalysisOfSelectQuery(select_query);

    // add projections for window function params, partition by keys, sorting keys
    {
        ASTs window_inputs;

        for (auto & window_item: window_analysis)
        {
            append(window_inputs, window_item->expression->arguments->children);

            if (window_item->resolved_window->partition_by)
                append(window_inputs, window_item->resolved_window->partition_by->children);

            if (window_item->resolved_window->order_by)
                append(window_inputs, window_item->resolved_window->order_by->children,
                       [](ASTPtr & order_item) {return order_item->as<ASTOrderByElement &>().children[0];});
        }

        planSubqueryExpression(builder, select_query, window_inputs);
        builder.appendProjection(window_inputs);
        PRINT_PLAN(builder.plan, plan_prepare_window);
    }

    // build window description
    WindowDescriptions window_descriptions;

    for (auto & window_item: window_analysis)
    {
        if (window_descriptions.find(window_item->window_name) == window_descriptions.end())
        {
            WindowDescription window_desc;
            const auto & resolved_window = window_item->resolved_window;

            window_desc.window_name = window_item->window_name;

            if (resolved_window->partition_by)
            {
                for (const auto & ast : resolved_window->partition_by->children)
                {
                    window_desc.partition_by.push_back(SortColumnDescription(
                        builder.translateToSymbol(ast), 1 /* direction */,
                        1 /* nulls_direction */));
                }
            }

            if (resolved_window->order_by)
            {
                for (const auto & ast : resolved_window->order_by->children)
                {
                    const auto & order_by_element = ast->as<ASTOrderByElement &>();
                    window_desc.order_by.push_back(SortColumnDescription(
                        builder.translateToSymbol(order_by_element.children.front()),
                        order_by_element.direction,
                        order_by_element.nulls_direction));
                }
            }

            window_desc.full_sort_description = window_desc.partition_by;
            window_desc.full_sort_description.insert(window_desc.full_sort_description.end(),
                                                     window_desc.order_by.begin(), window_desc.order_by.end());

            window_desc.frame = resolved_window->frame;
            window_descriptions.insert({window_item->window_name, window_desc});
        }

        WindowFunctionDescription window_function;
        window_function.function_node = window_item->expression.get();
        window_function.column_name = context->getSymbolAllocator()->newSymbol(window_item->expression);
        window_function.aggregate_function = window_item->aggregator;
        window_function.function_parameters = window_item->parameters;

        const ASTs & arguments = window_item->expression->arguments->children;
        window_function.argument_types.resize(arguments.size());
        window_function.argument_names.resize(arguments.size());

        for (size_t i = 0; i < arguments.size(); ++i)
        {
            window_function.argument_types[i] = analysis.getExpressionType(arguments[i]);
            window_function.argument_names[i] = builder.translateToSymbol(arguments[i]);
        }

        window_descriptions[window_item->window_name].window_functions.push_back(window_function);
    }

    // add window steps
    for (const auto & [_, window_desc]: window_descriptions)
    {
        AstToSymbol mappings = createScopeAwaredASTMap<String>(analysis);

        for (const auto & window_func: window_desc.window_functions)
        {
            ASTPtr window_expr = std::const_pointer_cast<IAST>(window_func.function_node->shared_from_this());
            mappings.emplace(window_expr, window_func.column_name);
        }

        auto window_step = std::make_shared<WindowStep>(builder.getCurrentDataStream(), window_desc, true);
        builder.addStep(std::move(window_step));
        builder.withAdditionalMappings(mappings);
        PRINT_PLAN(builder.plan, plan_window);
    }
}

void QueryPlannerVisitor::planSelect(PlanBuilder & builder, ASTSelectQuery & select_query)
{
    auto & select_expressions = analysis.getSelectExpressions(select_query);
    planSubqueryExpression(builder, select_query, select_expressions);
    builder.appendProjection(select_expressions);

    // if order by scope exists, update scope & field_symbols
    if (const auto * order_by_scope = analysis.getScope(select_query); order_by_scope != builder.getScope())
    {
        FieldSymbolInfos field_symbols;
        append(field_symbols, builder.getFieldSymbolInfos());
        append(field_symbols, select_expressions, [&](ASTPtr & expr) -> FieldSymbolInfo
               {
                   if (auto col_ref = analysis.tryGetColumnReference(expr))
                       return builder.getGlobalFieldSymbolInfo(*col_ref);
                   else
                       return {builder.translateToSymbol(expr)};
               });
        builder.withScope(order_by_scope, field_symbols, false);
    }
}

void QueryPlannerVisitor::planDistinct(PlanBuilder & builder, ASTSelectQuery & select_query)
{
    if (!select_query.distinct)
        return;

    auto & select_expressions = analysis.getSelectExpressions(select_query);
    UInt64 limit_for_distinct = 0;

    auto distinct_step = std::make_shared<DistinctStep>(
        builder.getCurrentDataStream(),
        extractDistinctSizeLimits(),
        limit_for_distinct,
        builder.translateToSymbols(select_expressions),
        true);

    builder.addStep(std::move(distinct_step));
    PRINT_PLAN(builder.plan, plan_distinct);
}

void QueryPlannerVisitor::planOrderBy(PlanBuilder & builder, ASTSelectQuery & select_query)
{
    if (!select_query.orderBy())
        return;

    auto & order_by_analysis = analysis.getOrderByAnalysis(select_query);

    // project sort by keys
    ASTs sort_expressions;
    for (auto & order_by_item: order_by_analysis)
        sort_expressions.emplace_back(order_by_item->children.front());
    planSubqueryExpression(builder, select_query, sort_expressions);
    builder.appendProjection(sort_expressions);
    PRINT_PLAN(builder.plan, plan_prepare_order_by);

    // build sort description
    SortDescription sort_description;
    for (auto & order_by_item: order_by_analysis)
    {
        String sort_symbol = builder.translateToSymbol(order_by_item->children.front());
        std::shared_ptr<Collator> collator;

        if (order_by_item->collation)
            collator = std::make_shared<Collator>(order_by_item->collation->as<ASTLiteral &>().value.get<String>());

        sort_description.emplace_back(sort_symbol, order_by_item->direction, order_by_item->nulls_direction, collator);
    }

    // collect limit hint
    UInt64 limit = 0;

    if (!select_query.distinct && !select_query.limitBy())
    {
        auto [limit_length, limit_offset] = getLimitLengthAndOffset(select_query);
        limit = limit_length + limit_offset;
    }

    auto sorting_step = std::make_shared<SortingStep>(builder.getCurrentDataStream(), sort_description, limit, false);
    builder.addStep(std::move(sorting_step));
    PRINT_PLAN(builder.plan, plan_order_by);
}

void QueryPlannerVisitor::planLimitBy(PlanBuilder & builder, ASTSelectQuery & select_query)
{
    if (!select_query.limitBy())
        return;

    // project limit by keys
    ASTs & limit_by_expressions = select_query.limitBy()->children;
    planSubqueryExpression(builder, select_query, limit_by_expressions);
    builder.appendProjection(limit_by_expressions);
    PRINT_PLAN(builder.plan, plan_prepare_limit_by);

    // plan limit by node
    auto step = std::make_shared<LimitByStep>(
                    builder.getCurrentDataStream(),
                    analysis.getLimitByValue(select_query),
                    0, // TODO
                    builder.translateToUniqueSymbols(limit_by_expressions));
    builder.addStep(std::move(step));
    PRINT_PLAN(builder.plan, plan_limit_by);
}

RelationPlan QueryPlannerVisitor::projectFieldSymbols(const RelationPlan & plan)
{
    const auto & old_root = plan.getRoot();
    const auto & old_mappings = plan.getFieldSymbolInfos();
    PlanNodePtr new_root;
    FieldSymbolInfos new_mappings;

    {
        Assignments assignments;
        NameToType input_types = old_root->getOutputNamesToTypes();
        NameToType output_types;

        for (const auto & input_symbol_info: old_mappings)
        {
            const auto & field_symbol = input_symbol_info.getPrimarySymbol();
            assignments.emplace_back(field_symbol, toSymbolRef(field_symbol));
            output_types[field_symbol] = input_types.at(field_symbol);
            new_mappings.emplace_back(field_symbol);
        }

        auto project_step = std::make_shared<ProjectionStep>(old_root->getCurrentDataStream(), assignments, output_types);
        new_root = old_root->addStep(context->nextNodeId(), std::move(project_step));
    }

    return { new_root, new_mappings };
}

static bool hasWithTotalsInAnySubqueryInFromClause(const ASTSelectQuery & query)
{
    if (query.group_by_with_totals)
        return true;

    /** NOTE You can also check that the table in the subquery is distributed, and that it only looks at one shard.
     * In other cases, totals will be computed on the initiating server of the query, and it is not necessary to read the data to the end.
     */
    if (auto query_table = extractTableExpression(query, 0))
    {
        if (const auto * ast_union = query_table->as<ASTSelectWithUnionQuery>())
        {
            /** NOTE
            * 1. For ASTSelectWithUnionQuery after normalization for union child node the height of the AST tree is at most 2.
            * 2. For ASTSelectIntersectExceptQuery after normalization in case there are intersect or except nodes,
            * the height of the AST tree can have any depth (each intersect/except adds a level), but the
            * number of children in those nodes is always 2.
            */
            std::function<bool(ASTPtr)> traverse_recursively = [&](ASTPtr child_ast) -> bool
            {
                if (const auto * select_child = child_ast->as <ASTSelectQuery>())
                {
                    if (hasWithTotalsInAnySubqueryInFromClause(select_child->as<ASTSelectQuery &>()))
                        return true;
                }
                else if (const auto * union_child = child_ast->as<ASTSelectWithUnionQuery>())
                {
                    for (const auto & subchild : union_child->list_of_selects->children)
                        if (traverse_recursively(subchild))
                            return true;
                }
                else if (const auto * intersect_child = child_ast->as<ASTSelectIntersectExceptQuery>())
                {
                    auto selects = intersect_child->getListOfSelects();
                    for (const auto & subchild : selects)
                        if (traverse_recursively(subchild))
                            return true;
                }
                return false;
            };

            for (const auto & elem : ast_union->list_of_selects->children)
                if (traverse_recursively(elem))
                    return true;
        }
    }

    return false;
}

void QueryPlannerVisitor::planLimit(PlanBuilder & builder, ASTSelectQuery & select_query)
{
    if (select_query.limitLength())
    {
        /** Rare case:
          *  if there is no WITH TOTALS and there is a subquery in FROM, and there is WITH TOTALS on one of the levels,
          *  then when using LIMIT, you should read the data to the end, rather than cancel the query earlier,
          *  because if you cancel the query, we will not get `totals` data from the remote server.
          *
          * Another case:
          *  if there is WITH TOTALS and there is no ORDER BY, then read the data to the end,
          *  otherwise TOTALS is counted according to incomplete data.
          */
        bool always_read_till_end = false;

        if (select_query.group_by_with_totals && !select_query.orderBy())
            always_read_till_end = true;

        if (!select_query.group_by_with_totals && hasWithTotalsInAnySubqueryInFromClause(select_query))
            always_read_till_end = true;

        UInt64 limit_length;
        UInt64 limit_offset;
        std::tie(limit_length, limit_offset) = getLimitLengthAndOffset(select_query);
        auto step = std::make_shared<LimitStep>(builder.getCurrentDataStream(), limit_length, limit_offset, always_read_till_end);
        builder.addStep(std::move(step));
    }
}

/*
PlanNodePtr QueryPlannerVisitor::planSampling(PlanNodePtr plan, ASTSelectQuery & select_query)
{
    if (select_query.sample_size() && context->getSettingsRef().enable_final_sample)
    {
        ASTSampleRatio * sample = select_query.sample_size()->as<ASTSampleRatio>();
        ASTSampleRatio::BigNum numerator = sample->ratio.numerator;
        ASTSampleRatio::BigNum denominator = sample->ratio.denominator;
        if (numerator <= 1 || denominator > 1)
            return plan;

        auto step = std::make_shared<FinalSamplingStep>(plan->getCurrentDataStream(), numerator);
        plan = plan->addStep(context->nextNodeId(), std::move(step));
        PRINT_PLAN(plan, plan_sampling);
    }
    return plan;
}
*/

RelationPlan QueryPlannerVisitor::planFinalSelect(PlanBuilder & builder, ASTSelectQuery & select_query)
{
    const auto & select_expressions = analysis.getSelectExpressions(select_query);
    FieldSymbolInfos field_symbol_infos;

    Assignments assignments;
    NameToType input_types = builder.getOutputNamesToTypes();
    NameToType output_types;
    NameSet existing_symbols;

    // select_column has 2 effects:
    //   1. prune irrelevant columns
    //   2. duplicate column for same expression
    auto select_column = [&](const auto & input_symbol)
    {
        String output_symbol = input_symbol;
        if (existing_symbols.find(output_symbol) != existing_symbols.end())
            output_symbol = context->getSymbolAllocator()->newSymbol(output_symbol);

        assignments.emplace_back(output_symbol, toSymbolRef(input_symbol));
        if (!input_types.contains(input_symbol))
            throw Exception("Unknown column " + input_symbol, ErrorCodes::PLAN_BUILD_ERROR);
        output_types[output_symbol] = input_types.at(input_symbol);
        existing_symbols.insert(output_symbol);

        return output_symbol;
    };

    for (const auto & select_expr: select_expressions)
    {
        FieldSymbolInfo field_symbol_info;

        if (auto column_reference = analysis.tryGetColumnReference(select_expr))
        {
            field_symbol_info = builder.getGlobalFieldSymbolInfo(*column_reference);
        }
        else
        {
            field_symbol_info = {builder.translateToSymbol(select_expr)};
        }

        field_symbol_info.primary_symbol = select_column(field_symbol_info.primary_symbol);
        auto & sub_column_symbols = field_symbol_info.sub_column_symbols;

        for (auto & sub_col: sub_column_symbols)
        {
            sub_column_symbols[sub_col.first] = select_column(sub_col.second);
        }

        field_symbol_infos.push_back(field_symbol_info);
    }

    auto project = std::make_shared<ProjectionStep>(builder.getCurrentDataStream(), assignments, output_types);
    builder.addStep(std::move(project));

    return { builder.getRoot(), field_symbol_infos };
}

namespace
{

    template <typename UserContext>
    class ExtractSubqueryTraversalVisitor: public ExpressionTraversalVisitor<UserContext>
    {
    public:
        using ExpressionTraversalIncludeSubqueryVisitor<UserContext>::process;

        ExtractSubqueryTraversalVisitor(ExpressionVisitor<UserContext> & user_visitor_, UserContext & user_context_, Analysis & analysis_,
                                        ContextPtr context_, PlanBuilder & plan_builder_)
            : ExpressionTraversalVisitor<UserContext>(user_visitor_, user_context_, analysis_, context_), plan_builder(plan_builder_)
        {}

        void process(ASTPtr & node, const Void & traversal_context) override
        {
            // don't go through planned expresions(e.g. aggregate when planAggregate has been executed)
            if (plan_builder.canTranslateToSymbol(node))
                return;

            ExpressionTraversalVisitor<UserContext>::process(node, traversal_context);
        }

    private:
        PlanBuilder & plan_builder;
    };

    struct ExtractSubqueryVisitor: public ExpressionVisitor<const Void>
    {
    protected:
        void visitExpression(ASTPtr &, IAST &, const Void &) override {}

        void visitScalarSubquery(ASTPtr & node, ASTSubquery &, const Void &) override
        {
            scalar_subqueries.push_back(node);
        }

        void visitInSubquery(ASTPtr & node, ASTFunction &, const Void &) override
        {
            in_subqueries.push_back(node);
        }

        void visitExistsSubquery(ASTPtr & node, ASTFunction &, const Void &) override
        {
            exists_subqueries.push_back(node);
        }

        void visitQuantifiedComparisonSubquery(ASTPtr & node, ASTQuantifiedComparison & , const Void &) override
        {
            quantified_comparison_subqueries.push_back(node);
        }

    public:
        using ExpressionVisitor<const Void>::ExpressionVisitor;

        std::vector<ASTPtr> scalar_subqueries;
        std::vector<ASTPtr> in_subqueries;
        std::vector<ASTPtr> exists_subqueries;
        std::vector<ASTPtr> quantified_comparison_subqueries;
    };

}

void QueryPlannerVisitor::planSubqueryExpression(PlanBuilder & builder, ASTSelectQuery & /*select_query*/, ASTPtr root)
{
    ExtractSubqueryVisitor extract_visitor {context};
    ExtractSubqueryTraversalVisitor traversal_visitor {extract_visitor, {}, analysis, context, builder};
    traversal_visitor.process(root);

    for (auto & scalar_subquery: extract_visitor.scalar_subqueries)
        planScalarSubquery(builder, scalar_subquery);
    for (auto & in_subquery: extract_visitor.in_subqueries)
        planInSubquery(builder, in_subquery);
    for (auto & exists_subquery: extract_visitor.exists_subqueries)
        planExistsSubquery(builder, exists_subquery);
    for (auto & quantified_comparison_subquery : extract_visitor.quantified_comparison_subqueries)
        planQuantifiedComparisonSubquery(builder, quantified_comparison_subquery);
}

void QueryPlannerVisitor::planScalarSubquery(PlanBuilder & builder, const ASTPtr & scalar_subquery)
{
    // filter out planned subqueries
    if (builder.canTranslateToSymbol(scalar_subquery))
        return;

    auto subquery_plan = QueryPlanner::planQuery(scalar_subquery, builder.translation, analysis, context, cte_plans);

    subquery_plan = combineSubqueryOutputsToTuple(subquery_plan, scalar_subquery);

    if (auto coerced_type = analysis.getTypeCoercion(scalar_subquery))
    {
        subquery_plan = coerceTypeForSubquery(subquery_plan, coerced_type);
    }

    // Add EnforceSingleRow Step
    auto enforce_single_row_step = std::make_shared<EnforceSingleRowStep>(subquery_plan.getRoot()->getCurrentDataStream());
    auto enforce_single_row_node = subquery_plan.getRoot()->addStep(context->nextNodeId(), std::move(enforce_single_row_step));
    subquery_plan = subquery_plan.withNewRoot(enforce_single_row_node);

    // Add Apply Step
    String subquery_output_symbol = subquery_plan.getFirstPrimarySymbol();
    //String apply_output_symbol = context.getSymbolAllocator()->newSymbol("_scalar_subquery");
    Assignment scalar_assignment {subquery_output_symbol, toSymbolRef(subquery_output_symbol)};

    auto apply_step = std::make_shared<ApplyStep>(
                        DataStreams {builder.getCurrentDataStream(), subquery_plan.getRoot()->getCurrentDataStream()},
                        builder.getOutputNames(),
                        ApplyStep::ApplyType::CROSS,
                        ApplyStep::SubqueryType::SCALAR,
                        scalar_assignment);
    builder.addStep(std::move(apply_step), {builder.getRoot(), subquery_plan.getRoot()});
    builder.withAdditionalMapping(scalar_subquery, subquery_output_symbol);
    PRINT_PLAN(builder.plan, plan_scalar_subquery);
}

void QueryPlannerVisitor::planInSubquery(PlanBuilder & builder, const ASTPtr & node)
{
    // filter out planned subqueries
    if (builder.canTranslateToSymbol(node))
        return;

    auto & function = node->as<ASTFunction &>();

    //process two children of function
    RelationPlan rhs_plan;
    String rhs_symbol, lhs_symbol;
    processSubqueryArgs(builder, function.arguments->children, rhs_symbol, lhs_symbol, rhs_plan);

    // Add Apply Step
    String apply_output_symbol = context->getSymbolAllocator()->newSymbol("_in_subquery");
    Assignment in_assignment{
        apply_output_symbol,
        makeASTFunction(
            function.name, ASTs{toSymbolRef(lhs_symbol), toSymbolRef(rhs_symbol)})};

    auto apply_step = std::make_shared<ApplyStep>(
                        DataStreams {builder.getCurrentDataStream(), rhs_plan.getRoot()->getCurrentDataStream()},
                        builder.getOutputNames(),
                        ApplyStep::ApplyType::CROSS,
                        ApplyStep::SubqueryType::IN,
                        in_assignment);

    builder.addStep(std::move(apply_step), {builder.getRoot(), rhs_plan.getRoot()});
    builder.withAdditionalMapping(node, apply_output_symbol);
    PRINT_PLAN(builder.plan, plan_in_subquery);
}

void QueryPlannerVisitor::planExistsSubquery(PlanBuilder & builder, const ASTPtr & node)
{
    // filter out planned subqueries
    if (builder.canTranslateToSymbol(node))
        return;

    auto exists_subquery = node->as<ASTFunction &>();
    auto subquery_plan = QueryPlanner::planQuery(exists_subquery.arguments->children.at(0),
                                                 builder.translation,
                                                 analysis,
                                                 context,
                                                 cte_plans);
    // Add Projection Step
    {
        const auto & output_name = subquery_plan.getFirstPrimarySymbol();
        Assignment assignment{output_name, toSymbolRef(output_name)};
        Assignments assignments{assignment};
        NameToType types;
        types[output_name] = subquery_plan.getRoot()->getOutputNamesToTypes().at(output_name);

        auto expression_step = std::make_shared<ProjectionStep>(subquery_plan.getRoot()->getCurrentDataStream(), assignments, types);
        auto expression_node = subquery_plan.getRoot()->addStep(context->nextNodeId(), std::move(expression_step));
        subquery_plan = {expression_node, FieldSymbolInfos {{output_name}}};
    }

    // Add Apply Step
    String apply_output_symbol = context->getSymbolAllocator()->newSymbol("_exists_subquery");
    Assignment exist_assignment{apply_output_symbol, std::make_shared<ASTLiteral>(true)};
    auto apply_step = std::make_shared<ApplyStep>(
                        DataStreams {builder.getCurrentDataStream(), subquery_plan.getRoot()->getCurrentDataStream()},
                        builder.getOutputNames(),
                        ApplyStep::ApplyType::CROSS,
                        ApplyStep::SubqueryType::EXISTS,
                        exist_assignment);

    builder.addStep(std::move(apply_step), {builder.getRoot(), subquery_plan.getRoot()});
    builder.withAdditionalMapping(node, apply_output_symbol);
    PRINT_PLAN(builder.plan, plan_exists_subquery);
}

void QueryPlannerVisitor::planQuantifiedComparisonSubquery(PlanBuilder & builder, const ASTPtr & node)
{
    if (builder.canTranslateToSymbol(node))
        return;

    auto & quantified_comparison = node->as<ASTQuantifiedComparison &>();

    //process two children of quantified_comparison
    RelationPlan rhs_plan;
    String rhs_symbol, lhs_symbol;
    processSubqueryArgs(builder, quantified_comparison.children, rhs_symbol, lhs_symbol, rhs_plan);

    String apply_output_symbol;
    std::shared_ptr<ApplyStep> apply_step;
    // A = ANY B <=> A IN B
    // A <> ALL B <=> (A notIn B)
    bool comparator_is_equals = quantified_comparison.comparator == "equals";
    bool quantifier_is_all = quantified_comparison.quantifier_type == QuantifierType::ALL;
    bool comparator_is_range_op = quantified_comparison.comparator != "equals" && quantified_comparison.comparator != "notEquals";
    if (comparator_is_range_op || comparator_is_equals == quantifier_is_all)
    {
        apply_output_symbol = context->getSymbolAllocator()->newSymbol("_quantified_comparison_subquery");
        Assignment quantified_comparison_assignment{
            apply_output_symbol,
            makeASTQuantifiedComparison(quantified_comparison.comparator, quantified_comparison.quantifier_type,
                                        ASTs{toSymbolRef(lhs_symbol), toSymbolRef(rhs_symbol)})};

        apply_step = std::make_shared<ApplyStep>(
            DataStreams {builder.getCurrentDataStream(), rhs_plan.getRoot()->getCurrentDataStream()},
            builder.getOutputNames(),
            ApplyStep::ApplyType::CROSS,
            ApplyStep::SubqueryType::QUANTIFIED_COMPARISON,
            quantified_comparison_assignment);
    }
    else
    {
        String function_name = "in";
        if (!comparator_is_equals)
            function_name = "notIn";
        apply_output_symbol = context->getSymbolAllocator()->newSymbol("_in_subquery");
        Assignment in_assignment{
            apply_output_symbol,
            makeASTFunction(
                function_name, ASTs{toSymbolRef(lhs_symbol), toSymbolRef(rhs_symbol)})};
        apply_step = std::make_shared<ApplyStep>(
            DataStreams {builder.getCurrentDataStream(), rhs_plan.getRoot()->getCurrentDataStream()},
            builder.getOutputNames(),
            ApplyStep::ApplyType::CROSS,
            ApplyStep::SubqueryType::IN,
            in_assignment);
    }

    builder.addStep(std::move(apply_step), {builder.getRoot(), rhs_plan.getRoot()});
    builder.withAdditionalMapping(node, apply_output_symbol);
    PRINT_PLAN(builder.plan, plan_quantified_comparison_subquery);
}

RelationPlan QueryPlannerVisitor::combineSubqueryOutputsToTuple(const RelationPlan & plan, const ASTPtr & subquery)
{
    const auto & outputs = plan.getFieldSymbolInfos();

    if (outputs.size() > 1)
    {
        ASTs tuple_func_args(outputs.size());
        std::transform(outputs.begin(), outputs.end(), tuple_func_args.begin(), [](auto & out) {return toSymbolRef(out.getPrimarySymbol());});

        auto tuple_func_expr = makeASTFunction("tuple", std::move(tuple_func_args));
        auto tuple_func_symbol = context->getSymbolAllocator()->newSymbol(tuple_func_expr);
        Assignments assignments{{tuple_func_symbol, tuple_func_expr}};
        NameToType types {{tuple_func_symbol, analysis.getExpressionType(subquery)}};
        auto old_root = plan.getRoot();
        auto expression_step = std::make_shared<ProjectionStep>(old_root->getCurrentDataStream(), assignments, types);
        auto new_root = old_root->addStep(context->nextNodeId(), std::move(expression_step));

        return {new_root, FieldSymbolInfos{{tuple_func_symbol}}};
    }
    return plan;
}

RelationPlan QueryPlannerVisitor::planSetOperation(ASTs & selects, ASTSelectWithUnionQuery::Mode union_mode)
{
    RelationPlans sub_plans;

    // 1. plan set element
    for (auto & select: selects)
        sub_plans.push_back(process(select));

    if (sub_plans.empty())
        throw Exception(ErrorCodes::LOGICAL_ERROR, "Set operation must have at least 1 element");

    if (sub_plans.size() == 1)
        return sub_plans.front();

    // 2. prepare sub plan & collect input info
    DataStreams input_streams;
    PlanNodes source_nodes;

    for (size_t select_id = 0; select_id < selects.size(); ++select_id)
    {
        auto & select = selects[select_id];
        auto & sub_plan = sub_plans[select_id];
        // prune invisible columns, copy duplicated columns
        sub_plan = projectFieldSymbols(sub_plan);

        // coerce to common type
        if (enable_implicit_type_conversion && analysis.hasRelationTypeCoercion(*select))
        {
            const auto & field_symbol_infos = sub_plan.getFieldSymbolInfos();
            const auto & target_types = analysis.getRelationTypeCoercion(*select);
            assert(target_types.size() == field_symbol_infos.size());
            NameToType symbols_and_types;

            for (size_t i = 0; i < target_types.size(); ++i)
            {
                auto target_type = target_types[i];
                if (target_type)
                    symbols_and_types.emplace(field_symbol_infos[i].getPrimarySymbol(), target_type);
            }

            auto coerced_plan = coerceTypesForSymbols(sub_plan.getRoot(), symbols_and_types, true);
            sub_plan = RelationPlan {coerced_plan.plan, field_symbol_infos};
        }

        source_nodes.push_back(sub_plan.getRoot());
        input_streams.push_back(sub_plan.getRoot()->getCurrentDataStream());
    }

    // 3. build output info
    DataStream output_stream;
    FieldSymbolInfos field_symbols;
    auto & first_sub_plan = sub_plans[0];

    for (auto & col : first_sub_plan.getRoot()->getOutputNamesAndTypes())
    {
        auto new_name = context->getSymbolAllocator()->newSymbol(col.name);
        output_stream.header.insert(ColumnWithTypeAndName{col.type, new_name});
        field_symbols.emplace_back(new_name);
    }

    // 4. build step
    QueryPlanStepPtr set_operation_step;
    switch (union_mode)
    {
        case ASTSelectWithUnionQuery::Mode::ALL:
        case ASTSelectWithUnionQuery::Mode::DISTINCT:
            set_operation_step = std::make_shared<UnionStep>(input_streams, output_stream, false);
            break;
        case ASTSelectWithUnionQuery::Mode::INTERSECT_ALL:
            set_operation_step = std::make_shared<IntersectStep>(input_streams, output_stream, false);
            break;
        case ASTSelectWithUnionQuery::Mode::INTERSECT_DISTINCT:
            set_operation_step = std::make_shared<IntersectStep>(input_streams, output_stream, true);
            break;
        case ASTSelectWithUnionQuery::Mode::EXCEPT_ALL:
            set_operation_step = std::make_shared<ExceptStep>(input_streams, output_stream, false);
            break;
        case ASTSelectWithUnionQuery::Mode::EXCEPT_DISTINCT:
            set_operation_step = std::make_shared<ExceptStep>(input_streams, output_stream, true);
            break;
        default:
            throw Exception("Unsupported union mode: " + std::to_string(static_cast<UInt8>(union_mode)), ErrorCodes::PLAN_BUILD_ERROR);
    }

    auto set_operation_node = first_sub_plan.getRoot()->addStep(context->nextNodeId(), std::move(set_operation_step), source_nodes);

    if (union_mode == ASTSelectWithUnionQuery::Mode::DISTINCT)
    {
        auto distinct_step = std::make_shared<DistinctStep>(
            set_operation_node->getCurrentDataStream(),
            extractDistinctSizeLimits(),
            0,
            set_operation_node->getOutputNames(),
            true);

        auto distinct_node = set_operation_node->addStep(context->nextNodeId(), std::move(distinct_step));

        return {distinct_node, field_symbols};
    }

    return {set_operation_node, field_symbols};
}

PlanWithSymbolMappings QueryPlannerVisitor::coerceTypesForSymbols(const PlanNodePtr & node, const NameToType & symbol_and_types, bool replace_symbol)
{
    Assignments assignments;
    NameToType output_types;
    NameToNameMap symbol_mappings;

    for (const auto & input_symbol_and_type : node->getCurrentDataStream().header)
    {
        const auto & input_symbol = input_symbol_and_type.name;
        const auto & input_type = input_symbol_and_type.type;

        if (auto it = symbol_and_types.find(input_symbol); it != symbol_and_types.end() && it->second)
        {
            const auto & output_type = it->second;
            String output_symbol = input_symbol;

            if (!replace_symbol)
            {
                output_symbol = context->getSymbolAllocator()->newSymbol(input_symbol);
                assignments.emplace_back(input_symbol, toSymbolRef(input_symbol));
                output_types[input_symbol] = input_type;
                symbol_mappings.emplace(input_symbol, output_symbol);
            }

            assignments.emplace_back(output_symbol, makeCastFunction(toSymbolRef(input_symbol), output_type));
            output_types[output_symbol] = output_type;
        }
        else
        {
            assignments.emplace_back(input_symbol, toSymbolRef(input_symbol));
            output_types[input_symbol] = input_type;
        }
    }

    auto casting_step = std::make_shared<ProjectionStep>(node->getCurrentDataStream(), assignments, output_types);
    auto casting_node = node->addStep(context->nextNodeId(), std::move(casting_step));

    return {casting_node, symbol_mappings};
}

NameToNameMap QueryPlannerVisitor::coerceTypesForSymbols(PlanBuilder & builder, const NameToType & symbol_and_types, bool replace_symbol)
{
    auto plan_with_mapping = coerceTypesForSymbols(builder.getRoot(), symbol_and_types, replace_symbol);
    builder.withNewRoot(plan_with_mapping.plan);
    return plan_with_mapping.mappings;
}

RelationPlan QueryPlannerVisitor::coerceTypeForSubquery(const RelationPlan & plan, const DataTypePtr & type)
{
    NameToType symbol_and_types {{plan.getFirstPrimarySymbol(), type}};
    auto plan_with_mapping = coerceTypesForSymbols(plan.getRoot(), symbol_and_types, true);
    return plan.withNewRoot(plan_with_mapping.plan);
}

PlanWithSymbols QueryPlannerVisitor::projectExpressionsWithCoercion(const PlanNodePtr & node, const TranslationMapPtr & translation,
                                                                    const ExpressionsAndTypes & expression_and_types)
{
    Assignments assignments;
    NameToType output_types;
    Names output_symbols;

    putIdentities(node->getCurrentDataStream().header, assignments, output_types);

    for (const auto & [expr, cast_type]: expression_and_types)
    {
        auto rewritten_expr = translation->translate(expr);

        // if an expression has been translated and no type coercion happens, just skip it
        if (!cast_type && rewritten_expr->as<ASTIdentifier>())
        {
            output_symbols.push_back(rewritten_expr->as<ASTIdentifier>()->name());
            continue;
        }

        if (cast_type)
        {
            rewritten_expr = makeCastFunction(rewritten_expr, cast_type);
        }

        auto output_symbol = context->getSymbolAllocator()->newSymbol(rewritten_expr);

        assignments.emplace_back(output_symbol, rewritten_expr);
        output_types[output_symbol] = cast_type ? cast_type : analysis.getExpressionType(expr);
        output_symbols.push_back(output_symbol);
    }

    auto casting_step = std::make_shared<ProjectionStep>(node->getCurrentDataStream(), assignments, output_types);
    auto casting_node = node->addStep(context->nextNodeId(), std::move(casting_step));

    return {casting_node, output_symbols};
}

Names QueryPlannerVisitor::projectExpressionsWithCoercion(PlanBuilder & builder, const ExpressionsAndTypes & expression_and_types)
{
    auto plan_with_symbols = projectExpressionsWithCoercion(builder.getRoot(), builder.getTranslation(), expression_and_types);
    builder.withNewRoot(plan_with_symbols.plan);
    return plan_with_symbols.symbols;
}

SizeLimits QueryPlannerVisitor::extractDistinctSizeLimits()
{
    const auto & settings = context->getSettingsRef();
    return {settings.max_rows_in_distinct, settings.max_bytes_in_distinct, settings.distinct_overflow_mode};
}

PlanBuilder QueryPlannerVisitor::toPlanBuilder(const RelationPlan & plan, ScopePtr scope)
{
    auto translation_map = std::make_shared<TranslationMap>(outer_context, scope, plan.field_symbol_infos, analysis, context);
    return {analysis, context->getPlanNodeIdAllocator(), context->getSymbolAllocator(), plan.root, translation_map};
}
std::pair<UInt64, UInt64> QueryPlannerVisitor::getLimitLengthAndOffset(ASTSelectQuery & query)
{
    UInt64 length = 0;
    UInt64 offset = 0;

    if (query.limitLength())
    {
        length = analysis.getLimitLength(query);
        if (query.limitOffset())
            offset = analysis.getLimitOffset(query);
    }

    return {length, offset};
}

void QueryPlannerVisitor::processSubqueryArgs(PlanBuilder & builder, ASTs & children, String & rhs_symbol, String & lhs_symbol, RelationPlan & rhs_plan)
{
    //process lhs
    auto & lhs_ast = children.at(0);
    builder.appendProjection(lhs_ast);
    lhs_symbol = builder.translateToSymbol(lhs_ast);

    if (auto coerced_type = analysis.getTypeCoercion(lhs_ast))
    {
        auto mapping = coerceTypesForSymbols(builder, {{lhs_symbol, coerced_type}}, false);
        lhs_symbol = mapping.at(lhs_symbol);
    }

    // process rhs
    auto & rhs_ast = children.at(1);
    rhs_plan = QueryPlanner::planQuery(rhs_ast, builder.translation, analysis, context, cte_plans);
    rhs_plan = combineSubqueryOutputsToTuple(rhs_plan, rhs_ast);

    if (auto coerced_type = analysis.getTypeCoercion(rhs_ast))
    {
        rhs_plan = coerceTypeForSubquery(rhs_plan, coerced_type);
    }
    rhs_symbol = rhs_plan.getFirstPrimarySymbol();
}

}
