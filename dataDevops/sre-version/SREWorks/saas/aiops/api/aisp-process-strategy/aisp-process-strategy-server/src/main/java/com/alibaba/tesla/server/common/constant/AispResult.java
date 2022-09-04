package com.alibaba.tesla.server.common.constant;

import java.io.Serializable;

import lombok.Data;

/**
 * @ClassName: AispResult
 * @Author: dyj
 * @DATE: 2021-11-25
 * @Description:
 **/
@Data
public class AispResult implements Serializable {
    private static final long serialVersionUID = 6271723981181363985L;

    public static String SUCCEED_MESG = "SUCCESS";

    /**
     * 任务UUID
     */
    private String taskUUID;

    /**
     * 返回码
     */
    private String code;

    /**
     * 文本消息描述
     */
    private String message;

    /**
     * 返回的详细数据
     */
    private Object data;

    public AispResult() {
        this.code = String.valueOf(200);
        this.message = SUCCEED_MESG;
        this.data = null;
    }

    public AispResult(Object o) {
        this.code = String.valueOf(200);
        this.message = SUCCEED_MESG;
        this.data = o;
    }

    /**
     * 成功返回
     */
    public AispResult(String taskUUID, Object data) {
        this.taskUUID = taskUUID;
        this.code = String.valueOf(200);
        this.message = SUCCEED_MESG;
        this.data = data;
    }

    /**
     * 自定义失败返回数据
     */
    public AispResult(String code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public AispResult(String taskUUID, String code, String message, Object data) {
        this.taskUUID = taskUUID;
        this.code = code;
        this.message = message;
        this.data = data;
    }
}
