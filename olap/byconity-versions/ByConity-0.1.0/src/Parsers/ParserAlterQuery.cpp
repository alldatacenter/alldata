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

#include <Common/typeid_cast.h>
#include <Parsers/ParserAlterQuery.h>
#include <Parsers/CommonParsers.h>
#include <Parsers/ExpressionElementParsers.h>
#include <Parsers/ExpressionListParsers.h>
#include <Parsers/ParserCreateQuery.h>
#include <Parsers/ParserPartition.h>
#include <Parsers/ParserSelectWithUnionQuery.h>
#include <Parsers/ParserSetQuery.h>
#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTIndexDeclaration.h>
#include <Parsers/ASTAlterQuery.h>
#include <Parsers/ASTLiteral.h>
#include <Parsers/parseDatabaseAndTableName.h>


namespace DB
{

bool ParserAlterCommand::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    auto command = std::make_shared<ASTAlterCommand>();
    node = command;

    ParserKeyword s_add_column("ADD COLUMN");
    ParserKeyword s_drop_column("DROP COLUMN");
    ParserKeyword s_clear_column("CLEAR COLUMN");
    ParserKeyword s_modify_column("MODIFY COLUMN");
    ParserKeyword s_clear_map_key("CLEAR MAP KEY");
    ParserKeyword s_rename_column("RENAME COLUMN");
    ParserKeyword s_comment_column("COMMENT COLUMN");
    ParserKeyword s_modify_order_by("MODIFY ORDER BY");
    ParserKeyword s_modify_cluster_by("MODIFY CLUSTER BY");
    ParserKeyword s_drop_cluster("DROP CLUSTER");
    ParserKeyword s_modify_sample_by("MODIFY SAMPLE BY");
    ParserKeyword s_modify_ttl("MODIFY TTL");
    ParserKeyword s_materialize_ttl("MATERIALIZE TTL");
    ParserKeyword s_modify_setting("MODIFY SETTING");
    ParserKeyword s_reset_setting("RESET SETTING");
    ParserKeyword s_modify_query("MODIFY QUERY");

    ParserKeyword s_add_index("ADD INDEX");
    ParserKeyword s_drop_index("DROP INDEX");
    ParserKeyword s_clear_index("CLEAR INDEX");
    ParserKeyword s_materialize_index("MATERIALIZE INDEX");

    ParserKeyword s_add_constraint("ADD CONSTRAINT");
    ParserKeyword s_drop_constraint("DROP CONSTRAINT");

    ParserKeyword s_add_projection("ADD PROJECTION");
    ParserKeyword s_drop_projection("DROP PROJECTION");
    ParserKeyword s_clear_projection("CLEAR PROJECTION");
    ParserKeyword s_materialize_projection("MATERIALIZE PROJECTION");

    ParserKeyword s_add("ADD");
    ParserKeyword s_drop("DROP");
    ParserKeyword s_suspend("SUSPEND");
    ParserKeyword s_resume("RESUME");
    ParserKeyword s_refresh("REFRESH");
    ParserKeyword s_modify("MODIFY");
    ParserKeyword s_detach("DETACH");

    ParserKeyword s_attach_partition("ATTACH PARTITION");
    ParserKeyword s_attach_detached_partition("ATTACH DETACHED PARTITION");
    ParserKeyword s_attach_parts("ATTACH PARTS");
    ParserKeyword s_attach_part("ATTACH PART");
    ParserKeyword s_detach_partition("DETACH PARTITION");
    ParserKeyword s_detach_part("DETACH PART");
    ParserKeyword s_detach_partition_where("DETACH PARTITION WHERE");
    ParserKeyword s_drop_partition("DROP PARTITION");
    ParserKeyword s_drop_part("DROP PART");
    ParserKeyword s_drop_partition_where("DROP PARTITION WHERE");
    ParserKeyword s_move_partition("MOVE PARTITION");
    ParserKeyword s_move_part("MOVE PART");
    ParserKeyword s_drop_detached_partition("DROP DETACHED PARTITION");
    ParserKeyword s_drop_detached_part("DROP DETACHED PART");
    ParserKeyword s_fetch_partition("FETCH PARTITION");
    ParserKeyword s_fetch_part("FETCH PART");
    ParserKeyword s_fetch_partition_where("FETCH PARTITION WHERE");
    ParserKeyword s_repair_partition("REPAIR PARTITION");
    ParserKeyword s_repair_part("REPAIR PART");
    ParserKeyword s_replace_partition("REPLACE PARTITION");
    ParserKeyword s_replace_partition_where("REPLACE PARTITION WHERE");
    ParserKeyword s_ingest_partition("INGEST PARTITION");
    ParserKeyword s_freeze("FREEZE");
    ParserKeyword s_unfreeze("UNFREEZE");
    ParserKeyword s_partition("PARTITION");

