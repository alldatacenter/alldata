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
#include <IO/WriteHelpers.h>
#include <IO/ReadHelpers.h>
#include <common/logger_useful.h>

#include <Columns/ColumnVector.h>
#include <Common/ArenaAllocator.h>

#include <DataTypes/DataTypeArray.h>
#include <DataTypes/DataTypesNumber.h>

#include <Columns/ColumnArray.h>
#include <Common/SpaceSaving.h>

#include <Functions/FunctionHelpers.h>

#include <AggregateFunctions/AggregateRetentionCommon.h>
#include <Columns/ColumnString.h>


namespace DB
{
#if !__clang__
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wsign-compare"
#endif

/// Counts the retention based on start and end events
template <typename T, typename CompressT>
class AggregateFunctionRetention2 final : public IAggregateFunctionDataHelper<AggregateFunctionRetentionData, AggregateFunctionRetention2<T, CompressT>>
{
    UInt64 m_ret_window; // retention window size
    UInt64 m_ret_array_size;
    size_t m_window_words;
    std::vector<UInt64> slots;

public:
    AggregateFunctionRetention2(UInt64 retWindow,
                                const DataTypes & arguments,
                                const Array & params
                                ) :
        IAggregateFunctionDataHelper<AggregateFunctionRetentionData, AggregateFunctionRetention2<T, CompressT>>(arguments, params),
        m_ret_window(retWindow)
    {
        m_ret_array_size = m_ret_window * m_ret_window;
        m_window_words = (m_ret_window + sizeof(T) * 8 -1) / (sizeof(T)*8);

        if (params.size() > 2)
        {
            for (size_t i = 1; i < params.size(); ++i)
            {
                UInt64 slot = params[i].safeGet<UInt64>();
                if (slot > m_ret_window)
                    throw Exception("Slot is large than window " + std::to_string(slot) + " > " + std::to_string(m_ret_window), ErrorCodes::LOGICAL_ERROR);

                slots.push_back(slot);
            }

            m_ret_array_size = m_ret_window * (slots.size() / 2 + 1);

            if (slots.size() < 2 || slots.size() % 2 != 0)
                throw Exception("Illegal size of params, it should be 1, 2, or an odd number", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);
        }
    }

    String getName() const override { return "retention2"; }

    void create(const AggregateDataPtr place) const override
    {
        auto *d = new (place) AggregateFunctionRetentionData;
        std::fill(d->retentions, d->retentions+m_ret_array_size, 0);
    }

    size_t sizeOfData() const override
    {
        // reserve additional space for retentions information.
        return sizeof(AggregateFunctionRetentionData) + m_ret_array_size * sizeof(RType);
    }

    DataTypePtr getReturnType() const override
    {
        return std::make_shared<DataTypeArray>(std::make_shared<DataTypeNumber<RType>>());
    }

    template<typename MT = CompressT>
    inline typename std::enable_if<std::is_same<MT, compress_trait<COMPRESS_BIT>>::value, bool>::type
    getBit(const T* container, size_t i) const
    {
        size_t ind_word = (i >> 3) / (sizeof(T));
        T g = 1 << (i - (ind_word << 3));
        return (container[ind_word] & g);
    }

    template<typename MT = CompressT>
    inline typename std::enable_if<std::is_same<MT, compress_trait<COMPRESS_BIT>>::value, bool>::type
    getWordBit(const T word, size_t i) const
    {
        return (word & (1 << i));
    }

