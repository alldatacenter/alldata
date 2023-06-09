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


namespace re2
{
    class RE2;
}


namespace DB
{

class WriteBuffer;

namespace ErrorCodes
{
}

struct AsteriskSemantic;
struct AsteriskSemanticImpl;


/** SELECT COLUMNS('regexp') is expanded to multiple columns like * (asterisk).
  * Optional transformers can be attached to further manipulate these expanded columns.
  */
class ASTColumnsMatcher : public IAST
{
public:
    String getID(char) const override { return "ColumnsMatcher"; }
    ASTPtr clone() const override;

    ASTType getType() const override { return ASTType::ASTColumnsMatcher; }

    void appendColumnName(WriteBuffer & ostr) const override;
    void setPattern(String pattern);
    bool isColumnMatching(const String & column_name) const;
    void updateTreeHashImpl(SipHash & hash_state) const override;

    ASTPtr column_list;

protected:
    void formatImpl(const FormatSettings & settings, FormatState &, FormatStateStacked) const override;

private:
    std::shared_ptr<re2::RE2> column_matcher;
    String original_pattern;
    std::shared_ptr<AsteriskSemanticImpl> semantic; /// pimpl

    friend struct AsteriskSemantic;
};


}
