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

import org.apache.seatunnel.api.configuration.Option;
import org.apache.seatunnel.api.configuration.SingleChoiceOption;
import org.apache.seatunnel.api.configuration.util.Expression;
import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.api.configuration.util.RequiredOption;
import org.apache.seatunnel.app.dynamicforms.AbstractFormOption;
import org.apache.seatunnel.app.dynamicforms.FormLocale;
import org.apache.seatunnel.app.dynamicforms.FormOptionBuilder;
import org.apache.seatunnel.app.dynamicforms.FormStructure;
import org.apache.seatunnel.app.dynamicforms.FormStructureBuilder;
import org.apache.seatunnel.app.dynamicforms.validate.ValidateBuilder;
import org.apache.seatunnel.common.constants.PluginType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import lombok.NonNull;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.apache.seatunnel.app.common.SeaTunnelConnectorI18n.CONNECTOR_I18N_CONFIG_EN;
import static org.apache.seatunnel.app.common.SeaTunnelConnectorI18n.CONNECTOR_I18N_CONFIG_ZH;

public class SeaTunnelOptionRuleWrapper {
    public static FormStructure wrapper(
            @NonNull OptionRule optionRule,
            @NonNull String connectorName,
            @NonNull PluginType pluginType) {
        return wrapper(
                optionRule.getOptionalOptions(),
                optionRule.getRequiredOptions(),
                connectorName + "[" + pluginType.getType() + "]");
    }

    public static FormStructure wrapper(
            @NonNull List<Option<?>> optionList,
            @NonNull List<RequiredOption> requiredList,
            @NonNull String connectorName,
            @NonNull PluginType pluginType) {
        return wrapper(optionList, requiredList, connectorName + "[" + pluginType.getType() + "]");
    }

    public static FormStructure wrapper(@NonNull OptionRule optionRule, @NonNull String name) {
        return wrapper(optionRule.getOptionalOptions(), optionRule.getRequiredOptions(), name);
    }

    public static FormStructure wrapper(
            @NonNull List<Option<?>> optionList,
            @NonNull List<RequiredOption> requiredList,
            @NonNull String name) {
        FormLocale locale = new FormLocale();
        List<AbstractFormOption> optionFormOptions = wrapperOptionOptions(name, optionList, locale);
        List<List<AbstractFormOption>> requiredFormOptions =
                wrapperRequiredOptions(name, requiredList, locale);

        FormStructureBuilder formStructureBuilder = FormStructure.builder().name(name);

        if (!CollectionUtils.isEmpty(requiredFormOptions)) {
            requiredFormOptions.forEach(
                    list -> {
                        if (CollectionUtils.isEmpty(list)) {
                            return;
                        }

                        formStructureBuilder.addFormOption(list.toArray(new AbstractFormOption[1]));
                    });
        }

        if (!CollectionUtils.isEmpty(optionFormOptions)) {
            formStructureBuilder.addFormOption(
                    optionFormOptions.toArray(new AbstractFormOption[1]));
        }

        formStructureBuilder.withLocale(locale);

        return FormOptionSort.sortFormStructure(formStructureBuilder.build());
    }

    private static List<AbstractFormOption> wrapperOptionOptions(
            @NonNull String connectorName, @NonNull List<Option<?>> optionList, FormLocale locale) {
        return optionList.stream()
                .map(
                        option -> {
                            return wrapperToFormOption(connectorName, option, locale);
                        })
                .collect(Collectors.toList());
    }

