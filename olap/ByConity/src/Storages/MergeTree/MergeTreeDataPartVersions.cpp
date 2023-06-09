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

#include "MergeTreeDataPartVersions.h"
#include <IO/ReadBufferFromString.h>
#include <IO/ReadHelpers.h>
#include <IO/WriteHelpers.h>

namespace DB
{
void MergeTreeDataPartVersions::write(WriteBuffer & to)
{
    writeString("versions\n", to);
    writeString("enable_compact_map_data ", to);
    writeBoolText(enable_compact_map_data, to);
}

bool MergeTreeDataPartVersions::read(ReadBuffer & from, bool needCheckHeader)
{
    if (needCheckHeader)
    {
        assertString("versions\n", from);
    }
    assertString("enable_compact_map_data ", from);
    readBoolText(enable_compact_map_data, from);

    return true;
}

}
