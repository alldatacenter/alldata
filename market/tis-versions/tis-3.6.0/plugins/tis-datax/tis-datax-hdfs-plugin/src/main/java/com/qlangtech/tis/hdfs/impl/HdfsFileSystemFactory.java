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
package com.qlangtech.tis.hdfs.impl;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.config.ParamsConfig;
import com.qlangtech.tis.config.Utils;
import com.qlangtech.tis.config.kerberos.IKerberos;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.fs.ITISFileSystem;
import com.qlangtech.tis.fs.ITISFileSystemFactory;
import com.qlangtech.tis.kerberos.KerberosCfg;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.offline.FileSystemFactory;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.util.Progressable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2018年11月23日
 */
@Public
public class HdfsFileSystemFactory extends FileSystemFactory implements ITISFileSystemFactory {

    private static final Logger Logger = LoggerFactory.getLogger(HdfsFileSystemFactory.class);

    // private static final String KEY_FIELD_HDFS_ADDRESS = "hdfsAddress";
    private static final String KEY_FIELD_HDFS_SITE_CONTENT = "hdfsSiteContent";

    @FormField(identity = true, ordinal = 0, validate = {Validator.require, Validator.identity})
    public String name;

    @FormField(ordinal = 1, type = FormFieldType.ENUM, advance = true, validate = {Validator.require})
    public Boolean userHostname;

//    @FormField(ordinal = 4, validate = {Validator.require, Validator.url})
//    public String hdfsAddress;

    @FormField(ordinal = 7, validate = {Validator.require, Validator.absolute_path})
    public String rootDir;

    @FormField(ordinal = 8, type = FormFieldType.SELECTABLE, advance = true, validate = {})
    public String kerberos;


    @FormField(ordinal = 10, type = FormFieldType.TEXTAREA, validate = {Validator.require})
    public String hdfsSiteContent;

    private ITISFileSystem fileSystem;

    @Override
    public String identityValue() {
        return this.name;
    }

