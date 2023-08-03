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

#include <Common/CGroup/CGroupManagerFactory.h>
#include <iostream>

namespace DB
{

CGroupManagerPtr CGroupManagerFactory::cgroup_manager_instance = std::make_shared<CGroupManager>(CGroupManager::PassKey());

CGroupManager & DB::CGroupManagerFactory::instance()
{
    return *cgroup_manager_instance;
}

void CGroupManagerFactory::loadFromConfig(const Poco::Util::AbstractConfiguration & config)
{
    if (!config.has("enable_cgroup") || !config.getBool("enable_cgroup"))
        return;

    if (!CGroupManagerFactory::instance().isInit() && config.has("enable_cpuset") && config.getBool("enable_cpuset"))
    {
        LOG_INFO(&Poco::Logger::get("CGroupManager"), "Init CGroupManager");
        CGroupManagerFactory::instance().init();
        if (config.has("root_cpuset_path") && !config.getString("root_cpuset_path").empty())
        {
            CGroupManagerFactory::instance().setCGroupCpuSetPath(config.getString("root_cpuset_path"));
        }

        if (cgroup_manager_instance->enable())
        {
            using Keys = Poco::Util::AbstractConfiguration::Keys;
            Keys keys;
            config.keys("cpuset", keys);
            for (const String & key : keys)
            {
                const String & cpu_set_name = key;
                String cpus = config.getString("cpuset."+key);
                if (nullptr != cgroup_manager_instance->getCpuSet(cpu_set_name))
                    continue;
                LOG_INFO(&Poco::Logger::get("CGroupManager"), "create cpuset: " + cpu_set_name + " cpus: " + cpus);
                cgroup_manager_instance->createCpuSet(cpu_set_name, cpus);
            }
        }
    }

    if (config.has("enable_cpu") && config.getBool("enable_cpu"))
    {
        if (config.has("root_cpu_path") && !config.getString("root_cpu_path").empty())
            CGroupManagerFactory::instance().setCGgroupCpuPath(config.getString("root_cpu_path"));
    }
}

}
