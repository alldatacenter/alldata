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

#include <Parsers/ASTAlterQuery.h>
#include <IO/Operators.h>
#include <iomanip>
#include <Common/quoteString.h>


namespace DB
{

namespace ErrorCodes
{
    extern const int UNEXPECTED_AST_STRUCTURE;
}

ASTPtr ASTAlterCommand::clone() const
{
    auto res = std::make_shared<ASTAlterCommand>(*this);
    res->children.clear();

    if (col_decl)
    {
        res->col_decl = col_decl->clone();
        res->children.push_back(res->col_decl);
    }
    if (column)
    {
        res->column = column->clone();
        res->children.push_back(res->column);
    }
    if (order_by)
    {
        res->order_by = order_by->clone();
        res->children.push_back(res->order_by);
    }
    if (cluster_by)
    {
        res->cluster_by = cluster_by->clone();
        res->children.push_back(res->cluster_by);
    }
    if (partition)
    {
        res->partition = partition->clone();
        res->children.push_back(res->partition);
    }
    if (predicate)
    {
        res->predicate = predicate->clone();
        res->children.push_back(res->predicate);
    }
    if (ttl)
    {
        res->ttl = ttl->clone();
        res->children.push_back(res->ttl);
    }
    if (settings_changes)
    {
        res->settings_changes = settings_changes->clone();
        res->children.push_back(res->settings_changes);
    }
    if (settings_resets)
    {
        res->settings_resets = settings_resets->clone();
        res->children.push_back(res->settings_resets);
    }
    if (values)
    {
        res->values = values->clone();
        res->children.push_back(res->values);
    }
    if (rename_to)
    {
        res->rename_to = rename_to->clone();
        res->children.push_back(res->rename_to);
    }
    if (columns)
    {
        res->columns = columns->clone();
        res->children.push_back(res->columns);
    }

    return res;
}

void ASTAlterCommand::formatImpl(
    const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const
{
    std::string indent_str = settings.one_line ? "" : std::string(4 * frame.indent, ' ');

    if (type == ASTAlterCommand::ADD_COLUMN)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "ADD COLUMN " << (if_not_exists ? "IF NOT EXISTS " : "") << (settings.hilite ? hilite_none : "");
        col_decl->formatImpl(settings, state, frame);

        if (first)
            settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << " FIRST " << (settings.hilite ? hilite_none : "");
        else if (column)    /// AFTER
        {
            settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << " AFTER " << (settings.hilite ? hilite_none : "");
            column->formatImpl(settings, state, frame);
        }
    }
    else if (type == ASTAlterCommand::DROP_COLUMN)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << (clear_column ? "CLEAR " : "DROP ") << "COLUMN "
                      << (if_exists ? "IF EXISTS " : "") << (settings.hilite ? hilite_none : "");
        column->formatImpl(settings, state, frame);
        if (partition || predicate)
        {
            settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str
                        << (predicate ? " IN PARTITION WHERE " : " IN PARTITION ") << (settings.hilite ? hilite_none : "");
            if (partition)
                partition->formatImpl(settings, state, frame);
            else if (predicate)
                predicate->formatImpl(settings, state, frame);
        }
    }
    else if (type == ASTAlterCommand::MODIFY_COLUMN)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "MODIFY COLUMN " << (if_exists ? "IF EXISTS " : "") << (settings.hilite ? hilite_none : "");
        col_decl->formatImpl(settings, state, frame);

