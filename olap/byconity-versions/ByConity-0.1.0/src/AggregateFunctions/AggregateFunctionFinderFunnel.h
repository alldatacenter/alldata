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

#include <unordered_set>
#include <IO/ReadHelpers.h>
#include <IO/WriteHelpers.h>
#include <common/logger_useful.h>

#include <Common/ArenaAllocator.h>

#include <Columns/ColumnsNumber.h>
#include <Columns/ColumnArray.h>
#include <Columns/ColumnTuple.h>

#include <DataTypes/DataTypesNumber.h>
#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypeTuple.h>

#include <AggregateFunctions/IAggregateFunction.h>
#include <AggregateFunctions/AggregateFunnelCommon.h>

namespace DB
{

template <typename ParamType>
class AggregateFunctionFinderFunnel final : public IAggregateFunctionDataHelper<AggregateFunctionFinderFunnelData<ParamType>,
                                                                                AggregateFunctionFinderFunnel<ParamType>>
{
private:
    // Parameters got from agg function
    UInt64 m_watch_start; // start timestamp of 'per day' check
    UInt32 m_watch_step; // granuality of funnel count, e.g. per day
    UInt32 m_watch_numbers; // how many fine granuality checks. e.g. 30 for per day check for one month records
    UInt64 m_window; // only events in the window will be counted as conversion
    Int32 m_window_type; // funnel in the same day, or same week
    String time_zone;  // time_zone helps calculate the datetime a timestamp means for different area.

    size_t m_num_events;
    const DateLUTImpl & date_lut;

    UInt32 attr_related;
    UInt32 related_num;

    bool time_interval = false;
    mutable bool is_step = false;

    /*
     * The main loop for funnel calculate:
     * | A -> B -> C | will calculate once
     *
     */
    template<bool is_step, bool with_time, bool with_attr>
    void calculateFunnel(const AggregateFunctionFinderFunnelData<ParamType> & data,  LEVELType * levels, std::vector<Times>& intervals,
                         [[maybe_unused]] size_t start_step_num = 0, [[maybe_unused]] size_t end_step_num = 0) const
    {
        const_cast<AggregateFunctionFinderFunnelData<ParamType>&>(data).sort();
        auto const & events = data.event_lists;
        size_t num_events = events.size();

        if (num_events == 0) return;

        // relative means the window is not a fixed interval such as a day or a week, etc.
        // But the same day, or the same week.
        bool is_relative_window = m_window_type != 0;
        UInt64 funnel_window = m_window;
        [[maybe_unused]] ssize_t truncate_index = -1;

        size_t i = 0;
        std::vector<std::vector<size_t>> funnel_index;
        std::vector<size_t> s1, s2;
        funnel_index.reserve(2);
        s1.reserve(m_num_events);
        s2.reserve(m_num_events);

        funnel_index.emplace_back(s1);
        funnel_index.emplace_back(s2);
        std::vector<size_t> current_window_funnel;

        while (true)
        {
            size_t next_seq = 0;
            UInt32 slot_begin = 0, slot_end = 0, slot_idx = 0; // count base by slot
            UInt64 window_start = 0;
            UInt64 window_end = 0;
            int last_start = -1; // for slot smaller than window, need recheck within window start event
            // attribute related
            [[maybe_unused]] ParamType attr_check[2] = {};
            [[maybe_unused]] bool      attr_set[2] = {false, false};
            UInt64 start_window[2] = {0ULL,0ULL};

            if (i < num_events)
                slot_idx = events[i].stime / m_watch_step;

            while (i <= num_events)
            {
                if (unlikely(i == num_events)) // finish loop
                {
                    if (last_start == -1) ++i; // no start event in new slot
                    size_t max_arr = funnel_index[1].size() < funnel_index[0].size() ? 0 : 1;
                    if (current_window_funnel.size() < funnel_index[max_arr].size())
                        current_window_funnel.swap(funnel_index[max_arr]);

                    break;
                }

                auto stime = events[i].stime;
                auto ctime = events[i].ctime;
                auto event = events[i].event;

                // found best funnel
                if (funnel_index[next_seq].size() == m_num_events)
                {
                    current_window_funnel.swap(funnel_index[next_seq]);
                    break;
                }

                // check valid window
                if (window_start && (ctime > window_end))
                {
                    // 1. record the current max funnel
                    size_t max_arr = funnel_index[1].size() < funnel_index[0].size() ? 0 : 1;
                    if (current_window_funnel.size() < funnel_index[max_arr].size())
                        current_window_funnel = funnel_index[max_arr]; // need copy here

                    // find the longest funnel in the slot
                    if (current_window_funnel.size() == m_num_events)
                        break;

                    // 2. drop the outside window seq
                    size_t drop = start_window[0] > start_window[1] ? 1 : 0;

                    // still in the same slot goto next
                    size_t op = !drop;
                    bool has_second_chance = false;
                    if (start_window[op])
                    {
                        slot_begin = slot_idx * m_watch_step;
                        if (is_relative_window)
                        {
                            funnel_window = setValidWindow(start_window[op], date_lut);
                            slot_end = slot_begin + funnel_window/1000 + 1; // exclusive
                        }
                        else
                            slot_end = slot_begin + m_watch_step; // exclusive

                        has_second_chance = (ctime <= (start_window[op] + funnel_window));
                    }

                    if ((stime >= slot_begin && stime < slot_end) || has_second_chance)
                    {
                        funnel_index[drop].resize(0);
                        window_start = 0; // new window

                        // fix
                        if (has_second_chance)
                        {
                            window_start = start_window[op];
                            window_end   = window_start + funnel_window;
                            next_seq = op;
                        }
                        else
                        {
                            funnel_index[op].resize(0);
                            if constexpr (with_attr)
                            {
                                attr_check[op] = {};
                                attr_set[op] = false;
                            }
                        }

                        if constexpr (with_attr)
                        {
                            attr_check[drop] = {};
                            attr_set[drop] = false;
                        }
                    }
                    else
                        break;
                }

                if (event & 0x1ULL) //new start event
                {
                    if constexpr (is_step)
                    {
                        if (slot_idx < start_step_num || slot_idx >= end_step_num)
                        {
                            if (truncate_index == -1 && slot_idx >= end_step_num)
                                truncate_index = i;

                            ++i;
                            continue;
                        }
                    }
                    else
                    {
                        if (slot_idx >= m_watch_numbers)
                        {
                            ++i;
                            continue;
                        }
                    }

                    // the start event must be in the same slot
                    if ((stime / m_watch_step) == slot_idx || (event > 1 && !funnel_index[next_seq].empty()))
                    {
                        //funnel for same event
                        if (event > 1 && !funnel_index[next_seq].empty() && isNextLevel(event, funnel_index[next_seq].size()))
                        {
                            bool is_legal = true;
                            if constexpr (with_attr)
                            {
                                if (event & attr_related)
                                {
                                    if (attr_set[next_seq])
                                    {
                                        if (attr_check[next_seq] != events[i].param) // attr not match
                                            is_legal = false;
                                    }
                                    else
                                    {
                                        attr_check[next_seq] = events[i].param;
                                        attr_set[next_seq] = true;
                                    }
                                }
                            }

                            if (is_legal)
                            {
                                if (funnel_index[next_seq].size() == 1)
                                    if (last_start == -1) last_start = i;

                                funnel_index[next_seq].push_back(i);
                            }
                            ++i;
                            continue;
                        }
                        else if (event > 1 && !funnel_index[next_seq].empty())
                        {
                            // for A->A->A ..., only use one seq
                            ++i;
                            continue;
                        }

                        if constexpr (with_attr)
                        {
                            if (!funnel_index[0].empty() && !funnel_index[1].empty())
                            {
                                // may have different attr from start event
                                if (last_start == -1) last_start = i;
                                ++i;
                                continue;
                            }
                        }

                        next_seq = funnel_index[0].size() > funnel_index[1].size() ? 1 : 0;
                        bool need_update_window = false;
                        if (funnel_index[next_seq].size() > 1)
                        {
                            if (funnel_index[0].size() != funnel_index[1].size())
                            {
                                if (last_start == -1) last_start = i;
                                ++i;
                                continue;
                            }
                            else
                            {
                                next_seq = start_window[0] > start_window[1] ? 1 : 0;
                                need_update_window = true;
                            }
                        }
                        else if (funnel_index[next_seq].size() == 1 && funnel_index[!next_seq].size() == 1)
                        {
                            next_seq = start_window[0] > start_window[1] ? 1 : 0; // new start replace the old one
                            need_update_window = true;
                        }

                        funnel_index[next_seq].resize(0);
                        funnel_index[next_seq].push_back(i);
                        start_window[next_seq] = ctime;

                        if (window_start == 0 || need_update_window)
                        {
                            window_start = start_window[!next_seq] ? start_window[!next_seq] : ctime;
                            slot_begin = slot_idx * m_watch_step;

                            if (is_relative_window)
                            {
                                funnel_window = setValidWindow(window_start, date_lut);
                                slot_end = slot_begin + funnel_window/1000 + 1; // exclusive
                            }
                            else
                                slot_end = slot_begin + m_watch_step; // exclusive

                            window_end = window_start + funnel_window;
                        }
                        else if (window_start != start_window[0] && window_start != start_window[1])
                        {
                            window_start = start_window[0] >  start_window[1] ? start_window[1] : start_window[0];
                            slot_begin = slot_idx * m_watch_step;

                            if (is_relative_window)
                            {
                                funnel_window = setValidWindow(window_start, date_lut);
                                slot_end = slot_begin + funnel_window/1000 + 1; // exclusive
                            }
                            else
                                slot_end = slot_begin + m_watch_step; // exclusive

                            window_end = window_start + funnel_window;
                        }

                        if constexpr (with_attr)
                        {
                            if (event & attr_related)
                            {
                                attr_check[next_seq] = events[i].param;
                                attr_set[next_seq] = true;
                            }
                        }
                    }
                    else
                    {
                        if (last_start == -1 && ((stime / m_watch_step) > slot_idx))
                            last_start = i;
                    }
                }
                else if (isNextLevel(event, funnel_index[next_seq].size()))
                {
                    if constexpr (with_attr)
                    {
                        if (event & attr_related)
                        {
                            if (attr_set[next_seq])
                            {
                                if (attr_check[next_seq] != events[i].param) // attr not match
                                {
                                    // check the other seq with attr
                                    if (isNextLevel(event, funnel_index[!next_seq].size()))
                                    {
                                        if (attr_set[!next_seq] && attr_check[!next_seq] == events[i].param)
                                            funnel_index[!next_seq].push_back(i);
                                    }
                                    ++i;
                                    continue;
                                }
                            }
                            else
                            {
                                attr_check[next_seq] = events[i].param;
                                attr_set[next_seq] = true;
                            }
                        }
                    }

                    funnel_index[next_seq].push_back(i);
                    // same middle event : ((flag_event & 1) == 0) && (flag_event & (flag_event - 1))
                    if ((event & (event - 1)) && isNextLevel(event, funnel_index[!next_seq].size()))
                    {
                        // this event can be reuse in the other seq
                        if constexpr (with_attr)
                        {
                            if (event & attr_related)
                            {
                                if (attr_set[!next_seq])
                                {
                                    if (attr_check[!next_seq] != events[i].param) // attr not match
                                    {
                                        ++i;
                                        continue;
                                    }
                                }
                                else
                                {
                                    attr_check[!next_seq] = events[i].param;
                                    attr_set[!next_seq] = true;
                                }
                            }
                        }

                        funnel_index[!next_seq].push_back(i);
                    }
                }
                else if (isNextLevel(event, funnel_index[!next_seq].size()))
                {
                    if constexpr (with_attr)
                    {
                        if (event & attr_related)
                        {
                            if (attr_set[!next_seq])
                            {
                                if (attr_check[!next_seq] != events[i].param) // attr not match
                                {
                                    ++i;
                                    continue;
                                }
                            }
                            else
                            {
                                attr_check[!next_seq] = events[i].param;
                                attr_set[!next_seq] = true;
                            }
                        }
                    }

                    funnel_index[!next_seq].push_back(i);
                }

                ++i;
            }

            // one slot end, count funnel
            auto funnel  = current_window_funnel.size();
            if (funnel > 0)
            {
                levels[0] = std::max<UInt64>(levels[0], funnel);
                // # per day funnel level
                if (levels[slot_idx + 1] < funnel)
                {
                    levels[slot_idx + 1] = funnel;
                    if constexpr (with_time)
                    {
                        // time interval
                        Times cur_times;
                        for (const auto & index : current_window_funnel)
                        {
                            cur_times.push_back(events[index].ctime);
                        }
                        intervals[slot_idx + 1] = std::move(cur_times);
                    }
                }
                // levels[slot_idx + 1] = std::max<UInt64>(levels[slot_idx + 1], funnel);
            }

            // skip to next slot index
            if (!is_relative_window && last_start == -1)
            {
                while (i < num_events)
                {
                    if ((events[i].stime / m_watch_step) <= slot_idx)
                        ++i;
                    else
                        break;
                }
            }

            // start new round
            i = last_start != -1 ? last_start : i;
            if (i >= num_events)
                break;

            funnel_index[0].resize(0);
            funnel_index[1].resize(0);
            current_window_funnel.resize(0);
        }

        if constexpr (is_step)
        {
            if (truncate_index < 0) truncate_index = num_events;
            const_cast<AggregateFunctionFinderFunnelData<ParamType>&>(data).truncate(truncate_index);
        }
    }

public:

    AggregateFunctionFinderFunnel(UInt64 window, UInt64 watchStart, UInt64 watchStep,
                                  UInt64 watchNumbers, UInt64 window_type, String time_zone_,
                                  UInt64 numVirts, UInt32 attr_related_, bool time_interval_,
                                  const DataTypes & arguments, const Array & params) :
        IAggregateFunctionDataHelper<AggregateFunctionFinderFunnelData<ParamType>,
                                     AggregateFunctionFinderFunnel<ParamType> >(arguments, params),
        m_watch_start(watchStart), m_watch_step(watchStep), m_watch_numbers(watchNumbers),
        m_window(window), m_window_type(window_type), time_zone(time_zone_), m_num_events(numVirts),
        date_lut(DateLUT::instance(time_zone_)), attr_related(attr_related_), time_interval(time_interval_)
    {
        related_num = attr_related ? __builtin_popcount(attr_related) + 2 : 2;
    }

    String getName() const override
    {
        return "finderFunnel";
    }

    void create(const AggregateDataPtr place) const override
    {
        new (place) AggregateFunctionFinderFunnelData<ParamType>;
    }

    DataTypePtr getReturnTypeWithTimeInterval() const
    {
        DataTypes types;
        types.emplace_back(std::make_shared<DataTypeArray>(std::make_shared<DataTypeNumber<UInt8>>()));
        types.emplace_back(std::make_shared<DataTypeArray>(std::make_shared<DataTypeArray>(std::make_shared<DataTypeNumber<UInt64>>())));
        return std::make_shared<DataTypeTuple>(types);
    }

    DataTypePtr getReturnType() const override
    {
        if (time_interval)
            return getReturnTypeWithTimeInterval();

        return std::make_shared<DataTypeArray>(std::make_shared<DataTypeNumber<UInt8> >());
    }

    void add(AggregateDataPtr place, const IColumn ** columns, size_t row_num, Arena *) const override
    {
        // server_timestamp, client_timestamp, event_flag_1, event_flag_2 .....
        auto s_time = static_cast<const ColumnVector<UInt64> *>(columns[0])->getData()[row_num];
        auto c_time = static_cast<const ColumnVector<UInt64> *>(columns[1])->getData()[row_num];

        // Ignore those columns which start earlier than watch point
        if (unlikely(s_time < m_watch_start))
            return;

        UInt32 stime = static_cast<UInt32>(s_time - m_watch_start);

        UInt64 flag_event = 0;
        int event_index_offset = attr_related ? related_num : 2; // start offset of event flag column, need skip the attr column
        for(size_t i = 0; i < m_num_events; i++)
        {
            UInt64 flag = (static_cast<const ColumnVector<UInt8> *>(columns[i + event_index_offset])->getData()[row_num] != 0);
            flag_event |= (flag << i);
        }

        if (unlikely(flag_event == 0)) return; // Mostly filter ensure none empty

        if (attr_related & flag_event)
        {
            int index = __builtin_popcount(((flag_event & attr_related) -1) & attr_related);
            // get correspond param column
            ParamType attr = getAttribution<ParamType>(columns, row_num, index + 2);
            this->data(place).add(stime, c_time, flag_event, attr);
        }
        else
        {
            this->data(place).add(stime, c_time, flag_event);
        }
    }

    void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, Arena * arena) const override
    {
        this->data(place).merge(this->data(rhs), arena);
    }

