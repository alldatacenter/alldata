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

#include <Parsers/ASTSubquery.h>
#include <Parsers/ASTSerDerHelper.h>
#include <IO/WriteHelpers.h>
#include <IO/ReadHelpers.h>
#include <IO/Operators.h>
#include <Common/SipHash.h>

namespace DB
{

void ASTSubquery::appendColumnNameImpl(WriteBuffer & ostr) const
{
    /// This is a hack. We use alias, if available, because otherwise tree could change during analysis.
    if (!alias.empty())
    {
        writeString(alias, ostr);
    }
    else if (!cte_name.empty())
    {
        writeString(cte_name, ostr);
    }
    else
    {
        Hash hash = getTreeHash();
        writeCString("__subquery_", ostr);
        writeText(hash.first, ostr);
        ostr.write('_');
        writeText(hash.second, ostr);
    }
}

void ASTSubquery::formatImplWithoutAlias(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const
{
    /// NOTE: due to trickery of filling cte_name (in interpreters) it is hard
    /// to print it w/o newline (for !oneline case), since if nl_or_ws
    /// prepended here, then formatting will be incorrect with alias:
    ///
    ///   (select 1 in ((select 1) as sub))
    if (!cte_name.empty())
    {
        settings.ostr << (settings.hilite ? hilite_identifier : "");
        settings.writeIdentifier(cte_name);
        settings.ostr << (settings.hilite ? hilite_none : "");
        return;
    }

    std::string indent_str = settings.one_line ? "" : std::string(4u * frame.indent, ' ');
    std::string nl_or_nothing = settings.one_line ? "" : "\n";

    settings.ostr << "(" << nl_or_nothing;
    FormatStateStacked frame_nested = frame;
    frame_nested.need_parens = false;
    ++frame_nested.indent;
    children[0]->formatImpl(settings, state, frame_nested);
    settings.ostr << nl_or_nothing << indent_str << ")";
}

void ASTSubquery::updateTreeHashImpl(SipHash & hash_state) const
{
    if (!cte_name.empty())
        hash_state.update(cte_name);
    IAST::updateTreeHashImpl(hash_state);
}

void ASTSubquery::serialize(WriteBuffer & buf) const
{
    ASTWithAlias::serialize(buf);

    writeBinary(cte_name, buf);
    serializeASTs(children, buf);
}

void ASTSubquery::deserializeImpl(ReadBuffer & buf)
{
    ASTWithAlias::deserializeImpl(buf);

    readBinary(cte_name, buf);
    children = deserializeASTs(buf);
}

ASTPtr ASTSubquery::deserialize(ReadBuffer & buf)
{
    auto subquery = std::make_shared<ASTSubquery>();
    subquery->deserializeImpl(buf);
    return subquery;
}

}

