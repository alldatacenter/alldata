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

#include <Functions/IFunction.h>
#include <Functions/FunctionFactory.h>
#include <Functions/FunctionHelpers.h>
#include <Functions/FunctionsConversion.h>
#include <Functions/FunctionStringToString.h>
#include <DataTypes/IDataType.h>
#include <DataTypes/DataTypeByteMap.h>
#include <DataTypes/MapHelpers.h>
#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypeNullable.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypeDate.h>
#include <DataTypes/DataTypeDateTime.h>
#include <DataTypes/getLeastSupertype.h>
#include <Columns/ColumnByteMap.h>
#include <Columns/ColumnArray.h>
#include <Columns/ColumnNullable.h>
#include <Columns/ColumnString.h>
#include <Common/Stopwatch.h>
#include <Poco/File.h>
#include <Poco/DirectoryIterator.h>
#include <Interpreters/Context.h>
#include <Storages/IStorage.h>
#include <Storages/StorageMergeTree.h>
#include <Storages/StorageDistributed.h>
#include <DataStreams/RemoteBlockInputStream.h>
#include <DataStreams/UnionBlockInputStream.h>
#include <Interpreters/InterpreterDescribeQuery.h>
#include <Interpreters/ClusterProxy/executeQuery.h>
#include <Interpreters/ClusterProxy/SelectStreamFactory.h>
#include <Interpreters/InterpreterSelectQuery.h>
#include <Interpreters/executeQuery.h>
#include <Parsers/ParserQuery.h>
#include <Parsers/parseQuery.h>

#include <unordered_set>
#include <iostream>
#include <regex>
#include <common/range.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int NUMBER_OF_ARGUMENTS_DOESNT_MATCH;
    extern const int NOT_FOUND_EXPECTED_DATA_PART;
    extern const int CANNOT_CONVERT_TYPE;
}

/**
 * mapElement(map, key) is a function that allows you to retrieve a column from map
 * How to implement it depends on the storage model of map type
 *  - option 1: if map is simply serialized lob, and this function need to get the
 *    deserialized map type, and access element correspondingly
 *
 *  - option 2: if map is stored as expanded implicit column, and per key's value is
 *    stored as single file, this functions could be intepreted as implicited column
 *    ref, and more efficient.
 *
 * Option 2 will be used in TEA project, but we go to option 1 firstly for demo, and
 * debug end-to-end prototype.
 *
 *
 * mapKeys(map) is a function that retrieve keys array of a map column row.
 */

class FunctionMapElement : public IFunction
{
public:
    static constexpr auto name = "mapElement";

    static FunctionPtr create(const ContextPtr &) { return std::make_shared<FunctionMapElement>(); }

    String getName() const override { return name; }

    size_t getNumberOfArguments() const override { return 2; }

    bool useDefaultImplementationForConstants() const override { return true; }

    ColumnNumbers getArgumentsThatAreAlwaysConstant() const override { return {1}; }

    bool useDefaultImplementationForLowCardinalityColumns() const override { return false; }

    DataTypePtr getReturnTypeImpl(const ColumnsWithTypeAndName & arguments) const override
    {
        const IDataType * map_col = arguments[0].type.get();
        const DataTypeByteMap * map = checkAndGetDataType<DataTypeByteMap>(map_col);

        if (!map)
            throw Exception(ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT, "First argument for function {} must be map.", getName());

        /// TODO: fix Map<Int32, Int32> KV case
        if (map->getKeyType()->getName() != arguments[1].type->getName())
            throw Exception(
                ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT,
                "Second argument which type is {} for function {} does not match map key type {}.",
                arguments[1].type->getName(),
                getName(),
                map->getKeyType()->getName());

        return map->getValueTypeForImplicitColumn();
    }