    void addImplWithSlots(AggregateDataPtr place, const IColumn** columns, size_t row_num) const
    {
        const ColumnArray & array_column_start = static_cast<const ColumnArray &>(*columns[0]);
        const IColumn::Offsets & start_offsets = array_column_start.getOffsets();
        auto & start_container = static_cast<const ColumnVector<T> &>(array_column_start.getData()).getData();
        const size_t start_vec_offset = row_num == 0 ? 0 : start_offsets[row_num - 1];
        const T* p_cur_start_container = &start_container[0] + start_vec_offset;

        const ColumnArray &array_column_end = static_cast<const ColumnArray &>(*columns[1]);
        const IColumn::Offsets & end_offsets = array_column_end.getOffsets();
        auto & end_container = static_cast<const ColumnVector<T> &>(array_column_end.getData()).getData();
        const size_t end_vec_offset = row_num == 0 ? 0 : end_offsets[row_num - 1];
        const T* p_cur_end_container = &end_container[0] + end_vec_offset;

        const size_t start_vec_size = (start_offsets[row_num] - start_vec_offset);
        const size_t end_vec_size = (end_offsets[row_num] - end_vec_offset);

        T start_word, end_word;
        const size_t word_size_bit = sizeof(T) << 3;
        const size_t slot_size = slots.size();
        const size_t end_slot_size = slot_size / 2;

        std::vector<std::vector<T>> end_slots_container(m_ret_window, std::vector<T>(end_slot_size, 0));

        // result array for retention
        auto & values = this->data(place).retentions;

        for (size_t start_i = 0; start_i < m_ret_window; ++start_i)
        {
            for (size_t i = 0, j = 1; j < slot_size; i += 2, j += 2)
            {
                size_t slot_i = start_i + slots[i];
                size_t slot_j = start_i + slots[j];

                // index start from 0, window range is [0, m_retWindow-1]
                if (slot_j >= m_ret_window) continue;

                for (size_t end_i = (slot_i / word_size_bit); end_i <= (slot_j / word_size_bit) && end_i < m_window_words && end_i < end_vec_size; ++end_i)
                {
                    end_word = p_cur_end_container[end_i];
                    for (size_t end_ii = 0; end_ii < word_size_bit; ++end_ii)
                    {
                        size_t end_index = end_i * word_size_bit + end_ii;
                        if ( end_index < slot_i || end_index > slot_j)
                            continue;

                        if (getWordBit(end_word, end_ii))
                        {
                            end_slots_container[start_i][i/2] = 1;
                            break;
                        }
                    }
                }
            }
        }

        size_t iw_offset = 0;
        for (size_t iw = 0; iw < m_window_words && iw < start_vec_size; iw_offset += word_size_bit, iw++)
        {
            start_word = p_cur_start_container[iw];
            if (start_word == 0) continue;

            for (size_t iiw = 0; iiw < word_size_bit; iiw++)
            {
                if (getWordBit(start_word, iiw))
                {
                    size_t start_offset = iw_offset + iiw;
                    size_t value_offset = (iw_offset + iiw) * (end_slot_size + 1);
                    values[value_offset] += 1;
                    for (size_t jw = 0; jw < end_slot_size; ++jw)
                    {
                        if (end_slots_container[start_offset][jw])
                            values[value_offset + jw + 1] += 1;
                    }
                }
            }
        }
    }

