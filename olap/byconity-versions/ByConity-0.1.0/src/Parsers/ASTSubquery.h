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


/** SELECT subquery
  */
class ASTSubquery : public ASTWithAlias
{
public:
    // Stored the name when the subquery is defined in WITH clause. For example:
    // WITH (SELECT 1) AS a SELECT * FROM a AS b; cte_name will be `a`.
    std::string cte_name;

    // Stored the database name when the subquery is defined by a view. For example:
    // CREATE VIEW db1.v1 AS SELECT number FROM system.numbers LIMIT 10;
    // SELECT * FROM v1; database_of_view will be `db1`; cte_name will be `v1`.
    std::string database_of_view;
    /** Get the text that identifies this element. */
    String getID(char) const override { return "Subquery"; }

    ASTType getType() const override { return ASTType::ASTSubquery; }

    ASTPtr clone() const override
    {
        const auto res = std::make_shared<ASTSubquery>(*this);
        ASTPtr ptr{res};

        res->children.clear();

        for (const auto & child : children)
            res->children.emplace_back(child->clone());

        return ptr;
    }

    void updateTreeHashImpl(SipHash & hash_state) const override;

    bool isWithClause() const { return !cte_name.empty(); }

    void serialize(WriteBuffer & buf) const override;
    void deserializeImpl(ReadBuffer & buf) override;
    static ASTPtr deserialize(ReadBuffer & buf);

protected:
    void formatImplWithoutAlias(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const override;
    void appendColumnNameImpl(WriteBuffer & ostr) const override;
};

}
