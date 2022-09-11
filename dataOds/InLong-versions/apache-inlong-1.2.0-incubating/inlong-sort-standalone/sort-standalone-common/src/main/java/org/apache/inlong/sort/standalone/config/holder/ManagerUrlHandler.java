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

package org.apache.inlong.sort.standalone.config.holder;

import org.apache.commons.lang.ClassUtils;
import org.apache.flume.Context;
import org.apache.inlong.sort.standalone.config.loader.CommonPropertiesManagerUrlLoader;
import org.apache.inlong.sort.standalone.config.loader.ManagerUrlLoader;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Manager address get handler.
 *
 * <p> Used to acquire the ip and port of manager, which sort sdk and sort-standalone request config from. </p>
 * <p> The default implementation {@link CommonPropertiesManagerUrlLoader}
 * is base on {@link CommonPropertiesHolder} to acquire properties. </p>
 */
public class ManagerUrlHandler {

    private static final Logger LOG = InlongLoggerFactory.getLogger(ManagerUrlHandler.class);
    private static final String KEY_MANAGER_URL_LOADER_TYPE = "managerUrlLoaderType";

    private static ManagerUrlLoader instance;

    /**
     * Delete no argument constructor.
     */
    private ManagerUrlHandler() {

    }

    /**
     * Get URL where SortSdk request SortSourceConfig.
     *
     * @return URL to get SortSourceConfig.
     */
    public static String getSortSourceConfigUrl() {
        return get().acquireSortSourceConfigUrl();
    }

    /**
     * Get URL where Sort-Standalone request SortClusterConfig.
     *
     * @return URL to get SortClusterConfig.
     */
    public static String getSortClusterConfigUrl() {
        return get().acquireSortClusterConfigUrl();
    }

    private static ManagerUrlLoader get() {
        if (instance != null) {
            return instance;
        }
        synchronized (ManagerUrlLoader.class) {
            String loaderType = CommonPropertiesHolder
                    .getString(KEY_MANAGER_URL_LOADER_TYPE, CommonPropertiesManagerUrlLoader.class.getName());
            LOG.info("Start to load ManagerUrlLoader, type is {}.", loaderType);
            try {
                Class<?> handlerClass = ClassUtils.getClass(loaderType);
                Object handlerObj = handlerClass.getDeclaredConstructor().newInstance();
                if (handlerObj instanceof ManagerUrlLoader) {
                    instance = (ManagerUrlLoader) handlerObj;
                }
            } catch (Throwable t) {
                LOG.error("Got exception when load ManagerAddrGetHandler, type is {}, err is {}",
                        loaderType, t.getMessage());
            }
            Optional.ofNullable(instance).ifPresent(inst -> inst.configure(new Context(CommonPropertiesHolder.get())));
        }
        return instance;
    }

}
