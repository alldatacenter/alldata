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

package org.apache.inlong.dataproxy.admin;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.inlong.sdk.commons.admin.AbstractAdminEventHandler;

import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.servlet.http.HttpServletResponse;

import static org.apache.inlong.dataproxy.admin.ProxyServiceMBean.MBEAN_TYPE;

/**
 * StopServiceAdminEventHandler
 */
public class ProxyServiceAdminEventHandler extends AbstractAdminEventHandler {

    /**
     * configure
     *
     * @param context
     */
    @Override
    public void configure(Context context) {
    }

    /**
     * process
     *
     * @param cmd
     * @param event
     * @param response
     */
    @Override
    public void process(String cmd, Event event, HttpServletResponse response) {
        String sourceName = event.getHeaders().get(ProxyServiceMBean.KEY_SOURCENAME);
        LOG.info("start to process admin task:{},sourceName:{}", cmd, sourceName);
        switch (cmd) {
            case ProxyServiceMBean.METHOD_STOPSERVICE:
            case ProxyServiceMBean.METHOD_RECOVERSERVICE:
                if (sourceName == null) {
                    break;
                }
                if (StringUtils.equals(sourceName, ProxyServiceMBean.ALL_SOURCENAME)) {
                    this.processAll(cmd, event, response);
                } else {
                    this.processOne(cmd, sourceName, response);
                }
                break;
            default:
                break;
        }
        LOG.info("end to process admin task:{},sourceName:{}", cmd, sourceName);
    }

    /**
     * processOne
     *
     * @param cmd
     * @param sourceName
     * @param response
     */
    private void processOne(String cmd, String sourceName, HttpServletResponse response) {
        LOG.info("start to processOne admin task:{},sort task:{}", cmd, sourceName);
        StringBuilder result = new StringBuilder();
        try {
            String beanName = JMX_DOMAIN + DOMAIN_SEPARATOR
                    + JMX_TYPE + PROPERTY_EQUAL + MBEAN_TYPE + PROPERTY_SEPARATOR
                    + JMX_NAME + PROPERTY_EQUAL + sourceName;

            ObjectName objName = new ObjectName(beanName);
            final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectInstance mbean = mbs.getObjectInstance(objName);
            LOG.info("getObjectInstance for type:{},name:{},result:{}", MBEAN_TYPE, sourceName, mbean);
            String className = mbean.getClassName();
            Class<?> clazz = ClassUtils.getClass(className);
            if (ClassUtils.isAssignable(clazz, ProxyServiceMBean.class)) {
                mbs.invoke(mbean.getObjectName(), cmd, null, null);
                result.append(String.format("Execute command:%s success in bean:%s\n",
                        cmd, mbean.getObjectName().toString()));
            }
            this.outputResponse(response, result.toString());
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            this.outputResponse(response, result.toString());
        }
        LOG.info("end to processOne admin task:{},sort task:{}", cmd, sourceName);
    }

    /**
     * processAll
     *
     * @param cmd
     * @param event
     * @param response
     */
    private void processAll(String cmd, Event event, HttpServletResponse response) {
        LOG.info("start to processAll admin task:{}", cmd);
        StringBuilder result = new StringBuilder();
        try {
            String beanName = JMX_DOMAIN + DOMAIN_SEPARATOR
                    + JMX_TYPE + PROPERTY_EQUAL + MBEAN_TYPE + PROPERTY_SEPARATOR
                    + "*";
            ObjectName objName = new ObjectName(beanName.toString());
            final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            Set<ObjectInstance> mbeans = mbs.queryMBeans(objName, null);
            LOG.info("queryMBeans for type:{},result:{}", MBEAN_TYPE, mbeans);
            for (ObjectInstance mbean : mbeans) {
                ObjectName beanObjectName = mbean.getObjectName();
                String className = mbean.getClassName();
                Class<?> clazz = ClassUtils.getClass(className);
                if (ClassUtils.isAssignable(clazz, ProxyServiceMBean.class)) {
                    mbs.invoke(mbean.getObjectName(), cmd, null, null);
                    result.append(String.format("Execute command:%s success in bean:%s\n",
                            cmd, beanObjectName.toString()));
                }
            }
            this.outputResponse(response, result.toString());
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            this.outputResponse(response, result.toString());
        }
        LOG.info("end to processAll admin task:{}", cmd);
    }
}
