package com.alibaba.tesla.appmanager.server.provider.impl;

import com.alibaba.tesla.appmanager.api.provider.FlowManagerProvider;
import com.alibaba.tesla.appmanager.autoconfig.PackageProperties;
import com.alibaba.tesla.appmanager.server.storage.Storage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * Flow 管理服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class FlowManagerProviderImpl implements FlowManagerProvider {

    @Autowired
    private Storage storage;

    @Autowired
    private PackageProperties packageProperties;

    @Value("${dag.hot.load.jar.oss.bucket.name:}")
    private String dagOssBucketName;

    @Value("${dag.hot.load.jar.minio.bucket.name:}")
    private String dagMinioBucketName;

    /**
     * 升级指定文件到远端目录中
     *
     * @param inputStream 文件流
     * @param operator    操作人
     */
    public void upgrade(InputStream inputStream, String operator) {
        String bucketName = getDagBucketName();
        String objectName = packageProperties.getFlowName();
        storage.putObject(bucketName, objectName, inputStream);
        log.info("dag jar has put into bucket {}|operator={}", bucketName, operator);
    }
//
//    /**
//     * 同步当前 Flow Jar 到外部环境中
//     *
//     * @param req      请求
//     * @param operator 操作人
//     */
//    @Override
//    public void syncToExternal(FlowManagerSyncExternalReq req, String operator) throws IOException, URISyntaxException {
//        String endpoint = req.getEndpoint();
//        OkHttpClient httpClient;
//        if (StringUtils.isNotEmpty(req.getProxyIp()) && req.getProxyPort() > 0) {
//            httpClient = HttpClientFactory.getHttpClient(req.getProxyIp(), req.getProxyPort());
//        } else {
//            httpClient = HttpClientFactory.getHttpClient();
//        }
//        String bucketName = getDagBucketName();
//        String objectName = packageProperties.getFlowName();
//        Path tempFile = Files.createTempFile(null, null);
//        try {
//            String url = storage.getObjectUrl(bucketName, objectName, DefaultConstant.DEFAULT_FILE_EXPIRATION);
//            FileUtils.copyURLToFile(new URL(url), tempFile.toFile());
//            log.info("currnet flow jar has download to local|url={}|tempFilePath={}", url, tempFile);
//
//            // 发送请求到对应代理环境
//            RequestBody requestBody = new MultipartBody.Builder()
//                .setType(MultipartBody.FORM)
//                .addFormDataPart("file", packageProperties.getFlowName(),
//                    RequestBody.create(MediaType.parse("application/octet-stream"), tempFile.toFile()))
//                .build();
//            Request request = new Request.Builder()
//                .url(NetworkUtil.concatenate(new URL(endpoint), "flowManager/upgrade"))
//                .header("X-EmpId", "EXTERNAL")
//                .post(requestBody)
//                .build();
//            NetworkUtil.sendRequest(httpClient, request);
//        } catch (Exception e) {
//            Files.deleteIfExists(tempFile);
//            if (e instanceof AppException) {
//                throw e;
//            }
//            throw new AppException(AppErrorCode.DEPLOY_ERROR, String.format("cannot sync to external environment, " +
//                "exception=%s", ExceptionUtils.getStackTrace(e)));
//        }
//    }

    /**
     * 获取当前 DAG 的 Bucket 名称
     *
     * @return oss or minio
     */
    private String getDagBucketName() {
        if (!StringUtils.isEmpty(dagOssBucketName)) {
            return dagOssBucketName;
        } else {
            return dagMinioBucketName;
        }
    }
}
