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
import org.apache.seatunnel.app.dynamicforms.validate.AbstractValidate;
import org.apache.seatunnel.app.dynamicforms.validate.MutuallyExclusiveValidate;
import org.apache.seatunnel.app.dynamicforms.validate.UnionNonEmptyValidate;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Check whether the form structure is correct */
public class FormStructureValidate {

    /** validate rules */
    public static void validateFormStructure(@NonNull FormStructure formStructure)
            throws FormStructureValidateException {

        List<String> apiErrorList = validateApiOption(formStructure);
        List<String> localeErrorList = validateLocaleOption(formStructure);
        List<String> showErrorList = validateShow(formStructure);
        List<String> unionNonErrorList = validateUnionNonEmpty(formStructure);
        List<String> exclusiveErrorList = validateMutuallyExclusive(formStructure);

        apiErrorList.addAll(localeErrorList);
        apiErrorList.addAll(showErrorList);
        apiErrorList.addAll(unionNonErrorList);
        apiErrorList.addAll(exclusiveErrorList);

        if (apiErrorList.size() > 0) {
            throw new FormStructureValidateException(formStructure.getName(), apiErrorList);
        }
    }

    private static List<String> validateApiOption(@NonNull FormStructure formStructure) {
        List<String> errorMessageList = new ArrayList();
        Map<String, Map<String, String>> apis = formStructure.getApis();
        formStructure
                .getForms()
                .forEach(
                        formOption -> {
                            if (formOption instanceof DynamicSelectOption) {
                                String api = ((DynamicSelectOption) formOption).getApi();
                                if (apis == null || !apis.keySet().contains(api)) {
                                    errorMessageList.add(
                                            String.format(
                                                    "DynamicSelectOption[%s] used api[%s] can not found in FormStructure.apis",
                                                    ((DynamicSelectOption) formOption).getLabel(),
                                                    api));
                                }
                            }
                        });
        return errorMessageList;
    }

    private static List<String> validateLocaleOption(@NonNull FormStructure formStructure) {
        List<String> errorMessageList = new ArrayList();
        FormLocale locales = formStructure.getLocales();
        formStructure
                .getForms()
                .forEach(
                        formOption -> {
                            if (formOption.getLabel().startsWith(FormLocale.I18N_PREFIX)) {
                                String labelName =
                                        formOption.getLabel().replace(FormLocale.I18N_PREFIX, "");
                                validateOneI18nOption(
                                        locales,
                                        formOption.getLabel(),
                                        "label",
                                        labelName,
                                        errorMessageList);
                            }

                            if (formOption.getDescription().startsWith(FormLocale.I18N_PREFIX)) {
                                String description =
                                        formOption
                                                .getDescription()
                                                .replace(FormLocale.I18N_PREFIX, "");
                                validateOneI18nOption(
                                        locales,
                                        formOption.getLabel(),
                                        "description",
                                        description,
                                        errorMessageList);
                            }

                            if (formOption.getPlaceholder().startsWith(FormLocale.I18N_PREFIX)) {
                                String placeholder =
                                        formOption
                                                .getPlaceholder()
                                                .replace(FormLocale.I18N_PREFIX, "");
                                validateOneI18nOption(
                                        locales,
                                        formOption.getLabel(),
                                        "placeholder",
                                        placeholder,
                                        errorMessageList);
                            }

                            AbstractValidate validate = formOption.getValidate();
                            if (validate != null
                                    && validate.getMessage().startsWith(FormLocale.I18N_PREFIX)) {
                                String message =
                                        validate.getMessage().replace(FormLocale.I18N_PREFIX, "");
                                validateOneI18nOption(
                                        locales,
                                        formOption.getLabel(),
                                        "validateMessage",
                                        message,
                                        errorMessageList);
                            }
                        });
        return errorMessageList;
    }

