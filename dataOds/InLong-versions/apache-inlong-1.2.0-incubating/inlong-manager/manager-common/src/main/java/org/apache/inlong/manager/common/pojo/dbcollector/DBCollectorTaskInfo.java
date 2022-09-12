/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.common.pojo.dbcollector;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

/**
 * db collector operation
 */
@ApiModel("db collector task info")
@Data
@Builder
public class DBCollectorTaskInfo {

    @ApiModelProperty(value = "version")
    private String version;

    @ApiModelProperty(value = "return code")
    private int returnCode;

    @ApiModelProperty(value = "md5")
    private String md5;

    @ApiModelProperty(value = "task info detail")
    private TaskInfo data;

    public static class TaskInfo {

        @ApiModelProperty(value = "task id")
        private int id;

        @ApiModelProperty(value = "task type")
        private int type;

        @ApiModelProperty(value = "db type")
        private int dbType;

        @ApiModelProperty(value = "ip")
        private String ip;

        @ApiModelProperty(value = "db port")
        private int dbPort;

        @ApiModelProperty(value = "db name")
        private String dbName;

        @ApiModelProperty(value = "user")
        private String user;

        @ApiModelProperty(value = "password")
        private String password;

        @ApiModelProperty(value = "sql")
        private String sqlStatement;

        @ApiModelProperty(value = "total limit of data lines")
        private int totalLimit;

        @ApiModelProperty(value = "the limit of one batch")
        private int onceLimit;

        @ApiModelProperty(value = "task will be forced out after time limit")
        private int timeLimit;

        @ApiModelProperty(value = "retry times if task failes")
        private int retryTimes;

        @ApiModelProperty(value = "inlong groupid")
        private String inlongGroupId;

        @ApiModelProperty(value = "inlong streamId")
        private String inlongStreamId;

        public int getId() {
            return id;
        }

        public int getType() {
            return type;
        }

        public int getDBType() {
            return dbType;
        }

        public String getIp() {
            return ip;
        }

        public int getDBPort() {
            return dbPort;
        }

        public String getDBName() {
            return dbName;
        }

        public String getUser() {
            return user;
        }

        public String getPassword() {
            return password;
        }

        public String getSqlStatement() {
            return sqlStatement;
        }

        public int getTotalLimit() {
            return totalLimit;
        }

        public int getOnceLimit() {
            return onceLimit;
        }

        public int getTimeLimit() {
            return timeLimit;
        }

        public int getRetryTimes() {
            return retryTimes;
        }

        public String getInlongGroupId() {
            return inlongGroupId;
        }

        public String getInlongStreamId() {
            return inlongStreamId;
        }

        public void setId(int id) {
            this.id = id;
        }

        public void setType(int type) {
            this.type = type;
        }

        public void setDBType(int dbType) {
            this.dbType = dbType;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public void setDBPort(int dbPort) {
            this.dbPort = dbPort;
        }

        public void setDBName(String dbName) {
            this.dbName = dbName;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public void setSqlStatement(String sql) {
            this.sqlStatement = sql;
        }

        public void setTotalLimit(int totalLimit) {
            this.totalLimit = totalLimit;
        }

        public void setOnceLimit(int onceLimit) {
            this.onceLimit = onceLimit;
        }

        public void setTimeLimit(int timeLimit) {
            this.timeLimit = timeLimit;
        }

        public void setRetryTimes(int retryTimes) {
            this.retryTimes = retryTimes;
        }

        public void setInlongGroupId(String inlongGroupId) {
            this.inlongGroupId = inlongGroupId;
        }

        public void setInlongStreamId(String inlongStreamId) {
            this.inlongStreamId = inlongStreamId;
        }
    }
}
