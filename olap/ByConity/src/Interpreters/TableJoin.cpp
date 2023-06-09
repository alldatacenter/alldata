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

#include <Interpreters/TableJoin.h>
#include <Interpreters/Context.h>
#include <Interpreters/JoinedTables.h>

#include <common/logger_useful.h>

#include <Parsers/ASTExpressionList.h>
#include <Parsers/ASTSelectQuery.h>
#include <Parsers/ASTSerDerHelper.h>
#include <Parsers/queryToString.h>

#include <Core/Settings.h>
#include <Core/Block.h>
#include <Core/ColumnsWithTypeAndName.h>

#include <Common/StringUtils/StringUtils.h>

#include <DataTypes/DataTypeNullable.h>
#include <DataTypes/DataTypeHelper.h>
#include <DataStreams/materializeBlock.h>
#include <QueryPlan/PlanSerDerHelper.h>


namespace DB
{

namespace ErrorCodes
{
    extern const int TYPE_MISMATCH;
}

TableJoin::TableJoin(const Settings & settings, VolumePtr tmp_volume_)
    : size_limits(SizeLimits{settings.max_rows_in_join, settings.max_bytes_in_join, settings.join_overflow_mode})
    , default_max_bytes(settings.default_max_bytes_in_join)
    , join_use_nulls(settings.join_use_nulls)
    , max_joined_block_rows(settings.max_joined_block_size_rows)
    , join_algorithm(settings.join_algorithm)
    , partial_merge_join_optimizations(settings.partial_merge_join_optimizations)
    , partial_merge_join_rows_in_right_blocks(settings.partial_merge_join_rows_in_right_blocks)
    , partial_merge_join_left_table_buffer_bytes(settings.partial_merge_join_left_table_buffer_bytes)
    , max_files_to_merge(settings.join_on_disk_max_files_to_merge)
    , temporary_files_codec(settings.temporary_files_codec)
    , allow_extended_conversion(settings.allow_extended_type_conversion)
    , tmp_volume(tmp_volume_)
{
}

void TableJoin::resetCollected()
{
    key_names_left.clear();
    key_names_right.clear();
    key_asts_left.clear();
    key_asts_right.clear();
    columns_from_joined_table.clear();
    columns_added_by_join.clear();
    original_names.clear();
    renames.clear();
    left_type_map.clear();
    right_type_map.clear();
    left_converting_actions = nullptr;
    right_converting_actions = nullptr;
}

void TableJoin::addUsingKey(const ASTPtr & ast)
{
    key_names_left.push_back(ast->getColumnName());
    key_names_right.push_back(ast->getAliasOrColumnName());

    key_asts_left.push_back(ast);
    key_asts_right.push_back(ast);

    auto & right_key = key_names_right.back();
    if (renames.count(right_key))
        right_key = renames[right_key];
}

void TableJoin::addOnKeys(ASTPtr & left_table_ast, ASTPtr & right_table_ast)
{
    key_names_left.push_back(left_table_ast->getColumnName());
    key_names_right.push_back(right_table_ast->getAliasOrColumnName());

    key_asts_left.push_back(left_table_ast);
    key_asts_right.push_back(right_table_ast);
}

/// @return how many times right key appears in ON section.
size_t TableJoin::rightKeyInclusion(const String & name) const
{
    if (hasUsing())
        return 0;

    size_t count = 0;
    for (const auto & key_name : key_names_right)
        if (name == key_name)
            ++count;
    return count;
}

void TableJoin::deduplicateAndQualifyColumnNames(const NameSet & left_table_columns, const String & right_table_prefix)
{
    NameSet joined_columns;
    NamesAndTypesList dedup_columns;

    for (auto & column : columns_from_joined_table)
    {
        if (joined_columns.count(column.name))
            continue;

        joined_columns.insert(column.name);

        dedup_columns.push_back(column);
        auto & inserted = dedup_columns.back();

        /// Also qualify unusual column names - that does not look like identifiers.

        if (left_table_columns.count(column.name) || !isValidIdentifierBegin(column.name.at(0)))
            inserted.name = right_table_prefix + column.name;

        original_names[inserted.name] = column.name;
        if (inserted.name != column.name)
            renames[column.name] = inserted.name;
    }

    columns_from_joined_table.swap(dedup_columns);
}

NamesWithAliases TableJoin::getNamesWithAliases(const NameSet & required_columns) const
{
    NamesWithAliases out;
    for (const auto & column : required_columns)
    {
        auto it = original_names.find(column);
        if (it != original_names.end())
            out.emplace_back(it->second, it->first); /// {original_name, name}
    }
    return out;
}

ASTPtr TableJoin::leftKeysList() const
{
    ASTPtr keys_list = std::make_shared<ASTExpressionList>();
    keys_list->children = key_asts_left;
    return keys_list;
}

ASTPtr TableJoin::rightKeysList() const
{
    ASTPtr keys_list = std::make_shared<ASTExpressionList>();
    if (hasOn())
        keys_list->children = key_asts_right;
    return keys_list;
}

Names TableJoin::requiredJoinedNames() const
{
    NameSet required_columns_set(key_names_right.begin(), key_names_right.end());
    for (const auto & joined_column : columns_added_by_join)
        required_columns_set.insert(joined_column.name);

    return Names(required_columns_set.begin(), required_columns_set.end());
}

NameSet TableJoin::requiredRightKeys() const
{
    NameSet required;
    for (const auto & name : key_names_right)
    {
        auto rename = renamedRightColumnName(name);
        for (const auto & column : columns_added_by_join)
            if (rename == column.name)
                required.insert(name);
    }
    return required;
}

NamesWithAliases TableJoin::getRequiredColumns(const Block & sample, const Names & action_required_columns) const
{
    NameSet required_columns(action_required_columns.begin(), action_required_columns.end());

    for (auto & column : requiredJoinedNames())
        if (!sample.has(column))
            required_columns.insert(column);

    return getNamesWithAliases(required_columns);
}

void TableJoin::splitAdditionalColumns(const Block & sample_block, Block & block_keys, Block & block_others) const
{
    block_others = materializeBlock(sample_block);

    for (const String & column_name : key_names_right)
    {
        /// Extract right keys with correct keys order. There could be the same key names.
        if (!block_keys.has(column_name))
        {
            auto & col = block_others.getByName(column_name);
            block_keys.insert(col);
            block_others.erase(column_name);
        }
    }
}

Block TableJoin::getRequiredRightKeys(const Block & right_table_keys, std::vector<String> & keys_sources) const
{
    const Names & left_keys = keyNamesLeft();
    const Names & right_keys = keyNamesRight();
    NameSet required_keys(requiredRightKeys().begin(), requiredRightKeys().end());
    Block required_right_keys;

    for (size_t i = 0; i < right_keys.size(); ++i)
    {
        const String & right_key_name = right_keys[i];

        if (required_keys.count(right_key_name) && !required_right_keys.has(right_key_name))
        {
            const auto & right_key = right_table_keys.getByName(right_key_name);
            required_right_keys.insert(right_key);
            keys_sources.push_back(left_keys[i]);
        }
    }

    return required_right_keys;
}


bool TableJoin::leftBecomeNullable(const DataTypePtr & column_type) const
{
    return forceNullableLeft() && JoinCommon::canBecomeNullable(column_type);
}

bool TableJoin::rightBecomeNullable(const DataTypePtr & column_type) const
{
    return forceNullableRight() && JoinCommon::canBecomeNullable(column_type);
}

void TableJoin::addJoinedColumn(const NameAndTypePair & joined_column)
{
    DataTypePtr type = joined_column.type;

    if (hasUsing())
    {
        if (auto it = right_type_map.find(joined_column.name); it != right_type_map.end())
            type = it->second;
    }

    if (rightBecomeNullable(type))
        type = JoinCommon::convertTypeToNullable(type);

    columns_added_by_join.emplace_back(joined_column.name, type);
}

void TableJoin::addJoinedColumnsAndCorrectTypes(NamesAndTypesList & names_and_types, bool correct_nullability) const
{
    ColumnsWithTypeAndName columns;
    for (auto & pair : names_and_types)
        columns.emplace_back(nullptr, std::move(pair.type), std::move(pair.name));
    names_and_types.clear();

    addJoinedColumnsAndCorrectTypes(columns, correct_nullability);

    for (auto & col : columns)
        names_and_types.emplace_back(std::move(col.name), std::move(col.type));
}

void TableJoin::addJoinedColumnsAndCorrectTypes(ColumnsWithTypeAndName & columns, bool correct_nullability) const
{
    for (auto & col : columns)
    {
        if (hasUsing())
        {
            if (auto it = left_type_map.find(col.name); it != left_type_map.end())
                col.type = it->second;
        }
        if (correct_nullability && leftBecomeNullable(col.type))
        {
            /// No need to nullify constants
            bool is_column_const = col.column && isColumnConst(*col.column);
            if (!is_column_const)
                col.type = JoinCommon::convertTypeToNullable(col.type);
        }
    }

    /// Types in columns_added_by_join already converted and set nullable if needed
    for (const auto & col : columns_added_by_join)
        columns.emplace_back(nullptr, col.type, col.name);
}

bool TableJoin::sameStrictnessAndKind(ASTTableJoin::Strictness strictness_, ASTTableJoin::Kind kind_) const
{
    if (strictness_ == strictness() && kind_ == kind())
        return true;

    /// Compatibility: old ANY INNER == new SEMI LEFT
    if (strictness_ == ASTTableJoin::Strictness::Semi && isLeft(kind_) &&
        strictness() == ASTTableJoin::Strictness::RightAny && isInner(kind()))
        return true;
    if (strictness() == ASTTableJoin::Strictness::Semi && isLeft(kind()) &&
        strictness_ == ASTTableJoin::Strictness::RightAny && isInner(kind_))
        return true;

    return false;
}

bool TableJoin::allowMergeJoin() const
{
    bool is_any = (strictness() == ASTTableJoin::Strictness::Any);
    bool is_all = (strictness() == ASTTableJoin::Strictness::All);
    bool is_semi = (strictness() == ASTTableJoin::Strictness::Semi);

    bool all_join = is_all && (isInner(kind()) || isLeft(kind()) || isRight(kind()) || isFull(kind()));
    bool special_left = isLeft(kind()) && (is_any || is_semi);
    return all_join || special_left;
}

bool TableJoin::needStreamWithNonJoinedRows() const
{
    if (strictness() == ASTTableJoin::Strictness::Asof ||
        strictness() == ASTTableJoin::Strictness::Semi)
        return false;
    return isRightOrFull(kind());
}

bool TableJoin::allowDictJoin(const String & dict_key, const Block & sample_block, Names & src_names, NamesAndTypesList & dst_columns) const
{
    /// Support ALL INNER, [ANY | ALL | SEMI | ANTI] LEFT
    if (!isLeft(kind()) && !(isInner(kind()) && strictness() == ASTTableJoin::Strictness::All))
        return false;

    const Names & right_keys = keyNamesRight();
    if (right_keys.size() != 1)
        return false;

    /// TODO: support 'JOIN ... ON expr(dict_key) = table_key'
    auto it_key = original_names.find(right_keys[0]);
    if (it_key == original_names.end())
        return false;

    if (dict_key != it_key->second)
        return false; /// JOIN key != Dictionary key

    for (const auto & col : sample_block)
    {
        if (col.name == right_keys[0])
            continue; /// do not extract key column

        auto it = original_names.find(col.name);
        if (it != original_names.end())
        {
            String original = it->second;
            src_names.push_back(original);
            dst_columns.push_back({col.name, col.type});
        }
    }

    return true;
}

bool TableJoin::applyJoinKeyConvert(const ColumnsWithTypeAndName & left_sample_columns, const ColumnsWithTypeAndName & right_sample_columns)
{
    bool need_convert = needConvert();
    if (!need_convert && !hasUsing())
    {
        /// For `USING` we already inferred common type an syntax analyzer stage
        NamesAndTypesList left_list;
        NamesAndTypesList right_list;
        for (const auto & col : left_sample_columns)
            left_list.emplace_back(col.name, col.type);
        for (const auto & col : right_sample_columns)
            right_list.emplace_back(col.name, col.type);

        need_convert = inferJoinKeyCommonType(left_list, right_list);
    }

    if (need_convert)
    {
        left_converting_actions = applyKeyConvertToTable(left_sample_columns, left_type_map, key_names_left);
        right_converting_actions = applyKeyConvertToTable(right_sample_columns, right_type_map, key_names_right);
    }

    return need_convert;
}

bool TableJoin::inferJoinKeyCommonType(const NamesAndTypesList & left, const NamesAndTypesList & right)
{
    std::unordered_map<String, DataTypePtr> left_types;
    for (const auto & col : left)
    {
        left_types[col.name] = col.type;
    }

    std::unordered_map<String, DataTypePtr> right_types;
    for (const auto & col : right)
    {
        if (auto it = renames.find(col.name); it != renames.end())
            right_types[it->second] = col.type;
        else
            right_types[col.name] = col.type;
    }

    for (size_t i = 0; i < key_names_left.size(); ++i)
    {
        auto ltype = left_types.find(key_names_left[i]);
        auto rtype = right_types.find(key_names_right[i]);
        if (ltype == left_types.end() || rtype == right_types.end())
        {
            /// Name mismatch, give up
            left_type_map.clear();
            right_type_map.clear();
            return false;
        }

        if (JoinCommon::typesEqualUpToNullability(ltype->second, rtype->second))
            continue;

        DataTypePtr supertype;
        try
        {
            supertype = DB::getLeastSupertype({ltype->second, rtype->second}, allow_extended_conversion);
        }
        catch (DB::Exception & ex)
        {
            throw Exception(
                "Type mismatch of columns to JOIN by: " +
                    key_names_left[i] + ": " + ltype->second->getName() + " at left, " +
                    key_names_right[i] + ": " + rtype->second->getName() + " at right. " +
                    "Can't get supertype: " + ex.message(),
                ErrorCodes::TYPE_MISMATCH);
        }
        left_type_map[key_names_left[i]] = right_type_map[key_names_right[i]] = supertype;
    }

    if (!left_type_map.empty() || !right_type_map.empty())
    {
        auto format_type_map = [](NameToTypeMap mapping) -> std::string
        {
            std::vector<std::string> text;
            for (const auto & [k, v] : mapping)
                text.push_back(k + ": " + v->getName());
            return fmt::format("{}", fmt::join(text, ", "));
        };
        LOG_TRACE(
            &Poco::Logger::get("TableJoin"),
            "Infer supertype for joined columns. Left: [{}], Right: [{}]",
            format_type_map(left_type_map),
            format_type_map(right_type_map));
    }

    return !left_type_map.empty();
}

ActionsDAGPtr TableJoin::applyKeyConvertToTable(
    const ColumnsWithTypeAndName & cols_src, const NameToTypeMap & type_mapping, Names & names_to_rename) const
{
    ColumnsWithTypeAndName cols_dst = cols_src;
    for (auto & col : cols_dst)
    {
        if (auto it = type_mapping.find(col.name); it != type_mapping.end())
        {
            col.type = it->second;
            col.column = nullptr;
        }
    }

    NameToNameMap key_column_rename;
    /// Returns converting actions for tables that need to be performed before join
    auto dag = ActionsDAG::makeConvertingActions(
        cols_src, cols_dst, ActionsDAG::MatchColumnsMode::Name, true, !hasUsing(), &key_column_rename);

    for (auto & name : names_to_rename)
    {
        const auto it = key_column_rename.find(name);
        if (it != key_column_rename.end())
            name = it->second;
    }
    return dag;
}

String TableJoin::renamedRightColumnName(const String & name) const
{
    if (const auto it = renames.find(name); it != renames.end())
        return it->second;
    return name;
}

void TableJoin::serialize(WriteBuffer & buf) const
{
    select_query->serialize(buf);

    writeBinary(key_names_left, buf);
    writeBinary(key_names_right, buf);

    serializeASTs(key_asts_left, buf);
    serializeASTs(key_asts_right, buf);
    serializeAST(table_join, buf);

    serializeEnum(asof_inequality, buf);

    columns_from_joined_table.serialize(buf);
    columns_added_by_join.serialize(buf);

    writeBinary(left_type_map.size(), buf);
    for (auto & item : left_type_map)
    {
        writeBinary(item.first, buf);
        serializeDataType(item.second, buf);
    }

    writeBinary(right_type_map.size(), buf);
    for (auto & item : right_type_map)
    {
        writeBinary(item.first, buf);
        serializeDataType(item.second, buf);
    }

    if (left_converting_actions)
    {
        writeBinary(true, buf);
        left_converting_actions->serialize(buf);
    }
    else
        writeBinary(false, buf);

    if (right_converting_actions)
    {
        writeBinary(true, buf);
        right_converting_actions->serialize(buf);
    }
    else
        writeBinary(false, buf);

    writeBinary(original_names.size(), buf);
    for (auto & item : original_names)
    {
        writeBinary(item.first, buf);
        writeBinary(item.second, buf);
    }

    writeBinary(renames.size(), buf);
    for (auto & item : renames)
    {
        writeBinary(item.first, buf);
        writeBinary(item.second, buf);
    }
}

void TableJoin::deserializeImpl(ReadBuffer & buf, ContextPtr context)
{
    readBinary(key_names_left, buf);
    readBinary(key_names_right, buf);

    key_asts_left = deserializeASTs(buf);
    key_asts_right = deserializeASTs(buf);
    auto table_join_ptr = deserializeAST(buf);
    table_join = *(table_join_ptr->as<ASTTableJoin>());

    deserializeEnum(asof_inequality, buf);

    columns_from_joined_table.deserialize(buf);
    columns_added_by_join.deserialize(buf);

    size_t left_size;
    readBinary(left_size, buf);
    for (size_t i = 0; i < left_size; ++i)
    {
        String name;
        readBinary(name, buf);
        auto type = deserializeDataType(buf);
        left_type_map[name] = type;
    }

    size_t right_size;
    readBinary(right_size, buf);
    for (size_t i = 0; i < right_size; ++i)
    {
        String name;
        readBinary(name, buf);
        auto type = deserializeDataType(buf);
        right_type_map[name] = type;
    }

    bool has_left_converting_actions;
    readBinary(has_left_converting_actions, buf);
    if (has_left_converting_actions)
        left_converting_actions = ActionsDAG::deserialize(buf, context);

    bool has_right_converting_actions;
    readBinary(has_right_converting_actions, buf);
    if (has_right_converting_actions)
        right_converting_actions = ActionsDAG::deserialize(buf, context);

    size_t original_names_size;
    readBinary(original_names_size, buf);
    for (size_t i = 0; i < original_names_size; ++i)
    {
        String key;
        String value;
        readBinary(key, buf);
        readBinary(value, buf);
        original_names[key] = value;
    }

    size_t renames_size;
    readBinary(renames_size, buf);
    for (size_t i = 0; i < renames_size; ++i)
    {
        String key;
        String value;
        readBinary(key, buf);
        readBinary(value, buf);
        renames[key] = value;
    }
}

std::shared_ptr<TableJoin> TableJoin::deserialize(ReadBuffer & buf, ContextPtr context)
{
    ASTPtr select_query = ASTSelectQuery::deserialize(buf);
    if (const auto * query = select_query->as<ASTSelectQuery>())
    {
        JoinedTables joined_tables(context, *query);
        joined_tables.resolveTables();
        auto table_join = joined_tables.makeTableJoin(*query);
        table_join->setSelectQuery(select_query);
        table_join->deserializeImpl(buf, context);
        return table_join;
    }
    throw Exception(ErrorCodes::LOGICAL_ERROR, "deserializeTableJoin needs ASTSelectQuery");
}

}
