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
import ch.qos.logback.core.FileAppender;
import com.qlangtech.tis.web.start.TisAppLaunch;

import java.io.File;
import java.util.*;

/**
 * 全量构建过程中通过的
 *
 * @author 百岁（baisui@qlangtech.com）
 * @create: 2020-05-11 11:32
 */
public class RealtimeLoggerCollectorAppender extends FileAppender<LoggingEvent> {

    // extends CyclicBufferAppender<E>
    private static final Map<String, RealtimeLoggerCollectorAppender> bufferAppenderMap = new HashMap<>();

    public static final LoggerCollectorAppenderListenerWrapper appenderListener = new LoggerCollectorAppenderListenerWrapper();

    /**
     * 取得全量构建loggerBuffer
     *
     * @param
     * @return
     */
    public static void addListener(String targetAppenderName, LoggingEventMeta mtarget, LoggerCollectorAppenderListener listener) {
        appenderListener.addLoggerEventListener(targetAppenderName, mtarget, listener);
    }

    static RealtimeLoggerCollectorAppender getBufferAppender(String name) {
        RealtimeLoggerCollectorAppender appender = bufferAppenderMap.get(name);
        if (appender == null) {
            throw new IllegalStateException("appender:" + name + " relevant appender can not be null,exist keys:" + bufferAppenderMap.keySet());
        }
        return appender;
    }

    @Override
    protected void append(LoggingEvent eventObject) {
        super.append(eventObject);
        if (!isStarted()) {
            return;
        }
        appenderListener.process(this.name, eventObject);
    }

    public interface LoggerCollectorAppenderListener {

        public void process(LoggingEventMeta mtarget, LoggingEvent e);

        /**
         * 初次打开文件，读文件最后几条
         *
         * @param logFile
         */
        public void readLogTailer(LoggingEventMeta meta, File logFile);

        public boolean isClosed();
    }

    public static class LoggingEventMeta {

        public Integer taskid;

        public String collection;

        public int logTypeVal;
    }

    public static class LoggerCollectorAppenderListenerWrapper {

        private final Map<String, LogTypeListeners> targetAppenderLister = new HashMap<>();

        public LogTypeListeners getLogTypeListeners(String targetAppenderName) {
            return targetAppenderLister.get(targetAppenderName);
        }

        public void process(String targetAppenderName, LoggingEvent event) {
            LogTypeListeners targetListeners = targetAppenderLister.get(targetAppenderName);
            if (targetListeners == null) {
                return;
            }
            boolean hasListener = targetListeners.send(event);
            if (!hasListener) {
                synchronized (LoggerCollectorAppenderListenerWrapper.class) {
                    targetAppenderLister.remove(targetAppenderName);
                }
            }
        }

        public void addLoggerEventListener(String targetAppenderName, LoggingEventMeta mtarget, LoggerCollectorAppenderListener l) {
            synchronized (LoggerCollectorAppenderListenerWrapper.class) {
                LogTypeListeners listeners = targetAppenderLister.get(targetAppenderName);
                if (listeners == null) {
                    listeners = new LogTypeListeners(mtarget);
                    targetAppenderLister.put(targetAppenderName, listeners);
                }
                RealtimeLoggerCollectorAppender appender = bufferAppenderMap.get(targetAppenderName);
                File file = null;
                if (appender != null) {
                    file = new File(appender.getFile());
                } else {
                    file = new File(TisAppLaunch.getAssebleTaskDir(), targetAppenderName + ".log");
                }
                if (file.exists()) {
                    l.readLogTailer(mtarget, file);
                }
                listeners.listeners.add(l);
            }
        }
    }

    public static class LogTypeListeners {

        private final LoggingEventMeta mtarget;

        private final List<LoggerCollectorAppenderListener> listeners = new ArrayList<>();

        public LogTypeListeners(LoggingEventMeta mtarget) {
            this.mtarget = mtarget;
        }

        public int getListenerSize() {
            return this.listeners.size();
        }

        boolean send(LoggingEvent event) {
            Iterator<LoggerCollectorAppenderListener> it = listeners.iterator();
            LoggerCollectorAppenderListener listener = null;
            boolean hasListener = false;
            while (it.hasNext()) {
                listener = it.next();
                if (listener.isClosed()) {
                    it.remove();
                } else {
                    listener.process(this.mtarget, event);
                    hasListener = true;
                }
            }
            return hasListener;
        }
    }

    @Override
    public void setName(String name) {
        super.setName(name);
        bufferAppenderMap.put(name, this);
    }
}