    ColumnPtr executeImpl(const ColumnsWithTypeAndName & arguments, const DataTypePtr & /*result_type*/, size_t /*input_rows_count*/) const override
    {
        const ColumnByteMap * col_map = checkAndGetColumn<ColumnByteMap>(arguments[0].column.get());
        auto & col_sel = arguments[1].column;
        if (!col_map)
            throw Exception("Input column doesn't match", ErrorCodes::LOGICAL_ERROR);

        auto & key_column = col_map->getKey();
        auto & value_column = col_map->getValue();

        // fix result type for low cardinality
        auto col_res = col_map->createEmptyImplicitColumn()->assumeMutable();
        bool add_nullable = !col_res->lowCardinality();

        auto & offsets = col_map->getOffsets();
        size_t size = offsets.size();

        ColumnByteMap::Offset src_prev_offset = 0;

        for (size_t i = 0; i < size; ++i)
        {
            bool found = false;
            for (size_t j = src_prev_offset; j < offsets[i]; ++j)
            {
                // locate if key match input
                if (key_column[j] == (*col_sel)[i])
                {
                    if (!add_nullable)
                        col_res->insertFrom(value_column, j);
                    else
                    {
                        static_cast<ColumnNullable &>(*col_res).getNestedColumn().insertFrom(value_column, j);
                        static_cast<ColumnNullable &>(*col_res).getNullMapData().push_back(0);
                    }
                    found = true;
                    break;
                }
            }

            if (!found)
            {
                col_res->insert(Null());
            }

            src_prev_offset = offsets[i];
        }

        return col_res;
    }
};

class FunctionMapKeys : public IFunction
{
public:
    static constexpr auto name = "mapKeys";

    static FunctionPtr create(const ContextPtr &) { return std::make_shared<FunctionMapKeys>(); }

    String getName() const override { return name; }

    size_t getNumberOfArguments() const override { return 1; }

    bool useDefaultImplementationForConstants() const override { return true; }

    ColumnNumbers getArgumentsThatAreAlwaysConstant() const override { return {}; }

    DataTypePtr getReturnTypeImpl(const ColumnsWithTypeAndName & arguments) const override
    {
        const IDataType * map_col = arguments[0].type.get();
        const DataTypeByteMap * map = checkAndGetDataType<DataTypeByteMap>(map_col);

        if (!map)
            throw Exception("First argument for function " + getName() + " must be map.", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        return std::make_shared<DataTypeArray>(map->getKeyType());
    }

    ColumnPtr executeImpl(const ColumnsWithTypeAndName & arguments, const DataTypePtr & /*result_type*/, size_t /*input_rows_count*/) const override
    {
        const ColumnByteMap * col_map = checkAndGetColumn<ColumnByteMap>(arguments[0].column.get());
        if (!col_map)
            throw Exception("Input column doesn't match", ErrorCodes::LOGICAL_ERROR);

        auto col_res = ColumnArray::create(col_map->getKeyPtr(), col_map->getOffsetsPtr());
        return col_res;

    }
};

class FunctionMapValues : public IFunction
{
public:
    static constexpr auto name = "mapValues";

    static FunctionPtr create(const ContextPtr &) { return std::make_shared<FunctionMapValues>(); }

    String getName() const override { return name; }

    size_t getNumberOfArguments() const override { return 1; }

    bool useDefaultImplementationForConstants() const override { return true; }

    ColumnNumbers getArgumentsThatAreAlwaysConstant() const override { return {}; }

    DataTypePtr getReturnTypeImpl(const ColumnsWithTypeAndName & arguments) const override
    {
        const IDataType * map_col = arguments[0].type.get();
        const DataTypeByteMap * map = checkAndGetDataType<DataTypeByteMap>(map_col);

        if (!map)
            throw Exception("First argument for function " + getName() + " must be map.", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        return std::make_shared<DataTypeArray>(map->getValueType());
    }

    ColumnPtr executeImpl(const ColumnsWithTypeAndName & arguments, const DataTypePtr & /*result_type*/, size_t /*input_rows_count*/) const override
    {
        const ColumnByteMap * col_map = checkAndGetColumn<ColumnByteMap>(arguments[0].column.get());
        if (!col_map)
            throw Exception("Input column doesn't match", ErrorCodes::LOGICAL_ERROR);

        auto col_res = ColumnArray::create(col_map->getValuePtr(), col_map->getOffsetsPtr());
        return col_res;

    }
};


class FunctionGetMapKeys : public IFunction
{
public:
    static constexpr auto name = "getMapKeys";

