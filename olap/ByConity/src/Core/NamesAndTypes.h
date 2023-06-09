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

#include <map>
#include <list>
#include <optional>
#include <string>
#include <set>
#include <initializer_list>

#include <DataTypes/IDataType.h>
#include <Core/Names.h>

#include <Storages/MergeTree/MergeTreeSuffix.h>

namespace DB
{

struct NameAndTypePair
{
public:
    NameAndTypePair() = default;
    NameAndTypePair(const String & name_, const DataTypePtr & type_)
        : name(name_), type(type_), type_in_storage(type_) {}

    NameAndTypePair(const String & name_in_storage_, const String & subcolumn_name_,
        const DataTypePtr & type_in_storage_, const DataTypePtr & subcolumn_type_);

    String getNameInStorage() const;
    String getSubcolumnName() const;

    bool isSubcolumn() const { return subcolumn_delimiter_position != std::nullopt; }
    DataTypePtr getTypeInStorage() const { return type_in_storage; }

    bool operator<(const NameAndTypePair & rhs) const
    {
        return std::forward_as_tuple(name, type->getName()) < std::forward_as_tuple(rhs.name, rhs.type->getName());
    }

    bool operator==(const NameAndTypePair & rhs) const
    {
        return name == rhs.name && type->equals(*rhs.type);
    }

    String name;
    DataTypePtr type;

    void serialize(WriteBuffer & buf) const;
    void deserialize(ReadBuffer & buf);

private:
    DataTypePtr type_in_storage;
    std::optional<size_t> subcolumn_delimiter_position;
};

/// This needed to use structured bindings for NameAndTypePair
/// const auto & [name, type] = name_and_type
template <int I>
decltype(auto) get(const NameAndTypePair & name_and_type)
{
    if constexpr (I == 0)
        return name_and_type.name;
    else if constexpr (I == 1)
        return name_and_type.type;
}

using NamesAndTypes = std::vector<NameAndTypePair>;

class NamesAndTypesList : public std::list<NameAndTypePair>
{
public:
    NamesAndTypesList() = default;

    NamesAndTypesList(std::initializer_list<NameAndTypePair> init) : std::list<NameAndTypePair>(init) {}

    template <typename Iterator>
    NamesAndTypesList(Iterator begin, Iterator end) : std::list<NameAndTypePair>(begin, end) {}


    void readText(ReadBuffer & buf);
    void writeText(WriteBuffer & buf) const;

    String toString() const;
    static NamesAndTypesList parse(const String & s);

    /// All `rhs` elements must be different.
    bool isSubsetOf(const NamesAndTypesList & rhs) const;

    /// Hamming distance between sets
    ///  (in other words, the added and deleted columns are counted once, the columns that changed the type - twice).
    size_t sizeOfDifference(const NamesAndTypesList & rhs) const;

    /// Check if columns are compatable. If return true, the data with such columns counld be shared. Eg: table with
    /// current columns can attach parts with columns 'rhs', and vice versas.  if there are more than one keys_columns,
    /// they should appear with the same order in this two column lists.
    bool isCompatableWithKeyColumns(const NamesAndTypesList & rhs, const Names & keys_columns);

    /// If an element changes type, it is present both in deleted (with the old type) and in added (with the new type).
    void getDifference(const NamesAndTypesList & rhs, NamesAndTypesList & deleted, NamesAndTypesList & added) const;

    Names getNames() const;
    DataTypes getTypes() const;

    /// Leave only the columns whose names are in the `names`. In `names` there can be superfluous columns.
    NamesAndTypesList filter(const NameSet & names) const;

    /// Leave only the columns whose names are in the `names`. In `names` there can be superfluous columns.
    NamesAndTypesList filter(const Names & names) const;

    /// Unlike `filter`, returns columns in the order in which they go in `names`.
    NamesAndTypesList addTypes(const Names & names) const;

    /// Check that column contains in list
    bool contains(const String & name) const;

    /// Try to get column by name, return empty optional if column not found
    std::optional<NameAndTypePair> tryGetByName(const std::string & name) const;

    /// Try to get column position by name, returns number of columns if column isn't found
    size_t getPosByName(const std::string & name) const noexcept;

    void serialize(WriteBuffer & buf) const;
    void deserialize(ReadBuffer & buf);
};

using NamesAndTypesListPtr = std::shared_ptr<NamesAndTypesList>;
using NamesAndTypesLists = std::vector<NamesAndTypesList>;
}

namespace std
{
    template <> struct tuple_size<DB::NameAndTypePair> : std::integral_constant<size_t, 2> {};
    template <> struct tuple_element<0, DB::NameAndTypePair> { using type = DB::String; };
    template <> struct tuple_element<1, DB::NameAndTypePair> { using type = DB::DataTypePtr; };
}
