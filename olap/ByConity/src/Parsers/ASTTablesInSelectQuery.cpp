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

#include <Parsers/ASTTablesInSelectQuery.h>

#include <Parsers/ASTExpressionList.h>
#include <Parsers/ASTSerDerHelper.h>
#include <Common/SipHash.h>
#include <IO/Operators.h>
#include <IO/ReadHelpers.h>
#include <IO/WriteHelpers.h>
#include <QueryPlan/PlanSerDerHelper.h>


namespace DB
{

#define CLONE(member) \
do \
{ \
    if (member) \
    { \
        res->member = (member)->clone(); \
        res->children.push_back(res->member); \
    } \
} \
while (false)


void ASTTableExpression::updateTreeHashImpl(SipHash & hash_state) const
{
    hash_state.update(final);
    IAST::updateTreeHashImpl(hash_state);
}


ASTPtr ASTTableExpression::clone() const
{
    auto res = std::make_shared<ASTTableExpression>(*this);
    res->children.clear();

    CLONE(database_and_table_name);
    CLONE(table_function);
    CLONE(subquery);
    CLONE(sample_size);
    CLONE(sample_offset);

    return res;
}

void ASTTableExpression::serialize(WriteBuffer & buf) const
{
    serializeAST(database_and_table_name, buf);
    serializeAST(table_function, buf);
    serializeAST(subquery, buf);

    writeBinary(final, buf);

    serializeAST(sample_size, buf);
    serializeAST(sample_offset, buf);
}

void ASTTableExpression::deserializeImpl(ReadBuffer & buf)
{
    database_and_table_name = deserializeASTWithChildren(children, buf);
    table_function = deserializeASTWithChildren(children, buf);
    subquery = deserializeASTWithChildren(children, buf);

    readBinary(final, buf);

    sample_size = deserializeASTWithChildren(children, buf);
    sample_offset = deserializeASTWithChildren(children, buf);
}

ASTPtr ASTTableExpression::deserialize(ReadBuffer & buf)
{
    auto expression = std::make_shared<ASTTableExpression>();
    expression->deserializeImpl(buf);
    return expression;
}

void ASTTableJoin::updateTreeHashImpl(SipHash & hash_state) const
{
    hash_state.update(locality);
    hash_state.update(strictness);
    hash_state.update(kind);
    IAST::updateTreeHashImpl(hash_state);
}

ASTPtr ASTTableJoin::clone() const
{
    auto res = std::make_shared<ASTTableJoin>(*this);
    res->children.clear();

    CLONE(using_expression_list);
    CLONE(on_expression);

    return res;
}

void ASTTableJoin::serialize(WriteBuffer & buf) const
{
    serializeEnum(locality, buf);
    serializeEnum(strictness, buf);
    serializeEnum(kind, buf);

    serializeAST(using_expression_list, buf);
    serializeAST(on_expression, buf);
}

void ASTTableJoin::deserializeImpl(ReadBuffer & buf)
{
    deserializeEnum(locality, buf);
    deserializeEnum(strictness, buf);
    deserializeEnum(kind, buf);

    using_expression_list = deserializeASTWithChildren(children, buf);
    on_expression = deserializeASTWithChildren(children, buf);
}

ASTPtr ASTTableJoin::deserialize(ReadBuffer & buf)
{
    auto table_join = std::make_shared<ASTTableJoin>();
    table_join->deserializeImpl(buf);
    return table_join;
}

void ASTArrayJoin::updateTreeHashImpl(SipHash & hash_state) const
{
    hash_state.update(kind);
    IAST::updateTreeHashImpl(hash_state);
}

ASTPtr ASTArrayJoin::clone() const
{
    auto res = std::make_shared<ASTArrayJoin>(*this);
    res->children.clear();

    CLONE(expression_list);

    return res;
}

void ASTArrayJoin::serialize(WriteBuffer & buf) const
{
    serializeEnum(kind, buf);
    serializeAST(expression_list, buf);
}

void ASTArrayJoin::deserializeImpl(ReadBuffer & buf)
{
    deserializeEnum(kind, buf);
    expression_list = deserializeASTWithChildren(children, buf);
}

ASTPtr ASTArrayJoin::deserialize(ReadBuffer & buf)
{
    auto array_join = std::make_shared<ASTArrayJoin>();
    array_join->deserializeImpl(buf);
    return array_join;
}

ASTPtr ASTTablesInSelectQueryElement::clone() const
{
    auto res = std::make_shared<ASTTablesInSelectQueryElement>(*this);
    res->children.clear();

    CLONE(table_join);
    CLONE(table_expression);
    CLONE(array_join);

    return res;
}

void ASTTablesInSelectQueryElement::serialize(WriteBuffer & buf) const
{
    serializeAST(table_join, buf);
    serializeAST(table_expression, buf);
    serializeAST(array_join, buf);
}

void ASTTablesInSelectQueryElement::deserializeImpl(ReadBuffer & buf)
{
    table_join = deserializeASTWithChildren(children, buf);
    table_expression = deserializeASTWithChildren(children, buf);
    array_join = deserializeASTWithChildren(children, buf);
}

ASTPtr ASTTablesInSelectQueryElement::deserialize(ReadBuffer & buf)
{
    auto element = std::make_shared<ASTTablesInSelectQueryElement>();
    element->deserializeImpl(buf);
    return element;
}

ASTPtr ASTTablesInSelectQuery::clone() const
{
    const auto res = std::make_shared<ASTTablesInSelectQuery>(*this);
    res->children.clear();

    for (const auto & child : children)
        res->children.emplace_back(child->clone());

    return res;
}

void ASTTablesInSelectQuery::serialize(WriteBuffer & buf) const
{
    serializeASTs(children, buf);
}

void ASTTablesInSelectQuery::deserializeImpl(ReadBuffer & buf)
{
    children = deserializeASTs(buf);
}

ASTPtr ASTTablesInSelectQuery::deserialize(ReadBuffer & buf)
{
    auto tables = std::make_shared<ASTTablesInSelectQuery>();
    tables->deserializeImpl(buf);
    return tables;
}

#undef CLONE


void ASTTableExpression::formatImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const
{
    frame.current_select = this;
    std::string indent_str = settings.one_line ? "" : std::string(4 * frame.indent, ' ');

    if (database_and_table_name)
    {
        settings.ostr << " ";
        database_and_table_name->formatImpl(settings, state, frame);
    }
    else if (table_function)
    {
        settings.ostr << " ";
        table_function->formatImpl(settings, state, frame);
    }
    else if (subquery)
    {
        settings.ostr << settings.nl_or_ws << indent_str;
        subquery->formatImpl(settings, state, frame);
    }

    if (final)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << settings.nl_or_ws << indent_str
            << "FINAL" << (settings.hilite ? hilite_none : "");
    }

