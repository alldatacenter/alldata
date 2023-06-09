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

#include <QueryPlan/SymbolAllocator.h>

#include <Poco/NumberParser.h>

namespace DB
{

String SymbolAllocator::newSymbol(const ASTPtr & expression)
{
    return newSymbol("expr#" + expression->getColumnName());
}

String SymbolAllocator::newSymbol(String name_hint)
{
    // extract name prefix if name_hint is from a symbol
    {
        auto index = name_hint.rfind('_');
        int ignored;

        if (index != String::npos && Poco::NumberParser::tryParse(name_hint.substr(index + 1), ignored))
        {
            name_hint = name_hint.substr(0, index);
        }
    }

    if (next_ids.find(name_hint) == next_ids.end())
        next_ids[name_hint] = 1;

    String name = name_hint;

    while (symbols.find(name) != symbols.end())
    {
        name = name_hint + '_' + std::to_string(next_ids[name_hint]++);
    }

    symbols.insert(name);
    return name;
}

}
