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
#include <Columns/ColumnNullable.h>
#include <Columns/ColumnString.h>
#include <Columns/ColumnTuple.h>
#include <Columns/IColumn.h>
#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypeFunction.h>
#include <DataTypes/DataTypeNullable.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypeTuple.h>
#include <DataTypes/DataTypesNumber.h>
#include <IO/ReadHelpers.h>
#include <IO/WriteHelpers.h>
#include <Common/Arena.h>
#include <Common/ArenaAllocator.h>
#include <Common/PODArray.h>
#include <common/StringRef.h>
#include <common/logger_useful.h>


namespace DB
{
struct AggregateFunctionSessionAnalysisData
{
    using EventType = UInt8;
    using Time = UInt64;
    struct Event
    {
        EventType type;
        Time time;
        StringRef * params;
        Event() = default;
        Event(Event && e) : type(e.type), time(e.time), params(e.params) { e.params = nullptr; }
        Event(EventType _type, Time _time, StringRef * _params) : type(_type), time(_time), params(_params) { }
        Event & operator=(Event && e)
        {
            type = e.type;
            time = e.time;
            params = e.params;
            e.params = nullptr;
            return *this;
        }
    };
    using Allocator = MixedArenaAllocator<8192>;
    using Events = PODArray<Event, 32 * sizeof(Event), Allocator>;
    Events events;

    bool sorted = false;
    void sort()
    {
        if (sorted)
            return;
        pdqsort(events.begin(), events.end(), [](Event & lhs, Event & rhs) { return lhs.time < rhs.time; });
        sorted = true;
    }

    void add(EventType type, Time time, StringRef * params, Arena * arena) { events.push_back(Event(type, time, params), arena); }
    void merge(const AggregateFunctionSessionAnalysisData & other, Arena * arena, size_t param_size)
    {
        sorted = false;
        size_t size = events.size();
        events.insert(std::begin(other.events), std::end(other.events), arena);
        // realloc from arena
        for (size_t i = size; i < events.size(); ++i)
        {
            StringRef * params = new (arena->alloc((param_size + 1) * sizeof(StringRef))) StringRef[param_size + 1];
            for (size_t j = 0; j < param_size + 1; ++j)
            {
                auto t_param = events[i].params[j];
                if (t_param.size)
                    params[j] = StringRef(arena->insert(t_param.data, t_param.size), t_param.size);
            }
            events[i].params = params;
        }
    }
    void serialize(WriteBuffer & buf, size_t param_size) const
    {
        writeBinary(sorted, buf);
        size_t size = events.size();
        writeBinary(size, buf);
        for (size_t i = 0; i < size; ++i)
        {
            writeBinary(events[i].type, buf);
            writeBinary(events[i].time, buf);
            for (size_t j = 0; j < param_size + 1; ++j)
                writeBinary(events[i].params[j], buf);
        }
    }
    void deserialize(ReadBuffer & buf, Arena * arena, size_t param_size)
    {
        readBinary(sorted, buf);
        size_t size;
        readBinary(size, buf);
        events.reserve(size, arena);
        for (size_t i = 0; i < size; ++i)
        {
            EventType type;
            Time time;
            StringRef * params = new (arena->alloc((param_size + 1) * sizeof(StringRef))) StringRef[param_size + 1];
            readBinary(type, buf);
            readBinary(time, buf);
            for (size_t j = 0; j < param_size + 1; ++j)
                params[j] = readStringBinaryInto(*arena, buf);

            add(type, time, params, arena);
        }
    }

