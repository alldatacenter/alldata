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

#include <Analyzers/ASTEquals.h>
#include <Parsers/ASTFunction.h>
#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTLiteral.h>
#include <Parsers/ASTOrderByElement.h>
#include <Parsers/ASTSelectQuery.h>
#include <Parsers/ASTSubquery.h>
#include <Parsers/ASTTablesInSelectQuery.h>
#include <Parsers/ASTWindowDefinition.h>
#include <Parsers/ASTSetQuery.h>
#include <Parsers/ASTTableColumnReference.h>
#include <Common/SipHash.h>

namespace DB::ASTEquality
{

bool compareNode(const ASTFunction & left, const ASTFunction & right, const SubtreeComparator & comparator)
{
    return left.name == right.name && left.window_name == right.window_name
        && compareTree(left.window_definition, right.window_definition, comparator);
}

bool compareNode(const ASTLiteral & left, const ASTLiteral & right)
{
    return left.value == right.value;
}

bool compareNode(const ASTIdentifier & left, const ASTIdentifier & right)
{
    return left.name() == right.name();
}

bool compareNode(const ASTWindowDefinition & left, const ASTWindowDefinition & right, const SubtreeComparator & comparator)
{
    return left.parent_window_name == right.parent_window_name &&
        left.frame_type == right.frame_type &&
        left.frame_begin_type == right.frame_begin_type &&
        compareTree(left.frame_begin_offset, right.frame_begin_offset, comparator) &&
        left.frame_begin_preceding == right.frame_begin_preceding &&
        left.frame_end_type == right.frame_end_type&&
        compareTree(left.frame_end_offset, right.frame_end_offset, comparator) &&
        left.frame_end_preceding == right.frame_end_preceding;
}

bool compareNode(const ASTSubquery & left, const ASTSubquery & right)
{
    return left.cte_name == right.cte_name && left.database_of_view == right.database_of_view;
}

bool compareNode(const ASTArrayJoin & left, const ASTArrayJoin & right)
{
    return left.kind == right.kind;
}

bool compareNode(const ASTOrderByElement & left, const ASTOrderByElement & right, const SubtreeComparator & comparator) // NOLINT(misc-no-recursion)
{
    return left.direction == right.direction && left.nulls_direction == right.nulls_direction
        && left.nulls_direction_was_explicitly_specified == right.nulls_direction_was_explicitly_specified
        && left.with_fill == right.with_fill && compareTree(left.collation, right.collation, comparator)
        && compareTree(left.fill_from, right.fill_from, comparator) && compareTree(left.fill_to, right.fill_to, comparator)
        && compareTree(left.fill_step, right.fill_step, comparator);
}

bool compareNode(const ASTSetQuery & left, const ASTSetQuery & right)
{
    return left.changes == right.changes;
}

bool compareNode(const ASTTableColumnReference & left, const ASTTableColumnReference & right)
{
    return left.storage == right.storage && left.column_name == right.column_name;
}


bool compareTree(const ASTPtr & left, const ASTPtr & right, const SubtreeComparator & comparator)
{
    // step 1. special cases
    if (left == right)
        return true;

    if (!left || !right)
        return false;

    // step 2. compare tree with extra strategy
    std::optional<bool> result = comparator(left, right);

    if (result.has_value())
        return *result;

    // step 3. compare current root
    if (left->getType() != right->getType())
        return false;

    bool node_equals;

    switch (left->getType())
    {
        case ASTType::ASTLiteral:
            node_equals = compareNode(left->as<ASTLiteral &>(), right->as<ASTLiteral &>());
            break;
        case ASTType::ASTFunction:
            node_equals = compareNode(left->as<ASTFunction &>(), right->as<ASTFunction &>(), comparator);
            break;
        case ASTType::ASTIdentifier:
            node_equals = compareNode(left->as<ASTIdentifier &>(), right->as<ASTIdentifier &>());
            break;
        case ASTType::ASTWindowDefinition:
            node_equals = compareNode(left->as<ASTWindowDefinition&>(), right->as<ASTWindowDefinition&>(), comparator);
            break;
        case ASTType::ASTSubquery:
            node_equals = compareNode(left->as<ASTSubquery&>(), right->as<ASTSubquery&>());
            break;
        case ASTType::ASTArrayJoin:
            node_equals = compareNode(left->as<ASTArrayJoin&>(), right->as<ASTArrayJoin&>());
            break;
        case ASTType::ASTOrderByElement:
            node_equals = compareNode(left->as<ASTOrderByElement&>(), right->as<ASTOrderByElement&>(), comparator);
            break;
        case ASTType::ASTSetQuery:
            node_equals = compareNode(left->as<ASTSetQuery &>(), right->as<ASTSetQuery &>());
            break;
        case ASTType::ASTTableColumnReference:
            node_equals = compareNode(left->as<ASTTableColumnReference &>(), right->as<ASTTableColumnReference &>());
            break;
        default:
            node_equals = true;
    }

    if (!node_equals)
        return false;

    // step 4. compare children
    if (left->children.size() != right->children.size())
        return false;

    // for ASTSelectQuery, we also check if children with same index are same clause
    if (left->getType() == ASTType::ASTSelectQuery)
    {
        const auto & left_query = left->as<ASTSelectQuery &>();
        const auto & right_query = right->as<ASTSelectQuery &>();

        if (left_query.getExpressionTypes() != right_query.getExpressionTypes())
            return false;
    }

    for (int i = 0; i < static_cast<int>(left->children.size()); ++i)
        if (!compareTree(left->children[i], right->children[i], comparator))
            return false;

    return true;
}

void hashTreeImpl(const ASTPtr & ast, SipHash & hash_state, const SubtreeHasher & hasher)
{
    if (!ast)
        return;

    auto result = hasher(ast);

    if (result.has_value())
    {
        hash_state.update(*result);
        return;
    }

    hash_state.update(ast->getID());
    hash_state.update(ast->children.size());

    for (auto & child: ast->children)
    {
        hashTreeImpl(child, hash_state, hasher);
    }
}

size_t hashTree(const ASTPtr & ast, const SubtreeHasher & hasher)
{
    SipHash hash_state;
    hashTreeImpl(ast, hash_state, hasher);
    return static_cast<size_t>(hash_state.get64());
}

}
