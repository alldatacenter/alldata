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

import com.aliyun.oss.common.utils.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Life cycle rule class.
 */
public class LifecycleRule {

    public static enum RuleStatus {
        Unknown, Enabled, // The Rule is enabled.
        Disabled // The rule is disabled.
    };

    public static class AbortMultipartUpload {
        private int expirationDays;
        private Date createdBeforeDate;

        public AbortMultipartUpload() {
        }

        public AbortMultipartUpload(int expirationDays) {
            this.expirationDays = expirationDays;
        }

        public AbortMultipartUpload(Date createdBeforeDate) {
            this.createdBeforeDate = createdBeforeDate;
        }

        public int getExpirationDays() {
            return expirationDays;
        }

        public void setExpirationDays(int expirationDays) {
            this.expirationDays = expirationDays;
        }

        public AbortMultipartUpload withExpirationDays(int expirationDays) {
            setExpirationDays(expirationDays);
            return this;
        }

        public boolean hasExpirationDays() {
            return this.expirationDays != 0;
        }

        public Date getCreatedBeforeDate() {
            return createdBeforeDate;
        }

        public void setCreatedBeforeDate(Date createdBeforeDate) {
            this.createdBeforeDate = createdBeforeDate;
        }

        public AbortMultipartUpload withCreatedBeforeDate(Date createdBeforeDate) {
            setCreatedBeforeDate(createdBeforeDate);
            return this;
        }

        public boolean hasCreatedBeforeDate() {
            return this.createdBeforeDate != null;
        }
    }

    public static class StorageTransition {
        private Integer expirationDays;
        private Date createdBeforeDate;
        private StorageClass storageClass;
        private Boolean isAccessTime;
        private Boolean returnToStdWhenVisit;
        private Boolean allowSmallFile;

        public StorageTransition() {
        }

        public StorageTransition(Integer expirationDays, StorageClass storageClass) {
            this.expirationDays = expirationDays;
            this.storageClass = storageClass;
        }

        public StorageTransition(Date createdBeforeDate, StorageClass storageClass) {
            this.createdBeforeDate = createdBeforeDate;
            this.storageClass = storageClass;
        }


        public StorageTransition(Integer expirationDays, StorageClass storageClass, Boolean isAccessTime, Boolean returnToStdWhenVisit) {
            this.expirationDays = expirationDays;
            this.storageClass = storageClass;
            this.isAccessTime = isAccessTime;
            this.returnToStdWhenVisit = returnToStdWhenVisit;
        }

        public StorageTransition(Date createdBeforeDate, StorageClass storageClass, Boolean isAccessTime, Boolean returnToStdWhenVisit) {
            this.createdBeforeDate = createdBeforeDate;
            this.storageClass = storageClass;
            this.isAccessTime = isAccessTime;
            this.returnToStdWhenVisit = returnToStdWhenVisit;
        }

        public Integer getExpirationDays() {
            return expirationDays;
        }

        public void setExpirationDays(Integer expirationDays) {
            this.expirationDays = expirationDays;
        }

        public StorageTransition withExpirationDays(Integer expirationDays) {
            setExpirationDays(expirationDays);
            return this;
        }

        public boolean hasExpirationDays() {
            return this.expirationDays != null;
        }

        public Date getCreatedBeforeDate() {
            return createdBeforeDate;
        }

        public void setCreatedBeforeDate(Date createdBeforeDate) {
            this.createdBeforeDate = createdBeforeDate;
        }

        public StorageTransition withCreatedBeforeDate(Date createdBeforeDate) {
            setCreatedBeforeDate(createdBeforeDate);
            return this;
        }

        public boolean hasCreatedBeforeDate() {
            return this.createdBeforeDate != null;
        }

        public StorageClass getStorageClass() {
            return storageClass;
        }

        public void setStorageClass(StorageClass storageClass) {
            this.storageClass = storageClass;
        }

        public StorageTransition withStrorageClass(StorageClass storageClass) {
            setStorageClass(storageClass);
            return this;
        }

        public boolean hasIsAccessTime() {
            return isAccessTime != null;
        }

