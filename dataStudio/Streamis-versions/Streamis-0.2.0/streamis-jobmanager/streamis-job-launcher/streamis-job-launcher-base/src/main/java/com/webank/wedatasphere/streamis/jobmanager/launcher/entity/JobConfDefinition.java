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

package com.webank.wedatasphere.streamis.jobmanager.launcher.entity;

/**
 * Job configuration definition
 */
public class JobConfDefinition {
    /**
     * Id
     */
    private Long id;

    /**
     * keyword
     */
    private String key;

    /**
     * Display name equals 'option'
     */
    private String name;

    /**
     * Type: NONE: 0, INPUT: 1, SELECT: 2, NUMBER: 3
     */
    private String type;

    /**
     * Sort
     */
    private Integer sort;

    /**
     * Description
     */
    private String description;

    /**
     * Validate type
     */
    private String validateType;

    /**
     * Validate rule
     */
    private String validateRule;

    /**
     * Style (Json/html/css)
     */
    private String style;

    /**
     * Visiable
     */
    private int visiable = 1;

    /**
     * Level
     */
    private int level = 1;

    /**
     * Unit symbol
     */
    private String unit;

    /**
     * Default value
     */
    private String defaultValue;

    /**
     * Refer values
     */
    private String refValues;

    /**
     * Parent ref
     */
    private Long parentRef;

    /**
     * Is required
     */
    private boolean required;

    public JobConfDefinition(){

    }

    public JobConfDefinition(Long id, String key,
                             String type, Long parentRef, Integer level){
        this.id = id;
        this.key = key;
        this.type = type;
        this.parentRef = parentRef;
        this.level = level;
    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getValidateType() {
        return validateType;
    }

    public void setValidateType(String validateType) {
        this.validateType = validateType;
    }

    public String getValidateRule() {
        return validateRule;
    }

    public void setValidateRule(String validateRule) {
        this.validateRule = validateRule;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public int getVisiable() {
        return visiable;
    }

    public void setVisiable(int visiable) {
        this.visiable = visiable;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getRefValues() {
        return refValues;
    }

    public void setRefValues(String refValues) {
        this.refValues = refValues;
    }

    public Long getParentRef() {
        return parentRef;
    }

    public void setParentRef(Long parentRef) {
        this.parentRef = parentRef;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }
}
