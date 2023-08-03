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

#include <Parsers/IAST.h>

#include <functional>
#include <optional>
#include <unordered_set>
#include <unordered_map>

namespace DB
{

namespace ASTEquality
{

    using SubtreeComparator = std::function<std::optional<bool>(const ASTPtr &, const ASTPtr &)>;
    using SubtreeHasher = std::function<std::optional<size_t>(const ASTPtr &)>;

    // compare ASTs by extra comparison strategy first, if not applicable, compare AST by syntax
    bool compareTree(const ASTPtr & left, const ASTPtr & right, const SubtreeComparator & comparator);

    inline bool compareTree(const ASTPtr & left, const ASTPtr & right)
    {
        return compareTree(left, right, [](auto &, auto &) {return std::nullopt;});
    }

    inline bool compareTree(const ConstASTPtr & left, const ConstASTPtr & right)
    {
        return compareTree(std::const_pointer_cast<IAST>(left), std::const_pointer_cast<IAST>(right));
    }

    size_t hashTree(const ASTPtr & ast, const SubtreeHasher & hasher);

    inline size_t hashTree(const ASTPtr & ast)
    {
        return hashTree(ast, [](auto &) {return std::nullopt;});
    }

    // Equals & hash for syntactic comparison
    struct ASTEquals
    {
        bool operator()(const ASTPtr & left, const ASTPtr & right) const
        {
            return compareTree(left, right);
        }

        bool operator()(const ConstASTPtr & left, const ConstASTPtr & right) const
        {
            return compareTree(left, right);
        }
    };

    struct ASTHash
    {
        size_t operator()(const ASTPtr & ast) const
        {
            return hashTree(ast);
        }

        size_t operator()(const ConstASTPtr & ast) const
        {
            return hashTree(std::const_pointer_cast<IAST>(ast));
        }
    };
}

template <typename ASTType = ASTPtr>
using ASTSet = std::unordered_set<ASTType, ASTEquality::ASTHash, ASTEquality::ASTEquals>;

template <typename T, typename ASTType = ASTPtr>
using ASTMap = std::unordered_map<ASTType, T, ASTEquality::ASTHash, ASTEquality::ASTEquals>;

}
