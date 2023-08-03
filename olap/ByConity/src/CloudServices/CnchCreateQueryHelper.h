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

#include <Core/Field.h>
#include <Interpreters/Context_fwd.h>
#include <Parsers/IAST_fwd.h>
#include <Storages/IStorage_fwd.h>

namespace DB
{

class ASTCreateQuery;
class ASTSetQuery;

/// see Databases/DatabaseOnDisk.h
extern String getObjectDefinitionFromCreateQuery(const ASTPtr & query, std::optional<bool> attach);

std::shared_ptr<ASTCreateQuery> getASTCreateQueryFromString(const String & query, const ContextPtr & context);
std::shared_ptr<ASTCreateQuery> getASTCreateQueryFromStorage(const IStorage & storage, const ContextPtr & context);

StoragePtr createStorageFromQuery(const String & query, ContextMutablePtr & context);

void replaceCnchWithCloud(ASTCreateQuery & create_query, const String & new_table_name, const String & cnch_db, const String & cnch_table);

void modifyOrAddSetting(ASTSetQuery & set_query, const String & name, Field value);
void modifyOrAddSetting(ASTCreateQuery & create_query, const String & name, Field value);

}