        if (!remove_property.empty())
        {
            settings.ostr << (settings.hilite ? hilite_keyword : "") << " REMOVE " << remove_property;
        }
        else
        {
            if (first)
                settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << " FIRST " << (settings.hilite ? hilite_none : "");
            else if (column)    /// AFTER
            {
                settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << " AFTER " << (settings.hilite ? hilite_none : "");
                column->formatImpl(settings, state, frame);
            }
        }
    }
    else if (type == ASTAlterCommand::COMMENT_COLUMN)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "COMMENT COLUMN " << (if_exists ? "IF EXISTS " : "") << (settings.hilite ? hilite_none : "");
        column->formatImpl(settings, state, frame);
        settings.ostr << " " << (settings.hilite ? hilite_none : "");
        comment->formatImpl(settings, state, frame);
    }
    else if (type == ASTAlterCommand::MODIFY_ORDER_BY)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "MODIFY ORDER BY " << (settings.hilite ? hilite_none : "");
        order_by->formatImpl(settings, state, frame);
    }
    else if (type == ASTAlterCommand::MODIFY_CLUSTER_BY)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "MODIFY CLUSTER BY " << (settings.hilite ? hilite_none : "");
        cluster_by->formatImpl(settings, state, frame);
    }
    else if (type == ASTAlterCommand::DROP_CLUSTER)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "DROP CLUSTER";
    }
    else if (type == ASTAlterCommand::MODIFY_SAMPLE_BY)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "MODIFY SAMPLE BY " << (settings.hilite ? hilite_none : "");
        sample_by->formatImpl(settings, state, frame);
    }
    else if (type == ASTAlterCommand::ADD_INDEX)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "ADD INDEX " << (if_not_exists ? "IF NOT EXISTS " : "") << (settings.hilite ? hilite_none : "");
        index_decl->formatImpl(settings, state, frame);

        if (first)
            settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << " FIRST " << (settings.hilite ? hilite_none : "");
        else if (index)    /// AFTER
        {
            settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << " AFTER " << (settings.hilite ? hilite_none : "");
            index->formatImpl(settings, state, frame);
        }
    }
    else if (type == ASTAlterCommand::DROP_INDEX)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str
                      << (clear_index ? "CLEAR " : "DROP ") << "INDEX " << (if_exists ? "IF EXISTS " : "") << (settings.hilite ? hilite_none : "");
        index->formatImpl(settings, state, frame);
        if (partition)
        {
            settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << " IN PARTITION " << (settings.hilite ? hilite_none : "");
            partition->formatImpl(settings, state, frame);
        }
    }
    else if (type == ASTAlterCommand::MATERIALIZE_INDEX)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str
                      << "MATERIALIZE INDEX " << (settings.hilite ? hilite_none : "");
        index->formatImpl(settings, state, frame);
        if (partition)
        {
            settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << " IN PARTITION " << (settings.hilite ? hilite_none : "");
            partition->formatImpl(settings, state, frame);
        }
    }
    else if (type == ASTAlterCommand::ADD_CONSTRAINT)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "ADD CONSTRAINT " << (if_not_exists ? "IF NOT EXISTS " : "") << (settings.hilite ? hilite_none : "");
        constraint_decl->formatImpl(settings, state, frame);
    }
    else if (type == ASTAlterCommand::DROP_CONSTRAINT)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str
                      << "DROP CONSTRAINT " << (if_exists ? "IF EXISTS " : "") << (settings.hilite ? hilite_none : "");
        constraint->formatImpl(settings, state, frame);
    }
    else if (type == ASTAlterCommand::ADD_PROJECTION)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "ADD PROJECTION " << (if_not_exists ? "IF NOT EXISTS " : "") << (settings.hilite ? hilite_none : "");
        projection_decl->formatImpl(settings, state, frame);

        if (first)
            settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << " FIRST " << (settings.hilite ? hilite_none : "");
        else if (projection)
        {
            settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << " AFTER " << (settings.hilite ? hilite_none : "");
            projection->formatImpl(settings, state, frame);
        }
    }
    else if (type == ASTAlterCommand::DROP_PROJECTION)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str
                      << (clear_projection ? "CLEAR " : "DROP ") << "PROJECTION " << (if_exists ? "IF EXISTS " : "") << (settings.hilite ? hilite_none : "");
        projection->formatImpl(settings, state, frame);
        if (partition)
        {
            settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str<< " IN PARTITION " << (settings.hilite ? hilite_none : "");
            partition->formatImpl(settings, state, frame);
        }
    }
    else if (type == ASTAlterCommand::MATERIALIZE_PROJECTION)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str
                      << "MATERIALIZE PROJECTION " << (settings.hilite ? hilite_none : "");
        projection->formatImpl(settings, state, frame);
        if (partition)
        {
            settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str<< " IN PARTITION " << (settings.hilite ? hilite_none : "");
            partition->formatImpl(settings, state, frame);
        }
    }
    else if (type == ASTAlterCommand::DROP_PARTITION)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << (cascading ? "CASCADING " : "")
                      << (detach ? "DETACH" : "DROP") << (part ? " PART " : " PARTITION ")
                      << (settings.hilite ? hilite_none : "");
        partition->formatImpl(settings, state, frame);
    }
    else if (type == ASTAlterCommand::DROP_PARTITION_WHERE)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << (cascading ? "CASCADING " : "")
                      << (detach ? "DETACH" : "DROP") << " PARTITION WHERE "
                      << (settings.hilite ? hilite_none : "");
        predicate->formatImpl(settings, state, frame);
    }
    else if (type == ASTAlterCommand::DROP_DETACHED_PARTITION)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "DROP DETACHED" << (part ? " PART " : " PARTITION ")
                      << (settings.hilite ? hilite_none : "");
        partition->formatImpl(settings, state, frame);
    }
    else if (type == ASTAlterCommand::ATTACH_PARTITION)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "ATTACH "
                      << (parts ? "PARTS" : part ? "PART " : "PARTITION ") << (settings.hilite ? hilite_none : "");
        if (!parts)
            partition->formatImpl(settings, state, frame);
    }
    else if (type == ASTAlterCommand::ATTACH_DETACHED_PARTITION)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "ATTACH DETACHED PARTITION "
                      << (settings.hilite ? hilite_none : "");
        partition->formatImpl(settings, state, frame);
        settings.ostr << (settings.hilite ? hilite_keyword : "") << " FROM " << (settings.hilite ? hilite_none : "");
        if (!from_database.empty())
        {
            settings.ostr << (settings.hilite ? hilite_identifier : "") << backQuoteIfNeed(from_database)
                          << (settings.hilite ? hilite_none : "") << ".";
        }
        settings.ostr << (settings.hilite ? hilite_identifier : "") << backQuoteIfNeed(from_table) << (settings.hilite ? hilite_none : "");
    }
    else if (type == ASTAlterCommand::MOVE_PARTITION)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "MOVE "
                      << (part ? "PART " : "PARTITION ") << (settings.hilite ? hilite_none : "");
        partition->formatImpl(settings, state, frame);
        settings.ostr << " TO ";
        switch (move_destination_type)
        {
            case DataDestinationType::DISK:
                settings.ostr << "DISK ";
                break;
            case DataDestinationType::VOLUME:
                settings.ostr << "VOLUME ";
                break;
            case DataDestinationType::TABLE:
                settings.ostr << "TABLE ";
                if (!to_database.empty())
                {
                    settings.ostr << (settings.hilite ? hilite_identifier : "") << backQuoteIfNeed(to_database)
                                  << (settings.hilite ? hilite_none : "") << ".";
                }
                settings.ostr << (settings.hilite ? hilite_identifier : "")
                              << backQuoteIfNeed(to_table)
                              << (settings.hilite ? hilite_none : "");
                return;
            default:
                break;
        }
        if (move_destination_type != DataDestinationType::TABLE)
        {
            settings.ostr << quoteString(move_destination_name);
        }
    }
    else if (type == ASTAlterCommand::MOVE_PARTITION_FROM)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "MOVE PARTITION " << (settings.hilite ? hilite_none : "");
        partition->formatImpl(settings, state, frame);
        settings.ostr << (settings.hilite ? hilite_keyword : "") << " FROM " << (settings.hilite ? hilite_none : "") ;
        if (!from_database.empty())
        {
            settings.ostr << (settings.hilite ? hilite_identifier : "") << backQuoteIfNeed(from_database)
                          << (settings.hilite ? hilite_none : "") << ".";
        }
        settings.ostr << (settings.hilite ? hilite_identifier : "") << backQuoteIfNeed(from_table) << (settings.hilite ? hilite_none : "");
    }
    else if (type == ASTAlterCommand::REPLACE_PARTITION)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << (replace ? "REPLACE" : "ATTACH") << " PARTITION "
                      << (settings.hilite ? hilite_none : "");
        partition->formatImpl(settings, state, frame);
        settings.ostr << (settings.hilite ? hilite_keyword : "") << " FROM " << (settings.hilite ? hilite_none : "");
        if (!from_database.empty())
        {
            settings.ostr << (settings.hilite ? hilite_identifier : "") << backQuoteIfNeed(from_database)
                          << (settings.hilite ? hilite_none : "") << ".";
        }
        settings.ostr << (settings.hilite ? hilite_identifier : "") << backQuoteIfNeed(from_table) << (settings.hilite ? hilite_none : "");
    }
    else if (type == ASTAlterCommand::REPLACE_PARTITION_WHERE)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << (replace ? "REPLACE" : "ATTACH") << " PARTITION WHERE "
                      << (settings.hilite ? hilite_none : "");
        predicate->formatImpl(settings, state, frame);
        settings.ostr << (settings.hilite ? hilite_keyword : "") << " FROM " << (settings.hilite ? hilite_none : "");
        if (!from_database.empty())
        {
            settings.ostr << (settings.hilite ? hilite_identifier : "") << backQuoteIfNeed(from_database)
                          << (settings.hilite ? hilite_none : "") << ".";
        }
        settings.ostr << (settings.hilite ? hilite_identifier : "") << backQuoteIfNeed(from_table) << (settings.hilite ? hilite_none : "");
    }
    else if (type == ASTAlterCommand::INGEST_PARTITION)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "INGEST PARTITION " << (settings.hilite ? hilite_none : "");
        partition->formatImpl(settings, state, frame);
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << " COLUMNS " << (settings.hilite ? hilite_none : "");
        columns->formatImpl(settings, state, frame);
        if (keys)
        {
            settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << " KEY " << (settings.hilite ? hilite_none : "");
            keys->formatImpl(settings, state, frame);
        }
        settings.ostr << (settings.hilite ? hilite_keyword : "") << " FROM " << (settings.hilite ? hilite_none : "") ;
        if (!from_database.empty())
        {
            settings.ostr << (settings.hilite ? hilite_identifier : "") << backQuoteIfNeed(from_database)
                          << (settings.hilite ? hilite_none : "") << ".";
        }
        settings.ostr << (settings.hilite ? hilite_identifier : "") << backQuoteIfNeed(from_table) << (settings.hilite ? hilite_none : "");
    }
    else if (type == ASTAlterCommand::FETCH_PARTITION)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "FETCH "
                      << (part ? "PART " : "PARTITION ") << (settings.hilite ? hilite_none : "");
        partition->formatImpl(settings, state, frame);
        settings.ostr << (settings.hilite ? hilite_keyword : "")
                      << " FROM " << (settings.hilite ? hilite_none : "") << DB::quote << from;
    }
    else if (type == ASTAlterCommand::FETCH_PARTITION_WHERE)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "FETCH "
                      << "PARTITION WHERE " << (settings.hilite ? hilite_none : "");
        predicate->formatImpl(settings, state, frame);
        settings.ostr << (settings.hilite ? hilite_keyword : "")
                      << " FROM " << (settings.hilite ? hilite_none : "") << DB::quote << from;
    }
    else if (type == ASTAlterCommand::REPAIR_PARTITION)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "REPAIR "
                      << "PARTITION " << (settings.hilite ? hilite_none : "");
        partition->formatImpl(settings, state, frame);
        settings.ostr << (settings.hilite ? hilite_keyword : "")
                      << " FROM " << (settings.hilite ? hilite_none : "") << DB::quote << from;
    }
    else if (type == ASTAlterCommand::FREEZE_PARTITION)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "FREEZE PARTITION " << (settings.hilite ? hilite_none : "");
        partition->formatImpl(settings, state, frame);

        if (!with_name.empty())
        {
            settings.ostr << " " << (settings.hilite ? hilite_keyword : "") << "WITH NAME" << (settings.hilite ? hilite_none : "")
                          << " " << DB::quote << with_name;
        }
    }
    else if (type == ASTAlterCommand::FREEZE_ALL)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "FREEZE";

        if (!with_name.empty())
        {
            settings.ostr << " " << (settings.hilite ? hilite_keyword : "") << "WITH NAME" << (settings.hilite ? hilite_none : "")
                          << " " << DB::quote << with_name;
        }
    }
    else if (type == ASTAlterCommand::UNFREEZE_PARTITION)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "UNFREEZE PARTITION " << (settings.hilite ? hilite_none : "");
        partition->formatImpl(settings, state, frame);

        if (!with_name.empty())
        {
            settings.ostr << " " << (settings.hilite ? hilite_keyword : "") << "WITH NAME" << (settings.hilite ? hilite_none : "")
                          << " " << DB::quote << with_name;
        }
    }
    else if (type == ASTAlterCommand::UNFREEZE_ALL)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "UNFREEZE";

        if (!with_name.empty())
        {
            settings.ostr << " " << (settings.hilite ? hilite_keyword : "") << "WITH NAME" << (settings.hilite ? hilite_none : "")
                          << " " << DB::quote << with_name;
        }
    }
    else if (type == ASTAlterCommand::DELETE)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "DELETE" << (settings.hilite ? hilite_none : "");

        if (partition)
        {
            settings.ostr << (settings.hilite ? hilite_keyword : "") << " IN PARTITION " << (settings.hilite ? hilite_none : "");
            partition->formatImpl(settings, state, frame);
        }

        settings.ostr << (settings.hilite ? hilite_keyword : "") << " WHERE " << (settings.hilite ? hilite_none : "");
        predicate->formatImpl(settings, state, frame);
    }
    else if (type == ASTAlterCommand::FAST_DELETE)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "FASTDELETE " << (settings.hilite ? hilite_none : "");
        if (columns)
        {
            columns->formatImpl(settings, state, frame);
            settings.ostr << settings.nl_or_ws;
        }
        if (partition)
        {
            settings.ostr << (settings.hilite ? hilite_keyword : "") << " IN PARTITION " << (settings.hilite ? hilite_none : "");
            partition->formatImpl(settings, state, frame);
        }
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "WHERE " << (settings.hilite ? hilite_none : "");
        predicate->formatImpl(settings, state, frame);
    }
    else if (type == ASTAlterCommand::UPDATE)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "UPDATE " << (settings.hilite ? hilite_none : "");
        update_assignments->formatImpl(settings, state, frame);

        if (partition)
        {
            settings.ostr << (settings.hilite ? hilite_keyword : "") << " IN PARTITION " << (settings.hilite ? hilite_none : "");
            partition->formatImpl(settings, state, frame);
        }

        settings.ostr << (settings.hilite ? hilite_keyword : "") << " WHERE " << (settings.hilite ? hilite_none : "");
        predicate->formatImpl(settings, state, frame);
    }
    else if (type == ASTAlterCommand::MODIFY_TTL)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "MODIFY TTL " << (settings.hilite ? hilite_none : "");
        ttl->formatImpl(settings, state, frame);
    }
    else if (type == ASTAlterCommand::REMOVE_TTL)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "REMOVE TTL" << (settings.hilite ? hilite_none : "");
    }
    else if (type == ASTAlterCommand::MATERIALIZE_TTL)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "MATERIALIZE TTL"
                      << (settings.hilite ? hilite_none : "");
        if (partition)
        {
            settings.ostr << (settings.hilite ? hilite_keyword : "") << " IN PARTITION " << (settings.hilite ? hilite_none : "");
            partition->formatImpl(settings, state, frame);
        }
    }
    else if (type == ASTAlterCommand::MODIFY_SETTING)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "MODIFY SETTING " << (settings.hilite ? hilite_none : "");
        settings_changes->formatImpl(settings, state, frame);
    }
    else if (type == ASTAlterCommand::RESET_SETTING)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "RESET SETTING " << (settings.hilite ? hilite_none : "");
        settings_resets->formatImpl(settings, state, frame);
    }
    else if (type == ASTAlterCommand::MODIFY_QUERY)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "MODIFY QUERY " << settings.nl_or_ws << (settings.hilite ? hilite_none : "");
        select->formatImpl(settings, state, frame);
    }
    else if (type == ASTAlterCommand::LIVE_VIEW_REFRESH)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "REFRESH " << (settings.hilite ? hilite_none : "");
    }
    else if (type == ASTAlterCommand::RENAME_COLUMN)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "RENAME COLUMN " << (if_exists ? "IF EXISTS " : "")
                      << (settings.hilite ? hilite_none : "");
        column->formatImpl(settings, state, frame);

        settings.ostr << (settings.hilite ? hilite_keyword : "") << " TO ";
        rename_to->formatImpl(settings, state, frame);
    }
    else if (type == ASTAlterCommand::CLEAR_MAP_KEY)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "CLEAR MAP KEY " << (settings.hilite ? hilite_none : "");
        column->formatImpl(settings, state, frame);
        settings.ostr << "(";
        map_keys->formatImpl(settings, state, frame);
        settings.ostr << ")";
    }
    else if (type == ASTAlterCommand::SAMPLE_PARTITION_WHERE)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "SAMPLE PARTITION WITH " << (settings.hilite ? hilite_none : "");
        with_sharding_exp->formatImpl(settings, state, frame);
        settings.ostr << (settings.hilite ? hilite_keyword : "") << " WHERE " << (settings.hilite ? hilite_none : "");
        predicate->formatImpl(settings, state, frame);
        settings.ostr << (settings.hilite ? hilite_keyword : "") << " TO " << (settings.hilite ? hilite_none : "");
        if (!from_database.empty())
        {
            settings.ostr << (settings.hilite ? hilite_identifier : "") << backQuoteIfNeed(from_database)
                          << (settings.hilite ? hilite_none : "") << ".";
        }
        settings.ostr << (settings.hilite ? hilite_identifier : "") << backQuoteIfNeed(from_table) << (settings.hilite ? hilite_none : "");
    }
    else
        throw Exception("Unexpected type of ALTER", ErrorCodes::UNEXPECTED_AST_STRUCTURE);
}

