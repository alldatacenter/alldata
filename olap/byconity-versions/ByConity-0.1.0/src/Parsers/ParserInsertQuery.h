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


/** Cases:
  *
  * Normal case:
  * INSERT INTO [db.]table (c1, c2, c3) VALUES (v11, v12, v13), (v21, v22, v23), ...
  * INSERT INTO [db.]table VALUES (v11, v12, v13), (v21, v22, v23), ...
  *
  * Insert of data in an arbitrary format.
  * The data itself comes after LF(line feed), if it exists, or after all the whitespace characters, otherwise.
  * INSERT INTO [db.]table (c1, c2, c3) FORMAT format \n ...
  * INSERT INTO [db.]table FORMAT format \n ...
  *
  * Insert the result of the SELECT or WATCH query.
  * INSERT INTO [db.]table (c1, c2, c3) SELECT | WATCH  ...
  * INSERT INTO [db.]table SELECT | WATCH ...
  */
class ParserInsertQuery : public IParserDialectBase
{
private:
    const char * end;

    const char * getName() const override { return "INSERT query"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    explicit ParserInsertQuery(const char * end_, ParserSettingsImpl t) : IParserDialectBase(t), end(end_) {}
};

/** Insert accepts an identifier and an asterisk with variants.
  */
class ParserInsertElement : public IParserDialectBase
{
protected:
    const char * getName() const override { return "insert element"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};

}
