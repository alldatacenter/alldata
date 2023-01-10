// Copyright (c) 2011 The LevelDB Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file. See the AUTHORS file for names of contributors.

/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#include <Storages/IndexFile/IndexFileMergeIterator.h>
#include <cassert>

namespace DB::IndexFile
{
IndexFileMergeIterator::IndexFileMergeIterator(const Comparator * comparator, std::vector<std::unique_ptr<Iterator>> children)
    : comparator_(comparator), children_(nullptr), n_(children.size()), current_(-1)
    , min_heap_(MinHeapComparator(comparator_, children_)) /// children_ is not initialized, must re-init min_heap_ before use
{
    children_ = new IteratorWrapper[n_];
    for (int i = 0; i < n_; ++i)
        children_[i].Set(children[i].release());
}

IndexFileMergeIterator::~IndexFileMergeIterator()
{
    delete[] children_;
}

void IndexFileMergeIterator::SeekToFirst()
{
    min_heap_ = MinHeap(MinHeapComparator(comparator_, children_));
    for (int i = 0; i < n_; ++i)
    {
        children_[i].SeekToFirst();
        if (children_[i].Valid())
            min_heap_.push(i);
    }
    FindSmallest();
}

void IndexFileMergeIterator::Seek(const Slice & target)
{
    min_heap_ = MinHeap(MinHeapComparator(comparator_, children_));
    for (int i = 0; i < n_; ++i)
    {
        children_[i].Seek(target);
        if (children_[i].Valid())
            min_heap_.push(i);
    }
    FindSmallest();
}

void IndexFileMergeIterator::Next()
{
    assert(Valid());
    children_[current_].Next();
    if (children_[current_].Valid())
        min_heap_.push(current_);
    FindSmallest();
}

void IndexFileMergeIterator::NextUntil(const Slice & target, bool & exact_match)
{
    assert(Compare(key(), target) < 0);
    children_[current_].NextUntil(target, exact_match);
    if (children_[current_].Valid())
        min_heap_.push(current_);

    current_ = -1;
    while (!min_heap_.empty())
    {
        int i = min_heap_.top();
        min_heap_.pop();
        assert(children_[i].Valid());
        int cmp = Compare(children_[i].key(), target);
        if (cmp < 0)
        {
            children_[i].NextUntil(target, exact_match);
            if (children_[i].Valid())
                min_heap_.push(i);
        }
        else
        {
            // found the first key (children_[i].key()) >= target
            exact_match = (cmp == 0);
            current_ = i;
            break;
        }
    }
}

void IndexFileMergeIterator::FindSmallest()
{
    if (min_heap_.empty())
    {
        current_ = -1;
    }
    else
    {
        current_ = min_heap_.top();
        assert(children_[current_].Valid());
        min_heap_.pop();
    }
}

Slice IndexFileMergeIterator::key() const
{
    assert(Valid());
    return children_[current_].key();
}

Slice IndexFileMergeIterator::value() const
{
    assert(Valid());
    return children_[current_].value();
}

int IndexFileMergeIterator::child_index() const
{
    assert(Valid());
    return current_;
}

Status IndexFileMergeIterator::status() const
{
    if (!err_status.ok())
        return err_status;
    Status status;
    for (int i = 0; i < n_; ++i)
    {
        status = children_[i].status();
        if (!status.ok())
        {
            break;
        }
    }
    return status;
}

}
