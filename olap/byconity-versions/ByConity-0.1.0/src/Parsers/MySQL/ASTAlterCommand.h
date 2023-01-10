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

#include <Parsers/ASTExpressionList.h>
#include <Parsers/IAST.h>
#include <Parsers/MySQL/ASTDeclareColumn.h>
#include <Parsers/MySQL/ASTDeclareIndex.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int NOT_IMPLEMENTED;
}

namespace MySQLParser
{

class ASTAlterCommand : public IAST
{
public:
    enum Type
    {
        ADD_INDEX,
        ADD_COLUMN,

        DROP_INDEX,
        DROP_CHECK,
        DROP_COLUMN,
        DROP_COLUMN_DEFAULT,

        RENAME_INDEX,
        RENAME_COLUMN,
        RENAME_TABLE,

        MODIFY_CHECK,
        MODIFY_COLUMN,
        MODIFY_INDEX_VISIBLE,
        MODIFY_COLUMN_DEFAULT,
        MODIFY_PROPERTIES,

        ORDER_BY,

        NO_TYPE
    };

    Type type = NO_TYPE;

    /// For ADD INDEX
    ASTDeclareIndex * index_decl = nullptr;

    /// For modify default expression
    IAST * default_expression = nullptr;

    /// For ADD COLUMN
    ASTExpressionList * additional_columns = nullptr;
    /// For ORDER BY
    ASTExpressionList * order_by_columns = nullptr;

    bool first = false;
    bool index_visible = false;
    bool not_check_enforced = false;

    String old_name;
    String index_type;
    String index_name;
    String column_name;
    String constraint_name;
    String new_database_name;
    String new_table_name;

    IAST * properties = nullptr;

    ASTPtr clone() const override;

    String getID(char delim) const override { return "AlterCommand" + (delim + std::to_string(static_cast<int>(type))); }

    ASTType getType() const override { return ASTType::ASTAlterCommand; }

protected:
    void formatImpl(const FormatSettings & /*settings*/, FormatState & /*state*/, FormatStateStacked /*frame*/) const override
    {
        throw Exception("Method formatImpl is not supported by MySQLParser::ASTAlterCommand.", ErrorCodes::NOT_IMPLEMENTED);
    }
};

class ParserAlterCommand : public IParserBase
{
protected:
    const char * getName() const override { return "alter command"; }

    bool parseImpl(Pos & pos, ASTPtr & node, Expected & expected) override;
};

}

}
