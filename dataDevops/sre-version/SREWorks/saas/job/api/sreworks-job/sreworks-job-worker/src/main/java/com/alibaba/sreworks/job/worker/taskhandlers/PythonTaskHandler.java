package com.alibaba.sreworks.job.worker.taskhandlers;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class PythonTaskHandler extends AbstractScriptTaskHandler {

    public static String execType = "python";

    @Override
    public String getCmd() {

        return String.format("python3 %s", getScriptPath());

    }

    @Override
    public String getSuffix() {
        return ".py";
    }

}
