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

#include <QueryPlan/PlanPrinter.h>

#include <Analyzers/ASTEquals.h>
#include <Optimizer/PlanNodeSearcher.h>
#include <Optimizer/PredicateConst.h>
#include <Optimizer/PredicateUtils.h>
#include <Optimizer/Utils.h>
#include <Optimizer/OptimizerMetrics.h>
#include <AggregateFunctions/AggregateFunctionNull.h>
#include <Poco/JSON/Object.h>
#include <Parsers/formatAST.h>
#include <QueryPlan/QueryPlan.h>

#include <utility>

namespace DB
{
namespace
{
template <class V>
std::string join(const V & v, const String & sep, const String & prefix = {}, const String & suffix = {})
{
    std::stringstream out;
    out << prefix;
    if (!v.empty())
    {
        auto it = v.begin();
        out << *it;
        for (++it; it != v.end(); ++it)
            out << sep << *it;
    }
    out << suffix;
    return out.str();
}
}

std::string PlanPrinter::textLogicalPlan(
    QueryPlan & plan,
    ContextMutablePtr context,
    bool print_stats,
    bool verbose,
    PlanCostMap costs)
{
    TextPrinter printer{print_stats, verbose, costs};
    auto output = printer.printLogicalPlan(*plan.getPlanNode());

    for (auto & item : plan.getCTEInfo().getCTEs())
    {
        output += "CTEDef [" + std::to_string(item.first) + "]\n";
        output += printer.printLogicalPlan(*item.second);
    }

    auto magic_sets = PlanNodeSearcher::searchFrom(plan)
                          .where([](auto & node) {
                              return node.getStep()->getType() == IQueryPlanStep::Type::Join
                                  && dynamic_cast<const JoinStep &>(*node.getStep()).isMagic();
                          })
                          .count();

    if (magic_sets > 0)
        output += "note: Magic Set is applied for " + std::to_string(magic_sets) + " parts.\n";

    auto filter_nodes = PlanNodeSearcher::searchFrom(plan)
                            .where([](auto & node) { return node.getStep()->getType() == IQueryPlanStep::Type::Filter; })
                            .findAll();

    size_t dynamic_filters = 0;
    for (auto & filter : filter_nodes)
    {
        const auto * filter_step = dynamic_cast<const FilterStep *>(filter->getStep().get());
        auto filters = DynamicFilters::extractDynamicFilters(filter_step->getFilter());
        dynamic_filters += filters.first.size();
    }

    if (dynamic_filters > 0)
        output += "note: Dynamic Filter is applied for " + std::to_string(dynamic_filters) + " times.\n";

    auto cte_nodes = PlanNodeSearcher::searchFrom(plan)
                         .where([](auto & node) { return node.getStep()->getType() == IQueryPlanStep::Type::CTERef; })
                         .count();

    if (cte_nodes > 0)
        output += "note: CTE(Common Table Expression) is applied for " + std::to_string(cte_nodes) + " times.\n";

    auto & optimizer_metrics = context->getOptimizerMetrics();
    if (optimizer_metrics && !optimizer_metrics->getUsedMaterializedViews().empty())
    {
        output += "note: Materialized Views is applied for " + std::to_string(optimizer_metrics->getUsedMaterializedViews().size())
            + " times: ";
        const auto & views = optimizer_metrics->getUsedMaterializedViews();
        auto it = views.begin();
        output += it->getDatabaseName() + "." + it->getTableName();
        for (++it; it != views.end(); ++it)
            output += ", " + it->getDatabaseName() + "." + it->getTableName();
        output += ".";
    }

    return output;
}

std::string PlanPrinter::jsonLogicalPlan(QueryPlan & plan, bool print_stats, bool)
{
    std::ostringstream os;
    JsonPrinter printer{print_stats};
    auto json = printer.printLogicalPlan(*plan.getPlanNode());
    json->stringify(os, 2);
    return os.str();
}

TextPrinterIntent TextPrinterIntent::forChild(bool last, bool hasChildren_) const
{
    return TextPrinterIntent{
        next_lines_prefix + (last ? LAST_PREFIX : INTERMEDIATE_PREFIX),
        next_lines_prefix + (last ? EMPTY_PREFIX : VERTICAL_LINE),
        hasChildren_};
}

TextPrinterIntent::TextPrinterIntent(std::string current_lines_prefix_, std::string next_lines_prefix_, bool hasChildren_)
    : current_lines_prefix(std::move(current_lines_prefix_)), next_lines_prefix(std::move(next_lines_prefix_)), hasChildren(hasChildren_)
{
}

std::string TextPrinterIntent::detailIntent() const
{
    return "\n" + next_lines_prefix + (hasChildren ? VERTICAL_LINE : EMPTY_PREFIX) + EMPTY_PREFIX;
}

std::string PlanPrinter::TextPrinter::printLogicalPlan(PlanNodeBase & plan, const TextPrinterIntent & intent) // NOLINT(misc-no-recursion)
{
    std::stringstream out;

    auto step = plan.getStep();
    out << intent.print() << printPrefix(plan) << step->getName() << printSuffix(plan) << printStatistics(plan) << printDetail(plan, intent)
        << "\n";
    for (auto it = plan.getChildren().begin(); it != plan.getChildren().end();)
    {
        auto child = *it++;
        bool last = it == plan.getChildren().end();
        bool has_children = !child->getChildren().empty();
        out << printLogicalPlan(*child, intent.forChild(last, has_children));
    }

    return out.str();
}

std::string PlanPrinter::TextPrinter::printStatistics(const PlanNodeBase & plan) const
{
    if (!print_stats)
        return "";
    std::stringstream out;
    const auto & stats = plan.getStatistics();
    out << " est. " << (stats ? std::to_string(stats.value()->getRowCount()) : "?") << " rows";
    if (costs.contains(plan.getId()))
        out << ", cost " << std::scientific << costs.at(plan.getId());
    return out.str();
}

std::string PlanPrinter::TextPrinter::printPrefix(PlanNodeBase & plan)
{
    if (plan.getStep()->getType() == IQueryPlanStep::Type::Exchange)
    {
        const auto * exchange = dynamic_cast<const ExchangeStep *>(plan.getStep().get());
        auto f = [](ExchangeMode mode) {
            switch (mode)
            {
                case ExchangeMode::LOCAL_NO_NEED_REPARTITION:
                case ExchangeMode::LOCAL_MAY_NEED_REPARTITION:
                    return "Local ";
                case ExchangeMode::BROADCAST:
                    return "Broadcast ";
                case ExchangeMode::REPARTITION:
                    return "Repartition ";
                case ExchangeMode::GATHER:
                    return "Gather ";
                default:
                    return "";
            }
        };
        return f(exchange->getExchangeMode());
    }

    if (plan.getStep()->getType() == IQueryPlanStep::Type::Join)
    {
        const auto * join = dynamic_cast<const JoinStep *>(plan.getStep().get());
        auto f = [](ASTTableJoin::Kind kind) {
            switch (kind)
            {
                case ASTTableJoin::Kind::Inner:
                    return "Inner ";
                case ASTTableJoin::Kind::Left:
                    return "Left ";
                case ASTTableJoin::Kind::Right:
                    return "Right ";
                case ASTTableJoin::Kind::Full:
                    return "Full ";
                case ASTTableJoin::Kind::Cross:
                    return "Cross ";
                default:
                    return "";
            }
        };

        return f(join->getKind());
    }
    return "";
}

std::string PlanPrinter::TextPrinter::printSuffix(PlanNodeBase & plan)
{
    std::stringstream out;
    if (plan.getStep()->getType() == IQueryPlanStep::Type::TableScan)
    {
        const auto * table_scan = dynamic_cast<const TableScanStep *>(plan.getStep().get());
        out << " " << table_scan->getDatabase() << "." << table_scan->getTable();
    }

    if (plan.getStep()->getType() == IQueryPlanStep::Type::CTERef)
    {
        auto cte = dynamic_cast<const CTERefStep *>(plan.getStep().get());
        out << " [" << cte->getId() << "]";
    }
    return out.str();
}

std::string PlanPrinter::TextPrinter::printDetail(PlanNodeBase & plan, const TextPrinterIntent & intent) const
{
    std::stringstream out;
    if (verbose && plan.getStep()->getType() == IQueryPlanStep::Type::Join)
    {
        const auto * join = dynamic_cast<const JoinStep *>(plan.getStep().get());
        out << intent.detailIntent() << "Condition: ";
        if (!join->getLeftKeys().empty())
            out << join->getLeftKeys()[0] << " == " << join->getRightKeys()[0];
        for (size_t i = 1; i < join->getLeftKeys().size(); i++)
            out << ", " << join->getLeftKeys()[i] << " == " << join->getRightKeys()[i];

        if (!ASTEquality::compareTree(join->getFilter(), PredicateConst::TRUE_VALUE))
        {
            out << intent.detailIntent() << "Filter: ";
            out << serializeAST(*join->getFilter());
        }
    }

    if (verbose && plan.getStep()->getType() == IQueryPlanStep::Type::Sorting)
    {
        auto sort = dynamic_cast<const SortingStep *>(plan.getStep().get());
        std::vector<String> sort_columns;
        for (auto & desc : sort->getSortDescription())
            sort_columns.emplace_back(
                desc.column_name + (desc.direction == -1 ? " desc" : " asc") + (desc.nulls_direction == -1 ? " nulls_last" : ""));
        out << intent.detailIntent() << "Order by: " << join(sort_columns, ", ", "{", "}");
    }


    if (verbose && plan.getStep()->getType() == IQueryPlanStep::Type::Limit)
    {
        const auto * limit = dynamic_cast<const LimitStep *>(plan.getStep().get());
        out << intent.detailIntent();
        if (limit->getLimit())
            out << "Limit: " << limit->getLimit();
        if (limit->getOffset())
            out << "Offset: " << limit->getOffset();
    }

    if (verbose && plan.getStep()->getType() == IQueryPlanStep::Type::Aggregating)
    {
        const auto * agg = dynamic_cast<const AggregatingStep *>(plan.getStep().get());
        auto keys = agg->getKeys();
        std::sort(keys.begin(), keys.end());
        out << intent.detailIntent() << "Group by: " << join(keys, ", ", "{", "}");

        std::vector<String> aggregates;
        for (const auto & desc : agg->getAggregates())
        {
            std::stringstream ss;
            String func_name = desc.function->getName();
            auto type_name = String(typeid(desc.function.get()).name());
            if (type_name.find("AggregateFunctionNull"))
                func_name = String("AggNull(").append(std::move(func_name)).append(")");
            ss << desc.column_name << ":=" << func_name << join(desc.argument_names, "," ,"(", ")");
            aggregates.emplace_back(ss.str());
        }
        if (!aggregates.empty())
            out << intent.detailIntent() << "Aggregates: " << join(aggregates, ", ");
    }

    if (verbose && plan.getStep()->getType() == IQueryPlanStep::Type::Exchange)
    {
        const auto * exchange = dynamic_cast<const ExchangeStep *>(plan.getStep().get());
        if (exchange->getExchangeMode() == ExchangeMode::REPARTITION)
        {
            auto keys = exchange->getSchema().getPartitioningColumns();
            std::sort(keys.begin(), keys.end());
            out << intent.detailIntent() << "Partition by: " << join(keys, ", ", "{", "}");
        }
    }

    if (verbose && plan.getStep()->getType() == IQueryPlanStep::Type::Filter)
    {
        const auto * filter = dynamic_cast<const FilterStep *>(plan.getStep().get());
        auto filters = DynamicFilters::extractDynamicFilters(filter->getFilter());
        if (!filters.second.empty())
            out << intent.detailIntent() << "Condition: " << serializeAST(*PredicateUtils::combineConjuncts(filters.second));
        if (!filters.first.empty())
        {
            std::vector<std::string> dynamic_filters;
            for (auto & item : filters.first)
                dynamic_filters.emplace_back(DynamicFilters::toString(DynamicFilters::extractDescription(item).value()));
            std::sort(dynamic_filters.begin(), dynamic_filters.end());
            out << intent.detailIntent() << "Dynamic Filters: " << join(dynamic_filters, ",", "{", "}");
        }
    }

    if (verbose && plan.getStep()->getType() == IQueryPlanStep::Type::Projection)
    {
        const auto * projection = dynamic_cast<const ProjectionStep *>(plan.getStep().get());

        std::vector<String> identities;
        std::vector<String> assignments;

        for (const auto & assignment : projection->getAssignments())
            if (Utils::isIdentity(assignment))
                identities.emplace_back(assignment.first);
            else
                assignments.emplace_back(assignment.first + ":=" + serializeAST(*assignment.second));

        std::sort(assignments.begin(), assignments.end());
        if (!identities.empty())
        {
            std::stringstream ss;
            std::sort(identities.begin(), identities.end());
            ss << join(identities, ", ", "[", "]");
            assignments.insert(assignments.begin(), ss.str());
        }

        out << intent.detailIntent() << "Expressions: " << join(assignments, ", ");

        if (!projection->getDynamicFilters().empty())
        {
            std::vector<std::string> dynamic_filters;
            for (const auto & item : projection->getDynamicFilters())
                dynamic_filters.emplace_back(item.first);
            std::sort(dynamic_filters.begin(), dynamic_filters.end());
            out << intent.detailIntent() << "Dynamic Filters Builder: " << join(dynamic_filters, ",", "{", "}");
        }
    }

    if (verbose && plan.getStep()->getType() == IQueryPlanStep::Type::TableScan)
    {
        const auto * table_scan = dynamic_cast<const TableScanStep *>(plan.getStep().get());
        std::vector<String> identities;
        std::vector<String> assignments;
        for (const auto & name_with_alias : table_scan->getColumnAlias())
            if (name_with_alias.second == name_with_alias.first)
                identities.emplace_back(name_with_alias.second);
            else
                assignments.emplace_back(name_with_alias.second + ":=" + name_with_alias.first);

        std::sort(assignments.begin(), assignments.end());
        if (!identities.empty())
        {
            std::stringstream ss;
            std::sort(identities.begin(), identities.end());
            ss << join(identities, ", ", "[", "]");
            assignments.insert(assignments.begin(), ss.str());
        }

        out << intent.detailIntent() << "Outputs: " << join(assignments, ", ");
    }
    return out.str();
}

Poco::JSON::Object::Ptr PlanPrinter::JsonPrinter::printLogicalPlan(PlanNodeBase & plan) // NOLINT(misc-no-recursion)
{
    Poco::JSON::Object::Ptr json = new Poco::JSON::Object(true);
    json->set("id", plan.getId());
    json->set("type", IQueryPlanStep::toString(plan.getStep()->getType()));
    detail(json, plan);

    if (print_stats)
    {
        const auto & statistics = plan.getStatistics();
        if (statistics)
            json->set("statistics", statistics.value()->toJson());
        else
            json->set("statistics", nullptr);
    }

    if (!plan.getChildren().empty())
    {
        Poco::JSON::Array children;
        for (auto & child : plan.getChildren())
            children.add(printLogicalPlan(*child));

        json->set("children", children);
    }

    return json;
}

void PlanPrinter::JsonPrinter::detail(Poco::JSON::Object::Ptr & json, PlanNodeBase & plan)
{
    if (plan.getStep()->getType() == IQueryPlanStep::Type::Join)
    {
        const auto *join = dynamic_cast<const JoinStep *>(plan.getStep().get());
        Poco::JSON::Array left_keys;
        Poco::JSON::Array right_keys;

        for (size_t i = 0; i < join->getLeftKeys().size(); i++)
        {
            left_keys.add(join->getLeftKeys()[i]);
            right_keys.add(join->getRightKeys()[i]);
        }
        json->set("leftKeys", left_keys);
        json->set("rightKeys", right_keys);

        json->set("filter", serializeAST(*join->getFilter()));

        auto f = [](ASTTableJoin::Kind kind) {
            switch (kind)
            {
                case ASTTableJoin::Kind::Inner:
                    return "inner";
                case ASTTableJoin::Kind::Left:
                    return "left";
                case ASTTableJoin::Kind::Right:
                    return "right";
                case ASTTableJoin::Kind::Full:
                    return "full";
                case ASTTableJoin::Kind::Cross:
                    return "cross";
                default:
                    return "";
            }
        };

        json->set("kind", f(join->getKind()));
    }

    if (plan.getStep()->getType() == IQueryPlanStep::Type::Aggregating)
    {
        const auto * agg = dynamic_cast<const AggregatingStep *>(plan.getStep().get());
        Poco::JSON::Array keys;
        for (const auto & item : agg->getKeys())
            keys.add(item);
        json->set("groupKeys", keys);
    }

    if (plan.getStep()->getType() == IQueryPlanStep::Type::Exchange)
    {
        const auto * exchange = dynamic_cast<const ExchangeStep *>(plan.getStep().get());
        Poco::JSON::Array keys;
        for (const auto & item : (exchange->getSchema().getPartitioningColumns()))
            keys.add(item);
        json->set("partitionKeys", keys);

        auto f = [](ExchangeMode mode) {
            switch (mode)
            {
                case ExchangeMode::LOCAL_NO_NEED_REPARTITION:
                case ExchangeMode::LOCAL_MAY_NEED_REPARTITION:
                    return "local";
                case ExchangeMode::BROADCAST:
                    return "broadcast";
                case ExchangeMode::REPARTITION:
                    return "repartition";
                case ExchangeMode::GATHER:
                    return "gather";
                default:
                    return "";
            }
        };
        json->set("mode", f(exchange->getExchangeMode()));
    }

    if (plan.getStep()->getType() == IQueryPlanStep::Type::Filter)
    {
        const auto * filter = dynamic_cast<const FilterStep *>(plan.getStep().get());
        auto filters = DynamicFilters::extractDynamicFilters(filter->getFilter());
        json->set("filter", serializeAST(*PredicateUtils::combineConjuncts(filters.second)));
        if (!filters.first.empty())
            json->set("dynamicFilter", serializeAST(*PredicateUtils::combineConjuncts(filters.first)));
    }

    if (plan.getStep()->getType() == IQueryPlanStep::Type::TableScan)
    {
        const auto * table_scan = dynamic_cast<const TableScanStep *>(plan.getStep().get());
        json->set("database", table_scan->getDatabase());
        json->set("table", table_scan->getTable());
    }
}

}
