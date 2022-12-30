package com.alibaba.tesla.appmanager.server.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.api.provider.AppMetaProvider;
import com.alibaba.tesla.appmanager.api.provider.AppPackageProvider;
import com.alibaba.tesla.appmanager.api.provider.MarketProvider;
import com.alibaba.tesla.appmanager.auth.controller.AppManagerBaseController;
import com.alibaba.tesla.appmanager.common.util.NetworkUtil;
import com.alibaba.tesla.appmanager.common.util.SchemaUtil;
import com.alibaba.tesla.appmanager.domain.dto.AppPackageDTO;
import com.alibaba.tesla.appmanager.domain.dto.MarketEndpointDTO;
import com.alibaba.tesla.appmanager.domain.dto.MarketPackageDTO;
import com.alibaba.tesla.appmanager.domain.req.AppMetaUpdateReq;
import com.alibaba.tesla.appmanager.domain.req.apppackage.AppPackageImportReq;
import com.alibaba.tesla.appmanager.domain.req.apppackage.AppPackageQueryReq;
import com.alibaba.tesla.appmanager.domain.req.market.*;
import com.alibaba.tesla.appmanager.domain.schema.AppPackageSchema;
import com.alibaba.tesla.appmanager.server.storage.impl.OssStorage;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 应用市场 Controller
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
@RequestMapping("/market")
@RestController
public class MarketController extends AppManagerBaseController {

    @Autowired
    private MarketProvider marketProvider;

    @Autowired
    private AppPackageProvider appPackageProvider;

    @Autowired
    private AppMetaProvider appMetaProvider;


    @GetMapping(value = "/apps")
    public TeslaBaseResult listApp(MarketAppListReq request) {
        return buildSucceedResult(marketProvider.list(request));
    }

    @PostMapping(value = "/check")
    public TeslaBaseResult endpointCheck(
            @RequestBody MarketCheckReq request, OAuth2Authentication auth) throws IOException {
        JSONObject result = new JSONObject();
        result.put("write", false);
        result.put("read", false);

        String remotePackagePath = request.getRemotePackagePath().replace("..", "");
        String endpoint = request.getEndpoint().replace("..", "");
        String remoteBucket = request.getRemoteBucket().replace("..", "");
        if (remotePackagePath.startsWith("/")) {
            // 用户传入的路径如果以/开头，则自动适配
            remotePackagePath = remotePackagePath.substring(1);
        }

        if (StringUtils.equals(request.getEndpointType(), "oss")) {
            if (StringUtils.isNotBlank(request.getAccessKey())) {
                // 测试写入
                OssStorage client = new OssStorage(endpoint,
                        request.getAccessKey(), request.getSecretKey());
                Path tempFile = Files.createTempFile("sw", ".txt");
                String remoteFilePath = remotePackagePath + "/" + tempFile.getFileName().toString();
                client.putObject(remoteBucket, remoteFilePath, tempFile.toAbsolutePath().toString());
                boolean writeTest = client.objectExists(remoteBucket, remoteFilePath);
                result.put("write", writeTest);
                client.removeObject(remoteBucket, remoteFilePath);
                log.info("action=check|message=check oss write|endpoint={}|bucket={}|writePath={}",
                        endpoint, remoteBucket, remotePackagePath);
            }
            /**
             *  测试读取
             */
            if (StringUtils.isNotBlank(request.getAccessKey())) {
                try {
                    OssStorage client = new OssStorage(endpoint,
                            request.getAccessKey(), request.getSecretKey());
                    client.listObjects(remoteBucket, remotePackagePath);
                    result.put("read", true);
                } catch (Exception e) {
                    log.info("action=check|message=check oss read fail|{}", ExceptionUtils.getStackTrace(e));
                }
            } else {
                String swIndexUrl = "https://" + Paths.get(remoteBucket + "." + endpoint,
                        remotePackagePath, "sw-index.json");
                try {
                    File swIndexFile = Files.createTempFile("market", ".json").toFile();
                    NetworkUtil.download(swIndexUrl, swIndexFile.getAbsolutePath());
                    JSONObject.parseObject(FileUtils.readFileToString(swIndexFile, "UTF-8"));
                    result.put("read", true);
                } catch (IOException e) {
                    log.info("endpointCheck|swIndexUrl:{} return:{}", swIndexUrl, e);
                }
            }
        } else {
            buildClientErrorResult("not support endpointType " + request.getEndpointType());
        }

        return buildSucceedResult(result);
    }


