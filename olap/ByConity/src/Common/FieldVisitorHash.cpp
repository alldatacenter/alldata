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

#include <Common/FieldVisitorHash.h>

#include <Common/SipHash.h>


namespace DB
{

FieldVisitorHash::FieldVisitorHash(SipHash & hash_) : hash(hash_) {}

void FieldVisitorHash::operator() (const Null &) const
{
    UInt8 type = Field::Types::Null;
    hash.update(type);
}

void FieldVisitorHash::operator() (const NegativeInfinity &) const
{
    UInt8 type = Field::Types::NegativeInfinity;
    hash.update(type);
}

void FieldVisitorHash::operator() (const PositiveInfinity &) const
{
    UInt8 type = Field::Types::PositiveInfinity;
    hash.update(type);
}

void FieldVisitorHash::operator() (const UInt64 & x) const
{
    UInt8 type = Field::Types::UInt64;
    hash.update(type);
    hash.update(x);
}

void FieldVisitorHash::operator() (const UInt128 & x) const
{
    UInt8 type = Field::Types::UInt128;
    hash.update(type);
    hash.update(x);
}

void FieldVisitorHash::operator() (const Int64 & x) const
{
    UInt8 type = Field::Types::Int64;
    hash.update(type);
    hash.update(x);
}

void FieldVisitorHash::operator() (const Int128 & x) const
{
    UInt8 type = Field::Types::Int128;
    hash.update(type);
    hash.update(x);
}

void FieldVisitorHash::operator() (const UUID & x) const
{
    UInt8 type = Field::Types::UUID;
    hash.update(type);
    hash.update(x);
}

void FieldVisitorHash::operator() (const Float64 & x) const
{
    UInt8 type = Field::Types::Float64;
    hash.update(type);
    hash.update(x);
}

void FieldVisitorHash::operator() (const String & x) const
{
    UInt8 type = Field::Types::String;
    hash.update(type);
    hash.update(x.size());
    hash.update(x.data(), x.size());
}

void FieldVisitorHash::operator() (const Tuple & x) const
{
    UInt8 type = Field::Types::Tuple;
    hash.update(type);
    hash.update(x.size());

    for (const auto & elem : x)
        applyVisitor(*this, elem);
}

void FieldVisitorHash::operator() (const Map & x) const
{
    UInt8 type = Field::Types::Map;
    hash.update(type);
    hash.update(x.size());

    for (const auto & elem : x)
        applyVisitor(*this, elem);
}

void FieldVisitorHash::operator() ([[maybe_unused]] const ByteMap & x) const
{
    throw Exception("FieldVisitorHash Map type not implemented!", ErrorCodes::NOT_IMPLEMENTED);
}

void FieldVisitorHash::operator() (const Array & x) const
{
    UInt8 type = Field::Types::Array;
    hash.update(type);
    hash.update(x.size());

    for (const auto & elem : x)
        applyVisitor(*this, elem);
}

void FieldVisitorHash::operator() (const DecimalField<Decimal32> & x) const
{
    UInt8 type = Field::Types::Decimal32;
    hash.update(type);
    hash.update(x.getValue().value);
}

void FieldVisitorHash::operator() (const DecimalField<Decimal64> & x) const
{
    UInt8 type = Field::Types::Decimal64;
    hash.update(type);
    hash.update(x.getValue().value);
}

void FieldVisitorHash::operator() (const DecimalField<Decimal128> & x) const
{
    UInt8 type = Field::Types::Decimal128;
    hash.update(type);
    hash.update(x.getValue().value);
}

void FieldVisitorHash::operator() (const DecimalField<Decimal256> & x) const
{
    UInt8 type = Field::Types::Decimal256;
    hash.update(type);
    hash.update(x.getValue().value);
}

void FieldVisitorHash::operator() (const AggregateFunctionStateData & x) const
{
    UInt8 type = Field::Types::AggregateFunctionState;
    hash.update(type);
    hash.update(x.name.size());
    hash.update(x.name.data(), x.name.size());
    hash.update(x.data.size());
    hash.update(x.data.data(), x.data.size());
}

void FieldVisitorHash::operator() (const UInt256 & x) const
{
    UInt8 type = Field::Types::UInt256;
    hash.update(type);
    hash.update(x);
}

void FieldVisitorHash::operator() (const Int256 & x) const
{
    UInt8 type = Field::Types::Int256;
    hash.update(type);
    hash.update(x);
}

void FieldVisitorHash::operator() (const BitMap64 & x) const
{
    UInt8 type = Field::Types::BitMap64;
    hash.update(type);
    hash.update(x.cardinality());

    for (roaring::Roaring64MapSetBitForwardIterator it(x); it != x.end(); ++it)
        applyVisitor(*this, Field(*it));
}

}
