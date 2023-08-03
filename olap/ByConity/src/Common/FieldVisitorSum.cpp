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

#include <Common/FieldVisitorSum.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
}


FieldVisitorSum::FieldVisitorSum(const Field & rhs_) : rhs(rhs_) {}

// We can add all ints as unsigned regardless of their actual signedness.
bool FieldVisitorSum::operator() (Int64 & x) const { return this->operator()(reinterpret_cast<UInt64 &>(x)); }
bool FieldVisitorSum::operator() (UInt64 & x) const
{
    x += rhs.reinterpret<UInt64>();
    return x != 0;
}

bool FieldVisitorSum::operator() (Float64 & x) const { x += get<Float64>(rhs); return x != 0; }

bool FieldVisitorSum::operator() (Null &) const { throw Exception("Cannot sum Nulls", ErrorCodes::LOGICAL_ERROR); }
bool FieldVisitorSum::operator() (NegativeInfinity &) const { throw Exception("Cannot sum -Inf", ErrorCodes::LOGICAL_ERROR); }
bool FieldVisitorSum::operator() (PositiveInfinity &) const { throw Exception("Cannot sum +Inf", ErrorCodes::LOGICAL_ERROR); }
bool FieldVisitorSum::operator() (String &) const { throw Exception("Cannot sum Strings", ErrorCodes::LOGICAL_ERROR); }
bool FieldVisitorSum::operator() (Array &) const { throw Exception("Cannot sum Arrays", ErrorCodes::LOGICAL_ERROR); }
bool FieldVisitorSum::operator() (Tuple &) const { throw Exception("Cannot sum Tuples", ErrorCodes::LOGICAL_ERROR); }
bool FieldVisitorSum::operator() (Map &) const { throw Exception("Cannot sum Maps", ErrorCodes::LOGICAL_ERROR); }
bool FieldVisitorSum::operator() (ByteMap &) const { throw Exception("Cannot sum Maps", ErrorCodes::LOGICAL_ERROR); }
bool FieldVisitorSum::operator() (UUID &) const { throw Exception("Cannot sum UUIDs", ErrorCodes::LOGICAL_ERROR); }
bool FieldVisitorSum::operator() (BitMap64 &) const { throw Exception("Cannot sum BitMap64", ErrorCodes::LOGICAL_ERROR); }

bool FieldVisitorSum::operator() (AggregateFunctionStateData &) const
{
    throw Exception("Cannot sum AggregateFunctionStates", ErrorCodes::LOGICAL_ERROR);
}

}
