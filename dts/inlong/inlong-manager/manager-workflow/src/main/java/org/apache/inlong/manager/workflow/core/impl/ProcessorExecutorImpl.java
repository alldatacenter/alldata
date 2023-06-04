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

package org.apache.inlong.manager.workflow.core.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.common.exceptions.WorkflowException;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.core.ProcessorExecutor;
import org.apache.inlong.manager.workflow.definition.Element;
import org.apache.inlong.manager.workflow.processor.ElementProcessor;
import org.apache.inlong.manager.workflow.processor.EndEventProcessor;
import org.apache.inlong.manager.workflow.processor.ServiceTaskProcessor;
import org.apache.inlong.manager.workflow.processor.StartEventProcessor;
import org.apache.inlong.manager.workflow.processor.UserTaskProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Workload component processor execution
 */
@Slf4j
@Service
public class ProcessorExecutorImpl implements ProcessorExecutor {

    private ImmutableMap<Class<? extends Element>, ElementProcessor<? extends Element>> elementProcessor;

    @Autowired
    private StartEventProcessor startEventProcessor;
    @Autowired
    private EndEventProcessor endEventProcessor;
    @Autowired
    private UserTaskProcessor userTaskProcessor;
    @Autowired
    private ServiceTaskProcessor serviceTaskProcessor;

    @PostConstruct
    private void initProcessors() {
        List<ElementProcessor<?>> processors = Lists.newArrayList();
        processors.add(startEventProcessor);
        processors.add(endEventProcessor);
        processors.add(userTaskProcessor);
        processors.add(serviceTaskProcessor);

        ImmutableMap.Builder<Class<? extends Element>, ElementProcessor<? extends Element>> builder =
                ImmutableMap.builder();
        processors.forEach(processor -> builder.put(processor.watch(), processor));
        elementProcessor = builder.build();
    }

    private ElementProcessor<? extends Element> getProcessor(Class<? extends Element> elementClazz) {
        if (!elementProcessor.containsKey(elementClazz)) {
            throw new WorkflowException("element executor not found " + elementClazz.getName());
        }
        return elementProcessor.get(elementClazz);
    }

    @Override
    public void executeStart(Element element, WorkflowContext context) {
        ElementProcessor processor = this.getProcessor(element.getClass());
        context.setCurrentElement(element);

        if (!processor.create(element, context)) {
            return;
        }

        if (processor.pendingForAction(context)) {
            return;
        }

        executeComplete(element, context);
    }

    @Override
    public void executeComplete(Element element, WorkflowContext context) {
        ElementProcessor processor = this.getProcessor(element.getClass());
        context.setCurrentElement(element);
        if (!processor.complete(context)) {
            return;
        }
        List<Element> nextElements = processor.next(element, context);
        for (Element next : nextElements) {
            executeStart(next, context);
        }
    }

}
