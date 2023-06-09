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

#include <Parsers/ParserDictionaryAttributeDeclaration.h>

#include <Parsers/ExpressionElementParsers.h>
#include <Parsers/ExpressionListParsers.h>
#include <Parsers/ParserDataType.h>

namespace DB
{

bool ParserDictionaryAttributeDeclaration::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    ParserIdentifier name_parser;
    ParserDataType type_parser;
    ParserKeyword s_default{"DEFAULT"};
    ParserKeyword s_expression{"EXPRESSION"};
    ParserKeyword s_hierarchical{"HIERARCHICAL"};
    ParserKeyword s_injective{"INJECTIVE"};
    ParserKeyword s_is_object_id{"IS_OBJECT_ID"};
    ParserLiteral default_parser(dt);
    ParserArrayOfLiterals array_literals_parser(dt);
    ParserTernaryOperatorExpression expression_parser(dt);

    /// mandatory attribute name
    ASTPtr name;
    if (!name_parser.parse(pos, name, expected))
        return false;

    ASTPtr type;
    ASTPtr default_value;
    ASTPtr expression;
    bool hierarchical = false;
    bool injective = false;
    bool is_object_id = false;

    /// attribute name should be followed by type name if it
    if (!type_parser.parse(pos, type, expected))
        return false;

    /// loop to avoid strict order of attribute properties
    while (true)
    {
        if (!default_value && s_default.ignore(pos, expected))
        {
            if (!default_parser.parse(pos, default_value, expected) &&
                !array_literals_parser.parse(pos, default_value, expected))
                return false;

            continue;
        }

        if (!expression && s_expression.ignore(pos, expected))
        {
            if (!expression_parser.parse(pos, expression, expected))
                return false;
            continue;
        }

        /// just single keyword, we don't use "true" or "1" for value
        if (!hierarchical && s_hierarchical.ignore(pos, expected))
        {
            hierarchical = true;
            continue;
        }

        if (!injective && s_injective.ignore(pos, expected))
        {
            injective = true;
            continue;
        }

        if (!is_object_id && s_is_object_id.ignore(pos, expected))
        {
            is_object_id = true;
            continue;
        }

        break;
    }

    auto attribute_declaration = std::make_shared<ASTDictionaryAttributeDeclaration>();
    node = attribute_declaration;
    tryGetIdentifierNameInto(name, attribute_declaration->name);

    if (type)
    {
        attribute_declaration->type = type;
        attribute_declaration->children.push_back(std::move(type));
    }

    if (default_value)
    {
        attribute_declaration->default_value = default_value;
        attribute_declaration->children.push_back(std::move(default_value));
    }

    if (expression)
    {
        attribute_declaration->expression = expression;
        attribute_declaration->children.push_back(std::move(expression));
    }

    attribute_declaration->hierarchical = hierarchical;
    attribute_declaration->injective = injective;
    attribute_declaration->is_object_id = is_object_id;

    return true;
}


bool ParserDictionaryAttributeDeclarationList::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    return ParserList(std::make_unique<ParserDictionaryAttributeDeclaration>(dt),
        std::make_unique<ParserToken>(TokenType::Comma), false)
        .parse(pos, node, expected);
}

}
