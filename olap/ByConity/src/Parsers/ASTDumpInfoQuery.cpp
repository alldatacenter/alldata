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

#include <Parsers/ASTDumpInfoQuery.h>
namespace DB
{
ASTPtr ASTDumpInfoQuery::clone() const
{
    auto res = std::make_shared<ASTDumpInfoQuery>(*this);
    res->children.clear();
    res->dump_string = dump_string;
    if (dump_query)
    {
        res->dump_query = dump_query->clone();
        res->children.push_back(res->dump_query);
    }
    return res;
}
void ASTDumpInfoQuery::formatQueryImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const
{
    settings.ostr << (settings.hilite ? hilite_keyword : "") << "DUMP" << (settings.hilite ? hilite_none : "");
    settings.ostr << settings.nl_or_ws;
    if (dump_query)
        dump_query->formatImpl(settings, state, frame);
}
}
