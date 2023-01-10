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

#include <cstring>
#include <cassert>

#include <Columns/IColumn.h>
#include <Columns/IColumnImpl.h>
#include <Common/PODArray.h>
#include <Common/SipHash.h>
#include <Common/memcpySmall.h>
#include <Common/memcmpSmall.h>
#include <Core/Field.h>


class Collator;


namespace DB
{

/** Column for BitMap values.
  */
class ColumnBitMap64 final : public COWHelper<IColumn, ColumnBitMap64>
{
public:
    using Chars = PaddedPODArray<UInt8>;
    using BitMaps = std::unordered_map<Int64, BitMap64>;

private:
    friend class COWHelper<IColumn, ColumnBitMap64>;

    /// Binary bytes of bitmaps, and all bitmap are placed contiguously.
    /// For convenience, every bitmaps ends with terminating zero byte. It's familiar with ColumnString
    Chars chars;

    /// Records the index of chars of the last byte of each bitmap.
    /// Last value maps to the end of all chars (is the size of all chars).
    /// refers to sizeAt() to obtain how to get size of each bitmap via offsets
    Offsets offsets;

    /// A simple cache of bitmaps
    BitMaps bitmaps;

    size_t ALWAYS_INLINE offsetAt(ssize_t i) const { return offsets[i - 1]; }

    /// Size of i-th element, including terminating zero.
    size_t ALWAYS_INLINE sizeAt(ssize_t i) const { return offsets[i] - offsets[i - 1]; }

    template <bool positive>
    struct Cmp;

    template <bool positive>
    struct CmpWithCollation;

    ColumnBitMap64() = default;
    ColumnBitMap64(const ColumnBitMap64 & src);

    template <typename Comparator>
    void getPermutationImpl(size_t limit, Permutation & res, Comparator cmp) const;

    template <typename Comparator>
    void updatePermutationImpl(size_t limit, Permutation & res, EqualRanges & equal_ranges, Comparator cmp) const;

public:
    const char * getFamilyName() const override { return "BitMap64"; }

    TypeIndex getDataType() const override { return TypeIndex::BitMap64; }

    size_t size() const override
    {
        return offsets.size();
    }

    size_t byteSize() const override
    {
        return chars.size() + offsets.size() * sizeof(offsets[0]);
    }

    size_t byteSizeAt(size_t n) const override
    {
        assert(n < size());
        return sizeAt(n) + sizeof(offsets[0]);
    }

    size_t allocatedBytes() const override
    {
        return chars.allocated_bytes() + offsets.allocated_bytes();
    }

    void protect() override;

    MutableColumnPtr cloneResized(size_t to_size) const override;

    Field operator[](size_t n) const override
    {
        assert(n < size());
        auto s = roaring::Roaring64Map::readSafe(reinterpret_cast<const char *>(&chars[offsetAt(n)]), sizeAt(n) - 1);
        return Field(std::move(s));
    }

    void get(size_t n, Field & res) const override
    {
        assert(n < size());
        auto s = roaring::Roaring64Map::readSafe(reinterpret_cast<const char *>(&chars[offsetAt(n)]), sizeAt(n) - 1);
        res = Field(std::move(s));
    }

    StringRef getDataAt(size_t n) const override
    {
        assert(n < size());
        return StringRef(reinterpret_cast<const char *>(&chars[offsetAt(n)]), sizeAt(n) - 1);
    }

    BitMap64 getBitMapAtImpl(size_t n) const
    {
        assert(n < size());
        BitMap64 res;
        if (sizeAt(n) - 1 > 0)
            return BitMap64::readSafe(reinterpret_cast<const char *>(&chars[offsetAt(n)]), sizeAt(n) - 1);
        return res;
    }

    const BitMap64 & getBitMapAt(size_t n) const
    {
        assert(n < size());
        auto it = bitmaps.find(n);
        if (it != bitmaps.end())
            return it->second;
        else
        {
            auto [data_iter, inserted] = const_cast<BitMaps &>(bitmaps).emplace(n, getBitMapAtImpl(n));
            return data_iter->second;
        }
    }

    StringRef getDataAtWithTerminatingZero(size_t n) const override
    {
        assert(n < size());
        return StringRef(reinterpret_cast<const char *>(&chars[offsetAt(n)]), sizeAt(n));
    }

/// Suppress gcc 7.3.1 warning: '*((void*)&<anonymous> +8)' may be used uninitialized in this function
#if !__clang__
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wmaybe-uninitialized"
#endif

    void insert(const Field & x) override
    {
        const BitMap64 & s = DB::get<const BitMap64 &>(x);
        insert(s);
    }

    void insert(const BitMap64 & x)
    {
        const size_t old_size = chars.size();
        const size_t size_to_append = x.getSizeInBytes() + 1;
        const size_t new_size = old_size + size_to_append;

        chars.resize(new_size);
        x.write(reinterpret_cast<char *>(chars.data() + old_size));
        chars[new_size - 1] = 0; /// append an extra 0 as ending

        offsets.push_back(new_size);
    }

#if !__clang__
#pragma GCC diagnostic pop
#endif

