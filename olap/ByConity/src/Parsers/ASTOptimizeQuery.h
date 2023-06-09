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
#include <Parsers/ASTQueryWithTableAndOutput.h>
#include <Parsers/ASTQueryWithOnCluster.h>

namespace DB
{


/** OPTIMIZE query
  */
class ASTOptimizeQuery : public ASTQueryWithTableAndOutput, public ASTQueryWithOnCluster
{
public:
    /// The partition to optimize can be specified.
    ASTPtr partition;
    /// A flag can be specified - perform optimization "to the end" instead of one step.
    bool final = false;
    /// Do deduplicate (default: false)
    bool deduplicate = false;
    /// Deduplicate by columns.
    ASTPtr deduplicate_by_columns;
    /// take a try
    bool enable_try = false;

    /** Get the text that identifies this element. */
    String getID(char delim) const override
    {
        return "OptimizeQuery" + (delim + database) + delim + table + (final ? "_final" : "") + (deduplicate ? "_deduplicate" : "");
    }

    ASTType getType() const override { return ASTType::ASTOptimizeQuery; }

    ASTPtr clone() const override
    {
        auto res = std::make_shared<ASTOptimizeQuery>(*this);
        res->children.clear();

        if (partition)
        {
            res->partition = partition->clone();
            res->children.push_back(res->partition);
        }

        if (deduplicate_by_columns)
        {
            res->deduplicate_by_columns = deduplicate_by_columns->clone();
            res->children.push_back(res->deduplicate_by_columns);
        }

        return res;
    }

    void formatQueryImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const override;

    ASTPtr getRewrittenASTWithoutOnCluster(const std::string &new_database) const override
    {
        return removeOnCluster<ASTOptimizeQuery>(clone(), new_database);
    }
};

}
