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

#include <Common/FieldVisitors.h>


namespace DB
{

/** Implements `+=` operation.
 *  Returns false if the result is zero.
 */
class FieldVisitorSum : public StaticVisitor<bool>
{
private:
    const Field & rhs;
public:
    explicit FieldVisitorSum(const Field & rhs_);

    // We can add all ints as unsigned regardless of their actual signedness.
    bool operator() (Int64 & x) const;
    bool operator() (UInt64 & x) const;
    bool operator() (Float64 & x) const;
    bool operator() (Null &) const;
    bool operator() (NegativeInfinity & x) const;
    bool operator() (PositiveInfinity & x) const;
    bool operator() (String &) const;
    bool operator() (Array &) const;
    bool operator() (Tuple &) const;
    bool operator() (Map &) const;
    bool operator() (ByteMap &) const;
    bool operator() (UUID &) const;
    bool operator() (AggregateFunctionStateData &) const;
    bool operator() (BitMap64 &) const;

    template <typename T>
    bool operator() (DecimalField<T> & x) const
    {
        x += get<DecimalField<T>>(rhs);
        return x.getValue() != T(0);
    }

    template <typename T, typename = std::enable_if_t<is_big_int_v<T>> >
    bool operator() (T & x) const
    {
        x += rhs.reinterpret<T>();
        return x != T(0);
    }
};

}
