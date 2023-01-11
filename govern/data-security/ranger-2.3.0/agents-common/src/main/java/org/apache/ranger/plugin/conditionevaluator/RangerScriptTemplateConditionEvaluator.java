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
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RangerScriptTemplateConditionEvaluator extends RangerScriptConditionEvaluator {
	private static final Logger LOG = LoggerFactory.getLogger(RangerScriptTemplateConditionEvaluator.class);

	protected String  script;
	private   boolean reverseResult;

	@Override
	public void init() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerScriptTemplateConditionEvaluator.init(" + condition + ")");
		}

		super.init();

		if(CollectionUtils.isNotEmpty(condition.getValues())) {
			String expectedScriptReturn = condition.getValues().get(0);

			if(StringUtils.isNotBlank(expectedScriptReturn)) {
				if(StringUtils.equalsIgnoreCase(expectedScriptReturn, "false") || StringUtils.equalsIgnoreCase(expectedScriptReturn, "no")) {
					reverseResult = true;
				}

				script = MapUtils.getString(conditionDef.getEvaluatorOptions(), "scriptTemplate");

				if(script != null) {
					script = script.trim();
				}
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerScriptTemplateConditionEvaluator.init(" + condition + "): script=" + script + "; reverseResult=" + reverseResult);
		}
	}

	@Override
	public boolean isMatched(RangerAccessRequest request) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerScriptTemplateConditionEvaluator.isMatched()");
		}

		boolean ret = super.isMatched(request);

		if(reverseResult) {
			ret = !ret;
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerScriptTemplateConditionEvaluator.isMatched(): ret=" + ret);
		}

		return ret;
	}

	@Override
	protected String getScript() {
		return script;
	}
}
