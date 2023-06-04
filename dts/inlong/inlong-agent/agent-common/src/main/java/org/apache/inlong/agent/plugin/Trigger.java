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

package org.apache.inlong.agent.plugin;

import org.apache.inlong.agent.conf.TriggerProfile;

import java.io.IOException;
import java.util.Map;

/**
 * Trigger interface, which generates job in condition.
 */
public interface Trigger {

    /**
     * init trigger by trigger profile
     *
     * @throws IOException
     */
    void init(TriggerProfile profile) throws IOException;

    /**
     * run trigger.
     */
    void run();

    /**
     * destroy trigger.
     */
    void destroy();

    // todo:fetch TaskProfile
    /**
     * fetch task profile from trigger
     *
     * @return task profile
     */
    Map<String, String> fetchJobProfile();

    /**
     * get trigger profile
     */
    TriggerProfile getTriggerProfile();
}
