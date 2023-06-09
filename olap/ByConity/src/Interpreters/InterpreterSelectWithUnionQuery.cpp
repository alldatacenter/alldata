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

#include <Columns/getLeastSuperColumn.h>
#include <Interpreters/Context.h>
#include <Interpreters/InterpreterSelectQuery.h>
#include <Interpreters/InterpreterSelectWithUnionQuery.h>
#include <Parsers/ASTSelectQuery.h>
#include <Parsers/ASTSelectWithUnionQuery.h>
#include <Parsers/queryToString.h>
#include <Parsers/ASTTEALimit.h>
#include <Parsers/ASTOrderByElement.h>
#include <Parsers/ASTSubquery.h>
#include <Processors/Executors/PullingAsyncPipelineExecutor.h>

#include <Storages/StorageMemory.h>

#include <QueryPlan/DistinctStep.h>
#include <QueryPlan/ExpressionStep.h>
#include <QueryPlan/IQueryPlanStep.h>
#include <QueryPlan/QueryPlan.h>
#include <QueryPlan/UnionStep.h>
#include <QueryPlan/LimitStep.h>
#include <QueryPlan/OffsetStep.h>
#include <QueryPlan/Optimizations/QueryPlanOptimizationSettings.h>
#include <Common/typeid_cast.h>

#include <Interpreters/InDepthNodeVisitor.h>
#include <Interpreters/executeQuery.h>

#include <algorithm>

namespace DB
{

namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int UNION_ALL_RESULT_STRUCTURES_MISMATCH;
}

