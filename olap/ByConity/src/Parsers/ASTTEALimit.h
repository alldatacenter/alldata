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
#include <Core/Field.h>

namespace DB
{
/**
 * TEALIMIT N GROUP (g_0, ... , g_n) ORDER EXPR(cnt_0, ... cnt_n) ASC|DESC
 */
class ASTTEALimit : public IAST
{
public:
    ASTPtr limit_value;            // ASTLiteral
    ASTPtr limit_offset;            // ASTLiteral
    ASTPtr group_expr_list;  // ASTExpressionList
    ASTPtr order_expr_list;  // ASTOrderByElement

    ASTType getType() const override { return ASTType::ASTTEALimit; }

    String getID(char) const override;
    ASTPtr clone() const override;

protected:
    void formatImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const override;
};

}
