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

#include <Interpreters/PredicateImplicationChecker.h>
#include <Interpreters/predicateExpressionsUtils.h>
#include <Parsers/ASTFunction.h>
#include <Parsers/ASTLiteral.h>
#include <AggregateFunctions/AggregateFunctionFactory.h>
#include <Common/TypePromotion.h>

#include <unordered_set>
#include <memory>
#include <algorithm>
#include <iterator>

namespace DB
{
    /// key : a comparison function name, value : the opposite comparison function name
    static const std::unordered_map<String, String> COMPARISON_OR_LIKE_FUNC_MAP{{"equals",          "notEquals"},
                                                                                {"notEquals",       "equals"},
                                                                                {"less",            "greaterOrEquals"},
                                                                                {"greater",         "lessOrEquals"},
                                                                                {"lessOrEquals",    "greater"},
                                                                                {"greaterOrEquals", "less"},
                                                                                {"like",            "notLike"},
                                                                                {"notLike",         "like"}};

    namespace ErrorCodes
    {
        extern const int ILLEGAL_AGGREGATION;
        extern const int LOGICAL_ERROR;
    }

    void assertNoAggregates(ASTPtr & ast)
    {
        if (auto * function = ast->as<ASTFunction>())
        {
            if (AggregateFunctionFactory::instance().isAggregateFunctionName(function->name))
                throw Exception("Aggregate function " + function->getColumnName() +
                                " is found in WHERE or PREWHERE in query", ErrorCodes::ILLEGAL_AGGREGATION);
        }
    }

    /// Return whether the first greater than second.
    bool greater(const Field & first, const Field & second, bool support_equals)
    {
        /// Throw an exception and we'll catch it later.
        /// If we compare two fields with different types, the result may be not correct.
        /// For examples, a field of Int64 always greater than a field of UInt64 in current implementation, this is not we want.
        /// We simply throw and set the implication result to false. We'll found a better way to handle this.
        if (first.getType() != second.getType())
            throw Exception("Compare two fields with different types.", ErrorCodes::LOGICAL_ERROR);

        if (support_equals)
            return first >= second;
        else
            return first > second;
    }

    /// Return whether the first less than second.
    bool less(const Field & first, const Field & second, bool support_equals)
    {
        if (first.getType() != second.getType())
            throw Exception("Compare two fields with different types.", ErrorCodes::LOGICAL_ERROR);

        if (support_equals)
            return first <= second;
        else
            return first < second;
    }

    /// Return whether the first is a subset of the second
    bool subset(const std::set<Field> & first, const std::set<Field> & second)
    {
        auto first_iter = first.begin();
        auto second_iter = second.begin();
        while (first_iter != first.end() && second_iter != second.end())
        {
            if (*first_iter == *second_iter)
            {
                ++first_iter;
                ++second_iter;
            }
            else if (*first_iter > *second_iter)
                ++second_iter;
            else
                return false;
        }

        return first_iter == first.end();
    }

    /// Return whether there is intersection between first and second
    bool existIntersection(const std::set<Field> & first, const std::set<Field> & second)
    {
        auto first_iter = first.begin();
        auto second_iter = second.begin();
        while (first_iter != first.end() && second_iter != second.end())
        {
            if (*first_iter < *second_iter)
                ++first_iter;
            else if (*first_iter > *second_iter)
                ++second_iter;
            else
                return true;
        }

        return false;
    }

    /// Remove predicates which are complete same between first and second.
    void removeDuplicate(ASTs & first_predicates, ASTs & second_predicates)
    {
        std::unordered_set<String> second_set;
        for (auto & c : second_predicates)
        {
            second_set.emplace(c->getColumnName());
        }

        std::unordered_set<String> same_set;
        for (auto iter = first_predicates.begin(); iter != first_predicates.end();)
        {
            auto column_name = (*iter)->getColumnName();
            if (second_set.find(column_name) != second_set.end())
            {
                iter = first_predicates.erase(iter);
                same_set.emplace(column_name);
            }
            else
                ++iter;
        }

        for (auto iter = second_predicates.begin(); iter != second_predicates.end();)
        {
            auto column_name = (*iter)->getColumnName();
            if (same_set.find(column_name) != same_set.end())
                iter = second_predicates.erase(iter);
            else
                ++iter;
        }
    }

