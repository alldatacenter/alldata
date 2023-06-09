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

#include <AggregateFunctions/IAggregateFunction.h>
#include <Common/Stopwatch.h>
#include <common/logger_useful.h>
#include <Columns/ColumnVector.h>
#include <Columns/ColumnString.h>
#include <Columns/ColumnTuple.h>
#include <Columns/ColumnArray.h>
#include <Columns/ColumnNullable.h>
#include <DataTypes/DataTypeTuple.h>
#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypesNumber.h>
#include <DataTypes/DataTypeNullable.h>
#include <IO/WriteHelpers.h>
#include <IO/ReadHelpers.h>
#include <pdqsort.h>
#include <Poco/Logger.h>

namespace DB
{

namespace ErrorCodes
{
extern const int LOGICAL_ERROR;
extern const int ILLEGAL_TYPE_OF_ARGUMENT;
extern const int NUMBER_OF_ARGUMENTS_DOESNT_MATCH;
extern const int TOO_MANY_ARGUMENTS_FOR_FUNCTION;
}

/**
 * page_view event: time( = start_time = end_time), url, refer
 * be_active event: start_time, end_time, url
 */
struct SessionEvent
{
    enum EventType: uint8_t
    {
        KnownType,
        PageView,
        BeActive
    };

    UInt64 server_time;
    EventType event = KnownType;
    UInt64 time;
    UInt64 start_time;
    UInt64 end_time;
    StringRef url;
    StringRef refer_type; // sessionSplitR2 => refer
    std::vector<StringRef> args;

    SessionEvent() = default;

    SessionEvent(UInt64 server_time_, EventType event_, UInt32 time_,
                 UInt64 start_time_, UInt64 end_time_, StringRef url_,
                 StringRef refer_type_, const std::vector<StringRef> & args_ = {}) :
        server_time(server_time_), event(event_), time(time_),
        start_time(start_time_), end_time(end_time_), url(url_),
        refer_type(refer_type_), args(args_)
    {}

    inline UInt64 getStartTime() const { return event == PageView ? time : start_time; }
    inline UInt64 getEndTime() const { return event == PageView ? time : end_time; }
    inline bool isPageView() const { return event == PageView; }

    void serialize(WriteBuffer &buf) const
    {
        writeBinary(server_time, buf);
        writeBinary(static_cast<UInt8>(event), buf);
        writeBinary(time, buf);
        writeBinary(start_time, buf);
        writeBinary(end_time, buf);
        writeStringBinary(url, buf);
        writeStringBinary(refer_type, buf);
        writeVectorBinary(args, buf);
    }

    void deserialize(ReadBuffer &buf, Arena * arena)
    {
        readBinary(server_time, buf);
        UInt8 event_type;
        readBinary(event_type, buf);
        event = static_cast<EventType>(event_type);
        readBinary(time, buf);
        readBinary(start_time, buf);
        readBinary(end_time, buf);
        url = readStringBinaryInto(*arena, buf);
        refer_type = readStringBinaryInto(*arena, buf);
        readStringRefsBinary(args, buf, *arena);
    }

    static EventType mapEvent(StringRef event_)
    {
        if (event_ == "predefine_pageview")
            return EventType::PageView;
        else if (event_ == "_be_active")
            return EventType::BeActive;
        else
            return EventType::KnownType;
    }
};


struct AggregateFunctionSessionSplitData
{
    /// TODO: change std::vector to PODArray
    /// PODArray will core dump, since class SessionEvent contain vector<StringRef>
    std::vector<SessionEvent> events;
    PaddedPODArray<size_t> order;
    bool sorted = true;

    void sort()
    {
        size_t size = events.size();
        order.resize(size);
        for (size_t i = 0; i < size; ++i)
            order[i] = i;

        if(sorted)
            return;

        Stopwatch watch;
        pdqsort(order.begin(), order.end(), [&](const auto & lhs, const auto & rhs) { return events[lhs].time < events[rhs].time; });

        double elapsed = watch.elapsedSeconds();

        std::stringstream log_helper;
        log_helper << std::fixed << std::setprecision(3)
                   << "Sorted " << std::to_string(size) << " rows SessionEvent data."
                   << " in " << elapsed << " sec.";

        LOG_TRACE(&Poco::Logger::get(__PRETTY_FUNCTION__), log_helper.str());
        sorted = true;
    }

    auto get(size_t i) const { return events.begin() + order[i]; }

    void add(UInt64 server_time, SessionEvent::EventType event, UInt64 time, UInt64 start_time, UInt64 end_time, StringRef url, StringRef refer_type, const std::vector<StringRef> & args = {})
    {
        if(sorted && !events.empty() && events.back().time > time)
            sorted = false;
        events.emplace_back(server_time, event, time, start_time, end_time, url, refer_type, args);
    }

