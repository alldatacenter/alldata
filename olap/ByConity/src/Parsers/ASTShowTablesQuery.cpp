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

#include <iomanip>
#include <Parsers/ASTShowTablesQuery.h>
#include <Common/quoteString.h>
#include <IO/Operators.h>

namespace DB
{

ASTPtr ASTShowTablesQuery::clone() const
{
    auto res = std::make_shared<ASTShowTablesQuery>(*this);
    res->children.clear();
    cloneOutputOptions(*res);
    return res;
}

void ASTShowTablesQuery::formatLike(const FormatSettings & settings) const
{
    if (!like.empty())
        settings.ostr
            << (settings.hilite ? hilite_keyword : "")
            << (not_like ? " NOT" : "")
            << (case_insensitive_like ? " ILIKE " : " LIKE ")
            << (settings.hilite ? hilite_none : "")
            << DB::quote << like;
}

void ASTShowTablesQuery::formatLimit(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const
{
    if (limit_length)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << " LIMIT " << (settings.hilite ? hilite_none : "");
        limit_length->formatImpl(settings, state, frame);
    }
}

void ASTShowTablesQuery::formatQueryImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const
{
    if (databases)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << "SHOW DATABASES" << (settings.hilite ? hilite_none : "")
            << (history ? " HISTORY" : "");
        formatLike(settings);
        formatLimit(settings, state, frame);

    }
    else if (clusters)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << "SHOW CLUSTERS" << (settings.hilite ? hilite_none : "");
        formatLike(settings);
        formatLimit(settings, state, frame);

    }
    else if (cluster)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << "SHOW CLUSTER" << (settings.hilite ? hilite_none : "");
        settings.ostr << " " << backQuoteIfNeed(cluster_str);
    }
    else if (m_settings)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << "SHOW " << (changed ? "CHANGED " : "") << "SETTINGS" <<
            (settings.hilite ? hilite_none : "");
        formatLike(settings);
    }
    else
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << "SHOW " << (temporary ? "TEMPORARY " : "") <<
             (dictionaries ? "DICTIONARIES" : "TABLES") << (history ? " HISTORY" : "") << (settings.hilite ? hilite_none : "");

        if (!from.empty())
            settings.ostr << (settings.hilite ? hilite_keyword : "") << " FROM " << (settings.hilite ? hilite_none : "")
                << backQuoteIfNeed(from);

        formatLike(settings);

        if (where_expression)
        {
            settings.ostr << (settings.hilite ? hilite_keyword : "") << " WHERE " << (settings.hilite ? hilite_none : "");
            where_expression->formatImpl(settings, state, frame);
        }

        formatLimit(settings, state, frame);
    }
}

}
