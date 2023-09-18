///**
// * Licensed to the Apache Software Foundation (ASF) under one
// * or more contributor license agreements.  See the NOTICE file
// * distributed with this work for additional information
// * regarding copyright ownership.  The ASF licenses this file
// * to you under the Apache License, Version 2.0 (the
// * "License"); you may not use this file except in compliance
// * with the License.  You may obtain a copy of the License at
// * <p>
// * http://www.apache.org/licenses/LICENSE-2.0
// * <p>
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package com.qlangtech.tis.plugin.fs.aliyun.oss;
//
//import com.aliyun.oss.OSS;
//import com.aliyun.oss.OSSClientBuilder;
//import com.aliyun.oss.model.*;
//import com.qlangtech.tis.config.aliyun.IHttpToken;
//import com.qlangtech.tis.fs.*;
//import org.apache.commons.lang.StringUtils;
//
//import java.io.*;
//import java.util.List;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.stream.Collectors;
//
///**
// * @author 百岁（baisui@qlangtech.com）
// * @create: 2020-04-12 20:10
// * @date 2020/04/13
// */
//public class AliyunOSSFileSystem implements ITISFileSystem {
//
//    //private final IHttpToken aliyunToken;
//    private final OSS client;
//    private final String bucketName;
//    private final String rootDir;
//
//    private static final ExecutorService ossPutExecutor = Executors.newCachedThreadPool();
//
//    public AliyunOSSFileSystem(IHttpToken aliyunToken, String endpoint, String buket, String rootDir) {
//        this.bucketName = buket;
//        this.rootDir = rootDir;
//
//        aliyunToken.
//
//        client = new OSSClientBuilder().build(endpoint, aliyunToken.getAccessKeyId(), aliyunToken.getAccessKeySecret());
//    }
//
//    @Override
//    public OSS unwrap() {
//        return this.client;
//    }
//
//    @Override
//    public IPath getRootDir() {
//        return this.getPath(this.rootDir);
//    }
//
//    @Override
//    public String getName() {
//        return "aliyun-oss";
//    }
//
//    @Override
//    public IPath getPath(String path) {
//        // ObjectMetadata metadata = client.getObjectMetadata(this.bucketName, path);
//        return new OSSPath(path);
//    }
//
//    private class OSSPath implements IPath {
//        //private final ObjectMetadata metadata;
//        private final String path;
//
//        public OSSPath(String path) {
//            //  this.metadata = metadata;
//            this.path = path;
//        }
//
//        @Override
//        public String getName() {
//            return this.path;
//        }
//
//        @Override
//        public <T> T unwrap(Class<T> iface) {
//            //return iface.cast(this.metadata);
//            return iface.cast(this);
//        }
//    }
//
//    @Override
//    public IPath getPath(IPath parent, String name) {
//
//        boolean parentEndWithSlash = StringUtils.endsWith(parent.getName(), "/");
//        boolean childStartWithSlash = StringUtils.startsWith(name, "/");
//        String filePath = null;
//        if (parentEndWithSlash && childStartWithSlash) {
//            filePath = parent.getName() + StringUtils.substring(name, 1);
//        } else if (!parentEndWithSlash && !childStartWithSlash) {
//            filePath = parent.getName() + "/" + name;
//        } else {
//            filePath = parent.getName() + name;
//        }
//        return new OSSPath(filePath);
//    }
//
//    @Override
//    public OutputStream getOutputStream(IPath path) {
//
//        try {
//            PipedOutputStream outputStream = new PipedOutputStream();
//            PipedInputStream inputStream = new PipedInputStream(outputStream);
//            ossPutExecutor.execute(() -> {
//                this.client.putObject(this.bucketName, path.getName(), inputStream);
//            });
//            return new OSSDataOutputStream(outputStream);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        // metaData.
//
//        //  this.client.
//    }
//
//    @Override
//    public FSDataInputStream open(IPath path, int bufferSize) {
//        OSSObject oObj = this.client.getObject(new GetObjectRequest(bucketName, path.getName()));
//        return new OSSDataInputStream(new BufferedInputStream(oObj.getObjectContent(), bufferSize));
//    }
//
//    private static class OSSDataInputStream extends FSDataInputStream {
//        public OSSDataInputStream(InputStream in) {
//            super(in);
//        }
//
//        @Override
//        public void readFully(long position, byte[] buffer, int offset, int length) throws IOException {
//            throw new UnsupportedOperationException();
//        }
//
//        @Override
//        public void seek(long position) {
//            throw new UnsupportedOperationException();
//        }
//    }
//
//    @Override
//    public FSDataInputStream open(IPath path) {
//        OSSObject oObj = this.client.getObject(new GetObjectRequest(bucketName, path.getName()));
//        return new OSSDataInputStream((oObj.getObjectContent()));
//    }
//
//    @Override
//    public TISFSDataOutputStream create(IPath f, boolean overwrite, int bufferSize) throws IOException {
//
//        PipedOutputStream outputStream = new PipedOutputStream();
//        PipedInputStream inputStream = new PipedInputStream(outputStream);
//        ossPutExecutor.execute(() -> {
//            this.client.putObject(this.bucketName, f.getName(), inputStream);
//        });
//        return new OSSDataOutputStream(new BufferedOutputStream(outputStream, bufferSize));
//    }
//
//
//    @Override
//    public TISFSDataOutputStream create(IPath f, boolean overwrite) throws IOException {
//        return create(f, overwrite, 2048);
//    }
//
//    @Override
//    public boolean exists(IPath path) {
//        return false;
//    }
//
//    @Override
//    public boolean mkdirs(IPath f) throws IOException {
//        return false;
//    }
//
//    @Override
//    public void copyToLocalFile(IPath srcPath, File dstPath) {
//    }
//
//    @Override
//    public void rename(IPath from, IPath to) {
//    }
//
//    @Override
//    public boolean copyFromLocalFile(File localIncrPath, IPath remoteIncrPath) {
//        return false;
//    }
//
//    @Override
//    public IFileSplitor getSplitor(IPath path) throws Exception {
//        return null;
//    }
//
//    @Override
//    public IContentSummary getContentSummary(IPath path) {
//        ObjectMetadata meta = this.client.getObjectMetadata(this.bucketName, path.getName());
//        return () -> {
//            return meta.getContentLength();
//        };
//    }
//
//    @Override
//    public List<IPathInfo> listChildren(IPath path) {
//        ObjectListing objectListing = this.client.listObjects(this.bucketName, path.getName());
//        return objectListing.getObjectSummaries().stream().map((summary) -> {
//            return new OSSPathInfo(path, summary);
//        }).collect(Collectors.toList());
//    }
//
//
//    private static class OSSPathInfo implements IPathInfo {
//        private final OSSObjectSummary meta;
//        private final IPath path;
//
//        public OSSPathInfo(IPath path, OSSObjectSummary meta) {
//            this.meta = meta;
//            this.path = path;
//        }
//
//        @Override
//        public String getName() {
//            return meta.getKey();
//        }
//
//        @Override
//        public IPath getPath() {
//            return this.path;
//        }
//
//        @Override
//        public boolean isDir() {
//            return false;
//        }
//
//        @Override
//        public long getModificationTime() {
//            return meta.getLastModified().getTime();
//        }
//
//        @Override
//        public long getLength() {
//            return meta.getSize();
//        }
//    }
//
//    @Override
//    public List<IPathInfo> listChildren(IPath path, IPathFilter filter) {
//        return null;
//    }
//
//    @Override
//    public IPathInfo getFileInfo(IPath path) {
//        return null;
//    }
//
//    @Override
//    public boolean delete(IPath f, boolean recursive) throws IOException {
//        return false;
//    }
//
//    @Override
//    public boolean delete(IPath f) throws IOException {
//        return false;
//    }
//
//    @Override
//    public void close() {
//    }
//
//    private static class OSSDataOutputStream extends TISFSDataOutputStream {
//        public OSSDataOutputStream(OutputStream out) {
//            super(out);
//        }
//
//        @Override
//        public void write(int b) throws IOException {
//            this.out.write(b);
//        }
//
//        @Override
//        public long getPos() throws IOException {
//            throw new UnsupportedOperationException();
//        }
//    }
//}
