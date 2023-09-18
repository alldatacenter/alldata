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
package com.koubei.web.tag.pager;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletRequest;

/**
 * 因为要做到能够在页面（jsp）和action（webwork或者Struts2）<br>
 * 之间传递分页控件对象 分页控件也和webwork 和struts的实现无关,<br>
 * 所以做这个pageDTO类
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2010-2-25
 */
public class PagerDTO {

    private final Map<String, Pager> pageControl = new HashMap<String, Pager>();

    private static final String REQUEST_KEY = PagerDTO.class.getName();

    private final ServletRequest request;

    public static PagerDTO get(ServletRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request can not be null");
        }
        PagerDTO dto = (PagerDTO) request.getAttribute(REQUEST_KEY);
        if (dto == null) {
            synchronized (PagerDTO.class) {
                if (request.getAttribute(REQUEST_KEY) == null) {
                    dto = new PagerDTO(request);
                    request.setAttribute(REQUEST_KEY, dto);
                }
            }
        }
        return dto;
    }

    protected PagerDTO(ServletRequest request) {
        this.request = request;
    }

    void add(String name, Pager action) {
        // BeanInfo beanInfo = null;
        // try {
        // beanInfo = Introspector.getBeanInfo(action.getClass(),
        // Introspector.IGNORE_ALL_BEANINFO);
        // } catch (IntrospectionException e) {
        // throw new RuntimeException(e);
        // }
        //
        // PropertyDescriptor[] pDescript = beanInfo.getPropertyDescriptors();
        // Method m = null;
        //
        // for (PropertyDescriptor d : pDescript) {
        //
        // m = d.getReadMethod();
        //
        // try {
        //
        // if (m.getReturnType() == Pager.class) {
        // pageControl
        // .put(d.getName(), (Pager) m.invoke(action, null));
        // }
        //
        // } catch (Exception e) {
        // throw new RuntimeException(e);
        // }
        // }
        pageControl.put(name, action);
    }

    /**
     * 取得分页控件
     *
     * @param name
     * @return
     */
    public Pager getByName(String name) {
        return pageControl.get(name);
    }

    // public Pager getPager() {
    // return new Pager(null);
    // }
    public static void main(String[] arg) {
        BeanInfo beanInfo = null;
        try {
            beanInfo = Introspector.getBeanInfo(User.class, Introspector.IGNORE_ALL_BEANINFO);
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
        PropertyDescriptor[] pDescript = beanInfo.getPropertyDescriptors();
        Method m = null;
        for (PropertyDescriptor d : pDescript) {
            m = d.getReadMethod();
            try {
                // if (m.getReturnType() == Pager.class) {
                System.out.println(m.invoke(new User(), null));
            // }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class User {

        public String getName() {
            return "aa";
        }
    }
}
