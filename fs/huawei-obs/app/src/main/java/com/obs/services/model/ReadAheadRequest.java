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
 * Request parameters for reading ahead objects
 */
public class ReadAheadRequest extends GenericRequest {

    {
        httpMethod = HttpMethodEnum.POST;
    }

    private String prefix;

    private CacheOptionEnum cacheOption;

    private long ttl = 60 * 60 * 24L;

    /**
     * Constructor
     *
     * @param bucketName
     *            Bucket name
     * @param prefix
     *            Name prefix of objects to be read ahead
     */
    public ReadAheadRequest(String bucketName, String prefix) {
        this.bucketName = bucketName;
        this.prefix = prefix;
    }

    /**
     * Constructor
     *
     * @param bucketName
     *            Bucket name
     * @param prefix
     *            Name prefix of objects to be read ahead
     * @param cacheOption
     *            Control option of the read-ahead cache
     * @param ttl
     *            Expiration time of cached data, in seconds. The value ranges
     *            from 0 to 259200 (3 days)
     */
    public ReadAheadRequest(String bucketName, String prefix, CacheOptionEnum cacheOption, long ttl) {
        this.bucketName = bucketName;
        this.prefix = prefix;
        this.cacheOption = cacheOption;
        this.ttl = ttl;
    }

    /**
     * Obtain the name prefix of objects to be read ahead.
     *
     * @return Name prefix of objects to be read ahead
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Set the name prefix of objects to be read ahead.
     *
     * @param prefix
     *            Name prefix of objects to be read ahead
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Obtain the control option of the read-ahead cache.
     *
     * @return Control option of the read-ahead cache
     */
    public CacheOptionEnum getCacheOption() {
        return cacheOption;
    }

    /**
     * Set the control option of the read-ahead cache.
     *
     * @param cacheOption
     *            Control option of the read-ahead cache
     */
    public void setCacheOption(CacheOptionEnum cacheOption) {
        this.cacheOption = cacheOption;
    }

    /**
     * Obtain the cache data expiration time.
     *
     * @return Expiration time of cached data, in seconds
     */
    public long getTtl() {
        return ttl;
    }

    /**
     * Set the cache data expiration time.
     *
     * @param ttl
     *            Expiration time of cached data, in seconds. The value ranges
     *            from 0 to 259200 (72 hours). The default value is 24 hours.
     */
    public void setTtl(long ttl) {
        if (ttl < 0 || ttl > 259200) {
            return;
        }
        this.ttl = ttl;
    }
}
