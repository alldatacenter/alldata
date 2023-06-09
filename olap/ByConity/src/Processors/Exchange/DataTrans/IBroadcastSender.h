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
#include <optional>
#include <Processors/Chunk.h>
#include <Processors/Exchange/DataTrans/DataTrans_fwd.h>
#include <common/types.h>

namespace DB
{

enum class BroadcastSenderType
{
    Local = 0,
    Brpc
};
class IBroadcastSender
{
public:
    virtual BroadcastStatus send(Chunk chunk) = 0;

    /// Merge sender to get 1:N sender and can avoid duplicated serialization when send chunk
    virtual void merge(IBroadcastSender && sender) = 0;

    virtual String getName() const = 0;
    virtual BroadcastSenderType getType() = 0;
    virtual BroadcastStatus finish(BroadcastStatusCode status_code_, String message) = 0;
    virtual ~IBroadcastSender() = default;
};

using BroadcastSenderPtr = std::shared_ptr<IBroadcastSender>;

}
