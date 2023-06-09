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
#include <Core/BackgroundSchedulePool.h>
#include <Common/ResourceMonitor.h>
#include <Interpreters/Context_fwd.h>

namespace DB
{

class Context;

namespace ResourceManagement
{

class ResourceReporterTask : protected WithContext
{
public:
    ResourceReporterTask(ContextPtr context);
    ~ResourceReporterTask();
    void run();
    void start();
    void stop();

private:
    bool sendHeartbeat();
    void sendRegister();
    void sendRemove();

    inline String getenv(const char * name) { return std::getenv(name) ? std::getenv(name) : ""; }

private:
    bool init_request = true;
    Poco::Logger * log;
    std::unique_ptr<ResourceMonitor> resource_monitor;
    BackgroundSchedulePool::TaskHolder background_task;
};

}

}
