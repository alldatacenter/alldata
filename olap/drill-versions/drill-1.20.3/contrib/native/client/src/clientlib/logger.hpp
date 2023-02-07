/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#ifndef __LOGGER_H
#define __LOGGER_H

#include <iostream>
#include <sstream>
#include <ostream>
#include <iostream>
#include <fstream>
#include <string>
#include <stdio.h>

#include <boost/thread/mutex.hpp>
#include "drill/common.hpp"

namespace Drill{

class Logger{
    public:
        Logger(){
            m_level = LOG_ERROR;
            m_pOutFileStream = NULL;
            m_pOutStream = &std::cout;
        }
        ~Logger(){ }

        void init(const char* path);
        void close();
        std::ostream& log(logLevel_t level);
        std::string levelAsString(logLevel_t level) {
            static const char* const levelNames[] = {
                "TRACE  ",
                "DEBUG  ",
                "INFO   ",
                "WARNING",
                "ERROR  ",
                "FATAL  "
            };
            return levelNames[level>=LOG_TRACE && level<=LOG_FATAL?level:LOG_ERROR];
        }

        // The logging level
        logLevel_t m_level;
        std::ostream* m_pOutStream;
        boost::mutex m_logMutex;

    private:
        std::ofstream* m_pOutFileStream;
        std::string m_filepath;

}; // Logger

    Logger& getLogger();
    std::string getTime();
    std::string getTid();

#define DRILL_MT_LOG(LOG) \
    { \
    boost::lock_guard<boost::mutex> logLock(getLogger().m_logMutex); \
    LOG \
    }

#define DRILL_LOG(level) \
    if (getLogger().m_pOutStream==NULL || level < getLogger().m_level); \
        else getLogger().log(level)       \


} // namespace Drill

#endif
