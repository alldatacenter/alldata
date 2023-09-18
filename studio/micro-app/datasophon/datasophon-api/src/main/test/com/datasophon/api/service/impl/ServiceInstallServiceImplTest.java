package com.datasophon.api.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.datasophon.api.service.*;
import com.datasophon.common.model.HostServiceRoleMapping;
import com.datasophon.common.model.ServiceConfig;
import com.datasophon.common.model.ServiceRoleHostMapping;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.*;
import com.datasophon.dao.enums.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ServiceInstallServiceImplTest {

    @Mock
    private ClusterInfoService mockClusterInfoService;
    @Mock
    private FrameInfoService mockFrameInfoService;
    @Mock
    private FrameServiceService mockFrameService;
    @Mock
    private ClusterServiceCommandService mockCommandService;
    @Mock
    private ClusterServiceInstanceService mockServiceInstanceService;
    @Mock
    private ClusterServiceInstanceConfigService mockServiceInstanceConfigService;
    @Mock
    private ClusterServiceCommandHostCommandService mockHostCommandService;
    @Mock
    private ClusterVariableService mockVariableService;
    @Mock
    private ClusterHostService mockHostService;
    @Mock
    private ClusterServiceInstanceRoleGroupService mockRoleGroupService;
    @Mock
    private ClusterServiceRoleGroupConfigService mockGroupConfigService;
    @Mock
    private ClusterServiceRoleInstanceService mockRoleInstanceService;

    @InjectMocks
    private ServiceInstallServiceImpl serviceInstallServiceImplUnderTest;

    @Test
    public void testGetServiceConfigOption() {
        // Setup
        final Result expectedResult = new Result();
        expectedResult.setCode(0);
        expectedResult.setMsg("msg");
        expectedResult.setData("data");

        // Configure ClusterInfoService.getById(...).
        final ClusterInfoEntity clusterInfoEntity = new ClusterInfoEntity();
        clusterInfoEntity.setId(0);
        clusterInfoEntity.setCreateBy("createBy");
        clusterInfoEntity.setCreateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterInfoEntity.setClusterName("clusterName");
        clusterInfoEntity.setClusterCode("clusterCode");
        clusterInfoEntity.setClusterFrame("clusterFrame");
        clusterInfoEntity.setFrameVersion("frameVersion");
        clusterInfoEntity.setClusterState(ClusterState.RUNNING);
        clusterInfoEntity.setFrameId(0);
        final UserInfoEntity userInfoEntity = new UserInfoEntity();
        userInfoEntity.setId(0);
        userInfoEntity.setUsername("username");
        userInfoEntity.setPassword("password");
        userInfoEntity.setEmail("email");
        userInfoEntity.setPhone("phone");
        clusterInfoEntity.setClusterManagerList(Arrays.asList(userInfoEntity));
        when(mockClusterInfoService.getById(0)).thenReturn(clusterInfoEntity);

        // Configure ClusterServiceInstanceService.getServiceInstanceByClusterIdAndServiceName(...).
        final ClusterServiceInstanceEntity clusterServiceInstanceEntity = new ClusterServiceInstanceEntity();
        clusterServiceInstanceEntity.setId(0);
        clusterServiceInstanceEntity.setClusterId(0);
        clusterServiceInstanceEntity.setServiceName("serviceName");
        clusterServiceInstanceEntity.setLabel("label");
        clusterServiceInstanceEntity.setServiceState(ServiceState.WAIT_INSTALL);
        clusterServiceInstanceEntity.setServiceStateCode(0);
        clusterServiceInstanceEntity.setUpdateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterServiceInstanceEntity.setCreateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterServiceInstanceEntity.setNeedRestart(NeedRestart.NO);
        clusterServiceInstanceEntity.setFrameServiceId(0);
        clusterServiceInstanceEntity.setDashboardUrl("dashboardUrl");
        clusterServiceInstanceEntity.setAlertNum(0);
        clusterServiceInstanceEntity.setSortNum(0);
        when(mockServiceInstanceService.getServiceInstanceByClusterIdAndServiceName(0, "serviceName")).thenReturn(
                clusterServiceInstanceEntity);

        // Configure ClusterServiceInstanceRoleGroupService.getRoleGroupByServiceInstanceId(...).
        final ClusterServiceInstanceRoleGroup clusterServiceInstanceRoleGroup = new ClusterServiceInstanceRoleGroup();
        clusterServiceInstanceRoleGroup.setId(0);
        clusterServiceInstanceRoleGroup.setRoleGroupName("默认角色组");
        clusterServiceInstanceRoleGroup.setServiceInstanceId(0);
        clusterServiceInstanceRoleGroup.setServiceName("serviceName");
        clusterServiceInstanceRoleGroup.setClusterId(0);
        clusterServiceInstanceRoleGroup.setRoleGroupType("auto");
        when(mockRoleGroupService.getRoleGroupByServiceInstanceId(0)).thenReturn(clusterServiceInstanceRoleGroup);

        // Configure ClusterServiceRoleGroupConfigService.getConfigByRoleGroupId(...).
        final ClusterServiceRoleGroupConfig clusterServiceRoleGroupConfig = new ClusterServiceRoleGroupConfig();
        clusterServiceRoleGroupConfig.setId(0);
        clusterServiceRoleGroupConfig.setRoleGroupId(0);
        clusterServiceRoleGroupConfig.setConfigJson("configJson");
        clusterServiceRoleGroupConfig.setConfigJsonMd5("configJsonMd5");
        clusterServiceRoleGroupConfig.setConfigVersion(0);
        clusterServiceRoleGroupConfig.setConfigFileJson("configFileJson");
        clusterServiceRoleGroupConfig.setConfigFileJsonMd5("configFileJsonMd5");
        clusterServiceRoleGroupConfig.setClusterId(0);
        clusterServiceRoleGroupConfig.setCreateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterServiceRoleGroupConfig.setUpdateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterServiceRoleGroupConfig.setServiceName("serviceName");
        when(mockGroupConfigService.getConfigByRoleGroupId(0)).thenReturn(clusterServiceRoleGroupConfig);

        // Configure FrameServiceService.getServiceByFrameCodeAndServiceName(...).
        final FrameServiceEntity frameServiceEntity = new FrameServiceEntity();
        frameServiceEntity.setId(0);
        frameServiceEntity.setFrameId(0);
        frameServiceEntity.setServiceName("serviceName");
        frameServiceEntity.setLabel("label");
        frameServiceEntity.setServiceVersion("serviceVersion");
        frameServiceEntity.setServiceDesc("serviceDesc");
        frameServiceEntity.setPackageName("packageName");
        frameServiceEntity.setDependencies("dependencies");
        frameServiceEntity.setServiceJson("serviceJson");
        frameServiceEntity.setServiceJsonMd5("serviceJsonMd5");
        frameServiceEntity.setServiceConfig("serviceConfig");
        frameServiceEntity.setFrameCode("frameCode");
        frameServiceEntity.setConfigFileJson("configFileJson");
        frameServiceEntity.setConfigFileJsonMd5("configFileJsonMd5");
        frameServiceEntity.setSortNum(0);
        when(mockFrameService.getServiceByFrameCodeAndServiceName("clusterFrame", "serviceName")).thenReturn(
                frameServiceEntity);

        // Run the test
        final Result result = serviceInstallServiceImplUnderTest.getServiceConfigOption(0, "serviceName");

        // Verify the results
        assertEquals(expectedResult, result);
    }

    @Test
    public void testSaveServiceConfig() {
        // Setup
        final ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setName("name");
        serviceConfig.setValue("value");
        serviceConfig.setLabel("label");
        serviceConfig.setDescription("description");
        serviceConfig.setRequired(false);
        serviceConfig.setType("type");
        serviceConfig.setConfigurableInWizard(false);
        serviceConfig.setDefaultValue("defaultValue");
        serviceConfig.setMinValue(0);
        serviceConfig.setMaxValue(0);
        serviceConfig.setUnit("unit");
        serviceConfig.setHidden(false);
        serviceConfig.setSelectValue(Arrays.asList("value"));
        serviceConfig.setConfigType("configType");
        final List<ServiceConfig> list = Arrays.asList(serviceConfig);
        final Result expectedResult = new Result();
        expectedResult.setCode(0);
        expectedResult.setMsg("msg");
        expectedResult.setData("data");

        // Configure ClusterInfoService.getById(...).
        final ClusterInfoEntity clusterInfoEntity = new ClusterInfoEntity();
        clusterInfoEntity.setId(0);
        clusterInfoEntity.setCreateBy("createBy");
        clusterInfoEntity.setCreateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterInfoEntity.setClusterName("clusterName");
        clusterInfoEntity.setClusterCode("clusterCode");
        clusterInfoEntity.setClusterFrame("clusterFrame");
        clusterInfoEntity.setFrameVersion("frameVersion");
        clusterInfoEntity.setClusterState(ClusterState.RUNNING);
        clusterInfoEntity.setFrameId(0);
        final UserInfoEntity userInfoEntity = new UserInfoEntity();
        userInfoEntity.setId(0);
        userInfoEntity.setUsername("username");
        userInfoEntity.setPassword("password");
        userInfoEntity.setEmail("email");
        userInfoEntity.setPhone("phone");
        clusterInfoEntity.setClusterManagerList(Arrays.asList(userInfoEntity));
        when(mockClusterInfoService.getById(0)).thenReturn(clusterInfoEntity);

        // Configure FrameServiceService.getServiceByFrameCodeAndServiceName(...).
        final FrameServiceEntity frameServiceEntity = new FrameServiceEntity();
        frameServiceEntity.setId(0);
        frameServiceEntity.setFrameId(0);
        frameServiceEntity.setServiceName("serviceName");
        frameServiceEntity.setLabel("label");
        frameServiceEntity.setServiceVersion("serviceVersion");
        frameServiceEntity.setServiceDesc("serviceDesc");
        frameServiceEntity.setPackageName("packageName");
        frameServiceEntity.setDependencies("dependencies");
        frameServiceEntity.setServiceJson("serviceJson");
        frameServiceEntity.setServiceJsonMd5("serviceJsonMd5");
        frameServiceEntity.setServiceConfig("serviceConfig");
        frameServiceEntity.setFrameCode("frameCode");
        frameServiceEntity.setConfigFileJson("configFileJson");
        frameServiceEntity.setConfigFileJsonMd5("configFileJsonMd5");
        frameServiceEntity.setSortNum(0);
        when(mockFrameService.getServiceByFrameCodeAndServiceName("clusterFrame", "serviceName")).thenReturn(
                frameServiceEntity);

        // Configure ClusterVariableService.getVariableByVariableName(...).
        final ClusterVariable clusterVariable = new ClusterVariable();
        clusterVariable.setId(0);
        clusterVariable.setClusterId(0);
        clusterVariable.setVariableName("variableName");
        clusterVariable.setVariableValue("variableValue");
        when(mockVariableService.getVariableByVariableName("variableName", 0)).thenReturn(clusterVariable);

        when(mockVariableService.updateById(new ClusterVariable())).thenReturn(false);
        when(mockVariableService.save(new ClusterVariable())).thenReturn(false);

        // Configure ClusterHostService.list(...).
        final ClusterHostEntity clusterHostEntity = new ClusterHostEntity();
        clusterHostEntity.setId(0);
        clusterHostEntity.setCreateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterHostEntity.setHostname("hostname");
        clusterHostEntity.setIp("ip");
        clusterHostEntity.setRack("rack");
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
        final List<ClusterHostEntity> clusterHostEntities = Arrays.asList(clusterHostEntity);
        when(mockHostService.list(any(QueryWrapper.class))).thenReturn(clusterHostEntities);

        // Configure ClusterServiceInstanceService.getServiceInstanceByClusterIdAndServiceName(...).
        final ClusterServiceInstanceEntity clusterServiceInstanceEntity = new ClusterServiceInstanceEntity();
        clusterServiceInstanceEntity.setId(0);
        clusterServiceInstanceEntity.setClusterId(0);
        clusterServiceInstanceEntity.setServiceName("serviceName");
        clusterServiceInstanceEntity.setLabel("label");
        clusterServiceInstanceEntity.setServiceState(ServiceState.WAIT_INSTALL);
        clusterServiceInstanceEntity.setServiceStateCode(0);
        clusterServiceInstanceEntity.setUpdateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterServiceInstanceEntity.setCreateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterServiceInstanceEntity.setNeedRestart(NeedRestart.NO);
        clusterServiceInstanceEntity.setFrameServiceId(0);
        clusterServiceInstanceEntity.setDashboardUrl("dashboardUrl");
        clusterServiceInstanceEntity.setAlertNum(0);
        clusterServiceInstanceEntity.setSortNum(0);
        when(mockServiceInstanceService.getServiceInstanceByClusterIdAndServiceName(0, "serviceName")).thenReturn(
                clusterServiceInstanceEntity);

        when(mockServiceInstanceService.save(new ClusterServiceInstanceEntity())).thenReturn(false);
        when(mockRoleGroupService.save(new ClusterServiceInstanceRoleGroup())).thenReturn(false);
        when(mockGroupConfigService.save(new ClusterServiceRoleGroupConfig())).thenReturn(false);

        // Configure ClusterServiceInstanceRoleGroupService.getRoleGroupByServiceInstanceId(...).
        final ClusterServiceInstanceRoleGroup clusterServiceInstanceRoleGroup = new ClusterServiceInstanceRoleGroup();
        clusterServiceInstanceRoleGroup.setId(0);
        clusterServiceInstanceRoleGroup.setRoleGroupName("默认角色组");
        clusterServiceInstanceRoleGroup.setServiceInstanceId(0);
        clusterServiceInstanceRoleGroup.setServiceName("serviceName");
        clusterServiceInstanceRoleGroup.setClusterId(0);
        clusterServiceInstanceRoleGroup.setRoleGroupType("auto");
        when(mockRoleGroupService.getRoleGroupByServiceInstanceId(0)).thenReturn(clusterServiceInstanceRoleGroup);

        // Configure ClusterServiceRoleGroupConfigService.getConfigByRoleGroupId(...).
        final ClusterServiceRoleGroupConfig clusterServiceRoleGroupConfig = new ClusterServiceRoleGroupConfig();
        clusterServiceRoleGroupConfig.setId(0);
        clusterServiceRoleGroupConfig.setRoleGroupId(0);
        clusterServiceRoleGroupConfig.setConfigJson("configJson");
        clusterServiceRoleGroupConfig.setConfigJsonMd5("configJsonMd5");
        clusterServiceRoleGroupConfig.setConfigVersion(0);
        clusterServiceRoleGroupConfig.setConfigFileJson("configFileJson");
        clusterServiceRoleGroupConfig.setConfigFileJsonMd5("configFileJsonMd5");
        clusterServiceRoleGroupConfig.setClusterId(0);
        clusterServiceRoleGroupConfig.setCreateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterServiceRoleGroupConfig.setUpdateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterServiceRoleGroupConfig.setServiceName("serviceName");
        when(mockGroupConfigService.getConfigByRoleGroupId(0)).thenReturn(clusterServiceRoleGroupConfig);

        when(mockRoleGroupService.count(any(QueryWrapper.class))).thenReturn(0);
        when(mockServiceInstanceService.updateById(new ClusterServiceInstanceEntity())).thenReturn(false);

        // Run the test
        final Result result = serviceInstallServiceImplUnderTest.saveServiceConfig(0, "serviceName", list, 0);

        // Verify the results
        assertEquals(expectedResult, result);
        verify(mockVariableService).updateById(new ClusterVariable());
        verify(mockVariableService).save(new ClusterVariable());
        verify(mockServiceInstanceService).save(new ClusterServiceInstanceEntity());
        verify(mockRoleGroupService).save(new ClusterServiceInstanceRoleGroup());
        verify(mockGroupConfigService).save(new ClusterServiceRoleGroupConfig());
        verify(mockRoleInstanceService).updateToNeedRestart(0);
        verify(mockServiceInstanceService).updateById(new ClusterServiceInstanceEntity());
    }

    @Test
    public void testSaveServiceConfig_ClusterHostServiceReturnsNoItems() {
        // Setup
        final ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setName("name");
        serviceConfig.setValue("value");
        serviceConfig.setLabel("label");
        serviceConfig.setDescription("description");
        serviceConfig.setRequired(false);
        serviceConfig.setType("type");
        serviceConfig.setConfigurableInWizard(false);
        serviceConfig.setDefaultValue("defaultValue");
        serviceConfig.setMinValue(0);
        serviceConfig.setMaxValue(0);
        serviceConfig.setUnit("unit");
        serviceConfig.setHidden(false);
        serviceConfig.setSelectValue(Arrays.asList("value"));
        serviceConfig.setConfigType("configType");
        final List<ServiceConfig> list = Arrays.asList(serviceConfig);
        final Result expectedResult = new Result();
        expectedResult.setCode(0);
        expectedResult.setMsg("msg");
        expectedResult.setData("data");

        // Configure ClusterInfoService.getById(...).
        final ClusterInfoEntity clusterInfoEntity = new ClusterInfoEntity();
        clusterInfoEntity.setId(0);
        clusterInfoEntity.setCreateBy("createBy");
        clusterInfoEntity.setCreateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterInfoEntity.setClusterName("clusterName");
        clusterInfoEntity.setClusterCode("clusterCode");
        clusterInfoEntity.setClusterFrame("clusterFrame");
        clusterInfoEntity.setFrameVersion("frameVersion");
        clusterInfoEntity.setClusterState(ClusterState.RUNNING);
        clusterInfoEntity.setFrameId(0);
        final UserInfoEntity userInfoEntity = new UserInfoEntity();
        userInfoEntity.setId(0);
        userInfoEntity.setUsername("username");
        userInfoEntity.setPassword("password");
        userInfoEntity.setEmail("email");
        userInfoEntity.setPhone("phone");
        clusterInfoEntity.setClusterManagerList(Arrays.asList(userInfoEntity));
        when(mockClusterInfoService.getById(0)).thenReturn(clusterInfoEntity);

        // Configure FrameServiceService.getServiceByFrameCodeAndServiceName(...).
        final FrameServiceEntity frameServiceEntity = new FrameServiceEntity();
        frameServiceEntity.setId(0);
        frameServiceEntity.setFrameId(0);
        frameServiceEntity.setServiceName("serviceName");
        frameServiceEntity.setLabel("label");
        frameServiceEntity.setServiceVersion("serviceVersion");
        frameServiceEntity.setServiceDesc("serviceDesc");
        frameServiceEntity.setPackageName("packageName");
        frameServiceEntity.setDependencies("dependencies");
        frameServiceEntity.setServiceJson("serviceJson");
        frameServiceEntity.setServiceJsonMd5("serviceJsonMd5");
        frameServiceEntity.setServiceConfig("serviceConfig");
        frameServiceEntity.setFrameCode("frameCode");
        frameServiceEntity.setConfigFileJson("configFileJson");
        frameServiceEntity.setConfigFileJsonMd5("configFileJsonMd5");
        frameServiceEntity.setSortNum(0);
        when(mockFrameService.getServiceByFrameCodeAndServiceName("clusterFrame", "serviceName")).thenReturn(
                frameServiceEntity);

        // Configure ClusterVariableService.getVariableByVariableName(...).
        final ClusterVariable clusterVariable = new ClusterVariable();
        clusterVariable.setId(0);
        clusterVariable.setClusterId(0);
        clusterVariable.setVariableName("variableName");
        clusterVariable.setVariableValue("variableValue");
        when(mockVariableService.getVariableByVariableName("variableName", 0)).thenReturn(clusterVariable);

        when(mockVariableService.updateById(new ClusterVariable())).thenReturn(false);
        when(mockVariableService.save(new ClusterVariable())).thenReturn(false);
        when(mockHostService.list(any(QueryWrapper.class))).thenReturn(Collections.emptyList());

        // Configure ClusterServiceInstanceService.getServiceInstanceByClusterIdAndServiceName(...).
        final ClusterServiceInstanceEntity clusterServiceInstanceEntity = new ClusterServiceInstanceEntity();
        clusterServiceInstanceEntity.setId(0);
        clusterServiceInstanceEntity.setClusterId(0);
        clusterServiceInstanceEntity.setServiceName("serviceName");
        clusterServiceInstanceEntity.setLabel("label");
        clusterServiceInstanceEntity.setServiceState(ServiceState.WAIT_INSTALL);
        clusterServiceInstanceEntity.setServiceStateCode(0);
        clusterServiceInstanceEntity.setUpdateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterServiceInstanceEntity.setCreateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterServiceInstanceEntity.setNeedRestart(NeedRestart.NO);
        clusterServiceInstanceEntity.setFrameServiceId(0);
        clusterServiceInstanceEntity.setDashboardUrl("dashboardUrl");
        clusterServiceInstanceEntity.setAlertNum(0);
        clusterServiceInstanceEntity.setSortNum(0);
        when(mockServiceInstanceService.getServiceInstanceByClusterIdAndServiceName(0, "serviceName")).thenReturn(
                clusterServiceInstanceEntity);

        when(mockServiceInstanceService.save(new ClusterServiceInstanceEntity())).thenReturn(false);
        when(mockRoleGroupService.save(new ClusterServiceInstanceRoleGroup())).thenReturn(false);
        when(mockGroupConfigService.save(new ClusterServiceRoleGroupConfig())).thenReturn(false);

        // Configure ClusterServiceInstanceRoleGroupService.getRoleGroupByServiceInstanceId(...).
        final ClusterServiceInstanceRoleGroup clusterServiceInstanceRoleGroup = new ClusterServiceInstanceRoleGroup();
        clusterServiceInstanceRoleGroup.setId(0);
        clusterServiceInstanceRoleGroup.setRoleGroupName("默认角色组");
        clusterServiceInstanceRoleGroup.setServiceInstanceId(0);
        clusterServiceInstanceRoleGroup.setServiceName("serviceName");
        clusterServiceInstanceRoleGroup.setClusterId(0);
        clusterServiceInstanceRoleGroup.setRoleGroupType("auto");
        when(mockRoleGroupService.getRoleGroupByServiceInstanceId(0)).thenReturn(clusterServiceInstanceRoleGroup);

        // Configure ClusterServiceRoleGroupConfigService.getConfigByRoleGroupId(...).
        final ClusterServiceRoleGroupConfig clusterServiceRoleGroupConfig = new ClusterServiceRoleGroupConfig();
        clusterServiceRoleGroupConfig.setId(0);
        clusterServiceRoleGroupConfig.setRoleGroupId(0);
        clusterServiceRoleGroupConfig.setConfigJson("configJson");
        clusterServiceRoleGroupConfig.setConfigJsonMd5("configJsonMd5");
        clusterServiceRoleGroupConfig.setConfigVersion(0);
        clusterServiceRoleGroupConfig.setConfigFileJson("configFileJson");
        clusterServiceRoleGroupConfig.setConfigFileJsonMd5("configFileJsonMd5");
        clusterServiceRoleGroupConfig.setClusterId(0);
        clusterServiceRoleGroupConfig.setCreateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterServiceRoleGroupConfig.setUpdateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterServiceRoleGroupConfig.setServiceName("serviceName");
        when(mockGroupConfigService.getConfigByRoleGroupId(0)).thenReturn(clusterServiceRoleGroupConfig);

        when(mockRoleGroupService.count(any(QueryWrapper.class))).thenReturn(0);
        when(mockServiceInstanceService.updateById(new ClusterServiceInstanceEntity())).thenReturn(false);

        // Run the test
        final Result result = serviceInstallServiceImplUnderTest.saveServiceConfig(0, "serviceName", list, 0);

        // Verify the results
        assertEquals(expectedResult, result);
        verify(mockVariableService).updateById(new ClusterVariable());
        verify(mockVariableService).save(new ClusterVariable());
        verify(mockServiceInstanceService).save(new ClusterServiceInstanceEntity());
        verify(mockRoleGroupService).save(new ClusterServiceInstanceRoleGroup());
        verify(mockGroupConfigService).save(new ClusterServiceRoleGroupConfig());
        verify(mockRoleInstanceService).updateToNeedRestart(0);
        verify(mockServiceInstanceService).updateById(new ClusterServiceInstanceEntity());
    }

    @Test
    public void testSaveServiceRoleHostMapping() {
        // Setup
        final ServiceRoleHostMapping serviceRoleHostMapping = new ServiceRoleHostMapping();
        serviceRoleHostMapping.setServiceRole("serviceRole");
        serviceRoleHostMapping.setHosts(Arrays.asList("value"));
        final List<ServiceRoleHostMapping> list = Arrays.asList(serviceRoleHostMapping);
        final Result expectedResult = new Result();
        expectedResult.setCode(0);
        expectedResult.setMsg("msg");
        expectedResult.setData("data");

        // Configure ClusterInfoService.getById(...).
        final ClusterInfoEntity clusterInfoEntity = new ClusterInfoEntity();
        clusterInfoEntity.setId(0);
        clusterInfoEntity.setCreateBy("createBy");
        clusterInfoEntity.setCreateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterInfoEntity.setClusterName("clusterName");
        clusterInfoEntity.setClusterCode("clusterCode");
        clusterInfoEntity.setClusterFrame("clusterFrame");
        clusterInfoEntity.setFrameVersion("frameVersion");
        clusterInfoEntity.setClusterState(ClusterState.RUNNING);
        clusterInfoEntity.setFrameId(0);
        final UserInfoEntity userInfoEntity = new UserInfoEntity();
        userInfoEntity.setId(0);
        userInfoEntity.setUsername("username");
        userInfoEntity.setPassword("password");
        userInfoEntity.setEmail("email");
        userInfoEntity.setPhone("phone");
        clusterInfoEntity.setClusterManagerList(Arrays.asList(userInfoEntity));
        when(mockClusterInfoService.getById(0)).thenReturn(clusterInfoEntity);

        // Run the test
        final Result result = serviceInstallServiceImplUnderTest.saveServiceRoleHostMapping(0, list);

        // Verify the results
        assertEquals(expectedResult, result);
    }

    @Test
    public void testSaveHostServiceRoleMapping() {
        // Setup
        final HostServiceRoleMapping hostServiceRoleMapping = new HostServiceRoleMapping();
        hostServiceRoleMapping.setHost("host");
        hostServiceRoleMapping.setServiceRoles(Arrays.asList("value"));
        final List<HostServiceRoleMapping> list = Arrays.asList(hostServiceRoleMapping);
        final Result expectedResult = new Result();
        expectedResult.setCode(0);
        expectedResult.setMsg("msg");
        expectedResult.setData("data");

        // Configure ClusterInfoService.getById(...).
        final ClusterInfoEntity clusterInfoEntity = new ClusterInfoEntity();
        clusterInfoEntity.setId(0);
        clusterInfoEntity.setCreateBy("createBy");
        clusterInfoEntity.setCreateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterInfoEntity.setClusterName("clusterName");
        clusterInfoEntity.setClusterCode("clusterCode");
        clusterInfoEntity.setClusterFrame("clusterFrame");
        clusterInfoEntity.setFrameVersion("frameVersion");
        clusterInfoEntity.setClusterState(ClusterState.RUNNING);
        clusterInfoEntity.setFrameId(0);
        final UserInfoEntity userInfoEntity = new UserInfoEntity();
        userInfoEntity.setId(0);
        userInfoEntity.setUsername("username");
        userInfoEntity.setPassword("password");
        userInfoEntity.setEmail("email");
        userInfoEntity.setPhone("phone");
        clusterInfoEntity.setClusterManagerList(Arrays.asList(userInfoEntity));
        when(mockClusterInfoService.getById(0)).thenReturn(clusterInfoEntity);

        // Run the test
        final Result result = serviceInstallServiceImplUnderTest.saveHostServiceRoleMapping(0, list);

        // Verify the results
        assertEquals(expectedResult, result);
    }

    @Test
    public void testGetServiceRoleDeployOverview() {
        // Setup
        final Result expectedResult = new Result();
        expectedResult.setCode(0);
        expectedResult.setMsg("msg");
        expectedResult.setData("data");

        // Configure ClusterInfoService.getById(...).
        final ClusterInfoEntity clusterInfoEntity = new ClusterInfoEntity();
        clusterInfoEntity.setId(0);
        clusterInfoEntity.setCreateBy("createBy");
        clusterInfoEntity.setCreateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterInfoEntity.setClusterName("clusterName");
        clusterInfoEntity.setClusterCode("clusterCode");
        clusterInfoEntity.setClusterFrame("clusterFrame");
        clusterInfoEntity.setFrameVersion("frameVersion");
        clusterInfoEntity.setClusterState(ClusterState.RUNNING);
        clusterInfoEntity.setFrameId(0);
        final UserInfoEntity userInfoEntity = new UserInfoEntity();
        userInfoEntity.setId(0);
        userInfoEntity.setUsername("username");
        userInfoEntity.setPassword("password");
        userInfoEntity.setEmail("email");
        userInfoEntity.setPhone("phone");
        clusterInfoEntity.setClusterManagerList(Arrays.asList(userInfoEntity));
        when(mockClusterInfoService.getById(0)).thenReturn(clusterInfoEntity);

        // Run the test
        final Result result = serviceInstallServiceImplUnderTest.getServiceRoleDeployOverview(0);

        // Verify the results
        assertEquals(expectedResult, result);
    }

    @Test
    public void testStartInstallService() {
        // Setup
        final Result expectedResult = new Result();
        expectedResult.setCode(0);
        expectedResult.setMsg("msg");
        expectedResult.setData("data");

        // Configure ClusterServiceCommandService.listByIds(...).
        final ClusterServiceCommandEntity clusterServiceCommandEntity = new ClusterServiceCommandEntity();
        clusterServiceCommandEntity.setCommandId("commandId");
        clusterServiceCommandEntity.setCreateBy("createBy");
        clusterServiceCommandEntity.setCreateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterServiceCommandEntity.setCommandName("commandName");
        clusterServiceCommandEntity.setCommandState(CommandState.WAIT);
        clusterServiceCommandEntity.setCommandStateCode(0);
        clusterServiceCommandEntity.setCommandProgress(0);
        clusterServiceCommandEntity.setClusterId(0);
        clusterServiceCommandEntity.setServiceName("parentName");
        clusterServiceCommandEntity.setCommandType(0);
        clusterServiceCommandEntity.setDurationTime("durationTime");
        clusterServiceCommandEntity.setEndTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterServiceCommandEntity.setServiceInstanceId(0);
        final Collection<ClusterServiceCommandEntity> clusterServiceCommandEntities = Arrays.asList(
                clusterServiceCommandEntity);
        when(mockCommandService.listByIds(Arrays.asList("value"))).thenReturn(clusterServiceCommandEntities);

        // Configure ClusterInfoService.getById(...).
        final ClusterInfoEntity clusterInfoEntity = new ClusterInfoEntity();
        clusterInfoEntity.setId(0);
        clusterInfoEntity.setCreateBy("createBy");
        clusterInfoEntity.setCreateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterInfoEntity.setClusterName("clusterName");
        clusterInfoEntity.setClusterCode("clusterCode");
        clusterInfoEntity.setClusterFrame("clusterFrame");
        clusterInfoEntity.setFrameVersion("frameVersion");
        clusterInfoEntity.setClusterState(ClusterState.RUNNING);
        clusterInfoEntity.setFrameId(0);
        final UserInfoEntity userInfoEntity = new UserInfoEntity();
        userInfoEntity.setId(0);
        userInfoEntity.setUsername("username");
        userInfoEntity.setPassword("password");
        userInfoEntity.setEmail("email");
        userInfoEntity.setPhone("phone");
        clusterInfoEntity.setClusterManagerList(Arrays.asList(userInfoEntity));
        when(mockClusterInfoService.getById(0)).thenReturn(clusterInfoEntity);

        // Configure ClusterServiceCommandHostCommandService.getHostCommandListByCommandId(...).
        final ClusterServiceCommandHostCommandEntity clusterServiceCommandHostCommandEntity = new ClusterServiceCommandHostCommandEntity();
        clusterServiceCommandHostCommandEntity.setHostCommandId("hostCommandId");
        clusterServiceCommandHostCommandEntity.setCommandName("commandName");
        clusterServiceCommandHostCommandEntity.setCommandState(CommandState.WAIT);
        clusterServiceCommandHostCommandEntity.setCommandStateCode(0);
        clusterServiceCommandHostCommandEntity.setCommandProgress(0);
        clusterServiceCommandHostCommandEntity.setCommandHostId("commandHostId");
        clusterServiceCommandHostCommandEntity.setCommandId("commandId");
        clusterServiceCommandHostCommandEntity.setHostname("hostname");
        clusterServiceCommandHostCommandEntity.setServiceRoleName("serviceRoleName");
        clusterServiceCommandHostCommandEntity.setServiceRoleType(RoleType.MASTER);
        clusterServiceCommandHostCommandEntity.setResultMsg("resultMsg");
        clusterServiceCommandHostCommandEntity.setCreateTime(
                new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterServiceCommandHostCommandEntity.setCommandType(0);
        final List<ClusterServiceCommandHostCommandEntity> clusterServiceCommandHostCommandEntities = Arrays.asList(
                clusterServiceCommandHostCommandEntity);
        when(mockHostCommandService.getHostCommandListByCommandId("commandId")).thenReturn(
                clusterServiceCommandHostCommandEntities);

        // Run the test
        final Result result = serviceInstallServiceImplUnderTest.startInstallService(0, Arrays.asList("value"));

        // Verify the results
        assertEquals(expectedResult, result);
    }

    @Test
    public void testStartInstallService_ClusterServiceCommandServiceReturnsNoItems() {
        // Setup
        final Result expectedResult = new Result();
        expectedResult.setCode(0);
        expectedResult.setMsg("msg");
        expectedResult.setData("data");

        when(mockCommandService.listByIds(Arrays.asList("value"))).thenReturn(Collections.emptyList());

        // Configure ClusterInfoService.getById(...).
        final ClusterInfoEntity clusterInfoEntity = new ClusterInfoEntity();
        clusterInfoEntity.setId(0);
        clusterInfoEntity.setCreateBy("createBy");
        clusterInfoEntity.setCreateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterInfoEntity.setClusterName("clusterName");
        clusterInfoEntity.setClusterCode("clusterCode");
        clusterInfoEntity.setClusterFrame("clusterFrame");
        clusterInfoEntity.setFrameVersion("frameVersion");
        clusterInfoEntity.setClusterState(ClusterState.RUNNING);
        clusterInfoEntity.setFrameId(0);
        final UserInfoEntity userInfoEntity = new UserInfoEntity();
        userInfoEntity.setId(0);
        userInfoEntity.setUsername("username");
        userInfoEntity.setPassword("password");
        userInfoEntity.setEmail("email");
        userInfoEntity.setPhone("phone");
        clusterInfoEntity.setClusterManagerList(Arrays.asList(userInfoEntity));
        when(mockClusterInfoService.getById(0)).thenReturn(clusterInfoEntity);

        // Configure ClusterServiceCommandHostCommandService.getHostCommandListByCommandId(...).
        final ClusterServiceCommandHostCommandEntity clusterServiceCommandHostCommandEntity = new ClusterServiceCommandHostCommandEntity();
        clusterServiceCommandHostCommandEntity.setHostCommandId("hostCommandId");
        clusterServiceCommandHostCommandEntity.setCommandName("commandName");
        clusterServiceCommandHostCommandEntity.setCommandState(CommandState.WAIT);
        clusterServiceCommandHostCommandEntity.setCommandStateCode(0);
        clusterServiceCommandHostCommandEntity.setCommandProgress(0);
        clusterServiceCommandHostCommandEntity.setCommandHostId("commandHostId");
        clusterServiceCommandHostCommandEntity.setCommandId("commandId");
        clusterServiceCommandHostCommandEntity.setHostname("hostname");
        clusterServiceCommandHostCommandEntity.setServiceRoleName("serviceRoleName");
        clusterServiceCommandHostCommandEntity.setServiceRoleType(RoleType.MASTER);
        clusterServiceCommandHostCommandEntity.setResultMsg("resultMsg");
        clusterServiceCommandHostCommandEntity.setCreateTime(
                new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterServiceCommandHostCommandEntity.setCommandType(0);
        final List<ClusterServiceCommandHostCommandEntity> clusterServiceCommandHostCommandEntities = Arrays.asList(
                clusterServiceCommandHostCommandEntity);
        when(mockHostCommandService.getHostCommandListByCommandId("commandId")).thenReturn(
                clusterServiceCommandHostCommandEntities);

        // Run the test
        final Result result = serviceInstallServiceImplUnderTest.startInstallService(0, Arrays.asList("value"));

        // Verify the results
        assertEquals(expectedResult, result);
    }

    @Test
    public void testStartInstallService_ClusterServiceCommandHostCommandServiceReturnsNoItems() {
        // Setup
        final Result expectedResult = new Result();
        expectedResult.setCode(0);
        expectedResult.setMsg("msg");
        expectedResult.setData("data");

        // Configure ClusterServiceCommandService.listByIds(...).
        final ClusterServiceCommandEntity clusterServiceCommandEntity = new ClusterServiceCommandEntity();
        clusterServiceCommandEntity.setCommandId("commandId");
        clusterServiceCommandEntity.setCreateBy("createBy");
        clusterServiceCommandEntity.setCreateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterServiceCommandEntity.setCommandName("commandName");
        clusterServiceCommandEntity.setCommandState(CommandState.WAIT);
        clusterServiceCommandEntity.setCommandStateCode(0);
        clusterServiceCommandEntity.setCommandProgress(0);
        clusterServiceCommandEntity.setClusterId(0);
        clusterServiceCommandEntity.setServiceName("parentName");
        clusterServiceCommandEntity.setCommandType(0);
        clusterServiceCommandEntity.setDurationTime("durationTime");
        clusterServiceCommandEntity.setEndTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterServiceCommandEntity.setServiceInstanceId(0);
        final Collection<ClusterServiceCommandEntity> clusterServiceCommandEntities = Arrays.asList(
                clusterServiceCommandEntity);
        when(mockCommandService.listByIds(Arrays.asList("value"))).thenReturn(clusterServiceCommandEntities);

        // Configure ClusterInfoService.getById(...).
        final ClusterInfoEntity clusterInfoEntity = new ClusterInfoEntity();
        clusterInfoEntity.setId(0);
        clusterInfoEntity.setCreateBy("createBy");
        clusterInfoEntity.setCreateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterInfoEntity.setClusterName("clusterName");
        clusterInfoEntity.setClusterCode("clusterCode");
        clusterInfoEntity.setClusterFrame("clusterFrame");
        clusterInfoEntity.setFrameVersion("frameVersion");
        clusterInfoEntity.setClusterState(ClusterState.RUNNING);
        clusterInfoEntity.setFrameId(0);
        final UserInfoEntity userInfoEntity = new UserInfoEntity();
        userInfoEntity.setId(0);
        userInfoEntity.setUsername("username");
        userInfoEntity.setPassword("password");
        userInfoEntity.setEmail("email");
        userInfoEntity.setPhone("phone");
        clusterInfoEntity.setClusterManagerList(Arrays.asList(userInfoEntity));
        when(mockClusterInfoService.getById(0)).thenReturn(clusterInfoEntity);

        when(mockHostCommandService.getHostCommandListByCommandId("commandId")).thenReturn(Collections.emptyList());

        // Run the test
        final Result result = serviceInstallServiceImplUnderTest.startInstallService(0, Arrays.asList("value"));

        // Verify the results
        assertEquals(expectedResult, result);
    }

    @Test
    public void testDownloadPackage() throws Exception {
        // Setup
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);

        // Run the test
        serviceInstallServiceImplUnderTest.downloadPackage("packageName", mockResponse);

        // Verify the results
    }

    @Test(expected = IOException.class)
    public void testDownloadPackage_ThrowsIOException() throws Exception {
        // Setup
        final HttpServletResponse mockResponse = mock(HttpServletResponse.class);

        // Run the test
        serviceInstallServiceImplUnderTest.downloadPackage("packageName", mockResponse);
    }

    @Test
    public void testGetServiceRoleHostMapping() {
        assertNull(serviceInstallServiceImplUnderTest.getServiceRoleHostMapping(0));
    }

    @Test
    public void testCheckServiceDependency() {
        // Setup
        final Result expectedResult = new Result();
        expectedResult.setCode(0);
        expectedResult.setMsg("msg");
        expectedResult.setData("data");

        // Configure ClusterServiceInstanceService.listRunningServiceInstance(...).
        final ClusterServiceInstanceEntity clusterServiceInstanceEntity = new ClusterServiceInstanceEntity();
        clusterServiceInstanceEntity.setId(0);
        clusterServiceInstanceEntity.setClusterId(0);
        clusterServiceInstanceEntity.setServiceName("serviceName");
        clusterServiceInstanceEntity.setLabel("label");
        clusterServiceInstanceEntity.setServiceState(ServiceState.WAIT_INSTALL);
        clusterServiceInstanceEntity.setServiceStateCode(0);
        clusterServiceInstanceEntity.setUpdateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterServiceInstanceEntity.setCreateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterServiceInstanceEntity.setNeedRestart(NeedRestart.NO);
        clusterServiceInstanceEntity.setFrameServiceId(0);
        clusterServiceInstanceEntity.setDashboardUrl("dashboardUrl");
        clusterServiceInstanceEntity.setAlertNum(0);
        clusterServiceInstanceEntity.setSortNum(0);
        final List<ClusterServiceInstanceEntity> serviceInstanceEntityList = Arrays.asList(
                clusterServiceInstanceEntity);
        when(mockServiceInstanceService.listRunningServiceInstance(0)).thenReturn(serviceInstanceEntityList);

        // Configure FrameServiceService.listServices(...).
        final FrameServiceEntity frameServiceEntity = new FrameServiceEntity();
        frameServiceEntity.setId(0);
        frameServiceEntity.setFrameId(0);
        frameServiceEntity.setServiceName("serviceName");
        frameServiceEntity.setLabel("label");
        frameServiceEntity.setServiceVersion("serviceVersion");
        frameServiceEntity.setServiceDesc("serviceDesc");
        frameServiceEntity.setPackageName("packageName");
        frameServiceEntity.setDependencies("dependencies");
        frameServiceEntity.setServiceJson("serviceJson");
        frameServiceEntity.setServiceJsonMd5("serviceJsonMd5");
        frameServiceEntity.setServiceConfig("serviceConfig");
        frameServiceEntity.setFrameCode("frameCode");
        frameServiceEntity.setConfigFileJson("configFileJson");
        frameServiceEntity.setConfigFileJsonMd5("configFileJsonMd5");
        frameServiceEntity.setSortNum(0);
        final List<FrameServiceEntity> frameServiceEntities = Arrays.asList(frameServiceEntity);
        when(mockFrameService.listServices("serviceIds")).thenReturn(frameServiceEntities);

        // Run the test
        final Result result = serviceInstallServiceImplUnderTest.checkServiceDependency(0, "serviceIds");

        // Verify the results
        assertEquals(expectedResult, result);
    }

    @Test
    public void testCheckServiceDependency_ClusterServiceInstanceServiceReturnsNoItems() {
        // Setup
        final Result expectedResult = new Result();
        expectedResult.setCode(0);
        expectedResult.setMsg("msg");
        expectedResult.setData("data");

        when(mockServiceInstanceService.listRunningServiceInstance(0)).thenReturn(Collections.emptyList());

        // Configure FrameServiceService.listServices(...).
        final FrameServiceEntity frameServiceEntity = new FrameServiceEntity();
        frameServiceEntity.setId(0);
        frameServiceEntity.setFrameId(0);
        frameServiceEntity.setServiceName("serviceName");
        frameServiceEntity.setLabel("label");
        frameServiceEntity.setServiceVersion("serviceVersion");
        frameServiceEntity.setServiceDesc("serviceDesc");
        frameServiceEntity.setPackageName("packageName");
        frameServiceEntity.setDependencies("dependencies");
        frameServiceEntity.setServiceJson("serviceJson");
        frameServiceEntity.setServiceJsonMd5("serviceJsonMd5");
        frameServiceEntity.setServiceConfig("serviceConfig");
        frameServiceEntity.setFrameCode("frameCode");
        frameServiceEntity.setConfigFileJson("configFileJson");
        frameServiceEntity.setConfigFileJsonMd5("configFileJsonMd5");
        frameServiceEntity.setSortNum(0);
        final List<FrameServiceEntity> frameServiceEntities = Arrays.asList(frameServiceEntity);
        when(mockFrameService.listServices("serviceIds")).thenReturn(frameServiceEntities);

        // Run the test
        final Result result = serviceInstallServiceImplUnderTest.checkServiceDependency(0, "serviceIds");

        // Verify the results
        assertEquals(expectedResult, result);
    }

    @Test
    public void testCheckServiceDependency_FrameServiceServiceReturnsNoItems() {
        // Setup
        final Result expectedResult = new Result();
        expectedResult.setCode(0);
        expectedResult.setMsg("msg");
        expectedResult.setData("data");

        // Configure ClusterServiceInstanceService.listRunningServiceInstance(...).
        final ClusterServiceInstanceEntity clusterServiceInstanceEntity = new ClusterServiceInstanceEntity();
        clusterServiceInstanceEntity.setId(0);
        clusterServiceInstanceEntity.setClusterId(0);
        clusterServiceInstanceEntity.setServiceName("serviceName");
        clusterServiceInstanceEntity.setLabel("label");
        clusterServiceInstanceEntity.setServiceState(ServiceState.WAIT_INSTALL);
        clusterServiceInstanceEntity.setServiceStateCode(0);
        clusterServiceInstanceEntity.setUpdateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterServiceInstanceEntity.setCreateTime(new GregorianCalendar(2020, Calendar.JANUARY, 1).getTime());
        clusterServiceInstanceEntity.setNeedRestart(NeedRestart.NO);
        clusterServiceInstanceEntity.setFrameServiceId(0);
        clusterServiceInstanceEntity.setDashboardUrl("dashboardUrl");
        clusterServiceInstanceEntity.setAlertNum(0);
        clusterServiceInstanceEntity.setSortNum(0);
        final List<ClusterServiceInstanceEntity> serviceInstanceEntityList = Arrays.asList(
                clusterServiceInstanceEntity);
        when(mockServiceInstanceService.listRunningServiceInstance(0)).thenReturn(serviceInstanceEntityList);

        when(mockFrameService.listServices("serviceIds")).thenReturn(Collections.emptyList());

        // Run the test
        final Result result = serviceInstallServiceImplUnderTest.checkServiceDependency(0, "serviceIds");

        // Verify the results
        assertEquals(expectedResult, result);
    }
}
