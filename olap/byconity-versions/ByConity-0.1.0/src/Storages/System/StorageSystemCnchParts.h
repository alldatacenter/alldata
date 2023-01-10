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

#include <common/shared_ptr_helper.h>
#include <Storages/System/IStorageSystemOneBlock.h>

#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTLiteral.h>
#include <vector>
#include <Core/Field.h>

namespace DB
{

class Context;

class StorageSystemCnchParts : public shared_ptr_helper<StorageSystemCnchParts>, public IStorageSystemOneBlock<StorageSystemCnchParts>
{
public:

    std::string getName() const override
    {
        return "SystemCnchParts";
    }

    static NamesAndTypesList getNamesAndTypes();

    static NamesAndAliases getNamesAndAliases();

    enum PartType
    {
        VisiblePart = 1,
        InvisiblePart = 2,
        DropRange = 3,
        DroppedPart = 4,
    };

protected:
    using IStorageSystemOneBlock::IStorageSystemOneBlock;
    void fillData(MutableColumns & res_columns, ContextPtr context, const SelectQueryInfo & query_info) const override;
};

}