InterpreterSelectWithUnionQuery::InterpreterSelectWithUnionQuery(
        const ASTPtr & query_ptr_, ContextPtr context_,
        const SelectQueryOptions & options_, const Names & required_result_column_names)
    : IInterpreterUnionOrSelectQuery(query_ptr_, context_, options_)
{
    ASTSelectWithUnionQuery * ast = query_ptr->as<ASTSelectWithUnionQuery>();
    bool require_full_header = ast->hasNonDefaultUnionMode();

    const Settings & settings = context->getSettingsRef();
    if (options.subquery_depth == 0 && (settings.limit > 0 || settings.offset > 0))
        settings_limit_offset_needed = true;

    size_t num_children = ast->list_of_selects->children.size();
    if (!num_children)
        throw Exception("Logical error: no children in ASTSelectWithUnionQuery", ErrorCodes::LOGICAL_ERROR);

    /// Note that we pass 'required_result_column_names' to first SELECT.
    /// And for the rest, we pass names at the corresponding positions of 'required_result_column_names' in the result of first SELECT,
    ///  because names could be different.

    nested_interpreters.reserve(num_children);
    std::vector<Names> required_result_column_names_for_other_selects(num_children);

    if (!require_full_header && !required_result_column_names.empty() && num_children > 1)
    {
        /// Result header if there are no filtering by 'required_result_column_names'.
        /// We use it to determine positions of 'required_result_column_names' in SELECT clause.

        Block full_result_header = getCurrentChildResultHeader(ast->list_of_selects->children.at(0), required_result_column_names);

        std::vector<size_t> positions_of_required_result_columns(required_result_column_names.size());

        for (size_t required_result_num = 0, size = required_result_column_names.size(); required_result_num < size; ++required_result_num)
            positions_of_required_result_columns[required_result_num] = full_result_header.getPositionByName(required_result_column_names[required_result_num]);

        for (size_t query_num = 1; query_num < num_children; ++query_num)
        {
            Block full_result_header_for_current_select
                = getCurrentChildResultHeader(ast->list_of_selects->children.at(query_num), required_result_column_names);

            if (full_result_header_for_current_select.columns() != full_result_header.columns())
                throw Exception("Different number of columns in UNION ALL elements:\n"
                    + full_result_header.dumpNames()
                    + "\nand\n"
                    + full_result_header_for_current_select.dumpNames() + "\n",
                    ErrorCodes::UNION_ALL_RESULT_STRUCTURES_MISMATCH);

            required_result_column_names_for_other_selects[query_num].reserve(required_result_column_names.size());
            for (const auto & pos : positions_of_required_result_columns)
                required_result_column_names_for_other_selects[query_num].push_back(full_result_header_for_current_select.getByPosition(pos).name);
        }
    }

    if (num_children == 1 && settings_limit_offset_needed)
    {
        const ASTPtr first_select_ast = ast->list_of_selects->children.at(0);
        ASTSelectQuery * select_query = first_select_ast->as<ASTSelectQuery>();

        if (!select_query->withFill() && !select_query->limit_with_ties)
        {
            UInt64 limit_length = 0;
            UInt64 limit_offset = 0;

            const ASTPtr limit_offset_ast = select_query->limitOffset();
            if (limit_offset_ast)
            {
                limit_offset = limit_offset_ast->as<ASTLiteral &>().value.safeGet<UInt64>();
                UInt64 new_limit_offset = settings.offset + limit_offset;
                limit_offset_ast->as<ASTLiteral &>().value = Field(new_limit_offset);
            }
            else if (settings.offset)
            {
                ASTPtr new_limit_offset_ast = std::make_shared<ASTLiteral>(Field(UInt64(settings.offset)));
                select_query->setExpression(ASTSelectQuery::Expression::LIMIT_OFFSET, std::move(new_limit_offset_ast));
            }

            const ASTPtr limit_length_ast = select_query->limitLength();
            if (limit_length_ast)
            {
                limit_length = limit_length_ast->as<ASTLiteral &>().value.safeGet<UInt64>();

                UInt64 new_limit_length = 0;
                if (settings.offset == 0)
                    new_limit_length = std::min(limit_length, UInt64(settings.limit));
                else if (settings.offset < limit_length)
                    new_limit_length =  settings.limit ? std::min(UInt64(settings.limit), limit_length - settings.offset) : (limit_length - settings.offset);

                limit_length_ast->as<ASTLiteral &>().value = Field(new_limit_length);
            }
            else if (settings.limit)
            {
                ASTPtr new_limit_length_ast = std::make_shared<ASTLiteral>(Field(UInt64(settings.limit)));
                select_query->setExpression(ASTSelectQuery::Expression::LIMIT_LENGTH, std::move(new_limit_length_ast));
            }

            settings_limit_offset_done = true;
        }
    }

    for (size_t query_num = 0; query_num < num_children; ++query_num)
    {
        const Names & current_required_result_column_names
            = query_num == 0 ? required_result_column_names : required_result_column_names_for_other_selects[query_num];

        nested_interpreters.emplace_back(
            buildCurrentChildInterpreter(ast->list_of_selects->children.at(query_num), require_full_header ? Names() : current_required_result_column_names));
    }

    /// Determine structure of the result.

    if (num_children == 1)
    {
        result_header = nested_interpreters.front()->getSampleBlock();
    }
    else
    {
        Blocks headers(num_children);
        for (size_t query_num = 0; query_num < num_children; ++query_num)
            headers[query_num] = nested_interpreters[query_num]->getSampleBlock();

        result_header = getCommonHeaderForUnion(headers, context->getSettingsRef().allow_extended_type_conversion);
    }

    /// InterpreterSelectWithUnionQuery ignores limits if all nested interpreters ignore limits.
    bool all_nested_ignore_limits = true;
    bool all_nested_ignore_quota = true;
    for (auto & interpreter : nested_interpreters)
    {
        if (!interpreter->ignoreLimits())
            all_nested_ignore_limits = false;
        if (!interpreter->ignoreQuota())
            all_nested_ignore_quota = false;
    }
    options.ignore_limits |= all_nested_ignore_limits;
    options.ignore_quota |= all_nested_ignore_quota;

}

Block InterpreterSelectWithUnionQuery::getCommonHeaderForUnion(const Blocks & headers, bool allow_extended_conversion)
{
    size_t num_selects = headers.size();
    Block common_header = headers.front();
    size_t num_columns = common_header.columns();

    for (size_t query_num = 1; query_num < num_selects; ++query_num)
    {
        if (headers[query_num].columns() != num_columns)
            throw Exception("Different number of columns in UNION ALL elements:\n"
                            + common_header.dumpNames()
                            + "\nand\n"
                            + headers[query_num].dumpNames() + "\n",
                            ErrorCodes::UNION_ALL_RESULT_STRUCTURES_MISMATCH);
    }

    std::vector<const ColumnWithTypeAndName *> columns(num_selects);

    for (size_t column_num = 0; column_num < num_columns; ++column_num)
    {
        for (size_t i = 0; i < num_selects; ++i)
            columns[i] = &headers[i].getByPosition(column_num);

        ColumnWithTypeAndName & result_elem = common_header.getByPosition(column_num);
        result_elem = getLeastSuperColumn(columns, allow_extended_conversion);
    }

    return common_header;
}

