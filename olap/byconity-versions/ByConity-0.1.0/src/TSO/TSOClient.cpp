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

#include <TSO/TSOClient.h>

#include <Protos/tso.pb.h>
#include <Protos/RPCHelpers.h>
#include <TSO/TSOImpl.h>
#include <Interpreters/Context.h>
#include <brpc/channel.h>
#include <brpc/controller.h>

#include <chrono>
#include <thread>

namespace DB
{

namespace ErrorCodes
{
    extern const int BRPC_TIMEOUT;
    extern const int TSO_INTERNAL_ERROR;
}

namespace TSO
{
TSOClient::TSOClient(String host_port_)
    : RpcClientBase(getName(), std::move(host_port_)), stub(std::make_unique<TSO_Stub>(&getChannel()))
{
}

TSOClient::TSOClient(HostWithPorts host_ports_)
    : RpcClientBase(getName(), std::move(host_ports_)), stub(std::make_unique<TSO_Stub>(&getChannel()))
{
}

TSOClient::~TSOClient() = default;

GetTimestampResp TSOClient::getTimestamp()
{
    GetTimestampReq req;
    GetTimestampResp resp;
    brpc::Controller cntl;

    stub->GetTimestamp(&cntl, &req, &resp, nullptr);

    assertController(cntl);
    RPCHelpers::checkResponse(resp);

    return resp;
}

GetTimestampsResp TSOClient::getTimestamps(UInt32 size)
{
    GetTimestampsReq req;
    GetTimestampsResp resp;
    brpc::Controller cntl;

    req.set_size(size);
    stub->GetTimestamps(&cntl, &req, &resp, nullptr);

    assertController(cntl);
    RPCHelpers::checkResponse(resp);

    return resp;
}

UInt64 getTSOResponse(const Context & context, TSORequestType type, size_t size)
{
    const auto & config = context.getConfigRef();
    int tos_max_retry = config.getInt("tso_service.tso_max_retry_count", 3);
    bool use_tso_fallback = config.getBool("tso_service.use_fallback", true);

    std::string new_leader;
    int retry = tos_max_retry;

    while (retry--)
    {
        try
        {
            auto tso_client = context.getCnchTSOClient();

            switch (type)
            {
                case TSORequestType::GetTimestamp:
                {
                    auto response = tso_client->getTimestamp();
                    if (response.is_leader())
                        return response.timestamp();
                    break;
                }
                case TSORequestType::GetTimestamps:
                {
                    auto response = tso_client->getTimestamps(size);
                    if (response.is_leader())
                        return response.max_timestamp();
                    break;
                }
            }

            context.updateTSOLeaderHostPort();
        }
        catch (Exception & e)
        {
            if (use_tso_fallback && e.code() != ErrorCodes::BRPC_TIMEOUT)
            {
                /// old leader may be unavailable
                context.updateTSOLeaderHostPort();
                throw;
            }

            const auto error_string = fmt::format(
                    "TSO request: {} failed. Retries: {}/{}, Error message: {}",
                    typeToString(type),
                    std::to_string(tos_max_retry - retry),
                    std::to_string(tos_max_retry),
                    e.displayText());

            tryLogCurrentException(__PRETTY_FUNCTION__, error_string);
        }

        std::this_thread::sleep_for(std::chrono::milliseconds(30));
    }

    context.updateTSOLeaderHostPort();
    throw Exception(ErrorCodes::TSO_OPERATION_ERROR, "Can't get process TSO request, type: {}", typeToString(type));
}

}

}
