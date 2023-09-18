package org.dromara.cloudeon.utils;

import cn.hutool.core.io.FileUtil;

import java.io.File;

public class DirectoryScanner {


    public static void scanDirectory(File directory,String rootDir) {
        if (directory.isDirectory()) {
            System.out.println("Scanning directory: " + directory.getAbsolutePath());
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    scanDirectory(file,rootDir);
                }
            }
        } else {
            System.out.println("File found: " + FileUtil.subPath(rootDir, directory));
        }
    }
}
