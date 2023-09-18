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
package io.datavines.core.aop;

import io.datavines.core.entity.ResultMap;
import io.datavines.core.utils.TokenManager;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect
@Slf4j
@Component
@Order(Integer.MAX_VALUE)
public class RefreshTokenAop {

    @Resource
    private TokenManager tokenManager;

    @Pointcut("@within(RefreshToken)")
    public void pointCut() {

    }

    @Around(value = "pointCut()")
    public Object doAroundReturningAdvice(ProceedingJoinPoint point) throws Throwable{
        Object[] args = point.getArgs();

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

        Object result = point.proceed(args);

        return ResponseEntity.ok(new ResultMap(tokenManager).successAndRefreshToken(request).payload(result));
    }
}
