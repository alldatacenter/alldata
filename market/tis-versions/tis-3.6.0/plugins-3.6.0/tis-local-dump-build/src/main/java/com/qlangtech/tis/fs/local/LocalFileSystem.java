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
package com.qlangtech.tis.fs.local;

import com.google.common.collect.Lists;
import com.qlangtech.tis.fs.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * 基于本地文件系统的FileSystem实现
 *
 * @author: baisui 百岁
 * @create: 2021-03-02 13:00
 **/
public class LocalFileSystem implements ITISFileSystem {
    // private static final String NAME_LOCAL_FS = "localFileSys";
    private final String rootDir;

    public LocalFileSystem(String rootDir) {
        this.rootDir = rootDir;
    }

    @Override
    public <T> T unwrap() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRootDir() {
        return this.rootDir;
    }

    @Override
    public String getName() {
        return NAME_LOCAL_FS;
    }

    @Override
    public IPath getPath(String path) {
        return new LocalFilePath(new File(path));
    }

    @Override
    public IPath getPath(IPath parent, String name) {
        return new LocalFilePath(new File(getUnwrap(parent), name));
    }

    @Override
    public OutputStream getOutputStream(IPath path) {
        throw new UnsupportedOperationException();
    }


    @Override
    public FSDataInputStream open(IPath path, int bufferSize) {
//        File local = getUnwrap(path);
//        try {
//            return new LocalFSDataInputStream(FileUtils.openInputStream(local), bufferSize);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
        throw new UnsupportedOperationException();
    }

    @Override
    public FSDataInputStream open(IPath path) {

        File local = getUnwrap(path);
        try {
            return new LocalFSDataInputStream(FileUtils.openInputStream(local), 1024 * 5);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public TISFSDataOutputStream create(IPath f, boolean overwrite, int bufferSize) throws IOException {
        return new LocalDataOutputStream(getUnwrap(f), overwrite);
    }

    @Override
    public TISFSDataOutputStream create(IPath f, boolean overwrite) throws IOException {
        return new LocalDataOutputStream(getUnwrap(f), overwrite);
    }

    @Override
    public boolean exists(IPath path) {
        File local = getUnwrap(path);
        return local.exists();
    }

    private File getUnwrap(IPath path) {
        return path.unwrap(File.class);
    }

    @Override
    public boolean mkdirs(IPath f) throws IOException {
        File local = getUnwrap(f);
        FileUtils.forceMkdir(local);
        return true;
    }

    @Override
    public void copyToLocalFile(IPath srcPath, File dstPath) {
        File local = getUnwrap(srcPath);
        if (!local.exists()) {
            throw new IllegalStateException("local file:" + local.getAbsolutePath() + " is not exist");
        }
        try {
            FileUtils.copyFile(local, dstPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void rename(IPath from, IPath to) {

    }

    @Override
    public boolean copyFromLocalFile(File localIncrPath, IPath remoteIncrPath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IFileSplitor getSplitor(IPath path) throws Exception {
        File local = getUnwrap(path);
        if (!local.exists()) {
            throw new IllegalStateException("file is not exist:" + local.getAbsolutePath());
        }
        final long sizeOf = FileUtils.sizeOf(local);
        return new IFileSplitor() {
            @Override
            public List<IFileSplit> getSplits(IndexBuildConfig config) throws Exception {
                List<IFileSplit> splits = Lists.newArrayList();
                String[] subDir = local.list();
                for (String sub : subDir) {
                    if (StringUtils.isNumeric(sub)) {
                        collectAllDataFile(new File(local, sub), splits);
                    }
                }
                return splits;
            }


            @Override
            public long getTotalSize() {
                return sizeOf;//local.length();
            }
        };
    }

    private void collectAllDataFile(File dir, List<IFileSplit> splits) {
        if (dir.isFile()) {
            throw new IllegalArgumentException("path:" + dir.getAbsolutePath() + " must be a dir");
        }
        File dataFile = null;
        for (String f : dir.list()) {
            dataFile = new File(dir, f);
            if (dataFile.isDirectory() || dataFile.length() < 1) {
                continue;
            }
            splits.add(new LocalFileSplit(new LocalFilePath(dataFile)));
        }
    }

    @Override
    public IContentSummary getContentSummary(IPath path) {
        File f = this.getUnwrap(path);
        return () -> {
            return FileUtils.sizeOf(f);
        };
    }

    @Override
    public List<IPathInfo> listChildren(IPath path) {
        return listChildren(path, (p) -> true);
    }

    @Override
    public List<IPathInfo> listChildren(IPath path, IPathFilter filter) {
        LocalFilePath lpath = (LocalFilePath) path;

        if (!lpath.file.exists()) {
            throw new IllegalStateException("file:" + lpath.file + " is not exist");
        }
        List<IPathInfo> childInfos = Lists.newArrayList();
        LocalFilePath lfpath = null;
        for (String child : lpath.file.list()) {
            lfpath = new LocalFilePath(new File(lpath.file, child));
            if (!filter.accept(lfpath)) {
                continue;
            }
            childInfos.add(new LocalPathInfo(lfpath));
        }
        return childInfos;
    }

    @Override
    public IPathInfo getFileInfo(IPath path) {
        return new LocalPathInfo((LocalFilePath) path);
    }

    @Override
    public boolean delete(IPath f, boolean recursive) throws IOException {
        return this.delete(f);
    }

    @Override
    public boolean delete(IPath f) throws IOException {
        File local = getUnwrap(f);
        FileUtils.deleteQuietly(local);
        return true;
    }

    @Override
    public void close() {

    }
}
