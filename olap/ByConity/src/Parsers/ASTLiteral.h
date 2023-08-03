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

#include <Core/Field.h>
#include <Parsers/ASTWithAlias.h>
#include <Parsers/TokenIterator.h>
#include <Common/FieldVisitorDump.h>

#include <optional>


namespace DB
{

/// Literal (atomic) - number, string, NULL
class ASTLiteral : public ASTWithAlias
{
public:
    explicit ASTLiteral(Field && value_) : value(value_) {}
    explicit ASTLiteral(const Field & value_) : value(value_) {}

    Field value;

    /// For ConstantExpressionTemplate
    std::optional<TokenIterator> begin;
    std::optional<TokenIterator> end;

    /*
     * The name of the column corresponding to this literal. Only used to
     * disambiguate the literal columns with the same display name that are
     * created at the expression analyzer stage. In the future, we might want to
     * have a full separation between display names and column identifiers. For
     * now, this field is effectively just some private EA data.
     */
    String unique_column_name;

    /// For compatibility reasons in distributed queries,
    /// we may need to use legacy column name for tuple literal.
    bool use_legacy_column_name_of_tuple = false;

    /** Get the text that identifies this element. */
    String getID(char delim) const override { return "Literal" + (delim + applyVisitor(FieldVisitorDump(), value)); }

    ASTType getType() const override { return ASTType::ASTLiteral; }

    ASTPtr clone() const override { return std::make_shared<ASTLiteral>(*this); }

    void updateTreeHashImpl(SipHash & hash_state) const override;

    void serialize(WriteBuffer & buf) const override;
    void deserializeImpl(ReadBuffer & buf) override;
    static ASTPtr deserialize(ReadBuffer & buf);

protected:
    void formatImplWithoutAlias(const FormatSettings & settings, FormatState &, FormatStateStacked) const override;

    void appendColumnNameImpl(WriteBuffer & ostr) const override;

private:
    /// Legacy version of 'appendColumnNameImpl'. It differs only with tuple literals.
    /// It's only needed to continue working of queries with tuple literals
    /// in distributed tables while rolling update.
    void appendColumnNameImplLegacy(WriteBuffer & ostr) const;
};

}
