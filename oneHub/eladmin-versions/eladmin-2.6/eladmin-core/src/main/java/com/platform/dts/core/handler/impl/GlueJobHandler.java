package com.platform.dts.core.handler.impl;

import com.platform.dts.core.biz.model.ReturnT;
import com.platform.dts.core.biz.model.TriggerParam;
import com.platform.dts.core.log.JobLogger;
import com.platform.dts.core.handler.IJobHandler;

/**
 * glue job handler
 * @author AllDataDC 2022/11/19 21:05:45
 */
public class GlueJobHandler extends IJobHandler {

	private long glueUpdatetime;
	private IJobHandler jobHandler;
	public GlueJobHandler(IJobHandler jobHandler, long glueUpdatetime) {
		this.jobHandler = jobHandler;
		this.glueUpdatetime = glueUpdatetime;
	}
	public long getGlueUpdatetime() {
		return glueUpdatetime;
	}

	@Override
	public ReturnT<String> execute(TriggerParam tgParam) throws Exception {
		JobLogger.log("----------- glue.version:"+ glueUpdatetime +" -----------");
		return jobHandler.execute(tgParam);
	}
}
