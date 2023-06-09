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

#include <DataTypes/MapHelpers.h>

#include <cctype>
#include <cstring>
#include <Storages/MergeTree/MergeTreeSuffix.h>
#include <Common/Exception.h>
#include <Common/StringUtils/StringUtils.h>
#include <Common/escapeForFileName.h>
#include <Core/Field.h>
#include <DataTypes/IDataType.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int INVALID_IMPLICIT_COLUMN_NAME;
    extern const int INVALID_IMPLICIT_COLUMN_FILE_NAME;
    extern const int INVALID_MAP_SEPARATOR;
}

static String map_separator = "__";

const String & getMapSeparator()
{
    return map_separator;
}

void checkAndSetMapSeparator(const String & map_separator_)
{
    const char * pos = map_separator_.data();
    const char * end = map_separator_.data() + map_separator_.size();
    while (pos != end)
    {
        unsigned char c = *pos;
        if (!isWordCharASCII(c))
            throw Exception(
                ErrorCodes::INVALID_MAP_SEPARATOR,
                "Map separator {} contains invalid char {}, which can only contain [0-9] | [a-z] | [A-Z] | _",
                map_separator_,
                c);
        ++pos;
    }

    map_separator = map_separator_;
}

std::string_view ExtractMapColumn::apply(std::string_view src)
{
    if (src.size() < std::string(getMapSeparator() + "M" + getMapSeparator() + "1.bin").size()) /// minimum example
        return {};

    bool success = true; // check start with getMapSeparator()
    for (size_t i = 0; i < getMapSeparator().size(); ++i)
    {
        if (src[i] != getMapSeparator().at(i))
        {
            success = false;
            break;
        }
    }
    if (!success)
        return {};

    size_t column_end = getMapSeparator().size(); /// start with getMapSeparator()
    while (column_end + getMapSeparator().size() - 1 < src.size())
    {
        success = true;
        for (size_t i = 0; i < getMapSeparator().size(); ++i)
        {
            if (src[column_end + i] != getMapSeparator().at(i))
            {
                success = false;
                break;
            }
        }
        if (!success)
            break;
        ++column_end;
    }

    if (column_end + getMapSeparator().size() - 1 == src.size()) /// not found
        return {};
    /// Just ignore chars after getMapSeparator()

    return std::string_view(src.data() + getMapSeparator().size(), column_end - getMapSeparator().size());
}

std::string_view ExtractMapKey::apply(std::string_view src, std::string_view * out_column)
{
    auto column = ExtractMapColumn::apply(src);
    if (column.size() == 0)
        return {};

    if (out_column)
        *out_column = column;

    const size_t column_part_size = column.size() + getMapSeparator().size() * 2;
    size_t key_end = column_part_size;
    if (src[key_end] == '\'') /// begin with ' for Map(String, T)
    {
        key_end += 1;
        while (key_end + 1 < src.size() && !(src[key_end] == '\'' && src[key_end + 1] == '.')) /// end with '.bin or '.mrk
            ++key_end;

        if (key_end + 1 == src.size()) /// not found
            return {};
        /// Just ignore chars after '.

        return std::string_view(src.data() + column_part_size + 1, key_end - (column_part_size + 1));
    }
    else if (std::isdigit(src[key_end])) /// begin with digit for Map(Int*/Float*, T)
    {
        while (key_end + 1 < src.size() && !(src[key_end] == '.' && std::islower(src[key_end + 1]))) /// end with .bin or .mrk
            ++key_end;

        if (key_end + 1 == src.size()) /// not found
            return {};
        /// Just ignore chars after '.

        return std::string_view(src.data() + column_part_size, key_end - column_part_size);
    }
    else
        return {};
}

String genMapKeyFilePrefix(const String & column)
{
    return escapeForFileName(getMapKeyPrefix(column));
}

String genMapBaseFilePrefix(const String & column)
{
    return escapeForFileName(getBaseNameForMapCol(column)) + '.';
}

String getImplicitColNameForMapKey(const String & map_col, const String & key_name)
{
    return String(getMapSeparator() + map_col + getMapSeparator() + key_name);
}

String getBaseNameForMapCol(const String & map_col)
{
    return String(getMapSeparator() + map_col + "_base");
}

String getMapKeyPrefix(const String & column)
{
    return getMapSeparator() + column + getMapSeparator();
}

String parseMapNameFromImplicitFileName(const String & implicit_file_name)
{
    String unescape_file_name = unescapeForFileName(implicit_file_name);
    if (!startsWith(unescape_file_name, getMapSeparator()))
        throw Exception(ErrorCodes::INVALID_IMPLICIT_COLUMN_NAME, "Invalid implicit file name: {}", implicit_file_name);

    /// Due to map column name meets the constraint in MergeTreeData::checkColumnsValidity, so we can find second separator directly.
    auto location = unescape_file_name.find(getMapSeparator(), getMapSeparator().size());
    if (location == String::npos)
        throw Exception(ErrorCodes::INVALID_IMPLICIT_COLUMN_NAME, "Invalid implicit column name: {}", implicit_file_name);
    return unescape_file_name.substr(getMapSeparator().size(), location - getMapSeparator().size());
}

