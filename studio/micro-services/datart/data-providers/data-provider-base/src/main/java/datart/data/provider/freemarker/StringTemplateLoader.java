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
package datart.data.provider.freemarker;

import freemarker.cache.TemplateLoader;
import org.apache.commons.collections4.map.LRUMap;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.Map;

public class StringTemplateLoader implements TemplateLoader {

    public static final Map<String, String> SCRIPT_MAP = Collections.synchronizedMap(new LRUMap<>(1000));

    @Override
    public Object findTemplateSource(String name) throws IOException {
        return SCRIPT_MAP.get(name);
    }

    @Override
    public long getLastModified(Object templateSource) {
        return 0;
    }

    @Override
    public Reader getReader(Object templateSource, String encoding) throws IOException {
        return new StringReader(templateSource.toString());
    }

    @Override
    public void closeTemplateSource(Object templateSource) throws IOException {

    }
}
