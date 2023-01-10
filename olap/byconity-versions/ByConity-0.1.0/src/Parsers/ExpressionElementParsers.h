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

#include <Core/Field.h>
#include <Core/MultiEnum.h>
#include <Parsers/IParserBase.h>


namespace DB
{


class ParserArray : public IParserDialectBase
{
protected:
    const char * getName() const override { return "array"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};


/** If in parenthesis an expression from one element - returns this element in `node`;
  *  or if there is a SELECT subquery in parenthesis, then this subquery returned in `node`;
  *  otherwise returns `tuple` function from the contents of brackets.
  */
class ParserParenthesisExpression : public IParserDialectBase
{
protected:
    const char * getName() const override { return "parenthesized expression"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};


/** The SELECT subquery is in parenthesis.
  */
class ParserSubquery : public IParserDialectBase
{
protected:
    const char * getName() const override { return "SELECT subquery"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};


/** An identifier, for example, x_yz123 or `something special`
  * If allow_query_parameter_ = true, also parses substitutions in form {name:Identifier}
  */
class ParserIdentifier : public IParserBase
{
public:
    explicit ParserIdentifier(bool allow_query_parameter_ = false) : allow_query_parameter(allow_query_parameter_) {}

protected:
    const char * getName() const override { return "identifier"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
    bool allow_query_parameter;
};


/** An identifier, possibly containing a dot, for example, x_yz123 or `something special` or Hits.EventTime,
 *  possibly with UUID clause like `db name`.`table name` UUID 'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx'
  */
class ParserCompoundIdentifier : public IParserBase
{
public:
    explicit ParserCompoundIdentifier(bool table_name_with_optional_uuid_ = false, bool allow_query_parameter_ = false)
        : table_name_with_optional_uuid(table_name_with_optional_uuid_), allow_query_parameter(allow_query_parameter_)
    {
    }

protected:
    const char * getName() const override { return "compound identifier"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
    bool table_name_with_optional_uuid;
    bool allow_query_parameter;
};

/** *, t.*, db.table.*, COLUMNS('<regular expression>') APPLY(...) or EXCEPT(...) or REPLACE(...)
  */
class ParserColumnsTransformers : public IParserDialectBase
{
public:
    enum class ColumnTransformer : UInt8
    {
        APPLY,
        EXCEPT,
        REPLACE,
    };
    using ColumnTransformers = MultiEnum<ColumnTransformer, UInt8>;
    static constexpr auto AllTransformers = ColumnTransformers{ColumnTransformer::APPLY, ColumnTransformer::EXCEPT, ColumnTransformer::REPLACE};

