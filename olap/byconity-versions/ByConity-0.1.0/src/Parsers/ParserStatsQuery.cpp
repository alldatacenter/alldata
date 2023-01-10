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

#include <Parsers/ASTLiteral.h>
#include <Parsers/CommonParsers.h>
#include <Parsers/ExpressionListParsers.h>
#include <Parsers/ParserPartition.h>
#include <Parsers/ParserStatsQuery.h>
#include <Common/FieldVisitorConvertToNumber.h>

namespace DB
{

bool parseStatsQueryKind(IParser::Pos & pos, Expected & expected, StatsQueryKind & kind)
{
    ParserKeyword s_stats("STATS");
    ParserKeyword s_table_stats("TABLE_STATS");
    ParserKeyword s_column_stats("COLUMN_STATS");

    if (s_stats.ignore(pos, expected))
        kind = StatsQueryKind::ALL_STATS;
    else if (s_table_stats.ignore(pos, expected))
        kind = StatsQueryKind::TABLE_STATS;
    else if (s_column_stats.ignore(pos, expected))
        kind = StatsQueryKind::COLUMN_STATS;
    else
        return false;

    return true;
}

template <typename SimpleWithSpecifierInfo>
class ParserCreateStatsQuerySimpleSpecifier : public IParserBase
{
public:
    [[nodiscard]] const char * getName() const override { return SimpleWithSpecifierInfo::Name; }

protected:
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override
    {
        typename SimpleWithSpecifierInfo::ParserType number_p;
        ParserKeyword s_specifier(SimpleWithSpecifierInfo::Specifier);

        if (!number_p.parse(pos, node, expected))
            return false;

        if (!s_specifier.ignore(pos, expected))
        {
            node = nullptr;
            return false;
        }

        return true;
    }
};

struct ParserCreateStatsQuerySampleRowsSpecifierInfo
{
    using ParserType = ParserUnsignedInteger;
    static constexpr auto Name = "CreateStat query sample rows specifier";
    static constexpr auto Specifier = "ROWS";
};

struct ParserCreateStatsQuerySampleRatioSpecifierInfo
{
    using ParserType = ParserNumber;
    static constexpr auto Name = "CreateStat query sample ratio specifier";
    static constexpr auto Specifier = "Ratio";
};

using ParserCreateStatsQuerySampleRowsSpecifier = ParserCreateStatsQuerySimpleSpecifier<ParserCreateStatsQuerySampleRowsSpecifierInfo>;
using ParserCreateStatsQuerySampleRatioSpecifier = ParserCreateStatsQuerySimpleSpecifier<ParserCreateStatsQuerySampleRatioSpecifierInfo>;

Int64 getValueFromUInt64Literal(const ASTPtr & node)
{
    if (auto * literal = node->as<ASTLiteral>())
    {
        UInt64 val = literal->value.safeGet<UInt64>();

        if (val > static_cast<UInt64>(std::numeric_limits<Int64>::max()))
            throw Exception("Value exceed limit", ErrorCodes::SYNTAX_ERROR);

        return static_cast<Int64>(val);
    }
    else
        throw Exception("Not a literal node", ErrorCodes::SYNTAX_ERROR);
}

Float64 getValueFromNumberLiteral(const ASTPtr & node)
{
    if (auto * literal = node->as<ASTLiteral>())
    {
        try
        {
            double val = applyVisitor(FieldVisitorConvertToNumber<Float64>(), literal->value);
            return val;
        }
        catch (...)
        {
            throw Exception("Not a literal node", ErrorCodes::SYNTAX_ERROR);
        }
    }
    else
        throw Exception("Not a literal node", ErrorCodes::SYNTAX_ERROR);
}

bool ParserCreateStatsQuery::parseSuffix(Pos & pos, Expected & expected, IAST & ast)
{
    ParserKeyword s_partition("PARTITION");
    ParserKeyword s_with("WITH");
    ParserPartition partition_p;
    ParserNothing dummy_p;

    auto & create_stats_ast = dynamic_cast<ASTCreateStatsQuery &>(ast);

    if (s_partition.ignore(pos, expected))
    {
        ASTPtr partition;

        if (!partition_p.parse(pos, partition, expected))
            return false;

        create_stats_ast.partition = partition;
        create_stats_ast.children.push_back(create_stats_ast.partition);
    }

    if (s_with.ignore(pos, expected))
    {
        auto parse_specifier = [&pos, &expected, &create_stats_ast] {
            ParserKeyword s_sample("SAMPLE");
            ParserKeyword s_fullscan("FULLSCAN");
            ASTPtr node;

            if (s_fullscan.ignore(pos, expected))
            {
                if (create_stats_ast.sample_type != SampleType::Default)
                    return false; // duplicate
                create_stats_ast.sample_type = SampleType::FullScan;
                return true;
            }
            else if (s_sample.ignore(pos, expected))
            {
                if (create_stats_ast.sample_type != SampleType::Default)
                    return false; // duplicate

                create_stats_ast.sample_type = SampleType::Sample;
                ParserCreateStatsQuerySampleRowsSpecifier rows_p;
                ParserCreateStatsQuerySampleRatioSpecifier ratio_p;
                while (true)
                {
                    if (rows_p.parse(pos, node, expected))
                    {
                        if (create_stats_ast.sample_rows)
                            return false; // duplicate
                        create_stats_ast.sample_rows = getValueFromUInt64Literal(node);
                    }
                    else if (ratio_p.parse(pos, node, expected))
                    {
                        if (create_stats_ast.sample_ratio)
                            return false; // duplicate
                        create_stats_ast.sample_ratio = getValueFromNumberLiteral(node);
                    }
                    else
                    {
                        break;
                    }
                }
                return true;
            }
            else
                return false;
        };

        if (!ParserList::parseUtil(pos, expected, parse_specifier, dummy_p, false))
            return false;
    }

    return true;
}

}
