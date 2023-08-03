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

#include <Parsers/ASTQuantifiedComparison.h>
#include <Parsers/ASTSerDerHelper.h>
#include <IO/WriteHelpers.h>
#include <IO/WriteBufferFromString.h>
#include <IO/ReadHelpers.h>
#include <IO/Operators.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int UNEXPECTED_AST_STRUCTURE;
}

void ASTQuantifiedComparison::appendColumnNameImpl(WriteBuffer &ostr) const
{
    writeString(comparator, ostr);
    switch (quantifier_type)
    {
        case QuantifierType::ANY:
            writeString(" ANY ", ostr);
            break;
        case QuantifierType::ALL:
            writeString(" ALL ", ostr);
            break;
        case QuantifierType::SOME:
            writeString(" SOME ", ostr);
            break;
    }
    writeChar('(', ostr);
    for (auto it = children.begin(); it != children.end(); ++it)
    {
        if (it!=children.begin())
            writeCString(", ", ostr);
        (*it)->appendColumnName(ostr);
    }
    writeChar(')', ostr);
}
String ASTQuantifiedComparison::getID(char delim) const
{
    switch (quantifier_type)
    {
        case QuantifierType::ANY:
            return "QuantifiedComparison" + (delim + comparator) + "_ANY";
        case QuantifierType::ALL:
            return "QuantifiedComparison" + (delim + comparator) + "_ALL";
        case QuantifierType::SOME:
            return "QuantifiedComparison" + (delim + comparator) + "_SOME";
    }
    return "";
}

ASTPtr ASTQuantifiedComparison::clone() const
{
    auto res = make_shared<ASTQuantifiedComparison>(*this);
    res->cloneChildren();
    return res;
}

void ASTQuantifiedComparison::serialize(WriteBuffer & buf) const
{
    writeBinary(comparator,buf);
    writeBinary(UInt8(quantifier_type), buf);
    serializeASTs(children, buf);
}

void ASTQuantifiedComparison::deserializeImpl(ReadBuffer & buf)
{
    readBinary(comparator, buf);
    UInt8 read_value = 0;
    readBinary(read_value, buf);
    quantifier_type = QuantifierType(read_value);
    children = deserializeASTs(buf);
}

void ASTQuantifiedComparison::formatImplWithoutAlias(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const
{
    FormatStateStacked nested_need_parens = frame;
    FormatStateStacked nested_dont_need_parens = frame;
    nested_need_parens.need_parens = true;
    nested_dont_need_parens.need_parens = false;
    const char * operators[] =
        {
            "notEquals",       " != ",
            "lessOrEquals",    " <= ",
            "greaterOrEquals", " >= ",
            "less",            " < ",
            "greater",         " > ",
            "equals",          " = ",
            nullptr
        };
    const char ** it = nullptr;
    for (it = operators; *it; it+=2)
    {
        if(0 == strcmp(comparator.c_str(), it[0]))
        {
            if (frame.need_parens)
                settings.ostr << '(';
            children[0]->formatImpl(settings, state, nested_need_parens);
            settings.ostr<<(settings.hilite ? hilite_operator : "") << it[1] << (settings.hilite ? hilite_none : "");
            switch (quantifier_type)
            {
                case QuantifierType::ANY:
                    settings.ostr << " ANY ";
                    break;
                case QuantifierType::ALL:
                    settings.ostr << " ALL ";
                    break;
                case QuantifierType::SOME:
                    settings.ostr << " SOME ";
                    break;
            }
            children[1]->formatImpl(settings, state, nested_dont_need_parens);
            if (frame.need_parens)
                settings.ostr << ')';
        }
    }
}

}