    void insertFrom(const IColumn & src_, size_t n) override
    {
        const ColumnBitMap64 & src = static_cast<const ColumnBitMap64 &>(src_);
        const size_t size_to_append = src.offsets[n] - src.offsets[n - 1];  /// -1th index is Ok, see PaddedPODArray.

        if (size_to_append == 1)
        {
            /// shortcut for empty string
            chars.push_back(0);
            offsets.push_back(chars.size());
        }
        else
        {
            const size_t old_size = chars.size();
            const size_t offset = src.offsets[n - 1];
            const size_t new_size = old_size + size_to_append;

            chars.resize(new_size);
            memcpySmallAllowReadWriteOverflow15(chars.data() + old_size, &src.chars[offset], size_to_append);
            offsets.push_back(new_size);
        }
    }

    void insertData(const char * pos, size_t length) override
    {
        const size_t old_size = chars.size();
        const size_t new_size = old_size + length + 1;

        chars.resize(new_size);
        if (length)
            memcpy(chars.data() + old_size, pos, length);
        chars[old_size + length] = 0;
        offsets.push_back(new_size);
    }

    void popBack(size_t n) override
    {
        assert(n <= offsets.size());
        size_t nested_n = offsets.back() - offsetAt(offsets.size() - n);
        chars.resize(chars.size() - nested_n);
        offsets.resize_assume_reserved(offsets.size() - n);
    }

    StringRef serializeValueIntoArena(size_t n, Arena & arena, char const *& begin) const override;

    const char * deserializeAndInsertFromArena(const char * pos) override;

    const char * skipSerializedInArena(const char * pos) const override;

    void updateHashWithValue(size_t n, SipHash & hash) const override
    {
        size_t bytes = sizeAt(n);
        size_t offset = offsetAt(n);

        hash.update(reinterpret_cast<const char *>(&bytes), sizeof(bytes));
        hash.update(reinterpret_cast<const char *>(&chars[offset]), bytes);
    }

    void updateWeakHash32(WeakHash32 & hash) const override;

    void updateHashFast(SipHash & hash) const override
    {
        hash.update(reinterpret_cast<const char *>(offsets.data()), size() * sizeof(offsets[0]));
        hash.update(reinterpret_cast<const char *>(chars.data()), size() * sizeof(chars[0]));
    }

    void insertRangeFrom(const IColumn & src, size_t start, size_t length) override;

    void insertRangeSelective(const IColumn & src, const Selector & selector, size_t selector_start, size_t length) override;

    ColumnPtr filter(const Filter & filt, ssize_t result_size_hint) const override;

    ColumnPtr permute(const Permutation & perm, size_t limit) const override;

    ColumnPtr index(const IColumn & indexes, size_t limit) const override;

    template <typename Type>
    ColumnPtr indexImpl(const PaddedPODArray<Type> & indexes, size_t limit) const;

    void insertDefault() override
    {
        insert(BitMap64());
    }

    int compareAt(size_t n, size_t m, const IColumn & rhs_, int /*nan_direction_hint*/) const override
    {
        const ColumnBitMap64 & rhs = static_cast<const ColumnBitMap64&>(rhs_);
        return memcmpSmallAllowOverflow15(chars.data() + offsetAt(n), sizeAt(n) - 1, rhs.chars.data() + rhs.offsetAt(m), rhs.sizeAt(m) - 1);
    }

    void compareColumn(const IColumn & rhs, size_t rhs_row_num,
                       PaddedPODArray<UInt64> * row_indexes, PaddedPODArray<Int8> & compare_results,
                       int direction, int nan_direction_hint) const override;

    bool hasEqualValues() const override;

    /// Variant of compareAt for string comparison with respect of collation.
    int compareAtWithCollation(size_t n, size_t m, const IColumn & rhs_, int, const Collator & collator) const override;

    void getPermutation(bool reverse, size_t limit, int nan_direction_hint, Permutation & res) const override;

    void updatePermutation(bool reverse, size_t limit, int, Permutation & res, EqualRanges & equal_ranges) const override;

    /// Sorting with respect of collation.
    void getPermutationWithCollation(const Collator & collator, bool reverse, size_t limit, int, Permutation & res) const override;

    void updatePermutationWithCollation(const Collator & collator, bool reverse, size_t limit, int, Permutation & res, EqualRanges & equal_ranges) const override;

    ColumnPtr replicate(const Offsets & replicate_offsets) const override;

    MutableColumns scatter(ColumnIndex num_columns, const Selector & selector) const override
    {
        return scatterImpl<ColumnBitMap64>(num_columns, selector);
    }

    void gather(ColumnGathererStream & gatherer_stream) override;

    void reserve(size_t n) override;

    void getExtremes(Field & min, Field & max) const override;


    bool canBeInsideNullable() const override { return true; }

    bool structureEquals(const IColumn & rhs) const override
    {
        return typeid(rhs) == typeid(ColumnBitMap64);
    }

    Chars & getChars() { return chars; }
    const Chars & getChars() const { return chars; }

    Offsets & getOffsets() { return offsets; }
    const Offsets & getOffsets() const { return offsets; }

    // Throws an exception if offsets/chars are messed up
    void validate() const;
};

}
