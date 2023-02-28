/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.fs;

import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2018年11月23日
 */
public interface ITISFileSystem {
    String NAME_LOCAL_FS = "localFileSys";
    Pattern DATE_PATTERN = Pattern.compile("20\\d{12}");

    /**
     * 删除历史文件
     */
    default void deleteHistoryFile(EntityName dumpTable) throws Exception {
        FSHistoryFileUtils.deleteHistoryFile(this, dumpTable);
    }

    default void deleteHistoryFile(EntityName dumpTable, String timestamp) throws Exception {
        FSHistoryFileUtils.deleteHistoryFile(this, dumpTable, timestamp);
    }

    /**
     * 文件系统的根目录
     *
     * @return
     */
    public IPath getRootDir();

    /**
     * 取得文件系统的名称 hdfs ，OSS，或者其他
     *
     * @return
     */
    public String getName();

    public IPath getPath(String path);

    public IPath getPath(IPath parent, String name);

    public OutputStream getOutputStream(IPath path);

    public FSDataInputStream open(IPath path, int bufferSize);

    public FSDataInputStream open(IPath path);

    public TISFSDataOutputStream create(IPath f, boolean overwrite, int bufferSize) throws IOException;

    public TISFSDataOutputStream create(IPath f, boolean overwrite) throws IOException;

    public boolean exists(IPath path);

    public boolean mkdirs(IPath f) throws IOException;

    public void copyToLocalFile(IPath srcPath, File dstPath);

    public void rename(IPath from, IPath to);

    public boolean copyFromLocalFile(File localIncrPath, IPath remoteIncrPath);

    public IFileSplitor getSplitor(IPath path) throws Exception;

    /**
     * 路径内容汇总信息
     *
     * @param path
     * @return
     */
    public IContentSummary getContentSummary(IPath path);

    /**
     * 取得子目录信息
     *
     * @param path
     * @return
     */
    public List<IPathInfo> listChildren(IPath path);

    public List<IPathInfo> listChildren(IPath path, IPathFilter filter);

    /**
     * 取得文件信息
     *
     * @param path
     * @return
     */
    public IPathInfo getFileInfo(IPath path);

    public boolean delete(IPath f, boolean recursive) throws IOException;

    public boolean delete(IPath f) throws IOException;

    public void close();

    public interface IPathFilter {

        /**
         * Tests whether or not the specified abstract pathname should be included in a
         * pathname list.
         *
         * @param path The abstract pathname to be tested
         * @return <code>true</code> if and only if <code>pathname</code> should be
         * included
         */
        boolean accept(IPath path);
    }

    /**
     * 取得被包裹的 SPI实现对象
     *
     * @param
     * @return
     */
    public <T> T unwrap();
}
