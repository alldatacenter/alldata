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

#include <Parsers/ExpressionListParsers.h>

#include <Parsers/ASTExpressionList.h>
#include <Parsers/ASTFunction.h>
#include <Parsers/ASTFunctionWithKeyValueArguments.h>
#include <Parsers/ParserCreateQuery.h>
#include <Parsers/parseIntervalKind.h>
#include <Parsers/ParserUnionQueryElement.h>
#include <Parsers/ASTQuantifiedComparison.h>
#include <Common/StringUtils/StringUtils.h>


namespace DB
{


const char * ParserMultiplicativeExpression::operators[] =
{
    "*",     "multiply",
    "/",     "divide",
    "%",     "modulo",
    "MOD",   "modulo",
    "DIV",   "intDiv",
    nullptr
};

const char * ParserUnaryExpression::operators[] =
{
    "-",     "negate",
    "NOT",   "not",
    nullptr
};

const char * ParserAdditiveExpression::operators[] =
{
    "+",     "plus",
    "-",     "minus",
    nullptr
};

const char * ParserComparisonExpression::operators[] =
{
    "==",            "equals",
    "!=",            "notEquals",
    "<>",            "notEquals",
    "<=",            "lessOrEquals",
    ">=",            "greaterOrEquals",
    "<",             "less",
    ">",             "greater",
    "=",             "equals",
    "LIKE",          "like",
    "ILIKE",         "ilike",
    "NOT LIKE",      "notLike",
    "NOT ILIKE",     "notILike",
    "IN",            "in",
    "NOT IN",        "notIn",
    "GLOBAL IN",     "globalIn",
    "GLOBAL NOT IN", "globalNotIn",
    nullptr
};

const char * ParserComparisonExpression::overlapping_operators_to_skip[] =
{
    "IN PARTITION",
    nullptr
};

const char * ParserLogicalNotExpression::operators[] =
{
    "NOT", "not",
    nullptr
};

const char * ParserArrayElementExpression::operators[] =
{
    "[", "arrayElement",
    nullptr
};

const char * ParserTupleElementExpression::operators[] =
{
    ".", "tupleElement",
    nullptr
};

const char * ParserMapElementExpression::operators[] =
{
    "{", "mapElement",
    nullptr
};

bool ParserList::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    ASTs elements;

    auto parse_element = [&]
    {
        ASTPtr element;
        if (!elem_parser->parse(pos, element, expected))
            return false;

        elements.push_back(element);
        return true;
    };

    if (!parseUtil(pos, expected, parse_element, *separator_parser, allow_empty))
        return false;

    auto list = std::make_shared<ASTExpressionList>(result_separator);
    list->children = std::move(elements);
    node = list;

    return true;
}

bool ParserUnionList::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    ParserUnionQueryElement elem_parser(dt);
    ParserKeyword s_union_parser("UNION");
    ParserKeyword s_all_parser("ALL");
    ParserKeyword s_distinct_parser("DISTINCT");
    ParserKeyword s_except_parser("EXCEPT");
    ParserKeyword s_intersect_parser("INTERSECT");
    ASTs elements;

    auto parse_element = [&]
    {
        ASTPtr element;
        if (!elem_parser.parse(pos, element, expected))
            return false;

        elements.push_back(element);
        return true;
    };

    /// Parse UNION type
    auto parse_separator = [&]
    {
        if (s_union_parser.ignore(pos, expected))
        {
            // SELECT ... UNION ALL SELECT ...
            if (s_all_parser.check(pos, expected))
            {
                union_modes.push_back(ASTSelectWithUnionQuery::Mode::ALL);
            }
            // SELECT ... UNION DISTINCT SELECT ...
            else if (s_distinct_parser.check(pos, expected))
            {
                union_modes.push_back(ASTSelectWithUnionQuery::Mode::DISTINCT);
            }
            // SELECT ... UNION SELECT ...
            else
                union_modes.push_back(ASTSelectWithUnionQuery::Mode::Unspecified);
            return true;
        }
        else if (s_except_parser.check(pos, expected))
        {
            // SELECT ... EXCEPT ALL SELECT ...
            if (s_all_parser.check(pos, expected))
            {
                union_modes.push_back(ASTSelectWithUnionQuery::Mode::EXCEPT_ALL);
            }
            // SELECT ... EXCEPT DISTINCT SELECT ...
            else if (s_distinct_parser.check(pos, expected))
            {
                union_modes.push_back(ASTSelectWithUnionQuery::Mode::EXCEPT_DISTINCT);
            }
            // SELECT ... EXCEPT SELECT ...
            else
                union_modes.push_back(ASTSelectWithUnionQuery::Mode::EXCEPT_UNSPECIFIED);
            return true;
        }
        else if (s_intersect_parser.check(pos, expected))
        {
            // SELECT ... INTERSECT ALL SELECT ...
            if (s_all_parser.check(pos, expected))
            {
                union_modes.push_back(ASTSelectWithUnionQuery::Mode::INTERSECT_ALL);
            }
            // SELECT ... INTERSECT DISTINCT SELECT ...
            else if (s_distinct_parser.check(pos, expected))
            {
                union_modes.push_back(ASTSelectWithUnionQuery::Mode::INTERSECT_DISTINCT);
            }
            // SELECT ... INTERSECT SELECT ...
            else
                union_modes.push_back(ASTSelectWithUnionQuery::Mode::INTERSECT_UNSPECIFIED);
            return true;
        }
        return false;
    };

    if (!parseUtil(pos, parse_element, parse_separator))
        return false;

    auto list = std::make_shared<ASTExpressionList>();
    list->children = std::move(elements);
    node = list;
    return true;
}

