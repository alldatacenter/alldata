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

#include <Interpreters/InDepthNodeVisitor.h>
#include <Interpreters/DatabaseAndTableWithAlias.h>
#include <Parsers/IAST_fwd.h>
#include <Storages/IStorage_fwd.h>

namespace DB
{

struct ASTTableExpression;
class ASTIdentifier;
class ASTQualifiedAsterisk;
class Context;
using ContextPtr = std::shared_ptr<const Context>;
class Cluster;
using ClusterPtr = std::shared_ptr<Cluster>;

class RewriteDistributedQueryMatcher
{
public:

    struct Data
    {
        std::unordered_map<IAST*, std::pair<String, String>> table_rewrite_info;
        std::vector<std::pair<DatabaseAndTableWithAlias, String>> identifier_rewrite_info;
        ClusterPtr cluster;
        String cluster_name;
        bool all_distributed = true;
    };

    static bool needChildVisit(ASTPtr & node, const ASTPtr & child);

    static void visit(ASTPtr & ast, Data & data);

    static StoragePtr tryGetTable(const ASTPtr & database_and_table, const ContextPtr & context);

    static Data collectTableInfos(const ASTPtr & query, const ContextPtr & context);

private:

    static void visit(ASTTableExpression & query, ASTPtr & node, Data & data);
    static void visit(ASTIdentifier & query, ASTPtr & node, Data & data);
    static void visit(ASTQualifiedAsterisk & query, ASTPtr & node, Data & data);
};

using RewriteDistributedQueryVisitor = InDepthNodeVisitor<RewriteDistributedQueryMatcher, true>;

}
