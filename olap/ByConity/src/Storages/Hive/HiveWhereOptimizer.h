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

#include <memory>
#include <set>
#include <unordered_map>
#include <Storages/IStorage.h>
#include <Storages/SelectQueryInfo.h>
#include <boost/noncopyable.hpp>

namespace DB
{
class ASTSelectQuery;
class StorageCnchHive;
using StoragePtr = std::shared_ptr<IStorage>;

class HiveWhereOptimizer : private boost::noncopyable
{
public:
    HiveWhereOptimizer(const SelectQueryInfo & query_info_, ContextPtr & /*context_*/, const StoragePtr & storage_);

    void implicitwhereOptimize() const;

    struct Condition
    {
        ASTPtr node;
        NameSet identifiers;
        bool viable = false; //is partitionkey condition
        bool good = false;

        auto tuple() const { return std::make_tuple(!viable, !good); }

        bool operator<(const Condition & rhs) const { return tuple() < rhs.tuple(); }

        Condition clone() const
        {
            Condition condition;
            condition.node = node->clone();
            condition.identifiers = identifiers;
            condition.viable = viable;
            condition.good = good;
            return condition;
        }
    };

    using Conditions = std::list<Condition>;

    ASTs getConditions(const ASTPtr & ast) const;

    ASTs getWhereOptimizerConditions(const ASTPtr & ast) const;

    void implicitAnalyzeImpl(Conditions & res, const ASTPtr & node) const;

    /// Transform conjunctions chain in WHERE expression to Conditions list.
    Conditions implicitAnalyze(const ASTPtr & expression) const;

    /// Transform Conditions list to WHERE or PREWHERE expression.
    ASTPtr reconstruct(const Conditions & conditions) const;

    bool isValidPartitionColumn(const IAST * condition) const;

    bool isSubsetOfTableColumns(const NameSet & identifiers) const;

    // bool hasColumnOfTableColumns(const HiveWhereOptimizer::Conditions & conditions) const;
    // bool hasColumnOfTableColumns() const;
    // bool isInTableColumns(const ASTPtr & node) const;

    bool getUsefullFilter(String & filter);
    bool convertImplicitWhereToUsefullFilter(String & filter);
    bool convertWhereToUsefullFilter(ASTs & conditions, String & filter);

private:
    using StringSet = std::unordered_set<String>;

    StringSet table_columns;
    ASTPtr partition_expr_ast;
    // const Context & context;
    ASTSelectQuery & select;
    const StoragePtr & storage;
};

}
