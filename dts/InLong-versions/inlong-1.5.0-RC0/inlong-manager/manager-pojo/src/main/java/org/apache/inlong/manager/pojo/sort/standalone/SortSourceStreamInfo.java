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

package org.apache.inlong.manager.pojo.sort.standalone;

import com.google.gson.Gson;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class SortSourceStreamInfo {

    private static final Logger LOGGER = LoggerFactory.getLogger(SortSourceStreamInfo.class);
    private static final Gson GSON = new Gson();

    private String inlongGroupId;
    private String inlongStreamId;
    private String mqResource;
    String extParams;
    Map<String, String> extParamsMap = new ConcurrentHashMap<>();

    public Map<String, String> getExtParamsMap() {
        if (extParamsMap.isEmpty() && StringUtils.isNotBlank(extParams)) {
            try {
                extParamsMap = GSON.fromJson(extParams, Map.class);
            } catch (Throwable t) {
                LOGGER.error("fail to parse group ext params", t);
            }
        }
        return extParamsMap;
    }
}
