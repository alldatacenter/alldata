// Copyright (c) 2011 The LevelDB Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file. See the AUTHORS file for names of contributors.

/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */


#include <Storages/IndexFile/Comparator.h>

#include <stdint.h>
#include <Common/Slice.h>

namespace DB::IndexFile
{
Comparator::~Comparator()
{
}

namespace
{
    class BytewiseComparatorImpl : public Comparator
    {
    public:
        BytewiseComparatorImpl() { }

        virtual const char * Name() const override { return "leveldb.BytewiseComparator"; }

        virtual int Compare(const Slice & a, const Slice & b) const override { return a.compare(b); }

        virtual void FindShortestSeparator(std::string * start, const Slice & limit) const override
        {
            // Find length of common prefix
            size_t min_length = std::min(start->size(), limit.size());
            size_t diff_index = 0;
            while ((diff_index < min_length) && ((*start)[diff_index] == limit[diff_index]))
            {
                diff_index++;
            }

            if (diff_index >= min_length)
            {
                // Do not shorten if one string is a prefix of the other
            }
            else
            {
                uint8_t diff_byte = static_cast<uint8_t>((*start)[diff_index]);
                if (diff_byte < static_cast<uint8_t>(0xff) && diff_byte + 1 < static_cast<uint8_t>(limit[diff_index]))
                {
                    (*start)[diff_index]++;
                    start->resize(diff_index + 1);
                    assert(Compare(*start, limit) < 0);
                }
            }
        }

        virtual void FindShortSuccessor(std::string * key) const override
        {
            // Find first character that can be incremented
            size_t n = key->size();
            for (size_t i = 0; i < n; i++)
            {
                const uint8_t byte = (*key)[i];
                if (byte != static_cast<uint8_t>(0xff))
                {
                    (*key)[i] = byte + 1;
                    key->resize(i + 1);
                    return;
                }
            }
            // *key is a run of 0xffs.  Leave it alone.
        }
    };
} // namespace

const Comparator * BytewiseComparator()
{
    static BytewiseComparatorImpl bytewise;
    return &bytewise;
}

}
