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

package org.apache.inlong.sort.standalone.admin;

import javax.management.MXBean;

/**
 * ConsumerServiceMBean<br>
 * Stop cache consumer before stopping Sort-Standalone process.<br>
 * Avoid to miss the Event data in the channel when stopping Sort-Standalone process immediately.<br>
 * After Sort-Standalone send all channel data to sink target and acknowledge all offset,<br>
 * Sort-Standalone can be stopped.<br>
 */
@MXBean
public interface ConsumerServiceMBean {

    String MBEAN_TYPE = "ConsumerService";
    String METHOD_STOPCONSUMER = "stopConsumer";
    String KEY_TASKNAME = "sortTaskId";
    String ALL_TASKNAME = "*";
    String METHOD_RECOVERCONSUMER = "recoverConsumer";

    /**
     * stopConsumer
     */
    void stopConsumer();

    /**
     * recoverConsumer
     */
    void recoverConsumer();
}
