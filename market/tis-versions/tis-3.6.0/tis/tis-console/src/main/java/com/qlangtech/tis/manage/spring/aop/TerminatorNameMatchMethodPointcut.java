/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.manage.spring.aop;

import java.lang.reflect.Method;
import org.apache.commons.lang.StringUtils;
import org.springframework.aop.support.StaticMethodMatcherPointcut;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2012-9-11
 */
public class TerminatorNameMatchMethodPointcut extends StaticMethodMatcherPointcut {

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        if (StringUtils.startsWith(method.getName(), "count")) {
            return false;
        }
        if (StringUtils.startsWith(method.getName(), "select")) {
            return false;
        }
        if (StringUtils.startsWith(method.getName(), "load")) {
            return false;
        }
        if (StringUtils.startsWith(method.getName(), "get")) {
            return false;
        }
        if (StringUtils.startsWith(method.getName(), "is")) {
            return false;
        }
        return true;
    }
}
