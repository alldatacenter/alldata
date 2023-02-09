/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.internal.handler;

import java.lang.reflect.Method;
import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;

import org.xml.sax.Attributes;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.obs.log.ILogger;
import com.obs.log.LoggerBuilder;

public abstract class SimpleHandler extends DefaultHandler {
    private static final ILogger LOG = LoggerBuilder.getLogger(SimpleHandler.class);

    protected XMLReader xr;
    private StringBuilder textBuffer;

    private Deque<SimpleHandler> handlerStack = new LinkedBlockingDeque<>();

    public SimpleHandler(XMLReader xr) {
        this.xr = xr;
        this.textBuffer = new StringBuilder();
        this.handlerStack.push(this);
    }

    public void transferControl(SimpleHandler toHandler) {
        toHandler.setHandlerStack(this.handlerStack);
        this.handlerStack.push(toHandler);
        setReaderHandler(handlerStack.peek());
    }

    public void returnControlToParentHandler() {
        if (hasParentHandler()) {
            this.handlerStack.removeFirst();
            this.handlerStack.peek().controlReturned(this);
            setReaderHandler(handlerStack.peek());
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("this class has no parent: " + this.getClass().getName());
            }
        }
    }

    public boolean hasParentHandler() {
        return handlerStack.size() >= 2;
    }

    public void controlReturned(SimpleHandler childHandler) {
        return;
    }

    public void setHandlerStack(Deque<SimpleHandler> handlerStack) {
        this.handlerStack = handlerStack;
    }

    @Override
    public void startElement(String uri, String name, String qualifiedName, Attributes attrs) {
        invokeMethodWithoutException("start" + name, null);
    }

    @Override
    public void endElement(String uri, String name, String qualifiedName) {
        String elementContent = this.textBuffer.toString();

        invokeMethodWithoutException("end" + name, elementContent);

        this.textBuffer = new StringBuilder();
    }

    private void setReaderHandler(SimpleHandler handler) {
        xr.setContentHandler(handler);
        xr.setErrorHandler(handler);
    }

    @SuppressWarnings("rawtypes")
    private void invokeMethodWithoutException(String methodName, String parameter) {
        SimpleHandler handler = this.handlerStack.peek();

        if (null == handler) {
            if (LOG.isInfoEnabled()) {
                LOG.info("non-existent SimpleHandler in " + this.getClass().getName());
            }
            return;
        }

        Class[] clazz = new Class[] {};
        Object[] parameters = new Object[] {};

        if (null != parameter) {
            clazz = new Class[] {String.class};
            parameters = new Object[] {parameter};
        }

        try {
            Method method = handler.getClass().getMethod(methodName, clazz);
            method.invoke(handler, parameters);
        } catch (NoSuchMethodException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("non-existent SimpleHandler subclass's method for '" + methodName + "' in "
                        + this.getClass().getName());
            }
        } catch (Throwable t) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Unable to invoke SimpleHandler subclass's method for '" + methodName + "' in "
                        + this.getClass().getName(), t);
            }
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        this.textBuffer.append(ch, start, length);
    }

}
