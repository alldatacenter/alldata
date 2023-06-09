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

#include <Parsers/IParserBase.h>
#include <Parsers/CommonParsers.h>

namespace DB
{


class ParserPartToolkitQuery : public IParserDialectBase
{
protected:
    const char * end;
    const char * getName() const override { return "Part toolkit"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    explicit ParserPartToolkitQuery(const char * end_, ParserSettingsImpl t = ParserSettings::CLICKHOUSE) : IParserDialectBase(t), end(end_) {}
};


/**
  * Almost the same with ParserStorage, but without ENGINE type.
  * [PARTITION BY expr] [ORDER BY expr] [PRIMARY KEY expr] [UNIQUE KEY expr] [SAMPLE BY expr] [SETTINGS name = value, ...]
  */
class ParserPWStorage : public IParserDialectBase
{
protected:
    const char * getName() const override { return "PartToolkit storage definition"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};

}
