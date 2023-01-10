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
#include <Parsers/Lexer.h>


namespace DB
{

/** List of expressions, for example "a, b + c, f(d)"
  */
class ASTExpressionList : public IAST
{
public:
    explicit ASTExpressionList(char separator_ = ',') : separator(separator_) {}
    String getID(char) const override { return "ExpressionList"; }

    ASTType getType() const override { return ASTType::ASTExpressionList; }

    ASTPtr clone() const override;
    void formatImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const override;
    void formatImplMultiline(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const;

    char separator;

    void serialize(WriteBuffer & buf) const override;
    void deserializeImpl(ReadBuffer & buf) override;
    static ASTPtr deserialize(ReadBuffer & buf);
};

}
