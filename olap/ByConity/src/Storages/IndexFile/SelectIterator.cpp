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

#include <Storages/IndexFile/Iterator.h>
#include <Storages/IndexFile/SelectIterator.h>

namespace DB::IndexFile
{
namespace
{
    class SelectIterator : public Iterator
    {
    public:
        SelectIterator(Iterator * child, Predicate select_predicate)
            : child_(child), select_predicate_(select_predicate)
        {
        }

        ~SelectIterator() override
        {
            delete child_;
        }

        bool Valid() const override { return child_->Valid(); }
        void SeekToFirst() override
        {
            child_->SeekToFirst();
            SkipFilteredForward();
        }
        void SeekToLast() override
        {
            child_->SeekToLast();
            SkipFilteredBackward();
        }
        void Seek(const Slice & target) override
        {
            child_->Seek(target);
            SkipFilteredForward();
        }
        void Next() override
        {
            child_->Next();
            SkipFilteredForward();
        }

        void NextUntil(const Slice & target, bool & exact_match) override
        {
            child_->NextUntil(target, exact_match);
            if (SkipFilteredForward())
            {
                // index file doesn't allow duplicate keys, thus there is at most one exact match.
                exact_match = false;
            }
        }

        void Prev() override
        {
            child_->Prev();
            SkipFilteredBackward();
        }
        Slice key() const override { return child_->key(); }
        Slice value() const override { return child_->value(); }
        Status status() const override { return child_->status(); }

    private:
        bool SkipFilteredForward()
        {
            bool skipped = false;
            if (!select_predicate_)
                return skipped;
            while (child_->Valid() && !select_predicate_(child_->key(), child_->value()))
            {
                skipped = true;
                child_->Next();
            }
            return skipped;
        }

        void SkipFilteredBackward()
        {
            if (!select_predicate_)
                return;
            while (child_->Valid() && !select_predicate_(child_->key(), child_->value()))
                child_->Prev();
        }

        Iterator * child_;
        Predicate select_predicate_;
    };
} // namespace

Iterator * NewSelectIterator(Iterator * child, Predicate select_predicate)
{
    return new SelectIterator(child, select_predicate);
}

}
