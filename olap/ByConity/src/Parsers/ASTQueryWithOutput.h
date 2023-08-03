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
#include <IO/Operators.h>


namespace DB
{

/** Query with output options
  * (supporting [INTO OUTFILE 'file_name'] [FORMAT format_name] [SETTINGS key1 = value1, key2 = value2, ...] suffix).
  */
class ASTQueryWithOutput : public IAST
{
public:
    ASTPtr out_file;
    ASTPtr format;
    ASTPtr settings_ast;

    void formatImpl(const FormatSettings & s, FormatState & state, FormatStateStacked frame) const final;

    ASTType getType() const override { return ASTType::ASTQueryWithOutput; }

    /// Remove 'FORMAT <fmt> and INTO OUTFILE <file>' if exists
    static bool resetOutputASTIfExist(IAST & ast);

    void serialize(WriteBuffer & buf) const override;
    void deserializeImpl(ReadBuffer & buf) override;

protected:
    /// NOTE: call this helper at the end of the clone() method of descendant class.
    void cloneOutputOptions(ASTQueryWithOutput & cloned) const;

    /// Format only the query part of the AST (without output options).
    virtual void formatQueryImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const = 0;
};


/** Helper template for simple queries like SHOW PROCESSLIST.
  */
template <typename ASTIDAndQueryNames>
class ASTQueryWithOutputImpl : public ASTQueryWithOutput
{
public:
    String getID(char) const override { return ASTIDAndQueryNames::ID; }

    ASTPtr clone() const override
    {
        auto res = std::make_shared<ASTQueryWithOutputImpl<ASTIDAndQueryNames>>(*this);
        res->children.clear();
        cloneOutputOptions(*res);
        return res;
    }

protected:
    void formatQueryImpl(const FormatSettings & settings, FormatState &, FormatStateStacked) const override
    {
        settings.ostr << (settings.hilite ? hilite_keyword : "")
            << ASTIDAndQueryNames::Query << (settings.hilite ? hilite_none : "");
    }
};

}
