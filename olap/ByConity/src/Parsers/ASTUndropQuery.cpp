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

#include <Parsers/ASTUndropQuery.h>
#include <Common/quoteString.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int SYNTAX_ERROR;
}


String ASTUndropQuery::getID(char delim) const
{
    return "UndropQuery" + (delim + database) + delim + table;
}

ASTPtr ASTUndropQuery::clone() const
{
    auto res = std::make_shared<ASTUndropQuery>(*this);
    cloneOutputOptions(*res);
    return res;
}

void ASTUndropQuery::formatQueryImpl(const FormatSettings & settings, FormatState &, FormatStateStacked) const
{
    settings.ostr << (settings.hilite ? hilite_keyword : "");
        settings.ostr << "UNDROP ";

    settings.ostr << ((table.empty() && !database.empty()) ? "DATABASE " : "TABLE ");

    settings.ostr << (settings.hilite ? hilite_none : "");

    if (table.empty() && !database.empty())
        settings.ostr << backQuoteIfNeed(database);
    else
        settings.ostr << (!database.empty() ? backQuoteIfNeed(database) + "." : "") << backQuoteIfNeed(table);
}

}
