/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 **/

package com.obs.services.model;

import java.util.List;

/**
 * Response to a request for listing bucket alias
 *
 */
public class ListBucketAliasResult extends HeaderResponse {

    private List<BucketAlias> bucketAlias;

    private Owner owner;

    public ListBucketAliasResult(List<BucketAlias> bucketAlias, Owner owner) {
        this.bucketAlias = bucketAlias;
        this.owner = owner;
    }

    public static class BucketAlias {
        private String alias;

        private List<String> bucketList;

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        public List<String> getBucketList() {
            return bucketList;
        }

        public void setBucketList(List<String> bucketList) {
            this.bucketList = bucketList;
        }

        @Override
        public String toString() {
            return "BucketAlias{" +
                    "alias='" + alias + '\'' +
                    ", bucketList=" + bucketList +
                    '}';
        }
    }

    public List<BucketAlias> getBucketAlias() {
        return bucketAlias;
    }

    public void setBucketAlias(List<BucketAlias> bucketAlias) {
        this.bucketAlias = bucketAlias;
    }

    public Owner getOwner() {
        return owner;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    @Override
    public String toString() {
        return "ListBucketAliasResult{" +
                "bucketAlias=" + bucketAlias +
                ", owner=" + owner +
                '}';
    }
}

