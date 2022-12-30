package com.alibaba.sreworks.clustermanage.server.services;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.CmdUtil;
import com.alibaba.sreworks.common.DTO.RunCmdOutPut;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.sreworks.clustermanage.server.utils.CommandUtil;
import org.zeroturnaround.exec.ProcessResult;

@Slf4j
@Service
public class DeployClientService {

    @Autowired
    ClientPackageService clientPackageService;

    public JSONObject installAllPackages(String kubeconfig, JSONObject jsonEnvMap) throws IOException, InterruptedException, TimeoutException {
        JSONObject result = new JSONObject();
        for(String clientName: this.clientPackageService.getNames()){
            JSONObject clientInfo = this.clientPackageService.getValue(clientName);
            JSONObject resultInfo = new JSONObject();
            String deployCommand = clientInfo.getString("deployCommand");
            if(StringUtils.isBlank(deployCommand)){
                continue;
            }
            resultInfo.put("deployCommand", deployCommand);
            String kubeconfigPath = "/tmp/" + System.currentTimeMillis() + ".kubeconfig";
            FileUtils.writeStringToFile(new File(kubeconfigPath), kubeconfig, "utf-8");

            String runCommand = deployCommand.replace("helm ", "cd /app/client-deploy-packages/" + clientName + "/; /app/helm --kubeconfig=" + kubeconfigPath + " ");
            log.info("{},deployCommand={}", clientName, runCommand);
            Map<String, String> envMap;
            if(jsonEnvMap != null){
                envMap = JSONObject.toJavaObject(jsonEnvMap, Map.class);
            }else{
                envMap = new HashMap<>();
            }
            ProcessResult commandResult = CommandUtil.runLocalCommand(runCommand, envMap);
            resultInfo.put("output", commandResult.outputUTF8());
            resultInfo.put("retCode", commandResult.getExitValue());
            resultInfo.put("clientName", clientName);
            result.put(clientName, resultInfo);
        }
        return result;
    }

    public void run(String kubeconfig) throws IOException, InterruptedException {
        String deployClientClusterPackageUrl = System.getenv("DEPLOY_CLIENT_CLUSTER_PACKAGE_URL");
        String deployClientClusterCmd = System.getenv("DEPLOY_CLIENT_CLUSTER_CMD");

        String kubeconfigPath = "/tmp/" + System.currentTimeMillis() + ".kubeconfig";
        FileUtils.writeStringToFile(new File(kubeconfigPath), kubeconfig, "utf-8");

        String filePath = downloadFromUrl(deployClientClusterPackageUrl, "/tmp/");
        String unzipDir = "/tmp/" + System.currentTimeMillis();
        new File(unzipDir).mkdirs();
        unzip(filePath, unzipDir);

        CmdUtil.exec(String.format("cd %s; %s --kubeconfig=%s", unzipDir, deployClientClusterCmd, kubeconfigPath));
    }

    public JSONArray listHelmInstallStatus(String kubeconfig) throws IOException, InterruptedException {

        String kubeconfigPath = "/tmp/" + System.currentTimeMillis() + ".kubeconfig";
        FileUtils.writeStringToFile(new File(kubeconfigPath), kubeconfig, "utf-8");

        String helmListCmd = String.format("/app/helm list -o json -A --kubeconfig=%s", kubeconfigPath);
        log.info("helmListCmd={}", helmListCmd);
        RunCmdOutPut res = CmdUtil.exec(helmListCmd);
        log.info("helmListResult={}", res.getStdout());
        JSONArray helmList = JSONArray.parseArray(res.getStdout());
        return helmList;
    }

    public String getFileNameFromUrl(String url) {
        String name = System.currentTimeMillis() + ".X";
        int index = url.lastIndexOf("/");
        if (index > 0) {
            name = url.substring(index + 1);
            if (name.trim().length() > 0) {
                return name;
            }
        }
        return name;
    }

    public String downloadFromUrl(String url, String dir) throws IOException {
        URL httpUrl = new URL(url);
        String fileName = getFileNameFromUrl(url);
        File f = new File(dir + fileName);
        FileUtils.copyURLToFile(httpUrl, f);
        return f.getPath();
    }

    public void unzip(String zipFilePath, String saveFileDir) throws IOException {
        if (!saveFileDir.endsWith("\\") && !saveFileDir.endsWith("/")) {
            saveFileDir += File.separator;
        }
        File dir = new File(saveFileDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(zipFilePath);
        if (file.exists()) {
            try (
                InputStream is = new FileInputStream(file);
                ZipArchiveInputStream zais = new ZipArchiveInputStream(is)
            ) {
                ArchiveEntry archiveEntry;
                while ((archiveEntry = zais.getNextEntry()) != null) {
                    // 获取文件名
                    String entryFileName = archiveEntry.getName();
                    // 构造解压出来的文件存放路径
                    String entryFilePath = saveFileDir + entryFileName;
                    OutputStream os = null;
                    try {
                        // 把解压出来的文件写到指定路径
                        File entryFile = new File(entryFilePath);
                        if (entryFileName.endsWith("/")) {
                            entryFile.mkdirs();
                        } else {
                            os = new BufferedOutputStream(new FileOutputStream(entryFile));
                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = zais.read(buffer)) != -1) {
                                os.write(buffer, 0, len);
                            }
                        }
                    } finally {
                        if (os != null) {
                            os.flush();
                            os.close();
                        }
                    }

                }
            }
        }
    }

}
