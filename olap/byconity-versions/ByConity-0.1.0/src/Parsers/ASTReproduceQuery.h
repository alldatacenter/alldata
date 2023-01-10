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

#include <Parsers/IAST.h>
#include <Parsers/ASTQueryWithOutput.h>
namespace DB
{
/**
 * Query Dump certain information for SELECT xxx, such as ddl, statistics, settings_changed and so on.
**/
class ASTReproduceQuery : public ASTQueryWithOutput
{
public:
    String reproduce_path;
    String getID(char) const override { return "ReproduceQuery"; }

    ASTType getType() const override { return ASTType::ASTReproduceQuery; }

    ASTPtr clone() const override;

protected:
    void formatQueryImpl(const FormatSettings & settings, FormatState &, FormatStateStacked) const override;
};
}
