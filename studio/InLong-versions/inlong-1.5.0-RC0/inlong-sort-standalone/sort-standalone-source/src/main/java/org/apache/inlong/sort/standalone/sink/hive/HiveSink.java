/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.Transaction;
import org.apache.flume.conf.Configurable;
import org.apache.flume.sink.AbstractSink;
import org.apache.inlong.sort.standalone.channel.ProfileEvent;
import org.apache.inlong.sort.standalone.dispatch.DispatchManager;
import org.apache.inlong.sort.standalone.dispatch.DispatchProfile;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 
 * HiveSink
 */
public class HiveSink extends AbstractSink implements Configurable {

    private static final Logger LOG = InlongLoggerFactory.getLogger(HiveSink.class);

    private Context parentContext;
    private HiveSinkContext context;
    // message group
    private DispatchManager dispatchManager;
    private LinkedBlockingQueue<DispatchProfile> dispatchQueue = new LinkedBlockingQueue<>();
    // message file
    private Map<String, HdfsIdFile> hdfsIdFileMap = new ConcurrentHashMap<>();
    // scheduled thread pool
    // partition leader election runnable
    // reload
    // dispatch
    private ScheduledExecutorService scheduledPool;

    /**
     * start
     */
    @Override
    public void start() {
        try {
            this.context = new HiveSinkContext(getName(), parentContext, getChannel(), this.dispatchQueue);
            if (getChannel() == null) {
                LOG.error("channel is null");
            }
            this.context.start();
            this.dispatchManager = new DispatchManager(parentContext, dispatchQueue);
            this.scheduledPool = Executors.newScheduledThreadPool(2);
            // dispatch
            this.scheduledPool.scheduleWithFixedDelay(new Runnable() {

                public void run() {
                    dispatchManager.setNeedOutputOvertimeData();
                }
            }, this.dispatchManager.getDispatchTimeout(), this.dispatchManager.getDispatchTimeout(),
                    TimeUnit.MILLISECONDS);
            // partition leader election runnable
            this.scheduledPool.scheduleWithFixedDelay(new PartitionLeaderElectionRunnable(context),
                    0, this.context.getMaxFileOpenDelayMinute() * HiveSinkContext.MINUTE_MS,
                    TimeUnit.MILLISECONDS);
            // process
            this.scheduledPool.scheduleWithFixedDelay(new Runnable() {

                public void run() {
                    try {
                        writeHdfsFile();
                    } catch (Exception e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
            }, this.context.getProcessInterval(), this.context.getProcessInterval(), TimeUnit.MILLISECONDS);
            // close overtime file
            this.scheduledPool.scheduleWithFixedDelay(new Runnable() {

                public void run() {
                    closeOvertimeFile();
                }
            }, 0, this.context.getMaxFileOpenDelayMinute() * HiveSinkContext.MINUTE_MS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        super.start();
    }

    /**
     * stop
     */
    @Override
    public void stop() {
        try {
            this.context.close();
            this.scheduledPool.shutdown();
            super.stop();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * configure
     * 
     * @param context
     */
    @Override
    public void configure(Context context) {
        LOG.info("start to configure:{}, context:{}.", this.getClass().getSimpleName(), context.toString());
        this.parentContext = context;
    }

    /**
     * process
     * 
     * @return                        Status
     * @throws EventDeliveryException
     */
    @Override
    public Status process() throws EventDeliveryException {
        Channel channel = getChannel();
        Transaction tx = channel.getTransaction();
        tx.begin();
        try {
            Event event = channel.take();
            if (event == null) {
                tx.commit();
                return Status.BACKOFF;
            }
            if (!(event instanceof ProfileEvent)) {
                tx.commit();
                this.context.addSendFailMetric();
                return Status.READY;
            }
            //
            ProfileEvent profileEvent = (ProfileEvent) event;
            this.dispatchManager.addEvent(profileEvent);
            tx.commit();
            return Status.READY;
        } catch (Throwable t) {
            LOG.error("Process event failed!" + this.getName(), t);
            try {
                tx.rollback();
            } catch (Throwable e) {
                LOG.error("Channel take transaction rollback exception:" + getName(), e);
            }
            return Status.BACKOFF;
        } finally {
            tx.close();
        }
    }

    /**
     * writeHdfsFile
     * 
     * @throws JsonProcessingException
     */
    private void writeHdfsFile() throws JsonProcessingException {
        // write file
        DispatchProfile dispatchProfile = this.dispatchQueue.poll();
        while (dispatchProfile != null) {
            String uid = dispatchProfile.getUid();
            HdfsIdConfig idConfig = context.getIdConfigMap().get(uid);
            if (idConfig == null) {
                // monitor
                LOG.error("can not find uid:{},idConfigMap:{}", uid,
                        new ObjectMapper().writeValueAsString(context.getIdConfigMap()));
                this.context.addSendResultMetric(dispatchProfile, uid, false, 0);
                dispatchProfile.ack();
                dispatchProfile = this.dispatchQueue.poll();
                continue;
            }
            String strIdRootPath = idConfig.parsePartitionPath(dispatchProfile.getDispatchTime());
            HdfsIdFile idFile = this.hdfsIdFileMap.get(strIdRootPath);
            if (idFile != null && !idFile.isOpen()) {
                this.hdfsIdFileMap.remove(strIdRootPath);
                idFile = null;
            }
            if (idFile == null) {
                try {
                    idFile = new HdfsIdFile(context, idConfig, strIdRootPath);
                } catch (Exception e) {
                    // monitor
                    LOG.error(String.format("can not connect to hdfsPath:%s,write file:%s,error:%s",
                            context.getHdfsPath(), strIdRootPath, e.getMessage()), e);
                    this.context.addSendResultMetric(dispatchProfile, uid, false, 0);
                    this.dispatchQueue.offer(dispatchProfile);
                    dispatchProfile = this.dispatchQueue.poll();
                    continue;
                }
                this.hdfsIdFileMap.put(strIdRootPath, idFile);
            }
            long currentTime = System.currentTimeMillis();
            idFile.setModifiedTime(currentTime);
            // new runnable
            WriteHdfsFileRunnable writeTask = new WriteHdfsFileRunnable(context, idFile, dispatchProfile);
            context.getOutputPool().execute(writeTask);
            context.addSendMetric(dispatchProfile, context.getTaskName());
            dispatchProfile = this.dispatchQueue.poll();
        }
    }

    /**
     * closeOvertimeFile
     */
    private void closeOvertimeFile() {
        // close overtime file
        long currentTime = System.currentTimeMillis();
        long overtime = currentTime - context.getFileArchiveDelayMinute() * HiveSinkContext.MINUTE_MS;
        Set<String> overtimePathSet = new HashSet<>();
        for (Entry<String, HdfsIdFile> entry : this.hdfsIdFileMap.entrySet()) {
            if (entry.getValue().getModifiedTime() < overtime) {
                overtimePathSet.add(entry.getKey());
                entry.getValue().close();
            }
        }
        // remove key
        for (String key : overtimePathSet) {
            HdfsIdFile idFile = this.hdfsIdFileMap.remove(key);
            synchronized (idFile) {
                idFile.close();
            }
        }
    }
}