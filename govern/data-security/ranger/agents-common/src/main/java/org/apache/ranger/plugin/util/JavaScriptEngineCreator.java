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

package org.apache.ranger.plugin.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;


public class JavaScriptEngineCreator implements ScriptEngineCreator {
    private static final Logger LOG = LoggerFactory.getLogger(JavaScriptEngineCreator.class);

    static final String ENGINE_NAME = "JavaScript";

    public ScriptEngine getScriptEngine(ClassLoader clsLoader) {
        ScriptEngine ret = null;

        if (clsLoader == null) {
            clsLoader = Thread.currentThread().getContextClassLoader();
        }

        try {
            ScriptEngineManager mgr = new ScriptEngineManager(clsLoader);

            ret = mgr.getEngineByName(ENGINE_NAME);
        } catch (Throwable t) {
            LOG.debug("JavaScriptEngineCreator.getScriptEngine(): failed to create engine type {}", ENGINE_NAME, t);
        }

        if (ret == null) {
            LOG.debug("JavaScriptEngineCreator.getScriptEngine(): failed to create engine type {}", ENGINE_NAME);
        }

        return ret;
    }
}
