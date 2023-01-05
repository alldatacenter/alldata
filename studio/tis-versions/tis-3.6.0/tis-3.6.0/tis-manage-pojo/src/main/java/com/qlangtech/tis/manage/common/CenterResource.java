/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qlangtech.tis.manage.common;

import com.google.common.collect.Lists;
import com.qlangtech.tis.pubhook.common.RunEnvironment;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 中央资源
 *
 * @author 百岁（baisui@qlangtech.com）
 * @create: 2020-04-26 21:14
 */
public class CenterResource {
    private static final Logger logger = LoggerFactory.getLogger(CenterResource.class);

    //public static  PluginAndCfgsSnapshot

    public static final String KEY_LAST_MODIFIED_EXTENDION = ".lastmodified";

    public static final String KEY_notFetchFromCenterRepository = "notFetchFromCenterRepository";

    private static Boolean notFetchFromCenterRepository;

    public static boolean notFetchFromCenterRepository() {
        if (notFetchFromCenterRepository == null) {
            notFetchFromCenterRepository = Boolean.getBoolean(KEY_notFetchFromCenterRepository);
            logger.info("notFetchFromCenterRepository:{}", notFetchFromCenterRepository);
        }
        return notFetchFromCenterRepository;
    }

    public static void setNotFetchFromCenterRepository() {
        //System.setProperty(KEY_notFetchFromCenterRepository, String.valueOf(true));

        setFetchFromCenterRepository(true);
    }

    public static void setFetchFromCenterRepository(boolean notFetchFromCenterRepository) {
        System.setProperty(KEY_notFetchFromCenterRepository, String.valueOf(notFetchFromCenterRepository));
        CenterResource.notFetchFromCenterRepository = null;
    }

    private static void copyFromRemote2Local(final URL url, final File local) {
        copyFromRemote2Local(url, local, false);
    }

    /**
     * 远程文件拷贝到本地
     *
     * @param filePath
     */
    public static File copyFromRemote2Local(String filePath, boolean isConfig) {
        URL url = getPathURL((isConfig ? Config.SUB_DIR_CFG_REPO : Config.SUB_DIR_LIBS) + "/" + filePath);
        File local = new File(isConfig ? Config.getMetaCfgDir() : Config.getLibDir(), filePath);
        copyFromRemote2Local(url, local, false);
        return local;
    }

    public static List<String> getSubDirs(final String relativePath) {
        return getSubFiles(relativePath, true, false);
    }

    /**
     * 同步配置自文件
     *
     * @param relativePath
     * @return
     */
    public static List<File> synchronizeSubFiles(String relativePath) {
        List<String> subFiles = CenterResource.getSubFiles(relativePath, false, true);
        List<File> subs = Lists.newArrayList();
        for (String f : subFiles) {
            subs.add(CenterResource.copyFromRemote2Local(CenterResource.getPath(relativePath, f), true));
        }
        return subs;
    }

    /**
     * @param
     * @param dir  取子文件的目录
     * @param file 取子文件
     * @return
     */
    public static List<String> getSubFiles(String relativePath, boolean dir, boolean file) {
        // 是否取本地文件
        if (notFetchFromCenterRepository()) {
            File parent = new File(Config.getMetaCfgDir(), relativePath);
            if (!parent.exists()) {
                throw new IllegalStateException("parent:" + parent.getAbsolutePath() + " is not exist");
            }
            File c = null;
            List<String> suNames = Lists.newArrayList();
            for (String child : parent.list()) {
                c = new File(parent, child);
                if ((c.isFile() && file) || (c.isDirectory() && dir)) {
                    suNames.add(child);
                }
            }
            return suNames;
        }
        final URL url = getPathURL(Config.SUB_DIR_CFG_REPO, relativePath);
        List<String> subDirs = Lists.newArrayList();
        subDirs.addAll(HttpUtils.get(url, new ConfigFileContext.StreamProcess<List<String>>() {
            @Override
            public List<ConfigFileContext.Header> getHeaders() {
                return HEADER_GET_META;
            }

            @Override
            public List<String> p(int status, InputStream stream, Map<String, List<String>> headerFields) {
                if (isTargetResourceNotExist(headerFields)) {
                    return Collections.emptyList();
                }
                List<String> subChild = headerFields.get(ConfigFileContext.KEY_HEAD_FILES);
                Optional<String> first = subChild.stream().findFirst();
                if (!first.isPresent()) {
                    return Collections.emptyList();
                } else {
                    List<String> result = Lists.newArrayList();
                    String[] childs = StringUtils.split(first.get(), ",");
                    for (String c : childs) {
                        // 说明是文件夹
                        if (dir && StringUtils.endsWith(c, ":d")) {
                            result.add(StringUtils.substringBefore(c, ":"));
                        }
                        if (file && StringUtils.endsWith(c, ":f")) {
                            result.add(StringUtils.substringBefore(c, ":"));
                        }
                    }
                    return result;
                }
            }
        }));
        return subDirs;
    }

