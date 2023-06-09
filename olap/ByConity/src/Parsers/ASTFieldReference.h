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
#include <Parsers/ASTWithAlias.h>

namespace DB
{

/// this AST is only used by optimizer.
class ASTFieldReference : public ASTWithAlias
{
public:
    size_t field_index;

    explicit ASTFieldReference(size_t field_index_): field_index(field_index_)
    {}

    String getID(char delim) const override { return "FieldRef" + (delim + std::to_string(field_index)); }

    ASTType getType() const override { return ASTType::ASTFieldReference; }

    ASTPtr clone() const override;

protected:
    void formatImplWithoutAlias(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const override;
    void appendColumnNameImpl(WriteBuffer & ostr) const override;
};

}