    void serialize(ConstAggregateDataPtr place, WriteBuffer & buf) const override
    {
        this->data(place).serialize(buf);
    }

    void deserialize(AggregateDataPtr place, ReadBuffer & buf, Arena *arena) const override
    {
        this->data(place).deserialize(buf, arena);
    }

    inline bool needCalculateStep(AggregateDataPtr place) const override
    {
        return !this->data(place).event_lists.empty();
    }

    void calculateStepResult(AggregateDataPtr place, size_t start_step_num, size_t end_step_num, bool, Arena * arena) const override
    {
        is_step = true;
        if (this->data(place).levels.size() != (m_watch_numbers + 1))
        {
            this->data(place).levels.resize_fill(m_watch_numbers + 1, 0, arena);
        }

        if (time_interval)
        {
            if (this->data(place).intervals.size() != (m_watch_numbers + 1))
            {
                this->data(place).intervals.resize(m_watch_numbers + 1);
            }

            if (attr_related > 0)
                calculateFunnel<true, true, true>(this->data(place), &(this->data(place).levels[0]), this->data(place).intervals, start_step_num, end_step_num);
            else
                calculateFunnel<true, true, false>(this->data(place), &(this->data(place).levels[0]), this->data(place).intervals, start_step_num, end_step_num);
        }
        else
        {
            if (attr_related > 0)
                calculateFunnel<true, false, true>(this->data(place), &(this->data(place).levels[0]), this->data(place).intervals, start_step_num, end_step_num);
            else
                calculateFunnel<true, false, false>(this->data(place), &(this->data(place).levels[0]), this->data(place).intervals, start_step_num, end_step_num);
        }
    }

