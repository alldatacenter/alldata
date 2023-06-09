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

#include <Common/SettingsChanges.h>
#include <IO/WriteHelpers.h>
#include <IO/ReadHelpers.h>

namespace DB
{
namespace
{
    SettingChange * find(SettingsChanges & changes, const std::string_view & name)
    {
        auto it = std::find_if(changes.begin(), changes.end(), [&name](const SettingChange & change) { return change.name == name; });
        if (it == changes.end())
            return nullptr;
        return &*it;
    }

    const SettingChange * find(const SettingsChanges & changes, const std::string_view & name)
    {
        auto it = std::find_if(changes.begin(), changes.end(), [&name](const SettingChange & change) { return change.name == name; });
        if (it == changes.end())
            return nullptr;
        return &*it;
    }
}

void SettingChange::serialize(WriteBuffer & buf) const
{
    writeBinary(name, buf);
    writeFieldBinary(value, buf);
}

void SettingChange::deserialize(ReadBuffer & buf)
{
    readBinary(name, buf);
    readFieldBinary(value, buf);
}

bool SettingsChanges::tryGet(const std::string_view & name, Field & out_value) const
{
    const auto * change = find(*this, name);
    if (!change)
        return false;
    out_value = change->value;
    return true;
}

const Field * SettingsChanges::tryGet(const std::string_view & name) const
{
    const auto * change = find(*this, name);
    if (!change)
        return nullptr;
    return &change->value;
}

Field * SettingsChanges::tryGet(const std::string_view & name)
{
    auto * change = find(*this, name);
    if (!change)
        return nullptr;
    return &change->value;
}

void SettingsChanges::serialize(WriteBuffer & buf) const
{
    writeBinary(size(), buf);
    for (auto & change : *this)
        change.serialize(buf);
}

void SettingsChanges::deserialize(ReadBuffer & buf)
{
    size_t size;
    readBinary(size, buf);
    for (size_t i = 0; i < size; ++i)
    {
        SettingChange change;
        change.deserialize(buf);
        this->push_back(change);
    }
}

}
