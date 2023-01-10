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

#include <Parsers/ASTSelectQuery.h>
#include <Parsers/ASTSelectWithUnionQuery.h>
#include <Parsers/ASTSubquery.h>
#include <Parsers/ASTSerDerHelper.h>
#include <Parsers/ASTTEALimit.h>
#include <QueryPlan/PlanSerDerHelper.h>
#include <Common/typeid_cast.h>
#include <IO/Operators.h>
#include <IO/ReadBuffer.h>
#include <IO/WriteBuffer.h>

#include <iostream>

namespace DB
{

ASTPtr ASTSelectWithUnionQuery::clone() const
{
    auto res = std::make_shared<ASTSelectWithUnionQuery>(*this);
    res->children.clear();

    res->list_of_selects = list_of_selects->clone();
    res->children.push_back(res->list_of_selects);

    res->union_mode = union_mode;

    res->list_of_modes = list_of_modes;
    res->set_of_modes = set_of_modes;

    cloneOutputOptions(*res);

    if (tealimit)
    {
        res->tealimit = tealimit->clone();
        res->children.push_back(res->tealimit);
    }
    return res;
}


void ASTSelectWithUnionQuery::formatQueryImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const
{
    std::string indent_str = settings.one_line ? "" : std::string(4 * frame.indent, ' ');

    auto mode_to_str = [&](auto mode)
    {
        if (mode == Mode::ALL)
            return "UNION ALL";
        else if (mode == Mode::DISTINCT)
            return "UNION DISTINCT";
        else if (mode == Mode::INTERSECT_UNSPECIFIED)
            return "INTERSECT";
        else if (mode == Mode::INTERSECT_ALL)
            return "INTERSECT ALL";
        else if (mode == Mode::INTERSECT_DISTINCT)
            return "INTERSECT DISTINCT";
        else if (mode == Mode::EXCEPT_UNSPECIFIED)
            return "EXCEPT";
        else if (mode == Mode::EXCEPT_ALL)
            return "EXCEPT ALL";
        else if (mode == Mode::EXCEPT_DISTINCT)
            return "EXCEPT DISTINCT";
        return "";
    };

    for (ASTs::const_iterator it = list_of_selects->children.begin(); it != list_of_selects->children.end(); ++it)
    {
        if (it != list_of_selects->children.begin())
            settings.ostr << settings.nl_or_ws << indent_str << (settings.hilite ? hilite_keyword : "")
                          << mode_to_str((is_normalized) ? union_mode : list_of_modes[it - list_of_selects->children.begin() - 1])
                          << (settings.hilite ? hilite_none : "");

        if (auto * node = (*it)->as<ASTSelectWithUnionQuery>())
        {
            settings.ostr << settings.nl_or_ws << indent_str;

            if (node->list_of_selects->children.size() == 1)
            {
                (node->list_of_selects->children.at(0))->formatImpl(settings, state, frame);
            }
            else
            {
                auto sub_query = std::make_shared<ASTSubquery>();
                sub_query->children.push_back(*it);
                sub_query->formatImpl(settings, state, frame);
            }
        }
        else
        {
            if (it != list_of_selects->children.begin())
                settings.ostr << settings.nl_or_ws;
            (*it)->formatImpl(settings, state, frame);
        }
    }

    if (tealimit)
    {
        settings.ostr << settings.nl_or_ws;
        tealimit->formatImpl(settings, state, frame);
    }
}

void ASTSelectWithUnionQuery::collectAllTables(std::vector<ASTPtr>& all_tables, bool & has_table_functions) const
{
    for (auto & child : list_of_selects->children)
    {
        auto& select = typeid_cast<ASTSelectQuery&>(*child);
        select.collectAllTables(all_tables, has_table_functions);
    }
}

void ASTSelectWithUnionQuery::resetTEALimit()
{
    if (tealimit)
    {
        tealimit = nullptr;
        // sanity check
        if (!typeid_cast<ASTTEALimit *>(children.back().get()))
            throw Exception("last child should be TEALIMIT in SelectWithUnionQuery", ErrorCodes::LOGICAL_ERROR);
        children.pop_back();
    }
}


bool ASTSelectWithUnionQuery::hasNonDefaultUnionMode() const
{
    return set_of_modes.contains(Mode::DISTINCT);
}

void ASTSelectWithUnionQuery::serialize(WriteBuffer & buf) const
{
    ASTQueryWithOutput::serialize(buf);
    serializeEnum(union_mode, buf);

    writeBinary(list_of_modes.size(), buf);
    for (auto & mode : list_of_modes)
        serializeEnum(mode, buf);

    writeBinary(is_normalized, buf);

    serializeAST(list_of_selects, buf);

    writeBinary(set_of_modes.size(), buf);
    for (auto & mode : set_of_modes)
        serializeEnum(mode, buf);
}

void ASTSelectWithUnionQuery::deserializeImpl(ReadBuffer & buf)
{
    ASTQueryWithOutput::deserializeImpl(buf);
    deserializeEnum(union_mode, buf);

    size_t s1;
    readBinary(s1, buf);
    list_of_modes.resize(s1);
    for (size_t i = 0; i < s1; ++i)
        deserializeEnum(list_of_modes[i], buf);

    readBinary(is_normalized, buf);

    list_of_selects = deserializeASTWithChildren(children, buf);

    size_t s2;
    readBinary(s2, buf);
    for (size_t i = 0; i < s2; ++i)
    {
        Mode mode;
        deserializeEnum(mode, buf);
        set_of_modes.insert(mode);
    }
}

ASTPtr ASTSelectWithUnionQuery::deserialize(ReadBuffer & buf)
{
    auto select_with_union = std::make_shared<ASTSelectWithUnionQuery>();
    select_with_union->deserializeImpl(buf);
    return select_with_union;
}

}