    ParserKeyword s_drop_bitmap_of_partition_where("DROP BITMAP OF PARTITION WHERE");
    ParserKeyword s_drop_bitmap_of_partition("DROP BITMAP OF PARTITION");
    ParserKeyword s_build_bitmap_of_partition_where("BUILD BITMAP OF PARTITION WHERE");
    ParserKeyword s_build_bitmap_of_partition("BUILD BITMAP OF PARTITION");
    ParserKeyword s_drop_mark_bitmap_of_partition_where("DROP MARK BITMAP OF PARTITION WHERE");
    ParserKeyword s_drop_mark_bitmap_of_partition("DROP MARK BITMAP OF PARTITION");
    ParserKeyword s_build_mark_bitmap_of_partition_where("BUILD MARK BITMAP OF PARTITION WHERE");
    ParserKeyword s_build_mark_bitmap_of_partition("BUILD MARK BITMAP OF PARTITION");
    ParserKeyword s_sample_partition_with("SAMPLE PARTITION WITH");

    ParserKeyword s_first("FIRST");
    ParserKeyword s_after("AFTER");
    ParserKeyword s_if_not_exists("IF NOT EXISTS");
    ParserKeyword s_if_exists("IF EXISTS");
    ParserKeyword s_from("FROM");
    ParserKeyword s_in_partition("IN PARTITION");
    ParserKeyword s_with("WITH");
    ParserKeyword s_name("NAME");
    ParserKeyword s_cascading("CASCADING");

    ParserKeyword s_to_disk("TO DISK");
    ParserKeyword s_to_volume("TO VOLUME");
    ParserKeyword s_to_table("TO TABLE");
    ParserKeyword s_to_shard("TO SHARD");

    ParserKeyword s_delete("DELETE");
    ParserKeyword s_fast_delete("FASTDELETE");
    ParserKeyword s_update("UPDATE");
    ParserKeyword s_where("WHERE");
    ParserKeyword s_to("TO");

    ParserKeyword s_remove("REMOVE");
    ParserKeyword s_default("DEFAULT");
    ParserKeyword s_materialized("MATERIALIZED");
    ParserKeyword s_alias("ALIAS");
    ParserKeyword s_comment("COMMENT");
    ParserKeyword s_codec("CODEC");
    ParserKeyword s_ttl("TTL");

    ParserKeyword s_remove_ttl("REMOVE TTL");

    ParserCompoundIdentifier parser_name;
    ParserStringLiteral parser_string_literal;
    ParserIdentifier parser_remove_property;
    ParserCompoundColumnDeclaration parser_col_decl(dt);
    ParserIndexDeclaration parser_idx_decl(dt);
    ParserConstraintDeclaration parser_constraint_decl(dt);
    ParserProjectionDeclaration parser_projection_decl(dt);
    ParserCompoundColumnDeclaration parser_modify_col_decl(dt, false, false, true);
    ParserPartition parser_partition(dt);
    ParserExpression parser_exp_elem(dt);
    ParserList parser_assignment_list(
        std::make_unique<ParserAssignment>(dt), std::make_unique<ParserToken>(TokenType::Comma),
        /* allow_empty = */ false);
    ParserSetQuery parser_settings(true);
    ParserList parser_reset_setting(
        std::make_unique<ParserIdentifier>(), std::make_unique<ParserToken>(TokenType::Comma),
        /* allow_empty = */ false);
    ParserNameList values_p(dt);
    ParserSelectWithUnionQuery select_p(dt);
    ParserTTLExpressionList parser_ttl_list(dt);
    ParserList parser_map_key_list(std::make_unique<ParserStringLiteral>(), std::make_unique<ParserToken>(TokenType::Comma), false);
    ParserClusterByElement cluster_p;

    // Optional CASCADING keyword for drop/detach partition
    if (s_cascading.ignore(pos, expected))
    {
        command->cascading = true;
    }

