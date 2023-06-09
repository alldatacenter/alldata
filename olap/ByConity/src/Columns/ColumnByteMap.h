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

#pragma once

#include <Columns/IColumn.h>
#include <Columns/ColumnVector.h>
#include <Columns/ColumnArray.h>
#include <Columns/ColumnTuple.h>
#include <DataTypes/DataTypeByteMap.h>
#include <map>

namespace DB
{

/**
 * Column, runtime ColumnByteMap is implicited stored as two implicit columns(key, value) and one offset column to count # k-v pair per row.
 */
class ColumnByteMap final : public COWHelper<IColumn, ColumnByteMap>
{
private:
    friend class COWHelper<IColumn, ColumnByteMap>;

    WrappedPtr key_column;
    WrappedPtr value_column;
    WrappedPtr offsets;

    /** Create an map column with key, value, offsets information */
    ColumnByteMap(MutableColumnPtr && key_column, MutableColumnPtr && value_column, MutableColumnPtr && offsets_column);
    /** Create an empty column of Map with key, value information */
    explicit ColumnByteMap(MutableColumnPtr && key_column, MutableColumnPtr && value_column);
    ColumnByteMap(const ColumnByteMap &) = default;

public:
    /** Create immutable column using immutable arguments. This arguments may be shared with other columns.
      * Use IColumn::mutate in order to make mutable column and mutate shared nested columns.
      */
    using Base = COWHelper<IColumn, ColumnByteMap>;

    using ColumnOffsets = ColumnVector<Offset>;

    static Ptr create(const ColumnPtr & key_column, const ColumnPtr & value_column, const ColumnPtr & offsets_column)
    {
        return ColumnByteMap::create(key_column->assumeMutable(), value_column->assumeMutable(), offsets_column->assumeMutable());
    }


    static Ptr create(const ColumnPtr & key_column, const ColumnPtr & value_column)
    {
        return ColumnByteMap::create(key_column->assumeMutable(), value_column->assumeMutable());
    }

    template <typename ... Args, typename = typename std::enable_if<IsMutableColumns<Args ...>::value>::type>
    static MutablePtr create(Args &&... args) { return Base::create(std::forward<Args>(args)...); }

    std::string getName() const override;
    const char * getFamilyName() const override { return "Map"; }
    TypeIndex getDataType() const override { return TypeIndex::ByteMap; }

    MutableColumnPtr cloneEmpty() const override;

    size_t size() const override
    {
        return getOffsets().size();
    }

    size_t ALWAYS_INLINE offsetAt(size_t i) const {return getOffsets()[i-1];}
    size_t ALWAYS_INLINE sizeAt(size_t i) const {return getOffsets()[i] - getOffsets()[i-1];}

    Field operator[](size_t n) const override;
    void get(size_t n, Field & res) const override;

    StringRef getDataAt(size_t n) const override;
    void insertData(const char * pos, size_t length) override;
    void insert(const Field & x) override;
    void insertFrom(const IColumn & src_, size_t n) override;
    void insertDefault() override;
    void popBack(size_t n) override;
    StringRef serializeValueIntoArena(size_t n, Arena & arena, char const *& begin) const override;
    const char * deserializeAndInsertFromArena(const char * pos) override;

    const char * skipSerializedInArena(const char * pos) const override;
    void updateHashWithValue(size_t n, SipHash & hash) const override;
    void updateWeakHash32(WeakHash32 & hash) const override;
    void updateHashFast(SipHash & hash) const override;

    void insertRangeFrom(const IColumn & src, size_t start, size_t length) override;
    void insertRangeSelective(const IColumn & src, const IColumn::Selector & selector, size_t selector_start, size_t length) override;
    ColumnPtr filter(const Filter & filt, ssize_t result_size_hint) const override;
    ColumnPtr permute(const Permutation & perm, size_t limit) const override;
    ColumnPtr index(const IColumn & indexes, size_t limit) const override;
    ColumnPtr replicate(const Offsets & offsets) const override;
    MutableColumns scatter(ColumnIndex num_columns, const Selector & selector) const override;
    void gather(ColumnGathererStream & gatherer_stream) override;
    int compareAt(size_t n, size_t m, const IColumn & rhs, int nan_direction_hint) const override;
    void compareColumn(const IColumn & rhs, size_t rhs_row_num,
                       PaddedPODArray<UInt64> * row_indexes, PaddedPODArray<Int8> & compare_results,
                       int direction, int nan_direction_hint) const override;
    bool hasEqualValues() const override;
    void getExtremes(Field & min, Field & max) const override;
    void getPermutation(bool reverse, size_t limit, int nan_direction_hint, Permutation & res) const override;
    void updatePermutation(bool reverse, size_t limit, int nan_direction_hint, IColumn::Permutation & res, EqualRanges & equal_range) const override;
    void reserve(size_t n) override;
    size_t byteSize() const override;
    size_t byteSizeAt(size_t n) const override;
    size_t allocatedBytes() const override;
    void protect() override;