    static FunctionPtr create(const ContextPtr & context) { return std::make_shared<FunctionGetMapKeys>(context); }

    FunctionGetMapKeys(const ContextPtr & c) : context(c) { }

    String getName() const override { return name; }

    bool isVariadic() const override { return true; }

    size_t getNumberOfArguments() const override { return 0; }

    bool useDefaultImplementationForConstants() const override { return true; }

    ColumnNumbers getArgumentsThatAreAlwaysConstant() const override { return {}; }

    DataTypePtr getReturnTypeImpl(const ColumnsWithTypeAndName & arguments) const override
    {
        if (arguments.size() != 3 && arguments.size() != 4 && arguments.size() != 5)
            throw Exception(
                "Function " + getName()
                    + " requires 3 or 4 or 5 parameters: db, table, column, [partition expression], [max execute time]. Passed "
                    + toString(arguments.size()),
                ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        for (unsigned int i = 0; i < (arguments.size() == 5 ? 4 : arguments.size()); i++)
        {
            const IDataType * argument_type = arguments[i].type.get();
            const DataTypeString * argument = checkAndGetDataType<DataTypeString>(argument_type);
            if (!argument)
                throw Exception(
                    "Illegal column " + arguments[i].name + " of argument of function " + getName(), ErrorCodes::ILLEGAL_COLUMN);
        }

        if (arguments.size() == 5)
        {
            bool ok = checkAndGetDataType<DataTypeUInt64>(arguments[4].type.get())
                || checkAndGetDataType<DataTypeUInt32>(arguments[4].type.get())
                || checkAndGetDataType<DataTypeUInt16>(arguments[4].type.get())
                || checkAndGetDataType<DataTypeUInt8>(arguments[4].type.get());
            if (!ok)
                throw Exception(
                    "Illegal column " + arguments[4].name + " of argument of function " + getName(), ErrorCodes::ILLEGAL_COLUMN);
        }

        return std::make_shared<DataTypeArray>(std::make_shared<DataTypeString>());
    }

    inline String getColumnStringValue(const ColumnWithTypeAndName & argument) const { return argument.column->getDataAt(0).toString(); }

    ColumnPtr executeImpl(const ColumnsWithTypeAndName & arguments, const DataTypePtr & result_type, size_t input_rows_count) const override
    {
        String db_name = getColumnStringValue(arguments[0]);
        String table_name = getColumnStringValue(arguments[1]);
        String column_name = getColumnStringValue(arguments[2]);
        if (db_name.empty() || table_name.empty() || column_name.empty())
            throw Exception("Bad arguments: database/table/column should not be empty", ErrorCodes::BAD_ARGUMENTS);

        String pattern;
        if (arguments.size() >= 4)
        {
            pattern = getColumnStringValue(arguments[3]);
            if (pattern.empty())
                throw Exception("Bad arguments: regex should not be empty", ErrorCodes::BAD_ARGUMENTS);
        }

        /**
            SELECT groupUniqArrayArray(ks) AS keys FROM (
                SELECT arrayMap(t -> t.2, arrayFilter(t -> t.1 = 'some_map', _map_column_keys)) AS ks
                FROM some_db.some_table WHERE match(_partition_id, '.*2020.*10.*10.*')
                SETTINGS early_limit_for_map_virtual_columns = 1, max_threads = 1
            )
         */
        String inner_query = "SELECT arrayMap(t -> t.2, arrayFilter(t -> t.1 = '" + column_name + "', _map_column_keys)) AS ks" //
            + " FROM `" + db_name + "`.`" + table_name + "`" //
            + (pattern.empty() ? "" : " WHERE match(_partition_id, '" + pattern + "')") //
            + " SETTINGS  max_threads = 1";
        String query = "SELECT groupUniqArrayArray(ks) AS keys FROM ( " + inner_query + " )";

        auto stream = executeQuery(query, context->getQueryContext(), true).getInputStream();
        auto res = stream->read();
        if (res)
        {
            Field field;
            res.getByName("keys").column->get(0, field);
            return result_type->createColumnConst(input_rows_count, field)->convertToFullColumnIfConst();
        }
        else
        {
            return result_type->createColumnConst(input_rows_count, Array{})->convertToFullColumnIfConst();
        }
    }

private:
    ContextPtr context;
};


class FunctionStrToMap: public IFunction
{
public:
    static constexpr auto name = "str_to_map";

