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

#include <Optimizer/Rule/Pattern.h>

#include <Optimizer/Rule/Patterns.h>

namespace DB
{
PatternPtr Pattern::capturedAs(const Capture & capture)
{
    return capturedAs(
        capture, [](const PlanNodePtr & node) { return std::any{node}; }, "`this`");
}

PatternPtr Pattern::capturedAs(const Capture & capture, const PatternProperty & property)
{
    return capturedAs(capture, property, "unknown");
}

PatternPtr Pattern::capturedAs(const Capture & capture, const PatternProperty & property, const std::string & name)
{
    return std::make_shared<CapturePattern>(name, property, capture, this->shared_from_this());
}

PatternPtr Pattern::matching(const PatternPredicate & predicate)
{
    return matching(predicate, "unknown");
}

PatternPtr Pattern::matching(const PatternPredicate & predicate, const std::string & name)
{
    return std::make_shared<FilterPattern>(name, predicate, this->shared_from_this());
}

PatternPtr Pattern::matchingCapture(const std::function<bool(const Captures &)> & capture_predicate)
{
    return matchingCapture(capture_predicate, "unknown");
}

PatternPtr Pattern::matchingCapture(const std::function<bool(const Captures &)> & capture_predicate, const std::string & name)
{
    return matching(std::bind(capture_predicate, std::placeholders::_2), name); // NOLINT(modernize-avoid-bind)
}

PatternPtr Pattern::withEmpty()
{
    return std::make_shared<WithPattern>(PatternQuantifier::EMPTY, nullptr, this->shared_from_this());
}

PatternPtr Pattern::withSingle(const PatternPtr & sub_pattern)
{
    return std::make_shared<WithPattern>(PatternQuantifier::SINGLE, sub_pattern, this->shared_from_this());
}

PatternPtr Pattern::withAny(const PatternPtr & sub_pattern)
{
    return std::make_shared<WithPattern>(PatternQuantifier::ANY, sub_pattern, this->shared_from_this());
}

PatternPtr Pattern::withAll(const PatternPtr & sub_pattern)
{
    return std::make_shared<WithPattern>(PatternQuantifier::ALL, sub_pattern, this->shared_from_this());
}

PatternPtr Pattern::with(std::vector<PatternPtr> sub_patterns)
{
    return std::make_shared<WithPattern>(PatternQuantifier::ALL, sub_patterns, this->shared_from_this());
}

std::optional<Match> Pattern::match(const PlanNodePtr & node, Captures & captures) const
{
    if (!previous)
        return accept(node, captures);

    if (auto prev_res = previous->match(node, captures))
        return accept(node, prev_res->captures);
    else
        return {};
}

std::vector<PatternPtr> Pattern::getChildrenPatterns()
{
    std::vector<PatternPtr> children_patterns;
    auto pattern = this->shared_from_this();
    while (pattern->getPrevious())
    {
        if (auto with_pattern = std::dynamic_pointer_cast<WithPattern>(pattern))
        {
            children_patterns = with_pattern->getSubPatterns();
        }
        pattern = pattern->getPrevious();
    }

    if (auto type_of_pattern = std::dynamic_pointer_cast<TypeOfPattern>(pattern))
    {
        if (type_of_pattern->type == IQueryPlanStep::Type::Tree)
            return {Patterns::tree()};
    }
    return children_patterns;
}

// By convention, the head pattern is a TypeOfPattern,
// which indicates which plan node type this pattern is targeted to
IQueryPlanStep::Type Pattern::getTargetType()
{
    auto pattern = this->shared_from_this();
    while (pattern->getPrevious())
        pattern = pattern->getPrevious();

    if (auto type_of_pattern = std::dynamic_pointer_cast<TypeOfPattern>(pattern))
        return type_of_pattern->type;
    else
        throw Exception("Head pattern must be a TypeOfPattern, illegal pattern found: " + toString(), ErrorCodes::LOGICAL_ERROR);
}

String Pattern::toString()
{
    PatternPrinter printer;
    this->accept(printer);
    return printer.formatted_str.str();
}

std::optional<Match> TypeOfPattern::accept(const PlanNodePtr & node, Captures & captures) const
{
    if (type == IQueryPlanStep::Type::Any || type == IQueryPlanStep::Type::Tree || type == node->getStep()->getType())
        return {Match{std::move(captures)}};
    else
        return {};
}

void TypeOfPattern::accept(PatternVisitor & pattern_visitor)
{
    pattern_visitor.visitTypeOfPattern(*this);
}

std::optional<Match> CapturePattern::accept(const PlanNodePtr & node, Captures & captures) const
{
    captures.insert(std::make_pair(capture, property(node)));
    return {Match{std::move(captures)}};
}

void CapturePattern::accept(PatternVisitor & pattern_visitor)
{
    pattern_visitor.visitCapturePattern(*this);
}

std::optional<Match> FilterPattern::accept(const PlanNodePtr & node, Captures & captures) const
{
    if (predicate(node, captures))
        return {Match{std::move(captures)}};
    else
        return {};
}

void FilterPattern::accept(PatternVisitor & pattern_visitor)
{
    pattern_visitor.visitFilterPattern(*this);
}

std::optional<Match> WithPattern::accept(const PlanNodePtr & node, Captures & captures) const
{
    const PlanNodes & sub_nodes = node->getChildren();

    if (quantifier == PatternQuantifier::EMPTY)
    {
        if (sub_nodes.empty())
            return {Match{std::move(captures)}};
        else
            return {};
    }
    else if (quantifier == PatternQuantifier::SINGLE)
    {
        if (sub_nodes.size() == 1)
            return sub_patterns[0]->match(sub_nodes[0], captures);
        else
            return {};
    }
    else
    {
        bool matched = false;
        size_t index = 0;
        for (const PlanNodePtr & subNode : sub_nodes)
        {
            Captures sub_captures = captures;
            auto pattern = sub_patterns[0];
            if (sub_patterns.size() > index)
            {
                pattern = sub_patterns[index];
                index++;
            }
            std::optional<Match> subResult = pattern->match(subNode, sub_captures);

            if (subResult)
            {
                captures = std::move(subResult->captures);
                matched = true;
            }
            else if (quantifier == PatternQuantifier::ALL)
            {
                matched = false;
                break;
            }
        }

        if (matched)
            return {Match{std::move(captures)}};
        else
            return {};
    }
}

void WithPattern::accept(PatternVisitor & pattern_visitor)
{
    pattern_visitor.visitWithPattern(*this);
}

void PatternPrinter::appendLine(const std::string & str)
{
    if (first)
        first = false;
    else
        formatted_str << '\n';

    formatted_str << std::string(level, '\t') << str;
}

void PatternPrinter::visitWithPattern(WithPattern & pattern)
{
    visitPrevious(pattern);

    std::string withType = ([](WithPattern & pattern_) -> std::string {
        switch (pattern_.getQuantifier())
        {
            case PatternQuantifier::EMPTY:
                return "empty";
            case PatternQuantifier::SINGLE:
                return "single";
            case PatternQuantifier::ANY:
                return "any";
            case PatternQuantifier::ALL:
                return "all";
        }

        throw Exception("Unknown with type", ErrorCodes::LOGICAL_ERROR);
    })(pattern);

    appendLine("with " + withType + ":");

    for (auto & sub_pattern : pattern.getSubPatterns())
    {
        level++;
        sub_pattern->accept(*this);
        level--;
    }
}

void PatternPrinter::visitTypeOfPattern(TypeOfPattern & pattern)
{
    visitPrevious(pattern);
#define PRINT_STEP_TYPE(ITEM) \
    case IQueryPlanStep::Type::ITEM: \
        appendLine("typeOf: " #ITEM); \
        break;

    switch (pattern.type)
    {
        case IQueryPlanStep::Type::Any:
            appendLine("typeOf: Any");
            break;
            APPLY_STEP_TYPES(PRINT_STEP_TYPE)
        default:
            break;
    }
#undef PRINT_STEP_TYPE
}

void PatternPrinter::visitCapturePattern(CapturePattern & pattern)
{
    visitPrevious(pattern);
    appendLine("capture " + pattern.name + " as: " + pattern.capture.desc);
}

void PatternPrinter::visitFilterPattern(FilterPattern & pattern)
{
    visitPrevious(pattern);
    appendLine("filter by: " + pattern.name);
}

}
