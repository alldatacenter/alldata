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

#include <Common/SipHash.h>
#include <Common/FieldVisitorToString.h>
#include <Common/FieldVisitorHash.h>
#include <Parsers/ASTLiteral.h>
#include <IO/WriteHelpers.h>
#include <IO/ReadHelpers.h>
#include <IO/WriteBufferFromString.h>
#include <IO/Operators.h>


namespace DB
{

void ASTLiteral::updateTreeHashImpl(SipHash & hash_state) const
{
    const char * prefix = "Literal_";
    hash_state.update(prefix, strlen(prefix));
    applyVisitor(FieldVisitorHash(hash_state), value);
}

namespace
{

/// Writes 'tuple' word before tuple literals for backward compatibility reasons.
class FieldVisitorToColumnName : public StaticVisitor<String>
{
public:
    template<typename T>
    String operator() (const T & x) const { return visitor(x); }

private:
    FieldVisitorToString visitor;
};

template<>
String FieldVisitorToColumnName::operator() (const Tuple & x) const
{
    WriteBufferFromOwnString wb;

    wb << "tuple(";
    for (auto it = x.begin(); it != x.end(); ++it)
    {
        if (it != x.begin())
            wb << ", ";
        wb << applyVisitor(*this, *it);
    }
    wb << ')';

    return wb.str();
}

}

void ASTLiteral::appendColumnNameImpl(WriteBuffer & ostr) const
{
    if (use_legacy_column_name_of_tuple)
    {
        appendColumnNameImplLegacy(ostr);
        return;
    }

    /// 100 - just arbitrary value.
    constexpr auto min_elements_for_hashing = 100;

    /// Special case for very large arrays and tuples. Instead of listing all elements, will use hash of them.
    /// (Otherwise column name will be too long, that will lead to significant slowdown of expression analysis.)
    auto type = value.getType();
    if ((type == Field::Types::Array && value.get<const Array &>().size() > min_elements_for_hashing)
        || (type == Field::Types::Tuple && value.get<const Tuple &>().size() > min_elements_for_hashing))
    {
        SipHash hash;
        applyVisitor(FieldVisitorHash(hash), value);
        UInt64 low, high;
        hash.get128(low, high);

        writeCString(type == Field::Types::Array ? "__array_" : "__tuple_", ostr);
        writeText(low, ostr);
        ostr.write('_');
        writeText(high, ostr);
    }
    else
    {
        String column_name = applyVisitor(FieldVisitorToString(), value);
        writeString(column_name, ostr);
    }
}

void ASTLiteral::appendColumnNameImplLegacy(WriteBuffer & ostr) const
{
     /// 100 - just arbitrary value.
    constexpr auto min_elements_for_hashing = 100;

    /// Special case for very large arrays. Instead of listing all elements, will use hash of them.
    /// (Otherwise column name will be too long, that will lead to significant slowdown of expression analysis.)
    auto type = value.getType();
    if ((type == Field::Types::Array && value.get<const Array &>().size() > min_elements_for_hashing))
    {
        SipHash hash;
        applyVisitor(FieldVisitorHash(hash), value);
        UInt64 low, high;
        hash.get128(low, high);

        writeCString("__array_", ostr);
        writeText(low, ostr);
        ostr.write('_');
        writeText(high, ostr);
    }
    else
    {
        String column_name = applyVisitor(FieldVisitorToColumnName(), value);
        writeString(column_name, ostr);
    }
}

void ASTLiteral::formatImplWithoutAlias(const FormatSettings & settings, IAST::FormatState &, IAST::FormatStateStacked) const
{
    settings.ostr << applyVisitor(FieldVisitorToString(), value);
}

void ASTLiteral::serialize(WriteBuffer & buf) const
{
    ASTWithAlias::serialize(buf);

    writeFieldBinary(value, buf);
    writeBinary(unique_column_name, buf);
    writeBinary(use_legacy_column_name_of_tuple, buf);
}

void ASTLiteral::deserializeImpl(ReadBuffer & buf)
{
    ASTWithAlias::deserializeImpl(buf);

    readFieldBinary(value, buf);
    readBinary(unique_column_name, buf);
    readBinary(use_legacy_column_name_of_tuple, buf);
}

ASTPtr ASTLiteral::deserialize(ReadBuffer & buf)
{
    auto literal = std::make_shared<ASTLiteral>(Field());
    literal->deserializeImpl(buf);
    return literal;
}

}
