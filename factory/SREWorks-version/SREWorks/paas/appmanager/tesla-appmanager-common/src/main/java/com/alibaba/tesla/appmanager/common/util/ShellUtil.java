package com.alibaba.tesla.appmanager.common.util;

import com.alibaba.tesla.appmanager.common.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

@Slf4j
public class ShellUtil {
    public static boolean init(String script, String name, String target, String dockerFile,
                               String scheme) throws AppException {
        StringBuilder cmd = new StringBuilder();
        cmd.append(script).append(" init -n ").append(name).append(" -t ").append(target).append(" -d ").append(dockerFile).append(" -s ").append(scheme);
        return exec(cmd.toString());
    }

    public static boolean push(String script, String repoUrl, String ciAccount, String ciToken) throws AppException {
        StringBuilder cmd = new StringBuilder();
        cmd.append(script).append(" push -u ").append(repoUrl).append(" -a ").append(ciAccount).append(" -t ").append(ciToken);
        return exec(cmd.toString());
    }

    public static boolean check(String repoUrl, String ciAccount, String ciToken){
        /**
         * 判断git仓库是否存在
         * git ls-remote https://sreworks_public:sreworkspublic123@code.aliyun.com/sreworks_public/aac.git
         */
        StringBuilder cmd = new StringBuilder();
        String authRepoUrl;
        if(StringUtils.isNotEmpty(ciAccount) && StringUtils.isNotEmpty(ciToken)){
            if (repoUrl.startsWith("http://")) {
                authRepoUrl = repoUrl.replace("http://", "http://" + ciAccount + ":" + ciToken + "@");
            } else {
                authRepoUrl = repoUrl.replace("https://", "https://" + ciAccount + ":" + ciToken + "@");
            }
        }else {
            authRepoUrl = repoUrl;
        }
        cmd.append("git ").append(" ls-remote ").append(authRepoUrl);
        return exec(cmd.toString());
    }

    private static boolean exec(String cmd) throws AppException {
        log.info(">>>shellUtil|exec enter|cmd={}", cmd);
        Process pro;

        InputStream stdIn = null;
        BufferedReader stdRead = null;

        InputStream errIn = null;
        BufferedReader errRead = null;
        try {
            pro = Runtime.getRuntime().exec(cmd);
            int result = pro.waitFor();

            stdIn = pro.getInputStream();
            stdRead = new BufferedReader(new InputStreamReader(stdIn));

            StringBuffer stdMessageBuffer = new StringBuffer();
            String stdMessage;
            while ((stdMessage = stdRead.readLine()) != null) {
                stdMessageBuffer.append("\n").append(stdMessage);
            }
            log.info(">>>shellUtil|exec output|cmd={}, result={}", cmd, result);
            log.info(">>>shellUtil|exec output|stdMessage={}", stdMessageBuffer);

            errIn = pro.getErrorStream();
            errRead = new BufferedReader(new InputStreamReader(errIn));

            StringBuffer errMessageBuffer = new StringBuffer();
            String errMessage;
            while ((errMessage = errRead.readLine()) != null) {
                errMessageBuffer.append("\n").append(errMessage);
            }
            log.info(">>>shellUtil|exec output|errMessage={}", errMessageBuffer);

            if (Objects.equals(result, 0)) {
                return true;
            }
        } catch (Exception e) {
            log.error(">>>shellUtil|exec Err|error={}", e.getMessage(), e);
            throw new AppException((e.getMessage()));
        } finally {
            IOUtils.closeQuietly(stdIn);
            IOUtils.closeQuietly(stdRead);
            IOUtils.closeQuietly(errIn);
            IOUtils.closeQuietly(errRead);
        }


        return false;
    }
}
