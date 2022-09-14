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

#ifndef _TUBEMQ_TUBEMQ_CODEC_H_
#define _TUBEMQ_TUBEMQ_CODEC_H_

#include <assert.h>
#include <google/protobuf/io/coded_stream.h>
#include <google/protobuf/io/zero_copy_stream.h>
#include <google/protobuf/io/zero_copy_stream_impl.h>
#include <google/protobuf/io/zero_copy_stream_impl_lite.h>
#include <google/protobuf/message.h>

#include <string>

#include "BrokerService.pb.h"
#include "MasterService.pb.h"
#include "RPC.pb.h"
#include "any.h"
#include "buffer.h"
#include "codec_protocol.h"
#include "const_config.h"
#include "const_rpc.h"
#include "logger.h"
#include "tubemq/tubemq_errcode.h"
#include "tubemq/tubemq_return.h"
#include "utils.h"

namespace tubemq {

class TubeMQCodec final : public CodecProtocol {
 public:
  struct ReqProtocol {
    int32_t rpc_read_timeout_;
    uint32_t request_id_;
    int32_t method_id_;
    string prot_msg_;
  };

  struct RspProtocol {
    int32_t serial_no_;
    bool success_;
    int32_t code_;
    string error_msg_;
    int64_t message_id_;
    int32_t method_;
    RspResponseBody rsp_body_;
  };
  using ReqProtocolPtr = std::shared_ptr<ReqProtocol>;
  using RspProtocolPtr = std::shared_ptr<RspProtocol>;

 public:
  TubeMQCodec() {}

  virtual ~TubeMQCodec() {}

  virtual std::string Name() const { return "tubemq_v1"; }

  virtual bool Decode(const BufferPtr &buff, uint32_t request_id, Any &out) {
    // check total length
    int32_t total_len = buff->length();
    if (total_len <= 0) {
      return false;
    }
    // check package is valid
    RpcConnHeader rpc_header;
    ResponseHeader rsp_header;
    RspProtocolPtr rsp_protocol = GetRspProtocol();
    // parse pb data
    google::protobuf::io::ArrayInputStream rawOutput(buff->data(), total_len);
    bool result = readDelimitedFrom(&rawOutput, &rpc_header);
    if (!result) {
      return result;
    }
    result = readDelimitedFrom(&rawOutput, &rsp_header);
    if (!result) {
      return result;
    }
    ResponseHeader_Status rspStatus = rsp_header.status();
    if (rspStatus == ResponseHeader_Status_SUCCESS) {
      RspResponseBody response_body;
      rsp_protocol->success_ = true;
      rsp_protocol->code_ = err_code::kErrSuccess;
      rsp_protocol->error_msg_ = "OK";
      result = readDelimitedFrom(&rawOutput, &response_body);
      if (!result) {
        return false;
      }
      rsp_protocol->method_ = response_body.method();
      rsp_protocol->rsp_body_ = response_body;
    } else {
      RspExceptionBody rpc_exception;
      rsp_protocol->success_ = false;
      result = readDelimitedFrom(&rawOutput, &rpc_exception);
      if (!result) {
        return false;
      }
      string err_info = rpc_exception.exceptionname();
      err_info += delimiter::kDelimiterPound;
      err_info += rpc_exception.stacktrace();
      rsp_protocol->code_ = err_code::kErrRcvThrowError;
      rsp_protocol->error_msg_ = err_info;
    }
    rsp_protocol->serial_no_ = request_id;
    out = Any(rsp_protocol);
    LOG_TRACE("Decode: decode message finished, success_=%d, request_id=%d",
      rsp_protocol->success_, request_id);
    return true;
  }

  virtual bool Encode(const Any &in, BufferPtr &buff) {
    RequestBody req_body;
    ReqProtocolPtr req_protocol = any_cast<ReqProtocolPtr>(in);
    req_body.set_method(req_protocol->method_id_);
    req_body.set_timeout(req_protocol->rpc_read_timeout_);
    req_body.set_request(req_protocol->prot_msg_);
    RequestHeader req_header;
    req_header.set_servicetype(Utils::GetServiceTypeByMethodId(req_protocol->method_id_));
    req_header.set_protocolver(2);
    RpcConnHeader rpc_header;
    rpc_header.set_flag(rpc_config::kRpcFlagMsgRequest);
    // calc total list size
    uint32_t serial_len =
        4 + rpc_header.ByteSizeLong() + 4 + req_header.ByteSizeLong() + 4 + req_body.ByteSizeLong();
    std::string step_buff;
    step_buff.resize(serial_len);
    google::protobuf::io::ArrayOutputStream rawOutput(
      static_cast<void*>(const_cast<char*>(step_buff.data())), serial_len);
    bool result = writeDelimitedTo(rpc_header, &rawOutput);
    if (!result) {
      return result;
    }
    result = writeDelimitedTo(req_header, &rawOutput);
    if (!result) {
      return result;
    }
    result = writeDelimitedTo(req_body, &rawOutput);
    if (!result) {
      return result;
    }
    // append data to buffer
    uint32_t list_size = calcBlockCount(serial_len);
    buff->AppendInt32((int32_t)rpc_config::kRpcPrtBeginToken);
    buff->AppendInt32((int32_t)req_protocol->request_id_);
    buff->AppendInt32(list_size);
    uint32_t write_pos = 0;
    for (uint32_t i = 0; i < list_size; i++) {
      uint32_t slice_len = serial_len - i * rpc_config::kRpcMaxBufferSize;
      if (slice_len > rpc_config::kRpcMaxBufferSize) {
        slice_len = rpc_config::kRpcMaxBufferSize;
      }
      LOG_TRACE("Encode: encode slice [%d] slice_len = %d, serial_len = %d",
        i, slice_len, serial_len);
      buff->AppendInt32(slice_len);
      buff->Write(step_buff.data() + write_pos, slice_len);
      write_pos += slice_len;
    }
    LOG_TRACE("Encode: encode message success, request_id=%d, method_id=%d",
              req_protocol->request_id_, req_protocol->method_id_);
    return true;
  }

