package com.alibaba.tesla.appmanager.server.provider.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.tesla.appmanager.api.provider.AppPackageProvider;
import com.alibaba.tesla.appmanager.api.provider.MarketProvider;
import com.alibaba.tesla.appmanager.common.constants.DefaultConstant;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.common.util.*;
import com.alibaba.tesla.appmanager.domain.dto.*;
import com.alibaba.tesla.appmanager.domain.req.apppackage.AppPackageVersionCountReq;
import com.alibaba.tesla.appmanager.domain.req.market.MarketAppListReq;
import com.alibaba.tesla.appmanager.domain.res.apppackage.AppPackageUrlRes;
import com.alibaba.tesla.appmanager.domain.schema.AppPackageSchema;
import com.alibaba.tesla.appmanager.domain.schema.ComponentSchema;
import com.alibaba.tesla.appmanager.server.assembly.AppPackageDtoConvert;
import com.alibaba.tesla.appmanager.server.repository.AppMetaRepository;
import com.alibaba.tesla.appmanager.server.repository.condition.AppMarketQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.condition.AppMetaQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.AppMetaDO;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageDO;
import com.alibaba.tesla.appmanager.server.service.appmarket.AppMarketService;
import com.alibaba.tesla.appmanager.server.service.appoption.AppOptionService;
import com.alibaba.tesla.appmanager.server.service.apppackage.AppPackageService;
import com.alibaba.tesla.appmanager.server.storage.impl.OssStorage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author qianmo.zm@alibaba-inc.com
 * @date 2020/11/19.
 */
@Slf4j
@Service
public class MarketProviderImpl implements MarketProvider {

    @Autowired
    private AppPackageDtoConvert appPackageDtoConvert;

    @Autowired
    private AppMarketService appMarketService;

    @Autowired
    private AppMetaRepository appMetaRepository;

    @Autowired
    private AppOptionService appOptionService;

    @Autowired
    private AppPackageService appPackageService;

    @Autowired
    private AppPackageProvider appPackageProvider;

    @Override
    public Pagination<MarketAppItemDTO> list(MarketAppListReq request) {
        AppMarketQueryCondition condition = AppMarketQueryCondition.builder()
                .tag(DefaultConstant.ON_SALE)
                .optionKey(request.getOptionKey())
                .optionValue(request.getOptionValue())
                .page(request.getPage())
                .pageSize(request.getPageSize())
                .withBlobs(request.isWithBlobs())
                .build();
        Pagination<AppPackageDO> packages = appMarketService.list(condition);

        List<AppMetaDO> metaList = new ArrayList<>();
        List<AppPackageVersionCountDTO> countVersionList = new ArrayList<>();
        if (!packages.isEmpty()) {
            List<String> appIds = packages.getItems().stream()
                    .map(AppPackageDO::getAppId)
                    .collect(Collectors.toList());
            metaList.addAll(appMetaRepository.selectByCondition(
                    AppMetaQueryCondition.builder()
                            .appIdList(appIds)
                            .withBlobs(true)
                            .build()));
            countVersionList.addAll(appPackageService.countVersion(
                    AppPackageVersionCountReq.builder()
                            .appIds(appIds)
                            .tag(DefaultConstant.ON_SALE)
                            .build()));
        }
        return Pagination.transform(packages, item -> transform(item, metaList, countVersionList));
    }

