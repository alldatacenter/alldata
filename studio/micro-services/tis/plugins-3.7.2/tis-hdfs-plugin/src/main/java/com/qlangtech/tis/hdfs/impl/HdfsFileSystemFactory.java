/**
 * Copyright (c) 2020 QingLang, Inc. <baisui@qlangtech.com>
 * <p>
 * This program is free software: you can use, redistribute, and/or modify
 * it under the terms of the GNU Affero General Public License, version 3
 * or later ("AGPL"), as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.qlangtech.tis.hdfs.impl;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.fs.ITISFileSystem;
import com.qlangtech.tis.fs.ITISFileSystemFactory;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.offline.FileSystemFactory;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import com.qlangtech.tis.util.IPluginContext;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.util.Progressable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2018年11月23日
 */
public class HdfsFileSystemFactory extends FileSystemFactory implements ITISFileSystemFactory {

    private static final Logger Logger = LoggerFactory.getLogger(HdfsFileSystemFactory.class);

    private static final String KEY_FIELD_HDFS_ADDRESS = "hdfsAddress";

    @FormField(identity = true, ordinal = 0, validate = {Validator.require, Validator.identity})
    public String name;

    @FormField(ordinal = 1, validate = {Validator.require, Validator.url})
    public String hdfsAddress;

    @FormField(ordinal = 2, validate = {Validator.require})
    public String rootDir;

    @FormField(ordinal = 3, type = FormFieldType.TEXTAREA, validate = {Validator.require})
    public String hdfsSiteContent;

    private ITISFileSystem fileSystem;

    @Override
    public ITISFileSystem getFileSystem() {
        if (fileSystem == null) {
            fileSystem = new HdfsFileSystem(HdfsUtils.getFileSystem(hdfsAddress, hdfsSiteContent), this.rootDir);
        }
        return fileSystem;
    }


    public String getHdfsAddress() {
        return hdfsAddress;
    }

    public void setHdfsAddress(String hdfsAddress) {
        this.hdfsAddress = hdfsAddress;
    }

    public String getHdfsSiteContent() {
        return hdfsSiteContent;
    }

    public void setHdfsSiteContent(String hdfsSiteContent) {
        this.hdfsSiteContent = hdfsSiteContent;
    }

    private static class HdfsUtils {

        private static final Map<String, FileSystem> fileSys = new HashMap<String, FileSystem>();

        public static FileSystem getFileSystem(String hdfsAddress, String hdfsContent) {

            FileSystem fileSystem = fileSys.get(hdfsAddress);
            if (fileSystem == null) {
                synchronized (HdfsUtils.class) {

                    final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                    try {
                        Thread.currentThread().setContextClassLoader(HdfsFileSystemFactory.class.getClassLoader());

                        fileSystem = fileSys.get(hdfsAddress);
                        if (fileSystem == null) {
                            Configuration conf = new Configuration();
                            conf.set(FsPermission.UMASK_LABEL, "000");
                            // fs.defaultFS
                            conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
                            conf.set(FileSystem.FS_DEFAULT_NAME_KEY, hdfsAddress);
                            conf.set("fs.default.name", hdfsAddress);
                            conf.set("hadoop.job.ugi", "admin");
                            try (InputStream input = new ByteArrayInputStream(hdfsContent.getBytes(TisUTF8.get()))) {
                                conf.addResource(input);
                                // 这个缓存还是需要的，不然如果另外的调用FileSystem实例不是通过调用getFileSystem这个方法的进入的化就调用不到了
                                conf.setBoolean("fs.hdfs.impl.disable.cache", false);
                                fileSystem = new FilterFileSystem(FileSystem.get(conf)) {
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
                                    public FSDataOutputStream create(Path f, FsPermission permission, boolean overwrite, int bufferSize, short replication, long blockSize, Progressable progress) throws IOException {
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
                                fileSys.put(hdfsAddress, fileSystem);
                            }
                        }
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    } finally {
                        Thread.currentThread().setContextClassLoader(contextClassLoader);
                    }
                }

            }
            return fileSystem;
        }


    }


    @TISExtension(ordinal = 0)
    public static class DefaultDescriptor extends Descriptor<FileSystemFactory> {
        @Override
        public String getDisplayName() {
            return "HDFS";
        }

        @Override
        protected boolean verify(IControlMsgHandler msgHandler, Context context, PostFormVals postFormVals) {
            ParseDescribable<FileSystemFactory> fs = null;
            try {
                fs = this.newInstance((IPluginContext) msgHandler, postFormVals.rawFormData, Optional.empty());
                HdfsFileSystemFactory hdfsFactory = (HdfsFileSystemFactory) fs.instance;
                ITISFileSystem hdfs = hdfsFactory.getFileSystem();
                hdfs.listChildren(hdfs.getPath("/"));
                msgHandler.addActionMessage(context, "hdfs连接:" + ((HdfsFileSystemFactory) fs.instance).hdfsAddress + "连接正常");
                return true;
            } catch (Exception e) {
                Logger.warn(e.getMessage(), e);
                msgHandler.addFieldError(context, KEY_FIELD_HDFS_ADDRESS, "请检查连接地址，服务端是否能正常,错误:" + e.getMessage());
                return false;
            }


        }

        public boolean validate() {
            return true;
        }
    }
}