    void merge(const AggregateFunctionSessionSplitData &other, Arena * arena)
    {
        sorted = false;
        size_t size = events.size();
        Stopwatch watch;
        events.insert(events.end(), std::begin(other.events), std::end(other.events));

        // copy other.arena
        for(size_t i = size; i < events.size(); ++i)
        {
            auto & url = events[i].url;
            auto & refer_type= events[i].refer_type;
            char * url_data = arena->alloc(url.size);
            char * refer_data = arena->alloc(refer_type.size);
            strncpy(url_data, url.data, url.size);
            strncpy(refer_data, refer_type.data, refer_type.size);
            events[i].url = StringRef(url_data, url.size);
            events[i].refer_type = StringRef(refer_data, refer_type.size);

            auto & args = events[i].args;
            for(auto & arg: args)
            {
                char * data = arena->alloc(arg.size);
                strncpy(data, arg.data, arg.size);
                arg = StringRef(data, arg.size);
            }
        }

        double elapsed = watch.elapsedSeconds();

        std::stringstream log_helper;
        log_helper << std::fixed << std::setprecision(3)
                   << "Merged " << std::to_string(other.events.size()) << " rows SessionEvent data."
                   <<  " in " << elapsed << " sec."
                   << " (" << other.events.size() / elapsed << " rows/sec.)";

        LOG_TRACE(&Poco::Logger::get(__PRETTY_FUNCTION__), log_helper.str());
    }

    void serialize(WriteBuffer & buf) const
    {
        size_t size = events.size();
        writeBinary(size, buf);
        for (size_t i = 0; i < size; ++i)
        {
            events[i].serialize(buf);
        }
    }

    void deserialize(ReadBuffer & buf, Arena * arena)
    {
        size_t size;
        readBinary(size, buf);
        events.resize(size);
        for (size_t i = 0; i < size; ++i)
        {
            events[i].deserialize(buf, arena);
        }
    }
};

struct Session
{
    UInt32 session_duration;
    UInt32 session_depth;
    StringRef first_page_param;
    StringRef last_page_param;
    Session(UInt32 session_duration_, UInt32 session_depth_, StringRef first_page_param_, StringRef last_page_param_) :
        session_duration(session_duration_), session_depth(session_depth_), first_page_param(first_page_param_), last_page_param(last_page_param_) {}
};

class AggregateFunctionSessionSplitR2 final : public IAggregateFunctionDataHelper<AggregateFunctionSessionSplitData, AggregateFunctionSessionSplitR2>
{
private:
    UInt64 m_session_split_time;
    UInt64 m_window_size;
    UInt64 m_base_time;
    UInt8 type;

    /**
     * type = 0 fist page_view 相关属性
     * type = 1 last page_view 相关属性
     * type = 2 first page_view 第一个属性 + last page_view 第二个属性
     * Notes: 第一个属性应该是url, 第二个属性为referer
     */

public:
    AggregateFunctionSessionSplitR2(UInt64 watch_tart_, UInt64 window_size_, UInt64 base_time_, UInt8 type_, const DataTypes & arguments, const Array & params):
        IAggregateFunctionDataHelper<AggregateFunctionSessionSplitData, AggregateFunctionSessionSplitR2>(arguments, params),
        m_session_split_time(watch_tart_), m_window_size(window_size_), m_base_time(base_time_), type(type_) {}

    String getName() const override { return "sessionSplitR2"; }

    void create(AggregateDataPtr place) const override
    {
        new(place) AggregateFunctionSessionSplitData;
    }

    /**
     * Session description:
     * (duration, depth, is_jmp, entry_url, exit_url)
     * [tuple(UInt32, UInt32, UInt8, String, String), tuple(...)]
     */
    DataTypePtr getReturnType() const override
    {
        DataTypes types {
            std::make_shared<DataTypeUInt32>(), // session duration
            std::make_shared<DataTypeUInt32>(), // session depth
            std::make_shared<DataTypeString>(),
            std::make_shared<DataTypeString>()
        };
        return std::make_shared<DataTypeArray>(std::make_shared<DataTypeTuple>(types));
    }

    void add(AggregateDataPtr place, const IColumn **columns, size_t row_num, Arena * arena) const override
    {
        if (dynamic_cast<const ColumnNullable *>(columns[3]))
            std::cout << "Nullable Column" << std::endl;
        else if (dynamic_cast<const ColumnUInt64 *>(columns[3]))
            std::cout << "UInt64 column" << std::endl;
        else if (dynamic_cast<const ColumnInt64 *>(columns[3]))
            std::cout << "Int64 column" << std::endl;
        else
            std::cout << "Known column" << std::endl;

        UInt64 server_time = assert_cast<const ColumnUInt64 *>(columns[0])->getData()[row_num];
        auto event = SessionEvent::mapEvent(assert_cast<const ColumnString *>(columns[1])->getDataAt(row_num));
        UInt64 event_time = assert_cast<const ColumnUInt64 *>(columns[2])->getData()[row_num];

        if (event == SessionEvent::KnownType || server_time < m_base_time)
            return;

        UInt64 start_time = 0;
        UInt64 end_time = 0;

        if (event == SessionEvent::BeActive)
        {
            start_time = assert_cast<const ColumnUInt64 *>(columns[3])->getData()[row_num];
            end_time = assert_cast<const ColumnUInt64 *>(columns[4])->getData()[row_num];

            if (start_time >= std::numeric_limits<UInt32>::max()) // ms
                start_time /= 1000;
            if (end_time >= std::numeric_limits<UInt32>::max()) // ms
                end_time /= 1000;

            if (start_time > end_time)
                return;
        }

        const auto url = assert_cast<const ColumnString *>(columns[5])->getDataAt(row_num);
        const auto ref = assert_cast<const ColumnString *>(columns[6])->getDataAt(row_num);

        char * url_data = arena->alloc(url.size);
        char * refer_data = arena->alloc(ref.size);
        strncpy(url_data, url.data, url.size);
        strncpy(refer_data, ref.data, ref.size);

        this->data(place).add(server_time, event, event_time, start_time, end_time, {url_data, url.size}, {refer_data, ref.size});
    }

