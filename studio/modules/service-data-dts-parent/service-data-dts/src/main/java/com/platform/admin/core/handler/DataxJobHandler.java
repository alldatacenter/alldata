package com.platform.admin.core.handler;

import com.platform.admin.core.thread.JobTriggerPoolHelper;
import com.platform.core.biz.model.ReturnT;
import com.platform.core.biz.model.TriggerParam;
import com.platform.core.handler.IJobHandler;
import com.platform.core.handler.annotation.JobHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @ClassName： DataxJobHandler
 * @Description: executorJobHandler
 * @author：AllDataDC
 */
@Slf4j
@Component
@JobHandler("executorJobHandler")
public class DataxJobHandler extends IJobHandler {

    @Override
    public ReturnT<String> execute(TriggerParam tgParam) throws Exception {
        log.info("---------Datax定时任务开始执行--------");
        //数据抽取具体的执行方法
        JobTriggerPoolHelper.runJob(tgParam.getJobId());
        System.out.println("---------Datax定时任务执行成功--------");
        return ReturnT.SUCCESS;
    }
}