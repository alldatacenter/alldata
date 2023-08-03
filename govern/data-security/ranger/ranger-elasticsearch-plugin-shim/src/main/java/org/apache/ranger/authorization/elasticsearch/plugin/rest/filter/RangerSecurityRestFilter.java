/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.authorization.elasticsearch.plugin.rest.filter;

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.authorization.elasticsearch.plugin.authc.user.UsernamePasswordToken;
import org.apache.ranger.authorization.elasticsearch.plugin.utils.RequestUtils;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RangerSecurityRestFilter extends AbstractLifecycleComponent implements RestHandler {

	private static final Logger LOG = LoggerFactory.getLogger(RangerSecurityRestFilter.class);

	private final RestHandler restHandler;

	private final ThreadContext threadContext;

	public RangerSecurityRestFilter(final ThreadContext threadContext,
			final RestHandler restHandler) {
		super();
		this.restHandler = restHandler;
		this.threadContext = threadContext;
	}

	@Override
	public void handleRequest(final RestRequest request, final RestChannel channel, final NodeClient client)
			throws Exception {
		// Now only support to get user from request,
		// it should work with other elasticsearch identity authentication plugins in fact.
		UsernamePasswordToken user = UsernamePasswordToken.parseToken(request);
		if (user == null) {
			throw new ElasticsearchStatusException("Error: User is null, the request requires user authentication.",
					RestStatus.UNAUTHORIZED);
		} else {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Success to parse user[{}] from request[{}].", user, request);
			}
		}
		threadContext.putTransient(UsernamePasswordToken.USERNAME, user.getUsername());

		String clientIPAddress = RequestUtils.getClientIPAddress(request);
		if (StringUtils.isNotEmpty(clientIPAddress)) {
			threadContext.putTransient(RequestUtils.CLIENT_IP_ADDRESS, clientIPAddress);
		}

		this.restHandler.handleRequest(request, channel, client);
	}

    @Override
    protected void doStart() {

    }

    @Override
    protected void doStop() {

    }

    @Override
    protected void doClose() {

    }
}
