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

#include "QueryExchangeLog.h"
#include <array>
#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypeDate.h>
#include <DataTypes/DataTypeByteMap.h>
#include <DataTypes/DataTypeDateTime.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypesNumber.h>
#include <Interpreters/ProfileEventsExt.h>

namespace DB
{

NamesAndTypesList QueryExchangeLogElement::getNamesAndTypes()
{
    return {
        {"initial_query_id", std::make_shared<DataTypeString>()},
        {"event_date", std::make_shared<DataTypeDate>()},
        {"event_time", std::make_shared<DataTypeDateTime>()},
        {"type", std::make_shared<DataTypeString>()},
        {"write_segment_id", std::make_shared<DataTypeString>()},
        {"read_segment_id", std::make_shared<DataTypeString>()},
        {"partition_id", std::make_shared<DataTypeString>()},
        {"coordinator_address", std::make_shared<DataTypeString>()},

        {"finish_code", std::make_shared<DataTypeInt32>()},
        {"is_modifier", std::make_shared<DataTypeInt8>()},
        {"message", std::make_shared<DataTypeString>()},

        {"send_time_ms", std::make_shared<DataTypeUInt64>()},
        {"send_rows", std::make_shared<DataTypeUInt64>()},
        {"send_bytes", std::make_shared<DataTypeUInt64>()},
        {"send_uncompressed_bytes", std::make_shared<DataTypeUInt64>()},
        {"num_send_times", std::make_shared<DataTypeUInt64>()},
        {"ser_time_ms", std::make_shared<DataTypeUInt64>()},
        {"send_retry", std::make_shared<DataTypeInt64>()},
        {"send_retry_ms", std::make_shared<DataTypeInt64>()},
        {"overcrowded_retry", std::make_shared<DataTypeInt64>()},

        {"recv_time_ms", std::make_shared<DataTypeUInt64>()},
        {"register_time_ms", std::make_shared<DataTypeUInt64>()},
        {"recv_bytes", std::make_shared<DataTypeUInt64>()},
        {"dser_time_ms", std::make_shared<DataTypeInt64>()},

#ifdef USE_COMMUNITY_MAP
        {"ProfileEvents", std::make_shared<DataTypeMap>(std::make_shared<DataTypeString>(), std::make_shared<DataTypeUInt64>())},
#else
        {"ProfileEvents", std::make_shared<DataTypeByteMap>(std::make_shared<DataTypeString>(), std::make_shared<DataTypeUInt64>())},
#endif
    };
}

NamesAndAliases QueryExchangeLogElement::getNamesAndAliases()
{
    return
    {
        {"ProfileEvents.Names", {std::make_shared<DataTypeArray>(std::make_shared<DataTypeString>())}, "mapKeys(ProfileEvents)"},
        {"ProfileEvents.Values", {std::make_shared<DataTypeArray>(std::make_shared<DataTypeUInt64>())}, "mapValues(ProfileEvents)"}
    };
}

void QueryExchangeLogElement::appendToBlock(MutableColumns & columns) const
{
    size_t i = 0;
    columns[i++]->insert(initial_query_id);
    columns[i++]->insert(DateLUT::instance().toDayNum(event_time).toUnderType());
    columns[i++]->insert(event_time);
    columns[i++]->insertData(type.data(), type.size());
    columns[i++]->insert(write_segment_id);
    columns[i++]->insert(read_segment_id);
    columns[i++]->insert(partition_id);
    columns[i++]->insert(coordinator_address);

    columns[i++]->insert(finish_code);
    columns[i++]->insert(is_modifier);
    columns[i++]->insertData(message.data(), message.size());

    columns[i++]->insert(send_time_ms);
    columns[i++]->insert(send_rows);
    columns[i++]->insert(send_bytes);
    columns[i++]->insert(send_uncompressed_bytes);
    columns[i++]->insert(num_send_times);
    columns[i++]->insert(ser_time_ms);
    columns[i++]->insert(send_retry);
    columns[i++]->insert(send_retry_ms);
    columns[i++]->insert(overcrowded_retry);

    columns[i++]->insert(recv_time_ms);
    columns[i++]->insert(register_time_ms);
    columns[i++]->insert(recv_bytes);
    columns[i++]->insert(dser_time_ms);


    if (profile_counters)
    {
        auto * column = columns[i++].get();
        ProfileEvents::dumpToMapColumn(*profile_counters, column, true);
    }
    else
    {
        columns[i++]->insertDefault();
    }
}

}
