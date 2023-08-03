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

import com.github.pagehelper.autoconfigure.PageHelperAutoConfiguration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

import java.util.List;

/**
 * Configuration to add {@link MultiTenantInterceptor} into the sql session interceptorChain.
 *
 * <p>This MultiTenantInterceptor must be added after {@link com.github.pagehelper.PageInterceptor} to ensure the
 * correct parameter mapping.</p>
 */
@Configuration
public class MultiTenantInterceptorConfiguration {

    @Autowired
    private List<SqlSessionFactory> sqlSessionFactoryList;
    @Autowired
    private PageHelperAutoConfiguration pageHelperAutoConfiguration;

    @PostConstruct
    public void init() throws Exception {
        MultiTenantInterceptor multiTenantInterceptor = new MultiTenantInterceptor();
        for (SqlSessionFactory sqlSessionFactory : sqlSessionFactoryList) {
            org.apache.ibatis.session.Configuration configuration = sqlSessionFactory.getConfiguration();
            configuration.addInterceptor(multiTenantInterceptor);
        }
    }
}
