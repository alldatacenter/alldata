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

#include <Core/SortDescription.h>
#include <Core/Block.h>
#include <IO/Operators.h>
#include <IO/ReadHelpers.h>
#include <IO/WriteHelpers.h>
#include <Common/JSONBuilder.h>
#include <Columns/Collator.h>

namespace DB
{

void FillColumnDescription::serialize(WriteBuffer & buffer) const
{
    writeFieldBinary(fill_from, buffer);
    writeFieldBinary(fill_to, buffer);
    writeFieldBinary(fill_step, buffer);
}

void FillColumnDescription::deserialize(ReadBuffer & buffer)
{
    readFieldBinary(fill_from, buffer);
    readFieldBinary(fill_to, buffer);
    readFieldBinary(fill_step, buffer);
}

void SortColumnDescription::serialize(WriteBuffer & buffer) const
{
    writeBinary(column_name, buffer);
    writeBinary(column_number, buffer);
    writeBinary(direction, buffer);
    writeBinary(nulls_direction, buffer);

    if (!collator)
        writeBinary(false, buffer);
    else
    {
        writeBinary(true, buffer);
        writeBinary(collator->getLocale(), buffer);
    }
    writeBinary(with_fill, buffer);
    fill_description.serialize(buffer);
}

void SortColumnDescription::deserialize(ReadBuffer & buffer)
{
    readBinary(column_name, buffer);
    readBinary(column_number, buffer);
    readBinary(direction, buffer);
    readBinary(nulls_direction, buffer);

    bool has_collator = false;
    readBinary(has_collator, buffer);
    if (has_collator)
    {
        String locale;
        readBinary(locale, buffer);

        collator = std::make_shared<Collator>(locale);
    }
    readBinary(with_fill, buffer);
    fill_description.deserialize(buffer);
}

void dumpSortDescription(const SortDescription & description, const Block & header, WriteBuffer & out)
{
    bool first = true;

    for (const auto & desc : description)
    {
        if (!first)
            out << ", ";
        first = false;

        if (!desc.column_name.empty())
            out << desc.column_name;
        else
        {
            if (desc.column_number < header.columns())
                out << header.getByPosition(desc.column_number).name;
            else
                out << "?";

            out << " (pos " << desc.column_number << ")";
        }

        if (desc.direction > 0)
            out << " ASC";
        else
            out << " DESC";

        if (desc.with_fill)
            out << " WITH FILL";
    }
}

void SortColumnDescription::explain(JSONBuilder::JSONMap & map, const Block & header) const
{
    if (!column_name.empty())
        map.add("Column", column_name);
    else
    {
        if (column_number < header.columns())
            map.add("Column", header.getByPosition(column_number).name);

        map.add("Position", column_number);
    }

    map.add("Ascending", direction > 0);
    map.add("With Fill", with_fill);
}

std::string dumpSortDescription(const SortDescription & description)
{
    WriteBufferFromOwnString wb;
    dumpSortDescription(description, Block{}, wb);
    return wb.str();
}

JSONBuilder::ItemPtr explainSortDescription(const SortDescription & description, const Block & header)
{
    auto json_array = std::make_unique<JSONBuilder::JSONArray>();
    for (const auto & descr : description)
    {
        auto json_map = std::make_unique<JSONBuilder::JSONMap>();
        descr.explain(*json_map, header);
        json_array->add(std::move(json_map));
    }

    return json_array;
}

void serializeSortDescription(const SortDescription & sort_descriptions, WriteBuffer & buffer)
{
    writeBinary(sort_descriptions.size(), buffer);
    for (const auto & sort_description : sort_descriptions)
        sort_description.serialize(buffer);
}

void deserializeSortDescription(SortDescription & sort_descriptions, ReadBuffer & buffer)
{
    size_t sort_size;
    readBinary(sort_size, buffer);
    sort_descriptions.resize(sort_size);
    for (size_t i = 0; i < sort_size; ++i)
        sort_descriptions[i].deserialize(buffer);
}

}
