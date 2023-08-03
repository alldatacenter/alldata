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

#include <gtest/gtest.h>
#include <Core/UUID.h>
#include <Common/Exception.h>

namespace
{
using namespace DB;

TEST(UUIDHelperTest, UUIDToString)
{
    UUID uuid_input{UInt128{0, 1}};
    std::string uuid_str = UUIDHelpers::UUIDToString(uuid_input);
    EXPECT_EQ(uuid_str, "00000000-0000-0000-0000-000000000001");
    UUID uuid_output = UUIDHelpers::toUUID(uuid_str);
    EXPECT_EQ(uuid_output, uuid_input);
    EXPECT_THROW(UUIDHelpers::toUUID("3"), Exception);
}

} /// end anonymous namespace
