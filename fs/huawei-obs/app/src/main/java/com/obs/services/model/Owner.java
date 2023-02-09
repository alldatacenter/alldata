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
 */

package com.obs.services.model;

/**
 * Bucket or object owner
 */
public class Owner {
    private String displayName;

    private String id;

    /**
     * Obtain the owner name.
     * 
     * @return Owner name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Set the owner name.
     * 
     * @param displayName
     *            Owner name
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Obtain the ID of the domain to which the owner belongs.
     * 
     * @return ID of the domain to which the owner belongs
     */
    public String getId() {
        return id;
    }

    /**
     * Set the ID of the domain to which the owner belongs.
     * 
     * @param id
     *            ID of the domain to which the owner belongs
     */
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Owner [displayName=" + displayName + ", id=" + id + "]";
    }

}
