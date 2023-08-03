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
#include <Parsers/ASTFunctionWithKeyValueArguments.h>
#include <Parsers/ASTLiteral.h>
#include <Parsers/ASTExpressionList.h>

#include <Parsers/ASTSetQuery.h>

#include <Parsers/ParserSetQuery.h>

namespace DB
{

/// AST for external dictionary lifetime:
/// lifetime(min 10 max 100)
class ASTDictionaryLifetime : public IAST
{
public:
    UInt64 min_sec = 0;
    UInt64 max_sec = 0;

    String getID(char) const override { return "Dictionary lifetime"; }

    ASTType getType() const override { return ASTType::ASTDictionaryLifetime; }

    ASTPtr clone() const override;

    void formatImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const override;
};

/// AST for external dictionary layout. Has name and contain single parameter
/// layout(type()) or layout(type(param value))
class ASTDictionaryLayout : public IAST
{
    using KeyValue = std::pair<std::string, ASTLiteral *>;
public:
    /// flat, cache, hashed, etc.
    String layout_type;
    /// parameters (size_in_cells, ...)
    /// ASTExpressionList -> ASTPair -> (ASTLiteral key, ASTLiteral value).
    ASTExpressionList * parameters;
    /// has brackets after layout type
    bool has_brackets = true;

    String getID(char) const override { return "Dictionary layout"; }

    ASTType getType() const override { return ASTType::ASTDictionaryLayout; }

    ASTPtr clone() const override;

    void formatImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const override;
};


/// AST for external range-hashed dictionary
/// Range bounded with two attributes from minimum to maximum
/// RANGE(min attr1 max attr2)
class ASTDictionaryRange : public IAST
{
public:
    String min_attr_name;
    String max_attr_name;

    String getID(char) const override { return "Dictionary range"; }

    ASTType getType() const override { return ASTType::ASTDictionaryRange; }

    ASTPtr clone() const override;

    void formatImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const override;
};

class ASTDictionarySettings : public IAST
{
public:
    SettingsChanges changes;

    String getID(char) const override { return "Dictionary settings"; }

    ASTType getType() const override { return ASTType::ASTDictionarySettings; }

    ASTPtr clone() const override;

    void formatImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const override;
};


/// AST contains all parts of external dictionary definition except attributes
class ASTDictionary : public IAST
{
public:
    /// Dictionary keys -- one or more
    ASTExpressionList * primary_key = nullptr;
    /// Dictionary external source, doesn't have own AST, because
    /// source parameters absolutely different for different sources
    ASTFunctionWithKeyValueArguments * source = nullptr;

    /// Lifetime of dictionary (required part)
    ASTDictionaryLifetime * lifetime = nullptr;
    /// Layout of dictionary (required part)
    ASTDictionaryLayout * layout = nullptr;
    /// Range for dictionary (only for range-hashed dictionaries)
    ASTDictionaryRange * range = nullptr;
    /// Settings for dictionary (optionally)
    ASTDictionarySettings * dict_settings = nullptr;

    String getID(char) const override { return "Dictionary definition"; }

    ASTType getType() const override { return ASTType::ASTDictionary; }

    ASTPtr clone() const override;

    void formatImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const override;
};

}
