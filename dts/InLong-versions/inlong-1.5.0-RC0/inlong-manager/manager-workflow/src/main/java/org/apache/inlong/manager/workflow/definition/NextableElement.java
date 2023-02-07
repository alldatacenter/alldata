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

package org.apache.inlong.manager.workflow.definition;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.workflow.WorkflowAction;
import org.apache.inlong.manager.workflow.WorkflowContext;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Non-terminal element
 */
public abstract class NextableElement extends Element {

    public static final List<ConditionNextElement> EMPTY_NEXT = Lists.newArrayList();

    private Map<WorkflowAction, List<ConditionNextElement>> actionToNextElementMap = Maps.newHashMap();

    public NextableElement addNext(Element element) {
        return this.addNext(defaultNextAction(), ConditionNextElement.TRUE, element);
    }

    public NextableElement addNext(WorkflowAction workflowAction, Element element) {
        return this.addNext(workflowAction, ConditionNextElement.TRUE, element);
    }

    public NextableElement addNext(Predicate<WorkflowContext> condition, Element element) {
        return this.addNext(defaultNextAction(), condition, element);
    }

    public NextableElement addNext(WorkflowAction action, Predicate<WorkflowContext> condition, Element next) {
        Preconditions.checkTrue(supportedActions().contains(action),
                "not support workflow action " + action + ", it should in one of " + supportedActions());
        actionToNextElementMap.computeIfAbsent(action, ac -> Lists.newArrayList())
                .add(new ConditionNextElement().setCondition(condition).setElement(next));
        return this;
    }

    public List<Element> getNextList(WorkflowContext context) {
        return this.getNextList(defaultNextAction(), context);
    }

    public List<Element> getNextList(WorkflowAction action, WorkflowContext context) {
        Preconditions.checkTrue(supportedActions().contains(action),
                "not support workflow action " + action + ", it should in one of " + supportedActions());
        return this.actionToNextElementMap.getOrDefault(action, NextableElement.EMPTY_NEXT)
                .stream()
                .filter(conditionNextElement -> conditionNextElement.getCondition().test(context))
                .map(ConditionNextElement::getElement)
                .collect(Collectors.toList());
    }

    public abstract WorkflowAction defaultNextAction();

    protected abstract Set<WorkflowAction> supportedActions();

    @Override
    public void validate() {
        Preconditions.checkNotEmpty(actionToNextElementMap,
                "next elements cannot be null " + this.getClass().getName());
    }

    public Map<WorkflowAction, List<ConditionNextElement>> getActionToNextElementMap() {
        return actionToNextElementMap;
    }

    public NextableElement setActionToNextElementMap(
            Map<WorkflowAction, List<ConditionNextElement>> actionToNextElementMap) {
        this.actionToNextElementMap = actionToNextElementMap;
        return this;
    }

    @Override
    public NextableElement clone() throws CloneNotSupportedException {
        NextableElement nextAbleElement = (NextableElement) super.clone();
        Map<WorkflowAction, List<ConditionNextElement>> cloneActionToNextElementMap = Maps.newHashMap();
        actionToNextElementMap.forEach(
                (k, v) -> cloneActionToNextElementMap.put(k, v.stream().map(ele -> {
                    try {
                        return (ConditionNextElement) ele.clone();
                    } catch (CloneNotSupportedException e) {
                        e.printStackTrace();
                    }
                    return null;
                }).collect(Collectors.toList())));
        return nextAbleElement.setActionToNextElementMap(cloneActionToNextElementMap);
    }

}
