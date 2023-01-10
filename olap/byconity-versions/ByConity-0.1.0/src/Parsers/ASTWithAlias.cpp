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

#include <Parsers/ASTWithAlias.h>
#include <IO/WriteHelpers.h>
#include <IO/ReadHelpers.h>
#include <IO/Operators.h>


namespace DB
{

static void writeAlias(const String & name, const ASTWithAlias::FormatSettings & settings)
{
    settings.ostr << (settings.hilite ? IAST::hilite_keyword : "") << " AS " << (settings.hilite ? IAST::hilite_alias : "");
    settings.writeIdentifier(name);
    settings.ostr << (settings.hilite ? IAST::hilite_none : "");
}


void ASTWithAlias::formatImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const
{
    /// If we have previously output this node elsewhere in the query, now it is enough to output only the alias.
    /// This is needed because the query can become extraordinary large after substitution of aliases.
    if (!alias.empty() && !settings.without_alias && !state.printed_asts_with_alias.emplace(frame.current_select, alias, getTreeHash()).second)
    {
        settings.writeIdentifier(alias);
    }
    else
    {
        /// If there is an alias, then parentheses are required around the entire expression, including the alias.
        /// Because a record of the form `0 AS x + 0` is syntactically invalid.
        if (frame.need_parens && !alias.empty() && !settings.without_alias)
            settings.ostr << '(';

        formatImplWithoutAlias(settings, state, frame);

        if (!alias.empty() && !settings.without_alias)
        {
            writeAlias(alias, settings);
            if (frame.need_parens)
                settings.ostr << ')';
        }
    }
}

void ASTWithAlias::appendColumnName(WriteBuffer & ostr) const
{
    if (prefer_alias_to_column_name && !alias.empty())
        writeString(alias, ostr);
    else
        appendColumnNameImpl(ostr);
}

void ASTWithAlias::appendColumnNameWithoutAlias(WriteBuffer & ostr) const
{
    appendColumnNameImpl(ostr);
}

void ASTWithAlias::serialize(WriteBuffer & buf) const
{
    writeBinary(alias, buf);
    writeBinary(prefer_alias_to_column_name, buf);
}

void ASTWithAlias::deserializeImpl(ReadBuffer & buf)
{
    readBinary(alias, buf);
    readBinary(prefer_alias_to_column_name, buf);
}

}
