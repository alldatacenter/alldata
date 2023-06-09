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

#include <IO/VarInt.h>
#include <IO/ReadHelpers.h>
#include <IO/WriteHelpers.h>

#include <array>
#include <numeric>
#include <AggregateFunctions/IAggregateFunction.h>
#include <Columns/ColumnNullable.h>
#include <Columns/ColumnString.h>
#include <Columns/ColumnVector.h>
#include <Columns/ColumnTuple.h>
#include <Columns/ColumnArray.h>
#include <DataTypes/DataTypeString.h>
#include <DataTypes/DataTypesNumber.h>
#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypeTuple.h>
#include <common/logger_useful.h>


namespace DB
{

namespace ErrorCodes
{
    extern const int BAD_ARGUMENTS;
    extern const int LOGICAL_ERROR;
    extern const int NUMBER_OF_ARGUMENTS_DOESNT_MATCH;
}

struct AttrAnalysisEvent
{
    UInt64 event_time{};
    String event_name;
    String event_type;
    UInt64 type_index{};
    String event_attribution_value_string;
    Float64 event_attribution_value_float{};
    std::vector<Field> relation_attr;

    bool operator < (const AttrAnalysisEvent & event) const
    {
        return event_time < event.event_time || (event_time == event.event_time && event_name < event.event_name);
    }

    AttrAnalysisEvent() = default;
    AttrAnalysisEvent(UInt64 event_time_, String event_name_, String event_type_, UInt64 type_index_, String event_attribution_value_string_, Float64 event_attribution_value_float_) : event_time(event_time_), event_name(event_name_), event_type(event_type_), type_index(type_index_), event_attribution_value_string(event_attribution_value_string_), event_attribution_value_float(event_attribution_value_float_) {}
    AttrAnalysisEvent(UInt64 event_time_, String event_name_, String event_type_, UInt64 type_index_, String event_attribution_value_string_, Float64 event_attribution_value_float_, std::vector<Field> relation_attr_) : event_time(event_time_), event_name(event_name_), event_type(event_type_), type_index(type_index_), event_attribution_value_string(event_attribution_value_string_), event_attribution_value_float(event_attribution_value_float_), relation_attr(relation_attr_) {}

    void eventSerialize(WriteBuffer & buf) const
    {
        writeBinary(event_time, buf);
        writeBinary(event_name, buf);
        writeBinary(event_type, buf);
        writeBinary(type_index, buf);
        writeBinary(event_attribution_value_string, buf);
        writeBinary(event_attribution_value_float, buf);

        writeBinary(relation_attr.size(), buf);
        for (const auto & attr : relation_attr)
            writeFieldBinary(attr, buf);
    }

    void eventDeserialize(ReadBuffer & buf)
    {
        readBinary(event_time, buf);
        readBinary(event_name, buf);
        readBinary(event_type, buf);
        readBinary(type_index, buf);
        readBinary(event_attribution_value_string, buf);
        readBinary(event_attribution_value_float, buf);

        size_t size;
        readBinary(size, buf);
        relation_attr.clear();
        relation_attr.reserve(size);
        for (size_t i = 0; i < size; i++)
        {
            Field attr;
            readFieldBinary(attr, buf);
            relation_attr.emplace_back(attr);
        }
    }

};

using Events = std::vector<AttrAnalysisEvent>;
using MultipleEvents = std::vector<Events>;

struct AttributionAnalysisResult
{
    std::vector<std::vector<String>> touch_events;
    std::vector<UInt64> click_cnt;
    std::vector<UInt64> valid_transform_cnt;
    std::vector<std::vector<UInt64>> transform_times;
    std::vector<std::vector<UInt64>> transform_steps;
    std::vector<Float64> contribution;
    std::vector<Float64> value;
};

struct AggregateFunctionAttributionAnalysisData
{
    Events events;
    MultipleEvents multiple_events;
    Events valid_events;
    AttributionAnalysisResult res;

    void add(UInt64 event_time, const String& event_name, const String& event_type, UInt64 type_index,
             String event_attribution_value_string, Float64 event_attribution_value_float,
             Arena *)
    {
        AttrAnalysisEvent event {event_time, event_name, event_type, type_index, event_attribution_value_string, event_attribution_value_float};
        events.push_back(event);
    }

