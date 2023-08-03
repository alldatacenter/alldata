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
#include <Interpreters/StorageID.h>

namespace DB
{


/** INSERT query
  */
class ASTInsertQuery : public IAST
{
public:
    StorageID table_id = StorageID::createEmpty();
    ASTPtr columns;
    String format;
    ASTPtr select;
    ASTPtr watch;
    ASTPtr table_function;
    ASTPtr in_file;
    ASTPtr settings_ast;

    /// Data to insert
    const char * data = nullptr;
    const char * end = nullptr;

    /// Query has additional data, which will be sent later
    bool has_tail = false;

    /// Try to find table function input() in SELECT part
    void tryFindInputFunction(ASTPtr & input_function) const;

    /** Get the text that identifies this element. */
    String getID(char delim) const override { return "InsertQuery" + (delim + table_id.database_name) + delim + table_id.table_name; }

    ASTType getType() const override { return ASTType::ASTInsertQuery; }

    ASTPtr clone() const override
    {
        auto res = std::make_shared<ASTInsertQuery>(*this);
        res->children.clear();

        if (columns) { res->columns = columns->clone(); res->children.push_back(res->columns); }
        if (select) { res->select = select->clone(); res->children.push_back(res->select); }
        if (watch) { res->watch = watch->clone(); res->children.push_back(res->watch); }
        if (table_function) { res->table_function = table_function->clone(); res->children.push_back(res->table_function); }
        if (settings_ast) { res->settings_ast = settings_ast->clone(); res->children.push_back(res->settings_ast); }

        if (in_file)
        {
            res->in_file = in_file->clone(); res->children.push_back(res->in_file);
        }

        return res;
    }

protected:
    void formatImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const override;
};

}
