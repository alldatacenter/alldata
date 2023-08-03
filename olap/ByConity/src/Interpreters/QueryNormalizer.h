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

#pragma once

#include <map>

#include <Parsers/IAST.h>
#include <Parsers/ASTFunction.h>
#include <Interpreters/Aliases.h>
#include <Storages/IStorage.h>
#include <Interpreters/Context_fwd.h>
#include <Core/Names.h>

namespace DB
{

class ASTSelectQuery;
class ASTIdentifier;
struct ASTTablesInSelectQueryElement;
class Context;


class QueryNormalizer
{
    /// Extracts settings, mostly to show which are used and which are not.
    struct ExtractedSettings
    {
        const UInt64 max_ast_depth;
        const UInt64 max_expanded_ast_elements;
        bool prefer_column_name_to_alias;

        template <typename T>
        ExtractedSettings(const T & settings)
            : max_ast_depth(settings.max_ast_depth)
            , max_expanded_ast_elements(settings.max_expanded_ast_elements)
            , prefer_column_name_to_alias(settings.prefer_column_name_to_alias)
        {
        }
    };

public:
    struct Data
    {
        using SetOfASTs = std::set<const IAST *>;
        using MapOfASTs = std::map<ASTPtr, ASTPtr>;

        const Aliases & aliases;
        const NameSet & source_columns_set;
        ExtractedSettings settings;

        /// tmp data
        size_t level;
        MapOfASTs finished_asts;    /// already processed vertices (and by what they replaced)
        SetOfASTs current_asts;     /// vertices in the current call stack of this method
        std::string current_alias;  /// the alias referencing to the ancestor of ast (the deepest ancestor with aliases)
        const bool ignore_alias; /// normalize query without any aliases

        /// It's Ok to have "c + 1 AS c" in queries, but not in table definition
        const bool allow_self_aliases; /// for constructs like "SELECT column + 1 AS column"

        ContextPtr context;
        ConstStoragePtr storage;
        const StorageMetadataPtr metadata_snapshot;
        bool rewrite_map_col;

        Data(
            const Aliases & aliases_,
            const NameSet & source_columns_set_,
            bool ignore_alias_,
            ExtractedSettings && settings_,
            bool allow_self_aliases_,
            ContextPtr context_ = {},
            ConstStoragePtr storage_ = nullptr,
            const StorageMetadataPtr & metadata_snapshot_ = {},
            bool rewrite_map_col_ = true)
            : aliases(aliases_)
            , source_columns_set(source_columns_set_)
            , settings(settings_)
            , level(0)
            , ignore_alias(ignore_alias_)
            , allow_self_aliases(allow_self_aliases_)
            , context(std::move(context_))
            , storage(storage_)
            , metadata_snapshot(metadata_snapshot_)
            , rewrite_map_col(rewrite_map_col_)
        {
        }
    };

    explicit QueryNormalizer(Data & data)
        : visitor_data(data)
    {}

    void visit(ASTPtr & ast)
    {
        visit(ast, visitor_data);
    }

private:
    Data & visitor_data;

    static void visit(ASTPtr & ast, Data & data);

    static void visit(ASTIdentifier &, ASTPtr &, Data &);
    static void visit(ASTFunction &, ASTPtr &, Data &);
    static void visit(ASTTablesInSelectQueryElement &, const ASTPtr &, Data &);
    static void visit(ASTSelectQuery &, const ASTPtr &, Data &);

    static void visitChildren(IAST * node, Data & data);

    static String getMapKeyName(ASTFunction & node, Data & data);
    static void rewriteMapElement(ASTPtr & ast, const String & map_name, const String & key_name);
};

}
