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

#include <Protos/runtime_filter.pb.h>
#include <brpc/stream.h>
#include <brpc/server.h>
#include <Interpreters/Context.h>

namespace DB
{
class RuntimeFilterService : public Protos::RuntimeFilterService
{
public:
    explicit RuntimeFilterService(ContextMutablePtr context_) : context(context_), log(&Poco::Logger::get("RuntimeFilterService")){}

    /// transfer dynamic filer (segment executor host --> coordinator host)
    void transferRuntimeFilter(
        ::google::protobuf::RpcController * controller,
        const ::DB::Protos::TransferRuntimeFilterRequest * request,
        ::DB::Protos::TransferRuntimeFilterResponse * response,
        ::google::protobuf::Closure * done) override;

    /// dispatch dynamic filter (coordinator host --> segment executor host)
    void dispatchRuntimeFilter(
        ::google::protobuf::RpcController * controller,
        const ::DB::Protos::DispatchRuntimeFilterRequest * request,
        ::DB::Protos::DispatchRuntimeFilterResponse * response,
        ::google::protobuf::Closure * done) override;

private:
    ContextMutablePtr context;
    Poco::Logger * log;
};
}
