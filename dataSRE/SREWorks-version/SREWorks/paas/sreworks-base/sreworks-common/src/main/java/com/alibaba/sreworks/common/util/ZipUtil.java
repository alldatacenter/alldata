package com.alibaba.sreworks.common.util;

import com.google.common.io.Files;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * zip 工具类
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class ZipUtil {

    public static void zipDirectory(String zipPath, String directory) throws ZipException {
        new ZipFile(zipPath).addFolder(new File(directory));
    }

    public static void zipFiles(String zipPath, List<File> files) throws ZipException {
        ZipParameters parameters = new ZipParameters();
        parameters.setIncludeRootFolder(false);
        new ZipFile(zipPath).addFiles(files, parameters);
    }

    public static void unzip(String zipPath, String destDir) throws ZipException {
        new ZipFile(zipPath).extractAll(destDir);
    }

    public static String getZipSpecifiedFile(String zipPath, String filePath) throws IOException {
        File tmpMetaYaml = File.createTempFile("meta", ".yaml");
        String parentDir = tmpMetaYaml.toPath().getParent().toString();
        String absPath = tmpMetaYaml.toPath().toString();
        new ZipFile(zipPath).extractFile(filePath, parentDir, tmpMetaYaml.getName());
        String content = Files.asCharSource(tmpMetaYaml, StandardCharsets.UTF_8).read();
        if (!tmpMetaYaml.delete()) {
            log.error("cannot delete temp file {} when get zip specified file", absPath);
        }
        return content;
    }
}
