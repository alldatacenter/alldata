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

#include "TableFunctionRemote.h"

#include <Storages/getStructureOfRemoteTable.h>
#include <Storages/StorageDistributed.h>
#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTLiteral.h>
#include <Parsers/ASTFunction.h>
#include <Parsers/ASTExpressionList.h>
#include <Interpreters/evaluateConstantExpression.h>
#include <Interpreters/Cluster.h>
#include <Interpreters/Context.h>
#include <Interpreters/IdentifierSemantic.h>
#include <Common/typeid_cast.h>
#include <Common/parseRemoteDescription.h>
#include <TableFunctions/TableFunctionFactory.h>
#include <Core/Defines.h>
#include <common/range.h>
#include "registerTableFunctions.h"


namespace DB
{

namespace ErrorCodes
{
    extern const int NUMBER_OF_ARGUMENTS_DOESNT_MATCH;
    extern const int BAD_ARGUMENTS;
    extern const int TYPE_MISMATCH;
}


void TableFunctionRemote::parseArguments(const ASTPtr & ast_function, ContextPtr context)
{
    ASTs & args_func = ast_function->children;

    if (args_func.size() != 1)
        throw Exception(help_message, ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

    ASTs & args = args_func.at(0)->children;

    const size_t max_args = is_cluster_function ? 4 : 6;
    if (args.size() < 2 || args.size() > max_args)
        throw Exception(help_message, ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

    String cluster_name;
    String cluster_description;
    String remote_database;
    String remote_table;
    String username;
    String password;

    size_t arg_num = 0;

    auto get_string_literal = [](const IAST & node, String & res)
    {
        const auto * lit = node.as<ASTLiteral>();
        if (!lit)
            return false;

        if (lit->value.getType() != Field::Types::String)
            return false;

        res = safeGet<const String &>(lit->value);
        return true;
    };

    if (is_cluster_function)
    {
        args[arg_num] = evaluateConstantExpressionOrIdentifierAsLiteral(args[arg_num], context);
        cluster_name = args[arg_num]->as<ASTLiteral &>().value.safeGet<const String &>();
    }
    else
    {
        if (!tryGetIdentifierNameInto(args[arg_num], cluster_name))
        {
            if (!get_string_literal(*args[arg_num], cluster_description))
                throw Exception("Hosts pattern must be string literal (in single quotes).", ErrorCodes::BAD_ARGUMENTS);
        }
    }
    ++arg_num;

    const auto * function = args[arg_num]->as<ASTFunction>();

    if (function && TableFunctionFactory::instance().isTableFunctionName(function->name))
    {
        remote_table_function_ptr = args[arg_num];
        ++arg_num;
    }
    else
    {
        args[arg_num] = evaluateConstantExpressionForDatabaseName(args[arg_num], context);
        remote_database = args[arg_num]->as<ASTLiteral &>().value.safeGet<String>();

        ++arg_num;

        size_t dot = remote_database.find('.');
        if (dot != String::npos)
        {
            /// NOTE Bad - do not support identifiers in backquotes.
            remote_table = remote_database.substr(dot + 1);
            remote_database = remote_database.substr(0, dot);
        }
        else
        {
            if (arg_num >= args.size())
            {
                throw Exception(help_message, ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);
            }
            else
            {
                args[arg_num] = evaluateConstantExpressionOrIdentifierAsLiteral(args[arg_num], context);
                remote_table = args[arg_num]->as<ASTLiteral &>().value.safeGet<String>();
                ++arg_num;
            }
        }
    }

    /// Cluster function may have sharding key for insert
    if (is_cluster_function && arg_num < args.size())
    {
        sharding_key = args[arg_num];
        ++arg_num;
    }

    /// Username and password parameters are prohibited in cluster version of the function
    if (!is_cluster_function)
    {
        if (arg_num < args.size())
        {
            if (!get_string_literal(*args[arg_num], username))
            {
                username = "default";
                sharding_key = args[arg_num];
            }
            ++arg_num;
        }

        if (arg_num < args.size() && !sharding_key)
        {
            if (!get_string_literal(*args[arg_num], password))
            {
                sharding_key = args[arg_num];
            }
            ++arg_num;
        }

        if (arg_num < args.size() && !sharding_key)
        {
            sharding_key = args[arg_num];
            ++arg_num;
        }
    }

    std::set<UInt64> replica_nums;
    if (name == "cluster" && sharding_key)
    {
        if (const auto * lit = sharding_key->as<ASTLiteral>())
        {
            if (lit && lit->value.getType() == Field::Types::UInt64)
            {
                replica_nums.emplace(safeGet<const UInt64 &>(lit->value));
            }
            else if (lit && lit->value.getType() == Field::Types::Tuple)
            {
                auto replicas = lit->value.safeGet<Tuple>();

                for (auto replica : replicas)
                    replica_nums.emplace(safeGet<const UInt64 &>(replica));
            }
        }
    }

    if (arg_num < args.size())
        throw Exception(help_message, ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

    if (!cluster_name.empty())
    {
        /// Use an existing cluster from the main config
        if (name != "clusterAllReplicas")
            cluster = context->getCluster(cluster_name);
        else
            cluster = context->getCluster(cluster_name)->getClusterWithReplicasAsShards(context->getSettings());

        if (name == "cluster" && !replica_nums.empty() && *replica_nums.begin() != 0)
        {
            auto shard_addresses = cluster->getShardsAddresses();
            std::vector<std::vector<String>> names; // get name of # replica

            // Duplicate host_name will be removed to avoid sending to same host multiple times
            std::set<String> uniq_names;
            bool set_name_pass_word = false;
            for (auto& address : shard_addresses)
            {
                for (auto replica_num : replica_nums)
                {
                    if (replica_num > address.size() || replica_num <= 0)
                        continue;

                    uniq_names.insert(address[replica_num - 1].readableString());
                    if (!set_name_pass_word)
                    {
                        username = address[replica_num - 1].user;
                        password = address[replica_num - 1].password;
                        set_name_pass_word = true;
                    }
                }
            }

            for (const auto& uniq_name : uniq_names)
                names.push_back({uniq_name});

            if (names.empty())
                throw Exception("Shard list of specified replica_num is empty", ErrorCodes::BAD_ARGUMENTS);

            cluster = std::make_shared<Cluster>(context->getSettings(), names, username, password, context->getTCPPort(), false);
        }
    }
    else
    {
        /// Create new cluster from the scratch
        size_t max_addresses = context->getSettingsRef().table_function_remote_max_addresses;
        std::vector<String> shards = parseRemoteDescription(cluster_description, 0, cluster_description.size(), ',', max_addresses);

        std::vector<std::vector<String>> names;
        names.reserve(shards.size());
        for (const auto & shard : shards)
            names.push_back(parseRemoteDescription(shard, 0, shard.size(), '|', max_addresses));

        if (names.empty())
            throw Exception("Shard list is empty after parsing first argument", ErrorCodes::BAD_ARGUMENTS);

        auto maybe_secure_port = context->getTCPPortSecure();

        /// Check host and port on affiliation allowed hosts.
        for (const auto & hosts : names)
        {
            for (const auto & host : hosts)
            {
                size_t colon = host.find(':');
                if (colon == String::npos)
                    context->getRemoteHostFilter().checkHostAndPort(
                        host,
                        toString((secure ? (maybe_secure_port ? *maybe_secure_port : DBMS_DEFAULT_SECURE_PORT) : context->getTCPPort())));
                else
                    context->getRemoteHostFilter().checkHostAndPort(host.substr(0, colon), host.substr(colon + 1));
            }
        }

        cluster = std::make_shared<Cluster>(
            context->getSettings(),
            names,
            username,
            password,
            (secure ? (maybe_secure_port ? *maybe_secure_port : DBMS_DEFAULT_SECURE_PORT) : context->getTCPPort()),
            false,
            secure);
    }

    if (!remote_table_function_ptr && remote_table.empty())
        throw Exception("The name of remote table cannot be empty", ErrorCodes::BAD_ARGUMENTS);

    remote_table_id.database_name = remote_database;
    remote_table_id.table_name = remote_table;
}

StoragePtr TableFunctionRemote::executeImpl(const ASTPtr & /*ast_function*/, ContextPtr context, const std::string & table_name, ColumnsDescription cached_columns) const
{
    /// StorageDistributed supports mismatching structure of remote table, so we can use outdated structure for CREATE ... AS remote(...)
    /// without additional conversion in StorageTableFunctionProxy
    if (cached_columns.empty())
        cached_columns = getActualTableStructure(context);

    assert(cluster);
    StoragePtr res = remote_table_function_ptr
        ? StorageDistributed::create(
            StorageID(getDatabaseName(), table_name),
            cached_columns,
            ConstraintsDescription{},
            remote_table_function_ptr,
            String{},
            context,
            sharding_key,
            String{},
            String{},
            DistributedSettings{},
            false,
            cluster)
        : StorageDistributed::create(
            StorageID(getDatabaseName(), table_name),
            cached_columns,
            ConstraintsDescription{},
            String{},
            remote_table_id.database_name,
            remote_table_id.table_name,
            String{},
            context,
            sharding_key,
            String{},
            String{},
            DistributedSettings{},
            false,
            cluster);

    res->startup();
    return res;
}

ColumnsDescription TableFunctionRemote::getActualTableStructure(ContextPtr context) const
{
    assert(cluster);
    return getStructureOfRemoteTable(*cluster, remote_table_id, context, remote_table_function_ptr);
}

TableFunctionRemote::TableFunctionRemote(const std::string & name_, bool secure_)
    : name{name_}, secure{secure_}
{
    is_cluster_function = (name == "cluster" || name == "clusterAllReplicas");
    help_message = fmt::format(
        "Table function '{}' requires from 2 to {} parameters: "
        "<addresses pattern or cluster name>, <name of remote database>, <name of remote table>{}",
        name,
        is_cluster_function ? 4 : 6,
        is_cluster_function ? " [, sharding_key]" : " [, username[, password], sharding_key]");
}


void registerTableFunctionRemote(TableFunctionFactory & factory)
{
    factory.registerFunction("remote", [] () -> TableFunctionPtr { return std::make_shared<TableFunctionRemote>("remote"); });
    factory.registerFunction("remoteSecure", [] () -> TableFunctionPtr { return std::make_shared<TableFunctionRemote>("remote", /* secure = */ true); });
    factory.registerFunction("cluster", [] () -> TableFunctionPtr { return std::make_shared<TableFunctionRemote>("cluster"); });
    factory.registerFunction("clusterAllReplicas", [] () -> TableFunctionPtr { return std::make_shared<TableFunctionRemote>("clusterAllReplicas"); });
}

}
