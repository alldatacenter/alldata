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

import org.apache.inlong.manager.workflow.WorkflowContext;

import java.util.function.Predicate;

/**
 * Next node with condition
 */
public class ConditionNextElement implements Cloneable {

    public static final Predicate<WorkflowContext> TRUE = c -> true;

    private Element element;
    private Predicate<WorkflowContext> condition = TRUE;

    public Element getElement() {
        return element;
    }

    public ConditionNextElement setElement(Element element) {
        this.element = element;
        return this;
    }

    public Predicate<WorkflowContext> getCondition() {
        return condition;
    }

    public ConditionNextElement setCondition(
            Predicate<WorkflowContext> condition) {
        this.condition = condition;
        return this;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        ConditionNextElement cloneNextElement = (ConditionNextElement) super.clone();
        cloneNextElement.setElement(element.clone());
        cloneNextElement.setCondition(condition);
        return cloneNextElement;
    }

}
