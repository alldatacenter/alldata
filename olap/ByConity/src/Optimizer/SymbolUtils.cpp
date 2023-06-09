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

#include <algorithm>
#include <Optimizer/SymbolUtils.h>

namespace DB
{
bool SymbolUtils::contains(std::vector<String> & symbols, String symbol)
{
    return std::find(symbols.begin(), symbols.end(), symbol) != symbols.end();
}

bool SymbolUtils::containsAll(std::set<String> & left_symbols, std::set<String> & right_symbols)
{
    return std::includes(left_symbols.begin(), left_symbols.end(), right_symbols.begin(), right_symbols.end());
}

bool SymbolUtils::containsAll(std::vector<String> & left_symbols, std::set<String> & right_symbols)
{
    return std::all_of(right_symbols.begin(), right_symbols.end(), [&left_symbols](const String & symbol) {
        return std::find(left_symbols.begin(), left_symbols.end(), symbol) != left_symbols.end();
    });
}

}