    /**
     * 转换 appPackage，增加附加数据，并获取实际市场需要的 Item 对象
     *
     * @param item             appPackage item
     * @param metaList         元信息
     * @param countVersionList 版本计数列表
     * @return
     */
    private MarketAppItemDTO transform(
            AppPackageDO item, List<AppMetaDO> metaList, List<AppPackageVersionCountDTO> countVersionList) {
        AppPackageDTO mid = appPackageDtoConvert.to(item);
        if (Objects.nonNull(mid)) {
            AppMetaDO appMetaDO = metaList.stream()
                    .filter(m -> StringUtils.equals(m.getAppId(), mid.getAppId()))
                    .findFirst()
                    .orElse(null);
            if (Objects.nonNull(appMetaDO)) {
                JSONObject optionMap = appOptionService.getOptionMap(appMetaDO.getAppId());
                String name = appMetaDO.getAppId();
                if (StringUtils.isNotEmpty(optionMap.getString("name"))) {
                    name = optionMap.getString("name");
                }
                mid.setAppName(name);
                mid.setAppOptions(optionMap);
            }
        }
        MarketAppItemDTO result = new MarketAppItemDTO();
        ClassUtil.copy(mid, result);

        // 填充 package version 计数
        List<AppPackageVersionCountDTO> filteredCountVersion = countVersionList.stream()
                .filter(countVersion -> item.getAppId().equals(countVersion.getAppId()))
                .collect(Collectors.toList());
        if (filteredCountVersion.size() > 0) {
            result.setPackageCount(filteredCountVersion.get(0).getPackageCount());
        } else {
            result.setPackageCount(0L);
        }
        return result;
    }

//    @Override
//    public Long lockPublish(){
//
//    }
//
//    @Override
//    public boolean unlockPublish(){
//
//    }
//
    private boolean updateMarketPackageIndex(MarketEndpointDTO marketEndpoint, MarketPackageDTO marketPackage, String relativeRemotePath) throws IOException {

        /**
         * 更新 sw-index.json
         * {
         *    "packages":[{
         *       "appId": "xxx",
         *       "appName": "中文",
         *       "latestTags": ["xxx", "yy"],
         *       "packageVersions": [
         *          "1.1.1+20220608160822137535",
         *       ],
         *       "logoUrl": "applications/upload/logo.png",
         *       "latestComponents":[
         *           {"componentName": "aaa", "componentType": "K8S_MICROSERVICE"}
         *           ...
         *       ],
         *       "category":"交付",
         *       "description": "xxxx",
         *       "urls":{
         *          "1.1.1+20220608160822137535": "applications/upload/1.1.1+20220608160822137535.zip"
         *       }
         *    }]
         * }
         */
        if (StringUtils.equals(marketEndpoint.getEndpointType(), "oss")) {
            JSONObject swIndexObject;
            JSONObject appInfo = null;
            String remoteIndexFilePath = marketEndpoint.getRemotePackagePath() + "/sw-index.json";
            OssStorage client = new OssStorage(
                    marketEndpoint.getEndpoint(), marketEndpoint.getAccessKey(), marketEndpoint.getSecretKey());
            if(client.objectExists(marketEndpoint.getRemoteBucket(), remoteIndexFilePath)){
                File swIndexFile = Files.createTempFile("market", ".json").toFile();
                NetworkUtil.download(
                    client.getObjectUrl(marketEndpoint.getRemoteBucket(), remoteIndexFilePath, 86400),
                    swIndexFile.getAbsolutePath()
                );
                swIndexObject = JSONObject.parseObject(FileUtils.readFileToString(swIndexFile, "UTF-8"));
            }else{
                swIndexObject = new JSONObject();
                swIndexObject.put("packages", new JSONArray());
            }

            JSONArray swIndexPackages = swIndexObject.getJSONArray("packages");

            for(int i=0; i < swIndexPackages.size(); i++){
                if(StringUtils.equals(swIndexPackages.getJSONObject(i).getString("appId"), marketPackage.getAppId())){
                    appInfo = swIndexPackages.getJSONObject(i);
                    break;
                }
            }

            if(appInfo == null){
                appInfo = new JSONObject();
                appInfo.put("appId", marketPackage.getAppId());
                appInfo.put("appName", marketPackage.getAppName());
                if(marketPackage.getAppOptions() != null) {
                    if (marketPackage.getAppOptions().getString("category") != null) {
                        appInfo.put("category", marketPackage.getAppOptions().getString("category"));
                    }
                    if (marketPackage.getAppOptions().getString("description") != null) {
                        appInfo.put("description", marketPackage.getAppOptions().getString("description"));
                    }
                }
                appInfo.put("urls", new JSONObject());
                appInfo.put("packageVersions", new JSONArray());
                swIndexPackages.add(appInfo);
            }

            if(marketPackage.getAppSchemaObject() != null){
                appInfo.put("latestComponents", new JSONArray());
                for(AppPackageSchema.ComponentPackageItem componentPackage : marketPackage.getAppSchemaObject().getComponentPackages()){
                    JSONObject componentInfo = new JSONObject();
                    componentInfo.put("componentName", componentPackage.getComponentName());
                    componentInfo.put("componentType", componentPackage.getComponentType());
                    appInfo.getJSONArray("latestComponents").add(componentInfo);
                }
            }
            if(marketPackage.getAppOptions() != null){
                marketPackage.getAppOptions().put("swapp", null);
                appInfo.put("appOptions", marketPackage.getAppOptions());
            }
            JSONObject appUrls = appInfo.getJSONObject("urls");
            JSONArray packageVersions = appInfo.getJSONArray("packageVersions");
            appUrls.put(marketPackage.getPackageVersion(), relativeRemotePath);
            packageVersions.add(marketPackage.getPackageVersion());


            File swIndexFile = Files.createTempFile("market_index", ".json").toFile();
            FileUtils.writeStringToFile(swIndexFile, swIndexObject.toString(SerializerFeature.PrettyFormat), true);

            client.putObject(marketEndpoint.getRemoteBucket(), remoteIndexFilePath, swIndexFile.getAbsolutePath());
            client.setObjectAclPublic(marketEndpoint.getRemoteBucket(), remoteIndexFilePath);

        }
        return true;
    }

