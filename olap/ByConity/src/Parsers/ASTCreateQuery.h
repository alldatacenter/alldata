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

#include <Parsers/ASTQueryWithTableAndOutput.h>
#include <Parsers/ASTQueryWithOnCluster.h>
#include <Parsers/ASTDictionary.h>
#include <Parsers/ASTDictionaryAttributeDeclaration.h>
#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTSelectWithUnionQuery.h>
#include <Interpreters/StorageID.h>

namespace DB
{

class ASTFunction;
class ASTSetQuery;

class ASTStorage : public IAST
{
public:
    ASTFunction * engine = nullptr;
    IAST * partition_by = nullptr;
    IAST * primary_key = nullptr;
    IAST * order_by = nullptr;
    IAST * cluster_by = nullptr;
    IAST * unique_key = nullptr;
    IAST * sample_by = nullptr;
    IAST * ttl_table = nullptr;
    IAST * comment = nullptr;
    ASTSetQuery * settings = nullptr;


    String getID(char) const override { return "Storage definition"; }

    ASTType getType() const override { return ASTType::ASTStorage; }

    ASTPtr clone() const override;

    void formatImpl(const FormatSettings & s, FormatState & state, FormatStateStacked frame) const override;
};


class ASTExpressionList;

class ASTColumns : public IAST
{
public:
    ASTExpressionList * columns = nullptr;
    ASTExpressionList * indices = nullptr;
    ASTExpressionList * constraints = nullptr;
    ASTExpressionList * projections = nullptr;
    IAST              * primary_key = nullptr;

    String getID(char) const override { return "Columns definition"; }

    ASTType getType() const override { return ASTType::ASTColumns; }

    ASTPtr clone() const override;

    void formatImpl(const FormatSettings & s, FormatState & state, FormatStateStacked frame) const override;
};


/// CREATE TABLE or ATTACH TABLE query
class ASTCreateQuery : public ASTQueryWithTableAndOutput, public ASTQueryWithOnCluster
{
public:
    bool attach{false};    /// Query ATTACH TABLE, not CREATE TABLE.
    bool create{false};     /// for CnchHive CREATE TABLE check schema flag.
    bool if_not_exists{false};
    bool is_ordinary_view{false};
    bool is_materialized_view{false};
    bool is_live_view{false};
    bool is_populate{false};
    bool replace_view{false}; /// CREATE OR REPLACE VIEW
    bool ignore_replicated{false};
    bool ignore_async{false};
    bool ignore_ttl{false};

    ASTColumns * columns_list = nullptr;
    ASTExpressionList * tables = nullptr;

    StorageID to_table_id = StorageID::createEmpty();   /// For CREATE MATERIALIZED VIEW mv TO table.
    UUID to_inner_uuid = UUIDHelpers::Nil;      /// For materialized view with inner table
    ASTStorage * storage = nullptr;
    String as_database;
    String as_table;
    ASTPtr as_table_function;
    ASTSelectWithUnionQuery * select = nullptr;

    bool is_dictionary{false}; /// CREATE DICTIONARY
    ASTExpressionList * dictionary_attributes_list = nullptr; /// attributes of
    ASTDictionary * dictionary = nullptr; /// dictionary definition (layout, primary key, etc.)

    std::optional<UInt64> live_view_timeout;    /// For CREATE LIVE VIEW ... WITH TIMEOUT ...
    std::optional<UInt64> live_view_periodic_refresh;    /// For CREATE LIVE VIEW ... WITH [PERIODIC] REFRESH ...

    bool attach_short_syntax{false};

    std::optional<String> attach_from_path = std::nullopt;

    bool replace_table{false};
    bool create_or_replace{false};

    /** Get the text that identifies this element. */
    String getID(char delim) const override { return (attach ? "AttachQuery" : "CreateQuery") + (delim + database) + delim + table; }

    ASTType getType() const override { return ASTType::ASTCreateQuery; }

    ASTPtr clone() const override;

    ASTPtr getRewrittenASTWithoutOnCluster(const std::string & new_database) const override
    {
        return removeOnCluster<ASTCreateQuery>(clone(), new_database);
    }

    bool isView() const { return is_ordinary_view || is_materialized_view || is_live_view; }

protected:
    void formatQueryImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const override;
};

}
