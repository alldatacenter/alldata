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

#include <Storages/PartitionCommands.h>
#include <Storages/IStorage.h>
#include <Storages/DataDestinationType.h>
#include <Parsers/ASTAlterQuery.h>
#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTFunction.h>
#include <Parsers/ASTLiteral.h>
#include <Core/ColumnWithTypeAndName.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/MapHelpers.h>
#include <Processors/Chunk.h>
#include <Processors/Pipe.h>
#include <Processors/Sources/SourceFromSingleChunk.h>
#include <Interpreters/IdentifierSemantic.h>


namespace DB
{

namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
}

std::optional<PartitionCommand> PartitionCommand::parse(const ASTAlterCommand * command_ast)
{
    if (command_ast->type == ASTAlterCommand::DROP_PARTITION)
    {
        PartitionCommand res;
        res.type = DROP_PARTITION;
        res.partition = command_ast->partition;
        res.detach = command_ast->detach;
        res.cascading = command_ast->cascading;
        res.part = command_ast->part;
        return res;
    }
    else if (command_ast->type == ASTAlterCommand::DROP_DETACHED_PARTITION)
    {
        PartitionCommand res;
        res.type = DROP_DETACHED_PARTITION;
        res.partition = command_ast->partition;
        res.part = command_ast->part;
        return res;
    }
    else if (command_ast->type == ASTAlterCommand::DROP_PARTITION_WHERE)
    {
        PartitionCommand res;
        res.type = DROP_PARTITION_WHERE;
        res.partition = command_ast->predicate;
        res.cascading = command_ast->cascading;
        res.detach = command_ast->detach;
        return res;
    }
    else if (command_ast->type == ASTAlterCommand::ATTACH_PARTITION)
    {
        PartitionCommand res;
        res.type = ATTACH_PARTITION;
        res.partition = command_ast->partition;
        res.part = command_ast->part;
        res.parts = command_ast->parts;
        res.replace = command_ast->replace;
        res.from_zookeeper_path = command_ast->from;
        return res;
    }
    else if (command_ast->type == ASTAlterCommand::ATTACH_DETACHED_PARTITION)
    {
        PartitionCommand res;
        res.type = ATTACH_DETACHED_PARTITION;
        res.partition = command_ast->partition;
        res.replace = command_ast->replace;
        res.attach_from_detached = command_ast->attach_from_detached;
        res.from_database = command_ast->from_database;
        res.from_table = command_ast->from_table;
        res.detach = command_ast->detach;
        return res;
    }
    else if (command_ast->type == ASTAlterCommand::MOVE_PARTITION)
    {
        PartitionCommand res;
        res.type = MOVE_PARTITION;
        res.partition = command_ast->partition;
        res.part = command_ast->part;
        switch (command_ast->move_destination_type)
        {
            case DataDestinationType::DISK:
                res.move_destination_type = PartitionCommand::MoveDestinationType::DISK;
                break;
            case DataDestinationType::VOLUME:
                res.move_destination_type = PartitionCommand::MoveDestinationType::VOLUME;
                break;
            case DataDestinationType::TABLE:
                res.move_destination_type = PartitionCommand::MoveDestinationType::TABLE;
                res.to_database = command_ast->to_database;
                res.to_table = command_ast->to_table;
                break;
            case DataDestinationType::SHARD:
                res.move_destination_type = PartitionCommand::MoveDestinationType::SHARD;
                break;
            case DataDestinationType::DELETE:
                throw Exception("ALTER with this destination type is not handled. This is a bug.", ErrorCodes::LOGICAL_ERROR);
        }
        if (res.move_destination_type != PartitionCommand::MoveDestinationType::TABLE)
            res.move_destination_name = command_ast->move_destination_name;
        return res;
    }
    else if (command_ast->type == ASTAlterCommand::MOVE_PARTITION_FROM)
    {
        PartitionCommand res;
        res.type = MOVE_PARTITION_FROM;
        res.partition = command_ast->partition;
        res.from_database = command_ast->from_database;
        res.from_table = command_ast->from_table;
        return res;
    }
    else if (command_ast->type == ASTAlterCommand::REPLACE_PARTITION)
    {
        PartitionCommand res;
        res.type = REPLACE_PARTITION;
        res.partition = command_ast->partition;
        res.replace = command_ast->replace;
        res.cascading = command_ast->cascading;
        res.from_database = command_ast->from_database;
        res.from_table = command_ast->from_table;
        res.from_zookeeper_path = command_ast->from;
        res.detach = command_ast->detach;
        return res;
    }
    else if (command_ast->type == ASTAlterCommand::REPLACE_PARTITION_WHERE)
    {
        PartitionCommand res;
        res.type = REPLACE_PARTITION_WHERE;
        res.partition = command_ast->predicate;
        res.replace = command_ast->replace;
        res.cascading = command_ast->cascading;
        res.from_database = command_ast->from_database;
        res.from_table = command_ast->from_table;
        return res;
    }
    else if (command_ast->type == ASTAlterCommand::INGEST_PARTITION)
    {
        PartitionCommand res;
        res.type = INGEST_PARTITION;
        res.partition = command_ast->partition;

        const auto & column_expr_list = command_ast->columns->as<ASTExpressionList &>();
        for (const auto & child : column_expr_list.children)
        {
            if (auto * identifier = child->as<ASTIdentifier>())
            {
                res.column_names.push_back(identifier->name());
                continue;
            }
            else if (auto * function = child->as<ASTFunction>())
            {
                if (startsWith(Poco::toLower(function->name), "mapelement") && function->arguments->children.size() == 2)
                {
                    ASTIdentifier * map_col = function->arguments->children[0]->as<ASTIdentifier>();
                    ASTLiteral * key_lit = function->arguments->children[1]->as<ASTLiteral>(); // Constant Literal
                    ASTFunction * key_func = function->arguments->children[1]->as<ASTFunction>(); // for constant foldable functions' case

                    if (map_col && !IdentifierSemantic::isSpecial(*map_col))
                    {
                        String key_name;
                        if (key_lit) // key is literal
                            key_name = key_lit->getColumnName();
                        else if (key_func)
                            throw Exception("Invalid map key for Ingestion", ErrorCodes::BAD_ARGUMENTS);

                        if (!key_name.empty())
                        {
                            res.column_names.push_back(getImplicitColNameForMapKey(map_col->name(), key_name));
                            continue;
                        }
                    }
                }
            }

            throw Exception("Illegal column: " + child->getColumnName(), ErrorCodes::BAD_ARGUMENTS);
        }

        if (command_ast->keys)
        {
            const auto & key_expr_list = command_ast->keys->as<ASTExpressionList &>();
            for (const auto & child : key_expr_list.children)
            {
                if (auto * identifier = child->as<ASTIdentifier>())
                    res.key_names.push_back(identifier->name());
                else
                    throw Exception("Illegal key: " + child->getColumnName(), ErrorCodes::BAD_ARGUMENTS);
            }
        }

        res.from_database = command_ast->from_database;
        res.from_table = command_ast->from_table;

        return res;
    }
    else if (command_ast->type == ASTAlterCommand::FETCH_PARTITION)
    {
        PartitionCommand res;
        res.type = FETCH_PARTITION;
        res.partition = command_ast->partition;
        res.from_zookeeper_path = command_ast->from;
        res.part = command_ast->part;
        return res;
    }
    else if (command_ast->type == ASTAlterCommand::FETCH_PARTITION_WHERE)
    {
        PartitionCommand res;
        res.type = FETCH_PARTITION_WHERE;
        res.partition = command_ast->predicate;
        res.from_zookeeper_path = command_ast->from;
        return res;
    }
    else if (command_ast->type == ASTAlterCommand::REPAIR_PARTITION)
    {
        PartitionCommand res;
        res.type = REPAIR_PARTITION;
        res.part = command_ast->part;
        res.partition = command_ast->partition;
        res.from_zookeeper_path = command_ast->from;
        return res;
    }
    else if (command_ast->type == ASTAlterCommand::FREEZE_PARTITION)
    {
        PartitionCommand res;
        res.type = FREEZE_PARTITION;
        res.partition = command_ast->partition;
        res.with_name = command_ast->with_name;
        return res;
    }
    else if (command_ast->type == ASTAlterCommand::FREEZE_ALL)
    {
        PartitionCommand res;
        res.type = PartitionCommand::FREEZE_ALL_PARTITIONS;
        res.with_name = command_ast->with_name;
        return res;
    }
    else if (command_ast->type == ASTAlterCommand::UNFREEZE_PARTITION)
    {
        PartitionCommand res;
        res.type = PartitionCommand::UNFREEZE_PARTITION;
        res.partition = command_ast->partition;
        res.with_name = command_ast->with_name;
        return res;
    }
    else if (command_ast->type == ASTAlterCommand::UNFREEZE_ALL)
    {
        PartitionCommand res;
        res.type = PartitionCommand::UNFREEZE_ALL_PARTITIONS;
        res.with_name = command_ast->with_name;
        return res;
    }
    else if(command_ast->type == ASTAlterCommand::SAMPLE_PARTITION_WHERE)
    {
        PartitionCommand res;
        res.type = SAMPLE_PARTITION_WHERE;
        res.sharding_exp = command_ast->with_sharding_exp;
        res.partition = command_ast->predicate;
        res.from_database = command_ast->from_database;
        res.from_table = command_ast->from_table;
        return res;
    }
    else
        return {};
}

