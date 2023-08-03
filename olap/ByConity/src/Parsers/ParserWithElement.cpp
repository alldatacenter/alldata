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

#include <Parsers/ASTExpressionList.h>
#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTSubquery.h>
#include <Parsers/ASTWithElement.h>
#include <Parsers/CommonParsers.h>
#include <Parsers/ExpressionElementParsers.h>
#include <Parsers/ExpressionListParsers.h>
#include <Parsers/ParserWithElement.h>


namespace DB
{
bool ParserWithElement::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    ParserIdentifier s_ident;
    ParserKeyword s_as("AS");
    ParserSubquery s_subquery(dt);

    auto old_pos = pos;
    if (ASTPtr name, subquery;
        s_ident.parse(pos, name, expected) && s_as.ignore(pos, expected) && s_subquery.parse(pos, subquery, expected))
    {
        auto with_element = std::make_shared<ASTWithElement>();
        tryGetIdentifierNameInto(name, with_element->name);
        with_element->subquery = subquery;
        with_element->children.push_back(with_element->subquery);
        node = with_element;
        return true;
    }
    else if (dt.parse_with_alias)
    {
        pos = old_pos;
        ParserExpressionWithOptionalAlias s_expr(false, dt, false);
        if (s_expr.parse(pos, node, expected))
            return true;
    }
    return false;

}


}
