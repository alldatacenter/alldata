package com.alibaba.sreworks.job.master.params;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Data
@Slf4j
public class ListenWorkerParam {

    private String address;

    private List<String> execTypeList;

}
