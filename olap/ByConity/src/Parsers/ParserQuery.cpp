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

#include <Parsers/ParserAlterQuery.h>
#include <Parsers/ParserAlterWarehouseQuery.h>
#include <Parsers/ParserCreateQuery.h>
#include <Parsers/ParserCreateQuotaQuery.h>
#include <Parsers/ParserCreateRoleQuery.h>
#include <Parsers/ParserCreateRowPolicyQuery.h>
#include <Parsers/ParserCreateSettingsProfileQuery.h>
#include <Parsers/ParserCreateUserQuery.h>
#include <Parsers/ParserCreateWarehouseQuery.h>
#include <Parsers/ParserCreateWorkerGroupQuery.h>
#include <Parsers/ParserDropAccessEntityQuery.h>
#include <Parsers/ParserDropQuery.h>
#include <Parsers/ParserDropWarehouseQuery.h>
#include <Parsers/ParserDropWorkerGroupQuery.h>
#include <Parsers/ParserGrantQuery.h>
#include <Parsers/ParserInsertQuery.h>
#include <Parsers/ParserOptimizeQuery.h>
#include <Parsers/ParserQuery.h>
#include <Parsers/ParserQueryWithOutput.h>
#include <Parsers/ParserRenameQuery.h>
#include <Parsers/ParserSetQuery.h>
#include <Parsers/ParserSetRoleQuery.h>
#include <Parsers/ParserShowWarehousesQuery.h>
#include <Parsers/ParserSystemQuery.h>
#include <Parsers/ParserUseQuery.h>
#include <Parsers/ParserExternalDDLQuery.h>


namespace DB
{


bool ParserQuery::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    ParserQueryWithOutput query_with_output_p(end, dt);
    ParserInsertQuery insert_p(end, dt);
    ParserUseQuery use_p;
    ParserSetQuery set_p(false);
    ParserSystemQuery system_p(dt);
    ParserCreateUserQuery create_user_p;
    ParserCreateRoleQuery create_role_p;
    ParserCreateQuotaQuery create_quota_p;
    ParserCreateRowPolicyQuery create_row_policy_p(dt);
    ParserCreateSettingsProfileQuery create_settings_profile_p;
    ParserDropAccessEntityQuery drop_access_entity_p;
    ParserGrantQuery grant_p;
    ParserSetRoleQuery set_role_p;
    ParserExternalDDLQuery external_ddl_p(dt);
    ParserAlterWarehouseQuery alter_warehouse_p;
    ParserCreateWarehouseQuery create_warehouse_p;
    ParserDropWarehouseQuery drop_warehouse_p;
    ParserShowWarehousesQuery show_warehouse_p;
    ParserCreateWorkerGroupQuery create_worker_group_p;
    ParserDropWorkerGroupQuery drop_worker_group_p;

    bool res = query_with_output_p.parse(pos, node, expected)
        || insert_p.parse(pos, node, expected)
        || use_p.parse(pos, node, expected)
        || set_role_p.parse(pos, node, expected)
        || set_p.parse(pos, node, expected)
        || system_p.parse(pos, node, expected)
        || create_user_p.parse(pos, node, expected)
        || create_role_p.parse(pos, node, expected)
        || create_quota_p.parse(pos, node, expected)
        || create_row_policy_p.parse(pos, node, expected)
        || create_settings_profile_p.parse(pos, node, expected)
        || drop_access_entity_p.parse(pos, node, expected)
        || grant_p.parse(pos, node, expected)
        || external_ddl_p.parse(pos, node, expected)
        || create_warehouse_p.parse(pos, node, expected)
        || alter_warehouse_p.parse(pos, node, expected)
        || drop_warehouse_p.parse(pos, node, expected)
        || show_warehouse_p.parse(pos, node, expected)
        || create_worker_group_p.parse(pos, node, expected)
        || drop_worker_group_p.parse(pos, node, expected);

    return res;
}

}
