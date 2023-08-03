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
#include <Statistics/StatsTableBasic.h>

namespace DB::Statistics
{
String StatsTableBasic::serialize() const
{
    return table_basic_pb.SerializeAsString();
}
void StatsTableBasic::deserialize(std::string_view blob)
{
    table_basic_pb.ParseFromArray(blob.data(), static_cast<int>(blob.size()));
}
void StatsTableBasic::setRowCount(int64_t row_count)
{
    table_basic_pb.set_row_count(row_count);
}
int64_t StatsTableBasic::getRowCount() const
{
    return table_basic_pb.row_count();
}
String StatsTableBasic::serializeToJson() const
{
    String json_str;
    Json2Pb::pbMsg2JsonStr(table_basic_pb, json_str, false);
    return json_str;
}
void StatsTableBasic::deserializeFromJson(std::string_view json)
{
    Json2Pb::jsonStr2PbMsg({json.data(), json.size()}, table_basic_pb, false);
}

} // namespace DB
