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
#ifndef DRILL_CONFIG_H
#define DRILL_CONFIG_H

#include "drill/common.hpp"
#include <boost/thread/mutex.hpp>

namespace exec{
    namespace shared{
        class DrillPBError;
    };
};

namespace Drill{

// Only one instance of this class exists. A static member of DrillClientImpl;
 
class DECLSPEC_DRILL_CLIENT DrillClientConfig{
    public:
        DrillClientConfig();
        ~DrillClientConfig();
        static void initLogging(const char* path);
        static void setLogLevel(logLevel_t l);
        static void setSaslPluginPath(const char* path);
        static const char* getSaslPluginPath();
        static void setBufferLimit(uint64_t l);
        static uint64_t getBufferLimit();
        static void setSocketTimeout(int32_t l);
        static void setHandshakeTimeout(int32_t l);
        static void setQueryTimeout(int32_t l);
        static void setHeartbeatFrequency(int32_t l);
        static int32_t getSocketTimeout();
        static int32_t getHandshakeTimeout();
        static int32_t getQueryTimeout();
        static int32_t getHeartbeatFrequency();
        static logLevel_t getLogLevel();
        /**
         * Return the client name sent to the server when connecting
         *
         * @return the current client name
         */
        static const std::string& getClientName();

        /**
         * Set the client name to be sent to the server when connecting.
         *
         * Only new connections will use the new value. Existing connections
         * will be left unchanged.
         *
         * @param name the name to be send to the server
         */
        static void setClientName(const std::string& name);

        /**
         * Return the application name sent to the server when connecting
         *
         * @return the current application name
         */
        static const std::string& getApplicationName();

        /**
         * Set the application name to be sent to the server when connecting.
         *
         * Only new connections will use the new value. Existing connections
         * will be left unchanged.
         *
         * @param name the name to be send to the server
         */
        static void setApplicationName(const std::string& name);


    private:
        // The logging level
        static logLevel_t s_logLevel;
        // The total amount of memory to be allocated by an instance of DrillClient.
        // For future use. Currently, not enforced.
        static uint64_t s_bufferLimit;

        static const char* s_saslPluginPath;

        /**
         * DrillClient configures timeout (in seconds) in a fine granularity.
         * Disabled by setting the value to zero.
         *
         * s_socketTimout: (default 0)
         *      set SO_RCVTIMEO and SO_SNDTIMEO socket options and place a
         *		timeout on socket receives and sends. It is disabled by default.
         *
         * s_handshakeTimeout: (default 5)
         *      place a timeout on validating handshake. When an endpoint (host:port)
         *		is reachable but drillbit hangs or running another service. It will
         *		avoid the client hanging.
         *
         * s_queryTimeout: (default 180)
         *      place a timeout on waiting result of querying.
         *
         * s_heartbeatFrequency: (default 30)
         *      Seconds of idle activity after which a heartbeat is sent to the drillbit
         */
        static int32_t s_socketTimeout;
        static int32_t s_handshakeTimeout;
        static int32_t s_queryTimeout;
        static int32_t s_heartbeatFrequency;
        static boost::mutex s_mutex;
        // The client name (default to DRILL_CONNECTOR_NAME)
        static std::string s_clientName;
        // The application name (default to <empty>)
        static std::string s_applicationName;
};



} // namespace Drill

#endif
