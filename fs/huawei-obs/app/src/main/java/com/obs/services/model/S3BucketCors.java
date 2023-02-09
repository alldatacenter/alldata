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

package com.obs.services.model;

import java.util.ArrayList;
import java.util.List;


public class S3BucketCors extends HeaderResponse {

    private List<BucketCorsRule> rules;

    public S3BucketCors() {
    }

    public S3BucketCors(List<BucketCorsRule> rules) {
        this.rules = rules;
    }

    /**
     * Obtain the bucket CORS rule list.
     * 
     * @return Bucket CORS rule list
     */
    public List<BucketCorsRule> getRules() {
        if (null == rules) {
            rules = new ArrayList<BucketCorsRule>();
        }
        return rules;
    }

    /**
     * Configure the bucket CORS rule list.
     * 
     * @param rules
     *            Bucket CORS rule list
     */
    public void setRules(List<BucketCorsRule> rules) {
        this.rules = rules;
    }

    @Override
    public String toString() {
        return "ObsBucketCors [rules=" + rules + "]";
    }

}
