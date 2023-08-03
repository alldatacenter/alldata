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

#include <Parsers/IParserBase.h>


namespace DB
{

struct ASTTableJoin;

/** List of single or multiple JOIN-ed tables or subqueries in SELECT query, with ARRAY JOINs and SAMPLE, FINAL modifiers.
  */
class ParserTablesInSelectQuery : public IParserDialectBase
{
protected:
    const char * getName() const override { return "table, table function, subquery or list of joined tables"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};


class ParserTablesInSelectQueryElement : public IParserDialectBase
{
public:
    explicit ParserTablesInSelectQueryElement(bool is_first_, ParserSettingsImpl t) : IParserDialectBase(t), is_first(is_first_) {}

protected:
    const char * getName() const override { return "table, table function, subquery or list of joined tables"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;

private:
    bool is_first;

    static void parseJoinStrictness(Pos & pos, ASTTableJoin & table_join);
};


class ParserTableExpression : public IParserDialectBase
{
protected:
    const char * getName() const override { return "table or subquery or table function"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};


class ParserArrayJoin : public IParserDialectBase
{
protected:
    const char * getName() const override { return "array join"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};


}
