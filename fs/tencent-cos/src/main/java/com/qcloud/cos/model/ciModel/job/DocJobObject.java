package com.qcloud.cos.model.ciModel.job;

import com.qcloud.cos.model.ciModel.common.MediaInputObject;

/**
 * 媒体处理 动图任务实体 https://cloud.tencent.com/document/product/460/48217
 */
public class DocJobObject {
    /**
     * 任务的输入文件路径	(cos桶相对路径)
     */
    private MediaInputObject input;
    /**
     * 请求类型 文档预览固定使用 DocProcess
     */
    private String tag = "DocProcess";
    /**
     * 任务队列id 可在控制台查看或调用查询队列接口获取
     */
    private String queueId;
    /**
     * 任务规则
     */
    private DocOperationObject operation;

    public MediaInputObject getInput() {
        if (input == null) {
            input = new MediaInputObject();
        }
        return input;
    }

    public void setInput(MediaInputObject input) {
        this.input = input;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getQueueId() {
        return queueId;
    }

    public void setQueueId(String queueId) {
        this.queueId = queueId;
    }

    public DocOperationObject getOperation() {
        if (operation == null) {
            operation = new DocOperationObject();
        }
        return operation;
    }

    public void setOperation(DocOperationObject operation) {
        this.operation = operation;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DocJobObject{");
        sb.append("input=").append(input);
        sb.append(", tag='").append(tag).append('\'');
        sb.append(", queueId='").append(queueId).append('\'');
        sb.append(", operation=").append(operation);
        sb.append('}');
        return sb.toString();
    }
}
