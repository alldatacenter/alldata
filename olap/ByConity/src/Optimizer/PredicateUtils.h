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

#include <Parsers/ASTVisitor.h>
#include <Parsers/IAST_fwd.h>
#include <QueryPlan/PlanNode.h>

namespace DB
{
class PredicateUtils
{
public:

    static bool equals(ASTPtr & p1, ASTPtr & p2);
    static bool equals(ConstASTPtr & p1, ConstASTPtr & p2);

    /**
     * Extract predicate according 'and' function.
     *
     * (A & B & C & D) =>  A, B, C, D
     */
    static std::vector<ConstASTPtr> extractConjuncts(ConstASTPtr predicate);

    /**
     * Extract predicate according 'or' function.
     *
     * (A & B) | (C & D) =>  (A & B), (C & D)
     */
    static std::vector<ConstASTPtr> extractDisjuncts(ConstASTPtr predicate);

    /**
     * Extract predicate according function's name.
     */
    static std::vector<ConstASTPtr> extractPredicate(ConstASTPtr predicate);

    /**
     * Extract sub-predicate :
     *
     * (A & B & C & D) | (E & F & G & H) | (I & J & K & L) =>
     *
     * A, B, C, D
     * E, F, G, H
     * I, J, K, L
     */
    static std::vector<std::vector<ConstASTPtr>> extractSubPredicates(ConstASTPtr predicate);

    /**
     * Extract common predicate
     *
     * (A & B & C & X & Y) | (E & F & G & X & Y) | (I & J & K & X & Y) =>
     *
     * (X & Y ) & ((A & B & C) | (E & F & G) | (I & J & K))
     */
    static ConstASTPtr extractCommonPredicates(ConstASTPtr predicate, ContextMutablePtr & context);

    /**
     * Applies the boolean distributive property.
     *
     * For example:
     * ( A & B ) | ( C & D ) => ( A | C ) & ( A | D ) & ( B | C ) & ( B | D)
     *
     * Returns the original expression if the expression is non-deterministic or if the distribution will
     * expand the expression by too much.
     */
    static ConstASTPtr distributePredicate(ConstASTPtr or_predicate, ContextMutablePtr & context);

    static ASTPtr combineConjuncts(const std::vector<ConstASTPtr> & predicates);
    static ASTPtr combineDisjuncts(const std::vector<ConstASTPtr> & predicates);
    static ASTPtr combineDisjunctsWithDefault(const std::vector<ConstASTPtr> & predicates, const ASTPtr & default_ast);
    static ASTPtr combinePredicates(const String & fun, std::vector<ConstASTPtr> predicates);

    static bool isTruePredicate(const ConstASTPtr & predicate);
    static bool isFalsePredicate(const ConstASTPtr & predicate);

    static bool isInliningCandidate(ConstASTPtr & predicate, ProjectionNode & node);
    static ASTPtr extractJoinPredicate(JoinNode &);
    static bool isJoinClause(ConstASTPtr expression, std::set<String> & left_symbols, std::set<String> & right_symbols, ContextMutablePtr & context);
    static bool
    isJoinClauseUnmodified(std::set<std::pair<String, String>> & join_clauses, const Names & left_keys, const Names & right_keys);

    /**
     * @return residue expression if source expression is stronger than target,
     *         {@code true} if it is equal to target,
     *         {@code null} f it is weaker than target.
     */
    static ASTPtr splitPredicates(const ConstASTPtr & source, const ConstASTPtr & target);

    static std::pair<std::vector<std::pair<ConstASTPtr, ConstASTPtr>>, std::vector<ConstASTPtr>>
    extractEqualPredicates(const std::vector<ConstASTPtr> & predicates);

private:
    static String flip(const String & fun_name);
    static void extractPredicate(ConstASTPtr & predicate, const std::string & fun_name, std::vector<ConstASTPtr> & result);
    static std::vector<std::pair<ConstASTPtr, String>>
    removeAll(std::vector<std::pair<ConstASTPtr, String>> & collection, std::set<String> & elements_to_remove);
    static std::set<std::vector<ConstASTPtr>> cartesianProduct(std::vector<std::set<ConstASTPtr>> &);
};

}
