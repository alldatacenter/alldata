package com.alibaba.tesla.server.service;

import java.io.IOException;
import java.util.List;

import com.alibaba.tesla.server.controller.param.ProcessParam;

/**
 * @InterfaceName:ProcessService
 * @Author:dyj
 * @DATE: 2022-02-28
 * @Description:
 **/
public interface ProcessService {
    /**
     * @param param
     * @return
     */
    public boolean process(ProcessParam param);

    /**
     * @param taskUuid
     * @param jobName
     * @param windowList
     * @return
     */
    List<String> start(String taskUuid, String jobName, List<Object> windowList);

    /**
     * @param taskUuid
     */
    void shuffleWindow(String taskUuid);

    /**
     * @return
     */
    String doc() throws IOException;
}
