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

#include <Parsers/ASTAlterQuery.h>
#include <Storages/IStorage_fwd.h>
#include <DataTypes/IDataType.h>
#include <Core/Names.h>

#include <optional>
#include <unordered_map>


namespace DB
{

class Context;
class WriteBuffer;
class ReadBuffer;

/// Represents set of actions which should be applied
/// to values from set of columns which satisfy predicate.
struct MutationCommand
{
    ASTPtr ast; /// The AST of the whole command

    enum Type
    {
        EMPTY,     /// Not used.
        DELETE,
        FAST_DELETE,
        UPDATE,
        MATERIALIZE_INDEX,
        MATERIALIZE_PROJECTION,
        ADD_COLUMN, /// For detecting conflicts
        READ_COLUMN, /// Read column and apply conversions (MODIFY COLUMN alter query).
        DROP_COLUMN,
        DROP_INDEX,
        DROP_PROJECTION,
        MATERIALIZE_TTL,
        RENAME_COLUMN,
        CLEAR_MAP_KEY,
        RECLUSTER,
    };

    Type type = EMPTY;

    /// WHERE part of mutation
    ASTPtr predicate;

    /// Columns with corresponding actions
    std::unordered_map<String, ASTPtr> column_to_update_expression;

    /// For MATERIALIZE INDEX and PROJECTION
    String index_name;
    String projection_name;

    /// For MATERIALIZE INDEX, UPDATE and DELETE/FASTDELETE.
    ASTPtr partition;

    /// For CLEAR MAP KEYS
    ASTPtr map_keys;

    /// For reads, drops and etc.
    String column_name;
    DataTypePtr data_type; /// Maybe empty if we just want to drop column

    /// We need just clear column, not drop from metadata.
    bool clear = false;

    /// Column rename_to
    String rename_to;

    /// For FASTDELETE columns
    ASTPtr columns;

    /// If parse_alter_commands, than consider more Alter commands as mutation commands
    static std::optional<MutationCommand> parse(ASTAlterCommand * command, bool parse_alter_commands = false);
};

/// Multiple mutation commands, possible from different ALTER queries
class MutationCommands : public std::vector<MutationCommand>
{
public:
    std::shared_ptr<ASTExpressionList> ast() const;

    bool willMutateData() const;

    /// whether current commands can be executed together with other commands
    bool requireIndependentExecution() const;

    bool allOf(MutationCommand::Type type) const;

    void writeText(WriteBuffer & out) const;
    void readText(ReadBuffer & in);
    bool changeSchema() const
    {
        for (const auto & command : *this)
        {
            if (command.type != MutationCommand::EMPTY &&
                /* command.type != MutationCommand::BUILD_BITMAP && */
                command.type != MutationCommand::CLEAR_MAP_KEY &&
                command.type != MutationCommand::MATERIALIZE_INDEX /* &&
                command.type != MutationCommand::DROP_BUILD_BITMAP */)

                return true;
        }
        return false;
    }
};

}
