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
#include <Core/Names.h>


namespace DB
{

struct ASTTablesInSelectQueryElement;
struct StorageID;


/** SELECT query
  */
class ASTSelectQuery : public IAST
{
public:
    enum class Expression : uint8_t
    {
        WITH,
        SELECT,
        TABLES,
        PREWHERE,
        IMPLICITWHERE,  /// only used in CnchHive
        WHERE,
        GROUP_BY,
        HAVING,
        WINDOW,
        ORDER_BY,
        LIMIT_BY_OFFSET,
        LIMIT_BY_LENGTH,
        LIMIT_BY,
        LIMIT_OFFSET,
        LIMIT_LENGTH,
        SETTINGS
    };

    /** Get the text that identifies this element. */
    String getID(char) const override { return "SelectQuery"; }

    ASTType getType() const override { return ASTType::ASTSelectQuery; }

    ASTPtr clone() const override;

    void collectAllTables(std::vector<ASTPtr> &, bool &) const;

    bool distinct = false;
    bool group_by_with_totals = false;
    bool group_by_with_rollup = false;
    bool group_by_with_cube = false;
    bool group_by_with_constant_keys = false;
    bool group_by_with_grouping_sets = false;
    bool limit_with_ties = false;

    ASTPtr & refSelect()    { return getExpression(Expression::SELECT); }
    ASTPtr & refTables()    { return getExpression(Expression::TABLES); }
    ASTPtr & refPrewhere()  { return getExpression(Expression::PREWHERE); }
    ASTPtr & refImplicitWhere() { return getExpression(Expression::IMPLICITWHERE); }
    ASTPtr & refWhere()     { return getExpression(Expression::WHERE); }
    ASTPtr & refHaving()    { return getExpression(Expression::HAVING); }
    ASTPtr & refLimitLength()   { return getExpression(Expression::LIMIT_LENGTH); }

    const ASTPtr with()           const { return getExpression(Expression::WITH); }
    const ASTPtr select()         const { return getExpression(Expression::SELECT); }
    const ASTPtr tables()         const { return getExpression(Expression::TABLES); }
    const ASTPtr prewhere()       const { return getExpression(Expression::PREWHERE); }
    const ASTPtr implicitWhere()  const { return getExpression(Expression::IMPLICITWHERE); }
    const ASTPtr where()          const { return getExpression(Expression::WHERE); }
    const ASTPtr groupBy()        const { return getExpression(Expression::GROUP_BY); }
    const ASTPtr having()         const { return getExpression(Expression::HAVING); }
    const ASTPtr window() const { return getExpression(Expression::WINDOW); }
    const ASTPtr orderBy()        const { return getExpression(Expression::ORDER_BY); }
    const ASTPtr limitByOffset()  const { return getExpression(Expression::LIMIT_BY_OFFSET); }
    const ASTPtr limitByLength()  const { return getExpression(Expression::LIMIT_BY_LENGTH); }
    const ASTPtr limitBy()        const { return getExpression(Expression::LIMIT_BY); }
    const ASTPtr limitOffset()    const { return getExpression(Expression::LIMIT_OFFSET); }
    const ASTPtr limitLength()    const { return getExpression(Expression::LIMIT_LENGTH); }
    const ASTPtr settings()       const { return getExpression(Expression::SETTINGS); }

    ASTPtr getWith()            { return getExpression(Expression::WITH, true); }
    ASTPtr getSelect()          { return getExpression(Expression::SELECT, true); }
    ASTPtr getTables()          { return getExpression(Expression::TABLES, true); }
    ASTPtr getPrewhere()        { return getExpression(Expression::PREWHERE, true); }
    ASTPtr getWhere()           { return getExpression(Expression::WHERE, true); }
    ASTPtr getGroupBy()         { return getExpression(Expression::GROUP_BY, true); }
    ASTPtr getHaving()          { return getExpression(Expression::HAVING, true); }
    ASTPtr getWindow()          { return getExpression(Expression::WINDOW, true); }
    ASTPtr getOrderBy()         { return getExpression(Expression::ORDER_BY, true); }
    ASTPtr getLimitByOffset()   { return getExpression(Expression::LIMIT_BY_OFFSET, true); }
    ASTPtr getLimitByLength()   { return getExpression(Expression::LIMIT_BY_LENGTH, true); }
    ASTPtr getLimitBy()         { return getExpression(Expression::LIMIT_BY, true); }
    ASTPtr getLimitOffset()     { return getExpression(Expression::LIMIT_OFFSET, true); }
    ASTPtr getLimitLength()     { return getExpression(Expression::LIMIT_LENGTH, true); }
    ASTPtr getSettings()        { return getExpression(Expression::SETTINGS, true); }

    /// Set/Reset/Remove expression.
    void setExpression(Expression expr, ASTPtr && ast);

    ASTPtr getExpression(Expression expr, bool clone = false) const
    {
        auto it = positions.find(expr);
        if (it != positions.end())
            return clone ? children[it->second]->clone() : children[it->second];
        return {};
    }

    /// Compatibility with old parser of tables list. TODO remove
    ASTPtr sampleSize() const;
    ASTPtr sampleOffset() const;
    ASTPtr arrayJoinExpressionList(bool & is_left) const;
    ASTPtr arrayJoinExpressionList() const;
    const ASTTablesInSelectQueryElement * join() const;
    bool final() const;
    bool withFill() const;
    void replaceDatabaseAndTable(const String & database_name, const String & table_name);
    void replaceDatabaseAndTable(const StorageID & table_id);
    void addTableFunction(ASTPtr & table_function_ptr);
    void updateTreeHashImpl(SipHash & hash_state) const override;

    void setFinal();

    void serialize(WriteBuffer & buf) const override;
    void deserializeImpl(ReadBuffer & buf) override;
    static ASTPtr deserialize(ReadBuffer & buf);

    std::vector<Expression> getExpressionTypes() const;
protected:
    void formatImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const override;

private:
    std::unordered_map<Expression, size_t> positions;

    ASTPtr & getExpression(Expression expr);
};

}
