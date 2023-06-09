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

#include <list>

#include <Parsers/IParserBase.h>
#include <Parsers/CommonParsers.h>

#include <Parsers/ASTSelectWithUnionQuery.h>
#include <Parsers/ExpressionElementParsers.h>
#include <Common/IntervalKind.h>

namespace DB
{

/** Consequent pairs of rows: the operator and the corresponding function. For example, "+" -> "plus".
  * The parsing order of the operators is significant.
  */
using Operators_t = const char **;


/** List of elements separated by something. */
class ParserList : public IParserBase
{
public:
    ParserList(ParserPtr && elem_parser_, ParserPtr && separator_parser_, bool allow_empty_ = true, char result_separator_ = ',')
        : elem_parser(std::move(elem_parser_))
        , separator_parser(std::move(separator_parser_))
        , allow_empty(allow_empty_)
        , result_separator(result_separator_)
    {
    }

    template <typename F>
    static bool parseUtil(Pos & pos, Expected & expected, const F & parse_element, IParser & separator_parser_, bool allow_empty_ = true)
    {
        Pos begin = pos;
        if (!parse_element())
        {
            pos = begin;
            return allow_empty_;
        }

        while (true)
        {
            begin = pos;
            if (!separator_parser_.ignore(pos, expected) || !parse_element())
            {
                pos = begin;
                return true;
            }
        }

        return false;
    }

    template <typename F>
    static bool parseUtil(Pos & pos, Expected & expected, const F & parse_element, TokenType separator, bool allow_empty_ = true)
    {
        ParserToken sep_parser{separator};
        return parseUtil(pos, expected, parse_element, sep_parser, allow_empty_);
    }

    template <typename F>
    static bool parseUtil(Pos & pos, Expected & expected, const F & parse_element, bool allow_empty_ = true)
    {
        return parseUtil(pos, expected, parse_element, TokenType::Comma, allow_empty_);
    }

protected:
    const char * getName() const override { return "list of elements"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
private:
    ParserPtr elem_parser;
    ParserPtr separator_parser;
    bool allow_empty;
    char result_separator;
};

class ParserUnionList : public IParserDialectBase
{
public:
    ParserUnionList(ParserSettingsImpl t) : IParserDialectBase(t)
    {
    }

    template <typename ElemFunc, typename SepFunc>
    static bool parseUtil(Pos & pos, const ElemFunc & parse_element, const SepFunc & parse_separator)
    {
        Pos begin = pos;
        if (!parse_element())
        {
            pos = begin;
            return false;
        }

        while (true)
        {
            begin = pos;
            if (!parse_separator() || !parse_element())
            {
                pos = begin;
                return true;
            }
        }

        return false;
    }

    auto getUnionModes() const { return union_modes; }

protected:
    const char * getName() const override { return "list of union elements"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
private:
    ASTSelectWithUnionQuery::UnionModes union_modes;
};

/** An expression with an infix binary left-associative operator.
  * For example, a + b - c + d.
  */
class ParserLeftAssociativeBinaryOperatorList : public IParserBase
{
private:
    Operators_t operators;
    Operators_t overlapping_operators_to_skip = { (const char *[]){ nullptr } };
    ParserPtr first_elem_parser;
    ParserPtr remaining_elem_parser;
    bool allow_any_all_operators = false;

public:
    /** `operators_` - allowed operators and their corresponding functions
      */
    ParserLeftAssociativeBinaryOperatorList(Operators_t operators_, ParserPtr && first_elem_parser_,  bool allow_any_all_operators_ = false)
        : operators(operators_), first_elem_parser(std::move(first_elem_parser_)), allow_any_all_operators(allow_any_all_operators_)
    {
    }

    ParserLeftAssociativeBinaryOperatorList(Operators_t operators_, Operators_t overlapping_operators_to_skip_, ParserPtr && first_elem_parser_,  bool allow_any_all_operators_ = false)
        : operators(operators_), overlapping_operators_to_skip(overlapping_operators_to_skip_), first_elem_parser(std::move(first_elem_parser_)), allow_any_all_operators(allow_any_all_operators_)
    {
    }

