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

#include <Interpreters/predicateExpressionsUtils.h>
#include <Parsers/ASTExpressionList.h>
#include <Parsers/ASTFunction.h>
#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTLiteral.h>
#include <Parsers/ASTSelectQuery.h>
#include <Parsers/ASTSubquery.h>
#include <Parsers/queryToString.h>
#include <Storages/Hive/HiveWhereOptimizer.h>
#include <Storages/StorageCnchHive.h>
#include <Common/typeid_cast.h>


namespace DB
{
namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
}

HiveWhereOptimizer::HiveWhereOptimizer(const SelectQueryInfo & query_info_, ContextPtr & /*context_*/, const StoragePtr & storage_)
    : select(typeid_cast<ASTSelectQuery &>(*query_info_.query)), storage(storage_)
{
    if (const auto & cnchhive = dynamic_cast<const StorageCnchHive *>(storage.get()))
    {
        NamesAndTypesList available_real_columns = cnchhive->getInMemoryMetadataPtr()->getColumns().getAllPhysical();
        for (const NameAndTypePair & col : available_real_columns)
            table_columns.insert(col.name);

        partition_expr_ast = cnchhive->getPartitionKeyList();
    }
}

// bool HiveWhereOptimizer::hasColumnOfTableColumns(const HiveWhereOptimizer::Conditions & conditions) const
// {
//     for (auto it = conditions.begin(); it != conditions.end(); ++it)
//     {
//         if (isSubsetOfTableColumns(it->identifiers))
//             return true;
//     }
//     return false;
// }

// bool HiveWhereOptimizer::hasColumnOfTableColumns() const
// {
//     if (isInTableColumns(select.select())
//         || isInTableColumns(select.groupBy())
//         || isInTableColumns(select.having())
//         || isInTableColumns(select.orderBy())
//         || isInTableColumns(select.limitByValue())
//         || isInTableColumns(select.limitBy()))
//         return true;
//     return false;
// }

// bool HiveWhereOptimizer::isInTableColumns(const ASTPtr & node) const
// {
//     if (!node)
//         return false;

//     if (const auto * identifier = node->as<ASTIdentifier>())
//     {
//         if (table_columns.count(identifier->name))
//             return true;
//     }
//     if (!node->as<ASTSubquery>())
//     {
//         for (const auto & child : node->children)
//             if(isInTableColumns(child))
//                 return true;
//     }

//     return false;
// }

/// get where condition usefull filter
/// for example:  app_name is partition key.
/// app_name IN ('test', 'test2') will be convert to ((app_name = 'test') or (app_name = 'test2'))
bool HiveWhereOptimizer::convertWhereToUsefullFilter(ASTs & conditions, String & filter)
{
    if (conditions.empty())
        return false;

    ASTs ret_conditions;

    for (const auto & condition : conditions)
    {
        bool flag = false;
        if (const auto & func = typeid_cast<const ASTFunction *>(condition.get()))
        {
            if (func->name == "and")
                return false;

            if (func->name == "in")
            {
                const ASTPtr left_arg = func->arguments->children[0];
                const ASTPtr right_arg = func->arguments->children[1];

                LOG_TRACE(
                    &Poco::Logger::get("HiveWhereOptimizer"),
                    "right arg : {} left arg: {}",
                    right_arg->getAliasOrColumnName(),
                    left_arg->getAliasOrColumnName());

                for (auto & child : partition_expr_ast->children)
                {
                    if (child->getAliasOrColumnName() == left_arg->getAliasOrColumnName())
                        flag = true;
                }

                if (!flag)
                    continue;

                const auto * second_arg_func = typeid_cast<ASTFunction *>(right_arg.get());
                if (!second_arg_func)
                    continue;

                ASTs new_right_args;
                for (const auto & child : second_arg_func->arguments->children)
                {
                    ASTLiteral * literal = typeid_cast<ASTLiteral *>(child.get());
                    if (!literal)
                        throw Exception(
                            "The conditions of IN function right arg Tuple element should be literal", ErrorCodes::LOGICAL_ERROR);

                    new_right_args.push_back(child);
                }

                for (const auto & new_right_arg : new_right_args)
                {
                    const auto new_function = makeASTFunction("equals", ASTs{left_arg, new_right_arg});
                    ret_conditions.push_back(new_function);
                }
            }
        }
    }

    if (ret_conditions.size() < 2)
        return false;

    const auto function = std::make_shared<ASTFunction>();
    function->name = "or";
    function->arguments = std::make_shared<ASTExpressionList>();
    function->children.push_back(function->arguments);

    for (const auto & elem : ret_conditions)
    {
        function->arguments->children.push_back(elem);
    }

    String prefix(" ( ");
    filter = prefix + queryToString(function) + " ) ";

    return true;
}