    @Override
    public String uploadPackage(MarketEndpointDTO marketEndpoint, MarketPackageDTO marketPackage) throws IOException {
        String applicationRemotePath = "applications/" + marketPackage.getAppId();
        String relativeRemotePath = applicationRemotePath + "/" + marketPackage.getPackageVersion() + ".zip";
        String fullRemotePath = marketEndpoint.getRemotePackagePath() + "/" + relativeRemotePath;
        if (StringUtils.equals(marketEndpoint.getEndpointType(), "oss")) {
            OssStorage client = new OssStorage(
                    marketEndpoint.getEndpoint(), marketEndpoint.getAccessKey(), marketEndpoint.getSecretKey());
            log.info("action=init|message=oss client has initialized|endpoint={}", marketEndpoint.getEndpoint());
            client.putObject(marketEndpoint.getRemoteBucket(), fullRemotePath, marketPackage.getPackageLocalPath());
            client.setObjectAclPublic(marketEndpoint.getRemoteBucket(), fullRemotePath);

            // logo如果为本地minio地址，则直接上传后替换
            if(marketPackage.getAppOptions() != null){
                if(marketPackage.getAppOptions().getString("logoImg") != null && marketPackage.getAppOptions().getString("logoImg").startsWith("/gateway/minio/")) {
                    String logoLocalUrl = marketPackage.getAppOptions().getString("logoImg").replace("/gateway/minio/", "http://sreworks-minio:9000/");
                    File logoTempFile = Files.createTempFile("logo", null).toFile();
                    NetworkUtil.download(logoLocalUrl, logoTempFile.getAbsolutePath());
                    String logoRemoteUrl = marketEndpoint.getRemotePackagePath() + "/" + applicationRemotePath + "/logo";
                    client.putObject(marketEndpoint.getRemoteBucket(), logoRemoteUrl, logoTempFile.getAbsolutePath());
                    client.setObjectAclPublic(marketEndpoint.getRemoteBucket(), logoRemoteUrl);
                    marketPackage.getAppOptions().put("logoImg", "https://" + marketEndpoint.getRemoteBucket() + "." + marketEndpoint.getEndpoint() + "/" + logoRemoteUrl);
                }
                // 进入市场之后，处于非开发态
                marketPackage.getAppOptions().put("isDevelopment", 0);
            }

            this.updateMarketPackageIndex(marketEndpoint, marketPackage, relativeRemotePath);
            return fullRemotePath;
        }
        return null;
    }

    @Override
    public File downloadAppPackage(AppPackageDTO appPackage, String operator) throws IOException {
        AppPackageUrlRes appPackageUrl = appPackageProvider.generateUrl(appPackage.getId(), operator);
        File zipFile = Files.createTempFile("market_publish", ".zip").toFile();
        NetworkUtil.download(appPackageUrl.getUrl(), zipFile.getAbsolutePath());
        return zipFile;
    }

