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

#include <Parsers/ASTSettingsProfileElement.h>
#include <Parsers/formatSettingName.h>
#include <Common/FieldVisitorToString.h>
#include <Common/quoteString.h>
#include <IO/Operators.h>


namespace DB
{
namespace
{
    void formatProfileNameOrID(const String & str, bool is_id, const IAST::FormatSettings & settings)
    {
        if (is_id)
        {
            settings.ostr << (settings.hilite ? IAST::hilite_keyword : "") << "ID" << (settings.hilite ? IAST::hilite_none : "") << "("
                          << quoteString(str) << ")";
        }
        else
        {
            settings.ostr << backQuoteIfNeed(str);
        }
    }
}

void ASTSettingsProfileElement::formatImpl(const FormatSettings & settings, FormatState &, FormatStateStacked) const
{
    if (!parent_profile.empty())
    {
        settings.ostr << (settings.hilite ? IAST::hilite_keyword : "") << (use_inherit_keyword ? "INHERIT" : "PROFILE") << " "
                      << (settings.hilite ? IAST::hilite_none : "");
        formatProfileNameOrID(parent_profile, id_mode, settings);
        return;
    }

    formatSettingName(setting_name, settings.ostr);

    if (!value.isNull())
    {
        settings.ostr << " = " << applyVisitor(FieldVisitorToString{}, value);
    }

    if (!min_value.isNull())
    {
        settings.ostr << (settings.hilite ? IAST::hilite_keyword : "") << " MIN " << (settings.hilite ? IAST::hilite_none : "")
                      << applyVisitor(FieldVisitorToString{}, min_value);
    }

    if (!max_value.isNull())
    {
        settings.ostr << (settings.hilite ? IAST::hilite_keyword : "") << " MAX " << (settings.hilite ? IAST::hilite_none : "")
                      << applyVisitor(FieldVisitorToString{}, max_value);
    }

    if (readonly)
    {
        settings.ostr << (settings.hilite ? IAST::hilite_keyword : "") << (*readonly ? " READONLY" : " WRITABLE")
                      << (settings.hilite ? IAST::hilite_none : "");
    }
}

void ASTSettingsProfileElement::serialize(WriteBuffer & buf) const
{
    writeBinary(parent_profile, buf);
    writeBinary(setting_name, buf);
    writeFieldBinary(value, buf);
    writeFieldBinary(min_value, buf);
    writeFieldBinary(max_value, buf);
    if (readonly)
    {
        writeBinary(true, buf);
        writeBinary(readonly.value(), buf);
    }
    else
        writeBinary(false, buf);
    writeBinary(id_mode, buf);
    writeBinary(use_inherit_keyword, buf);
}

void ASTSettingsProfileElement::deserializeImpl(ReadBuffer & buf)
{
    readBinary(parent_profile, buf);
    readBinary(setting_name, buf);
    readFieldBinary(value, buf);
    readFieldBinary(min_value, buf);
    readFieldBinary(max_value, buf);
    bool has_readonly;
    readBinary(has_readonly, buf);
    if (has_readonly)
    {
        bool read_tmp;
        readBinary(read_tmp, buf);
        readonly = read_tmp;
    }
    readBinary(id_mode, buf);
    readBinary(use_inherit_keyword, buf);
}

ASTPtr ASTSettingsProfileElement::deserialize(ReadBuffer & buf)
{
    auto element = std::make_shared<ASTSettingsProfileElement>();
    element->deserializeImpl(buf);
    return element;
}

bool ASTSettingsProfileElements::empty() const
{
    for (const auto & element : elements)
        if (!element->empty())
            return false;
    return true;
}


void ASTSettingsProfileElements::formatImpl(const FormatSettings & settings, FormatState &, FormatStateStacked) const
{
    if (empty())
    {
        settings.ostr << (settings.hilite ? IAST::hilite_keyword : "") << "NONE" << (settings.hilite ? IAST::hilite_none : "");
        return;
    }

    bool need_comma = false;
    for (const auto & element : elements)
    {
        if (need_comma)
            settings.ostr << ", ";
        need_comma = true;

        element->format(settings);
    }
}


void ASTSettingsProfileElements::setUseInheritKeyword(bool use_inherit_keyword_)
{
    for (auto & element : elements)
        element->use_inherit_keyword = use_inherit_keyword_;
}

void ASTSettingsProfileElements::serialize(WriteBuffer & buf) const
{
    writeBinary(elements.size(), buf);
    for (auto & element : elements)
        element->serialize(buf);
}

void ASTSettingsProfileElements::deserializeImpl(ReadBuffer & buf)
{
    size_t size;
    readBinary(size, buf);
    elements.resize(size);
    for (size_t i = 0; i < size; ++i)
        elements[i]->deserializeImpl(buf);
}

ASTPtr ASTSettingsProfileElements::deserialize(ReadBuffer & buf)
{
    auto element = std::make_shared<ASTSettingsProfileElements>();
    element->deserializeImpl(buf);
    return element;
}

}
