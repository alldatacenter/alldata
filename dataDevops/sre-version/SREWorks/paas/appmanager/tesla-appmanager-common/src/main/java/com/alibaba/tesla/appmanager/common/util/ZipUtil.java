package com.alibaba.tesla.appmanager.common.util;

import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.google.common.io.Files;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * zip 工具类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class ZipUtil {

    /**
     * 将某个目录打包到指定路径的 zip 文件上
     * @param zipPath zip 文件绝对路径
     * @param directory 目录
     */
    public static void zipDirectory(String zipPath, File directory) {
        ZipParameters parameters = new ZipParameters();
        parameters.setIncludeRootFolder(true);
        try {
            new ZipFile(zipPath).addFolder(directory, parameters);
        } catch (ZipException e) {
            throw new AppException(AppErrorCode.IO_ERROR,
                String.format("Zip %s to %s failed", directory, zipPath), e);
        }
    }

    /**
     * 将某批文件打包到指定路径的 zip 文件上
     * @param zipPath zip 文件绝对路径
     * @param files 文件列表
     */
    public static void zipFiles(String zipPath, List<File> files) {
        ZipParameters parameters = new ZipParameters();
        parameters.setIncludeRootFolder(false);
        try {
            new ZipFile(zipPath).addFiles(files, parameters);
        } catch (ZipException e) {
            throw new AppException(AppErrorCode.IO_ERROR,
                String.format("Zip %s failed", zipPath), e);
        }
    }

    /**
     * 将某个文件打包到指定路径的 zip 文件上
     * @param zipPath zip 文件绝对路径
     * @param file 文件
     */
    public static void zipFile(String zipPath, File file) {
        ZipParameters parameters = new ZipParameters();
        parameters.setIncludeRootFolder(true);
        try {
            new ZipFile(zipPath).addFile(file, parameters);
        } catch (ZipException e) {
            throw new AppException(AppErrorCode.IO_ERROR,
                String.format("Zip %s failed", zipPath), e);
        }
    }

    /**
     * 解压指定 .zip 文件到目标目录下
     *
     * @param zipPath 要解压的 zip 文件绝对路径
     * @param destDir 解压的目标目录
     */
    public static void unzip(String zipPath, String destDir) {
        try {
            new ZipFile(zipPath).extractAll(destDir);
        } catch (ZipException e) {
            throw new AppException(AppErrorCode.IO_ERROR,
                String.format("Unzip %s to %s failed", zipPath, destDir), e);
        }
    }

    /**
     * 获取指定 zip 包中的指定文件内容
     *
     * @param zipPath  zip 包路径
     * @param filePath 文件相对路径
     * @return 文件内容
     */
    public static String getZipSpecifiedFile(String zipPath, String filePath) {
        try {
            File tmpMetaYaml = File.createTempFile("meta", ".yaml");
            String parentDir = tmpMetaYaml.toPath().getParent().toString();
            String absPath = tmpMetaYaml.toPath().toString();
            new ZipFile(zipPath).extractFile(filePath, parentDir, tmpMetaYaml.getName());
            String content = Files.asCharSource(tmpMetaYaml, StandardCharsets.UTF_8).read();
            if (!tmpMetaYaml.delete()) {
                log.error("cannot delete temp file {} when get zip specified file", absPath);
            }
            return content;
        } catch (Exception e) {
            throw new AppException(AppErrorCode.IO_ERROR,
                String.format("Get file %s from zip %s failed", filePath, zipPath), e);
        }
    }
}
