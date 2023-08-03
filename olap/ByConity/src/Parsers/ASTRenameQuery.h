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
#include <Common/quoteString.h>
#include <IO/Operators.h>


namespace DB
{

/** RENAME query
  */
class ASTRenameQuery : public ASTQueryWithOutput, public ASTQueryWithOnCluster
{
public:
    struct Table
    {
        String database;
        String table;
    };

    struct Element
    {
        Table from;
        Table to;
    };

    using Elements = std::vector<Element>;
    Elements elements;

    bool exchange{false};   /// For EXCHANGE TABLES
    bool database{false};   /// For RENAME DATABASE
    bool dictionary{false};   /// For RENAME DICTIONARY

    /** Get the text that identifies this element. */
    String getID(char) const override { return "Rename"; }

    ASTType getType() const override { return ASTType::ASTRenameQuery; }

    ASTPtr clone() const override
    {
        auto res = std::make_shared<ASTRenameQuery>(*this);
        cloneOutputOptions(*res);
        return res;
    }

    ASTPtr getRewrittenASTWithoutOnCluster(const std::string & new_database) const override
    {
        auto query_ptr = clone();
        auto & query = query_ptr->as<ASTRenameQuery &>();

        query.cluster.clear();
        for (Element & elem : query.elements)
        {
            if (elem.from.database.empty())
                elem.from.database = new_database;
            if (elem.to.database.empty())
                elem.to.database = new_database;
        }

        return query_ptr;
    }

protected:
    void formatQueryImpl(const FormatSettings & settings, FormatState &, FormatStateStacked) const override
    {
        if (database)
        {
            settings.ostr << (settings.hilite ? hilite_keyword : "") << "RENAME DATABASE " << (settings.hilite ? hilite_none : "");
            settings.ostr << backQuoteIfNeed(elements.at(0).from.database);
            settings.ostr << (settings.hilite ? hilite_keyword : "") << " TO " << (settings.hilite ? hilite_none : "");
            settings.ostr << backQuoteIfNeed(elements.at(0).to.database);
            formatOnCluster(settings);
            return;
        }

        settings.ostr << (settings.hilite ? hilite_keyword : "");
        if (exchange && dictionary)
            settings.ostr << "EXCHANGE DICTIONARIES ";
        else if (exchange)
            settings.ostr << "EXCHANGE TABLES ";
        else if (dictionary)
            settings.ostr << "RENAME DICTIONARY ";
        else
            settings.ostr << "RENAME TABLE ";

        settings.ostr << (settings.hilite ? hilite_none : "");

        for (auto it = elements.cbegin(); it != elements.cend(); ++it)
        {
            if (it != elements.cbegin())
                settings.ostr << ", ";

            settings.ostr << (!it->from.database.empty() ? backQuoteIfNeed(it->from.database) + "." : "") << backQuoteIfNeed(it->from.table)
                << (settings.hilite ? hilite_keyword : "") << (exchange ? " AND " : " TO ") << (settings.hilite ? hilite_none : "")
                << (!it->to.database.empty() ? backQuoteIfNeed(it->to.database) + "." : "") << backQuoteIfNeed(it->to.table);
        }

        formatOnCluster(settings);
    }
};

}
