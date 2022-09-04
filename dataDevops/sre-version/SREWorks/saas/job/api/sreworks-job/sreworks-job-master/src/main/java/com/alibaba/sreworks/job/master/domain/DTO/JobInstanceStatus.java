package com.alibaba.sreworks.job.master.domain.DTO;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum JobInstanceStatus {

    INIT,

    RUNNING,

    SUCCESS,

    EXCEPTION,

    STOP;

    public static List<String> nameValues() {
        return Arrays.stream(values()).map(Enum::name).collect(Collectors.toList());
    }

}