bool ASTAlterQuery::isOneCommandTypeOnly(const ASTAlterCommand::Type & type) const
{
    if (command_list)
    {
        if (command_list->children.empty())
            return false;
        for (const auto & child : command_list->children)
        {
            const auto & command = child->as<const ASTAlterCommand &>();
            if (command.type != type)
                return false;
        }
        return true;
    }
    return false;
}

bool ASTAlterQuery::isSettingsAlter() const
{
    return isOneCommandTypeOnly(ASTAlterCommand::MODIFY_SETTING);
}

bool ASTAlterQuery::isFreezeAlter() const
{
    return isOneCommandTypeOnly(ASTAlterCommand::FREEZE_PARTITION) || isOneCommandTypeOnly(ASTAlterCommand::FREEZE_ALL)
        || isOneCommandTypeOnly(ASTAlterCommand::UNFREEZE_PARTITION) || isOneCommandTypeOnly(ASTAlterCommand::UNFREEZE_ALL);
}

/** Get the text that identifies this element. */
String ASTAlterQuery::getID(char delim) const
{
    return "AlterQuery" + (delim + database) + delim + table;
}

ASTPtr ASTAlterQuery::clone() const
{
    auto res = std::make_shared<ASTAlterQuery>(*this);
    res->children.clear();

    if (command_list)
        res->set(res->command_list, command_list->clone());

    return res;
}

void ASTAlterQuery::formatQueryImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const
{
    frame.need_parens = false;

    std::string indent_str = settings.one_line ? "" : std::string(4u * frame.indent, ' ');

    if (is_live_view)
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "ALTER LIVE VIEW " << (settings.hilite ? hilite_none : "");
    else
        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "ALTER TABLE " << (settings.hilite ? hilite_none : "");

    if (!table.empty())
    {
        if (!database.empty())
        {
            settings.ostr << indent_str << backQuoteIfNeed(database);
            settings.ostr << ".";
        }
        settings.ostr << indent_str << backQuoteIfNeed(table);
    }
    formatOnCluster(settings);
    settings.ostr << settings.nl_or_ws;

    FormatStateStacked frame_nested = frame;
    frame_nested.need_parens = false;
    ++frame_nested.indent;
    static_cast<IAST *>(command_list)->formatImpl(settings, state, frame_nested);
}

}
