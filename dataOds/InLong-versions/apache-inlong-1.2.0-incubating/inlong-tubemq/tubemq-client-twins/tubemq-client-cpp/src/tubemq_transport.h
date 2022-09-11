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

#ifndef _TUBEMQ_TUBEMQ_TRANSPORT_H_
#define _TUBEMQ_TUBEMQ_TRANSPORT_H_

#include "buffer.h"
#include "codec_protocol.h"
#include "connection_pool.h"
#include "executor_pool.h"
#include "future.h"
#include "logger.h"
#include "transport.h"
#include "tubemq_codec.h"

namespace tubemq {

template <typename RequestProtocol>
Future<ResponseContext> AsyncRequest(RequestContextPtr& request, RequestProtocol& protocol) {
  request->buf_ = std::make_shared<Buffer>();
  Any in(protocol);
  request->codec_->Encode(in, request->buf_);
  auto future = request->promise_.GetFuture();
  auto pool = TubeMQService::Instance()->GetConnectionPool();
  if (pool != nullptr) {
    pool->GetConnection(request)->AsyncWrite(request);
  } else {
    request->promise_.SetFailed(ErrorCode(err_code::kErrServerStop, "server is stop"));
  }
  return future;
}

template <typename RequestProtocol>
ErrorCode SyncRequest(ResponseContext& response_context, RequestContextPtr& request,
                      RequestProtocol& protocol) {
  auto future = AsyncRequest(request, protocol);
  return future.Get(response_context);
}

}  // namespace tubemq

#endif  // _TUBEMQ_TUBEMQ_TRANSPORT_H_