    /** Read limited implicit column for the given key. */
    ColumnPtr getValueColumnByKey(const StringRef & key, size_t rows_to_read = 0) const;

    /** Read all data and construct implicit columns */
    void constructAllImplicitColumns(
        std::unordered_map<StringRef, String> & key_name_map, std::unordered_map<StringRef, ColumnPtr> & value_columns) const;

    void fillByExpandedColumns(const DataTypeByteMap &, const std::map<String, std::pair<size_t, const IColumn *>> &);

    /**
     * Remove data of map keys from the column.
     * Note: currently, this mothod is only used in the case that handling command of "clear map key" and the type of part is in-memory. In other on-disk part type(compact and wide), it can directly handle the files.
     */
    void removeKeys(const NameSet & keys);

    void insertImplicitMapColumns(const std::unordered_map<String, ColumnPtr> & implicit_columns);

    /** Access embeded columns*/
    IColumn & getKey() {return key_column->assumeMutableRef();}
    const IColumn & getKey() const {return *key_column;}

    ColumnPtr& getKeyPtr() {return key_column;}
    const ColumnPtr& getKeyPtr() const {return key_column;}

    IColumn & getValue() {return value_column->assumeMutableRef();}
    const IColumn & getValue() const { return *value_column; }
    /** Make column nullable for implicit column */
    ColumnPtr createEmptyImplicitColumn() const;

    ColumnPtr getKeyStorePtr() const { return ColumnArray::create(getKeyPtr(), getOffsetsPtr()); }
    ColumnPtr getValueStorePtr() const { return ColumnArray::create(getValuePtr(), getOffsetsPtr()); }

    ColumnPtr& getValuePtr() {return value_column;}
    const ColumnPtr& getValuePtr() const {return value_column;}

    ColumnPtr& getOffsetsPtr() {return offsets;}
    const ColumnPtr& getOffsetsPtr() const {return offsets;}


    IColumn & getOffsetsColumn() {return offsets->assumeMutableRef();}
    const IColumn & getOffsetsColumn() const {return *offsets;}

    ColumnPtr getNestedColumnPtr() const
    {
        return ColumnArray::create(ColumnTuple::create(Columns{key_column, value_column}), offsets);
    }

    Offsets& ALWAYS_INLINE getOffsets()
    {
        return static_cast<ColumnOffsets&>(offsets->assumeMutableRef()).getData();
    }

    const Offsets& ALWAYS_INLINE getOffsets() const
    {
        return static_cast<const ColumnOffsets&>(*offsets).getData();
    }

    void forEachSubcolumn(ColumnCallback callback) override;

    /// Specializations for the filter function.
    template <typename T>
    static void filterNumber(const ColumnPtr& implCol, ColumnPtr& implResCol, const Offsets& offsets, const Filter & filt, ssize_t result_size_hint);
    static void filterString(const ColumnPtr& implCol, ColumnPtr& implResCol, const Offsets& offsets, const Filter & filt, ssize_t result_size_hint);
    static void filterNullable(const ColumnPtr& implCol, ColumnPtr& implResCol, const Offsets& offsets, const Filter & filt, ssize_t result_size_hint);
    static void filterArray(const ColumnPtr& implCol, ColumnPtr& implResCol, const Offsets& offsets, const Filter & filt, ssize_t result_size_hint);
    static void filter(const ColumnPtr& implCol, ColumnPtr& implResCol, const Offsets& offsets, const Filter& filt, ssize_t result_size_hint);
    static void filterLowCardinality(const ColumnPtr& implCol, ColumnPtr& implResCol, const Offsets& offsets, const Filter & filt, ssize_t result_size_hint);

    /// Specializations for the filter function.
    template <typename T>
    static void replicateNumber(const ColumnPtr& implCol, ColumnPtr& implResCol, const Offsets& offsets, const Offsets& replicate_offsets);
    static void replicateString(const ColumnPtr& implCol, ColumnPtr& implResCol, const Offsets& offsets, const Offsets& replicate_offsets);
    static void replicateNullable(const ColumnPtr& implCol, ColumnPtr& implResCol, const Offsets& offsets, const Offsets& replicate_offsets);
    static void replicateArray(const ColumnPtr& implCol, ColumnPtr& implResCol, const Offsets& offsets, const Offsets& replicate_offsets);
    template <typename T>
    static void replicateArrayNumber(const ColumnPtr& implCol, ColumnPtr& implResCol, const Offsets& offsets, const Offsets& replicate_offsets);
    static void replicateArrayString(const ColumnPtr& implCol, ColumnPtr& implResCol, const Offsets& offsets, const Offsets& replicate_offsets);
    static void replicateLowCardinality(const ColumnPtr& implCol, ColumnPtr& implResCol, const Offsets& src_offsets, const Offsets& replicate_offsets);

    static void replicate(const ColumnPtr& implCol, ColumnPtr& implResCol, const Offsets& offsets, const Offsets& replicate_offsets);

};

}
