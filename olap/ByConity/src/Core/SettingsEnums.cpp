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

#include <Core/SettingsEnums.h>


namespace DB
{
namespace ErrorCodes
{
    extern const int UNKNOWN_LOAD_BALANCING;
    extern const int UNKNOWN_OVERFLOW_MODE;
    extern const int UNKNOWN_TOTALS_MODE;
    extern const int UNKNOWN_DISTRIBUTED_PRODUCT_MODE;
    extern const int UNKNOWN_JOIN;
    extern const int BAD_ARGUMENTS;
    extern const int UNKNOWN_MYSQL_DATATYPES_SUPPORT_LEVEL;
    extern const int UNKNOWN_UNION;
}


IMPLEMENT_SETTING_ENUM(LoadBalancing, ErrorCodes::UNKNOWN_LOAD_BALANCING,
    {{"random",           LoadBalancing::RANDOM},
     {"nearest_hostname", LoadBalancing::NEAREST_HOSTNAME},
     {"in_order",         LoadBalancing::IN_ORDER},
     {"first_or_random",  LoadBalancing::FIRST_OR_RANDOM},
     {"round_robin",      LoadBalancing::ROUND_ROBIN}})


IMPLEMENT_SETTING_ENUM(JoinStrictness, ErrorCodes::UNKNOWN_JOIN,
    {{"",    JoinStrictness::Unspecified},
     {"ALL", JoinStrictness::ALL},
     {"ANY", JoinStrictness::ANY}})


IMPLEMENT_SETTING_ENUM(JoinAlgorithm, ErrorCodes::UNKNOWN_JOIN,
    {{"auto",                 JoinAlgorithm::AUTO},
     {"hash",                 JoinAlgorithm::HASH},
     {"partial_merge",        JoinAlgorithm::PARTIAL_MERGE},
     {"prefer_partial_merge", JoinAlgorithm::PREFER_PARTIAL_MERGE}})


IMPLEMENT_SETTING_ENUM(TotalsMode, ErrorCodes::UNKNOWN_TOTALS_MODE,
    {{"before_having",          TotalsMode::BEFORE_HAVING},
     {"after_having_exclusive", TotalsMode::AFTER_HAVING_EXCLUSIVE},
     {"after_having_inclusive", TotalsMode::AFTER_HAVING_INCLUSIVE},
     {"after_having_auto",      TotalsMode::AFTER_HAVING_AUTO}})


IMPLEMENT_SETTING_ENUM(OverflowMode, ErrorCodes::UNKNOWN_OVERFLOW_MODE,
    {{"throw", OverflowMode::THROW},
     {"break", OverflowMode::BREAK}})


IMPLEMENT_SETTING_ENUM_WITH_RENAME(OverflowModeGroupBy, ErrorCodes::UNKNOWN_OVERFLOW_MODE,
    {{"throw", OverflowMode::THROW},
     {"break", OverflowMode::BREAK},
     {"any", OverflowMode::ANY}})


IMPLEMENT_SETTING_ENUM(DistributedProductMode, ErrorCodes::UNKNOWN_DISTRIBUTED_PRODUCT_MODE,
    {{"deny",   DistributedProductMode::DENY},
     {"local",  DistributedProductMode::LOCAL},
     {"global", DistributedProductMode::GLOBAL},
     {"allow",  DistributedProductMode::ALLOW}})


IMPLEMENT_SETTING_ENUM_WITH_RENAME(DateTimeInputFormat, ErrorCodes::BAD_ARGUMENTS,
    {{"basic",       FormatSettings::DateTimeInputFormat::Basic},
     {"best_effort", FormatSettings::DateTimeInputFormat::BestEffort}})


IMPLEMENT_SETTING_ENUM_WITH_RENAME(DateTimeOutputFormat, ErrorCodes::BAD_ARGUMENTS,
    {{"simple",         FormatSettings::DateTimeOutputFormat::Simple},
     {"iso",            FormatSettings::DateTimeOutputFormat::ISO},
     {"unix_timestamp", FormatSettings::DateTimeOutputFormat::UnixTimestamp}})

IMPLEMENT_SETTING_ENUM(LogsLevel, ErrorCodes::BAD_ARGUMENTS,
    {{"none",        LogsLevel::none},
     {"fatal",       LogsLevel::fatal},
     {"error",       LogsLevel::error},
     {"warning",     LogsLevel::warning},
     {"information", LogsLevel::information},
     {"debug",       LogsLevel::debug},
     {"trace",       LogsLevel::trace}})


IMPLEMENT_SETTING_ENUM_WITH_RENAME(LogQueriesType, ErrorCodes::BAD_ARGUMENTS,
    {{"QUERY_START",                QUERY_START},
     {"QUERY_FINISH",               QUERY_FINISH},
     {"EXCEPTION_BEFORE_START",     EXCEPTION_BEFORE_START},
     {"EXCEPTION_WHILE_PROCESSING", EXCEPTION_WHILE_PROCESSING}})


IMPLEMENT_SETTING_ENUM_WITH_RENAME(DefaultDatabaseEngine, ErrorCodes::BAD_ARGUMENTS,
    {{"Ordinary", DefaultDatabaseEngine::Ordinary},
     {"Atomic",   DefaultDatabaseEngine::Atomic},
     {"Cnch",     DefaultDatabaseEngine::Cnch},
     {"Memory",   DefaultDatabaseEngine::Memory}})

IMPLEMENT_SETTING_MULTI_ENUM(MySQLDataTypesSupport, ErrorCodes::UNKNOWN_MYSQL_DATATYPES_SUPPORT_LEVEL,
    {{"decimal",    MySQLDataTypesSupport::DECIMAL},
     {"datetime64", MySQLDataTypesSupport::DATETIME64}})

IMPLEMENT_SETTING_ENUM(UnionMode, ErrorCodes::UNKNOWN_UNION,
    {{"",         UnionMode::Unspecified},
     {"ALL",      UnionMode::ALL},
     {"DISTINCT", UnionMode::DISTINCT}})

IMPLEMENT_SETTING_ENUM(DistributedDDLOutputMode, ErrorCodes::BAD_ARGUMENTS,
    {{"none",         DistributedDDLOutputMode::NONE},
     {"throw",    DistributedDDLOutputMode::THROW},
     {"null_status_on_timeout", DistributedDDLOutputMode::NULL_STATUS_ON_TIMEOUT},
     {"never_throw", DistributedDDLOutputMode::NEVER_THROW}})

IMPLEMENT_SETTING_ENUM(HandleKafkaErrorMode, ErrorCodes::BAD_ARGUMENTS,
    {{"default",      HandleKafkaErrorMode::DEFAULT},
     {"stream",       HandleKafkaErrorMode::STREAM}})

IMPLEMENT_SETTING_ENUM(DialectType, ErrorCodes::BAD_ARGUMENTS,
    {{"CLICKHOUSE", DialectType::CLICKHOUSE},
     {"ANSI",       DialectType::ANSI}})

IMPLEMENT_SETTING_ENUM(CTEMode, ErrorCodes::BAD_ARGUMENTS,
    {{"INLINED", CTEMode::INLINED},
     {"SHARED", CTEMode::SHARED},
     {"AUTO", CTEMode::AUTO}})

IMPLEMENT_SETTING_ENUM(StatisticsAccurateSampleNdvMode, ErrorCodes::BAD_ARGUMENTS,
    {{"NEVER", StatisticsAccurateSampleNdvMode::NEVER},
     {"AUTO", StatisticsAccurateSampleNdvMode::AUTO},
     {"ALWAYS", StatisticsAccurateSampleNdvMode::ALWAYS}})

IMPLEMENT_SETTING_ENUM(DiskCacheMode, ErrorCodes::BAD_ARGUMENTS,
    {{"AUTO", DiskCacheMode::AUTO},
     {"USE_DISK_CACHE", DiskCacheMode::USE_DISK_CACHE},
     {"SKIP_DISK_CACHE", DiskCacheMode::SKIP_DISK_CACHE},
     {"FORCE_CHECKSUMS_DISK_CACHE", DiskCacheMode::FORCE_CHECKSUMS_DISK_CACHE}})
}
