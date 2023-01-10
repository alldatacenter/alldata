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

#include <Parsers/ASTQueryWithOutput.h>
#include <Access/IAccessEntity.h>


namespace DB
{
class ASTRowPolicyNames;

using Strings = std::vector<String>;

/** SHOW CREATE USER [name | CURRENT_USER]
  * SHOW CREATE USERS [name [, name2 ...]
  * SHOW CREATE ROLE name
  * SHOW CREATE ROLES [name [, name2 ...]]
  * SHOW CREATE [SETTINGS] PROFILE name
  * SHOW CREATE [SETTINGS] PROFILES [name [, name2 ...]]
  * SHOW CREATE [ROW] POLICY name ON [database.]table
  * SHOW CREATE [ROW] POLICIES [name ON [database.]table [, name2 ON database2.table2 ...] | name | ON database.table]
  * SHOW CREATE QUOTA [name]
  * SHOW CREATE QUOTAS [name [, name2 ...]]
  */
class ASTShowCreateAccessEntityQuery : public ASTQueryWithOutput
{
public:
    using EntityType = IAccessEntity::Type;

    EntityType type;
    Strings names;
    std::shared_ptr<ASTRowPolicyNames> row_policy_names;

    bool current_quota = false;
    bool current_user = false;
    bool all = false;

    String short_name;
    std::optional<std::pair<String, String>> database_and_table_name;

    String getID(char) const override;

    ASTType getType() const override { return ASTType::ASTShowCreateAccessEntityQuery; }

    ASTPtr clone() const override;

    void replaceEmptyDatabase(const String & current_database);

protected:
    String getKeyword() const;
    void formatQueryImpl(const FormatSettings & settings, FormatState &, FormatStateStacked) const override;
};

}
