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

import java.util.regex.Pattern;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.beans.BeansException;
import com.qlangtech.tis.manage.biz.dal.dao.IDepartmentDAO;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2012-5-29
 */
public class DAOTypeAutoProxyCreator extends AbstractAutoProxyCreator {

    private static final long serialVersionUID = 1L;

    private static final Pattern methodPattern = Pattern.compile("^([^((get)(count)(select))]).*");

    public static void main(String[] arg) {
        // System.out.println(PatternMatchUtils.simpleMatch("updateUser",
        // ));
        System.out.println(methodPattern.matcher("updateUser").matches());
        System.out.println(methodPattern.matcher("getUser").matches());
        System.out.println(methodPattern.matcher("countUser").matches());
        System.out.println(methodPattern.matcher("selectUser").matches());
    }

    @Override
    protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName, TargetSource customTargetSource) throws BeansException {
        if (beanClass == null || beanClass.getPackage() == null) {
            return DO_NOT_PROXY;
        }
        try {
            if (beanClass.equals(com.qlangtech.tis.manage.biz.dal.dao.impl.OperationLogDAOImpl.class) || IDepartmentDAO.class.isAssignableFrom(beanClass)) {
                return DO_NOT_PROXY;
            }
        } catch (Exception e) {
            throw new BeansException(e.getMessage(), e) {

                private static final long serialVersionUID = 1L;
            };
        }
        return ((com.qlangtech.tis.manage.common.OperationLogger.class.isAssignableFrom(beanClass))) ? PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS : DO_NOT_PROXY;
    }
}