        public Boolean getIsAccessTime() {
            return isAccessTime;
        }

        public void setIsAccessTime(Boolean accessTime) {
            isAccessTime = accessTime;
        }

        public StorageTransition withIsAccessTime(Boolean isAccessTime) {
            setIsAccessTime(isAccessTime);
            return this;
        }

        public boolean hasReturnToStdWhenVisit() {
            return returnToStdWhenVisit != null;
        }

        public Boolean getReturnToStdWhenVisit() {
            return returnToStdWhenVisit;
        }

        public void setReturnToStdWhenVisit(Boolean returnToStdWhenVisit) {
            this.returnToStdWhenVisit = returnToStdWhenVisit;
        }

        public StorageTransition withReturnToStdWhenVisit(Boolean returnToStdWhenVisit) {
            setReturnToStdWhenVisit(returnToStdWhenVisit);
            return this;
        }

        public boolean hasAllowSmallFile() {
            return allowSmallFile != null;
        }

        public Boolean getAllowSmallFile() {
            return allowSmallFile;
        }

        public void setAllowSmallFile(Boolean allowSmallFile) {
            this.allowSmallFile = allowSmallFile;
        }
    }

    public static class NoncurrentVersionStorageTransition {
        private Integer noncurrentDays;
        private StorageClass storageClass;
        private Boolean isAccessTime;
        private Boolean returnToStdWhenVisit;
        private Boolean allowSmallFile;

        public NoncurrentVersionStorageTransition() {
        }

        public NoncurrentVersionStorageTransition(Integer noncurrentDays, StorageClass storageClass) {
            this.noncurrentDays = noncurrentDays;
            this.storageClass = storageClass;
        }

        public NoncurrentVersionStorageTransition(Integer noncurrentDays, StorageClass storageClass, Boolean isAccessTime, Boolean returnToStdWhenVisit) {
            this.noncurrentDays = noncurrentDays;
            this.storageClass = storageClass;
            this.isAccessTime = isAccessTime;
            this.returnToStdWhenVisit = returnToStdWhenVisit;
        }

        public Integer getNoncurrentDays() {
            return noncurrentDays;
        }

        public void setNoncurrentDays(Integer noncurrentDays) {
            this.noncurrentDays = noncurrentDays;
        }

        public NoncurrentVersionStorageTransition withNoncurrentDays(Integer noncurrentDays) {
            setNoncurrentDays(noncurrentDays);
            return this;
        }

        public boolean hasNoncurrentDays() {
            return this.noncurrentDays != null;
        }

        public StorageClass getStorageClass() {
            return storageClass;
        }

        public void setStorageClass(StorageClass storageClass) {
            this.storageClass = storageClass;
        }

        public NoncurrentVersionStorageTransition withStrorageClass(StorageClass storageClass) {
            setStorageClass(storageClass);
            return this;
        }

        public boolean hasIsAccessTime() {
            return isAccessTime != null;
        }

        public Boolean getIsAccessTime() {
            return isAccessTime;
        }

        public void setIsAccessTime(Boolean isAccessTime) {
            this.isAccessTime = isAccessTime;
        }

        public NoncurrentVersionStorageTransition withIsAccessTime(Boolean isAccessTime) {
            setIsAccessTime(isAccessTime);
            return this;
        }

        public boolean hasReturnToStdWhenVisit() {
            return returnToStdWhenVisit != null;
        }

        public Boolean getReturnToStdWhenVisit() {
            return returnToStdWhenVisit;
        }

        public void setReturnToStdWhenVisit(Boolean returnToStdWhenVisit) {
            this.returnToStdWhenVisit = returnToStdWhenVisit;
        }

        public NoncurrentVersionStorageTransition withReturnToStdWhenVisit(Boolean returnToStdWhenVisit) {
            setReturnToStdWhenVisit(returnToStdWhenVisit);
            return this;
        }

        public boolean hasAllowSmallFile() {
            return allowSmallFile != null;
        }

        public Boolean getAllowSmallFile() {
            return allowSmallFile;
        }

