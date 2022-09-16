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

#include "client_connection.h"

using namespace tubemq;

const uint32_t ClientConnection::kConnnectMaxTimeMs;

void ClientConnection::AsyncWrite(RequestContextPtr& req) {
  auto self = shared_from_this();
  executor_->Post([self, this, req]() {
    if (request_list_.find(req->request_id_) != request_list_.end()) {
      LOG_ERROR("Write requestid[%d] is repeat", req->request_id_);
      return;
    }
    auto& transport_req = request_list_[req->request_id_];
    transport_req.req_ = req;
    bool queue_empty = write_queue_.empty();
    write_queue_.push_back(req->request_id_);
    if (req->timeout_ > 0) {
      transport_req.deadline_ = executor_->CreateSteadyTimer();
      transport_req.deadline_->expires_after(std::chrono::milliseconds(req->timeout_));
      transport_req.deadline_->async_wait(std::bind(&ClientConnection::requestTimeoutHandle,
                                                    shared_from_this(), std::placeholders::_1,
                                                    transport_req.req_));
    }
    if (IsConnected() && queue_empty) {
      asyncWrite();
    }
  });
}

void ClientConnection::Close() {
  auto self = shared_from_this();
  executor_->Post([self, this]() { close(); });
}

void ClientConnection::requestTimeoutHandle(const std::error_code& ec, RequestContextPtr req) {
  if (ec) {
    return;
  }
  auto request_id = req->request_id_;
  auto err = ErrorCode(err_code::kErrNetWorkTimeout, "Request is timeout");
  requestCallback(request_id, &err);
}

void ClientConnection::close(const std::error_code* err) {
  if (IsStop()) {
    return;
  }
  status_ = kDisconnected;
  LOG_INFO("%scloseed", ToString().c_str());
  socket_->close();
  if (notifier_ != nullptr) {
    notifier_(err);
  }
  releaseAllRequest(err);
}

void ClientConnection::releaseAllRequest(const std::error_code* err) {
  std::string msg = "connect close ";
  if (err != nullptr) {
    msg += "error_code, value:";
    msg += std::to_string(err->value());
    msg += ", msg:";
    msg += err->message();
    msg += ", category:";
    msg += err->category().name();
  }

  auto terr = ErrorCode(err_code::kErrNetworkError, msg);
  for (auto& it : request_list_) {
    it.second.req_->promise_.SetFailed(terr);
    it.second.deadline_->cancel();
  }
  request_list_.clear();
  write_queue_.clear();
  recv_buffer_->Reset();
}

void ClientConnection::connect(const asio::ip::tcp::resolver::results_type& endpoints) {
  if (IsStop()) {
    return;
  }
  status_ = kConnecting;
  deadline_->expires_after(std::chrono::milliseconds(kConnnectMaxTimeMs));
  deadline_->async_wait(std::bind(&ClientConnection::checkDeadline, this, std::placeholders::_1));
  asio::async_connect(
      *socket_, endpoints, [this](std::error_code ec, asio::ip::tcp::endpoint endpoint) {
        deadline_->cancel();
        if (ec) {
          status_ = kDisconnected;
          LOG_ERROR("%s[%s:%d]async connect error:%d, %s, %s", ToString().c_str(), ip_.c_str(),
                    port_, ec.value(), ec.message().c_str(), ec.category().name());
          close(&ec);
          return;
        }
        status_ = kConnected;
        socket_->set_option(asio::ip::tcp::no_delay(true));
        // socket_->set_option(asio::ip::tcp::socket::reuse_address(true));
        contextString();
        LOG_INFO("%sis connected", ToString().c_str());

        asyncWrite();
        asyncRead();
      });
}

void ClientConnection::checkDeadline(const std::error_code& ec) {
  if (ec) {
    return;
  }
  if (IsStop()) {
    return;
  }
  LOG_ERROR("%s connect timeout", ToString().c_str());
  close();
}

void ClientConnection::contextString() {
  std::stringstream stream;
  stream << "[" << socket_->local_endpoint() << " -> " << socket_->remote_endpoint() << "] ";
  context_string_ += stream.str();
}

