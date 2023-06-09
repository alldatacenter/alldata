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

#include <Parsers/IParser.h>
#include <Parsers/IParserBase.h>

#include <Parsers/ParserSetQuery.h>

namespace DB
{

/// Parser for dictionary lifetime part. It should contain "lifetime" keyword,
/// opening bracket, literal value or two pairs and closing bracket:
/// lifetime(300), lifetime(min 100 max 200). Produces ASTDictionaryLifetime.
class ParserDictionaryLifetime : public IParserDialectBase
{
protected:
    const char * getName() const override { return "lifetime definition"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};

/// Parser for dictionary range part. It should contain "range" keyword opening
/// bracket, two pairs and closing bracket: range(min attr1 max attr2). Produces
/// ASTDictionaryRange.
class ParserDictionaryRange : public IParserDialectBase
{
protected:
    const char * getName() const override { return "range definition"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};


/// Parser for dictionary layout part. It should contain "layout" keyword,
/// opening bracket, possible pair with param value and closing bracket:
/// layout(type()) or layout(type(param value)). Produces ASTDictionaryLayout.
class ParserDictionaryLayout : public IParserDialectBase
{
protected:
    const char * getName() const override { return "layout definition"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};

class ParserDictionarySettings: public IParserDialectBase
{
protected:
    const char * getName() const override { return "settings definition"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};


/// Combines together all parsers from above and also parses primary key and
/// dictionary source, which consists of custom key-value pairs:
///
/// PRIMARY KEY key_column1, key_column2
/// SOURCE(MYSQL(HOST 'localhost' PORT 9000 USER 'default' REPLICA(HOST '127.0.0.1' PRIORITY 1) PASSWORD ''))
/// LAYOUT(CACHE(size_in_cells 50))
/// LIFETIME(MIN 1 MAX 10)
/// RANGE(MIN second_column MAX third_column)
///
/// Produces ASTDictionary.
class ParserDictionary : public IParserDialectBase
{
protected:
    const char * getName() const override { return "dictionary definition"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};

}
