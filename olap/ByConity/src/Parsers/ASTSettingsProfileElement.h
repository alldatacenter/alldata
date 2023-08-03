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

#pragma once

#include <Parsers/IAST.h>
#include <Core/Field.h>


namespace DB
{
/** Represents a settings profile's element like the following
  * {variable [= value] [MIN [=] min_value] [MAX [=] max_value] [READONLY|WRITABLE]} | PROFILE 'profile_name'
  */
class ASTSettingsProfileElement : public IAST
{
public:
    String parent_profile;
    String setting_name;
    Field value;
    Field min_value;
    Field max_value;
    std::optional<bool> readonly;
    bool id_mode = false;  /// If true then `parent_profile` keeps UUID, not a name.
    bool use_inherit_keyword = false;  /// If true then this element is a part of ASTCreateSettingsProfileQuery.

    bool empty() const { return parent_profile.empty() && setting_name.empty(); }

    String getID(char) const override { return "SettingsProfileElement"; }

    ASTType getType() const override { return ASTType::ASTSettingsProfileElement; }

    ASTPtr clone() const override { return std::make_shared<ASTSettingsProfileElement>(*this); }
    void formatImpl(const FormatSettings & settings, FormatState &, FormatStateStacked) const override;

    void serialize(WriteBuffer & buf) const override;
    void deserializeImpl(ReadBuffer & buf) override;
    static ASTPtr deserialize(ReadBuffer & buf);
};


/** Represents settings profile's elements like the following
  * {{variable [= value] [MIN [=] min_value] [MAX [=] max_value] [READONLY|WRITABLE]} | PROFILE 'profile_name'} [,...]
  */
class ASTSettingsProfileElements : public IAST
{
public:
    std::vector<std::shared_ptr<ASTSettingsProfileElement>> elements;

    bool empty() const;

    String getID(char) const override { return "SettingsProfileElements"; }

    ASTType getType() const override { return ASTType::ASTSettingsProfileElements; }

    ASTPtr clone() const override { return std::make_shared<ASTSettingsProfileElements>(*this); }
    void formatImpl(const FormatSettings & settings, FormatState &, FormatStateStacked) const override;

    void setUseInheritKeyword(bool use_inherit_keyword_);

    void serialize(WriteBuffer & buf) const override;
    void deserializeImpl(ReadBuffer & buf) override;
    static ASTPtr deserialize(ReadBuffer & buf);
};
}
