package com.datasophon.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;

public class CompressUtils {
    private static final Logger logger = LoggerFactory.getLogger(CompressUtils.class);

    public static void main(String[] args) throws IOException {
        decompressTarGz("D:\\DDP\\apache-druid-0.20.2-bin.tar.gz", "D:\\360downloads");
    }


    public static Boolean decompressTarGz(String sourceTarGzFile, String targetDir) {
        logger.info("use tar -zxvf to decompress");
        ArrayList<String> command = new ArrayList<>();
        command.add("tar");
        command.add("-zxvf");
        command.add(sourceTarGzFile);
        command.add("-C");
        command.add(targetDir);
        ExecResult execResult = ShellUtils.execWithStatus(targetDir, command, 120);
        return execResult.getExecResult();
    }


}
