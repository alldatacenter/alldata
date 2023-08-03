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

#include <Columns/Collator.h>
#include <Parsers/ASTClusterByElement.h>
#include <IO/Operators.h>


namespace DB
{

void ASTClusterByElement::formatImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const
{
    getColumns()->formatImpl(settings, state, frame);
    settings.ostr << (settings.hilite ? hilite_keyword : "")
        << " INTO "
        << (settings.hilite ? hilite_none : "");
    getTotalBucketNumber()->formatImpl(settings, state, frame);
    settings.ostr << (settings.hilite ? hilite_keyword : "")
        << " BUCKETS"
        << (settings.hilite ? hilite_none : "");
    if (split_number > 0)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << " SPLIT_NUMBER " << split_number << (settings.hilite ? hilite_none : "");
    }
    if (is_with_range)
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "")
        << " WITH_RANGE"
        << (settings.hilite ? hilite_none : "");
    }
}

ASTPtr ASTClusterByElement::clone() const
{
    auto clone = std::make_shared<ASTClusterByElement>(*this);
    clone->cloneChildren();
    return clone;
}


}
