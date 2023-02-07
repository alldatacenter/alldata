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

import org.apache.commons.lang3.ClassUtils;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.source.http.HTTPBadRequestException;
import org.apache.flume.source.http.JSONHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 
 * AdminJsonHandler
 */
public class AdminJsonHandler implements AdminHttpSourceHandler {

    public static final Logger LOG = LoggerFactory.getLogger(AdminJsonHandler.class);
    public static final String KEY_CMD = "cmd";
    private Context context;
    private JSONHandler requestHandler;
    private Map<String, AdminEventHandler> handlerMap = new HashMap<>();

    /**
     * configure
     * 
     * @param context
     */
    @Override
    public void configure(Context context) {
        this.context = context;
        this.requestHandler = new JSONHandler();
        this.requestHandler.configure(context);
    }

    @Override
    public List<Event> getEvents(HttpServletRequest request, HttpServletResponse response)
            throws HTTPBadRequestException, Exception {
        List<Event> events = this.requestHandler.getEvents(request);
        for (Event event : events) {
            String cmd = event.getHeaders().get(KEY_CMD);
            if (cmd == null) {
                LOG.error("Invalid admin event,{} is null", KEY_CMD);
                continue;
            }
            AdminEventHandler handler = this.handlerMap.get(cmd);
            if (handler == null) {
                String handlerType = context.getString(cmd + ".type");
                if (handlerType == null) {
                    LOG.error("Invalid admin event,{}:{},type is null", KEY_CMD, cmd);
                    continue;
                }
                try {
                    Class<?> handlerClass = ClassUtils.getClass(handlerType);
                    Object handlerObject = handlerClass.getDeclaredConstructor().newInstance();
                    if (handlerObject instanceof AdminEventHandler) {
                        handler = (AdminEventHandler) handlerObject;
                        Context subContext = new Context(context.getSubProperties(cmd + "."));
                        handler.configure(subContext);
                        this.handlerMap.put(cmd, handler);
                    } else {
                        LOG.error("Invalid admin event,{}:{},type:{} is not AdminEventHandler", KEY_CMD, cmd,
                                handlerType);
                        continue;
                    }
                } catch (Exception e) {
                    LOG.error("Invalid admin event,{}:{},type:{} can not be created,error:{}", KEY_CMD, cmd,
                            handlerType, e.getMessage(), e);
                    continue;
                }
            }
            handler.process(cmd, event, response);
        }
        return null;
    }
}
