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

#include <Columns/ColumnByteMap.h>
#include <Columns/ColumnsNumber.h>
#include <Columns/ColumnString.h>
#include <Columns/ColumnNullable.h>
#include <Columns/ColumnsCommon.h>
#include <Columns/ColumnArray.h>
#include <Columns/ColumnLowCardinality.h>
#include <common/logger_useful.h>

#include <Common/Arena.h>
#include <DataStreams/ColumnGathererStream.h>

#include <Common/typeid_cast.h>
#include <IO/WriteHelpers.h> // toString func
#include <Common/FieldVisitorToString.h>


namespace DB
{

namespace ErrorCodes
{
    extern const int NOT_IMPLEMENTED;
    extern const int BAD_ARGUMENTS;
    extern const int LOGICAL_ERROR;
}


std::string ColumnByteMap::getName() const
{
    return "Map("+ getKey().getName() + "," + getValue().getName() + ")" ;
}

ColumnByteMap::ColumnByteMap(MutableColumnPtr && key_column_, MutableColumnPtr && value_column_, MutableColumnPtr && offsets_column)
    :key_column(std::move(key_column_)), value_column(std::move(value_column_)), offsets(std::move(offsets_column))
{
}

ColumnByteMap::ColumnByteMap(MutableColumnPtr && key_column_, MutableColumnPtr && value_column_)
    :key_column(std::move(key_column_)), value_column(std::move(value_column_))
{
    if (!key_column->empty() || !value_column->empty())
       throw Exception("Not empty key, value passed to ColumnByteMap, but no offsets passed " + toString(key_column_->size()) + " : " + toString(value_column_->size()),
               ErrorCodes::BAD_ARGUMENTS);
    offsets = ColumnOffsets::create();
}

MutableColumnPtr ColumnByteMap::cloneEmpty() const
{
    return ColumnByteMap::create(getKey().cloneEmpty(), getValue().cloneEmpty());
}

Field ColumnByteMap::operator[](size_t n) const
{
    size_t offset = offsetAt(n);
    size_t size = sizeAt(n);
    ByteMap res(size);

    for (size_t i=0; i<size; ++i)
    {
        res[i].first = getKey()[offset + i];
        res[i].second = getValue()[offset + i];
    }
    return res;
}

void ColumnByteMap::get(size_t n, Field & res) const
{
    size_t offset = offsetAt(n);
    size_t size = sizeAt(n);
    res = ByteMap(size);
    ByteMap & res_map = DB::get<ByteMap&>(res);
    for (size_t i=0; i<size; ++i)
    {
        getKey().get(offset+i, res_map[i].first);
        getValue().get(offset+i, res_map[i].second);
    }
}

StringRef ColumnByteMap::getDataAt(size_t) const
{
    throw Exception("Method getDataAt is not supported for " + getName(), ErrorCodes::NOT_IMPLEMENTED);
}

void ColumnByteMap::insertData(const char *, size_t)
{
    throw Exception("Method insertData is not supported for " + getName(), ErrorCodes::NOT_IMPLEMENTED);
}

void ColumnByteMap::insert(const Field & x)
{
    const ByteMap & map = DB::get<const ByteMap &>(x);
    size_t size = map.size();

    for (size_t i = 0; i < size; ++i)
    {
        getKey().insert(map[i].first);
        getValue().insert(map[i].second);
    }
    getOffsets().push_back(getOffsets().back() + size);
}

void ColumnByteMap::insertFrom(const IColumn & src_, size_t n)
{
    const ColumnByteMap & src = static_cast<const ColumnByteMap &>(src_);
    size_t size = src.sizeAt(n);
    size_t offset = src.offsetAt(n);

    getKey().insertRangeFrom(src.getKey(), offset, size);
    getValue().insertRangeFrom(src.getValue(), offset, size);
    getOffsets().push_back(getOffsets().back() + size);
}

void ColumnByteMap::insertDefault()
{
    /// NOTE 1: We can use back() even if the array is empty (due to zero -1th element in PODArray).
    /// NOTE 2: We cannot use reference in push_back, because reference get invalidated if array is reallocated.
    auto last_offset = getOffsets().back();
    getOffsets().push_back(last_offset);
}

void ColumnByteMap::popBack(size_t n)
{
    auto & offsets_ = getOffsets();
    size_t nested_n = offsets_.back() - offsetAt(offsets_.size() - n);
    if (nested_n)
    {
        getKey().popBack(nested_n);
        getValue().popBack(nested_n);
    }
    offsets_.resize_assume_reserved(offsets_.size() - n);
}

StringRef ColumnByteMap::serializeValueIntoArena(size_t n, Arena & arena, char const *& begin) const
{
    size_t map_size = sizeAt(n);
    size_t offset = offsetAt(n);

    char * pos = arena.allocContinue(sizeof(map_size), begin);
    memcpy(pos, &map_size, sizeof(map_size));

    size_t value_size(0);
    for (size_t i = 0; i < map_size; ++i)
    {
        value_size += getKey().serializeValueIntoArena(offset+i, arena, begin).size;
        value_size += getValue().serializeValueIntoArena(offset+i, arena, begin).size;
    }

    return StringRef(begin, sizeof(map_size) + value_size);
}

const char * ColumnByteMap::deserializeAndInsertFromArena(const char * pos)
{
    size_t map_size = *reinterpret_cast<const size_t *>(pos);
    pos += sizeof(map_size);

    for (size_t i = 0; i<map_size; ++i)
    {
        pos = getKey().deserializeAndInsertFromArena(pos);
        pos = getValue().deserializeAndInsertFromArena(pos);
    }

    getOffsets().push_back(getOffsets().back() + map_size);
    return pos;
}

const char * ColumnByteMap::skipSerializedInArena(const char * pos) const
{
    size_t map_size = unalignedLoad<size_t>(pos);
    pos += sizeof(map_size);

    for (size_t i = 0; i < map_size; ++i)
    {
        pos = getKey().skipSerializedInArena(pos);
        pos = getValue().skipSerializedInArena(pos);
    }

    return pos;
}

void ColumnByteMap::updateHashWithValue([[maybe_unused]] size_t n, [[maybe_unused]] SipHash & hash) const
{
    throw Exception("Map doesn't support updateHashWithValue", ErrorCodes::NOT_IMPLEMENTED);
}

void ColumnByteMap::insertRangeFrom(const IColumn & src, size_t start, size_t length)
{
    if (length == 0) return;
    const ColumnByteMap & src_concrete = static_cast<const ColumnByteMap &>(src);

    if (start + length > src_concrete.getOffsets().size())
        throw Exception("Parameter out of bound in ColumnByteMap::insertRangeFrom method.",
            ErrorCodes::BAD_ARGUMENTS);
    size_t nested_offset = src_concrete.offsetAt(start);
    size_t nested_length = src_concrete.getOffsets()[start + length -1] - nested_offset;

    getKey().insertRangeFrom(src_concrete.getKey(), nested_offset, nested_length);
    getValue().insertRangeFrom(src_concrete.getValue(), nested_offset, nested_length);

    Offsets & cur_offsets = getOffsets();
    const Offsets & src_offsets = src_concrete.getOffsets();

    if (start == 0 && cur_offsets.empty())
    {
        cur_offsets.assign(src_offsets.begin(), src_offsets.begin() + length);
    }
    else
    {
        size_t old_size = cur_offsets.size();
        size_t prev_max_offset = old_size ? cur_offsets.back() : 0;
        cur_offsets.resize(old_size + length);

        for (size_t i = 0; i < length; ++i)
            cur_offsets[old_size + i] = src_offsets[start+i] - nested_offset + prev_max_offset;
    }
}

void ColumnByteMap::insertRangeSelective(const IColumn & src, const IColumn::Selector & selector, size_t selector_start, size_t length)
{
    if (length == 0) return;

    const ColumnByteMap & src_concrete = static_cast<const ColumnByteMap &>(src);
    const IColumn & src_key_col = src_concrete.getKey();
    const IColumn & src_value_col = src_concrete.getValue();
    IColumn & cur_key_col = getKey();
    IColumn & cur_value_col = getValue();
    Offsets & cur_offsets = getOffsets();

    size_t old_size = cur_offsets.size();
    cur_offsets.resize(old_size + length);

    size_t cur_offset_size = cur_offsets[old_size - 1];
    for (size_t i = 0; i < length; i++)
    {
        size_t n = selector[selector_start + i];
        size_t size = src_concrete.sizeAt(n);
        size_t offset = src_concrete.offsetAt(n);

        cur_key_col.insertRangeFrom(src_key_col, offset, size);
        cur_value_col.insertRangeFrom(src_value_col, offset, size);
        cur_offsets[old_size + i] = cur_offset_size + size;
        cur_offset_size += size;
    }
}

// static method to filter Columns which was built on Offsets(similar to ColumnArray)
void ColumnByteMap::filter(const ColumnPtr& implCol, ColumnPtr& implResCol,
                              const Offsets& offsets, const Filter& filt,
                              ssize_t result_size_hint)
{
    // Handle implicit key column
    if (typeid_cast<const ColumnUInt8 *>(implCol.get()))
    {
        ColumnByteMap::filterNumber<UInt8>(implCol, implResCol, offsets, filt, result_size_hint);
    }
    else if (typeid_cast<const ColumnUInt16 *>(implCol.get()))
    {
        ColumnByteMap::filterNumber<UInt16>(implCol, implResCol, offsets, filt, result_size_hint);
    }
    else if (typeid_cast<const ColumnUInt32 *>(implCol.get()))
    {
        ColumnByteMap::filterNumber<UInt32>(implCol, implResCol, offsets, filt, result_size_hint);
    }
    else if (typeid_cast<const ColumnUInt64 *>(implCol.get()))
    {
        ColumnByteMap::filterNumber<UInt64>(implCol, implResCol, offsets, filt, result_size_hint);
    }
    else if (typeid_cast<const ColumnInt8 *>(implCol.get()))
    {
        ColumnByteMap::filterNumber<Int8>(implCol, implResCol, offsets, filt, result_size_hint);
    }
    else if (typeid_cast<const ColumnInt16 *>(implCol.get()))
    {
        ColumnByteMap::filterNumber<Int16>(implCol, implResCol, offsets, filt, result_size_hint);
    }
    else if (typeid_cast<const ColumnInt32 *>(implCol.get()))
    {
        ColumnByteMap::filterNumber<Int32>(implCol, implResCol, offsets, filt, result_size_hint);
    }
    else if (typeid_cast<const ColumnInt64 *>(implCol.get()))
    {
        ColumnByteMap::filterNumber<Int64>(implCol, implResCol, offsets, filt, result_size_hint);
    }
    else if (typeid_cast<const ColumnFloat32 *>(implCol.get()))
    {
        ColumnByteMap::filterNumber<Float32>(implCol, implResCol, offsets, filt, result_size_hint);
    }
    else if (typeid_cast<const ColumnFloat64 *>(implCol.get()))
    {
        ColumnByteMap::filterNumber<Float64>(implCol, implResCol, offsets, filt, result_size_hint);
    }
    else if (typeid_cast<const ColumnString *>(implCol.get()))
    {
        ColumnByteMap::filterString(implCol, implResCol, offsets, filt, result_size_hint);
    }
    else if (typeid_cast<const ColumnNullable *>(implCol.get()))
    {
        ColumnByteMap::filterNullable(implCol, implResCol, offsets, filt, result_size_hint);
    }
    else if (typeid_cast<const ColumnArray *>(implCol.get()))
    {
        ColumnByteMap::filterArray(implCol, implResCol, offsets, filt, result_size_hint);
    }
    else if (typeid_cast<const ColumnLowCardinality *>(implCol.get()))
    {
        ColumnByteMap::filterLowCardinality(implCol, implResCol, offsets, filt, result_size_hint);
    }
    else
    {
        throw Exception("ColumnByteMap doesn't support this implicit type", ErrorCodes::NOT_IMPLEMENTED);
    }
}

ColumnPtr ColumnByteMap::filter(const Filter & filt, ssize_t result_size_hint) const
{
    size_t size = getOffsets().size();
    if (size == 0)
    {
        return ColumnByteMap::create(key_column, value_column);
    }
    // There are two implicit columns that need to be filtered, and Offsets need adjustments.
    auto res = ColumnByteMap::create(key_column->cloneEmpty(), value_column->cloneEmpty());

    Offsets & res_offsets = res->getOffsets();

    // Handle implicit key column
    ColumnByteMap::filter(key_column, res->getKeyPtr(), getOffsets(), filt, result_size_hint);

    // Handle implicit value column
    ColumnByteMap::filter(value_column, res->getValuePtr(), getOffsets(), filt, result_size_hint);

    // Handle Offsets explicitly
    size_t current_offset = 0;
    for (size_t i = 0; i<size; ++i)
    {
        if (filt[i])
        {
            current_offset += sizeAt(i);
            res_offsets.push_back(current_offset);
        }
    }

    return res;
}

// template processing based on key value types
template<typename T>
void ColumnByteMap::filterNumber(const ColumnPtr& implCol, ColumnPtr& implResCol,
                                    const Offsets& offsets, const Filter & filt,
                                    ssize_t result_size_hint)
{
    filterArraysImplOnlyData<T>(static_cast<const ColumnVector<T> &>(*implCol).getData(),
                                offsets,
                                static_cast<ColumnVector<T>&>(implResCol->assumeMutableRef()).getData(),
                                filt,
                                result_size_hint);
}

void ColumnByteMap::filterArray(const ColumnPtr & implCol, ColumnPtr & implResCol, const Offsets & offsets,
                                const Filter & filt, ssize_t result_size_hint)
{
    // Construct a new filter for filtering column
    Filter new_filter;
    Offset prev_offset = 0;
    for (size_t i = 0; i < offsets.size(); ++i)
    {
        size_t array_size = offsets[i] - prev_offset;
        size_t old_size = new_filter.size();
        if (filt[i])
            new_filter.resize_fill(old_size + array_size, 1);
        else
            new_filter.resize_fill(old_size + array_size, 0);

        prev_offset += array_size;
    }

    const ColumnArray & src_array = typeid_cast<const ColumnArray &>(*implCol);
    ColumnPtr res_ptr = src_array.filter(new_filter, result_size_hint);
    implResCol = std::move(res_ptr);
}

void ColumnByteMap::filterLowCardinality(const ColumnPtr & implCol, ColumnPtr & implResCol, const Offsets & offsets,
                            const Filter & filt, ssize_t result_size_hint)
{
    // Construct a new filter for filtering column
    Filter new_filter;
    Offset prev_offset = 0;
    for (size_t i = 0; i < offsets.size(); ++i)
    {
        size_t array_size = offsets[i] - prev_offset;
        size_t old_size = new_filter.size();
        if (filt[i])
            new_filter.resize_fill(old_size + array_size, 1);
        else
            new_filter.resize_fill(old_size + array_size, 0);

        prev_offset += array_size;
    }

    const ColumnLowCardinality & src_col = typeid_cast<const ColumnLowCardinality &>(*implCol);
    ColumnPtr res_ptr = src_col.filter(new_filter, result_size_hint);
    implResCol = std::move(res_ptr);
}

void ColumnByteMap::filterString(const ColumnPtr& implCol, ColumnPtr& implResCol,
                                    const Offsets& offsets, const Filter & filt,
                                    ssize_t result_size_hint)
{
    size_t col_size = offsets.size();
    const ColumnString & src_string = typeid_cast<const ColumnString &>(*implCol);
    const ColumnString::Chars & src_chars = src_string.getChars();
    const Offsets & src_string_offsets = src_string.getOffsets();
    const Offsets & src_offsets = offsets;

    ColumnString::Chars & res_chars = typeid_cast<ColumnString &>(implResCol->assumeMutableRef()).getChars();
    Offsets& res_string_offsets = typeid_cast<ColumnString &>(implResCol->assumeMutableRef()).getOffsets();

    if (result_size_hint < 0)
    {
        res_chars.reserve(src_chars.size());
        res_string_offsets.reserve(src_string_offsets.size());
    }

    Offset prev_src_offset = 0;
    Offset prev_src_string_offset = 0;

    Offset prev_res_offset = 0;
    Offset prev_res_string_offset = 0;

    for (size_t i = 0; i < col_size; ++i)
    {
        /// Number of rows in the array.
        size_t array_size = src_offsets[i] - prev_src_offset;

        if (filt[i])
        {
            /// If the array is not empty - copy content.
            if (array_size)
            {
                size_t chars_to_copy = src_string_offsets[array_size + prev_src_offset - 1] - prev_src_string_offset;
                size_t res_chars_prev_size = res_chars.size();
                res_chars.resize(res_chars_prev_size + chars_to_copy);
                memcpy(&res_chars[res_chars_prev_size], &src_chars[prev_src_string_offset], chars_to_copy);

                for (size_t j = 0; j < array_size; ++j)
                    res_string_offsets.push_back(src_string_offsets[j + prev_src_offset] + prev_res_string_offset - prev_src_string_offset);

                prev_res_string_offset = res_string_offsets.back();
            }

            prev_res_offset += array_size;
        }

        if (array_size)
        {
            prev_src_offset += array_size;
            prev_src_string_offset = src_string_offsets[prev_src_offset - 1];
        }
    }
}

void ColumnByteMap::filterNullable(const ColumnPtr& implCol, ColumnPtr& implResCol,
                                      const Offsets& offsets, const Filter & filt,
                                      ssize_t result_size_hint)
{
    const ColumnNullable & nullable_elems = static_cast<const ColumnNullable &>(*implCol);
    ColumnNullable & nullable_res = static_cast<ColumnNullable &>(implResCol->assumeMutableRef());

    auto& src_nested_column = nullable_elems.getNestedColumnPtr();
    auto& src_null_ind = nullable_elems.getNullMapColumnPtr();

    auto& res_nested_column = nullable_res.getNestedColumnPtr();
    auto& res_null_ind = nullable_res.getNullMapColumnPtr();

    // Handle nested column
    ColumnByteMap::filter(src_nested_column, res_nested_column, offsets, filt, result_size_hint);
    // Handle null indicator
    ColumnByteMap::filter(src_null_ind, res_null_ind, offsets, filt, result_size_hint);
}

/** TODO: double check the logic later **/
ColumnPtr ColumnByteMap::permute(const Permutation & perm, size_t limit) const
{
    size_t size = getOffsets().size();
    if (limit == 0)
        limit = size;
    else
        limit = std::min(size, limit);

    if (perm.size() < limit)
        throw Exception("Size of permutation is less than required.", ErrorCodes::SIZES_OF_COLUMNS_DOESNT_MATCH);

    if (limit == 0)
    {
        return ColumnByteMap::create(key_column, value_column);
    }

    Permutation nested_perm(getOffsets().back());

    auto res = ColumnByteMap::create(key_column->cloneEmpty(), value_column->cloneEmpty());
    Offsets& res_offsets = res->getOffsets();
    res_offsets.resize(limit);

    size_t current_offset = 0;
    for (size_t i = 0; i < limit; ++i)
    {
        for (size_t j = 0; j < sizeAt(perm[i]); ++j)
            nested_perm[current_offset + j] = offsetAt(perm[i]) + j;
        current_offset += sizeAt(perm[i]);
        res_offsets[i] = current_offset;
    }

    if (current_offset != 0)
    {
        res->key_column = key_column->permute(nested_perm, current_offset);
        res->value_column = value_column->permute(nested_perm, current_offset);
    }

    return res;
}

ColumnPtr ColumnByteMap::index(const IColumn & indexes, size_t limit) const
{
    auto key_index_column = key_column->index(indexes, limit);
    auto value_index_column = value_column->index(indexes, limit);
    return ColumnByteMap::create(key_index_column, value_index_column);
}

void ColumnByteMap::getPermutation([[maybe_unused]] bool reverse,
                               [[maybe_unused]] size_t limit,
                               [[maybe_unused]] int nan_direction_hint,
                               [[maybe_unused]] Permutation & res) const
{
    throw Exception("ColumnByteMap::getPermutation not implemented", ErrorCodes::NOT_IMPLEMENTED);
}

void ColumnByteMap::updatePermutation(bool, size_t, int, IColumn::Permutation &, EqualRanges &) const
{
    throw Exception("ColumnByteMap::updatePermutation not implemented", ErrorCodes::NOT_IMPLEMENTED);
}

ColumnPtr ColumnByteMap::replicate(const Offsets & replicate_offsets) const
{
    size_t col_size = size();
    if (col_size != replicate_offsets.size())
        throw Exception("Size of offsets doesn't match size of column.", ErrorCodes::SIZES_OF_COLUMNS_DOESNT_MATCH);
    auto res = ColumnByteMap::create(key_column->cloneEmpty(), value_column->cloneEmpty());
    if (col_size == 0)
    {
        return res;
    }

    Offsets & res_offsets = res->getOffsets();

    ColumnByteMap::replicate(key_column, res->getKeyPtr(), getOffsets(), replicate_offsets);
    ColumnByteMap::replicate(value_column, res->getValuePtr(), getOffsets(), replicate_offsets);

    // Handle offsets explicitly
    const Offsets & src_offsets = getOffsets();
    res_offsets.reserve(replicate_offsets.back());

    Offset prev_replicate_offset = 0;
    Offset prev_data_offset = 0;
    Offset current_new_offset = 0;

    for (size_t i = 0; i < col_size; ++i)
    {
        size_t size_to_replicate = replicate_offsets[i] - prev_replicate_offset;
        size_t value_size = src_offsets[i] - prev_data_offset;

        for (size_t j = 0; j < size_to_replicate; ++j)
        {
            current_new_offset += value_size;
            res_offsets.push_back(current_new_offset);
        }

        prev_replicate_offset = replicate_offsets[i];
        prev_data_offset = src_offsets[i];
    }

    return res;
}

void ColumnByteMap::replicate(const ColumnPtr& implCol, ColumnPtr& implResCol,
                                 const Offsets& offsets, const Offsets& replicate_offsets)
{
    // Handle implicit key column
    if (typeid_cast<const ColumnUInt8 *>(implCol.get()))
    {
        ColumnByteMap::replicateNumber<UInt8>(implCol, implResCol, offsets, replicate_offsets);
    }
    else if (typeid_cast<const ColumnUInt16 *>(implCol.get()))
    {
        ColumnByteMap::replicateNumber<UInt16>(implCol, implResCol, offsets, replicate_offsets);
    }
    else if (typeid_cast<const ColumnUInt32 *>(implCol.get()))
    {
        ColumnByteMap::replicateNumber<UInt32>(implCol, implResCol, offsets, replicate_offsets);
    }
    else if (typeid_cast<const ColumnUInt64 *>(implCol.get()))
    {
        ColumnByteMap::replicateNumber<UInt64>(implCol, implResCol, offsets, replicate_offsets);
    }
    else if (typeid_cast<const ColumnInt8 *>(implCol.get()))
    {
        ColumnByteMap::replicateNumber<Int8>(implCol, implResCol, offsets, replicate_offsets);
    }
    else if (typeid_cast<const ColumnInt16 *>(implCol.get()))
    {
        ColumnByteMap::replicateNumber<Int16>(implCol, implResCol, offsets, replicate_offsets);
    }
    else if (typeid_cast<const ColumnInt32 *>(implCol.get()))
    {
        ColumnByteMap::replicateNumber<Int32>(implCol, implResCol, offsets, replicate_offsets);
    }
    else if (typeid_cast<const ColumnInt64 *>(implCol.get()))
    {
        ColumnByteMap::replicateNumber<Int64>(implCol, implResCol, offsets, replicate_offsets);
    }
    else if (typeid_cast<const ColumnFloat32 *>(implCol.get()))
    {
        ColumnByteMap::replicateNumber<Float32>(implCol, implResCol, offsets, replicate_offsets);
    }
    else if (typeid_cast<const ColumnFloat64 *>(implCol.get()))
    {
        ColumnByteMap::replicateNumber<Float64>(implCol, implResCol, offsets, replicate_offsets);
    }
    else if (typeid_cast<const ColumnString *>(implCol.get()))
    {
        ColumnByteMap::replicateString(implCol, implResCol, offsets, replicate_offsets);
    }
    else if (typeid_cast<const ColumnNullable *>(implCol.get()))
    {
        ColumnByteMap::replicateNullable(implCol, implResCol, offsets, replicate_offsets);
    }
    else if (typeid_cast<const ColumnArray *>(implCol.get()))
    {
        ColumnByteMap::replicateArray(implCol, implResCol, offsets, replicate_offsets);
    }
    else if (typeid_cast<const ColumnLowCardinality *>(implCol.get()))
    {
        ColumnByteMap::replicateLowCardinality(implCol, implResCol, offsets, replicate_offsets);
    }
    else
    {
        throw Exception("ColumnByteMap doesn't support this implicit type", ErrorCodes::NOT_IMPLEMENTED);
    }

}

void ColumnByteMap::replicateLowCardinality(const ColumnPtr& implCol, ColumnPtr& implResCol,
                                const Offsets& src_offsets, const Offsets& replicate_offsets)
{
    if (implCol->size() == 0)
        return;

    auto * src_lc = typeid_cast<const ColumnLowCardinality *>(implCol.get());
    auto * lc = typeid_cast<const ColumnLowCardinality *>(implResCol.get());

    auto & index = const_cast<ColumnPtr& >(lc->getIndexesPtr());
    auto const & src_index = src_lc->getIndexesPtr();
    ColumnByteMap::replicate(src_index, index, src_offsets, replicate_offsets);

    implResCol = ColumnLowCardinality::create(src_lc->getDictionaryPtr(), index);
}

template <typename T>
void ColumnByteMap::replicateNumber(const ColumnPtr& implCol, ColumnPtr& implResCol,
                                const Offsets& src_offsets, const Offsets& replicate_offsets)
{
    size_t col_size = src_offsets.size();

    const typename ColumnVector<T>::Container& src_data =
        typeid_cast<const ColumnVector<T> &>(*implCol).getData();

    typename ColumnVector<T>::Container & res_data = typeid_cast<ColumnVector<T> &>(implResCol->assumeMutableRef()).getData();

    res_data.reserve(implCol->size() / col_size * replicate_offsets.back());

    Offset prev_replicate_offset = 0;
    Offset prev_data_offset = 0;
    Offset current_new_offset = 0;
    for (size_t i = 0; i < col_size; ++i)
    {
        size_t size_to_replicate = replicate_offsets[i] - prev_replicate_offset;
        size_t value_size = src_offsets[i] - prev_data_offset;
        for (size_t j = 0; j < size_to_replicate; ++j)
        {
            current_new_offset += value_size;
            res_data.resize(res_data.size() + value_size);

            memcpy(&res_data[res_data.size() - value_size], &src_data[prev_data_offset], value_size * sizeof(T));
        }

        prev_replicate_offset = replicate_offsets[i];
        prev_data_offset = src_offsets[i];
    }
}

template <typename T>
void ColumnByteMap::replicateArrayNumber(const ColumnPtr & implCol, ColumnPtr & implResCol, const Offsets & src_offsets, const Offsets & replicate_offsets)
{
    size_t col_size = src_offsets.size();
    if (col_size != replicate_offsets.size())
        throw Exception("Size of offsets doesn't match size of column.", ErrorCodes::SIZES_OF_COLUMNS_DOESNT_MATCH);

    const ColumnArray & src_array_num = typeid_cast<const ColumnArray &>(*implCol);
    const typename ColumnVector<T>::Container & src_data = typeid_cast<const ColumnVector<T> &>(src_array_num.getData()).getData();
    const Offsets & src_num_offsets = src_array_num.getOffsets();

    ColumnArray & res_array_num = typeid_cast<ColumnArray &>(implResCol->assumeMutableRef());
    typename ColumnVector<T>::Container & res_data = typeid_cast<ColumnVector<T> &>(res_array_num.getData()).getData();
    Offsets & res_num_offsets = res_array_num.getOffsets();

    res_data.reserve(src_data.size() / col_size * replicate_offsets.back());
    res_num_offsets.reserve(src_num_offsets.size() / col_size * replicate_offsets.back());

    Offset prev_replicate_offset = 0;
    Offset prev_src_offset = 0;
    Offset prev_src_num_offset = 0;
    Offset current_res_offset = 0;
    Offset current_res_num_offset = 0;

    for (size_t i = 0; i< col_size; ++i)
    {
        size_t size_to_replicate = replicate_offsets[i] - prev_replicate_offset;
        size_t value_size = src_offsets[i] - prev_src_offset;
        size_t sum_num_size = value_size == 0 ? 0 : (src_num_offsets[prev_src_offset + value_size - 1] - prev_src_num_offset);
        for (size_t j = 0; j < size_to_replicate; ++j)
        {
            current_res_offset += value_size;
            size_t prev_src_num_offset_local = prev_src_num_offset;
            for (size_t k = 0; k < value_size; ++k)
            {
                size_t num_size = src_num_offsets[k + prev_src_offset] - prev_src_num_offset_local;
                current_res_num_offset += num_size;
                res_num_offsets.push_back(current_res_num_offset);
                prev_src_num_offset_local += num_size;
            }

            if (sum_num_size)
            {
                res_data.resize(res_data.size() + sum_num_size);
                memcpy(&res_data[res_data.size() - sum_num_size], &src_data[prev_src_num_offset], sum_num_size * sizeof(T));
            }
        }

        prev_replicate_offset = replicate_offsets[i];
        prev_src_offset = src_offsets[i];
        prev_src_num_offset += sum_num_size;
    }
}

void ColumnByteMap::replicateArrayString(const ColumnPtr & implCol, ColumnPtr & implResCol, const Offsets & src_offsets, const Offsets & replicate_offsets)
{
    size_t col_size = src_offsets.size();
    if (col_size != replicate_offsets.size())
        throw Exception("Size of offsets doesn't match size of column.", ErrorCodes::SIZES_OF_COLUMNS_DOESNT_MATCH);

    const ColumnArray & src_array_string = typeid_cast<const ColumnArray &>(*implCol);
    const ColumnString & src_string = typeid_cast<const ColumnString &>(src_array_string.getData());
    const ColumnString::Chars & src_chars = src_string.getChars();
    const Offsets & src_array_offsets = src_array_string.getOffsets();
    const Offsets & src_string_offsets = src_string.getOffsets();

    ColumnArray & res_array_string = typeid_cast<ColumnArray &>(implResCol->assumeMutableRef());
    ColumnString & res_string = typeid_cast<ColumnString &>(res_array_string.getData());
    ColumnString::Chars & res_chars = res_string.getChars();
    Offsets & res_array_offsets = res_array_string.getOffsets();
    Offsets & res_string_offsets = res_string.getOffsets();

    res_chars.reserve(src_chars.size() / col_size * replicate_offsets.back());
    res_array_offsets.reserve(src_array_offsets.size() / col_size * replicate_offsets.back());
    res_string_offsets.reserve(src_string_offsets.size() / col_size * replicate_offsets.back());

    Offset prev_replicate_offset = 0;
    Offset prev_src_offset = 0;
    Offset prev_src_array_offset = 0;
    Offset prev_src_string_offset = 0;
    Offset current_res_array_offset = 0;
    Offset current_res_string_offset = 0;

    for (size_t i = 0; i< col_size; ++i)
    {
        size_t size_to_replicate = replicate_offsets[i] - prev_replicate_offset;
        size_t value_size = src_offsets[i] - prev_src_offset;
        size_t sum_string_size = value_size == 0 ? 0 : (src_array_offsets[prev_src_offset + value_size - 1] - prev_src_array_offset);
        size_t sum_chars_size = sum_string_size == 0 ? 0 : (src_string_offsets[prev_src_array_offset + sum_string_size - 1] - prev_src_string_offset);

        for (size_t j = 0; j < size_to_replicate; ++j)
        {
            size_t prev_src_array_offset_local = prev_src_array_offset;
            size_t prev_src_string_offset_local = prev_src_string_offset;
            for (size_t k = 0; k < value_size; ++k)
            {
                // Add array offset
                size_t string_size = src_array_offsets[k + prev_src_offset] - prev_src_array_offset_local;
                current_res_array_offset += string_size;
                res_array_offsets.push_back(current_res_array_offset);

                // Add string offset
                for (size_t l = 0; l < string_size; ++l)
                {
                    size_t chars_size = src_string_offsets[l + prev_src_array_offset_local] - prev_src_string_offset_local;
                    current_res_string_offset += chars_size;
                    res_string_offsets.push_back(current_res_string_offset);
                    prev_src_string_offset_local += chars_size;
                }

                prev_src_array_offset_local += string_size;
            }

            if (sum_chars_size)
            {
                res_chars.resize(res_chars.size() + sum_chars_size);
                memcpySmallAllowReadWriteOverflow15(&res_chars[res_chars.size() - sum_chars_size], &src_chars[prev_src_string_offset], sum_chars_size);
            }
        }

        prev_replicate_offset = replicate_offsets[i];
        prev_src_offset = src_offsets[i];
        prev_src_array_offset += sum_string_size;
        prev_src_string_offset += sum_chars_size;
    }

}

void ColumnByteMap::replicateArray(const ColumnPtr & implCol, ColumnPtr & implResCol, const Offsets & offsets, const Offsets & replicate_offsets)
{
    // Handle implicit key column
    const ColumnArray & col_array = typeid_cast<const ColumnArray &>(*implCol);
    if (typeid_cast<const ColumnUInt8 *>(col_array.getDataPtr().get()))
    {
        ColumnByteMap::replicateArrayNumber<UInt8>(implCol, implResCol, offsets, replicate_offsets);
    }
    else if (typeid_cast<const ColumnUInt16 *>(col_array.getDataPtr().get()))
    {
        ColumnByteMap::replicateArrayNumber<UInt16>(implCol, implResCol, offsets, replicate_offsets);
    }
    else if (typeid_cast<const ColumnUInt32 *>(col_array.getDataPtr().get()))
    {
        ColumnByteMap::replicateArrayNumber<UInt32>(implCol, implResCol, offsets, replicate_offsets);
    }
    else if (typeid_cast<const ColumnUInt64 *>(col_array.getDataPtr().get()))
    {
        ColumnByteMap::replicateArrayNumber<UInt64>(implCol, implResCol, offsets, replicate_offsets);
    }
    else if (typeid_cast<const ColumnInt8 *>(col_array.getDataPtr().get()))
    {
        ColumnByteMap::replicateArrayNumber<Int8>(implCol, implResCol, offsets, replicate_offsets);
    }
    else if (typeid_cast<const ColumnInt16 *>(col_array.getDataPtr().get()))
    {
        ColumnByteMap::replicateArrayNumber<Int16>(implCol, implResCol, offsets, replicate_offsets);
    }
    else if (typeid_cast<const ColumnInt32 *>(col_array.getDataPtr().get()))
    {
        ColumnByteMap::replicateArrayNumber<Int32>(implCol, implResCol, offsets, replicate_offsets);
    }
    else if (typeid_cast<const ColumnInt64 *>(col_array.getDataPtr().get()))
    {
        ColumnByteMap::replicateArrayNumber<Int64>(implCol, implResCol, offsets, replicate_offsets);
    }
    else if (typeid_cast<const ColumnFloat32 *>(col_array.getDataPtr().get()))
    {
        ColumnByteMap::replicateArrayNumber<Float32>(implCol, implResCol, offsets, replicate_offsets);
    }
    else if (typeid_cast<const ColumnFloat64 *>(col_array.getDataPtr().get()))
    {
        ColumnByteMap::replicateArrayNumber<Float64>(implCol, implResCol, offsets, replicate_offsets);
    }
    else if (typeid_cast<const ColumnString *>(col_array.getDataPtr().get()))
    {
        ColumnByteMap::replicateArrayString(implCol, implResCol, offsets, replicate_offsets);
    }
    else if (typeid_cast<const ColumnNullable *>(col_array.getDataPtr().get()))
    {
        ColumnByteMap::replicateNullable(implCol, implResCol, offsets, replicate_offsets);
    }
    else
    {
        throw Exception("ColumnByteMap doesn't support this implicit type", ErrorCodes::NOT_IMPLEMENTED);
    }
}

void ColumnByteMap::replicateString(const ColumnPtr& implCol, ColumnPtr& implResCol,
                                const Offsets& src_offsets, const Offsets& replicate_offsets)
{
    size_t col_size = src_offsets.size();
    const ColumnString & src_string = typeid_cast<const ColumnString &>(*implCol);
    const ColumnString::Chars & src_chars = src_string.getChars();
    const Offsets & src_string_offsets = src_string.getOffsets();

    ColumnString::Chars& res_chars = typeid_cast<ColumnString &>(implResCol->assumeMutableRef()).getChars();
    Offsets & res_string_offsets = typeid_cast<ColumnString &>(implResCol->assumeMutableRef()).getOffsets();

    res_chars.reserve(src_chars.size() / col_size * replicate_offsets.back());
    res_string_offsets.reserve(src_string_offsets.size() / col_size * replicate_offsets.back());

    Offset prev_replicate_offset = 0;
    Offset prev_src_offset = 0;
    Offset prev_src_string_offset = 0;
    Offset current_res_offset = 0;
    Offset current_res_string_offset = 0;

    for (size_t i = 0; i<col_size; ++i)
    {
        size_t size_to_replicate = replicate_offsets[i] - prev_replicate_offset;
        size_t value_size = src_offsets[i] - prev_src_offset;
        size_t sum_chars_size = value_size == 0 ? 0 : (src_string_offsets[prev_src_offset + value_size - 1] - prev_src_string_offset);

        for (size_t j = 0; j< size_to_replicate; ++j)
        {
            current_res_offset += value_size;
            size_t prev_src_string_offset_local = prev_src_string_offset;
            for (size_t k = 0; k < value_size; ++k)
            {
                /// Size of one row.
                size_t chars_size = src_string_offsets[k + prev_src_offset] - prev_src_string_offset_local;
                current_res_string_offset += chars_size;
                res_string_offsets.push_back(current_res_string_offset);
                prev_src_string_offset_local += chars_size;
            }
            /// Copies the characters of the array of rows.
            res_chars.resize(res_chars.size() + sum_chars_size);
            memcpySmallAllowReadWriteOverflow15(
                &res_chars[res_chars.size() - sum_chars_size], &src_chars[prev_src_string_offset], sum_chars_size);
        }

        prev_replicate_offset = replicate_offsets[i];
        prev_src_offset = src_offsets[i];
        prev_src_string_offset += sum_chars_size;
    }
}

void ColumnByteMap::replicateNullable(const ColumnPtr& implCol, ColumnPtr& implResCol,
                                         const Offsets& offsets, const Offsets& replicate_offsets)
{
    const ColumnNullable & nullable_elems = static_cast<const ColumnNullable &>(*implCol);
    ColumnNullable & nullable_res = static_cast<ColumnNullable &>(implResCol->assumeMutableRef());

    auto& src_nested_column = nullable_elems.getNestedColumnPtr();
    auto& src_null_ind = nullable_elems.getNullMapColumnPtr();

    auto& res_nested_column = nullable_res.getNestedColumnPtr();
    auto& res_null_ind = nullable_res.getNullMapColumnPtr();

    ColumnByteMap::replicate(src_nested_column, res_nested_column, offsets, replicate_offsets);

    ColumnByteMap::replicate(src_null_ind, res_null_ind, offsets, replicate_offsets);
}

MutableColumns ColumnByteMap::scatter(ColumnIndex num_columns, const Selector & selector) const
{
    return scatterImpl<ColumnByteMap>(num_columns, selector);
}

int ColumnByteMap::compareAt([[maybe_unused]]size_t n,
                         [[maybe_unused]]size_t m,
                         [[maybe_unused]]const IColumn & rhs,
                         [[maybe_unused]]int nan_direction_hint) const
{
    throw Exception("ColumnByteMap::compareAt not implemented yet!", ErrorCodes::NOT_IMPLEMENTED);
}

void ColumnByteMap::compareColumn(const IColumn &, size_t,
                                PaddedPODArray<UInt64> *, PaddedPODArray<Int8> &,
                                int, int) const
{
    throw Exception("ColumnByteMap::compareColumn not implemented yet!", ErrorCodes::NOT_IMPLEMENTED);
}

void ColumnByteMap::gather(ColumnGathererStream & gatherer)
{
    gatherer.gather(*this);
}

void ColumnByteMap::reserve(size_t n)
{
    getOffsets().reserve(n);
    getKey().reserve(n); // conservative reserve n slot
    getValue().reserve(n);
}

size_t ColumnByteMap::byteSize() const
{
    return getKey().byteSize() + getValue().byteSize() + sizeof(getOffsets()[0]) * size();
}

size_t ColumnByteMap::byteSizeAt(size_t n) const
{
    return getKey().byteSizeAt(n) + getValue().byteSizeAt(n) + sizeof(getOffsets()[0]);
}



size_t ColumnByteMap::allocatedBytes() const
{
    return getKey().allocatedBytes() + getValue().allocatedBytes() + getOffsets().allocated_bytes();
}

void ColumnByteMap::getExtremes([[maybe_unused]] Field & min, [[maybe_unused]] Field & max) const
{
    throw Exception("ColumnByteMap::getExtremes not implemented yet!", ErrorCodes::NOT_IMPLEMENTED);
}

void ColumnByteMap::protect()
{
    getKey().protect();
    getValue().protect();
    getOffsets().protect();
}


void ColumnByteMap::forEachSubcolumn(ColumnCallback callback)
{
    callback(offsets);
    callback(key_column);
    callback(value_column);
}

namespace
{
    ColumnPtr makeNullableForMapValue(const ColumnPtr & column)
    {
        /// When column is low cardinality, its dictionary type must be nullable, see more detail in DataTypeLowCardinality::canBeMapValueType()
        return column->lowCardinality() ? column : makeNullable(column);
    }

