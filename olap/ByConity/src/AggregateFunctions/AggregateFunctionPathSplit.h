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

#include <pdqsort.h>
#include <AggregateFunctions/IAggregateFunction.h>
#include <Columns/ColumnArray.h>
#include <Columns/ColumnTuple.h>
#include <Columns/IColumn.h>
#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypeTuple.h>
#include <DataTypes/DataTypesNumber.h>
#include <IO/ReadHelpers.h>
#include <IO/WriteHelpers.h>
#include <Common/ArenaAllocator.h>
#include <Common/PODArray.h>
#include <common/logger_useful.h>


namespace DB
{

template<typename EventIndex>
struct AggregateFunctionPathSplitData
{
    using Time = UInt64;
    struct Event
    {
        EventIndex index;
        Time time;
        StringRef param;
        Event(EventIndex index_, Time time_, StringRef param_) : index(index_), time(time_), param(param_) { }
    };
    using Allocator = MixedArenaAllocator<8192>;
    using Events = PODArray<Event, 32 * sizeof(Event), Allocator>;
    Events events;

    bool sorted = false;
    void sort(bool reverse=false)
    {
        if (sorted)
            return;
        if (reverse) {
            pdqsort(events.begin(), events.end(), [](Event & lhs, Event & rhs) {
                return lhs.time > rhs.time
                    || (lhs.time == rhs.time && (lhs.index > rhs.index || (lhs.index == rhs.index && lhs.param > rhs.param)));
            });
        } else {
            pdqsort(events.begin(), events.end(), [](Event & lhs, Event & rhs) {
                return lhs.time < rhs.time
                    || (lhs.time == rhs.time && (lhs.index < rhs.index || (lhs.index == rhs.index && lhs.param < rhs.param)));
            });
        }
        sorted = true;
    }

    void add(EventIndex index, Time time, StringRef param, Arena * arena)
    {
        events.push_back(Event(index, time, param), arena);
    }

    void merge(const AggregateFunctionPathSplitData & other, Arena * arena)
    {
        sorted = false;
        size_t size = events.size();
        events.insert(std::begin(other.events), std::end(other.events), arena);
        // realloc from arena
        for (size_t i = size; i < events.size(); ++i)
        {
            auto t_param = events[i].param;
            events[i].param = StringRef(arena->insert(t_param.data, t_param.size), t_param.size);
        }
    }

    void serialize(WriteBuffer & buf) const
    {
        writeBinary(sorted, buf);
        size_t size = events.size();
        writeBinary(size, buf);
        for (size_t i = 0; i < size; ++i)
        {
            writeBinary(events[i].index, buf);
            writeBinary(events[i].time, buf);
            writeBinary(events[i].param, buf);
        }
    }

    void deserialize(ReadBuffer & buf, Arena * arena)
    {
        readBinary(sorted, buf);
        size_t size;
        readBinary(size, buf);
        events.reserve(size, arena);
        for (size_t i = 0; i < size; ++i)
        {
            EventIndex index;
            Time time;
            readBinary(index, buf);
            readBinary(time, buf);
            StringRef param = readStringBinaryInto(*arena, buf);
            add(index, time, param, arena);
        }
    }

