/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qlangtech.tis.plugin.annotation;

import com.alibaba.citrus.turbine.Context;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qlangtech.tis.plugin.ValidatorCommons;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public enum Validator {

    require((msgHandler, context, fieldKey, fieldData) -> {
        if (StringUtils.isBlank(fieldData)) {
            msgHandler.addFieldError(context, fieldKey, ValidatorCommons.MSG_EMPTY_INPUT_ERROR);
            return false;
        }
        return true;
    }),
    user_name((msgHandler, context, fieldKey, fieldData) -> {

        return validatePattern(msgHandler, context
                , rule(ValidatorCommons.pattern_user_name, ValidatorCommons.MSG_USER_NAME_ERROR), fieldKey, fieldData);

//        if (StringUtils.isEmpty(fieldData)) {
//            return true;
//        }
//        Matcher matcher = ValidatorCommons.pattern_identity.matcher(fieldData);
//        if (!matcher.matches()) {
//            msgHandler.addFieldError(context, fieldKey, ValidatorCommons.MSG_IDENTITY_ERROR);
//            return false;
//        }
//        return true;
    }),
    identity((msgHandler, context, fieldKey, fieldData) -> {

        return validatePattern(msgHandler, context
                , rule(ValidatorCommons.pattern_identity, ValidatorCommons.MSG_IDENTITY_ERROR), fieldKey, fieldData);

//        if (StringUtils.isEmpty(fieldData)) {
//            return true;
//        }
//        Matcher matcher = ValidatorCommons.pattern_identity.matcher(fieldData);
//        if (!matcher.matches()) {
//            msgHandler.addFieldError(context, fieldKey, ValidatorCommons.MSG_IDENTITY_ERROR);
//            return false;
//        }
//        return true;
    }),
    //
    integer((msgHandler, context, fieldKey, fieldData) -> {

        return validatePattern(msgHandler, context
                , rule(ValidatorCommons.pattern_integer, ValidatorCommons.MSG_INTEGER_ERROR), fieldKey, fieldData);

//        if (StringUtils.isEmpty(fieldData)) {
//            return true;
//        }
//        Matcher matcher = ValidatorCommons.pattern_integer.matcher(fieldData);
//        if (!matcher.matches()) {
//            msgHandler.addFieldError(context, fieldKey, ValidatorCommons.MSG_INTEGER_ERROR);
//            return false;
//        }
//        return true;
    }),
    //
    host((msgHandler, context, fieldKey, fieldData) -> {

        return validatePattern(msgHandler, context
                , rule(ValidatorCommons.host_pattern, ValidatorCommons.MSG_HOST_IP_ERROR), fieldKey, fieldData);
    }),

    hostWithoutPort((msgHandler, context, fieldKey, fieldData) -> {
        return validatePattern(msgHandler, context
                , rule(ValidatorCommons.host_without_port_pattern, ValidatorCommons.MSG_HOST_IP_WITHOUT_PORT_ERROR), fieldKey, fieldData);
    }),



    //
    url((msgHandler, context, fieldKey, fieldData) -> {

        return validatePattern(msgHandler, context
                , rule(ValidatorCommons.PATTERN_URL, ValidatorCommons.MSG_URL_ERROR), fieldKey, fieldData);

//        if (StringUtils.isEmpty(fieldData)) {
//            return true;
//        }
//        Matcher matcher = ValidatorCommons.PATTERN_URL.matcher(fieldData);
//        if (!matcher.matches()) {
//            msgHandler.addFieldError(context, fieldKey, ValidatorCommons.MSG_URL_ERROR);
//            return false;
//        }
//        return true;
    }),
    db_col_name((msgHandler, context, fieldKey, fieldData) -> {

        return validatePattern(msgHandler, context
                , rule(ValidatorCommons.PATTERN_DB_COL_NAME, ValidatorCommons.MSG_DB_COL_NAME_ERROR), fieldKey, fieldData);

//        if (StringUtils.isEmpty(fieldData)) {
//            return true;
//        }
//        Matcher matcher = ValidatorCommons.PATTERN_DB_COL_NAME.matcher(fieldData);
//        if (!matcher.matches()) {
//            msgHandler.addFieldError(context, fieldKey, ValidatorCommons.MSG_DB_COL_NAME_ERROR);
//            return false;
//        }
//        return true;
    }),
    relative_path((msgHandler, context, fieldKey, fieldData) -> {
        return validatePattern(msgHandler, context
                , rule(ValidatorCommons.PATTERN_RELATIVE_PATH, ValidatorCommons.MSG_RELATIVE_PATH_ERROR), fieldKey, fieldData);
    }),
    absolute_path((msgHandler, context, fieldKey, fieldData) -> {
        return validatePattern(msgHandler, context
                , rule(ValidatorCommons.PATTERN_ABSOLUTE_PATH, ValidatorCommons.MSG_ABSOLUTE_PATH_ERROR), fieldKey, fieldData);

//        if (StringUtils.isEmpty(fieldData)) {
//            return true;
//        }
//        Matcher matcher = ValidatorCommons.PATTERN_ABSOLUTE_PATH.matcher(fieldData);
//        if (!matcher.matches()) {
//            msgHandler.addFieldError(context, fieldKey, ValidatorCommons.MSG_ABSOLUTE_PATH_ERROR);
//            return false;
//        }
//        return true;
    }),
    none_blank((msgHandler, context, fieldKey, fieldData) -> {
        return validatePattern(msgHandler, context
                , rule(ValidatorCommons.PATTERN_NONE_BLANK, ValidatorCommons.MSG_NONE_BLANK_ERROR), fieldKey, fieldData);
    }),
    ;

    static ValidateRule rule(Pattern p, String errMsg) {
        return new ValidateRule(p, errMsg);
    }

    private static boolean validatePattern(IFieldErrorHandler msgHandler, Context context, ValidateRule validateRule, String fieldKey, String fieldData) {
        if (StringUtils.isEmpty(fieldData)) {
            return true;
        }
        Matcher matcher = validateRule.pattern.matcher(fieldData);
        if (!matcher.matches()) {
            msgHandler.addFieldError(context, fieldKey, validateRule.errorMsg);
            return false;
        }
        return true;
    }

    private static class ValidateRule {
        private final Pattern pattern;
        private final String errorMsg;

        public ValidateRule(Pattern pattern, String errorMsg) {
            this.pattern = pattern;
            this.errorMsg = errorMsg;
        }
    }

    private final IFieldValidator fieldValidator;

    public IFieldValidator getFieldValidator() {
        return this.fieldValidator;
    }

    public static final String FORM_ERROR_SUMMARY = "提交表单内容有错误";

    /**
     * @param msgHandler
     * @param context
     * @param fieldKey
     * @param fieldData
     * @return false 校验失败
     */
    public boolean validate(//
                            IFieldErrorHandler msgHandler, //
                            Context context, String fieldKey, String fieldData) {
        return this.fieldValidator.validate(msgHandler, context, fieldKey, fieldData);
    }

    /**
     * 开始校验
     *
     * @param handler
     * @param context
     * @param fieldKey
     * @param
     * @return
     */
    private static //
    FieldValidatorResult validate(//
                                  IControlMsgHandler handler, //
                                  Context context, String fieldKey, FieldValidators fvalidator) {

        FieldValidatorResult fieldData = new FieldValidatorResult(handler.getString(fieldKey));
        for (IFieldValidator v : fvalidator.validators) {
            if (!v.validate(handler, context, fieldKey, fieldData.fieldData)) {
                return fieldData.faild();
            }
        }
        fvalidator.setFieldVal(fieldData.fieldData);
        return fieldData.success();
    }

    /**
     * String:fieldName , FieldValidators：利用现有的枚举校验规则 , IFieldValidator 自定义校验规则, Object[] 规则组
     * 构建form校验规则
     *
     * @param p
     * @return
     */
    public static Map<String, FieldValidators> fieldsValidator(Object... p) {
        // }
        if (p.length < 1 || !(p[0] instanceof String)) {
            throw new IllegalArgumentException();
        }

        Map<String, FieldValidators> result = Maps.newHashMap();
        addValidateRule(result, p);
        return result;
    }

    public static void addValidateRule(Map<String, FieldValidators> result, Object[] p) {
        if (p == null || p.length < 1) {
            return;
        }
        String fieldName = null;
        List<Object> rules = null;
        for (int i = 0; i < p.length; i++) {
            if (p[i] instanceof String) {
                if (fieldName != null) {
                    processPreFieldValidateRule(result, fieldName, rules);
                }
                fieldName = (String) p[i];
                rules = Lists.newArrayList();
                // 开始定义一个新的Validator
                // result.put(fieldName,fieldValidators);
            } else if (p[i].getClass().isArray()) {
                Object[] rparams = (Object[]) p[i];
                if (rparams.length > 0) {
                    addValidateRule(result, rparams);
                }
                // 保证数组下一个元素必须为String类型
                if ((i + 1) < p.length && !(p[i + 1] instanceof String)) {
                    throw new IllegalStateException("index:" + (i + 1) + ",element:" + p[i + 1] + " must be type of 'String'");
                }
            } else {
                // fieldValidators.validators
                rules.add(p[i]);
            }
            // result.put((String) p[i], (FieldValidators) p[i + 1]);
        }
        processPreFieldValidateRule(result, fieldName, rules);
    }

    private static void processPreFieldValidateRule(Map<String, FieldValidators> result, String fieldName, List<Object> rules) {
        if (StringUtils.isEmpty(fieldName)) {
            throw new IllegalArgumentException("param fieldName can not be null");
        }
        if (CollectionUtils.isEmpty(rules)) {
            throw new IllegalArgumentException("param rules can not be empty");
        }
        List<FieldValidators> validatContainer = rules.stream().filter((r) -> r instanceof FieldValidators).map((r) -> (FieldValidators) r).collect(Collectors.toList());
        FieldValidators validator = null;
        int validatContainerSize = validatContainer.size();
        if (validatContainerSize == 1) {
            validator = validatContainer.stream().findFirst().get();
        } else if (validatContainerSize < 1) {
            validator = new FieldValidators() {
            };
        } else {
            throw new IllegalStateException("field:" + fieldName + " relevant rules must contain one 'FieldValidators' but now the size is " + validatContainer.size());
        }
        List<IFieldValidator> fieldValidators = rules.stream().filter((r) -> {
            boolean isFieldValidator = false;
            if (!((isFieldValidator = (r instanceof IFieldValidator)) || r instanceof FieldValidators)) {
                throw new IllegalStateException("rule type:" + r.getClass() + " is not valid");
            }
            return isFieldValidator;
        }).map((r) -> (IFieldValidator) r).collect(Collectors.toList());
        validator.validators.addAll(fieldValidators);
        result.put(fieldName, validator);
    }

    /**
     * @param handler
     * @param context
     * @param fieldsValidator
     * @return false: 失败
     */
    public static //
    boolean validate(//
                     IControlMsgHandler handler, //
                     Context context, Map<String, FieldValidators> fieldsValidator) {
        handler.errorsPageShow(context);
        String fieldKey = null;
        FieldValidators fvalidator = null;
        FieldValidatorResult vresult = null;
        // 第一轮 校验依赖节点为空的
        boolean faild = validateAllFieldRules(handler, true, context, fieldsValidator)
                || validateAllFieldRules(handler, false, context, fieldsValidator);
        if (faild) {
            // 判断提交的plugin表单是否有错误？错误则退出
            handler.addErrorMessage(context, FORM_ERROR_SUMMARY);
        }
        return !faild;
    }

    /**
     * @param handler
     * @param validateEmptyDependency 是否只校验依赖节点为空的规则
     * @param context
     * @param fieldsValidator
     * @return
     */
    private static boolean validateAllFieldRules(IControlMsgHandler handler
            , boolean validateEmptyDependency, Context context, Map<String, FieldValidators> fieldsValidator) {
        String fieldKey;
        FieldValidators fvalidator;
        FieldValidatorResult vresult;
        boolean faild = false;
        for (Map.Entry<String, FieldValidators> entry : fieldsValidator.entrySet().stream().filter((e) -> {
            return validateEmptyDependency == e.getValue().dependencyRules.isEmpty();
        }).collect(Collectors.toList())) {
            fieldKey = entry.getKey();
            fvalidator = entry.getValue();

            vresult = validate(handler, context, fieldKey, fvalidator);
            if (!vresult.valid) {
                faild = true;
            }
        }
        return faild;
    }

    public abstract static class FieldValidators {

        final List<IFieldValidator> validators = Lists.newArrayList();
        private final List<String> dependencyRules = Lists.newArrayList();

        /**
         * 添加校验依赖
         *
         * @param rule
         */
        public FieldValidators addDependency(String rule) {
            this.dependencyRules.add(rule);
            return this;
        }

        public FieldValidators(Validator... validators) {
            for (Validator validator : validators) {
                this.validators.add(validator.fieldValidator);
            }
        }

        public void setFieldVal(String val) {
        }
    }

    public static class FieldValidatorResult {

        public boolean valid;

        public final String fieldData;

        public FieldValidatorResult(String fieldData) {
            this.fieldData = fieldData;
        }

        public FieldValidatorResult faild() {
            this.valid = false;
            return this;
        }

        public FieldValidatorResult success() {
            this.valid = true;
            return this;
        }
    }

    Validator(IFieldValidator fv) {
        this.fieldValidator = fv;
    }

    public interface IFieldValidator {

        /**
         * @param msgHandler
         * @param context
         * @param fieldKey
         * @param fieldData
         * @return false：校验失败有错误
         */
        boolean validate(//
                         IFieldErrorHandler msgHandler, //
                         Context context, String fieldKey, String fieldData);
    }
}