Block InterpreterSelectWithUnionQuery::getCurrentChildResultHeader(const ASTPtr & ast_ptr_, const Names & required_result_column_names)
{
    if (ast_ptr_->as<ASTSelectWithUnionQuery>())
        return InterpreterSelectWithUnionQuery(ast_ptr_, context, options.copy().analyze().noModify(), required_result_column_names)
            .getSampleBlock();
    else
        return InterpreterSelectQuery(ast_ptr_, context, options.copy().analyze().noModify()).getSampleBlock();
}

std::unique_ptr<IInterpreterUnionOrSelectQuery>
InterpreterSelectWithUnionQuery::buildCurrentChildInterpreter(const ASTPtr & ast_ptr_, const Names & current_required_result_column_names)
{
    if (ast_ptr_->as<ASTSelectWithUnionQuery>())
        return std::make_unique<InterpreterSelectWithUnionQuery>(ast_ptr_, context, options, current_required_result_column_names);
    else if (ast_ptr_->as<ASTSelectQuery>())
        return std::make_unique<InterpreterSelectQuery>(ast_ptr_, context, options, current_required_result_column_names);
    else
        throw Exception("Unrecognized Query kind", ErrorCodes::NOT_IMPLEMENTED);
}

InterpreterSelectWithUnionQuery::~InterpreterSelectWithUnionQuery() = default;

Block InterpreterSelectWithUnionQuery::getSampleBlock(const ASTPtr & query_ptr_, ContextPtr context_, bool is_subquery)
{
    auto & cache = context_->getSampleBlockCache();
    /// Using query string because query_ptr changes for every internal SELECT
    auto key = queryToString(query_ptr_);
    if (cache.find(key) != cache.end())
    {
        return cache[key];
    }

    if (is_subquery)
        return cache[key]
            = InterpreterSelectWithUnionQuery(query_ptr_, context_, SelectQueryOptions().subquery().analyze()).getSampleBlock();
    else
        return cache[key] = InterpreterSelectWithUnionQuery(query_ptr_, context_, SelectQueryOptions().analyze()).getSampleBlock();
}


void InterpreterSelectWithUnionQuery::buildQueryPlan(QueryPlan & query_plan)
{
    // auto num_distinct_union = optimizeUnionList();
    size_t num_plans = nested_interpreters.size();
    const Settings & settings = context->getSettingsRef();

    /// Skip union for single interpreter.
    if (num_plans == 1)
    {
        nested_interpreters.front()->buildQueryPlan(query_plan);
    }
    else
    {
        std::vector<std::unique_ptr<QueryPlan>> plans(num_plans);
        DataStreams data_streams(num_plans);

        for (size_t i = 0; i < num_plans; ++i)
        {
            plans[i] = std::make_unique<QueryPlan>();
            nested_interpreters[i]->buildQueryPlan(*plans[i]);

            if (!blocksHaveEqualStructure(plans[i]->getCurrentDataStream().header, result_header))
            {
                auto actions_dag = ActionsDAG::makeConvertingActions(
                        plans[i]->getCurrentDataStream().header.getColumnsWithTypeAndName(),
                        result_header.getColumnsWithTypeAndName(),
                        ActionsDAG::MatchColumnsMode::Position);
                auto converting_step = std::make_unique<ExpressionStep>(plans[i]->getCurrentDataStream(), std::move(actions_dag));
                converting_step->setStepDescription("Conversion before UNION");
                plans[i]->addStep(std::move(converting_step));
            }

            data_streams[i] = plans[i]->getCurrentDataStream();
        }

        auto max_threads = context->getSettingsRef().max_threads;
        auto union_step = std::make_unique<UnionStep>(std::move(data_streams), max_threads);

        query_plan.unitePlans(std::move(union_step), std::move(plans));

        const auto & query = query_ptr->as<ASTSelectWithUnionQuery &>();
        if (query.union_mode == ASTSelectWithUnionQuery::Mode::DISTINCT)
        {
            /// Add distinct transform
            SizeLimits limits(settings.max_rows_in_distinct, settings.max_bytes_in_distinct, settings.distinct_overflow_mode);

            auto distinct_step
                = std::make_unique<DistinctStep>(query_plan.getCurrentDataStream(), limits, 0, result_header.getNames(), false);

            query_plan.addStep(std::move(distinct_step));
        }
    }

    if (settings_limit_offset_needed && !settings_limit_offset_done)
    {
        if (settings.limit > 0)
        {
            auto limit = std::make_unique<LimitStep>(query_plan.getCurrentDataStream(), settings.limit, settings.offset);
            limit->setStepDescription("LIMIT OFFSET for SETTINGS");
            query_plan.addStep(std::move(limit));
        }
        else
        {
            auto offset = std::make_unique<OffsetStep>(query_plan.getCurrentDataStream(), settings.offset);
            offset->setStepDescription("OFFSET for SETTINGS");
            query_plan.addStep(std::move(offset));
        }
    }

}