    template<typename MT = CompressT>
    typename std::enable_if<std::is_same<MT, compress_trait<COMPRESS_BIT>>::value, void>::type
    addImpl(AggregateDataPtr place, const IColumn** columns, size_t row_num, [[maybe_unused]] Arena * arena) const
    {
        if (slots.size() >= 2)
        {
            addImplWithSlots(place, columns, row_num);
            return;
        }

        const ColumnArray & array_column_start = static_cast<const ColumnArray &>(*columns[0]);
        const IColumn::Offsets & start_offsets = array_column_start.getOffsets();
        auto & start_container = static_cast<const ColumnVector<T> &>(array_column_start.getData()).getData();
        const size_t start_vec_offset = row_num == 0 ? 0 : start_offsets[row_num - 1];
        const T* p_cur_start_container = &start_container[0] + start_vec_offset;

        const ColumnArray &array_column_end = static_cast<const ColumnArray &>(*columns[1]);
        const IColumn::Offsets & end_offsets = array_column_end.getOffsets();
        auto & end_container = static_cast<const ColumnVector<T> &>(array_column_end.getData()).getData();
        const size_t end_vec_offset = row_num == 0 ? 0 : end_offsets[row_num - 1];
        const T* p_cur_end_container = &end_container[0] + end_vec_offset;

        // result array for retention
        auto & values = this->data(place).retentions;

        // Assert start_vec_size == end_vec_size and is used to record m_retWindow bits
        // The above assumption might break if two input array is combined by join.
        // in this case, zero output seems correct
        const size_t start_vec_size = (start_offsets[row_num] - start_vec_offset);
        const size_t end_vec_size = (end_offsets[row_num] - end_vec_offset);

        T start_word, end_word;
        size_t iw_offset = 0, jw_offset = 0;

#define WORD_SIZE_BIT (sizeof(T) << 3)
        if (start_vec_size != end_vec_size || start_vec_size != m_window_words)
        {
            // In this case, we need to count total user even end_event is not valid
            if (start_vec_size == m_window_words)
            {
                for (size_t iw = 0; iw < m_window_words; iw_offset += WORD_SIZE_BIT, iw++)
                {
                    start_word = p_cur_start_container[iw];
                    if (start_word == 0) continue;
                    if (start_word == T(~0))
                    {
                        // Count total user in triangle vector
                        for (size_t iiw = 0; iiw < WORD_SIZE_BIT; iiw++)
                            values[(iw_offset + iiw) * m_ret_window + iw_offset + iiw] += 1;
                    }
                    else
                    {
                        for(size_t iiw = 0; iiw < WORD_SIZE_BIT; iiw++)
                        {
                            // count total user in triangle vector
                            if (getWordBit(start_word, iiw))
                                values[(iw_offset + iiw) * m_ret_window + iw_offset + iiw] += 1;
                        }
                    }
                }
            }
            return;
        }

        // General algorithm
#if 0
        for (size_t i = 0; i< m_retWindow; i++)
          {
              if (getBit(p_cur_start_container, i))
              {
                for (size_t j = i; j<m_retWindow; j++)
                {
                    //TODO: perf issue here
                    if (getBit(p_cur_end_container, j))
                    {
                      values[i*m_retWindow + j] += 1;
                    }
                }
              }
          }
#endif

        for (size_t iw = 0; iw< m_window_words; iw_offset += WORD_SIZE_BIT, iw++)
        {
            jw_offset = iw * WORD_SIZE_BIT; // start from iw word

            start_word = p_cur_start_container[iw];
            if (start_word == 0) continue;
            // optimize all one case
            if (start_word == T(~0))
            {
                // Count total user in triangle vector
                for (size_t iiw = 0; iiw < WORD_SIZE_BIT; iiw++)
                {
                    values[(iw_offset + iiw) * m_ret_window + iw_offset + iiw] += 1;
                }

                for (size_t jw = iw; jw < m_window_words; jw_offset += WORD_SIZE_BIT, jw++)
                {
                    end_word = p_cur_end_container[jw];
                    if (end_word == 0) continue;
                    if (end_word == T(~0))
                    {
                        // assume residual piece is 0
                        for (size_t iiw = 0; iiw < WORD_SIZE_BIT; iiw++)
                            for (size_t jjw = 0; jjw < WORD_SIZE_BIT; jjw++)
                            {
                                if ((jw_offset + jjw) > (iw_offset + iiw))
                                    values[(iw_offset + iiw) * m_ret_window + jw_offset + jjw] += 1;
                            }
                    }
                    else
                    {
                        for (size_t jjw = 0; jjw < WORD_SIZE_BIT; jjw++)
                        {
                            // check bit by bit
                            if (getWordBit(end_word, jjw))
                            {
                                // update result;
                                for (size_t iiw = 0; iiw < WORD_SIZE_BIT; iiw++)
                                    if ((jw_offset + jjw) > (iw_offset + iiw))
                                        values[(iw_offset + iiw) * m_ret_window + jw_offset + jjw] += 1;
                            }
                        }
                    }
                }
            }
            else
            {
                size_t tmp_offset = iw * WORD_SIZE_BIT;// start from iw word
                for(size_t iiw = 0; iiw < WORD_SIZE_BIT; iiw++)
                {
                    jw_offset = tmp_offset;
                    if (getWordBit(start_word, iiw))
                    {
                        // count total user in triangle vector
                        values[(iw_offset + iiw) * m_ret_window + iw_offset + iiw] += 1;

                        for (size_t jw = iw; jw < m_window_words; jw_offset += WORD_SIZE_BIT, jw++)
                        {
                            end_word = p_cur_end_container[jw];
                            if (end_word == 0) continue;
                            if (end_word == T(~0))
                            {
                                //set array
                                for (size_t jjw = 0; jjw < WORD_SIZE_BIT; jjw++)
                                {
                                    if ((jw_offset +jjw) > (iw_offset + iiw))
                                        values[(iw_offset + iiw) * m_ret_window + jw_offset + jjw] += 1;
                                }
                            }
                            else
                            {
                                for (size_t jjw = 0; jjw < WORD_SIZE_BIT; jjw++)
                                {
                                    // check bit by bit
                                    if (getWordBit(end_word, jjw))
                                    {
                                        // update result;
                                        if ((jw_offset +jjw) > (iw_offset + iiw))
                                            values[(iw_offset + iiw)*m_ret_window + jw_offset + jjw] += 1;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

#undef WORD_SIZE_BIT
    }

    template<typename MT = CompressT>
    typename std::enable_if<std::is_same<MT, compress_trait<NO_COMPRESS_SORTED>>::value, void>::type
    addImpl(AggregateDataPtr place, const IColumn** columns, size_t row_num, [[maybe_unused]]Arena * arena) const
    {
          const ColumnArray & array_column_start = static_cast<const ColumnArray &>(*columns[0]);
          const IColumn::Offsets & start_offsets = array_column_start.getOffsets();
          auto & start_container = static_cast<const ColumnVector<T> &>(array_column_start.getData()).getData();
          const size_t start_vec_offset = row_num == 0 ? 0 : start_offsets[row_num - 1];
          const size_t start_vec_size = (start_offsets[row_num] - start_vec_offset);

          const ColumnArray &array_column_end = static_cast<const ColumnArray &>(*columns[1]);
          const IColumn::Offsets & end_offsets = array_column_end.getOffsets();
          auto & end_container = static_cast<const ColumnVector<T> &>(array_column_end.getData()).getData();
          const size_t end_vec_offset = row_num == 0 ? 0 : end_offsets[row_num - 1];
          const size_t end_vec_size = (end_offsets[row_num] - end_vec_offset);

          // result array for retention
          auto & values = this->data(place).retentions;

          // Get the column value from input array, and form the target array.
          // Note that input array contain the time index of the event.
          for (size_t j = 0; j < end_vec_size; ++j)  // loop from endEvent array
          {
              const size_t & index_j = end_container[end_vec_offset + j];
              // Sanity check no overflow index
#ifndef DP_IGNORE_CHECK
              if (index_j >= m_ret_window)
              {
                  break;
              }
#endif
              for (size_t i = 0; i < start_vec_size; ++i)
              {
                  const size_t & index_i = start_container[start_vec_offset + i];
                  // start_event time is bigger than end_event time, ignore it
                  if (index_i >= index_j)
                  {
                      // NOTE: assume both array are sorted. this is true for our cases
                      break;
                  }
                  values[index_i * m_ret_window + index_j] += 1;
              }
          }

          // Count total users in the triangle 2D array.
          for (size_t i = 0; i < start_vec_size; i++)
          {
              const size_t & index_i = start_container[start_vec_offset + i];
              if (index_i >= m_ret_window) break;
              values[index_i * m_ret_window + index_i] += 1;
          }
    }

    template<typename MT = CompressT>
    typename std::enable_if<std::is_same<MT, compress_trait<NO_COMPRESS_NO_SORTED>>::value, void>::type
    addImpl(AggregateDataPtr place, const IColumn** columns, size_t row_num, [[maybe_unused]] Arena * arena) const
    {
          const ColumnArray & array_column_start = static_cast<const ColumnArray &>(*columns[0]);
          const IColumn::Offsets & start_offsets = array_column_start.getOffsets();
          auto & start_container = static_cast<const ColumnVector<T> &>(array_column_start.getData()).getData();
          const size_t start_vec_offset = row_num == 0 ? 0 : start_offsets[row_num - 1];
          const T* p_cur_start_container = &start_container[0] + start_vec_offset;
          const size_t start_vec_size = (start_offsets[row_num] - start_vec_offset);

          const ColumnArray &array_column_end = static_cast<const ColumnArray &>(*columns[1]);
          const IColumn::Offsets & end_offsets = array_column_end.getOffsets();
          auto & end_container = static_cast<const ColumnVector<T> &>(array_column_end.getData()).getData();
          const size_t end_vec_offset = row_num == 0 ? 0 : end_offsets[row_num - 1];
          const T* p_cur_end_container = &end_container[0] + end_vec_offset;
          const size_t end_vec_size = (end_offsets[row_num] - end_vec_offset);

          // result array for retention
          auto & values = this->data(place).retentions;
          if (start_vec_size != end_vec_size || start_vec_size != m_ret_window)
          {
              if (start_vec_size == m_ret_window)
              {
                  for (size_t i = 0; i < m_ret_window; i++)
                  {
                      if (p_cur_start_container[i])
                          values[i * m_ret_window + i] += 1;
                  }
              }
              return;
          }

          for (size_t i = 0; i< m_ret_window; i++)
          {
              if (p_cur_start_container[i])
              {
                  values[i * m_ret_window + i] += 1;
                  for (size_t j = i+1; j < m_ret_window; j++)
                      values[i * m_ret_window + j] += p_cur_end_container[j];
              }
          }
    }


    void add(AggregateDataPtr place, const IColumn** columns, size_t row_num, Arena * arena) const override
    {
        addImpl(place, columns, row_num, arena);
    }

    void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, [[maybe_unused]] Arena * arena) const override
    {
        auto & cur_elems = this->data(place).retentions;
        auto & rhs_elems = this->data(rhs).retentions;

        for (size_t i = 0; i < m_ret_array_size; i++)
            cur_elems[i] += rhs_elems[i];
    }

    void serialize(ConstAggregateDataPtr place, WriteBuffer & buf) const override
    {
        const auto & value = this->data(place).retentions;
        buf.write(reinterpret_cast<const char *>(&value[0]), m_ret_array_size * sizeof(value[0]));
    }

    void deserialize(AggregateDataPtr place, ReadBuffer & buf, [[maybe_unused]] Arena *arena) const override
    {
        auto & value = this->data(place).retentions;
        buf.read(reinterpret_cast<char *>(&value[0]), m_ret_array_size * sizeof(value[0]));
    }

    void insertResultInto(AggregateDataPtr __restrict place, IColumn & to, Arena * ) const override
    {
        const auto & value = this->data(place).retentions;

        ColumnArray & arr_to = static_cast<ColumnArray &>(to);
        ColumnArray::Offsets & offsets_to = arr_to.getOffsets();

        offsets_to.push_back((offsets_to.empty() ? 0 : offsets_to.back()) + m_ret_array_size);

        typename ColumnVector<RType>::Container & data_to = static_cast<ColumnVector<RType> &>(arr_to.getData()).getData();

        data_to.insert(value, value + m_ret_array_size);
    }


    bool allocatesMemoryInArena() const override
    {
        return true;
    }
};

using T = UInt8;
template <typename CompressT>
class AggregateFunctionAttrRetention2 final : public IAggregateFunctionDataHelper<AggregateFunctionRetentionData, AggregateFunctionAttrRetention2<CompressT>>
{
    UInt64 m_ret_window; // retention window size
    UInt64 m_ret_array_size;
    size_t m_window_words;
    std::vector<UInt64> slots;

public:
    AggregateFunctionAttrRetention2(UInt64 retWindow,
                                const DataTypes & arguments,
                                const Array & params
    ) :
        IAggregateFunctionDataHelper<AggregateFunctionRetentionData, AggregateFunctionAttrRetention2<CompressT>>(arguments, params),
        m_ret_window(retWindow)
    {
        m_ret_array_size = m_ret_window * m_ret_window;
        m_window_words = (m_ret_window + sizeof(T) * 8 -1) / (sizeof(T)*8);

        if (params.size() > 2)
        {
            for (size_t i = 1; i < params.size(); ++i)
            {
                UInt64 slot = params[i].safeGet<UInt64>();
                if (slot > m_ret_window)
                    throw Exception("Slot is large than window " + std::to_string(slot) + " > " + std::to_string(m_ret_window), ErrorCodes::LOGICAL_ERROR);

                slots.push_back(slot);
            }

            m_ret_array_size = m_ret_window * (slots.size() / 2 + 1);

            if (slots.size() < 2 || slots.size() % 2 != 0)
                throw Exception("Illegal size of params, it should be 1, 2, or an odd number", ErrorCodes::NUMBER_OF_ARGUMENTS_DOESNT_MATCH);
        }
    }

    String getName() const override { return "retention2"; }

    void create(const AggregateDataPtr place) const override
    {
        auto *d = new (place) AggregateFunctionRetentionData;
        std::fill(d->retentions, d->retentions+m_ret_array_size, 0);
    }

    size_t sizeOfData() const override
    {
        // reserve additional space for retentions information.
        return sizeof(AggregateFunctionRetentionData) + m_ret_array_size * sizeof(RType);
    }

    DataTypePtr getReturnType() const override
    {
        return std::make_shared<DataTypeArray>(std::make_shared<DataTypeNumber<RType>>());
    }

    template<typename MT = CompressT>
    inline typename std::enable_if<std::is_same<MT, compress_trait<COMPRESS_BIT>>::value, bool>::type
    getWordBit(const T word, size_t i) const
    {
        return (word & (1 << i));
    }

    bool intersect(const Array & first_event, const Array & return_event) const
    {
        for (Field field : return_event)
        {
            String s = field.get<String>();
            for (Field f : first_event)
            {
                if (f.get<String>() == s)
                    return true;
            }
        }
        return false;
    }

   bool intersect(const std::set<String> & end_slots_set, const std::vector<String> & start_events) const
   {
        for (const auto & start_e : start_events)
        {
            if (end_slots_set.count(start_e))
                return true;
        }
        return false;
   }

    void addImplWithSlots(AggregateDataPtr place, const IColumn** columns, size_t row_num) const
    {
        const auto& nested_arr_start = static_cast<const ColumnArray &>(*columns[0]); //Array(Array(String))
        const auto& nested_arr_end = static_cast<const ColumnArray &>(*columns[1]);

        const auto& field_column_start = static_cast<const Field &>(nested_arr_start.operator[](row_num));
        const auto& field_column_end = static_cast<const Field &>(nested_arr_end.operator[](row_num));

        const auto& array_column_start = field_column_start.get<Array>();
        const auto& array_column_end = field_column_end.get<Array>();

        const size_t slot_size = slots.size();
        const size_t end_slot_size = slot_size / 2;

        std::vector<std::vector<String>> start_slots_container(m_ret_window, std::vector<String>());
        std::vector<std::vector<std::set<String>>> end_slots_set(m_ret_window, std::vector<std::set<String>>(end_slot_size, std::set<String>()));

        for (size_t i = 0; i < m_ret_window && i < array_column_start.size(); ++i)
        {
            const auto & nested_array = array_column_start[i].get<Array>();
            for (const auto & e : nested_array)
                start_slots_container[i].push_back(e.get<String>());
        }

        // result array for retention
        auto & values = this->data(place).retentions;

        for (size_t start_i = 0; start_i < m_ret_window; ++start_i)
        {
            for (size_t i = 0, j = 1; j < slot_size; i += 2, j += 2)
            {
                size_t slot_i = start_i + slots[i];
                size_t slot_j = start_i + slots[j];

                // index start from 0, window range is [0, m_retWindow-1]
                if (slot_j >= m_ret_window) continue;

                for (size_t end_i = slot_i; end_i <= slot_j && end_i < m_ret_window && end_i < array_column_end.size(); ++end_i)
                {
                    const Array & end_events_array = array_column_end[end_i].get<Array>();
                    for (size_t end_ii = 0; end_ii < end_events_array.size(); ++end_ii)
                    {
                        const String & end_e = end_events_array[end_ii].get<String>();
                        end_slots_set[start_i][i/2].insert(end_e);
                    }
                }
            }
        }

        for (size_t iw = 0; iw < m_ret_window; iw++)
        {
            const std::vector<String> & start_events = start_slots_container[iw];
            if (start_events.empty()) continue;

            size_t value_offset = iw * (end_slot_size + 1);
            values[value_offset] += 1;

            for (size_t jw = 0; jw < end_slot_size; ++jw)
            {
                if (intersect(end_slots_set[iw][jw], start_events))
                    values[value_offset + jw + 1] += 1;
            }
        }
    }

    template<typename MT = CompressT>
    typename std::enable_if<std::is_same<MT, compress_trait<COMPRESS_BIT>>::value, void>::type
    addImpl(AggregateDataPtr place, const IColumn** columns, size_t row_num, [[maybe_unused]] Arena * arena) const
    {
        if (slots.size() >= 2)
        {
            addImplWithSlots(place, columns, row_num);
            return;
        }

        const auto& nested_arr_start = static_cast<const ColumnArray &>(*columns[0]); //Array(Array(String))
        const auto& nested_arr_end = static_cast<const ColumnArray &>(*columns[1]);

        const auto & nest_nested_arr_start = static_cast<const ColumnArray &>(nested_arr_start.getData()); //Array(String)
        const auto & nest_nested_arr_end = static_cast<const ColumnArray &>(nested_arr_end.getData());

        const auto& field_column_start = static_cast<const Field &>(nested_arr_start.operator[](row_num));
        const auto& field_column_end = static_cast<const Field &>(nested_arr_end.operator[](row_num));

        const auto& array_column_start = field_column_start.get<Array>();
        const auto& array_column_end = field_column_end.get<Array>();

        if(array_column_start.size() < array_column_end.size() || nested_arr_start.size() < nested_arr_end.size())
            throw Exception("The scope of start events need to be larger than the scope of end event", ErrorCodes::ILLEGAL_TYPE_OF_ARGUMENT);

#define WORD_SIZE_BIT (sizeof(T) << 3)

        //Note that start&end events might be with different size
        T start_words[m_window_words], end_words[m_window_words];
        size_t windows_start = array_column_start.size();
        size_t windows_end = array_column_end.size();

        for (size_t i = 0; i < m_window_words; i++)
        {
            start_words[i] = 0;
            for (size_t j = 0; j < WORD_SIZE_BIT; j++)
            {
                size_t index = i*WORD_SIZE_BIT + j;
                if (index >= windows_start) break;
                if (nested_arr_start.sizeAt(row_num) != 0 && nest_nested_arr_start.sizeAt(nested_arr_start.offsetAt(row_num)+index) != 0)
                    start_words[i] |= (1 << j);
            }
        }


        for (size_t i = 0; i < m_window_words; i++)
        {
            end_words[i] = 0;
            for (size_t j = 0; j < WORD_SIZE_BIT; j++)
            {
                size_t index = i * WORD_SIZE_BIT + j;
                if (index >= windows_end) break;
                if (nested_arr_end.sizeAt(row_num) != 0 && nest_nested_arr_end.sizeAt(nested_arr_end.offsetAt(row_num)+index) != 0)
                    end_words[i] |= (1 << j);
            }
        }

        // result array for retention
        auto & values = this->data(place).retentions;

        const size_t start_vec_size = (windows_start + WORD_SIZE_BIT-1) / WORD_SIZE_BIT;
        const size_t end_vec_size = (windows_end + WORD_SIZE_BIT - 1) / WORD_SIZE_BIT;

        T start_word, end_word;
        size_t iw_offset = 0, jw_offset = 0;

        if (start_vec_size != end_vec_size || start_vec_size != m_window_words)
        {
            // In this case, we need to count total user even end_event is not valid
            if (start_vec_size == m_window_words)
            {
                for (size_t iw = 0; iw < m_window_words; iw_offset += WORD_SIZE_BIT, iw++)
                {
                    start_word = start_words[iw];
                    if (start_word == 0) continue;
                    if (start_word == T(~0))
                    {
                        // Count total user in triangle vector
                        for (size_t iiw = 0; iiw < WORD_SIZE_BIT; iiw++)
                            values[(iw_offset + iiw) * m_ret_window + iw_offset + iiw] += 1;
                    }
                    else
                    {
                        for(size_t iiw = 0; iiw < WORD_SIZE_BIT; iiw++)
                        {
                            // count total user in triangle vector
                            if (getWordBit(start_word, iiw))
                                values[(iw_offset + iiw) * m_ret_window + iw_offset + iiw] += 1;
                        }
                    }
                }
            }
            return;
        }

        for (size_t iw = 0; iw< m_window_words; iw_offset += WORD_SIZE_BIT, iw++)
        {
            jw_offset = iw * WORD_SIZE_BIT; // start from iw word

            start_word = start_words[iw];
            if (start_word == 0) continue;
            // optimize all one case
            if (start_word == T(~0))
            {
                // Count total user in triangle vector
                for (size_t iiw = 0; iiw < WORD_SIZE_BIT; iiw++)
                    values[ (iw_offset + iiw) * m_ret_window + iw_offset + iiw] += 1;

                for (size_t jw = iw; jw < m_window_words; jw_offset += WORD_SIZE_BIT, jw++)
                {
                    end_word = end_words[jw];
                    if (end_word == 0) continue;
                    if (end_word == T(~0))
                    {
                        // assume residual piece is 0
                        for (size_t iiw = 0; iiw < WORD_SIZE_BIT; iiw++)
                        {
                            for (size_t jjw = 0; jjw < WORD_SIZE_BIT; jjw++)
                            {
                                bool is_intersect = intersect(array_column_start[iw_offset + iiw].get<Array>(), array_column_end[jw_offset + jjw].get<Array>());
                                if ((jw_offset +jjw) > (iw_offset +iiw) && is_intersect)
                                    values[(iw_offset + iiw) * m_ret_window + jw_offset + jjw] += 1;
                            }
                        }
                    }
                    else
                    {
                        for (size_t jjw = 0; jjw<WORD_SIZE_BIT; jjw++)
                        {
                            // check bit by bit
                            if (getWordBit(end_word, jjw))
                            {
                                // update result;
                                for (size_t iiw = 0; iiw < WORD_SIZE_BIT; iiw++)
                                {
                                    bool is_intersect = intersect(array_column_start[iw_offset + iiw].get<Array>(), array_column_end[jw_offset + jjw].get<Array>());
                                    if ((jw_offset + jjw) > (iw_offset + iiw) && is_intersect)
                                        values[(iw_offset + iiw) * m_ret_window + jw_offset + jjw] += 1;
                                }
                            }
                        }
                    }
                }
            }
            else
            {
                size_t tmp_offset = iw * WORD_SIZE_BIT;// start from iw word
                for (size_t iiw = 0; iiw<WORD_SIZE_BIT; iiw++)
                {
                    jw_offset = tmp_offset;
                    if (getWordBit(start_word, iiw))
                    {
                        // count total user in triangle vector
                        values[(iw_offset + iiw) * m_ret_window + iw_offset + iiw] += 1;

                        for (size_t jw = iw; jw < m_window_words; jw_offset += WORD_SIZE_BIT, jw++)
                        {
                            end_word = end_words[jw];
                            if (end_word == 0) continue;
                            if (end_word == T(~0))
                            {
                                //set array
                                for (size_t jjw = 0; jjw < WORD_SIZE_BIT; jjw++)
                                {
                                    bool is_intersect = intersect(array_column_start[iw_offset + iiw].get<Array>(), array_column_end[jw_offset + jjw].get<Array>());
                                    if ((jw_offset + jjw) > (iw_offset + iiw) && is_intersect)
                                        values[(iw_offset + iiw) * m_ret_window + jw_offset + jjw] += 1;
                                }
                            }
                            else
                            {
                                for (size_t jjw = 0; jjw < WORD_SIZE_BIT; jjw++)
                                {
                                    // check bit by bit
                                    if (getWordBit(end_word, jjw))
                                    {
                                        // update result;
                                        bool is_intersect = intersect(array_column_start[iw_offset + iiw].get<Array>(), array_column_end[jw_offset + jjw].get<Array>());
                                        if ((jw_offset + jjw) > (iw_offset + iiw) && is_intersect)
                                            values[(iw_offset + iiw) * m_ret_window + jw_offset + jjw] += 1;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

#undef WORD_SIZE_BIT
    }

    void add(AggregateDataPtr place, const IColumn** columns, size_t row_num, Arena * arena) const override
    {
        addImpl(place, columns, row_num, arena);
    }

    void merge(AggregateDataPtr place, ConstAggregateDataPtr rhs, [[maybe_unused]] Arena * arena) const override
    {
        auto & cur_elems = this->data(place).retentions;
        auto & rhs_elems = this->data(rhs).retentions;

        for (size_t i = 0; i < m_ret_array_size; i++)
            cur_elems[i] += rhs_elems[i];
    }

    void serialize(ConstAggregateDataPtr place, WriteBuffer & buf) const override
    {
        const auto & value = this->data(place).retentions;
        buf.write(reinterpret_cast<const char *>(&value[0]), m_ret_array_size * sizeof(value[0]));
    }

    void deserialize(AggregateDataPtr place, ReadBuffer & buf, [[maybe_unused]] Arena *arena) const override
    {
        auto & value = this->data(place).retentions;
        buf.read(reinterpret_cast<char *>(&value[0]), m_ret_array_size * sizeof(value[0]));
    }

    void insertResultInto(AggregateDataPtr __restrict place, IColumn & to, Arena * ) const override
    {
        const auto & value = this->data(place).retentions;

        ColumnArray & arr_to = static_cast<ColumnArray &>(to);
        ColumnArray::Offsets & offsets_to = arr_to.getOffsets();

        offsets_to.push_back((offsets_to.empty() ? 0 : offsets_to.back()) + m_ret_array_size);

        typename ColumnVector<RType>::Container & data_to = static_cast<ColumnVector<RType> &>(arr_to.getData()).getData();

        data_to.insert(value, value + m_ret_array_size);
    }

    bool allocatesMemoryInArena() const override { return true; }

};

#if !__clang__
#pragma GCC diagnostic pop
#endif

}

