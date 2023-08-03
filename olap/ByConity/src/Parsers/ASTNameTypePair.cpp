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

#include <Parsers/ASTNameTypePair.h>
#include <Parsers/ASTSerDerHelper.h>
#include <Common/quoteString.h>
#include <IO/Operators.h>


namespace DB
{

ASTPtr ASTNameTypePair::clone() const
{
    auto res = std::make_shared<ASTNameTypePair>(*this);
    res->children.clear();

    if (type)
    {
        res->type = type->clone();
        res->children.push_back(res->type);
    }

    return res;
}


void ASTNameTypePair::formatImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const
{
    std::string indent_str = settings.one_line ? "" : std::string(4 * frame.indent, ' ');

    settings.ostr << indent_str << backQuoteIfNeed(name) << ' ';
    type->formatImpl(settings, state, frame);
}

void ASTNameTypePair::serialize(WriteBuffer & buf) const
{
    writeBinary(name, buf);
    serializeAST(type, buf);
}

void ASTNameTypePair::deserializeImpl(ReadBuffer & buf)
{
    readBinary(name, buf);
    type = deserializeASTWithChildren(children, buf);
}

ASTPtr ASTNameTypePair::deserialize(ReadBuffer & buf)
{
    auto name_type = std::make_shared<ASTNameTypePair>();
    name_type->deserializeImpl(buf);
    return name_type;
}

}


