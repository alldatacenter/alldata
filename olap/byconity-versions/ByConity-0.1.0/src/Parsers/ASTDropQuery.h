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

#include <Parsers/ASTQueryWithTableAndOutput.h>
#include <Parsers/ASTQueryWithOnCluster.h>


namespace DB
{

/** DROP query
  */
class ASTDropQuery : public ASTQueryWithTableAndOutput, public ASTQueryWithOnCluster
{
public:
    enum Kind
    {
        Drop,
        Detach,
        Truncate,
    };

    Kind kind;
    bool if_exists{false};

    /// Useful if we already have a DDL lock
    bool no_ddl_lock{false};

    /// We dropping dictionary, so print correct word
    bool is_dictionary{false};

    /// Same as above
    bool is_view{false};

    bool no_delay{false};

    // We detach the object permanently, so it will not be reattached back during server restart.
    bool permanently{false};

    /** Get the text that identifies this element. */
    String getID(char) const override;
    ASTPtr clone() const override;

    ASTType getType() const override { return ASTType::ASTDropQuery; }

    ASTPtr getRewrittenASTWithoutOnCluster(const std::string & new_database) const override
    {
        return removeOnCluster<ASTDropQuery>(clone(), new_database);
    }

protected:
    void formatQueryImpl(const FormatSettings & settings, FormatState &, FormatStateStacked) const override;
};

}