    if (sample_size)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << settings.nl_or_ws << indent_str
            << "SAMPLE " << (settings.hilite ? hilite_none : "");
        sample_size->formatImpl(settings, state, frame);

        if (sample_offset)
        {
            settings.ostr << (settings.hilite ? hilite_keyword : "") << ' '
                << "OFFSET " << (settings.hilite ? hilite_none : "");
            sample_offset->formatImpl(settings, state, frame);
        }
    }
}


void ASTTableJoin::formatImplBeforeTable(const FormatSettings & settings, FormatState &, FormatStateStacked frame) const
{
    settings.ostr << (settings.hilite ? hilite_keyword : "");
    std::string indent_str = settings.one_line ? "" : std::string(4 * frame.indent, ' ');

    if (kind != Kind::Comma)
    {
        settings.ostr << settings.nl_or_ws << indent_str;
    }

    switch (locality)
    {
        case Locality::Unspecified:
            break;
        case Locality::Local:
            break;
        case Locality::Global:
            settings.ostr << "GLOBAL ";
            break;
    }

    if (kind != Kind::Cross && kind != Kind::Comma)
    {
        switch (strictness)
        {
            case Strictness::Unspecified:
                break;
            case Strictness::RightAny:
            case Strictness::Any:
                settings.ostr << "ANY ";
                break;
            case Strictness::All:
                settings.ostr << "ALL ";
                break;
            case Strictness::Asof:
                settings.ostr << "ASOF ";
                break;
            case Strictness::Semi:
                settings.ostr << "SEMI ";
                break;
            case Strictness::Anti:
                settings.ostr << "ANTI ";
                break;
        }
    }

    switch (kind)
    {
        case Kind::Inner:
            settings.ostr << "INNER JOIN";
            break;
        case Kind::Left:
            settings.ostr << "LEFT JOIN";
            break;
        case Kind::Right:
            settings.ostr << "RIGHT JOIN";
            break;
        case Kind::Full:
            settings.ostr << "FULL OUTER JOIN";
            break;
        case Kind::Cross:
            settings.ostr << "CROSS JOIN";
            break;
        case Kind::Comma:
            settings.ostr << ",";
            break;
    }

    settings.ostr << (settings.hilite ? hilite_none : "");
}


void ASTTableJoin::formatImplAfterTable(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const
{
    frame.need_parens = false;
    frame.expression_list_prepend_whitespace = false;

    if (using_expression_list)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << " USING " << (settings.hilite ? hilite_none : "");
        settings.ostr << "(";
        using_expression_list->formatImpl(settings, state, frame);
        settings.ostr << ")";
    }
    else if (on_expression)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << " ON " << (settings.hilite ? hilite_none : "");
        on_expression->formatImpl(settings, state, frame);
    }
}


void ASTTableJoin::formatImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const
{
    formatImplBeforeTable(settings, state, frame);
    settings.ostr << " ... ";
    formatImplAfterTable(settings, state, frame);
}


void ASTArrayJoin::formatImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const
{
    frame.expression_list_prepend_whitespace = true;

    settings.ostr << (settings.hilite ? hilite_keyword : "")
        << settings.nl_or_ws
        << (kind == Kind::Left ? "LEFT " : "") << "ARRAY JOIN" << (settings.hilite ? hilite_none : "");

    settings.one_line
        ? expression_list->formatImpl(settings, state, frame)
        : expression_list->as<ASTExpressionList &>().formatImplMultiline(settings, state, frame);
}


void ASTTablesInSelectQueryElement::formatImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const
{
    if (table_expression)
    {
        if (table_join)
            table_join->as<ASTTableJoin &>().formatImplBeforeTable(settings, state, frame);

        table_expression->formatImpl(settings, state, frame);

        if (table_join)
            table_join->as<ASTTableJoin &>().formatImplAfterTable(settings, state, frame);
    }
    else if (array_join)
    {
        array_join->formatImpl(settings, state, frame);
    }
}


void ASTTablesInSelectQuery::formatImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const
{
    std::string indent_str = settings.one_line ? "" : std::string(4 * frame.indent, ' ');

    for (const auto & child : children)
        child->formatImpl(settings, state, frame);
}

}
