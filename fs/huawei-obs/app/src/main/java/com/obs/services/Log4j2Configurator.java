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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * Log configuration class integrated with Log4j2
 *
 */
public class Log4j2Configurator {

    private static volatile boolean isWatchStart = false;

    private static volatile boolean log4j2Enabled = false;

    private static final Logger logger = Logger.getLogger(Log4j2Configurator.class.getName());
    
    /**
     * Configure logs.
     * 
     * @param configPath
     *            Path to the log configuration file
     */
    public static synchronized void setLogConfig(String configPath) {
        setLogConfig(configPath, false);
    }

    /**
     * Configure logs.
     * 
     * @param configPath
     *            Path to the log configuration file
     * @param isWatchConfig
     *            Whether to monitor changes of the log configuration file
     */
    public static synchronized void setLogConfig(String configPath, boolean isWatchConfig) {
        setLogConfig(configPath, isWatchConfig, 60000);
    }

    private static Object getLogContext(String configPath) {
        Object ctx = null;
        FileInputStream configInputStream = null;
        try {
            Class<?> configurationSource = Class.forName("org.apache.logging.log4j.core.config.ConfigurationSource");
            Class<?> configurator = Class.forName("org.apache.logging.log4j.core.config.Configurator");
            Constructor<?> con = configurationSource.getConstructor(InputStream.class);
            Method m = configurator.getMethod("initialize", ClassLoader.class, configurationSource);

            configInputStream = new FileInputStream(configPath);
            ctx = m.invoke(null, null, con.newInstance(configInputStream));
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InvocationTargetException
                | IllegalAccessException | InstantiationException | FileNotFoundException e) {
            logger.warning(e.getMessage());
        } finally {
            if (null != configInputStream) {
                try {
                    configInputStream.close();
                } catch (IOException e) {
                    logger.warning(e.getMessage());
                }
            }
        }
        return ctx;
    }

    private static class LogWatcher extends Thread {
        private Object ctx;
        private long watchInterval;
        private String configPath;

        LogWatcher(String configPath, Object ctx, long watchInterval) {
            this.configPath = configPath;
            this.ctx = ctx;
            this.watchInterval = watchInterval;

        }

        public void run() {
            try {
                Class<?> configuration = Class.forName("org.apache.logging.log4j.core.config.Configuration");
                final Method stop = ctx.getClass().getMethod("stop");
                final Method start = ctx.getClass().getMethod("start", configuration);
                Class<?> xmlConfiguration = Class.forName("org.apache.logging.log4j.core.config.xml.XmlConfiguration");
                Class<?> configurationSource = Class
                        .forName("org.apache.logging.log4j.core.config.ConfigurationSource");
                Constructor<?> configurationSourceConstructor = configurationSource.getConstructor(InputStream.class);
                Constructor<?> xmlConfigurationConstructor = xmlConfiguration.getConstructor(ctx.getClass(),
                        configurationSource);
                while (true) {
                    Thread.sleep(this.watchInterval);
                    stop.invoke(ctx);
                    start.invoke(ctx, xmlConfigurationConstructor.newInstance(ctx,
                            configurationSourceConstructor.newInstance(new FileInputStream(this.configPath))));
                }
            } catch (ClassNotFoundException | FileNotFoundException | NoSuchMethodException | SecurityException
                    | InterruptedException | InvocationTargetException | IllegalAccessException
                    | InstantiationException e) {
                logger.warning(e.getMessage());
            }
        }
    }

    /**
     * Configure logs.
     * 
     * @param configPath
     *            Path to the log configuration file
     * @param isWatchConfig
     *            Whether to monitor changes of the log configuration file
     * @param watchInterval
     *            Interval for monitoring changes of the log configuration file,
     *            in units of ms
     */
    public static synchronized void setLogConfig(String configPath, boolean isWatchConfig, long watchInterval) {
        if (log4j2Enabled) {
            return;
        }
        Object ctx = getLogContext(configPath);
        if (isWatchConfig && ctx != null && !isWatchStart) {
            try {
                isWatchStart = true;
                long interval = watchInterval > 0 ? watchInterval : 60000;
                LogWatcher wather = new LogWatcher(configPath, ctx, interval);
                wather.setDaemon(true);
                wather.start();
            } catch (Exception e) {
                logger.warning(e.getMessage());
            }
        }
        log4j2Enabled = true;
    }

}
