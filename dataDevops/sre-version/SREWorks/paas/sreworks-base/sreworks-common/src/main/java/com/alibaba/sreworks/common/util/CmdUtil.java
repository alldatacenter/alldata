package com.alibaba.sreworks.common.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import com.alibaba.sreworks.common.DTO.RunCmdOutPut;

/**
 * @author yangjinghua
 */

public class CmdUtil {

    public static RunCmdOutPut exec(String cmd) throws IOException, InterruptedException {
        return exec(cmd, Integer.MAX_VALUE);
    }

    public static String read(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String s;
        while ((s = br.readLine()) != null) {
            sb.append(s);
        }
        return sb.toString();
    }

    public static RunCmdOutPut exec(String cmd, int timeout) throws IOException, InterruptedException {
        Process proc = Runtime.getRuntime().exec(cmd);
        boolean isNormal = proc.waitFor(timeout, TimeUnit.SECONDS);
        if (!isNormal) {
            exec("kill -9 " + proc.pid());
        }
        int exitValue = proc.exitValue();
        String stdout = read(proc.getInputStream());
        String stderr = read(proc.getErrorStream());
        proc.destroy();
        return RunCmdOutPut.builder()
            .cmd(cmd)
            .timeout(timeout)
            .isNormal(isNormal)
            .code(exitValue)
            .stderr(stderr)
            .stdout(stdout)
            .build();
    }

}

