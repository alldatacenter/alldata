package com.alibaba.tesla.appmanager.trait.plugin;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.domain.core.WorkloadResource;
import com.alibaba.tesla.appmanager.domain.schema.TraitDefinition;
import com.alibaba.tesla.appmanager.trait.BaseTrait;
import com.alibaba.tesla.appmanager.trait.plugin.util.JsonUtil;
import com.alibaba.tesla.appmanager.trait.plugin.util.RequestsUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.alibaba.tesla.appmanager.common.util.StringUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GatewayTrait extends BaseTrait {

    public GatewayTrait(String name, TraitDefinition traitDefinition, JSONObject spec, WorkloadResource ref) {
        super(name, traitDefinition, spec, ref);
    }

    // traits:
    // - name: gateway.trait.abm.io
    //   runtime: post
    //   spec:
    //     path: "/sreworks-job/**",
    //     servicePort: 81,
    //     serviceName: prod-job-job-master

    @Override
    public void execute() {

        /*
          1. get metadata from workload
         */
        log.info("start execute gateway trait {}", getSpec().toJSONString());

        WorkloadResource workloadResource = getWorkloadRef();
        String name = workloadResource.getMetadata().getName();
        String path = getSpec().getString("path");
        String stageId = getComponent().getStageId();
        String namespaceId = getComponent().getNamespaceId();
        String clusterId = getComponent().getClusterId();
        String gatewayEndpoint = getSpec().getString("gatewayEndpoint");
        Integer order = 2500;
        if (StringUtils.isEmpty(gatewayEndpoint)){
            gatewayEndpoint = "http://prod-flycore-paas-gateway";
        }
        if (StringUtils.isEmpty(stageId)){
            stageId = "dev";
        }
        if (StringUtils.isEmpty(namespaceId)){
            namespaceId = "sreworks";
        }
        String serviceName = getSpec().getString("serviceName");
        if (StringUtils.isEmpty(serviceName)){
            serviceName = name;
        }
        int servicePort = getSpec().getIntValue("servicePort");
        if (servicePort == 0) {
            servicePort = 80;
        }
        String routeId = String.format("%s-%s-%s-%s", name, clusterId, namespaceId, stageId);
        if (!StringUtils.isEmpty(getSpec().getString("routeId"))) {
            routeId = getSpec().getString("routeId");
        }
        if (getSpec().getInteger("order") != null){
            order = getSpec().getInteger("order");
        }
        boolean authEnabled = true;
        if (getSpec().getBoolean("authEnabled") != null ){
            authEnabled = getSpec().getBoolean("authEnabled");
        }
        /*
          2. apply route config to gateway
         */
        try {
            for (int i = 0; i < 300; i++) {
                try {
                    JSONObject applyResult = applyGatewayRoute(routeId, stageId, gatewayEndpoint, path, serviceName, servicePort, order, authEnabled);
                    log.info("apply gateway conf {}", applyResult.toJSONString());
                }catch (Exception throwable) {
                    if (i == 299) {
                        throw throwable;
                    }
                    log.info("apply gateway conf failed, wait next try {}", getSpec().toJSONString());
                    TimeUnit.MILLISECONDS.sleep(5000);
                }
                break;
            }
        } catch (Exception e) {
            log.error("apply gateway failed {}", ExceptionUtils.getStackTrace(e));
            throw new AppException(AppErrorCode.DEPLOY_ERROR, "apply gateway failed");
        }

        try {
            for (int i = 0; i < 3; i++) {
                if (checkGatewayRoute(routeId, stageId, gatewayEndpoint, path, serviceName, servicePort, order, authEnabled)) {
                    break;
                }
                if (i == 2){
                    throw new AppException(AppErrorCode.DEPLOY_ERROR, "check apply gateway not pass");
                }
                TimeUnit.MILLISECONDS.sleep(5000);
            }
        } catch ( Exception e) {
            log.error("check gateway failed {}", ExceptionUtils.getStackTrace(e));
            throw new AppException(AppErrorCode.DEPLOY_ERROR, "check apply gateway failed");
        }

    }

    private String getAuthPasswdHash(String username, String password){

        // key = "%(user_name)s%(local_time)s%(passwd)s" % {
        //        'user_name': user_name,
        //        'local_time': time.strftime('%Y%m%d', time.localtime(time.time())),
        //        'passwd': user_passwd }
        // m = hashlib.md5()
        // m.update(key)
        // return m.hexdigest()
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
        String key = String.format("%s%s%s", username, formatter.format(new Date()), password);

//        log.info("getAuthPasswdHash origin string {}", key);

        return StringUtil.md5sum(key);
    }

    /**
     * 校验gateway路由
     *
     * @param path      路由路径
     * @param serviceName    服务名称
     * @param servicePort     服务端口
     * @return boolean
     * */

    private boolean checkGatewayRoute(String routeId, String stageId, String gatewayEndpoint, String path, String serviceName, int servicePort, int order, boolean authEnabled) throws Exception {
        String username = System.getenv("ACCOUNT_SUPER_ID");
        String password = System.getenv("ACCOUNT_SUPER_SECRET_KEY");
        String clientId = System.getenv("ACCOUNT_SUPER_CLIENT_ID");
        String clientSecret = System.getenv("ACCOUNT_SUPER_CLIENT_SECRET");

        String gatewayRouteApi = String.format("%s/v2/common/gateway/route/%s", gatewayEndpoint, routeId);
        String url = String.format("http://%s:%s/", serviceName, servicePort);

        JSONObject resp = new RequestsUtil(gatewayRouteApi).headers(
                "x-auth-app", clientId,
                "x-auth-key", clientSecret,
                "x-auth-user", username,
                "x-auth-passwd", getAuthPasswdHash(username, password)
        ).get().isSuccessful()
                .getJSONObject();
        log.info("gateway check route:{} {}", gatewayRouteApi, resp.toJSONString());
        if(resp.getInteger("code") == 200 && resp.getJSONObject("data") != null){
            if(!resp.getJSONObject("data").getString("routeId").equals(routeId)){
                log.info("gateway check route [routeId] not pass:{} ", resp.getJSONObject("data").toJSONString());
                return false;
            }
            if(!resp.getJSONObject("data").getString("path").equals(path)){
                log.info("gateway check route [path] not pass:{} ", resp.getJSONObject("data").toJSONString());
                return false;
            }
            if(!resp.getJSONObject("data").getString("url").equals(url)){
                log.info("gateway check route [url] not pass:{} ", resp.getJSONObject("data").toJSONString());
                return false;
            }
            if(resp.getJSONObject("data").getString("stageId") == null){
                if(!StringUtils.equals(stageId, "prod")){
                    log.info("gateway check route [noStageId] not pass:{} ", resp.getJSONObject("data").toJSONString());
                }
            } else if (!resp.getJSONObject("data").getString("stageId").equals(stageId)){
                log.info("gateway check route [stageId] not pass:{} ", resp.getJSONObject("data").toJSONString());
                return false;
            }
            if(resp.getJSONObject("data").getInteger("order") != order){
                log.info("gateway check route [order] not pass:{} ", resp.getJSONObject("data").toJSONString());
                return false;
            }
            if(resp.getJSONObject("data").getBoolean("authCheck") != authEnabled){
                log.info("gateway check route [authCheck] not pass:{} ", resp.getJSONObject("data").toJSONString());
                return false;
            }
            return true;
        }else {
            log.info("gateway check route not exist:{} {}", gatewayRouteApi, resp.toJSONString());
            return false;
        }
    }

    /**
     * 推送gateway路由
     *
     * @param path      路由路径
     * @param serviceName    服务名称
     * @param servicePort     服务端口
     * @return JSONObject
     */
    private JSONObject applyGatewayRoute(String routeId, String stageId, String gatewayEndpoint, String path, String serviceName, int servicePort, int order, boolean authEnabled) throws Exception {

        String username = System.getenv("ACCOUNT_SUPER_ID");
        String password = System.getenv("ACCOUNT_SUPER_SECRET_KEY");
        String clientId = System.getenv("ACCOUNT_SUPER_CLIENT_ID");
        String clientSecret = System.getenv("ACCOUNT_SUPER_CLIENT_SECRET");

        String gatewayRouteApi = String.format("%s/v2/common/gateway/route/%s", gatewayEndpoint, routeId);
        String gatewayInsertApi = String.format("%s/v2/common/gateway/route", gatewayEndpoint);

        String mode = "insert";

        JSONObject resp = new RequestsUtil(gatewayRouteApi).headers(
            "x-auth-app", clientId,
            "x-auth-key", clientSecret,
            "x-auth-user", username,
            "x-auth-passwd", getAuthPasswdHash(username, password)
        ).get().isSuccessful()
        .getJSONObject();
        log.info("gateway check route:{} {}", gatewayRouteApi, resp.toJSONString());
        if(resp.getInteger("code") == 200 && resp.getJSONObject("data") != null){
            mode = "update";
        }

        JSONObject routeJson = JsonUtil.map(
                "appId", routeId,
                "authCheck", authEnabled,
                "authHeader", authEnabled,
                "authLogin", authEnabled,
                "enable", true,
                "enableFunction", false,
                "enableSwaggerDoc", false,
                "name", "health",
                "path", path,
                "routeId", routeId,
                "routeType", "PATH",
                "serverType", "PAAS",
//                "stageId", stageId,
                "order", order,
                "url", String.format("http://%s:%s/", serviceName, servicePort)
        );
        // 当前stageId=prod时候，不增加stageId，作为默认路由
        if (!StringUtils.equals(stageId, "prod")){
            routeJson.put("stageId", stageId);
        }
        String authPasswd = getAuthPasswdHash(username, password);
        log.info("gateway routeJson: {}", routeJson.toJSONString());

        if (mode.equals("insert")){

            return new RequestsUtil(gatewayInsertApi)
                    .postJson(routeJson)
                    .headers(
                        "x-auth-app", clientId,
                        "x-auth-key", clientSecret,
                        "x-auth-user", username,
                        "x-auth-passwd", authPasswd
                    )
                    .post().isSuccessful()
                    .getJSONObject();

        }else {

            return new RequestsUtil(gatewayRouteApi)
                    .headers(
                            "x-auth-app", clientId,
                            "x-auth-key", clientSecret,
                            "x-auth-user", username,
                            "x-auth-passwd", authPasswd
                    )
                    .postJson(routeJson)
                    .put().isSuccessful()
                    .getJSONObject();
        }
    }
}
