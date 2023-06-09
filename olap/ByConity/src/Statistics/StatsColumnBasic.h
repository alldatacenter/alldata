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
#include <Protos/optimizer_statistics.pb.h>
#include <Statistics/StatisticsBaseImpl.h>
#include <Common/Exception.h>

namespace DB::Statistics
{
// basic statistics at column level
// a wrapper of Protos::ColumnBasic
// currently just row_count
class StatsColumnBasic : public StatisticsBase
{
public:
    static constexpr auto tag = StatisticsTag::ColumnBasic;
    StatsColumnBasic() = default;
    String serialize() const override;
    void deserialize(std::string_view blob) override;
    StatisticsTag getTag() const override { return tag; }

    Protos::StatsColumnBasic & mutableProto() { return proto; }
    const Protos::StatsColumnBasic & getProto() const { return proto; }

    String serializeToJson() const override;
    void deserializeFromJson(std::string_view json) override;

private:
    Protos::StatsColumnBasic proto;
};
}