static bool parseOperator(IParser::Pos & pos, const char * op, Expected & expected)
{
    if (isWordCharASCII(*op))
    {
        return ParserKeyword(op).ignore(pos, expected);
    }
    else
    {
        if (strlen(op) == pos->size() && 0 == memcmp(op, pos->begin, pos->size()))
        {
            ++pos;
            return true;
        }
        return false;
    }
}


bool ParserLeftAssociativeBinaryOperatorList::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    bool first = true;

    auto current_depth = pos.depth;
    while (true)
    {
        if (first)
        {
            ASTPtr elem;
            if (!first_elem_parser->parse(pos, elem, expected))
                return false;

            node = elem;
            first = false;
        }
        else
        {
            /// try to find any of the valid operators

            const char ** it;
            Expected stub;
            for (it = overlapping_operators_to_skip; *it; ++it)
                if (ParserKeyword{*it}.checkWithoutMoving(pos, stub))
                    break;

            if (*it)
                break;

            for (it = operators; *it; it += 2)
                if (parseOperator(pos, *it, expected))
                    break;

            if (!*it)
                break;

            /// the function corresponding to the operator
            auto function = std::make_shared<ASTFunction>();

            /// the ast of quantified comparison
            auto quantified_comparison = std::make_shared<ASTQuantifiedComparison>();

            /// function arguments
            auto exp_list = std::make_shared<ASTExpressionList>();

            ASTPtr elem;
            QuantifierType quantified_type_node = QuantifierType::ANY;
            bool have_quantifier_comparison =  false;
            if (allow_any_all_operators && ParserKeyword("ANY").ignore(pos, expected))
            {
                quantified_type_node = QuantifierType::ANY;
                have_quantifier_comparison = true;
            }
            else if (allow_any_all_operators && ParserKeyword("ALL").ignore(pos, expected))
            {
                quantified_type_node = QuantifierType::ALL;
                have_quantifier_comparison = true;
            }
            else if (allow_any_all_operators && ParserKeyword("SOME").ignore(pos, expected))
            {
                quantified_type_node = QuantifierType::SOME;
                have_quantifier_comparison = true;
            }
            else if (!(remaining_elem_parser ? remaining_elem_parser : first_elem_parser)->parse(pos, elem, expected))
                return false;

            if (allow_any_all_operators && have_quantifier_comparison && !ParserSubquery().parse(pos, elem, expected))
                return false;

            if (!have_quantifier_comparison)
            {
                /// the first argument of the function is the previous element, the second is the next one
                function->name = it[1];
                function->arguments = exp_list;
                function->children.push_back(exp_list);

                exp_list->children.push_back(node);
                exp_list->children.push_back(elem);

                /** special exception for the access operator to the element of the array `x[y]`, which
              * contains the infix part '[' and the suffix ''] '(specified as' [')
              */
                if (0 == strcmp(it[0], "["))
                {
                    if (pos->type != TokenType::ClosingSquareBracket)
                        return false;
                    ++pos;
                }

                // Special handling mapElement function map[key]
                if (0 == strcmp(it[0], "{"))
                {
                    if (pos->type != TokenType::ClosingCurlyBrace)
                        return false;
                    ++pos;
                }


                /// Left associative operator chain is parsed as a tree: ((((1 + 1) + 1) + 1) + 1)...
                /// We must account it's depth - otherwise we may end up with stack overflow later - on destruction of AST.
                pos.increaseDepth();
                node = function;
            }
            else
            {
                quantified_comparison->comparator = it[1];
                quantified_comparison->quantifier_type = quantified_type_node;
                quantified_comparison->children.push_back(node);
                quantified_comparison->children.push_back(elem);
                node = quantified_comparison;
            }

        }
    }

    pos.depth = current_depth;
    return true;
}