    explicit ParserColumnsTransformers(ParserSettingsImpl t, ColumnTransformers allowed_transformers_ = AllTransformers, bool is_strict_ = false)
        : IParserDialectBase(t), allowed_transformers(allowed_transformers_)
        , is_strict(is_strict_)
    {}

protected:
    const char * getName() const override { return "COLUMNS transformers"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
    ColumnTransformers allowed_transformers;
    bool is_strict;
};


/// Just *
class ParserAsterisk : public IParserDialectBase
{
public:
    using ColumnTransformers = ParserColumnsTransformers::ColumnTransformers;
    explicit ParserAsterisk(ParserSettingsImpl t, ColumnTransformers allowed_transformers_ = ParserColumnsTransformers::AllTransformers)
        : IParserDialectBase(t), allowed_transformers(allowed_transformers_)
    {}

protected:
    const char * getName() const override { return "asterisk"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;

    ColumnTransformers allowed_transformers;
};

/** Something like t.* or db.table.*
  */
class ParserQualifiedAsterisk : public IParserDialectBase
{
protected:
    const char * getName() const override { return "qualified asterisk"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};

/** COLUMNS('<regular expression>')
  */
class ParserColumnsMatcher : public IParserDialectBase
{
public:
    using ColumnTransformers = ParserColumnsTransformers::ColumnTransformers;
    explicit ParserColumnsMatcher(ParserSettingsImpl t, ColumnTransformers allowed_transformers_ = ParserColumnsTransformers::AllTransformers)
        : IParserDialectBase(t), allowed_transformers(allowed_transformers_)
    {}

protected:
    const char * getName() const override { return "COLUMNS matcher"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;

    ColumnTransformers allowed_transformers;
};

/** A function, for example, f(x, y + 1, g(z)).
  * Or an aggregate function: sum(x + f(y)), corr(x, y). The syntax is the same as the usual function.
  * Or a parametric aggregate function: quantile(0.9)(x + y).
  *  Syntax - two pairs of parentheses instead of one. The first is for parameters, the second for arguments.
  * For functions, the DISTINCT modifier can be specified, for example, count(DISTINCT x, y).
  */
class ParserFunction : public IParserDialectBase
{
public:
    explicit ParserFunction(ParserSettingsImpl t, bool allow_function_parameters_ = true, bool is_table_function_ = false)
        : IParserDialectBase(t), allow_function_parameters(allow_function_parameters_), is_table_function(is_table_function_)
    {
    }

protected:
    const char * getName() const override { return "function"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
    bool allow_function_parameters;
    bool is_table_function;
};

// A special function parser for view table function.
// It parses an SELECT query as its argument and doesn't support getColumnName().
class ParserTableFunctionView : public IParserDialectBase
{
protected:
    const char * getName() const override { return "function"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};

// Window reference (the thing that goes after OVER) for window function.
// Can be either window name or window definition.
class ParserWindowReference : public IParserDialectBase
{
    const char * getName() const override { return "window reference"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};

class ParserWindowDefinition : public IParserDialectBase
{
    const char * getName() const override { return "window definition"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};

// The WINDOW clause of a SELECT query that defines a list of named windows.
// Returns an ASTExpressionList of ASTWindowListElement's.
class ParserWindowList : public IParserDialectBase
{
    const char * getName() const override { return "WINDOW clause"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};

class ParserCodecDeclarationList : public IParserBase
{
protected:
    const char * getName() const override { return "codec declaration list"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
};

/** Parse compression codec
  * CODEC(ZSTD(2))
  */
class ParserCodec : public IParserBase
{
protected:
    const char * getName() const override { return "codec"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
};

/// Fast path of cast operator "::".
/// It tries to read literal as text.
/// If it fails, later operator will be transformed to function CAST.
/// Examples: "0.1::Decimal(38, 38)", "[1, 2]::Array(UInt8)"
class ParserCastOperator : public IParserDialectBase
{
protected:
    const char * getName() const override { return "CAST operator"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};

ASTPtr createFunctionCast(const ASTPtr & expr_ast, const ASTPtr & type_ast);
class ParserCastAsExpression : public IParserDialectBase
{
protected:
    const char * getName() const override { return "CAST AS expression"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};

class ParserSubstringExpression : public IParserDialectBase
{
protected:
    const char * getName() const override { return "SUBSTRING expression"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};

class ParserTrimExpression : public IParserDialectBase
{
protected:
    const char * getName() const override { return "TRIM expression"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};

class ParserLeftExpression : public IParserDialectBase
{
protected:
    const char * getName() const override { return "LEFT expression"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};

class ParserRightExpression : public IParserDialectBase
{
protected:
    const char * getName() const override { return "RIGHT expression"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};

class ParserExtractExpression : public IParserDialectBase
{
protected:
    const char * getName() const override { return "EXTRACT expression"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};

class ParserDateAddExpression : public IParserDialectBase
{
protected:
    const char * getName() const override { return "DATE_ADD expression"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};

class ParserDateDiffExpression : public IParserDialectBase
{
protected:
    const char * getName() const override { return "DATE_DIFF expression"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};

/** NULL literal.
  */
class ParserNull : public IParserBase
{
protected:
    const char * getName() const override { return "NULL"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
};


/** Numeric literal.
  */
class ParserNumber : public IParserDialectBase
{
protected:
    const char * getName() const override { return "number"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};

/** Unsigned integer, used in right hand side of tuple access operator (x.1).
  */
class ParserUnsignedInteger : public IParserBase
{
protected:
    const char * getName() const override { return "unsigned integer"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
};


/** String in single quotes.
  */
class ParserStringLiteral : public IParserBase
{
protected:
    const char * getName() const override { return "string literal"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
};


/** An array or tuple of literals.
  * Arrays can also be parsed as an application of [] operator and tuples as an application of 'tuple' function.
  * But parsing the whole array/tuple as a whole constant seriously speeds up the analysis of expressions in the case of very large collection.
  * We try to parse the array or tuple as a collection of literals first (fast path),
  *  and if it did not work out (when the collection consists of complex expressions) -
  *  parse as an application of [] operator or 'tuple' function (slow path).
  */
template <typename Collection>
class ParserCollectionOfLiterals : public IParserDialectBase
{
public:
    ParserCollectionOfLiterals(TokenType opening_bracket_, TokenType closing_bracket_, ParserSettingsImpl t)
        : IParserDialectBase(t), opening_bracket(opening_bracket_), closing_bracket(closing_bracket_) {}
protected:
    const char * getName() const override { return "collection of literals"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
private:
    TokenType opening_bracket;
    TokenType closing_bracket;
};

/// A tuple of literals with same type.
class ParserTupleOfLiterals : public IParserDialectBase
{
public:
    using IParserDialectBase::IParserDialectBase;
    ParserCollectionOfLiterals<Tuple> tuple_parser{TokenType::OpeningRoundBracket, TokenType::ClosingRoundBracket, dt};
protected:
    const char * getName() const override { return "tuple"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override
    {
        return tuple_parser.parse(pos, node, expected);
    }
};

class ParserArrayOfLiterals : public IParserDialectBase
{
public:
    using IParserDialectBase::IParserDialectBase;
    ParserCollectionOfLiterals<Array> array_parser{TokenType::OpeningSquareBracket, TokenType::ClosingSquareBracket, dt};
protected:
    const char * getName() const override { return "array"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override
    {
        return array_parser.parse(pos, node, expected);
    }
};

/**
  * Parse query with EXISTS expression.
  */
class ParserExistsExpression : public IParserDialectBase
{
protected:
    const char * getName() const override { return "exists expression"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};

/** The literal is one of: NULL, UInt64, Int64, Float64, String.
  */
class ParserLiteral : public IParserDialectBase
{
protected:
    const char * getName() const override { return "literal"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};


/** The alias is the identifier before which `AS` comes. For example: AS x_yz123.
  */
class ParserAlias : public IParserBase
{
public:
    explicit ParserAlias(bool allow_alias_without_as_keyword_) : allow_alias_without_as_keyword(allow_alias_without_as_keyword_) { }

private:
    static const char * restricted_keywords[];

