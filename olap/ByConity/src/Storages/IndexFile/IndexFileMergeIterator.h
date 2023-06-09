// Copyright (c) 2011 The LevelDB Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file. See the AUTHORS file for names of contributors.

/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */


#pragma once

#include <Storages/IndexFile/Comparator.h>
#include <Storages/IndexFile/Iterator.h>
#include <Storages/IndexFile/IteratorWrapper.h>

#include <memory>
#include <queue>
#include <vector>

namespace DB::IndexFile
{

class IndexFileMergeIterator : public Iterator
{
public:
    IndexFileMergeIterator(const Comparator * comparator, std::vector<std::unique_ptr<Iterator>> children);

    ~IndexFileMergeIterator() override;

    virtual bool Valid() const override { return current_ != -1; }

    // Position at the first key in the source.  The iterator is Valid()
    // after this call iff the source is not empty.
    virtual void SeekToFirst() override;

    virtual void SeekToLast() override
    {
        current_ = -1;
        err_status = Status::NotSupported("IndexFileMergeIterator::SeekToLast()");
    }

    // Position at the first key in the source that is at or past target.
    // The iterator is Valid() after this call iff the source contains
    // an entry that comes at or past target.
    virtual void Seek(const Slice & target) override;

    // REQUIRES: Valid()
    virtual void Next() override;

    // REQUIRES: Valid() && target > key()
    virtual void NextUntil(const Slice & target, bool & exact_match) override;

    virtual void Prev() override
    {
        current_ = -1;
        err_status = Status::NotSupported("IndexFileMergeIterator::Prev()");
    }

    // REQUIRES: Valid()
    virtual Slice key() const override;

    // REQUIRES: Valid()
    virtual Slice value() const override;

    // Return the index of child iterator for the current entry.
    // REQUIRES: Valid()
    int child_index() const;

    // If an error has occurred, return it.  Else return an ok status.
    virtual Status status() const override;

private:
    inline int Compare(const Slice & a, const Slice & b) const { return comparator_->Compare(a, b); }
    void FindSmallest();

    /// put iterator with the smallest key and idx on top of the priority queue
    struct MinHeapComparator
    {
    public:
        MinHeapComparator(const Comparator * comparator, IteratorWrapper * iters)
            : comparator_(comparator), iters_(iters) {}

        bool operator()(int lhs, int rhs)
        {
            int res = comparator_->Compare(iters_[lhs].key(), iters_[rhs].key());
            if (res > 0)
                return true;
            else if (res < 0)
                return false;
            else
                return lhs > rhs;
        }

    private:
        const Comparator * comparator_;
        IteratorWrapper * iters_;
    };

    using MinHeap = std::priority_queue<int, std::vector<int>, MinHeapComparator>;

    const Comparator * comparator_;
    IteratorWrapper * children_; /// take ownership of input iterators
    int n_;
    int current_; /// -1 if !Valid()
    Status err_status;
    MinHeap min_heap_;
};

}
