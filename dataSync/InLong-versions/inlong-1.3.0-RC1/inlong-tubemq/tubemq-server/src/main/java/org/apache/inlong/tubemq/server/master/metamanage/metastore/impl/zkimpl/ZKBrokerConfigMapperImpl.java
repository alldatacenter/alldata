/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.master.metamanage.metastore.impl.zkimpl;

import java.lang.reflect.Type;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.inlong.tubemq.corebase.TokenConstants;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.server.broker.stats.BrokerSrvStatsHolder;
import org.apache.inlong.tubemq.server.common.exception.LoadMetaException;
import org.apache.inlong.tubemq.server.common.zookeeper.ZKUtil;
import org.apache.inlong.tubemq.server.common.zookeeper.ZooKeeperWatcher;
import org.apache.inlong.tubemq.server.master.metamanage.DataOpErrCode;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.BrokerConfEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.impl.AbsBrokerConfigMapperImpl;
import org.apache.zookeeper.KeeperException;

public class ZKBrokerConfigMapperImpl extends AbsBrokerConfigMapperImpl {
    private final ZooKeeperWatcher zkWatcher;
    private final String brokerCfgRootDir;

    public ZKBrokerConfigMapperImpl(String metaNodePrefix,
                                    ZooKeeperWatcher zkWatcher,
                                    StringBuilder strBuff) {
        super();
        this.zkWatcher = zkWatcher;
        this.brokerCfgRootDir = strBuff.append(metaNodePrefix)
                .append(TokenConstants.SLASH).append(TZKNodeKeys.ZK_LEAF_BROKER_CONFIG).toString();
        strBuff.delete(0, strBuff.length());
    }

    @Override
    public void close() {
        clearCachedData();
        logger.info("[ZK Impl] close broker configure finished!");
    }

    @Override
    public void loadConfig(StringBuilder strBuff) throws LoadMetaException {
        long totalCnt = 0L;
        logger.info("[ZK Impl] load broker configure start...");
        // clear cache data
        clearCachedData();
        // load data from zookeeper
        List<String> childNodes = ZKUtil.getChildren(zkWatcher, brokerCfgRootDir);
        if (childNodes == null) {
            logger.info("[ZK Impl] Not found broker configure from ZooKeeper");
            return;
        }
        String confStr;
        Gson gson = new Gson();
        Type type = new TypeToken<BrokerConfEntity>() {}.getType();
        // read data item, parse and store data to cache
        for (String itemKey : childNodes) {
            if (TStringUtils.isEmpty(itemKey)) {
                continue;
            }
            try {
                confStr = ZKUtil.readDataMaybeNull(zkWatcher, strBuff.append(brokerCfgRootDir)
                        .append(TokenConstants.SLASH).append(itemKey).toString());
                strBuff.delete(0, strBuff.length());
            } catch (KeeperException e) {
                BrokerSrvStatsHolder.incZKExcCnt();
                logger.error("KeeperException during load broker configure from ZooKeeper", e);
                throw new LoadMetaException(e.getMessage());
            }
            if (confStr == null) {
                continue;
            }
            putRecord2Caches(gson.fromJson(confStr, type));
            totalCnt++;
        }
        logger.info(strBuff.append("[ZK Impl] loaded ").append(totalCnt)
                .append(" broker configure successfully...").toString());
        strBuff.delete(0, strBuff.length());
    }

    protected boolean putConfig2Persistent(BrokerConfEntity entity,
                                           StringBuilder strBuff, ProcessResult result) {
        String entityStr = entity.toString();
        String confNode = strBuff.append(brokerCfgRootDir)
                .append(TokenConstants.SLASH).append(entity.getBrokerId()).toString();
        strBuff.delete(0, strBuff.length());
        try {
            ZKUtil.updatePersistentPath(zkWatcher, confNode, entityStr);
        } catch (Throwable t) {
            BrokerSrvStatsHolder.incZKExcCnt();
            logger.error("[ZK Impl] put broker configure failure ", t);
            result.setFailResult(DataOpErrCode.DERR_STORE_ABNORMAL.getCode(),
                    strBuff.append("Put broker configure failure: ")
                            .append(t.getMessage()).toString());
            strBuff.delete(0, strBuff.length());
            return result.isSuccess();
        }
        result.setSuccResult(null);
        return result.isSuccess();
    }

    protected boolean delConfigFromPersistent(int brokerId, StringBuilder strBuff) {
        ZKUtil.delZNode(this.zkWatcher, strBuff.append(brokerCfgRootDir)
                .append(TokenConstants.SLASH).append(brokerId).toString());
        strBuff.delete(0, strBuff.length());
        return true;
    }
}