  // return code: -1 failed; 0-Unfinished; > 0 package buffer size
  virtual int32_t Check(BufferPtr &in, Any &out, uint32_t &request_id, bool &has_request_id,
                        size_t &package_length) {
    // check package is valid
    if (in->length() < 12) {
      package_length = 12;
      return 0;
    }
    // check frameToken
    uint32_t token = in->ReadUint32();
    if (token != rpc_config::kRpcPrtBeginToken) {
      return -1;
    }
    // get request_id
    request_id = in->ReadUint32();
    uint32_t list_size = in->ReadUint32();
    if (list_size > rpc_config::kRpcMaxFrameListCnt) {
      return -1;
    }
    // check data list
    uint32_t item_len = 0;
    package_length = 12;
    auto check_buf = in->Slice();
    for (uint32_t i = 0; i < list_size; i++) {
      package_length += 4;
      if (check_buf->length() < 4) {
        return 0;
      }
      item_len = check_buf->ReadUint32();
      if (item_len == 0) {
        return -1;
      }
      if (item_len > rpc_config::kRpcMaxBufferSize) {
        return -1;
      }
      package_length += item_len;
      if (item_len > check_buf->length()) {
        return 0;
      }
      check_buf->Skip(item_len);
    }
    has_request_id = true;
    uint32_t readed_len = 12;
    auto buf = std::make_shared<Buffer>();
    for (uint32_t i = 0; i < list_size; i++) {
      item_len = in->ReadUint32();
      readed_len += 4;
      buf->Write(in->data(), item_len);
      readed_len += item_len;
      in->Skip(item_len);
    }
    out = buf;
    LOG_TRACE("Check: received message check success, request_id=%d, readed_len:%d",
      request_id, readed_len);
    return readed_len;
  }

  static ReqProtocolPtr GetReqProtocol() { return std::make_shared<ReqProtocol>(); }
  static RspProtocolPtr GetRspProtocol() { return std::make_shared<RspProtocol>(); }

  static bool readDelimitedFrom(google::protobuf::io::ZeroCopyInputStream *rawInput,
                                google::protobuf::MessageLite *message) {
    // We create a new coded stream for each message.  Don't worry, this is fast,
    // and it makes sure the 64MB total size limit is imposed per-message rather
    // than on the whole stream.  (See the CodedInputStream interface for more
    // info on this limit.)
    google::protobuf::io::CodedInputStream input(rawInput);

    // Read the size.
    uint32_t size;
    if (!input.ReadVarint32(&size)) return false;
    // Tell the stream not to read beyond that size.
    google::protobuf::io::CodedInputStream::Limit limit = input.PushLimit(size);
    // Parse the message.
    if (!message->MergeFromCodedStream(&input)) return false;
    if (!input.ConsumedEntireMessage()) return false;

    // Release the limit.
    input.PopLimit(limit);

    return true;
  }

  static bool writeDelimitedTo(const google::protobuf::MessageLite &message,
                               google::protobuf::io::ZeroCopyOutputStream *rawOutput) {
    // We create a new coded stream for each message.  Don't worry, this is fast.
    google::protobuf::io::CodedOutputStream output(rawOutput);

    // Write the size.
    const int32_t size = message.ByteSizeLong();
    output.WriteVarint32(size);

    uint8_t *buffer = output.GetDirectBufferForNBytesAndAdvance(size);
    if (buffer != NULL) {
      // Optimization:  The message fits in one buffer, so use the faster
      // direct-to-array serialization path.
      message.SerializeWithCachedSizesToArray(buffer);
    } else {
      // Slightly-slower path when the message is multiple buffers.
      message.SerializeWithCachedSizes(&output);
      if (output.HadError()) return false;
    }

    return true;
  }

  uint32_t calcBlockCount(uint32_t content_len) {
    uint32_t block_cnt = content_len / rpc_config::kRpcMaxBufferSize;
    uint32_t remain_size = content_len % rpc_config::kRpcMaxBufferSize;
    if (remain_size > 0) {
      block_cnt++;
    }
    return block_cnt;
  }
};
}  // namespace tubemq
#endif

