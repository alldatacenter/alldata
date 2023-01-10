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

#include <Columns/ColumnsNumber.h>
#include <Columns/ColumnString.h>
#include <DataTypes/DataTypeEnum.h>
#include <DataTypes/DataTypeDate.h>
#include <DataTypes/DataTypeDateTime.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypesNumber.h>
#include <Interpreters/KafkaLog.h>


namespace DB
{


NamesAndTypesList KafkaLogElement::getNamesAndTypes()
{
    auto event_type_datatype = std::make_shared<DataTypeEnum8>(
            DataTypeEnum8::Values {
            {"EMPTY",           static_cast<Int8>(EMPTY)},
            {"POLL",            static_cast<Int8>(POLL)},
            {"PARSE_ERROR",     static_cast<Int8>(PARSE_ERROR)},
            {"WRITE",           static_cast<Int8>(WRITE)},
            {"EXCEPTION",       static_cast<Int8>(EXCEPTION)},
            {"EMPTY_MESSAGE",   static_cast<Int8>(EMPTY_MESSAGE)},
            {"FILTER",          static_cast<Int8>(FILTER)},
            {"COMMIT",          static_cast<Int8>(COMMIT)},
            });

    return
    {
        {"event_type",      std::move(event_type_datatype)        },
        {"event_date",      std::make_shared<DataTypeDate>()      },
        {"event_time",      std::make_shared<DataTypeDateTime>()  },
        {"duration_ms",     std::make_shared<DataTypeUInt64>()    },

        {"cnch_database",   std::make_shared<DataTypeString>()    },
        {"cnch_table",      std::make_shared<DataTypeString>()    },
        {"database",        std::make_shared<DataTypeString>()    },
        {"table",           std::make_shared<DataTypeString>()    },
        {"consumer",        std::make_shared<DataTypeString>()    },

        {"metric",          std::make_shared<DataTypeUInt64>()    },
        {"bytes",           std::make_shared<DataTypeUInt64>()    },

        {"has_error",       std::make_shared<DataTypeUInt8>()     },
        {"exception",       std::make_shared<DataTypeString>()    },
    };
}

void KafkaLogElement::appendToBlock(MutableColumns & columns) const
{
    size_t i = 0;

    columns[i++]->insert(UInt64(event_type));
    columns[i++]->insert(UInt64(DateLUT::instance().toDayNum(event_time)));
    columns[i++]->insert(UInt64(event_time));
    columns[i++]->insert(UInt64(duration_ms));

    columns[i++]->insert(cnch_database);
    columns[i++]->insert(cnch_table);
    columns[i++]->insert(database);
    columns[i++]->insert(table);
    columns[i++]->insert(consumer);

    columns[i++]->insert(UInt64(metric));
    columns[i++]->insert(UInt64(bytes));

    columns[i++]->insert(UInt64(has_error));
    columns[i++]->insert(last_exception);
}

} // end of namespace DB
