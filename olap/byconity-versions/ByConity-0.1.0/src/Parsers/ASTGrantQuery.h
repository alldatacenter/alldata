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
#include <Access/AccessRightsElement.h>
#include <Parsers/ASTQueryWithOnCluster.h>


namespace DB
{
class ASTRolesOrUsersSet;


/** GRANT access_type[(column_name [,...])] [,...] ON {db.table|db.*|*.*|table|*} TO {user_name | CURRENT_USER} [,...] [WITH GRANT OPTION]
  * REVOKE access_type[(column_name [,...])] [,...] ON {db.table|db.*|*.*|table|*} FROM {user_name | CURRENT_USER} [,...] | ALL | ALL EXCEPT {user_name | CURRENT_USER} [,...]
  *
  * GRANT role [,...] TO {user_name | role_name | CURRENT_USER} [,...] [WITH ADMIN OPTION]
  * REVOKE [ADMIN OPTION FOR] role [,...] FROM {user_name | role_name | CURRENT_USER} [,...] | ALL | ALL EXCEPT {user_name | role_name | CURRENT_USER} [,...]
  */
class ASTGrantQuery : public IAST, public ASTQueryWithOnCluster
{
public:
    bool attach_mode = false;
    bool is_revoke = false;
    AccessRightsElements access_rights_elements;
    std::shared_ptr<ASTRolesOrUsersSet> roles;
    bool admin_option = false;
    std::shared_ptr<ASTRolesOrUsersSet> grantees;

    String getID(char) const override;

    ASTType getType() const override { return ASTType::ASTGrantQuery; }

    ASTPtr clone() const override;
    void formatImpl(const FormatSettings & settings, FormatState &, FormatStateStacked) const override;
    void replaceEmptyDatabase(const String & current_database);
    void replaceCurrentUserTag(const String & current_user_name) const;
    ASTPtr getRewrittenASTWithoutOnCluster(const std::string &) const override { return removeOnCluster<ASTGrantQuery>(clone()); }
};
}
