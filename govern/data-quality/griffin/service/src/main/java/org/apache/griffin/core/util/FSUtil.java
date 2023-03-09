/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.griffin.core.util;

import static org.apache.griffin.core.exception.GriffinExceptionMessage.HDFS_FILE_NOT_EXIST;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.griffin.core.exception.GriffinException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FSUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FSUtil.class);
    private static final int SAMPLE_ROW_COUNT = 100;

    private static String fsDefaultName;

    private static FileSystem fileSystem;

    private static FileSystem defaultFS = getDefaultFileSystem();

    private static FileSystem getDefaultFileSystem() {
        FileSystem fs = null;
        Configuration conf = new Configuration();
        try {
            fs = FileSystem.get(conf);
        } catch (Exception e) {
            LOGGER.error("Can not get default hdfs file system. {}", e);
        }
        return fs;
    }

    private static FileSystem getFileSystem() {
        if (fileSystem == null) {
            initFileSystem();
        }
        return fileSystem;
    }

    public FSUtil(@Value("${fs.defaultFS}") String defaultName) {
        fsDefaultName = defaultName;
    }

    private static void initFileSystem() {
        Configuration conf = new Configuration();
        if (!StringUtils.isEmpty(fsDefaultName)) {
            conf.set("fs.defaultFS", fsDefaultName);
            LOGGER.info("Setting fs.defaultFS:{}", fsDefaultName);
        }

        if (StringUtils.isEmpty(conf.get("fs.hdfs.impl"))) {
            LOGGER.info("Setting fs.hdfs.impl:{}", org.apache.hadoop.hdfs.DistributedFileSystem.class.getName());
            conf.set("fs.hdfs.impl", org.apache.hadoop.hdfs.DistributedFileSystem.class.getName());
        }
        if (StringUtils.isEmpty(conf.get("fs.file.impl"))) {
            LOGGER.info("Setting fs.file.impl:{}", org.apache.hadoop.fs.LocalFileSystem.class.getName());
            conf.set("fs.file.impl", org.apache.hadoop.fs.LocalFileSystem.class.getName());
        }
        try {
            fileSystem = FileSystem.get(conf);
        } catch (Exception e) {
            LOGGER.error("Can not get hdfs file system. {}", e);
            fileSystem = defaultFS;
        }

    }


    /**
     * list all sub dir of a dir
     */
    public static List<String> listSubDir(String dir) throws IOException {
        checkHDFSConf();
        List<String> fileList = new ArrayList<>();
        Path path = new Path(dir);
        if (fileSystem.isFile(path)) {
            return fileList;
        }
        FileStatus[] statuses = fileSystem.listStatus(path);
        for (FileStatus fileStatus : statuses) {
            if (fileStatus.isDirectory()) {
                fileList.add(fileStatus.getPath().toString());
            }
        }
        return fileList;

    }

    /**
     * get all file status of a dir.
     */
    public static List<FileStatus> listFileStatus(String dir) throws IOException {
        checkHDFSConf();
        List<FileStatus> fileStatusList = new ArrayList<>();
        Path path = new Path(dir);
        if (fileSystem.isFile(path)) {
            return fileStatusList;
        }
        FileStatus[] statuses = fileSystem.listStatus(path);
        for (FileStatus fileStatus : statuses) {
            if (!fileStatus.isDirectory()) {
                fileStatusList.add(fileStatus);
            }
        }
        return fileStatusList;
    }

    /**
     * touch file
     */
    public static void touch(String filePath) throws IOException {
        checkHDFSConf();
        Path path = new Path(filePath);
        FileStatus st;
        if (fileSystem.exists(path)) {
            st = fileSystem.getFileStatus(path);
            if (st.isDirectory()) {
                throw new IOException(filePath + " is a directory");
            } else if (st.getLen() != 0) {
                throw new IOException(filePath + " must be a zero-length file");
            }
        }
        FSDataOutputStream out = null;
        try {
            out = fileSystem.create(path);
        } finally {
            if (out != null) {
                out.close();
            }
        }

    }

    public static boolean isFileExist(String path) throws IOException {
        checkHDFSConf();
        Path hdfsPath = new Path(path);
        return fileSystem.isFile(hdfsPath) || fileSystem.isDirectory(hdfsPath);
    }

    public static InputStream getSampleInputStream(String path)
        throws IOException {
        checkHDFSConf();
        if (isFileExist(path)) {
            FSDataInputStream missingData = fileSystem.open(new Path(path));
            BufferedReader bufReader = new BufferedReader(
                new InputStreamReader(missingData, Charsets.UTF_8));
            try {
                String line = null;
                int rowCnt = 0;
                StringBuilder output = new StringBuilder(1024);

                while ((line = bufReader.readLine()) != null) {
                    if (rowCnt < SAMPLE_ROW_COUNT) {
                        output.append(line);
                        output.append("\n");
                    }
                    rowCnt++;
                }

                return IOUtils.toInputStream(output, Charsets.UTF_8);
            } finally {
                bufReader.close();
            }
        } else {
            LOGGER.warn("HDFS file does not exist.", path);
            throw new GriffinException.NotFoundException(HDFS_FILE_NOT_EXIST);
        }
    }

    private static void checkHDFSConf() {
        if (getFileSystem() == null) {
            throw new NullPointerException("FileSystem is null. " +
                "Please check your hdfs config default name.");
        }
    }

    public static String getFirstMissRecordPath(String hdfsDir)
        throws Exception {
        List<FileStatus> fileList = listFileStatus(hdfsDir);
        for (int i = 0; i < fileList.size(); i++) {
            if (fileList.get(i).getPath().toUri().toString().toLowerCase()
                .contains("missrecord")) {
                return fileList.get(i).getPath().toUri().toString();
            }
        }
        return null;
    }

    public static InputStream getMissSampleInputStream(String path)
        throws Exception {
        List<String> subDirList = listSubDir(path);
        //FIXME: only handle 1-sub dir here now
        for (int i = 0; i < subDirList.size(); i++) {
            return getSampleInputStream(getFirstMissRecordPath(
                subDirList.get(i)));
        }
        return getSampleInputStream(getFirstMissRecordPath(path));
    }

}
