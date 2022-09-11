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

package org.apache.inlong.manager.dao.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * Agent system config info, including retry thread info, message queue info, etc.
 */
@Data
public class AgentSysConfigEntity implements Serializable {

    private static final long serialVersionUID = 1L;
    private String ip;
    private Integer maxRetryThreads;
    private Integer minRetryThreads;
    private String dbPath;
    private Integer scanIntervalSec;
    private Integer batchSize;
    private Integer msgSize;
    private Integer sendRunnableSize;
    private Integer msgQueueSize;
    private Integer maxReaderCnt;
    private Integer threadManagerSleepInterval;
    private Integer onelineSize;
    private Integer clearDayOffset;
    private Integer clearIntervalSec;
    private Integer bufferSizeInBytes;
    private Integer agentRpcReconnectTime;
    private Integer sendTimeoutMillSec;
    private Integer flushEventTimeoutMillSec;
    private Integer statIntervalSec;
    private Integer confRefreshIntervalSecs;
    private Integer flowSize;
    private Integer buffersize;
    private Byte compress;
    private Integer eventCheckInterval;
    private Byte isCalmd5;

}