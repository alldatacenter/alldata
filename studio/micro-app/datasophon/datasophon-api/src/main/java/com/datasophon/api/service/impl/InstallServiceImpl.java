package com.datasophon.api.service.impl;

import akka.actor.ActorRef;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datasophon.api.enums.Status;
import com.datasophon.api.master.DispatcherWorkerActor;
import com.datasophon.api.master.HostActor;
import com.datasophon.api.service.ClusterInfoService;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.utils.MessageResolverUtils;
import com.datasophon.api.utils.MinaUtils;
import com.datasophon.common.command.DispatcherHostAgentCommand;
import com.datasophon.common.model.CheckResult;
import com.datasophon.common.model.HostInfo;
import com.datasophon.api.service.ClusterHostService;
import com.datasophon.api.service.InstallService;
import com.datasophon.common.Constants;
import com.datasophon.common.cache.CacheUtils;
import com.datasophon.common.command.HostCheckCommand;
import com.datasophon.common.utils.*;
import com.datasophon.dao.entity.ClusterHostEntity;
import com.datasophon.dao.entity.ClusterInfoEntity;
import com.datasophon.dao.entity.InstallStepEntity;
import com.datasophon.common.enums.InstallState;
import com.datasophon.dao.mapper.InstallStepMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.sshd.client.session.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import scala.collection.immutable.Stream;

import java.util.*;
import java.util.stream.Collectors;


@Service("installService")
public class InstallServiceImpl implements InstallService {

    private static final Logger logger = LoggerFactory.getLogger(InstallServiceImpl.class);

    @Autowired
    InstallStepMapper stepMapper;

    @Autowired
    ClusterInfoService clusterInfoService;

    @Autowired
    ClusterHostService hostService;


    @Override
    public Result getInstallStep(Integer type) {
        List<InstallStepEntity> list = stepMapper.selectList(new QueryWrapper<InstallStepEntity>().eq(Constants.INSTALL_TYPE, type));
        return Result.success(list);
    }

