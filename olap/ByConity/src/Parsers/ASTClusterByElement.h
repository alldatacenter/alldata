/*
 * Copyright (2022) Bytedance Ltd. and/or its affiliates
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#pragma once

#include <Parsers/IAST.h>


namespace DB
{
/** Element of expression: INTO <TOTAL_BUCKET_NUMBER> SPLIT_NUMBER <SPLIT_NUMBER> WITH_RANGE
  */
class ASTClusterByElement : public IAST
{
public:

    Int64 split_number;
    bool is_with_range;

    ASTClusterByElement() = default;

    ASTClusterByElement(ASTPtr columns_elem, ASTPtr total_bucket_number_elem, Int64 split_number_, bool is_with_range_)
        : split_number(split_number_), is_with_range(is_with_range_)
    {
        children.push_back(columns_elem);
        children.push_back(total_bucket_number_elem);
    }

    const ASTPtr & getColumns() const { return children.front(); }
    const ASTPtr & getTotalBucketNumber() const { return children.back(); }

    String getID(char) const override { return "ClusterByElement"; }
    ASTPtr clone() const override;

protected:
    void formatImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const override;
};
}
