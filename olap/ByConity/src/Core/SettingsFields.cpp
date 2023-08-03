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

#include <Core/SettingsFields.h>

#include <Core/Field.h>
#include <Common/getNumberOfPhysicalCPUCores.h>
#include <Common/FieldVisitorConvertToNumber.h>
#include <common/logger_useful.h>
#include <IO/ReadHelpers.h>
#include <IO/ReadBufferFromString.h>
#include <IO/WriteHelpers.h>
#include <IO/Operators.h>
#include <boost/algorithm/string/predicate.hpp>
#include <regex>


namespace DB
{
namespace ErrorCodes
{
    extern const int SIZE_OF_FIXED_STRING_DOESNT_MATCH;
    extern const int CANNOT_PARSE_BOOL;
    extern const int BAD_ARGUMENTS;
}


namespace
{
    template <typename T>
    T stringToNumber(const String & str)
    {
        if constexpr (std::is_same_v<T, bool>)
        {
            if (str == "0")
                return false;
            if (str == "1")
                return true;
            if (boost::iequals(str, "false"))
                return false;
            if (boost::iequals(str, "true"))
                return true;
            throw Exception("Cannot parse bool from string '" + str + "'", ErrorCodes::CANNOT_PARSE_BOOL);
        }
        else
            return parseWithSizeSuffix<T>(str);
    }

    template <typename T>
    T fieldToNumber(const Field & f)
    {
        if (f.getType() == Field::Types::String)
            return stringToNumber<T>(f.get<const String &>());
        else
            return applyVisitor(FieldVisitorConvertToNumber<T>(), f);
    }
}

template <typename T>
SettingFieldNumber<T>::SettingFieldNumber(const Field & f) : SettingFieldNumber(fieldToNumber<T>(f))
{
}

template <typename T>
SettingFieldNumber<T> & SettingFieldNumber<T>::operator=(const Field & f)
{
    *this = fieldToNumber<T>(f);
    return *this;
}

template <typename T>
String SettingFieldNumber<T>::toString() const
{
    return ::DB::toString(value);
}

template <typename T>
void SettingFieldNumber<T>::parseFromString(const String & str)
{
    *this = stringToNumber<T>(str);
}

template <typename T>
void SettingFieldNumber<T>::writeBinary(WriteBuffer & out) const
{
    if constexpr (std::is_integral_v<T> && is_unsigned_v<T>)
        writeVarUInt(static_cast<UInt64>(value), out);
    else if constexpr (std::is_integral_v<T> && is_signed_v<T>)
        writeVarInt(static_cast<Int64>(value), out);
    else
    {
        static_assert(std::is_floating_point_v<T>);
        writeStringBinary(::DB::toString(value), out);
    }
}

template <typename T>
void SettingFieldNumber<T>::readBinary(ReadBuffer & in)
{
    if constexpr (std::is_integral_v<T> && is_unsigned_v<T>)
    {
        UInt64 x;
        readVarUInt(x, in);
        *this = static_cast<T>(x);
    }
    else if constexpr (std::is_integral_v<T> && is_signed_v<T>)
    {
        Int64 x;
        readVarInt(x, in);
        *this = static_cast<T>(value);
    }
    else
    {
        static_assert(std::is_floating_point_v<T>);
        String str;
        readStringBinary(str, in);
        *this = ::DB::parseFromString<T>(str);
    }
}

template struct SettingFieldNumber<UInt64>;
template struct SettingFieldNumber<Int64>;
template struct SettingFieldNumber<float>;
template struct SettingFieldNumber<bool>;


namespace
{
    UInt64 stringToMaxThreads(const String & str)
    {
        if (startsWith(str, "auto"))
            return 0;
        return parseFromString<UInt64>(str);
    }

