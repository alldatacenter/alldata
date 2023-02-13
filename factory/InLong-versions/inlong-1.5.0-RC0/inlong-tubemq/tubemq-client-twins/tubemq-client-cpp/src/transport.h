/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

#ifndef _TUBEMQ_TRANSPORT_H_
#define _TUBEMQ_TRANSPORT_H_

#include <string>

#include "any.h"
#include "buffer.h"
#include "codec_protocol.h"
#include "future.h"

namespace tubemq {

// On Close Callback
using CloseNotifier = std::function<void(const std::error_code*)>;

struct ResponseContext;
using ResponseContextPtr = std::shared_ptr<ResponseContext>;

struct RequestContext {
  uint32_t request_id_{0};
  std::string ip_;
  uint32_t port_;
  uint32_t timeout_{0};  // millisecond
  uint32_t connection_pool_id_{0};
  uint64_t create_time_ms_{0};  // create time millisecond

  CodecProtocolPtr codec_;
  CloseNotifier close_notifier_;

  BufferPtr buf_;
  Promise<ResponseContext> promise_;
};
using RequestContextPtr = std::shared_ptr<RequestContext>;

struct ResponseContext {
  Any rsp_;
};

}  // namespace tubemq

#endif  // _TUBEMQ_TRANSPORT_H_
