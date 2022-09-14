package com.alibaba.tesla.appmanager.server.service.componentwatchcron;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.tesla.appmanager.autoconfig.ThreadPoolProperties;
import com.alibaba.tesla.appmanager.common.enums.ComponentInstanceStatusEnum;
import com.alibaba.tesla.appmanager.common.enums.DynamicScriptKindEnum;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.common.util.InstanceIdUtil;
import com.alibaba.tesla.appmanager.common.util.VersionUtil;
import com.alibaba.tesla.appmanager.domain.res.rtcomponentinstance.RtComponentInstanceGetStatusRes;
import com.alibaba.tesla.appmanager.dynamicscript.core.GroovyHandlerFactory;
import com.alibaba.tesla.appmanager.server.dynamicscript.handler.ComponentHandler;
import com.alibaba.tesla.appmanager.server.dynamicscript.handler.ComponentWatchCronHandler;
import com.alibaba.tesla.appmanager.server.repository.condition.RtComponentInstanceQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.RtComponentInstanceDO;
import com.alibaba.tesla.appmanager.server.service.rtcomponentinstance.RtComponentInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 测试 component instance 的状态变更
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@RunWith(SpringRunner.class)
@Slf4j
public class TestServiceComponentPackageWatchCronStatusChange {

    private static final String COMPONENT_TYPE = "ABM_CHART";
    private static final String WATCH_SCRIPT_NAME = "default_watch";
    private static final String UNSTABLE_APP_ID = "UNSTABLE_APP_ID";
    private static final String STABLE_APP_ID = "STABLE_APP_ID";
    private static final String CLUSTER_ID = "testcluster";
    private static final String NAMESPACE_ID = "testnamespace";
    private static final String STAGE_ID = "teststage";

    @Mock
    private GroovyHandlerFactory groovyHandlerFactory;

    @Mock
    private RtComponentInstanceService rtComponentInstanceService;

