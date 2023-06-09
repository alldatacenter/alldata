/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

// Copyright (c) 2011 The LevelDB Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file. See the AUTHORS file for names of contributors.

#include <Storages/IndexFile/Block.h>

#include <cstdint>

#include <Storages/IndexFile/Comparator.h>
#include <Storages/IndexFile/Format.h>
#include <Common/Coding.h>

namespace DB::IndexFile
{

inline uint32_t Block::NumRestarts() const
{
    assert(size_ >= sizeof(uint32_t));
    return DecodeFixed32(data_ + size_ - sizeof(uint32_t));
}

Block::Block(const BlockContents & contents) : data_(contents.data.data()), size_(contents.data.size()), owned_(contents.heap_allocated)
{
    if (size_ < sizeof(uint32_t))
    {
        size_ = 0; // Error marker
    }
    else
    {
        size_t max_restarts_allowed = (size_ - sizeof(uint32_t)) / sizeof(uint32_t);
        if (NumRestarts() > max_restarts_allowed)
        {
            // The size is too small for NumRestarts()
            size_ = 0;
        }
        else
        {
            restart_offset_ = size_ - (1 + NumRestarts()) * sizeof(uint32_t);
        }
    }
}

Block::~Block()
{
    if (owned_)
    {
        delete[] data_;
    }
}

// Helper routine: decode the next block entry starting at "p",
// storing the number of shared key bytes, non_shared key bytes,
// and the length of the value in "*shared", "*non_shared", and
// "*value_length", respectively.  Will not dereference past "limit".
//
// If any errors are detected, returns nullptr.  Otherwise, returns a
// pointer to the key delta (just past the three decoded values).
static inline const char *
DecodeEntry(const char * p, const char * limit, uint32_t * shared, uint32_t * non_shared, uint32_t * value_length)
{
    if (limit - p < 3)
        return nullptr;
    *shared = reinterpret_cast<const uint8_t *>(p)[0];
    *non_shared = reinterpret_cast<const uint8_t *>(p)[1];
    *value_length = reinterpret_cast<const uint8_t *>(p)[2];
    if ((*shared | *non_shared | *value_length) < 128)
    {
        // Fast path: all three values are encoded in one byte each
        p += 3;
    }
    else
    {
        if ((p = GetVarint32Ptr(p, limit, shared)) == nullptr)
            return nullptr;
        if ((p = GetVarint32Ptr(p, limit, non_shared)) == nullptr)
            return nullptr;
        if ((p = GetVarint32Ptr(p, limit, value_length)) == nullptr)
            return nullptr;
    }

    if (static_cast<uint32_t>(limit - p) < (*non_shared + *value_length))
    {
        return nullptr;
    }
    return p;
}

class Block::Iter : public Iterator
{
private:
    const Comparator * const comparator_;
    const char * const data_; // underlying block contents
    uint32_t const restarts_; // Offset of restart array (list of fixed32)
    uint32_t const num_restarts_; // Number of uint32_t entries in restart array

    // current_ is offset in data_ of current entry.  >= restarts_ if !Valid
    uint32_t current_;
    uint32_t restart_index_; // Index of restart block in which current_ falls
    std::string key_;
    Slice value_;
    Status status_;

    inline int Compare(const Slice & a, const Slice & b) const { return comparator_->Compare(a, b); }

    // Return the offset in data_ just past the end of the current entry.
    inline uint32_t NextEntryOffset() const { return (value_.data() + value_.size()) - data_; }

    uint32_t GetRestartPoint(uint32_t index)
    {
        assert(index < num_restarts_);
        return DecodeFixed32(data_ + restarts_ + index * sizeof(uint32_t));
    }

    void SeekToRestartPoint(uint32_t index)
    {
        key_.clear();
        restart_index_ = index;
        // current_ will be fixed by ParseNextKey();

        // ParseNextKey() starts at the end of value_, so set value_ accordingly
        uint32_t offset = GetRestartPoint(index);
        value_ = Slice(data_ + offset, 0);
    }

public:
    Iter(const Comparator * comparator, const char * data, uint32_t restarts, uint32_t num_restarts)
        : comparator_(comparator)
        , data_(data)
        , restarts_(restarts)
        , num_restarts_(num_restarts)
        , current_(restarts_)
        , restart_index_(num_restarts_)
    {
        assert(num_restarts_ > 0);
    }

    bool Valid() const override { return current_ < restarts_; }
    Status status() const override { return status_; }
    Slice key() const override
    {
        assert(Valid());
        return key_;
    }
    Slice value() const override
    {
        assert(Valid());
        return value_;
    }

    void Next() override
    {
        assert(Valid());
        ParseNextKey();
    }

    void Prev() override
    {
        assert(Valid());

        // Scan backwards to a restart point before current_
        const uint32_t original = current_;
        while (GetRestartPoint(restart_index_) >= original)
        {
            if (restart_index_ == 0)
            {
                // No more entries
                current_ = restarts_;
                restart_index_ = num_restarts_;
                return;
            }
            restart_index_--;
        }

        SeekToRestartPoint(restart_index_);
        do
        {
            // Loop until end of current entry hits the start of original entry
        } while (ParseNextKey() && NextEntryOffset() < original);
    }

