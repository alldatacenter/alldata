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

namespace DB
{

class ASTFunction;

/** name BY expr TYPE typename(args) GRANULARITY int in create query
  */
class ASTIndexDeclaration : public IAST
{
public:
    String name;
    IAST * expr;
    ASTFunction * type;
    UInt64 granularity;

    /** Get the text that identifies this element. */
    String getID(char) const override { return "Index"; }

    ASTType getType() const override { return ASTType::ASTIndexDeclaration; }

    ASTPtr clone() const override;
    void formatImpl(const FormatSettings & s, FormatState & state, FormatStateStacked frame) const override;
};

}
