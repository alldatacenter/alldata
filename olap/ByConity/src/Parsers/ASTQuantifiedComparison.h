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

#include <Parsers/ASTExpressionList.h>
#include <Parsers/ASTWithAlias.h>

namespace DB
{
//AST For Quantified Comparison, for example, '> all', '= all'
class ASTQuantifiedComparison : public ASTWithAlias
{

public:
    enum class QuantifierType
    {
        ANY,
        ALL,
        SOME
    };
    String comparator;
    QuantifierType quantifier_type;
    String getID(char delim) const override;
    ASTType getType() const override { return ASTType::ASTQuantifiedComparison; }
    ASTPtr clone() const override;
    void serialize(WriteBuffer & buf) const override;
    void deserializeImpl(ReadBuffer & buf) override;

protected:
    void formatImplWithoutAlias(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const override;
    void appendColumnNameImpl(WriteBuffer & ostr) const override;
};

using QuantifierType = ASTQuantifiedComparison::QuantifierType;

template <typename... Args>
std::shared_ptr<ASTQuantifiedComparison> makeASTQuantifiedComparison(const String & comparator, QuantifierType & quantifier_type, Args &&... args)
{
    const auto quantified_comparison = std::make_shared<ASTQuantifiedComparison>();

    quantified_comparison->comparator = comparator;
    quantified_comparison->quantifier_type = quantifier_type;
    quantified_comparison->children = {std::forward<Args>(args)...};
    return quantified_comparison;
}

}