    private ComponentWatchCronManager componentWatchCronManager;

    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);

        // Mock ComponentHandler
        Mockito.doReturn(new ComponentHandler() {
                    @Override
                    public String buildScriptName() {
                        return null;
                    }

                    @Override
                    public String deployScriptName() {
                        return null;
                    }

                    @Override
                    public String destroyName() {
                        return null;
                    }

                    @Override
                    public String watchKind() {
                        return ComponentWatchCronManager.WATCH_KIND;
                    }

                    @Override
                    public String watchScriptName() {
                        return WATCH_SCRIPT_NAME;
                    }
                })
                .when(groovyHandlerFactory)
                .get(ComponentHandler.class, DynamicScriptKindEnum.COMPONENT.toString(), COMPONENT_TYPE);

        // Mock ComponentWatchCronHandler
        Mockito.doReturn((ComponentWatchCronHandler) request -> {
                    if (request.getAppId().equals(UNSTABLE_APP_ID)) {
                        return RtComponentInstanceGetStatusRes.builder()
                                .status(ComponentInstanceStatusEnum.UPDATING.toString())
                                .conditions(new JSONArray())
                                .build();
                    } else if (request.getAppId().equals(STABLE_APP_ID)) {
                        return RtComponentInstanceGetStatusRes.builder()
                                .status(ComponentInstanceStatusEnum.RUNNING.toString())
                                .conditions(new JSONArray())
                                .build();
                    } else {
                        throw new AppException(AppErrorCode.INVALID_USER_ARGS, "invalid app id for test");
                    }
                })
                .when(groovyHandlerFactory)
                .get(ComponentWatchCronHandler.class,
                        DynamicScriptKindEnum.COMPONENT_WATCH_CRON.toString(), WATCH_SCRIPT_NAME);

        // 初始化 component watch cron manager
        componentWatchCronManager = Mockito.spy(new ComponentWatchCronManager(
                rtComponentInstanceService,
                groovyHandlerFactory,
                new ThreadPoolProperties()
        ));
        componentWatchCronManager.init();
    }

    /**
     * 测试 component instance 状态变更 (状态稳定变迁, RUNNING -> RUNNING)
     */
    @Test
    public void testWithStableComponentInstanceStatusChange() throws Exception {
        String appInstanceId = InstanceIdUtil.genAppInstanceId(STABLE_APP_ID, CLUSTER_ID, NAMESPACE_ID, STAGE_ID);
        String componentInstanceId = InstanceIdUtil.genComponentInstanceId();
        RtComponentInstanceDO componentInstance = RtComponentInstanceDO.builder()
                .appInstanceId(appInstanceId)
                .componentInstanceId(componentInstanceId)
                .clusterId(CLUSTER_ID)
                .namespaceId(NAMESPACE_ID)
                .stageId(STAGE_ID)
                .appId(STABLE_APP_ID)
                .watchKind(ComponentWatchCronManager.WATCH_KIND)
                .componentType(COMPONENT_TYPE)
                .componentName("testname")
                .times(1L)
                .status(ComponentInstanceStatusEnum.RUNNING.toString())
                .version(VersionUtil.buildNextPatch())
                .build();
        Mockito.doReturn(Pagination.valueOf(List.of(componentInstance), Function.identity()))
                .when(rtComponentInstanceService)
                .list(RtComponentInstanceQueryCondition.builder()
                        .statusList(ComponentWatchCronManager.RUNNING_STATUS_LIST)
                        .watchKind(ComponentWatchCronManager.WATCH_KIND)
                        .timesGreaterThan(0L)
                        .timesLessThan(ComponentWatchCronManager.BORDER_TIMES_5S)
                        .build());
        componentWatchCronManager.refresh5s();
        componentWatchCronManager.refresh10s();
        componentWatchCronManager.refresh30s();
        componentWatchCronManager.refresh1m();
        componentWatchCronManager.refresh2m();
        componentWatchCronManager.refresh3m();
        componentWatchCronManager.refresh4m();
        componentWatchCronManager.refresh5m();

        // 检查上报数据
        ArgumentCaptor<RtComponentInstanceDO> arg = ArgumentCaptor.forClass(RtComponentInstanceDO.class);
        Mockito.verify(rtComponentInstanceService).reportRaw(arg.capture());
        assertThat(arg.getValue().getTimes()).isEqualTo(2L);  // +1
        assertThat(arg.getValue().getClusterId()).isEqualTo(CLUSTER_ID);
        assertThat(arg.getValue().getNamespaceId()).isEqualTo(NAMESPACE_ID);
        assertThat(arg.getValue().getStageId()).isEqualTo(STAGE_ID);
        assertThat(arg.getValue().getAppId()).isEqualTo(STABLE_APP_ID);
        assertThat(arg.getValue().getStatus()).isEqualTo(ComponentInstanceStatusEnum.RUNNING.toString());
        assertThat(arg.getValue().getWatchKind()).isEqualTo(ComponentWatchCronManager.WATCH_KIND);
        assertThat(arg.getValue().getComponentType()).isEqualTo(COMPONENT_TYPE);
    }

    /**
     * 测试 component instance 状态变更（不稳定变迁）RUNNING -> UPDATING
     */
    @Test
    public void testWithUnstableComponentInstanceStatusChange() throws Exception {
        String appInstanceId = InstanceIdUtil.genAppInstanceId(UNSTABLE_APP_ID, CLUSTER_ID, NAMESPACE_ID, STAGE_ID);
        String componentInstanceId = InstanceIdUtil.genComponentInstanceId();
        RtComponentInstanceDO componentInstance = RtComponentInstanceDO.builder()
                .appInstanceId(appInstanceId)
                .componentInstanceId(componentInstanceId)
                .clusterId(CLUSTER_ID)
                .namespaceId(NAMESPACE_ID)
                .stageId(STAGE_ID)
                .appId(UNSTABLE_APP_ID)
                .watchKind(ComponentWatchCronManager.WATCH_KIND)
                .componentType(COMPONENT_TYPE)
                .componentName("testname")
                .times(1L)
                .status(ComponentInstanceStatusEnum.RUNNING.toString())
                .version(VersionUtil.buildNextPatch())
                .build();
        Mockito.doReturn(Pagination.valueOf(List.of(componentInstance), Function.identity()))
                .when(rtComponentInstanceService)
                .list(RtComponentInstanceQueryCondition.builder()
                        .statusList(ComponentWatchCronManager.RUNNING_STATUS_LIST)
                        .watchKind(ComponentWatchCronManager.WATCH_KIND)
                        .timesGreaterThan(0L)
                        .timesLessThan(ComponentWatchCronManager.BORDER_TIMES_5S)
                        .build());
        componentWatchCronManager.refresh5s();

        // 检查上报数据
        ArgumentCaptor<RtComponentInstanceDO> arg = ArgumentCaptor.forClass(RtComponentInstanceDO.class);
        Mockito.verify(rtComponentInstanceService).reportRaw(arg.capture());
        assertThat(arg.getValue().getTimes()).isEqualTo(0L);  // 重置
        assertThat(arg.getValue().getClusterId()).isEqualTo(CLUSTER_ID);
        assertThat(arg.getValue().getNamespaceId()).isEqualTo(NAMESPACE_ID);
        assertThat(arg.getValue().getStageId()).isEqualTo(STAGE_ID);
        assertThat(arg.getValue().getAppId()).isEqualTo(UNSTABLE_APP_ID);
        assertThat(arg.getValue().getStatus()).isEqualTo(ComponentInstanceStatusEnum.UPDATING.toString());
        assertThat(arg.getValue().getWatchKind()).isEqualTo(ComponentWatchCronManager.WATCH_KIND);
        assertThat(arg.getValue().getComponentType()).isEqualTo(COMPONENT_TYPE);
    }

    /**
     * 测试 component instance 状态变更（不稳定变迁）UPDATING -> UPDATING
     */
    @Test
    public void testWithUnstableComponentInstanceStatusNotChange() throws Exception {
        String appInstanceId = InstanceIdUtil.genAppInstanceId(UNSTABLE_APP_ID, CLUSTER_ID, NAMESPACE_ID, STAGE_ID);
        String componentInstanceId = InstanceIdUtil.genComponentInstanceId();
        RtComponentInstanceDO componentInstance = RtComponentInstanceDO.builder()
                .appInstanceId(appInstanceId)
                .componentInstanceId(componentInstanceId)
                .clusterId(CLUSTER_ID)
                .namespaceId(NAMESPACE_ID)
                .stageId(STAGE_ID)
                .appId(UNSTABLE_APP_ID)
                .watchKind(ComponentWatchCronManager.WATCH_KIND)
                .componentType(COMPONENT_TYPE)
                .componentName("testname")
                .times(1L)
                .status(ComponentInstanceStatusEnum.UPDATING.toString())
                .version(VersionUtil.buildNextPatch())
                .build();
        Mockito.doReturn(Pagination.valueOf(List.of(componentInstance), Function.identity()))
                .when(rtComponentInstanceService)
                .list(RtComponentInstanceQueryCondition.builder()
                        .statusList(ComponentWatchCronManager.RUNNING_STATUS_LIST)
                        .watchKind(ComponentWatchCronManager.WATCH_KIND)
                        .timesGreaterThan(0L)
                        .timesLessThan(ComponentWatchCronManager.BORDER_TIMES_5S)
                        .build());
        componentWatchCronManager.refresh5s();

        // 检查上报数据
        ArgumentCaptor<RtComponentInstanceDO> arg = ArgumentCaptor.forClass(RtComponentInstanceDO.class);
        Mockito.verify(rtComponentInstanceService).reportRaw(arg.capture());
        assertThat(arg.getValue().getTimes()).isEqualTo(2L);  // +1
        assertThat(arg.getValue().getClusterId()).isEqualTo(CLUSTER_ID);
        assertThat(arg.getValue().getNamespaceId()).isEqualTo(NAMESPACE_ID);
        assertThat(arg.getValue().getStageId()).isEqualTo(STAGE_ID);
        assertThat(arg.getValue().getAppId()).isEqualTo(UNSTABLE_APP_ID);
        assertThat(arg.getValue().getStatus()).isEqualTo(ComponentInstanceStatusEnum.UPDATING.toString());
        assertThat(arg.getValue().getWatchKind()).isEqualTo(ComponentWatchCronManager.WATCH_KIND);
        assertThat(arg.getValue().getComponentType()).isEqualTo(COMPONENT_TYPE);
    }
}