    /**
     * Column could be in the following two mode:
     * 1. Column is low cardinality type and its dictionary type is nullable, see more detail in DataTypeLowCardinality::canBeMapValueType()
     * 2. Column is other type column, i.e. String, Numbers which can be inside nullable and need to be wrapped in Nullable again for querying implicit column.
     * For the first mode, we can use value column type directly when querying implicit column.
     */
    bool needAddNullable(const ColumnPtr & column) { return !column->lowCardinality(); }

    /**
     * Check if new column which will be inserted has obeyed the above rules.
     */
    void checkNewColumnCanBeInserted(const ColumnPtr & value_column, bool add_nullable, const IColumn & new_column)
    {
        if (add_nullable)
        {
            const auto * nullable_value_column = dynamic_cast<const ColumnNullable *>(&new_column);
            if (!nullable_value_column)
            {
                throw Exception(
                    ErrorCodes::BAD_ARGUMENTS,
                    "The new column can not be cast to nullable, its name is {}, map value column name is {}",
                    new_column.getName(),
                    value_column->getName());
            }
            if (nullable_value_column->getNestedColumnPtr()->getDataType() != value_column->getDataType())
            {
                throw Exception(
                    ErrorCodes::BAD_ARGUMENTS,
                    "The nested column type {} of nullable column is not equal to map value column type {}",
                    nullable_value_column->getNestedColumnPtr()->getName(),
                    value_column->getName());
            }
        }
        else if (new_column.getDataType() != value_column->getDataType())
        {
            throw Exception(
                ErrorCodes::BAD_ARGUMENTS,
                "The new column type {} is not equal to map value column type {}",
                new_column.getName(),
                value_column->getName());
        }
    }
}

/**
 * Generic implementation of get implicit value column based on key value.
 * TODO: specialize this function for Number type and String type.
 */
ColumnPtr ColumnByteMap::getValueColumnByKey(const StringRef & key, size_t rows_to_read) const
{
    size_t col_size = size();

    const Offsets & offsets_ = getOffsets();
    /// Mark whether res has been wrapped nullable explicitly.
    bool add_nullable = needAddNullable(value_column);
    ColumnPtr res = createEmptyImplicitColumn();

    if (col_size == 0)
        return res;
    auto & res_col = res->assumeMutableRef();
    res_col.reserve(col_size);
    size_t offset = 0;

    size_t row_size = offsets_[0];
    bool found_key = false;

    size_t rows = rows_to_read == 0 ? col_size : std::min(rows_to_read, col_size);
    for (size_t i = 0; i < rows; ++i)
    {
        found_key = false;
        offset = offsets_[i - 1];
        row_size = offsets_[i] - offset;

        for (size_t r = 0; r < row_size; ++r)
        {
            auto tmp_key = key_column->getDataAt(offset + r);
            if (tmp_key == key)
            {
                found_key = true;
                /// Handle lowcardinality type
                if (!add_nullable)
                    res_col.insertFrom(*value_column, offset + r);
                else
                {
                    static_cast<ColumnNullable &>(res_col).getNestedColumn().insertFrom(*value_column, offset + r);
                    static_cast<ColumnNullable &>(res_col).getNullMapData().push_back(0);
                }
                break;
            }
        }

        if (!found_key)
        {
            res_col.insert(Null());
        }
    }

    return res;
}

void ColumnByteMap::constructAllImplicitColumns(
    std::unordered_map<StringRef, String> & key_name_map, std::unordered_map<StringRef, ColumnPtr> & value_columns) const
{
    /// This is an one-pass algorithm, to minimize the cost of traversing all key-value pairs:
    /// 1) Names of key and implicit value columns are created lazily;
    /// 2) NULLs are filled as need before non-null values inserted or in the end.

    const Offsets & offsets_ = getOffsets();
    /// Mark whether res has been wrapped nullable explicitly
    bool add_nullable = needAddNullable(value_column);
    size_t col_size = size();

    for (size_t r = 0; r < col_size; ++r)
    {
        const size_t offset = offsets_[r - 1]; /// -1th index is Ok, see PaddedPODArray
        const size_t curr_num_pair = offsets_[r] - offset;

        for (size_t p = 0; p < curr_num_pair; ++p)
        {
            auto tmp_key = key_column->getDataAt(offset + p);
            auto iter = value_columns.find(tmp_key);

            if (iter == value_columns.end())
            {
                key_name_map[tmp_key] = applyVisitor(DB::FieldVisitorToString(), (*key_column)[offset + p]);
                ColumnPtr new_column = createEmptyImplicitColumn();
                new_column->assumeMutableRef().reserve(col_size);

                iter = value_columns.try_emplace(tmp_key, new_column).first;
            }

            auto & impl_value_column = iter->second->assumeMutableRef();
            /// Fill NULLs as need
            while (impl_value_column.size() < r)
                impl_value_column.insert(Null());

            /// Handle duplicated keys in map
            if (!add_nullable)
                impl_value_column.insertFrom(*value_column, offset + r);
            else
            {
                static_cast<ColumnNullable &>(impl_value_column).getNestedColumn().insertFrom(*value_column, offset + p);
                static_cast<ColumnNullable &>(impl_value_column).getNullMapData().push_back(0);
            }
        }
    }

    /// Fill NULLs until all columns reach the same size
    for (auto & [k, column] : value_columns)
    {
        auto & impl_value_column = column->assumeMutableRef();
        while (impl_value_column.size() < col_size)
            impl_value_column.insert(Null());
    }
}

/**
 * This routine will reconsturct MAP column based on its implicit columns.
 * \param impl_key_values is the collection of {key_name, {offset, key_column}}
 * and this routine will be compatible with non-zero offset. key_name has been escaped.
 * non-zero offset could happen for scenario that map column and implicit column are both
 * referenced in the same query, e.g.
 *                  select map from table where map{'key'} =1
 * In the above case, MergeTreeReader will use the same stream __map__%27key%27 for
 * both keys, while reconstructing(append) map column, size of implicit column
 * __map__%27key%27 was accumulated.
 */
void ColumnByteMap::fillByExpandedColumns(
    const DataTypeByteMap & map_type, const std::map<String, std::pair<size_t, const IColumn *>> & impl_key_values)
{
    // Append to ends of this ColumnByteMap
    if (impl_key_values.empty())
        return;

    if (impl_key_values.begin()->second.second->size() < impl_key_values.begin()->second.first)
    {
        throw Exception(
            "MAP implicit key size is slow than offset " + toString(impl_key_values.begin()->second.second->size()) + " "
                + toString(impl_key_values.begin()->second.first),
            ErrorCodes::LOGICAL_ERROR);
    }

    size_t rows = impl_key_values.begin()->second.second->size() - impl_key_values.begin()->second.first;


    IColumn & key_col = getKey();
    IColumn & value_col = getValue();
    Offsets & offsets_ = getOffsets();
    size_t all_keys_num = impl_key_values.size();
    std::vector<Field> keys;
    keys.reserve(all_keys_num);

    auto & key_type = map_type.getKeyType();

    //Intepreter key columns
    for (auto & kv : impl_key_values)
    {
        keys.push_back(key_type->stringToVisitorField(kv.first));
        // Sanity check that all implicit columns(plus offset) are the same size
        if (rows + kv.second.first != kv.second.second->size())
        {
            throw Exception(
                "implicit column is with different size " + toString(kv.second.second->size()) + " " + toString(rows + kv.second.first),
                ErrorCodes::LOGICAL_ERROR);
        }
    }

    std::vector<const IColumn *> nested_columns;
    /// Mark whether res has been wrapped nullable explicitly in order to use insertFrom method instead of insert and [] method to improve performance.
    bool add_nullable = needAddNullable(value_column);
    /// Check type of input columns and prepare nest columns for the columns whose type is neither nullable or low cardinality.
    for (auto it = impl_key_values.begin(); it != impl_key_values.end(); ++it)
    {
        checkNewColumnCanBeInserted(value_column, add_nullable, *it->second.second);
        if (add_nullable)
        {
            const auto & inner_value_column = typeid_cast<const ColumnNullable &>(*it->second.second);
            nested_columns.emplace_back(&inner_value_column.getNestedColumn());
        }
    }
    for (size_t i = 0; i < rows; i++)
    {
        size_t num_kv_pairs = 0;
        size_t iter_idx = 0;
        for (auto it = impl_key_values.begin(); it != impl_key_values.end(); ++it, ++iter_idx)
        {
            size_t impl_offset = it->second.first;
            const IColumn & impl_value_col = *it->second.second;

            // Ignore those input whose value is NULL.
            if (impl_value_col.isNullAt(i + impl_offset))
                continue;

            key_col.insert(keys[iter_idx]);
            if (add_nullable)
                value_col.insertFrom(*nested_columns[iter_idx], i + impl_offset);
            else
                value_col.insertFrom(impl_value_col, i + impl_offset);
            num_kv_pairs++;
        }
        offsets_.push_back((offsets_.size() == 0 ? 0 : offsets_.back()) + num_kv_pairs);
    }
}

void ColumnByteMap::removeKeys(const NameSet & keys)
{
    size_t col_size = size();
    if (col_size == 0)
        return;

    ColumnPtr key_res = key_column->cloneEmpty();
    ColumnPtr value_res = value_column->cloneEmpty();
    ColumnPtr offset_res = offsets->cloneEmpty();

    const Offsets & offsets_ = getOffsets();

    auto & key_res_col = key_res->assumeMutableRef();
    auto & value_res_col = value_res->assumeMutableRef();
    Offsets& offset_res_col = static_cast<ColumnOffsets &>(offset_res->assumeMutableRef()).getData();
    size_t offset = 0;

    size_t row_size = offsets_[0];

    for (size_t i = 0; i < col_size; ++i)
    {
        offset = offsets_[i - 1];
        row_size = offsets_[i] - offset;

        size_t num_kv_pairs = 0;
        for (size_t r = 0; r < row_size; ++r)
        {
            auto tmp_key = key_column->getDataAt(offset + r).toString();
            if (!keys.count(tmp_key))
            {
                key_res_col.insertFrom(*key_column, offset + r);
                value_res_col.insertFrom(*value_column, offset + r);
                num_kv_pairs++;
            }
        }
        offset_res_col.push_back((offset_res_col.size() == 0 ? 0 : offset_res_col.back()) + num_kv_pairs);
    }

    key_column = std::move(key_res);
    value_column = std::move(value_res);
    offsets = std::move(offset_res);
}

/**
 * Insert implicit map column from outside, only when the current ColumnByteMap has the same size with implicit columns.
 * Currently, this method works for ingestion column feature.
 *
 * @param implicit_columns it should has the same type with return value of ColumnByteMap::getValueColumnByKey. Otherwise, throw exception.
 */
void ColumnByteMap::insertImplicitMapColumns(const std::unordered_map<String, ColumnPtr> & implicit_columns)
{
    if (implicit_columns.empty())
        return;

    size_t origin_size = size();
    ColumnPtr key_res = key_column->cloneEmpty();
    ColumnPtr value_res = value_column->cloneEmpty();
    ColumnPtr offset_res = offsets->cloneEmpty();
    const Offsets & offsets_ = getOffsets();
    auto & key_res_col = key_res->assumeMutableRef();
    auto & value_res_col = value_res->assumeMutableRef();
    Offsets & offset_res_col = static_cast<ColumnOffsets &>(offset_res->assumeMutableRef()).getData();

    /// Mark whether res has been wrapped nullable explicitly.
    bool add_nullable = needAddNullable(value_column);
    std::vector<const IColumn *> nested_columns;
    /// Check type of input columns and prepare nested columns for the columns whose type is neither nullable or low cardinality.
    for (auto it = implicit_columns.begin(); it != implicit_columns.end(); ++it)
    {
        checkNewColumnCanBeInserted(value_column, add_nullable, *it->second);
        if (add_nullable)
        {
            const auto & inner_value_column = typeid_cast<const ColumnNullable &>(*it->second);
            nested_columns.emplace_back(&inner_value_column.getNestedColumn());
        }
    }

    size_t add_size = implicit_columns.begin()->second->size();
    if (origin_size != 0 && origin_size != add_size)
        throw Exception(
            ErrorCodes::LOGICAL_ERROR, "Expect equal size between map and implicit columns, but got {}/{}", origin_size, add_size);

    size_t offset = 0;
    size_t row_size = offsets_[0];
    /// Handle each row data
    for (size_t i = 0; i < add_size; ++i)
    {
        /// Handle the case that origin column is empty
        offset = origin_size == 0 ? 0 : offsets_[i - 1];
        row_size = origin_size == 0 ? 0 : (offsets_[i] - offset);

        size_t num_kv_pairs = 0;
        for (size_t r = 0; r < row_size; ++r)
        {
            auto tmp_key = key_column->getDataAt(offset + r).toString();
            if (!implicit_columns.count(tmp_key))
            {
                key_res_col.insertFrom(*key_column, offset + r);
                value_res_col.insertFrom(*value_column, offset + r);
                num_kv_pairs++;
            }
        }

        size_t id = 0;
        for (auto it = implicit_columns.begin(); it != implicit_columns.end(); ++it, ++id)
        {
            /// we ignore null value due to it's meaningless.
            if (!it->second->isNullAt(i))
            {
                key_res_col.insert(Field(it->first));
                value_res_col.insertFrom(add_nullable ? *nested_columns[id]: *it->second, i);
                num_kv_pairs++;
            }
        }
        offset_res_col.push_back((offset_res_col.size() == 0 ? 0 : offset_res_col.back()) + num_kv_pairs);
    }

    key_column = std::move(key_res);
    value_column = std::move(value_res);
    offsets = std::move(offset_res);
}

ColumnPtr ColumnByteMap::createEmptyImplicitColumn() const
{
    return makeNullableForMapValue(value_column->cloneEmpty());
}

// this optimization mostly not work for Map type
bool ColumnByteMap::hasEqualValues() const
{
    return false;
}

void ColumnByteMap::updateWeakHash32(WeakHash32 &) const
{
    throw Exception("Map doesn't support updateWeakHash32", ErrorCodes::NOT_IMPLEMENTED);
}

void ColumnByteMap::updateHashFast(SipHash & hash) const
{
    offsets->updateHashFast(hash);
    key_column->updateHashFast(hash);
    value_column->updateHashFast(hash);
}

}
