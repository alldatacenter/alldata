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

package org.apache.ranger.plugin.policyevaluator;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemCondition;
import org.apache.ranger.plugin.conditionevaluator.RangerConditionEvaluator;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.policyengine.RangerPolicyEngineOptions;
import org.apache.ranger.plugin.util.RangerPerfTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


public class RangerCustomConditionEvaluator {

    private static final Logger LOG = LoggerFactory.getLogger(RangerCustomConditionEvaluator.class);
    private static final Logger PERF_POLICY_INIT_LOG = RangerPerfTracer.getPerfLogger("policy.init");
    private static final Logger PERF_POLICYITEM_INIT_LOG = RangerPerfTracer.getPerfLogger("policyitem.init");
    private static final Logger PERF_POLICYCONDITION_INIT_LOG = RangerPerfTracer.getPerfLogger("policycondition.init");

    public List<RangerConditionEvaluator> getRangerPolicyConditionEvaluator(RangerPolicy policy,
                                                                                  RangerServiceDef serviceDef,
                                                                                  RangerPolicyEngineOptions options) {
        List<RangerConditionEvaluator> conditionEvaluators = new ArrayList<>();

        if (!getConditionsDisabledOption(options) && CollectionUtils.isNotEmpty(policy.getConditions())) {

            RangerPerfTracer perf = null;

            long policyId = policy.getId();

            if(RangerPerfTracer.isPerfTraceEnabled(PERF_POLICY_INIT_LOG)) {
                perf = RangerPerfTracer.getPerfTracer(PERF_POLICY_INIT_LOG, "RangerCustomConditionEvaluator.init(policyId=" + policyId + ")");
            }

            for (RangerPolicy.RangerPolicyItemCondition condition : policy.getConditions()) {
                RangerServiceDef.RangerPolicyConditionDef conditionDef = getConditionDef(condition.getType(),serviceDef);

                if (conditionDef == null) {
                    LOG.error("RangerCustomConditionEvaluator.getRangerPolicyConditionEvaluator(policyId=" + policyId + "): conditionDef '" + condition.getType() + "' not found. Ignoring the condition");

                    continue;
                }

                RangerConditionEvaluator conditionEvaluator = newConditionEvaluator(conditionDef.getEvaluator());

                if (conditionEvaluator != null) {
                    conditionEvaluator.setServiceDef(serviceDef);
                    conditionEvaluator.setConditionDef(conditionDef);
                    conditionEvaluator.setPolicyItemCondition(condition);

                    RangerPerfTracer perfConditionInit = null;

                    if (RangerPerfTracer.isPerfTraceEnabled(PERF_POLICYCONDITION_INIT_LOG)) {
                        perfConditionInit = RangerPerfTracer.getPerfTracer(PERF_POLICYCONDITION_INIT_LOG, "RangerConditionEvaluator.init(policyId=" + policyId + "policyConditionType=" + condition.getType() + ")");
                    }

                    conditionEvaluator.init();

                    RangerPerfTracer.log(perfConditionInit);

                    conditionEvaluators.add(conditionEvaluator);
                } else {
                    LOG.error("RangerCustomConditionEvaluator.getRangerPolicyConditionEvaluator(policyId=" + policyId + "): failed to init Policy ConditionEvaluator '" + condition.getType() + "'; evaluatorClassName='" + conditionDef.getEvaluator() + "'");
                }
            }

            RangerPerfTracer.log(perf);
        }
        return conditionEvaluators;
    }


