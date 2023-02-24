/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.web.service;

import org.apache.atlas.AtlasConfiguration;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Aspect
@Component
public class TimedAspectInterceptor {
    private static final boolean debugMetricsEnabled = AtlasConfiguration.DEBUG_METRICS_ENABLED.getBoolean();

    private final DebugMetricsWrapper wrapper;

    @Inject
    public TimedAspectInterceptor(DebugMetricsWrapper wrapper) {
        this.wrapper = wrapper;
    }

    @Around("@annotation(org.apache.atlas.annotation.Timed) && execution(public * *(..))")
    public Object timerAdvice(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        try {
            return proceedingJoinPoint.proceed();
        } catch (Throwable throwable){
            throw throwable;
        } finally {
            if (debugMetricsEnabled) {
                reportMetrics(start, proceedingJoinPoint.getSignature());
            }
        }
    }

    private void reportMetrics(long start, Signature signature) {
        long executionTime = System.currentTimeMillis() - start;

        wrapper.update(signature, executionTime);
    }
}