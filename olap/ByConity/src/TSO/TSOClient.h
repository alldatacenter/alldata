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
#include <CloudServices/RpcClientBase.h>
#include <Protos/tso.pb.h>

namespace DB::TSO
{
class TSO_Stub;

class TSOClient : public RpcClientBase
{
public:
    static String getName() { return "TSOClient"; }

    TSOClient(String host_port_);
    TSOClient(HostWithPorts host_ports_);
    ~TSOClient() override;

    GetTimestampResp getTimestamp();
    GetTimestampsResp getTimestamps(UInt32 size);

private:
    void assertRPCSuccess(brpc::Controller & cntl, int status);

    std::unique_ptr<TSO_Stub> stub;
};

using TSOClientPtr = std::shared_ptr<TSOClient>;

enum class TSORequestType
{
    GetTimestamp,
    GetTimestamps
};

inline std::string typeToString(TSORequestType type)
{
    switch (type)
    {
        case TSORequestType::GetTimestamp:
            return "GetTimestamp";
        case TSORequestType::GetTimestamps:
            return "GetTimestamps";
    }

    __builtin_unreachable();
}

UInt64 getTSOResponse(const Context & context, TSORequestType type, size_t size = 1);

}
