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

#include <Core/SettingsFields.h>
#include <DataStreams/SizeLimits.h>
#include <Formats/FormatSettings.h>


namespace DB
{
enum class LoadBalancing
{
    /// among replicas with a minimum number of errors selected randomly
    RANDOM = 0,
    /// a replica is selected among the replicas with the minimum number of errors
    /// with the minimum number of distinguished characters in the replica name and local hostname
    NEAREST_HOSTNAME,
    // replicas with the same number of errors are accessed in the same order
    // as they are specified in the configuration.
    IN_ORDER,
    /// if first replica one has higher number of errors,
    ///   pick a random one from replicas with minimum number of errors
    FIRST_OR_RANDOM,
    // round robin across replicas with the same number of errors.
    ROUND_ROBIN,
};

DECLARE_SETTING_ENUM(LoadBalancing)


enum class JoinStrictness
{
    Unspecified = 0, /// Query JOIN without strictness will throw Exception.
    ALL, /// Query JOIN without strictness -> ALL JOIN ...
    ANY, /// Query JOIN without strictness -> ANY JOIN ...
};

DECLARE_SETTING_ENUM(JoinStrictness)

enum class JoinAlgorithm
{
    AUTO = 0,
    HASH,
    PARTIAL_MERGE,
    PREFER_PARTIAL_MERGE,
    NESTED_LOOP_JOIN,
};

DECLARE_SETTING_ENUM(JoinAlgorithm)


/// Which rows should be included in TOTALS.
enum class TotalsMode
{
    BEFORE_HAVING            = 0, /// Count HAVING for all read rows;
                                  ///  including those not in max_rows_to_group_by
                                  ///  and have not passed HAVING after grouping.
    AFTER_HAVING_INCLUSIVE    = 1, /// Count on all rows except those that have not passed HAVING;
                                   ///  that is, to include in TOTALS all the rows that did not pass max_rows_to_group_by.
    AFTER_HAVING_EXCLUSIVE    = 2, /// Include only the rows that passed and max_rows_to_group_by, and HAVING.
    AFTER_HAVING_AUTO         = 3, /// Automatically select between INCLUSIVE and EXCLUSIVE,
};

DECLARE_SETTING_ENUM(TotalsMode)


/// The settings keeps OverflowMode which cannot be OverflowMode::ANY.
DECLARE_SETTING_ENUM(OverflowMode)

/// The settings keeps OverflowMode which can be OverflowMode::ANY.
DECLARE_SETTING_ENUM_WITH_RENAME(OverflowModeGroupBy, OverflowMode)


/// The setting for executing distributed subqueries inside IN or JOIN sections.
enum class DistributedProductMode
{
    DENY = 0,    /// Disable
    LOCAL,       /// Convert to local query
    GLOBAL,      /// Convert to global query
    ALLOW        /// Enable
};

DECLARE_SETTING_ENUM(DistributedProductMode)


DECLARE_SETTING_ENUM_WITH_RENAME(DateTimeInputFormat, FormatSettings::DateTimeInputFormat)

DECLARE_SETTING_ENUM_WITH_RENAME(DateTimeOutputFormat, FormatSettings::DateTimeOutputFormat)

enum class LogsLevel
{
    none = 0,    /// Disable
    fatal,
    error,
    warning,
    information,
    debug,
    trace,
};

DECLARE_SETTING_ENUM(LogsLevel)


// Make it signed for compatibility with DataTypeEnum8
enum QueryLogElementType : int8_t
{
    QUERY_START = 1,
    QUERY_FINISH = 2,
    EXCEPTION_BEFORE_START = 3,
    EXCEPTION_WHILE_PROCESSING = 4,
};

DECLARE_SETTING_ENUM_WITH_RENAME(LogQueriesType, QueryLogElementType)


enum class DefaultDatabaseEngine
{
    Ordinary,
    Atomic,
    Cnch,
    Memory,
};

DECLARE_SETTING_ENUM(DefaultDatabaseEngine)


enum class MySQLDataTypesSupport
{
    DECIMAL, // convert MySQL's decimal and number to ClickHouse Decimal when applicable
    DATETIME64, // convert MySQL's DATETIME and TIMESTAMP and ClickHouse DateTime64 if precision is > 0 or range is greater that for DateTime.
    // ENUM
};

DECLARE_SETTING_MULTI_ENUM(MySQLDataTypesSupport)

enum class UnionMode
{
    Unspecified = 0, // Query UNION without UnionMode will throw exception
    ALL, // Query UNION without UnionMode -> SELECT ... UNION ALL SELECT ...
    DISTINCT // Query UNION without UnionMode -> SELECT ... UNION DISTINCT SELECT ...
};

DECLARE_SETTING_ENUM(UnionMode)

enum class DistributedDDLOutputMode
{
    NONE,
    THROW,
    NULL_STATUS_ON_TIMEOUT,
    NEVER_THROW,
};

DECLARE_SETTING_ENUM(DistributedDDLOutputMode)

enum class HandleKafkaErrorMode
{
    DEFAULT = 0, // Ignore errors whit threshold.
    STREAM, // Put errors to stream in the virtual column named ``_error.
    /*FIXED_SYSTEM_TABLE, Put errors to in a fixed system table likey system.kafka_errors. This is not implemented now.  */
    /*CUSTOM_SYSTEM_TABLE, Put errors to in a custom system table. This is not implemented now.  */
};

DECLARE_SETTING_ENUM(HandleKafkaErrorMode)

enum class DialectType {
    CLICKHOUSE,
    ANSI,
};

struct SettingFieldDialectTypeTraits
{
    using EnumType = DialectType;
    static const String & toString(DialectType value);
    static DialectType fromString(const std::string_view & str);
};

struct SettingFieldDialectType
{
    using EnumType = DialectType;
    using Traits = SettingFieldDialectTypeTraits;

    DialectType value;
    bool changed = false;
    bool pending = false;

    explicit SettingFieldDialectType(DialectType x = DialectType{0}) : value(x) {}
    explicit SettingFieldDialectType(const Field & f) :
        SettingFieldDialectType(Traits::fromString(f.safeGet<const String &>())) {}

    SettingFieldDialectType& operator =(DialectType x) {
        value = x;
        changed = true;
        return *this;
    }
    SettingFieldDialectType& operator =(const Field & f) {
        *this = Traits::fromString(f.safeGet<const String &>());
        return *this;
    }

    operator DialectType() const { return value; }
    explicit operator Field() const { return toString(); }

    String toString() const { return Traits::toString(value); }
    void parseFromString(const String & str) {
        *this = Traits::fromString(str);
    }

    void writeBinary(WriteBuffer & out) const {
        SettingFieldEnumHelpers::writeBinary(toString(), out);
    }

    void readBinary(ReadBuffer & in) {
        *this = Traits::fromString(SettingFieldEnumHelpers::readBinary(in));
    }
};

enum class CTEMode
{
    INLINED,
    SHARED,
    AUTO,
};

DECLARE_SETTING_ENUM(CTEMode)

enum class StatisticsAccurateSampleNdvMode
{
    NEVER,
    AUTO,
    ALWAYS,
};

DECLARE_SETTING_ENUM(StatisticsAccurateSampleNdvMode)
}