std::string PartitionCommand::typeToString() const
{
    switch (type)
    {
    case PartitionCommand::Type::ATTACH_PARTITION:
        if (part)
            return "ATTACH PART";
        else
            return "ATTACH PARTITION";
    case PartitionCommand::Type::MOVE_PARTITION:
        return "MOVE PARTITION";
    case PartitionCommand::Type::MOVE_PARTITION_FROM:
        return "MOVE PARTITION FROM";
    case PartitionCommand::Type::DROP_PARTITION:
        if (detach)
            return "DETACH PARTITION";
        else
            return "DROP PARTITION";
    case PartitionCommand::Type::DROP_PARTITION_WHERE:
        if (detach)
            return "DETACH PARTITION WHERE";
        else
            return "DROP PARTITION WHERE";
    case PartitionCommand::Type::DROP_DETACHED_PARTITION:
        if (part)
            return "DROP DETACHED PART";
        else
            return "DROP DETACHED PARTITION";
    case PartitionCommand::Type::FETCH_PARTITION:
        if (part)
            return "FETCH PART";
        else
            return "FETCH PARTITION";
    case PartitionCommand::Type::FETCH_PARTITION_WHERE:
        return "FETCH PARTITION WHERE";
    case PartitionCommand::Type::FREEZE_ALL_PARTITIONS:
        return "FREEZE ALL";
    case PartitionCommand::Type::FREEZE_PARTITION:
        return "FREEZE PARTITION";
    case PartitionCommand::Type::UNFREEZE_PARTITION:
        return "UNFREEZE PARTITION";
    case PartitionCommand::Type::UNFREEZE_ALL_PARTITIONS:
        return "UNFREEZE ALL";
    case PartitionCommand::Type::REPLACE_PARTITION:
        return "REPLACE PARTITION";
    case PartitionCommand::Type::INGEST_PARTITION:
        return "INGEST PARTITION";
    default:
        throw Exception("Uninitialized partition command", ErrorCodes::LOGICAL_ERROR);
    }
}

