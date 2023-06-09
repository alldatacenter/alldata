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

namespace DB
{

enum class PartToolType
{
    NOTYPE = 0,
    WRITER = 0,
    MERGER = 1,
    CONVERTER = 2
};

class ASTPartToolKit : public IAST
{
public:
    ASTPtr data_format;
    ASTPtr create_query;
    ASTPtr source_path;
    ASTPtr target_path;
    ASTPtr settings;

    PartToolType type = PartToolType::NOTYPE;

    String getID(char) const override;

    ASTType getType() const override { return ASTType::ASTPartToolKit; }

    ASTPtr clone() const override;

protected:
    void formatImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const override;
};

}
