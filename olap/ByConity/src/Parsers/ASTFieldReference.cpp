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

#include <Parsers/ASTFieldReference.h>
#include <IO/WriteHelpers.h>
#include <IO/Operators.h>

namespace DB
{

ASTPtr ASTFieldReference::clone() const
{
    return std::make_shared<ASTFieldReference>(*this);
}

void ASTFieldReference::formatImplWithoutAlias(const FormatSettings & settings, FormatState &, FormatStateStacked) const
{
    settings.ostr << (settings.hilite ? hilite_identifier : "");
    settings.writeIdentifier("@" + std::to_string(field_index));
    settings.ostr << (settings.hilite ? hilite_none : "");
}

void ASTFieldReference::appendColumnNameImpl(WriteBuffer & ostr) const
{
    writeString("@" + std::to_string(field_index), ostr);
}

}
