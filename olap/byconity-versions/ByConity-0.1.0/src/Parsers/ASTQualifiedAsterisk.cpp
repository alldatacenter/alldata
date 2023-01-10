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

#include <Parsers/ASTQualifiedAsterisk.h>
#include <Parsers/ASTSerDerHelper.h>
#include <IO/WriteHelpers.h>
#include <IO/Operators.h>

namespace DB
{

void ASTQualifiedAsterisk::appendColumnName(WriteBuffer & ostr) const
{
    const auto & qualifier = children.at(0);
    qualifier->appendColumnName(ostr);
    writeCString(".*", ostr);
}

void ASTQualifiedAsterisk::formatImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const
{
    const auto & qualifier = children.at(0);
    qualifier->formatImpl(settings, state, frame);
    settings.ostr << ".*";
    for (ASTs::const_iterator it = children.begin() + 1; it != children.end(); ++it)
    {
        settings.ostr << ' ';
        (*it)->formatImpl(settings, state, frame);
    }
}

void ASTQualifiedAsterisk::serialize(WriteBuffer & buf) const
{
    serializeASTs(children, buf);
}

void ASTQualifiedAsterisk::deserializeImpl(ReadBuffer & buf)
{
    children = deserializeASTs(buf);
}

ASTPtr ASTQualifiedAsterisk::deserialize(ReadBuffer & buf)
{
    auto asterisk = std::make_shared<ASTQualifiedAsterisk>();
    asterisk->deserializeImpl(buf);
    return asterisk;
}

}
