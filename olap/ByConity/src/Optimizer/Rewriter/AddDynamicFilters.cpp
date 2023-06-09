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

#include <Optimizer/Rewriter/AddDynamicFilters.h>

#include <Optimizer/CardinalityEstimate/CardinalityEstimator.h>
#include <Optimizer/DynamicFilters.h>
#include <Optimizer/Rewriter/PredicatePushdown.h>
#include <Optimizer/Rewriter/UnifyNullableType.h>
#include <QueryPlan/GraphvizPrinter.h>

namespace DB
{
void AddDynamicFilters::rewrite(QueryPlan & plan, ContextMutablePtr context) const
{
    if (!context->getSettingsRef().enable_dynamic_filter)
        return;

    // must run UnifyNullableType before PredicatePushdown
    UnifyNullableType unify_nullable_type;
    unify_nullable_type.rewrite(plan, context);

    // push down predicate with dynamic filter enabled
    PredicatePushdown predicate_push_down{true};
    predicate_push_down.rewrite(plan, context);

    GraphvizPrinter::printLogicalPlan(plan, context, "3901-AddDynamicFilters");

    // extract all dynamic filter builders from projection,
    // then try to merge dynamic filters with the same effect
    DynamicFilterExtractorResult dynamic_filters = DynamicFilterExtractor::extract(plan, context);

    // extract and filter effective dynamic filter execute predicates with statistics.
    DynamicFilterPredicatesRewriter predicate_rewriter{
        plan.getCTEInfo(),
        dynamic_filters.dynamic_filter_statistics,
        dynamic_filters.dynamic_filter_cost,
        dynamic_filters.merged_dynamic_filters,
        context->getSettingsRef().dynamic_filter_min_filter_rows,
        context->getSettingsRef().dynamic_filter_max_filter_factor,
        context->getSettingsRef().enable_dynamic_filter_for_join};
    PlanWithDynamicFilterPredicates result = predicate_rewriter.rewrite(plan.getPlanNode(), context);

    GraphvizPrinter::printLogicalPlan(plan, context, "3902-AddDynamicFilters");

    RemoveUnusedDynamicFilterRewriter rewriter{context, plan.getCTEInfo(), result.effective_dynamic_filters};
    auto rewrite = rewriter.rewrite(result.plan);
    rewrite = AddDynamicFilters::AddExchange::rewrite(rewrite, context, plan.getCTEInfo());
    plan.update(rewrite);
}

DynamicFilterExtractorResult AddDynamicFilters::DynamicFilterExtractor::extract(QueryPlan & plan, ContextMutablePtr & context)
{
    DynamicFilterExtractor visitor{plan.getCTEInfo()};
    VisitorUtil::accept(plan.getPlanNode(), visitor, context);
    return DynamicFilterExtractorResult{visitor.dynamic_filter_statistics, visitor.dynamic_filter_cost, visitor.merged_dynamic_filters};
}

DynamicFilterWithScanRows AddDynamicFilters::DynamicFilterExtractor::visitPlanNode(PlanNodeBase & node, ContextMutablePtr & context)
{
    size_t total_rows = 0;
    for (auto & child : node.getChildren())
    {
        auto result = VisitorUtil::accept(child, *this, context);
        total_rows += result.scan_rows;
    }
    return DynamicFilterWithScanRows{std::unordered_map<std::string, DynamicFilterId>{}, total_rows};
}

DynamicFilterWithScanRows AddDynamicFilters::DynamicFilterExtractor::visitExchangeNode(ExchangeNode & node, ContextMutablePtr & context)
{
    return VisitorUtil::accept(node.getChildren()[0], *this, context);
}

DynamicFilterWithScanRows AddDynamicFilters::DynamicFilterExtractor::visitProjectionNode(ProjectionNode & node, ContextMutablePtr & context)
{
    auto result = VisitorUtil::accept(node.getChildren()[0], *this, context);

    const auto * project_step = dynamic_cast<const ProjectionStep *>(node.getStep().get());

    std::unordered_map<std::string, DynamicFilterId> children_dynamic_filters;
    for (const auto & assignment : project_step->getAssignments())
    {
        if (assignment.second->getType() == ASTType::ASTIdentifier && result.inherited_dynamic_filters.contains(assignment.first))
        {
            auto identifier = assignment.second->as<ASTIdentifier &>();
            children_dynamic_filters.emplace(identifier.name(), result.inherited_dynamic_filters.at(assignment.first));
        }
    }

    auto stats = CardinalityEstimator::estimate(node, cte_info, context);
    if (!stats)
        return DynamicFilterWithScanRows{{}, result.scan_rows};

    for (const auto & item : project_step->getDynamicFilters())
    {
        const auto & name = item.first;
        const auto & id = item.second.id;

        // register statistics
        dynamic_filter_statistics.emplace(id, stats.value()->getSymbolStatistics(name));
        dynamic_filter_cost.emplace(id, result.scan_rows);
        // merge duplicate dynamic filter build side.
        if (children_dynamic_filters.contains(name))
        {
            merged_dynamic_filters.emplace(id, children_dynamic_filters.at(name));
        }
        else
        {
            merged_dynamic_filters.emplace(id, id);
        }
        children_dynamic_filters[name] = id;
    }
    return DynamicFilterWithScanRows{children_dynamic_filters, result.scan_rows};
}

DynamicFilterWithScanRows AddDynamicFilters::DynamicFilterExtractor::visitJoinNode(JoinNode & node, ContextMutablePtr & context)
{
    auto left_result = VisitorUtil::accept(node.getChildren()[0], *this, context);
    auto right_result = VisitorUtil::accept(node.getChildren()[1], *this, context);

    auto left_stats = CardinalityEstimator::estimate(*node.getChildren()[0], cte_info, context);
    auto right_stats = CardinalityEstimator::estimate(*node.getChildren()[1], cte_info, context);
    auto stats = CardinalityEstimator::estimate(node, cte_info, context);
    if (!stats || !left_stats || !right_stats)
        return DynamicFilterWithScanRows{{}, left_result.scan_rows + right_result.scan_rows};

    const auto * join_step = dynamic_cast<const JoinStep *>(node.getStep().get());

    NameSet outputs;
    for (const auto & name_and_type : join_step->getOutputStream().header)
        outputs.emplace(name_and_type.name);

    std::unordered_map<std::string, std::string> left_to_right;
    std::unordered_map<std::string, std::string> right_to_left;
    for (auto left = join_step->getLeftKeys().begin(), right = join_step->getRightKeys().begin(); left != join_step->getLeftKeys().end();
         ++left, ++right)
    {
        left_to_right[*left] = *right;
        right_to_left[*right] = *left;
    }

    std::unordered_map<std::string, DynamicFilterId> children_dynamic_filters;
    for (auto & filter : left_result.inherited_dynamic_filters)
    {
        std::string output;
        if (outputs.contains(filter.first))
            output = filter.first;
        else if (left_to_right.contains(filter.first) && outputs.contains(left_to_right.at(filter.first)))
            output = left_to_right.at(filter.first);
        else
            continue;

        if (!left_stats.value()->getSymbolStatistics(filter.first)->isUnknown() && !stats.value()->getSymbolStatistics(output)->isUnknown()
            && left_stats.value()->getSymbolStatistics(filter.first)->getNdv() <= stats.value()->getSymbolStatistics(output)->getNdv())
            children_dynamic_filters.emplace(output, filter.second);
    }
    for (auto & filter : right_result.inherited_dynamic_filters)
    {
        std::string output;
        if (outputs.contains(filter.first))
            output = filter.first;
        else if (right_to_left.contains(filter.first) && outputs.contains(right_to_left.at(filter.first)))
            output = right_to_left.at(filter.first);
        else
            continue;


        if (!right_stats.value()->getSymbolStatistics(filter.first)->isUnknown() && !stats.value()->getSymbolStatistics(output)->isUnknown()
            && right_stats.value()->getSymbolStatistics(filter.first)->getNdv() <= stats.value()->getSymbolStatistics(output)->getNdv())
            children_dynamic_filters.emplace(output, filter.second);
    }

    return DynamicFilterWithScanRows{children_dynamic_filters, left_result.scan_rows + right_result.scan_rows};
}

DynamicFilterWithScanRows AddDynamicFilters::DynamicFilterExtractor::visitTableScanNode(TableScanNode & node, ContextMutablePtr & context)
{
    auto stats = CardinalityEstimator::estimate(node, cte_info, context);
    if (!stats)
        return DynamicFilterWithScanRows{{}, 0L};
    return DynamicFilterWithScanRows{{}, stats.value()->getRowCount()};
}

DynamicFilterWithScanRows AddDynamicFilters::DynamicFilterExtractor::visitCTERefNode(CTERefNode & node, ContextMutablePtr & context)
{
    const auto * cte = dynamic_cast<const CTERefStep *>(node.getStep().get());
    return cte_helper.accept(cte->getId(), *this, context);
}

PlanWithDynamicFilterPredicates AddDynamicFilters::DynamicFilterPredicatesRewriter::rewrite(const PlanNodePtr & plan, ContextMutablePtr & context)
{
    AllowedDynamicFilters extractor_context{std::unordered_set<DynamicFilterId>{}, std::unordered_set<DynamicFilterId>{}, context};
    auto rewrite = VisitorUtil::accept(plan, *this, extractor_context);
    return PlanWithDynamicFilterPredicates{rewrite.plan, effective_dynamic_filters};
}

AddDynamicFilters::DynamicFilterPredicatesRewriter::DynamicFilterPredicatesRewriter(
    CTEInfo & cte_info_,
    const std::unordered_map<DynamicFilterId, SymbolStatisticsPtr> & dynamic_filter_statistics_,
    const std::unordered_map<DynamicFilterId, size_t> & dynamic_filter_cost_,
    const std::unordered_map<DynamicFilterId, DynamicFilterId> & merged_dynamic_filters_,
    UInt64 min_filter_rows_,
    double max_filter_factor_,
    bool enable_dynamic_filter_for_join_)
    : cte_helper(cte_info_)
    , dynamic_filter_statistics(dynamic_filter_statistics_)
    , dynamic_filter_cost(dynamic_filter_cost_)
    , merged_dynamic_filters(merged_dynamic_filters_)
    , min_filter_rows(min_filter_rows_)
    , max_filter_factor(max_filter_factor_)
    , enable_dynamic_filter_for_join(enable_dynamic_filter_for_join_)
{
}

PlanWithScanRows AddDynamicFilters::DynamicFilterPredicatesRewriter::visitPlanNode(PlanNodeBase & plan, AllowedDynamicFilters & context)
{
    PlanNodes children;
    size_t total_rows = 0;
    DataStreams inputs;
    for (auto & child : plan.getChildren())
    {
        auto result = VisitorUtil::accept(*child, *this, context);
        children.emplace_back(result.plan);
        total_rows += result.scan_rows;
        inputs.emplace_back(child->getStep()->getOutputStream());
    }
    auto new_step = plan.getStep()->copy(context.context);
    new_step->setInputStreams(inputs);
    plan.setStep(new_step);

    plan.replaceChildren(children);
    return PlanWithScanRows{plan.shared_from_this(), total_rows};
}

static bool isSupportedForTableScan(FilterNode & node, const DynamicFilterDescription & description)
{
    return node.getChildren()[0]->getStep()->getType() == IQueryPlanStep::Type::TableScan
        && DynamicFilters::isSupportedForTableScan(description);
}

static ConstASTPtr removeAllDynamicFilters(const ConstASTPtr & expr)
{
    std::vector<ConstASTPtr> ret;
    auto filters = PredicateUtils::extractConjuncts(expr);
    for (auto & filter : filters)
        if (!DynamicFilters::isDynamicFilter(filter))
            ret.emplace_back(filter);

    if (ret.size() == filters.size())
        return expr;

    return PredicateUtils::combineConjuncts(ret);
}

PlanWithScanRows AddDynamicFilters::DynamicFilterPredicatesRewriter::visitFilterNode(FilterNode & node, AllowedDynamicFilters & context)
{
    auto child = VisitorUtil::accept(*node.getChildren()[0], *this, context);

    auto child_stats = CardinalityEstimator::estimate(*node.getChildren()[0], cte_helper.getCTEInfo(), context.context);
    const auto * filter_step = dynamic_cast<const FilterStep *>(node.getStep().get());

    if (!child_stats || child_stats.value()->getRowCount() < min_filter_rows)
    {
        return PlanWithScanRows{
            PlanNodeBase::createPlanNode(
                node.getId(),
                std::make_shared<FilterStep>(
                    child.plan->getCurrentDataStream(),
                    removeAllDynamicFilters(filter_step->getFilter()),
                    filter_step->removesFilterColumn()),
                PlanNodes{child.plan},
                node.getStatistics()),
            child.scan_rows};
    }

    auto filters = DynamicFilters::extractDynamicFilters(filter_step->getFilter());
    auto predicates = std::move(filters.second);
    for (auto & dynamic_filter : filters.first)
    {
        auto description = DynamicFilters::extractDescription(dynamic_filter).value();
        if (!merged_dynamic_filters.contains(description.id))
            continue;

        auto build_id = merged_dynamic_filters.at(description.id);
        description.id = build_id;

        if (context.allowed_dynamic_filters.contains(description.id)
            && (context.effective_dynamic_filters.contains(description.id) || isSupportedForTableScan(node, description)))
        {

            auto filter_cost = dynamic_filter_cost.at(build_id);
            double filter_factor = DynamicFilters::estimateSelectivity(
                description, dynamic_filter_statistics.at(build_id), child_stats.value(), *filter_step, context.context);
            if (filter_factor <= max_filter_factor && child.scan_rows > filter_cost)
            {
                predicates.emplace_back(DynamicFilters::createDynamicFilterExpression(description));
                effective_dynamic_filters[description.id].emplace(description.type);
            }
        }
    }

    return PlanWithScanRows{
        PlanNodeBase::createPlanNode(
            node.getId(),
            std::make_shared<FilterStep>(
                child.plan->getCurrentDataStream(), PredicateUtils::combineConjuncts(predicates), filter_step->removesFilterColumn()),
            PlanNodes{child.plan},
            node.getStatistics()),
        child.scan_rows};
}

PlanWithScanRows
AddDynamicFilters::DynamicFilterPredicatesRewriter::visitAggregatingNode(AggregatingNode & node, AllowedDynamicFilters & context)
{
    context.effective_dynamic_filters.insert(context.allowed_dynamic_filters.begin(), context.allowed_dynamic_filters.end());
    return visitPlanNode(node, context);
}

PlanWithScanRows AddDynamicFilters::DynamicFilterPredicatesRewriter::visitExchangeNode(
    ExchangeNode & node, AllowedDynamicFilters & context)
{
    const auto * exchange = dynamic_cast<const ExchangeStep *>(node.getStep().get());
    if (exchange->getExchangeMode() != ExchangeMode::UNKNOWN && exchange->getExchangeMode() != ExchangeMode::LOCAL_NO_NEED_REPARTITION
        && exchange->getExchangeMode() != ExchangeMode::LOCAL_MAY_NEED_REPARTITION)
    {
        context.effective_dynamic_filters.insert(context.allowed_dynamic_filters.begin(), context.allowed_dynamic_filters.end());
    }
    return visitPlanNode(node, context);
}

PlanWithScanRows AddDynamicFilters::DynamicFilterPredicatesRewriter::visitJoinNode(JoinNode & plan, AllowedDynamicFilters & context)
{
    context.effective_dynamic_filters.insert(context.allowed_dynamic_filters.begin(), context.allowed_dynamic_filters.end());
    AllowedDynamicFilters left_context{context.allowed_dynamic_filters, context.effective_dynamic_filters, context.context};

    auto right_child = plan.getChildren()[1];
    if (const auto * right_projection_step = dynamic_cast<const ProjectionStep *>(right_child->getStep().get()))
        for (const auto & item : right_projection_step->getDynamicFilters())
        {
            if (merged_dynamic_filters.contains(item.second.id))
                left_context.allowed_dynamic_filters.emplace(merged_dynamic_filters.at(item.second.id));
            if (enable_dynamic_filter_for_join) {
                left_context.effective_dynamic_filters.emplace(item.second.id);
            }
        }


    auto left = VisitorUtil::accept(plan.getChildren()[0], *this, left_context);
    auto right = VisitorUtil::accept(plan.getChildren()[1], *this, context);

    const auto *join_step = dynamic_cast<const JoinStep *>(plan.getStep().get());

    // remove them dynamic filters in join filter, which are not supported.
    auto filters = removeAllDynamicFilters(join_step->getFilter());

    return PlanWithScanRows{
        PlanNodeBase::createPlanNode(
            plan.getId(),
            std::make_shared<JoinStep>(
                DataStreams{left.plan->getCurrentDataStream(), right.plan->getCurrentDataStream()},
                join_step->getOutputStream(),
                join_step->getKind(),
                join_step->getStrictness(),
                join_step->getLeftKeys(),
                join_step->getRightKeys(),
                filters,
                join_step->isHasUsing(),
                join_step->getRequireRightKeys(),
                join_step->getAsofInequality(),
                join_step->getDistributionType(),
                join_step->isMagic()),
            PlanNodes{left.plan, right.plan},
            plan.getStatistics()),
        left.scan_rows + right.scan_rows};
}

PlanWithScanRows AddDynamicFilters::DynamicFilterPredicatesRewriter::visitTableScanNode(TableScanNode & node, AllowedDynamicFilters & context)
{
    auto stats = CardinalityEstimator::estimate(node, cte_helper.getCTEInfo(), context.context);
    if (!stats)
        return PlanWithScanRows{node.shared_from_this(), 0L};
    return PlanWithScanRows{node.shared_from_this(), stats.value()->getRowCount()};
}

PlanWithScanRows AddDynamicFilters::DynamicFilterPredicatesRewriter::visitCTERefNode(CTERefNode & node, AllowedDynamicFilters & context)
{
    const auto * cte = dynamic_cast<const CTERefStep *>(node.getStep().get());
    auto res = cte_helper.accept(cte->getId(), *this, context);
    return PlanWithScanRows{node.shared_from_this(), res.scan_rows};
}

AddDynamicFilters::RemoveUnusedDynamicFilterRewriter::RemoveUnusedDynamicFilterRewriter(
    ContextMutablePtr & context_,
    CTEInfo & cte_info,
    const std::unordered_map<DynamicFilterId, DynamicFilterTypes> & effective_dynamic_filters_)
    : context(context_), cte_helper(cte_info), effective_dynamic_filters(effective_dynamic_filters_)
{
}

PlanNodePtr AddDynamicFilters::RemoveUnusedDynamicFilterRewriter::rewrite(PlanNodePtr & plan)
{
    Void c;
    return VisitorUtil::accept(plan, *this, c);
}

PlanNodePtr AddDynamicFilters::RemoveUnusedDynamicFilterRewriter::visitPlanNode(PlanNodeBase & plan, Void & c)
{
    PlanNodes children;
    DataStreams inputs;
    for (auto & child : plan.getChildren())
    {
        auto result = VisitorUtil::accept(*child, *this, c);
        children.emplace_back(result);
        inputs.push_back(child->getStep()->getOutputStream());
    }

    auto new_step = plan.getStep()->copy(context);
    new_step->setInputStreams(inputs);
    plan.setStep(new_step);
    plan.replaceChildren(children);
    return plan.shared_from_this();
}

PlanNodePtr AddDynamicFilters::RemoveUnusedDynamicFilterRewriter::visitProjectionNode(ProjectionNode & node, Void & c)
{
    auto result = VisitorUtil::accept(node.getChildren()[0], *this, c);

    const auto * project_step = dynamic_cast<const ProjectionStep *>(node.getStep().get());

    std::unordered_map<String, DynamicFilterBuildInfo> dynamic_filters;
    for (const auto & item : project_step->getDynamicFilters())
    {
        const auto & column_name = item.first;
        const auto & id = item.second.id;
        const auto & original_symbol = item.second.original_symbol;
        if (effective_dynamic_filters.contains(id))
        {
            dynamic_filters.emplace(column_name, DynamicFilterBuildInfo{id, original_symbol, effective_dynamic_filters.at(id)});
        }
    }

    auto new_project_step = std::make_shared<ProjectionStep>(
        result->getCurrentDataStream(),
        project_step->getAssignments(),
        project_step->getNameToType(),
        project_step->isFinalProject(),
        dynamic_filters);

    return PlanNodeBase::createPlanNode(node.getId(), new_project_step, PlanNodes{result}, node.getStatistics());
}

PlanNodePtr AddDynamicFilters::RemoveUnusedDynamicFilterRewriter::visitCTERefNode(CTERefNode & node, Void & context_)
{
    const auto * cte = dynamic_cast<const CTERefStep *>(node.getStep().get());
    cte_helper.acceptAndUpdate(cte->getId(), *this, context_);
    return node.shared_from_this();
}


PlanNodePtr AddDynamicFilters::AddExchange::rewrite(const PlanNodePtr & node, ContextMutablePtr context_, CTEInfo & cte_info_)
{
    AddExchange rewriter{context_, cte_info_};
    bool has_exchange = false;
    return VisitorUtil::accept(node, rewriter, has_exchange);
}

PlanNodePtr AddDynamicFilters::AddExchange::visitPlanNode(PlanNodeBase & node, bool &)
{
    bool has_exchange = false;
    return SimplePlanRewriter::visitPlanNode(node, has_exchange);
}

PlanNodePtr AddDynamicFilters::AddExchange::visitExchangeNode(ExchangeNode & node, bool &)
{
    bool has_exchange = true;
    return SimplePlanRewriter::visitPlanNode(node, has_exchange);
}

PlanNodePtr AddDynamicFilters::AddExchange::visitFilterNode(FilterNode & node, bool & has_exchange)
{
    bool has_exchange_visit_child = has_exchange;
    auto result = SimplePlanRewriter::visitPlanNode(node, has_exchange_visit_child);
    if (has_exchange)
        return result;

    /// enforce local exchange
    const auto * filter_step = dynamic_cast<const FilterStep *>(node.getStep().get());
    auto filters = DynamicFilters::extractDynamicFilters(filter_step->getFilter());
    if (filters.first.empty())
        return result;

    return PlanNodeBase::createPlanNode(
        context->nextNodeId(),
        std::make_unique<ExchangeStep>(
            DataStreams{result->getCurrentDataStream()},
            ExchangeMode::LOCAL_NO_NEED_REPARTITION,
            Partitioning{Partitioning::Handle::FIXED_ARBITRARY},
            context->getSettingsRef().enable_shuffle_with_order),
        PlanNodes{result});
}

}
