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

#include <Optimizer/SymbolUtils.h>

namespace DB
{
bool SymbolUtils::contains(std::vector<String> & symbols, String symbol)
{
    if (std::find(symbols.begin(), symbols.end(), symbol) != symbols.end())
    {
        return true;
    }
    return false;
}

bool SymbolUtils::containsAll(std::set<String> & left_symbols, std::set<String> & right_symbols)
{
    for (auto & symbol : right_symbols)
    {
        if (!left_symbols.contains(symbol))
        {
            return false;
        }
    }
    return true;
}

bool SymbolUtils::containsAll(std::vector<String> & left_symbols, std::set<String> & right_symbols)
{
    for (auto & symbol : right_symbols)
    {
        if (std::find(left_symbols.begin(), left_symbols.end(), symbol) == left_symbols.end())
        {
            return false;
        }
    }
    return true;
}

}
