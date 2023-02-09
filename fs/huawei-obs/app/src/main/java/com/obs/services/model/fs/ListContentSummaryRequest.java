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

package com.obs.services.model.fs;

import com.obs.services.model.GenericRequest;

/**
 * Parameters in a request for obtaining folder contentSummary
 *
 */
public class ListContentSummaryRequest extends GenericRequest {
    private String prefix;

    private String marker;

    private int maxKeys;

    private String delimiter;

    private int listTimeout;

    public ListContentSummaryRequest() {
    }

    /**
     * 构造函数
     *
     *  @param bucketName 桶名
     */
    public ListContentSummaryRequest(String bucketName) {
        this.bucketName = bucketName;
        this.delimiter = "/";
    }

    /**
     * 获取列举对象时的对象名前缀
     *
     * @return 对象名前缀
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * 设置列举对象时的对象名前缀
     *
     * @param prefix 对象名前缀
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * 获取列举对象时的起始位置
     *
     * @return 起始位置标识
     */
    public String getMarker() {
        return marker;
    }

    /**
     * 设置列举对象时的起始位置
     *
     * @param marker 起始位置标识
     */
    public void setMarker(String marker) {
        this.marker = marker;
    }

    /**
     * 获取列举对象的最大条目数
     *
     * @return 列举对象的最大条目数
     */
    public int getMaxKeys() {
        return maxKeys;
    }

    /**
     * 设置列举对象的最大条目数
     *
     * @param maxKeys 列举对象的最大条目数
     */
    public void setMaxKeys(int maxKeys) {
        this.maxKeys = maxKeys;
    }

    /**
     * 获取用于对对象名进行分组的字符
     *
     * @return 分组字符
     */
    public String getDelimiter() {
        return delimiter;
    }

    @Override
    public String toString() {
        return "ListContentSummaryRequest [bucketName=" + bucketName + ", prefix=" + prefix + ", marker=" + marker
            + ", maxKeys=" + maxKeys + ", delimiter=" + delimiter + ", listTimeout=" + listTimeout + "]";
    }

    public int getListTimeout() {
        return listTimeout;
    }

    public void setListTimeout(int listTimeout) {
        this.listTimeout = listTimeout;
    }
}
