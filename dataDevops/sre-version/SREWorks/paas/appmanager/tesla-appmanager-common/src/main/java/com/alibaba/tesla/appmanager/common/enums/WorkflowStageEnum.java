package com.alibaba.tesla.appmanager.common.enums;

/**
 * Workflow Task Stage 枚举类型
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public enum WorkflowStageEnum {

    PRE_RENDER("pre-render"),

    POST_RENDER("post-render"),

    POST_DEPLOY("post-deploy");

    private final String text;

    WorkflowStageEnum(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }

    /**
     * String to Enum
     *
     * @param text String
     * @return Enum
     */
    public static WorkflowStageEnum fromString(String text) {
        for (WorkflowStageEnum item : WorkflowStageEnum.values()) {
            if (item.text.equalsIgnoreCase(text)) {
                return item;
            }
        }
        return PRE_RENDER;
    }
}
