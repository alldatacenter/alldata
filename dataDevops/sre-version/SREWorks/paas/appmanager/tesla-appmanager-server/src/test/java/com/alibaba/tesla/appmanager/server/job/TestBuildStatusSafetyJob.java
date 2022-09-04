package com.alibaba.tesla.appmanager.server.job;

import com.alibaba.tesla.appmanager.autoconfig.SystemProperties;
import com.alibaba.tesla.appmanager.common.enums.ComponentPackageTaskStateEnum;
import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.common.util.PackageUtil;
import com.alibaba.tesla.appmanager.server.repository.condition.ComponentPackageTaskQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageTaskDO;
import com.alibaba.tesla.appmanager.server.service.componentpackage.ComponentPackageTaskService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;
import java.util.Date;
import java.util.function.Function;

/**
 * 测试构建包兜底任务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@RunWith(SpringRunner.class)
@Slf4j
public class TestBuildStatusSafetyJob {

    private static final int BUILD_MAX_RUNNING_SECONDS = 3600;
    private static final String BUCKET_NAME = "appmanager";
    private static final String APP_ID = "testapp";
    private static final ComponentTypeEnum COMPONENT_TYPE = ComponentTypeEnum.K8S_MICROSERVICE;
    private static final String COMPONENT_NAME = "testserver";
    private static final String PACKAGE_CREATOR = "SYSTEM";
    private static final String PACKAGE_VERSION = "2.1.1+20220201234212";
    private static final String PACKAGE_MD5 = "433966fbc2ea534ca32c706ab41f60ff";
    private static final String ENV_ID = "prod";
    private static final String EMPTY_DATA = "{}";

    @Mock
    private SystemProperties systemProperties;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ComponentPackageTaskService componentPackageTaskService;

    private BuildStatusSafetyJob buildStatusSafetyJob;

    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);
        buildStatusSafetyJob = Mockito.spy(new BuildStatusSafetyJob(
                systemProperties, eventPublisher, componentPackageTaskService
        ));
    }

    /**
     * 测试一个超时 RUNNING 和一个未超时 CREATED 的处理逻辑
     */
    @Test
    public void testExecute() {
        Mockito.doReturn(BUILD_MAX_RUNNING_SECONDS).when(systemProperties).getBuildMaxRunningSeconds();
        Date expired = new DateTime(new Date()).minusSeconds(BUILD_MAX_RUNNING_SECONDS + 1).toDate();
        ComponentPackageTaskDO expiredTask = ComponentPackageTaskDO.builder()
                .id(1L)
                .gmtCreate(expired)
                .gmtModified(expired)
                .appId(APP_ID)
                .componentType(COMPONENT_TYPE.toString())
                .componentName(COMPONENT_NAME)
                .packageVersion(PACKAGE_VERSION)
                .packagePath(PackageUtil.buildComponentPackagePath(BUCKET_NAME, APP_ID,
                        COMPONENT_TYPE.toString(), COMPONENT_NAME, PACKAGE_VERSION))
                .packageCreator(PACKAGE_CREATOR)
                .packageMd5(PACKAGE_MD5)
                .taskStatus(ComponentPackageTaskStateEnum.RUNNING.toString())
                .componentPackageId(0L)
                .appPackageTaskId(0L)
                .version(0)
                .envId(ENV_ID)
                .packageAddon(EMPTY_DATA)
                .packageOptions(EMPTY_DATA)
                .packageExt(EMPTY_DATA)
                .taskLog("")
                .build();
        ComponentPackageTaskDO notExpiredTask = ComponentPackageTaskDO.builder()
                .id(2L)
                .gmtCreate(new Date())
                .gmtModified(new Date())
                .appId(APP_ID)
                .componentType(COMPONENT_TYPE.toString())
                .componentName(COMPONENT_NAME)
                .packageVersion(PACKAGE_VERSION)
                .packagePath(PackageUtil.buildComponentPackagePath(BUCKET_NAME, APP_ID,
                        COMPONENT_TYPE.toString(), COMPONENT_NAME, PACKAGE_VERSION))
                .packageCreator(PACKAGE_CREATOR)
                .packageMd5(PACKAGE_MD5)
                .taskStatus(ComponentPackageTaskStateEnum.CREATED.toString())
                .componentPackageId(0L)
                .appPackageTaskId(0L)
                .version(0)
                .envId(ENV_ID)
                .packageAddon(EMPTY_DATA)
                .packageOptions(EMPTY_DATA)
                .packageExt(EMPTY_DATA)
                .taskLog("")
                .build();
        Mockito.doReturn(Pagination.valueOf(Collections.singletonList(expiredTask), Function.identity()))
                .when(componentPackageTaskService)
                .list(ComponentPackageTaskQueryCondition.builder()
                        .taskStatus(ComponentPackageTaskStateEnum.RUNNING.toString())
                        .build());
        Mockito.doReturn(Pagination.valueOf(Collections.singletonList(notExpiredTask), Function.identity()))
                .when(componentPackageTaskService)
                .list(ComponentPackageTaskQueryCondition.builder()
                        .taskStatus(ComponentPackageTaskStateEnum.CREATED.toString())
                        .build());
        Mockito.doReturn(expiredTask)
                .when(componentPackageTaskService)
                .get(ComponentPackageTaskQueryCondition.builder()
                        .id(1L)
                        .withBlobs(true)
                        .build());
        Mockito.doReturn(notExpiredTask)
                .when(componentPackageTaskService)
                .get(ComponentPackageTaskQueryCondition.builder()
                        .id(2L)
                        .withBlobs(true)
                        .build());
        buildStatusSafetyJob.execute();

        Mockito.verify(componentPackageTaskService, Mockito.times(1)).update(Mockito.any(), Mockito.any());
    }
}
