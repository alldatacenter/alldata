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

/// SHOW USERS
/// SHOW [CURRENT|ENABLED] ROLES
/// SHOW [SETTINGS] PROFILES
/// SHOW [ROW] POLICIES [name | ON [database.]table]
/// SHOW QUOTAS
/// SHOW [CURRENT] QUOTA
class ASTShowAccessEntitiesQuery : public ASTQueryWithOutput
{
public:
    using EntityType = IAccessEntity::Type;

    EntityType type;

    bool all = false;
    bool current_quota = false;
    bool current_roles = false;
    bool enabled_roles = false;

    String short_name;
    std::optional<std::pair<String, String>> database_and_table_name;

    String getID(char) const override;

    ASTType getType() const override { return ASTType::ASTShowAccessEntitiesQuery; }

    ASTPtr clone() const override { return std::make_shared<ASTShowAccessEntitiesQuery>(*this); }

    void replaceEmptyDatabase(const String & current_database);

protected:
    void formatQueryImpl(const FormatSettings & settings, FormatState &, FormatStateStacked) const override;

private:
    String getKeyword() const;
};

}