        public void setAllowSmallFile(Boolean allowSmallFile) {
            this.allowSmallFile = allowSmallFile;
        }
    }

    public static class NoncurrentVersionExpiration {
        private Integer noncurrentDays;

        public NoncurrentVersionExpiration() {
        }

        public NoncurrentVersionExpiration(Integer noncurrentDays) {
            this.noncurrentDays = noncurrentDays;
        }

        public Integer getNoncurrentDays() {
            return noncurrentDays;
        }

        public void setNoncurrentDays(Integer noncurrentDays) {
            this.noncurrentDays = noncurrentDays;
        }

        public NoncurrentVersionExpiration withNoncurrentDays(Integer noncurrentDays) {
            setNoncurrentDays(noncurrentDays);
            return this;
        }

        public boolean hasNoncurrentDays() {
            return this.noncurrentDays != null;
        }
    }

    private String id;
    private String prefix;
    private RuleStatus status;
    private int expirationDays;
    private Date expirationTime;
    private Date createdBeforeDate;
    private Boolean expiredDeleteMarker;
    private LifecycleFilter filter;

    /***
     * access time return
     */
    private String aTimeBase;

    private AbortMultipartUpload abortMultipartUpload;
    private List<StorageTransition> storageTransitions = new ArrayList<StorageTransition>();
    private Map<String, String> tags = new HashMap<String, String>();
    private NoncurrentVersionExpiration noncurrentVersionExpiration;
    private List<NoncurrentVersionStorageTransition> noncurrentVersionStorageTransitions =
            new ArrayList<NoncurrentVersionStorageTransition>();


    public LifecycleRule() {
        status = RuleStatus.Unknown;
    }

    public LifecycleRule(String id, String prefix, RuleStatus status) {
        this(id, prefix, status, null, null, null);
    }

    public LifecycleRule(String id, String prefix, RuleStatus status, int expirationDays) {
        this(id, prefix, status, expirationDays, null, null);
    }

    public LifecycleRule(String id, String prefix, RuleStatus status, Date expirationTime) {
        this(id, prefix, status, expirationTime, null, null);
    }

    public LifecycleRule(String id, String prefix, RuleStatus status, int expirationDays,
            AbortMultipartUpload abortMultipartUpload) {
        this.id = id;
        this.prefix = prefix;
        this.status = status;
        this.expirationDays = expirationDays;
        this.abortMultipartUpload = abortMultipartUpload;
    }

    public LifecycleRule(String id, String prefix, RuleStatus status, Date expirationTime,
            AbortMultipartUpload abortMultipartUpload) {
        this.id = id;
        this.prefix = prefix;
        this.status = status;
        this.expirationTime = expirationTime;
        this.abortMultipartUpload = abortMultipartUpload;
    }

    public LifecycleRule(String id, String prefix, RuleStatus status, int expirationDays,
            List<StorageTransition> storageTransitions) {
        this.id = id;
        this.prefix = prefix;
        this.status = status;
        this.expirationDays = expirationDays;
        if (storageTransitions != null && !storageTransitions.isEmpty()) {
            this.storageTransitions.addAll(storageTransitions);
        }
    }

    public LifecycleRule(String id, String prefix, RuleStatus status, Date expirationTime,
            List<StorageTransition> storageTransitions) {
        this.id = id;
        this.prefix = prefix;
        this.status = status;
        this.expirationTime = expirationTime;
        if (storageTransitions != null && !storageTransitions.isEmpty()) {
            this.storageTransitions.addAll(storageTransitions);
        }
    }

    public LifecycleRule(String id, String prefix, RuleStatus status, int expirationDays,
            AbortMultipartUpload abortMultipartUpload, List<StorageTransition> storageTransitions) {
        this.id = id;
        this.prefix = prefix;
        this.status = status;
        this.expirationDays = expirationDays;
        this.abortMultipartUpload = abortMultipartUpload;
        if (storageTransitions != null && !storageTransitions.isEmpty()) {
            this.storageTransitions.addAll(storageTransitions);
        }
    }

