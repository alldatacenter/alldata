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

#include <Core/Types.h>
#include <Optimizer/CardinalityEstimate/PlanNodeStatistics.h>
#include <Optimizer/Property/Property.h>
#include <Optimizer/Rule/Rule.h>
#include <QueryPlan/IQueryPlanStep.h>

#include <memory>
#include <utility>

namespace DB
{
using GroupId = UInt32;
class GroupExpression;
using GroupExprPtr = std::shared_ptr<GroupExpression>;

class Memo;
class OptimizationContext;
using OptContextPtr = std::shared_ptr<OptimizationContext>;

class Property;
static GroupId UNDEFINED_GROUP = -1;

class Group;
using GroupPtr = std::shared_ptr<Group>;

class Winner;
using WinnerPtr = std::shared_ptr<Winner>;

class CascadesContext;

class Winner
{
public:
    Winner(
        GroupExprPtr group_expr_,
        GroupExprPtr remote_exchange_,
        GroupExprPtr local_exchange_,
        PropertySet require_children_,
        Property actual_,
        double cost_)
        : group_expr(std::move(group_expr_))
        , remote_exchange(std::move(remote_exchange_))
        , local_exchange(std::move(local_exchange_))
        , require_children(std::move(require_children_))
        , actual(std::move(actual_))
        , cost(cost_)
    {
    }

    const GroupExprPtr & getGroupExpr() const { return group_expr; }
    const PropertySet & getRequireChildren() const { return require_children; }
    const Property & getActualProperty() const { return actual; }

    const GroupExprPtr & getRemoteExchange() const { return remote_exchange; }
    const GroupExprPtr & getLocalExchange() const { return local_exchange; }
    const Property & getActual() const { return actual; }

    double getCost() const { return cost; }

    PlanNodePtr buildPlanNode(CascadesContext & context, PlanNodes & children);

private:
    GroupExprPtr group_expr;
    GroupExprPtr remote_exchange;
    GroupExprPtr local_exchange;

    PropertySet require_children;
    Property actual;
    double cost;
};

class GroupExpression
{
public:
    GroupExpression(ConstQueryPlanStepPtr step_, std::vector<GroupId> child_groups_,
                    RuleType produce_rule_ = RuleType::UNDEFINED, GroupId group_id_ = UNDEFINED_GROUP)
        : step(std::move(step_)), group_id(group_id_), child_groups(std::move(child_groups_)), produce_rule(produce_rule_)
    {
    }

    ConstQueryPlanStepPtr & getStep() { return step; }

    void setGroupId(GroupId group_id_) { group_id = group_id_; }
    GroupId getGroupId() const { return group_id; }

    const std::vector<GroupId> & getChildrenGroups() const { return child_groups; }

    bool isPhysical() const { return step->isPhysical(); }
    bool isLogical() const { return step->isLogical(); }

    void setDeleted(bool deleted_) { deleted = deleted_;}
    bool isDeleted() const { return deleted;}

    RuleType getProduceRule() const { return produce_rule; }

    /**
     * Checks whether a rule has been explored
     * @param type Rule type to see if explored
     * @returns TRUE if the rule has been explored already
     */
    bool hasRuleExplored(RuleType type) { return rule_mask.test(static_cast<UInt32>(type)); }

    /**
     * Marks a rule as having being explored in this GroupExpression
     * @param type Rule type to mark as explored
     */
    void setRuleExplored(RuleType type) { rule_mask.set(static_cast<UInt32>(type), true); }

    /**
     * Hashes GroupExpression
     * @returns hash code of GroupExpression
     */
    size_t hash();

    /**
     * Checks for equality for GroupExpression
     * @param r Other GroupExpression
     * @returns TRUE if equal to other GroupExpression
     */
    bool operator==(const GroupExpression & r) const
    {
        // Having an undefined group id is considered equal to any group id
        return (*step == *r.step) && (child_groups == r.child_groups)
            && ((group_id == UNDEFINED_GROUP) || (r.group_id == UNDEFINED_GROUP) || (r.group_id == group_id));
    }

private:
    ConstQueryPlanStepPtr step;
    GroupId group_id;

    /**
     * Vector of child groups
     */
    std::vector<GroupId> child_groups;

    /**
     * Mask of explored rules
     */
    std::bitset<static_cast<UInt32>(RuleType::NUM_RULES)> rule_mask;

    bool deleted = false;

