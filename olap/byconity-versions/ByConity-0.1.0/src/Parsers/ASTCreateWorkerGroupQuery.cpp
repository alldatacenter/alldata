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

#include <IO/Operators.h>
#include <Parsers/ASTCreateWorkerGroupQuery.h>
#include <Parsers/ASTSetQuery.h>

namespace DB
{
ASTPtr ASTCreateWorkerGroupQuery::clone() const
{
    auto res = std::make_shared<ASTCreateWorkerGroupQuery>(*this);
    res->children.clear();

    if (settings)
        res->set(res->settings, settings->clone());

    return res;
}

void ASTCreateWorkerGroupQuery::formatImpl(const FormatSettings & s, FormatState & state, FormatStateStacked frame) const
{
    s.ostr << (s.hilite ? hilite_keyword : "")
           << "CREATE WORKER GROUP "
           << (if_not_exists ? "IF NOT EXISTS " : "")
           << (s.hilite ? hilite_none : "");

    s.ostr << (s.hilite ? hilite_identifier : "")
           << worker_group_id
           << (s.hilite ? hilite_none : "");

    if (!vw_name.empty())
    {
        s.ostr << (s.hilite ? hilite_keyword : "")
               << " IN "
               << (s.hilite ? hilite_identifier : "")
               << (s.hilite ? hilite_none : "");

        s.ostr << (s.hilite ? hilite_identifier : "") << vw_name << (s.hilite ? hilite_none : "");
    }

    if (settings)
    {
        s.ostr << (s.hilite ? hilite_keyword : "")
               << s.nl_or_ws
               << "SETTINGS "
               << (s.hilite ? hilite_none : "");

        settings->formatImpl(s, state, frame);
    }
}

}
