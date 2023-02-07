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

package org.apache.inlong.sort.standalone.config.loader;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.io.IOUtils;
import org.apache.flume.Context;
import org.apache.inlong.common.pojo.sortstandalone.SortClusterConfig;
import org.apache.inlong.sort.standalone.config.holder.SortClusterConfigType;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * 
 * ClassResourceCommonPropertiesLoader
 */
public class ClassResourceSortClusterConfigLoader implements SortClusterConfigLoader {

    public static final Logger LOG = InlongLoggerFactory.getLogger(ClassResourceSortClusterConfigLoader.class);

    private Context context;

    /**
     * load
     * 
     * @return
     */
    @Override
    public SortClusterConfig load() {
        String fileName = SortClusterConfigType.DEFAULT_FILE;
        try {
            if (context != null) {
                fileName = context.getString(SortClusterConfigType.KEY_FILE, SortClusterConfigType.DEFAULT_FILE);
            }
            String confString = IOUtils.toString(getClass().getClassLoader().getResource(fileName),
                    Charset.defaultCharset());
            int index = confString.indexOf('{');
            confString = confString.substring(index);
            ObjectMapper objectMapper = new ObjectMapper();
            SortClusterConfig config = objectMapper.readValue(confString, SortClusterConfig.class);
            return config;
        } catch (UnsupportedEncodingException e) {
            LOG.error("fail to load properties, file ={}, and e= {}", fileName, e);
        } catch (Exception e) {
            LOG.error("fail to load properties, file ={}, and e= {}", fileName, e);
        }
        return SortClusterConfig.builder().build();
    }

    /**
     * configure
     * 
     * @param context
     */
    @Override
    public void configure(Context context) {
        this.context = context;
    }
}
