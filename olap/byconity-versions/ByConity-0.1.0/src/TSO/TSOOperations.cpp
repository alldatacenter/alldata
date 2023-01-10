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

#include <TSO/TSOOperations.h>

namespace DB
{

namespace TSO
{

const std::string Operation(const OperationType & type)
{
    switch (type)
    {
        case OperationType::OPEN :
        return "OPEN";
        case OperationType::PUT :
        return "PUT";
        case OperationType::GET :
        return "GET";
        case OperationType::CLEAN :
        return "CLEAN";
    }

    __builtin_unreachable();
}

}

}