String parseKeyNameFromImplicitFileName(const String & implicit_file_name, const String & map_col)
{
    String prefix = getMapKeyPrefix(map_col);
    String unescape_file_name = unescapeForFileName(implicit_file_name);
    if (!startsWith(unescape_file_name, prefix))
        throw Exception(ErrorCodes::INVALID_IMPLICIT_COLUMN_FILE_NAME, "Implicit file name {} is not belong to map column {} ", implicit_file_name, map_col);

    auto extension_loc = implicit_file_name.find('.'); /// only extension contain dot, other dots in column name are escaped.
    if (extension_loc == String::npos)
        throw Exception(ErrorCodes::INVALID_IMPLICIT_COLUMN_FILE_NAME, "Invalid file name of implicit column: {]", implicit_file_name);
    size_t extension_size = implicit_file_name.size() - extension_loc;

    return unescape_file_name.substr(prefix.size(), unescape_file_name.size() - prefix.size() - extension_size);
}

String parseMapNameFromImplicitColName(const String & implicit_column_name)
{
    if (!startsWith(implicit_column_name, getMapSeparator()))
        throw Exception(ErrorCodes::INVALID_IMPLICIT_COLUMN_NAME, "Invalid implicit column name: {}", implicit_column_name);

    /// Due to map column name meets the constraint in MergeTreeData::checkColumnsValidity, so we can find second separator directly.
    auto location = implicit_column_name.find(getMapSeparator(), getMapSeparator().size());
    if (location == String::npos)
        throw Exception(ErrorCodes::INVALID_IMPLICIT_COLUMN_NAME, "Invalid implicit column name: {}", implicit_column_name);
    return implicit_column_name.substr(getMapSeparator().size(), location - getMapSeparator().size());
}

String parseKeyNameFromImplicitColName(const String & implicit_col, const String & map_col)
{
    String prefix = getMapKeyPrefix(map_col);
    if (!startsWith(implicit_col, prefix))
        throw Exception(ErrorCodes::INVALID_IMPLICIT_COLUMN_NAME, "Invalid implicit column {} when parsing key", implicit_col);
    return implicit_col.substr(prefix.size(), implicit_col.size() - prefix.size());
}

String parseMapNameFromImplicitKVName(const String & implicit_col)
{
    if (endsWith(implicit_col, ".key"))
        return implicit_col.substr(0, implicit_col.size() - 4);
    else if (endsWith(implicit_col, ".value"))
        return implicit_col.substr(0, implicit_col.size() - 6);
    else
        throw Exception(ErrorCodes::INVALID_IMPLICIT_COLUMN_NAME, "Invalid implciti kv name {} when parsing map name", implicit_col);
}

String getMapFileNameFromImplicitFileName(const String & implicit_file_name)
{
    String map_name = parseMapNameFromImplicitFileName(implicit_file_name);
    auto extension_loc = implicit_file_name.find('.'); /// only extension contain dot, other dots in column name are escaped.
    if (extension_loc == String::npos)
        throw Exception(ErrorCodes::INVALID_IMPLICIT_COLUMN_FILE_NAME, "Invalid file name of implicit column: {}", implicit_file_name);
    return escapeForFileName(map_name) + implicit_file_name.substr(extension_loc, implicit_file_name.size() - extension_loc);
}

bool isMapBaseFile(const String & file_name)
{
    return startsWith(file_name, getMapSeparator()) && file_name.find("_base.") != std::string::npos;
}

bool isMapImplicitKey(const String & map_col)
{
    return startsWith(map_col, getMapSeparator()) || endsWith(map_col, ".key") || endsWith(map_col, ".value");
}

bool isMapImplicitKeyOfSpecialMapName(const String & implicit_col, const String & map_col)
{
    return startsWith(implicit_col, getMapKeyPrefix(map_col));
}

bool isMapImplicitKeyNotKV(const String & map_col)
{
    return startsWith(map_col, getMapSeparator());
}

bool isMapKV(const String & map_col)
{
    return endsWith(map_col, ".key") || endsWith(map_col, ".value");
}

bool isMapKVOfSpecialMapName(const String & implicit_col, const String & map_col)
{
    return implicit_col == map_col + ".key" || implicit_col == map_col + ".value";
}

bool isMapImplicitDataFileNameNotBaseOfSpecialMapName(const String file_name, const String map_col)
{
    String escape_prefix = genMapKeyFilePrefix(map_col);
    if (!startsWith(file_name, escape_prefix))
        return false;

    auto extension_loc = file_name.find('.'); /// only extension contain dot, other dots in column name are escaped.
    if (extension_loc == String::npos)
        throw Exception(ErrorCodes::INVALID_IMPLICIT_COLUMN_FILE_NAME, "Invalid implicit column file name {}", file_name);

    return file_name.substr(extension_loc) == DATA_FILE_EXTENSION;
}

bool isMapImplicitFileNameOfSpecialMapName(const String file_name, const String map_col)
{
    if (isMapBaseFile(file_name))
        return startsWith(file_name, genMapBaseFilePrefix(map_col));
    else
        return startsWith(file_name, genMapKeyFilePrefix(map_col));
}

bool isMapCompactFileNameOfSpecialMapName(const String file_name, const String map_col)
{
    auto extension_loc = file_name.find('.');
    if (extension_loc == String::npos)
        throw Exception(ErrorCodes::INVALID_IMPLICIT_COLUMN_FILE_NAME, "Invalid implicit column file name {}", file_name);
    return file_name.substr(0, extension_loc) == escapeForFileName(map_col);
}

}
