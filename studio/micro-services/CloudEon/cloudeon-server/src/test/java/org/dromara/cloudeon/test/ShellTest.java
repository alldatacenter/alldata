package org.dromara.cloudeon.test;

import org.dromara.cloudeon.utils.ShellUtils;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;

@Slf4j
public class ShellTest {

    @Test
    public void cpu() {

        System.out.println(ShellUtils.builder().logger(log).build().getCpuArchitecture());
    }

    @Test
    public void exec() {

//        System.out.println(ShellUtils.builder().logger(log).build().exceShell("echo mntr |  nc  localhost 2181"));
        System.out.println("=====================");
        System.out.println(ShellUtils.builder().build().execWithStatus("/Users/huzekang/cdh5.16/zookeeper-3.4.5-cdh5.16.2/", Lists.newArrayList("sh", "1.sh"), 100000));
    }
}
