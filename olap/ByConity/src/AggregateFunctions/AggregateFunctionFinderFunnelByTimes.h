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
class AggregateFunctionFinderFunnelByTimes final : public IAggregateFunctionDataHelper<AggregateFunctionFinderFunnelData<ParamType>,
                                                                                       AggregateFunctionFinderFunnelByTimes<ParamType> >
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
    UInt32 target_step;

    /*
     * The main loop for funnel calculate:
     * | A -> B -> C | will calculate once
     *
     */
    template<bool is_step, bool with_time, bool with_attr>
    void calculateFunnel(const AggregateFunctionFinderFunnelData<ParamType> & data,  LEVELType * levels, std::vector<Times>& intervals,
                         [[maybe_unused]] size_t start_step_num = 0, [[maybe_unused]] size_t end_step_num = 0) const
    {
        auto &data_origin = const_cast<AggregateFunctionFinderFunnelData<ParamType>&>(data);
        data_origin.sort();
        auto & events = data_origin.event_lists;
        size_t num_events = events.size();

        if (num_events == 0) return;

        // relative means the window is not a fixed interval such as a day or a week, etc.
        // But the same day, or the same week.
        bool is_relative_window = m_window_type != 0;
        UInt64 funnel_window = m_window;
        [[maybe_unused]] ssize_t truncate_index = -1;

        size_t i = 0;
        std::vector<size_t> funnel_index;

        auto countFunnel = [&](std::vector<size_t> &current_window_funnel, UInt32 slot_idx)
        {
            auto funnel  = current_window_funnel.size();
            if (funnel > 0)
            {
                // first the total count
                for (size_t e = 0; e < m_num_events; e++)
                    levels[e] += (funnel > e);

                // the slot count
                size_t output_offset = (slot_idx + 1) * m_num_events;
                for (size_t e = 0; e < m_num_events; e++)
                    levels[output_offset + e] += (funnel > e);

                if constexpr (with_time)
                {
                    if (target_step == 0 && funnel == m_num_events)
                    {
                        UInt64 interval = 0;
                        for (size_t ii = 0; ii < funnel - 1; ii++)
                            interval += events[current_window_funnel[ii+1]].ctime - events[current_window_funnel[ii]].ctime;

                        intervals[slot_idx + 1].push_back(interval);
                    }

                    if (target_step > 0 && funnel > target_step)
                    {
                        UInt64 interval = events[current_window_funnel[target_step]].ctime - events[current_window_funnel[target_step-1]].ctime;
                        intervals[slot_idx + 1].push_back(interval);
                    }
                }

                for (const auto index : current_window_funnel)
                    events[index].event = 0;

                current_window_funnel.resize(0);
            }
        };

        while (true)
        {
            UInt32 slot_begin = 0, slot_end = 0, slot_idx = 0; // count base by slot
            UInt64 window_start = 0;
            UInt64 window_end = 0;
            int last_start = -1; // for slot smaller than window, need recheck within window start event
            // attribute related
            [[maybe_unused]] ParamType attr_check = {};
            [[maybe_unused]] bool      attr_set = {false};

            if (i < num_events)
                slot_idx = events[i].stime / m_watch_step;

            while (i <= num_events)
            {
                if (unlikely(i == num_events)) // finish loop
                {
                    if (last_start == -1) ++i; // no start event in new slot
                    break;
                }

                auto stime = events[i].stime;
                auto ctime = events[i].ctime;
                auto event = events[i].event;

                // found best funnel, stop and count
                if (funnel_index.size() == m_num_events)
                    break;

                // check valid window
                if (window_start && (ctime > window_end))
                {
                    // 1. record the current max funnel
                    countFunnel(funnel_index, slot_idx);
                    if ((stime >= slot_begin && stime < slot_end))
                    {
                        window_start = 0; // new window
                        if constexpr (with_attr)
                        {
                            attr_check = {};
                            attr_set = false;
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
                    if ((stime / m_watch_step) == slot_idx || (event > 1 && !funnel_index.empty()))
                    {
                        if (event == 1 && !funnel_index.empty())
                        {
                            // new start A event
                            if (last_start == -1) last_start = i;
                            ++i;
                            continue;
                        }

                        //funnel for same event
                        if (event > 1 && !funnel_index.empty() && isNextLevel(event, funnel_index.size()))
                        {
                            bool is_legal = true;
                            if constexpr (with_attr)
                            {
                                if (event & attr_related)
                                {
                                    if (attr_set)
                                    {
                                        if (attr_check != events[i].param) // attr not match
                                            is_legal = false;
                                    }
                                    else
                                    {
                                        attr_check = events[i].param;
                                        attr_set = true;
                                    }
                                }
                            }

                            if (is_legal)
                            {
                                if (funnel_index.size() == 1)
                                    if (last_start == -1) last_start = i;

                                funnel_index.push_back(i);
                            }
                            ++i;
                            continue;
                        }
                        else if (event > 1 && !funnel_index.empty())
                        {
                            // for A->A->A ..., only use one seq
                            ++i;
                            continue;
                        }

                        if constexpr (with_attr)
                        {
                            if (!funnel_index.empty())
                            {
                                // may have different attr from start event
                                if (last_start == -1) last_start = i;
                                ++i;
                                continue;
                            }
                        }

                        funnel_index.push_back(i);
                        window_start = ctime;

                        slot_begin = slot_idx * m_watch_step;

                        if (is_relative_window)
                        {
                            funnel_window = setValidWindow(window_start, date_lut);
                            slot_end = slot_begin + funnel_window/1000 + 1; // exclusive
                        }
                        else
                            slot_end = slot_begin + m_watch_step; // exclusive

                        window_end = window_start + funnel_window;

                        if constexpr (with_attr)
                        {
                            if (event & attr_related)
                            {
                                attr_check = events[i].param;
                                attr_set = true;
                            }
                        }
                    }
                    else
                    {
                        if (last_start == -1 && ((stime / m_watch_step) > slot_idx))
                            last_start = i;
                    }
                }
                else if (isNextLevel(event, funnel_index.size()))
                {
                    if constexpr (with_attr)
                    {
                        if (event & attr_related)
                        {
                            if (attr_set)
                            {
                                if (attr_check != events[i].param) // attr not match
                                {
                                    ++i;
                                    continue;
                                }
                            }
                            else
                            {
                                attr_check = events[i].param;
                                attr_set = true;
                            }
                        }
                    }

                    funnel_index.push_back(i);
                }
                else
                {
                }

                ++i;
            }

            // count funnel
            countFunnel(funnel_index, slot_idx);

            // start new round
            i = last_start != -1 ? last_start : i;
            if (i >= num_events)
                break;

            funnel_index.resize(0);
        }

        if constexpr (is_step)
        {
            if (truncate_index < 0) truncate_index = num_events;
            const_cast<AggregateFunctionFinderFunnelData<ParamType>&>(data).truncate(truncate_index);
        }
    }

public:

    AggregateFunctionFinderFunnelByTimes(UInt64 window, UInt64 watch_start, UInt64 watch_step,
                                         UInt64 watch_numbers, UInt64 window_type, String time_zone_,
                                         UInt64 numVirts, UInt32 attr_related_, bool time_interval_, UInt32 target_step_,
                                         const DataTypes & arguments, const Array & params) :
        IAggregateFunctionDataHelper<AggregateFunctionFinderFunnelData<ParamType>,
                                     AggregateFunctionFinderFunnelByTimes<ParamType> >(arguments, params),
        m_watch_start(watch_start), m_watch_step(watch_step), m_watch_numbers(watch_numbers),
        m_window(window), m_window_type(window_type), time_zone(time_zone_), m_num_events(numVirts),
        date_lut(DateLUT::instance(time_zone_)), attr_related(attr_related_), time_interval(time_interval_), target_step(target_step_)
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
        {
            return;
        }

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
        if (this->data(place).levels.size() != (m_watch_numbers + 1) * m_num_events)
            this->data(place).levels.resize_fill((m_watch_numbers + 1) * m_num_events, 0, arena);

        if (time_interval)
        {
            if (this->data(place).intervals.size() != (m_watch_numbers + 1))
                this->data(place).intervals.resize(m_watch_numbers + 1);

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
        levels_to_offset.push_back(prev_offset + (m_watch_numbers  + 1) * m_num_events);

        auto& data_to = static_cast<ColumnVector<LEVELType> &>(levels_to.getData()).getData();
        data_to.insert_nzero((m_watch_numbers  + 1) * m_num_events);
        LEVELType* levels = &(data_to[prev_offset]);
        ColumnArray& intervals_to = static_cast<ColumnArray &>(tuple_to.getColumn(1));

        if (is_step)
        {
            const LEVELs & data_levels = this->data(place).levels;
            auto min_size = std::min<size_t>(data_levels.size(), (m_watch_numbers  + 1) * m_num_events);
            std::memcpy(levels, data_levels.data(), min_size * sizeof(LEVELType));
            auto & data_intervals = const_cast<std::vector<Times> &>(this->data(place).intervals);
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
            offsets_to.push_back(prev_offset + (m_watch_numbers  + 1) * m_num_events);

            typename ColumnVector<LEVELType>::Container& data_to = static_cast<ColumnVector<LEVELType> &>(arr_to.getData()).getData();
            data_to.insert_nzero((m_watch_numbers  + 1) * m_num_events);
            LEVELType* levels = &(data_to[prev_offset]);

            if (is_step)
            {
                const LEVELs & data_levels = this->data(place).levels;
                auto min_size = std::min<size_t>(data_levels.size(), (m_watch_numbers  + 1) * m_num_events);
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
        static_cast<const AggregateFunctionFinderFunnelByTimes<ParamType> &>(*that).add(place, columns, row_num, arena);
    }

    IAggregateFunction::AddFunc getAddressOfAddFunction() const override final { return &addFree; }

    bool allocatesMemoryInArena() const override { return true; }
};
}
