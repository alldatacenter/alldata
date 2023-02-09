/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.model;

import java.util.ArrayList;
import java.util.List;
import com.aliyun.oss.model.SetBucketCORSRequest.CORSRule;

public class CORSConfiguration  {

    private List<CORSRule> corsRules = new ArrayList<CORSRule>();

    private Boolean responseVary;

    public List<CORSRule> getCorsRules() {
        return this.corsRules;
    }

    public void setCorsRules(List<CORSRule> rules) {
        this.corsRules = rules;
    }

    public Boolean getResponseVary() {
        return this.responseVary;
    }

    public void setResponseVary(Boolean responseVary) {
        this.responseVary = responseVary;
    }
}
