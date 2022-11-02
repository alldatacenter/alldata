package com.alibaba.sreworks.flyadmin.server.services;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.AppmanagerServiceUtil;
import com.alibaba.sreworks.common.util.JsonUtil;
import com.alibaba.sreworks.common.util.Requests;
import com.alibaba.sreworks.domain.DO.AppComponent;
import com.alibaba.sreworks.domain.DO.TeamRegistry;
import com.alibaba.sreworks.domain.DO.TeamRepo;
import com.alibaba.sreworks.domain.repository.AppComponentRepository;
import com.alibaba.sreworks.domain.repository.TeamRegistryRepository;
import com.alibaba.sreworks.domain.repository.TeamRepoRepository;
import com.alibaba.tesla.web.constant.HttpHeaderNames;

import io.kubernetes.client.openapi.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import static com.alibaba.sreworks.domain.utils.AppUtil.appmanagerId;

/**
 * @author jinghua.yjh
 */
@Slf4j
@Service
public class FlyadminAppmanagerMarketService {

    @Autowired
    FlyadminAppmanagerComponentService acService;

    @Autowired
    AppComponentRepository appComponentRepository;

    @Autowired
    TeamRepoRepository teamRepoRepository;

    @Autowired
    TeamRegistryRepository teamRegistryRepository;

    public void onSale(String appId, Long appPackageId, String user) throws IOException, ApiException {
        String url = AppmanagerServiceUtil.getEndpoint()
            + "/apps/" + appId
            + "/app-packages/" + appPackageId + "/on-sale";
        new Requests(url)
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .post().isSuccessful();
    }

    public String download(String appId, Long appPackageId, String user) throws IOException, ApiException {
        String filePath = "/tmp/" + UUID.randomUUID().toString() + ".zip";
        String url = AppmanagerServiceUtil.getEndpoint() + "/apps/" + appId + "/app-packages/" + appPackageId + "/url";
        String downloadUrl = new Requests(url)
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .get().isSuccessful().getJSONObject().getJSONObject("data").getString("url");
        FileUtils.copyURLToFile(new URL(downloadUrl), new File(filePath));
        return filePath;
    }

    public List<JSONObject> list(String user) throws IOException, ApiException {
        String url = AppmanagerServiceUtil.getEndpoint() + "/market/apps";
        return new Requests(url)
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .get().isSuccessful()
            .getJSONObject().getJSONObject("data").getJSONArray("items").toJavaList(JSONObject.class)
            .stream().filter(x -> x.getString("appId").startsWith("sreworks")).collect(Collectors.toList());
    }

    public JSONObject importPackage(String appId, String packageVersion, File file, String user)
        throws IOException, ApiException {

        String url = AppmanagerServiceUtil.getEndpoint()
            + "/apps/" + appId
            + "/app-packages/import?"
            + "packageVersion=" + packageVersion.replace("+", "%2B");

        return new Requests(url)
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .upload(file).isSuccessful()
            .getJSONObject().getJSONObject("data");
    }

}