    @Override
    public MarketPackageDTO rebuildAppPackage(File appPackageLocal, String operator, String oldAppId, String newAppId, String newSimplePackageVersion) throws IOException {


        /**
         * 将应用包解压
         */
        Path workDirFile = Files.createTempDirectory("market_publish");
        String workDirAbsPath = workDirFile.toFile().getAbsolutePath();
        ZipUtil.unzip(appPackageLocal.getAbsolutePath(), workDirAbsPath);
        FileUtils.deleteQuietly(appPackageLocal);

        /**
         * 将组件包进行解压
         */
        List<File> subfiles = Files.walk(workDirFile).map(p -> p.toFile()).collect(Collectors.toList());
        for(File component: subfiles ){
            if(!component.getName().endsWith(".zip")){
                continue;
            }
            Path componentPath = Paths.get(component.getAbsolutePath().replace(".zip", ""));
            Files.createDirectory(componentPath);
            ZipUtil.unzip(component.getAbsolutePath(), componentPath.toString());
            FileUtils.deleteQuietly(component);

            /**
             * 将组件包的meta.yaml进行字符串替换
             */
            String metaYaml = FileUtils.readFileToString(Paths.get(componentPath.toString(), "meta.yaml").toFile(), StandardCharsets.UTF_8);
            if(newAppId != null) {
                ComponentSchema metaYamlObject = SchemaUtil.toSchema(ComponentSchema.class, metaYaml);
                JSONObject labels = (JSONObject) metaYamlObject.getMetadata().getLabels();
                labels.put("labels.appmanager.oam.dev/appId", newAppId);
                labels.put("appId", newAppId);
                JSONObject specLabels = (JSONObject) metaYamlObject.getSpec().getWorkload().getMetadata().getLabels();
                specLabels.put("labels.appmanager.oam.dev/appId", newAppId);
                specLabels.put("appId", newAppId);
                FileUtils.writeStringToFile(Paths.get(componentPath.toString(), "meta.yaml").toFile(), SchemaUtil.toYamlMapStr(metaYamlObject), StandardCharsets.UTF_8);
            }

            /**
            * 将组件包进行重新压缩，并删除组件目录
            */
            ZipUtil.zipFiles(component.getAbsolutePath(), Files.walk(componentPath).map(p -> p.toFile()).collect(Collectors.toList()));
            FileUtils.deleteDirectory(componentPath.toFile());

        }

        /**
         * 针对Application的meta.yaml进行解析替换
         */
        String metaYaml = FileUtils.readFileToString(Paths.get(workDirAbsPath, "/meta.yaml").toFile(), StandardCharsets.UTF_8);
        if(newAppId != null){
            metaYaml = metaYaml.replace("appId: " + oldAppId, "appId: " + newAppId);
            metaYaml = metaYaml.replace("appId: \"" + oldAppId + "\"", "appId: \"" + newAppId + "\"");
            metaYaml = metaYaml.replace("labels.appmanager.oam.dev/appId: \"" + oldAppId + "\"", "labels.appmanager.oam.dev/appId: \"" + newAppId + "\"");
        }
        AppPackageSchema metaYamlObject = SchemaUtil.toSchema(AppPackageSchema.class, metaYaml);
        if(!metaYamlObject.getTags().contains("on-sale")){
            metaYamlObject.getTags().add("on-sale");
        }
        String newPackageVersion;
        if(newSimplePackageVersion != null){
            newPackageVersion = VersionUtil.buildVersion(newSimplePackageVersion);
            metaYamlObject.setPackageVersion(newPackageVersion);
        }else{
            newPackageVersion = metaYamlObject.getPackageVersion();
        }
        FileUtils.writeStringToFile(Paths.get(workDirAbsPath, "/meta.yaml").toFile(), SchemaUtil.toYamlMapStr(metaYamlObject), StandardCharsets.UTF_8);

        /**
         * 针对应用进行重新压缩
         */
        Path workDirResult = Files.createTempDirectory("market_publish_result");
        String zipPath = workDirResult.resolve("app_package.zip").toString();
        ZipUtil.zipFiles(workDirResult.resolve("app_package.zip").toString(), Files.walk(workDirFile).map(p -> p.toFile()).collect(Collectors.toList()));

        MarketPackageDTO marketPackage = new MarketPackageDTO();
        marketPackage.setPackageLocalPath(zipPath);
        marketPackage.setAppId(newAppId);
        marketPackage.setPackageVersion(newPackageVersion);
        marketPackage.setSimplePackageVersion(newPackageVersion.split("\\+")[0]);
        return marketPackage;
    }

}