    public List<RangerConditionEvaluator> getPolicyItemConditionEvaluator(RangerPolicy policy,
                                                                           RangerPolicyItem policyItem,
                                                                           RangerServiceDef serviceDef,
                                                                           RangerPolicyEngineOptions options,
                                                                           int policyItemIndex) {

        List<RangerConditionEvaluator> conditionEvaluators = new ArrayList<>();

        if (!getConditionsDisabledOption(options) && CollectionUtils.isNotEmpty(policyItem.getConditions())) {

            RangerPerfTracer perf = null;

            Long policyId = policy.getId();

            if(RangerPerfTracer.isPerfTraceEnabled(PERF_POLICYITEM_INIT_LOG)) {
                perf = RangerPerfTracer.getPerfTracer(PERF_POLICYITEM_INIT_LOG, "RangerPolicyItemEvaluator.getRangerPolicyConditionEvaluator(policyId=" + policyId + ",policyItemIndex=" + policyItemIndex + ")");
            }

            for (RangerPolicyItemCondition condition : policyItem.getConditions()) {
                RangerServiceDef.RangerPolicyConditionDef conditionDef = getConditionDef(condition.getType(), serviceDef);

                if (conditionDef == null) {
                    LOG.error("RangerCustomConditionEvaluator.getPolicyItemConditionEvaluator(policyId=" + policyId + "): conditionDef '" + condition.getType() + "' not found. Ignoring the condition");

                    continue;
                }

                RangerConditionEvaluator conditionEvaluator = newConditionEvaluator(conditionDef.getEvaluator());

                if (conditionEvaluator != null) {
                    conditionEvaluator.setServiceDef(serviceDef);
                    conditionEvaluator.setConditionDef(conditionDef);
                    conditionEvaluator.setPolicyItemCondition(condition);

                    RangerPerfTracer perfConditionInit = null;

                    if(RangerPerfTracer.isPerfTraceEnabled(PERF_POLICYCONDITION_INIT_LOG)) {
                        perfConditionInit = RangerPerfTracer.getPerfTracer(PERF_POLICYCONDITION_INIT_LOG, "RangerConditionEvaluator.init(policyId=" + policyId + ",policyItemIndex=" + policyItemIndex + ",policyConditionType=" + condition.getType() + ")");
                    }

                    conditionEvaluator.init();

                    RangerPerfTracer.log(perfConditionInit);

                    conditionEvaluators.add(conditionEvaluator);
                } else {
                    LOG.error("RangerCustomConditionEvaluator.getPolicyItemConditionEvaluator(policyId=" + policyId + "): failed to init PolicyItem ConditionEvaluator '" + condition.getType() + "'; evaluatorClassName='" + conditionDef.getEvaluator() + "'");
                }
            }
            RangerPerfTracer.log(perf);
        }
        return  conditionEvaluators;
    }

    private RangerServiceDef.RangerPolicyConditionDef getConditionDef(String conditionName, RangerServiceDef serviceDef) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> RangerCustomConditionEvaluator.getConditionDef(" + conditionName + ")");
        }

        RangerServiceDef.RangerPolicyConditionDef ret = null;

        if (serviceDef != null && CollectionUtils.isNotEmpty(serviceDef.getPolicyConditions())) {
            for(RangerServiceDef.RangerPolicyConditionDef conditionDef : serviceDef.getPolicyConditions()) {
                if(StringUtils.equals(conditionName, conditionDef.getName())) {
                    ret = conditionDef;
                    break;
                }
            }
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== RangerCustomConditionEvaluator.getConditionDef(" + conditionName + "): " + ret);
        }

        return ret;
    }


    private RangerConditionEvaluator newConditionEvaluator(String className) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("==> RangerCustomConditionEvaluator.newConditionEvaluator(" + className + ")");
        }

        RangerConditionEvaluator evaluator = null;

        try {
            @SuppressWarnings("unchecked")
            Class<org.apache.ranger.plugin.conditionevaluator.RangerConditionEvaluator> matcherClass = (Class<org.apache.ranger.plugin.conditionevaluator.RangerConditionEvaluator>)Class.forName(className);

            evaluator = matcherClass.newInstance();
        } catch(Throwable t) {
            LOG.error("RangerCustomConditionEvaluator.newConditionEvaluator(" + className + "): error instantiating evaluator", t);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("<== RangerCustomConditionEvaluator.newConditionEvaluator(" + className + "): " + evaluator);
        }

        return evaluator;
    }

    private boolean getConditionsDisabledOption(RangerPolicyEngineOptions options) {
        return options != null && options.disableCustomConditions;
    }
}
