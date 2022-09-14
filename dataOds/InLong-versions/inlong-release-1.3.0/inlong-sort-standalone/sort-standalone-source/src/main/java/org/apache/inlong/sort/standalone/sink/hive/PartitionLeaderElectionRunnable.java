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

package org.apache.inlong.sort.standalone.sink.hive;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.inlong.sort.standalone.config.pojo.InlongId;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;
import org.slf4j.Logger;

/**
 * 
 * PartitionLeaderElectionRunnable
 */
public class PartitionLeaderElectionRunnable implements Runnable {

    public static final Logger LOG = InlongLoggerFactory.getLogger(HiveSinkContext.class);
    private final HiveSinkContext context;
    private Map<String, PartitionCreateRunnable> partitionCreateMap = new ConcurrentHashMap<>();

    /**
     * Constructor
     * 
     * @param context
     */
    public PartitionLeaderElectionRunnable(HiveSinkContext context) {
        this.context = context;
    }

    /**
     * run
     */
    @Override
    public void run() {
        LOG.info("start to PartitionLeaderElectionRunnable.");
        // clear stopped runnable
        Set<String> uidPartitions = new HashSet<>();
        for (Entry<String, PartitionCreateRunnable> entry : this.partitionCreateMap.entrySet()) {
            if (entry.getValue().getState() != PartitionState.INIT
                    && entry.getValue().getState() != PartitionState.CREATING) {
                uidPartitions.add(entry.getKey());
            }
        }
        uidPartitions.forEach(item -> this.partitionCreateMap.remove(item));
        // add runnable
        try (Connection conn = context.getHiveConnection()) {
            Map<String, HdfsIdConfig> idConfigMap = context.getIdConfigMap();
            ExecutorService partitionCreatePool = context.getPartitionCreatePool();
            Statement stat = conn.createStatement();
            for (Entry<String, HdfsIdConfig> entry : idConfigMap.entrySet()) {
                LOG.info("start to PartitionLeaderElectionRunnable check id token:{}", entry.getKey());
                if (hasToken(entry.getValue())) {
                    HdfsIdConfig idConfig = entry.getValue();
                    // get partition list of table
                    String tableName = idConfig.getHiveTableName();
                    ResultSet rs = stat.executeQuery("show partitions " + tableName);
                    Set<String> partitionSet = new HashSet<>();
                    while (rs.next()) {
                        String strPartition = rs.getString(1);
                        int index = strPartition.indexOf('=');
                        if (index < 0) {
                            continue;
                        }
                        partitionSet.add(strPartition.substring(index + 1));
                    }
                    rs.close();
                    LOG.info("find id:{},partitions:{}", entry.getKey(), partitionSet);
                    // close partition
                    long currentTime = System.currentTimeMillis();
                    long beginScanTime = currentTime
                            - 2 * idConfig.getMaxPartitionOpenDelayHour() * HdfsIdConfig.HOUR_MS;
                    long endScanTime = currentTime - context.getMaxFileOpenDelayMinute() * HiveSinkContext.MINUTE_MS;
                    long forceCloseTime = currentTime - idConfig.getMaxPartitionOpenDelayHour() * HdfsIdConfig.HOUR_MS;
                    LOG.info("start to PartitionLeaderElectionRunnable scan:beginScanTime:{},"
                            + "endScanTime:{},getPartitionIntervalMs:{}",
                            beginScanTime, endScanTime, idConfig.getPartitionIntervalMs());
                    for (long pt = beginScanTime; pt < endScanTime; pt += idConfig.getPartitionIntervalMs()) {
                        String strPartitionValue = idConfig.parsePartitionField(pt);
                        if (partitionSet.contains(strPartitionValue)) {
                            continue;
                        }
                        boolean isForce = (pt < forceCloseTime);
                        // force to close partition that has overtimed.
                        // try to close partition that has no new data and can be closed.
                        // clear "outtmp" directory.
                        // merge and copy files in "in" directory to "outtmp" directory.
                        // rename outtmp file to "out" directory.
                        // delete file in "in" directory.
                        // execute the sql of adding partition.
                        String inlongGroupId = idConfig.getInlongGroupId();
                        String inlongStreamId = idConfig.getInlongStreamId();
                        String uid = InlongId.generateUid(inlongGroupId, inlongStreamId);
                        String uidPartitionKey = uid + "." + strPartitionValue;

                        PartitionCreateRunnable createTask = this.partitionCreateMap.get(uidPartitionKey);
                        LOG.info("start to PartitionLeaderElectionRunnable createTask:{},isForce:{}", uidPartitionKey,
                                isForce);
                        if (createTask != null) {
                            createTask.setForce(isForce);
                            continue;
                        }
                        createTask = new PartitionCreateRunnable(context, idConfig,
                                strPartitionValue, pt, isForce);
                        this.partitionCreateMap.put(uidPartitionKey, createTask);
                        partitionCreatePool.execute(createTask);

                    }
                }
            }
            stat.close();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * hasToken
     * 
     * @param  idConfig
     * @return
     */
    private boolean hasToken(HdfsIdConfig idConfig) {
        String containerName = context.getNodeId();
        String strHdfsPath = context.getHdfsPath();
        String strIdRootPath = strHdfsPath + idConfig.getIdRootPath();
        String strTokenFile = strIdRootPath + "/token";
        FileSystem fs = null;
        try {
            // check if token file exists
            Path hdfsRootPath = new Path(strHdfsPath);
            fs = hdfsRootPath.getFileSystem(new Configuration());
            Path tokenPath = new Path(strTokenFile);
            if (!fs.exists(tokenPath)) {
                // create token file
                FSDataOutputStream fsdos = fs.create(tokenPath, true);
                // write container name to file
                fsdos.write(containerName.getBytes());
                fsdos.flush();
                fsdos.close();
                LOG.info("node:{} get id token:inlongGroupId:{},inlongStreamId:{} because token file is not existed.",
                        containerName, idConfig.getInlongGroupId(), idConfig.getInlongStreamId());
                return true;
            } else {
                // check if last modified time of token file is over
                FileStatus tokenFileStatus = fs.getFileStatus(tokenPath);
                long tokenOvertime = context.getTokenOvertimeMinute() * HiveSinkContext.MINUTE_MS;
                long currentTime = System.currentTimeMillis();
                long accessTime = tokenFileStatus.getAccessTime();
                long modifiedTime = tokenFileStatus.getModificationTime();
                if (currentTime - modifiedTime < tokenOvertime
                        && tokenFileStatus.getLen() < HiveSinkContext.KB_BYTES) {
                    // check if leader is same with local container name
                    FSDataInputStream fsdis = fs.open(tokenPath);
                    byte[] leaderBytes = new byte[(int) tokenFileStatus.getLen()];
                    int readLen = fsdis.read(leaderBytes);
                    fsdis.close();
                    String leaderName = new String(leaderBytes, 0, readLen);
                    LOG.info("node:{},leader:{},containerNameLength:{},leaderNameLength:{},"
                            + "leaderBytesLength:{}",
                            containerName, leaderName, containerName.length(), leaderName.length(),
                            leaderBytes.length);
                    if (leaderName.equals(containerName)) {
                        // rewrite container name to file
                        FSDataOutputStream fsdos = fs.create(tokenPath, true);
                        fsdos.write(containerName.getBytes());
                        fsdos.flush();
                        fsdos.close();
                        // "leader rewrite file.";
                        LOG.info("node:{} get id token:inlongGroupId:{},inlongStreamId:{} "
                                + "because leader rewrite file:"
                                + "currentTime:{},accessTime:{},modifiedTime:{},tokenOvertime:{}.",
                                containerName, idConfig.getInlongGroupId(), idConfig.getInlongStreamId(),
                                currentTime, accessTime, modifiedTime, tokenOvertime);
                        return true;
                    } else {
                        // "leader is the other container.";
                        LOG.info("node:{},leader:{},inlongGroupId:{},inlongStreamId:{} "
                                + "because leader is the other container:"
                                + "currentTime:{},accessTime:{},modifiedTime:{},tokenOvertime:{}.",
                                containerName, leaderName, idConfig.getInlongGroupId(), idConfig.getInlongStreamId(),
                                currentTime, accessTime, modifiedTime, tokenOvertime);
                        return false;
                    }
                } else {
                    // override container name to file
                    FSDataOutputStream fsdos = fs.create(tokenPath, true);
                    fsdos.write(containerName.getBytes());
                    fsdos.flush();
                    fsdos.close();
                    // "leader is overtime, current container become to leader.";
                    LOG.info("node:{} get id token:inlongGroupId:{},inlongStreamId:{} because leader is overtime:"
                            + "currentTime:{},accessTime:{},modifiedTime:{},tokenOvertime:{}.",
                            containerName, idConfig.getInlongGroupId(), idConfig.getInlongStreamId(),
                            currentTime, accessTime, modifiedTime, tokenOvertime);
                    return true;
                }
            }
        } catch (Exception e) {
            LOG.error("node:{} is fail to get id token:inlongGroupId:{},inlongStreamId:{} because of error:{}",
                    containerName, idConfig.getInlongGroupId(), idConfig.getInlongStreamId(), e);
            return false;
        } finally {
            if (fs != null) {
                try {
                    fs.close();
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
    }
}