void ClientConnection::asyncRead() {
  if (IsStop()) {
    return;
  }
  if (recv_buffer_->capacity() > rpc_config::kRpcRecvBufferMaxBytes) {
    LOG_ERROR("%sbuffer capacity over config:%d", ToString().c_str(),
              rpc_config::kRpcRecvBufferMaxBytes);
    close();
    return;
  }
  recv_buffer_->EnsureWritableBytes(rpc_config::kRpcEnsureWriteableBytes);
  auto self = shared_from_this();
  socket_->async_receive(
      asio::buffer(recv_buffer_->WriteBegin(), recv_buffer_->WritableBytes()),
      [self, this](std::error_code ec, std::size_t len) {
        if (ec) {
          LOG_ERROR("[%s]async read error:%d, %s, %s", ToString().c_str(), ec.value(),
                    ec.message().c_str(), ec.category().name());
          close(&ec);
          return;
        }
        if (len == 0) {
          LOG_ERROR("[%s]async read zero", ToString().c_str());
          close(&ec);
          return;
        }
        recv_time_ = std::time(nullptr);
        recv_buffer_->WriteBytes(len);
        std::error_code error;
        size_t availsize = socket_->available(error);
        if (availsize > 0 && !error) {
          recv_buffer_->EnsureWritableBytes(availsize);
          size_t rlen = socket_->receive(asio::buffer(recv_buffer_->WriteBegin(), availsize));
          if (rlen > 0) {
            recv_buffer_->WriteBytes(rlen);
          }
        }
        while (checkPackageDone() > 0 && recv_buffer_->length() > 0) {
        }
        asyncRead();
      });
}

int ClientConnection::checkPackageDone() {
  if (check_ == nullptr) {
    recv_buffer_->Reset();
    LOG_ERROR("check codec func not set");
    return -1;
  }
  if (package_length_ > recv_buffer_->length()) {
    return 0;
  }
  uint32_t request_id = 0;
  bool has_request_id = false;
  Any check_out;
  auto buff = recv_buffer_->Slice();
  size_t package_length = 0;
  auto result = check_(buff, check_out, request_id, has_request_id, package_length);
  if (result < 0) {
    package_length_ = 0;
    LOG_ERROR("%s, check codec package result:%d", ToString().c_str(), result);
    close();
    return -1;
  }
  if (result == 0) {
    package_length_ = package_length;
    return 0;
  }
  ++read_package_number_;
  package_length_ = 0;
  recv_buffer_->Skip(result);
  if (!has_request_id) {
    auto it = request_list_.begin();
    if (it == request_list_.end()) {
      LOG_ERROR("%s, not find request", ToString().c_str());
      return 1;
    }
    requestCallback(it->first, nullptr, &check_out);
    return 1;
  }
  requestCallback(request_id, nullptr, &check_out);
  return 1;
}

void ClientConnection::requestCallback(uint32_t request_id, ErrorCode* err, Any* check_out) {
  auto it = request_list_.find(request_id);
  if (it == request_list_.end()) {
    LOG_INFO("%srequest[%d] not find from request_list_", ToString().c_str(), request_id);
    return;
  }
  auto req = &it->second;
  req->deadline_->cancel();
  if (err != nullptr) {
    LOG_ERROR("%srequest[%d] error:%d, msg:%s", ToString().c_str(), request_id, err->Value(),
              err->Message().c_str());
    req->req_->promise_.SetFailed(*err);
    request_list_.erase(it);
    return;
  }
  if (check_out != nullptr) {
    ResponseContext rsp;
    BufferPtr* buff = any_cast<BufferPtr>(check_out);
    if (buff != nullptr) {
      req->req_->codec_->Decode(*buff, request_id, rsp.rsp_);
    } else {
      rsp.rsp_ = *check_out;
    }
    req->req_->promise_.SetValue(rsp);
  } else {
    req->req_->promise_.SetFailed(ErrorCode(err_code::kErrNetworkError, "response is null"));
  }
  request_list_.erase(it);
}

TransportRequest* ClientConnection::nextTransportRequest() {
  uint32_t request_id;
  TransportRequest* transport_req = nullptr;
  while (!write_queue_.empty()) {
    request_id = write_queue_.front();
    write_queue_.pop_front();
    auto it = request_list_.find(request_id);
    if (it == request_list_.end()) {
      continue;
    }
    transport_req = &it->second;
    break;
  }
  return transport_req;
}

void ClientConnection::asyncWrite() {
  if (IsStop()) {
    return;
  }
  auto transport_req = nextTransportRequest();
  if (transport_req == nullptr) {
    return;
  }
  auto self = shared_from_this();
  auto& req = transport_req->req_;
  asio::async_write(
      *socket_,
      asio::buffer(transport_req->req_->buf_->data(), transport_req->req_->buf_->length()),
      [self, this, req](std::error_code ec, std::size_t length) {
        if (ec) {
          close(&ec);
          LOG_ERROR("[%s]async write error:%d, message:%s, category:%s", ToString().c_str(),
                    ec.value(), ec.message().c_str(), ec.category().name());
          return;
        }
        ++write_package_number_;
        LOG_TRACE("[%s]async write done, request_id:%d", ToString().c_str(), req->request_id_);
        asyncWrite();
      });
}

