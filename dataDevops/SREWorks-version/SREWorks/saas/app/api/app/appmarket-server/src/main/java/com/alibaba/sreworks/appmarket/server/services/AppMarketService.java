package com.alibaba.sreworks.appmarket.server.services;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.FileUtil;
import com.alibaba.sreworks.common.util.Requests;
import com.alibaba.sreworks.common.util.ZipUtil;
import com.alibaba.sreworks.domain.DO.App;
import com.alibaba.sreworks.domain.DO.AppComponent;
import com.alibaba.sreworks.domain.DO.AppPackage;
import com.alibaba.sreworks.domain.repository.AppComponentRepository;
import com.alibaba.sreworks.domain.repository.AppPackageRepository;
import com.alibaba.sreworks.domain.repository.AppRepository;
import com.alibaba.sreworks.domain.repository.TeamUserRepository;
import com.alibaba.sreworks.flyadmin.server.services.FlyadminAppmanagerAppService;
import com.alibaba.sreworks.flyadmin.server.services.FlyadminAppmanagerComponentService;
import com.alibaba.sreworks.flyadmin.server.services.FlyadminAppmanagerMarketService;
import com.alibaba.tesla.web.constant.HttpHeaderNames;

import io.kubernetes.client.openapi.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static com.alibaba.sreworks.domain.utils.AppUtil.appmanagerId;

@Slf4j
@Service
public class AppMarketService {

    @Autowired
    FlyadminAppmanagerMarketService flyadminAppmanagerMarketService;

    @Autowired
    AppRepository appRepository;

    @Autowired
    AppComponentRepository appComponentRepository;

    @Autowired
    FlyadminAppmanagerAppService flyadminAppmanagerAppService;

    @Autowired
    TeamUserRepository teamUserRepository;

    @Autowired
    FlyadminAppmanagerComponentService flyadminAppmanagerComponentService;

    @Autowired
    AppPackageRepository appPackageRepository;

    @Value("${remote.sreworks.endpoint}")
    private String remoteSreworksEndpoint;

    public List<JSONObject> listRemote(String user) throws IOException {
        String url = remoteSreworksEndpoint + "/gateway/sreworks/appdev/appMarket/list";
        return new Requests(url)
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .get().isSuccessful()
            .getJSONObject().getJSONArray("data").toJavaList(JSONObject.class);
    }

    public String download(AppPackage appPackage, String user) throws IOException, ApiException {
        String appmanagerId = appmanagerId(appPackage.getAppId());
        String filePath = flyadminAppmanagerMarketService.download(appmanagerId, appPackage.getAppPackageId(), user);

        String metaFilePath = "/tmp/" + UUID.randomUUID().toString();
        Files.write(Path.of(metaFilePath), JSONObject.toJSONString(appPackage).getBytes());

        String zipFilePath = "/tmp/" + UUID.randomUUID().toString() + ".zip";
        ZipUtil.zipFiles(zipFilePath, Arrays.asList(new File(filePath), new File(metaFilePath)));

        return zipFilePath;
    }

    public List<String> downloadRemote(Long appPackageId) throws IOException {
        List<String> ret = new ArrayList<>();
        String filePath = "/tmp/" + UUID.randomUUID().toString() + ".zip";
        String destDir = "/tmp/" + UUID.randomUUID().toString();
        String url = remoteSreworksEndpoint + "/gateway/sreworks/appdev/appMarket/download?"
            + "appPackageId=" + appPackageId;
        FileUtils.copyURLToFile(new URL(url), new File(filePath));
        ZipUtil.unzip(filePath, destDir);
        for (File file : Objects.requireNonNull(new File(destDir).listFiles())) {
            String fileName = file.getName();
            if (fileName.endsWith(".zip")) {
                ret.add(file.getPath());
            } else {
                ret.add(0, file.getPath());
            }
        }
        log.info(JSONObject.toJSONString(ret));
        return ret;
    }

    public AppPackage createAppAndComponent(String metaFilePath, Long teamId, String appName, String user, Long display)
        throws Exception {

        AppPackage appPackage = FileUtil.readObject(metaFilePath, AppPackage.class);
        App app = appPackage.app();
        app.setId(null);
        app.setName(appName);
        app.setGmtCreate(System.currentTimeMillis() / 1000);
        app.setGmtModified(System.currentTimeMillis() / 1000);
        app.setCreator(user);
        app.setLastModifier(user);
        app.setTeamId(teamId);
        app.setDisplay(display);
        appRepository.saveAndFlush(app);
        flyadminAppmanagerAppService.create(app);
        teamUserRepository.updateGmtAccessByTeamIdAndUser(teamId, user);
        appPackage.setAppId(app.getId());
        appPackage.setApp(JSONObject.toJSONString(app));

        List<AppComponent> appComponentList = appPackage.appComponentList();
        for (AppComponent appComponent : appComponentList) {
            appComponent.setId(null);
            appComponent.setGmtCreate(System.currentTimeMillis() / 1000);
            appComponent.setGmtModified(System.currentTimeMillis() / 1000);
            appComponent.setCreator(user);
            appComponent.setLastModifier(user);
            appComponent.setAppId(app.getId());
            Long exId = flyadminAppmanagerComponentService.create(
                app.getId(), appComponent.getName(), appComponent.repoDetail(), user);
            appComponent.setExId(exId);
            appComponentRepository.saveAndFlush(appComponent);
        }
        appPackage.setAppComponentList(JSONObject.toJSONString(appComponentList));

        return appPackage;

    }

    private void importData(AppPackage appPackage, String dataFilePath, String user) throws IOException, ApiException {
        JSONObject jsonObject = flyadminAppmanagerMarketService.importPackage(
            appmanagerId(appPackage.getAppId()),
            appPackage.getVersion(),
            new File(dataFilePath),
            user
        );
        appPackage.setAppPackageId(jsonObject.getLong("id"));
    }

    public void syncRemote2Local(Long appPackageId, Long teamId, String appName, String user, Long display, int onSale)
        throws Exception {

        List<String> subFilePathList = downloadRemote(appPackageId);
        String metaFilePath = subFilePathList.get(0);
        String dataFilePath = subFilePathList.get(1);
        AppPackage appPackage = createAppAndComponent(metaFilePath, teamId, appName, user, display);
        importData(appPackage, dataFilePath, user);

        appPackage.setId(null);
        appPackage.setGmtCreate(System.currentTimeMillis() / 1000);
        appPackage.setGmtModified(System.currentTimeMillis() / 1000);
        appPackage.setCreator(user);
        appPackage.setLastModifier(user);
        appPackage.setAppPackageTaskId(null);
        appPackage.setOnSale(onSale);
        appPackageRepository.saveAndFlush(appPackage);

    }

}
