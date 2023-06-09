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

#include "StringUtils.h"


namespace detail
{

bool startsWith(const std::string & s, const char * prefix, size_t prefix_size)
{
    return s.size() >= prefix_size && 0 == memcmp(s.data(), prefix, prefix_size);
}

bool endsWith(const std::string & s, const char * suffix, size_t suffix_size)
{
    return s.size() >= suffix_size && 0 == memcmp(s.data() + s.size() - suffix_size, suffix, suffix_size);
}

void parseSlowQuery(const std::string & query, size_t & pos)
{
    const std::string whitespace = " \t\n";
    pos = query.find_first_not_of(whitespace, pos);
    if (pos == std::string::npos || query[pos] != '/')
        return;

    pos++;

    size_t length = query.size();
    if (pos >= length || query[pos] != '*')
        return;

    while (pos < length)
    {
        if (pos + 1 < length && query[pos] == '*' && query[pos + 1] == '/')
        {
            pos += 2;
            break;
        }
        pos++;
    }

    // recursively parse the query
    parseSlowQuery(query, pos);
}

}
