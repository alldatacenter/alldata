package com.alibaba.tesla.appmanager.server.service.unit.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.common.util.NetworkUtil;
import com.alibaba.tesla.appmanager.domain.core.StorageFile;
import com.alibaba.tesla.appmanager.domain.req.deploy.DeployAppGetReq;
import com.alibaba.tesla.appmanager.domain.req.deploy.DeployAppLaunchReq;
import com.alibaba.tesla.appmanager.server.repository.UnitRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AppPackageQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.UnitQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageDO;
import com.alibaba.tesla.appmanager.server.repository.domain.UnitDO;
import com.alibaba.tesla.appmanager.server.service.apppackage.AppPackageService;
import com.alibaba.tesla.appmanager.server.service.unit.UnitService;
import com.alibaba.tesla.appmanager.server.service.unit.helper.UnitHttpHelper;
import com.alibaba.tesla.appmanager.server.storage.Storage;
import com.google.common.io.ByteStreams;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 单元服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class UnitServiceImpl implements UnitService {

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private AppPackageService appPackageService;

    @Autowired
    private Storage storage;

    /**
     * 同步指定包到目标单元中的环境
     *
     * @param unitId       单元 ID
     * @param appPackageId 应用包 ID
     * @return
     */
    @Override
    public JSONObject syncRemote(String unitId, Long appPackageId) throws IOException, URISyntaxException {
        // 获取目标单元信息
        UnitDO unit = get(UnitQueryCondition.builder().unitId(unitId).build());
        if (unit == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "the unit you provided does not exists");
        }
        OkHttpClient httpClient = UnitHttpHelper.getHttpClient(unit);
        String authToken = UnitHttpHelper.getAuthToken(unit, httpClient);

        // 下载应用包到本地
        AppPackageDO appPackage = appPackageService.get(AppPackageQueryCondition.builder().id(appPackageId).build());
        if (appPackage == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "cannot find app package by appPackageId");
        }
        String packagePath = appPackage.getPackagePath();
        if (StringUtils.isEmpty(packagePath)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "cannot find package path by appPackageId");
        }
        StorageFile storageFile = new StorageFile(packagePath);
        String bucketName = storageFile.getBucketName();
        String objectName = storageFile.getObjectName();
        String url = storage.getObjectUrl(bucketName, objectName, DefaultConstant.DEFAULT_FILE_EXPIRATION);
        Path tempFile = Files.createTempFile("remote_package", ".zip");
        NetworkUtil.download(url, tempFile.toFile().getAbsolutePath());
        log.info("app package has downloaded into local filesystem|url={}|file={}",
                url, tempFile.toFile().getAbsolutePath());

        // 同步到远端单元
        String appId = appPackage.getAppId();
        String urlPrefix = NetworkUtil.concatenateStr(unit.getEndpoint(),
                String.format("apps/%s/app-packages/import", appId));
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(urlPrefix)).newBuilder();
        urlBuilder.addQueryParameter("appId", appId);
        urlBuilder.addQueryParameter("packageVersion", appPackage.getPackageVersion());
        urlBuilder.addQueryParameter("packageCreator", appPackage.getPackageCreator());
        urlBuilder.addQueryParameter("force", "true");
        urlBuilder.addQueryParameter("resetVersion", "false");
        byte[] data = FileUtils.readFileToByteArray(tempFile.toFile());
        RequestBody body = RequestBody.create(data, MediaType.parse("application/octet-stream"));
        Request.Builder requestBuilder = new Request.Builder()
                .url(urlBuilder.build())
                .addHeader("Content-type", "application/octet-stream")
                .post(body);
        return NetworkUtil.sendRequest(httpClient, requestBuilder, authToken);
    }

    /**
     * 启动单元环境部署
     *
     * @param unitId    单元 ID
     * @param launchReq 实际部署请求
     * @return
     */
    @Override
    public JSONObject launchDeployment(String unitId, DeployAppLaunchReq launchReq) {
        // 获取目标单元信息
        UnitDO unit = get(UnitQueryCondition.builder().unitId(unitId).build());
        if (unit == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "the unit you provided does not exists");
        }
        OkHttpClient httpClient = UnitHttpHelper.getHttpClient(unit);
        String authToken = UnitHttpHelper.getAuthToken(unit, httpClient);

        // 发起部署请求
        String urlPrefix = NetworkUtil.concatenateStr(unit.getEndpoint(), "deployments/launch");
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(urlPrefix)).newBuilder();
        urlBuilder.addQueryParameter("autoEnvironment", launchReq.getAutoEnvironment());
        RequestBody body = RequestBody.create(launchReq.getConfiguration(), MediaType.parse("application/x-yaml"));
        HttpUrl finalUrl = urlBuilder.build();
        try {
            Request.Builder requestBuilder = new Request.Builder().url(finalUrl).post(body);
            JSONObject response = NetworkUtil.sendRequest(httpClient, requestBuilder, authToken);
            return response.getJSONObject("data");
        } catch (Exception e) {
            throw new AppException(AppErrorCode.NETWORK_ERROR,
                    String.format("cannot send request to unit|unitId=%s|url=%s|exception=%s",
                            unitId, finalUrl, ExceptionUtils.getStackTrace(e)));
        }
    }

    /**
     * 查询单元环境部署详情
     *
     * @param unitId 但愿 ID
     * @param getReq 查询请求
     * @return
     */
    @Override
    public JSONObject getDeployment(String unitId, DeployAppGetReq getReq) {
        // 获取目标单元信息
        UnitDO unit = get(UnitQueryCondition.builder().unitId(unitId).build());
        if (unit == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "the unit you provided does not exists");
        }
        OkHttpClient httpClient = UnitHttpHelper.getHttpClient(unit);
        String authToken = UnitHttpHelper.getAuthToken(unit, httpClient);

        // 发起部署请求
        String urlPrefix = NetworkUtil.concatenateStr(unit.getEndpoint(),
                String.format("deployments/%d", getReq.getDeployAppId()));
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(urlPrefix)).newBuilder();
        HttpUrl finalUrl = urlBuilder.build();
        try {
            Request.Builder requestBuilder = new Request.Builder().url(finalUrl).get();
            JSONObject response = NetworkUtil.sendRequest(httpClient, requestBuilder, authToken);
            return response.getJSONObject("data");
        } catch (Exception e) {
            throw new AppException(AppErrorCode.NETWORK_ERROR,
                    String.format("cannot send request to unit|unitId=%s|url=%s|exception=%s",
                            unitId, finalUrl, ExceptionUtils.getStackTrace(e)));
        }
    }

    /**
     * 通用单元 HTTP 请求转发
     * <p>
     * 要求 request 中的 URL 必须遵循 /units/{unitId}/? 的形式
     *
     * @param unitId     目标单元 ID
     * @param method     Http 方法
     * @param requestUri Request URI (目标单元)
     * @param request    Http 请求
     * @param response   Http 响应
     */
    @Override
    public void proxy(
            String unitId, String method, String requestUri, HttpServletRequest request, HttpServletResponse response) {
        UnitDO unit = get(UnitQueryCondition.builder().unitId(unitId).build());
        if (unit == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "the unit you provided does not exists");
        }
        OkHttpClient httpClient = UnitHttpHelper.getHttpClient(unit);
        String authToken = UnitHttpHelper.getAuthToken(unit, httpClient);
        String urlPrefix = NetworkUtil.concatenateStr(unit.getEndpoint(), requestUri);
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(urlPrefix)).newBuilder();
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (parameterMap != null) {
            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                urlBuilder.addQueryParameter(entry.getKey(), String.join(",", entry.getValue()));
            }
        }
        HttpUrl finalUrl = urlBuilder.build();
        try {
            byte[] bodyBytes = ByteStreams.toByteArray(request.getInputStream());
            Request.Builder requestBuilder = new Request.Builder()
                    .url(finalUrl);
            if ("GET".equals(method)) {
                requestBuilder = requestBuilder.get();
            } else if ("DELETE".equals(method)) {
                requestBuilder = requestBuilder.delete();
            } else {
                requestBuilder = requestBuilder.method(method,
                        RequestBody.create(bodyBytes, MediaType.parse(request.getContentType())));
            }
            Response proxyResponse = NetworkUtil.sendRequestSimple(httpClient, requestBuilder, authToken);
            byte[] proxyResponseBody = Objects.requireNonNull(proxyResponse.body()).bytes();
            response.setStatus(proxyResponse.code());
            response.getOutputStream().write(proxyResponseBody);
        } catch (Exception e) {
            throw new AppException(AppErrorCode.NETWORK_ERROR,
                    String.format("cannot send request to unit|unitId=%s|url=%s|exception=%s",
                            unitId, finalUrl, ExceptionUtils.getStackTrace(e)));
        }
    }

    /**
     * 根据查询条件获取指定的单元信息
     *
     * @param condition 查询条件
     * @return 单元信息 DO
     */
    @Override
    public UnitDO get(UnitQueryCondition condition) {
        List<UnitDO> units = unitRepository.selectByCondition(condition);
        if (CollectionUtils.isEmpty(units)) {
            return null;
        }
        if (units.size() > 1) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("multiple units found, condition=%s", JSONObject.toJSONString(condition)));
        }
        return units.get(0);
    }

    /**
     * 根据条件过滤单元信息
     *
     * @param condition 过滤条件
     * @return List
     */
    @Override
    public Pagination<UnitDO> list(UnitQueryCondition condition) {
        List<UnitDO> unitList = unitRepository.selectByCondition(condition);
        if (CollectionUtils.isEmpty(unitList)) {
            return new Pagination<>();
        }
        return Pagination.valueOf(unitList, unit -> unit);
    }

    /**
     * 创建单元
     *
     * @param unit 单元对象
     */
    @Override
    public UnitDO create(UnitDO unit) {
        if (checkExist(unit.getUnitId())) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("the specified unitId %s already exists", unit.getUnitId()));
        }
        unitRepository.insert(unit);
        return get(UnitQueryCondition.builder().unitId(unit.getUnitId()).build());
    }

    /**
     * 更新单元
     *
     * @param unit 单元对象
     */
    @Override
    public UnitDO update(UnitQueryCondition condition, UnitDO unit) {
        int count = unitRepository.updateByCondition(unit, condition);
        if (count != 1) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("update unit failed, updated %d records, condition=%s",
                            count, JSONObject.toJSONString(condition)));
        }
        return get(condition);
    }

    /**
     * 删除单元
     *
     * @param condition 查询条件
     */
    @Override
    public int delete(UnitQueryCondition condition) {
        return unitRepository.deleteByCondition(condition);
    }

    /**
     * 检测指定 unitId 是否存在对应记录
     *
     * @param unitId 定位 UnitId
     * @return true or false
     */
    private boolean checkExist(String unitId) {
        UnitDO unitDO = get(UnitQueryCondition.builder().unitId(unitId).build());
        return unitDO != null;
    }
}
