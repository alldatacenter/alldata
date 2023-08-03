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

#include <Poco/String.h>
#include <Core/Names.h>
#include <Interpreters/QueryNormalizer.h>
#include <Interpreters/IdentifierSemantic.h>
#include <Interpreters/evaluateConstantExpression.h>
#include <Interpreters/Context.h>
#include <Parsers/ASTFunction.h>
#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTLiteral.h>
#include <Parsers/ASTPartition.h>
#include <Parsers/ASTSelectQuery.h>
#include <Parsers/ASTQueryParameter.h>
#include <Parsers/ASTTablesInSelectQuery.h>
#include <DataTypes/MapHelpers.h> // for getImplicitFileNameForMapKey
#include <Common/StringUtils/StringUtils.h>
#include <Common/quoteString.h>
#include <Common/FieldVisitorToString.h>
#include <IO/WriteHelpers.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int TOO_DEEP_AST;
    extern const int CYCLIC_ALIASES;
    extern const int UNKNOWN_QUERY_PARAMETER;
    extern const int BAD_ARGUMENTS;
	extern const int NUMBER_OF_ARGUMENTS_DOESNT_MATCH;
}


class CheckASTDepth
{
public:
    explicit CheckASTDepth(QueryNormalizer::Data & data_)
        : data(data_)
    {
        if (data.level > data.settings.max_ast_depth)
            throw Exception("Normalized AST is too deep. Maximum: " + toString(data.settings.max_ast_depth), ErrorCodes::TOO_DEEP_AST);
        ++data.level;
    }

    ~CheckASTDepth()
    {
        --data.level;
    }

private:
    QueryNormalizer::Data & data;
};


class RestoreAliasOnExitScope
{
public:
    explicit RestoreAliasOnExitScope(String & alias_)
        : alias(alias_)
        , copy(alias_)
    {}

    ~RestoreAliasOnExitScope()
    {
        alias = copy;
    }

private:
    String & alias;
    const String copy;
};

String QueryNormalizer::getMapKeyName(ASTFunction & node, Data & data)
{
    ASTLiteral * key_lit = node.arguments->children[1]->as<ASTLiteral>(); // Constant Literal
    ASTFunction * key_func = node.arguments->children[1]->as<ASTFunction>(); // for constant foldable functions' case

    String key_name;
    if (key_lit) // key is literal
    {
        key_name = key_lit->getColumnName();
    }
    else if (key_func)
    {
        // check whether key_func's inputs are all constant literal, if yes, invoke constant folding logic.
        // This handling is specially added for Date key which is toDate('YYYY-MM_DD') in SQL
        bool all_input_const = true;
        for (auto & c : key_func->arguments->children)
        {
            if (!c->as<ASTLiteral>())
            {
                all_input_const = false;
                break;
            }
        }

        if (all_input_const)
        {
            std::pair<Field, DataTypePtr> value_raw = evaluateConstantExpression(node.arguments->children[1], data.context);
            key_name = applyVisitor(DB::FieldVisitorToString(), value_raw.first);
        }
    }

    return key_name;
}

void QueryNormalizer::rewriteMapElement(ASTPtr & ast, const String & map_name, const String & key_name)
{
    String my_alias = ast->tryGetAlias();
    if (!key_name.empty())
    {
        // create identifier
        String implicit_name = getImplicitColNameForMapKey(map_name, key_name);
        ast = std::make_shared<ASTIdentifier>(implicit_name);
        ast->as<ASTIdentifier>()->is_implicit_map_key = true;
        //set alias back
        ast->setAlias(my_alias);
    }
}

