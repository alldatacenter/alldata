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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.beans.BeansException;
import com.qlangtech.tis.runtime.module.action.BasicModule;

/**
 * 权限控制拦截器
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2012-3-15
 */
public class BeanTypeAutoProxyCreator extends AbstractAutoProxyCreator {

    private static final long serialVersionUID = 1L;

    private static final Pattern pkg_pattern = Pattern.compile("^com\\.qlangtech\\.tis\\.(runtime|trigger|coredefine)\\..*");

    @Override
    protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName, TargetSource customTargetSource) throws BeansException {
        if (beanClass == null || beanClass.getPackage() == null) {
            return DO_NOT_PROXY;
        }
        Matcher m = pkg_pattern.matcher(beanClass.getPackage().getName());
        return ((m.matches()) && BasicModule.class.isAssignableFrom(beanClass)) ? PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS : DO_NOT_PROXY;
    }

    public static void main(String[] arg) {
        Matcher m = pkg_pattern.matcher("com.taobao.terminator.runtimee.spring.aop");
        if (m.matches()) {
            System.out.println("match");
        }
    }
}