bool ParserVariableArityOperatorList::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    ASTPtr arguments;

    if (!elem_parser->parse(pos, node, expected))
        return false;

    while (true)
    {
        if (!parseOperator(pos, infix, expected))
            break;

        if (!arguments)
        {
            node = makeASTFunction(function_name, node);
            arguments = node->as<ASTFunction &>().arguments;
        }

        ASTPtr elem;
        if (!elem_parser->parse(pos, elem, expected))
            return false;

        arguments->children.push_back(elem);
    }

    return true;
}

bool ParserBetweenExpression::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    /// For the expression (subject [NOT] BETWEEN left AND right)
    ///  create an AST the same as for (subject> = left AND subject <= right).

    ParserKeyword s_not("NOT");
    ParserKeyword s_between("BETWEEN");
    ParserKeyword s_and("AND");

    ASTPtr subject;
    ASTPtr left;
    ASTPtr right;

    if (!elem_parser.parse(pos, subject, expected))
        return false;

    bool negative = s_not.ignore(pos, expected);

    if (!s_between.ignore(pos, expected))
    {
        if (negative)
            --pos;

        /// No operator was parsed, just return element.
        node = subject;
    }
    else
    {
        if (!elem_parser.parse(pos, left, expected))
            return false;

        if (!s_and.ignore(pos, expected))
            return false;

        if (!elem_parser.parse(pos, right, expected))
            return false;

        auto f_combined_expression = std::make_shared<ASTFunction>();
        auto args_combined_expression = std::make_shared<ASTExpressionList>();

        /// [NOT] BETWEEN left AND right
        auto f_left_expr = std::make_shared<ASTFunction>();
        auto args_left_expr = std::make_shared<ASTExpressionList>();

        auto f_right_expr = std::make_shared<ASTFunction>();
        auto args_right_expr = std::make_shared<ASTExpressionList>();

        args_left_expr->children.emplace_back(subject);
        args_left_expr->children.emplace_back(left);

        args_right_expr->children.emplace_back(subject);
        args_right_expr->children.emplace_back(right);

        if (negative)
        {
            /// NOT BETWEEN
            f_left_expr->name = "less";
            f_right_expr->name = "greater";
            f_combined_expression->name = "or";
        }
        else
        {
            /// BETWEEN
            f_left_expr->name = "greaterOrEquals";
            f_right_expr->name = "lessOrEquals";
            f_combined_expression->name = "and";
        }

        f_left_expr->arguments = args_left_expr;
        f_left_expr->children.emplace_back(f_left_expr->arguments);

        f_right_expr->arguments = args_right_expr;
        f_right_expr->children.emplace_back(f_right_expr->arguments);

        args_combined_expression->children.emplace_back(f_left_expr);
        args_combined_expression->children.emplace_back(f_right_expr);

        f_combined_expression->arguments = args_combined_expression;
        f_combined_expression->children.emplace_back(f_combined_expression->arguments);

        node = f_combined_expression;
    }

    return true;
}

bool ParserTernaryOperatorExpression::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    ParserToken symbol1(TokenType::QuestionMark);
    ParserToken symbol2(TokenType::Colon);

    ASTPtr elem_cond;
    ASTPtr elem_then;
    ASTPtr elem_else;

    if (!elem_parser.parse(pos, elem_cond, expected))
        return false;

    if (!symbol1.ignore(pos, expected))
        node = elem_cond;
    else
    {
        if (!elem_parser.parse(pos, elem_then, expected))
            return false;

        if (!symbol2.ignore(pos, expected))
            return false;

        if (!elem_parser.parse(pos, elem_else, expected))
            return false;

        /// the function corresponding to the operator
        auto function = std::make_shared<ASTFunction>();

        /// function arguments
        auto exp_list = std::make_shared<ASTExpressionList>();

        function->name = "if";
        function->arguments = exp_list;
        function->children.push_back(exp_list);

        exp_list->children.push_back(elem_cond);
        exp_list->children.push_back(elem_then);
        exp_list->children.push_back(elem_else);

        node = function;
    }

    return true;
}