    /**
     * @param url
     * @param local
     * @param directDownload 取得目标文件的元数据信息，比如最新更新时间
     * @return 是否已经更新本地文件
     */
    public static boolean copyFromRemote2Local(final URL url, final File local, boolean directDownload) {
        if (notFetchFromCenterRepository()) {
            return false;
        }
        // final File lastModifiedFile = new File(local.getParentFile(), local.getName() + KEY_LAST_MODIFIED_EXTENDION);
        // ShallWriteLocalResult shallWriteLocal = null;
        // if (!directDownload) {
//            shallWriteLocal = HttpUtils.get(url, new ConfigFileContext.StreamProcess<ShallWriteLocalResult>() {
//                @Override
//                public List<ConfigFileContext.Header> getHeaders() {
//                    return HEADER_GET_META;
//                }
//
//                @Override
//                public ShallWriteLocalResult p(int status, InputStream stream, Map<String, List<String>> headerFields) {
//                    return shallWriteLocal(headerFields, url, local, lastModifiedFile);
//                }
//            });
//            if (!shallWriteLocal.shall) {
//                return false;
//            }
        //}
        // ShallWriteLocalResult[] shallWriteLocalAry = new ShallWriteLocalResult[]{shallWriteLocal};
        return HttpUtils.get(url, new ConfigFileContext.StreamProcess<Boolean>() {

            @Override
            public Boolean p(int status, InputStream stream, Map<String, List<String>> headerFields) {

                FileUtils.deleteQuietly(local);
                if (isTargetResourceNotExist(headerFields)) {
                    logger.warn("remote resource not exist:" + url);
                    return false;
                }
//                if (shallWriteLocalAry[0] == null) {
//                    if (!(shallWriteLocalAry[0] = shallWriteLocal(headerFields, url, local, lastModifiedFile)).shall) {
//                        return false;
//                    }
//                }
                // Assert.assertTrue("shallWriteLocalAry shall be true", shallWriteLocalAry[0].shall);

                // long lastUpdate = shallWriteLocalAry[0].lastUpdateTimestamp;// getLastUpdateTimeStamp(headerFields, url);
                try {

                    FileUtils.copyInputStreamToFile(stream, local);
                } catch (IOException e) {
                    throw new RuntimeException("local file:" + local.getAbsolutePath(), e);
                }
//                try {
//                    FileUtils.write(lastModifiedFile, String.valueOf(lastUpdate), TisUTF8.get());
//                } catch (IOException e) {
//                    throw new RuntimeException("can not write:" + lastUpdate + " to lastModifiedFile:" + lastModifiedFile.getAbsolutePath(), e);
//                }
                return true;
            }
        });
    }

//    /**
//     * 根据远端返回的元数据信息判断是否需要写本地文件
//     *
//     * @param headerFields
//     * @param url
//     * @param local
//     * @return
//     */
//    private static ShallWriteLocalResult shallWriteLocal(Map<String, List<String>> headerFields, URL url, File local, File lastModifiedFile) {
//        ShallWriteLocalResult result = new ShallWriteLocalResult();
//        result.shall = false;
//        List<String> notExist = null;
//        if (isTargetResourceNotExist(headerFields)) {
//            // 远端文件不存在不需要拷贝
//            logger.warn("remote file not exist:{},local:", url, local.getAbsolutePath());
//            return result;
//        }
//
//        long lastUpdate = getLastUpdateTimeStamp(headerFields, url);
//        result.lastUpdateTimestamp = lastUpdate;
//        if (local.exists()) {
//            long localLastModified = 0;
//            try {
//                localLastModified = Long.parseLong(FileUtils.readFileToString(lastModifiedFile, TisUTF8.get()));
//            } catch (Throwable e) {
//            }
//            if (lastUpdate <= localLastModified) {
//                return result;
//            }
//        }
//        result.shall = true;
//        return result;
//    }

    /**
     * 目标资源是否存在?
     *
     * @param headerFields
     * @return
     */
    private static boolean isTargetResourceNotExist(Map<String, List<String>> headerFields) {
        List<String> notExist;
        return (notExist = headerFields.get(ConfigFileContext.KEY_HEAD_FILE_NOT_EXIST)) != null
                && notExist.contains(Boolean.TRUE.toString());
    }

//    private static class ShallWriteLocalResult {
//        boolean shall;
//        long lastUpdateTimestamp;
//    }

//    private static long getLastUpdateTimeStamp(Map<String, List<String>> headerFields, URL url) {
//        Optional<String> first = null;
//        List<String> lastupdate = headerFields.get(ConfigFileContext.KEY_HEAD_LAST_UPDATE);
//        if (lastupdate == null || !(first = lastupdate.stream().findFirst()).isPresent()) {
//            throw new IllegalStateException("url:" + url + " can not find " + ConfigFileContext.KEY_HEAD_LAST_UPDATE + " in headers");
//        }
//        return Long.parseLong(first.get());
//    }

    public static String getPath(String parent, String filePath) {
        boolean parentEndWithSlash = StringUtils.endsWith(parent, "/");
        boolean childStartWithSlash = StringUtils.startsWith(filePath, "/");
        if (parentEndWithSlash && childStartWithSlash) {
            filePath = parent + StringUtils.substring(filePath, 1);
        } else if (!parentEndWithSlash && !childStartWithSlash) {
            filePath = parent + "/" + filePath;
        } else {
            filePath = parent + filePath;
        }
        return filePath;
    }

    public static URL getPathURL(String parent, String filePath) {
        return getPathURL(getPath(parent, filePath));
    }

    public static URL getPathURL(String filePath) {
        try {
            final RunEnvironment runtime = RunEnvironment.getSysRuntime();
            return new URL(runtime.getInnerRepositoryURL() + "/config/stream_script_repo.action?path=" + URLEncoder.encode(filePath, TisUTF8.getName()));
        } catch (Exception e) {
            throw new RuntimeException("filepath:" + filePath, e);
        }
    }
}
