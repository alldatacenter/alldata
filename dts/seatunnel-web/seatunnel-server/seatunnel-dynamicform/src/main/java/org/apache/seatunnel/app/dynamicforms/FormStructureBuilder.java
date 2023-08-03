/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.app.dynamicforms;

import org.apache.seatunnel.app.dynamicforms.exception.FormStructureValidateException;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FormStructureBuilder {
    private String name;

    private List<AbstractFormOption> forms = new ArrayList<>();

    private FormLocale locales;

    private Map<String, Map<String, String>> apis;

    public FormStructureBuilder name(@NonNull String name) {
        this.name = name;
        return this;
    }

    public FormStructureBuilder addFormOption(@NonNull AbstractFormOption... formOptions) {
        for (AbstractFormOption formOption : formOptions) {
            forms.add(formOption);
        }
        return this;
    }

    public FormStructureBuilder withLocale(FormLocale locale) {
        this.locales = locale;
        return this;
    }

    public FormStructureBuilder addApi(
            @NonNull String apiName,
            @NonNull String url,
            @NonNull FormStructure.HttpMethod method) {
        if (apis == null) {
            apis = new HashMap<>();
        }
        apis.putIfAbsent(apiName, new HashMap<>());
        apis.get(apiName).put("url", url);
        apis.get(apiName).put("method", method.name().toLowerCase(java.util.Locale.ROOT));

        return this;
    }

    public FormStructure build() throws FormStructureValidateException {
        FormStructure formStructure = new FormStructure(name, forms, locales, apis);
        FormStructureValidate.validateFormStructure(formStructure);
        return formStructure;
    }
}
