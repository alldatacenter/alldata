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

#include <unordered_set>
#include <gflags/gflags.h>
#include <Poco/Exception.h>
#include <Common/Brpc/BrpcGflagsConfigHolder.h>

namespace DB
{
static std::unordered_map<std::string, std::string> configurable_brpc_gflags /* NOLINT */
    {/// Number of event dispatcher
     {"event_dispatcher_num", "2"},
     /// Max unwritten bytes in each socket, if the limit is reached
     {"socket_max_unwritten_bytes", "1073741824"},
     /// Set the recv buffer size of socket if this value is positive
     {"socket_recv_buffer_size", ""},
     /// Set send buffer size of sockets if this value is positive
     {"socket_send_buffer_size", ""},
     /// Defer close of connections for so many seconds even if the connection
     /// is not used by anyone. Close immediately for non-positive values
     {"defer_close_second", "60"},
     /// Try to return free memory to system every so many seconds
     /// values <= 0 disables this feature
     {"free_memory_to_system_interval", ""},
     /// Maximum size of a single message body in all protocols
     {"max_body_size", "671088640"},
     /// Print Controller.ErrorText() when server is about to respond a failed RPC
     {"log_error_text", ""}};


static std::unordered_set<std::string> reconfigurable_brpc_gflags /* NOLINT */
    {"socket_max_unwritten_bytes",
     "socket_recv_buffer_size",
     "socket_send_buffer_size",
     "defer_close_second",
     "free_memory_to_system_interval",
     "log_error_text"};

void BrpcGflagsConfigHolder::afterInit(const RawConfig * config_ptr)
{
    Poco::Util::AbstractConfiguration::Keys config_keys;
    config_ptr->keys(name, config_keys);

    for(const auto & entry: configurable_brpc_gflags)
    {
        auto config_val = entry.second;
        auto tag = this->name + "." + entry.first;
        if(config_ptr->hasProperty(tag)){
            config_val = config_ptr->getString(tag);
        }
        if (config_val.empty()) continue;
        LOG_INFO(logger, "Set brpc gflags [ {} : {} ]", entry.first, config_val);
        auto result = GFLAGS_NAMESPACE::SetCommandLineOption(entry.first.c_str(), config_val.c_str());
        if (result.empty())
            throw Poco::IllegalStateException(Poco::format("Fail to set gflags with key : {}, value : {}", entry.first, config_val));
    }
}

bool BrpcGflagsConfigHolder::hasChanged(const RawConfig *, const RawConfig *)
{
    return true;
}

void BrpcGflagsConfigHolder::onChange(const RawConfig * old_conf_ptr, const RawConfig * new_conf_ptr)
{
    Poco::Util::AbstractConfiguration::Keys config_keys;
    new_conf_ptr->keys(name, config_keys);
    for (const auto & key : config_keys)
    {
        auto tag = this->name + "." + key;
        if (reconfigurable_brpc_gflags.find(key) != reconfigurable_brpc_gflags.end())
        {
            auto new_config_val = new_conf_ptr->getString(tag);
            std::string old_config_val = old_conf_ptr ? old_conf_ptr->getString(tag, "NULL") : "NULL";
            if (new_config_val == old_config_val)
                continue;
            auto result = GFLAGS_NAMESPACE::SetCommandLineOption(key.c_str(), new_config_val.c_str());
            if (result.empty())
            {
                throw Poco::IllegalStateException(Poco::format("Fail to set gflags with key : {}, value : {}", key, new_config_val));
            }
            LOG_INFO(logger, "Reload brpc gflags {} from {}  to {}", key,  old_config_val, new_config_val);
        }
    }
}
}
