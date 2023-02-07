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
#include <boost/bind.hpp>
#include <drill/drillClient.hpp>
#include <drill/drillConfig.hpp>
#include "zookeeperClient.hpp"

#include "errmsgs.hpp"
#include "logger.hpp"

namespace Drill {
std::string ZookeeperClient::s_defaultDrillPath("/drill/drillbits1");
static void watcherCallback(zhandle_t *zzh, int type, int state, const char *path, void* context) {
	static_cast<ZookeeperClient*>(context)->watcher(zzh, type, state, path, context);
}

ZookeeperClient::ZookeeperClient(const std::string& drillPath)
: p_zh(), m_state(), m_path(drillPath) {
    m_bConnecting=true;
    memset(&m_id, 0, sizeof(m_id));
}

ZookeeperClient::~ZookeeperClient(){
}

ZooLogLevel ZookeeperClient::getZkLogLevel(){
    //typedef enum {ZOO_LOG_LEVEL_ERROR=1,
    //    ZOO_LOG_LEVEL_WARN=2,
    //    ZOO_LOG_LEVEL_INFO=3,
    //    ZOO_LOG_LEVEL_DEBUG=4
    //} ZooLogLevel;
    switch(DrillClientConfig::getLogLevel()){
        case LOG_TRACE:
        case LOG_DEBUG:
            return ZOO_LOG_LEVEL_DEBUG;
        case LOG_INFO:
            return ZOO_LOG_LEVEL_INFO;
        case LOG_WARNING:
            return ZOO_LOG_LEVEL_WARN;
        case LOG_ERROR:
        case LOG_FATAL:
        default:
            return ZOO_LOG_LEVEL_ERROR;
    }
    return ZOO_LOG_LEVEL_ERROR;
}

void ZookeeperClient::watcher(zhandle_t *zzh, int type, int state, const char *path, void*) {
    //From cli.c

    /* Be careful using zh here rather than zzh - as this may be mt code
     * the client lib may call the watcher before zookeeper_init returns */

    this->m_state=state;
    if (type == ZOO_SESSION_EVENT) {
        if (state == ZOO_CONNECTED_STATE) {
        } else if (state == ZOO_AUTH_FAILED_STATE) {
            this->m_err= getMessage(ERR_CONN_ZKNOAUTH);
            this->close();
        } else if (state == ZOO_EXPIRED_SESSION_STATE) {
        	this->m_err= getMessage(ERR_CONN_ZKEXP);
        	this->close();
        }
    }
    // signal the cond var
    {
        if (state == ZOO_CONNECTED_STATE){
            DRILL_MT_LOG(DRILL_LOG(LOG_TRACE) << "Connected to Zookeeper." << std::endl;)
        }
        boost::lock_guard<boost::mutex> bufferLock(this->m_cvMutex);
        this->m_bConnecting=false;
    }
    this->m_cv.notify_one();
}

int ZookeeperClient::getAllDrillbits(const std::string& connectStr, std::vector<std::string>& drillbits){
    uint32_t waitTime=30000; // 10 seconds
    zoo_set_debug_level(getZkLogLevel());
    zoo_deterministic_conn_order(1); // enable deterministic order

    p_zh = boost::shared_ptr<zhandle_t>(zookeeper_init(connectStr.c_str(), &watcherCallback, waitTime, &m_id, this, 0), zookeeper_close);
    if(!p_zh) {
        m_err = getMessage(ERR_CONN_ZKFAIL);
        return -1;
    }

    m_err="";
	//Wait for the completion handler to signal successful connection
	boost::unique_lock<boost::mutex> bufferLock(this->m_cvMutex);
	boost::system_time const timeout=boost::get_system_time()+ boost::posix_time::milliseconds(waitTime);
	while(this->m_bConnecting) {
		if(!this->m_cv.timed_wait(bufferLock, timeout)){
			m_err = getMessage(ERR_CONN_ZKTIMOUT);
			return -1;
		}
	}

    if(m_state!=ZOO_CONNECTED_STATE){
        return -1;
    }

    int rc = ZOK;

    struct String_vector drillbitsVector;
    rc=zoo_get_children(p_zh.get(), m_path.c_str(), 0, &drillbitsVector);
    if(rc!=ZOK){
        m_err=getMessage(ERR_CONN_ZKERR, rc);
        p_zh.reset();
        return -1;
    }

    // Make sure we deallocate drillbitsVector properly when we exit
    boost::shared_ptr<String_vector> guard(&drillbitsVector, deallocate_String_vector);

    if(drillbitsVector.count > 0){
        DRILL_MT_LOG(DRILL_LOG(LOG_TRACE) << "Found " << drillbitsVector.count << " drillbits in cluster ("
                << connectStr << "/" << m_path
                << ")." <<std::endl;)
		for(int i=0; i<drillbitsVector.count; i++){
			drillbits.push_back(drillbitsVector.data[i]);
		}
        for(int i=0; i<drillbits.size(); i++){
            DRILL_MT_LOG(DRILL_LOG(LOG_TRACE) << "\t Unshuffled Drillbit id: " << drillbits[i] << std::endl;)
        }
    }
    return 0;
}

int ZookeeperClient::getEndPoint(const std::string& drillbit, exec::DrillbitEndpoint& endpoint){
    int rc = ZOK;
	// pick the drillbit at 'index'
	std::string s(m_path +  "/" + drillbit);
	int buffer_len=MAX_CONNECT_STR;
	char buffer[MAX_CONNECT_STR+1];
	struct Stat stat;
	buffer[MAX_CONNECT_STR]=0;
	rc= zoo_get(p_zh.get(), s.c_str(), 0, buffer,  &buffer_len, &stat);
	if(rc!=ZOK){
		m_err=getMessage(ERR_CONN_ZKDBITERR, rc);
		return -1;
	}
	exec::DrillServiceInstance drillServiceInstance;
	drillServiceInstance.ParseFromArray(buffer, buffer_len);
	endpoint=drillServiceInstance.endpoint();

    if(p_zh!=NULL && m_state==ZOO_CONNECTED_STATE){
        DRILL_LOG(LOG_TRACE) << drillServiceInstance.DebugString() << std::endl;
    }

    return 0;
}

void ZookeeperClient::close(){
	p_zh.reset();
}

} /* namespace Drill */
