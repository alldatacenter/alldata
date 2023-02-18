package com.datasophon.api.service.impl;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import cn.hutool.core.io.FileUtil;
import com.datasophon.api.exceptions.ServiceException;
import com.datasophon.api.master.ActorUtils;
import com.datasophon.api.service.ClusterKerberosService;
import com.datasophon.api.service.ClusterServiceRoleInstanceService;
import com.datasophon.common.Constants;
import com.datasophon.common.command.remote.GenerateKeytabFileCommand;
import com.datasophon.common.utils.ExecResult;
import com.datasophon.dao.entity.ClusterServiceRoleInstanceEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

@Service("clusterKerberosService")
@Transactional
public class ClusterKerberosServiceImpl implements ClusterKerberosService {


    @Autowired
    private ClusterServiceRoleInstanceService roleInstanceService;

    @Override
    public void downloadKeytab(Integer clusterId, String principal, String keytabName, String hostname, HttpServletResponse response) throws IOException {
        //通过文件路径获得File对象
        String userDir = System.getProperty("user.dir");
        String keytabFilePath = "/etc/security/keytab"+ Constants.SLASH + hostname + Constants.SLASH + keytabName;
        File file = new File(keytabFilePath);
        if (!file.exists()) {
            generateKeytabFile(clusterId, keytabFilePath, principal, keytabName,hostname);
        }
        FileInputStream inputStream = new FileInputStream(file);

        response.reset();
        response.setContentType("application/octet-stream");
        response.addHeader("Content-Length", "" + file.length());
        // 支持中文名称文件,需要对header进行单独设置，不然下载的文件名会出现乱码或者无法显示的情况
        // 设置响应头，控制浏览器下载该文件
        response.setHeader("Content-Disposition", "attachment;filename=" + keytabName);
        //通过response获取ServletOutputStream对象(out)
        OutputStream out = response.getOutputStream();
        int length = 0;
        byte[] buffer = new byte[1024];
        while ((length = inputStream.read(buffer)) != -1) {
            //4.写到输出流(out)中
            out.write(buffer, 0, length);
        }
        inputStream.close();
        out.flush();
        out.close();
    }

    private void generateKeytabFile(Integer clusterId, String keytabFilePath, String principal, String keytabName,String hostname) {
        ClusterServiceRoleInstanceEntity roleInstanceEntity = roleInstanceService.getKAdminRoleIns(clusterId);
        ActorRef kerberosActor = ActorUtils.getRemoteActor(roleInstanceEntity.getHostname(), "kerberosActor");
        GenerateKeytabFileCommand command = new GenerateKeytabFileCommand();
        command.setKeytabName(keytabName);
        command.setPrincipal(principal);
        command.setHostname(hostname);
        Timeout timeout = new Timeout(Duration.create(180, TimeUnit.SECONDS));
        Future<Object> execFuture = Patterns.ask(kerberosActor, command, timeout);
        ExecResult execResult = null;
        try {
            execResult = (ExecResult) Await.result(execFuture, timeout.duration());
            if (execResult.getExecResult()) {
//                FileUtil.writeString(execResult.getExecOut(), keytabFilePath, Charset.forName("ISO-8859-1"));
            }
        } catch (Exception e) {

        }
    }
}