    void print(size_t param_size) const
    {
        String s = "Event size: " + std::to_string(events.size()) + "\n";
        for (size_t i = 0; i < events.size(); ++i)
        {
            s += "Event(type=" + std::to_string(events[i].type) + ", time=" + std::to_string(events[i].time);

            s += ", params=Params(";
            for (size_t j = 0; j < param_size + 1; ++j)
            {
                if (events[i].params[j].size)
                    s += events[i].params[j].toString() + ", ";
                else
                    s += "Null, ";
            }
            s += ")";

            s += ")\n";
        }
        LOG_DEBUG(&Poco::Logger::get("AggregateFunctionSessionAnalysis"), "events:" + s + ".");
    }
};


template <bool has_start_event, bool has_end_event, bool has_target_event, bool nullable_event, bool nullable_time> // for constexpr
class AggregateFunctionSessionAnalysis final
    : public IAggregateFunctionDataHelper<
          AggregateFunctionSessionAnalysisData,
          AggregateFunctionSessionAnalysis<has_start_event, has_end_event, has_target_event, nullable_event, nullable_time>>
{
private:
    UInt64 maxSessionSize;
    // UInt64 windowSize;
    DataTypes types;
    std::vector<bool> nullColFlags;
    UInt8 paramSize;
    String startEvent;
    String targetEvent;
    String endEvent;
    UInt8 startBit = 1 << 0;
    UInt8 targetBit = 1 << 1;
    UInt8 endBit = 1 << 2;

public:
    AggregateFunctionSessionAnalysis(
        UInt64 max_seesion_size,
        /*UInt64 window_size,*/ String start_event,
        String end_event,
        String target_event,
        const DataTypes & arguments,
        const Array & params)
        : IAggregateFunctionDataHelper<AggregateFunctionSessionAnalysisData, AggregateFunctionSessionAnalysis>(arguments, params)
        , maxSessionSize(max_seesion_size)
        , /*windowSize(window_size),*/ startEvent(start_event)
        , targetEvent(target_event)
        , endEvent(end_event)
    {
        types.reserve(arguments.size() - 2 + 3);
        types.emplace_back(std::make_shared<DataTypeUInt32>()); // session duration
        types.emplace_back(std::make_shared<DataTypeUInt32>()); // session depth
        types.emplace_back(std::make_shared<DataTypeString>()); // end event
        types.emplace_back(std::make_shared<DataTypeUInt64>()); // session time
        for (size_t i = 2; i < arguments.size(); ++i)
        {
            types.emplace_back(arguments[i]); // param type
        }

        nullColFlags.reserve(arguments.size());
        std::transform(arguments.begin(), arguments.end(), std::back_inserter(nullColFlags), [](DataTypePtr argument) -> bool {
            return argument->isNullable();
        });

        paramSize = arguments.size() - 2;

        if (startEvent == targetEvent)
            startBit |= targetBit;
        if (startEvent == endEvent)
            startBit |= endBit;
        if (targetEvent == endEvent)
            targetBit |= endBit;
    }

    String getName() const override { return "sessionAnalysis"; }

    // [(session_duratin, session_depth, end_event, param...)]
    DataTypePtr getReturnType() const override { return std::make_shared<DataTypeArray>(std::make_shared<DataTypeTuple>(types)); }

    void add(AggregateDataPtr place, const IColumn ** columns, size_t row_num, Arena * arena) const override
    {
        //index 0: event
        UInt8 event_type = 0;
        StringRef event;
        do
        {
            StringRef t_event;
            if constexpr (nullable_event)
            {
                t_event = columns[0]->isNullAt(row_num)
                    ? event
                    : (static_cast<const ColumnNullable *>(columns[0]))->getNestedColumn().getDataAtWithTerminatingZero(row_num);
            }
            else
            {
                t_event = columns[0]->getDataAt(row_num);
            }
            // process null
            if (!t_event.size)
                break;

            // alloc from arena
            event = StringRef(arena->insert(t_event.data, t_event.size), t_event.size);
            if constexpr (has_start_event)
            {
                if (t_event == startEvent)
                {
                    event_type = startBit;
                    break;
                }
            }
            if constexpr (has_target_event)
            {
                if (t_event == targetEvent)
                {
                    event_type = targetBit;
                    break;
                }
            }
            if constexpr (has_end_event)
            {
                if (t_event == endEvent)
                {
                    event_type = endBit;
                    break;
                }
            }
        } while (0);

        //index 1: time
        // auto time = nullColFlags[1] ? (columns[1]->isNullAt(row_num) ? 0 : ((static_cast<const ColumnNullable*>(columns[1]))->getNestedColumn().getUInt(row_num))) : columns[1]->getUInt(row_num);
        UInt64 time;
        if constexpr (nullable_time)
            time = columns[1]->isNullAt(row_num) ? 0 : (static_cast<const ColumnNullable *>(columns[1]))->getNestedColumn().getUInt(row_num);
        else
            time = columns[1]->getUInt(row_num);

        //index 2...: params alloc from arena
        StringRef * params = new (arena->alloc((paramSize + 1) * sizeof(StringRef))) StringRef[paramSize + 1];

        params[0] = event;
        auto add_params = [&]() {
            for (size_t i = 2; i < paramSize + 2; ++i)
            {
                auto t_param = nullColFlags[i]
                    ? (columns[i]->isNullAt(row_num)
                           ? StringRef()
                           : ((static_cast<const ColumnNullable *>(columns[i]))->getNestedColumn().getDataAtWithTerminatingZero(row_num)))
                    : columns[i]->getDataAt(row_num);
                params[i - 1] = StringRef(arena->insert(t_param.data, t_param.size), t_param.size);
            }
        };
        if constexpr (has_target_event)
        {
            if (event_type & (1 << 1))
            {
                // If no event is specified, all to be default?
                add_params();
            }
            // } else if constexpr (has_start_event) {
            //     if (event_type & (1<<0)) {
            //         add_params();
            //     }
        }
        else
        {
            add_params();
        }

        this->data(place).add(event_type, time, params, arena);
    }

    void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, Arena * arena) const override
    {
        this->data(place).merge(this->data(rhs), arena, paramSize);
    }

