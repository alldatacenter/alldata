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

#include <Analyzers/Analysis.h>

namespace DB
{

struct ExprAnalyzerOptions
{
    // aggregate functions are not allowed in ON/WHERE/GROUP BY clauses
    enum class AggregateSupport
    {
        DISALLOWED,
        ALLOWED
    };

    // window functions are not allowed in ON/WHERE/GROUP BY/HAVING clauses
    enum class WindowSupport
    {
        DISALLOWED,
        ALLOWED
    };

    // currently, subqueries are not allowed in ON clause
    enum class SubquerySupport
    {
        DISALLOWED,
        // UNCORRELATED,
        CORRELATED
    };

    String statement_name;                                                // the statement name of the analyzed expression,
                                                                          // used for error reporting

    ASTSelectQuery * select_query = nullptr;                              // the ASTSelectQuery of the analyzed expression,
                                                                          // used for register subquery, aggregates & windows functions

    AggregateSupport aggregate_support = AggregateSupport::DISALLOWED;
    WindowSupport window_support = WindowSupport::DISALLOWED;
    SubquerySupport subquery_support = SubquerySupport::DISALLOWED;

    // constructor
    ExprAnalyzerOptions(String statement_name_ = ""): statement_name(std::move(statement_name_)) // NOLINT(google-explicit-constructor)
    {}

    // setters
    ExprAnalyzerOptions & selectQuery(ASTSelectQuery & query)
    {
        select_query = &query;
        return *this;
    }

    ExprAnalyzerOptions & aggregateSupport(AggregateSupport arg)
    {
        aggregate_support = arg;
        return *this;
    }

    ExprAnalyzerOptions & windowSupport(WindowSupport arg)
    {
        window_support = arg;
        return *this;
    }

    ExprAnalyzerOptions & subquerySupport(SubquerySupport arg)
    {
        subquery_support = arg;
        return *this;
    }
};

class ExprAnalyzer
{
public:
    /**
     * Expression analyze entry method.
     *
     * Traverse the expression tree and do below things:
     * 1. Perform basic expression validations:
     *    - ASTIdentifier should be resolved to a FieldDescription
     *    - ASTFunction should have correct arguments
     *    - function type of a window function must be a AggregateFunction or NavigationFunction
     *    - no nested window function under a window/aggregate function
     *    - no nested aggregate function under a aggregate function
     *    - no subquery under a lambda function(TODO: add support)
     * 2. For each sub expression, register its return type into Analysis::expression_types
     * 3. For each ASTIdentifier referring to a table column, register it into Analysis::column_references
     * 4. Set determinism for ASTFunction.
     * 5. Register subquery expressions.
     * 6. Register aggregate functions.
     * 7. Register window functions.
     */
    static DataTypePtr analyze(ASTPtr expression,
                               ScopePtr scope,
                               ContextMutablePtr context,
                               Analysis & analysis,
                               ExprAnalyzerOptions options = ExprAnalyzerOptions {});
};

}
