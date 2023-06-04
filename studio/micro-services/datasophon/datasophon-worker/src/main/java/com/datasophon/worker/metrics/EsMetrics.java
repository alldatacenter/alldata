package com.datasophon.worker.metrics;

import cn.hutool.core.io.FileUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.model.ServiceRoleRunner;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.worker.handler.ServiceHandler;

import java.util.ArrayList;

/**
 *
 */
public class EsMetrics implements EsMetricsMXBean {

    private Integer esUp;


    @Override
    public Integer getEsUp() {
        if(FileUtil.exist(Constants.INSTALL_PATH+"/elasticsearch-7.16.2")){
            ServiceHandler serviceHandler = new ServiceHandler();
            ServiceRoleRunner serviceRoleRunner = new ServiceRoleRunner();
            serviceRoleRunner.setTimeout("60");
            ArrayList<String> args = new ArrayList<>();
            args.add("status");
            args.add("elasticsearch");
            serviceRoleRunner.setArgs(args);
            serviceRoleRunner.setProgram("control_es.sh");
            ExecResult status = serviceHandler.status(serviceRoleRunner, "elasticsearch-7.16.2");
            if(status.getExecResult()){
                return 1;
            }
        }
        return 0;
    }

    @Override
    public Integer setEsUp(Integer esUp) {
        return this.esUp = esUp;
    }
}