    void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, Arena * arena) const override
    {
        this->data(place).merge(this->data(rhs), arena);
    }

    void serialize(ConstAggregateDataPtr place, WriteBuffer &buf) const override
    {
        this->data(place).serialize(buf);
    }

    void deserialize(AggregateDataPtr place, ReadBuffer &buf, Arena * arena) const override
    {
        this->data(place).deserialize(buf, arena);
    }

    void splitToSessions(AggregateFunctionSessionSplitData & data, std::vector <Session> & res) const
    {
        if (data.events.empty())
            return;

        data.sort();
        auto curr_session = data.get(0);
        UInt64 session_start_time = curr_session->getStartTime();
        UInt64 session_end_time = curr_session->getEndTime();
        UInt32 depth = 1;
        bool new_session = false;
        bool has_pv = curr_session->isPageView(); // has_pv 切分之后的session中是否含有predefine_pageview事件
        StringRef url[2] {curr_session->url, curr_session->refer_type};

        UInt64 cur_start_time = 0;
        UInt64 cur_end_time = 0;
        for (size_t i = 1; i < data.events.size(); ++i)
        {
            curr_session = data.get(i);
            cur_start_time = curr_session->getStartTime();
            /// split session by mWindowSize
            if (session_start_time / m_window_size != cur_start_time / m_window_size)
                new_session = true;

            /// split session by mSessionSplitTime
            if (cur_start_time >= session_end_time && cur_start_time - session_end_time > m_session_split_time)
                new_session = true;

            if (!new_session)
            {
                depth += curr_session->isPageView();
                cur_end_time = curr_session->getEndTime();
                session_end_time = std::max(session_end_time, cur_end_time);
                session_start_time = std::min(session_start_time, cur_start_time);
                if (curr_session->isPageView())
                {
                    if (!has_pv || type == 1)
                    {
                        has_pv = true;
                        url[0] = (*curr_session).url;
                        url[1] = (*curr_session).refer_type;
                    }
                    else if (type == 2)
                    {
                        url[1] = (*curr_session).refer_type;
                    }
                }
            }
            else
            {
                if (!has_pv)
                    url[0] = url[1] = StringRef();
                res.emplace_back(static_cast<UInt32>(session_end_time - session_start_time), depth, url[0], url[1]);

                new_session = false;
                session_start_time = curr_session->getStartTime();
                session_end_time = curr_session->getEndTime();
                url[0] = (*curr_session).url;
                url[1] = (*curr_session).refer_type;
                depth = 1;
                has_pv = curr_session->isPageView();
            }
        }
        if (!has_pv)
            url[0] = url[1] = StringRef();
        res.emplace_back(static_cast<UInt32>(session_end_time - session_start_time), depth, url[0], url[1]);
    }

    void insertResultInto(AggregateDataPtr place, IColumn & to, Arena *) const override
    {
        std::vector<Session> sess;
        splitToSessions(static_cast<AggregateFunctionSessionSplitData &>(this->data(place)), sess);
        ColumnArray & array_to = assert_cast<ColumnArray &>(to);
        ColumnArray::Offsets & offset_to = array_to.getOffsets();
        ColumnTuple & array_to_nest = assert_cast<ColumnTuple &>(array_to.getData());
        offset_to.push_back(sess.size() + (offset_to.empty() ? 0 : offset_to.back()));
        auto & dur_col_data = assert_cast<ColumnUInt32 &>(array_to_nest.getColumn(0)).getData();
        auto & depth_col_data = assert_cast<ColumnUInt32 &>(array_to_nest.getColumn(1)).getData();
        auto & url_col = assert_cast<ColumnString &>(array_to_nest.getColumn(2));
        auto & ref_col = assert_cast<ColumnString &>(array_to_nest.getColumn(3));

        for (auto & s : sess)
        {
            dur_col_data.push_back(s.session_duration);
            depth_col_data.push_back(s.session_depth);
            url_col.insertData(s.first_page_param.data, s.first_page_param.size);
            ref_col.insertData(s.last_page_param.data, s.last_page_param.size);
        }
    }

    bool allocatesMemoryInArena() const override { return true; }
};

struct SessionRes
{
    UInt32 session_duration;
    UInt32 session_depth;
    StringRef url;
    StringRef refer_type;
    std::vector<StringRef> args;
    SessionRes(UInt32 session_duration_, UInt32 session_depth_, StringRef url_, StringRef refer_type_, const std::vector<StringRef> & args_):
        session_duration(session_duration_), session_depth(session_depth_), url(url_), refer_type(refer_type_), args(args_) {}
};

class AggregateFunctionSessionSplit final : public IAggregateFunctionDataHelper<AggregateFunctionSessionSplitData, AggregateFunctionSessionSplit>
{
private:
    UInt64 m_session_split_time;
    UInt64 m_window_size;
    UInt64 m_base_time;
    UInt8 type;
    DataTypes types; // support mutable arguments

public:
    AggregateFunctionSessionSplit(UInt64 watch_start, UInt64 window_size, UInt64 base_time, UInt8 type_, const DataTypes & arguments, const Array & params) :
        IAggregateFunctionDataHelper<AggregateFunctionSessionSplitData, AggregateFunctionSessionSplit>(arguments, params),
        m_session_split_time(watch_start),
        m_window_size(window_size),
        m_base_time(base_time),
        type(type_)
    {
        types.reserve(argument_types.size() - 3); // argument_types.size() - 7 + 4
        types.emplace_back(std::make_shared<DataTypeUInt32>()); // session duration
        types.emplace_back(std::make_shared<DataTypeUInt32>()); // session depth
        types.emplace_back(std::make_shared<DataTypeString>()); // url
        types.emplace_back(std::make_shared<DataTypeString>()); // refer_type
        for(size_t i = 7; i < argument_types.size(); ++i)
            types.emplace_back(std::make_shared<DataTypeString>());
    }

