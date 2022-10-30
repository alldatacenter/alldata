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

#ifndef _TUBEMQ_CODEC_PROTOCOL_H_
#define _TUBEMQ_CODEC_PROTOCOL_H_

#include <list>
#include <memory>
#include <string>

#include "any.h"
#include "buffer.h"

namespace tubemq {

class CodecProtocol {
 public:
  CodecProtocol() {}

  virtual ~CodecProtocol() {}

  virtual std::string Name() const = 0;

  virtual bool Decode(const BufferPtr &buff, uint32_t request_id, Any &out) = 0;

  virtual bool Encode(const Any &in, BufferPtr &buff) = 0;

  // return code: -1 failed; 0-Unfinished; > 0 package buffer size
  virtual int32_t Check(BufferPtr &in, Any &out, uint32_t &request_id, bool &has_request_id,
                        size_t &package_length) = 0;

  // get protocol request id
  virtual int32_t GetRequestId(uint32_t &request_id, const Any &rsp) const { return -1; }

  // set protocol request request id
  virtual int32_t SetRequestId(uint32_t request_id, Any &req) { return -1; }
};

using CodecProtocolPtr = std::shared_ptr<CodecProtocol>;

}  // namespace tubemq
#endif  // _TUBEMQ_CODEC_PROTOCOL_H_
