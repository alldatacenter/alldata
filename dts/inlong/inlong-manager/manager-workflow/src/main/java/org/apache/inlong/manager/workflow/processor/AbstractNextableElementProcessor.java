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

package org.apache.inlong.manager.workflow.processor;

import org.apache.inlong.manager.common.exceptions.WorkflowException;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.definition.Element;
import org.apache.inlong.manager.workflow.definition.EndEvent;
import org.apache.inlong.manager.workflow.definition.NextableElement;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Non-terminal element processor
 */
public abstract class AbstractNextableElementProcessor<T extends NextableElement> implements ElementProcessor<T> {

    @Override
    public List<Element> next(T element, WorkflowContext context) {
        WorkflowContext.ActionContext actionContext = context.getActionContext();
        List<Element> nextElements = element.getNextList(actionContext.getAction(), context);
        Preconditions.expectNotEmpty(nextElements, "not found next element ");

        Element endEvent = nextElements
                .stream()
                .filter(EndEvent.class::isInstance)
                .findFirst()
                .orElse(null);

        if (endEvent == null) {
            return nextElements;
        }

        List<Element> notEndEventElements = nextElements.stream()
                .filter(ele -> !(ele instanceof EndEvent))
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(notEndEventElements)) {
            return Collections.singletonList(endEvent);
        }

        throw new WorkflowException("process definition error, find endEvent and not endEvent at the same time");
    }

}
