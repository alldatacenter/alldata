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

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(SortSourceStreamInfo.class);
    String sortClusterName;
    String sortTaskName;
    String groupId;
    String extParams;
    Map<String, String> extParamsMap;

    public Map<String, String> getExtParamsMap() {
        if (extParamsMap != null) {
            return extParamsMap;
        }
        if (StringUtils.isNotBlank(extParams)) {
            try {
                Gson gson = new Gson();
                extParamsMap = gson.fromJson(extParams, Map.class);
            } catch (Throwable t) {
                LOGGER.error("fail to parse source stream ext params", t);
                extParamsMap = new ConcurrentHashMap<>();
            }
        }
        return extParamsMap;
    }
}