    void print(size_t  /*param_size*/) const
    {
        String s = "Event size: " + std::to_string(events.size()) + "\n";
        for (size_t i = 0; i < events.size(); ++i)
        {
            s += "Event(index=" + std::to_string(events[i].index) + ", time=" + std::to_string(events[i].time)
                + ", param=" + events[i].param.toString() + ").\n";
        }
        LOG_DEBUG(&Poco::Logger::get("AggregateFunctionPathSplit"), "events:" + s + ".");
    }
};


template <typename EventIndex, bool is_terminating_event = false>
class AggregateFunctionPathSplit final : public IAggregateFunctionDataHelper<
                                             AggregateFunctionPathSplitData<EventIndex>,
                                             AggregateFunctionPathSplit<EventIndex, is_terminating_event>>
{
private:
    UInt64 max_session_size;
    UInt64 max_session_depth;

public:
    AggregateFunctionPathSplit(UInt64 max_seesion_size_, UInt64 max_session_depth_, const DataTypes & arguments, const Array & params)
        : IAggregateFunctionDataHelper<
            AggregateFunctionPathSplitData<EventIndex>,
            AggregateFunctionPathSplit<EventIndex, is_terminating_event>>(arguments, params)
        , max_session_size(max_seesion_size_)
        , max_session_depth(max_session_depth_)
    {
    }

    String getName() const override
    {
        return "pathSplit";
    }

    //[[(event,param)...]...]
    DataTypePtr getReturnType() const override
    {
        DataTypes types;
        types.emplace_back(std::make_shared<DataTypeNumber<EventIndex>>()); // event index
        types.emplace_back(std::make_shared<DataTypeString>()); // param
        return std::make_shared<DataTypeArray>(std::make_shared<DataTypeArray>(std::make_shared<DataTypeTuple>(types)));
    }

    void add(AggregateDataPtr place, const IColumn ** columns, size_t row_num, Arena * arena) const override
    {
        //index 0: time
        UInt64 time = columns[0]->getUInt(row_num);

        //index 1: event
        EventIndex event_index = columns[1]->getUInt(row_num);

        //index 2...: param
        StringRef t_param = columns[2]->getDataAt(row_num);
        StringRef param = StringRef(arena->insert(t_param.data, t_param.size), t_param.size);

        this->data(place).add(event_index, time, param, arena);
    }

    void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, Arena * arena) const override
    {
        this->data(place).merge(this->data(rhs), arena);
    }

    void serialize(ConstAggregateDataPtr place, WriteBuffer & buf) const override
    {
        this->data(place).serialize(buf);
    }

    void deserialize(AggregateDataPtr place, ReadBuffer & buf, Arena * arena) const override
    {
        this->data(place).deserialize(buf, arena);
    }

    void insertResultInto(AggregateDataPtr __restrict place, IColumn & to, Arena * ) const override
    {
        auto & data = const_cast<AggregateFunctionPathSplitData<EventIndex> &>(this->data(place));
        data.sort(is_terminating_event);
        auto & events = data.events;
        size_t size = events.size();
        size_t session_num = 0;

        /*
        arr_data
            arr_data_data
                arr_data_data.at(0)		event index
                arr_data_data.at(1)		string param
            arr_data_offsets
        arr_offsets
        */
        auto & arr_col = static_cast<ColumnArray &>(to);
        auto & arr_data = static_cast<ColumnArray &>(arr_col.getData());
        auto & arr_offset = arr_col.getOffsets();

        auto & arr_data_data = static_cast<ColumnTuple &>(arr_data.getData());
        auto & arr_data_offset = arr_data.getOffsets();

        auto & event_col = static_cast<ColumnVector<EventIndex> &>(arr_data_data.getColumn(0));
        auto & param_col = arr_data_data.getColumn(1);

        auto insert_session = [&](size_t start, size_t end) {
            for (size_t i = start; i < end; ++i)
            {
                event_col.insertValue(events[i].index);
                param_col.insertData(events[i].param.data, events[i].param.size);
            }
            arr_data_offset.push_back(end - start + (!arr_data_offset.empty() ? arr_data_offset.back() : 0));
            ++session_num;
        };


        size_t i = 0;
        auto find_first_event = [&]() {
            while (i < size && events[i].index != 1)
                ++i;
            return i < size;
        };

        while (find_first_event())
        {
            size_t start = i, end = size;
            ++i;
            while (i < size)
            {
                UInt64 t1, t2;
                if constexpr (is_terminating_event)
                {
                    t2 = events[i - 1].time;
                    t1 = events[i].time;
                }
                else
                {
                    t1 = events[i - 1].time;
                    t2 = events[i].time;
                }
                if (t2 - t1 > max_session_size)
                {
                    break;
                }
                ++i;
            }
            end = std::min<size_t>(start + max_session_depth, i);
            //process session
            insert_session(start, end);
            //next session
        }
        arr_offset.push_back(session_num + (!arr_offset.empty() ? arr_offset.back() : 0));
    }

    bool allocatesMemoryInArena() const override { return true; }

    bool handleNullItSelf() const override { return true; }

};

}
