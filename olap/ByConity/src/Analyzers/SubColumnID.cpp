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

#include <Analyzers/SubColumnID.h>
#include <Common/Exception.h>
#include <DataTypes/MapHelpers.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int NOT_IMPLEMENTED;
}

String SubColumnID::getSubColumnName(const String & primary_column) const
{
    switch (type)
    {
        case Type::MAP_ELEMENT:
            return getImplicitColNameForMapKey(primary_column, map_element_key);
        case Type::MAP_KEYS:
            return primary_column + ".key";
    }
    throw Exception("Not implemented for this type.", ErrorCodes::NOT_IMPLEMENTED);
}

bool SubColumnID::operator==(const SubColumnID & other) const
{
    if (type != other.type)
        return false;

    if (type == Type::MAP_ELEMENT)
    {
        return map_element_key == other.map_element_key;
    }

    return true;
}

size_t SubColumnID::Hash::operator()(const SubColumnID & id) const
{
    size_t hash_for_type = static_cast<typename std::underlying_type<Type>::type>(id.type);
    size_t hash_for_other_members = 0;

    if (id.type == Type::MAP_ELEMENT)
    {
        hash_for_other_members = std::hash<String>()(id.map_element_key);
    }

    return hash_for_type | ((hash_for_other_members >> 8) << 8);
}

}