    void add(UInt64 event_time, const String& event_name, const String& event_type, UInt64 type_index,
             String event_attribution_value_string, Float64 event_attribution_value_float, std::vector<Field> relation_attr,
             Arena *)
    {
        AttrAnalysisEvent event {event_time, event_name, event_type, type_index, event_attribution_value_string, event_attribution_value_float, relation_attr};
        events.push_back(event);
    }

    void merge(const AggregateFunctionAttributionAnalysisData & other, Arena *)
    {
        events.insert(events.end(), other.events.begin(), other.events.end());
    }

    void serialize(WriteBuffer & buf) const
    {
        writeBinary(events.size(), buf);
        for (const auto & event : events)
        {
            event.eventSerialize(buf);
        }
    }

    void deserialize(ReadBuffer & buf, Arena *)
    {
        size_t size;
        readBinary(size, buf);

        events.clear();
        events.reserve(size);

        AttrAnalysisEvent event;

        for (size_t i = 0; i < size; ++i)
        {
            event.eventDeserialize(buf);
            events.insert(lower_bound(events.begin(), events.end(), event), event);
        }
    }
};

template <typename TargetAttrType>
class AggregateFunctionAttributionAnalysis final: public IAggregateFunctionDataHelper<AggregateFunctionAttributionAnalysisData, AggregateFunctionAttributionAnalysis<TargetAttrType>>
{
private:
    String target_event;
    std::vector<String> touch_events;
    std::vector<String> procedure_events;
    /// Both back_time and half_time are in milliseconds
    UInt64 back_time;
    UInt64 attribution_mode;
    bool other_transform;
    std::vector<UInt16> relation_matrix;
    const DateLUTImpl & date_lut;
    UInt64 t;
    double o, p, q;

    bool need_procedure_events;

public:

    AggregateFunctionAttributionAnalysis(
        String target_event_, std::vector<String> touch_events_, std::vector<String> procedure_events_,
        UInt64 back_time_, UInt64 attribution_mode_, bool other_transform_, std::vector<UInt16> relation_matrix_,
        String time_zone_, UInt64 t_, double o_, double p_, double q_,
        const DataTypes & arguments, const Array & params) :
        IAggregateFunctionDataHelper<AggregateFunctionAttributionAnalysisData, AggregateFunctionAttributionAnalysis<TargetAttrType>>(arguments, params),
        target_event(target_event_), touch_events(touch_events_), procedure_events(procedure_events_),
        back_time(back_time_), attribution_mode(attribution_mode_), other_transform(other_transform_), relation_matrix(relation_matrix_),
        date_lut(DateLUT::instance(time_zone_)), t(t_), o(o_), p(p_), q(q_)
    {
        need_procedure_events = !(procedure_events[0].empty());
    }

    String getName() const override
    {
        return "attributionAnalysis";
    }

    DataTypePtr getAttributionAnalysisReturnType() const
    {
        DataTypes types;
        DataTypePtr touch_events_type =
            std::make_shared<DataTypeArray>(std::make_shared<DataTypeArray>(std::make_shared<DataTypeString>()));
        DataTypePtr click_cnt_type =
            std::make_shared<DataTypeArray>(std::make_shared<DataTypeUInt64>());
        DataTypePtr valid_transform_cnt_type =
            std::make_shared<DataTypeArray>(std::make_shared<DataTypeUInt64>());
        DataTypePtr transform_times_type =
            std::make_shared<DataTypeArray>(std::make_shared<DataTypeArray>(std::make_shared<DataTypeUInt64>()));
        DataTypePtr transform_steps_type =
            std::make_shared<DataTypeArray>(std::make_shared<DataTypeArray>(std::make_shared<DataTypeUInt64>()));
        DataTypePtr contribution_type =
            std::make_shared<DataTypeArray>(std::make_shared<DataTypeFloat64>());
        DataTypePtr value_type =
            std::make_shared<DataTypeArray>(std::make_shared<DataTypeFloat64>());

        types.push_back(touch_events_type);
        types.push_back(click_cnt_type);
        types.push_back(valid_transform_cnt_type);
        types.push_back(transform_times_type);
        types.push_back(transform_steps_type);
        types.push_back(value_type);
        types.push_back(contribution_type);

        return std::make_shared<DataTypeTuple>(types);
    }

