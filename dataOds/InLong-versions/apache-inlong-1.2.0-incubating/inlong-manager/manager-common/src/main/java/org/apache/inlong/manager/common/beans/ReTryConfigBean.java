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

package org.apache.inlong.manager.common.beans;

import lombok.Data;
import org.springframework.stereotype.Component;

/**
 * Re try config bean, including max attempts, delay, max delay, multiplier.
 */
@Data
@Component
public class ReTryConfigBean {

    /**
     * Maximum number of attempts
     */
    private Integer maxAttempts = 3;
    /**
     * The first delay time, in milliseconds
     */
    private Long delay = 30000L;
    /**
     * The max delay time, in milliseconds
     */
    private Long maxDelay = 300000L;
    /**
     * Delay time increase factor
     */
    private Integer multiplier = 2;

}
