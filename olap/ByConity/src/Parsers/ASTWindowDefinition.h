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

#include <Interpreters/WindowDescription.h>

#include <Parsers/IAST.h>


namespace DB
{

struct ASTWindowDefinition : public IAST
{
    std::string parent_window_name;

    ASTPtr partition_by;

    ASTPtr order_by;

    bool frame_is_default = true;
    WindowFrame::FrameType frame_type = WindowFrame::FrameType::Range;
    WindowFrame::BoundaryType frame_begin_type = WindowFrame::BoundaryType::Unbounded;
    ASTPtr frame_begin_offset;
    bool frame_begin_preceding = true;
    WindowFrame::BoundaryType frame_end_type = WindowFrame::BoundaryType::Current;
    ASTPtr frame_end_offset;
    bool frame_end_preceding = false;

    ASTPtr clone() const override;

    String getID(char delimiter) const override;

    ASTType getType() const override { return ASTType::ASTWindowDefinition; }

    void formatImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const override;

    std::string getDefaultWindowName() const;
};

struct ASTWindowListElement : public IAST
{
    String name;

    // ASTWindowDefinition
    ASTPtr definition;

    ASTType getType() const override { return ASTType::ASTWindowListElement; }

    ASTPtr clone() const override;

    String getID(char delimiter) const override;

    void formatImpl(const FormatSettings & settings, FormatState & state, FormatStateStacked frame) const override;
};

}
