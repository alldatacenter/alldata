/*
 * Copyright 2021 WeBank
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.wedatasphere.streamis.jobmanager.manager.util;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webank.wedatasphere.streamis.jobmanager.manager.entity.MetaJsonInfo;
import com.webank.wedatasphere.streamis.jobmanager.manager.entity.vo.PublishRequestVo;
import com.webank.wedatasphere.streamis.jobmanager.manager.exception.FileException;
import com.webank.wedatasphere.streamis.jobmanager.manager.exception.FileExceptionManager;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class ReaderUtils {
    private static final String metaFileName = "meta.txt";
    private static final String metaFileJsonName = "meta.json";
    private static final String type = "type";
    private static final String fileName = "filename";
    private static final String projectName = "projectname";
    private static final String jobName = "jobname";
    private static final String tags = "tags";
    private static final String description = "description";
    private static final String defaultTagList = "prod,streamis";
    private static final String version = "v00001";
    private static final String regex = "^[a-z0-9A-Z_-]+$";
    private static final String jarRegex = "^[a-z0-9A-Z._-]+$";
    private static final int defaultLength = 64;
    private static final int descriptionLength = 128;
    private static final List<String> supportFileType = new ArrayList<>(Arrays.asList("sql"));
    private String basePath;
    private String zipName;
    private boolean hasTags = false;
    private boolean hasProjectName = false;


    private static final Logger LOG = LoggerFactory.getLogger(ReaderUtils.class);

    public PublishRequestVo parseFile(String dirPath) throws IOException, FileException {
        getBasePath(dirPath);
        try (InputStream inputStream = generateInputStream(basePath)) {
            return read(inputStream);
        } catch (Exception e) {
            throw e;
        }
    }

    public String setName(Object name) {
        return name == null ? null : name.toString();
    }

    public boolean checkName(String fileName) {
        String name = fileName.substring(0, fileName.lastIndexOf("."));
        if (fileName.endsWith(".jar")) {
            return name.matches(jarRegex);
        }
        return name.matches(regex);
    }

    public List<String> listFiles(String path) throws FileException {
        String[] strArr = path.split("\\/");
        String shortPath = strArr[strArr.length - 1];
        String workPath = path.substring(0, path.length() - shortPath.length() - 1);
        File file = new File(workPath);
        if (!file.exists()) {
            throw new FileException(30034, "the file is not exists");
        }
        List<String> list = new ArrayList<>();
        File[] files = file.listFiles();
        if (files != null) {
            for (File filePath : files) {
                if (!metaFileJsonName.equals(filePath.getName()) && !filePath.getName().endsWith(".zip")) {
                    if (!checkName(filePath.getName())) {
                        throw FileExceptionManager.createException(30601,filePath.getName());
                    }
                    list.add(filePath.getAbsolutePath());
                }
            }
        }
        return list;
    }

    public String getFileName(String path) {
        if (StringUtils.isBlank(path)) {
            return path;
        }
        String[] strArr = path.split("\\/");
        return strArr[strArr.length - 1];
    }

    public String readAsJson(MetaJsonInfo metaJsonInfo) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(metaJsonInfo.getJobContent()).replaceAll("[\\t\\n\\r]", "");
    }

    public String readAsJson(String version, String resourceId) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> map = new HashMap<>();
        map.put("version", version);
        map.put("resourceId", resourceId);
        return objectMapper.writeValueAsString(map);
    }


    private MetaJsonInfo readJson(BufferedReader reader) throws IOException, FileException {
        String line = null;
        StringBuilder sb = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        ObjectMapper objectMapper = new ObjectMapper();
        MetaJsonInfo metaJsonInfo = objectMapper.readValue(sb.toString(), MetaJsonInfo.class);
        metaJsonInfo.setMetaInfo(objectMapper.writeValueAsString(metaJsonInfo.getJobContent()).replaceAll("[\\t\\n\\r]", ""));
        return metaJsonInfo;
    }


    public MetaJsonInfo parseJson(String dirPath) throws IOException, FileException {
        getBasePath(dirPath);
        try (InputStream inputStream = generateInputStream(basePath);
             InputStreamReader streamReader = new InputStreamReader(inputStream);
             BufferedReader reader = new BufferedReader(streamReader);) {
            return readJson(reader);
        } catch (Exception e) {
            throw e;
        }
    }

    private InputStream generateInputStream(String basePath) throws IOException, FileException {
        File metaFile = new File(basePath + File.separator + metaFileJsonName);
        if (!metaFile.exists()) {
            throw new FileException(30603, metaFileJsonName);
        }
        return IoUtils.generateInputInputStream(basePath + File.separator + metaFileJsonName);
    }

    private PublishRequestVo read(InputStream inputStream) throws IOException, FileException {
        try (InputStreamReader streamReader = new InputStreamReader(inputStream);
             BufferedReader reader = new BufferedReader(streamReader);) {
            return readFile(reader);
        } catch (Exception e) {
            throw e;
        }
    }

    private PublishRequestVo readFile(BufferedReader reader) throws IOException, FileException {
        String line = null;
        PublishRequestVo publishRequestVO = new PublishRequestVo();
        while ((line = reader.readLine()) != null) {
            String[] lineArray = line.split(":");
            if (lineArray.length <= 1) {
                LOG.error("Illegal file format(文件输入格式不正确): meta.txt, {} does not have a corresponding value(没有写对应的值)", lineArray[0]);
                break;
            }
            String key = lineArray[0].trim().toLowerCase();
            String value = lineArray[1].trim();
            if (key.equals(type)) {
                if (!supportFileType.contains(value)) {
                    throw FileExceptionManager.createException(30602, value);
                }
                publishRequestVO.setType(value);
                LOG.info("Successfully set Project type(完成设置项目的类型):{}", value);
                continue;
            }
            if (key.equals(jobName)) {
                validateItem(key, value, defaultLength);
                publishRequestVO.setStreamisJobName(value);
                LOG.info("Successfully set Porject Name(完成设置项目的任务名):{}", value);
                continue;
            }
            if (key.equals(tags)) {
                hasTags = true;
                publishRequestVO.setTags(tags);
                continue;
            }
            if (key.equals(projectName)) {
                hasProjectName = true;
                validateItem(key, value, defaultLength);
                setProjectName(value, publishRequestVO);
                continue;
            }
            if (key.equals(description)) {
                if (value.length() > descriptionLength) {
                    throw FileExceptionManager.createException(30600, key, descriptionLength);
                }
                publishRequestVO.setDescription(value);
                LOG.info("Successfully set Porject description(完成项目描述的设置)");
            }
        }

        if (!hasTags) {
            publishRequestVO.setTags(defaultTagList);
        }
        if (!hasProjectName) {
            validateItem(projectName, zipName, defaultLength);
            setProjectName(zipName, publishRequestVO);
        }
        if (publishRequestVO.getType().equals("sql")) {
            setExecutionCode(publishRequestVO);
            LOG.info("Successfully extracted execution-code from sql file(sql文件待执行内容提取完成)");
        }
        publishRequestVO.setVersion(version);
        LOG.info("Successfully parsed meta.txt(meta.txt文件解析完成).");
        return publishRequestVO;
    }

    public boolean checkFile(String path) {
        return new File(path).exists();
    }

    private void setProjectName(String name, PublishRequestVo publishRequestVO) {
        publishRequestVO.setProjectName(name);
        LOG.info("Successfully set Project name(完成设置项目的名称):{}", name);
    }

    private void setExecutionCode(PublishRequestVo publishRequestVO) throws IOException, FileException {
        String sqlFilePath = getSqlFileAbsolutePath(basePath);
        if (StringUtils.isBlank(sqlFilePath)) {
            throw FileExceptionManager.createException(30603, "sql");
        }
        if (!StringUtils.isBlank(sqlFilePath)) {
            try (InputStream inputStream = IoUtils.generateInputInputStream(sqlFilePath);
                 InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                 BufferedReader reader = new BufferedReader(inputStreamReader)) {
                StringBuilder sqlsb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sqlsb.append(line);
                }
                publishRequestVO.setExecutionCode(sqlsb.toString());
            }
        }
    }

    private String getSqlFileAbsolutePath(String filePath) {
        File[] fileList = new File(filePath).listFiles();
        if (fileList != null) {
            for (File file : fileList) {
                if (file.isFile() && file.getName().endsWith(".sql")) {
                    return file.getAbsolutePath();
                }
            }
        }
        return "";
    }

    private void validateItem(String key, String value, int length) throws FileException {
        if (value.length() > defaultLength) {
            LOG.error("{} length exceeds limit(长度超过限制)", key);
            throw FileExceptionManager.createException(30600, key, length);
        }
        if (!isLetterDigitOrChinese(value)) {
            LOG.error("{} should only contains numeric/Chinese/English characters and '_'(仅允许包含数字，中文，英文和下划线)", key);
            throw FileExceptionManager.createException(30601, key);
        }
    }

    private void getBasePath(String dirPath) {
        String[] strArr = dirPath.split("\\/");
        this.zipName = strArr[strArr.length - 1];
        this.basePath = dirPath.substring(0, dirPath.length() - zipName.length() - 1);
    }

    public static boolean isLetterDigitOrChinese(String str) {
        String regex = "^[a-zA-Z0-9_\u4e00-\u9fa5]+$";
        return str.matches(regex);
    }

}
