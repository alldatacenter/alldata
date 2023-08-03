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
#include <Statistics/StatisticsBaseImpl.h>
#include <Common/Exception.h>
namespace DB::Statistics
{
class StatsDummyAlpha : public StatisticsBase
{
public:
    static constexpr auto tag = StatisticsTag::DummyAlpha;
    StatsDummyAlpha() = default;
    StatsDummyAlpha(int num) : magic_num(num) { }

    String serialize() const override;
    void deserialize(std::string_view blob) override;
    StatisticsTag getTag() const override { return tag; }
    auto get() { return magic_num; }

private:
    int magic_num = 0;
};


class StatsDummyBeta : public StatisticsBase
{
public:
    static constexpr auto tag = StatisticsTag::DummyBeta;
    StatsDummyBeta() = default;
    StatsDummyBeta(String str) : magic_str(std::move(str)) { }
    String serialize() const override;
    void deserialize(std::string_view blob) override;
    StatisticsTag getTag() const override { return tag; }
    auto get() { return magic_str; }

private:
    String magic_str;
};
}
