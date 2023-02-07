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

package org.apache.inlong.agent.constant;

/**
 * Collection type of file data
 */
public class FileTriggerType {

    /**
     * Increment only collect newly created files content.
     *
     * <p>Here is an example. Collect task submit at '2022-01-01 23:00:00' with pattern '/bin/*.sh'.
     * <blockquote><pre>
     * .
     * └── [2022-01-01 20:49:42]  bin
     *     ├── [2022-01-01 20:10:00]  managerctl
     *     ├── [2022-01-01 21:10:00]  restart.sh
     *     ├── [2022-01-01 22:10:00]  shutdown.sh
     *     └── [2022-01-01 23:49:00]  startup.sh
     * </pre></blockquote>
     *
     * <p>It Finally collect file is:
     * <blockquote><pre>
     * ./bin/startup.sh
     * </pre></blockquote>
     */
    public static final String INCREMENT = "INCREMENT";

    /**
     * FULL collect existing files, as well as newly created files.
     *
     * <p>Here is an example. Collect task submit at '2022-01-01 23:00:00' with pattern '/bin/*.sh'.
     * <blockquote><pre>
     * .
     * └── [2022-01-01 20:49:42]  bin
     *     ├── [2022-01-01 20:10:00]  managerctl
     *     ├── [2022-01-01 21:10:00]  restart.sh
     *     ├── [2022-01-01 22:10:00]  shutdown.sh
     *     └── [2022-01-01 23:49:00]  startup.sh
     * </pre></blockquote>
     *
     * <p>It Finally collect file is:
     * <blockquote><pre>
     * ./bin/startup.sh
     * ./bin/shutdown.sh
     * ./bin/startup.sh
     * </pre></blockquote>
     */
    public static final String FULL = "FULL";
}
