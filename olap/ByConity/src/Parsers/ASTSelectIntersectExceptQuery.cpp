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

#include <Parsers/ASTSelectIntersectExceptQuery.h>
#include <Parsers/ASTSubquery.h>
#include <Parsers/ASTSelectWithUnionQuery.h>
#include <Parsers/ASTSerDerHelper.h>
#include <QueryPlan/PlanSerDerHelper.h>


namespace DB
{

ASTPtr ASTSelectIntersectExceptQuery::clone() const
{
    auto res = std::make_shared<ASTSelectIntersectExceptQuery>(*this);

    res->children.clear();
    for (const auto & child : children)
        res->children.push_back(child->clone());

    res->final_operator = final_operator;
    return res;
}

void ASTSelectIntersectExceptQuery::formatImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const
{
    std::string indent_str = settings.one_line ? "" : std::string(4 * frame.indent, ' ');

    auto operator_to_str = [&](auto op)
    {
        if (op == Operator::INTERSECT_ALL)
            return "INTERSECT ALL";
        else if (op == Operator::INTERSECT_DISTINCT)
            return "INTERSECT DISTINCT";
        else if (op == Operator::EXCEPT_ALL)
            return "EXCEPT ALL";
        else if (op == Operator::EXCEPT_DISTINCT)
            return "EXCEPT DISTINCT";
        return "";
    };

    for (ASTs::const_iterator it = children.begin(); it != children.end(); ++it)
    {
        if (it != children.begin())
        {
            settings.ostr << settings.nl_or_ws << indent_str << (settings.hilite ? hilite_keyword : "")
                          << operator_to_str(final_operator)
                          << (settings.hilite ? hilite_none : "")
                          << settings.nl_or_ws;
        }

        (*it)->formatImpl(settings, state, frame);
    }
}

ASTs ASTSelectIntersectExceptQuery::getListOfSelects() const
{
    /**
     * Because of normalization actual number of selects is 2.
     * But this is checked in InterpreterSelectIntersectExceptQuery.
     */
    ASTs selects;
    for (const auto & child : children)
    {
        if (typeid_cast<ASTSelectQuery *>(child.get())
            || typeid_cast<ASTSelectWithUnionQuery *>(child.get())
            || typeid_cast<ASTSelectIntersectExceptQuery *>(child.get()))
            selects.push_back(child);
    }
    return selects;
}

void ASTSelectIntersectExceptQuery::serialize(WriteBuffer & buf) const
{
    serializeEnum(final_operator, buf);

    auto selects = getListOfSelects();

    writeBinary(selects.size(), buf);
    for (auto & child : selects)
        serializeAST(child, buf);
}

void ASTSelectIntersectExceptQuery::deserializeImpl(ReadBuffer & buf)
{
    deserializeEnum(final_operator, buf);

    size_t s1;
    readBinary(s1, buf);
    children.resize(s1);
    for (size_t i = 0; i < s1; ++i)
        deserializeASTWithChildren(children, buf);
}

ASTPtr ASTSelectIntersectExceptQuery::deserialize(ReadBuffer & buf)
{
    auto select_intersect_except = std::make_shared<ASTSelectIntersectExceptQuery>();
    select_intersect_except->deserializeImpl(buf);
    return select_intersect_except;
}

}
