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

#include <Optimizer/DynamicFilters.h>

#include <Functions/InternalFunctionsDynamicFilter.h>
#include <Interpreters/RuntimeFilter/RuntimeFilterManager.h>
#include <Optimizer/CardinalityEstimate/CardinalityEstimator.h>
#include <Optimizer/CardinalityEstimate/FilterEstimator.h>
#include <Optimizer/CardinalityEstimate/SymbolStatistics.h>
#include <Optimizer/PredicateUtils.h>
#include <Optimizer/Utils.h>
#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTLiteral.h>
#include <Parsers/formatAST.h>
#include <common/logger_useful.h>

namespace DB
{

ConstASTPtr DynamicFilters::createDynamicFilterExpression(DynamicFilterId id, const std::string & symbol,
                                                          DynamicFilterType type)
{
    return makeASTFunction(
        InternalFunctionDynamicFilter::name,
        std::make_shared<ASTLiteral>(id),
        std::make_shared<ASTLiteral>(symbol),
        std::make_shared<ASTIdentifier>(symbol),
        std::make_shared<ASTLiteral>(static_cast<UInt8>(type)));
}

ConstASTPtr DynamicFilters::createDynamicFilterExpression(const DynamicFilterDescription & description)
{
    return makeASTFunction(
        InternalFunctionDynamicFilter::name,
        std::make_shared<ASTLiteral>(description.id),
        std::make_shared<ASTLiteral>(description.original_symbol),
        description.expr->clone(),
        std::make_shared<ASTLiteral>(static_cast<UInt8>(description.type)));
}

bool DynamicFilters::isDynamicFilter(const ConstASTPtr & expr)
{
    return expr && expr->getType() == ASTType::ASTFunction
        && expr->as<ASTFunction &>().name == InternalFunctionDynamicFilter::name;
}

bool DynamicFilters::isSupportedForTableScan(const DynamicFilterDescription & description)
{
    return description.type == DynamicFilterType::Range && description.expr->getType() == ASTType::ASTIdentifier;
}

double DynamicFilters::estimateSelectivity(const DynamicFilterDescription & description, const SymbolStatisticsPtr & filter_source,
                                          const PlanNodeStatisticsPtr & child_stats, const FilterStep & step, ContextMutablePtr & context)
{
    double min_selectivity = 1.0;

    switch (description.type)
    {
        case DynamicFilterType::Range: {
            auto stats = child_stats->copy();
            const auto & names_and_types = step.getInputStreams()[0].header.getNamesAndTypes();
            ConstASTPtr constructed_dynamic_filter = makeASTFunction(
                "and",
                makeASTFunction("greaterOrEquals", description.expr->clone(), std::make_shared<ASTLiteral>(filter_source->getMin())),
                makeASTFunction("lessOrEquals", description.expr->clone(), std::make_shared<ASTLiteral>(filter_source->getMax())));
            auto selectivity = FilterEstimator::estimateFilterSelectivity(stats, constructed_dynamic_filter, names_and_types, context);
            min_selectivity = std::min(selectivity, min_selectivity);
        }
            [[fallthrough]]; /* fall though, range estimates may not inaccurate */
        case DynamicFilterType::BloomFilter:
            if (const auto * identifier = description.expr->as<ASTIdentifier>())
            {
                auto selectivity
                    = static_cast<double>(filter_source->getNdv()) / child_stats->getSymbolStatistics(identifier->name())->getNdv();
                min_selectivity = std::min(selectivity, min_selectivity);
            }
            //            const std::string dynamic_filter_source_name = "$internal$dynamic_filter_symbol";
            //            stats->updateOrCollectSymbolStatistics(dynamic_filter_source_name, filter_source, true);
            //            namesAndTypes.emplace_back(NameAndTypePair{dynamic_filter_source_name, TypeAnalyzer::getType(description.expr->clone(), context, namesAndTypes)});
            //            constructed_dynamic_filter
            //                = makeASTFunction("equals", description.expr->clone(), std::make_shared<ASTIdentifier>(dynamic_filter_source_name));
            //            break;
    }
    return min_selectivity;
}

std::pair<std::vector<ConstASTPtr>, std::vector<ConstASTPtr>> DynamicFilters::extractDynamicFilters(const ConstASTPtr & conjuncts)
{
    std::vector<ConstASTPtr> dynamic_filters;
    std::vector<ConstASTPtr> static_filters;
    Utils::checkArgument(conjuncts.get());
    for (auto & filter : PredicateUtils::extractConjuncts(conjuncts))
    {
        if (isDynamicFilter(filter))
        {
            dynamic_filters.emplace_back(filter);
        }
        else
        {
            static_filters.emplace_back(filter);
        }
    }
    return std::make_pair(dynamic_filters, static_filters);
}

DynamicFilterId DynamicFilters::extractId(const ConstASTPtr & dynamic_filter)
{
    Utils::checkArgument(isDynamicFilter(dynamic_filter));

    auto function = dynamic_filter->as<ASTFunction &>();
    auto id = function.arguments->children[0];
    return id->as<ASTLiteral &>().value.get<DynamicFilterId>();
}

std::optional<DynamicFilterDescription> DynamicFilters::extractDescription(const ConstASTPtr & dynamic_filter)
{
    if (dynamic_filter->getType() != ASTType::ASTFunction)
        return {};

    auto function = dynamic_filter->as<ASTFunction &>();
    if (function.name != InternalFunctionDynamicFilter::name)
        return {};

    auto id = function.arguments->children[0]->as<ASTLiteral &>().value.get<DynamicFilterId>();
    auto symbol = function.arguments->children[1]->as<ASTLiteral &>().value.get<std::string>();
    auto expr = function.arguments->children[2];
    auto type = function.arguments->children[3]->as<ASTLiteral &>().value.get<UInt8>();
    return DynamicFilterDescription{static_cast<DynamicFilterId>(id), symbol, expr, static_cast<DynamicFilterType>(type)};
}

std::vector<ASTPtr> DynamicFilters::createDynamicFilterRuntime(
    const DynamicFilterDescription & description,
    const String & query_id,
    const size_t & segment_id,
    UInt64 timeout_ms,
    RuntimeFilterManager & manager,
    const String & caller)
{
    switch (description.type)
    {
        case DynamicFilterType::BloomFilter: {
            const auto bloom_function = std::make_shared<ASTFunction>();
            bloom_function->name = "bloomFilterExist";
            bloom_function->arguments = std::make_shared<ASTExpressionList>();
            bloom_function->children.push_back(bloom_function->arguments);
            bloom_function->arguments->children.push_back(std::make_shared<ASTLiteral>(RuntimeFilterManager::makeKey(query_id, segment_id, description.id)));
            bloom_function->arguments->children.push_back(std::make_shared<ASTLiteral>(description.original_symbol));
            bloom_function->arguments->children.push_back(description.expr->clone());
            return {bloom_function};
        }
        case DynamicFilterType::Range:
            RuntimeFilterPtr runtime_filter = manager.getRuntimeFilter(query_id, segment_id, description.id, timeout_ms);

            LOG_DEBUG(&Poco::Logger::get("RuntimeFiltersConsumer"), "{"
                          + caller + "} try load range dynamic filter id: "
                          + std::to_string(description.id) + " " + (runtime_filter ? "success" : "failed"));

            if (runtime_filter)
            {
                if (!runtime_filter->getRangeValid())
                {
                    LOG_DEBUG(&Poco::Logger::get("RuntimeFiltersConsumer"), "{"
                                  + caller + "} invalid range dynamic filter id: " + std::to_string(description.id));
                    return {PredicateConst::FALSE_VALUE}; // no data in builder side, simply filter all data in consumer side
                }
                auto min_max_values = runtime_filter->getMinMax();
                assert(min_max_values.size() == 1);
                auto min = min_max_values.begin()->second.first;
                auto max = min_max_values.begin()->second.second;
                LOG_DEBUG(&Poco::Logger::get("RuntimeFiltersConsumer"), "{"
                              + caller + "} dynamic filter id: " + std::to_string(description.id) + " max: " + max->getColumnName() + " min: " + min->getColumnName());

                const auto greater_function = std::make_shared<ASTFunction>();
                greater_function->name = "greaterOrEquals";
                greater_function->arguments = std::make_shared<ASTExpressionList>();
                greater_function->children.push_back(greater_function->arguments);
                greater_function->arguments->children.push_back(description.expr->clone());
                greater_function->arguments->children.push_back(min);

                const auto less_function = std::make_shared<ASTFunction>();
                less_function->name = "lessOrEquals";
                less_function->arguments = std::make_shared<ASTExpressionList>();
                less_function->children.push_back(less_function->arguments);
                less_function->arguments->children.push_back(description.expr->clone());
                less_function->arguments->children.push_back(max);

                return {greater_function, less_function};
            }
    }
    return {};
}

std::string DynamicFilters::toString(const DynamicFilterDescription & description)
{
    auto f = [](DynamicFilterType type) {
        switch (type)
        {
            case DynamicFilterType::Range:
                return "range";
            case DynamicFilterType::BloomFilter:
                return "bloomfilter";
        }
        throw Exception("Unknown dynamic filter type", ErrorCodes::LOGICAL_ERROR);
    };

    return serializeAST(*description.expr->clone()) + ":" + f(description.type);
}

}
