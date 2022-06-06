/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.notification.spool.models;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.atlas.type.AtlasType;
import org.apache.commons.lang.StringUtils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class IndexRecord implements Serializable {
    public static final int    RECORD_SIZE              = 500;
    public static final String STATUS_PENDING           = "PENDING";
    public static final String STATUS_WRITE_IN_PROGRESS = "WRITE_IN_PROGRESS";
    public static final String STATUS_READ_IN_PROGRESS  = "READ_IN_PROGRESS";
    public static final String STATUS_DONE              = "DONE";

    private String  id;
    private String  path;
    private int     line;
    private long    created;
    private long    writeCompleted;
    private long    doneCompleted;
    private long    lastSuccess;
    private long    lastFailed;
    private boolean lastAttempt;
    private int     failedAttempt;
    private String  status;

    public IndexRecord() {
        this.status      = STATUS_WRITE_IN_PROGRESS;
        this.lastAttempt = false;
    }

    public IndexRecord(String path) {
        this.id            = UUID.randomUUID().toString();
        this.path          = path;
        this.failedAttempt = 0;
        this.status        = STATUS_WRITE_IN_PROGRESS;
        this.created       = System.currentTimeMillis();

        setLastAttempt(false);
    }

    @Override
    public String toString() {
        return "IndexRecord [id=" + id + ", filePath=" + path
                + ", linePosition=" + line + ", status=" + status
                + ", fileCreateTime=" + created
                + ", writeCompleteTime=" + writeCompleted
                + ", doneCompleteTime=" + doneCompleted
                + ", lastSuccessTime=" + lastSuccess
                + ", lastFailedTime=" + lastFailed
                + ", failedAttemptCount=" + failedAttempt
                + ", lastAttempt=" + lastAttempt + "]";
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return this.path;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public int getLine() {
        return line;
    }

    public void setCreated(long fileCreateTime) {
        this.created = fileCreateTime;
    }

    public long getCreated() {
        return this.created;
    }

    public void setWriteCompleted(long writeCompleted) {
        this.writeCompleted = writeCompleted;
    }

    public long getWriteCompleted() {
        return this.writeCompleted;
    }

    public void setDoneCompleted(long doneCompleted) {
        this.doneCompleted = doneCompleted;
    }

    public long getDoneCompleted() {
        return doneCompleted;
    }

    public void setLastSuccess(long lastSuccess) {
        this.lastSuccess = lastSuccess;
    }

    public long getLastSuccess() {
        return lastSuccess;
    }

    public void setLastFailed(long lastFailed) {
        this.lastFailed = lastFailed;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return this.status;
    }

    public void setLastAttempt(boolean lastAttempt) {
        this.lastAttempt = lastAttempt;
    }

    public void setFailedAttempt(int failedAttempt) {
        this.failedAttempt = failedAttempt;
    }

    public int getFailedAttempt() {
        return failedAttempt;
    }

    @JsonIgnore
    public void setDone() {
        setStatus(IndexRecord.STATUS_DONE);
        setDoneCompleted(System.currentTimeMillis());
        setLastAttempt(true);
    }

    @JsonIgnore
    public void setStatusPending() {
        setStatus(IndexRecord.STATUS_PENDING);
        setWriteCompleted(System.currentTimeMillis());
        setLastAttempt(true);
    }

    @JsonIgnore
    public void updateFailedAttempt() {
        setLastFailed(System.currentTimeMillis());
        incrementFailedAttemptCount();
        setLastAttempt(false);
    }

    @JsonIgnore
    public boolean equals(IndexRecord record) {
        return this.id.equals(record.getId());
    }

    @JsonIgnore
    public void setCurrentLine(int line) {
        setLine(line);
        setStatus(STATUS_READ_IN_PROGRESS);
        setLastSuccess(System.currentTimeMillis());
        setLastAttempt(true);
    }

    @JsonIgnore
    public boolean isStatusDone() {
        return this.status.equals(STATUS_DONE);
    }

    @JsonIgnore
    public boolean isStatusWriteInProgress() {
        return this.status.equals(STATUS_WRITE_IN_PROGRESS);
    }

    @JsonIgnore
    public boolean isStatusReadInProgress() {
        return status.equals(IndexRecord.STATUS_READ_IN_PROGRESS);
    }

    @JsonIgnore
    public void incrementFailedAttemptCount() {
        this.failedAttempt++;
    }
}