bool ParserLambdaExpression::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    ParserToken arrow(TokenType::Arrow);
    ParserToken open(TokenType::OpeningRoundBracket);
    ParserToken close(TokenType::ClosingRoundBracket);

    Pos begin = pos;

    do
    {
        ASTPtr inner_arguments;
        ASTPtr expression;

        bool was_open = false;

        if (open.ignore(pos, expected))
        {
            was_open = true;
        }

        if (!ParserList(std::make_unique<ParserIdentifier>(), std::make_unique<ParserToken>(TokenType::Comma)).parse(pos, inner_arguments, expected))
            break;

        if (was_open)
        {
            if (!close.ignore(pos, expected))
                break;
        }

        if (!arrow.ignore(pos, expected))
            break;

        if (!elem_parser.parse(pos, expression, expected))
            return false;

        /// lambda(tuple(inner_arguments), expression)

        auto lambda = std::make_shared<ASTFunction>();
        node = lambda;
        lambda->name = "lambda";

        auto outer_arguments = std::make_shared<ASTExpressionList>();
        lambda->arguments = outer_arguments;
        lambda->children.push_back(lambda->arguments);

        auto tuple = std::make_shared<ASTFunction>();
        outer_arguments->children.push_back(tuple);
        tuple->name = "tuple";
        tuple->arguments = inner_arguments;
        tuple->children.push_back(inner_arguments);

        outer_arguments->children.push_back(expression);

        return true;
    }
    while (false);

    pos = begin;
    return elem_parser.parse(pos, node, expected);
}


bool ParserTableFunctionExpression::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    if (ParserTableFunctionView(dt).parse(pos, node, expected))
        return true;
    return elem_parser.parse(pos, node, expected);
}


bool ParserPrefixUnaryOperatorExpression::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    /// try to find any of the valid operators
    const char ** it;
    for (it = operators; *it; it += 2)
    {
        if (parseOperator(pos, *it, expected))
            break;
    }

    /// Let's parse chains of the form `NOT NOT x`. This is hack.
    /** This is done, because among the unary operators there is only a minus and NOT.
      * But for a minus the chain of unary operators does not need to be supported.
      */
    size_t count = 1;
    if (it[0] && 0 == strncmp(it[0], "NOT", 3))
    {
        while (true)
        {
            const char ** jt;
            for (jt = operators; *jt; jt += 2)
                if (parseOperator(pos, *jt, expected))
                    break;

            if (!*jt)
                break;

            ++count;
        }
    }

    ASTPtr elem;
    if (!elem_parser->parse(pos, elem, expected))
        return false;

    if (!*it)
        node = elem;
    else
    {
        for (size_t i = 0; i < count; ++i)
        {
            /// the function corresponding to the operator
            auto function = std::make_shared<ASTFunction>();

            /// function arguments
            auto exp_list = std::make_shared<ASTExpressionList>();

            function->name = it[1];
            function->arguments = exp_list;
            function->children.push_back(exp_list);

            if (node)
                exp_list->children.push_back(node);
            else
                exp_list->children.push_back(elem);

            node = function;
        }
    }

    return true;
}


bool ParserUnaryExpression::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    /// As an exception, negative numbers should be parsed as literals, and not as an application of the operator.

    if (pos->type == TokenType::Minus)
    {
        Pos begin = pos;
        if (ParserCastOperator(dt).parse(pos, node, expected))
            return true;

        pos = begin;
        if (ParserLiteral(dt).parse(pos, node, expected))
            return true;

        pos = begin;
    }

    return operator_parser.parse(pos, node, expected);
}

bool ParserMapElementExpression::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    return ParserLeftAssociativeBinaryOperatorList{
        operators,
        std::make_unique<ParserCastExpression>(dt),
        std::make_unique<ParserExpression>(dt)
       }.parse(pos, node, expected);
}

bool ParserCastExpression::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    ASTPtr expr_ast;
    if (!elem_parser.parse(pos, expr_ast, expected))
        return false;

    ASTPtr type_ast;
    if (ParserToken(TokenType::DoubleColon).ignore(pos, expected)
        && ParserDataType().parse(pos, type_ast, expected))
    {
        node = createFunctionCast(expr_ast, type_ast);
    }
    else
    {
        node = expr_ast;
    }

    return true;
}


