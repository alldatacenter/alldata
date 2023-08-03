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

package org.apache.inlong.manager.dao.interceptor;

import org.apache.inlong.manager.common.tenant.MultiTenantQuery;

import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Filter to check if SQLs from some method should add tenant condition or not.
 */
@Slf4j
@Component
public class MultiTenantQueryFilter {

    private static final String METHOD_FILTER_PATH = "org.apache.inlong.manager.dao.mapper";

    private static final Set<String> METHOD_SET = new HashSet<>();

    /**
     * Check whether the specified method supports multi-tenant queries.
     *
     * @param methodName method name
     * @return true if supports multi-tenant query, false if not
     */
    public static boolean isMultiTenantQuery(String methodName) {
        return METHOD_SET.contains(methodName);
    }

    /**
     * Find all methods that support multi-tenant queries - used MultiTenantQuery annotation.
     */
    @PostConstruct
    private void init() {
        Reflections methodReflections = new Reflections(METHOD_FILTER_PATH, Scanners.MethodsAnnotated);
        // process methods
        Set<Method> methodSet = methodReflections.getMethodsAnnotatedWith(MultiTenantQuery.class);
        markMethods(methodSet);

        // process classes
        Reflections reflections = new Reflections(METHOD_FILTER_PATH, Scanners.TypesAnnotated);
        Set<Class<?>> clazzSet = reflections.getTypesAnnotatedWith(MultiTenantQuery.class);
        clazzSet.stream()
                .filter(Class::isInterface)
                .forEach(clazz -> {
                    // Get the JsonTypeDefine annotation
                    MultiTenantQuery annotation = clazz.getAnnotation(MultiTenantQuery.class);
                    if (annotation == null || !annotation.with()) {
                        return;
                    }
                    List<Method> methods = Arrays.asList(clazz.getMethods());
                    markMethods(methods);
                });

        log.debug("success to find all methods that support multi-tenant queries, methods={}", METHOD_SET);
    }

    private static void markMethods(Collection<Method> methods) {
        methods.forEach(method -> {
            MultiTenantQuery annotation = method.getAnnotation(MultiTenantQuery.class);
            if (annotation != null && !annotation.with()) {
                METHOD_SET.remove(getMethodFullName(method));
            } else {
                METHOD_SET.add(getMethodFullName(method));
            }
        });
    }

    private static String getMethodFullName(Method method) {
        return method.getDeclaringClass().getName() + "." + method.getName();
    }

}