    if (is_live_view)
    {
        if (s_refresh.ignore(pos, expected))
        {
            command->type = ASTAlterCommand::LIVE_VIEW_REFRESH;
        }
        else
            return false;
    }
    else
    {
        if (s_add_column.ignore(pos, expected))
        {
            if (s_if_not_exists.ignore(pos, expected))
                command->if_not_exists = true;

            if (!parser_col_decl.parse(pos, command->col_decl, expected))
                return false;

            if (s_first.ignore(pos, expected))
                command->first = true;
            else if (s_after.ignore(pos, expected))
            {
                if (!parser_name.parse(pos, command->column, expected))
                    return false;
            }

            command->type = ASTAlterCommand::ADD_COLUMN;
        }
        else if (s_rename_column.ignore(pos, expected))
        {
            if (s_if_exists.ignore(pos, expected))
                command->if_exists = true;

            if (!parser_name.parse(pos, command->column, expected))
                return false;

            if (!s_to.ignore(pos, expected))
                return false;

            if (!parser_name.parse(pos, command->rename_to, expected))
                return false;

            command->type = ASTAlterCommand::RENAME_COLUMN;
        }
        else if (s_drop_partition_where.ignore(pos, expected))
        {
            if (!parser_exp_elem.parse(pos, command->predicate, expected))
                return false;

            command->type = ASTAlterCommand::DROP_PARTITION_WHERE;
        }
        else if (s_drop_partition.ignore(pos, expected))
        {
            if (!parser_partition.parse(pos, command->partition, expected))
                return false;

            command->type = ASTAlterCommand::DROP_PARTITION;
        }
        else if (s_drop_part.ignore(pos, expected))
        {
            if (!parser_string_literal.parse(pos, command->partition, expected))
                return false;

            command->type = ASTAlterCommand::DROP_PARTITION;
            command->part = true;
        }
        else if (s_drop_detached_partition.ignore(pos, expected))
        {
            if (!parser_partition.parse(pos, command->partition, expected))
                return false;

            command->type = ASTAlterCommand::DROP_DETACHED_PARTITION;
        }
        else if (s_drop_detached_part.ignore(pos, expected))
        {
            if (!parser_string_literal.parse(pos, command->partition, expected))
                return false;

            command->type = ASTAlterCommand::DROP_DETACHED_PARTITION;
            command->part = true;
        }
        else if (s_drop_column.ignore(pos, expected))
        {
            if (s_if_exists.ignore(pos, expected))
                command->if_exists = true;

            if (!parser_name.parse(pos, command->column, expected))
                return false;

            command->type = ASTAlterCommand::DROP_COLUMN;
            command->detach = false;
        }
        else if (s_clear_map_key.ignore(pos, expected))
        {
            if (!parser_name.parse(pos, command->column, expected))
                return false;

            if (pos->type != TokenType::OpeningRoundBracket)
                return false;
            ++pos;

            if (!parser_map_key_list.parse(pos, command->map_keys, expected))
                return false;

            if (pos->type != TokenType::ClosingRoundBracket)
                return false;
            ++pos;

            command->type = ASTAlterCommand::CLEAR_MAP_KEY;

            command->detach = false;
        }
        else if (s_clear_column.ignore(pos, expected))
        {
            if (s_if_exists.ignore(pos, expected))
                command->if_exists = true;

            if (!parser_name.parse(pos, command->column, expected))
                return false;

            command->type = ASTAlterCommand::DROP_COLUMN;
            command->clear_column = true;
            command->detach = false;

            if (s_in_partition.ignore(pos, expected))
            {
                if (s_where.ignore(pos, expected))
                {
                    if (!parser_exp_elem.parse(pos, command->predicate, expected))
                        return false;
                }
                else if (!parser_partition.parse(pos, command->partition, expected))
                    return false;
            }
        }
        else if (s_add_index.ignore(pos, expected))
        {
            if (s_if_not_exists.ignore(pos, expected))
                command->if_not_exists = true;

            if (!parser_idx_decl.parse(pos, command->index_decl, expected))
                return false;

            if (s_first.ignore(pos, expected))
                command->first = true;
            else if (s_after.ignore(pos, expected))
            {
                if (!parser_name.parse(pos, command->index, expected))
                    return false;
            }

            command->type = ASTAlterCommand::ADD_INDEX;
        }
        else if (s_drop_index.ignore(pos, expected))
        {
            if (s_if_exists.ignore(pos, expected))
                command->if_exists = true;

            if (!parser_name.parse(pos, command->index, expected))
                return false;

            command->type = ASTAlterCommand::DROP_INDEX;
            command->detach = false;
        }
        else if (s_clear_index.ignore(pos, expected))
        {
            if (s_if_exists.ignore(pos, expected))
                command->if_exists = true;

            if (!parser_name.parse(pos, command->index, expected))
                return false;

            command->type = ASTAlterCommand::DROP_INDEX;
            command->clear_index = true;
            command->detach = false;

            if (s_in_partition.ignore(pos, expected))
            {
                if (!parser_partition.parse(pos, command->partition, expected))
                    return false;
            }
        }
        else if (s_materialize_index.ignore(pos, expected))
        {
            if (s_if_exists.ignore(pos, expected))
                command->if_exists = true;

            if (!parser_name.parse(pos, command->index, expected))
                return false;

            command->type = ASTAlterCommand::MATERIALIZE_INDEX;
            command->detach = false;

            if (s_in_partition.ignore(pos, expected))
            {
                if (!parser_partition.parse(pos, command->partition, expected))
                    return false;
            }
        }
        else if (s_add_projection.ignore(pos, expected))
        {
            if (s_if_not_exists.ignore(pos, expected))
                command->if_not_exists = true;

            if (!parser_projection_decl.parse(pos, command->projection_decl, expected))
                return false;

            if (s_first.ignore(pos, expected))
                command->first = true;
            else if (s_after.ignore(pos, expected))
            {
                if (!parser_name.parse(pos, command->projection, expected))
                    return false;
            }

            command->type = ASTAlterCommand::ADD_PROJECTION;
        }
        else if (s_drop_projection.ignore(pos, expected))
        {
            if (s_if_exists.ignore(pos, expected))
                command->if_exists = true;

            if (!parser_name.parse(pos, command->projection, expected))
                return false;

            command->type = ASTAlterCommand::DROP_PROJECTION;
            command->detach = false;
        }
        else if (s_clear_projection.ignore(pos, expected))
        {
            if (s_if_exists.ignore(pos, expected))
                command->if_exists = true;

            if (!parser_name.parse(pos, command->projection, expected))
                return false;

            command->type = ASTAlterCommand::DROP_PROJECTION;
            command->clear_projection = true;
            command->detach = false;

            if (s_in_partition.ignore(pos, expected))
            {
                if (!parser_partition.parse(pos, command->partition, expected))
                    return false;
            }
        }
        else if (s_materialize_projection.ignore(pos, expected))
        {
            if (s_if_exists.ignore(pos, expected))
                command->if_exists = true;

            if (!parser_name.parse(pos, command->projection, expected))
                return false;

            command->type = ASTAlterCommand::MATERIALIZE_PROJECTION;
            command->detach = false;

            if (s_in_partition.ignore(pos, expected))
            {
                if (!parser_partition.parse(pos, command->partition, expected))
                    return false;
            }
        }
        else if (s_move_part.ignore(pos, expected))
        {
            if (!parser_string_literal.parse(pos, command->partition, expected))
                return false;

            command->type = ASTAlterCommand::MOVE_PARTITION;
            command->part = true;

            if (s_to_disk.ignore(pos))
                command->move_destination_type = DataDestinationType::DISK;
            else if (s_to_volume.ignore(pos))
                command->move_destination_type = DataDestinationType::VOLUME;
            else if (s_to_table.ignore(pos))
            {
                if (!parseDatabaseAndTableName(pos, expected, command->to_database, command->to_table))
                    return false;
                command->move_destination_type = DataDestinationType::TABLE;
            }
            else if (s_to_shard.ignore(pos))
            {
                command->move_destination_type = DataDestinationType::SHARD;
            }
            else
                return false;

            if (command->move_destination_type != DataDestinationType::TABLE)
            {
                ASTPtr ast_space_name;
                if (!parser_string_literal.parse(pos, ast_space_name, expected))
                    return false;

                command->move_destination_name = ast_space_name->as<ASTLiteral &>().value.get<const String &>();
            }
        }
        else if (s_move_partition.ignore(pos, expected))
        {
            if (!parser_partition.parse(pos, command->partition, expected))
                return false;

            if (s_from.ignore(pos, expected))
            {
                if (!parseDatabaseAndTableName(pos, expected, command->from_database, command->from_table))
                    return false;
                command->type = ASTAlterCommand::MOVE_PARTITION_FROM;
            }
            else
            {
                command->type = ASTAlterCommand::MOVE_PARTITION;

                if (s_to_disk.ignore(pos))
                    command->move_destination_type = DataDestinationType::DISK;
                else if (s_to_volume.ignore(pos))
                    command->move_destination_type = DataDestinationType::VOLUME;
                else if (s_to_table.ignore(pos))
                {
                    if (!parseDatabaseAndTableName(pos, expected, command->to_database, command->to_table))
                        return false;
                    command->move_destination_type = DataDestinationType::TABLE;
                }
                else
                    return false;

                if (command->move_destination_type != DataDestinationType::TABLE)
                {
                    ASTPtr ast_space_name;
                    if (!parser_string_literal.parse(pos, ast_space_name, expected))
                        return false;

                    command->move_destination_name = ast_space_name->as<ASTLiteral &>().value.get<const String &>();
                }
            }
        }
        else if (s_add_constraint.ignore(pos, expected))
        {
            if (s_if_not_exists.ignore(pos, expected))
                command->if_not_exists = true;

            if (!parser_constraint_decl.parse(pos, command->constraint_decl, expected))
                return false;

            command->type = ASTAlterCommand::ADD_CONSTRAINT;
        }
        else if (s_drop_constraint.ignore(pos, expected))
        {
            if (s_if_exists.ignore(pos, expected))
                command->if_exists = true;

            if (!parser_name.parse(pos, command->constraint, expected))
                return false;

            command->type = ASTAlterCommand::DROP_CONSTRAINT;
            command->detach = false;
        }
        else if (s_detach_partition_where.ignore(pos, expected))
	{
            if (!parser_exp_elem.parse(pos, command->predicate, expected))
		return false;

            command->type = ASTAlterCommand::DROP_PARTITION_WHERE;
            command->detach = true;
        }
        else if (s_detach_partition.ignore(pos, expected))
        {
            if (!parser_partition.parse(pos, command->partition, expected))
                return false;

            command->type = ASTAlterCommand::DROP_PARTITION;
            command->detach = true;
        }
        else if (s_detach_part.ignore(pos, expected))
        {
            if (!parser_string_literal.parse(pos, command->partition, expected))
                return false;

            command->type = ASTAlterCommand::DROP_PARTITION;
            command->part = true;
            command->detach = true;
        }
        else if (s_attach_detached_partition.ignore(pos, expected))
        {
            if (!parser_partition.parse(pos, command->partition, expected))
                return false;

            if (s_from.ignore(pos))
            {
                ASTPtr ast_from;
                if (parseDatabaseAndTableName(pos, expected, command->from_database, command->from_table))
                {
                    command->detach = true;
                    command->replace = false;
                    command->type = ASTAlterCommand::ATTACH_DETACHED_PARTITION;
                    command->attach_from_detached = true;
                }
                else
                    return false;
            }
            else
                return false;
        }
        else if (s_attach_parts.ignore(pos, expected))
        {
            if (!s_from.ignore(pos, expected))
                return false;

            ASTPtr ast_from;
            if (!parser_string_literal.parse(pos, ast_from, expected))
                return false;

            command->replace = false;
            command->from = ast_from->as<ASTLiteral &>().value.get<const String &>();
            command->parts = true;
            command->type = ASTAlterCommand::ATTACH_PARTITION;
        }
        else if (s_attach_partition.ignore(pos, expected))
        {
            if (!parser_partition.parse(pos, command->partition, expected))
                return false;

            if (s_from.ignore(pos))
            {
                if (!parseDatabaseAndTableName(pos, expected, command->from_database, command->from_table))
                    return false;

                command->replace = false;
                command->type = ASTAlterCommand::REPLACE_PARTITION;
            }
            else
            {
                command->type = ASTAlterCommand::ATTACH_PARTITION;
            }
        }
        else if (s_replace_partition_where.ignore(pos, expected))
        {
            if (!parser_exp_elem.parse(pos, command->predicate, expected))
                return false;

            if (!s_from.ignore(pos, expected))
                return false;

            if (!parseDatabaseAndTableName(pos, expected, command->from_database, command->from_table))
                return false;

            command->detach = true;
            command->replace = true;
            command->detach = true;
            command->type = ASTAlterCommand::REPLACE_PARTITION_WHERE;
        }
        else if (s_replace_partition.ignore(pos, expected))
        {
            if (!parser_partition.parse(pos, command->partition, expected))
                return false;

            if (!s_from.ignore(pos, expected))
                return false;

            if (!parseDatabaseAndTableName(pos, expected, command->from_database, command->from_table))
                return false;

            command->replace = true;
            command->type = ASTAlterCommand::REPLACE_PARTITION;
        }
        else if (s_ingest_partition.ignore(pos, expected))
        {
            if (!parser_partition.parse(pos, command->partition, expected))
                return false;

            if (!ParserKeyword("COLUMNS").ignore(pos, expected))
                return false;

            ParserList parser_column_list(std::make_unique<ParserExpression>(), std::make_unique<ParserToken>(TokenType::Comma));

            if (!parser_column_list.parse(pos, command->columns, expected))
                return false;

            if (ParserKeyword("KEY").ignore(pos, expected))
            {
                if (!parser_column_list.parse(pos, command->keys, expected))
                    return false;
            }

            if (!s_from.ignore(pos, expected))
                return false;

            if (!parseDatabaseAndTableName(pos, expected, command->from_database, command->from_table))
                return false;

            command->type = ASTAlterCommand::INGEST_PARTITION;
        }
        else if (s_attach_part.ignore(pos, expected))
        {
            if (!parser_string_literal.parse(pos, command->partition, expected))
                return false;

            command->part = true;
            command->type = ASTAlterCommand::ATTACH_PARTITION;
        }
        else if (s_fetch_partition_where.ignore(pos, expected))
        {
            if (!parser_exp_elem.parse(pos, command->predicate, expected))
                return false;

            if (!s_from.ignore(pos, expected))
                return false;

            ASTPtr ast_from;
            if (!parser_string_literal.parse(pos, ast_from, expected))
                return false;

            command->from = typeid_cast<const ASTLiteral &>(*ast_from).value.get<const String &>();
            command->type = ASTAlterCommand::FETCH_PARTITION_WHERE;
        }
        else if (s_fetch_partition.ignore(pos, expected))
        {
            if (!parser_partition.parse(pos, command->partition, expected))
                return false;

            if (!s_from.ignore(pos, expected))
                return false;

            ASTPtr ast_from;
            if (!parser_string_literal.parse(pos, ast_from, expected))
                return false;

            command->from = ast_from->as<ASTLiteral &>().value.get<const String &>();
            command->type = ASTAlterCommand::FETCH_PARTITION;
        }
        else if (s_repair_partition.ignore(pos, expected))
        {
            if (!parser_partition.parse(pos, command->partition, expected))
                return false;

            if (!s_from.ignore(pos, expected))
                return false;

            ASTPtr ast_from;
            if (!parser_string_literal.parse(pos, ast_from, expected))
                return false;

            command->from = ast_from->as<ASTLiteral &>().value.get<const String &>();
            command->type = ASTAlterCommand::REPAIR_PARTITION;
        }
        else if (s_repair_part.ignore(pos, expected))
        {
            if (!parser_string_literal.parse(pos, command->partition, expected))
                return false;

            if (!s_from.ignore(pos, expected))
                return false;

            ASTPtr ast_from;
            if (!parser_string_literal.parse(pos, ast_from, expected))
                return false;

            command->part = true;
            command->from = ast_from->as<ASTLiteral &>().value.get<const String &>();
            command->type = ASTAlterCommand::REPAIR_PARTITION;
        }
        else if (s_fetch_part.ignore(pos, expected))
        {
            if (!parser_string_literal.parse(pos, command->partition, expected))
                return false;

            if (!s_from.ignore(pos, expected))
                return false;

            ASTPtr ast_from;
            if (!parser_string_literal.parse(pos, ast_from, expected))
                return false;
            command->from = ast_from->as<ASTLiteral &>().value.get<const String &>();
            command->part = true;
            command->type = ASTAlterCommand::FETCH_PARTITION;
        }
        else if (s_freeze.ignore(pos, expected))
        {
            if (s_partition.ignore(pos, expected))
            {
                if (!parser_partition.parse(pos, command->partition, expected))
                    return false;

                command->type = ASTAlterCommand::FREEZE_PARTITION;
            }
            else
            {
                command->type = ASTAlterCommand::FREEZE_ALL;
            }

            /// WITH NAME 'name' - place local backup to directory with specified name
            if (s_with.ignore(pos, expected))
            {
                if (!s_name.ignore(pos, expected))
                    return false;

                ASTPtr ast_with_name;
                if (!parser_string_literal.parse(pos, ast_with_name, expected))
                    return false;

                command->with_name = ast_with_name->as<ASTLiteral &>().value.get<const String &>();
            }
        }
        else if (s_unfreeze.ignore(pos, expected))
        {
            if (s_partition.ignore(pos, expected))
            {
                if (!parser_partition.parse(pos, command->partition, expected))
                    return false;

                command->type = ASTAlterCommand::UNFREEZE_PARTITION;
            }
            else
            {
                command->type = ASTAlterCommand::UNFREEZE_ALL;
            }

            /// WITH NAME 'name' - remove local backup to directory with specified name
            if (s_with.ignore(pos, expected))
            {
                if (!s_name.ignore(pos, expected))
                    return false;

                ASTPtr ast_with_name;
                if (!parser_string_literal.parse(pos, ast_with_name, expected))
                    return false;

                command->with_name = ast_with_name->as<ASTLiteral &>().value.get<const String &>();
            }
            else
            {
                return false;
            }
        }
        else if (s_modify_column.ignore(pos, expected))
        {
            if (s_if_exists.ignore(pos, expected))
                command->if_exists = true;

            if (!parser_modify_col_decl.parse(pos, command->col_decl, expected))
                return false;

            if (s_remove.ignore(pos, expected))
            {
                if (s_default.ignore(pos, expected))
                    command->remove_property = "DEFAULT";
                else if (s_materialized.ignore(pos, expected))
                    command->remove_property = "MATERIALIZED";
                else if (s_alias.ignore(pos, expected))
                    command->remove_property = "ALIAS";
                else if (s_comment.ignore(pos, expected))
                    command->remove_property = "COMMENT";
                else if (s_codec.ignore(pos, expected))
                    command->remove_property = "CODEC";
                else if (s_ttl.ignore(pos, expected))
                    command->remove_property = "TTL";
                else
                    return false;
            }
            else
            {
                if (s_first.ignore(pos, expected))
                    command->first = true;
                else if (s_after.ignore(pos, expected))
                {
                    if (!parser_name.parse(pos, command->column, expected))
                        return false;
                }
            }
            command->type = ASTAlterCommand::MODIFY_COLUMN;
        }
        else if (s_modify_order_by.ignore(pos, expected))
        {
            if (!parser_exp_elem.parse(pos, command->order_by, expected))
                return false;

            command->type = ASTAlterCommand::MODIFY_ORDER_BY;
        }
        else if (s_modify_cluster_by.ignore(pos, expected))
        {
            if (!cluster_p.parse(pos, command->cluster_by, expected))
                return false;

            command->type = ASTAlterCommand::MODIFY_CLUSTER_BY;
        }
        else if (s_drop_cluster.ignore(pos, expected))
        {
            command->type = ASTAlterCommand::DROP_CLUSTER;
        }
        else if (s_modify_sample_by.ignore(pos, expected))
        {
            if (!parser_exp_elem.parse(pos, command->sample_by, expected))
                return false;

            command->type = ASTAlterCommand::MODIFY_SAMPLE_BY;
        }
        else if (s_delete.ignore(pos, expected))
        {
            if (s_in_partition.ignore(pos, expected))
            {
                if (!parser_partition.parse(pos, command->partition, expected))
                    return false;
            }

            if (!s_where.ignore(pos, expected))
                return false;

            if (!parser_exp_elem.parse(pos, command->predicate, expected))
                return false;

            command->type = ASTAlterCommand::DELETE;
        }
        else if (s_fast_delete.ignore(pos, expected))
        {
            if (!s_in_partition.checkWithoutMoving(pos, expected)
                && !s_where.checkWithoutMoving(pos, expected))
            {
                ParserList parser_column_list(std::make_unique<ParserTupleElementExpression>(ParserSettings::CLICKHOUSE), std::make_unique<ParserToken>(TokenType::Comma));
                if (!parser_column_list.parse(pos, command->columns, expected))
                    return false;
            }

            if (s_in_partition.ignore(pos, expected))
            {
                if (!parser_partition.parse(pos, command->partition, expected))
                    return false;
            }

            if (!s_where.ignore(pos, expected))
                return false;

            if (!parser_exp_elem.parse(pos, command->predicate, expected))
                return false;

            command->type = ASTAlterCommand::FAST_DELETE;
        }
        else if (s_update.ignore(pos, expected))
        {
            if (!parser_assignment_list.parse(pos, command->update_assignments, expected))
                return false;

            if (s_in_partition.ignore(pos, expected))
            {
                if (!parser_partition.parse(pos, command->partition, expected))
                    return false;
            }

            if (!s_where.ignore(pos, expected))
                return false;

            if (!parser_exp_elem.parse(pos, command->predicate, expected))
                return false;

            command->type = ASTAlterCommand::UPDATE;
        }
        else if (s_drop_bitmap_of_partition_where.ignore(pos, expected))
        {
            if (!parser_exp_elem.parse(pos, command->predicate, expected))
                return false;

            command->type = ASTAlterCommand::DROP_BITMAP_OF_PARTITION_WHERE;
        }
        else if (s_drop_bitmap_of_partition.ignore(pos, expected))
        {
            if (!parser_partition.parse(pos, command->partition, expected))
                return false;

            command->type = ASTAlterCommand::DROP_BITMAP_OF_PARTITION;
        }
        else if (s_build_bitmap_of_partition_where.ignore(pos, expected))
        {
            if (!parser_exp_elem.parse(pos, command->predicate, expected))
                return false;

            command->type = ASTAlterCommand::BUILD_BITMAP_OF_PARTITION_WHERE;
        }
        else if (s_build_bitmap_of_partition.ignore(pos, expected))
        {
            if (!parser_partition.parse(pos, command->partition, expected))
                return false;

            command->type = ASTAlterCommand::BUILD_BITMAP_OF_PARTITION;
        }
        else if (s_drop_mark_bitmap_of_partition_where.ignore(pos, expected))
        {
            if (!parser_exp_elem.parse(pos, command->predicate, expected))
                return false;

            command->type = ASTAlterCommand::DROP_MARK_BITMAP_OF_PARTITION_WHERE;
        }
        else if (s_drop_mark_bitmap_of_partition.ignore(pos, expected))
        {
            if (!parser_partition.parse(pos, command->partition, expected))
                return false;

            command->type = ASTAlterCommand::DROP_MARK_BITMAP_OF_PARTITION;
        }
        else if (s_build_mark_bitmap_of_partition_where.ignore(pos, expected))
        {
            if (!parser_exp_elem.parse(pos, command->predicate, expected))
                return false;

            command->type = ASTAlterCommand::BUILD_BITMAP_OF_PARTITION_WHERE;
        }
        else if (s_build_mark_bitmap_of_partition.ignore(pos, expected))
        {
            if (!parser_partition.parse(pos, command->partition, expected))
                return false;

            command->type = ASTAlterCommand::BUILD_MARK_BITMAP_OF_PARTITION;
        }
        else if (s_comment_column.ignore(pos, expected))
        {
            if (s_if_exists.ignore(pos, expected))
                command->if_exists = true;

            if (!parser_name.parse(pos, command->column, expected))
                return false;

            if (!parser_string_literal.parse(pos, command->comment, expected))
                return false;

            command->type = ASTAlterCommand::COMMENT_COLUMN;
        }
        else if (s_modify_ttl.ignore(pos, expected))
        {
            if (!parser_ttl_list.parse(pos, command->ttl, expected))
                return false;
            command->type = ASTAlterCommand::MODIFY_TTL;
        }
        else if (s_remove_ttl.ignore(pos, expected))
        {
            command->type = ASTAlterCommand::REMOVE_TTL;
        }
        else if (s_materialize_ttl.ignore(pos, expected))
        {
            command->type = ASTAlterCommand::MATERIALIZE_TTL;

            if (s_in_partition.ignore(pos, expected))
            {
                if (!parser_partition.parse(pos, command->partition, expected))
                    return false;
            }
        }
        else if (s_modify_setting.ignore(pos, expected))
        {
            if (!parser_settings.parse(pos, command->settings_changes, expected))
                return false;
            command->type = ASTAlterCommand::MODIFY_SETTING;
        }
        else if (s_reset_setting.ignore(pos, expected))
        {
            if (!parser_reset_setting.parse(pos, command->settings_resets, expected))
                return false;
            command->type = ASTAlterCommand::RESET_SETTING;
        }
        else if (s_modify_query.ignore(pos, expected))
        {
            if (!select_p.parse(pos, command->select, expected))
                return false;
            command->type = ASTAlterCommand::MODIFY_QUERY;
        }
        else if (s_sample_partition_with.ignore(pos, expected))
        {
            if (!parser_exp_elem.parse(pos, command->with_sharding_exp, expected))
                return false;

            if (!s_where.ignore(pos, expected))
                return false;

            if (!parser_exp_elem.parse(pos, command->predicate, expected))
                return false;

            if (!s_to.ignore(pos, expected))
                return false;

            if (!parseDatabaseAndTableName(pos, expected, command->from_database, command->from_table))
                return false;

            command->type = ASTAlterCommand::SAMPLE_PARTITION_WHERE;
        }
        else
            return false;
    }