bool ParserArrayElementExpression::parseImpl(Pos & pos, ASTPtr & node, Expected &expected)
{
    return ParserLeftAssociativeBinaryOperatorList{
        operators,
        std::make_unique<ParserMapElementExpression>(dt),
        std::make_unique<ParserExpressionWithOptionalAlias>(false, dt)
    }.parse(pos, node, expected);
}


bool ParserTupleElementExpression::parseImpl(Pos & pos, ASTPtr & node, Expected &expected)
{
    return ParserLeftAssociativeBinaryOperatorList{
        operators,
        std::make_unique<ParserArrayElementExpression>(dt),
        std::make_unique<ParserUnsignedInteger>()
    }.parse(pos, node, expected);
}


ParserExpressionWithOptionalAlias::ParserExpressionWithOptionalAlias(bool allow_alias_without_as_keyword, ParserSettingsImpl t, bool is_table_function)
    : IParserDialectBase(t), impl(std::make_unique<ParserWithOptionalAlias>(
                                 is_table_function ? ParserPtr(std::make_unique<ParserTableFunctionExpression>(dt)) : ParserPtr(std::make_unique<ParserExpression>(dt)),
                                 allow_alias_without_as_keyword, dt))
{
}


bool ParserExpressionList::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    return ParserList(
        std::make_unique<ParserExpressionWithOptionalAlias>(allow_alias_without_as_keyword, dt, is_table_function),
        std::make_unique<ParserToken>(TokenType::Comma))
        .parse(pos, node, expected);
}


bool ParserNotEmptyExpressionList::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    return nested_parser.parse(pos, node, expected) && !node->children.empty();
}


bool ParserOrderByExpressionList::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    return ParserList(std::make_unique<ParserOrderByElement>(dt), std::make_unique<ParserToken>(TokenType::Comma), false)
        .parse(pos, node, expected);
}

bool ParserGroupingSetsExpressionListElements::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    auto command_list = std::make_shared<ASTExpressionList>();
    node = command_list;

    ParserToken s_comma(TokenType::Comma);
    ParserToken s_open(TokenType::OpeningRoundBracket);
    ParserToken s_close(TokenType::ClosingRoundBracket);
    ParserExpressionWithOptionalAlias p_expression(false, dt);
    ParserList p_command(std::make_unique<ParserExpressionWithOptionalAlias>(false, dt),
                          std::make_unique<ParserToken>(TokenType::Comma), true);

    do
    {
        Pos begin = pos;
        ASTPtr command;
        if (!s_open.ignore(pos, expected))
        {
            pos = begin;
            if (!p_expression.parse(pos, command, expected))
            {
                return false;
            }
            auto list = std::make_shared<ASTExpressionList>(',');
            list->children.push_back(command);
            command = std::move(list);
        }
        else
        {
            if (!p_command.parse(pos, command, expected))
                return false;

            if (!s_close.ignore(pos, expected))
                break;
        }

        command_list->children.push_back(command);
    }
    while (s_comma.ignore(pos, expected));

    return true;
}

bool ParserGroupingSetsExpressionList::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    ParserGroupingSetsExpressionListElements grouping_sets_elements;
    return grouping_sets_elements.parse(pos, node, expected);

}

bool ParserTTLExpressionList::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    return ParserList(std::make_unique<ParserTTLElement>(dt), std::make_unique<ParserToken>(TokenType::Comma), false)
        .parse(pos, node, expected);
}


bool ParserNullityChecking::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    ASTPtr node_comp;
    if (!elem_parser.parse(pos, node_comp, expected))
        return false;

    ParserKeyword s_is{"IS"};
    ParserKeyword s_not{"NOT"};
    ParserKeyword s_null{"NULL"};

    if (s_is.ignore(pos, expected))
    {
        bool is_not = false;
        if (s_not.ignore(pos, expected))
            is_not = true;

        if (!s_null.ignore(pos, expected))
            return false;

        auto args = std::make_shared<ASTExpressionList>();
        args->children.push_back(node_comp);

        auto function = std::make_shared<ASTFunction>();
        function->name = is_not ? "isNotNull" : "isNull";
        function->arguments = args;
        function->children.push_back(function->arguments);

        node = function;
    }
    else
        node = node_comp;

    return true;
}

