/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qlangtech.tis.cloud;

import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.solrj.util.ZkUtils;
import org.apache.commons.lang.StringUtils;
//import org.apache.zookeeper.Watcher;
//import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * ZK的抽象
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public interface ITISCoordinator extends ICoordinator {
    static Logger logger = LoggerFactory.getLogger(ITISCoordinator.class);

    static ITISCoordinator create() {
        //if (Config.isStandaloneMode()) {
        logger.info("create ITISCoordinator with Standalone Mode");
        return new ITISCoordinator() {
            private final String DEFAULT_CHILD1_PATH = "child001";

            @Override
            public boolean shallConnect2RemoteIncrStatusServer() {
                return true;
            }

            @Override
            public List<String> getChildren(String zkPath //, Watcher watcher
                    , boolean b) {
                if (ZkUtils.ZK_ASSEMBLE_LOG_COLLECT_PATH.equals(zkPath)) {
                    return Collections.singletonList(DEFAULT_CHILD1_PATH);
                }
                throw new IllegalStateException("zkPath:" + zkPath + " is illegal");
            }

            @Override
            public void addOnReconnect(IOnReconnect onReconnect) {

            }

            @Override
            public byte[] getData(String s //, Watcher o, Stat stat
                    , boolean b) {
                if (StringUtils.equals(s
                        , ZkUtils.ZK_ASSEMBLE_LOG_COLLECT_PATH + ZkUtils.PATH_SPLIT + DEFAULT_CHILD1_PATH)) {
                    return (Config.getAssembleHost() + ":" + ZkUtils.ZK_ASSEMBLE_LOG_COLLECT_PORT).getBytes(TisUTF8.get());
                }
                throw new IllegalStateException("zkPath:" + s + " is illegal");
            }

            @Override
            public void create(String path, byte[] data, boolean persistent, boolean sequential) {

            }

            @Override
            public boolean exists(String path, boolean watch) {
                return true;
            }

            @Override
            public <T> T unwrap() {
                return null;
            }
        };
//        } else {
//            logger.info("create ITISCoordinator with Distribute Mode");
//            //  return new TisZkClient(Config.getZKHost(), 60000);
//            throw new UnsupportedOperationException();
//        }
    }

    /**
     * 是否应该连接Assemble日志收集服务，单元测试过程中需要返回false
     *
     * @return
     */
    boolean shallConnect2RemoteIncrStatusServer();

    List<String> getChildren(String zkPath //, Watcher watcher
            , boolean b);

    void addOnReconnect(IOnReconnect onReconnect);

    byte[] getData(String s //, Watcher o, Stat stat
            , boolean b);

    void create(String path, byte[] data, boolean persistent, boolean sequential);

    boolean exists(String path, boolean watch);

    public interface IOnReconnect {

        public void command();
    }
}
