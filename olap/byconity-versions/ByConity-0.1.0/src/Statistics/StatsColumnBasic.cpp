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

#include <Optimizer/Dump/Json2Pb.h>
#include <Statistics/StatsColumnBasic.h>

namespace DB::Statistics
{
String StatsColumnBasic::serialize() const
{
    return proto.SerializeAsString();
}
void StatsColumnBasic::deserialize(std::string_view blob)
{
    proto.ParseFromArray(blob.data(), blob.size());
}
String StatsColumnBasic::serializeToJson() const
{
    String json_str;
    Json2Pb::pbMsg2JsonStr(proto, json_str, false);
    return json_str;
}
void StatsColumnBasic::deserializeFromJson(std::string_view json)
{
    Json2Pb::jsonStr2PbMsg({json.data(), json.size()}, proto, false);
}

} // namespace DB
