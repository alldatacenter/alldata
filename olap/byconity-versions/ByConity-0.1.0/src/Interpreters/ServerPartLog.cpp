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

#include <Interpreters/ServerPartLog.h>

#include <Columns/ColumnsNumber.h>
#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypeDate.h>
#include <DataTypes/DataTypeDateTime.h>
#include <DataTypes/DataTypeEnum.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypeUUID.h>
#include <DataTypes/DataTypesNumber.h>
#include <Interpreters/Context.h>

#include <Common/CurrentThread.h>


namespace DB
{
NamesAndTypesList ServerPartLogElement::getNamesAndTypes()
{
    auto event_type_datatype = std::make_shared<DataTypeEnum8>(DataTypeEnum8::Values{
        {"InsertPart", static_cast<Int8>(INSERT_PART)},
        {"MergeParts", static_cast<Int8>(MERGE_PARTS)},
        {"MutatePart", static_cast<Int8>(MUTATE_PART)},
        {"DropRange", static_cast<Int8>(DROP_RANGE)},
        {"RemovePart", static_cast<Int8>(REMOVE_PART)},
    });

    return {
        {"event_type", std::move(event_type_datatype)},
        {"event_date", std::make_shared<DataTypeDate>()},
        {"event_time", std::make_shared<DataTypeDateTime>()},
        {"txn_id", std::make_shared<DataTypeUInt64>()},

        {"database", std::make_shared<DataTypeString>()},
        {"table", std::make_shared<DataTypeString>()},
        {"uuid", std::make_shared<DataTypeUUID>()},

        {"part_name", std::make_shared<DataTypeString>()},
        {"partition_id", std::make_shared<DataTypeString>()},
        {"rows", std::make_shared<DataTypeUInt64>()},
        {"bytes", std::make_shared<DataTypeUInt64>()},
        {"num_source_parts", std::make_shared<DataTypeUInt64>()},
        {"source_part_names", std::make_shared<DataTypeArray>(std::make_shared<DataTypeString>())},

        {"error", std::make_shared<DataTypeUInt8>()},
        {"exception", std::make_shared<DataTypeString>()},
    };
}

void ServerPartLogElement::appendToBlock(MutableColumns & columns) const
{
    size_t i = 0;

    columns[i++]->insert(UInt64(event_type));
    columns[i++]->insert(DateLUT::instance().toDayNum(event_time).toUnderType());
    columns[i++]->insert(event_time);
    columns[i++]->insert(txn_id);

    columns[i++]->insert(database_name);
    columns[i++]->insert(table_name);
    columns[i++]->insert(uuid);

    columns[i++]->insert(part_name);
    columns[i++]->insert(partition_id);
    columns[i++]->insert(rows);
    columns[i++]->insert(bytes);

    columns[i++]->insert(source_part_names.size());
    Array source_part_names_array;
    source_part_names_array.reserve(source_part_names.size());
    for (const auto & name : source_part_names)
        source_part_names_array.push_back(name);
    columns[i++]->insert(source_part_names_array);

    columns[i++]->insert(error);
    columns[i++]->insert(exception);
}

/*
bool ServerPartLog::addNewParts(ContextPtr context, ServerPartLogElement::Type type, const MutableMergeTreeDataPartsCNCHVector & parts, UInt64 txn_id, UInt8 error)
{
    std::shared_ptr<ServerPartLog> server_part_log;
    try
    {
        server_part_log = context->getServerPartLog();
        if (!server_part_log)
            return false;

        auto event_time = time(nullptr);

        for (auto & part : parts)
        {
            ServerPartLogElement elem;

            elem.event_type = type;
            elem.event_time = event_time;
            elem.txn_id = txn_id;

            elem.database_name = part->storage.getDatabaseName();
            elem.table_name = part->storage.getTableName();
            elem.uuid = part->storage.getStorageUUID();

            elem.part_name = part->name;
            elem.partition_id = part->info.partition_id;
            elem.rows = part->rows_count;
            elem.bytes = part->bytes_on_disk;

            elem.error = error;

            server_part_log->add(elem);
        }

        return true;
    }
    catch (...)
    {
        tryLogCurrentException(server_part_log ? server_part_log->log : &Logger::get("ServerPartLog"), __PRETTY_FUNCTION__);
        return false;
    }
}
*/

}