    String getName() const override { return "sessionSplit"; }

    void create(AggregateDataPtr place) const override
    {
        new(place) AggregateFunctionSessionSplitData;
    }
    /**
     * return type:
     * [(UInt32, UInt32, String, String, ...), ...]
     */
    DataTypePtr getReturnType() const override
    {
        return std::make_shared<DataTypeArray>(std::make_shared<DataTypeTuple>(types));
    }

    void add(AggregateDataPtr place, const IColumn **columns, size_t row_num, Arena * arena) const override
    {
        UInt64 server_time = assert_cast<const ColumnUInt64 *>(columns[0])->getData()[row_num];
        auto event = SessionEvent::mapEvent(assert_cast<const ColumnString *>(columns[1])->getDataAt(row_num));
        UInt64 event_time = assert_cast<const ColumnUInt64 *>(columns[2])->getData()[row_num];

        if (event == SessionEvent::KnownType || server_time < m_base_time)
            return;

        UInt64 start_time = 0;
        UInt64 end_time = 0;

        if (event == SessionEvent::BeActive)
        {
            start_time = assert_cast<const ColumnUInt64 *>(columns[3])->getData()[row_num];
            end_time = assert_cast<const ColumnUInt64 *>(columns[4])->getData()[row_num];

            if (start_time >= std::numeric_limits<UInt32>::max()) // ms
                start_time /= 1000;
            if (end_time >= std::numeric_limits<UInt32>::max()) // ms
                end_time /= 1000;

            if (start_time > end_time)
                return;
        }

        const auto url = assert_cast<const ColumnString *>(columns[5])->getDataAt(row_num);
        const auto refer_type = assert_cast<const ColumnString *>(columns[6])->getDataAt(row_num);

        char * url_data = arena->alloc(url.size);
        char * refer_data = arena->alloc(refer_type.size);
        strncpy(url_data, url.data, url.size);
        strncpy(refer_data, refer_type.data, refer_type.size);

        std::vector<StringRef> args;
        args.reserve(types.size() - 4);
        for (size_t i = 7; i < argument_types.size(); ++i)
        {
            const auto arg = assert_cast<const ColumnString *>(columns[i])->getDataAt(row_num);
            char * data = arena->alloc(arg.size);
            strncpy(data, arg.data, arg.size);
            args.emplace_back(data, arg.size);
        }

        this->data(place).add(server_time, event, event_time, start_time, end_time, {url_data, url.size}, {refer_data, refer_type.size}, args);
    }

    void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, Arena * arena) const override
    {
        this->data(place).merge(this->data(rhs), arena);
    }

    void serialize(ConstAggregateDataPtr place, WriteBuffer &buf) const override
    {
        this->data(place).serialize(buf);
    }

    void deserialize(AggregateDataPtr place, ReadBuffer &buf, Arena * arena) const override
    {
        this->data(place).deserialize(buf, arena);
    }

    void splitToSessions(AggregateFunctionSessionSplitData & data, std::vector <SessionRes> & res) const
    {
        if (data.events.empty())
            return;

        data.sort();
        auto curr_session = data.get(0);
        UInt64 session_start_time = curr_session->getStartTime();
        UInt64 session_end_time = curr_session->getEndTime();
        UInt32 depth = 1;
        bool new_session = false;
        bool has_pv = curr_session->isPageView(); // has_pv 切分之后的session中是否含有predefine_pageview事件
        StringRef url;
        StringRef refer_type;
        std::vector<StringRef> args;
        UInt64 cur_start_time = 0;
        UInt64 cur_end_time = 0;

        if (curr_session->isPageView())
        {
            url = (*curr_session).url;
            refer_type = (*curr_session).refer_type;
            args = (*curr_session).args;
        }
        for (size_t i = 1; i < data.events.size(); ++i)
        {
            curr_session = data.get(i);
            cur_start_time = curr_session->getStartTime();
            /// split session by mWindowSize
            if (session_start_time / m_window_size != cur_start_time / m_window_size)
                new_session = true;

            /// split session by mSessionSplitTime
            if (cur_start_time >= session_end_time && cur_start_time - session_end_time > m_session_split_time)
                new_session = true;

            /// split session by refer_type
            if ((*curr_session).event == 1 && ((*curr_session).refer_type.size && !((*curr_session).refer_type == "inner")))
                new_session = true;

            if (!new_session)
            {
                cur_end_time = curr_session->getEndTime();
                session_end_time = std::max(session_end_time, cur_end_time);
                session_start_time = std::min(session_start_time, cur_start_time);

                if (curr_session->isPageView())
                {
                    depth += 1;
                    if (!has_pv) // first predefine_pageview
                    {
                        url = (*curr_session).url;
                        refer_type = (*curr_session).refer_type;
                        args = (*curr_session).args;
                        has_pv = true;
                    }
                    else if (type == 1)
                    {
                        /**
                         * type = 0 fist page_view args
                         * type = 1 last page_view args
                         */
                        args = (*curr_session).args;
                    }
                }
            }
            else
            {
                res.emplace_back(static_cast<UInt32>(session_end_time - session_start_time), depth, url, refer_type, args);
                new_session = false;
                depth = 1;
                session_start_time = curr_session->getStartTime();
                session_end_time = curr_session->getEndTime();
                has_pv = curr_session->isPageView();

                if (curr_session->isPageView())
                {
                    url = (*curr_session).url;
                    refer_type = (*curr_session).refer_type;
                    args = (*curr_session).args;
                }
                else
                {
                    url = StringRef();
                    refer_type = StringRef();
                    args.clear();
                }
            }
        }
        res.emplace_back(static_cast<UInt32>(session_end_time - session_start_time), depth, url, refer_type, args);
    }

    void insertResultInto(AggregateDataPtr place, IColumn & to, Arena *) const override
    {
        std::vector <SessionRes> sess;
        splitToSessions(const_cast<AggregateFunctionSessionSplitData &>(this->data(place)), sess);
        ColumnArray & array_to = assert_cast<ColumnArray &>(to);
        ColumnArray::Offsets &offset_to = array_to.getOffsets();
        ColumnTuple &array_to_nest = assert_cast<ColumnTuple &>(array_to.getData());
        offset_to.push_back(sess.size() + (offset_to.empty() ? 0 : offset_to.back()));

        auto & dur_col_data = assert_cast<ColumnUInt32 &>(array_to_nest.getColumn(0)).getData();
        auto & depth_col_data = assert_cast<ColumnUInt32 &>(array_to_nest.getColumn(1)).getData();
        auto & url_col = assert_cast<ColumnString &>(array_to_nest.getColumn(2));
        auto & refer_col = assert_cast<ColumnString &>(array_to_nest.getColumn(3));

        for (auto & s : sess)
        {
            dur_col_data.push_back(s.session_duration);
            depth_col_data.push_back(s.session_depth);
            url_col.insertData(s.url.data, s.url.size);
            refer_col.insertData(s.refer_type.data, s.refer_type.size);
        }

        for(size_t i = 4; i < types.size(); ++i)
        {
            auto & arg = assert_cast<ColumnString &>(array_to_nest.getColumn(i));

            std::for_each(sess.begin(), sess.end(), [&](const auto & v) {
                /// If args is Empty, this session not contain pageView event
                /// insert default value to result
                if (!v.args.empty())
                    arg.insertData(v.args[i - 4].data, v.args[i - 4].size);
                else
                    arg.insertDefault();
            });
        }
    }

    bool allocatesMemoryInArena() const override { return true; }
};