    @PostMapping(value = "publish")
    @ResponseBody
    public TeslaBaseResult publish(@RequestBody MarketPublishReq request, OAuth2Authentication auth) throws IOException {

        /**
         * 根据appPackageId找到包
         */
        AppPackageDTO appPackageInfo = appPackageProvider.get(AppPackageQueryReq.builder().id(request.getAppPackageId())
                .withBlobs(true)
                .build(), getOperator(auth));

        String remoteAppId;
        if (StringUtils.isNotBlank(request.getRemoteAppId())) {
            remoteAppId = request.getRemoteAppId();
        } else {
            remoteAppId = appPackageInfo.getAppId();
        }
        String remotePackagePath = request.getRemotePackagePath().replace("..", "");
        String remoteBucket = request.getRemoteBucket().replace("..", "");
        String endpoint = request.getEndpoint().replace("..", "");
        if (remotePackagePath.startsWith("/")) {
            /**
             * 用户传入的路径如果以/开头，则自动适配
             */
            remotePackagePath = remotePackagePath.substring(1);
        }

        File appPackageLocal = marketProvider.downloadAppPackage(appPackageInfo, getOperator(auth));

        MarketPackageDTO marketPackage = marketProvider.rebuildAppPackage(
                appPackageLocal, getOperator(auth), appPackageInfo.getAppId(),
                remoteAppId, request.getRemoteSimplePackageVersion());
        marketPackage.setAppName(appPackageInfo.getAppName());
        marketPackage.setAppOptions(appPackageInfo.getAppOptions());
        marketPackage.setAppSchemaObject(SchemaUtil.toSchema(AppPackageSchema.class, appPackageInfo.getAppSchema()));

        JSONObject result = new JSONObject();
        result.put("remoteAppId", remoteAppId);
        result.put("remotePackageVersion", marketPackage.getPackageVersion());
        result.put("remoteBucket", remoteBucket);

        MarketEndpointDTO marketEndpoint = new MarketEndpointDTO();
        marketEndpoint.setEndpoint(endpoint);
        marketEndpoint.setEndpointType(request.getEndpointType());
        marketEndpoint.setAccessKey(request.getAccessKey());
        marketEndpoint.setSecretKey(request.getSecretKey());
        marketEndpoint.setRemoteBucket(remoteBucket);
        marketEndpoint.setRemotePackagePath(remotePackagePath);

        /**
         * 将包上传到远端市场
         */
        String filePath = marketProvider.uploadPackage(marketEndpoint, marketPackage);
        result.put("filePath", filePath);
        if (filePath == null) {
            return buildClientErrorResult("upload package failed");
        }

        return buildSucceedResult(result);

    }

    @GetMapping(value = "detail")
    @ResponseBody
    public TeslaBaseResult detail(MarketAppDetailReq request, OAuth2Authentication auth) throws IOException {
        JSONObject detailObject = new JSONObject();
        JSONObject appInfo = null;
        String remoteUrl = request.getRemoteUrl().replace("..", "");
        if (StringUtils.startsWith(remoteUrl, "oss://")) {
            String swIndexUrl = "https://" + Paths.get(remoteUrl.replace("oss://", ""), "sw-index.json");
            File swIndexFile = Files.createTempFile("market", ".json").toFile();
            NetworkUtil.download(swIndexUrl, swIndexFile.getAbsolutePath());
            JSONObject swIndexObject = JSONObject.parseObject(FileUtils.readFileToString(swIndexFile, "UTF-8"));
            JSONArray swIndexPackages = swIndexObject.getJSONArray("packages");
            for (int i = 0; i < swIndexPackages.size(); i++) {
                if (StringUtils.equals(swIndexPackages.getJSONObject(i).getString("appId"), request.getAppId())) {
                    appInfo = swIndexPackages.getJSONObject(i);
                    break;
                }
            }
            return buildSucceedResult(appInfo);
        }
        return buildSucceedResult(detailObject);
    }

