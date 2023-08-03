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

#include <Parsers/ASTSetQuery.h>
#include <Parsers/formatSettingName.h>
#include <Common/SipHash.h>
#include <Common/FieldVisitorHash.h>
#include <Common/FieldVisitorToString.h>
#include <IO/Operators.h>


namespace DB
{

void ASTSetQuery::updateTreeHashImpl(SipHash & hash_state) const
{
    for (const auto & change : changes)
    {
        hash_state.update(change.name.size());
        hash_state.update(change.name);
        applyVisitor(FieldVisitorHash(hash_state), change.value);
    }
}

void ASTSetQuery::formatImpl(const FormatSettings & format, FormatState &, FormatStateStacked) const
{
    if (is_standalone)
        format.ostr << (format.hilite ? hilite_keyword : "") << "SET " << (format.hilite ? hilite_none : "");

    for (auto it = changes.begin(); it != changes.end(); ++it)
    {
        if (it != changes.begin())
            format.ostr << ", ";

        formatSettingName(it->name, format.ostr);
        format.ostr << " = " << applyVisitor(FieldVisitorToString(), it->value);
    }
}

void ASTSetQuery::serialize(WriteBuffer & buf) const
{
    writeBinary(is_standalone, buf);
    changes.serialize(buf);
}

void ASTSetQuery::deserializeImpl(ReadBuffer & buf)
{
    readBinary(is_standalone, buf);
    changes.deserialize(buf);
}

ASTPtr ASTSetQuery::deserialize(ReadBuffer & buf)
{
    auto set = std::make_shared<ASTSetQuery>();
    set->deserializeImpl(buf);
    return set;
}

}
