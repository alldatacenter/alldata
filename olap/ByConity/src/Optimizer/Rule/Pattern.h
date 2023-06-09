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

#include <Optimizer/Rule/Match.h>

#include <functional>
#include <memory>
#include <utility>

/**
 * Pattern matching is used to match a plan node with a specified
 * structure(`WithPattern`) or by some criteria(`FilterPattern`),
 * and retrieve properties of a plan node(`CapturePattern`).
 */
namespace DB
{
class Pattern;
using PatternPtr = std::shared_ptr<Pattern>;
class PatternVisitor;
using PatternProperty = std::function<std::any(const PlanNodePtr &)>;
using PatternPredicate = std::function<bool(const PlanNodePtr &, const Captures &)>;
enum class PatternQuantifier;

class Pattern : public std::enable_shared_from_this<Pattern>
{
public:
    virtual ~Pattern() = default;

    IQueryPlanStep::Type getTargetType();
    String toString();

    PatternPtr capturedAs(const Capture & capture);
    PatternPtr capturedAs(const Capture & capture, const PatternProperty & property);
    PatternPtr capturedAs(const Capture & capture, const PatternProperty & property, const std::string & name);
    template <typename T>
    PatternPtr capturedStepAs(const Capture & capture, const std::function<std::any(const T &)> & step_property)
    {
        return capturedStepAs(capture, step_property, "unknown");
    }
    template <typename T>
    PatternPtr capturedStepAs(const Capture & capture, const std::function<std::any(const T &)> & step_property, const std::string & name)
    {
        static_assert(std::is_base_of<IQueryPlanStep, T>::value, "T must inherit from IQueryPlanStep");

        PatternProperty property = [step_property](const PlanNodePtr & node) -> std::any {
            auto * step = dynamic_cast<const T *>(node->getStep().get());

            if (!step)
                throw Exception("Unexpected plan step found in pattern matching", ErrorCodes::LOGICAL_ERROR);

            return step_property(*step);
        };
        return capturedAs(capture, property, name);
    }
    PatternPtr matching(const PatternPredicate & predicate);
    PatternPtr matching(const PatternPredicate & predicate, const std::string & name);
    PatternPtr matchingCapture(const std::function<bool(const Captures &)> & capture_predicate);
    PatternPtr matchingCapture(const std::function<bool(const Captures &)> & capture_predicate, const std::string & name);
    template <typename T>
    PatternPtr matchingStep(const std::function<bool(const T &)> & step_predicate)
    {
        return matchingStep(step_predicate, "unknown");
    }
    template <typename T>
    PatternPtr matchingStep(const std::function<bool(const T &)> & step_predicate, const std::string & name)
    {
        static_assert(std::is_base_of<const IQueryPlanStep, T>::value, "T must inherit from const IQueryPlanStep");

        PatternPredicate predicate = [step_predicate](const PlanNodePtr & node, const Captures &) -> bool {
            auto * step = dynamic_cast<const T *>(node->getStep().get());

            if (!step)
                throw Exception("Unexpected plan step found in pattern matching", ErrorCodes::LOGICAL_ERROR);

            return step_predicate(*step);
        };

        return matching(predicate, name);
    }
    PatternPtr withEmpty();
    PatternPtr withSingle(const PatternPtr & sub_pattern);
    PatternPtr withAny(const PatternPtr & sub_pattern);
    PatternPtr withAll(const PatternPtr & sub_pattern);
    PatternPtr with(std::vector<PatternPtr> sub_patterns);

    bool matches(const PlanNodePtr & node) const { return match(node).has_value(); }

    std::optional<Match> match(const PlanNodePtr & node) const
    {
        Captures captures;
        return match(node, captures);
    }

    std::optional<Match> match(const PlanNodePtr & node, Captures & captures) const;
    virtual std::optional<Match> accept(const PlanNodePtr & node, Captures & captures) const = 0;
    virtual void accept(PatternVisitor & pattern_visitor) = 0;
    const PatternPtr & getPrevious() const { return previous; }

    std::vector<PatternPtr> getChildrenPatterns();

protected:
    Pattern() = default;
    explicit Pattern(PatternPtr previous_) : previous(std::move(previous_)){}

private:
    PatternPtr previous;
};

class TypeOfPattern : public Pattern
{
public:
    explicit TypeOfPattern(IQueryPlanStep::Type type_) : Pattern(), type(type_){}
    TypeOfPattern(IQueryPlanStep::Type type_, PatternPtr previous) : Pattern(std::move(previous)), type(type_){}
    std::optional<Match> accept(const PlanNodePtr & node, Captures & captures) const override;
    void accept(PatternVisitor & pattern_visitor) override;

    IQueryPlanStep::Type type;
};

class CapturePattern : public Pattern
{
public:
    CapturePattern(std::string name_, PatternProperty property_, Capture capture_, PatternPtr previous)
        : Pattern(std::move(previous)), name(std::move(name_)), property(std::move(property_)), capture(std::move(capture_)){}
    std::optional<Match> accept(const PlanNodePtr & node, Captures & captures) const override;
    void accept(PatternVisitor & pattern_visitor) override;

    std::string name;
    PatternProperty property;
    Capture capture;
};

class FilterPattern : public Pattern
{
public:
    FilterPattern(std::string name_, PatternPredicate predicate_, PatternPtr previous)
        : Pattern(std::move(previous)), name(std::move(name_)), predicate(std::move(predicate_)){}
    std::optional<Match> accept(const PlanNodePtr & node, Captures & captures) const override;
    void accept(PatternVisitor & pattern_visitor) override;

    std::string name;
    PatternPredicate predicate;
};

enum class PatternQuantifier
{
    EMPTY,
    SINGLE,
    ANY,
    ALL
};

class WithPattern : public Pattern
{
public:
    WithPattern(PatternQuantifier quantifier_, PatternPtr sub_pattern, PatternPtr previous)
        : Pattern(std::move(previous)), quantifier(quantifier_), sub_patterns{std::move(sub_pattern)} {}

    WithPattern(PatternQuantifier quantifier_, std::vector<PatternPtr> sub_patterns_, PatternPtr previous)
        : Pattern(std::move(previous)), quantifier(quantifier_), sub_patterns{std::move(sub_patterns_)} {}
    std::optional<Match> accept(const PlanNodePtr & node, Captures & captures) const override;
    void accept(PatternVisitor & pattern_visitor) override;

    PatternQuantifier getQuantifier() const { return quantifier; }
    const std::vector<PatternPtr> & getSubPatterns() const { return sub_patterns; }

private:
    PatternQuantifier quantifier;
    std::vector<PatternPtr> sub_patterns;
};

class PatternVisitor
{
public:
    virtual ~PatternVisitor() = default;
    virtual void visitTypeOfPattern(TypeOfPattern & pattern) = 0;
    virtual void visitCapturePattern(CapturePattern & pattern) = 0;
    virtual void visitFilterPattern(FilterPattern & pattern) = 0;
    virtual void visitWithPattern(WithPattern & pattern) = 0;

    void visitPrevious(Pattern & pattern)
    {
        if (auto & prev = pattern.getPrevious())
            prev->accept(*this);
    }
};

class PatternPrinter : public PatternVisitor
{
public:
    void visitTypeOfPattern(TypeOfPattern & pattern) override;
    void visitCapturePattern(CapturePattern & pattern) override;
    void visitFilterPattern(FilterPattern & pattern) override;
    void visitWithPattern(WithPattern & pattern) override;

    void appendLine(const std::string & str);

    std::stringstream formatted_str;
    int level = 0;
    bool first = true;
};

}
