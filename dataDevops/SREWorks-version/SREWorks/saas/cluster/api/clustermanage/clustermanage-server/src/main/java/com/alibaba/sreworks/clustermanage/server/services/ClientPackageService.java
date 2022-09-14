package com.alibaba.sreworks.clustermanage.server.services;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import com.alibaba.sreworks.clustermanage.server.utils.SchemaUtil;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.util.Set;

import org.yaml.snakeyaml.Yaml;


@Slf4j
@Service
public class ClientPackageService {

    private Map<String, JSONObject> packages = new HashMap<>();

    @PostConstruct
    public void init() throws IOException {
        String path = "/app/client-deploy-packages";
        File file = new File(path);
        File[] fs = file.listFiles();
        for(File pkg:fs){
            if(!pkg.isDirectory()){
                continue;
            }
            File clientFile = new File(pkg.getAbsolutePath() + "/client.yaml");
            if (!clientFile.exists()){
                continue;
            }
            Yaml yaml = SchemaUtil.createYaml();
            String content = FileUtils.readFileToString(clientFile, StandardCharsets.UTF_8);
            JSONObject clientInfo = yaml.loadAs(content, JSONObject.class);
            packages.put(pkg.getName(), clientInfo);
        }
    }

    public JSONObject getValue(String name){
        return this.packages.get(name);
    }

    public Set<String> getNames(){
        return this.packages.keySet();
    }

    public JSONObject getAllValues(){
        JSONObject res = new JSONObject();
        for(String clientName: this.packages.keySet()){
            res.put(clientName, this.packages.get(clientName));
        }
        return res;
    }

}
