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

#include <Parsers/IAST.h>
#include <Parsers/ASTQueryWithOnCluster.h>
#include <Access/RowPolicy.h>
#include <optional>


namespace DB
{
class ASTRowPolicyNames;
class ASTRolesOrUsersSet;

/** CREATE [ROW] POLICY [IF NOT EXISTS | OR REPLACE] name ON [database.]table
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
class ASTCreateRowPolicyQuery : public IAST, public ASTQueryWithOnCluster
{
public:
    bool alter = false;
    bool attach = false;

    bool if_exists = false;
    bool if_not_exists = false;
    bool or_replace = false;

    std::shared_ptr<ASTRowPolicyNames> names;
    String new_short_name;

    std::optional<bool> is_restrictive;
    std::vector<std::pair<RowPolicy::ConditionType, ASTPtr>> conditions; /// `nullptr` means set to NONE.

    std::shared_ptr<ASTRolesOrUsersSet> roles;

    String getID(char) const override;

    ASTType getType() const override { return ASTType::ASTCreateRowPolicyQuery; }

    ASTPtr clone() const override;
    void formatImpl(const FormatSettings & settings, FormatState &, FormatStateStacked) const override;
    ASTPtr getRewrittenASTWithoutOnCluster(const std::string &) const override { return removeOnCluster<ASTCreateRowPolicyQuery>(clone()); }

    void replaceCurrentUserTag(const String & current_user_name) const;
    void replaceEmptyDatabase(const String & current_database) const;
};
}