struct AggregateFunctionSumMetricData
{
    UInt64 session_cnt = 0;
    UInt64 total_dur = 0;
    UInt64 total_depth = 0;
    UInt64 total_jump = 0;

    void add(UInt64 dur_, UInt64 depth_, UInt8 jmp_)
    {
        ++session_cnt;
        total_dur += dur_;
        total_depth += depth_;
        total_jump += jmp_; // 0 if not jump out session, otherwise it is 1
    }

    void merge(const AggregateFunctionSumMetricData &rhs)
    {
        session_cnt += rhs.session_cnt;
        total_dur += rhs.total_dur;
        total_depth += rhs.total_depth;
        total_jump += rhs.total_jump;
    }

    void serialize(WriteBuffer &buf) const
    {
        writeBinary(session_cnt, buf);
        writeBinary(total_dur, buf);
        writeBinary(total_depth, buf);
        writeBinary(total_jump, buf);
    }

    void deserialize(ReadBuffer &buf)
    {
        readBinary(session_cnt, buf);
        readBinary(total_dur, buf);
        readBinary(total_depth, buf);
        readBinary(total_jump, buf);
    }
};

class AggregateFunctionSumMetric final : public IAggregateFunctionDataHelper<AggregateFunctionSumMetricData, AggregateFunctionSumMetric>
{
public:
    AggregateFunctionSumMetric( const DataTypes & arguments, const Array & params) :
        IAggregateFunctionDataHelper<AggregateFunctionSumMetricData, AggregateFunctionSumMetric>(
            arguments, params) {}

    String getName() const override { return "sumMetric"; }

    void create(AggregateDataPtr place) const override
    {
        new(place) AggregateFunctionSumMetricData;
    }

    /**
     * Tuple with all required numeric metrics in PRD
     */
    DataTypePtr getReturnType() const override
    {
        DataTypes types(4, std::make_shared<DataTypeUInt64>());
        return std::make_shared<DataTypeTuple>(types);
    }

    /**
     *  Input is from sessionSplit function, and
     */
    void add(AggregateDataPtr place, const IColumn **columns, size_t row_num, Arena *) const override
    {
        // interpreter tuple column input
        const ColumnTuple *column = assert_cast<const ColumnTuple *>(columns[0]);
        const auto & dur_col = assert_cast<const ColumnUInt32 &>(column->getColumn(0));
        const auto & sess_col = assert_cast<const ColumnUInt32 &>(column->getColumn(1));
        const auto & jmp_col = assert_cast<const ColumnUInt8 &>(column->getColumn(2));
        this->data(place).add(dur_col.getData()[row_num], sess_col.getData()[row_num], jmp_col.getData()[row_num]);
    }

