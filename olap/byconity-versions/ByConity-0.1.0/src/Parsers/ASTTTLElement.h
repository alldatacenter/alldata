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
#include <Storages/DataDestinationType.h>
#include <Storages/TTLMode.h>


namespace DB
{

/** Element of TTL expression.
  */
class ASTTTLElement : public IAST
{
public:
    TTLMode mode;
    DataDestinationType destination_type;
    String destination_name;

    ASTs group_by_key;
    ASTs group_by_assignments;

    ASTPtr recompression_codec;

    ASTTTLElement(TTLMode mode_, DataDestinationType destination_type_, const String & destination_name_)
        : mode(mode_)
        , destination_type(destination_type_)
        , destination_name(destination_name_)
        , ttl_expr_pos(-1)
        , where_expr_pos(-1)
    {
    }

    String getID(char) const override { return "TTLElement"; }

    ASTType getType() const override { return ASTType::ASTTTLElement; }

    ASTPtr clone() const override;

    const ASTPtr ttl() const { return getExpression(ttl_expr_pos); }
    const ASTPtr where() const { return getExpression(where_expr_pos); }

    void setTTL(ASTPtr && ast) { setExpression(ttl_expr_pos, std::forward<ASTPtr>(ast)); }
    void setWhere(ASTPtr && ast) { setExpression(where_expr_pos, std::forward<ASTPtr>(ast)); }

protected:
    void formatImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const override;

private:
    int ttl_expr_pos;
    int where_expr_pos;

private:
    void setExpression(int & pos, ASTPtr && ast);
    ASTPtr getExpression(int pos, bool clone = false) const;
};

}