bool ParserDateOperatorExpression::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    auto begin = pos;

    /// If no DATE keyword, go to the nested parser.
    if (!ParserKeyword("DATE").ignore(pos, expected))
        return next_parser.parse(pos, node, expected);

    ASTPtr expr;
    if (!ParserStringLiteral().parse(pos, expr, expected))
    {
        pos = begin;
        return next_parser.parse(pos, node, expected);
    }

    /// the function corresponding to the operator
    auto function = std::make_shared<ASTFunction>();

    /// function arguments
    auto exp_list = std::make_shared<ASTExpressionList>();

    /// the first argument of the function is the previous element, the second is the next one
    function->name = "toDate";
    function->arguments = exp_list;
    function->children.push_back(exp_list);

    exp_list->children.push_back(expr);

    node = function;
    return true;
}

bool ParserTimestampOperatorExpression::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    auto begin = pos;

    /// If no TIMESTAMP keyword, go to the nested parser.
    if (!ParserKeyword("TIMESTAMP").ignore(pos, expected))
        return next_parser.parse(pos, node, expected);

    ASTPtr expr;
    if (!ParserStringLiteral().parse(pos, expr, expected))
    {
        pos = begin;
        return next_parser.parse(pos, node, expected);
    }

    /// the function corresponding to the operator
    auto function = std::make_shared<ASTFunction>();

    /// function arguments
    auto exp_list = std::make_shared<ASTExpressionList>();

    /// the first argument of the function is the previous element, the second is the next one
    function->name = "toDateTime";
    function->arguments = exp_list;
    function->children.push_back(exp_list);

    exp_list->children.push_back(expr);

    node = function;
    return true;
}

bool ParserIntervalOperatorExpression::parseArgumentAndIntervalKind(
    Pos & pos, ASTPtr & expr, IntervalKind & interval_kind, Expected & expected, ParserSettingsImpl t)
{
    auto begin = pos;
    auto init_expected = expected;
    ASTPtr string_literal;
    //// A String literal followed INTERVAL keyword,
    /// the literal can be a part of an expression or
    /// include Number and INTERVAL TYPE at the same time
    if (ParserStringLiteral{}.parse(pos, string_literal, expected))
    {
        String literal;
        if (string_literal->as<ASTLiteral &>().value.tryGet(literal))
        {
            Tokens tokens(literal.data(), literal.data() + literal.size());
            Pos token_pos(tokens, 0);
            Expected token_expected;

            if (!ParserNumber{t}.parse(token_pos, expr, token_expected))
                return false;
            else
            {
                /// case: INTERVAL '1' HOUR
                /// back to begin
                if (!token_pos.isValid())
                {
                    pos = begin;
                    expected = init_expected;
                }
                else
                    /// case: INTERVAL '1 HOUR'
                    return parseIntervalKind(token_pos, token_expected, interval_kind);
            }
        }
    }
    // case: INTERVAL expr HOUR
    if (!ParserExpressionWithOptionalAlias(false, t).parse(pos, expr, expected))
        return false;
    return parseIntervalKind(pos, expected, interval_kind);
}

static bool parseLiteral(const ASTPtr node, UInt64 &node_value) {
    return node->as<ASTLiteral &>().value.tryGet(node_value);
}

/** SQL standard defined interval types YEAR_MONTH and DAY_TIME
 * YEAR_MONTH interval type has the format year-month
 * DAY_TIME interval type has the format "day hour:minute:second"
 */
