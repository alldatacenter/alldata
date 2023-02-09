/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.obs.services.internal.utils.AccessLoggerUtils;
import com.obs.services.internal.utils.ServiceUtils;

/**
 * Log configuration class that uses the standard JDK log library
 *
 */
public class LogConfigurator {

    public static final Level OFF = Level.parse("OFF");

    public static final Level TRACE = Level.parse("FINEST");

    public static final Level DEBUG = Level.parse("FINE");

    public static final Level INFO = Level.parse("INFO");

    public static final Level WARN = Level.parse("WARNING");

    public static final Level ERROR = Level.parse("SEVERE");

    private static final Logger ILOG = Logger.getLogger(LogConfigurator.class.getName());

    static {
        LogConfigurator.disableLog();
        LogConfigurator.disableAccessLog();
    }

    private static Level logLevel;

    private static String logFileDir;

    private static int logFileSize = 30 * 1024 * 1024; // 30MB

    private static int logFileRolloverCount = 50;

    private static volatile boolean logEnabled = false;

    private static volatile boolean accessLogEnabled = false;

    private static String getDefaultLogFileDir() {
        try {
            Class<?> c = Class.forName("android.os.Environment");
            Method m = c.getMethod("getExternalStorageDirectory");
            if (m != null) {
                return m.invoke(c).toString() + "/logs";
            }
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException
                | IllegalAccessException e) {
            ILOG.warning(e.getMessage());
        }
        return System.getProperty("user.dir") + "/logs";
    }

    private synchronized static void logOn(final Logger currentLogger, String logName) {
        currentLogger.setUseParentHandlers(false);
        currentLogger.setLevel(logLevel == null ? LogConfigurator.WARN : logLevel);
        if (logFileDir == null) {
            logFileDir = getDefaultLogFileDir();
        }
        try {
            FileHandler fh = createFileHandler(currentLogger, logName);
            currentLogger.addHandler(fh);
            if (currentLogger == AccessLog.getLogger()) {
                accessLogEnabled = true;
            } else if (currentLogger == BaseLog.getLogger()) {
                logEnabled = true;
            }
        } catch (IOException e) {
            onException(currentLogger, e);
        }
    }

    private static void onException(final Logger currentLogger, IOException e) {
        try {
            Class<?> c = Class.forName("android.util.Log");
            try {
                Method m = c.getMethod("i", String.class, String.class, Throwable.class);
                m.invoke(null, "OBS Android SDK", "Enable SDK log failed", e);
            } catch (NoSuchMethodException | SecurityException ex) {
                Method m = c.getMethod("i", String.class, String.class);
                m.invoke(null, "OBS Android SDK", "Enable SDK log failed" + e.getMessage());
            }
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException
                | IllegalAccessException ex) {
            ILOG.warning(e.getMessage());
        }
        logOff(currentLogger);
    }

    private static FileHandler createFileHandler(final Logger currentLogger, String logName)
            throws IOException, UnsupportedEncodingException {
        File dir = new File(logFileDir);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                if (!dir.exists()) {
                    logFileDir = System.getProperty("user.dir") + "/";
                }
            }
        }
        FileHandler fh = new FileHandler(logFileDir + logName, logFileSize, logFileRolloverCount, true);
        fh.setEncoding("UTF-8");
        fh.setFormatter(new Formatter() {

            @Override
            public String format(LogRecord record) {
                String levelName = record.getLevel().getName();
                if ("SEVERE".equals(levelName)) {
                    levelName = "ERROR";
                } else if ("FINE".equals(levelName)) {
                    levelName = "DEBUG";
                } else if ("FINEST".equals(levelName)) {
                    levelName = "TRACE";
                }
                if (currentLogger == AccessLog.getLogger()) {
                    return Thread.currentThread().getName() + "\n" + record.getMessage()
                            + (record.getThrown() == null ? "" : record.getThrown())
                            + System.getProperty("line.separator");
                }
                Date d = new Date(record.getMillis());

                SimpleDateFormat format = AccessLoggerUtils.getFormat();

                return format.format(d) + "|" + Thread.currentThread().getName() + "|" + levelName + " |"
                        + record.getMessage() + (record.getThrown() == null ? "" : record.getThrown())
                        + System.getProperty("line.separator");
            }
        });
        return fh;
    }

    private static void logOff(Logger currentLogger) {
        currentLogger.setLevel(LogConfigurator.OFF);
        Handler[] handlers = currentLogger.getHandlers();
        if (handlers != null) {
            for (Handler handler : handlers) {
                currentLogger.removeHandler(handler);
            }
        }
        if (currentLogger == AccessLog.getLogger()) {
            accessLogEnabled = false;
        } else if (currentLogger == BaseLog.getLogger()) {
            logEnabled = false;
        }
    }

    /**
     * Enable SDK logging.
     */
    public synchronized static void enableLog() {
        if (logEnabled) {
            logOff(BaseLog.getLogger());
        }
        logOn(BaseLog.getLogger(), "/OBS-SDK.log");
    }

    /**
     * Disable SDK logging.
     */
    protected synchronized static void disableLog() {
        logOff(BaseLog.getLogger());
    }

    public synchronized static void enableAccessLog() {
        if (accessLogEnabled) {
            logOff(AccessLog.getLogger());
        }
        logOn(AccessLog.getLogger(), "/OBS-SDK-access.log");
    }

    protected synchronized static void disableAccessLog() {
        logOff(AccessLog.getLogger());
    }

    /**
     * Set the log level.
     * 
     * @param level
     *            Log level
     */
    public synchronized static void setLogLevel(Level level) {
        if (level != null) {
            logLevel = level;
        }
    }

    /**
     * Set the number of retained log files.
     * 
     * @param count
     *            Number of retained log files
     */
    public synchronized static void setLogFileRolloverCount(int count) {
        if (count > 0) {
            logFileRolloverCount = count;
        }
    }

    /**
     * Set the log file size (in bytes).
     * 
     * @param fileSize
     *            Log file size
     */
    public synchronized static void setLogFileSize(int fileSize) {
        if (fileSize >= 0) {
            logFileSize = fileSize;
        }
    }

    /**
     * Set a directory for saving log files.
     * 
     * @param dir
     *            Directory for saving log files
     */
    public synchronized static void setLogFileDir(String dir) {
        if (ServiceUtils.isValid(dir)) {
            logFileDir = dir;
        }
    }

    private static class BaseLog {
        private static final Logger logger = Logger.getLogger("com.obs");
        
        public static Logger getLogger() {
            return logger;
        }
    }
    
    private static class AccessLog {
        private static final Logger logger = Logger.getLogger("com.obs.log.AccessLogger");
        
        public static Logger getLogger() {
            return logger;
        }
    }
}
