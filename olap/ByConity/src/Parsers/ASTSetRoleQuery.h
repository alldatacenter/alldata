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


namespace DB
{
class ASTRolesOrUsersSet;

/** SET ROLE {DEFAULT | NONE | role [,...] | ALL | ALL EXCEPT role [,...]}
  * SET DEFAULT ROLE {NONE | role [,...] | ALL | ALL EXCEPT role [,...]} TO {user|CURRENT_USER} [,...]
  */
class ASTSetRoleQuery : public IAST
{
public:
    enum class Kind
    {
        SET_ROLE,
        SET_ROLE_DEFAULT,
        SET_DEFAULT_ROLE,
    };
    Kind kind = Kind::SET_ROLE;

    std::shared_ptr<ASTRolesOrUsersSet> roles;
    std::shared_ptr<ASTRolesOrUsersSet> to_users;

    String getID(char) const override;

    ASTType getType() const override { return ASTType::ASTSetRoleQuery; }

    ASTPtr clone() const override;
    void formatImpl(const FormatSettings & settings, FormatState &, FormatStateStacked) const override;
};
}