    RuleType produce_rule;
};


/**
 * Abstract interface for a BindingIterator defined similarly to
 * a traditional iterator (hasNext(), next()).
 */
class BindingIterator
{
public:
    /**
     * Constructor for a binding iterator
     * @param memo_ Memo to be used
     */
    explicit BindingIterator(const Memo & memo_, OptContextPtr context_) : memo(memo_), context(std::move(context_)) { }

    /**
     * Default destructor
     */
    virtual ~BindingIterator() = default;

    /**
     * Virtual function for whether a binding exists
     * @returns Whether or not a binding still exists
     */
    virtual bool hasNext() = 0;

    /**
     * Virtual function for getting the next binding
     * @returns next PlanNode that matches
     */
    virtual PlanNodePtr next() = 0;

protected:
    /**
     * Internal reference to Memo table
     */
    const Memo & memo;

    OptContextPtr context;
};

/**
 * GroupBindingIterator is an implementation of the BindingIterator abstract
 * class that is specialized for trying to bind a group against a pattern.
 */
class GroupBindingIterator : public BindingIterator
{
public:
    /**
     * Constructor for a group binding iterator
     * @param memo_ Memo to be used
     * @param id_ ID of the Group for binding
     * @param pattern_ Pattern to bind
     */
    GroupBindingIterator(const Memo & memo_, GroupId id_, PatternPtr pattern_, OptContextPtr context_);

    /**
     * Virtual function for whether a binding exists
     * @returns Whether or not a binding still exists
     */
    bool hasNext() override;

    /**
     * Virtual function for getting the next binding
     * @returns next AbstractOptimizerNode that matches
     */
    PlanNodePtr next() override;

private:
    /**
     * GroupID to try binding with
     */
    GroupId group_id;

    /**
     * Pattern to try binding to
     */
    PatternPtr pattern;

    /**
     * Pointer to the group with GroupID group_id_
     */
    GroupPtr target_group;

    /**
     * Number of items in the Group to try
     */
    size_t num_group_items;

    /**
     * Current GroupExpression being tried
     */
    size_t current_item_index;

    /**
     * Iterator used for binding against GroupExpression
     */
    std::unique_ptr<BindingIterator> current_iterator;
};

/**
 * GroupExprBindingIterator is an implementation of the BindingIterator abstract
 * class that is specialized for trying to bind a GroupExpression against a pattern.
 */
class GroupExprBindingIterator : public BindingIterator
{
public:
    /**
     * Constructor for a GroupExpression binding iterator
     * @param memo Memo to be used
     * @param group_expr_ GroupExpression to bind to
     * @param pattern_ Pattern to bind
     */
    GroupExprBindingIterator(const Memo & memo, GroupExprPtr group_expr_, const PatternPtr & pattern_, OptContextPtr context_);

    /**
     * Virtual function for whether a binding exists
     * @returns Whether or not a binding still exists
     */
    bool hasNext() override;

    /**
     * Virtual function for getting the next binding
     * Pointer returned must be deleted by caller when done.
     * @returns next AbstractOptimizerNode that matches
     */
    PlanNodePtr next() override { return std::move(current_binding); }

private:
    /**
     * GroupExpression to bind with
     */
    GroupExprPtr group_expr;

    /**
     * Flag indicating whether first binding or not
     */
    bool first;

    /**
     * Flag indicating whether there are anymore bindings
     */
    bool has_next;

    /**
     * Current binding
     */
    PlanNodePtr current_binding;

    /**
     * Stored bindings for children expressions
     */
    std::vector<std::vector<PlanNodePtr>> children_bindings;

    /**
     * Position indicators tracking progress within children_bindings
     */
    std::vector<size_t> children_bindings_pos;
};

/**
 * Struct implementing the == function for GroupExpression*
 */
struct GroupExprPtrEq
{
    /**
     * Implements the == operator for GroupExpression
     * @param t1 One of the inputs GroupExpression
     * @param t2 GroupExpression to check equality with against t1
     * @returns TRUE if equal
     */
    bool operator()(GroupExprPtr const & t1, GroupExprPtr const & t2) const { return *t1 == *t2; }
};

/**
 * Struct implementing the Hash() function for a GroupExpression*
 */
struct GroupExprPtrHash
{
    /**
     * Implements the hash() function for GroupExpression
     * @param s GroupExpression to hash
     * @returns hash code
     */
    std::size_t operator()(GroupExprPtr const & s) const
    {
        if (s == nullptr)
            return 0;
        return s->hash();
    }
};

}
