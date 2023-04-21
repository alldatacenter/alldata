package org.dromara.cloudeon.test;

import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;

@Slf4j
public class FileTest {
    @Test
    public void mkdir() {
        // 只删除conf目录
        FileUtil.del("/Volumes/Samsung_T5/opensource/e-mapreduce/work" + File.separator + "zookeeper1" + File.separator + "conf");
        // 只创建到全路径的父目录
//        FileUtil.mkParentDirs("/Volumes/Samsung_T5/opensource/e-mapreduce/work"+File.separator+"zookeeper1"+File.separator+"conf");

        // 创建全目录
//        FileUtil.mkdir("/Volumes/Samsung_T5/opensource/e-mapreduce/work"+File.separator+"zookeeper1"+File.separator+"conf");

    }





}