    /// Return whether the `notEquals` fields in first implies the `notEquals` fields in second
    /// Notes: the purpose that we generate this method is to reduce code duplication, and this
    /// method is only called from `PredicateImplicationChecker::impliesComparison`
    bool impliesNotEquals(CompactedComparison & first, CompactedComparison & second)
    {
        if (!first.hasLess() && !first.hasGreater())
            return subset(second.comparisons["notEquals"], first.comparisons["notEquals"]);
        else if (!first.hasLess() && first.hasGreater())
        {
            /// `notEquals` fields exist in second and implies in first comparison's scope
            std::set<Field> fields;
            for (auto & f : second.comparisons["notEquals"])
            {
                if (greater(f, first.getGreaterField(), first.exist_greater_or_equals))
                    fields.emplace(f);
            }

            return subset(fields, first.comparisons["notEquals"]);
        }
        else if (first.hasLess() && !first.hasGreater())
        {
            std::set<Field> fields;
            for (auto & f : second.comparisons["notEquals"])
            {
                if (less(f, first.getLessField(), first.exist_less_or_equals))
                    fields.emplace(f);
            }

            return subset(fields, first.comparisons["notEquals"]);
        }
        else
        {
            std::set<Field> fields;
            for (auto & f : second.comparisons["notEquals"])
            {
                if (greater(f, first.getGreaterField(), first.exist_greater_or_equals) &&
                    less(f, first.getLessField(), first.exist_less_or_equals))
                    fields.emplace(f);
            }

            return subset(fields, first.comparisons["notEquals"]);
        }
    }

    PredicateImplicationChecker::PredicateImplicationChecker(const ASTPtr & first, const ASTPtr & second, bool where_)
            : where(where_)
    {
        /**
         * Pre-processings before the real implication check
         *
         * Steps:
         *   1. Get DNF (disjunctive normal form) of the predicate expression.
         *   2. Decompose DNF into a list of predicates.
         *   3. Remove duplicate predicates between first and second.
         */
        auto first_dnf = toDNF(first);
        auto second_dnf = toDNF(second);

        first_predicates = decomposeOr(first_dnf);
        second_predicates = decomposeOr(second_dnf);

        /// We can remove duplicate predicates between first and second if they both are conjunctions.
        /// Example: first (x > 1 and y < 1), second (x > 1 and y < 2), after removed duplicates: first (y < 1), second (y < 2)
        /// The duplicate predicates could be ignored during implication check and future rewriting.
        if (first_predicates.size() == 1 && second_predicates.size() == 1)
        {
            auto first_temps = decomposeAnd(first_predicates[0]);
            auto second_temps = decomposeAnd(second_predicates[0]);
            removeDuplicate(first_temps, second_temps);

            if (first_temps.empty())
                first_predicates.clear();
            else
                first_predicates[0] = composeAnd(first_temps);

            if (second_temps.empty())
                second_predicates.clear();
            else
                second_predicates[0] = composeAnd(second_temps);
        }
    }

    ASTs PredicateImplicationChecker::getFirstPredicates()
    {
        return first_predicates;
    }

    bool PredicateImplicationChecker::implies()
    {
        /// after removed duplicates, we should check whether they are empty.
        if (first_predicates.empty() && second_predicates.empty())
            return true;
        else if (first_predicates.empty() && !second_predicates.empty())
            return false;
        else if (!first_predicates.empty() && second_predicates.empty())
            return true;
        else
        {
            for (auto & f : first_predicates)
            {
                /// check if f implies at least one of the predicates in second_predicates.
                /// If f could not imply even one predicate in second_predicates, then final implication may be false.
                if (!impliesAny(f, second_predicates))
                    return false;
            }

            return true;
        }
    }

    bool PredicateImplicationChecker::impliesAny(ASTPtr & first, ASTs & seconds)
    {
        for (auto & second : seconds)
        {
            if (impliesConjunction(first, second))
                return true;
        }

        return false;
    }

