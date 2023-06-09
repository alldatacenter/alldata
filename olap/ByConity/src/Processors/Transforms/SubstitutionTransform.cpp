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

#include <Processors/Transforms/SubstitutionTransform.h>
#include <DataStreams/materializeBlock.h>

namespace DB
{


SubstitutionTransform::SubstitutionTransform(const Block & header_, const std::unordered_map<String, String> & name_substitution_info_):
    ISimpleTransform(
        header_,
        transformHeader(header_, name_substitution_info_),
        true), name_substitution_info(name_substitution_info_) {}

Block SubstitutionTransform::transformHeader(Block header, const std::unordered_map<String, String> & name_substitution_info_)
{
    substituteBlock(header, name_substitution_info_);
    return header;
}

void SubstitutionTransform::transform(Chunk & chunk)
{
    size_t chunk_rows = chunk.getNumRows();
    auto columns = chunk.detachColumns();
    Block block = getInputPort().getHeader().cloneWithColumns(columns);
    columns.clear();
    substituteBlock(block, name_substitution_info);
    columns = block.getColumns();
    chunk.setColumns(std::move(columns), chunk_rows);
}

}
