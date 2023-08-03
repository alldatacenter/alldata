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
#include <Protos/RPCHelpers.h>

namespace
{
using namespace DB;

TEST(RPCHelperTest, UUIDTest)
{
    UUID uuid_input = UUIDHelpers::generateV4();
    Protos::UUID uuid_protos;
    DB::RPCHelpers::fillUUID(uuid_input, uuid_protos);
    UUID uuid_output = DB::RPCHelpers::createUUID(uuid_protos);
    EXPECT_EQ(uuid_output, uuid_input);
}


}
