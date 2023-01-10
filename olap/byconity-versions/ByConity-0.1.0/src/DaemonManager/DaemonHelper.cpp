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

#include <DaemonManager/DaemonHelper.h>
#include <DaemonManager/DaemonFactory.h>
#include <Common/Exception.h>

namespace DB
{

namespace ErrorCodes
{
    extern const int INVALID_CONFIG_PARAMETER;
}

namespace DaemonManager
{

std::map<std::string, unsigned int> updateConfig(
    std::map<std::string, unsigned int> && default_config,
    const Poco::Util::AbstractConfiguration & app_config)
{
    Poco::Util::AbstractConfiguration::Keys keys;
    app_config.keys("daemon_manager.daemon_jobs", keys);

    std::for_each(keys.begin(), keys.end(),
        [& default_config, & app_config] (const std::string & key)
        {
            if (startsWith(key, "job"))
            {
                auto job_name = app_config.getString("daemon_manager.daemon_jobs." + key + ".name");

                if (!DaemonFactory::instance().validateJobName(job_name))
                    throw Exception("invalid config, there is no job named " + job_name,
                        ErrorCodes::INVALID_CONFIG_PARAMETER);

                auto it = default_config.find(job_name);
                if (it == default_config.end())
                    return;

                bool disable = app_config.getBool("daemon_manager.daemon_jobs." + key + ".disable", false);
                if (disable)
                {
                    default_config.erase(it);
                    return;
                }

                auto job_interval = app_config.getUInt("daemon_manager.daemon_jobs." + key + ".interval", 5000);
                it->second = job_interval;
            }
        }
    );

    return std::move(default_config);
}

void printConfig(std::map<std::string, unsigned int> & config, Poco::Logger * log)
{
    std::ostringstream oss;
    std::for_each(config.begin(), config.end(),
        [& oss] (const std::pair<std::string, unsigned int> & p)
        {
            oss << "Job name : " << p.first << ", job interval : " << p.second << '\n';
        }
    );

    LOG_INFO(log, oss.str());
}

} // end namespace DaemonManager

} // end namespace DB

