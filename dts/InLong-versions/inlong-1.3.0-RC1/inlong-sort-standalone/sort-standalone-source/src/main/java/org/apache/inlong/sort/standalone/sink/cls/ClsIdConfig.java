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

package org.apache.inlong.sort.standalone.sink.cls;

import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Cls config of each uid.
 */
@Data
public class ClsIdConfig {
    private String inlongGroupId;
    private String inlongStreamId;
    private String separator = "|";
    private String endpoint;
    private String secretId;
    private String secretKey;
    private String topicId;
    private String fieldNames;
    private int fieldOffset = 2;
    private int contentOffset = 0;
    private List<String> fieldList;

    /**
     * Parse fieldNames to list of fields.
     * @return List of fields.
     */
    public List<String> getFieldList() {
        if (fieldList == null) {
            this.fieldList = new ArrayList<>();
            if (fieldNames != null) {
                String[] fieldNameArray = fieldNames.split("\\s+");
                this.fieldList.addAll(Arrays.asList(fieldNameArray));
            }
        }
        return fieldList;
    }
}
