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

#include <QueryPlan/PlanVisitor.h>

#include <Poco/JSON/Object.h>

namespace DB
{
using PlanCostMap = std::unordered_map<PlanNodeId, double>;

class PlanPrinter
{
public:
    PlanPrinter() = delete;

    static std::string textLogicalPlan(QueryPlan & plan, ContextMutablePtr context, bool print_stats, bool verbose, PlanCostMap costs = {});
    static std::string jsonLogicalPlan(QueryPlan & plan, bool print_stats, bool verbose);

private:
    class TextPrinter;
    class JsonPrinter;
};

class TextPrinterIntent
{
public:
    static constexpr auto VERTICAL_LINE = "│  ";
    static constexpr auto INTERMEDIATE_PREFIX = "├─ ";
    static constexpr auto LAST_PREFIX = "└─ ";
    static constexpr auto EMPTY_PREFIX = "   ";

    TextPrinterIntent() = default;
    TextPrinterIntent forChild(bool last, bool hasChildren) const;
    std::string print() const { return current_lines_prefix; }
    std::string detailIntent() const;

private:
    TextPrinterIntent(std::string current_lines_prefix_, std::string next_lines_prefix_, bool hasChildren);

    std::string current_lines_prefix;
    std::string next_lines_prefix;
    bool hasChildren{true};
};

class PlanPrinter::TextPrinter
{
public:
    TextPrinter(bool print_stats_, bool verbose_, const std::unordered_map<PlanNodeId, double> & costs_)
        : print_stats(print_stats_), verbose(verbose_), costs(costs_)
    {
    }
    std::string printLogicalPlan(PlanNodeBase & plan, const TextPrinterIntent & intent = {});

private:
    static std::string printPrefix(PlanNodeBase & plan);
    static std::string printSuffix(PlanNodeBase & plan);
    std::string printDetail(PlanNodeBase & plan, const TextPrinterIntent & intent) const;
    std::string printStatistics(const PlanNodeBase & plan) const;

    const bool print_stats;
    const bool verbose;
    const std::unordered_map<PlanNodeId, double> & costs;
};

class PlanPrinter::JsonPrinter
{
public:
    explicit JsonPrinter(bool print_stats_) : print_stats(print_stats_) { }
    Poco::JSON::Object::Ptr printLogicalPlan(PlanNodeBase & plan);

private:
    static void detail(Poco::JSON::Object::Ptr & json, PlanNodeBase & plan);

    const bool print_stats;
};

}
