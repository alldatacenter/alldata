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

#include <sstream>
#include <Core/Types.h>

namespace DB
{

namespace Catalog
{
/// Used to escape the origin string. Covert all '_' into '\_'
static String escapeString(const String & origin)
{
    std::stringstream escaped;
    for (std::string::const_iterator it = origin.begin(); it != origin.end(); it++)
    {
        std::string::value_type c = (*it);

        if (c == '\\' || c == '_')
            escaped << "\\" << c;
        else
            escaped << c;
    }

    return escaped.str();
}


}

}
