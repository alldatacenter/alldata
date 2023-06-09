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
#include <common/types.h>

namespace DB
{

/// Pair with name and value in lisp programming langugate style. It contain
/// string as key, but value either can be literal or list of
/// pairs.
class ASTPair : public IAST
{
public:
    /// Name or key of pair
    String first;
    /// Value of pair, which can be also list of pairs
    IAST * second = nullptr;
    /// Value is closed in brackets (HOST '127.0.0.1')
    bool second_with_brackets;

public:
    explicit ASTPair(bool second_with_brackets_)
        : second_with_brackets(second_with_brackets_)
    {
    }

    String getID(char delim) const override;

    ASTType getType() const override { return ASTType::ASTPair; }

    ASTPtr clone() const override;

    void formatImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const override;

    void updateTreeHashImpl(SipHash & hash_state) const override;
};


/// Function with key-value arguments is a function which arguments consist of
/// pairs (see above). For example:
///                                    ->Pair with list of pairs as value<-
/// SOURCE(USER 'clickhouse' PORT 9000 REPLICA(HOST '127.0.0.1' PRIORITY 1) TABLE 'some_table')
class ASTFunctionWithKeyValueArguments : public IAST
{
public:
    /// Name of function
    String name;
    /// Expression list
    ASTPtr elements;
    /// Has brackets around arguments
    bool has_brackets;

    explicit ASTFunctionWithKeyValueArguments(bool has_brackets_ = true)
        : has_brackets(has_brackets_)
    {
    }

public:
    String getID(char delim) const override;

    ASTType getType() const override { return ASTType::ASTFunctionWithKeyValueArguments; }

    ASTPtr clone() const override;

    void formatImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const override;

    void updateTreeHashImpl(SipHash & hash_state) const override;

    void serialize(WriteBuffer & buf) const override;
    void deserializeImpl(ReadBuffer & buf) override;
    static ASTPtr deserialize(ReadBuffer & buf);
};

}
