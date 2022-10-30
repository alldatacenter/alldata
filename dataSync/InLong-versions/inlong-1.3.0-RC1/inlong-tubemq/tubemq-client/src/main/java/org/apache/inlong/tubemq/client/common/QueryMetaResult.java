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

package org.apache.inlong.tubemq.client.common;

import java.util.HashMap;
import java.util.Map;
import org.apache.inlong.tubemq.corebase.rv.RetValue;

public class QueryMetaResult extends RetValue {

    Map<String, Boolean> partStatusMap = new HashMap<>();

    public QueryMetaResult() {
        super();
    }

    public QueryMetaResult(QueryMetaResult other) {
        super(other);
    }

    public QueryMetaResult(int errCode, String errInfo) {
        super(errCode, errInfo);
    }

    @Override
    public void setFailResult(int errCode, final String errMsg) {
        super.setFailResult(errCode, errMsg);
    }

    @Override
    public void setFailResult(final String errMsg) {
        super.setFailResult(errMsg);
    }

    public void setSuccResult(Map<String, Boolean> partStatusMap) {
        super.setSuccResult();
        this.partStatusMap = partStatusMap;
    }

    public Map<String, Boolean> getPartStatusMap() {
        return partStatusMap;
    }

    public void clear() {
        super.clear();
    }
}
