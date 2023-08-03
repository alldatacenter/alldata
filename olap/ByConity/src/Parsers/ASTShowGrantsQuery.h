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


namespace DB
{
class ASTRolesOrUsersSet;

/** SHOW GRANTS [FOR user_name]
  */
class ASTShowGrantsQuery : public ASTQueryWithOutput
{
public:
    std::shared_ptr<ASTRolesOrUsersSet> for_roles;

    String getID(char) const override;

    ASTType getType() const override { return ASTType::ASTShowGrantsQuery; }

    ASTPtr clone() const override;
    void formatQueryImpl(const FormatSettings & settings, FormatState &, FormatStateStacked) const override;
};
}
