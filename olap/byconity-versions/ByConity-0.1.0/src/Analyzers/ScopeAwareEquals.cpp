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

#include <Analyzers/ScopeAwareEquals.h>

namespace DB::ASTEquality
{

std::optional<bool> ScopeAwareEquals::equals(const ASTPtr & left, const ASTPtr & right) const
{
    auto left_col_ref = analysis->tryGetColumnReference(left);
    auto right_col_ref = analysis->tryGetColumnReference(right);

    if (left_col_ref || right_col_ref)
    {
        if (left_col_ref && right_col_ref)
        {
            return left_col_ref->scope == right_col_ref->scope && left_col_ref->local_index == right_col_ref->local_index;
        }

        return false;
    }

    return std::nullopt;
}

std::optional<size_t> ScopeAwareHash::hash(const ASTPtr & ast) const
{
    if (auto col_ref = analysis->tryGetColumnReference(ast))
    {
        auto t = static_cast<size_t>(reinterpret_cast<std::uintptr_t>(col_ref->scope));
        return t * 31 + col_ref->local_index;
    }

    return std::nullopt;
}

}
