/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

 /**
 *
 */
package org.apache.ranger.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

public class ErrorMessageUtil extends PropertyPlaceholderConfigurer {
    private static Map<String, String> messageMap;

    private ErrorMessageUtil() {

    }

    @Override
    protected void processProperties(
	    ConfigurableListableBeanFactory beanFactory, Properties props)
	    throws BeansException {
	super.processProperties(beanFactory, props);

	messageMap = new HashMap<String, String>();
	Set<Object> keySet = props.keySet();

	for (Object key : keySet) {
	    String keyStr = key.toString();
	    messageMap.put(keyStr, props.getProperty(keyStr));
	}
    }


    public static String getMessage(String key) {
	return messageMap.get(key);
    }

}