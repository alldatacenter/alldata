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

package org.apache.ranger.plugin.conditionevaluator;

import org.apache.commons.collections.CollectionUtils;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RangerAccessedNotFromClusterCondition extends RangerAbstractConditionEvaluator {
	private static final Logger LOG = LoggerFactory.getLogger(RangerAccessedNotFromClusterCondition.class);

	private boolean isAlwaysTrue = false;

	@Override
	public void init() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAccessedNotFromClusterCondition.init(" + condition + ")");
		}

		super.init();

		isAlwaysTrue = CollectionUtils.isEmpty(condition.getValues());

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAccessedNotFromClusterCondition.init(" + condition + ")");
		}
	}

	@Override
	public boolean isMatched(RangerAccessRequest request) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerAccessedNotFromClusterCondition.isMatched(" + condition + ")");
		}

		final boolean ret;

		if (isAlwaysTrue || request.getClusterName() == null) {
			ret = true;
		} else {
			ret = !condition.getValues().contains(request.getClusterName());
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerAccessedNotFromClusterCondition.isMatched(" + condition + "): " + ret);
		}

		return ret;
	}
}
