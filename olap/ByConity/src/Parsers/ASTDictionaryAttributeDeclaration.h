/*
 * Copyright 2016-2023 ClickHouse, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#pragma once

#include <Parsers/IAST.h>
#include <Parsers/ASTExpressionList.h>

namespace DB
{

/// AST for single dictionary attribute in dictionary DDL query
class ASTDictionaryAttributeDeclaration : public IAST
{
public:
    /// Attribute name
    String name;
    /// Attribute type
    ASTPtr type;
    /// Attribute default value
    ASTPtr default_value;
    /// Attribute expression
    ASTPtr expression;
    /// Is attribute mirrored to the parent identifier
    bool hierarchical;
    /// Flag that shows whether the id->attribute image is injective
    bool injective;
    /// MongoDB object ID
    bool is_object_id;

    String getID(char delim) const override { return "DictionaryAttributeDeclaration" + (delim + name); }

    ASTType getType() const override { return ASTType::ASTDictionaryAttributeDeclaration; }

    ASTPtr clone() const override;
    void formatImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const override;
};

}
