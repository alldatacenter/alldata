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

#include <Parsers/ASTTEALimit.h>
#include <IO/WriteHelpers.h>
#include <IO/Operators.h>

namespace DB
{
#define CLONE(member) if (member) { res->member = member->clone(); res->children.push_back(res->member); }

    ASTPtr ASTTEALimit::clone() const
    {
        auto res = std::make_shared<ASTTEALimit>(*this);
        res->children.clear();
        CLONE(limit_value)
        CLONE(limit_offset)
        CLONE(group_expr_list)
        CLONE(order_expr_list)
        return res;
    }
#undef CLONE

    String ASTTEALimit::getID(char) const
    {
        return "TEALIMIT";
    }

    void ASTTEALimit::formatImpl(const FormatSettings & s,
                                 FormatState & state,
                                 FormatStateStacked frame) const
    {
        s.ostr << (s.hilite ? hilite_keyword : "") << "TEALIMIT ";
        if (limit_value)
        {
            if (limit_offset)
            {
                limit_offset->formatImpl(s, state, frame);
                s.ostr << ", ";
            }

            limit_value->formatImpl(s, state, frame);
        }

        if (group_expr_list)
        {
            s.ostr << (s.hilite ? hilite_keyword : "") << " GROUP ";
            group_expr_list->formatImpl(s, state, frame);
        }

        if (order_expr_list)
        {
            s.ostr << (s.hilite ? hilite_keyword : "") << " ORDER ";
            order_expr_list->formatImpl(s, state, frame);
        }
    }
}
//  namespace DB
