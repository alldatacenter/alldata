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

#include <Parsers/ASTQueryWithTableAndOutput.h>
#include <Common/quoteString.h>


namespace DB
{

struct ASTCheckQuery : public ASTQueryWithTableAndOutput
{
    ASTPtr partition;

    /** Get the text that identifies this element. */
    String getID(char delim) const override { return "CheckQuery" + (delim + database) + delim + table; }

    ASTType getType() const override { return ASTType::ASTCheckQuery; }

    ASTPtr clone() const override
    {
        auto res = std::make_shared<ASTCheckQuery>(*this);
        res->children.clear();
        cloneOutputOptions(*res);
        return res;
    }

protected:
    void formatQueryImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const override
    {
        std::string nl_or_nothing = settings.one_line ? "" : "\n";

        std::string indent_str = settings.one_line ? "" : std::string(4 * frame.indent, ' ');
        std::string nl_or_ws = settings.one_line ? " " : "\n";

        settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << "CHECK TABLE " << (settings.hilite ? hilite_none : "");

        if (!table.empty())
        {
            if (!database.empty())
            {
                settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << backQuoteIfNeed(database) << (settings.hilite ? hilite_none : "");
                settings.ostr << ".";
            }
            settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << backQuoteIfNeed(table) << (settings.hilite ? hilite_none : "");
        }

        if (partition)
        {
            settings.ostr << (settings.hilite ? hilite_keyword : "") << indent_str << " PARTITION " << (settings.hilite ? hilite_none : "");
            partition->formatImpl(settings, state, frame);
        }
    }
};

}