void InterpreterSelectWithUnionQuery::checkQueryCache(QueryPlan & query_plan)
{
    if (!context->getSettings().enable_query_cache)
        return;

    auto query_cache_step = std::make_unique<QueryCacheStep>(query_plan.getCurrentDataStream(), query_ptr, context, options.to_stage);
    query_cache_step->setStepDescription("QUERY CACHE");

    if (!query_cache_step->isValidQuery())
        return;

    if (query_cache_step->hitCache())
    {
        QueryPlan empty_query_plan;
        query_plan = std::move(empty_query_plan);
    }
    query_plan.addStep(std::move(query_cache_step));
}

BlockIO InterpreterSelectWithUnionQuery::execute()
{
    BlockIO res;

    QueryPlan query_plan;
    buildQueryPlan(query_plan);

    checkQueryCache(query_plan);

    auto pipeline = query_plan.buildQueryPipeline(
        QueryPlanOptimizationSettings::fromContext(context),
        BuildQueryPipelineSettings::fromContext(context));

    // FIXME: Handle TEALimit
    if (unlikely(query_ptr->as<ASTSelectWithUnionQuery>()->tealimit))
        res.pipeline = executeTEALimit(pipeline);
    else
        res.pipeline = std::move(*pipeline);
    res.pipeline.addInterpreterContext(context);

    return res;
}


void InterpreterSelectWithUnionQuery::ignoreWithTotals()
{
    for (auto & interpreter : nested_interpreters)
        interpreter->ignoreWithTotals();
}

