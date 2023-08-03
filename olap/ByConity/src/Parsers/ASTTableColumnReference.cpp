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

#include <Parsers/ASTTableColumnReference.h>

#include <Storages/IStorage.h>
#include <IO/WriteHelpers.h>

namespace DB
{

static inline String formatStorageName(const StoragePtr & storage, const String & column_name, char delim = '.')
{
    return storage->getStorageID().getFullTableName() + delim + std::to_string(reinterpret_cast<size_t>(storage.get()))
        + delim + column_name;
}

String ASTTableColumnReference::getID(char delim) const
{
    return std::string("TableColumnRef") + delim + formatStorageName(storage, column_name, delim);
}

void ASTTableColumnReference::appendColumnName(WriteBuffer & buffer) const
{
    writeString(getID('.'), buffer);
}
}
