/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

// Copyright (c) 2011 The LevelDB Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file. See the AUTHORS file for names of contributors.

#include <Storages/IndexFile/TwoLevelIterator.h>

#include <Storages/IndexFile/Table.h>
#include <Storages/IndexFile/Block.h>
#include <Storages/IndexFile/Format.h>
#include <Storages/IndexFile/IteratorWrapper.h>

namespace DB::IndexFile
{
namespace
{
    typedef Iterator * (*BlockFunction)(void *, const ReadOptions &, const Slice &);

    class TwoLevelIterator : public Iterator
    {
    public:
        TwoLevelIterator(const Comparator * comparator, Iterator * index_iter, BlockFunction block_function, void * arg, const ReadOptions & options);

        ~TwoLevelIterator() override;

        void Seek(const Slice & target) override;
        void SeekToFirst() override;
        void SeekToLast() override;
        void Next() override;
        void NextUntil(const Slice & target, bool & exact_match) override;
        void Prev() override;

        bool Valid() const override { return data_iter_.Valid(); }
        Slice key() const override
        {
            assert(Valid());
            return data_iter_.key();
        }
        Slice value() const override
        {
            assert(Valid());
            return data_iter_.value();
        }
        Status status() const override
        {
            // It'd be nice if status() returned a const Status& instead of a Status
            if (!index_iter_.status().ok())
            {
                return index_iter_.status();
            }
            else if (data_iter_.iter() != nullptr && !data_iter_.status().ok())
            {
                return data_iter_.status();
            }
            else
            {
                return status_;
            }
        }

    private:
        inline int Compare(const Slice & a, const Slice & b) const { return comparator_->Compare(a, b); }
        void SaveError(const Status & s)
        {
            if (status_.ok() && !s.ok())
                status_ = s;
        }
        void SkipEmptyDataBlocksForward();
        void SkipEmptyDataBlocksBackward();
        void SetDataIterator(Iterator * data_iter);
        void InitDataBlock();

        const Comparator * comparator_;
        BlockFunction block_function_;
        void * arg_;
        const ReadOptions options_;
        Status status_;
        IteratorWrapper index_iter_;
        IteratorWrapper data_iter_; // May be nullptr
        // If data_iter_ is non-null, then "data_block_handle_" holds the
        // "index_value" passed to block_function_ to create the data_iter_.
        std::string data_block_handle_;
    };

    TwoLevelIterator::TwoLevelIterator(const Comparator * comparator, Iterator * index_iter, BlockFunction block_function, void * arg, const ReadOptions & options)
        : comparator_(comparator), block_function_(block_function), arg_(arg), options_(options), index_iter_(index_iter), data_iter_(nullptr)
    {
    }

    TwoLevelIterator::~TwoLevelIterator() = default;

    void TwoLevelIterator::Seek(const Slice & target)
    {
        index_iter_.Seek(target);
        InitDataBlock();
        if (data_iter_.iter() != nullptr)
            data_iter_.Seek(target);
        SkipEmptyDataBlocksForward();
    }

    void TwoLevelIterator::SeekToFirst()
    {
        index_iter_.SeekToFirst();
        InitDataBlock();
        if (data_iter_.iter() != nullptr)
            data_iter_.SeekToFirst();
        SkipEmptyDataBlocksForward();
    }

    void TwoLevelIterator::SeekToLast()
    {
        index_iter_.SeekToLast();
        InitDataBlock();
        if (data_iter_.iter() != nullptr)
            data_iter_.SeekToLast();
        SkipEmptyDataBlocksBackward();
    }

    void TwoLevelIterator::Next()
    {
        assert(Valid());
        data_iter_.Next();
        SkipEmptyDataBlocksForward();
    }

    void TwoLevelIterator::NextUntil(const Slice & target, bool & exact_match)
    {
        assert(Compare(key(), target) < 0);
        data_iter_.NextUntil(target, exact_match);

        while (!data_iter_.Valid())
        {
            if (!index_iter_.Valid())
            {
                // reaches the end of the iterator
                SetDataIterator(nullptr);
                return;
            }
            // move to the next block
            index_iter_.Next();
            if (index_iter_.Valid() && Compare(index_iter_.key(), target) < 0)
                continue; // skip the next block
            InitDataBlock();
            if (data_iter_.iter() != nullptr)
            {
                data_iter_.Seek(target);
                if (data_iter_.Valid())
                {
                    exact_match = Compare(data_iter_.key(), target) == 0;
                }
            }
        }
    }

    void TwoLevelIterator::Prev()
    {
        assert(Valid());
        data_iter_.Prev();
        SkipEmptyDataBlocksBackward();
    }

    void TwoLevelIterator::SkipEmptyDataBlocksForward()
    {
        while (data_iter_.iter() == nullptr || !data_iter_.Valid())
        {
            // Move to next block
            if (!index_iter_.Valid())
            {
                SetDataIterator(nullptr);
                return;
            }
            index_iter_.Next();
            InitDataBlock();
            if (data_iter_.iter() != nullptr)
                data_iter_.SeekToFirst();
        }
    }

    void TwoLevelIterator::SkipEmptyDataBlocksBackward()
    {
        while (data_iter_.iter() == nullptr || !data_iter_.Valid())
        {
            // Move to next block
            if (!index_iter_.Valid())
            {
                SetDataIterator(nullptr);
                return;
            }
            index_iter_.Prev();
            InitDataBlock();
            if (data_iter_.iter() != nullptr)
                data_iter_.SeekToLast();
        }
    }

    void TwoLevelIterator::SetDataIterator(Iterator * data_iter)
    {
        if (data_iter_.iter() != nullptr)
            SaveError(data_iter_.status());
        data_iter_.Set(data_iter);
    }

    void TwoLevelIterator::InitDataBlock()
    {
        if (!index_iter_.Valid())
        {
            SetDataIterator(nullptr);
        }
        else
        {
            Slice handle = index_iter_.value();
            if (data_iter_.iter() != nullptr && handle.compare(data_block_handle_) == 0)
            {
                // data_iter_ is already constructed with this iterator, so
                // no need to change anything
            }
            else
            {
                Iterator * iter = (*block_function_)(arg_, options_, handle);
                data_block_handle_.assign(handle.data(), handle.size());
                SetDataIterator(iter);
            }
        }
    }

} // namespace

Iterator * NewTwoLevelIterator(const Comparator * comparator, Iterator * index_iter, BlockFunction block_function, void * arg, const ReadOptions & options)
{
    return new TwoLevelIterator(comparator, index_iter, block_function, arg, options);
}

}