QueryPipeline InterpreterSelectWithUnionQuery::executeTEALimit(QueryPipelinePtr & pipeline)
{
    const ASTSelectWithUnionQuery & ast = query_ptr->as<ASTSelectWithUnionQuery&>();

    // Create implicit storage to buffer pre tealimit results
    NamesAndTypesList columns = result_header.getNamesAndTypesList();
    auto temporary_table = TemporaryTableHolder(context->getQueryContext(), ColumnsDescription{columns}, {});

    String implicit_name  = "_TEALIMITDATA";

    auto storage = temporary_table.getTable();
    BlockOutputStreamPtr output = storage->write(ASTPtr(), storage->getInMemoryMetadataPtr(), context);

    PullingAsyncPipelineExecutor executor(*pipeline);
    Block block;

    output->writePrefix();
    while(executor.pull(block, context->getSettingsRef().interactive_delay / 1000))
    {
        if (block) output->write(block);
    }
    output->writeSuffix();

    // Query level temporary table
    context->getQueryContext()->addExternalTable(implicit_name, std::move(temporary_table));

    // Construct the internal SQL
    //
    // select t,  g_0, g_1, ...., g_n, cnt_0 ,..., cnt_n from misc_online_all WHERE xxx group by t, g0, g_1...gn
    // TEALIMIT N /*METRIC cnt_0, ..., cnt_n*/ GROUP (g_0, ... , g_n) ORDER EXPR(cnt_0, ... cnt_n) ASC|DESC
    //
    // select t, g_0, g_1, ..., cnt_0, ..., cnt_n from implicit_storage where
    // （g_0, ..., g_n) in (select g_0, ...., g_n from implicit_storage
    //  group by g_0, ..., g_n order by EXPR(sum(cnt_0), ..., sum(cnt_n)) ASC|DESC LIMIT N)
    //
    //
    std::stringstream postQuery;
    postQuery << "SELECT ";

    auto implicit_select_expr_list = std::make_shared<ASTExpressionList>();
    for (const auto& column : columns)
    {
        implicit_select_expr_list->children.emplace_back(std::make_shared<ASTIdentifier>(column.name));
    }
    postQuery << queryToString(*implicit_select_expr_list) << " FROM  " << implicit_name << " WHERE ";

    bool tealimit_order_keep = context->getSettingsRef().tealimit_order_keep;

    auto tea_limit =  dynamic_cast<ASTTEALimit*>(ast.tealimit.get());
    String g_list_str = queryToString(*tea_limit->group_expr_list);
    postQuery << "(" << g_list_str << ") IN (";
    // SUBQUERY
    postQuery << "SELECT " << g_list_str << " FROM  " << implicit_name << " GROUP BY "
              << g_list_str;

    postQuery << " ORDER BY ";
    auto o_list = tea_limit->order_expr_list->clone(); // will rewrite
    ASTs& elems = o_list->children;

    // Check Whether order list is in group by list, if yes, we don't add implicit
    // SUM clause
    auto nodeInGroup = [&](ASTPtr group_expr_list, const ASTPtr & node) -> bool
    {
        if (!tealimit_order_keep) return false;

         // special handling group (g0, g1), case where group expr is tuple function, step forward
         // to get g0, g1
         if (group_expr_list->children.size() == 1)
         {
            const auto * tupleFunc = group_expr_list->children[0]->as<ASTFunction>();
            if (tupleFunc && tupleFunc->name == "tuple")
            {
                group_expr_list = group_expr_list->children[0]->children[0];
            }
         }

         for (auto & g : group_expr_list->children)
         {
             if (node->getAliasOrColumnName() == g->getAliasOrColumnName())
             {
                 return true;
             }
         }
         return false;
    };

    bool comma  = false;
    for (auto & elem : elems)
    {
        auto orderCol = elem->children.front();

        if (!nodeInGroup(tea_limit->group_expr_list, orderCol))
        {
            // check if orderCol is ASTFunction or ASTIdentifier, rewrite it
            const ASTFunction * func = orderCol->as<ASTFunction>();
            const ASTIdentifier * identifier = orderCol->as<ASTIdentifier>();
            if (identifier)
            {
                auto sum_function = std::make_shared<ASTFunction>();
                sum_function->name = "SUM";
                sum_function->arguments = std::make_shared<ASTExpressionList>();
                sum_function->children.push_back(sum_function->arguments);
                sum_function->arguments->children.push_back(orderCol);

                // ORDER BY SUM()
                elem->children[0] = std::move(sum_function);
            }
            else if (func)
            {
                size_t numArgs = func->arguments->children.size();
                for (size_t i = 0; i< numArgs; i++)
                {
                    auto& iArg = func->arguments->children[i];
                    if (nodeInGroup(tea_limit->group_expr_list, iArg)) continue;
                    auto sum_function = std::make_shared<ASTFunction>();
                    sum_function->name = "SUM";
                    sum_function->arguments = std::make_shared<ASTExpressionList>();
                    sum_function->children.push_back(sum_function->arguments);
                    sum_function->arguments->children.push_back(iArg);

                    // inplace replace EXPR with new argument
                    func->arguments->children[i] = std::move(sum_function);
                }
            }
            else
            {
                throw Exception("TEALimit unhandled " + queryToString(*elem), ErrorCodes::LOGICAL_ERROR);
            }
        }

        const ASTOrderByElement & order_by_elem = elem->as<ASTOrderByElement &>();

        if (comma) postQuery << ", ";
        comma = true; // skip the first one
        postQuery << serializeAST(order_by_elem, true);
    }

    postQuery << " LIMIT ";
    if (tea_limit->limit_offset)
    {
        postQuery << serializeAST(*tea_limit->limit_offset, true) << ", ";
    }
    postQuery << serializeAST(*tea_limit->limit_value, true);
    postQuery << ")";

    //@user-profile, TEALIMIT output need respect order by info
    if (tealimit_order_keep)
    {
        comma = false;
        postQuery << " ORDER BY ";
        for (auto &elem : tea_limit->order_expr_list->children)
        {
            if (comma) postQuery << ", ";
            comma = true;
            postQuery<< serializeAST(*elem, true);
        }
    }

    // evaluate the internal SQL and get the result
    return executeQuery(postQuery.str(), context->getQueryContext(), true).pipeline;

}

}
