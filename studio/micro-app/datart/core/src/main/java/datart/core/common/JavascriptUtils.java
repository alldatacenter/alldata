/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.core.common;

import datart.core.base.exception.Exceptions;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class JavascriptUtils {

    private static final ScriptEngineFactory engineFactory;

    static {
        engineFactory = new NashornScriptEngineFactory();
    }

    public static Object invoke(Invocable invocable, String functionName, Object... args) throws Exception {
        if (invocable != null) {
            return invocable.invokeFunction(functionName, args);
        }
        return null;
    }

    public static Invocable load(String path) throws IOException, ScriptException {
        InputStream stream = JavascriptUtils.class.getClassLoader().getResourceAsStream(path);
        if (stream == null) {
            Exceptions.notFound(path);
        }
        try (InputStreamReader reader = new InputStreamReader(stream)) {
            ScriptEngine engine = engineFactory.getScriptEngine();
            engine.eval(reader);
            if (engine instanceof Invocable) {
                return (Invocable) engine;
            }
            return null;
        }
    }

}
