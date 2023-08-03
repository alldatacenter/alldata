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
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RangerSimpleMatcher extends RangerAbstractConditionEvaluator {

	private static final Logger LOG = LoggerFactory.getLogger(RangerSimpleMatcher.class);

	public static final String CONTEXT_NAME = "CONTEXT_NAME";

	private boolean _allowAny;
	private String _contextName;
	private List<String> _values = new ArrayList<>();
	
	@Override
	public void init() {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerSimpleMatcher.init(" + condition + ")");
		}

		super.init();

		if (condition == null) {
			LOG.debug("init: null policy condition! Will match always!");
			_allowAny = true;
		} else if (conditionDef == null) {
			LOG.debug("init: null policy condition definition! Will match always!");
			_allowAny = true;
		} else if (CollectionUtils.isEmpty(condition.getValues())) {
			LOG.debug("init: empty conditions collection on policy condition!  Will match always!");
			_allowAny = true;
		} else if (MapUtils.isEmpty(conditionDef.getEvaluatorOptions())) {
			LOG.debug("init: Evaluator options were empty.  Can't determine what value to use from context.  Will match always.");
			_allowAny = true;
		} else if (StringUtils.isEmpty(conditionDef.getEvaluatorOptions().get(CONTEXT_NAME))) {
			LOG.debug("init: CONTEXT_NAME is not specified in evaluator options.  Can't determine what value to use from context.  Will match always.");
			_allowAny = true;
		} else {
			_contextName = conditionDef.getEvaluatorOptions().get(CONTEXT_NAME);
			_values.addAll(condition.getValues());
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerSimpleMatcher.init(" + condition + "): countries[" + _values + "]");
		}
	}

	@Override
	public boolean isMatched(RangerAccessRequest request) {
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerSimpleMatcher.isMatched(" + request + ")");
		}

		boolean matched = false;

		if (_allowAny) {
			matched = true;
		} else {
			String requestValue = extractValue(request, _contextName);
			if (StringUtils.isNotBlank(requestValue)) {
				for (String policyValue : _values) {
					if (FilenameUtils.wildcardMatch(requestValue, policyValue)) {
						matched = true;
						break;
					}
				}
			}
		}
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerSimpleMatcher.isMatched(" + request+ "): " + matched);
		}

		return matched;
	}

	String extractValue(final RangerAccessRequest request, String key) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerSimpleMatcher.extractValue(" + request+ ")");
		}

		String value = null;
		if (request == null) {
			LOG.debug("isMatched: Unexpected: null request.  Returning null!");
		} else if (request.getContext() == null) {
			LOG.debug("isMatched: Context map of request is null.  Ok. Returning null!");
		} else if (CollectionUtils.isEmpty(request.getContext().entrySet())) {
			LOG.debug("isMatched: Missing context on request.  Ok. Condition isn't applicable.  Returning null!");
		} else if (!request.getContext().containsKey(key)) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("isMatched: Unexpected: Context did not have data for condition[" + key + "]. Returning null!");
			}
		} else {
			value = (String)request.getContext().get(key);
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerSimpleMatcher.extractValue(" + request+ "): " + value);
		}
		return value;
	}
}
