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
#include <Parsers/ASTIdentifier.h>
#include <Interpreters/DatabaseAndTableWithAlias.h>

namespace DB
{

struct QualifiedName
{
    std::vector<String> parts;

    QualifiedName() = default;
    explicit QualifiedName(std::vector<String> parts_) : parts(std::move(parts_)) {}
    QualifiedName(std::initializer_list<String> parts_) : parts(parts_) {}

    static QualifiedName extractQualifiedName(const ASTIdentifier & identifier);
    static QualifiedName extractQualifiedName(const DatabaseAndTableWithAlias & identifier);

    bool empty() const {return parts.empty();}
    const String & getLast() const {return parts.back();}
    QualifiedName getPrefix() const;
    bool hasSuffix(const QualifiedName & suffix) const;
    String toString() const;
};

}