    bool allow_alias_without_as_keyword;

    const char * getName() const override { return "alias"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
};


/** Prepared statements.
  * Parse query with parameter expression {name:type}.
  */
class ParserIdentifierOrSubstitution : public IParserBase
{
protected:
    const char * getName() const override { return "identifier or substitution"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
};


/** Prepared statements.
  * Parse query with parameter expression {name:type}.
  */
class ParserSubstitution : public IParserDialectBase
{
protected:
    const char * getName() const override { return "substitution"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};


/** MySQL-style global variable: @@var
  */
class ParserMySQLGlobalVariable : public IParserBase
{
protected:
    const char * getName() const override { return "MySQL-style global variable"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
};


/** The expression element is one of: an expression in parentheses, an array, a literal, a function, an identifier, an asterisk.
  */
class ParserExpressionElement : public IParserDialectBase
{
protected:
    const char * getName() const override { return "element of expression"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};


/** An expression element, possibly with an alias, if appropriate.
  */
class ParserWithOptionalAlias : public IParserDialectBase
{
public:
    ParserWithOptionalAlias(ParserPtr && elem_parser_, bool allow_alias_without_as_keyword_, ParserSettingsImpl t)
    : IParserDialectBase(t),elem_parser(std::move(elem_parser_)), allow_alias_without_as_keyword(allow_alias_without_as_keyword_) {}
protected:
    ParserPtr elem_parser;
    bool allow_alias_without_as_keyword;

    const char * getName() const override { return "element of expression with optional alias"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
};

/** Element of CLUSTER BY expression - same as expression element, but in addition, INTO <TOTAL_BUCKET_NUMBER> BUCKETS
  * must be specified.
  */
class ParserClusterByElement : public IParserBase
{
protected:
    const char * getName() const override { return "element of CLUSTER BY expression"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
};


/** Element of ORDER BY expression - same as expression element, but in addition, ASC[ENDING] | DESC[ENDING] could be specified
  *  and optionally, NULLS LAST|FIRST
  *  and optionally, COLLATE 'locale'.
  *  and optionally, WITH FILL [FROM x] [TO y] [STEP z]
  */
class ParserOrderByElement : public IParserDialectBase
{
protected:
    const char * getName() const override { return "element of ORDER BY expression"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};

/** Parser for function with arguments like KEY VALUE (space separated)
  * no commas allowed, just space-separated pairs.
  */
class ParserFunctionWithKeyValueArguments : public IParserDialectBase
{
public:
    explicit ParserFunctionWithKeyValueArguments(ParserSettingsImpl t, bool brackets_can_be_omitted_ = false) : IParserDialectBase(t), brackets_can_be_omitted(brackets_can_be_omitted_)
    {
    }

protected:

    const char * getName() const override { return "function with key-value arguments"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;

    /// brackets for function arguments can be omitted
    bool brackets_can_be_omitted;
};

/** Table engine, possibly with parameters. See examples from ParserIdentifierWithParameters
  * Parse result is ASTFunction, with or without arguments.
  */
class ParserIdentifierWithOptionalParameters : public IParserDialectBase
{
protected:
    const char * getName() const  override{ return "identifier with optional parameters"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};

/** Element of TTL expression - same as expression element, but in addition,
 *   TO DISK 'xxx' | TO VOLUME 'xxx' | DELETE could be specified
  */
class ParserTTLElement : public IParserDialectBase
{
protected:
    const char * getName() const override { return "element of TTL expression"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};

/// Part of the UPDATE command or TTL with GROUP BY of the form: col_name = expr
class ParserAssignment : public IParserDialectBase
{
protected:
    const char * getName() const  override{ return "column assignment"; }
    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
public:
    using IParserDialectBase::IParserDialectBase;
};

}