    ParserLeftAssociativeBinaryOperatorList(Operators_t operators_, ParserPtr && first_elem_parser_,
        ParserPtr && remaining_elem_parser_,  bool allow_any_all_operators_ = false)
        : operators(operators_), first_elem_parser(std::move(first_elem_parser_)),
          remaining_elem_parser(std::move(remaining_elem_parser_)), allow_any_all_operators(allow_any_all_operators_)
    {
    }

protected:
    const char * getName() const override { return "list, delimited by binary operators"; }

    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
};


/** Expression with an infix operator of arbitrary arity.
  * For example, a AND b AND c AND d.
  */
class ParserVariableArityOperatorList : public IParserDialectBase
{
private:
    const char * infix;
    const char * function_name;
    ParserPtr elem_parser;

public:
    ParserVariableArityOperatorList(const char * infix_, const char * function_, ParserPtr && elem_parser_, ParserSettingsImpl t)
        : IParserDialectBase(t),infix(infix_), function_name(function_), elem_parser(std::move(elem_parser_))
    {
    }

protected:
    const char * getName() const override { return "list, delimited by operator of variable arity"; }

    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
};


/** An expression with a prefix unary operator.
  * Example, NOT x.
  */
class ParserPrefixUnaryOperatorExpression : public IParserDialectBase
{
private:
    Operators_t operators;
    ParserPtr elem_parser;

public:
    /** `operators_` - allowed operators and their corresponding functions
      */
    ParserPrefixUnaryOperatorExpression(Operators_t operators_, ParserPtr && elem_parser_, ParserSettingsImpl t)
        : IParserDialectBase(t),operators(operators_), elem_parser(std::move(elem_parser_))
    {
    }

protected:
    const char * getName() const override { return "expression with prefix unary operator"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
};

/// CAST operator "::". This parser is used if left argument
/// of operator cannot be read as simple literal from text of query.
/// Example: "[1, 1 + 1, 1 + 2]::Array(UInt8)"
class ParserCastExpression : public IParserDialectBase
{
private:
    ParserExpressionElement elem_parser{dt};

protected:
    const char * getName() const override { return "CAST expression"; }

    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};


class ParserArrayElementExpression : public IParserDialectBase
{
private:
    static const char * operators[];

protected:
    const char * getName() const  override{ return "array element expression"; }

    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};

class ParserMapElementExpression : public IParserDialectBase
{
private:
    static const char * operators[];

protected:
    const char * getName() const override { return "map element expression"; }

    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};

class ParserTupleElementExpression : public IParserDialectBase
{
private:
    static const char * operators[];

protected:
    const char * getName() const override { return "tuple element expression"; }

    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};


class ParserUnaryExpression : public IParserDialectBase
{
private:
    static const char * operators[];
    ParserPrefixUnaryOperatorExpression operator_parser {operators, std::make_unique<ParserTupleElementExpression>(dt), dt};

protected:
    const char * getName() const override { return "unary expression"; }

    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};


class ParserMultiplicativeExpression : public IParserDialectBase
{
private:
    static const char * operators[];
    ParserLeftAssociativeBinaryOperatorList operator_parser {operators, std::make_unique<ParserUnaryExpression>(dt)};

protected:
    const char * getName() const  override { return "multiplicative expression"; }

    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override
    {
        return operator_parser.parse(pos, node, expected);
    }
public:
    using IParserDialectBase::IParserDialectBase;
};

/// DATE operator. "DATE '2001-01-01'" would be parsed as "toDate('2001-01-01')".
class ParserDateOperatorExpression : public IParserDialectBase
{
protected:
    ParserMultiplicativeExpression next_parser{dt};

    const char * getName() const  override { return "DATE operator expression"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};

/// TIMESTAMP operator. "TIMESTAMP '2001-01-01 12:34:56'" would be parsed as "toDateTime('2001-01-01 12:34:56')".
class ParserTimestampOperatorExpression : public IParserDialectBase
{
protected:
    ParserDateOperatorExpression next_parser{dt};

    const char * getName() const  override { return "TIMESTAMP operator expression"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};

/// Optional conversion to INTERVAL data type. Example: "INTERVAL x SECOND" parsed as "toIntervalSecond(x)".
class ParserIntervalOperatorExpression : public IParserDialectBase
{
protected:
    ParserTimestampOperatorExpression next_parser{dt};