    void insertResultWithIntervals(ConstAggregateDataPtr place, ColumnTuple & tuple_to) const
    {
        ColumnArray& levels_to = static_cast<ColumnArray &>(tuple_to.getColumn(0));
        ColumnArray::Offsets& levels_to_offset = levels_to.getOffsets();
        size_t prev_offset = (levels_to_offset.empty() ? 0 : levels_to_offset.back());
        levels_to_offset.push_back(prev_offset + m_watch_numbers + 1);

        auto& data_to = static_cast<ColumnVector<LEVELType> &>(levels_to.getData()).getData();
        data_to.insert_nzero(m_watch_numbers + 1);
        LEVELType* levels = &(data_to[prev_offset]);
        ColumnArray& intervals_to = static_cast<ColumnArray &>(tuple_to.getColumn(1));

        if (is_step)
        {
            const LEVELs & data_levels = this->data(place).levels;
            auto min_size = std::min<size_t>(data_levels.size(), (m_watch_numbers  + 1));
            std::memcpy(levels, data_levels.data(), min_size * sizeof(LEVELType));

            auto & data_intervals = const_cast<std::vector<Times> &>(this->data(place).intervals);
            for (Times& times : data_intervals)
                adjacent_difference(times.begin(), times.end(), times.begin());

            insertNestedVectorNumberIntoColumn(intervals_to, data_intervals);
        }
        else
        {
            std::vector<Times> intervals;
            intervals.resize(m_watch_numbers + 1);

            if (attr_related > 0)
                calculateFunnel<false, true, true>(this->data(place), levels, intervals);
            else
                calculateFunnel<false, true, false>(this->data(place), levels, intervals);

            for (Times& times : intervals)
                adjacent_difference(times.begin(), times.end(), times.begin());

            insertNestedVectorNumberIntoColumn(intervals_to, intervals);
        }
    }

