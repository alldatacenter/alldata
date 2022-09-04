package com.alibaba.sreworks.appdev.server.services;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.appdev.server.params.AppPipelineCreateParam;
import com.alibaba.sreworks.common.util.JsonUtil;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Data
@Slf4j
@Service
public class AppPipelineService {

    public String getScript(AppPipelineCreateParam param) {
        String buildUrl = "http://dev-app-dev-app:7001/appPackage/runPackage?appId=" + param.getAppId();
        String buildPostBody = JsonUtil.map(
            "teamRegistryId", param.getTeamRegistryId(),
            "teamRepoId", param.getTeamRepoId()
        ).toJSONString();
        String buildCommand = String.format(
            "curl -XPOST '%s' -d'%s' -H 'Content-Type: application/json' ",
            buildUrl, buildPostBody
        );

        return String.format(""
                + "pipeline {\n"
                + "    agent any\n"
                + "\n"
                + "    stages {\n"
                + "        stage('构建') {\n"
                + "            steps {\n"
                + "                sh %s \n"
                + "            }\n"
                + "        }\n"
                + "        \n"
                + "        stage('部署') {\n"
                + "            steps {\n"
                + "                input \"deloy?\"\n"
                + "                echo 'deploy'\n"
                + "            }\n"
                + "        }\n"
                + "        \n"
                + "    }\n"
                + "}\n",
            JSONObject.toJSONString(buildCommand)
        );
    }

}