    void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, Arena *) const override
    {
        this->data(place).merge(this->data(rhs));
    }

    void serialize(ConstAggregateDataPtr place, WriteBuffer &buf) const override
    {
        this->data(place).serialize(buf);
    }

    void deserialize(AggregateDataPtr place, ReadBuffer &buf, Arena *) const override
    {
        this->data(place).deserialize(buf);
    }

    void insertResultInto(AggregateDataPtr place, IColumn & to, Arena *) const override
    {
        ColumnTuple & tuple_to = assert_cast<ColumnTuple &>(to);
        auto & cnt_data = assert_cast<ColumnUInt64 &>(tuple_to.getColumn(0)).getData();
        auto & dur_data = assert_cast<ColumnUInt64 &>(tuple_to.getColumn(1)).getData();
        auto & depth_data = assert_cast<ColumnUInt64 &>(tuple_to.getColumn(2)).getData();
        auto & jmp_data = assert_cast<ColumnUInt64 &>(tuple_to.getColumn(3)).getData();

        const auto & metric = this->data(place);
        cnt_data.push_back(metric.session_cnt);
        dur_data.push_back(metric.total_dur);
        depth_data.push_back(metric.total_depth);
        jmp_data.push_back(metric.total_jump);
    }

    bool allocatesMemoryInArena() const override { return false; }
};


struct AggregateFunctionRefer
{
    UInt32 url_cnt = 0;
    UInt32 url_dur = 0;
};

class AggregateFunctionPageTime final : public IAggregateFunctionDataHelper<AggregateFunctionSessionSplitData, AggregateFunctionPageTime>
{
private:
    // TODO: add required parameters:
    UInt64 m_session_split_time;
    UInt64 m_window_size;
    UInt64 m_base_time;
    String refer_url;
public:

    AggregateFunctionPageTime(UInt64 session_split_time, UInt64 window_size, UInt64 base_time, String refer_url_, const DataTypes & arguments, const Array & params) :
        IAggregateFunctionDataHelper<AggregateFunctionSessionSplitData, AggregateFunctionPageTime>(arguments, params),
        m_session_split_time(session_split_time),
        m_window_size(window_size),
        m_base_time(base_time),
        refer_url(refer_url_)
    {}

    String getName() const override { return "pageTime"; }

    void create(AggregateDataPtr place) const override
    {
        new(place) AggregateFunctionSessionSplitData;
    }

    /**
     * Session description:
     * (duration, depth, is_jmp, entry_url, exit_url)
     * [tuple(Sting, UInt32, UInt32, UInt32), tuple(...)]
     */
    DataTypePtr getReturnType() const override
    {
        DataTypes types {
            std::make_shared<DataTypeString>(), // page_view url
            std::make_shared<DataTypeUInt32>(), // cnt(all url)
            std::make_shared<DataTypeUInt32>() // total duration
        };
        return std::make_shared<DataTypeArray>(std::make_shared<DataTypeTuple>(types));
    }

    void add(AggregateDataPtr place, const IColumn **columns, size_t row_num, Arena * arena) const override
    {
        UInt64 server_time = assert_cast<const ColumnUInt64 *>(columns[0])->getData()[row_num];
        auto event = SessionEvent::mapEvent(assert_cast<const ColumnString *>(columns[1])->getDataAt(row_num));
        UInt64 event_time = assert_cast<const ColumnUInt64 *>(columns[2])->getData()[row_num];

        if (event == SessionEvent::KnownType || server_time < m_base_time)
            return;

        UInt64 start_time = 0;
        UInt64 end_time = 0;

        if (event == SessionEvent::BeActive)
        {
            start_time = assert_cast<const ColumnUInt64 *>(columns[3])->getData()[row_num];
            end_time = assert_cast<const ColumnUInt64 *>(columns[4])->getData()[row_num];

            if (start_time >= std::numeric_limits<UInt32>::max()) // ms
                start_time /= 1000;
            if (end_time >= std::numeric_limits<UInt32>::max()) // ms
                end_time /= 1000;

            if (start_time > end_time)
                return;
        }

        const auto url = assert_cast<const ColumnString *>(columns[5])->getDataAt(row_num);
        const auto ref = assert_cast<const ColumnString *>(columns[6])->getDataAt(row_num);

        char * url_data = arena->alloc(url.size);
        char * refer_data = arena->alloc(ref.size);
        strncpy(url_data, url.data, url.size);
        strncpy(refer_data, ref.data, ref.size);

        this->data(place).add(server_time, event, event_time, start_time, end_time, {url_data, url.size}, {refer_data, ref.size});
    }

