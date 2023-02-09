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
**/

package com.obs.services.model;

/**
 * Information about the versioning object to be deleted
 */
public class KeyAndVersion {
    private String key;
    private String version;

    /**
     * Constructor
     * 
     * @param key
     *            Object name
     * @param version
     *            Version ID of the object
     */
    public KeyAndVersion(String key, String version) {
        this.key = key;
        this.version = version;
    }

    /**
     * Constructor
     * 
     * @param key
     *            Object name
     */
    public KeyAndVersion(String key) {
        this(key, null);
    }

    /**
     * Obtain the object name.
     * 
     * @return Object name
     */
    public String getKey() {
        return key;
    }

    /**
     * Obtain the object version ID.
     * 
     * @return Version ID of the object
     */
    public String getVersion() {
        return version;
    }

    /**
     * Set the object name.
     * 
     * @param key
     *            Object name
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Set the version ID of the object.
     * 
     * @param version
     *            Version ID of the object
     */
    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "KeyAndVersion [key=" + key + ", version=" + version + "]";
    }

}
