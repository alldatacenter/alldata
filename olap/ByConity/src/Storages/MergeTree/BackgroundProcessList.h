/*
 * Copyright 2016-2023 ClickHouse, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#pragma once

#include <Common/CurrentMetrics.h>
#include <memory>
#include <list>
#include <mutex>
#include <atomic>

namespace DB
{

/// Common code for background processes lists, like system.merges and system.replicated_fetches
/// Look at examples in MergeList and ReplicatedFetchList

template <typename ListElement, typename Info>
class BackgroundProcessList;

template <typename ListElement, typename Info>
class BackgroundProcessListEntry
{
    BackgroundProcessList<ListElement, Info> & list;
    using container_t = std::list<ListElement>;
    typename container_t::iterator it;
    CurrentMetrics::Increment metric_increment;
public:
    BackgroundProcessListEntry(const BackgroundProcessListEntry &) = delete;
    BackgroundProcessListEntry & operator=(const BackgroundProcessListEntry &) = delete;

    BackgroundProcessListEntry(BackgroundProcessList<ListElement, Info> & list_, const typename container_t::iterator it_, const CurrentMetrics::Metric & metric)
        : list(list_), it{it_}, metric_increment{metric}
    {
        list.onEntryCreate(*this);
    }

    ~BackgroundProcessListEntry()
    {
        std::lock_guard lock{list.mutex};
        list.onEntryDestroy(*this);
        list.entries.erase(it);
    }

    ListElement * operator->() { return &*it; }
    const ListElement * operator->() const { return &*it; }

    ListElement * get() { return &*it; }
    const ListElement * get() const { return &*it; }
};


template <typename ListElement, typename Info>
class BackgroundProcessList
{
protected:
    friend class BackgroundProcessListEntry<ListElement, Info>;

    using container_t = std::list<ListElement>;
    using info_container_t = std::list<Info>;

    mutable std::mutex mutex;
    container_t entries;

    CurrentMetrics::Metric metric;

    BackgroundProcessList(const CurrentMetrics::Metric & metric_)
        : metric(metric_)
    {}
public:

    using Entry = BackgroundProcessListEntry<ListElement, Info>;
    using EntryPtr = std::unique_ptr<Entry>;

    template <typename... Args>
    EntryPtr insert(Args &&... args)
    {
        std::lock_guard lock{mutex};
        auto entry = std::make_unique<Entry>(*this, entries.emplace(entries.end(), std::forward<Args>(args)...), metric);
        return entry;
    }

    info_container_t get() const
    {
        std::lock_guard lock{mutex};
        info_container_t res;
        for (const auto & list_element : entries)
            res.emplace_back(list_element.getInfo());
        return res;
    }

    size_t size() const
    {
        std::lock_guard lock(mutex);
        return entries.size();
    }

    template <class F>
    void apply(F && f)
    {
        std::lock_guard lock(mutex);
        f(entries);
    }

    virtual void onEntryCreate(const Entry & /* entry */) {}
    virtual void onEntryDestroy(const Entry & /* entry */) {}
    virtual inline ~BackgroundProcessList() = default;
};

}
