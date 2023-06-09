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

#include <sstream>
#include <Parsers/ASTQueryWithOnCluster.h>
#include <Parsers/ASTQueryWithTableAndOutput.h>
#include <Parsers/IAST.h>
#include <Common/quoteString.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int SYNTAX_ERROR;
}

enum class StatsQueryKind
{
    TABLE_STATS,
    COLUMN_STATS,
    ALL_STATS
};

String formatStatsQueryKind(StatsQueryKind kind);
struct CreateStatsQueryInfo;


template <typename StatsQueryInfo>
class ASTStatsQueryBase : public ASTQueryWithTableAndOutput, public ASTQueryWithOnCluster
{
public:
    StatsQueryKind kind = StatsQueryKind::ALL_STATS;
    // whether this query target at a single table or all tables
    bool target_all = false;
    std::vector<String> columns;

    String getID(char delim) const override
    {
        std::ostringstream res;

        res << StatsQueryInfo::QueryPrefix << delim << formatStatsQueryKind(kind);

        if (!target_all)
        {
            if (!database.empty())
                res << delim << database;

            res << delim << table;
        }

        if (!columns.empty())
        {
            for (auto & col : columns)
            {
                res << delim << col;
            }
        }

        return res.str();
    }

    ASTPtr clone() const override
    {
        auto res = std::make_shared<ASTStatsQueryBase>(*this);
        res->children.clear();
        cloneOutputOptions(*res);
        return res;
    }

    ASTType getType() const override { return StatsQueryInfo::Type; }

    ASTPtr getRewrittenASTWithoutOnCluster(const std::string & new_database) const override
    {
        return removeOnCluster<ASTStatsQueryBase>(clone(), new_database);
    }

protected:
    // CREATE/DROP/SHOW STATS/COLUMN_STATS
    void formatQueryPrefix(const FormatSettings & settings, FormatState &, FormatStateStacked) const
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << StatsQueryInfo::QueryPrefix << ' ' << formatStatsQueryKind(kind)
                      << (settings.hilite ? hilite_none : "");
    }

    // ALL/<db_table_name> AT COLUMNS (<col1>, <col2>)
    void formatQueryMiddle(const FormatSettings & settings, FormatState &, FormatStateStacked) const
    {
        settings.ostr << ' ';
        if (target_all)
            settings.ostr << (settings.hilite ? hilite_keyword : "") << "ALL" << (settings.hilite ? hilite_none : "");
        else
        {
            settings.ostr << (settings.hilite ? hilite_identifier : "") << (!database.empty() ? backQuoteIfNeed(database) + "." : "")
                          << backQuoteIfNeed(table) << (settings.hilite ? hilite_none : "");
            if (!columns.empty())
            {
                settings.ostr << (settings.hilite ? hilite_keyword : "") << " (" << (settings.hilite ? hilite_none : "");
                settings.ostr << fmt::format(FMT_STRING("{}"), fmt::join(columns, ", "));
                settings.ostr << (settings.hilite ? hilite_keyword : "") << ")" << (settings.hilite ? hilite_none : "");
            }
        }

        formatOnCluster(settings);
    }

    // maybe override if this is not sufficient
    void formatQueryImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const override
    {
        formatQueryPrefix(settings, state, frame);
        formatQueryMiddle(settings, state, frame);
    }
};

struct CreateStatsQueryInfo
{
    static constexpr auto Type = ASTType::ASTCreateStatsQuery;
    static constexpr auto QueryPrefix = "CREATE";
};

struct ShowStatsQueryInfo
{
    static constexpr auto Type = ASTType::ASTShowStatsQuery;
    static constexpr auto QueryPrefix = "SHOW";
};

struct DropStatsQueryInfo
{
    static constexpr auto Type = ASTType::ASTDropStatsQuery;
    static constexpr auto QueryPrefix = "DROP";
};

using ASTShowStatsQuery = ASTStatsQueryBase<ShowStatsQueryInfo>;
using ASTDropStatsQuery = ASTStatsQueryBase<DropStatsQueryInfo>;

class ASTCreateStatsQuery : public ASTStatsQueryBase<CreateStatsQueryInfo>
{
public:
    ASTPtr partition;
    bool if_not_exists = false;
    enum class SampleType
    {
        Default = 0,
        FullScan = 1,
        Sample = 2
    };
    SampleType sample_type = SampleType::Default;
    std::optional<Int64> sample_rows = std::nullopt;
    std::optional<double> sample_ratio = std::nullopt;

    ASTPtr clone() const override;
    String getID(char delim) const override;

protected:
    void formatQueryImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const override;
};

}
