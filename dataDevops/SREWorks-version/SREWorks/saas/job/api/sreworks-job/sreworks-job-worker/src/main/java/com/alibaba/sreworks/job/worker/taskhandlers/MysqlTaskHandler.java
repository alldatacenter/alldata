package com.alibaba.sreworks.job.worker.taskhandlers;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.job.taskhandler.MysqlContent;
import com.alibaba.sreworks.job.taskhandler.MysqlContentAction;
import com.alibaba.sreworks.job.utils.BeansUtil;
import com.alibaba.sreworks.job.utils.JsonUtil;
import com.alibaba.sreworks.job.worker.services.ElasticTaskInstanceService;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class MysqlTaskHandler extends AbstractTaskHandler {

    public static String execType = "mysql";

    @Override
    public void execute() throws Exception {

        MysqlContent mysqlContent = JSONObject.parseObject(getTaskInstance().getExecContent(), MysqlContent.class);
        List<Map<String, Object>> ret = MysqlContentAction.run(mysqlContent);
        BeansUtil.context.getBean(ElasticTaskInstanceService.class)
            .update(getTaskInstance().getId(), JsonUtil.map("stdout", JSONObject.toJSONString(ret)));

    }

}
