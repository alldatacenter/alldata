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

#include <Coordination/pathUtils.h>
#include <iostream>

namespace DB::PathUtils
{

static size_t findLastSlash(StringRef path)
{
    if (path.size == 0)
        return std::string::npos;

    for (size_t i = path.size - 1; i > 0; --i)
    {
        if (path.data[i] == '/')
            return i;
    }

    if (path.data[0] == '/')
        return 0;

    return std::string::npos;
}

/// Note: Different from parentPath in IDisks.h
StringRef parentPath(StringRef path)
{
    auto rslash_pos = findLastSlash(path);
    if (rslash_pos > 0)
        return StringRef{path.data, rslash_pos};
    return "/";
}

StringRef getBaseName(StringRef path)
{
    size_t basename_start = findLastSlash(path);
    return StringRef{path.data + basename_start + 1, path.size - basename_start - 1};
}

}
