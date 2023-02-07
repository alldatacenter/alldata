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
#ifdef _WIN32
#include <zookeeper.h>
#else
#include <zookeeper/zookeeper.h>
#endif

#include <boost/shared_ptr.hpp>
#include <boost/thread/condition_variable.hpp>
#include <boost/thread/mutex.hpp>

#include "UserBitShared.pb.h"


#ifndef ZOOKEEPER_CLIENT_H
#define ZOOKEEPER_CLIENT_H

namespace Drill {
class ZookeeperClient{
    public:
		static std::string s_defaultDrillPath;

        ZookeeperClient(const std::string& drillPath = s_defaultDrillPath);
        ~ZookeeperClient();
        static ZooLogLevel getZkLogLevel();
        // comma separated host:port pairs, each corresponding to a zk
        // server. e.g. "127.0.0.1:3000,127.0.0.1:3001,127.0.0.1:3002
        void close();
        const std::string& getError() const{return m_err;}
        // return unshuffled list of drillbits
        int getAllDrillbits(const std::string& connectStr, std::vector<std::string>& drillbits);
        // picks the index drillbit and returns the corresponding endpoint object
        int getEndPoint(const std::string& drillbit, exec::DrillbitEndpoint& endpoint);

        void watcher(zhandle_t *zzh, int type, int state, const char *path, void* context);

    private:
        boost::shared_ptr<zhandle_t> p_zh;
        clientid_t m_id;
        int m_state;
        std::string m_err;

        boost::mutex m_cvMutex;
        // Condition variable to signal connection callback has been processed
        boost::condition_variable m_cv;
        bool m_bConnecting;
        std::string m_path;

};
} /* namespace Drill */



#endif /* ZOOKEEPER_H */
