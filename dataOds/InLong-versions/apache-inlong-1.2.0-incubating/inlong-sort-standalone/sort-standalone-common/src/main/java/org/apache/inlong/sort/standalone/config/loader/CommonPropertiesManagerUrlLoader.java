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

package org.apache.inlong.sort.standalone.config.loader;

import org.apache.commons.lang.StringUtils;
import org.apache.flume.Context;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Default ManagerUrlLoader. acquire URLs from common.properties.
 */
public class CommonPropertiesManagerUrlLoader implements ManagerUrlLoader {

    private static final Logger LOG = InlongLoggerFactory.getLogger(CommonPropertiesManagerUrlLoader.class);
    private static final String KEY_SORT_CLUSTER_CONFIG_MANAGER_URL = "sortClusterConfig.managerUrl";
    private static final String KEY_SORT_SOURCE_CONFIG_MANAGER_URL = "sortSourceConfig.managerUrl";

    private String sortSourceConfigUrl;
    private String sortClusterConfigUrl;
    public Context context;

    @Override
    public String acquireSortSourceConfigUrl() {
        if (sortSourceConfigUrl != null) {
            return sortSourceConfigUrl;
        }
        sortSourceConfigUrl = context.getString(KEY_SORT_SOURCE_CONFIG_MANAGER_URL);
        if (StringUtils.isBlank(sortSourceConfigUrl)) {
            String warnMsg = "Get key" + KEY_SORT_SOURCE_CONFIG_MANAGER_URL
                    + " from CommonPropertiesHolder failed, it's a optional property use to specify "
                    + "the url where SortSdk request SortSourceConfig.";
            LOG.warn(warnMsg);
            sortSourceConfigUrl = warnMsg;
        }
        return sortSourceConfigUrl;
    }

    @Override
    public String acquireSortClusterConfigUrl() {
        if (sortClusterConfigUrl != null) {
            return sortClusterConfigUrl;
        }
        sortClusterConfigUrl = context.getString(KEY_SORT_CLUSTER_CONFIG_MANAGER_URL);
        if (StringUtils.isBlank(sortClusterConfigUrl)) {
            String warnMsg = "Get key" + KEY_SORT_CLUSTER_CONFIG_MANAGER_URL
                    + " from CommonPropertiesHolder failed, it's a optional property use to specify "
                    + "the url where Sort-Standalone request SortSourceConfig.";
            LOG.warn(warnMsg);
            sortClusterConfigUrl = warnMsg;
        }
        return sortClusterConfigUrl;
    }

    @Override
    public void configure(Context context) {
        Optional.ofNullable(context).ifPresent(c -> this.context = c);
    }
}
