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

#include <Core/SettingsEnums.h>
#include <Parsers/IParser.h>


namespace DB
{

/** Base class for most parsers
  */
class IParserBase : public IParser
{
public:
    template <typename F>
    static bool wrapParseImpl(Pos & pos, const F & func)
    {
        Pos begin = pos;
        bool res = func();
        if (!res)
          pos = begin;
        return res;
    }

    struct IncreaseDepthTag {};

    template <typename F>
    static bool wrapParseImpl(Pos & pos, IncreaseDepthTag, const F & func)
    {
        Pos begin = pos;
        pos.increaseDepth();
        bool res = func();
        pos.decreaseDepth();
        if (!res)
          pos = begin;
        return res;
    }

    bool parse(Pos & pos, ASTPtr & node, Expected & expected) override;  // -V1071

protected:
    virtual bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) = 0;
};

struct ParserSettingsImpl
{
    bool parse_literal_as_decimal;

    /// parse syntax `WITH expr AS alias`
    bool parse_with_alias;

    /// parse outer join with using
    bool parse_outer_join_with_using;

};

struct ParserSettings
{
    const static inline ParserSettingsImpl CLICKHOUSE {
        .parse_literal_as_decimal = false,
        .parse_with_alias = true,
        .parse_outer_join_with_using = true,
    };

    const static inline ParserSettingsImpl ANSI {
        .parse_literal_as_decimal = true,
        .parse_with_alias = false,
        .parse_outer_join_with_using = false,
    };

    static ParserSettingsImpl valueOf(enum DialectType dt)
    {
        switch (dt)
        {
            case DialectType::CLICKHOUSE:
                return CLICKHOUSE;
            case DialectType::ANSI:
                return ANSI;
        }
    }
};

class IParserDialectBase : public IParserBase
{
public:
    explicit IParserDialectBase(ParserSettingsImpl t = ParserSettings::CLICKHOUSE) : dt(t) {}
protected:
    ParserSettingsImpl dt;
};

}
