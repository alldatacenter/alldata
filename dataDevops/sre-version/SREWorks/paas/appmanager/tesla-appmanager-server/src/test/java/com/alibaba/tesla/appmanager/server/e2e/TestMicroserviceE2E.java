//package com.alibaba.tesla.appmanager.server.e2e;
//
//import com.alibaba.fastjson.JSONObject;
//import com.alibaba.tesla.appmanager.api.provider.AppPackageTaskProvider;
//import com.alibaba.tesla.appmanager.api.provider.DeployAppProvider;
//import com.alibaba.tesla.appmanager.common.enums.AppPackageTaskStatusEnum;
//import com.alibaba.tesla.appmanager.common.enums.ComponentTypeEnum;
//import com.alibaba.tesla.appmanager.common.enums.DeployAppStateEnum;
//import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
//import com.alibaba.tesla.appmanager.common.exception.AppException;
//import com.alibaba.tesla.appmanager.domain.dto.AppPackageTaskDTO;
//import com.alibaba.tesla.appmanager.domain.dto.DeployAppDTO;
//import com.alibaba.tesla.appmanager.domain.req.apppackage.AppPackageTaskCreateReq;
//import com.alibaba.tesla.appmanager.domain.req.apppackage.AppPackageTaskQueryReq;
//import com.alibaba.tesla.appmanager.domain.req.apppackage.ComponentBinder;
//import com.alibaba.tesla.appmanager.domain.req.deploy.DeployAppGetReq;
//import com.alibaba.tesla.appmanager.domain.req.deploy.DeployAppLaunchReq;
//import com.alibaba.tesla.appmanager.domain.res.apppackage.AppPackageTaskCreateRes;
//import com.alibaba.tesla.appmanager.domain.res.deploy.DeployAppPackageLaunchRes;
//import com.alibaba.tesla.appmanager.server.TestApplication;
//import com.alibaba.tesla.appmanager.spring.util.FixtureUtil;
//import com.google.common.base.Enums;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.SpringBootConfiguration;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.junit4.SpringRunner;
//
//import java.util.Arrays;
//import java.util.Collections;
//
//import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
//
///**
// * 测试 Microservice 类型全流程 (构建 -> 部署 -> 状态感知)
// *
// * @author yaoxing.gyx@alibaba-inc.com
// */
//@Slf4j
//@RunWith(SpringRunner.class)
//@SpringBootTest(classes = TestApplication.class)
//@SpringBootConfiguration
//@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
//public class TestMicroserviceE2E {
//
//    private static final String OPERATOR = "122592";
//    private static final String APP_ID = "testapp";
//
//    @Autowired
//    private AppPackageTaskProvider appPackageTaskProvider;
//
//    @Autowired
//    private DeployAppProvider deployAppProvider;
//
//    @Test
//    public void run() throws Exception {
//        long appPackageId = buildStage();
//        long deployAppId = launchStage(appPackageId);
//        Thread.sleep(100000);
//    }
//
//    /**
//     * 发起服务部署
//     *
//     * @return 部署单 ID
//     */
//    private long launchStage(long appPackageId) throws Exception {
//        String ac = FixtureUtil.getFixture("application_configuration/k8s_microservice.yaml");
//        ac = ac.replace("PLACEHOLDER_APP_PACKAGE_ID", String.valueOf(appPackageId));
//        DeployAppLaunchReq request = DeployAppLaunchReq.builder()
//                .configuration(ac)
//                .autoEnvironment("false")
//                .build();
//        DeployAppPackageLaunchRes res = deployAppProvider.launch(request, OPERATOR);
//        for (int i = 0; i < 30; i++) {
//            DeployAppGetReq req = DeployAppGetReq.builder()
//                    .deployAppId(res.getDeployAppId())
//                    .build();
//            DeployAppDTO result = deployAppProvider.get(req, OPERATOR);
//            DeployAppStateEnum status = Enums.getIfPresent(DeployAppStateEnum.class, result.getDeployStatus()).orNull();
//            assertThat(status).isNotNull();
//            switch (status) {
//                case FAILURE:
//                case EXCEPTION:
//                case WAIT_FOR_OP:
//                    throw new AppException(AppErrorCode.DEPLOY_ERROR,
//                            String.format("test deploy app package failed||order=%s", JSONObject.toJSONString(result)));
//                case SUCCESS:
//                    return result.getId();
//                default:
//                    break;
//            }
//            Thread.sleep(5000);
//        }
//        throw new AppException(AppErrorCode.UNKNOWN_ERROR, "timeout applying component");
//    }
//
//    /**
//     * 发起服务构建
//     *
//     * @return 返回构建好的应用包 ID
//     */
//    private long buildStage() throws Exception {
//        // 发起服务构建
//        String serverOptions = FixtureUtil.getFixture("component_package/component_options_appmanager_python_demo.json");
//        String jobOptions = FixtureUtil.getFixture("component_package/component_options_job_test.json");
//        AppPackageTaskCreateReq request = AppPackageTaskCreateReq.builder()
//                .appId(APP_ID)
//                .tags(Collections.singletonList("unittest"))
//                .components(
//                        Arrays.asList(
//                                ComponentBinder.builder()
//                                        .componentType(ComponentTypeEnum.K8S_MICROSERVICE)
//                                        .componentName("server")
//                                        .useRawOptions(true)
//                                        .options(JSONObject.parseObject(serverOptions))
//                                        .build(),
//                                ComponentBinder.builder()
//                                        .componentType(ComponentTypeEnum.K8S_JOB)
//                                        .componentName("job")
//                                        .useRawOptions(true)
//                                        .options(JSONObject.parseObject(jobOptions))
//                                        .build()
//                        )
//                )
//                .build();
//        AppPackageTaskCreateRes task = appPackageTaskProvider.create(request, OPERATOR);
//
//        // 开始任务查询逻辑
//        for (int i = 0; i < 30; i++) {
//            AppPackageTaskDTO current = appPackageTaskProvider.get(AppPackageTaskQueryReq.builder()
//                    .appId(APP_ID)
//                    .appPackageTaskId(task.getAppPackageTaskId())
//                    .build(), OPERATOR);
//            AppPackageTaskStatusEnum status = Enums
//                    .getIfPresent(AppPackageTaskStatusEnum.class, current.getTaskStatus()).orNull();
//            assertThat(status).isNotNull();
//            assertThat(status).isNotEqualTo(AppPackageTaskStatusEnum.FAILURE);
//            if (status == AppPackageTaskStatusEnum.SUCCESS) {
//                return current.getAppPackageId();
//            } else {
//                Thread.sleep(5000);
//            }
//        }
//        throw new AppException(AppErrorCode.UNKNOWN_ERROR, "timeout creating app package");
//    }
//}