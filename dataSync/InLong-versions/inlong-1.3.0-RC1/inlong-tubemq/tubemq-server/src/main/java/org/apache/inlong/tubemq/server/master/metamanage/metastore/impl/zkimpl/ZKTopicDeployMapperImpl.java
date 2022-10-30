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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;
import org.apache.inlong.tubemq.corebase.TokenConstants;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.server.broker.stats.BrokerSrvStatsHolder;
import org.apache.inlong.tubemq.server.common.exception.LoadMetaException;
import org.apache.inlong.tubemq.server.common.zookeeper.ZKUtil;
import org.apache.inlong.tubemq.server.common.zookeeper.ZooKeeperWatcher;
import org.apache.inlong.tubemq.server.master.metamanage.DataOpErrCode;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.TopicDeployEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.impl.AbsTopicDeployMapperImpl;
import org.apache.zookeeper.KeeperException;

public class ZKTopicDeployMapperImpl extends AbsTopicDeployMapperImpl {
    private final ZooKeeperWatcher zkWatcher;
    private final String deployRootDir;

    public ZKTopicDeployMapperImpl(String metaNodePrefix,
                                   ZooKeeperWatcher zkWatcher,
                                   StringBuilder strBuff) {
        super();
        this.zkWatcher = zkWatcher;
        this.deployRootDir = strBuff.append(metaNodePrefix)
                .append(TokenConstants.SLASH).append(TZKNodeKeys.ZK_LEAF_TOPIC_DEPLOY_CONFIG).toString();
        strBuff.delete(0, strBuff.length());
    }

    @Override
    public void close() {
        clearCachedData();
        logger.info("[ZK Impl] close topic deploy configure finished!");
    }

    @Override
    public void loadConfig(StringBuilder strBuff) throws LoadMetaException {
        long totalCnt = 0L;
        logger.info("[ZK Impl] load topic deploy configure start...");
        // clear cache data
        clearCachedData();
        // query data set
        List<String> childNodes = ZKUtil.getChildren(zkWatcher, deployRootDir);
        if (childNodes == null) {
            logger.info("[ZK Impl] Not found topic deploy configure from ZooKeeper");
            return;
        }
        // load data from ZooKeeper
        String recordStr;
        Gson gson = new Gson();
        Type type = new TypeToken<TopicDeployEntity>() {}.getType();
        for (String itemKey : childNodes) {
            if (TStringUtils.isEmpty(itemKey)) {
                continue;
            }
            try {
                recordStr = ZKUtil.readDataMaybeNull(zkWatcher, strBuff.append(deployRootDir)
                        .append(TokenConstants.SLASH).append(itemKey).toString());
                strBuff.delete(0, strBuff.length());
            } catch (KeeperException e) {
                BrokerSrvStatsHolder.incZKExcCnt();
                logger.error("KeeperException during load topic deploy configure from ZooKeeper", e);
                throw new LoadMetaException(e.getMessage());
            }
            if (recordStr == null) {
                continue;
            }
            putRecord2Caches(gson.fromJson(recordStr, type));
            totalCnt++;
        }
        logger.info(strBuff.append("[ZK Impl] loaded ").append(totalCnt)
                .append(" topic deploy configure successfully...").toString());
        strBuff.delete(0, strBuff.length());
    }

    protected boolean putConfig2Persistent(TopicDeployEntity entity,
                                           StringBuilder strBuff, ProcessResult result) {
        String entityStr = entity.toString();
        String confNode = strBuff.append(deployRootDir)
                .append(TokenConstants.SLASH).append(entity.getRecordKey()).toString();
        strBuff.delete(0, strBuff.length());
        try {
            ZKUtil.updatePersistentPath(zkWatcher, confNode, entityStr);
        } catch (Throwable t) {
            BrokerSrvStatsHolder.incZKExcCnt();
            logger.error("[ZK Impl] put topic deploy configure failure ", t);
            result.setFailResult(DataOpErrCode.DERR_STORE_ABNORMAL.getCode(),
                    strBuff.append("Put topic deploy configure failure: ")
                            .append(t.getMessage()).toString());
            strBuff.delete(0, strBuff.length());
            return result.isSuccess();
        }
        result.setSuccResult(null);
        return result.isSuccess();
    }

    protected boolean delConfigFromPersistent(String recordKey, StringBuilder strBuff) {
        ZKUtil.delZNode(this.zkWatcher, strBuff.append(deployRootDir)
                .append(TokenConstants.SLASH).append(recordKey).toString());
        strBuff.delete(0, strBuff.length());
        return true;
    }
}