    void insertResultInto(AggregateDataPtr __restrict place, IColumn & to, Arena * ) const override
    {
        if (time_interval)
        {
            ColumnTuple & tuple_to = static_cast<ColumnTuple &>(to);
            insertResultWithIntervals(place, tuple_to);
        }
        else
        {
            ColumnArray & arr_to = static_cast<ColumnArray &>(to);
            ColumnArray::Offsets& offsets_to = arr_to.getOffsets();
            size_t prev_offset = (offsets_to.empty() ? 0 : offsets_to.back());
            offsets_to.push_back(prev_offset + m_watch_numbers + 1);

            typename ColumnVector<LEVELType>::Container& data_to = static_cast<ColumnVector<LEVELType> &>(arr_to.getData()).getData();
            data_to.insert_nzero(m_watch_numbers + 1);
            LEVELType* levels = &(data_to[prev_offset]);

            if (is_step)
            {
                const LEVELs & data_levels = this->data(place).levels;
                auto min_size = std::min<size_t>(data_levels.size(), (m_watch_numbers  + 1));
                std::memcpy(levels, data_levels.data(), min_size * sizeof(LEVELType));
            }
            else
            {
                std::vector<Times> tmp;
                if (attr_related > 0)
                    calculateFunnel<false, false, true>(this->data(place), levels, tmp);
                else
                    calculateFunnel<false, false, false>(this->data(place), levels, tmp);
            }
        }
    }

    static void addFree(const IAggregateFunction * that, AggregateDataPtr place, const IColumn ** columns, size_t row_num, Arena * arena)
    {
        static_cast<const AggregateFunctionFinderFunnel<ParamType> &>(*that).add(place, columns, row_num, arena);
    }

    IAggregateFunction::AddFunc getAddressOfAddFunction() const override final { return &addFree; }

    bool allocatesMemoryInArena() const override
    {
        return true;
    }
};
}