    bool PredicateImplicationChecker::impliesConjunction(ASTPtr & first, ASTPtr & second)
    {
        const auto & firsts = decomposeAnd(first);
        /// column name -> comparison or like function name -> set of fields
        std::unordered_map<String, std::unordered_map<String, std::set<Field>>> first_predicate_map;
        for (auto & f : firsts)
        {
            if (!genPredicateMap(first_predicate_map, f))
                return false;
        }

        const auto & seconds = decomposeAnd(second);
        std::unordered_map<String, std::unordered_map<String, std::set<Field>>> second_predicate_map;
        for (auto & s : seconds)
        {
            if (!genPredicateMap(second_predicate_map, s))
                return false;
        }

        for (auto & entry : second_predicate_map)
        {
            auto search =  first_predicate_map.find(entry.first);
            /// Any column in second must exist in first, otherwise first doesn't imply second.
            /// For example, first (x > 1) doesn't imply second (x > 1 and y < 1)
            if (search == first_predicate_map.end())
                return false;

            auto first_compacted = compactComparison(search->second);
            if (first_compacted.always_false)
                return true; // since first is always false, it can imply second

            auto second_compacted = compactComparison(entry.second);
            if (second_compacted.always_false)
                return false; // first can not imply false

            try
            {
                if (!impliesComparison(first_compacted, second_compacted))
                    return false;
            }
            catch (...)
            {
                return false;
            }
        }

        return true;
    }

    bool PredicateImplicationChecker::impliesComparison(CompactedComparison & first, CompactedComparison & second)
    {
        if (first.always_false)
            return true;
        if (second.always_false)
            return false;

        /// We first compare like/notLike functions, based on exact match
        if (!first.exist_like && !second.exist_like)
        {
            /// do nothing, go to next step
        }
        else if (!first.exist_like && second.exist_like)
        {
            return false;
        }
        else if (first.exist_like && !second.exist_like)
        {
            /// do nothing, go to next step
        }
        else
        {
            if (first.comparisons["like"] != second.comparisons["like"])
                return false;
        }

        if (!first.exist_not_like && !second.exist_not_like)
        {
            /// do nothing, go to next step
        }
        else if (!first.exist_not_like && second.exist_not_like)
        {
            return false;
        }
        else if (first.exist_not_like && !second.exist_not_like)
        {
            /// do nothing, go to next step
        }
        else
        {
            if (first.comparisons["notLike"] != second.comparisons["notLike"])
                return false;
        }

        /// Compare comparison functions
        if (!first.exist_equals && !second.exist_equals)
        {
            if (!first.hasLess() && !first.hasGreater())
            {
                if (!second.hasLess() && !second.hasGreater())
                    return impliesNotEquals(first, second);
                else
                    return false;
            }
            else if (!first.hasLess() && first.hasGreater())
            {
                if (!second.hasLess() && !second.hasGreater())
                    return impliesNotEquals(first, second);
                else if (!second.hasLess() && second.hasGreater())
                    return greater(first.getGreaterField(), second.getGreaterField(),
                                   first.exist_greater || second.exist_greater_or_equals) &&
                           impliesNotEquals(first, second);
                else
                    return false;
            }
            else if (first.hasLess() && !first.hasGreater())
            {
                if (!second.hasLess() && !second.hasGreater())
                    return impliesNotEquals(first, second);
                else if (second.hasLess() && !second.hasGreater())
                    return less(first.getLessField(), second.getLessField(),
                                first.exist_less || second.exist_less_or_equals) && impliesNotEquals(first, second);
                else
                    return false;
            }
            else
            {
                if (!second.hasLess() && !second.hasGreater())
                    return impliesNotEquals(first, second);
                else if (!second.hasLess() && second.hasGreater())
                    return greater(first.getGreaterField(), second.getGreaterField(),
                                   first.exist_greater || second.exist_greater_or_equals) &&
                           impliesNotEquals(first, second);
                else if (second.hasLess() && !second.hasGreater())
                    return less(first.getLessField(), second.getLessField(),
                                first.exist_less || second.exist_less_or_equals) && impliesNotEquals(first, second);
                else
                    return greater(first.getGreaterField(), second.getGreaterField(),
                                   first.exist_greater || second.exist_greater_or_equals) &&
                           less(first.getLessField(), second.getLessField(),
                                first.exist_less || second.exist_less_or_equals) && impliesNotEquals(first, second);
            }
        }
        else if (!first.exist_equals && second.exist_equals)
        {
            /// Because first is a value range, second is a value points, we think first can not imply second
            return false;
        }
        else if (first.exist_equals && !second.exist_equals)
        {
            if (!second.hasLess() && !second.hasGreater())
                return !existIntersection(first.comparisons["equals"], second.comparisons["notEquals"]);
            else if (!second.hasLess() && second.hasGreater())
                return greater(*(first.comparisons["equals"].begin()), second.getGreaterField(),
                               second.exist_greater_or_equals) &&
                       !existIntersection(first.comparisons["equals"], second.comparisons["notEquals"]);
            else if (second.hasLess() && !second.hasGreater())
                return less(*(first.comparisons["equals"].rbegin()), second.getLessField(), second.exist_less_or_equals) &&
                       !existIntersection(first.comparisons["equals"], second.comparisons["notEquals"]);
            else
                return greater(*(first.comparisons["equals"].begin()), second.getGreaterField(),
                               second.exist_greater_or_equals) &&
                       less(*(first.comparisons["equals"].rbegin()), second.getLessField(), second.exist_less_or_equals) &&
                       !existIntersection(first.comparisons["equals"], second.comparisons["notEquals"]);
        }
        else
        {
            return subset(first.comparisons["equals"], second.comparisons["equals"]);
        }
    }

