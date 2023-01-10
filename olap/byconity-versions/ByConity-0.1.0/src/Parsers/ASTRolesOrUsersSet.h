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

using Strings = std::vector<String>;

/// Represents a set of users/roles like
/// {user_name | role_name | CURRENT_USER | ALL | NONE} [,...]
/// [EXCEPT {user_name | role_name | CURRENT_USER | ALL | NONE} [,...]]
class ASTRolesOrUsersSet : public IAST
{
public:
    bool all = false;
    Strings names;
    bool current_user = false;
    Strings except_names;
    bool except_current_user = false;

    bool allow_users = true;      /// whether this set can contain names of users
    bool allow_roles = true;      /// whether this set can contain names of roles
    bool id_mode = false;         /// whether this set keep UUIDs instead of names
    bool use_keyword_any = false; /// whether the keyword ANY should be used instead of the keyword ALL

    bool empty() const { return names.empty() && !current_user && !all; }
    void replaceCurrentUserTag(const String & current_user_name);

    String getID(char) const override { return "RolesOrUsersSet"; }

    ASTType getType() const override { return ASTType::ASTRolesOrUsersSet; }

    ASTPtr clone() const override { return std::make_shared<ASTRolesOrUsersSet>(*this); }
    void formatImpl(const FormatSettings & settings, FormatState &, FormatStateStacked) const override;
};
}
