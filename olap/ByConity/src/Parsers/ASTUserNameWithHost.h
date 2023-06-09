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

#include <Parsers/IParser.h>


namespace DB
{

/** Represents a user name.
  * It can be a simple string or identifier or something like `name@host`.
  * In the last case `host` specifies the hosts user is allowed to connect from.
  * The `host` can be an ip address, ip subnet, or a host name.
  * The % and _ wildcard characters are permitted in `host`.
  * These have the same meaning as for pattern-matching operations performed with the LIKE operator.
  */
class ASTUserNameWithHost : public IAST
{
public:
    String base_name;
    String host_pattern;

    String toString() const;
    void concatParts();

    ASTUserNameWithHost() = default;
    ASTUserNameWithHost(const String & name_) : base_name(name_) {}
    String getID(char) const override { return "UserNameWithHost"; }

    ASTType getType() const override { return ASTType::ASTUserNameWithHost; }

    ASTPtr clone() const override { return std::make_shared<ASTUserNameWithHost>(*this); }
    void formatImpl(const FormatSettings & settings, FormatState &, FormatStateStacked) const override;
};


class ASTUserNamesWithHost : public IAST
{
public:
    std::vector<std::shared_ptr<ASTUserNameWithHost>> names;

    size_t size() const { return names.size(); }
    auto begin() const { return names.begin(); }
    auto end() const { return names.end(); }
    auto front() const { return *begin(); }
    void push_back(const String & name_) { names.push_back(std::make_shared<ASTUserNameWithHost>(name_)); }

    Strings toStrings() const;
    void concatParts();
    bool getHostPatternIfCommon(String & out_common_host_pattern) const;

    String getID(char) const override { return "UserNamesWithHost"; }

    ASTType getType() const override { return ASTType::ASTUserNamesWithHost; }

    ASTPtr clone() const override { return std::make_shared<ASTUserNamesWithHost>(*this); }
    void formatImpl(const FormatSettings & settings, FormatState &, FormatStateStacked) const override;
};

}