    if (command->col_decl)
        command->children.push_back(command->col_decl);
    if (command->column)
        command->children.push_back(command->column);
    if (command->partition)
        command->children.push_back(command->partition);
    if (command->order_by)
        command->children.push_back(command->order_by);
    if (command->cluster_by)
        command->children.push_back(command->cluster_by);
    if (command->sample_by)
        command->children.push_back(command->sample_by);
    if (command->index_decl)
        command->children.push_back(command->index_decl);
    if (command->index)
        command->children.push_back(command->index);
    if (command->constraint_decl)
        command->children.push_back(command->constraint_decl);
    if (command->constraint)
        command->children.push_back(command->constraint);
    if (command->predicate)
        command->children.push_back(command->predicate);
    if (command->update_assignments)
        command->children.push_back(command->update_assignments);
    if (command->values)
        command->children.push_back(command->values);
    if (command->comment)
        command->children.push_back(command->comment);
    if (command->ttl)
        command->children.push_back(command->ttl);
    if (command->settings_changes)
        command->children.push_back(command->settings_changes);
    if (command->select)
        command->children.push_back(command->select);
    if (command->rename_to)
        command->children.push_back(command->rename_to);

    return true;
}


bool ParserAlterCommandList::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    auto command_list = std::make_shared<ASTExpressionList>();
    node = command_list;

    ParserToken s_comma(TokenType::Comma);
    ParserAlterCommand p_command(dt, is_live_view);

    do
    {
        ASTPtr command;
        if (!p_command.parse(pos, command, expected))
            return false;

        command_list->children.push_back(command);
    }
    while (s_comma.ignore(pos, expected));

    return true;
}


bool ParserAlterQuery::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    auto query = std::make_shared<ASTAlterQuery>();
    node = query;

    ParserKeyword s_alter_table("ALTER TABLE");
    ParserKeyword s_alter_live_view("ALTER LIVE VIEW");

    bool is_live_view = false;

    if (!s_alter_table.ignore(pos, expected))
    {
        if (!s_alter_live_view.ignore(pos, expected))
            return false;
        else
            is_live_view = true;
    }

    if (is_live_view)
        query->is_live_view = true;

    if (!parseDatabaseAndTableName(pos, expected, query->database, query->table))
        return false;

    String cluster_str;
    if (ParserKeyword{"ON"}.ignore(pos, expected))
    {
        if (!ASTQueryWithOnCluster::parse(pos, cluster_str, expected))
            return false;
    }
    query->cluster = cluster_str;

    ParserAlterCommandList p_command_list(dt, is_live_view);
    ASTPtr command_list;
    if (!p_command_list.parse(pos, command_list, expected))
        return false;

    query->set(query->command_list, command_list);

    return true;
}

}
