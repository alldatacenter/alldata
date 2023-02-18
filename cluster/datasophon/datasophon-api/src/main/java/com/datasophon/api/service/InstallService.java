package com.datasophon.api.service;

import com.datasophon.common.utils.Result;

public interface InstallService {
    Result getInstallStep(Integer type);

    Result analysisHostList(Integer clusterId, String hosts, String sshUser, Integer sshPort, Integer page, Integer pageSize);

    Result getHostCheckStatus(Integer clusterId, String sshUser, Integer sshPort);

    Result rehostCheck(Integer clusterId, String hostnames, String sshUser, Integer sshPort);

    Result dispatcherHostAgentList(Integer id, Integer installStateCode,Integer page, Integer clusterId);

    Result reStartDispatcherHostAgent(Integer clusterId, String hostnames);

    Result hostCheckCompleted(Integer clusterId);

    Result cancelDispatcherHostAgent(Integer clusterId, String hostname, Integer installStateCode);

    Result dispatcherHostAgentCompleted(Integer clusterId);

    Result generateHostAgentCommand(String clusterHostIds,String commandType) throws Exception;
}
