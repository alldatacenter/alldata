/*
 * Copyright 2016-2023 ClickHouse, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypesNumber.h>
#include <DataTypes/getLeastSupertype.h>
#include <Functions/FunctionFactory.h>


namespace DB
{

namespace ErrorCodes
{
    extern const int NUMBER_OF_ARGUMENTS_DOESNT_MATCH;
}

namespace
{

/// Implements the CASE construction when it is
/// provided an expression. Users should not call this function.
class FunctionCaseWithExpression : public IFunction
{
public:
    static constexpr auto name = "caseWithExpression";
    static FunctionPtr create(ContextPtr context_) { return std::make_shared<FunctionCaseWithExpression>(context_); }

    explicit FunctionCaseWithExpression(ContextPtr context_) : context(context_) {}
    bool isVariadic() const override { return true; }
    size_t getNumberOfArguments() const override { return 0; }
    String getName() const override { return name; }
    bool useDefaultImplementationForNulls() const override { return false; }

    DataTypePtr getReturnTypeImpl(const DataTypes & args) const override
    {
        if (args.size() < 4 || args.size() % 2 != 0)
            throw Exception{"Invalid number of arguments for function " + getName(),
                ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH};

        auto for_conditions = [&args](auto && f) {
            size_t conditions_end = args.size() - 1;
            f(args[0]);
            for (size_t i = 1; i < conditions_end; i += 2)
                f(args[i]);
        };

        auto for_branches = [&args](auto && f) {
            size_t branches_end = args.size();
            for (size_t i = 2; i < branches_end; i += 2)
                f(args[i]);
            f(args.back());
        };

        DataTypes types_of_conditions;
        types_of_conditions.reserve(args.size() / 2);
        for_conditions([&](const DataTypePtr & arg) {
            types_of_conditions.emplace_back(arg);
        });

        auto condition_super_type = getLeastSupertype(types_of_conditions);

        DataTypes types_of_branches;
        types_of_branches.reserve(args.size() / 2);

        for_branches([&](const DataTypePtr & arg) {
            types_of_branches.emplace_back(arg);
        });

        return getLeastSupertype(types_of_branches);
    }

    ColumnPtr executeImpl(const ColumnsWithTypeAndName & args, const DataTypePtr & result_type, size_t input_rows_count) const override
    {
        /// Leverage function multiIf
        /// transform: expr, case1, then1, case2, then2, ... caseN, thenN, else
        ///        to: multiIf(expr = case1, then1, expr = case2, then2, ..., expr = caseN, thenN, else)

        if (args.size() < 4 || args.size() % 2 != 0)
            throw Exception{"Invalid number of arguments for function " + getName(),
                ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH};

        auto multi_if = FunctionFactory::instance().get("multiIf", context);
        auto equals = FunctionFactory::instance().get("equals", context);
        auto eq_type = std::make_shared<DataTypeUInt8>();

        ColumnsWithTypeAndName eq_args(2);
        ColumnsWithTypeAndName v;

        eq_args[0] =args[0]; /* push expr */
        v.reserve(args.size() - 1);

        /* prepare (expr = caseX, thenX) pair */
        for (size_t i = 1; i < args.size() - 1; i += 2) {
            eq_args[1] = args[i]; /* push current case */

            v.push_back({equals->build(eq_args)->execute(eq_args, eq_type, input_rows_count), eq_type, ""});
            v.push_back(args[i + 1]);
        }

        /* last else condition */
        v.push_back(args.back());

        return multi_if->build(v)->execute(v, result_type, input_rows_count);
    }

private:
    ContextPtr context;
};

}

void registerFunctionCaseWithExpression(FunctionFactory & factory)
{
    factory.registerFunction<FunctionCaseWithExpression>();

    /// These are obsolete function names.
    factory.registerFunction<FunctionCaseWithExpression>("caseWithExpr");
}

}


