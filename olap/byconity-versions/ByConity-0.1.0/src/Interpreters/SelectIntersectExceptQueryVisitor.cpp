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

#include <Interpreters/SelectIntersectExceptQueryVisitor.h>
#include <Parsers/ASTExpressionList.h>
#include <Parsers/ASTSelectWithUnionQuery.h>
#include <Common/typeid_cast.h>


namespace DB
{
namespace ErrorCodes
{
    extern const int LOGICAL_ERROR;
    extern const int EXPECTED_ALL_OR_DISTINCT;
}

/*
 * Note: there is a difference between intersect and except behaviour.
 * `intersect` is supposed to be a part of the last SelectQuery, i.e. the sequence with no parenthesis:
 * select 1 union all select 2 except select 1 intersect 2 except select 2 union distinct select 5;
 * is interpreted as:
 * select 1 union all select 2 except (select 1 intersect 2) except select 2 union distinct select 5;
 * Whereas `except` is applied to all left union part like:
 * (((select 1 union all select 2) except (select 1 intersect 2)) except select 2) union distinct select 5;
**/

void SelectIntersectExceptQueryMatcher::visit(ASTPtr & ast, Data & data)
{
    if (auto * select_union = ast->as<ASTSelectWithUnionQuery>())
        visit(*select_union, data);
}

void SelectIntersectExceptQueryMatcher::visit(ASTSelectWithUnionQuery & ast, Data & data)
{
    using SelectUnionMode = ASTSelectWithUnionQuery::Mode;
    using SelectUnionModes = ASTSelectWithUnionQuery::UnionModes;

    const auto & union_modes = ast.list_of_modes;

    if (ast.is_normalized || union_modes.empty())
        return;

    auto selects = std::move(ast.list_of_selects->children);

    if (union_modes.size() + 1 != selects.size())
        throw Exception(ErrorCodes::LOGICAL_ERROR, "Incorrect ASTSelectWithUnionQuery (modes: {}, selects: {})",
                        union_modes.size(), selects.size());

    std::reverse(selects.begin(), selects.end());

    ASTs children = {selects.back()};
    selects.pop_back();
    SelectUnionModes modes;

    for (auto mode : union_modes)
    {
        if (mode == SelectUnionMode::INTERSECT_UNSPECIFIED)
        {
            if (data.intersect_default_mode == UnionMode::ALL)
                mode = SelectUnionMode::INTERSECT_ALL;
            else if (data.intersect_default_mode == UnionMode::DISTINCT)
                mode = SelectUnionMode::INTERSECT_DISTINCT;
            else
                throw Exception(
                    "Expected ALL or DISTINCT in INTERSECT query, because setting (intersect_default_mode) is empty",
                    DB::ErrorCodes::EXPECTED_ALL_OR_DISTINCT);
        }

        if (mode == SelectUnionMode::EXCEPT_UNSPECIFIED)
        {
            if (data.except_default_mode == UnionMode::ALL)
                mode = SelectUnionMode::EXCEPT_ALL;
            else if (data.except_default_mode == UnionMode::DISTINCT)
                mode = SelectUnionMode::EXCEPT_DISTINCT;
            else
                throw Exception(
                    "Expected ALL or DISTINCT in EXCEPT query, because setting (except_default_mode) is empty",
                    DB::ErrorCodes::EXPECTED_ALL_OR_DISTINCT);
        }

        auto union_mode_to_final_operator = [](SelectUnionMode md) -> ASTSelectIntersectExceptQuery::Operator
        {
            switch (md)
            {
                case SelectUnionMode::EXCEPT_ALL:
                    return ASTSelectIntersectExceptQuery::Operator::EXCEPT_ALL;
                case SelectUnionMode::EXCEPT_DISTINCT:
                    return ASTSelectIntersectExceptQuery::Operator::EXCEPT_DISTINCT;
                case SelectUnionMode::INTERSECT_ALL:
                    return ASTSelectIntersectExceptQuery::Operator::INTERSECT_ALL;
                case SelectUnionMode::INTERSECT_DISTINCT:
                    return ASTSelectIntersectExceptQuery::Operator::INTERSECT_DISTINCT;
                default:
                    throw Exception("logical error", DB::ErrorCodes::LOGICAL_ERROR);
            }
        };

        switch (mode)
        {
            case SelectUnionMode::EXCEPT_ALL:
            case SelectUnionMode::EXCEPT_DISTINCT:
            {
                auto left = std::make_shared<ASTSelectWithUnionQuery>();
                left->union_mode = SelectUnionMode::ALL;

                left->list_of_selects = std::make_shared<ASTExpressionList>();
                left->children.push_back(left->list_of_selects);
                left->list_of_selects->children = std::move(children);

                left->list_of_modes = std::move(modes);
                modes = {};

                auto right = selects.back();
                selects.pop_back();

                auto except_node = std::make_shared<ASTSelectIntersectExceptQuery>();
                except_node->final_operator = union_mode_to_final_operator(mode);
                except_node->children = {left, right};

                children = {except_node};
                break;
            }
            case SelectUnionMode::INTERSECT_ALL:
            case SelectUnionMode::INTERSECT_DISTINCT:
            {
                bool from_except = false;
                const auto * except_ast = typeid_cast<const ASTSelectIntersectExceptQuery *>(children.back().get());
                if (except_ast && (except_ast->final_operator == ASTSelectIntersectExceptQuery::Operator::EXCEPT_ALL
                                   || except_ast->final_operator == ASTSelectIntersectExceptQuery::Operator::EXCEPT_DISTINCT))
                    from_except = true;

                ASTPtr left;
                if (from_except)
                {
                    left = std::move(children.back()->children[1]);
                }
                else
                {
                    left = children.back();
                    children.pop_back();
                }

                auto right = selects.back();
                selects.pop_back();

                auto intersect_node = std::make_shared<ASTSelectIntersectExceptQuery>();
                intersect_node->final_operator = union_mode_to_final_operator(mode);
                intersect_node->children = {left, right};

                if (from_except)
                    children.back()->children[1] = std::move(intersect_node);
                else
                    children.push_back(std::move(intersect_node));

                break;
            }
            default:
            {
                auto right = selects.back();
                selects.pop_back();
                children.emplace_back(std::move(right));
                modes.push_back(mode);
                break;
            }
        }
    }

    if (!selects.empty())
    {
        auto right = selects.back();
        selects.pop_back();
        children.emplace_back(std::move(right));
    }

    ast.union_mode = SelectUnionMode::Unspecified;
    ast.list_of_selects->children = std::move(children);
    ast.list_of_modes = std::move(modes);
}

}
