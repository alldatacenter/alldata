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

#include <Common/ZooKeeper/ZooKeeperConstants.h>
#include <Common/ZooKeeper/IKeeper.h>
#include <unordered_set>

namespace Coordination
{

static const std::unordered_set<int32_t> VALID_OPERATIONS =
{
    static_cast<int32_t>(OpNum::Close),
    static_cast<int32_t>(OpNum::Error),
    static_cast<int32_t>(OpNum::Create),
    static_cast<int32_t>(OpNum::Remove),
    static_cast<int32_t>(OpNum::Exists),
    static_cast<int32_t>(OpNum::Get),
    static_cast<int32_t>(OpNum::Set),
    static_cast<int32_t>(OpNum::SimpleList),
    static_cast<int32_t>(OpNum::Sync),
    static_cast<int32_t>(OpNum::Heartbeat),
    static_cast<int32_t>(OpNum::List),
    static_cast<int32_t>(OpNum::Check),
    static_cast<int32_t>(OpNum::Multi),
    static_cast<int32_t>(OpNum::Auth),
    static_cast<int32_t>(OpNum::SessionID),
    static_cast<int32_t>(OpNum::SetACL),
    static_cast<int32_t>(OpNum::GetACL),
    static_cast<int32_t>(OpNum::FilteredList),
};

std::string toString(OpNum op_num)
{
    switch (op_num)
    {
        case OpNum::Close:
            return "Close";
        case OpNum::Error:
            return "Error";
        case OpNum::Create:
            return "Create";
        case OpNum::Remove:
            return "Remove";
        case OpNum::Exists:
            return "Exists";
        case OpNum::Get:
            return "Get";
        case OpNum::Set:
            return "Set";
        case OpNum::SimpleList:
            return "SimpleList";
        case OpNum::List:
            return "List";
        case OpNum::Check:
            return "Check";
        case OpNum::Multi:
            return "Multi";
        case OpNum::Sync:
            return "Sync";
        case OpNum::Heartbeat:
            return "Heartbeat";
        case OpNum::Auth:
            return "Auth";
        case OpNum::SessionID:
            return "SessionID";
        case OpNum::SetACL:
            return "SetACL";
        case OpNum::GetACL:
            return "GetACL";
        case OpNum::FilteredList:
            return "FilteredList";
    }
    int32_t raw_op = static_cast<int32_t>(op_num);
    throw Exception("Operation " + std::to_string(raw_op) + " is unknown", Error::ZUNIMPLEMENTED);
}

OpNum getOpNum(int32_t raw_op_num)
{
    if (!VALID_OPERATIONS.count(raw_op_num))
        throw Exception("Operation " + std::to_string(raw_op_num) + " is unknown", Error::ZUNIMPLEMENTED);
    return static_cast<OpNum>(raw_op_num);
}

}
