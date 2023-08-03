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
#include <Access/Quota.h>


namespace DB
{
class ASTRolesOrUsersSet;


/** CREATE QUOTA [IF NOT EXISTS | OR REPLACE] name
  *      [KEYED BY {none | user_name | ip_address | client_key | client_key, user_name | client_key, ip_address} | NOT KEYED]
  *      [FOR [RANDOMIZED] INTERVAL number {second | minute | hour | day | week | month | quarter | year}
  *       {MAX {{queries | errors | result_rows | result_bytes | read_rows | read_bytes | execution_time} = number} [,...] |
  *        NO LIMITS | TRACKING ONLY} [,...]]
  *      [TO {role [,...] | ALL | ALL EXCEPT role [,...]}]
  *
  * ALTER QUOTA [IF EXISTS] name
  *      [RENAME TO new_name]
  *      [KEYED BY {none | user_name | ip_address | client_key | client_key, user_name | client_key, ip_address} | NOT KEYED]
  *      [FOR [RANDOMIZED] INTERVAL number {second | minute | hour | day | week | month | quarter | year}
  *       {MAX {{queries | errors | result_rows | result_bytes | read_rows | read_bytes | execution_time} = number} [,...] |
  *        NO LIMITS | TRACKING ONLY} [,...]]
  *      [TO {role [,...] | ALL | ALL EXCEPT role [,...]}]
  */
class ASTCreateQuotaQuery : public IAST, public ASTQueryWithOnCluster
{
public:
    bool alter = false;
    bool attach = false;

    bool if_exists = false;
    bool if_not_exists = false;
    bool or_replace = false;

    using KeyType = Quota::KeyType;
    using ResourceAmount = Quota::ResourceAmount;

    Strings names;
    String new_name;
    std::optional<KeyType> key_type;

    struct Limits
    {
        std::optional<ResourceAmount> max[Quota::MAX_RESOURCE_TYPE];
        bool drop = false;
        std::chrono::seconds duration = std::chrono::seconds::zero();
        bool randomize_interval = false;
    };
    std::vector<Limits> all_limits;

    std::shared_ptr<ASTRolesOrUsersSet> roles;

    String getID(char) const override;

    ASTType getType() const override { return ASTType::ASTCreateQuotaQuery; }

    ASTPtr clone() const override;
    void formatImpl(const FormatSettings & settings, FormatState &, FormatStateStacked) const override;
    void replaceCurrentUserTag(const String & current_user_name) const;
    ASTPtr getRewrittenASTWithoutOnCluster(const std::string &) const override { return removeOnCluster<ASTCreateQuotaQuery>(clone()); }
};
}
