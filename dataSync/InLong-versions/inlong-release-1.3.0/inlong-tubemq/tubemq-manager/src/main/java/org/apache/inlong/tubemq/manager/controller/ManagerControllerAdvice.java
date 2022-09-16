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

package org.apache.inlong.tubemq.manager.controller;

import org.apache.commons.lang3.ClassUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Controller advice for handling exceptions
 */
@RestControllerAdvice
public class ManagerControllerAdvice {

    /**
     * handling exception, and return json format string.
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(Exception.class)
    public TubeMQResult handlingParameterException(Exception ex) {
        TubeMQResult result = new TubeMQResult();
        result.setErrMsg(ClassUtils.getName(ex) + " " + ex.getMessage());
        result.setErrCode(-1);
        return result;
    }
}
