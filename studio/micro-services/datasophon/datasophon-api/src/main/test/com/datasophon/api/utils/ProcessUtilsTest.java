package com.datasophon.api.utils;

import akka.actor.ActorRef;
import com.datasophon.api.service.ClusterHostService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.common.command.ExecuteServiceRoleCommand;
import com.datasophon.common.enums.CommandType;
import com.datasophon.common.enums.ServiceExecuteState;
import com.datasophon.common.enums.ServiceRoleType;
import com.datasophon.common.model.*;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.*;
import com.datasophon.dao.enums.*;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class ProcessUtilsTest {

    @Test
    public void testSaveServiceInstallInfo() {
        // Setup
        final ServiceRoleInfo serviceRoleInfo = new ServiceRoleInfo();
        serviceRoleInfo.setId(0);
        serviceRoleInfo.setName("name");
        serviceRoleInfo.setRoleType(ServiceRoleType.MASTER);
        serviceRoleInfo.setCardinality("cardinality");
        serviceRoleInfo.setSortNum(0);
        final ServiceRoleRunner startRunner = new ServiceRoleRunner();
        startRunner.setTimeout("timeout");
        startRunner.setProgram("program");
        startRunner.setArgs(Arrays.asList("value"));
        serviceRoleInfo.setStartRunner(startRunner);
        final ExternalLink externalLink = new ExternalLink();
        externalLink.setName("name");
        externalLink.setUrl("url");
        serviceRoleInfo.setExternalLink(externalLink);
        serviceRoleInfo.setHostname("hostname");
        serviceRoleInfo.setClusterId(0);
        serviceRoleInfo.setParentName("parentName");

        // Run the test
        ProcessUtils.saveServiceInstallInfo(serviceRoleInfo);

        // Verify the results
    }

    @Test
    public void testSaveHostInstallInfo() {
        // Setup
        final StartWorkerMessage message = new StartWorkerMessage();
        message.setCoreNum(0);
        message.setTotalMem(0.0);
        message.setTotalDisk(0.0);
        message.setUsedDisk(0.0);
        message.setDiskAvail(0.0);
        message.setHostname("hostname");
        message.setMemUsedPersent(0.0);
        message.setDiskUsedPersent(0.0);
        message.setAverageLoad(0.0);
        message.setClusterId(0);
        message.setIp("ip");
        message.setCpuArchitecture("cpuArchitecture");
        message.setDeliveryId(0L);

        final ClusterHostService clusterHostService = null;

        // Run the test
        ProcessUtils.saveHostInstallInfo(message, "clusterCode", clusterHostService);

        // Verify the results
    }

    @Test
    public void testUpdateCommandStateToFailed() {
        // Setup
        // Run the test
//        ProcessUtils.updateCommandStateToFailed("hostCommandId");

        // Verify the results
    }

    @Test
    public void testTellCommandActorResult() {
        // Setup
        final ServiceRoleInfo serviceRoleInfo = new ServiceRoleInfo();
        serviceRoleInfo.setId(0);
        serviceRoleInfo.setName("name");
        serviceRoleInfo.setRoleType(ServiceRoleType.MASTER);
        serviceRoleInfo.setCardinality("cardinality");
        serviceRoleInfo.setSortNum(0);
        final ServiceRoleRunner startRunner = new ServiceRoleRunner();
        startRunner.setTimeout("timeout");
        startRunner.setProgram("program");
        startRunner.setArgs(Arrays.asList("value"));
        serviceRoleInfo.setStartRunner(startRunner);
        final ExternalLink externalLink = new ExternalLink();
        externalLink.setName("name");
        externalLink.setUrl("url");
        serviceRoleInfo.setExternalLink(externalLink);
        serviceRoleInfo.setHostname("hostname");
        serviceRoleInfo.setClusterId(0);
        serviceRoleInfo.setParentName("parentName");
        final ExecuteServiceRoleCommand executeServiceRoleCommand = new ExecuteServiceRoleCommand(0, "node",
                Arrays.asList(serviceRoleInfo));

        // Run the test
        ProcessUtils.tellCommandActorResult("serviceName", executeServiceRoleCommand, ServiceExecuteState.RUNNING);

        // Verify the results
    }

    @Test
    public void testHandleCommandResult() {
        // Setup
        final ClusterServiceCommandHostCommandEntity expectedResult = new ClusterServiceCommandHostCommandEntity();
        expectedResult.setHostCommandId("hostCommandId");
        expectedResult.setCommandName("commandName");
        expectedResult.setCommandState(CommandState.WAIT);
        expectedResult.setCommandStateCode(0);
        expectedResult.setCommandProgress(0);
        expectedResult.setCommandHostId("commandHostId");
        expectedResult.setCommandId("commandId");
        expectedResult.setHostname("hostname");
        expectedResult.setServiceRoleName("serviceRoleName");
        expectedResult.setServiceRoleType(RoleType.MASTER);
        expectedResult.setResultMsg("execOut");
        expectedResult.setCreateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        expectedResult.setCommandType(0);

        // Run the test
        final ClusterServiceCommandHostCommandEntity result = ProcessUtils.handleCommandResult("hostCommandId", false,
                "execOut");

        // Verify the results
        assertEquals(expectedResult, result);
    }

    @Test
    public void testBuildExecuteServiceRoleCommand() {
        // Setup
        final DAGGraph<String, ServiceNode, String> dag = new DAGGraph<>();
        dag.setNodesMap(new HashMap<>());
        dag.setEdgesMap(new HashMap<>());
        dag.setReverseEdgesMap(new HashMap<>());

        final Map<String, ServiceExecuteState> activeTaskList = new HashMap<>();
        final Map<String, String> errorTaskList = new HashMap<>();
        final Map<String, String> readyToSubmitTaskList = new HashMap<>();
        final Map<String, String> completeTaskList = new HashMap<>();
        final ServiceRoleInfo serviceRoleInfo = new ServiceRoleInfo();
        serviceRoleInfo.setId(0);
        serviceRoleInfo.setName("name");
        serviceRoleInfo.setRoleType(ServiceRoleType.MASTER);
        serviceRoleInfo.setCardinality("cardinality");
        serviceRoleInfo.setSortNum(0);
        final ServiceRoleRunner startRunner = new ServiceRoleRunner();
        startRunner.setTimeout("timeout");
        startRunner.setProgram("program");
        startRunner.setArgs(Arrays.asList("value"));
        serviceRoleInfo.setStartRunner(startRunner);
        final ExternalLink externalLink = new ExternalLink();
        externalLink.setName("name");
        externalLink.setUrl("url");
        serviceRoleInfo.setExternalLink(externalLink);
        serviceRoleInfo.setHostname("hostname");
        serviceRoleInfo.setClusterId(0);
        serviceRoleInfo.setParentName("parentName");
        final List<ServiceRoleInfo> masterRoles = Arrays.asList(serviceRoleInfo);
        final ServiceRoleInfo workerRole = new ServiceRoleInfo();
        workerRole.setId(0);
        workerRole.setName("name");
        workerRole.setRoleType(ServiceRoleType.MASTER);
        workerRole.setCardinality("cardinality");
        workerRole.setSortNum(0);
        final ServiceRoleRunner startRunner1 = new ServiceRoleRunner();
        startRunner1.setTimeout("timeout");
        startRunner1.setProgram("program");
        startRunner1.setArgs(Arrays.asList("value"));
        workerRole.setStartRunner(startRunner1);
        final ExternalLink externalLink1 = new ExternalLink();
        externalLink1.setName("name");
        externalLink1.setUrl("url");
        workerRole.setExternalLink(externalLink1);
        workerRole.setHostname("hostname");
        workerRole.setClusterId(0);
        workerRole.setParentName("parentName");

        final ActorRef serviceActor = ActorRef.noSender();

        // Run the test
        ProcessUtils.buildExecuteServiceRoleCommand(0, CommandType.INSTALL_SERVICE, "clusterCode", dag, activeTaskList,
                errorTaskList, readyToSubmitTaskList, completeTaskList, "node", masterRoles, workerRole, serviceActor,
                ServiceRoleType.MASTER);

        // Verify the results
    }

    @Test
    public void testGenerateCommandEntity() {
        // Setup
        final ClusterServiceCommandEntity expectedResult = new ClusterServiceCommandEntity();
        expectedResult.setCommandId("commandId");
        expectedResult.setCreateBy("admin");
        expectedResult.setCreateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        expectedResult.setCommandName("commandName");
        expectedResult.setCommandState(CommandState.WAIT);
        expectedResult.setCommandStateCode(0);
        expectedResult.setCommandProgress(0);
        expectedResult.setClusterId(0);
        expectedResult.setServiceName("serviceName");
        expectedResult.setCommandType(0);
        expectedResult.setDurationTime("durationTime");
        expectedResult.setEndTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        expectedResult.setServiceInstanceId(0);

        // Run the test
        final ClusterServiceCommandEntity result = ProcessUtils.generateCommandEntity(0, CommandType.INSTALL_SERVICE,
                "serviceName");

        // Verify the results
        assertEquals(expectedResult, result);
    }

    @Test
    public void testGenerateCommandHostEntity() {
        // Setup
        final ClusterServiceCommandHostEntity expectedResult = new ClusterServiceCommandHostEntity();
        expectedResult.setCommandHostId("commandHostId");
        expectedResult.setHostname("hostname");
        expectedResult.setCommandState(CommandState.WAIT);
        expectedResult.setCommandStateCode(0);
        expectedResult.setCommandProgress(0);
        expectedResult.setCommandId("commandId");
        expectedResult.setCreateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());

        // Run the test
        final ClusterServiceCommandHostEntity result = ProcessUtils.generateCommandHostEntity("commandId", "hostname");

        // Verify the results
        assertEquals(expectedResult, result);
    }

    @Test
    public void testGenerateCommandHostCommandEntity() {
        // Setup
        final ClusterServiceCommandHostEntity commandHost = new ClusterServiceCommandHostEntity();
        commandHost.setCommandHostId("commandHostId");
        commandHost.setHostname("hostname");
        commandHost.setCommandState(CommandState.WAIT);
        commandHost.setCommandStateCode(0);
        commandHost.setCommandProgress(0);
        commandHost.setCommandId("commandId");
        commandHost.setCreateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());

        final ClusterServiceCommandHostCommandEntity expectedResult = new ClusterServiceCommandHostCommandEntity();
        expectedResult.setHostCommandId("hostCommandId");
        expectedResult.setCommandName("commandName");
        expectedResult.setCommandState(CommandState.WAIT);
        expectedResult.setCommandStateCode(0);
        expectedResult.setCommandProgress(0);
        expectedResult.setCommandHostId("commandHostId");
        expectedResult.setCommandId("commandId");
        expectedResult.setHostname("hostname");
        expectedResult.setServiceRoleName("serviceRoleName");
        expectedResult.setServiceRoleType(RoleType.MASTER);
        expectedResult.setResultMsg("execOut");
        expectedResult.setCreateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        expectedResult.setCommandType(0);

        // Run the test
        final ClusterServiceCommandHostCommandEntity result = ProcessUtils.generateCommandHostCommandEntity(
                CommandType.INSTALL_SERVICE, "commandId", "serviceRoleName", RoleType.MASTER, commandHost);

        // Verify the results
        assertEquals(expectedResult, result);
    }

    @Test
    public void testUpdateServiceRoleState() {
        // Setup
        // Run the test
        ProcessUtils.updateServiceRoleState(CommandType.INSTALL_SERVICE, "serviceRoleName", "hostname", 0,
                ServiceRoleState.RUNNING);

        // Verify the results
    }

    @Test
    public void testGenerateClusterVariable() {
        // Setup
        final Map<String, String> globalVariables = new HashMap<>();

        // Run the test
        ProcessUtils.generateClusterVariable(globalVariables, 0, "variableName", "value");

        // Verify the results
    }

    @Test
    public void testHdfsECMethond() throws Exception {
        // Setup
        final ClusterServiceRoleInstanceService roleInstanceService = null;
        final TreeSet<String> list = new TreeSet<>(Arrays.asList("value"));

        // Run the test
//        ProcessUtils.hdfsECMethond(0, roleInstanceService, list, "type", "roleName");

        // Verify the results
    }

    @Test(expected = Exception.class)
    public void testHdfsECMethond_ThrowsException() throws Exception {
        // Setup
        final ClusterServiceRoleInstanceService roleInstanceService = null;
        final TreeSet<String> list = new TreeSet<>(Arrays.asList("value"));

        // Run the test
//        ProcessUtils.hdfsECMethond(0, roleInstanceService, list, "type", "roleName");
    }

    @Test
    public void testCreateServiceActor() {
        // Setup
        final ClusterInfoEntity clusterInfo = new ClusterInfoEntity();
        clusterInfo.setId(0);
        clusterInfo.setCreateBy("createBy");
        clusterInfo.setCreateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterInfo.setClusterName("clusterName");
        clusterInfo.setClusterCode("clusterCode");
        clusterInfo.setClusterFrame("clusterFrame");
        clusterInfo.setFrameVersion("frameVersion");
        clusterInfo.setClusterState(ClusterState.RUNNING);
        clusterInfo.setFrameId(0);
        final UserInfoEntity userInfoEntity = new UserInfoEntity();
        userInfoEntity.setId(0);
        userInfoEntity.setUsername("username");
        userInfoEntity.setPassword("password");
        userInfoEntity.setEmail("email");
        userInfoEntity.setPhone("phone");
        clusterInfo.setClusterManagerList(Arrays.asList(userInfoEntity));

        // Run the test
        ProcessUtils.createServiceActor(clusterInfo);

        // Verify the results
    }

    @Test
    public void testGetExceptionMessage() {
        assertEquals("result", ProcessUtils.getExceptionMessage(new Exception("message")));
    }

    @Test
    public void testRestartService() throws Exception {
        // Setup
        final ServiceRoleInfo serviceRoleInfo = new ServiceRoleInfo();
        serviceRoleInfo.setId(0);
        serviceRoleInfo.setName("name");
        serviceRoleInfo.setRoleType(ServiceRoleType.MASTER);
        serviceRoleInfo.setCardinality("cardinality");
        serviceRoleInfo.setSortNum(0);
        final ServiceRoleRunner startRunner = new ServiceRoleRunner();
        startRunner.setTimeout("timeout");
        startRunner.setProgram("program");
        startRunner.setArgs(Arrays.asList("value"));
        serviceRoleInfo.setStartRunner(startRunner);
        final ExternalLink externalLink = new ExternalLink();
        externalLink.setName("name");
        externalLink.setUrl("url");
        serviceRoleInfo.setExternalLink(externalLink);
        serviceRoleInfo.setHostname("hostname");
        serviceRoleInfo.setClusterId(0);
        serviceRoleInfo.setParentName("parentName");

        // Run the test
        final ExecResult result = ProcessUtils.restartService(serviceRoleInfo, false);

        // Verify the results
    }

    @Test(expected = Exception.class)
    public void testRestartService_ThrowsException() throws Exception {
        // Setup
        final ServiceRoleInfo serviceRoleInfo = new ServiceRoleInfo();
        serviceRoleInfo.setId(0);
        serviceRoleInfo.setName("name");
        serviceRoleInfo.setRoleType(ServiceRoleType.MASTER);
        serviceRoleInfo.setCardinality("cardinality");
        serviceRoleInfo.setSortNum(0);
        final ServiceRoleRunner startRunner = new ServiceRoleRunner();
        startRunner.setTimeout("timeout");
        startRunner.setProgram("program");
        startRunner.setArgs(Arrays.asList("value"));
        serviceRoleInfo.setStartRunner(startRunner);
        final ExternalLink externalLink = new ExternalLink();
        externalLink.setName("name");
        externalLink.setUrl("url");
        serviceRoleInfo.setExternalLink(externalLink);
        serviceRoleInfo.setHostname("hostname");
        serviceRoleInfo.setClusterId(0);
        serviceRoleInfo.setParentName("parentName");

        // Run the test
        ProcessUtils.restartService(serviceRoleInfo, false);
    }

    @Test
    public void testStartService() throws Exception {
        // Setup
        final ServiceRoleInfo serviceRoleInfo = new ServiceRoleInfo();
        serviceRoleInfo.setId(0);
        serviceRoleInfo.setName("name");
        serviceRoleInfo.setRoleType(ServiceRoleType.MASTER);
        serviceRoleInfo.setCardinality("cardinality");
        serviceRoleInfo.setSortNum(0);
        final ServiceRoleRunner startRunner = new ServiceRoleRunner();
        startRunner.setTimeout("timeout");
        startRunner.setProgram("program");
        startRunner.setArgs(Arrays.asList("value"));
        serviceRoleInfo.setStartRunner(startRunner);
        final ExternalLink externalLink = new ExternalLink();
        externalLink.setName("name");
        externalLink.setUrl("url");
        serviceRoleInfo.setExternalLink(externalLink);
        serviceRoleInfo.setHostname("hostname");
        serviceRoleInfo.setClusterId(0);
        serviceRoleInfo.setParentName("parentName");

        // Run the test
        final ExecResult result = ProcessUtils.startService(serviceRoleInfo, false);

        // Verify the results
    }

    @Test(expected = Exception.class)
    public void testStartService_ThrowsException() throws Exception {
        // Setup
        final ServiceRoleInfo serviceRoleInfo = new ServiceRoleInfo();
        serviceRoleInfo.setId(0);
        serviceRoleInfo.setName("name");
        serviceRoleInfo.setRoleType(ServiceRoleType.MASTER);
        serviceRoleInfo.setCardinality("cardinality");
        serviceRoleInfo.setSortNum(0);
        final ServiceRoleRunner startRunner = new ServiceRoleRunner();
        startRunner.setTimeout("timeout");
        startRunner.setProgram("program");
        startRunner.setArgs(Arrays.asList("value"));
        serviceRoleInfo.setStartRunner(startRunner);
        final ExternalLink externalLink = new ExternalLink();
        externalLink.setName("name");
        externalLink.setUrl("url");
        serviceRoleInfo.setExternalLink(externalLink);
        serviceRoleInfo.setHostname("hostname");
        serviceRoleInfo.setClusterId(0);
        serviceRoleInfo.setParentName("parentName");

        // Run the test
        ProcessUtils.startService(serviceRoleInfo, false);
    }

    @Test
    public void testStartInstallService() throws Exception {
        // Setup
        final ServiceRoleInfo serviceRoleInfo = new ServiceRoleInfo();
        serviceRoleInfo.setId(0);
        serviceRoleInfo.setName("name");
        serviceRoleInfo.setRoleType(ServiceRoleType.MASTER);
        serviceRoleInfo.setCardinality("cardinality");
        serviceRoleInfo.setSortNum(0);
        final ServiceRoleRunner startRunner = new ServiceRoleRunner();
        startRunner.setTimeout("timeout");
        startRunner.setProgram("program");
        startRunner.setArgs(Arrays.asList("value"));
        serviceRoleInfo.setStartRunner(startRunner);
        final ExternalLink externalLink = new ExternalLink();
        externalLink.setName("name");
        externalLink.setUrl("url");
        serviceRoleInfo.setExternalLink(externalLink);
        serviceRoleInfo.setHostname("hostname");
        serviceRoleInfo.setClusterId(0);
        serviceRoleInfo.setParentName("parentName");

        // Run the test
        final ExecResult result = ProcessUtils.startInstallService(serviceRoleInfo);

        // Verify the results
    }

    @Test(expected = Exception.class)
    public void testStartInstallService_ThrowsException() throws Exception {
        // Setup
        final ServiceRoleInfo serviceRoleInfo = new ServiceRoleInfo();
        serviceRoleInfo.setId(0);
        serviceRoleInfo.setName("name");
        serviceRoleInfo.setRoleType(ServiceRoleType.MASTER);
        serviceRoleInfo.setCardinality("cardinality");
        serviceRoleInfo.setSortNum(0);
        final ServiceRoleRunner startRunner = new ServiceRoleRunner();
        startRunner.setTimeout("timeout");
        startRunner.setProgram("program");
        startRunner.setArgs(Arrays.asList("value"));
        serviceRoleInfo.setStartRunner(startRunner);
        final ExternalLink externalLink = new ExternalLink();
        externalLink.setName("name");
        externalLink.setUrl("url");
        serviceRoleInfo.setExternalLink(externalLink);
        serviceRoleInfo.setHostname("hostname");
        serviceRoleInfo.setClusterId(0);
        serviceRoleInfo.setParentName("parentName");

        // Run the test
        ProcessUtils.startInstallService(serviceRoleInfo);
    }

    @Test
    public void testGenerateConfigFileMap() {
        // Setup
        final HashMap<Generators, List<ServiceConfig>> configFileMap = new HashMap<>();
        final ClusterServiceRoleGroupConfig config = new ClusterServiceRoleGroupConfig();
        config.setId(0);
        config.setRoleGroupId(0);
        config.setConfigJson("configJson");
        config.setConfigJsonMd5("configJsonMd5");
        config.setConfigVersion(0);
        config.setConfigFileJson("configFileJson");
        config.setConfigFileJsonMd5("configFileJsonMd5");
        config.setClusterId(0);
        config.setCreateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        config.setUpdateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        config.setServiceName("serviceName");

        // Run the test
        ProcessUtils.generateConfigFileMap(configFileMap, config);

        // Verify the results
    }

    @Test
    public void testCreateServiceConfig() {
        // Setup
        final ServiceConfig expectedResult = new ServiceConfig();
        expectedResult.setName("configName");
        expectedResult.setValue("configValue");
        expectedResult.setLabel("label");
        expectedResult.setDescription("description");
        expectedResult.setRequired(false);
        expectedResult.setType("input");
        expectedResult.setConfigurableInWizard(false);
        expectedResult.setDefaultValue("defaultValue");
        expectedResult.setMinValue(0);
        expectedResult.setMaxValue(0);
        expectedResult.setUnit("unit");
        expectedResult.setHidden(false);
        expectedResult.setSelectValue(Arrays.asList("value"));
        expectedResult.setConfigType("configType");

        // Run the test
        final ServiceConfig result = ProcessUtils.createServiceConfig("configName", "configValue", "type");

        // Verify the results
        assertEquals(expectedResult, result);
    }

    @Test
    public void testGetClusterInfo() {
        // Setup
        final ClusterInfoEntity expectedResult = new ClusterInfoEntity();
        expectedResult.setId(0);
        expectedResult.setCreateBy("createBy");
        expectedResult.setCreateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        expectedResult.setClusterName("clusterName");
        expectedResult.setClusterCode("clusterCode");
        expectedResult.setClusterFrame("clusterFrame");
        expectedResult.setFrameVersion("frameVersion");
        expectedResult.setClusterState(ClusterState.RUNNING);
        expectedResult.setFrameId(0);
        final UserInfoEntity userInfoEntity = new UserInfoEntity();
        userInfoEntity.setId(0);
        userInfoEntity.setUsername("username");
        userInfoEntity.setPassword("password");
        userInfoEntity.setEmail("email");
        userInfoEntity.setPhone("phone");
        expectedResult.setClusterManagerList(Arrays.asList(userInfoEntity));

        // Run the test
        final ClusterInfoEntity result = ProcessUtils.getClusterInfo(0);

        // Verify the results
        assertEquals(expectedResult, result);
    }

    @Test
    public void testAddAll() {
        // Setup
        final ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setName("configName");
        serviceConfig.setValue("configValue");
        serviceConfig.setLabel("label");
        serviceConfig.setDescription("description");
        serviceConfig.setRequired(false);
        serviceConfig.setType("input");
        serviceConfig.setConfigurableInWizard(false);
        serviceConfig.setDefaultValue("defaultValue");
        serviceConfig.setMinValue(0);
        serviceConfig.setMaxValue(0);
        serviceConfig.setUnit("unit");
        serviceConfig.setHidden(false);
        serviceConfig.setSelectValue(Arrays.asList("value"));
        serviceConfig.setConfigType("configType");
        final List<ServiceConfig> left = Arrays.asList(serviceConfig);
        final ServiceConfig serviceConfig1 = new ServiceConfig();
        serviceConfig1.setName("configName");
        serviceConfig1.setValue("configValue");
        serviceConfig1.setLabel("label");
        serviceConfig1.setDescription("description");
        serviceConfig1.setRequired(false);
        serviceConfig1.setType("input");
        serviceConfig1.setConfigurableInWizard(false);
        serviceConfig1.setDefaultValue("defaultValue");
        serviceConfig1.setMinValue(0);
        serviceConfig1.setMaxValue(0);
        serviceConfig1.setUnit("unit");
        serviceConfig1.setHidden(false);
        serviceConfig1.setSelectValue(Arrays.asList("value"));
        serviceConfig1.setConfigType("configType");
        final List<ServiceConfig> right = Arrays.asList(serviceConfig1);
        final ServiceConfig serviceConfig2 = new ServiceConfig();
        serviceConfig2.setName("configName");
        serviceConfig2.setValue("configValue");
        serviceConfig2.setLabel("label");
        serviceConfig2.setDescription("description");
        serviceConfig2.setRequired(false);
        serviceConfig2.setType("input");
        serviceConfig2.setConfigurableInWizard(false);
        serviceConfig2.setDefaultValue("defaultValue");
        serviceConfig2.setMinValue(0);
        serviceConfig2.setMaxValue(0);
        serviceConfig2.setUnit("unit");
        serviceConfig2.setHidden(false);
        serviceConfig2.setSelectValue(Arrays.asList("value"));
        serviceConfig2.setConfigType("configType");
        final List<ServiceConfig> expectedResult = Arrays.asList(serviceConfig2);

        // Run the test
        final List<ServiceConfig> result = ProcessUtils.addAll(left, right);

        // Verify the results
        assertEquals(expectedResult, result);
    }

    @Test
    public void testSyncUserGroupToHosts() {
        // Setup
        final ClusterHostEntity clusterHostEntity = new ClusterHostEntity();
        clusterHostEntity.setId(0);
        clusterHostEntity.setCreateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterHostEntity.setHostname("hostname");
        clusterHostEntity.setIp("ip");
        clusterHostEntity.setRack("default");
        clusterHostEntity.setCoreNum(0);
        clusterHostEntity.setTotalMem(0);
        clusterHostEntity.setTotalDisk(0);
        clusterHostEntity.setUsedMem(0);
        clusterHostEntity.setUsedDisk(0);
        clusterHostEntity.setAverageLoad("averageLoad");
        clusterHostEntity.setCheckTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterHostEntity.setClusterId(0);
        clusterHostEntity.setHostState(0);
        clusterHostEntity.setManaged(MANAGED.YES);
        final List<ClusterHostEntity> hostList = Arrays.asList(clusterHostEntity);

        // Run the test
        ProcessUtils.syncUserGroupToHosts(hostList, "groupName", "operate");

        // Verify the results
    }

    @Test
    public void testSyncUserToHosts() {
        // Setup
        final ClusterHostEntity clusterHostEntity = new ClusterHostEntity();
        clusterHostEntity.setId(0);
        clusterHostEntity.setCreateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterHostEntity.setHostname("hostname");
        clusterHostEntity.setIp("ip");
        clusterHostEntity.setRack("default");
        clusterHostEntity.setCoreNum(0);
        clusterHostEntity.setTotalMem(0);
        clusterHostEntity.setTotalDisk(0);
        clusterHostEntity.setUsedMem(0);
        clusterHostEntity.setUsedDisk(0);
        clusterHostEntity.setAverageLoad("averageLoad");
        clusterHostEntity.setCheckTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterHostEntity.setClusterId(0);
        clusterHostEntity.setHostState(0);
        clusterHostEntity.setManaged(MANAGED.YES);
        final List<ClusterHostEntity> hostList = Arrays.asList(clusterHostEntity);

        // Run the test
        ProcessUtils.syncUserToHosts(hostList, "username", "mainGroup", "otherGroup", "operate");

        // Verify the results
    }
}
