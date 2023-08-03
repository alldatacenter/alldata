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
import org.apache.seatunnel.app.dynamicforms.validate.ValidateBuilder;
import org.apache.seatunnel.common.utils.JsonUtils;

import org.apache.commons.lang3.tuple.ImmutablePair;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class FormStructureBuilderTest {

    @Test
    public void testFormStructureBuild() {
        FormLocale locale = new FormLocale();
        locale.addZhCN("name_password_union_required", "all name and password are required")
                .addZhCN("username", "username")
                .addEnUS("name_password_union_required", "all name and password are required")
                .addEnUS("username", "username");

        FormInputOption nameOption =
                (FormInputOption)
                        FormOptionBuilder.builder()
                                .withI18nLabel("username")
                                .withField("username")
                                .inputOptionBuilder()
                                .formTextInputOption()
                                .withDescription("username")
                                .withClearable()
                                .withPlaceholder("username")
                                .withShow("checkType", Arrays.asList("nameAndPassword"))
                                .withValidate(
                                        ValidateBuilder.builder()
                                                .unionNonEmptyValidateBuilder()
                                                .fields("username", "password")
                                                .unionNonEmptyValidate()
                                                .withI18nMessage("name_password_union_required"));

        FormInputOption passwordOption =
                (FormInputOption)
                        FormOptionBuilder.builder()
                                .withLabel("password")
                                .withField("password")
                                .inputOptionBuilder()
                                .formPasswordInputOption()
                                .withDescription("password")
                                .withPlaceholder("password")
                                .withShow("checkType", Arrays.asList("nameAndPassword"))
                                .withValidate(
                                        ValidateBuilder.builder()
                                                .unionNonEmptyValidateBuilder()
                                                .fields("username", "password")
                                                .unionNonEmptyValidate()
                                                .withI18nMessage("name_password_union_required"));

        FormInputOption textAreaOption =
                (FormInputOption)
                        FormOptionBuilder.builder()
                                .withLabel("content")
                                .withField("context")
                                .inputOptionBuilder()
                                .formTextareaInputOption()
                                .withClearable()
                                .withDescription("content");

        StaticSelectOption checkTypeOption =
                (StaticSelectOption)
                        FormOptionBuilder.builder()
                                .withLabel("checkType")
                                .withField("checkType")
                                .staticSelectOptionBuilder()
                                .addSelectOptions(
                                        new ImmutablePair("no", "no"),
                                        new ImmutablePair("nameAndPassword", "nameAndPassword"))
                                .formStaticSelectOption()
                                .withClearable()
                                .withDefaultValue("no")
                                .withDescription("check type")
                                .withValidate(
                                        ValidateBuilder.builder()
                                                .nonEmptyValidateBuilder()
                                                .nonEmptyValidate());

        DynamicSelectOption cityOption =
                (DynamicSelectOption)
                        FormOptionBuilder.builder()
                                .withField("city")
                                .withLabel("city")
                                .dynamicSelectOptionBuilder()
                                .withSelectApi("getCity")
                                .formDynamicSelectOption()
                                .withDescription("city")
                                .withValidate(
                                        ValidateBuilder.builder()
                                                .nonEmptyValidateBuilder()
                                                .nonEmptyValidate());

        AbstractFormOption exclusive1 =
                FormOptionBuilder.builder()
                        .withField("exclusive1")
                        .withLabel("exclusive1")
                        .inputOptionBuilder()
                        .formTextInputOption()
                        .withValidate(
                                ValidateBuilder.builder()
                                        .mutuallyExclusiveValidateBuilder()
                                        .fields("exclusive1", "exclusive2")
                                        .mutuallyExclusiveValidate());

        AbstractFormOption exclusive2 =
                FormOptionBuilder.builder()
                        .withField("exclusive2")
                        .withLabel("exclusive2")
                        .inputOptionBuilder()
                        .formTextInputOption()
                        .withValidate(
                                ValidateBuilder.builder()
                                        .mutuallyExclusiveValidateBuilder()
                                        .fields("exclusive1", "exclusive2")
                                        .mutuallyExclusiveValidate());

        FormStructure testForm =
                FormStructure.builder()
                        .name("testForm")
                        .addFormOption(
                                nameOption,
                                passwordOption,
                                textAreaOption,
                                checkTypeOption,
                                cityOption,
                                exclusive1,
                                exclusive2)
                        .withLocale(locale)
                        .addApi("getCity", "/api/get_city", FormStructure.HttpMethod.GET)
                        .build();

        String s = JsonUtils.toJsonString(testForm);
        String templateFilePath = TestUtils.getResource("test_form.json");
        //        String result = FileUtils.readFileToStr(Paths.get(templateFilePath));
        //        Assertions.assertEquals(result, s);
    }

    @Test
    public void testFormStructureValidate() {
        FormLocale locale = new FormLocale();
        locale.addZhCN("name_password_union_required", "all name and password are required")
                .addEnUS("name_password_union_required", "all name and password are required")
                .addEnUS("username", "username");

        FormInputOption nameOption =
                (FormInputOption)
                        FormOptionBuilder.builder()
                                .withI18nLabel("username")
                                .withField("username")
                                .inputOptionBuilder()
                                .formTextInputOption()
                                .withDescription("username")
                                .withClearable()
                                .withPlaceholder("username")
                                .withShow("checkType1", Arrays.asList("nameAndPassword"))
                                .withValidate(
                                        ValidateBuilder.builder()
                                                .unionNonEmptyValidateBuilder()
                                                .fields("user", "password")
                                                .unionNonEmptyValidate()
                                                .withI18nMessage("name_password_union_required"));

        FormInputOption passwordOption =
                (FormInputOption)
                        FormOptionBuilder.builder()
                                .withLabel("password")
                                .withField("password")
                                .inputOptionBuilder()
                                .formPasswordInputOption()
                                .withDescription("password")
                                .withPlaceholder("password")
                                .withShow("checkType", Arrays.asList("nameAndPassword"))
                                .withValidate(
                                        ValidateBuilder.builder()
                                                .unionNonEmptyValidateBuilder()
                                                .fields("username", "password")
                                                .unionNonEmptyValidate()
                                                .withI18nMessage("name_password_union_required"));

        FormInputOption textAreaOption =
                (FormInputOption)
                        FormOptionBuilder.builder()
                                .withLabel("content")
                                .withField("context")
                                .inputOptionBuilder()
                                .formTextareaInputOption()
                                .withClearable()
                                .withDescription("content");

        StaticSelectOption checkTypeOption =
                (StaticSelectOption)
                        FormOptionBuilder.builder()
                                .withLabel("checkType")
                                .withField("checkType")
                                .staticSelectOptionBuilder()
                                .addSelectOptions(
                                        new ImmutablePair("no", "no"),
                                        new ImmutablePair("nameAndPassword", "nameAndPassword"))
                                .formStaticSelectOption()
                                .withClearable()
                                .withDefaultValue("no")
                                .withDescription("check type")
                                .withValidate(
                                        ValidateBuilder.builder()
                                                .nonEmptyValidateBuilder()
                                                .nonEmptyValidate());

        DynamicSelectOption cityOption =
                (DynamicSelectOption)
                        FormOptionBuilder.builder()
                                .withField("city")
                                .withLabel("city")
                                .dynamicSelectOptionBuilder()
                                .withSelectApi("getCity")
                                .formDynamicSelectOption()
                                .withDescription("city")
                                .withValidate(
                                        ValidateBuilder.builder()
                                                .nonEmptyValidateBuilder()
                                                .nonEmptyValidate());

        AbstractFormOption exclusive1 =
                FormOptionBuilder.builder()
                        .withField("exclusive1")
                        .withLabel("exclusive1")
                        .inputOptionBuilder()
                        .formTextInputOption()
                        .withValidate(
                                ValidateBuilder.builder()
                                        .mutuallyExclusiveValidateBuilder()
                                        .fields("exclusive1", "exclusive2")
                                        .mutuallyExclusiveValidate());

        AbstractFormOption exclusive2 =
                FormOptionBuilder.builder()
                        .withField("exclusive2")
                        .withLabel("exclusive2")
                        .inputOptionBuilder()
                        .formTextInputOption()
                        .withValidate(
                                ValidateBuilder.builder()
                                        .mutuallyExclusiveValidateBuilder()
                                        .fields("exclusive1", "exclusive2")
                                        .mutuallyExclusiveValidate());

        AbstractFormOption exclusive3 =
                FormOptionBuilder.builder()
                        .withField("exclusive3")
                        .withLabel("exclusive3")
                        .inputOptionBuilder()
                        .formTextInputOption()
                        .withValidate(
                                ValidateBuilder.builder()
                                        .mutuallyExclusiveValidateBuilder()
                                        .fields("exclusive1", "exclusive2")
                                        .mutuallyExclusiveValidate());

        AbstractFormOption exclusive4 =
                FormOptionBuilder.builder()
                        .withField("exclusive4")
                        .withLabel("exclusive4")
                        .inputOptionBuilder()
                        .formTextInputOption()
                        .withValidate(
                                ValidateBuilder.builder()
                                        .mutuallyExclusiveValidateBuilder()
                                        .fields("exclusive1", "exclusive4", "exclusive5")
                                        .mutuallyExclusiveValidate());

        String error = "";
        try {
            FormStructure testForm =
                    FormStructure.builder()
                            .name("testForm")
                            .addFormOption(
                                    nameOption,
                                    passwordOption,
                                    textAreaOption,
                                    checkTypeOption,
                                    cityOption,
                                    exclusive1,
                                    exclusive3,
                                    exclusive2,
                                    exclusive4)
                            .withLocale(locale)
                            .addApi("getCity1", "/api/get_city", FormStructure.HttpMethod.GET)
                            .build();
        } catch (FormStructureValidateException e) {
            error = e.getMessage();
        }

        String result =
                "Form: testForm, validate error - "
                        + "[DynamicSelectOption[city] used api[getCity] can not found in FormStructure.apis, "
                        + "FormOption[i18n.username] used i18n label[username] can not found in FormStructure.locales zh_CN, "
                        + "FormOption[i18n.username] used show field[checkType1] can not found in form options, "
                        + "UnionNonEmptyValidate Option field[username] must in validate union field list, "
                        + "UnionNonEmptyValidate Option field[username] , validate union field[user] can not found in form options, "
                        + "MutuallyExclusiveValidate Option field[exclusive3] must in validate field list, "
                        + "MutuallyExclusiveValidate Option field[exclusive4] , validate field[exclusive5] can not found in form options]";
        Assertions.assertEquals(result, error);
    }
}
