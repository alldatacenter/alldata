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

#include <Core/NamesAndTypes.h>
#include <DataTypes/DataTypeByteMap.h>
#include <DataTypes/DataTypeFactory.h>
#include <DataTypes/DataTypeHelper.h>
#include <DataTypes/DataTypesNumber.h>
#include <DataTypes/MapHelpers.h>
#include <IO/ReadBuffer.h>
#include <IO/WriteBuffer.h>
#include <IO/ReadHelpers.h>
#include <IO/WriteHelpers.h>
#include <IO/ReadBufferFromString.h>
#include <IO/WriteBufferFromString.h>
#include <sparsehash/dense_hash_map>


namespace DB
{

namespace ErrorCodes
{
    extern const int THERE_IS_NO_COLUMN;
}

NameAndTypePair::NameAndTypePair(
    const String & name_in_storage_, const String & subcolumn_name_,
    const DataTypePtr & type_in_storage_, const DataTypePtr & subcolumn_type_)
    : name(name_in_storage_ + (subcolumn_name_.empty() ? "" : "." + subcolumn_name_))
    , type(subcolumn_type_)
    , type_in_storage(type_in_storage_)
    , subcolumn_delimiter_position(subcolumn_name_.empty() ? std::nullopt : std::make_optional(name_in_storage_.size()))
{
}

String NameAndTypePair::getNameInStorage() const
{
    if (!subcolumn_delimiter_position)
        return name;

    return name.substr(0, *subcolumn_delimiter_position);
}

String NameAndTypePair::getSubcolumnName() const
{
    if (!subcolumn_delimiter_position)
        return "";

    return name.substr(*subcolumn_delimiter_position + 1, name.size() - *subcolumn_delimiter_position);
}

void NameAndTypePair::serialize(WriteBuffer & buf) const
{
    writeBinary(name, buf);
    serializeDataType(type, buf);
    serializeDataType(type_in_storage, buf);
    if (subcolumn_delimiter_position)
    {
        writeBinary(true, buf);
        writeBinary(subcolumn_delimiter_position.value(), buf);
    }
    else
        writeBinary(false, buf);
}

void NameAndTypePair::deserialize(ReadBuffer & buf)
{
    readBinary(name, buf);
    type = deserializeDataType(buf);
    type_in_storage = deserializeDataType(buf);

    bool has_size;
    readBinary(has_size, buf);
    if (has_size)
    {
        size_t subcolumn_tmp;
        readBinary(subcolumn_tmp, buf);
        subcolumn_delimiter_position = subcolumn_tmp;
    }
}

void NamesAndTypesList::readText(ReadBuffer & buf)
{
    const DataTypeFactory & data_type_factory = DataTypeFactory::instance();

    assertString("columns format version: 1\n", buf);
    size_t count;
    DB::readText(count, buf);
    assertString(" columns:\n", buf);
    resize(count);

    for (NameAndTypePair & it : *this)
    {
        readBackQuotedStringWithSQLStyle(it.name, buf);
        assertChar(' ', buf);
        String type_name;
        readString(type_name, buf);
        it.type = data_type_factory.get(type_name);

        if (*buf.position() == '\n')
        {
            assertChar('\n', buf);
            continue;
        }

        assertChar('\t', buf);
        // optional settings
        String options;
        readString(options, buf);

        while (!options.empty())
        {
            if (options == "KV")
            {
                it.type->setFlags(TYPE_MAP_KV_STORE_FLAG);
            }
            else if(options == "COMPRESSION")
            {
                it.type->setFlags(TYPE_COMPRESSION_FLAG);
            }
            else
            {
                // TBD: ignore for now, or throw exception
            }

            if (*buf.position() == '\n')
                break;

            assertChar('\t', buf);
            readString(options, buf);
        }

        assertChar('\n', buf);
    }
}

void NamesAndTypesList::writeText(WriteBuffer & buf) const
{
    writeString("columns format version: 1\n", buf);
    DB::writeText(size(), buf);
    writeString(" columns:\n", buf);
    for (const auto & it : *this)
    {
        writeBackQuotedString(it.name, buf);
        writeChar(' ', buf);
        writeString(it.type->getName(), buf);

        UInt8 flag = it.type->getFlags();

        while (flag)
        {
            if (flag & TYPE_COMPRESSION_FLAG)
            {
                writeChar('\t', buf);
                writeString("COMPRESSION", buf);
                flag ^= TYPE_COMPRESSION_FLAG;
            }
            else if (flag & TYPE_MAP_KV_STORE_FLAG)
            {
                writeChar('\t', buf);
                writeString("KV", buf);
                flag ^= TYPE_MAP_KV_STORE_FLAG;
            }
            else
                break;
        }

        writeChar('\n', buf);
    }
}

String NamesAndTypesList::toString() const
{
    WriteBufferFromOwnString out;
    writeText(out);
    return out.str();
}

NamesAndTypesList NamesAndTypesList::parse(const String & s)
{
    ReadBufferFromString in(s);
    NamesAndTypesList res;
    res.readText(in);
    assertEOF(in);
    return res;
}

bool NamesAndTypesList::isSubsetOf(const NamesAndTypesList & rhs) const
{
    NamesAndTypes vector(rhs.begin(), rhs.end());
    vector.insert(vector.end(), begin(), end());
    std::sort(vector.begin(), vector.end());
    return std::unique(vector.begin(), vector.end()) == vector.begin() + rhs.size();
}

size_t NamesAndTypesList::sizeOfDifference(const NamesAndTypesList & rhs) const
{
    NamesAndTypes vector(rhs.begin(), rhs.end());
    vector.insert(vector.end(), begin(), end());
    std::sort(vector.begin(), vector.end());
    return (std::unique(vector.begin(), vector.end()) - vector.begin()) * 2 - size() - rhs.size();
}

bool NamesAndTypesList::isCompatableWithKeyColumns(const NamesAndTypesList & rhs, const Names & keys_columns)
{
    if (keys_columns.size() > 1)
    {
        NameSet keys(keys_columns.begin(), keys_columns.end());
        NamesAndTypes k1, k2;
        for (const NameAndTypePair & column : *this)
        {
            if (keys.count(column.name))
                k1.push_back(column);
        }
        for (const NameAndTypePair & column : rhs)
        {
            if (keys.count(column.name))
                k2.push_back(column);
        }

        if (k1 != k2)
            return false;
    }

    return isSubsetOf(rhs) || rhs.isSubsetOf(*this);
}

void NamesAndTypesList::getDifference(const NamesAndTypesList & rhs, NamesAndTypesList & deleted, NamesAndTypesList & added) const
{
    NamesAndTypes lhs_vector(begin(), end());
    std::sort(lhs_vector.begin(), lhs_vector.end());
    NamesAndTypes rhs_vector(rhs.begin(), rhs.end());
    std::sort(rhs_vector.begin(), rhs_vector.end());

    std::set_difference(lhs_vector.begin(), lhs_vector.end(), rhs_vector.begin(), rhs_vector.end(),
        std::back_inserter(deleted));
    std::set_difference(rhs_vector.begin(), rhs_vector.end(), lhs_vector.begin(), lhs_vector.end(),
        std::back_inserter(added));
}

Names NamesAndTypesList::getNames() const
{
    Names res;
    res.reserve(size());
    for (const NameAndTypePair & column : *this)
        res.push_back(column.name);
    return res;
}

DataTypes NamesAndTypesList::getTypes() const
{
    DataTypes res;
    res.reserve(size());
    for (const NameAndTypePair & column : *this)
        res.push_back(column.type);
    return res;
}

NamesAndTypesList NamesAndTypesList::filter(const NameSet & names) const
{
    NamesAndTypesList res;
    for (const NameAndTypePair & column : *this)
    {
        if (names.count(column.name))
            res.push_back(column);
    }
    return res;
}

NamesAndTypesList NamesAndTypesList::filter(const Names & names) const
{
    return filter(NameSet(names.begin(), names.end()));
}

NamesAndTypesList NamesAndTypesList::addTypes(const Names & names) const
{
    /// NOTE: It's better to make a map in `IStorage` than to create it here every time again.
#if !defined(ARCADIA_BUILD)
    google::dense_hash_map<StringRef, const DataTypePtr *, StringRefHash> types;
#else
    google::sparsehash::dense_hash_map<StringRef, const DataTypePtr *, StringRefHash> types;
#endif
    types.set_empty_key(StringRef());

    for (const auto & column : *this)
        types[column.name] = &column.type;

    NamesAndTypesList res;
    for (const String & name : names)
    {
        if (isMapImplicitKeyNotKV(name))
        {
            String map_name = parseMapNameFromImplicitColName(name);
            auto it = types.find(map_name);
            if (it == types.end())
                throw Exception(ErrorCodes::THERE_IS_NO_COLUMN, "No column {} when handing implicit column {}", map_name, name);
            if (!(*it->second)->isMap() || (*it->second)->isMapKVStore())
                throw Exception(
                    ErrorCodes::BAD_ARGUMENTS,
                    "Data type {} of column {} is not MapNotKV as expected when handling implicit column {}.",
                    (*it->second)->getName(),
                    map_name,
                    name);
            res.emplace_back(name, typeid_cast<const DataTypeByteMap &>(*(*it->second)).getValueTypeForImplicitColumn());
        }
        else if (isMapKV(name))
        {
            String map_name = parseMapNameFromImplicitKVName(name);
            auto it = types.find(map_name);
            if (it == types.end())
                throw Exception(ErrorCodes::THERE_IS_NO_COLUMN, "No column {} when handling implicit kv column {}", map_name, name);
            if (!(*it->second)->isMap() || !(*it->second)->isMapKVStore())
                throw Exception(
                    ErrorCodes::BAD_ARGUMENTS,
                    "Data type {} of column {} is not MapKV as expected when handing implicit kv column {}.",
                    (*it->second)->getName(),
                    map_name,
                    name);
            res.emplace_back(name, typeid_cast<const DataTypeByteMap &>(*(*it->second)).getMapStoreType(name));
        }
        else
        {
            auto it = types.find(name);
            if (it == types.end())
            {
                // if (endsWith(name, COMPRESSION_COLUMN_EXTENSION))
                //     res.emplace_back(name, std::make_shared<DataTypeUInt16>());
                // else
                throw Exception("No column " + name, ErrorCodes::THERE_IS_NO_COLUMN);
            }

            res.emplace_back(name, *it->second);
        }
    }

    return res;
}

bool NamesAndTypesList::contains(const String & name) const
{
    for (const NameAndTypePair & column : *this)
    {
        if (column.name == name)
            return true;
    }
    return false;
}

std::optional<NameAndTypePair> NamesAndTypesList::tryGetByName(const std::string & name) const
{
    for (const NameAndTypePair & column : *this)
    {
        if (column.name == name)
            return column;
    }
    return {};
}

size_t NamesAndTypesList::getPosByName(const std::string &name) const noexcept
{
    size_t pos = 0;
    for (const NameAndTypePair & column : *this)
    {
        if (column.name == name)
            break;
        ++pos;
    }
    return pos;
}

void NamesAndTypesList::serialize(WriteBuffer & buf) const
{
    writeBinary(size(), buf);
    for (auto & elem : *this)
        elem.serialize(buf);
}

void NamesAndTypesList::deserialize(ReadBuffer & buf)
{
    size_t size;
    readBinary(size, buf);
    for (size_t i = 0; i < size; ++i)
    {
        NameAndTypePair pair;
        pair.deserialize(buf);
        this->push_back(pair);
    }
}

}