    void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, Arena * arena) const override
    {
        this->data(place).merge(this->data(rhs), arena);
    }

    void serialize(ConstAggregateDataPtr place, WriteBuffer &buf) const override
    {
        this->data(place).serialize(buf);
    }

    void deserialize(AggregateDataPtr place, ReadBuffer &buf, Arena * arena) const override
    {
        this->data(place).deserialize(buf, arena);
    }

    void splitToSessions(AggregateFunctionSessionSplitData & data, std::unordered_map<StringRef, AggregateFunctionRefer> & res) const
    {
        if (data.events.empty())
            return;

        data.sort();
        auto curr_session = data.get(0);
        UInt64 session_start_time = curr_session->getStartTime();
        UInt64 session_end_time = curr_session->getEndTime();
        UInt64 page_start_time = 0;
        StringRef url;
        StringRef refer;
        bool new_session = false;
        UInt64 cur_start_time = 0;
        UInt64 cur_end_time = 0;

        auto valid_url = [&](StringRef refer_) -> bool { return refer_url == "all" || refer_url == refer_; };

        if (curr_session->isPageView())
        {
            url = (*curr_session).url;
            refer = (*curr_session).refer_type;
            page_start_time = session_start_time;

            /// urlCnt += 1 when event = 1
            /// urlDur += duration_time when page_view is not the last one of session
            if (valid_url(refer))
                res[url].url_cnt += 1;
        }

        for (size_t i = 1; i < data.events.size(); ++i)
        {
            curr_session = data.get(i);
            cur_start_time = curr_session->getStartTime();
            if (session_start_time / m_window_size != cur_start_time / m_window_size)
                new_session = true;

            if (cur_start_time >= session_end_time && cur_start_time - session_end_time > m_session_split_time)
                new_session = true;

            if (!new_session)
            {
                if (curr_session->isPageView() && valid_url(refer))
                {
                    if (page_start_time && curr_session->getStartTime() > page_start_time)
                        res[url].url_dur += curr_session->getStartTime() - page_start_time;
                    url = (*curr_session).url;
                    res[url].url_cnt += 1;
                    page_start_time = curr_session->getStartTime();
                }

                cur_end_time = curr_session->getEndTime();
                session_end_time = std::max(session_end_time, cur_end_time);
            }
            else
            {
                session_start_time = curr_session->getStartTime();
                session_end_time = curr_session->getEndTime();
                new_session = false;

                if (curr_session->isPageView())
                {
                    page_start_time = session_start_time;
                    refer = (*curr_session).refer_type;
                    url = (*curr_session).url;

                    if (valid_url(refer))
                        res[url].url_cnt += 1;
                }
                else
                {
                    page_start_time = 0;
                    refer = url = StringRef();
                }
            }
        }
    }

    void insertResultInto(AggregateDataPtr place, IColumn & to, Arena *) const override
    {
        std::unordered_map <StringRef, AggregateFunctionRefer> sess;
        splitToSessions(static_cast<AggregateFunctionSessionSplitData &>(this->data(place)), sess);
        ColumnArray & array_to = assert_cast<ColumnArray &>(to);
        ColumnArray::Offsets & offset_to = array_to.getOffsets();
        ColumnTuple & array_to_nest = assert_cast<ColumnTuple &>(array_to.getData());
        offset_to.push_back(sess.size() + (offset_to.empty() ? 0 : offset_to.back()));

        auto & ref_col = assert_cast<ColumnString &>(array_to_nest.getColumn(0));
        auto & cnt_col_data = assert_cast<ColumnUInt32 &>(array_to_nest.getColumn(1)).getData();
        auto & dur_col_data = assert_cast<ColumnUInt32 &>(array_to_nest.getColumn(2)).getData();

        for (auto &s : sess)
        {
            ref_col.insertData(s.first.data, s.first.size);
            cnt_col_data.push_back(s.second.url_cnt);
            dur_col_data.push_back(s.second.url_dur);
        }
    }

    bool allocatesMemoryInArena() const override { return true; }
};

struct PageTime2Data
{
    StringRef url;
    UInt32 url_dur;
    StringRef refer_type;
    std::vector<StringRef> args;
    PageTime2Data(StringRef url_, UInt32 url_dur_, StringRef refer_type_, const std::vector<StringRef> & args_):
        url(url_), url_dur(url_dur_), refer_type(refer_type_), args(args_) {}
};

class AggregateFunctionPageTime2 final : public IAggregateFunctionDataHelper<AggregateFunctionSessionSplitData, AggregateFunctionPageTime2>
{
private:
    UInt64 m_session_split_time;
    UInt64 m_window_size;
    UInt64 m_base_time;
    DataTypes types;
public:
    AggregateFunctionPageTime2(UInt64 session_split_time, UInt64 window_size, UInt64 base_time, const DataTypes & arguments, const Array & params) :
        IAggregateFunctionDataHelper<AggregateFunctionSessionSplitData, AggregateFunctionPageTime2>(arguments, params),
        m_session_split_time(session_split_time),
        m_window_size(window_size),
        m_base_time(base_time)
    {
        types.emplace_back(std::make_shared<DataTypeString>()); // pageview url
        types.emplace_back(std::make_shared<DataTypeUInt32>()); // total duration
        types.emplace_back(std::make_shared<DataTypeString>()); // refer_type
        for(size_t i = 7; i < argument_types.size(); ++i)
            types.emplace_back(std::make_shared<DataTypeString>());
    }

    String getName() const override { return "pageTime2"; }

    void create(AggregateDataPtr place) const override
    {
        new(place) AggregateFunctionSessionSplitData;
    }

    /**
     * Session description:
     * (url, depth, refer_type)
     * [tuple(Sting, UInt32, String, ...), ...]
     */
    DataTypePtr getReturnType() const override
    {
        return std::make_shared<DataTypeArray>(std::make_shared<DataTypeTuple>(types));
    }

