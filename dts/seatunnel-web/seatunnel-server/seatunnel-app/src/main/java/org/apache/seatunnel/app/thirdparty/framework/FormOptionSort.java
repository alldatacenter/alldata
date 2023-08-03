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
package org.apache.seatunnel.app.thirdparty.framework;

import org.apache.seatunnel.app.dynamicforms.AbstractFormOption;
import org.apache.seatunnel.app.dynamicforms.Constants;
import org.apache.seatunnel.app.dynamicforms.FormStructure;

import org.apache.commons.collections4.CollectionUtils;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FormOptionSort {
    /** Sort form structure. */
    public static FormStructure sortFormStructure(@NonNull FormStructure formStructure) {
        List<AbstractFormOption> newFormOptions = new ArrayList<>();
        List<AbstractFormOption> formOptions = formStructure.getForms();
        formOptions.forEach(
                currFormOption -> {
                    if (currFormOption.getShow() != null && currFormOption.getShow().size() > 0) {
                        return;
                    }
                    addShowOptionAfter(currFormOption, formOptions, newFormOptions);
                });

        return FormStructure.builder()
                .name(formStructure.getName())
                .withLocale(formStructure.getLocales())
                .addFormOption(newFormOptions.toArray(new AbstractFormOption[0]))
                .build();
    }

    public static void addShowOptionAfter(
            @NonNull AbstractFormOption currFormOption,
            @NonNull List<AbstractFormOption> allFormOptions,
            @NonNull List<AbstractFormOption> newFormOptions) {
        if (newFormOptions.contains(currFormOption)) {
            return;
        }

        newFormOptions.add(currFormOption);

        List<AbstractFormOption> showOptions =
                allFormOptions.stream()
                        .filter(
                                nextOption -> {
                                    return nextOption.getShow() != null
                                            && nextOption.getShow().size() > 0
                                            && nextOption
                                                    .getShow()
                                                    .get(Constants.SHOW_FIELD)
                                                    .toString()
                                                    .equals(currFormOption.getField());
                                })
                        .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(showOptions)) {
            return;
        }

        for (AbstractFormOption showOption : showOptions) {
            addShowOptionAfter(showOption, allFormOptions, newFormOptions);
        }
    }
}