    private static List<List<AbstractFormOption>> wrapperRequiredOptions(
            @NonNull String connectorName,
            @NonNull List<RequiredOption> requiredList,
            FormLocale locale) {
        List<List<AbstractFormOption>> formOptionsList =
                requiredList.stream()
                        .map(
                                option -> {
                                    if (option
                                            instanceof RequiredOption.AbsolutelyRequiredOptions) {
                                        List<AbstractFormOption> collect =
                                                ((RequiredOption.AbsolutelyRequiredOptions) option)
                                                        .getRequiredOption().stream()
                                                                .map(
                                                                        requiredOption -> {
                                                                            return wrapperToFormOption(
                                                                                            connectorName,
                                                                                            requiredOption,
                                                                                            locale)
                                                                                    .withValidate(
                                                                                            ValidateBuilder
                                                                                                    .builder()
                                                                                                    .nonEmptyValidateBuilder()
                                                                                                    .nonEmptyValidate());
                                                                        })
                                                                .collect(Collectors.toList());
                                        return collect;
                                    }

                                    if (option instanceof RequiredOption.BundledRequiredOptions) {
                                        List<Option<?>> bundledRequiredOptions =
                                                ((RequiredOption.BundledRequiredOptions) option)
                                                        .getRequiredOption();
                                        List<String> bundledFields =
                                                bundledRequiredOptions.stream()
                                                        .map(requiredOption -> requiredOption.key())
                                                        .collect(Collectors.toList());

                                        List<AbstractFormOption> collect =
                                                bundledRequiredOptions.stream()
                                                        .map(
                                                                requiredOption -> {
                                                                    AbstractFormOption
                                                                            bundledRequiredFormOption =
                                                                                    wrapperToFormOption(
                                                                                            connectorName,
                                                                                            requiredOption,
                                                                                            locale);
                                                                    bundledRequiredFormOption
                                                                            .withValidate(
                                                                                    ValidateBuilder
                                                                                            .builder()
                                                                                            .unionNonEmptyValidateBuilder()
                                                                                            .fields(
                                                                                                    bundledFields
                                                                                                            .toArray(
                                                                                                                    new String
                                                                                                                            [1]))
                                                                                            .unionNonEmptyValidate());
                                                                    return bundledRequiredFormOption;
                                                                })
                                                        .collect(Collectors.toList());
                                        return collect;
                                    }

                                    if (option instanceof RequiredOption.ExclusiveRequiredOptions) {
                                        List<Option<?>> exclusiveOptions =
                                                ((RequiredOption.ExclusiveRequiredOptions) option)
                                                        .getExclusiveOptions();
                                        List<String> exclusiveFields =
                                                exclusiveOptions.stream()
                                                        .map(requiredOption -> requiredOption.key())
                                                        .collect(Collectors.toList());

                                        List<AbstractFormOption> collect =
                                                exclusiveOptions.stream()
                                                        .map(
                                                                requiredOption -> {
                                                                    AbstractFormOption
                                                                            exclusiveRequiredFormOption =
                                                                                    wrapperToFormOption(
                                                                                                    connectorName,
                                                                                                    requiredOption,
                                                                                                    locale)
                                                                                            .withValidate(
                                                                                                    ValidateBuilder
                                                                                                            .builder()
                                                                                                            .mutuallyExclusiveValidateBuilder()
                                                                                                            .fields(
                                                                                                                    exclusiveFields
                                                                                                                            .toArray(
                                                                                                                                    new String
                                                                                                                                            [1]))
                                                                                                            .mutuallyExclusiveValidate());
                                                                    return exclusiveRequiredFormOption;
                                                                })
                                                        .collect(Collectors.toList());
                                        return collect;
                                    }

                                    if (option
                                            instanceof RequiredOption.ConditionalRequiredOptions) {
                                        RequiredOption.ConditionalRequiredOptions
                                                conditionalRequiredOptions =
                                                        (RequiredOption.ConditionalRequiredOptions)
                                                                option;

                                        // we only support one field to control a form option, so we
                                        // only need get condition key from the
                                        // first expression. And all expression is 'or' and every
                                        // condition have the same key.
                                        String conditionKey =
                                                conditionalRequiredOptions
                                                        .getExpression()
                                                        .getCondition()
                                                        .getOption()
                                                        .key();
                                        List<Object> expectValueList = new ArrayList<>();
                                        Expression expression =
                                                conditionalRequiredOptions.getExpression();
                                        expectValueList.add(
                                                expression
                                                        .getCondition()
                                                        .getExpectValue()
                                                        .toString());
                                        while (expression.hasNext()) {
                                            expression = expression.getNext();
                                            expectValueList.add(
                                                    expression
                                                            .getCondition()
                                                            .getExpectValue()
                                                            .toString());
                                        }
                                        List<AbstractFormOption> collect =
                                                conditionalRequiredOptions.getRequiredOption()
                                                        .stream()
                                                        .map(
                                                                requiredOption -> {
                                                                    return wrapperToFormOption(
                                                                                    connectorName,
                                                                                    requiredOption,
                                                                                    locale)
                                                                            .withShow(
                                                                                    conditionKey,
                                                                                    expectValueList)
                                                                            .withValidate(
                                                                                    ValidateBuilder
                                                                                            .builder()
                                                                                            .nonEmptyValidateBuilder()
                                                                                            .nonEmptyValidate());
                                                                })
                                                        .collect(Collectors.toList());
                                        return collect;
                                    }

                                    throw new UnSupportWrapperException(
                                            connectorName, "Unknown", option.toString());
                                })
                        .collect(Collectors.toList());

        return formOptionsList;
    }

