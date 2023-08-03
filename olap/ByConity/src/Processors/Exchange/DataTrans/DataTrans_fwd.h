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
#include "common/types.h"

namespace DB
{
class IBroadcastReceiver;
class IBroadcastSender;
using BroadcastReceiverPtr = std::shared_ptr<IBroadcastReceiver>;
using BroadcastSenderPtr = std::shared_ptr<IBroadcastSender>;
using BroadcastSenderPtrs = std::vector<BroadcastSenderPtr>;

/// Status code indicates the status of the broadcaster which consists by connected senders and receiver.
/// We should cancel data transport immediately when return positive status code and close gracefully when meet negative code.
/// Close gracefully means that no data can be send any more but in flight data shoule be consumed.
enum BroadcastStatusCode
{
    ALL_SENDERS_DONE = -1,
    RUNNING = 0,
    RECV_REACH_LIMIT = 9010,
    RECV_TIMEOUT = 9012,
    SEND_TIMEOUT = 9013,
    RECV_CANCELLED = 9014,
    SEND_CANCELLED = 9015,
    SEND_NOT_READY = 9016,
    RECV_UNKNOWN_ERROR = 90099,
    SEND_UNKNOWN_ERROR = 900100
};

struct BroadcastStatus
{
    explicit BroadcastStatus(BroadcastStatusCode status_code_) : code(status_code_), is_modifer(false){ }

    explicit BroadcastStatus(BroadcastStatusCode status_code_, bool is_modifer_) : code(status_code_), is_modifer(is_modifer_) { }

    explicit BroadcastStatus(BroadcastStatusCode status_code_, bool is_modifer_, String message_)
        : code(status_code_), is_modifer(is_modifer_), message(std::move(message_))
    {
    }

    BroadcastStatusCode code;

    /// Is this operation modified the status
    mutable bool is_modifer;

    /// message about why changed to this status
    String message;

    /// The one who change the status lastly
    String modifer;

    UInt64 time;


};


}
