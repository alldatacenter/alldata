/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.standalone.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * InlongLoggerFactory
 */
public class InlongLoggerFactory {

    /**
     * getLogger
     * 
     * @param  clazz
     * @return
     */
    public static Logger getLogger(Class<?> clazz) {
        String className = clazz.getName();
        String namePrefix = getClassNamePrefix(className, 3);
        return LoggerFactory.getLogger(namePrefix);
    }

    /**
     * getClassNamePrefix
     * 
     * @param  className
     * @param  layer
     * @return
     */
    public static String getClassNamePrefix(String className, int layer) {
        int index = 0;
        for (int i = 0; i < layer; i++) {
            int newIndex = className.indexOf('.', index + 1);
            if (newIndex <= 0) {
                break;
            }
            index = newIndex;
        }
        if (index == 0) {
            return "Inlong";
        }
        String namePrefix = className.substring(0, index);
        return namePrefix;
    }
}