    public LifecycleRule(String id, String prefix, RuleStatus status, Date expirationTime,
            AbortMultipartUpload abortMultipartUpload, List<StorageTransition> storageTransitions) {
        this.id = id;
        this.prefix = prefix;
        this.status = status;
        this.expirationTime = expirationTime;
        this.abortMultipartUpload = abortMultipartUpload;
        if (storageTransitions != null && !storageTransitions.isEmpty()) {
            this.storageTransitions.addAll(storageTransitions);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public RuleStatus getStatus() {
        return status;
    }

    public void setStatus(RuleStatus status) {
        this.status = status;
    }

    @Deprecated
    public int getExpriationDays() {
        return expirationDays;
    }

    @Deprecated
    public void setExpriationDays(int expriationDays) {
        this.expirationDays = expriationDays;
    }

    public int getExpirationDays() {
        return expirationDays;
    }

    public void setExpirationDays(int expirationDays) {
        this.expirationDays = expirationDays;
    }

    public boolean hasExpirationDays() {
        return this.expirationDays != 0;
    }

    public Date getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(Date expirationTime) {
        this.expirationTime = expirationTime;
    }

    public boolean hasExpirationTime() {
        return this.expirationTime != null;
    }

    public Date getCreatedBeforeDate() {
        return createdBeforeDate;
    }

    public void setCreatedBeforeDate(Date date) {
        this.createdBeforeDate = date;
    }

    public boolean hasCreatedBeforeDate() {
        return this.createdBeforeDate != null;
    }

    public Boolean getExpiredDeleteMarker() {
        return expiredDeleteMarker;
    }

    public void setExpiredDeleteMarker(Boolean expiredDeleteMarker) {
        this.expiredDeleteMarker = expiredDeleteMarker;
    }

    public boolean hasExpiredDeleteMarker() {
        return expiredDeleteMarker != null;
    }

    public AbortMultipartUpload getAbortMultipartUpload() {
        return abortMultipartUpload;
    }

    public void setAbortMultipartUpload(AbortMultipartUpload abortMultipartUpload) {
        this.abortMultipartUpload = abortMultipartUpload;
    }

    public boolean hasAbortMultipartUpload() {
        return this.abortMultipartUpload != null;
    }

    public List<StorageTransition> getStorageTransition() {
        return this.storageTransitions;
    }

    public void setStorageTransition(List<StorageTransition> storageTransitions) {
        this.storageTransitions = storageTransitions;
    }

    public boolean hasStorageTransition() {
        return this.storageTransitions != null && !storageTransitions.isEmpty();
    }
    
    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }
    
    public void addTag(String key, String value) {
        this.tags.put(key, value);
    }

    public boolean hasTags() {
        return this.tags != null && !tags.isEmpty();
    }

    public NoncurrentVersionExpiration getNoncurrentVersionExpiration() {
        return noncurrentVersionExpiration;
    }

    public void setNoncurrentVersionExpiration(NoncurrentVersionExpiration noncurrentVersionExpiration) {
        this.noncurrentVersionExpiration = noncurrentVersionExpiration;
    }

    public boolean hasNoncurrentVersionExpiration() {
        return noncurrentVersionExpiration != null;
    }

    public List<NoncurrentVersionStorageTransition> getNoncurrentVersionStorageTransitions() {
        return noncurrentVersionStorageTransitions;
    }

    public boolean hasNoncurrentVersionStorageTransitions() {
        return noncurrentVersionStorageTransitions != null && !noncurrentVersionStorageTransitions.isEmpty();
    }

    public void setNoncurrentVersionStorageTransitions(List<NoncurrentVersionStorageTransition>
                                                               noncurrentVersionStorageTransitions) {
        this.noncurrentVersionStorageTransitions = noncurrentVersionStorageTransitions;
    }


    public String getaTimeBase() {
        return aTimeBase;
    }

    public void setaTimeBase(String aTimeBase) {
        this.aTimeBase = aTimeBase;
    }

    public LifecycleFilter getFilter() {
        return filter;
    }

    public void setFilter(LifecycleFilter filter) {
        this.filter = filter;
    }
}