    bool PredicateImplicationChecker::genPredicateMap(
            std::unordered_map<String, std::unordered_map<String, std::set<Field>>> & predicate_map,
            const ASTPtr & predicate, bool opposite)
    {
        if (auto * function = predicate->as<ASTFunction>())
        {
            if (isComparisonFunctionName(function->name) || isLikeFunctionName(function->name))
            {
                auto & expr_lhs = function->arguments->children.at(0);
                if (where)
                    assertNoAggregates(expr_lhs);

                auto * expr_rhs = function->arguments->children.at(1)->as<ASTLiteral>();
                if (!expr_rhs)
                    return false;

                auto column_name = expr_lhs->getColumnName();
                auto func_name = opposite ? COMPARISON_OR_LIKE_FUNC_MAP.at(function->name) : function->name;
                auto column_search = predicate_map.find(column_name);
                if (column_search != predicate_map.end())
                {
                    auto func_search = column_search->second.find(func_name);
                    if (func_search != column_search->second.end())
                    {
                        if (func_name == "equals")
                        {
                            /// since the predicate is a conjunction, if there are several equals function,
                            /// we should only keep their fields' intersection.
                            std::set<Field> fields{expr_rhs->value};
                            std::set<Field> intersection;
                            std::set_intersection(func_search->second.begin(), func_search->second.end(),
                                                  fields.begin(), fields.end(),
                                                  std::inserter(intersection, intersection.begin()));
                            column_search->second[func_name] = intersection;
                        }
                        else
                            func_search->second.emplace(expr_rhs->value);
                    }
                    else
                        column_search->second.emplace(std::make_pair(func_name, std::set<Field>{expr_rhs->value}));
                }
                else
                {
                    std::set<Field> fields{expr_rhs->value};
                    predicate_map.emplace(column_name, std::unordered_map<String, std::set<Field>>{{func_name, fields}});
                }
            }
            else if (function->name == "in")
            {
                auto & expr_lhs = function->arguments->children.at(0);
                if (where)
                    assertNoAggregates(expr_lhs);

                auto & expr_rhs = function->arguments->children.at(1);
                std::set<Field> fields;

                if (auto * ast_literal = expr_rhs->as<ASTLiteral>())
                {
                    fields.emplace(ast_literal->value);
                }
                else if (auto * ast_func = expr_rhs->as<ASTFunction>(); ast_func && ast_func->name == "tuple")
                {
                    /// extract fields from tuple function
                    for (auto & child : ast_func->arguments->children)
                    {
                        if (auto * literal = child->as<ASTLiteral>())
                            fields.emplace(literal->value);
                        else
                            return false;
                    }
                }
                else
                {
                    return false;
                }

                auto column_name = expr_lhs->getColumnName();
                /// IN function can use equals/notEquals to represent
                String func_name = opposite ? "notEquals" : "equals";
                auto column_search = predicate_map.find(column_name);
                if (column_search != predicate_map.end())
                {
                    auto func_search = column_search->second.find(func_name);
                    if (func_search != column_search->second.end())
                    {
                        if (func_name == "equals")
                        {
                            std::set<Field> intersection;
                            std::set_intersection(func_search->second.begin(), func_search->second.end(),
                                                  fields.begin(), fields.end(),
                                                  std::inserter(intersection, intersection.begin()));
                            column_search->second[func_name] = intersection;
                        }
                        else
                            func_search->second.insert(fields.begin(), fields.end());
                    }
                    else
                        column_search->second.emplace(std::make_pair(func_name, fields));
                }
                else
                    predicate_map.emplace(column_name, std::unordered_map<String, std::set<Field>>{{func_name, fields}});
            }
            else if (function->name == "notIn")
            {
                function->name = "in";
                return genPredicateMap(predicate_map, predicate, !opposite);
            }
            else if (function->name == "not")
            {
                auto & operand = function->arguments->children.at(0);
                return genPredicateMap(predicate_map, operand, !opposite);
            }
            else
                return false;
        }
        else
            return false;

        return true;
    }

