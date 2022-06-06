/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.web.filters;

import org.apache.atlas.repository.graph.AtlasGraphProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * Filter that rolls back the stale transaction associated with
 * the thread running this request so that the request will
 * see all of the committed changes.
 *
 */
@Component
public class StaleTransactionCleanupFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(StaleTransactionCleanupFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        //nothing to do
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
    throws IOException, ServletException {
        LOG.debug("Cleaning stale transactions");
        AtlasGraphProvider.getGraphInstance().rollback();
        filterChain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // do nothing
    }
}