/// If implicit where condition contains 'in' function, convert 'in' to 'equals'
bool HiveWhereOptimizer::convertImplicitWhereToUsefullFilter(String & filter)
{
    if (!select.implicitWhere())
        return false;

    ASTs ret_conditions;
    size_t num_conditions = 0;

    if (const auto & function = typeid_cast<const ASTFunction *>(select.implicitWhere().get()))
    {
        if (function->name == "and")
        {
            auto & conditions = function->arguments->children;
            num_conditions = conditions.size();
            for (const auto & condition : conditions)
            {
                if (const auto & func = typeid_cast<const ASTFunction *>(condition.get()))
                {
                    if (func->name == "and")
                        throw Exception("The conditions of implicit_where should be linearized", ErrorCodes::LOGICAL_ERROR);

                    if (func->name == "in")
                    {
                        const ASTPtr left_arg = func->arguments->children[0];
                        const ASTPtr right_arg = func->arguments->children[1];

                        ASTLiteral * literal = typeid_cast<ASTLiteral *>(right_arg.get());
                        if (!literal)
                            throw Exception(
                                "The conditions of implicit_where IN function right arg should be literal", ErrorCodes::LOGICAL_ERROR);

                        const auto new_function = makeASTFunction("equals", ASTs{left_arg, right_arg});
                        ret_conditions.push_back(new_function);

                        continue;
                    }

                    ///
                    if (!isComparisonFunctionName(func->name))
                        continue;
                }

                ret_conditions.push_back(condition);
            }
        }
    }

    /// only have one condition
    if (ret_conditions.empty())
    {
        const auto & func = typeid_cast<const ASTFunction *>(select.implicitWhere().get());
        if (func->name == "in")
        {
            const ASTPtr left_arg = func->arguments->children[0];
            const ASTPtr right_arg = func->arguments->children[1];

            ASTLiteral * literal = typeid_cast<ASTLiteral *>(right_arg.get());
            if (!literal)
                throw Exception("The conditions of implicit_where IN function right arg should be literal", ErrorCodes::LOGICAL_ERROR);

            const auto new_function = makeASTFunction("equals", ASTs{left_arg, right_arg});
            filter = queryToString(new_function);
            return true;
        }

        return false;
    }

    const auto function = std::make_shared<ASTFunction>();
    function->name = "and";
    function->arguments = std::make_shared<ASTExpressionList>();
    function->children.push_back(function->arguments);

    for (const auto & elem : ret_conditions)
    {
        function->arguments->children.push_back(elem);
    }

    filter = queryToString(function);

    return num_conditions == ret_conditions.size();
}

/// get implicit where conditions,
/// if all of contitions is Comparison Function, it will be usefull filter,
/// ThriftHiveMetastoreClient interface can directly use it to filter partitions.
bool HiveWhereOptimizer::getUsefullFilter(String & filter)
{
    if (!select.implicitWhere())
        return false;

    ASTs ret_conditions;
    bool is_all_usefull = true;

    if (const auto & function = typeid_cast<const ASTFunction *>(select.implicitWhere().get()))
    {
        if (function->name == "and")
        {
            auto & conditions = function->arguments->children;
            for (const auto & condition : conditions)
            {
                if (const auto & func = typeid_cast<const ASTFunction *>(condition.get()))
                {
                    if (func->name == "and")
                        throw Exception("The conditions of implicit_where should be linearized", ErrorCodes::LOGICAL_ERROR);

                    if (!isComparisonFunctionName(func->name))
                    {
                        is_all_usefull = false;
                        continue;
                    }
                }

                ret_conditions.push_back(condition);
            }
        }
    }

    if (ret_conditions.empty())
    {
        const auto & func = typeid_cast<const ASTFunction *>(select.implicitWhere().get());
        String name = func->name;
        if (!isComparisonFunctionName(func->name))
            return false;

        filter = queryToString(select.implicitWhere());
        return true;
    }
    else if (ret_conditions.size() == 1)
    {
        filter = queryToString(ret_conditions[0]);
        return is_all_usefull;
    }

    const auto function = std::make_shared<ASTFunction>();
    function->name = "and";
    function->arguments = std::make_shared<ASTExpressionList>();
    function->children.push_back(function->arguments);

    for (const auto & elem : ret_conditions)
    {
        function->arguments->children.push_back(elem);
    }
    filter = queryToString(function);
    return is_all_usefull;
}

void HiveWhereOptimizer::implicitwhereOptimize() const
{
    if (!select.where() || select.implicitWhere())
        return;

    Conditions where_conditions = implicitAnalyze(select.where());
    Conditions implicitwhere_conditions;

    while (!where_conditions.empty())
    {
        auto it = std::min_element(where_conditions.begin(), where_conditions.end());
        if (!it->viable)
            break;

        implicitwhere_conditions.splice(implicitwhere_conditions.end(), where_conditions, it);
    }

    if (implicitwhere_conditions.empty())
        return;

    select.setExpression(ASTSelectQuery::Expression::IMPLICITWHERE, reconstruct(implicitwhere_conditions));
    select.setExpression(ASTSelectQuery::Expression::WHERE, reconstruct(where_conditions));
}

