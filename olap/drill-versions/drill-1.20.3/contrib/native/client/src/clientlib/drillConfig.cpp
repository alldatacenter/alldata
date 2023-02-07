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
#include <boost/thread/lock_guard.hpp>
#include "drill/common.hpp"
#include "drill/drillConfig.hpp"
#include "env.h"
#include "logger.hpp"

namespace Drill{

// Initialize static member of DrillClientConfig
logLevel_t DrillClientConfig::s_logLevel=LOG_ERROR;
uint64_t DrillClientConfig::s_bufferLimit=MAX_MEM_ALLOC_SIZE;
int32_t DrillClientConfig::s_socketTimeout=0;
int32_t DrillClientConfig::s_handshakeTimeout=5;
int32_t DrillClientConfig::s_queryTimeout=180;
int32_t DrillClientConfig::s_heartbeatFrequency=15; // 15 seconds
const char* DrillClientConfig::s_saslPluginPath = NULL;
std::string DrillClientConfig::s_clientName(DRILL_CONNECTOR_NAME);
std::string DrillClientConfig::s_applicationName;


boost::mutex DrillClientConfig::s_mutex;

DrillClientConfig::DrillClientConfig(){
        // Do not initialize logging. The Logger object is static and may 
        // not have been initialized yet
        //initLogging(NULL);
}

DrillClientConfig::~DrillClientConfig(){
}

void DrillClientConfig::initLogging(const char* path){
    getLogger().init(path);
}

void DrillClientConfig::setLogLevel(logLevel_t l){
    boost::lock_guard<boost::mutex> configLock(DrillClientConfig::s_mutex);
    s_logLevel=l;
    getLogger().m_level=l;
}

void DrillClientConfig::setBufferLimit(uint64_t l){
    boost::lock_guard<boost::mutex> configLock(DrillClientConfig::s_mutex);
    s_bufferLimit=l;
}

uint64_t DrillClientConfig::getBufferLimit(){
    boost::lock_guard<boost::mutex> configLock(DrillClientConfig::s_mutex);
    return s_bufferLimit;
}

void DrillClientConfig::setSocketTimeout(int32_t t){
    boost::lock_guard<boost::mutex> configLock(DrillClientConfig::s_mutex);
    s_socketTimeout=t;
}

void DrillClientConfig::setHandshakeTimeout(int32_t t){
    if (t > 0) {
        boost::lock_guard<boost::mutex> configLock(DrillClientConfig::s_mutex);
        s_handshakeTimeout = t;
    }
}

void DrillClientConfig::setQueryTimeout(int32_t t){
    if (t>0){
        boost::lock_guard<boost::mutex> configLock(DrillClientConfig::s_mutex);
        s_queryTimeout=t;
    }
}

void DrillClientConfig::setHeartbeatFrequency(int32_t t){
    if (t>0){
        boost::lock_guard<boost::mutex> configLock(DrillClientConfig::s_mutex);
        s_heartbeatFrequency=t;
    }
}

int32_t DrillClientConfig::getSocketTimeout(){
    boost::lock_guard<boost::mutex> configLock(DrillClientConfig::s_mutex);
    return s_socketTimeout;
}

int32_t DrillClientConfig::getHandshakeTimeout(){
    boost::lock_guard<boost::mutex> configLock(DrillClientConfig::s_mutex);
    return  s_handshakeTimeout;
}

int32_t DrillClientConfig::getQueryTimeout(){
    boost::lock_guard<boost::mutex> configLock(DrillClientConfig::s_mutex);
    return s_queryTimeout;
}

int32_t DrillClientConfig::getHeartbeatFrequency(){
    boost::lock_guard<boost::mutex> configLock(DrillClientConfig::s_mutex);
    return s_heartbeatFrequency;
}

logLevel_t DrillClientConfig::getLogLevel(){
    boost::lock_guard<boost::mutex> configLock(DrillClientConfig::s_mutex);
    return s_logLevel;
}

void DrillClientConfig::setSaslPluginPath(const char *path){
    boost::lock_guard<boost::mutex> configLock(DrillClientConfig::s_mutex);
    s_saslPluginPath = path;
}

const char* DrillClientConfig::getSaslPluginPath(){
    boost::lock_guard<boost::mutex> configLock(DrillClientConfig::s_mutex);
    return s_saslPluginPath;
}

const std::string& DrillClientConfig::getClientName() {
	boost::lock_guard<boost::mutex> configLock(DrillClientConfig::s_mutex);
	return s_clientName;
}

void DrillClientConfig::setClientName(const std::string& name) {
	boost::lock_guard<boost::mutex> configLock(DrillClientConfig::s_mutex);
	s_clientName = name;
}

const std::string& DrillClientConfig::getApplicationName() {
	boost::lock_guard<boost::mutex> configLock(DrillClientConfig::s_mutex);
	return s_applicationName;
}

void DrillClientConfig::setApplicationName(const std::string& name) {
	boost::lock_guard<boost::mutex> configLock(DrillClientConfig::s_mutex);
	s_applicationName = name;
}

} // namespace Drill
