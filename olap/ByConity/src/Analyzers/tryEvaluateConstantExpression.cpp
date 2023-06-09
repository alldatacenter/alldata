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

#include <Analyzers/tryEvaluateConstantExpression.h>
#include <DataTypes/DataTypesNumber.h>
#include <Columns/ColumnsNumber.h>
#include <Columns/ColumnConst.h>
#include <Interpreters/TreeRewriter.h>
#include <Interpreters/FunctionNameNormalizer.h>
#include <Interpreters/ReplaceQueryParameterVisitor.h>
#include <Interpreters/ExpressionAnalyzer.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int UNKNOWN_IDENTIFIER;
}

std::optional<Field> tryEvaluateConstantExpression(const ASTPtr & node, ContextPtr context)
{
    NamesAndTypesList source_columns = {{ "_dummy", std::make_shared<DataTypeUInt8>() }};
    auto ast = node->clone();
    ReplaceQueryParameterVisitor param_visitor(context->getQueryParameters());
    param_visitor.visit(ast);

    if (context->getSettingsRef().normalize_function_names)
        FunctionNameNormalizer().visit(ast.get());

    String name = ast->getColumnName();
    TreeRewriterResultPtr syntax_result;

    try
    {
        syntax_result = TreeRewriter(context).analyze(ast, source_columns);
    }
    catch (Exception & ex)
    {
        if (ex.code() == ErrorCodes::UNKNOWN_IDENTIFIER)
            return std::nullopt;

        throw;
    }

    ExpressionActionsPtr expr_for_constant_folding = ExpressionAnalyzer(ast, syntax_result, context).getConstActions();

    /// There must be at least one column in the block so that it knows the number of rows.
    Block block_with_constants{{ ColumnConst::create(ColumnUInt8::create(1, 0), 1), std::make_shared<DataTypeUInt8>(), "_dummy" }};

    expr_for_constant_folding->execute(block_with_constants);

    if (!block_with_constants || block_with_constants.rows() == 0)
        return std::nullopt;

    if (!block_with_constants.has(name))
        return std::nullopt;

    const ColumnWithTypeAndName & result = block_with_constants.getByName(name);
    const IColumn & result_column = *result.column;

    /// Expressions like rand() or now() are not constant
    if (!isColumnConst(result_column))
        return std::nullopt;

    return result_column[0];
}

}
