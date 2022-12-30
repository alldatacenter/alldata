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

package org.apache.inlong.manager.service.operationlog;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.enums.OperationType;
import org.apache.inlong.manager.common.util.NetworkUtils;
import org.apache.inlong.manager.dao.entity.OperationLogEntity;
import org.apache.inlong.manager.pojo.user.UserInfo;
import org.apache.inlong.manager.service.user.LoginUserUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Optional;

/**
 * Operation of log aspect
 */
@Slf4j
public class OperationLogRecorder {

    private static final String ANONYMOUS_USER = "AnonymousUser";
    private static final Gson GSON = new GsonBuilder().create(); // thread safe

    /**
     * Save operation logs of all Controller
     */
    public static Object doAround(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        UserInfo userInfo = Optional.ofNullable(LoginUserUtils.getLoginUser()).orElseGet(UserInfo::new);
        String operator = userInfo.getName();
        operator = StringUtils.isBlank(operator) ? ANONYMOUS_USER : operator;

        String requestUrl = request.getRequestURI();
        String httpMethod = request.getMethod();
        String remoteAddress = NetworkUtils.getClientIpAddress(request);
        String param = GSON.toJson(request.getParameterMap());
        String body = GSON.toJson(joinPoint.getArgs());

        long start = System.currentTimeMillis();
        boolean success = true;
        String errMsg = "";
        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            success = false;
            errMsg = throwable.getMessage();
            throw throwable;
        } finally {
            long costTime = System.currentTimeMillis() - start;
            OperationType operationType = operationLog.operation();
            OperationLogEntity operationLogEntity = new OperationLogEntity();
            operationLogEntity.setOperationType(operationType.name());
            operationLogEntity.setHttpMethod(httpMethod);
            operationLogEntity.setOperator(operator);
            operationLogEntity.setRequestUrl(requestUrl);
            operationLogEntity.setRemoteAddress(remoteAddress);
            operationLogEntity.setCostTime(costTime);
            operationLogEntity.setBody(body);
            operationLogEntity.setParam(param);
            operationLogEntity.setStatus(success);
            operationLogEntity.setRequestTime(new Date());
            operationLogEntity.setErrMsg(errMsg);

            if (operationLog.db()) {
                OperationLogPool.publish(operationLogEntity);
            } else if (success) {
                log.info("operation log: {}", GSON.toJson(operationLogEntity));
            } else {
                log.error("request handle failed : {}", GSON.toJson(operationLogEntity));
            }
        }
    }
}
