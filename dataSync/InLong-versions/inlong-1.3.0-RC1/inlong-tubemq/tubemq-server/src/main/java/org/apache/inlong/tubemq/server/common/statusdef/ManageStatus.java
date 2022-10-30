/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.common.statusdef;

import org.apache.inlong.tubemq.corebase.utils.Tuple2;

/*
 * The management status enumeration class of the Broker node
 */
public enum ManageStatus {

    STATUS_MANAGE_UNDEFINED(-2, "-", false, false),
    STATUS_MANAGE_APPLY(1, "draft", false, false),
    STATUS_MANAGE_ONLINE(5, "online", true, true),
    STATUS_MANAGE_ONLINE_NOT_WRITE(6, "only-read", false, true),
    STATUS_MANAGE_ONLINE_NOT_READ(7, "only-write", true, false),
    STATUS_MANAGE_OFFLINE(9, "offline", false, false);

    private int code;
    private String description;
    private boolean isAcceptPublish;
    private boolean isAcceptSubscribe;

    ManageStatus(int code, String description,
                 boolean acceptPublish,
                 boolean acceptSubscribe) {
        this.code = code;
        this.description = description;
        this.isAcceptPublish = acceptPublish;
        this.isAcceptSubscribe = acceptSubscribe;
    }

    public boolean isOnlineStatus() {
        return (this == ManageStatus.STATUS_MANAGE_ONLINE
                || this == ManageStatus.STATUS_MANAGE_ONLINE_NOT_WRITE
                || this == ManageStatus.STATUS_MANAGE_ONLINE_NOT_READ);
    }

    public boolean isApplied() {
        return (this.code > ManageStatus.STATUS_MANAGE_APPLY.getCode());
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public Tuple2<Boolean, Boolean> getPubSubStatus() {
        return new Tuple2<>(isAcceptPublish, isAcceptSubscribe);
    }

    public boolean isAcceptSubscribe() {
        return isAcceptSubscribe;
    }

    public boolean isAcceptPublish() {
        return isAcceptPublish;
    }

    public static ManageStatus valueOf(int code) {
        for (ManageStatus status : ManageStatus.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new IllegalArgumentException(String.format(
                "unknown broker manage status code %s", code));
    }

    public static ManageStatus descOf(String description) {
        for (ManageStatus status : ManageStatus.values()) {
            if (status.getDescription().equalsIgnoreCase(description)) {
                return status;
            }
        }
        throw new IllegalArgumentException(String.format(
                "unknown broker manage status name %s", description));
    }

    /**
     * Change broker read write status
     *
     * @param oldStatus        current broker manage status
     * @param acceptPublish    current broker reported publish status
     * @param acceptSubscribe  current broker reported subscribe status
     * @return   current broker's new manage status
     */
    public static ManageStatus getNewStatus(ManageStatus oldStatus,
                                            Boolean acceptPublish,
                                            Boolean acceptSubscribe) {
        if (acceptPublish == null && acceptSubscribe == null) {
            return oldStatus;
        }
        boolean newPublish = oldStatus.isAcceptPublish;
        boolean newSubscribe = oldStatus.isAcceptSubscribe;
        if (acceptPublish != null) {
            newPublish = acceptPublish;
        }
        if (acceptSubscribe != null) {
            newSubscribe = acceptSubscribe;
        }
        if (newPublish) {
            if (newSubscribe) {
                return ManageStatus.STATUS_MANAGE_ONLINE;
            } else {
                return ManageStatus.STATUS_MANAGE_ONLINE_NOT_READ;
            }
        } else {
            if (newSubscribe) {
                return ManageStatus.STATUS_MANAGE_ONLINE_NOT_WRITE;
            } else {
                return ManageStatus.STATUS_MANAGE_OFFLINE;
            }
        }
    }

}