    public static String dftHdfsSiteContent() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "\n" +
                "<configuration>\n" +
                "    <property>\n" +
                "        <name>" + CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY + "</name>\n" +
                "        <value></value>\n" +
                "    </property>\n" +
                "</configuration> ";
    }

    @Override
    public ITISFileSystem getFileSystem() {
        if (fileSystem == null) {
            Configuration cfg = getConfiguration();
            String hdfsAddress = getFSAddress();
            if (StringUtils.isEmpty(hdfsAddress)) {
                throw new IllegalStateException("hdfsAddress can not be null");
            }
            fileSystem = new HdfsFileSystem(HdfsUtils.getFileSystem(
                    hdfsAddress, cfg), hdfsAddress, this.rootDir);
        }
        return fileSystem;
    }

    @Override
    public void setConfigFile(File cfgDir) {
        Utils.setHadoopConfig2Local(cfgDir, "hdfs-site.xml", hdfsSiteContent);
    }

    @Override
    public Configuration getConfiguration() {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(HdfsFileSystemFactory.class.getClassLoader());
            Configuration conf = new Configuration();
            try (InputStream input = new ByteArrayInputStream(hdfsSiteContent.getBytes(TisUTF8.get()))) {
                conf.addResource(input);
            }
            // 支持重连？
            conf.setInt(DFSConfigKeys.DFS_CLIENT_FAILOVER_MAX_ATTEMPTS_KEY, -1);
            // conf.setBoolean(DFSConfigKeys.DFS_CLIENT_RETRY_POLICY_ENABLED_KEY, false);
            conf.set(FsPermission.UMASK_LABEL, "000");
            // fs.defaultFS
            conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
            // conf.set(FileSystem.FS_DEFAULT_NAME_KEY, hdfsAddress);
            //https://segmentfault.com/q/1010000008473574
            Logger.info("userHostname:{}", userHostname);
            if (userHostname != null && userHostname) {
                conf.setBoolean(DFSConfigKeys.DFS_CLIENT_USE_DN_HOSTNAME, true);
            }

            // conf.set(CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY, hdfsAddress);
            conf.set("hadoop.job.ugi", "admin");
            // 这个缓存还是需要的，不然如果另外的调用FileSystem实例不是通过调用getFileSystem这个方法的进入,就调用不到了
            conf.setBoolean("fs.hdfs.impl.disable.cache", false);

            if (StringUtils.isNotEmpty(this.kerberos)) {
                Logger.info("kerberos has been enabled,name:" + this.kerberos);
                KerberosCfg kerberosCfg = KerberosCfg.getKerberosCfg(this.kerberos);
                kerberosCfg.setConfiguration(conf);
            }

            conf.reloadConfiguration();
            return conf;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    @Override
    public String getFSAddress() {
        return getConfiguration().get(CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY);
    }
//
//    public void setHdfsAddress(String hdfsAddress) {
//        this.hdfsAddress = hdfsAddress;
//    }

    public String getHdfsSiteContent() {
        return hdfsSiteContent;
    }

    public void setHdfsSiteContent(String hdfsSiteContent) {
        this.hdfsSiteContent = hdfsSiteContent;
    }

    private static class HdfsUtils {

        private static final Map<String, FileSystem> fileSys = new HashMap<String, FileSystem>();

        public static FileSystem getFileSystem(String hdfsAddress, Configuration config) {

            FileSystem fileSystem = fileSys.get(hdfsAddress);
            if (fileSystem == null) {
                synchronized (HdfsUtils.class) {


                    try {


                        fileSystem = fileSys.get(hdfsAddress);
                        if (fileSystem == null) {
//                            Configuration conf = new Configuration();
//                            conf.set(FsPermission.UMASK_LABEL, "000");
//                            // fs.defaultFS
//                            conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
//                            conf.set(FileSystem.FS_DEFAULT_NAME_KEY, hdfsAddress);
//                            //https://segmentfault.com/q/1010000008473574
//                            Logger.info("userHostname:{}", userHostname);
//                            if (userHostname != null && userHostname) {
//                                conf.set("dfs.client.use.datanode.hostname", "true");
//                            }
//
//                            conf.set("fs.default.name", hdfsAddress);
//                            conf.set("hadoop.job.ugi", "admin");
//                            try (InputStream input = new ByteArrayInputStream(hdfsContent.getBytes(TisUTF8.get()))) {
//                                conf.addResource(input);
//                                // 这个缓存还是需要的，不然如果另外的调用FileSystem实例不是通过调用getFileSystem这个方法的进入,就调用不到了
//                                conf.setBoolean("fs.hdfs.impl.disable.cache", false);
                            fileSystem = new FilterFileSystem(FileSystem.get(config)) {
                                @Override
                                public boolean delete(Path f, boolean recursive) throws IOException {
                                    try {
                                        return super.delete(f, recursive);
                                    } catch (Exception e) {
                                        throw new RuntimeException("path:" + f, e);
                                    }
                                }

                                @Override
                                public boolean mkdirs(Path f, FsPermission permission) throws IOException {
                                    return super.mkdirs(f, FsPermission.getDirDefault());
                                }

                                @Override
                                public FSDataOutputStream create(Path f, FsPermission permission
                                        , boolean overwrite, int bufferSize, short replication, long blockSize, Progressable progress) throws IOException {

//                                    if (f.getName().indexOf("hoodie.properties") > -1) {
//                                        throw new IllegalStateException("not support:" + f.getName());
//                                    }

                                    return super.create(f, FsPermission.getDefault(), overwrite, bufferSize, replication, blockSize, progress);
                                }

                                @Override
                                public FileStatus[] listStatus(Path f) throws IOException {
                                    try {
                                        return super.listStatus(f);
                                    } catch (Exception e) {
                                        throw new RuntimeException("path:" + f, e);
                                    }
                                }

                                @Override
                                public void close() throws IOException {
                                    // super.close();
                                    // 设置不被关掉
                                }
                            };
                            fileSystem.listStatus(new Path("/"));
                            Logger.info("successful create hdfs with hdfsAddress:" + hdfsAddress);
                            fileSys.put(hdfsAddress, fileSystem);
                        }

                    } catch (Throwable e) {
                        throw new RuntimeException("link faild:" + hdfsAddress, e);
                    } finally {

                    }
                }

            }
            return fileSystem;
        }


    }


    @TISExtension(ordinal = 0)
    public static class DefaultDescriptor extends Descriptor<FileSystemFactory> {
        public DefaultDescriptor() {
            super();
            this.registerSelectOptions(IKerberos.IDENTITY, () -> ParamsConfig.getItems(IKerberos.IDENTITY));
        }

        @Override
        public String getDisplayName() {
            return "HDFS";
        }

        @Override
        protected boolean verify(IControlMsgHandler msgHandler, Context context, PostFormVals postFormVals) {
            String hdfsAddress = null;
            try {
                FileSystemFactory hdfsFactory = postFormVals.newInstance(this, msgHandler);
                Configuration conf = hdfsFactory.getConfiguration();
                hdfsAddress = conf.get(CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY);
                if (StringUtils.isEmpty(hdfsAddress)) {
                    msgHandler.addFieldError(context, KEY_FIELD_HDFS_SITE_CONTENT
                            , "必须要包含属性'" + CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY + "'");
                    return false;
                }

                ITISFileSystem hdfs = hdfsFactory.getFileSystem();
                hdfs.listChildren(hdfs.getPath("/"));
                msgHandler.addActionMessage(context, "hdfs连接:" + hdfsAddress + "连接正常");
                hdfs.close();
                return true;
            } catch (Exception e) {
                Logger.warn(e.getMessage(), e);
                msgHandler.addFieldError(context, KEY_FIELD_HDFS_SITE_CONTENT, "请检查连接地址，服务端是否能正常,"
                        + CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY + "=" + hdfsAddress + ",错误:" + e.getMessage());
                return false;
            }
        }

        public boolean validate() {
            return true;
        }
    }
}

