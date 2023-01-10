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

#include <Parsers/ASTFunctionWithKeyValueArguments.h>
#include <Parsers/ASTSerDerHelper.h>

#include <Poco/String.h>
#include <Common/SipHash.h>
#include <IO/Operators.h>

namespace DB
{

String ASTPair::getID(char) const
{
    return "pair";
}


ASTPtr ASTPair::clone() const
{
    auto res = std::make_shared<ASTPair>(*this);
    res->children.clear();
    res->set(res->second, second->clone());
    return res;
}


void ASTPair::formatImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const
{
    settings.ostr << (settings.hilite ? hilite_keyword : "") << Poco::toUpper(first) << " " << (settings.hilite ? hilite_none : "");

    if (second_with_brackets)
        settings.ostr << (settings.hilite ? hilite_keyword : "") << "(";

    second->formatImpl(settings, state, frame);

    if (second_with_brackets)
        settings.ostr << (settings.hilite ? hilite_keyword : "") << ")";

    settings.ostr << (settings.hilite ? hilite_none : "");
}


void ASTPair::updateTreeHashImpl(SipHash & hash_state) const
{
    hash_state.update(first.size());
    hash_state.update(first);
    hash_state.update(second_with_brackets);
    IAST::updateTreeHashImpl(hash_state);
}


String ASTFunctionWithKeyValueArguments::getID(char delim) const
{
    return "FunctionWithKeyValueArguments " + (delim + name);
}


ASTPtr ASTFunctionWithKeyValueArguments::clone() const
{
    auto res = std::make_shared<ASTFunctionWithKeyValueArguments>(*this);
    res->children.clear();

    if (elements)
    {
        res->elements = elements->clone();
        res->children.push_back(res->elements);
    }

    return res;
}


void ASTFunctionWithKeyValueArguments::formatImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const
{
    settings.ostr << (settings.hilite ? hilite_keyword : "") << Poco::toUpper(name) << (settings.hilite ? hilite_none : "") << (has_brackets ? "(" : "");
    elements->formatImpl(settings, state, frame);
    settings.ostr << (has_brackets ? ")" : "");
    settings.ostr << (settings.hilite ? hilite_none : "");
}


void ASTFunctionWithKeyValueArguments::updateTreeHashImpl(SipHash & hash_state) const
{
    hash_state.update(name.size());
    hash_state.update(name);
    hash_state.update(has_brackets);
    IAST::updateTreeHashImpl(hash_state);
}

void ASTFunctionWithKeyValueArguments::serialize(WriteBuffer & buf) const
{
    writeBinary(name, buf);
    serializeAST(elements, buf);
    writeBinary(has_brackets, buf);
}

void ASTFunctionWithKeyValueArguments::deserializeImpl(ReadBuffer & buf)
{
    readBinary(name, buf);
    elements = deserializeASTWithChildren(children, buf);
    readBinary(has_brackets, buf);
}

ASTPtr ASTFunctionWithKeyValueArguments::deserialize(ReadBuffer & buf)
{
    auto function = std::make_shared<ASTFunctionWithKeyValueArguments>();
    function->deserializeImpl(buf);
    return function;
}

}