bool HiveWhereOptimizer::isSubsetOfTableColumns(const NameSet & identifiers) const
{
    for (const auto & identifier : identifiers)
        if (table_columns.count(identifier) == 0)
            return false;

    return true;
}

HiveWhereOptimizer::Conditions HiveWhereOptimizer::implicitAnalyze(const ASTPtr & expression) const
{
    Conditions res;
    implicitAnalyzeImpl(res, expression);

    return res;
}

ASTPtr HiveWhereOptimizer::reconstruct(const Conditions & conditions) const
{
    if (conditions.empty())
        return {};

    if (conditions.size() == 1)
        return conditions.front().node;

    const auto function = std::make_shared<ASTFunction>();
    function->name = "and";
    function->arguments = std::make_shared<ASTExpressionList>();
    function->children.push_back(function->arguments);

    for (const auto & elem : conditions)
    {
        function->arguments->children.push_back(elem.node);
    }

    return function;
}

bool HiveWhereOptimizer::isValidPartitionColumn(const IAST * condition) const
{
    const auto * function = typeid_cast<const ASTFunction *>(condition);

    if (!function || !partition_expr_ast)
        return false;

    if (function->arguments->children.size() == 1)
        return false;

    bool flag = false;
    for (auto & arg_child : function->arguments->children)
    {
        const auto & identifier = typeid_cast<const ASTIdentifier *>(arg_child.get());
        const auto & func = typeid_cast<const ASTFunction *>(arg_child.get());
        const auto & literal = typeid_cast<const ASTLiteral *>(arg_child.get());

        if (!identifier && !func && !literal)
            return false;
        if ((func || identifier) && flag)
            return false;

        for (auto & child : partition_expr_ast->children)
        {
            if (identifier || func)
            {
                /// if partition column name, set flag = true
                if (child->getAliasOrColumnName() == arg_child->getAliasOrColumnName())
                    flag = true;
            }
        }
        if (!flag && !literal)
            return false;
    }

    return flag;
}

static void collectIdentifiersNoSubqueries(const ASTPtr & ast, NameSet & set)
{
    if (auto opt_name = tryGetIdentifierName(ast))
        return (void)set.insert(*opt_name);

    if (ast->as<ASTSubquery>())
        return;

    for (const auto & child : ast->children)
        collectIdentifiersNoSubqueries(child, set);
}

ASTs HiveWhereOptimizer::getWhereOptimizerConditions(const ASTPtr & ast) const
{
    if (!ast)
        return {};

    ASTs ret_conditions;

    if (const auto & function = typeid_cast<const ASTFunction *>(ast.get()))
    {
        if (function->name == "and")
        {
            auto & conditions = function->arguments->children;
            for (const auto & condition : conditions)
            {
                if (const auto & func = typeid_cast<const ASTFunction *>(condition.get()))
                {
                    if (func->name == "and")
                    {
                        return {};
                    }
                }

                ret_conditions.push_back(condition);
            }
        }
    }

    if (ret_conditions.empty())
    {
        const auto & func = typeid_cast<const ASTFunction *>(ast.get());
        if (isComparisonFunctionName(func->name) || isInFunctionName(func->name) || isLikeFunctionName(func->name))
            return {ast};
    }

    return ret_conditions;
}

ASTs HiveWhereOptimizer::getConditions(const ASTPtr & ast) const
{
    if (!ast)
        return {};

    ASTs ret_conditions;

    if (const auto & function = typeid_cast<const ASTFunction *>(ast.get()))
    {
        if (function->name == "and")
        {
            auto & conditions = function->arguments->children;
            for (const auto & condition : conditions)
            {
                if (const auto & func = typeid_cast<const ASTFunction *>(condition.get()))
                    if (func->name == "and" || func->name == "or" || func->name == "in")
                        return {};

                ret_conditions.push_back(condition);
            }
        }
    }


    if (ret_conditions.empty())
    {
        const auto & func = typeid_cast<const ASTFunction *>(ast.get());
        if (isComparisonFunctionName(func->name) || isInFunctionName(func->name) || isLikeFunctionName(func->name))
            return {ast};
    }

    return ret_conditions;
}

void HiveWhereOptimizer::implicitAnalyzeImpl(Conditions & res, const ASTPtr & node) const
{
    if (const auto * func_and = node->as<ASTFunction>(); func_and && func_and->name == "and")
    {
        for (const auto & elem : func_and->arguments->children)
            implicitAnalyzeImpl(res, elem);
    }
    else
    {
        Condition cond;
        cond.node = node;
        cond.viable = false;

        collectIdentifiersNoSubqueries(node, cond.identifiers);

        if (isSubsetOfTableColumns(cond.identifiers) && isValidPartitionColumn(node.get()))
        {
            cond.viable = true;
            cond.good = true;
        }
        res.emplace_back(std::move(cond));
    }
}

}