    void add(AggregateDataPtr place, const IColumn **columns, size_t row_num, Arena * arena) const override
    {
        UInt64 server_time = assert_cast<const ColumnUInt64 *>(columns[0])->getData()[row_num];
        auto event = SessionEvent::mapEvent(assert_cast<const ColumnString *>(columns[1])->getDataAt(row_num));
        UInt64 event_time = assert_cast<const ColumnUInt64 *>(columns[2])->getData()[row_num];

        if (event == SessionEvent::KnownType || server_time < m_base_time)
            return;

        UInt64 start_time = 0;
        UInt64 end_time = 0;

        if (event == SessionEvent::BeActive)
        {
            start_time = assert_cast<const ColumnUInt64 *>(columns[3])->getData()[row_num];
            end_time = assert_cast<const ColumnUInt64 *>(columns[4])->getData()[row_num];

            if (start_time >= std::numeric_limits<UInt32>::max()) // ms
                start_time /= 1000;
            if (end_time >= std::numeric_limits<UInt32>::max()) // ms
                end_time /= 1000;

            if (start_time > end_time)
                return;
        }

        const auto url = assert_cast<const ColumnString *>(columns[5])->getDataAt(row_num);
        const auto ref = assert_cast<const ColumnString *>(columns[6])->getDataAt(row_num);

        char * url_data = arena->alloc(url.size);
        char * refer_data = arena->alloc(ref.size);
        strncpy(url_data, url.data, url.size);
        strncpy(refer_data, ref.data, ref.size);

        std::vector<StringRef> args;
        args.reserve(types.size() - 3);
        for (size_t i = 7; i < argument_types.size(); ++i)
        {
            auto arg = assert_cast<const ColumnString *>(columns[i])->getDataAt(row_num);
            char * data = arena->alloc(arg.size);
            strncpy(data, arg.data, arg.size);
            args.emplace_back(data, arg.size);
        }

        this->data(place).add(server_time, event, event_time, start_time, end_time, {url_data, url.size}, {refer_data, ref.size}, args);
    }

    void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, Arena * arena) const override
    {
        this->data(place).merge(this->data(rhs), arena);
    }

    void serialize(ConstAggregateDataPtr place, WriteBuffer &buf) const override
    {
        this->data(place).serialize(buf);
    }

    void deserialize(AggregateDataPtr place, ReadBuffer &buf, Arena * arena) const override
    {
        this->data(place).deserialize(buf, arena);
    }

    void splitToSessions(AggregateFunctionSessionSplitData &data, std::vector<PageTime2Data> &res) const
    {
        if (data.events.empty())
            return;

        data.sort();
        auto curr_session = data.get(0);
        UInt64 session_start_time = curr_session->getStartTime();
        UInt64 session_end_time = curr_session->getEndTime();
        UInt64 page_start_time = 0;
        StringRef url;
        StringRef refer_type;
        std::vector<StringRef> args;

        bool new_session = false;
        UInt64 cur_start_time = 0;
        UInt64 cur_end_time = 0;

        if (curr_session->isPageView())
        {
            url = (*curr_session).url;
            refer_type = (*curr_session).refer_type;
            args = (*curr_session).args;
            page_start_time = session_start_time;
        }

        for (size_t i = 1; i < data.events.size(); ++i)
        {
            curr_session = data.get(i);
            cur_start_time = curr_session->getStartTime();
            if (session_start_time / m_window_size != cur_start_time / m_window_size)
                new_session = true;

            if (cur_start_time >= session_end_time && cur_start_time - session_end_time > m_session_split_time)
                new_session = true;

            if (curr_session->isPageView() && ((*curr_session).refer_type.size && !((*curr_session).refer_type == "inner")))
                new_session = true;

            if (!new_session)
            {
                if (curr_session->isPageView())
                {
                    if (page_start_time && curr_session->getStartTime() >= page_start_time)
                        res.emplace_back(url, curr_session->getStartTime() - page_start_time, refer_type, args);
                    url = (*curr_session).url;
                    refer_type = (*curr_session).refer_type;
                    args = (*curr_session).args;
                    page_start_time = curr_session->getStartTime();
                }

                cur_end_time = curr_session->getEndTime();
                session_end_time = std::max(session_end_time, cur_end_time);
            }
            else
            {
                new_session = false;
                if (page_start_time)
                    res.emplace_back(url, 0, refer_type, args);

                session_start_time = curr_session->getStartTime();
                session_end_time = curr_session->getEndTime();
                page_start_time = 0;

                if (curr_session->isPageView())
                {
                    url = (*curr_session).url;
                    refer_type = (*curr_session).refer_type;
                    args = (*curr_session).args;
                    page_start_time = session_start_time;
                }
            }
        }

        if (page_start_time)
            res.emplace_back(url, 0, refer_type, args);
    }

    void insertResultInto(AggregateDataPtr place, IColumn & to, Arena *) const override
    {
        std::vector<PageTime2Data> sess;
        splitToSessions(assert_cast<AggregateFunctionSessionSplitData &>(this->data(place)), sess);
        ColumnArray & array_to = assert_cast<ColumnArray &>(to);
        ColumnArray::Offsets & offset_to = array_to.getOffsets();
        ColumnTuple & array_to_nest = assert_cast<ColumnTuple &>(array_to.getData());
        offset_to.push_back(sess.size() + (offset_to.empty() ? 0 : offset_to.back()));

        auto & url_col = assert_cast<ColumnString &>(array_to_nest.getColumn(0));
        auto & dur_col_data = assert_cast<ColumnUInt32 &>(array_to_nest.getColumn(1)).getData();
        auto & ref_col = assert_cast<ColumnString &>(array_to_nest.getColumn(2));

        for (auto & s : sess)
        {
            url_col.insertData(s.url.data, s.url.size);
            dur_col_data.push_back(s.url_dur);
            ref_col.insertData(s.refer_type.data, s.refer_type.size);
        }

        for(size_t i = 3; i < types.size(); ++i)
        {
            auto & arg = assert_cast<ColumnString &>(array_to_nest.getColumn(i));
            std::for_each(sess.begin(), sess.end(), [&](const auto & v) { arg.insertData(v.args[i - 3].data, v.args[i - 3].size); });
        }
    }

    bool allocatesMemoryInArena() const override { return true; }
};

}
