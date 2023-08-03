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

#include <Analyzers/ResolvedWindow.h>
#include <Analyzers/tryEvaluateConstantExpression.h>
#include <Interpreters/Context_fwd.h>

namespace DB
{

ResolvedWindowPtr resolveWindow(const ASTPtr & node,
                                const std::unordered_map<String, ResolvedWindowPtr> & registered_windows,
                                ContextPtr context)
{
    const auto & definition = node->as<const ASTWindowDefinition &>();

    auto resolved_window = std::make_shared<ResolvedWindow>();
    resolved_window->origin_ast = &definition;

    if (!definition.parent_window_name.empty())
    {
        auto it = registered_windows.find(definition.parent_window_name);
        if (it == registered_windows.end())
        {
            throw Exception(ErrorCodes::BAD_ARGUMENTS,
                            "Window definition '{}' references an unknown window '{}'",
                            definition.formatForErrorMessage(),
                            definition.parent_window_name);
        }

        const auto & parent = it->second;
        resolved_window->partition_by = parent->partition_by;
        resolved_window->order_by = parent->order_by;
        resolved_window->frame = parent->frame;

        // If an existing_window_name is specified it must refer to an earlier
        // entry in the WINDOW list; the new window copies its partitioning clause
        // from that entry, as well as its ordering clause if any. In this case
        // the new window cannot specify its own PARTITION BY clause, and it can
        // specify ORDER BY only if the copied window does not have one. The new
        // window always uses its own frame clause; the copied window must not
        // specify a frame clause.
        // -- https://www.postgresql.org/docs/current/sql-select.html
        if (definition.partition_by)
        {
            throw Exception(ErrorCodes::BAD_ARGUMENTS,
                            "Derived window definition '{}' is not allowed to override PARTITION BY",
                            definition.formatForErrorMessage());
        }

        if (definition.order_by && !parent->order_by)
        {
            throw Exception(ErrorCodes::BAD_ARGUMENTS,
                            "Derived window definition '{}' is not allowed to override a non-empty ORDER BY",
                            definition.formatForErrorMessage());
        }

        if (!parent->frame.is_default)
        {
            throw Exception(ErrorCodes::BAD_ARGUMENTS,
                            "Parent window '{}' is not allowed to define a frame: while processing derived window definition '{}'",
                            definition.parent_window_name,
                            definition.formatForErrorMessage());
        }
    }

    if (definition.partition_by)
        resolved_window->partition_by = definition.partition_by;

    if (definition.order_by)
        resolved_window->order_by = definition.order_by;

    if (definition.frame_type != WindowFrame::FrameType::Rows
        && definition.frame_type != WindowFrame::FrameType::Range)
    {
        throw Exception(ErrorCodes::NOT_IMPLEMENTED,
                        "Window frame '{}' is not implemented (while processing '{}')",
                        WindowFrame::toString(definition.frame_type),
                        definition.formatForErrorMessage());
    }

    resolved_window->frame.is_default = definition.frame_is_default;
    resolved_window->frame.type = definition.frame_type;
    resolved_window->frame.begin_type = definition.frame_begin_type;
    resolved_window->frame.begin_preceding = definition.frame_begin_preceding;
    resolved_window->frame.end_type = definition.frame_end_type;
    resolved_window->frame.end_preceding = definition.frame_end_preceding;

    if (definition.frame_end_type == WindowFrame::BoundaryType::Offset)
    {
        auto value = tryEvaluateConstantExpression(definition.frame_end_offset, context);
        if (!value)
        {
            throw Exception(ErrorCodes::BAD_ARGUMENTS, "Window boundary offset should be a constant.");
        }
        resolved_window->frame.end_offset = *value;
    }

    if (definition.frame_begin_type == WindowFrame::BoundaryType::Offset)
    {
        auto value = tryEvaluateConstantExpression(definition.frame_begin_offset, context);
        if (!value)
        {
            throw Exception(ErrorCodes::BAD_ARGUMENTS, "Window boundary offset should be a constant.");
        }
        resolved_window->frame.begin_offset = *value;
    }

    return resolved_window;
}

}
