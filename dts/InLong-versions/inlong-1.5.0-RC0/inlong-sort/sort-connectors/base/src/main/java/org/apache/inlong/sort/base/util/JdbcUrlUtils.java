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

package org.apache.inlong.sort.base.util;

import static org.apache.inlong.sort.base.Constants.AUTO_DESERIALIZE_FALSE;
import static org.apache.inlong.sort.base.Constants.AUTO_DESERIALIZE_TRUE;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * utils for jdbc url
 */
public class JdbcUrlUtils {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcUrlUtils.class);

    /**
     * see https://su18.org/post/jdbc-connection-url-attack/
     * @param url
     * @return url after filtering out the invalid property
     */
    public static String replaceInvalidUrlProperty(String url) {
        if (StringUtils.containsIgnoreCase(url, AUTO_DESERIALIZE_TRUE)) {
            LOG.warn("url {} contains invalid property {}, replace it to {}", url,
                    AUTO_DESERIALIZE_TRUE, AUTO_DESERIALIZE_FALSE);
            return StringUtils.replaceIgnoreCase(url, AUTO_DESERIALIZE_TRUE,
                    AUTO_DESERIALIZE_FALSE);
        }
        return url;
    }

}
