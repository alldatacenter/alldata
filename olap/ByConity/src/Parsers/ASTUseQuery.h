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
#include <Common/quoteString.h>
#include <IO/Operators.h>


namespace DB
{


/** USE query
  */
class ASTUseQuery : public IAST
{
public:
    String database;

    /** Get the text that identifies this element. */
    String getID(char delim) const override { return "UseQuery" + (delim + database); }

    ASTType getType() const override { return ASTType::ASTUseQuery; }

    ASTPtr clone() const override { return std::make_shared<ASTUseQuery>(*this); }

protected:
    void formatImpl(const FormatSettings & settings, FormatState &, FormatStateStacked) const override
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "") << "USE " << (settings.hilite ? hilite_none : "") << backQuoteIfNeed(database);
        return;
    }
};

}
