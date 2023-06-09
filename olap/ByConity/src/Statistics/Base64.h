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
#include <string_view>
#include <Core/Types.h>
#include <common/StringRef.h>

namespace DB::Statistics
{
String base64Decode(std::string_view encoded);
String base64Encode(std::string_view decoded);

// inline String base64Decode(const StringRef& encoded) {
//     return base64Decode((encoded));
// }
// inline String base64Encode(const StringRef& decoded) {
//     return base64Encode(static_cast<std::string_view>(decoded));
// }
}

namespace DB
{
using Statistics::base64Decode;
using Statistics::base64Encode;
}
