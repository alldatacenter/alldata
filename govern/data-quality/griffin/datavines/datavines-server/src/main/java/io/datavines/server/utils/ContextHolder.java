/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.server.utils;

import io.datavines.core.constant.DataVinesConstants;
import io.datavines.server.repository.entity.User;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ContextHolder {

    private static final ThreadLocal<Map<String, Object>> THREAD_LOCAL = new ThreadLocal<>();

    public static Long getUserId(){
        Object obj = getContext().get(DataVinesConstants.LOGIN_USER);
        User user = null == obj ? null : (User)obj;
        return null == user ? null : user.getId();
    }

    /**
     * 移除上下文信息
     */
    public static void removeAll(){
        Map<String, Object> context = getContext();
        if(null != context){
            context.clear();
        }
    }

    /**
     * 移除上下文信息
     */
    public static void removeByKey(String key){
        Map<String, Object> context = getContext();
        if(null != context){
            context.remove(key);
        }
    }

    /**
     * 获取当前线程上线文
     * @return
     */
    public static Map<String, Object> getContext(){
        Map<String, Object> currentContext = THREAD_LOCAL.get();
        if(null == currentContext){
            Map<String, Object> concurrentHashMap = new ConcurrentHashMap<>();
            THREAD_LOCAL.set(concurrentHashMap);
            return concurrentHashMap;
        }
        return currentContext;
    }

    /**
     * 设置参数
     * @param key
     * @param value
     */
    public static void setParam(String key, Object value){
        Map<String, Object> context = getContext();
        context.put(key, value);
    }

    /**
     * 获取参数
     * @param key
     * @return
     */
    public static Object getParam(String key){
        Map<String, Object> context = getContext();
        return context.get(key);
    }

    /**
     * 获取用户信息
     * @return
     */
    public static User getUser(){
        Object obj = getContext().get(DataVinesConstants.LOGIN_USER);
        return null == obj ? null : (User)obj;
    }
}
