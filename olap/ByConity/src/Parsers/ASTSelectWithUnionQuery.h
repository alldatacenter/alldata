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

#include <Parsers/ASTQueryWithOutput.h>


namespace DB
{
/** Single SELECT query or multiple SELECT queries with UNION
 * or UNION or UNION DISTINCT
  */
class ASTSelectWithUnionQuery : public ASTQueryWithOutput
{
public:
    String getID(char) const override { return "SelectWithUnionQuery"; }

    ASTType getType() const override { return ASTType::ASTSelectWithUnionQuery; }

    ASTPtr clone() const override;

    void formatQueryImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const override;

    void collectAllTables(std::vector<ASTPtr> &, bool &) const;

    void resetTEALimit();

    enum class Mode
    {
        Unspecified,
        ALL,
        DISTINCT,
        EXCEPT_UNSPECIFIED,
        EXCEPT_ALL,
        EXCEPT_DISTINCT,
        INTERSECT_UNSPECIFIED,
        INTERSECT_ALL,
        INTERSECT_DISTINCT
    };

    using UnionModes = std::vector<Mode>;
    using UnionModesSet = std::unordered_set<Mode>;

    Mode union_mode;

    UnionModes list_of_modes;

    bool is_normalized = false;

    ASTPtr list_of_selects;

    // special info for TEA LIMIT post stage processing
    ASTPtr tealimit;

    UnionModesSet set_of_modes;

    /// Consider any mode other than ALL as non-default.
    bool hasNonDefaultUnionMode() const;

    void serialize(WriteBuffer & buf) const override;
    void deserializeImpl(ReadBuffer & buf) override;
    static ASTPtr deserialize(ReadBuffer & buf);
};

}
