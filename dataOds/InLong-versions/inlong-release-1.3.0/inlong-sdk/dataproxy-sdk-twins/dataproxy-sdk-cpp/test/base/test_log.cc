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

#include "atomic.h"
#include "logger.h"
#include "singleton.h"
#include <string>
#include <thread>
using namespace dataproxy_sdk;
using namespace std;

AtomicInt ati;

void logfunc()
{
    // for (int i = 0; i < 100; i++) {
    while (true)
    {
        LOG_INFO("%d, this is info log %d", ati.incrementAndGet(), 1);
        std::string str = "tttttt";
        LOG_TRACE("%d, this is trace log:%s", ati.incrementAndGet(), str.c_str());
        LOG_ERROR("%d, this is an error log", ati.incrementAndGet());
        LOG_WARN("%d, warn log: "
                 "wartryjreah5p3ihj504twujyhi6twjuhyit6jioawjhyiow4ehjygio3aqjhyginareowigpoijewpto4urgt94ure9guer9itug",
                 ati.incrementAndGet());
        LOG_DEBUG("%d, debug llllllllllllll %d", ati.incrementAndGet(), 124);
    }
}

int main(int argc, char const* argv[])
{
    debug_init_log();
    LOG_INFO("this is info log %d", 1);
    std::string str = "tttttt";
    LOG_TRACE("this is trace log:%s", str.c_str());
    LOG_ERROR("this is an error log");
    LOG_WARN("warn log: warnareowigpoijewpto4urgt94ure9guer9itug");
    LOG_DEBUG("debug llllllllllllll %d", 124);

    ati.getAndSet(0);
    std::thread t1(logfunc);
    std::thread t2(logfunc);
    std::thread t3(logfunc);
    std::thread t4(logfunc);

    t1.join();

    return 0;
}
