package com.webank.wedatasphere.streamis.jobmanager.launcher.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.webank.wedatasphere.streamis.jobmanager.launcher.entity.JobConfDefinition;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * According to JobConfDefinition
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobConfDefinitionVo {

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
    @JsonProperty("validate_type")
    private String validateType;

    /**
     * Validate rule
     */
    @JsonProperty("validate_rule")
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
    @JsonProperty("default_value")
    private String defaultValue;

    /**
     * Refer values
     */
    @JsonProperty("ref_values")
    private List<String> refValues = new ArrayList<>();

    /**
     * Children definition
     */
    @JsonProperty("child_def")
    private List<JobConfDefinitionVo> childDef;

    private boolean required;

    public JobConfDefinitionVo(){

    }

    public JobConfDefinitionVo(JobConfDefinition definition){
        this.key = definition.getKey();
        this.name = definition.getName();
        this.type = definition.getType();
        this.sort = definition.getSort();
        this.description = definition.getDescription();
        this.validateType = definition.getValidateType();
        this.validateRule = definition.getValidateRule();
        this.style = definition.getStyle();
        this.visiable = definition.getVisiable();
        this.level = definition.getLevel();
        this.defaultValue = definition.getDefaultValue();
        if (StringUtils.isNotBlank(definition.getRefValues())){
            this.refValues = Arrays.asList(definition.getRefValues().split(","));
        }
        this.required = definition.isRequired();
        this.unit = definition.getUnit();
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

    public List<String> getRefValues() {
        return refValues;
    }

    public void setRefValues(List<String> refValues) {
        this.refValues = refValues;
    }

    public List<JobConfDefinitionVo> getChildDef() {
        return childDef;
    }

    public void setChildDef(List<JobConfDefinitionVo> childDef) {
        this.childDef = childDef;
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
