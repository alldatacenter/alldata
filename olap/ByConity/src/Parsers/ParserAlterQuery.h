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
#include <Parsers/ExpressionElementParsers.h>

namespace DB
{

/** Query like this:
  * ALTER TABLE [db.]name [ON CLUSTER cluster]
  *     [ADD COLUMN [IF NOT EXISTS] col_name type [AFTER col_after],]
  *     [DROP COLUMN [IF EXISTS] col_to_drop, ...]
  *     [CLEAR COLUMN [IF EXISTS] col_to_clear[ IN PARTITION partition],]
  *     [MODIFY COLUMN [IF EXISTS] col_to_modify type, ...]
  *     [RENAME COLUMN [IF EXISTS] col_name TO col_name]
  *     [MODIFY PRIMARY KEY (a, b, c...)]
  *     [MODIFY ORDER BY new_expression]
  *     [MODIFY CLUSTER BY new_expression]
  *     [MODIFY SETTING setting_name=setting_value, ...]
  *     [RESET SETTING setting_name, ...]
  *     [COMMENT COLUMN [IF EXISTS] col_name string]
  *     [DROP|DETACH|ATTACH PARTITION|PART partition, ...]
  *     [FETCH PARTITION partition FROM ...]
  *     [FREEZE [PARTITION] [WITH NAME name]]
  *     [DELETE[ IN PARTITION partition] WHERE ...]
  *     [UPDATE col_name = expr, ...[ IN PARTITION partition] WHERE ...]
  *     [ADD INDEX [IF NOT EXISTS] index_name [AFTER index_name]]
  *     [DROP INDEX [IF EXISTS] index_name]
  *     [CLEAR INDEX [IF EXISTS] index_name IN PARTITION partition]
  *     [MATERIALIZE INDEX [IF EXISTS] index_name [IN PARTITION partition]]
  * ALTER LIVE VIEW [db.name]
  *     [REFRESH]
  */

class ParserAlterQuery : public IParserDialectBase
{
protected:
    const char * getName() const  override{ return "ALTER query"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};


class ParserAlterCommandList : public IParserDialectBase
{
protected:
    const char * getName() const  override{ return "a list of ALTER commands"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;

public:
    bool is_live_view;

    explicit ParserAlterCommandList(ParserSettingsImpl t, bool is_live_view_ = false) : IParserDialectBase(t), is_live_view(is_live_view_) {}
};


class ParserAlterCommand : public IParserDialectBase
{
protected:
    const char * getName() const  override{ return "ALTER command"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;

public:
    bool is_live_view;

    explicit ParserAlterCommand(ParserSettingsImpl t, bool is_live_view_ = false) : IParserDialectBase(t), is_live_view(is_live_view_) {}
};


}
