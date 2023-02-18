package com.datasophon.api.service.impl;

import com.datasophon.api.service.ClusterHostService;
import com.datasophon.api.service.ClusterUserGroupService;
import com.datasophon.common.utils.Result;
import com.datasophon.dao.entity.ClusterHostEntity;
import com.datasophon.dao.enums.MANAGED;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ClusterGroupServiceImplTest {

    @Mock
    private ClusterHostService mockHostService;
    @Mock
    private ClusterUserGroupService mockUserGroupService;

    @InjectMocks
    private ClusterGroupServiceImpl clusterGroupServiceImplUnderTest;

    @Test
    public void testSaveClusterGroup() {
        // Setup
        final Result expectedResult = new Result();
        expectedResult.setCode(0);
        expectedResult.setMsg("msg");
        expectedResult.setData("data");

        // Configure ClusterHostService.getHostListByClusterId(...).
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
        when(mockHostService.getHostListByClusterId(0)).thenReturn(clusterHostEntities);

        // Run the test
        final Result result = clusterGroupServiceImplUnderTest.saveClusterGroup(0, "groupName");

        // Verify the results
        assertEquals(expectedResult, result);
    }

    @Test
    public void testSaveClusterGroup_ClusterHostServiceReturnsNoItems() {
        // Setup
        final Result expectedResult = new Result();
        expectedResult.setCode(0);
        expectedResult.setMsg("msg");
        expectedResult.setData("data");

        when(mockHostService.getHostListByClusterId(0)).thenReturn(Collections.emptyList());

        // Run the test
        final Result result = clusterGroupServiceImplUnderTest.saveClusterGroup(0, "groupName");

        // Verify the results
        assertEquals(expectedResult, result);
    }

    @Test
    public void testRefreshUserGroupToHost() {
        // Setup
        // Configure ClusterHostService.getHostListByClusterId(...).
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
        when(mockHostService.getHostListByClusterId(0)).thenReturn(clusterHostEntities);

        // Run the test
        clusterGroupServiceImplUnderTest.refreshUserGroupToHost(0);

        // Verify the results
    }

    @Test
    public void testRefreshUserGroupToHost_ClusterHostServiceReturnsNoItems() {
        // Setup
        when(mockHostService.getHostListByClusterId(0)).thenReturn(Collections.emptyList());

        // Run the test
        clusterGroupServiceImplUnderTest.refreshUserGroupToHost(0);

        // Verify the results
    }

    @Test
    public void testDeleteUserGroup() {
        // Setup
        final Result expectedResult = new Result();
        expectedResult.setCode(0);
        expectedResult.setMsg("msg");
        expectedResult.setData("data");

        when(mockUserGroupService.countGroupUserNum(0)).thenReturn(0);

        // Configure ClusterHostService.getHostListByClusterId(...).
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
        when(mockHostService.getHostListByClusterId(0)).thenReturn(clusterHostEntities);

        // Run the test
        final Result result = clusterGroupServiceImplUnderTest.deleteUserGroup(0);

        // Verify the results
        assertEquals(expectedResult, result);
    }

    @Test
    public void testDeleteUserGroup_ClusterHostServiceReturnsNoItems() {
        // Setup
        final Result expectedResult = new Result();
        expectedResult.setCode(0);
        expectedResult.setMsg("msg");
        expectedResult.setData("data");

        when(mockUserGroupService.countGroupUserNum(0)).thenReturn(0);
        when(mockHostService.getHostListByClusterId(0)).thenReturn(Collections.emptyList());

        // Run the test
        final Result result = clusterGroupServiceImplUnderTest.deleteUserGroup(0);

        // Verify the results
        assertEquals(expectedResult, result);
    }

    @Test
    public void testListPage() {
        // Setup
        final Result expectedResult = new Result();
        expectedResult.setCode(0);
        expectedResult.setMsg("msg");
        expectedResult.setData("data");

        // Run the test
        final Result result = clusterGroupServiceImplUnderTest.listPage("groupName", 0, 0);

        // Verify the results
        assertEquals(expectedResult, result);
    }
}
