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

#include <Core/Names.h>
#include <Parsers/IAST.h>


namespace DB
{
/** PROJECTION SELECT query
  */
class ASTProjectionSelectQuery : public IAST
{
public:
    enum class Expression : uint8_t
    {
        WITH,
        SELECT,
        GROUP_BY,
        ORDER_BY,
    };

    /** Get the text that identifies this element. */
    String getID(char) const override { return "ProjectionSelectQuery"; }

    ASTType getType() const override { return ASTType::ASTProjectionSelectQuery; }

    ASTPtr clone() const override;

    ASTPtr & refSelect() { return getExpression(Expression::SELECT); }

    const ASTPtr with() const { return getExpression(Expression::WITH); }
    const ASTPtr select() const { return getExpression(Expression::SELECT); }
    const ASTPtr groupBy() const { return getExpression(Expression::GROUP_BY); }
    const ASTPtr orderBy() const { return getExpression(Expression::ORDER_BY); }

    /// Set/Reset/Remove expression.
    void setExpression(Expression expr, ASTPtr && ast);

    ASTPtr getExpression(Expression expr, bool clone = false) const
    {
        auto it = positions.find(expr);
        if (it != positions.end())
            return clone ? children[it->second]->clone() : children[it->second];
        return {};
    }

    ASTPtr cloneToASTSelect() const;

protected:
    void formatImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const override;

private:
    std::unordered_map<Expression, size_t> positions;

    ASTPtr & getExpression(Expression expr);
};

}
