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
import jdk.nashorn.api.scripting.ClassFilter;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

public class NashornScriptEngineCreator implements ScriptEngineCreator {
    private static final Logger LOG = LoggerFactory.getLogger(NashornScriptEngineCreator.class);

    private static final String[] SCRIPT_ENGINE_ARGS = new String[] { "--no-java", "--no-syntax-extensions" };
    private static final String   ENGINE_NAME        = "NashornScriptEngine";

    @Override
    public ScriptEngine getScriptEngine(ClassLoader clsLoader) {
        ScriptEngine ret = null;

        if (clsLoader == null) {
            clsLoader = Thread.currentThread().getContextClassLoader();
        }

        try {
            NashornScriptEngineFactory factory = new NashornScriptEngineFactory();

            ret = factory.getScriptEngine(SCRIPT_ENGINE_ARGS, clsLoader, RangerClassFilter.INSTANCE);
        } catch (Throwable t) {
            LOG.debug("NashornScriptEngineCreator.getScriptEngine(): failed to create engine type {}", ENGINE_NAME, t);
        }

        return ret;
    }

    private static class RangerClassFilter implements ClassFilter {
        static final RangerClassFilter INSTANCE = new RangerClassFilter();

        private RangerClassFilter() {
        }

        @Override
        public boolean exposeToScripts(String className) {
            LOG.warn("script blocked: attempt to use Java class {}", className);

            return false;
        }
    }
}
