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


namespace DB
{

class WriteBuffer;

/** Takes a syntax tree and turns it back into text.
  * In case of INSERT query, the data will be missing.
  */
void formatAST(const IAST & ast, WriteBuffer & buf, bool hilite = true, bool one_line = false, bool always_quote_identifiers = false);

String serializeAST(const IAST & ast, bool one_line = true);

inline WriteBuffer & operator<<(WriteBuffer & buf, const IAST & ast)
{
    formatAST(ast, buf, false, true);
    return buf;
}

inline WriteBuffer & operator<<(WriteBuffer & buf, const ASTPtr & ast)
{
    formatAST(*ast, buf, false, true);
    return buf;
}

}

template<>
struct fmt::formatter<DB::ASTPtr>
{
    template<typename ParseContext>
    constexpr auto parse(ParseContext & context)
    {
        return context.begin();
    }

    template<typename FormatContext>
    auto format(const DB::ASTPtr & ast, FormatContext & context)
    {
        return fmt::format_to(context.out(), "{}", DB::serializeAST(*ast));
    }
};

