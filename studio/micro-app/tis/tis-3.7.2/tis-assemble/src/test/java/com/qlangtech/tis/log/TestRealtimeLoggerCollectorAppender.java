/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.log;

import ch.qos.logback.classic.spi.LoggingEvent;
import com.qlangtech.tis.BaseTestCase;
import com.qlangtech.tis.job.common.JobCommon;
import com.qlangtech.tis.rpc.server.FullBuildStatCollectorServer;
import com.qlangtech.tis.rpc.server.IncrStatusUmbilicalProtocolImpl;
import com.qlangtech.tis.trigger.jst.MonotorTarget;
import com.qlangtech.tis.trigger.socket.LogType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @create: 2020-05-11 13:04
 */
public class TestRealtimeLoggerCollectorAppender extends BaseTestCase {

    private static final int taskid = 123;

    private static final String collection = "search4totalpay";

    private static final Logger logger = LoggerFactory.getLogger(TestRealtimeLoggerCollectorAppender.class);

    public void testIncrProcessLogger() {
        MonotorTarget mtarget = MonotorTarget.createRegister(collection, LogType.INCR);
        AtomicBoolean closed = new AtomicBoolean(false);
        final AtomicInteger receivedCount = new AtomicInteger();
        FullBuildStatCollectorServer.addListener(mtarget, new RealtimeLoggerCollectorAppender.LoggerCollectorAppenderListener() {

            @Override
            public void readLogTailer(RealtimeLoggerCollectorAppender.LoggingEventMeta meta, File logFile) {
            }

            @Override
            public void process(RealtimeLoggerCollectorAppender.LoggingEventMeta mtarget, LoggingEvent e) {
                receivedCount.incrementAndGet();
            }

            @Override
            public boolean isClosed() {
                return closed.get();
            }
        });
        IncrStatusUmbilicalProtocolImpl.setCollectionName(collection);
        IncrStatusUmbilicalProtocolImpl.statisLog.info("incr_1");
        IncrStatusUmbilicalProtocolImpl.statisLog.info("incr_2");
        String loggerName = "incr-" + collection;
        RealtimeLoggerCollectorAppender bufferAppender = RealtimeLoggerCollectorAppender.getBufferAppender(loggerName);
        assertNotNull(bufferAppender);
        assertEquals(2, receivedCount.get());
    }

    /**
     * 测试全量构建日志
     */
    public void testFullBuildLogger() {

        JobCommon.setMDC(taskid);
        logger.info("start");
        String loggerName = "full-" + taskid;
        RealtimeLoggerCollectorAppender bufferAppender = RealtimeLoggerCollectorAppender.getBufferAppender(loggerName);
        assertNotNull(bufferAppender);
        AtomicBoolean closed = new AtomicBoolean(false);
        final AtomicInteger receivedCount = new AtomicInteger();
        MonotorTarget mtarget = MonotorTarget.createRegister("", LogType.FULL);
        mtarget.setTaskid(taskid);
        String logMsg = "start to log";
        AtomicInteger logIndex = new AtomicInteger();
        FullBuildStatCollectorServer.addListener(mtarget, new RealtimeLoggerCollectorAppender.LoggerCollectorAppenderListener() {

            @Override
            public void readLogTailer(RealtimeLoggerCollectorAppender.LoggingEventMeta meta, File logFile) {
            }

            @Override
            public void process(RealtimeLoggerCollectorAppender.LoggingEventMeta mtarget, LoggingEvent e) {
                System.out.println(e.getFormattedMessage());
                assertEquals(logMsg + logIndex.getAndIncrement(), e.getFormattedMessage());
                receivedCount.incrementAndGet();
            }

            @Override
            public boolean isClosed() {
                return closed.get();
            }
        });
        int i = 0;
        logger.info("start to log{}", i++);
        logger.info("start to log{}", i++);
        closed.set(true);
        logger.info("start to log{}", i++);
        RealtimeLoggerCollectorAppender.LogTypeListeners logTypeListeners = RealtimeLoggerCollectorAppender.appenderListener.getLogTypeListeners(loggerName);
        assertNull(logTypeListeners);
        // for (LoggingEvent o : bufferAppender.cb.asList()) {
        //
        // System.out.println("=======" + o.getTimeStamp() + o + "," + o.getClass());
        // }
        System.out.println();
        // assertEquals(512, bufferAppender.getMaxSize());
        // assertEquals(3, bufferAppender.cb.length());
        assertEquals(2, receivedCount.get());
    }
}
