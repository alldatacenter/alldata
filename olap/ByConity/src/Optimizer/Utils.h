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

#include <Interpreters/Context_fwd.h>
#include <Parsers/ASTExpressionList.h>
#include <Parsers/ASTFunction.h>
#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTLiteral.h>
#include <Parsers/ASTOrderByElement.h>
#include <Parsers/ASTSubquery.h>
#include <Parsers/ASTWindowDefinition.h>
#include <QueryPlan/Assignment.h>

#include <unordered_map>

namespace DB
{
class ProjectionStep;
struct AggregateDescription;

namespace Utils
{
    void assertIff(bool expression1, bool expression2);
    void checkState(bool expression);
    void checkState(bool expression, const String & msg);
    void checkArgument(bool expression);
    void checkArgument(bool expression, const String & msg);
    bool isIdentity(const String & symbol, const ConstASTPtr & expression);
    bool isIdentity(const Assignment & assignment);
    bool isIdentity(const Assignments & assignments);
    bool isIdentity(const ProjectionStep & project);
    std::unordered_map<String, String> computeIdentityTranslations(Assignments & assignments);
    ASTPtr extractAggregateToFunction(const AggregateDescription & agg_descr);

    // this method is used to deal with function names which are case-insensitive or have an alias to.
    // should be called after `registerFunctions`
    bool checkFunctionName(const ASTFunction & function, const String & expect_name);
    inline bool checkFunctionName(const ASTPtr & function_ptr, const String & expect_name)
    {
        return checkFunctionName(function_ptr->as<ASTFunction &>(), expect_name);
    }

    /**
     * Ordering used to determine ASTPtr preference when determining canonicals
     *
     * Current cost heuristic:
     * 1) Prefer fewer input symbols
     * 2) Prefer smaller expression trees
     * 3) Sort the expressions alphabetically - creates a stable consistent ordering (extremely useful for unit testing)
     */
    struct ConstASTPtrOrdering
    {
        bool operator()(const ConstASTPtr & predicate_1, const ConstASTPtr & predicate_2) const;
    };

    //Determine whether it is NAN
    bool isFloatingPointNaN(const DataTypePtr & type, const Field & value);

    String flipOperator(const String & name);

    template <typename T>
    static std::vector<std::vector<T>> powerSet(std::vector<T> set)
    {
        /*set_size of power set of a set with set_size
        n is (2**n -1)*/
        size_t pow_set_size = pow(2, set.size());
        size_t counter, j;

        /*Run from counter 111..1 to 000..1 */
        std::vector<std::vector<T>> power_set;
        for (counter = pow_set_size - 1; counter > 0; counter--)
        {
            std::vector<T> subset;
            for (j = 0; j < set.size(); j++)
            {
                /* Check if jth bit in the counter is set
                If set then print jth element from set */
                if (counter & (1 << j))
                    subset.emplace_back(set[j]);
            }
            power_set.emplace_back(subset);
        }
        return power_set;
    }

    bool canChangeOutputRows(const Assignments & assignments, ContextPtr context);
    bool canChangeOutputRows(const ProjectionStep & project, ContextPtr context);
}

}
