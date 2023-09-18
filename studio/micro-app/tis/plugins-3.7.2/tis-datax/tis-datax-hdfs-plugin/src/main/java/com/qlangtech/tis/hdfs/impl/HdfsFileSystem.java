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

import com.qlangtech.tis.fs.FSDataInputStream;
import com.qlangtech.tis.fs.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.fs.permission.FsPermission;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

//import org.apache.hadoop.hdfs.client.HdfsDataInputStream;

/**
 * 相关的类是:TisAbstractDirectory
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2018年11月23日
 */
public class HdfsFileSystem implements ITISFileSystem {

    private final FileSystem fs;

    private static final String HDFS = "hdfs";

    @Override
    public String getName() {
        return HDFS;
    }

    public static final String HDFS_SYNC_BLOCK = "solr.hdfs.sync.block";

    public static final int BUFFER_SIZE = 16384;
    private final String rootDir;
    private final String hdfsAddress;

    public HdfsFileSystem(FileSystem fs, String hdfsAddress, String rootDir) {
        super();
        this.fs = fs;
        this.rootDir = rootDir;
        this.hdfsAddress = hdfsAddress;
    }

    @Override
    public FileSystem unwrap() {
        return this.fs;
    }

    @Override
    public IPath getRootDir() {
        return this.getPath(this.getPath(hdfsAddress), rootDir);
    }

    @Override
    public IFileSplitor getSplitor(IPath path) throws Exception {
        Path p = unwrap(path);
        return new HDFSFileSplitor(p, this.fs);
    }

    @Override
    public TISFSDataOutputStream create(IPath f, boolean overwrite, int bufferSize) throws IOException {
        FSDataOutputStream output = fs.create(unwrap(f), overwrite, bufferSize);
        return new HdfsDataOutputStream(output);
    }

    @Override
    public TISFSDataOutputStream create(IPath f, boolean overwrite) throws IOException {
        FSDataOutputStream output = fs.create(unwrap(f), overwrite, BUFFER_SIZE);
        return new HdfsDataOutputStream(output);
    }

    @Override
    public boolean exists(IPath path) {
        try {
            return fs.exists(unwrap(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Path unwrap(IPath path) {
        return path.unwrap(Path.class);
    }

    @Override
    public boolean mkdirs(IPath f) throws IOException {
        return fs.mkdirs(unwrap(f));
    }

    @Override
    public void rename(IPath from, IPath to) {
        try {
            fs.rename(unwrap(from), unwrap(to));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<IPathInfo> listChildren(IPath path) {
        try {
            FileStatus[] status = fs.listStatus(unwrap(path));
            return convert2PathInfo(status);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected List<IPathInfo> convert2PathInfo(FileStatus[] status) {
        List<IPathInfo> children = new ArrayList<>();
        for (FileStatus s : status) {
            children.add(new DefaultIPathInfo(s));
        }
        return children;
    }

    @Override
    public List<IPathInfo> listChildren(IPath path, IPathFilter filter) {
        try {
            FileStatus[] status = fs.listStatus(unwrap(path), new PathFilter() {

                @Override
                public boolean accept(Path path) {
                    return filter.accept(getPath(path.toString()));
                }
            });
            return convert2PathInfo(status);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private class DefaultIPathInfo implements IPathInfo {

        private final FileStatus stat;

        public DefaultIPathInfo(FileStatus stat) {
            super();
            this.stat = stat;
        }

        @Override
        public String getName() {
            return stat.getPath().getName();
        }

        @Override
        public IPath getPath() {
            return HdfsFileSystem.this.getPath(stat.getPath().toString());
        }

        @Override
        public boolean isDir() {
            return stat.isDirectory();
        }

        @Override
        public long getModificationTime() {
            return stat.getModificationTime();
        }

        @Override
        public long getLength() {
            return stat.getLen();
        }
    }

    @Override
    public boolean delete(IPath f) throws IOException {
        return fs.delete(this.unwrap(f), true);
    }

    @Override
    public FSDataInputStream open(IPath path) {
        try {
            org.apache.hadoop.fs.FSDataInputStream input = fs.open(this.unwrap(path));
            return new DefaultFSDataInputStream(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class DefaultFSDataInputStream extends FSDataInputStream {

        public DefaultFSDataInputStream(org.apache.hadoop.fs.FSDataInputStream in) {
            super(in);
        }

        @Override
        public void readFully(long position, byte[] buffer, int offset, int length) throws IOException {
            ((org.apache.hadoop.fs.FSDataInputStream) this.in).readFully(position, buffer, offset, length);
        }

        @Override
        public void seek(long position) {
            //try {
            ((FSDataInputStream) this.in).seek(position);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
        }
    }

    @Override
    public void copyToLocalFile(IPath srcPath, File dstPath) {
        try {
            this.fs.copyToLocalFile(this.unwrap(srcPath), new Path(dstPath.getAbsolutePath()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public IPath getPath(String path) {
        return new HdfsPath(path);
    }

    @Override
    public IPath getPath(IPath parent, String name) {
        return new HdfsPath(parent, name);
    }

    @Override
    public OutputStream getOutputStream(IPath p) {
        try {
            Path path = p.unwrap(Path.class);
            Configuration conf = fs.getConf();
            FsServerDefaults fsDefaults = fs.getServerDefaults(path);
            EnumSet<CreateFlag> flags = EnumSet.of(CreateFlag.CREATE, CreateFlag.OVERWRITE);
            if (Boolean.getBoolean(HDFS_SYNC_BLOCK)) {
                flags.add(CreateFlag.SYNC_BLOCK);
            }
            return fs.create(path, FsPermission.getDefault().applyUMask(FsPermission.getUMask(conf)), flags, fsDefaults.getFileBufferSize(), fsDefaults.getReplication(), fsDefaults.getBlockSize(), null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FSDataInputStream open(IPath path, int bufferSize) {
        try {
            org.apache.hadoop.fs.FSDataInputStream input = fs.open(this.unwrap(path), bufferSize);
            return new DefaultFSDataInputStream(input);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public IContentSummary getContentSummary(IPath path) {
        try {
            final ContentSummary summary = fs.getContentSummary(this.unwrap(path));
            return new IContentSummary() {

                @Override
                public long getLength() {
                    return summary.getLength();
                }
            };
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public IPathInfo getFileInfo(IPath path) {
        try {
            FileStatus status = fs.getFileStatus(this.unwrap(path));
            return new DefaultIPathInfo(status);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean delete(IPath f, boolean recursive) throws IOException {
        return fs.delete(this.unwrap(f), recursive);
    }

    @Override
    public void close() {
    }

    @Override
    public boolean copyFromLocalFile(File localIncrPath, IPath remoteIncrPath) {
        return false;
    }
}
