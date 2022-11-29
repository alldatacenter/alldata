package cn.datax.service.workflow.api.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务类型
 */
@Getter
@AllArgsConstructor
public enum ActionEnum {

    COMPLETE("complete", "任务完成"),
    CLAIM("claim", "任务签收"),
    UNCLAIM("unclaim", "任务反签收"),
    DELEGATE("delegate", "任务委派"),
    RESOLVE("resolve", "任务归还"),
    ASSIGNEE("assignee", "任务转办");

    private String action;
    private String title;

    public static ActionEnum actionOf(String action) {
        for(ActionEnum actionEnum : values()){
            if(actionEnum.getAction().equals(action)){
                return actionEnum;
            }
        }
        throw new RuntimeException("没有找到对应的枚举");
    }
}
