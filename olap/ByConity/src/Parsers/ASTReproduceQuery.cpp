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

#include <Parsers/ASTReproduceQuery.h>
namespace DB
{
ASTPtr ASTReproduceQuery::clone() const
{
    auto res = std::make_shared<ASTReproduceQuery>(*this);
    res->children.clear();
    res->reproduce_path = reproduce_path;
    return res;
}
void ASTReproduceQuery::formatQueryImpl(const FormatSettings & settings, FormatState &, FormatStateStacked) const
{
    settings.ostr << (settings.hilite ? hilite_keyword : "") << "REPRODUCE" << (settings.hilite ? hilite_none : "");
    settings.ostr << settings.nl_or_ws;
    settings.ostr << reproduce_path;
}
}