    DataTypePtr getReturnType() const override
    {
        return getAttributionAnalysisReturnType();
    }

    void add(AggregateDataPtr place, const IColumn** columns, size_t row_num, Arena *arena) const override
    {
        if (columns[0] == nullptr || columns[1] == nullptr || columns[2] == nullptr)
            throw Exception("Parameter is null", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

        UInt64 event_time = typeid_cast<const ColumnVector<UInt64> &>(*columns[0]).getData()[row_num];
        String event_name = (typeid_cast<const ColumnString &>(*columns[1])).getDataAt(row_num).toString();

        if (event_name == target_event)
        {
            // When event_attribution_value_float is -1, value is to get total count of target event
            Float64 event_attribution_value_float = static_cast<const ColumnVector<TargetAttrType> *>(columns[2])->getData()[row_num];
            if (event_attribution_value_float < 0)
                event_attribution_value_float = -1;

            std::vector<Field> relation_attr;
            relation_attr.reserve(relation_matrix[0]);
            for (size_t i = 0; i < relation_matrix[0]; i++)
                relation_attr.push_back(columns[4+i]->operator[](row_num));

            this->data(place).add(event_time, event_name, "target_event", -1, "", event_attribution_value_float, relation_attr, arena);
            return;
        }

        for (size_t i = 0; i < procedure_events.size(); i++)
        {
            if (event_name == procedure_events[i])
            {
                std::vector<Field> relation_attr;
                if (2*i+1 >= relation_matrix.size())
                {
                    this->data(place).add(event_time, event_name, "procedure_event", i, "", -1, arena);
                    return;
                }

                int index = relation_matrix[2*i+1];
                if (index >= relation_matrix[0] || index < 0)
                    throw Exception("Index out of range. Please check your relation matrix", ErrorCodes::BAD_ARGUMENTS);

                relation_attr.push_back(columns[4+index]->operator[](row_num));
                this->data(place).add(event_time, event_name, "procedure_event", i, "", -1, relation_attr, arena);
                return;
            }
        }

        for (const auto & touch_event : touch_events)
        {
            if (event_name == touch_event)
            {
                String event_attribution_value_string;
                if (columns[3] == nullptr || dynamic_cast<const ColumnString *>(columns[3]) == nullptr)
                    throw Exception("Parameter is null or type mismatch", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

                event_attribution_value_string = (dynamic_cast<const ColumnString *>(columns[3]))->getDataAt(row_num).toString();
                this->data(place).add(event_time, event_name, "touch_event", -1, event_attribution_value_string, -1, arena);
                return;
            }
        }
    }

    void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, Arena *arena) const override
    {
        this->data(place).merge(this->data(rhs), arena);
    }

    void serialize(ConstAggregateDataPtr place, WriteBuffer & buf) const override
    {
        this->data(place).serialize(buf);
    }

    void deserialize(AggregateDataPtr place, ReadBuffer &buf, Arena *arena) const override
    {
        this->data(place).deserialize(buf, arena);
    }

    void initAttributionAnalysisResult(int size, AttributionAnalysisResult & res) const
    {
        res.valid_transform_cnt.insert(res.valid_transform_cnt.end(), size, 0);
        res.transform_times.insert(res.transform_times.end(), size, {});
        res.transform_steps.insert(res.transform_steps.end(), size, {});
        res.contribution.insert(res.contribution.end(), size, 0.0);
        res.value.insert(res.value.end(), size, 0.0);
    }

    void truncateAttributionAnalysisResult(AggregateDataPtr place) const
    {
        AttributionAnalysisResult & res = this->data(place).res;
        res.click_cnt.clear();
        res.valid_transform_cnt.clear();
        res.click_cnt.insert(res.click_cnt.begin(), res.touch_events.size(), 0);
        res.transform_times.clear();
        res.transform_steps.clear();
        res.value.clear();
        res.contribution.clear();
    }

    void truncateValidEvents(AggregateDataPtr place) const
    {
        Events & valid_events = this->data(place).valid_events;
        valid_events.clear();
    }

    void getMultipleEvents(AggregateDataPtr place) const
    {
        Events & events = this->data(place).events;

        MultipleEvents & multiple_events = this->data(place).multiple_events;
        AttributionAnalysisResult & res = this->data(place).res;

        std::sort(events.begin(), events.end());

        Events temp_events;
        for (AttrAnalysisEvent & event : events)
        {
            if (event.event_type == "touch_event")
            {
                bool exist = false;
                // Get distinct touch events
                for (size_t i = 0; i < res.touch_events.size(); i++)
                {
                    if (event.event_name == res.touch_events[i][0] && event.event_attribution_value_string == res.touch_events[i][1])
                    {
                        res.click_cnt[i]++;
                        exist = true;
                        event.type_index = i;
                        break;
                    }
                }
                if (!exist)
                {
                    res.touch_events.emplace_back(std::vector<String>{event.event_name, event.event_attribution_value_string});
                    res.click_cnt.push_back(1);
                    event.type_index = res.touch_events.size()-1;
                }
            }

            temp_events.push_back(event);
            if (event.event_type == "target_event")
            {
                multiple_events.push_back(temp_events);
                temp_events.clear();
            }
        }

        /// todo click_cnt
        if (!temp_events.empty())
            multiple_events.push_back(temp_events);

        if (other_transform)
        {
            res.touch_events.emplace_back(std::vector<String>{"$other_conversions", ""});
            res.click_cnt.push_back(0);
        }
    }

    bool valueAssociation(const AttrAnalysisEvent& target, const AttrAnalysisEvent& procedure) const
    {
        /// Means that this attribute does not need to be associated
        if (procedure.relation_attr.empty()) return true;

        int index = relation_matrix[2*(procedure.type_index+1)];
        Field pro_attr = procedure.relation_attr[0];
        Field target_attr = target.relation_attr[index];

        if (target_attr.getType() == Field::Types::Array)
        {
            Array tars = target_attr.get<Array>();

            if (!tars.empty() && tars[0].getType() != pro_attr.getType())
                throw Exception("Inconsistent types of associated attributes", ErrorCodes::BAD_ARGUMENTS);

            for (size_t m = 0; m < tars.size(); m++)
                if (pro_attr == tars[m]) return true;

            return false;
        }

        if(target_attr.getType() != pro_attr.getType())
            throw Exception("Inconsistent types of associated attributes", ErrorCodes::BAD_ARGUMENTS);

        return target_attr == pro_attr;
    }

    void  getAndProcessValidEvents(Events & events, AggregateDataPtr place) const
    {
        Events & valid_events = this->data(place).valid_events;
        AttributionAnalysisResult & res = this->data(place).res;
        initAttributionAnalysisResult(res.touch_events.size(), res);

        if (!(events[events.size()-1].event_type == "target_event"))
            return;

        AttrAnalysisEvent current_target_event = events[events.size()-1];
        valid_events.push_back(current_target_event);

        std::unordered_set<UInt64> has_procedure;
        bool all_procedure = false;
        bool has_valid_touch_event = false;
        for (int i = events.size()-2; i >= 0; i--)
        {
            AttrAnalysisEvent event = events[i];

            if (!all_procedure && event.event_type == "procedure_event" && valueAssociation(current_target_event, event))
            {
                valid_events.push_back(event);

                has_procedure.insert(event.type_index);
                if (has_procedure.size() == procedure_events.size())
                    all_procedure = true;
            }

            // touch event is valid when all procedure events happened or dont need procedure event
            if (event.event_type == "touch_event")
            {
                bool out_of_back_time = back_time > 0 ?
                                                      (current_target_event.event_time - event.event_time > back_time) :
                                                      (date_lut.toDayNum(current_target_event.event_time/1000) > date_lut.toDayNum(event.event_time/1000));

                if (out_of_back_time) break;

                int index = event.type_index;
                if (all_procedure || !need_procedure_events)
                {
                    valid_events.push_back(event);
                    has_valid_touch_event = true;

                    res.valid_transform_cnt[index]++;
                    res.transform_times[index].push_back(current_target_event.event_time-event.event_time);
                    res.transform_steps[index].push_back(events.size()-1-i);
                }
            }
        }

        if (!has_valid_touch_event)
        {
            if (other_transform)
            {
                int index = res.touch_events.size()-1;
                Float64 total_value = current_target_event.event_attribution_value_float;
                res.click_cnt[index]++;
                res.value[index] = (total_value > 0) ? total_value : 1.0;
            }
            return;
        }

        calculateContribution(valid_events, res);

        Float64 total_value = current_target_event.event_attribution_value_float;
        if (total_value > 0)
        {
            for (size_t i = 0; i < res.touch_events.size(); i++)
                res.value[i] = total_value * res.contribution[i];
        }
        else
        {
            res.value = res.contribution;
        }
    }

    void calculateContribution(Events & valid_events, AttributionAnalysisResult & res) const
    {
        size_t size = res.touch_events.size();

        if (size == 0) return;

        if (attribution_mode == 0)
        {
            for (int i = valid_events.size()-1; i >= 0; i--)
            {
                if (valid_events[i].event_type == "touch_event")
                {
                    res.contribution[valid_events[i].type_index] = 1.0;
                    return;
                }
            }
        }

        if (attribution_mode == 1)
        {
            for (const AttrAnalysisEvent& event : valid_events)
            {
                if (event.event_type == "touch_event")
                {
                    res.contribution[event.type_index] = 1.0;
                    return;
                }
            }
        }

        int all_cnt = std::accumulate(res.valid_transform_cnt.begin() , res.valid_transform_cnt.end(), 0);
        if (all_cnt == 0) return;

        if (attribution_mode == 2 || (attribution_mode == 3 && all_cnt < 3))
        {
            for (size_t i = 0; i < size; i++)
                res.contribution[i] = res.valid_transform_cnt[i]*1.0 / all_cnt;

            return;
        }

        if (attribution_mode == 3)
        {
            double avg = p / (all_cnt-2)*1.0, extra;
            int cnt = 0;
            for (const auto& event : valid_events)
            {
                if (event.event_type == "touch_event")
                {
                    extra = (cnt == 0) ? q-avg : (cnt == all_cnt-1) ? o-avg : 0.0;
                    res.contribution[event.type_index] += (avg+extra);
                    cnt++;
                }
            }
            return;
        }

        if (attribution_mode == 4)
        {
            double total = 0.0;
            for (size_t i = 0; i < res.transform_times.size(); i++)
            {
                for (UInt64 transform_time : res.transform_times[i])
                {
                    double cur = pow(0.5, transform_time/t);
                    res.contribution[i] += cur;
                    total += cur;
                }
            }
            // Too small attenuation coefficient will cause the contribution degree to be treated as 0
            if (total == 0) return;
            for (double & i : res.contribution)
                i /= total;
        }
    }

    void integrateResult(std::map<std::vector<String>, int>& touch_events_map, AttributionAnalysisResult & outer_result, const AttributionAnalysisResult& result) const
    {
        for (size_t i = 0; i < result.touch_events.size(); i++)
        {
            std::vector<String> key = std::vector{result.touch_events[i][0], result.touch_events[i][1]};
            if (!touch_events_map.count(key))
            {
                touch_events_map.insert(make_pair(key, touch_events_map.size()));
                outer_result.touch_events.push_back(key);
                // Allocate a space for each attribute
                initAttributionAnalysisResult(1, outer_result);
                // Separate processing of click cnt during merge
                outer_result.click_cnt.push_back(0);
            }

            int index = touch_events_map[result.touch_events[i]];
            outer_result.click_cnt[index] += result.click_cnt[i];
            outer_result.valid_transform_cnt[index] += result.valid_transform_cnt[i];
            outer_result.value[index] += result.value[i];

            if (!result.transform_times.empty() && !result.transform_times[i].empty())
                outer_result.transform_times[index].insert(outer_result.transform_times[index].end(), result.transform_times[i].begin(), result.transform_times[i].end());

            if (!result.transform_steps.empty() && !result.transform_steps[i].empty())
                outer_result.transform_steps[index].insert(outer_result.transform_steps[index].end(), result.transform_steps[i].begin(), result.transform_steps[i].end());
        }
    }

    void insertResultInto(AggregateDataPtr __restrict place, IColumn & to, Arena * ) const override
    {
        getMultipleEvents(const_cast<AggregateDataPtr>(place));
        MultipleEvents multiple_events = this->data(place).multiple_events;

        AttributionAnalysisResult result;
        std::map<std::vector<String>, int> touch_events_map;
        for (auto & multiple_event : multiple_events)
        {
            getAndProcessValidEvents(multiple_event, const_cast<AggregateDataPtr>(place));
            integrateResult(touch_events_map, result, this->data(place).res);
            truncateAttributionAnalysisResult(const_cast<AggregateDataPtr>(place));
            truncateValidEvents(const_cast<AggregateDataPtr>(place));
        }

        insertResultIntoColumn(to, result);
    }

    template<typename ColumnNum, typename Num>
    void insertNestedVectorNumberIntoColumn(ColumnArray& vec_to, const std::vector<Num>& vec) const
    {
        auto& vec_to_offset = vec_to.getOffsets();
        vec_to_offset.push_back((vec_to_offset.empty() ? 0 : vec_to_offset.back()) + vec.size());
        auto& vec_to_nest = static_cast<ColumnArray &>(vec_to.getData());
        auto& vec_data_to = static_cast<ColumnNum &>(static_cast<ColumnArray &>(vec_to_nest).getData());
        auto& vec_to_nest_offset = vec_to_nest.getOffsets();
        for (const auto& item : vec)
        {
            for (const auto& i : item)
                vec_data_to.insert(i);

            vec_to_nest_offset.push_back((vec_to_nest_offset.empty() ? 0 : vec_to_nest_offset.back()) + item.size());
        }
    }

    template<typename ColumnNum, typename Num>
    void insertVectorNumberIntoColumn(ColumnArray& vec_to, const std::vector<Num>& vec) const
    {
        auto& vec_to_offset = vec_to.getOffsets();
        vec_to_offset.push_back((vec_to_offset.empty() ? 0 : vec_to_offset.back()) + vec.size());
        auto& vec_data_to = static_cast<ColumnNum &>(vec_to.getData());
        for (const auto& item : vec)
            vec_data_to.insert(item);
    }

    void insertResultIntoColumn(IColumn &to, const AttributionAnalysisResult& result) const
    {
        ColumnTuple & tuple_to = static_cast<ColumnTuple &>(to);

        ColumnArray& touch_events_to = static_cast<ColumnArray &>(tuple_to.getColumn(0));
        auto& touch_events_to_offset = touch_events_to.getOffsets();
        touch_events_to_offset.push_back((touch_events_to_offset.empty() ? 0 : touch_events_to_offset.back()) + result.touch_events.size());
        auto& touch_events_to_nest = static_cast<ColumnArray &>(touch_events_to.getData());
        auto &touch_events_to_nest_offset = touch_events_to_nest.getOffsets();
        auto& touch_events_data_to = static_cast<ColumnString &>(touch_events_to_nest.getData());
        for (const auto& item : result.touch_events)
        {
            for (const auto& s : item) touch_events_data_to.insertData(s.data(), s.size());
            touch_events_to_nest_offset.push_back((touch_events_to_nest_offset.empty() ? 0 : touch_events_to_nest_offset.back()) + item.size());
        }

        ColumnArray& click_cnt_to = static_cast<ColumnArray &>(tuple_to.getColumn(1));
        insertVectorNumberIntoColumn<ColumnUInt64>(click_cnt_to, result.click_cnt);

        ColumnArray& valid_transform_cnt_to = static_cast<ColumnArray &>(tuple_to.getColumn(2));
        insertVectorNumberIntoColumn<ColumnUInt64>(valid_transform_cnt_to, result.valid_transform_cnt);

        ColumnArray& transform_times_to = static_cast<ColumnArray &>(tuple_to.getColumn(3));
        insertNestedVectorNumberIntoColumn<ColumnUInt64>(transform_times_to, result.transform_times);

        ColumnArray& transform_steps_to = static_cast<ColumnArray &>(tuple_to.getColumn(4));
        insertNestedVectorNumberIntoColumn<ColumnUInt64>(transform_steps_to, result.transform_steps);

        ColumnArray& value_to = static_cast<ColumnArray &>(tuple_to.getColumn(5));
        insertVectorNumberIntoColumn<ColumnFloat64>(value_to, result.value);

        ColumnArray& contribution_to = static_cast<ColumnArray &>(tuple_to.getColumn(6));
        insertVectorNumberIntoColumn<ColumnFloat64>(contribution_to, result.contribution);
    }

    bool allocatesMemoryInArena() const override { return false; }

};

}
