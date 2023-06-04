package com.datasophon.worker.handler;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.StreamProgress;
import cn.hutool.core.lang.Console;
import cn.hutool.http.HttpUtil;
import com.datasophon.common.Constants;
import com.datasophon.common.model.RunAs;
import com.datasophon.common.utils.CompressUtils;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.common.utils.PropertyUtils;
import com.datasophon.common.utils.ShellUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class InstallServiceHandler {
    private static final Logger logger = LoggerFactory.getLogger(InstallServiceHandler.class);

    private static final String HADOOP = "hadoop";

    public ExecResult install(String packageName, String decompressPackageName, String packageMd5, RunAs runAs) {
        ExecResult execResult = new ExecResult();
        try {
            String destDir = Constants.INSTALL_PATH + Constants.SLASH + "DDP/packages" + Constants.SLASH;
            Boolean needDownLoad = true;
            if (FileUtil.exist(destDir + packageName)) {//check install package
                //check md5
                String md5cmd = "sh " + Constants.WORKER_SCRIPT_PATH + "md5.sh " + destDir + packageName;
                String md5 = ShellUtils.getPackageMd5(md5cmd);
                logger.info("packageMd5 is {}", packageMd5);
                logger.info("md5sum result is {}", md5);
                if (StringUtils.isNotBlank(md5) && packageMd5.trim().equals(md5.trim())) {
                    needDownLoad = false;
                }
            }
            String dest = destDir + packageName;
            if (needDownLoad) {
                String masterHost = PropertyUtils.getString(Constants.MASTER_HOST);
                String masterPort = PropertyUtils.getString(Constants.MASTER_WEB_PORT);

                String downloadUrl = "http://" + masterHost + ":" + masterPort + "/ddh/service/install/downloadPackage?packageName=" + packageName;

                logger.info("download url is {}", downloadUrl);

                HttpUtil.downloadFile(downloadUrl, FileUtil.file(dest), new StreamProgress() {
                    @Override
                    public void start() {
                        Console.log("start to install。。。。");
                    }

                    @Override
                    public void progress(long progressSize, long l1) {
                        Console.log("installed：{}", FileUtil.readableFileSize(progressSize));
                    }

                    @Override
                    public void finish() {
                        Console.log("install success！");
                    }
                });
                execResult.setExecOut("download package " + packageName + "success");
                logger.info("download package {} success", packageName);
            }
            //decompress tar.gz
            if (!FileUtil.exist(Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName)) {
                if (CompressUtils.decompressTarGz(dest, Constants.INSTALL_PATH)) {
                    execResult.setExecResult(true);
                    execResult.setExecOut("install package " + packageName + "success");
                    if (Objects.nonNull(runAs)) {
                        ShellUtils.exceShell(" chown -R " + runAs.getUser() + ":" + runAs.getGroup() + " " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName);
                    }
                    ShellUtils.exceShell(" chmod -R 770 " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName);
                    if (decompressPackageName.contains(Constants.PROMETHEUS)) {
                        String alertPath = Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName + Constants.SLASH + "alert_rules";
                        ShellUtils.exceShell("sed -i \"s/clusterIdValue/" + PropertyUtils.getString("clusterId") + "/g\" `grep clusterIdValue -rl " + alertPath + "`");
                    }
                    if (decompressPackageName.contains(HADOOP)) {
                        ShellUtils.exceShell(" chown -R  root:hadoop " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName);
                        ShellUtils.exceShell(" chmod 755 " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName);
                        ShellUtils.exceShell(" chmod -R 755 " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName + "/etc");
                        ShellUtils.exceShell(" chmod 6050 " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName + "/bin/container-executor");
                        ShellUtils.exceShell(" chmod 400 " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName + "/etc/hadoop/container-executor.cfg");
                        ShellUtils.exceShell(" chown -R yarn:hadoop " + Constants.INSTALL_PATH + Constants.SLASH + decompressPackageName + "/logs/userlogs");
                    }
                } else {
                    execResult.setExecOut("install package " + packageName + "failed");
                    logger.info("install package " + packageName + " failed");
                }
            } else {
                execResult.setExecResult(true);
            }
        } catch (Exception e) {
            execResult.setExecOut(e.getMessage());
            e.printStackTrace();
        }
        return execResult;
    }
}
