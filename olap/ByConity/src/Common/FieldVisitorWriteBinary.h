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

class FieldVisitorWriteBinary
{
public:
    void operator() (const Null & x, WriteBuffer & buf) const;
    void operator() (const NegativeInfinity & x, WriteBuffer & buf) const;
    void operator() (const PositiveInfinity & x, WriteBuffer & buf) const;
    void operator() (const UInt64 & x, WriteBuffer & buf) const;
    void operator() (const UInt128 & x, WriteBuffer & buf) const;
    void operator() (const UInt256 & x, WriteBuffer & buf) const;
    void operator() (const Int64 & x, WriteBuffer & buf) const;
    void operator() (const Int128 & x, WriteBuffer & buf) const;
    void operator() (const Int256 & x, WriteBuffer & buf) const;
    void operator() (const UUID & x, WriteBuffer & buf) const;
    void operator() (const Float64 & x, WriteBuffer & buf) const;
    void operator() (const String & x, WriteBuffer & buf) const;
    void operator() (const Array & x, WriteBuffer & buf) const;
    void operator() (const Tuple & x, WriteBuffer & buf) const;
    void operator() (const Map & x, WriteBuffer & buf) const;
    void operator() (const ByteMap & x, WriteBuffer & buf) const;
    void operator() (const DecimalField<Decimal32> & x, WriteBuffer & buf) const;
    void operator() (const DecimalField<Decimal64> & x, WriteBuffer & buf) const;
    void operator() (const DecimalField<Decimal128> & x, WriteBuffer & buf) const;
    void operator() (const DecimalField<Decimal256> & x, WriteBuffer & buf) const;
    void operator() (const AggregateFunctionStateData & x, WriteBuffer & buf) const;
    void operator() (const BitMap64 & x, WriteBuffer & buf) const;
};

}
