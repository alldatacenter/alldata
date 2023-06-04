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

package org.apache.inlong.sdk.commons.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import static org.apache.inlong.sdk.commons.admin.AdminEventHandler.DOMAIN_SEPARATOR;
import static org.apache.inlong.sdk.commons.admin.AdminEventHandler.JMX_DOMAIN;
import static org.apache.inlong.sdk.commons.admin.AdminEventHandler.JMX_NAME;
import static org.apache.inlong.sdk.commons.admin.AdminEventHandler.JMX_TYPE;
import static org.apache.inlong.sdk.commons.admin.AdminEventHandler.PROPERTY_EQUAL;
import static org.apache.inlong.sdk.commons.admin.AdminEventHandler.PROPERTY_SEPARATOR;

/**
 * AdminServiceRegister
 */
public class AdminServiceRegister {

    public static final Logger LOG = LoggerFactory.getLogger(AdminServiceRegister.class);

    /**
     * register AdminService
     */
    public static void register(String type, String name, Object mbean) {
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        String beanName = JMX_DOMAIN + DOMAIN_SEPARATOR
                + JMX_TYPE + PROPERTY_EQUAL + type + PROPERTY_SEPARATOR
                + JMX_NAME + PROPERTY_EQUAL + name;
        LOG.info("start to register mbean:{}", beanName);
        try {
            ObjectName objName = new ObjectName(beanName);
            mbs.registerMBean(mbean, objName);
            LOG.info("end to register mbean:{}", beanName);
        } catch (Exception ex) {
            LOG.error("exception while register mbean:{},error:{}", beanName, ex.getMessage(), ex);
        }
    }

    /**
     * main
     * 
     * @param args
     */
    public static void main(String[] args) {
        String type = "type1";
        String name = "name1";
        String beanName = JMX_DOMAIN + DOMAIN_SEPARATOR
                + JMX_TYPE + PROPERTY_EQUAL + type + PROPERTY_SEPARATOR
                + JMX_NAME + PROPERTY_EQUAL + name;
        try {
            ObjectName objName = new ObjectName(beanName);
            System.out.println(objName.toString());
            System.out.println(objName.getCanonicalKeyPropertyListString());
            System.out.println(objName.getCanonicalName());
            System.out.println(objName.getKeyProperty(JMX_NAME));
        } catch (Exception ex) {
            LOG.error("exception while register mbean:{},error:{}", beanName, ex.getMessage(), ex);
        }

    }
}