    void serialize(ConstAggregateDataPtr place, WriteBuffer & buf) const override { this->data(place).serialize(buf, paramSize); }

    void deserialize(AggregateDataPtr place, ReadBuffer & buf, Arena * arena) const override
    {
        this->data(place).deserialize(buf, arena, paramSize);
    }

    void insertResultInto(AggregateDataPtr __restrict place, IColumn & to, Arena *) const override
    {
        auto & data = const_cast<AggregateFunctionSessionAnalysisData &>(this->data(place));
        data.sort();
        auto & events = data.events;
        size_t size = events.size();
        size_t session_num = 0;

        auto & arr_col = static_cast<ColumnArray &>(to);
        auto & arr_offset = arr_col.getOffsets();
        auto & arr_data = static_cast<ColumnTuple &>(arr_col.getData());
        auto & duration_col = static_cast<ColumnUInt32 &>(arr_data.getColumn(0));
        auto & depth_col = static_cast<ColumnUInt32 &>(arr_data.getColumn(1));
        auto & session_time_col = static_cast<ColumnUInt64 &>(arr_data.getColumn(3));
        auto insert_session = [&](size_t start, std::vector<size_t> targets, size_t end) {
            // duration
            if constexpr (has_target_event)
            {
                UInt32 duration = (events[std::min(targets.back() + 1, end)].time - events[targets.back()].time);
                for (size_t i = 0, sentinel = targets.size() - 1; i < sentinel; ++i)
                    duration += (events[targets[i] + 1].time - events[targets[i]].time);

                duration_col.insertValue(duration);
            }
            else
            {
                duration_col.insertValue(events[end].time - events[start].time);
            }

            // depth
            if constexpr (has_target_event)
                depth_col.insertValue(targets.size());
            else
                depth_col.insertValue(end - start + 1);

            auto insert_param = [&](size_t index, bool nullable, StringRef & param) {
                if (nullable)
                {
                    if (param.size)
                    {
                        auto & nullable_col = static_cast<ColumnNullable &>(arr_data.getColumn(index));
                        nullable_col.getNestedColumn().insertData(param.data, param.size - 1); // nullable string with terminating zero
                        nullable_col.getNullMapData().push_back(0);
                    }
                    else
                        arr_data.getColumn(index).insertDefault();
                }
                else
                {
                    arr_data.getColumn(index).insertData(param.data, param.size);
                }
            };
            // end event
            insert_param(2, nullColFlags[0], events[end].params[0]);
            // session time
            session_time_col.insertValue(events[targets[0]].time);
            // params
            for (size_t i = 0; i < paramSize; ++i)
                insert_param(i + 4, nullColFlags[i + 2], events[targets[0]].params[i + 1]);

            ++session_num;
        };

        size_t i = 0;
        while (i < size)
        {
            size_t start = i, end = size;
            std::vector<size_t> targets;
            // UInt64 window_limit = (events[i].time/windowSize + 1) * windowSize;
            UInt64 session_limit = events[i].time + maxSessionSize;
            while (i < size)
            {
                //next start
                if constexpr (has_start_event)
                {
                    if (events[i].type & (1 << 0) && i != start)
                    {
                        --i;
                        break;
                    }
                }
                // out of limit
                if (events[i].time >= session_limit)
                {
                    --i;
                    break;
                }
                // if (events[i].time > window_limit) { // >= or >
                //     --i;
                //     break;
                // }
                //target
                if constexpr (has_target_event)
                {
                    if (events[i].type & (1 << 1))
                        targets.emplace_back(i);
                }
                //end
                if constexpr (has_end_event)
                {
                    if (events[i].type & (1 << 2))
                        break;
                }
                session_limit = events[i].time + maxSessionSize;
                ++i;
            }
            end = i == size ? size - 1 : i;
            ++i;
            if constexpr (has_target_event)
            {
                if (targets.empty())
                    continue;
            }
            else
            {
                targets.emplace_back(start);
            }
            //process session
            insert_session(start, targets, end);
            //next session
        }
        arr_offset.push_back(session_num + (arr_offset.size() ? arr_offset.back() : 0));
    }

    bool allocatesMemoryInArena() const override { return true; }

    bool handleNullItSelf() const override { return true; }

};

}
