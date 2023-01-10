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

#include <Core/Types.h>
#include <Parsers/IAST.h>

#include <memory>
#include <unordered_map>
#include <unordered_set>

namespace DB
{
class SymbolAllocator;
using SymbolAllocatorPtr = std::shared_ptr<SymbolAllocator>;

class SymbolAllocator
{
public:
    String newSymbol(String name_hint);
    String newSymbol(const ASTPtr & expression);

private:
    std::unordered_set<String> symbols;
    std::unordered_map<String, UInt32> next_ids;
};

}
