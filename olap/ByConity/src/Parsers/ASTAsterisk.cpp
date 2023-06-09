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

#include <Parsers/ASTAsterisk.h>
#include <Parsers/ASTSerDerHelper.h>
#include <IO/WriteBuffer.h>
#include <IO/Operators.h>

namespace DB
{

ASTPtr ASTAsterisk::clone() const
{
    auto clone = std::make_shared<ASTAsterisk>(*this);
    clone->cloneChildren();
    return clone;
}

void ASTAsterisk::appendColumnName(WriteBuffer & ostr) const { ostr.write('*'); }

void ASTAsterisk::formatImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const
{
    settings.ostr << "*";
    for (const auto & child : children)
    {
        settings.ostr << ' ';
        child->formatImpl(settings, state, frame);
    }
}

void ASTAsterisk::serialize(WriteBuffer & buf) const
{
    serializeASTs(children, buf);
}

void ASTAsterisk::deserializeImpl(ReadBuffer & buf)
{
    children = deserializeASTs(buf);
}

ASTPtr ASTAsterisk::deserialize(ReadBuffer & buf)
{
    auto asterisk = std::make_shared<ASTAsterisk>();
    asterisk->deserializeImpl(buf);
    return asterisk;
}

}
