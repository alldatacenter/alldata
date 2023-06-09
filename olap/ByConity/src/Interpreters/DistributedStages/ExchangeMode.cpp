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

#include <Interpreters/DistributedStages/ExchangeMode.h>
#include <sstream>

namespace DB
{

String exchangeModeToString(const ExchangeMode & exchange_mode)
{
    std::ostringstream ostr;

    switch(exchange_mode)
    {
        case ExchangeMode::UNKNOWN:
            ostr << "UNKNOWN";
            break;
        case ExchangeMode::LOCAL_NO_NEED_REPARTITION:
            ostr << "LOCAL_NO_NEED_REPARTITION";
            break;
        case ExchangeMode::LOCAL_MAY_NEED_REPARTITION:
            ostr << "LOCAL_MAY_NEED_REPARTITION";
            break;
        case ExchangeMode::REPARTITION:
            ostr << "REPARTITION";
            break;
        case ExchangeMode::BROADCAST:
            ostr << "BROADCAST";
            break;
        case ExchangeMode::GATHER:
            ostr << "GATHER";
            break;
    }

    return ostr.str();
}


}
