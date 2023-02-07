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

package org.apache.inlong.sdk.commons.admin;

import org.apache.flume.Event;
import org.apache.flume.conf.Configurable;

import javax.servlet.http.HttpServletResponse;

/**
 * IAdminEventHandler
 */
public interface AdminEventHandler extends Configurable {

    String JMX_DOMAIN = "org.apache.inlong";
    String JMX_TYPE = "type";
    String JMX_NAME = "name";
    char DOMAIN_SEPARATOR = ':';
    char PROPERTY_SEPARATOR = ',';
    char PROPERTY_EQUAL = '=';

    /**
     * process
     * 
     * @param cmd
     * @param event
     * @param response
     */
    void process(String cmd, Event event, HttpServletResponse response);
}
