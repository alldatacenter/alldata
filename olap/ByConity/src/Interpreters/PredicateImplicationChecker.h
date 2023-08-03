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

#include <Core/Field.h>
#include <Parsers/IAST_fwd.h>

#include <unordered_map>
#include <set>


namespace DB
{
    /**
     * A CompactedComparison object contains comparisons info about a column in a predicate,
     * for example there is a predicate : x >= 1 and x = 1 and y > 1, the compacted comparison
     * about x is `= 1` (compacted from `>= 1 and = 1`).
     *
     * A compelely compacted comparison can have three status:
     *  1. always_false is true
     *  2. always_false is false and exist_equals is true
     *  3. always_false is false and exist_equals is false
     */
    struct CompactedComparison
    {
        /// Indicate there is contradiction between the comparisons
        bool always_false = false;

        bool exist_equals = false;
        bool exist_not_equals = false;
        bool exist_less = false;
        bool exist_less_or_equals = false;
        bool exist_greater = false;
        bool exist_greater_or_equals = false;

        bool exist_like = false;
        bool exist_not_like = false;

        std::unordered_map<String, std::set<Field>> comparisons;

        inline bool hasLess()
        {
            return exist_less || exist_less_or_equals;
        }

        inline bool hasGreater()
        {
            return exist_greater || exist_greater_or_equals;
        }

        Field getLessField()
        {
            Field less_than;
            if (exist_less)
                less_than = *(comparisons["less"].begin());
            else if (exist_less_or_equals)
                less_than = *(comparisons["lessOrEquals"].begin());

            return less_than;
        }

        Field getGreaterField()
        {
            Field greater_than;
            if (exist_greater)
                greater_than = *(comparisons["greater"].begin());
            else if (exist_greater_or_equals)
                greater_than = *(comparisons["greaterOrEquals"].begin());

            return greater_than;
        }
    };

    /**
     * This class is used to check if one predicate implies another.
     * Such implication check reduces to a SAT problem which is NP-Complete.
     *
     * For example:
     *   x > 2 implies x > 1, but not vice versa.
     */
    class PredicateImplicationChecker
    {
    public:
        /**
         *
         * @param first The first predicate
         * @param second The second predicate
         * @param where_ Indicate whether the predicates are where/prewhere/implicitwhere expression in a SQL
         */
        PredicateImplicationChecker(const ASTPtr & first, const ASTPtr & second, bool where_);

        /// Check if the first predicate implies the second predicate.
        bool implies();

        /// Get the first predicates(decompose from DNF).
        ASTs getFirstPredicates();

    private:
        bool where;
        ASTs first_predicates;
        ASTs second_predicates;

        /// Return whether the first implies at least one predicate in seconds
        bool impliesAny(ASTPtr & first, ASTs & seconds);

        /**
         * Return whether the predicate first implies second. Both predicates are a conjunction or a simple predicate.
         * A conjunction is composed by AND, examples: `a AND b`, `NOT a AND b` and so on.
         * A simple predicate can not have any AND/OR, examples: `a`, `NOT a`.
         */
        bool impliesConjunction(ASTPtr & first, ASTPtr & second);

        /// Return whether the first compacted comparison implies the second.
        bool impliesComparison(CompactedComparison & first, CompactedComparison & second);

        /// Generate predicate usage map, the map structure is : column name -> comparison function name -> set of fields
        bool genPredicateMap(std::unordered_map<String, std::unordered_map<String, std::set<Field>>> & predicate_map,
                             const ASTPtr & predicate, bool opposite = false);

        /**
         * Compact comparisons on same column. If there is contradiction between the comparisons,
         * we'll set always_false to true and return.
         *
         * steps:
         *   1. Compact equals and notEquals
         *   2. Compact less and lessOrEquals
         *   3. Compact greater and greaterOrEquals
         *   4. Compact results of step 1, 2, 3
         *
         * Example: (x == 1 and x >= 1) can be compacted to (x == 1)
         */
        CompactedComparison compactComparison(std::unordered_map<String, std::set<Field>> & comparisons);
    };
}
