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

#include <memory>
#include <vector>
#include <roaring.hh>

namespace DB
{

class IMergeTreeDataPart;

/// Deprecated
using IMergeTreeDataPartPtr = std::shared_ptr<const IMergeTreeDataPart>;
using IMergeTreeMutableDataPartPtr = std::shared_ptr<IMergeTreeDataPart>;
using IMergeTreeDataPartsVector = std::vector<IMergeTreeDataPartPtr>;
using IMutableMergeTreeDataPartPtr = std::shared_ptr<IMergeTreeDataPart>;
using IMutableMergeTreeDataPartsVector = std::vector<IMutableMergeTreeDataPartPtr>;
/// end Deprecated

using Roaring = roaring::Roaring;
using DeleteBitmapPtr = std::shared_ptr<Roaring>;
using DeleteBitmapVector = std::vector<DeleteBitmapPtr>;
using ImmutableDeleteBitmapPtr = std::shared_ptr<const Roaring>;
using ImmutableDeleteBitmapVector = std::vector<ImmutableDeleteBitmapPtr>;

using MergeTreeDataPartPtr = std::shared_ptr<const IMergeTreeDataPart>;
using MergeTreeMutableDataPartPtr = std::shared_ptr<IMergeTreeDataPart>;
using MergeTreeDataPartsVector = std::vector<MergeTreeDataPartPtr>;
using MergeTreeMutableDataPartsVector = std::vector<MergeTreeMutableDataPartPtr>;

}
