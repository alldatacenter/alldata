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

#include <Interpreters/predicateExpressionsUtils.h>
#include <Common/TypePromotion.h>
#include <Parsers/ASTFunction.h>

#include <unordered_set>


namespace DB
{
    void decompose(const ASTPtr & predicate, ASTs & result, const String & func_name)
    {
        auto * function = predicate->as<ASTFunction>();
        if (function && function->name == func_name && function->children.size() == 1)
        {
            if (auto * expression_list = function->children[0]->as<ASTExpressionList>())
            {
                for (auto & child : expression_list->children)
                {
                    decompose(child, result, func_name);
                }
            }
        }
        else
            result.push_back(predicate);
    }

    ASTPtr compose(const ASTs & predicates, const String & func_name)
    {
        switch (predicates.size())
        {
            case 0:
                return nullptr;
            case 1:
                return predicates[0];
            default:
                const auto function = std::make_shared<ASTFunction>();
                function->name = func_name;
                function->arguments = std::make_shared<ASTExpressionList>();
                function->children.push_back(function->arguments);
                for (auto & p : predicates)
                {
                    function->arguments->children.push_back(p);
                }
                return function;
        }
    }

    ASTs toDNFs(ASTs predicates)
    {
        ASTs result;
        for (auto & p : predicates)
        {
            auto dnf = toDNF(p);
            if (auto * function = dnf->as<ASTFunction>())
            {
                if (function->name == "or")
                {
                    const auto & dnfs = decomposeOr(dnf);
                    result.insert(result.end(), dnfs.begin(), dnfs.end());
                }
                else
                    result.push_back(dnf);
            }
            else
                result.push_back(dnf);
        }
        return result;
    }

    ASTPtr toDNF(const ASTPtr & predicate)
    {
        if (auto * function = predicate->as<ASTFunction>())
        {
            if (function->name == "and")
            {
                const auto & predicates = decomposeAnd(predicate);
                auto & head = predicates[0];
                auto headDNF = toDNF(head);
                const auto & headDNFs = decomposeOr(headDNF);

                ASTs tail_predicates(predicates.begin() + 1, predicates.end());
                auto tail = composeAnd(tail_predicates);
                auto tailDNF = toDNF(tail);
                const auto & tailDNFs = decomposeOr(tailDNF);

                ASTs asts;
                for (auto & h : headDNFs)
                {
                    for (auto & t : tailDNFs)
                    {
                        asts.push_back(composeAnd(ASTs{h, t}));
                    }
                }
                return composeOr(asts);
            }
            else if (function->name == "or")
            {
                const auto & predicates = decomposeOr(predicate);
                const auto & dnfs = toDNFs(predicates);
                return composeOr(dnfs);
            }
            else if (function->name == "xor")
            {
                const auto & predicates = decomposeXor(predicate);
                auto & head = predicates[0];
                auto not_head = negate(head);

                ASTs tail_predicates(predicates.begin() + 1, predicates.end());
                auto tail = composeXor(tail_predicates);
                auto not_tail = negate(tail);

                auto equivalent = composeOr(ASTs{composeAnd(ASTs{not_head, tail}), composeAnd(ASTs{head, not_tail})});
                return toDNF(equivalent);
            }
            else if (function->name == "not")
            {
                auto & internal_predicate = function->arguments->children.at(0);
                if (auto * internal_function = internal_predicate->as<ASTFunction>())
                {
                    if (internal_function->name == "and")
                    {
                        const auto & predicates = decomposeAnd(internal_predicate);
                        ASTs not_predicates;
                        for (auto & p : predicates)
                        {
                            not_predicates.push_back(negate(p));
                        }
                        return toDNF(composeOr(not_predicates));
                    }
                    else if (internal_function->name == "or")
                    {
                        const auto & predicates = decomposeOr(internal_predicate);
                        ASTs not_predicates;
                        for (auto & p : predicates)
                        {
                            not_predicates.push_back(negate(p));
                        }
                        return toDNF(composeAnd(not_predicates));
                    }
                    else if (internal_function->name == "xor")
                    {
                        const auto & predicates = decomposeXor(internal_predicate);
                        auto & head = predicates[0];
                        auto not_head = negate(head);

                        ASTs tail_predicates(predicates.begin() + 1, predicates.end());
                        auto tail = composeXor(tail_predicates);
                        auto not_tail = negate(tail);

                        auto equivalent = composeOr(ASTs{composeAnd(ASTs{head, tail}), composeAnd(ASTs{not_head, not_tail})});
                        return toDNF(equivalent);
                    }
                    else if (internal_function->name == "not")
                        return toDNF(internal_function->arguments->children.at(0));
                    else
                        return predicate;
                }
                else
                    return predicate;
            }
            else
                return predicate;
        }
        else
            return predicate;
    }

    ASTs decomposeAnd(const ASTPtr & predicate)
    {
        ASTs result;
        decompose(predicate, result, "and");
        return result;
    }

    ASTs decomposeOr(const ASTPtr & predicate)
    {
        ASTs result;
        decompose(predicate, result, "or");
        return result;
    }

    ASTs decomposeXor(const ASTPtr & predicate)
    {
        ASTs result;
        decompose(predicate, result, "xor");
        return result;
    }

    ASTPtr composeAnd(const ASTs & predicates)
    {
        return compose(predicates, "and");
    }

    ASTPtr composeOr(const ASTs & predicates)
    {
        return compose(predicates, "or");
    }

    ASTPtr composeXor(const ASTs & predicates)
    {
        return compose(predicates, "xor");
    }

    ASTPtr negate(const ASTPtr & predicate)
    {
        return makeASTFunction("not", predicate);
    }
}
