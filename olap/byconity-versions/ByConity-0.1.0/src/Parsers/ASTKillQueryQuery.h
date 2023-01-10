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
#include <Parsers/ASTQueryWithOutput.h>
#include <Parsers/ASTQueryWithOnCluster.h>

namespace DB
{

class ASTKillQueryQuery : public ASTQueryWithOutput, public ASTQueryWithOnCluster
{
public:
    enum class Type
    {
        Query,      /// KILL QUERY
        Mutation,   /// KILL MUTATION
    };

    Type type = Type::Query;
    ASTPtr where_expression;    // expression to filter processes from system.processes table
    bool sync = false;          // SYNC or ASYNC mode
    bool test = false;          // does it TEST mode? (doesn't cancel queries just checks and shows them)

    ASTPtr clone() const override
    {
        auto clone = std::make_shared<ASTKillQueryQuery>(*this);
        if (where_expression)
        {
            clone->where_expression = where_expression->clone();
            clone->children = {clone->where_expression};
        }

        return clone;
    }

    String getID(char) const override;

    ASTType getType() const override { return ASTType::ASTKillQueryQuery; }

    void formatQueryImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const override;

    ASTPtr getRewrittenASTWithoutOnCluster(const std::string &) const override
    {
        return removeOnCluster<ASTKillQueryQuery>(clone());
    }
};

}
