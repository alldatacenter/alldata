package com.alibaba.sreworks.job.worker.taskhandlers;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ShellTaskHandler extends AbstractScriptTaskHandler {

    public static String execType = "shell";

    @Override
    public String getCmd() {
        return String.format("bash %s", getScriptPath());
    }

    @Override
    public String getSuffix() {
        return ".sh";
    }

}
