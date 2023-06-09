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
#include <Parsers/ASTQueryWithOnCluster.h>
#include <Access/RowPolicy.h>


namespace DB
{
/** Represents a row policy's name in one of the following forms:
  * short_name ON [db.]table_name [ON CLUSTER 'cluster_name']
  * short_name [ON CLUSTER 'cluster_name'] ON [db.]table_name
  */
class ASTRowPolicyName : public IAST, public ASTQueryWithOnCluster
{
public:
    RowPolicy::NameParts name_parts;
    String toString() const { return name_parts.getName(); }

    String getID(char) const override { return "RowPolicyName"; }

    ASTType getType() const override { return ASTType::ASTRowPolicyName; }

    ASTPtr clone() const override { return std::make_shared<ASTRowPolicyName>(*this); }
    void formatImpl(const FormatSettings & settings, FormatState &, FormatStateStacked) const override;
    ASTPtr getRewrittenASTWithoutOnCluster(const std::string &) const override { return removeOnCluster<ASTRowPolicyName>(clone()); }

    void replaceEmptyDatabase(const String & current_database);
};


/** Represents multiple names of row policies, comma-separated, in one of the following forms:
  * short_name1 ON [db1.]table_name1 [, short_name2 ON [db2.]table_name2 ...] [ON CLUSTER 'cluster_name']
  * short_name1 [, short_name2 ...] ON [db.]table_name [ON CLUSTER 'cluster_name']
  * short_name1 [, short_name2 ...] [ON CLUSTER 'cluster_name'] ON [db.]table_name
  * short_name ON [db1.]table_name1 [, [db2.]table_name2 ...] [ON CLUSTER 'cluster_name']
  * short_name [ON CLUSTER 'cluster_name'] ON [db1.]table_name1 [, [db2.]table_name2 ...]
  */
class ASTRowPolicyNames : public IAST, public ASTQueryWithOnCluster
{
public:
    std::vector<RowPolicy::NameParts> name_parts;
    Strings toStrings() const;

    String getID(char) const override { return "RowPolicyNames"; }

    ASTType getType() const override { return ASTType::ASTRowPolicyNames; }

    ASTPtr clone() const override { return std::make_shared<ASTRowPolicyNames>(*this); }
    void formatImpl(const FormatSettings & settings, FormatState &, FormatStateStacked) const override;
    ASTPtr getRewrittenASTWithoutOnCluster(const std::string &) const override { return removeOnCluster<ASTRowPolicyNames>(clone()); }

    void replaceEmptyDatabase(const String & current_database);
};
}