    const char * getName() const  override { return "INTERVAL operator expression"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;

private:
    static bool parseArgumentAndIntervalKind(Pos & pos, ASTPtr & expr, IntervalKind & interval_kind, Expected & expected, ParserSettingsImpl t);
    static bool parseSQLStandardArgumentAndIntervalKind(Pos & pos, ASTPtr & expr, IntervalKind & interval_kind, Expected & expected);
public:
    using IParserDialectBase::IParserDialectBase;
};

class ParserAdditiveExpression : public IParserDialectBase
{
private:
    static const char * operators[];
    ParserLeftAssociativeBinaryOperatorList operator_parser {operators, std::make_unique<ParserIntervalOperatorExpression>(dt)};

protected:
    const char * getName() const  override { return "additive expression"; }

    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override
    {
        return operator_parser.parse(pos, node, expected);
    }
public:
    using IParserDialectBase::IParserDialectBase;
};


class ParserConcatExpression : public IParserDialectBase
{
    ParserVariableArityOperatorList operator_parser {"||", "concat", std::make_unique<ParserAdditiveExpression>(dt), dt};

protected:
    const char * getName() const override { return "string concatenation expression"; }

    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override
    {
        return operator_parser.parse(pos, node, expected);
    }
public:
    using IParserDialectBase::IParserDialectBase;
};


class ParserBetweenExpression : public IParserDialectBase
{
private:
    ParserConcatExpression elem_parser{dt};

protected:
    const char * getName() const override { return "BETWEEN expression"; }

    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};


class ParserComparisonExpression : public IParserDialectBase
{
private:
    static const char * operators[];
    static const char * overlapping_operators_to_skip[];
    ParserLeftAssociativeBinaryOperatorList operator_parser {operators, overlapping_operators_to_skip, std::make_unique<ParserBetweenExpression>(dt), true};

protected:
    const char * getName() const  override{ return "comparison expression"; }

    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override
    {
        return operator_parser.parse(pos, node, expected);
    }
public:
    using IParserDialectBase::IParserDialectBase;
};


/** Parser for nullity checking with IS (NOT) NULL.
  */
class ParserNullityChecking : public IParserDialectBase
{
private:
    ParserComparisonExpression elem_parser{dt};

protected:
    const char * getName() const override { return "nullity checking"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};


class ParserLogicalNotExpression : public IParserDialectBase
{
private:
    static const char * operators[];
    ParserPrefixUnaryOperatorExpression operator_parser {operators, std::make_unique<ParserNullityChecking>(dt), dt};

protected:
    const char * getName() const  override{ return "logical-NOT expression"; }

    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override
    {
        return operator_parser.parse(pos, node, expected);
    }
public:
    using IParserDialectBase::IParserDialectBase;
};


class ParserLogicalAndExpression : public IParserDialectBase
{
private:
    ParserVariableArityOperatorList operator_parser {"AND", "and", std::make_unique<ParserLogicalNotExpression>(dt), dt};

protected:
    const char * getName() const override { return "logical-AND expression"; }

    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override
    {
        return operator_parser.parse(pos, node, expected);
    }
public:
    using IParserDialectBase::IParserDialectBase;
};


class ParserLogicalOrExpression : public IParserDialectBase
{
private:
    ParserVariableArityOperatorList operator_parser {"OR", "or", std::make_unique<ParserLogicalAndExpression>(dt), dt};

protected:
    const char * getName() const override { return "logical-OR expression"; }

    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override
    {
        return operator_parser.parse(pos, node, expected);
    }
public:
    using IParserDialectBase::IParserDialectBase;
};


/** An expression with ternary operator.
  * For example, a = 1 ? b + 1 : c * 2.
  */
class ParserTernaryOperatorExpression : public IParserDialectBase
{
private:
    ParserLogicalOrExpression elem_parser{dt};

protected:
    const char * getName() const override { return "expression with ternary operator"; }

    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};


class ParserLambdaExpression : public IParserDialectBase
{
private:
    ParserTernaryOperatorExpression elem_parser{dt};

protected:
    const char * getName() const override { return "lambda expression"; }

    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};


// It's used to parse expressions in table function.
class ParserTableFunctionExpression : public IParserDialectBase
{
private:
    ParserLambdaExpression elem_parser{dt};

protected:
    const char * getName() const override { return "table function expression"; }

    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};


using ParserExpression = ParserLambdaExpression;


class ParserExpressionWithOptionalAlias : public IParserDialectBase
{
public:
    explicit ParserExpressionWithOptionalAlias(bool allow_alias_without_as_keyword, ParserSettingsImpl t, bool is_table_function = false);
protected:
    ParserPtr impl;

    const char * getName() const override { return "expression with optional alias"; }

    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override
    {
        return impl->parse(pos, node, expected);
    }
};


/** A comma-separated list of expressions, probably empty. */
class ParserExpressionList : public IParserDialectBase
{
public:
    explicit ParserExpressionList(bool allow_alias_without_as_keyword_, ParserSettingsImpl t, bool is_table_function_ = false)
        : IParserDialectBase(t), allow_alias_without_as_keyword(allow_alias_without_as_keyword_), is_table_function(is_table_function_) {}

protected:
    bool allow_alias_without_as_keyword;
    bool is_table_function; // This expression list is used by a table function

    const char * getName() const override { return "list of expressions"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
};


class ParserNotEmptyExpressionList : public IParserDialectBase
{
public:
    explicit ParserNotEmptyExpressionList(bool allow_alias_without_as_keyword, ParserSettingsImpl t)
        : IParserDialectBase(t), nested_parser(allow_alias_without_as_keyword, t) {}
private:
    ParserExpressionList nested_parser;
protected:
    const char * getName() const override { return "not empty list of expressions"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
};


class ParserOrderByExpressionList : public IParserDialectBase
{
protected:
    const char * getName() const override { return "order by expression"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};

class ParserGroupingSetsExpressionList : public IParserDialectBase
{
protected:
    const char * getName() const override { return "grouping sets expression"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};

class ParserGroupingSetsExpressionListElements : public IParserDialectBase
{
protected:
    const char * getName() const override { return "grouping sets expression elements"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};

/// Parser for key-value pair, where value can be list of pairs.
class ParserKeyValuePair : public IParserDialectBase
{
protected:
    const char * getName() const override { return "key-value pair"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};


/// Parser for list of key-value pairs.
class ParserKeyValuePairsList : public IParserDialectBase
{
protected:
    const char * getName() const override { return "list of pairs"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};


class ParserTTLExpressionList : public IParserDialectBase
{
protected:
    const char * getName() const override { return "ttl expression"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};

}