void QueryNormalizer::visit(ASTFunction & node, ASTPtr & ast, Data & data)
{
    String & func_name = node.name;

    /// Special cases for count function.
    String func_name_lowercase = Poco::toLower(func_name);
    if (endsWith(func_name_lowercase, "vcfunnelopt"))
    {
		// TODO
        // /// reuse funnel_old_rule setting for skipping repeat event in Finder case
        // if (data.settings.funnel_old_rule)
        // {
        //     func_name = func_name + "Skip";
        // }
    }
#if 0 //TODO
    else if (func_name_lowercase == "sumdistinct")
    {
        /// replace sumDistinc with internal implementation uniqSum;
        func_name = "uniqSum";
    }
    else if (func_name_lowercase == "partitionstatus")
    {
        String old_col_name_or_alias = node.getAliasOrColumnName();

        ASTs & args = node.arguments->children;
        if (args.size() != 3)
            throw Exception("Function partitionStatus only accept three arguments.", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        size_t arg_num = 0;
        args[arg_num] = evaluateConstantExpressionOrIdentifierAsLiteral(args[arg_num], data.context);
        String database_name = args[arg_num]->as<ASTLiteral &>().value.safeGet<String>();

        ++arg_num;
        args[arg_num] = evaluateConstantExpressionOrIdentifierAsLiteral(args[arg_num], data.context);
        String table_name = args[arg_num]->as<ASTLiteral &>().value.safeGet<String>();

        ++arg_num;

        auto partition = std::make_shared<ASTPartition>();
        size_t fields_count;
        String fields_str;

        String partition_name = args[arg_num]->getColumnName();

        const auto * tuple_ast = args[arg_num]->as<ASTFunction>();
        if (tuple_ast && tuple_ast->name == "tuple")
        {
            const auto * arguments_ast = tuple_ast->arguments->as<ASTExpressionList>();
            if (arguments_ast)
                fields_count = arguments_ast->children.size();
            else
                fields_count = 0;

            auto left_paren = partition_name.data();
            auto right_paren = partition_name.data() + partition_name.size();

            while (left_paren != right_paren && *left_paren != '(')
                ++left_paren;
            if (*left_paren != '(')
                throw Exception("Provided partition in wrong format.", ErrorCodes::LOGICAL_ERROR);

            while (right_paren != left_paren && *right_paren != ')')
                --right_paren;
            if (*right_paren != ')')
                throw Exception("Provided partition in wrong format.", ErrorCodes::LOGICAL_ERROR);

            fields_str = String(left_paren + 1, right_paren - left_paren - 1);
        }
        else
        {
            fields_count = 1;
            fields_str = String(partition_name.data(), partition_name.size());
        }

        partition->value = args[arg_num];
        partition->children.push_back(args[arg_num]);
        partition->fields_str = std::move(fields_str);
        partition->fields_count = fields_count;

        String partition_id;
        auto table = DatabaseCatalog::instance().getTable(database_name, table_name);
        if (auto * materialized_view = dynamic_cast<StorageMaterializedView *>(table.get()))
            partition_id = materialized_view->getTargetTable()->getPartitionIDFromQuery(partition, data.context);
        else
            partition_id = table->getPartitionIDFromQuery(partition, data.context);

        /// rewrtie the partitionStatus function
        auto new_arguments = std::make_shared<ASTExpressionList>();
        new_arguments->children.push_back(std::make_shared<ASTLiteral>(database_name));
        new_arguments->children.push_back(std::make_shared<ASTLiteral>(table_name));
        new_arguments->children.push_back(std::make_shared<ASTLiteral>(partition_id));

        node.children.clear();
        node.arguments = new_arguments;
        node.children.push_back(new_arguments);
        // set the alias to the old column name or alias of this function, then the required result header will keep consistent after rewriting.
        node.setAlias(old_col_name_or_alias);
    }
#endif
    // Rewrite mapElement(c, 'k') to implicit column __c__'k'
    else if (
        data.rewrite_map_col && startsWith(func_name_lowercase, "mapelement") && node.arguments->children.size() == 2 && data.storage
        && data.storage->supportsMapImplicitColumn())
    {
        ASTIdentifier * map_col = node.arguments->children[0]->as<ASTIdentifier>();

        if (map_col && !IdentifierSemantic::isSpecial(*map_col))
        {
            String key_name = getMapKeyName(node, data);
            String map_name = map_col->name();
            if (data.storage && data.metadata_snapshot->columns.hasPhysical(map_name))
            {
                auto map_type = data.metadata_snapshot->columns.getPhysical(map_name).type;
                if (map_type->isMap() && !map_type->isMapKVStore())
                    rewriteMapElement(ast, map_name, key_name);
            }
        }
    }
    // Support square bracket for map element. Just convert it into mapElement function.
    // Rewrite arrayElement(c, 'k') to implicit column __c__'k'
    else if (
        data.rewrite_map_col && startsWith(func_name_lowercase, "arrayelement") && node.arguments->children.size() == 2 && data.storage
        && data.storage->supportsMapImplicitColumn())
    {
        ASTIdentifier * map_col = node.arguments->children[0]->as<ASTIdentifier>();

        if (map_col && !IdentifierSemantic::isSpecial(*map_col))
        {
            String key_name = getMapKeyName(node, data);
            String map_name = map_col->name();
            if (data.storage && data.metadata_snapshot->columns.hasPhysical(map_name))
            {
                auto map_type = data.metadata_snapshot->columns.getPhysical(map_name).type;
                if (map_type->isMap())
                {
                    node.name = "mapElement"; /// convert array element to mapelement for map type
                    if (!map_type->isMapKVStore())
                        rewriteMapElement(ast, map_name, key_name);
                }
            }
        }
    }
    // Rewrite mapKeys(c) to implicite column c.key for KV store map
    else if (
        data.rewrite_map_col && startsWith(func_name_lowercase, "mapkeys") && node.arguments->children.size() == 1 && data.storage
        && data.storage->supportsMapImplicitColumn())
    {
        ASTIdentifier * map_col = node.arguments->children[0]->as<ASTIdentifier>();
        if (map_col)
        {
            DataTypePtr type = data.metadata_snapshot->columns.getPhysical(map_col->name()).type;
            if (type->isMap() && type->isMapKVStore())
            {
                String origin_alias = ast->getAliasOrColumnName();
                ast = std::make_shared<ASTIdentifier>(map_col->name() + ".key");
                ast->setAlias(origin_alias);
            }
        }
    }
    // Rewrite mapValues(c) to implicite column c.value for KV store map
    else if (
        data.rewrite_map_col && startsWith(func_name_lowercase, "mapvalues") && node.arguments->children.size() == 1 && data.storage
        && data.storage->supportsMapImplicitColumn())
    {
        ASTIdentifier * map_col = node.arguments->children[0]->as<ASTIdentifier>();
        if (map_col)
        {
            DataTypePtr type = data.metadata_snapshot->columns.getPhysical(map_col->name()).type;
            if (type->isMap() && type->isMapKVStore())
            {
                String origin_alias = ast->getAliasOrColumnName();
                ast = std::make_shared<ASTIdentifier>(map_col->name() + ".value");
                ast->setAlias(origin_alias);
            }
        }
    }
}

void QueryNormalizer::visit(ASTIdentifier & node, ASTPtr & ast, Data & data)
{
    auto & current_asts = data.current_asts;
    String & current_alias = data.current_alias;

    if (!IdentifierSemantic::getColumnName(node))
        return;

    if (data.settings.prefer_column_name_to_alias)
    {
        if (data.source_columns_set.find(node.name()) != data.source_columns_set.end())
            return;
    }

    /// If it is an alias, but not a parent alias (for constructs like "SELECT column + 1 AS column").
    auto it_alias = data.aliases.find(node.name());
    if (!data.allow_self_aliases && current_alias == node.name())
        throw Exception(ErrorCodes::CYCLIC_ALIASES, "Self referencing of {} to {}. Cyclic alias", backQuote(current_alias), backQuote(node.name()));

    if (it_alias != data.aliases.end() && current_alias != node.name())
    {
        if (!IdentifierSemantic::canBeAlias(node))
            return;

        /// We are alias for other column (node.name), but we are alias by
        /// ourselves to some other column
        const auto & alias_node = it_alias->second;

        String our_alias_or_name = alias_node->getAliasOrColumnName();
        std::optional<String> our_name = IdentifierSemantic::getColumnName(alias_node);

        String node_alias = ast->tryGetAlias();

        if (current_asts.count(alias_node.get()) /// We have loop of multiple aliases
            || (node.name() == our_alias_or_name && our_name && node_alias == *our_name)) /// Our alias points to node.name, direct loop
            throw Exception("Cyclic aliases", ErrorCodes::CYCLIC_ALIASES);

        /// Let's replace it with the corresponding tree node.
        if (!node_alias.empty() && node_alias != our_alias_or_name)
        {
            /// Avoid infinite recursion here
            auto opt_name = IdentifierSemantic::getColumnName(alias_node);
            bool is_cycle = opt_name && *opt_name == node.name();

            if (!is_cycle)
            {
                /// In a construct like "a AS b", where a is an alias, you must set alias b to the result of substituting alias a.
                ast = alias_node->clone();
                ast->setAlias(node_alias);
            }
        }
        else
        {
            if (data.context->getSettingsRef().enable_optimizer)
            {
                ast = alias_node->clone(); // For ExprAnalyzer, an aliased expression may have different analyze result with
                                           // the origin expression, thus we clone the expression so that both analyze results
                                           // can reside in the Analysis object. see also 00057_join_aliases.sql
            }
            else
            {
                ast = alias_node;
            }
        }
    }
}

void QueryNormalizer::visit(ASTTablesInSelectQueryElement & node, const ASTPtr &, Data & data)
{
    /// normalize JOIN ON section
    if (node.table_join)
    {
        auto & join = node.table_join->as<ASTTableJoin &>();
        if (join.on_expression)
            visit(join.on_expression, data);
    }
}

static bool needVisitChild(const ASTPtr & child)
{
    return !(child->as<ASTSelectQuery>() || child->as<ASTTableExpression>());
}

/// special visitChildren() for ASTSelectQuery
void QueryNormalizer::visit(ASTSelectQuery & select, const ASTPtr &, Data & data)
{
    for (auto & child : select.children)
    {
        if (needVisitChild(child))
            visit(child, data);
    }

    /// If the WHERE clause or HAVING consists of a single alias, the reference must be replaced not only in children,
    /// but also in where_expression and having_expression.
    if (select.prewhere())
        visit(select.refPrewhere(), data);
    if (select.where())
        visit(select.refWhere(), data);
    if (select.having())
        visit(select.refHaving(), data);
}

/// Don't go into subqueries.
/// Don't go into select query. It processes children itself.
/// Do not go to the left argument of lambda expressions, so as not to replace the formal parameters
///  on aliases in expressions of the form 123 AS x, arrayMap(x -> 1, [2]).
void QueryNormalizer::visitChildren(IAST * node, Data & data)
{
    if (auto * func_node = node->as<ASTFunction>())
    {
        if (func_node->tryGetQueryArgument())
        {
            if (func_node->name != "view")
                throw Exception("Query argument can only be used in the `view` TableFunction", ErrorCodes::BAD_ARGUMENTS);
            /// Don't go into query argument.
            return;
        }
        /// We skip the first argument. We also assume that the lambda function can not have parameters.
        size_t first_pos = 0;
        if (func_node->name == "lambda")
            first_pos = 1;

        if (func_node->arguments)
        {
            auto & func_children = func_node->arguments->children;

            for (size_t i = first_pos; i < func_children.size(); ++i)
            {
                auto & child = func_children[i];

                if (needVisitChild(child))
                    visit(child, data);
            }
        }

        if (func_node->window_definition)
        {
            visitChildren(func_node->window_definition.get(), data);
        }
    }
    else if (!node->as<ASTSelectQuery>())
    {
        for (auto & child : node->children)
            if (needVisitChild(child))
                visit(child, data);
    }
}

void QueryNormalizer::visit(ASTPtr & ast, Data & data)
{
    CheckASTDepth scope1(data);
    RestoreAliasOnExitScope scope2(data.current_alias);

    auto & finished_asts = data.finished_asts;
    auto & current_asts = data.current_asts;

    if (finished_asts.count(ast))
    {
        ast = finished_asts[ast];
        return;
    }

    ASTPtr initial_ast = ast;
    current_asts.insert(initial_ast.get());

    {
        String my_alias = ast->tryGetAlias();
        if (!my_alias.empty())
            data.current_alias = my_alias;
    }
    if (auto * node = ast->as<ASTFunction>())
        visit(*node, ast, data);
    else if (auto * node_id = ast->as<ASTIdentifier>())
        visit(*node_id, ast, data);
    else if (auto * node_tables = ast->as<ASTTablesInSelectQueryElement>())
        visit(*node_tables, ast, data);
    else if (auto * node_select = ast->as<ASTSelectQuery>())
        visit(*node_select, ast, data);
    else if (auto * node_param = ast->as<ASTQueryParameter>())
        throw Exception("Query parameter " + backQuote(node_param->name) + " was not set", ErrorCodes::UNKNOWN_QUERY_PARAMETER);

    /// If we replace the root of the subtree, we will be called again for the new root, in case the alias is replaced by an alias.
    if (ast.get() != initial_ast.get())
        visit(ast, data);
    else
        visitChildren(ast.get(), data);

    current_asts.erase(initial_ast.get());
    current_asts.erase(ast.get());
    if (data.ignore_alias && !ast->tryGetAlias().empty())
        ast->setAlias("");
    finished_asts[initial_ast] = ast;

    /// @note can not place it in CheckASTDepth dtor cause of exception.
    if (data.level == 1)
    {
        try
        {
            ast->checkSize(data.settings.max_expanded_ast_elements);
        }
        catch (Exception & e)
        {
            e.addMessage("(after expansion of aliases)");
            throw;
        }
    }
}

}
