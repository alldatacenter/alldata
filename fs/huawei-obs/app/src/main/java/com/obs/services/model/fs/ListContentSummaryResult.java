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

import com.obs.services.model.HeaderResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Response to a request for obtaining folder contentSummary
 *
 */
public class ListContentSummaryResult extends HeaderResponse {
    private List<FolderContentSummary> folderContentSummaries;

    private String bucketName;

    private boolean truncated;

    private String prefix;

    private String marker;

    private int maxKeys;

    private String delimiter;

    private String nextMarker;

    private String location;

    @Deprecated
    //CHECKSTYLE:OFF
    public ListContentSummaryResult(List<FolderContentSummary> folderContentSummaries, String bucketName,
        boolean truncated, String prefix, String marker, int maxKeys, String delimiter, String nextMarker,
        String location) {
        super();
        this.folderContentSummaries = folderContentSummaries;
        this.bucketName = bucketName;
        this.truncated = truncated;
        this.prefix = prefix;
        this.marker = marker;
        this.maxKeys = maxKeys;
        this.delimiter = delimiter;
        this.nextMarker = nextMarker;
        this.location = location;
    }

    private ListContentSummaryResult(Builder builder) {
        super();
        this.folderContentSummaries = builder.folderContentSummaries;
        this.bucketName = builder.bucketName;
        this.truncated = builder.truncated;
        this.prefix = builder.prefix;
        this.marker = builder.marker;
        this.maxKeys = builder.maxKeys;
        this.delimiter = builder.delimiter;
        this.nextMarker = builder.nextMarker;
        this.location = builder.location;
    }
    
    public static final class Builder {
        private List<FolderContentSummary> folderContentSummaries;
        private String bucketName;
        private boolean truncated;
        private String prefix;
        private String marker;
        private int maxKeys;
        private String delimiter;
        private String nextMarker;
        private String location;
        
        public Builder folderContentSummaries(List<FolderContentSummary> folderContentSummaries) {
            this.folderContentSummaries = folderContentSummaries;
            return this;
        }
        
        public Builder bucketName(String bucketName) {
            this.bucketName = bucketName;
            return this;
        }
        
        public Builder truncated(boolean truncated) {
            this.truncated = truncated;
            return this;
        }
        
        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }
        
        public Builder marker(String marker) {
            this.marker = marker;
            return this;
        }
        
        public Builder maxKeys(int maxKeys) {
            this.maxKeys = maxKeys;
            return this;
        }
        
        public Builder delimiter(String delimiter) {
            this.delimiter = delimiter;
            return this;
        }
        
        public Builder nextMarker(String nextMarker) {
            this.nextMarker = nextMarker;
            return this;
        }
        
        public Builder location(String location) {
            this.location = location;
            return this;
        }
        
        public ListContentSummaryResult builder() {
            return new ListContentSummaryResult(this);
        }
    }
    
    /**
     * 获取下次请求的起始位置
     * @return 下次请求的起始位置标识
     */
    public String getNextMarker() {
        return nextMarker;
    }

    /**
     * 获取桶内目录统计信息列表
     * @return 桶内目录统计信息列表
     */
    public List<FolderContentSummary> getFolderContentSummaries() {
        if (this.folderContentSummaries == null) {
            this.folderContentSummaries = new ArrayList<FolderContentSummary>();
        }
        return folderContentSummaries;
    }

    /**
     * 获取桶名
     *
     * @return 桶名
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * 判断查询结果列表是否被截断。true表示截断，本次没有返回全部结果；false表示未截断，本次已经返回了全部结果。
     * @return 截断标识
     */
    public boolean isTruncated() {
        return truncated;
    }

    /**
     * 获取列举目录统计信息请求中的对象名前缀
     * @return 请求中的对象名前缀
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * 获取列举目录统计信息请求中的起始位置
     * @return 请求中的起始位置标识
     */
    public String getMarker() {
        return marker;
    }

    /**
     * 获取列举目录统计信息的最大条目数
     * @return 列举目录统计信息的最大条目数
     */
    public int getMaxKeys() {
        return maxKeys;
    }

    /**
     * 获取列举目录统计信息时请求中的分组字符
     *
     * @return 分组字符
     */
    public String getDelimiter() {
        return delimiter;
    }

    /**
     * 获取桶的区域位置
     * @return 桶的区域位置
     */
    public String getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "ContentSummaryResult [folderContentSummaries=" + folderContentSummaries + ", bucketName=" + bucketName
            + ", truncated=" + truncated + ", prefix=" + prefix + ", marker=" + marker + ", maxKeys=" + maxKeys
            + ", delimiter=" + delimiter
            + ", nextMarker=" + nextMarker + ", location=" + location + "]";
    }

}
