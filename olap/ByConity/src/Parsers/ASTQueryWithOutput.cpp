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

#include <Parsers/ASTQueryWithOutput.h>
#include <Parsers/ASTSerDerHelper.h>
#include <IO/ReadHelpers.h>
#include <IO/WriteHelpers.h>
namespace DB
{

void ASTQueryWithOutput::cloneOutputOptions(ASTQueryWithOutput & cloned) const
{
    if (out_file)
    {
        cloned.out_file = out_file->clone();
        cloned.children.push_back(cloned.out_file);
    }
    if (format)
    {
        cloned.format = format->clone();
        cloned.children.push_back(cloned.format);
    }
    if (settings_ast)
    {
        cloned.settings_ast = settings_ast->clone();
        cloned.children.push_back(cloned.settings_ast);
    }
}

void ASTQueryWithOutput::formatImpl(const FormatSettings & s, FormatState & state, FormatStateStacked frame) const
{
    formatQueryImpl(s, state, frame);

    std::string indent_str = s.one_line ? "" : std::string(4u * frame.indent, ' ');

    if (out_file)
    {
        s.ostr << (s.hilite ? hilite_keyword : "") << s.nl_or_ws << indent_str << "INTO OUTFILE " << (s.hilite ? hilite_none : "");
        out_file->formatImpl(s, state, frame);
    }

    if (format)
    {
        s.ostr << (s.hilite ? hilite_keyword : "") << s.nl_or_ws << indent_str << "FORMAT " << (s.hilite ? hilite_none : "");
        format->formatImpl(s, state, frame);
    }

    if (settings_ast)
    {
        s.ostr << (s.hilite ? hilite_keyword : "") << s.nl_or_ws << indent_str << "SETTINGS " << (s.hilite ? hilite_none : "");
        settings_ast->formatImpl(s, state, frame);
    }
}

bool ASTQueryWithOutput::resetOutputASTIfExist(IAST & ast)
{
    /// FIXME: try to prettify this cast using `as<>()`
    if (auto * ast_with_output = dynamic_cast<ASTQueryWithOutput *>(&ast))
    {
        ast_with_output->format.reset();
        ast_with_output->out_file.reset();
        ast_with_output->settings_ast.reset();
        return true;
    }

    return false;
}

void ASTQueryWithOutput::serialize(WriteBuffer & buf) const
{
    serializeAST(out_file, buf);
    serializeAST(format, buf);
    serializeAST(settings_ast, buf);
}

void ASTQueryWithOutput::deserializeImpl(ReadBuffer & buf)
{
    out_file = deserializeASTWithChildren(children, buf);
    format = deserializeASTWithChildren(children, buf);
    settings_ast = deserializeASTWithChildren(children, buf);
}

}
