/*
 * Copyright (2022) Bytedance Ltd. and/or its affiliates
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#pragma once
#include <Interpreters/Context.h>
namespace DB
{

bool createDatabaseInCatalog(
    const ContextPtr & global_context,
    const String & database_name,
    Poco::Logger * logger);

/// Detects change in table schema. Does not support modification of primary/partition keys
String makeAlterColumnQuery(
    const String & database,
    const String & table,
    const Block & expected,
    const Block & actual);

bool createCnchTable(
    ContextPtr global_context,
    const String & database,
    const String & table,
    ASTPtr & create_query_ast,
    Poco::Logger * logger);

bool prepareCnchTable(
    ContextPtr global_context,
    const String & database,
    const String & table,
    ASTPtr & create_query_ast,
    Poco::Logger * logger);

bool syncTableSchema(
    ContextPtr global_context,
    const String & database,
    const String & table,
    const Block & expected_block,
    Poco::Logger * logger);

bool createView(
    ContextPtr global_context,
    const String & database,
    const String & table,
    Poco::Logger * logger);

}/// end namespace
