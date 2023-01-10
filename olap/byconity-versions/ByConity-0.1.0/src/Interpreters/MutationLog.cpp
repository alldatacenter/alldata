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

#include <Columns/ColumnString.h>
#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypesNumber.h>
#include <DataTypes/DataTypeDateTime.h>
#include <DataTypes/DataTypeDate.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypeEnum.h>
#include <Interpreters/MutationLog.h>

namespace DB
{

NamesAndTypesList MutationLogElement::getNamesAndTypes()
{
    auto event_type_datatype = std::make_shared<DataTypeEnum8>(
        DataTypeEnum8::Values {
            {"MUTATION_START",  static_cast<Int8>(MUTATION_START)},
            {"MUTATION_KILL", static_cast<Int8>(MUTATION_KILL)},
            {"MUTATION_FINISH", static_cast<Int8>(MUTATION_FINISH)},
            {"MUTATION_ABORT", static_cast<Int8>(MUTATION_ABORT)},
        });

    return {
        {"event_type", std::move(event_type_datatype)},
        {"event_date", std::make_shared<DataTypeDate>()},
        {"event_time", std::make_shared<DataTypeDateTime>()},

        {"database", std::make_shared<DataTypeString>()},
        {"table", std::make_shared<DataTypeString>()},
        {"mutation_id", std::make_shared<DataTypeString>()},
        {"query_id", std::make_shared<DataTypeString>()},
        {"create_time", std::make_shared<DataTypeDateTime>()},
        {"block_number", std::make_shared<DataTypeUInt64>()},
        {"commands", std::make_shared<DataTypeArray>(std::make_shared<DataTypeString>())},
    };

}

void MutationLogElement::appendToBlock(MutableColumns & columns) const
{
    size_t i = 0;

    columns[i++]->insert(UInt64(event_type));
    columns[i++]->insert(UInt64(DateLUT::instance().toDayNum(event_time)));
    columns[i++]->insert(UInt64(event_time));

    columns[i++]->insert(database_name);
    columns[i++]->insert(table_name);
    columns[i++]->insert(mutation_id);
    columns[i++]->insert(query_id);
    columns[i++]->insert(UInt64(create_time));
    columns[i++]->insert(block_number);

    Array commands_array;
    commands_array.reserve(commands.size());
    for (auto & command : commands)
        commands_array.push_back(command);
    columns[i++]->insert(commands_array);
}

} // end of namespace DB