    private static AbstractFormOption wrapperToFormOption(
            @NonNull String connectorName, @NonNull Option<?> option, @NonNull FormLocale locale) {
        if (Boolean.class.equals(option.typeReference().getType())) {
            return selectInput(
                    connectorName,
                    option,
                    Arrays.asList(
                            new ImmutablePair("true", true), new ImmutablePair("false", false)),
                    locale);
        }

        if (Double.class.equals(option.typeReference().getType())
                || Duration.class.equals(option.typeReference().getType())
                || Float.class.equals(option.typeReference().getType())
                || Integer.class.equals(option.typeReference().getType())
                || Long.class.equals(option.typeReference().getType())
                || BigDecimal.class.equals(option.typeReference().getType())
                || String.class.equals(option.typeReference().getType())) {

            if (option.key().toLowerCase(Locale.ROOT).equals("password")) {
                return passwordInput(connectorName, option, locale);
            }
            if (option.defaultValue() != null && option.defaultValue().toString().contains("\n")) {
                return textareaInput(connectorName, option, locale);
            }

            return textInput(connectorName, option, locale);
        }

        if (option.typeReference().getType().getTypeName().startsWith("java.util.List")
                || option.typeReference().getType().getTypeName().startsWith("java.util.Map")
                || option.typeReference()
                        .getType()
                        .getTypeName()
                        .startsWith("org.apache.seatunnel.api.configuration.Options")) {

            return textareaInput(connectorName, option, locale);
        }

        if (SingleChoiceOption.class.isAssignableFrom(option.getClass())) {
            List<?> optionValues = ((SingleChoiceOption<?>) option).getOptionValues();
            List<ImmutablePair> staticSelectOptions =
                    optionValues.stream()
                            .map(o -> new ImmutablePair(o.toString(), o.toString()))
                            .collect(Collectors.toList());
            return selectInput(connectorName, option, staticSelectOptions, locale);
        }

        if (((Class) option.typeReference().getType()).isEnum()) {
            Object[] enumConstants = ((Class) option.typeReference().getType()).getEnumConstants();
            List<ImmutablePair> staticSelectOptions =
                    Arrays.stream(enumConstants)
                            .map(o -> new ImmutablePair(o.toString(), o.toString()))
                            .collect(Collectors.toList());
            return selectInput(connectorName, option, staticSelectOptions, locale);
        }

        if (((Class) option.typeReference().getType())
                .getTypeName()
                .startsWith("org.apache.seatunnel")) {
            return textareaInput(connectorName, option, locale);
        }

        // object type and map type will show as textarea
        throw new UnSupportWrapperException(
                connectorName, option.key(), option.typeReference().getType().getTypeName());
    }

    private static boolean enableLabelI18n(
            String connectorName, String optionI18nKey, FormLocale locale) {
        String realConnectorName = connectorName;
        if (connectorName.contains("[")) {
            realConnectorName = connectorName.substring(0, connectorName.indexOf("["));
        }

        boolean labelEnableI18n = false;
        if (CONNECTOR_I18N_CONFIG_ZH.hasPath(realConnectorName)
                && CONNECTOR_I18N_CONFIG_ZH.getConfig(realConnectorName).hasPath(optionI18nKey)) {
            locale.addZhCN(
                    optionI18nKey,
                    CONNECTOR_I18N_CONFIG_ZH.getConfig(realConnectorName).getString(optionI18nKey));
            labelEnableI18n = true;
        }

        if (CONNECTOR_I18N_CONFIG_EN.hasPath(realConnectorName)
                && CONNECTOR_I18N_CONFIG_EN.getConfig(realConnectorName).hasPath(optionI18nKey)) {
            locale.addEnUS(
                    optionI18nKey,
                    CONNECTOR_I18N_CONFIG_EN.getConfig(realConnectorName).getString(optionI18nKey));
            labelEnableI18n = true;
        }
        return labelEnableI18n;
    }

