package com.alibaba.tesla.appmanager.common.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;

import lombok.extern.slf4j.Slf4j;

/**
 * @ClassName: FileUtil
 * @Author: dyj
 * @DATE: 2021-02-04
 * @Description:
 **/
@Slf4j
public class FileUtil {

    public static File createFile(String destFileName, boolean cover) {
        File file = new File(destFileName);
        if(file.exists()) {
            if (cover){
                file.delete();
            } else {
                throw new AppException(AppErrorCode.USER_CONFIG_ERROR,
                    String.format("创建单个文件%s 失败，目标文件已存在！", destFileName));
            }
        }
        if (destFileName.endsWith(File.separator)) {
            throw new AppException(AppErrorCode.USER_CONFIG_ERROR,
                String.format("创建单个文件%s 失败，目标文件不能为目录！", destFileName));
        }
        //判断目标文件所在的目录是否存在
        if(!file.getParentFile().exists()) {
            //如果目标文件所在的目录不存在，则创建父目录
            if(!file.getParentFile().mkdirs()) {
                throw new AppException(AppErrorCode.USER_CONFIG_ERROR,
                    String.format("%s 目标文件所在目录失败", destFileName));
            }
        }
        //创建目标文件
        try {
            if (file.createNewFile()) {
                log.info("create file success!");
                return file;
            } else {
                throw new AppException(AppErrorCode.USER_CONFIG_ERROR,
                    String.format("创建单个文件 %s 失败", destFileName));
            }
        } catch (IOException e) {
            throw new AppException(AppErrorCode.USER_CONFIG_ERROR,
                String.format("创建单个文件 %s 失败", destFileName),e.getStackTrace());
        }
    }

    public static boolean writeStringToFile(String fileName, String data, boolean cover) throws IOException {
        if (cover){
            File file = createFile(fileName, cover);
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(data);
            fileWriter.flush();
            fileWriter.close();
            return true;
        } else {
            FileWriter fileWriter = new FileWriter(fileName, true);
            fileWriter.write(data);
            fileWriter.flush();
            fileWriter.close();
            return true;
        }
    }

    public static void writeStringToFile(String fileName, String data, boolean cover, StandardCharsets encode) throws IOException {
        if (cover){
            File file = createFile(fileName, cover);
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file),
                String.valueOf(encode));
            writer.write(data);
            writer.flush();
            writer.close();
        } else {
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(fileName, true),
                String.valueOf(encode));
            writer.write(data);
            writer.flush();
            writer.close();
        }
    }


    public static void createDir(String destDirName, boolean cover) {
        File dir = new File(destDirName);
        if (dir.exists()) {
            if (cover){
                deleteDir(dir);
            } else {
                throw new AppException(AppErrorCode.USER_CONFIG_ERROR,
                    String.format("创建目录%s 失败，目标目录已存在！", destDirName));
            }
        }
        if (!destDirName.endsWith(File.separator)) {
            destDirName = destDirName + File.separator;
        }
        //创建目录
        if (dir.mkdirs()) {
            log.info("create dirctionary success!");
        } else {
            throw new AppException(AppErrorCode.USER_CONFIG_ERROR,
                String.format("创建目录%s 失败！", destDirName));
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        // 目录此时为空，可以删除
        return dir.delete();
    }


    public static String createTempFile(String prefix, String suffix, String dirName) {
        File tempFile = null;
        if (dirName == null) {
            try{
                //在默认文件夹下创建临时文件
                tempFile = File.createTempFile(prefix, suffix);
                //返回临时文件的路径
                return tempFile.getCanonicalPath();
            } catch (IOException e) {
                throw new AppException(AppErrorCode.USER_CONFIG_ERROR,
                    "创建临时文件失败！", e.getStackTrace());
            }
        } else {
            File dir = new File(dirName);
            //如果临时文件所在目录不存在，首先创建
            if (!dir.exists()) {
                createDir(dirName, true);
            }
            try {
                //在指定目录下创建临时文件
                tempFile = File.createTempFile(prefix, suffix, dir);
                return tempFile.getCanonicalPath();
            } catch (IOException e) {
                throw new AppException(AppErrorCode.USER_CONFIG_ERROR,
                    "创建临时文件失败！", e.getStackTrace());
            }
        }
    }
}
