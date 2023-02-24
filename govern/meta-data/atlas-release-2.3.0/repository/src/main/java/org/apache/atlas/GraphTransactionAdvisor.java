/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas;

import org.aopalliance.aop.Advice;
import org.apache.atlas.annotation.GraphTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.lang.reflect.Method;

@Component
public class GraphTransactionAdvisor extends AbstractPointcutAdvisor {
    private static final Logger LOG = LoggerFactory.getLogger(GraphTransactionAdvisor.class);

    private final StaticMethodMatcherPointcut pointcut = new StaticMethodMatcherPointcut() {
        @Override
        public boolean matches(Method method, Class<?> targetClass) {
            boolean annotationPresent = method.isAnnotationPresent(GraphTransaction.class);
            if (annotationPresent) {
                LOG.info("GraphTransaction intercept for {}.{}", targetClass.getName(), method.getName());
            }
            return annotationPresent;
        }
    };

    private final GraphTransactionInterceptor interceptor;

    @Inject
    public GraphTransactionAdvisor(GraphTransactionInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public Pointcut getPointcut() {
        return pointcut;
    }

    @Override
    public Advice getAdvice() {
        return interceptor;
    }
}