    UInt64 fieldToMaxThreads(const Field & f)
    {
        if (f.getType() == Field::Types::String)
            return stringToMaxThreads(f.get<const String &>());
        else
            return applyVisitor(FieldVisitorConvertToNumber<UInt64>(), f);
    }
}

SettingFieldMaxThreads::SettingFieldMaxThreads(const Field & f) : SettingFieldMaxThreads(fieldToMaxThreads(f))
{
}

SettingFieldMaxThreads & SettingFieldMaxThreads::operator=(const Field & f)
{
    *this = fieldToMaxThreads(f);
    return *this;
}

String SettingFieldMaxThreads::toString() const
{
    if (is_auto)
        return "'auto(" + ::DB::toString(value) + ")'";
    else
        return ::DB::toString(value);
}

void SettingFieldMaxThreads::parseFromString(const String & str)
{
    *this = stringToMaxThreads(str);
}

void SettingFieldMaxThreads::writeBinary(WriteBuffer & out) const
{
    writeVarUInt(is_auto ? 0 : value, out);
}

void SettingFieldMaxThreads::readBinary(ReadBuffer & in)
{
    UInt64 x = 0;
    readVarUInt(x, in);
    *this = x;
}

UInt64 SettingFieldMaxThreads::getAuto()
{
    return getNumberOfPhysicalCPUCores();
}


template <SettingFieldTimespanUnit unit_>
SettingFieldTimespan<unit_>::SettingFieldTimespan(const Field & f) : SettingFieldTimespan(fieldToNumber<UInt64>(f))
{
}

template <SettingFieldTimespanUnit unit_>
SettingFieldTimespan<unit_> & SettingFieldTimespan<unit_>::operator=(const Field & f)
{
    *this = fieldToNumber<UInt64>(f);
    return *this;
}

template <SettingFieldTimespanUnit unit_>
String SettingFieldTimespan<unit_>::toString() const
{
    return ::DB::toString(operator UInt64());
}

template <SettingFieldTimespanUnit unit_>
void SettingFieldTimespan<unit_>::parseFromString(const String & str)
{
    *this = stringToNumber<UInt64>(str);
}

template <SettingFieldTimespanUnit unit_>
void SettingFieldTimespan<unit_>::writeBinary(WriteBuffer & out) const
{
    auto num_units = operator UInt64();
    writeVarUInt(num_units, out);
}

template <SettingFieldTimespanUnit unit_>
void SettingFieldTimespan<unit_>::readBinary(ReadBuffer & in)
{
    UInt64 num_units = 0;
    readVarUInt(num_units, in);
    *this = num_units;
}

template struct SettingFieldTimespan<SettingFieldTimespanUnit::Second>;
template struct SettingFieldTimespan<SettingFieldTimespanUnit::Millisecond>;


void SettingFieldString::writeBinary(WriteBuffer & out) const
{
    writeStringBinary(value, out);
}

void SettingFieldString::readBinary(ReadBuffer & in)
{
    String str;
    readStringBinary(str, in);
    *this = std::move(str);
}


namespace
{
    char stringToChar(const String & str)
    {
        if (str.size() > 1)
            throw Exception("A setting's value string has to be an exactly one character long", ErrorCodes::SIZE_OF_FIXED_STRING_DOESNT_MATCH);
        if (str.empty())
            return '\0';
        return str[0];
    }

    char fieldToChar(const Field & f)
    {
        return stringToChar(f.safeGet<const String &>());
    }
}

SettingFieldChar::SettingFieldChar(const Field & f) : SettingFieldChar(fieldToChar(f))
{
}

SettingFieldChar & SettingFieldChar::operator =(const Field & f)
{
    *this = fieldToChar(f);
    return *this;
}

void SettingFieldChar::parseFromString(const String & str)
{
    *this = stringToChar(str);
}

void SettingFieldChar::writeBinary(WriteBuffer & out) const
{
    writeStringBinary(toString(), out);
}

void SettingFieldChar::readBinary(ReadBuffer & in)
{
    String str;
    readStringBinary(str, in);
    *this = stringToChar(str);
}


void SettingFieldURI::writeBinary(WriteBuffer & out) const
{
    writeStringBinary(value.toString(), out);
}

void SettingFieldURI::readBinary(ReadBuffer & in)
{
    String str;
    readStringBinary(str, in);
    *this = Poco::URI{str};
}


void SettingFieldEnumHelpers::writeBinary(const std::string_view & str, WriteBuffer & out)
{
    writeStringBinary(str, out);
}

String SettingFieldEnumHelpers::readBinary(ReadBuffer & in)
{
    String str;
    readStringBinary(str, in);
    return str;
}


String SettingFieldCustom::toString() const
{
    return value.dump();
}

void SettingFieldCustom::parseFromString(const String & str)
{
    *this = Field::restoreFromDump(str);
}

void SettingFieldCustom::writeBinary(WriteBuffer & out) const
{
    writeStringBinary(toString(), out);
}

void SettingFieldCustom::readBinary(ReadBuffer & in)
{
    String str;
    readStringBinary(str, in);
    parseFromString(str);
}

SettingFieldMultiRegexString & SettingFieldMultiRegexString::operator=(const Field & f)
{
    value = parseStringToRegexSet(f.safeGet<const String &>());
    changed = true;
    return *this;
}

String SettingFieldMultiRegexString::toString() const
{
    WriteBufferFromOwnString res;

    bool is_first = true;
    for (const String & column : value)
    {
        if (!is_first)
            res << ", ";
        is_first = false;
        res << column;
    }
    return res.str();
}

void SettingFieldMultiRegexString::writeBinary(WriteBuffer & out) const
{
    writeVarUInt(value.size(), out);
    for (String item: value)
        writeStringBinary(item, out);
}

void SettingFieldMultiRegexString::readBinary(ReadBuffer & in)
{
    size_t cnt;
    readVarUInt(cnt, in);
    while(cnt-- > 0)
    {
        String item;
        readStringBinary(item, in);
        value.emplace(item);
    }
}

/// Split string into strings by comma
std::set<String> SettingFieldMultiRegexString::parseStringToRegexSet(String x)
{
    char comma = ',';
    std::set<String> res;
    x = x + comma;
    size_t last_pos = 0, pos = x.find(comma);
    while (pos != String::npos)
    {
        String item = x.substr(last_pos, pos - last_pos);
        if (!item.empty())
        {
            /// trim string
            item.erase(0, item.find_first_not_of("\r\t\n "));
            item.erase(item.find_last_not_of("\r\t\n ") + 1);
            if (!item.empty())
            {
                try
                {
                    std::regex regex(item);
                }
                catch(...)
                {
                    throw Exception(ErrorCodes::BAD_ARGUMENTS, "{} is not a valid regular expression.", item);
                }
                res.emplace(item);
            }
        }
        last_pos = pos + 1;
        pos = x.find(comma, last_pos);
    }
    return res;
}

}
