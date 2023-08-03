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

namespace re2
{
    class RE2;
}

namespace DB
{
class IASTColumnsTransformer : public IAST
{
public:
    virtual void transform(ASTs & nodes) const = 0;
    static void transform(const ASTPtr & transformer, ASTs & nodes);
};

class ASTColumnsApplyTransformer : public IASTColumnsTransformer
{
public:
    String getID(char) const override { return "ColumnsApplyTransformer"; }

    ASTType getType() const override { return ASTType::ASTColumnsApplyTransformer; }

    ASTPtr clone() const override
    {
        auto res = std::make_shared<ASTColumnsApplyTransformer>(*this);
        if (parameters)
            res->parameters = parameters->clone();
        return res;
    }
    void transform(ASTs & nodes) const override;
    String func_name;
    String column_name_prefix;
    ASTPtr parameters;

protected:
    void formatImpl(const FormatSettings & settings, FormatState &, FormatStateStacked) const override;
};

class ASTColumnsExceptTransformer : public IASTColumnsTransformer
{
public:
    bool is_strict = false;
    String getID(char) const override { return "ColumnsExceptTransformer"; }

    ASTType getType() const override { return ASTType::ASTColumnsExceptTransformer; }

    ASTPtr clone() const override
    {
        auto clone = std::make_shared<ASTColumnsExceptTransformer>(*this);
        clone->cloneChildren();
        return clone;
    }
    void transform(ASTs & nodes) const override;
    void setPattern(String pattern);
    bool isColumnMatching(const String & column_name) const;

protected:
    void formatImpl(const FormatSettings & settings, FormatState &, FormatStateStacked) const override;
    std::shared_ptr<re2::RE2> column_matcher;
    String original_pattern;
};

class ASTColumnsReplaceTransformer : public IASTColumnsTransformer
{
public:
    class Replacement : public IAST
    {
    public:
        String getID(char) const override { return "ColumnsReplaceTransformer::Replacement"; }

        ASTPtr clone() const override
        {
            auto replacement = std::make_shared<Replacement>(*this);
            replacement->children.clear();
            replacement->expr = expr->clone();
            replacement->children.push_back(replacement->expr);
            return replacement;
        }

        String name;
        ASTPtr expr;

    protected:
        void formatImpl(const FormatSettings & settings, FormatState &, FormatStateStacked) const override;
    };

    bool is_strict = false;
    String getID(char) const override { return "ColumnsReplaceTransformer"; }

    ASTType getType() const override { return ASTType::ASTColumnsReplaceTransformer; }

    ASTPtr clone() const override
    {
        auto clone = std::make_shared<ASTColumnsReplaceTransformer>(*this);
        clone->cloneChildren();
        return clone;
    }
    void transform(ASTs & nodes) const override;

protected:
    void formatImpl(const FormatSettings & settings, FormatState &, FormatStateStacked) const override;

private:
    static void replaceChildren(ASTPtr & node, const ASTPtr & replacement, const String & name);
};

}