    static FunctionPtr create(const ContextPtr &) { return std::make_shared<FunctionStrToMap>(); }

    String getName() const override { return name; }

    bool isVariadic() const override { return true; }

    size_t getNumberOfArguments() const override { return 3; }

    ColumnNumbers getArgumentsThatAreAlwaysConstant() const override { return {1, 2}; }

    /// throw error when argument[0] column is Nullable(String)
    /// TODO : add Function Convert Nullable(String) column to String column
    bool useDefaultImplementationForNulls() const override { return false; }

    DataTypePtr getReturnTypeImpl(const ColumnsWithTypeAndName & arguments) const override
    {
        if (arguments.size() != 3)
            throw Exception("Function " + getName() + " requires 3 argument. Passed " + toString(arguments.size()) + ".",
                            ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        if (!isString(arguments[0].type))
            throw Exception("First argument for function " + getName() + "  must be String, but parsed " + arguments[0].type->getName() + ".",
                            ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        if (!isString(arguments[1].type))
            throw Exception("Second argument for function " + getName() + " (delimiter) must be String.",
                            ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        if (!isString(arguments[2].type))
            throw Exception("Third argument for function " + getName() + " (delimiter) must be String.",
                            ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        return std::make_shared<DataTypeByteMap>(std::make_shared<DataTypeString>(), std::make_shared<DataTypeString>());
    }

    ColumnPtr executeImpl(const ColumnsWithTypeAndName & arguments, const DataTypePtr & result_type, size_t input_rows_count) const override
    {
        const ColumnPtr column_prt = arguments[0].column;
        auto item_delimiter = getDelimiter(arguments[1].column);
        auto key_value_delimiter = getDelimiter(arguments[2].column);
        auto col_res = result_type->createColumn();

        for (size_t i = 0; i < input_rows_count; ++i)
        {
            ByteMap map;
            const StringRef & str = column_prt->getDataAt(i);
            const char * curr = str.data;
            const char * end = str.data + str.size;

            while (curr != end)
            {
                auto parsed_key = parseStringValue(curr, end, key_value_delimiter);

                /// skip space
                while (curr != end && *curr == ' ')
                    ++curr;
                /// No matched value, discard this key
                if (curr == end)
                    break;

                auto parsed_value = parseStringValue(curr, end, item_delimiter);

                /// skip space
                while (curr != end && *curr == ' ')
                    ++curr;

                map.emplace_back(parsed_key, parsed_value);
            }
            col_res->insert(map);
        }
        return col_res;
    }

private:

    static String parseStringValue(const char *& curr, const char * end, char delimiter)
    {
        const auto * begin = curr;
        size_t length = 0;
        while (curr != end && *curr != delimiter)
        {
            ++curr;
            ++length;
        }

        /// skip delimiter
        if (curr != end && *curr == delimiter)
            ++curr;

        return {begin, length};
    }

    inline char getDelimiter(const ColumnPtr & column_ptr) const
    {
        const auto & value = column_ptr->getDataAt(0);
        if (!value.size)
            throw Exception("Delimiter of function " + getName() + " should be non-empty string", ErrorCodes::ILLEGAL_COLUMN);
        return value.data[0];
    }
};

class FunctionMapConstructor: public IFunction
{
public:
    static constexpr auto name = "map";

    static FunctionPtr create(const ContextPtr &) { return std::make_shared<FunctionMapConstructor>(); }

    String getName() const override { return name; }

    bool isVariadic() const override { return true; }

    size_t getNumberOfArguments() const override { return 0; }

    bool useDefaultImplementationForNulls() const override { return false; }

    bool useDefaultImplementationForConstants() const override { return true; }

    /// parse map(k1, v1, k2, v2 ...)
    DataTypePtr getReturnTypeImpl(const ColumnsWithTypeAndName & arguments) const override
    {
        if ((arguments.size() & 1) || arguments.empty())
            throw Exception("Function " + getName() + " requires none zero even number of argument. Passed " + toString(arguments.size()),
                    ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);

        DataTypes key_types;
        DataTypes value_types;
        key_types.reserve(arguments.size() >> 1);
        value_types.reserve(arguments.size() >> 1);

        for (size_t i = 0; i < arguments.size(); ++i)
        {
            if (i & 1)
                value_types.push_back(arguments[i].type);
            else
                key_types.push_back(arguments[i].type);
        }

        auto key_type = getLeastSupertype(key_types);
        auto value_type = getLeastSupertype(value_types);

        return std::make_shared<DataTypeByteMap>(removeNullable(key_type), removeNullable(value_type));
    }

    ColumnPtr executeImpl(const ColumnsWithTypeAndName & arguments, const DataTypePtr & result_type, size_t input_rows_count) const override
    {
        auto col_res = result_type->createColumn();

        for (size_t i = 0; i < input_rows_count; ++i)
        {
            ByteMap map;
            for(size_t pos = 0; pos < arguments.size(); pos += 2)
            {
                auto key = (*arguments[pos].column)[i];
                auto value = (*arguments[pos + 1].column)[i];

                /// remove null value
                if (key.isNull() || value.isNull())
                    continue;

                map.emplace_back(key, value);
            }
            col_res->insert(map);
        }
        return col_res;
    }
};

struct NameExtractMapColumn
{
    static constexpr auto name = "extractMapColumn";
};

struct NameExtractMapKey
{
    static constexpr auto name = "extractMapKey";
};

template <class Extract>
struct ExtractMapWrapper
{
    static void vector(const ColumnString::Chars & data,
        const ColumnString::Offsets & offsets,
        ColumnString::Chars & res_data,
        ColumnString::Offsets & res_offsets)
    {
        res_data.resize(data.size());
        size_t offsets_size = offsets.size();
        res_offsets.resize(offsets_size);

        size_t prev_offset = 0;
        size_t res_offset = 0;

        for (size_t i = 0; i < offsets_size; ++i)
        {
            const char * src_data = reinterpret_cast<const char *>(&data[prev_offset]);
            size_t src_size = offsets[i] - prev_offset;
            auto res_view = Extract::apply(std::string_view(src_data, src_size));
            memcpy(reinterpret_cast<char *>(res_data.data() + res_offset), res_view.data(), res_view.size());

            res_offset += res_view.size() + 1; /// remember add 1 for null char
            res_offsets[i] = res_offset;
            prev_offset = offsets[i];
        }

        res_data.resize(res_offset);
    }

    static void vectorFixed(const ColumnString::Chars &, size_t, ColumnString::Chars &)
    {
        throw Exception("Column of type FixedString is not supported by extractMapColumn", ErrorCodes::ILLEGAL_COLUMN);
    }
};

void registerFunctionsByteMap(FunctionFactory & factory)
{
    factory.registerFunction<FunctionMapElement>();
    factory.registerFunction<FunctionMapKeys>();
    factory.registerFunction<FunctionMapValues>();
    factory.registerFunction<FunctionGetMapKeys>();
    factory.registerFunction<FunctionStrToMap>(FunctionFactory::CaseInsensitive);
    factory.registerFunction<FunctionMapConstructor>(FunctionFactory::CaseInsensitive);

    using FunctionExtractMapColumn = FunctionStringToString<ExtractMapWrapper<ExtractMapColumn>, NameExtractMapColumn>;
    using FunctionExtractMapKey = FunctionStringToString<ExtractMapWrapper<ExtractMapKey>, NameExtractMapKey>;
    factory.registerFunction<FunctionExtractMapKey>();
    factory.registerFunction<FunctionExtractMapColumn>();
}

}