bool ParserIntervalOperatorExpression::parseSQLStandardArgumentAndIntervalKind(
    Pos & pos, ASTPtr & expr, IntervalKind & interval_kind, Expected & expected)
{
    ASTPtr string_literal;
    // A String literal followed INTERVAL keyword,
    /// the literal can be 'year-month' or 'day time:minute:second'.
    if (!ParserStringLiteral{}.parse(pos, string_literal, expected))
        return false;

    String literal;
    if (!string_literal->as<ASTLiteral &>().value.tryGet(literal))
        return false;

    Tokens tokens(literal.data(), literal.data() + literal.size());
    Pos token_pos(tokens, 0);

    if (ParserKeyword("YEAR_MONTH").ignore(pos, expected)) {
        ASTPtr year_literal, month_literal;
        ParserToken dash(TokenType::Minus);

        if (!ParserUnsignedInteger{}.parse(token_pos, year_literal, expected) ||
            !dash.ignore(token_pos, expected) ||
            !ParserUnsignedInteger{}.parse(token_pos, month_literal, expected))
            return false;

        UInt64 year = 0;
        UInt64 month = 0;

        if (!parseLiteral(year_literal, year)
            || !parseLiteral(month_literal, month)) {
            return false;
        }

        if (month > 12)
            return false;

        UInt64 to_month = year * 12 + month;
        expr = std::make_shared<ASTLiteral>(to_month);
        interval_kind = IntervalKind::Month;
        return true;
    }

    if (ParserKeyword("DAY_TIME").ignore(pos, expected)) {
        ASTPtr day_literal, hour_literal, minute_literal, second_literal;
        ParserToken colon(TokenType::Colon);

        if (!ParserUnsignedInteger{}.parse(token_pos, day_literal, expected)
            || !ParserUnsignedInteger{}.parse(token_pos, hour_literal, expected)
            || !colon.ignore(token_pos, expected)
            || !ParserUnsignedInteger{}.parse(token_pos, minute_literal, expected)
            || !colon.ignore(token_pos, expected)
            || !ParserUnsignedInteger{}.parse(token_pos, second_literal, expected))
            return false;

        UInt64 day = 0;
        UInt64 hour = 0;
        UInt64 minute = 0;
        UInt64 second = 0;

        if (!parseLiteral(day_literal, day)
            || !parseLiteral(hour_literal, hour)
            || !parseLiteral(minute_literal, minute)
            || !parseLiteral(second_literal, second)) {
            return false;
        }

        if (hour > 24 || minute > 60 || second > 60)
            return false;

        std::chrono::duration<UInt64> tm = std::chrono::hours(day * 24 + hour)
                                            + std::chrono::minutes(minute)
                                            + std::chrono::seconds(second);
        expr = std::make_shared<ASTLiteral>(tm.count());
        interval_kind = IntervalKind::Second;
        return true;
    }

    return false;
}

bool ParserIntervalOperatorExpression::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    auto begin = pos;

    /// If no INTERVAL keyword, go to the nested parser.
    if (!ParserKeyword("INTERVAL").ignore(pos, expected))
        return next_parser.parse(pos, node, expected);

    ASTPtr expr;
    IntervalKind interval_kind;
    if (!parseArgumentAndIntervalKind(pos, expr, interval_kind, expected, dt))
    {
        pos = begin;
        ++pos;
        // If the interval type is YEAR_MONTH or DAY_TIME
        if (!parseSQLStandardArgumentAndIntervalKind(pos, expr, interval_kind, expected)) {
            pos = begin;
            return next_parser.parse(pos, node, expected);
        }
    }

    /// the function corresponding to the operator
    auto function = std::make_shared<ASTFunction>();

    /// function arguments
    auto exp_list = std::make_shared<ASTExpressionList>();

    /// the first argument of the function is the previous element, the second is the next one
    function->name = interval_kind.toNameOfFunctionToIntervalDataType();
    function->arguments = exp_list;
    function->children.push_back(exp_list);

    exp_list->children.push_back(expr);

    node = function;
    return true;
}

bool ParserKeyValuePair::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    ParserIdentifier id_parser;
    ParserLiteral literal_parser(dt);
    ParserFunction func_parser(dt);

    ASTPtr identifier;
    ASTPtr value;
    bool with_brackets = false;
    if (!id_parser.parse(pos, identifier, expected))
        return false;

    /// If it's neither literal, nor identifier, nor function, than it's possible list of pairs
    if (!func_parser.parse(pos, value, expected) && !literal_parser.parse(pos, value, expected) && !id_parser.parse(pos, value, expected))
    {
        ParserKeyValuePairsList kv_pairs_list(dt);
        ParserToken open(TokenType::OpeningRoundBracket);
        ParserToken close(TokenType::ClosingRoundBracket);

        if (!open.ignore(pos))
            return false;

        if (!kv_pairs_list.parse(pos, value, expected))
            return false;

        if (!close.ignore(pos))
            return false;

        with_brackets = true;
    }

    auto pair = std::make_shared<ASTPair>(with_brackets);
    pair->first = Poco::toLower(identifier->as<ASTIdentifier>()->name());
    pair->set(pair->second, value);
    node = pair;
    return true;
}

bool ParserKeyValuePairsList::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    ParserList parser(std::make_unique<ParserKeyValuePair>(dt), std::make_unique<ParserNothing>(), true, 0);
    return parser.parse(pos, node, expected);
}

}