    void Seek(const Slice & target) override
    {
        // Binary search in restart array to find the last restart point
        // with a key < target
        uint32_t left = 0;
        uint32_t right = num_restarts_ - 1;
        while (left < right)
        {
            uint32_t mid = (left + right + 1) / 2;
            uint32_t region_offset = GetRestartPoint(mid);
            uint32_t shared, non_shared, value_length;
            const char * key_ptr = DecodeEntry(data_ + region_offset, data_ + restarts_, &shared, &non_shared, &value_length);
            if (key_ptr == nullptr || (shared != 0))
            {
                CorruptionError();
                return;
            }
            Slice mid_key(key_ptr, non_shared);
            if (Compare(mid_key, target) < 0)
            {
                // Key at "mid" is smaller than "target".  Therefore all
                // blocks before "mid" are uninteresting.
                left = mid;
            }
            else
            {
                // Key at "mid" is >= "target".  Therefore all blocks at or
                // after "mid" are uninteresting.
                right = mid - 1;
            }
        }

        // Linear search (within restart block) for first key >= target
        SeekToRestartPoint(left);
        while (true)
        {
            if (!ParseNextKey())
            {
                return;
            }
            if (Compare(key_, target) >= 0)
            {
                return;
            }
        }
    }

    void NextUntil(const Slice & target, bool & exact_match) override
    {
        assert(Compare(key(), target) < 0);

        /// First linear search in the current restart group
        uint32_t limit = restarts_;
        if (restart_index_ + 1 < num_restarts_)
            limit = GetRestartPoint(restart_index_ + 1);

        if (LinearSearchInGroup(target, exact_match, limit))
            return; /// found entry >= target

        if (!Valid())
            return; /// no such entry in this block

        /// now we moved to a new restart group and key() is the first key in that group
        int cmp = Compare(key(), target);
        if (cmp >= 0)
        {
            exact_match = (cmp == 0);
            return;
        }

        /// key() < target.
        /// Before linear search, first check next restart key to see if we can skip the current group
        uint32_t current_restart_index = restart_index_;
        while (current_restart_index + 1 < num_restarts_)
        {
            Slice next_restart_key = KeyAtRestartPoint(current_restart_index + 1);
            if (next_restart_key.empty())
            {
                CorruptionError();
                return;
            }
            cmp = Compare(next_restart_key, target);
            if (cmp < 0)
            {
                current_restart_index++;
            }
            else if (cmp > 0)
            {
                break;
            }
            else
            {
                SeekToRestartPoint(current_restart_index + 1);
                ParseNextKey();
                exact_match = true;
                return;
            }
        }

        if (current_restart_index > restart_index_)
        {
            SeekToRestartPoint(current_restart_index);
        }
        LinearSearchInGroup(target, exact_match, restarts_);
    }

    void SeekToFirst() override
    {
        SeekToRestartPoint(0);
        ParseNextKey();
    }

    void SeekToLast() override
    {
        SeekToRestartPoint(num_restarts_ - 1);
        while (ParseNextKey() && NextEntryOffset() < restarts_)
        {
            // Keep skipping
        }
    }

private:

    // return empty slice on error
    Slice KeyAtRestartPoint(uint32_t restart_array_index)
    {
        uint32_t region_offset = GetRestartPoint(restart_array_index);
        uint32_t shared, non_shared, value_length;
        const char * key_ptr = DecodeEntry(data_ + region_offset, data_ + restarts_, &shared, &non_shared, &value_length);
        if (key_ptr == nullptr || (shared != 0))
        {
            return {};
        }
        return {key_ptr, non_shared};
    }

    bool LinearSearchInGroup(const Slice & target, bool & exact_match, uint32_t limit)
    {
        while (ParseNextKey() && current_ < limit)
        {
            int cmp = Compare(key(), target);
            if (cmp >= 0)
            {
                exact_match = (cmp == 0);
                return true;
            }
        }
        return false;
    }

    void CorruptionError()
    {
        current_ = restarts_;
        restart_index_ = num_restarts_;
        status_ = Status::Corruption("bad entry in block");
        key_.clear();
        value_.clear();
    }

    bool ParseNextKey()
    {
        current_ = NextEntryOffset();
        const char * p = data_ + current_;
        const char * limit = data_ + restarts_; // Restarts come right after data
        if (p >= limit)
        {
            // No more entries to return.  Mark as invalid.
            current_ = restarts_;
            restart_index_ = num_restarts_;
            return false;
        }

        // Decode next entry
        uint32_t shared, non_shared, value_length;
        p = DecodeEntry(p, limit, &shared, &non_shared, &value_length);
        if (p == nullptr || key_.size() < shared)
        {
            CorruptionError();
            return false;
        }
        else
        {
            key_.resize(shared);
            key_.append(p, non_shared);
            value_ = Slice(p + non_shared, value_length);
            while (restart_index_ + 1 < num_restarts_ && GetRestartPoint(restart_index_ + 1) < current_)
            {
                ++restart_index_;
            }
            return true;
        }
    }
};

Iterator * Block::NewIterator(const Comparator * comparator)
{
    if (size_ < sizeof(uint32_t))
    {
        return NewErrorIterator(Status::Corruption("bad block contents"));
    }
    const uint32_t num_restarts = NumRestarts();
    if (num_restarts == 0)
    {
        return NewEmptyIterator();
    }
    else
    {
        return new Iter(comparator, data_, restart_offset_, num_restarts);
    }
}

}