    private static AbstractFormOption selectInput(
            String connectorName,
            @NonNull Option<?> option,
            List<ImmutablePair> staticSelectOptions,
            FormLocale locale) {
        FormOptionBuilder builder = FormOptionBuilder.builder();
        String i18nOptionKey = option.key().replace(".", "_").replace("-", "_");
        if (enableLabelI18n(connectorName, i18nOptionKey, locale)) {
            builder = builder.withI18nLabel(i18nOptionKey);
        } else {
            builder = builder.withLabel(option.key());
        }

        FormOptionBuilder.StaticSelectOptionBuilder staticSelectOptionBuilder =
                builder.withField(option.key()).staticSelectOptionBuilder();

        for (ImmutablePair selectOption : staticSelectOptions) {
            if (enableLabelI18n(connectorName, selectOption.getLeft().toString(), locale)) {
                staticSelectOptionBuilder.addI18nSelectOptions(selectOption);
            } else {
                staticSelectOptionBuilder.addSelectOptions(selectOption);
            }
        }

        AbstractFormOption abstractFormOption =
                staticSelectOptionBuilder
                        .formStaticSelectOption()
                        .withDefaultValue(
                                option.defaultValue() == null
                                        ? null
                                        : option.defaultValue().toString());

        String placeholderI18nOptionKey = i18nOptionKey + "_description";
        if (enableLabelI18n(connectorName, placeholderI18nOptionKey, locale)) {
            abstractFormOption = abstractFormOption.withI18nPlaceholder(placeholderI18nOptionKey);
        } else {
            abstractFormOption = abstractFormOption.withPlaceholder(option.getDescription());
        }

        return abstractFormOption;
    }

    private static AbstractFormOption passwordInput(
            String connectorName, @NonNull Option<?> option, FormLocale locale) {
        FormOptionBuilder builder = FormOptionBuilder.builder();
        String i18nOptionKey = option.key().replace(".", "_").replace("-", "_");
        if (enableLabelI18n(connectorName, i18nOptionKey, locale)) {
            builder = builder.withI18nLabel(i18nOptionKey);
        } else {
            builder = builder.withLabel(option.key());
        }

        AbstractFormOption abstractFormOption =
                builder.withField(option.key())
                        .inputOptionBuilder()
                        .formPasswordInputOption()
                        .withDefaultValue(option.defaultValue());

        String placeholderI18nOptionKey = i18nOptionKey + "_description";
        if (enableLabelI18n(connectorName, placeholderI18nOptionKey, locale)) {
            abstractFormOption = abstractFormOption.withI18nPlaceholder(placeholderI18nOptionKey);
        } else {
            abstractFormOption = abstractFormOption.withPlaceholder(option.getDescription());
        }

        return abstractFormOption;
    }

    private static AbstractFormOption textInput(
            String connectorName, @NonNull Option<?> option, FormLocale locale) {
        FormOptionBuilder builder = FormOptionBuilder.builder();
        String i18nOptionKey = option.key().replace(".", "_").replace("-", "_");
        String placeholderI18nOptionKey = i18nOptionKey + "_description";
        if (enableLabelI18n(connectorName, i18nOptionKey, locale)) {
            builder = builder.withI18nLabel(i18nOptionKey);
        } else {
            builder = builder.withLabel(option.key());
        }

        AbstractFormOption abstractFormOption =
                builder.withField(option.key())
                        .inputOptionBuilder()
                        .formTextInputOption()
                        .withDefaultValue(option.defaultValue());
        if (enableLabelI18n(connectorName, placeholderI18nOptionKey, locale)) {
            abstractFormOption = abstractFormOption.withI18nPlaceholder(placeholderI18nOptionKey);
        } else {
            abstractFormOption = abstractFormOption.withPlaceholder(option.getDescription());
        }

        return abstractFormOption;
    }

    private static AbstractFormOption textareaInput(
            String connectorName, @NonNull Option<?> option, FormLocale locale) {
        FormOptionBuilder builder = FormOptionBuilder.builder();
        String i18nOptionKey = option.key().replace(".", "_").replace("-", "_");
        String placeholderI18nOptionKey = i18nOptionKey + "_description";

        if (enableLabelI18n(connectorName, i18nOptionKey, locale)) {
            builder = builder.withI18nLabel(i18nOptionKey);
        } else {
            builder = builder.withLabel(option.key());
        }

        AbstractFormOption abstractFormOption =
                builder.withField(option.key())
                        .inputOptionBuilder()
                        .formTextareaInputOption()
                        .withClearable()
                        .withDefaultValue(option.defaultValue());
        if (enableLabelI18n(connectorName, placeholderI18nOptionKey, locale)) {
            abstractFormOption = abstractFormOption.withI18nPlaceholder(placeholderI18nOptionKey);
        } else {
            abstractFormOption = abstractFormOption.withPlaceholder(option.getDescription());
        }

        return abstractFormOption;
    }
}
