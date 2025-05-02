package com.platform.admin.core.kill;

import com.platform.admin.core.thread.JobTriggerPoolHelper;
import com.platform.core.biz.model.ReturnT;
import com.platform.core.biz.model.TriggerParam;
import com.platform.core.enums.ExecutorBlockStrategyEnum;
import com.platform.core.glue.GlueTypeEnum;
import com.platform.admin.core.trigger.JobTrigger;
import com.platform.core.util.Constants;
import com.platform.core.util.ProcessUtil;

import java.util.Date;

/**
 * flinkx-job trigger
 */
public class KillJob {

    /**
     * @param logId
     * @param address
     * @param processId
     */
	public static ReturnT<String> trigger(String processId) {
		ReturnT<String> triggerResult = null;
		try {
			//将作业杀掉
			String cmdstr="";
			if(JobTriggerPoolHelper.isWindows()){
				cmdstr= Constants.CMDWINDOWTASKKILL+processId;
			}else {
				cmdstr=Constants.CMDLINUXTASKKILL+processId;
			}
			final Process process = Runtime.getRuntime().exec(cmdstr);
			String prcsId = ProcessUtil.getProcessId(process);
			triggerResult = new ReturnT<>(ReturnT.SUCCESS_CODE, "成功停止作业 !!!");
		}catch (Exception e) {
			triggerResult = new ReturnT<>(ReturnT.FAIL_CODE, null);
		}
		return triggerResult;
	}

}
