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

class SipHash;

namespace DB
{

/** Updates SipHash by type and value of Field */
class FieldVisitorHash : public StaticVisitor<>
{
private:
    SipHash & hash;
public:
    FieldVisitorHash(SipHash & hash_);

    void operator() (const Null & x) const;
    void operator() (const NegativeInfinity & x) const;
    void operator() (const PositiveInfinity & x) const;
    void operator() (const UInt64 & x) const;
    void operator() (const UInt128 & x) const;
    void operator() (const UInt256 & x) const;
    void operator() (const Int64 & x) const;
    void operator() (const Int128 & x) const;
    void operator() (const Int256 & x) const;
    void operator() (const UUID & x) const;
    void operator() (const Float64 & x) const;
    void operator() (const String & x) const;
    void operator() (const Array & x) const;
    void operator() (const Tuple & x) const;
    void operator() (const Map & x) const;
    [[ noreturn ]] void operator() (const ByteMap & x) const;
    void operator() (const DecimalField<Decimal32> & x) const;
    void operator() (const DecimalField<Decimal64> & x) const;
    void operator() (const DecimalField<Decimal128> & x) const;
    void operator() (const DecimalField<Decimal256> & x) const;
    void operator() (const AggregateFunctionStateData & x) const;
    void operator() (const BitMap64 & x) const;
};

}