Pipe convertCommandsResultToSource(const PartitionCommandsResultInfo & commands_result)
{
    Block header {
         ColumnWithTypeAndName(std::make_shared<DataTypeString>(), "command_type"),
         ColumnWithTypeAndName(std::make_shared<DataTypeString>(), "partition_id"),
         ColumnWithTypeAndName(std::make_shared<DataTypeString>(), "part_name"),
    };

    for (const auto & command_result : commands_result)
    {
        if (!command_result.old_part_name.empty() && !header.has("old_part_name"))
            header.insert(ColumnWithTypeAndName(std::make_shared<DataTypeString>(), "old_part_name"));

        if (!command_result.backup_name.empty() && !header.has("backup_name"))
            header.insert(ColumnWithTypeAndName(std::make_shared<DataTypeString>(), "backup_name"));

        if (!command_result.backup_path.empty() && !header.has("backup_path"))
            header.insert(ColumnWithTypeAndName(std::make_shared<DataTypeString>(), "backup_path"));
        if (!command_result.backup_path.empty() && !header.has("part_backup_path"))
            header.insert(ColumnWithTypeAndName(std::make_shared<DataTypeString>(), "part_backup_path"));
    }

    MutableColumns res_columns = header.cloneEmptyColumns();

    for (const auto & command_result : commands_result)
    {
        res_columns[0]->insert(command_result.command_type);
        res_columns[1]->insert(command_result.partition_id);
        res_columns[2]->insert(command_result.part_name);
        if (header.has("old_part_name"))
        {
            size_t pos = header.getPositionByName("old_part_name");
            res_columns[pos]->insert(command_result.old_part_name);
        }
        if (header.has("backup_name"))
        {
            size_t pos = header.getPositionByName("backup_name");
            res_columns[pos]->insert(command_result.backup_name);
        }
        if (header.has("backup_path"))
        {
            size_t pos = header.getPositionByName("backup_path");
            res_columns[pos]->insert(command_result.backup_path);
        }
        if (header.has("part_backup_path"))
        {
            size_t pos = header.getPositionByName("part_backup_path");
            res_columns[pos]->insert(command_result.part_backup_path);
        }
    }

    Chunk chunk(std::move(res_columns), commands_result.size());
    return Pipe(std::make_shared<SourceFromSingleChunk>(std::move(header), std::move(chunk)));
}

bool partitionCommandHasWhere(const PartitionCommand & command)
{
    return command.type == PartitionCommand::Type::DROP_PARTITION_WHERE || command.type == PartitionCommand::Type::FETCH_PARTITION_WHERE
        || command.type == PartitionCommand::Type::REPLACE_PARTITION_WHERE || command.type == PartitionCommand::Type::SAMPLE_PARTITION_WHERE;
}
}
