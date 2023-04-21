package org.dromara.cloudeon.test;

import org.dromara.cloudeon.utils.ShellCommandExecUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.Charset;

@Slf4j
public class ShellCommandExecUtilTest {

    @Test
    public void testRuntimeShell() throws IOException {
        int errCode;
        errCode = ShellCommandExecUtil.builder().log(log).build().runShellWithRuntime("/Users/huzekang/cdh5.16/zookeeper-3.4.5-cdh5.16.2/",
                new String[]{"bin/zkServer.sh", "start"}, Charset.forName("utf-8"));
        System.out.println(errCode);
    }


    @Test
    public void testProcessShell1() throws IOException {
        int errCode;
        errCode = ShellCommandExecUtil.builder().log(log).build().builder().build().runShellCommandSync("/Users/huzekang/cdh5.16/hadoop-2.6.0-cdh5.16.2",
                new String[]{"bin/hadoop", "fs", "-ls", "/tmp"}, Charset.forName("utf-8"));

//        String logPath = "/tmp/cmd.log";
        errCode = ShellCommandExecUtil.builder().log(log).build().runShellCommandSync("/Users/huzekang/cdh5.16/zookeeper-3.4.5-cdh5.16.2/",
                new String[]{"bin/zkServer.sh", "start"}, Charset.forName("utf-8"), null);
    }


}