    /**
     * 1、查询缓存是否存在当前主机列表
     * 2、存在则根据分页返回数据
     * 3、不存在则解析hosts，产生主机列表并放入缓存中
     *
     * @param clusterId
     * @param hosts
     * @param sshUser
     * @param sshPort
     * @return
     */
    @Override
    public Result analysisHostList(Integer clusterId, String hosts, String sshUser, Integer sshPort, Integer page, Integer pageSize) {

        List<HostInfo> list = new ArrayList<>();
        hosts = hosts.replace(" ", "");
        String md5 = SecureUtil.md5(hosts);
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        String clusterCode = clusterInfo.getClusterCode();
        HashMap<String, HostInfo> map = new HashMap<>();
        if (CacheUtils.constainsKey(clusterCode + Constants.HOST_MAP) && CacheUtils.constainsKey(clusterCode + Constants.HOST_MD5)
                && md5.equals(CacheUtils.getString(clusterCode + Constants.HOST_MD5))) {
            logger.info("get host list from cache");
            map = (HashMap<String, HostInfo>) CacheUtils.get(clusterCode + Constants.HOST_MAP);

        } else {
            logger.info("analysis host list");
            HostUtils.read();
            String[] hostsArr = hosts.split(",");
            for (String host : hostsArr) {
                //解析ip域
                if (host.contains("[") && host.contains("-")) {
                    int start = host.indexOf("[");
                    String pre = host.substring(0, start);
                    String str = host.substring(start + 1, host.length() - 1);
                    String[] split = str.split("-");
                    if (host.matches(Constants.HAS_EN)) {
                        String preStr = split[0];
                        String endStr = split[1];
                        List<String> newEquipmentNoList = PlaceholderUtils.getNewEquipmentNoList(preStr, endStr);
                        for (String next : newEquipmentNoList) {
                            HostInfo hostInfo = createHostInfo(pre + next, sshPort, sshUser, clusterCode);
                            if (ObjectUtil.isNotNull(hostInfo) ) {
                                map.put(hostInfo.getHostname(), hostInfo);
                                if(!hostInfo.isManaged()){
                                    tellHostCheck( clusterCode, hostInfo);
                                }
                            }
                        }
                    } else {
                        int offset = Integer.parseInt(split[0]);
                        int limit = Integer.parseInt(split[1]);
                        for (int i = offset; i <= limit; i++) {
                            HostInfo hostInfo = createHostInfo(pre + i, sshPort, sshUser, clusterCode);
                            if (ObjectUtil.isNotNull(hostInfo)) {
                                map.put(hostInfo.getHostname(), hostInfo);
                                if(!hostInfo.isManaged()){
                                    tellHostCheck(clusterCode, hostInfo);
                                }
                            }
                        }
                    }
                } else {
                    HostInfo hostInfo = createHostInfo(host, sshPort, sshUser, clusterCode);
                    if (ObjectUtil.isNotNull(hostInfo) ) {
                        map.put(hostInfo.getHostname(), hostInfo);
                        if(!hostInfo.isManaged()){
                            tellHostCheck(clusterCode, hostInfo);
                        }
                    }
                }
            }
            //主机列表放入缓存
            CacheUtils.put(clusterCode + Constants.HOST_MAP, map);
            CacheUtils.put(clusterCode + Constants.HOST_MD5, md5);
            logger.info("put host list in cache");
        }
        //list分页
        list = map.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey()))
                .map(e -> e.getValue()).collect(Collectors.toList());
        Integer offset = (page - 1) * pageSize;
        List<HostInfo> result = getListPage(list, offset, pageSize);
        return Result.success(result).put(Constants.TOTAL, list.size());
    }

    private void tellHostCheck(String clusterCode, HostInfo hostInfo) {
        ActorRef actor = ActorUtils.getLocalActor(HostActor.class,"hostActor-" + hostInfo.getHostname());
        actor.tell(new HostCheckCommand(hostInfo, clusterCode), ActorRef.noSender());
    }

    public HostInfo createHostInfo(String host, Integer sshPort, String sshUser, String clusterCode) {
        HostInfo hostInfo = new HostInfo();
        Map<String, String> ipHost = (Map<String, String>) CacheUtils.get(Constants.IP_HOST);
        Map<String, String> hostIp = (Map<String, String>) CacheUtils.get(Constants.HOST_IP);
        if (host.matches(Constants.HAS_EN)) {
            if (ObjectUtil.isNull(hostIp) || !hostIp.containsKey(host)) {
                return null;
            }
            hostInfo.setHostname(host);
            hostInfo.setIp(hostIp.get(host));
        } else {
            if (ObjectUtil.isNull(ipHost) || !ipHost.containsKey(host)) {
                return null;
            }
            hostInfo.setIp(host);
            hostInfo.setHostname(ipHost.get(host));
        }
        //判断是否受管
        ClusterHostEntity hostEntity = hostService.getClusterHostByHostname(hostInfo.getHostname());
        if (ObjectUtil.isNotNull(hostEntity)) {
            hostInfo.setManaged(true);
            hostInfo.setInstallState(InstallState.SUCCESS);
            hostInfo.setInstallStateCode(InstallState.SUCCESS.getValue());
            hostInfo.setProgress(Constants.ONE_HUNDRRD);
            hostInfo.setCheckResult(new CheckResult(Status.CHECK_HOST_SUCCESS.getCode(),Status.CHECK_HOST_SUCCESS.getMsg()));
        } else {
            hostInfo.setManaged(false);
            hostInfo.setInstallState(InstallState.RUNNING);
            hostInfo.setInstallStateCode(InstallState.RUNNING.getValue());
            hostInfo.setProgress(0);
            hostInfo.setCheckResult(new CheckResult(Status.START_CHECK_HOST.getCode(),Status.START_CHECK_HOST.getMsg()));
        }
        hostInfo.setSshPort(sshPort);
        hostInfo.setSshUser(sshUser);
        hostInfo.setClusterCode(clusterCode);
        hostInfo.setCreateTime(new Date());
        return hostInfo;
    }

    @Override
    public Result getHostCheckStatus(Integer clusterId, String sshUser, Integer sshPort) {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        String clusterCode = clusterInfo.getClusterCode();
        Map<String, HostInfo> map = (Map<String, HostInfo>) CacheUtils.get(clusterCode + Constants.HOST_MAP);
        List<HostInfo> list = map.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey()))
                .map(e -> e.getValue()).collect(Collectors.toList());
        return Result.success(list);
    }

    @Override
    public Result rehostCheck(Integer clusterId, String hostnames, String sshUser, Integer sshPort) {
        //开启主机校验
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        String clusterCode = clusterInfo.getClusterCode();
        Map<String, HostInfo> map = (Map<String, HostInfo>) CacheUtils.get(clusterCode + Constants.HOST_MAP);
        for (String hostname : hostnames.split(",")) {
            if (map.containsKey(hostname)) {
                ActorRef hostActor = ActorUtils.getLocalActor(HostActor.class,"hostActor-" + hostname);
                HostInfo hostInfo = map.get(hostname);
                hostInfo.setCheckResult(new CheckResult(Status.START_CHECK_HOST.getCode(), Status.START_CHECK_HOST.getMsg()));
                hostActor.tell(new HostCheckCommand(hostInfo, clusterCode), ActorRef.noSender());
                
            }
        }
        return Result.success();
    }

    @Override
    public Result dispatcherHostAgentList(Integer clusterId, Integer installStateCode, Integer page, Integer pageSize) {

        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        String clusterCode = clusterInfo.getClusterCode();
        String distributeAgentKey = clusterCode + Constants.UNDERLINE + Constants.START_DISTRIBUTE_AGENT;
        Map<String, HostInfo> map = (Map<String, HostInfo>) CacheUtils.get(clusterCode + Constants.HOST_MAP);
        List<HostInfo> list = map.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey()))
                .map(e -> e.getValue()).filter(e -> e.getCheckResult().getCode() == 10001).collect(Collectors.toList());

        for (HostInfo hostInfo : list) {
            if(hostInfo.isManaged()){
                hostInfo.setInstallStateCode(InstallState.SUCCESS.getValue());
                hostInfo.setProgress(Constants.ONE_HUNDRRD);
                hostInfo.setMessage(MessageResolverUtils.getMessage("distribution.success"));
                hostInfo.setInstallState(InstallState.SUCCESS);
            }else if(!CacheUtils.constainsKey(distributeAgentKey+Constants.UNDERLINE+hostInfo.getHostname())){
                logger.info("start to dispatcher host agent to {}",hostInfo.getHostname());
                ActorRef hostActor = ActorUtils.getLocalActor(DispatcherWorkerActor.class,"dispatcherWorkerActor-" + hostInfo.getHostname());
                hostInfo.setInstallStateCode(InstallState.RUNNING.getValue());
                hostInfo.setCreateTime(new Date());
                hostActor.tell(new DispatcherHostAgentCommand(hostInfo, clusterId, clusterInfo.getClusterFrame()), ActorRef.noSender());
                //保存主机agent分发历史
                CacheUtils.put(distributeAgentKey+Constants.UNDERLINE+hostInfo.getHostname(), true);
                
            }else {
                long timeout = DateUtil.between(hostInfo.getCreateTime(), new Date(), DateUnit.MINUTE);
                long timeOutPeriodOne=PropertyUtils.getLong("timeOutPeriodOne");
                long timeOutPeriodTwo=PropertyUtils.getLong("timeOutPeriodTwo");
                Integer progress=hostInfo.getProgress();
                if("75".equals(String.valueOf(progress))&&timeout>timeOutPeriodOne){
                    hostInfo.setInstallStateCode(InstallState.FAILED.getValue());
                    hostInfo.setProgress(Constants.ONE_HUNDRRD);
                    hostInfo.setMessage(MessageResolverUtils.getMessage("distribution.fail.tips.one"));
                    hostInfo.setInstallState(InstallState.FAILED);
                }
                if (timeout > timeOutPeriodTwo) {
                    hostInfo.setInstallStateCode(InstallState.FAILED.getValue());
                    hostInfo.setProgress(Constants.ONE_HUNDRRD);
                    hostInfo.setInstallState(InstallState.FAILED);
                }
            }
        }
        //list分页
        Integer offset = (page - 1) * pageSize;
        List<HostInfo> result = getListPage(list, offset, pageSize);
        return Result.success(result).put(Constants.TOTAL, list.size());
    }

    @Override
    public Result reStartDispatcherHostAgent(Integer clusterId, String hostnames) {

        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        String clusterCode = clusterInfo.getClusterCode();
        Map<String, HostInfo> map = (Map<String, HostInfo>) CacheUtils.get(clusterCode + Constants.HOST_MAP);

        for (String hostname : hostnames.split(",")) {
            ClusterHostEntity clusterHost = hostService.getClusterHostByHostname(hostname);
            HostInfo hostInfo = new HostInfo();
            if (Objects.nonNull(map) && map.containsKey(hostname)) {
                hostInfo = map.get(hostname);
            }else if (Objects.nonNull(clusterHost)){
                hostInfo.setHostname(hostname);
                hostInfo.setSshUser("root");
                hostInfo.setSshPort(22);
            }
            ActorRef hostActor = ActorUtils.getLocalActor(DispatcherWorkerActor.class,"dispatcherWorkerActor-" + hostname);
            hostInfo.setInstallState(InstallState.RUNNING);
            hostInfo.setErrMsg("");
            hostInfo.setInstallStateCode(InstallState.RUNNING.getValue());
            hostActor.tell(new DispatcherHostAgentCommand(hostInfo, clusterId, clusterInfo.getClusterFrame()), ActorRef.noSender());
            
        }
        return Result.success();
    }

    @Override
    public Result hostCheckCompleted(Integer clusterId) {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        String clusterCode = clusterInfo.getClusterCode();
        Map<String, HostInfo> map = (Map<String, HostInfo>) CacheUtils.get(clusterCode + Constants.HOST_MAP);
        for (Map.Entry<String, HostInfo> hostInfoEntry : map.entrySet()) {
            HostInfo value = hostInfoEntry.getValue();
            if (Objects.isNull(value.getCheckResult()) || (Objects.nonNull(value.getCheckResult()) && value.getCheckResult().getCode() != 10001)) {
                return Result.success().put("hostCheckCompleted", false);
            }
        }
        return Result.success().put("hostCheckCompleted", true);
    }

    @Override
    public Result cancelDispatcherHostAgent(Integer clusterId, String hostname, Integer installStateCode) {

        return null;
    }

    @Override
    public Result dispatcherHostAgentCompleted(Integer clusterId) {
        ClusterInfoEntity clusterInfo = clusterInfoService.getById(clusterId);
        String clusterCode = clusterInfo.getClusterCode();
        Map<String, HostInfo> map = (Map<String, HostInfo>) CacheUtils.get(clusterCode + Constants.HOST_MAP);
        for (Map.Entry<String, HostInfo> hostInfoEntry : map.entrySet()) {
            HostInfo hostInfo = hostInfoEntry.getValue();
            if(hostInfo.getProgress() == 75 && DateUtil.between(hostInfo.getCreateTime(),new Date(), DateUnit.MINUTE) > 1){
                logger.info("dispatcher host agent timeout");
                hostInfo.setInstallState(InstallState.FAILED);
                hostInfo.setInstallStateCode(InstallState.FAILED.getValue());
                hostInfo.setErrMsg("dispatcher host agent timeout");
            }
            if (hostInfo.getInstallState() != InstallState.SUCCESS) {
                return Result.success().put("dispatcherHostAgentCompleted", false);
            }
        }
        return Result.success().put("dispatcherHostAgentCompleted", true);
    }


    @Override
    public Result generateHostAgentCommand(String clusterHostIds, String commandType) throws Exception {
        if(StringUtils.isBlank(clusterHostIds)){
            return Result.error(Status.SELECT_LEAST_ONE_HOST.getMsg());
        }
        String[] clusterHostIdArray = clusterHostIds.split(Constants.COMMA);
        List<String> clusterHostIdList = Arrays.asList(clusterHostIdArray);
        List<ClusterHostEntity> clusterHostList = hostService.getHostListByIds(clusterHostIdList);
        for (ClusterHostEntity clusterHostEntity : clusterHostList) {
            ClientSession session = MinaUtils.openConnection(
                    clusterHostEntity.getHostname(),
              22,
                     Constants.ROOT,
                    Constants.SLASH + Constants.ROOT + Constants.ID_RSA);
            MinaUtils.execCmdWithResult( session,"service datasophon-worker "+commandType);
            logger.info("hostAgent command:{}", "service datasophon-worker "+commandType);
            if (ObjectUtil.isNotEmpty(session)) {
                session.close();
            }
        }
        return Result.success();
    }

    private List<HostInfo> getListPage(List<HostInfo> list, Integer offset, Integer pageSize) {
        List<HostInfo> result = new ArrayList<>();
        Integer limit = offset + pageSize;
        if (list.size() < offset + pageSize) {
            limit = list.size();
        }
        for (int i = offset; i < limit; i++) {
            result.add(list.get(i));
        }
        return result;
    }


}
