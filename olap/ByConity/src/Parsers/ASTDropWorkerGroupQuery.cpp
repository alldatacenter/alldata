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
#include <Parsers/ASTDropWorkerGroupQuery.h>
#include <Parsers/ASTSetQuery.h>

namespace DB
{
ASTPtr ASTDropWorkerGroupQuery::clone() const
{
    auto res = std::make_shared<ASTDropWorkerGroupQuery>(*this);
    res->children.clear();

    if (settings)
        res->set(res->settings, settings->clone());

    return res;
}

void ASTDropWorkerGroupQuery::formatImpl(const FormatSettings & s, FormatState & state, FormatStateStacked frame) const
{
    s.ostr << (s.hilite ? hilite_keyword : "") << "DROP WORKER GROUP " << (if_exists ? "IF EXISTS " : "") << (s.hilite ? hilite_none : "");

    s.ostr << (s.hilite ? hilite_identifier : "") << worker_group_id << (s.hilite ? hilite_none : "");

    if (settings)
    {
        s.ostr << (s.hilite ? hilite_keyword : "") << s.nl_or_ws << "SETTINGS " << (s.hilite ? hilite_none : "");
        settings->formatImpl(s, state, frame);
    }
}

}
