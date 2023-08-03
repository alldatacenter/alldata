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

#include <Interpreters/Context.h>
#include <Protos/registry.pb.h>
#include <brpc/server.h>
#include <brpc/stream.h>
#include <common/logger_useful.h>

namespace DB
{
class BrpcExchangeReceiverRegistryService : public Protos::RegistryService
{
public:
    explicit BrpcExchangeReceiverRegistryService(int max_buf_size_) : max_buf_size(max_buf_size_) { }

    void registry(
        ::google::protobuf::RpcController * controller,
        const ::DB::Protos::RegistryRequest * request,
        ::DB::Protos::RegistryResponse * response,
        ::google::protobuf::Closure * done) override;


private:
    int max_buf_size;
    Poco::Logger * log = &Poco::Logger::get("BrpcExchangeReceiverRegistryService");
};
}
