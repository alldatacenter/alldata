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

#include <Parsers/ASTWithAlias.h>


namespace DB
{

/// Parameter in query with name and type of substitution ({name:type}).
/// Example: SELECT * FROM table WHERE id = {pid:UInt16}.
class ASTQueryParameter : public ASTWithAlias
{
public:
    String name;
    String type;

    ASTQueryParameter(const String & name_, const String & type_) : name(name_), type(type_) {}

    /** Get the text that identifies this element. */
    String getID(char delim) const override { return String("QueryParameter") + delim + name + ':' + type; }

    ASTType getType() const override { return ASTType::ASTQueryParameter; }

    ASTPtr clone() const override { return std::make_shared<ASTQueryParameter>(*this); }

protected:
    void formatImplWithoutAlias(const FormatSettings & settings, FormatState &, FormatStateStacked) const override;
    void appendColumnNameImpl(WriteBuffer & ostr) const override;
};

}
