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

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.inlong.sort.standalone.channel.ProfileEvent;
import org.apache.inlong.sort.standalone.dispatch.DispatchProfile;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;
import org.slf4j.Logger;

/**
 * 
 * WriteHdfsFileRunnable
 */
public class WriteHdfsFileRunnable implements Runnable {

    public static final Logger LOG = InlongLoggerFactory.getLogger(WriteHdfsFileRunnable.class);

    private final HiveSinkContext context;
    private final HdfsIdFile idFile;
    private final DispatchProfile profile;
    private final long sendTime;

    /**
     * Constructor
     * 
     * @param context
     * @param idFile
     * @param profile
     */
    public WriteHdfsFileRunnable(HiveSinkContext context, HdfsIdFile idFile, DispatchProfile profile) {
        this.context = context;
        this.idFile = idFile;
        this.profile = profile;
        this.sendTime = System.currentTimeMillis();
    }

    /**
     * run
     */
    @Override
    public void run() {
        synchronized (idFile) {
            if (!idFile.isOpen()) {
                context.addSendResultMetric(profile, context.getTaskName(), false, sendTime);
                context.getDispatchQueue().offer(profile);
                return;
            }
            try {
                IEventFormatHandler handler = context.getEventFormatHandler();
                FSDataOutputStream output = idFile.getIntmpOutput();
                for (ProfileEvent event : profile.getEvents()) {
                    byte[] formatBytes = handler.format(event, idFile.getIdConfig());
                    output.write(formatBytes);
                    output.writeByte(HdfsIdFile.SEPARATOR_MESSAGE);
                }
                output.flush();
                context.addSendResultMetric(profile, context.getTaskName(), true, sendTime);
                profile.ack();
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                context.addSendResultMetric(profile, context.getTaskName(), false, sendTime);
                context.getDispatchQueue().offer(profile);
            }
        }
    }
}