    CompactedComparison
    PredicateImplicationChecker::compactComparison(std::unordered_map<String, std::set<Field>> & comparisons)
    {
        CompactedComparison compacted;

        /// Step 1. Compact like and notLike
        auto like_search = comparisons.find("like");
        auto not_like_search = comparisons.find("notLike");
        if (like_search == comparisons.end() && not_like_search == comparisons.end())
        {
            /// do nothing
        }
        else if (like_search == comparisons.end() && not_like_search != comparisons.end())
        {
            compacted.exist_not_like = true;
            compacted.comparisons.emplace("notLike", not_like_search->second);
        }
        else if (like_search != comparisons.end() && not_like_search == comparisons.end())
        {
            compacted.exist_like = true;
            compacted.comparisons.emplace("like", like_search->second);
        }
        else
        {
            if (existIntersection(like_search->second, not_like_search->second))
            {
                compacted.always_false = true;
                return compacted;
            }

            compacted.exist_like = true;
            compacted.comparisons.emplace("like", like_search->second);

            compacted.exist_not_like = true;
            compacted.comparisons.emplace("notLike", not_like_search->second);
        }

        /// Step 2. Compact equals and notEquals
        auto equals_search = comparisons.find("equals");
        auto not_equals_search = comparisons.find("notEquals");
        if (equals_search == comparisons.end() && not_equals_search == comparisons.end())
        {
            /// do nothing
        }
        else if (equals_search == comparisons.end() && not_equals_search != comparisons.end())
        {
            compacted.exist_not_equals = true;
            compacted.comparisons.emplace("notEquals", not_equals_search->second);
        }
        else if (equals_search != comparisons.end() && not_equals_search == comparisons.end())
        {
            if (equals_search->second.empty())
            {
                compacted.always_false = true;
                return compacted;
            }

            compacted.exist_equals = true;
            compacted.comparisons.emplace("equals", equals_search->second);
        }
        else
        {
            std::set<Field> fields;
            for (auto & f : equals_search->second)
            {
                if (not_equals_search->second.find(f) == not_equals_search->second.end())
                    fields.emplace(f);
            }

            if (fields.empty())
            {
                compacted.always_false = true;
                return compacted;
            }

            compacted.exist_equals = true;
            compacted.comparisons.emplace("equals", fields);
        }

        /// Step 3. Compact less and lessOrEquals
        auto less_search = comparisons.find("less");
        auto less_or_equals_search = comparisons.find("lessOrEquals");
        if (less_search == comparisons.end() && less_or_equals_search == comparisons.end())
        {
            /// do nothing
        }
        else if (less_search == comparisons.end() && less_or_equals_search != comparisons.end())
        {
            compacted.exist_less_or_equals = true;
            std::set<Field> fields{*(less_or_equals_search->second.begin())};
            compacted.comparisons.emplace("lessOrEquals", fields);
        }
        else if (less_search != comparisons.end() && less_or_equals_search == comparisons.end())
        {
            compacted.exist_less = true;
            std::set<Field> fields{*(less_search->second.begin())};
            compacted.comparisons.emplace("less", fields);
        }
        else
        {
            auto & less_field = *(less_search->second.begin());
            auto & less_or_equals_field = *(less_or_equals_search->second.begin());

            if (less_field <= less_or_equals_field)
            {
                compacted.exist_less = true;
                std::set<Field> fields{less_field};
                compacted.comparisons.emplace("less", fields);
            }
            else
            {
                compacted.exist_less_or_equals = true;
                std::set<Field> fields{less_or_equals_field};
                compacted.comparisons.emplace("lessOrEquals", fields);
            }
        }

        /// Step 4. Compact greater and greaterOrEquals
        auto greater_search = comparisons.find("greater");
        auto greater_or_equals_search = comparisons.find("greaterOrEquals");
        if (greater_search == comparisons.end() && greater_or_equals_search == comparisons.end())
        {
            /// do nothing
        }
        else if (greater_search == comparisons.end() && greater_or_equals_search != comparisons.end())
        {
            compacted.exist_greater_or_equals = true;
            std::set<Field> fields{*(greater_or_equals_search->second.rbegin())};
            compacted.comparisons.emplace("greaterOrEquals", fields);
        }
        else if (greater_search != comparisons.end() && greater_or_equals_search == comparisons.end())
        {
            compacted.exist_greater = true;
            std::set<Field> fields{*(greater_search->second.rbegin())};
            compacted.comparisons.emplace("greater", fields);
        }
        else
        {
            auto & greater_field = *(greater_search->second.rbegin());
            auto & greater_or_equals_field = *(greater_or_equals_search->second.rbegin());

            if (greater_field >= greater_or_equals_field)
            {
                compacted.exist_greater = true;
                std::set<Field> fields{greater_field};
                compacted.comparisons.emplace("greater", fields);
            }
            else
            {
                compacted.exist_greater_or_equals = true;
                std::set<Field> fields{greater_or_equals_field};
                compacted.comparisons.emplace("greaterOrEquals", fields);
            }
        }

        /// Step 5. Compact results of step 2, 3, 4
        if (!compacted.hasLess() && !compacted.hasGreater())
        {
            /// do nothing
        }
        else if (!compacted.hasLess() && compacted.hasGreater())
        {
            Field greater_than = compacted.getGreaterField();

            if (compacted.exist_equals)
            {
                std::set<Field> fields;
                for (auto & f : compacted.comparisons["equals"])
                {
                    if (greater(f, greater_than, compacted.exist_greater_or_equals))
                        fields.emplace(f);
                }

                if (fields.empty())
                {
                    compacted.always_false = true;
                    return compacted;
                }
                else
                {
                    compacted.exist_greater = false;
                    compacted.exist_greater_or_equals = false;
                    compacted.comparisons["equals"] = fields;
                }
            }
            else if (compacted.exist_not_equals)
            {
                std::set<Field> fields;
                for (auto & f : compacted.comparisons["notEquals"])
                {
                    if (greater(f, greater_than, false))
                        fields.emplace(f);
                    else if (f == greater_than && compacted.exist_greater_or_equals)
                    {
                        /// compact greaterOrEquals and notEquals whose operand is equal,
                        /// example: (x >=1 and x!= 1) would be compacted to (x > 1)
                        compacted.exist_greater = true;
                        compacted.exist_greater_or_equals = false;
                        compacted.comparisons["greater"] = compacted.comparisons["greaterOrEquals"];
                    }
                }

                if (fields.empty())
                    compacted.exist_not_equals = false;
                else
                    compacted.comparisons["notEquals"] = fields;
            }
        }
        else if (compacted.hasLess() && !compacted.hasGreater())
        {
            Field less_than = compacted.getLessField();

            if (compacted.exist_equals)
            {
                std::set<Field> fields;
                for (auto & f : compacted.comparisons["equals"])
                {
                    if (less(f, less_than, compacted.exist_less_or_equals))
                        fields.emplace(f);
                }

                if (fields.empty())
                {
                    compacted.always_false = true;
                    return compacted;
                }
                else
                {
                    compacted.exist_less = false;
                    compacted.exist_less_or_equals = false;
                    compacted.comparisons["equals"] = fields;
                }
            }
            else if (compacted.exist_not_equals)
            {
                std::set<Field> fields;
                for (auto & f : compacted.comparisons["notEquals"])
                {
                    if (less(f, less_than, false))
                        fields.emplace(f);
                    else if (f == less_than && compacted.exist_less_or_equals)
                    {
                        /// compact lessOrEquals and notEquals whose operand is equal,
                        /// example: (x <=1 and x!= 1) would be compacted to (x < 1)
                        compacted.exist_less = true;
                        compacted.exist_less_or_equals = false;
                        compacted.comparisons["less"] = compacted.comparisons["lessOrEquals"];
                    }
                }

                if (fields.empty())
                    compacted.exist_not_equals = false;
                else
                    compacted.comparisons["notEquals"] = fields;
            }
        }
        else
        {
            Field less_than = compacted.getLessField();
            Field greater_than = compacted.getGreaterField();

            if (less(less_than, greater_than, false))
            {
                compacted.always_false = true;
                return compacted;
            }
            else if (less_than == greater_than)
            {
                /// compact lessOrEquals and greaterOrEquals whose operand is equal,
                /// example: (x <=1 and x>= 1) would be compacted to (x == 1)
                if (compacted.exist_less_or_equals && compacted.exist_greater_or_equals)
                {
                    if (compacted.exist_equals)
                    {
                        if (compacted.comparisons["equals"].find(less_than) != compacted.comparisons["equals"].end())
                            compacted.comparisons["equals"] = std::set<Field>{less_than};
                        else
                        {
                            compacted.always_false = true;
                            return compacted;
                        }
                    }
                    else if (compacted.exist_not_equals)
                    {
                        if (compacted.comparisons["notEquals"].find(less_than) != compacted.comparisons["notEquals"].end())
                        {
                            compacted.always_false = true;
                            return compacted;
                        }
                        else
                        {
                            compacted.exist_not_equals = false;
                            compacted.exist_equals = true;
                            compacted.comparisons["equals"] = std::set<Field>{less_than};
                        }
                    }
                }
                else
                {
                    compacted.always_false = true;
                    return compacted;
                }
            }
            else
            {
                if (compacted.exist_equals)
                {
                    std::set<Field> fields;
                    for (auto & f : compacted.comparisons["equals"])
                    {
                        if (less(f, less_than, compacted.exist_less_or_equals) &&
                            greater(f, greater_than, compacted.exist_greater_or_equals))
                            fields.emplace(f);
                    }

                    if (fields.empty())
                    {
                        compacted.always_false = true;
                        return compacted;
                    }
                    else
                    {
                        compacted.exist_less = false;
                        compacted.exist_less_or_equals = false;
                        compacted.exist_greater = false;
                        compacted.exist_greater_or_equals = false;
                        compacted.comparisons["equals"] = fields;
                    }
                }
                else if (compacted.exist_not_equals)
                {
                    std::set<Field> fields;
                    for (auto & f : compacted.comparisons["notEquals"])
                    {
                        if (less(f, less_than, false) && greater(f, greater_than, false))
                            fields.emplace(f);
                        else if (f == less_than && compacted.exist_less_or_equals)
                        {
                            compacted.exist_less = true;
                            compacted.exist_less_or_equals = false;
                            compacted.comparisons["less"] = compacted.comparisons["lessOrEquals"];
                        }
                        else if (f == greater_than && compacted.exist_greater_or_equals)
                        {
                            compacted.exist_greater = true;
                            compacted.exist_greater_or_equals = false;
                            compacted.comparisons["greater"] = compacted.comparisons["greaterOrEquals"];
                        }
                    }

                    if (fields.empty())
                        compacted.exist_not_equals = false;
                    else
                        compacted.comparisons["notEquals"] = fields;
                }
            }
        }

        return compacted;
    }
}
