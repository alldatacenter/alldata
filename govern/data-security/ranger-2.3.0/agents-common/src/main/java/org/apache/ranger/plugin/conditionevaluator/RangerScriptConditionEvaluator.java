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
import org.apache.ranger.plugin.policyengine.RangerRequestScriptEvaluator;
import org.apache.ranger.plugin.util.ScriptEngineUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import java.util.List;
import java.util.Map;

import static org.apache.ranger.plugin.util.RangerCommonConstants.*;

public class RangerScriptConditionEvaluator extends RangerAbstractConditionEvaluator {
	private static final Logger LOG = LoggerFactory.getLogger(RangerScriptConditionEvaluator.class);

	private ScriptEngine scriptEngine;
	private Boolean      enableJsonCtx = null;

	@Override
	public void init() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerScriptConditionEvaluator.init(" + condition + ")");
		}

		super.init();

		String              engineName  = "JavaScript";
		Map<String, String> evalOptions = conditionDef. getEvaluatorOptions();

		if (MapUtils.isNotEmpty(evalOptions)) {
			engineName = evalOptions.get("engineName");

			String strEnableJsonCtx = evalOptions.get(SCRIPT_OPTION_ENABLE_JSON_CTX);

			if (StringUtils.isNotEmpty(strEnableJsonCtx)) {
				enableJsonCtx = Boolean.parseBoolean(strEnableJsonCtx);
			}
		}

		if (StringUtils.isBlank(engineName)) {
			engineName = "JavaScript";
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("RangerScriptConditionEvaluator.init() - engineName=" + engineName);
		}

		scriptEngine = ScriptEngineUtil.createScriptEngine(engineName, serviceDef.getName());

		if (scriptEngine == null) {
			String conditionType = condition != null ? condition.getType() : null;

			LOG.error("failed to initialize condition '" + conditionType + "': script engine '" + engineName + "' was not created");
		} else {
			LOG.info("ScriptEngine for engineName=[" + engineName + "] is successfully created");
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerScriptConditionEvaluator.init(" + condition + ")");
		}
	}

	@Override
	public boolean isMatched(RangerAccessRequest request) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerScriptConditionEvaluator.isMatched()");
		}

		boolean result = true;

		if (scriptEngine != null) {
			String script = getScript();

			if (StringUtils.isNotBlank(script)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("RangerScriptConditionEvaluator.isMatched(): script={" + script + "}");
				}

				RangerRequestScriptEvaluator evaluator = new RangerRequestScriptEvaluator(request);

				if (enableJsonCtx == null) { // if not specified in evaluatorOptions, set it on first call to isMatched()
					enableJsonCtx = RangerRequestScriptEvaluator.needsJsonCtxEnabled(script);
				}

				evaluator.evaluateConditionScript(scriptEngine, script, enableJsonCtx);

				result = evaluator.getResult();
			} else {
				String conditionType = condition != null ? condition.getType() : null;

				LOG.error("failed to evaluate condition '" + conditionType + "': script is empty");
			}
		} else {
			String conditionType = condition != null ? condition.getType() : null;

			LOG.error("failed to evaluate condition '" + conditionType + "': script engine not found");
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerScriptConditionEvaluator.isMatched(), result=" + result);
		}

		return result;

	}

	protected String getScript() {
		String       ret    = null;
		List<String> values = condition.getValues();

		if (CollectionUtils.isNotEmpty(values)) {
			String value = values.get(0);

			if (StringUtils.isNotBlank(value)) {
				ret = value.trim();
			}
		}

		return ret;
	}
}
