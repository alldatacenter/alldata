/*
 * Copyright 2021 WeBank
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.url;

import org.apache.commons.lang.StringUtils;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Default linkis stream handler factory (support specific schemas)
 */
public class LinkisURLStreamHandlerFactory implements URLStreamHandlerFactory {

    /**
     * Support schemas
     */
    private final List<String> supportSchemas = new ArrayList<>();

    /**
     * Stream handler
     */
    private final URLStreamHandler defaultStreamHandler;

    public LinkisURLStreamHandlerFactory(String... schemas){
        supportSchemas.addAll(Arrays.asList(schemas));
        this.defaultStreamHandler = new LinkisURLStreamHandler();
    }

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if (supportSchemas.stream().anyMatch( schema -> schema.equals(protocol))){
            return this.defaultStreamHandler;
        }
        return null;
    }
}
