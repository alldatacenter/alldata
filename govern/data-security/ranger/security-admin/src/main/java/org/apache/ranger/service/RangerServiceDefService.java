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

package org.apache.ranger.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.authorization.hadoop.config.RangerAdminConfig;
import org.apache.ranger.common.PropertiesUtil;
import org.apache.ranger.entity.XXServiceDef;
import org.apache.ranger.plugin.conditionevaluator.RangerScriptConditionEvaluator;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerPolicyConditionDef;
import org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil;
import org.apache.ranger.plugin.util.ServiceDefUtil;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;


@Service
@Scope("singleton")
public class RangerServiceDefService extends RangerServiceDefServiceBase<XXServiceDef, RangerServiceDef> {
	public static final String PROP_ENABLE_IMPLICIT_CONDITION_EXPRESSION = "ranger.servicedef.enableImplicitConditionExpression";
	public static final String IMPLICIT_CONDITION_EXPRESSION_EVALUATOR   = RangerScriptConditionEvaluator.class.getCanonicalName();
	public static final String IMPLICIT_CONDITION_EXPRESSION_NAME        = "_expression";
	public static final String IMPLICIT_CONDITION_EXPRESSION_LABEL       = "Enter boolean expression";
	public static final String IMPLICIT_CONDITION_EXPRESSION_DESC        = "Boolean expression";

	private final RangerAdminConfig config;

	public RangerServiceDefService() {
		super();

		this.config = RangerAdminConfig.getInstance();
	}

	@Override
	protected void validateForCreate(RangerServiceDef vObj) {

	}

	@Override
	protected void validateForUpdate(RangerServiceDef vObj, XXServiceDef entityObj) {

	}

	@Override
	protected XXServiceDef mapViewToEntityBean(RangerServiceDef vObj, XXServiceDef xObj, int OPERATION_CONTEXT) {
		return super.mapViewToEntityBean(vObj, xObj, OPERATION_CONTEXT);
	}

	@Override
	protected RangerServiceDef mapEntityToViewBean(RangerServiceDef vObj, XXServiceDef xObj) {
		RangerServiceDef ret =  super.mapEntityToViewBean(vObj, xObj);

		Map<String, String> serviceDefOptions = ret.getOptions();

		if (serviceDefOptions.get(RangerServiceDef.OPTION_ENABLE_DENY_AND_EXCEPTIONS_IN_POLICIES) == null) {
			boolean enableDenyAndExceptionsInPoliciesHiddenOption = config.getBoolean("ranger.servicedef.enableDenyAndExceptionsInPolicies", true);
			if (enableDenyAndExceptionsInPoliciesHiddenOption || StringUtils.equalsIgnoreCase(ret.getName(), EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_TAG_NAME)) {
				serviceDefOptions.put(RangerServiceDef.OPTION_ENABLE_DENY_AND_EXCEPTIONS_IN_POLICIES, "true");
			} else {
				serviceDefOptions.put(RangerServiceDef.OPTION_ENABLE_DENY_AND_EXCEPTIONS_IN_POLICIES, "false");
			}
			ret.setOptions(serviceDefOptions);
		}

		addImplicitConditionExpressionIfNeeded(ret);

		return ret;
	}

	public List<RangerServiceDef> getAllServiceDefs() {
		List<XXServiceDef> xxServiceDefList = getDao().getAll();
		List<RangerServiceDef> serviceDefList = new ArrayList<RangerServiceDef>();

		for (XXServiceDef xxServiceDef : xxServiceDefList) {
			RangerServiceDef serviceDef = populateViewBean(xxServiceDef);
			serviceDefList.add(serviceDef);
		}
		return serviceDefList;
	}

	public RangerServiceDef getPopulatedViewObject(XXServiceDef xServiceDef) {
		return this.populateViewBean(xServiceDef);
	}


	boolean addImplicitConditionExpressionIfNeeded(RangerServiceDef serviceDef) {
		boolean ret                      = false;
		boolean implicitConditionDefault = PropertiesUtil.getBooleanProperty(PROP_ENABLE_IMPLICIT_CONDITION_EXPRESSION, true);
		boolean implicitConditionEnabled = ServiceDefUtil.getBooleanValue(serviceDef.getOptions(), RangerServiceDef.OPTION_ENABLE_IMPLICIT_CONDITION_EXPRESSION, implicitConditionDefault);

		if (implicitConditionEnabled) {
			boolean                        exists        = false;
			Long                           maxItemId     = 0L;
			List<RangerPolicyConditionDef> conditionDefs = serviceDef.getPolicyConditions();

			if (conditionDefs == null) {
				conditionDefs = new ArrayList<>();
			}

			for (RangerPolicyConditionDef conditionDef : conditionDefs) {
				if (StringUtils.equalsIgnoreCase(conditionDef.getEvaluator(), IMPLICIT_CONDITION_EXPRESSION_EVALUATOR)) {
					exists = true;

					break;
				}

				if (conditionDef.getItemId() != null && maxItemId < conditionDef.getItemId()) {
					maxItemId = conditionDef.getItemId();
				}
			}

			if (!exists) {
				RangerPolicyConditionDef conditionDef = new RangerPolicyConditionDef();
				Map<String, String>      options      = new HashMap<>();

				options.put("ui.isMultiline", "true");

				conditionDef.setItemId(maxItemId + 1);
				conditionDef.setName(IMPLICIT_CONDITION_EXPRESSION_NAME);
				conditionDef.setLabel(IMPLICIT_CONDITION_EXPRESSION_LABEL);
				conditionDef.setDescription(IMPLICIT_CONDITION_EXPRESSION_DESC);
				conditionDef.setEvaluator(IMPLICIT_CONDITION_EXPRESSION_EVALUATOR);
				conditionDef.setEvaluatorOptions(options);

				conditionDefs.add(conditionDef);

				serviceDef.setPolicyConditions(conditionDefs);

				ret = true;
			}
		}

		return ret;
	}
}
