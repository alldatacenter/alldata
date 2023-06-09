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
#include <Processors/ISimpleTransform.h>

namespace DB
{

class SubstitutionTransform : public ISimpleTransform
{
public:
    SubstitutionTransform(const Block & header_, const std::unordered_map<String, String> & name_substitution_info_);

    String getName() const override { return "SubstitutionTransform"; }

    static Block transformHeader(Block header, const std::unordered_map<String, String> & name_substitution_info_);

protected:
    void transform(Chunk & chunk) override;

private:
    std::unordered_map<String, String> name_substitution_info;
};

}
