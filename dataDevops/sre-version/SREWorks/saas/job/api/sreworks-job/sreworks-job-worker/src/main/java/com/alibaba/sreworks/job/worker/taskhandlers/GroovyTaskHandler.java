package com.alibaba.sreworks.job.worker.taskhandlers;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class GroovyTaskHandler extends AbstractScriptTaskHandler {

    public static String execType = "groovy";

    @Override
    public String getCmd() {

        return String.format("groovy %s", getScriptPath());

    }

    @Override
    public String getSuffix() {
        return ".groovy";
    }

}
