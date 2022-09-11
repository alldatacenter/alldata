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

package org.apache.inlong.agent.pojo;

import lombok.Data;

@Data
public class FileJob  {

    private String trigger;

    private Dir dir;
    private Thread thread;
    private int id;
    private String timeOffset;
    private String addictiveString;
    private String collectType;

    @Data
    public static class Dir {

        private String pattern;
    }

    @Data
    public static class Running {

        private String core;
    }

    @Data
    public static class Thread {

        private Running running;
    }

    @Data
    public static class FileJobTaskConfig {

        private String pattern;
        // '1m' means one minute after, '-1m' means one minute before
        // '1h' means one hour after, '-1h' means one hour before
        // '1d' means one day after, '-1d' means one day before
        // Null means from current timestamp
        private String timeOffset;
        //For example: a=b&c=b&e=f
        private String additionalAttr;

        private String collectType;
    }

}
