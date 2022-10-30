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

package org.apache.inlong.tubemq.server.master.metamanage;

public enum DataOpErrCode {
    DERR_SUCCESS(200, "Success"),
    DERR_SUCCESS_UNCHANGED(201, "Success, but unchanged"),
    DERR_NOT_EXIST(401, "Record not exist"),
    DERR_EXISTED(402, "Record has existed"),
    DERR_UNCHANGED(403, "Record not changed"),
    DERR_UNCLEANED(404, "Related configuration is not cleaned up"),
    DERR_CONDITION_LACK(405, "The preconditions are not met"),
    DERR_ILLEGAL_STATUS(406, "Illegal operate status"),
    DERR_ILLEGAL_VALUE(407, "Illegal data format or value"),
    DERR_CONFLICT_VALUE(408, "Conflicted configure value"),
    DERR_STORE_ABNORMAL(501, "Store layer throw exception"),
    DERR_UPD_NOT_EXIST(502, "Record updated but not exist"),
    DERR_STORE_STOPPED(510, "Store stopped"),
    DERR_STORE_NOT_MASTER(511, "Store not active master"),
    DERR_STORE_LOCK_FAILURE(512, "Failed to lock metadata lock"),
    DERR_MASTER_UNKNOWN(599, "Unknown error");

    private int code;
    private String description;

    DataOpErrCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static DataOpErrCode valueOf(int code) {
        for (DataOpErrCode status : DataOpErrCode.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new IllegalArgumentException(String.format("unknown data operate error code %s", code));
    }

}