    @GetMapping(value = "packageList")
    @ResponseBody
    public TeslaBaseResult packageList(MarketAppPackageListReq request, OAuth2Authentication auth) {
        JSONArray packageArray = new JSONArray();

        String remoteUrls = request.getRemoteUrls().replace("..", "");
        List<String> remoteUrlList = Arrays.stream(remoteUrls.split(","))
                .distinct()
                .collect(Collectors.toList());

        if (StringUtils.isNotBlank(remoteUrls)) {
            for (String remoteUrl : remoteUrlList) {
                String swIndexUrl = "https://" + Paths.get(remoteUrl.replace("oss://", ""), "sw-index.json");
                try {
                    File swIndexFile = Files.createTempFile("market", ".json").toFile();
                    NetworkUtil.download(swIndexUrl, swIndexFile.getAbsolutePath());
                    JSONObject swIndexObject = JSONObject.parseObject(FileUtils.readFileToString(swIndexFile, "UTF-8"));
                    JSONArray swIndexPackages = swIndexObject.getJSONArray("packages");
                    for (int i = 0; i < swIndexPackages.size(); i++) {
                        swIndexPackages.getJSONObject(i).put("remoteUrl", remoteUrl);
                    }
                    packageArray.addAll(swIndexPackages);
                } catch (IOException e) {
                    log.info("packageList|swIndexUrl:{} return:{}", swIndexUrl, e);
                }
            }
        }

        JSONObject result = new JSONObject();
        result.put("items", packageArray);
        return buildSucceedResult(result);
    }

    @PostMapping(value = "download")
    @ResponseBody
    public TeslaBaseResult download(
            @RequestBody MarketAppDownloadReq request, OAuth2Authentication auth) throws IOException {
        String downloadUrl;
        String remoteUrl = request.getRemoteUrl().replace("..", "");
        String packageUrl = request.getPackageUrl().replace("..", "");
        if (StringUtils.startsWith(remoteUrl, "oss://")) {
            downloadUrl = "https://" + Paths.get(remoteUrl.replace("oss://", ""),
                    URLEncoder.encode(packageUrl, StandardCharsets.UTF_8));
        } else {
            return buildClientErrorResult("not support");
        }

        File localPackageFile = Files.createTempFile("market", ".zip").toFile();
        NetworkUtil.download(downloadUrl, localPackageFile.getAbsolutePath());

        MarketPackageDTO marketPackage = marketProvider.rebuildAppPackage(
                localPackageFile, getOperator(auth), request.getAppId(), request.getLocalAppId(), null);

        File marketPackageFile = new File(marketPackage.getPackageLocalPath());
        InputStream marketPackageStream = new FileInputStream(marketPackageFile);

        AppPackageImportReq appPackageImportReq = AppPackageImportReq.builder()
                .appId(marketPackage.getAppId())
                .packageCreator(getOperator(auth))
                .packageVersion(marketPackage.getPackageVersion())
                .force(true)
                .resetVersion(false)
                .build();
        AppPackageDTO appPackageInfo = appPackageProvider
                .importPackage(appPackageImportReq, marketPackageStream, getOperator(auth));

        AppMetaUpdateReq appMetaUpdateReq = new AppMetaUpdateReq();
        appMetaUpdateReq.setAppId(marketPackage.getAppId());
        if (request.getAppOptions() != null) {
            appMetaUpdateReq.setOptions(request.getAppOptions());
        }
        appMetaProvider.save(appMetaUpdateReq, getOperator(auth));

        return buildSucceedResult(appPackageInfo);
    }
}
