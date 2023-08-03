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
package io.datavines.server.dqc.executor.cache;

import lombok.Data;

import java.util.concurrent.ConcurrentHashMap;

public class JobExecutionCache {

    private final ConcurrentHashMap<Long, JobExecutionContext> cache = new ConcurrentHashMap<>();

    private JobExecutionCache(){}

    private static class Singleton{
        static JobExecutionCache instance = new JobExecutionCache();
    }

    public static JobExecutionCache getInstance(){
        return Singleton.instance;
    }

    public JobExecutionContext getById(Long taskId){
        return cache.get(taskId);
    }

    public void cache(JobExecutionContext jobExecutionContext){
        cache.put(jobExecutionContext.getJobExecutionRequest().getJobExecutionId(), jobExecutionContext);
    }

    public void remove(Long taskId){
        cache.remove(taskId);
    }
}
