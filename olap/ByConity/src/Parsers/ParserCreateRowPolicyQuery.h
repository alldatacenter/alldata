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
/** Parses queries like
  * CREATE [ROW] POLICY [IF NOT EXISTS | OR REPLACE] name ON [database.]table
  *      [AS {permissive | restrictive}]
  *      [FOR {SELECT | INSERT | UPDATE | DELETE | ALL}]
  *      [USING condition]
  *      [WITH CHECK condition] [,...]
  *      [TO {role [,...] | ALL | ALL EXCEPT role [,...]}]
  *
  * ALTER [ROW] POLICY [IF EXISTS] name ON [database.]table
  *      [RENAME TO new_name]
  *      [AS {permissive | restrictive}]
  *      [FOR {SELECT | INSERT | UPDATE | DELETE | ALL}]
  *      [USING {condition | NONE}]
  *      [WITH CHECK {condition | NONE}] [,...]
  *      [TO {role [,...] | ALL | ALL EXCEPT role [,...]}]
  */
class ParserCreateRowPolicyQuery : public IParserDialectBase
{
public:
    void useAttachMode(bool attach_mode_ = true) { attach_mode = attach_mode_; }
    using IParserDialectBase::IParserDialectBase;

protected:
    const char * getName() const override { return "CREATE ROW POLICY or ALTER ROW POLICY query"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;

private:
    bool attach_mode = false;
};
}