    private static void validateOneI18nOption(
            FormLocale locale,
            @NonNull String formOptionLabel,
            @NonNull String formOptionName,
            @NonNull String key,
            @NonNull List<String> errorMessageList) {
        if (locale == null || !locale.getEnUS().containsKey(key)) {
            errorMessageList.add(
                    String.format(
                            "FormOption[%s] used i18n %s[%s] can not found in FormStructure.locales en_US",
                            formOptionLabel, formOptionName, key));
        }

        if (locale == null || !locale.getZhCN().containsKey(key)) {
            errorMessageList.add(
                    String.format(
                            "FormOption[%s] used i18n %s[%s] can not found in FormStructure.locales zh_CN",
                            formOptionLabel, formOptionName, key));
        }
    }

    private static List<String> validateShow(@NonNull FormStructure formStructure) {
        List<String> errorMessageList = new ArrayList();
        // Find all select options
        List<String> allFields =
                formStructure.getForms().stream()
                        .map(formOption -> formOption.getField())
                        .collect(Collectors.toList());
        formStructure
                .getForms()
                .forEach(
                        formOption -> {
                            Map show = formOption.getShow();
                            if (show == null) {
                                return;
                            }

                            String field = show.get("field").toString();
                            if (allFields == null || !allFields.contains(field)) {
                                errorMessageList.add(
                                        String.format(
                                                "FormOption[%s] used show field[%s] can not found in form options",
                                                formOption.getLabel(), field));
                            }
                        });

        return errorMessageList;
    }

    private static List<String> validateUnionNonEmpty(@NonNull FormStructure formStructure) {
        List<String> errorMessageList = new ArrayList();
        Map<String, List<String>> unionMap = new HashMap<>();
        // find all union-non-empty options
        formStructure
                .getForms()
                .forEach(
                        formOption -> {
                            if (formOption.getValidate() != null
                                    && formOption.getValidate() instanceof UnionNonEmptyValidate) {
                                unionMap.put(
                                        formOption.getField(),
                                        ((UnionNonEmptyValidate) formOption.getValidate())
                                                .getFields());
                            }
                        });

        unionMap.forEach(
                (k, v) -> {
                    if (v == null || !v.contains(k)) {
                        errorMessageList.add(
                                String.format(
                                        "UnionNonEmptyValidate Option field[%s] must in validate union field list",
                                        k));
                    }

                    if (v != null) {
                        v.forEach(
                                field -> {
                                    if (!unionMap.keySet().contains(field)) {
                                        errorMessageList.add(
                                                String.format(
                                                        "UnionNonEmptyValidate Option field[%s] , validate union field[%s] can not found in form options",
                                                        k, field));
                                    }
                                });
                    }
                });

        return errorMessageList;
    }

    private static List<String> validateMutuallyExclusive(@NonNull FormStructure formStructure) {
        List<String> errorMessageList = new ArrayList();
        Map<String, List<String>> exclusiveMap = new HashMap<>();
        // find all mutually-exclusive options
        formStructure
                .getForms()
                .forEach(
                        formOption -> {
                            if (formOption.getValidate() != null
                                    && formOption.getValidate()
                                            instanceof MutuallyExclusiveValidate) {
                                exclusiveMap.put(
                                        formOption.getField(),
                                        ((MutuallyExclusiveValidate) formOption.getValidate())
                                                .getFields());
                            }
                        });

        exclusiveMap.forEach(
                (k, v) -> {
                    if (v == null || !v.contains(k)) {
                        errorMessageList.add(
                                String.format(
                                        "MutuallyExclusiveValidate Option field[%s] must in validate field list",
                                        k));
                    }

                    if (v != null) {
                        v.forEach(
                                field -> {
                                    if (!exclusiveMap.keySet().contains(field)) {
                                        errorMessageList.add(
                                                String.format(
                                                        "MutuallyExclusiveValidate Option field[%s] , validate field[%s] can not found in form options",
                                                        k, field));
                                    }
                                });
                    }
                });

        return errorMessageList;
    }
}
