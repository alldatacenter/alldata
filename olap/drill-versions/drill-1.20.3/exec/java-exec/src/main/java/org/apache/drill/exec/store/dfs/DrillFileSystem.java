/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.dfs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.apache.drill.exec.ops.OperatorStats;
import org.apache.drill.exec.util.AssertionUtil;
import org.apache.hadoop.classification.InterfaceAudience.LimitedPrivate;
import org.apache.hadoop.classification.InterfaceAudience.Private;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.BlockLocation;
import org.apache.hadoop.fs.ContentSummary;
import org.apache.hadoop.fs.CreateFlag;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileChecksum;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FsServerDefaults;
import org.apache.hadoop.fs.FsStatus;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Options.ChecksumOpt;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.fs.XAttrSetFlag;
import org.apache.hadoop.fs.permission.AclEntry;
import org.apache.hadoop.fs.permission.AclStatus;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.apache.hadoop.security.AccessControlException;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.util.Progressable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;

/**
 * DrillFileSystem is the wrapper around the actual FileSystem implementation. The {@link DrillFileSystem} is
 * immutable.
 *
 * If {@link org.apache.drill.exec.ops.OperatorStats} are provided it returns an instrumented FSDataInputStream to
 * measure IO wait time and tracking file open/close operations.
 */
public class DrillFileSystem extends FileSystem implements OpenFileTracker {
  private static final Logger logger = LoggerFactory.getLogger(DrillFileSystem.class);
  private static final boolean TRACKING_ENABLED = AssertionUtil.isAssertionsEnabled();

  public static final String UNDERSCORE_PREFIX = "_";
  public static final String DOT_PREFIX = ".";

  private final ConcurrentMap<DrillFSDataInputStream, DebugStackTrace> openedFiles = Maps.newConcurrentMap();

  private final FileSystem underlyingFs;
  private final OperatorStats operatorStats;
  private final CompressionCodecFactory codecFactory;

  public DrillFileSystem(Configuration fsConf) throws IOException {
    this(fsConf, null);
  }

  public DrillFileSystem(Configuration fsConf, OperatorStats operatorStats) throws IOException {
    // Configuration objects are mutable, and the underlying FileSystem object may directly use a passed in Configuration.
    // In order to avoid scenarios where a Configuration can change after a DrillFileSystem is created, we make a copy
    // of the Configuration.
    fsConf = new Configuration(fsConf);
    this.underlyingFs = FileSystem.get(fsConf);
    this.codecFactory = new CompressionCodecFactory(fsConf);
    this.operatorStats = operatorStats;
  }

  private void throwUnsupported() {
    throw new UnsupportedOperationException(DrillFileSystem.class.getCanonicalName() + " is immutable and should not be changed after creation.");
  }

  /**
   * This method should never be used on {@link DrillFileSystem} since {@link DrillFileSystem} is immutable.
   * {@inheritDoc}
   * @throws UnsupportedOperationException when called.
   */
  @Override
  public void setConf(Configuration conf) {
    if (underlyingFs != null) {
      throwUnsupported();
    }

    // The parent class's constructor FileSystem() calls Configured(null) which calls setConf(null).
    // We want to let that first call to setConf succeed. Any subsequent calls would be made by a user and are not allowed.
  }

  /**
   * Returns a copy of {@link Configuration} for this {@link DrillFileSystem}.
   * <b>Note: </b> a copy of the {@link Configuration} is returned in order to enforce immutability.
   * @return A copy of {@link Configuration} for this {@link DrillFileSystem}.
   */
  @Override
  public Configuration getConf() {
    return new Configuration(this.underlyingFs.getConf());
  }

  /**
   * If OperatorStats are provided return a instrumented {@link org.apache.hadoop.fs.FSDataInputStream}.
   */
  @Override
  public FSDataInputStream open(Path f, int bufferSize) throws IOException {
    if (operatorStats == null) {
      return underlyingFs.open(f, bufferSize);
    }

    if (TRACKING_ENABLED) {
      DrillFSDataInputStream is = new DrillFSDataInputStream(underlyingFs.open(f, bufferSize), operatorStats, this);
      fileOpened(f, is);
      return is;
    }

    return new DrillFSDataInputStream(underlyingFs.open(f, bufferSize), operatorStats);
  }

  /**
   * If OperatorStats are provided return a instrumented {@link org.apache.hadoop.fs.FSDataInputStream}.
   */
  @Override
  public FSDataInputStream open(Path f) throws IOException {
    if (operatorStats == null) {
      return underlyingFs.open(f);
    }

    if (TRACKING_ENABLED) {
      DrillFSDataInputStream is = new DrillFSDataInputStream(underlyingFs.open(f), operatorStats, this);
      fileOpened(f, is);
      return is;
    }

    return new DrillFSDataInputStream(underlyingFs.open(f), operatorStats);
  }

  /**
   * This method should never be used on {@link DrillFileSystem} since {@link DrillFileSystem} is immutable.
   * {@inheritDoc}
   * @throws UnsupportedOperationException when called.
   */
  @Override
  public void initialize(URI name, Configuration conf) {
    throwUnsupported();
  }

  @Override
  public String getScheme() {
    return underlyingFs.getScheme();
  }

  @Override
  public FSDataOutputStream create(Path f) throws IOException {
    return underlyingFs.create(f);
  }

  @Override
  public FSDataOutputStream create(Path f, boolean overwrite) throws IOException {
    return underlyingFs.create(f, overwrite);
  }

  @Override
  public FSDataOutputStream create(Path f, Progressable progress) throws IOException {
    return underlyingFs.create(f, progress);
  }

  @Override
  public FSDataOutputStream create(Path f, short replication) throws IOException {
    return underlyingFs.create(f, replication);
  }

  @Override
  public FSDataOutputStream create(Path f, short replication, Progressable progress) throws IOException {
    return underlyingFs.create(f, replication, progress);
  }

  @Override
  public FSDataOutputStream create(Path f, boolean overwrite, int bufferSize) throws IOException {
    return underlyingFs.create(f, overwrite, bufferSize);
  }

  @Override
  public FSDataOutputStream create(Path f, boolean overwrite, int bufferSize, Progressable progress) throws IOException {
    return underlyingFs.create(f, overwrite, bufferSize, progress);
  }

  @Override
  public FSDataOutputStream create(Path f, boolean overwrite, int bufferSize, short replication,
      long blockSize) throws IOException {
    return underlyingFs.create(f, overwrite, bufferSize, replication, blockSize);
  }

  @Override
  public FSDataOutputStream create(Path f, boolean overwrite, int bufferSize, short replication, long blockSize, Progressable progress) throws IOException {
    return underlyingFs.create(f, overwrite, bufferSize, replication, blockSize, progress);
  }

  @Override
  public FileStatus getFileStatus(Path f) throws IOException {
    return underlyingFs.getFileStatus(f);
  }

  @Override
  public void createSymlink(Path target, Path link, boolean createParent) throws IOException {
    underlyingFs.createSymlink(target, link, createParent);
  }

  @Override
  public FileStatus getFileLinkStatus(Path f) throws IOException {
    return underlyingFs.getFileLinkStatus(f);
  }

  @Override
  public boolean supportsSymlinks() {
    return underlyingFs.supportsSymlinks();
  }

  @Override
  public Path getLinkTarget(Path f) throws IOException {
    return underlyingFs.getLinkTarget(f);
  }

  @Override
  public FileChecksum getFileChecksum(Path f) throws IOException {
    return underlyingFs.getFileChecksum(f);
  }

  /**
   * This method should never be used on {@link DrillFileSystem} since {@link DrillFileSystem} is immutable.
   * {@inheritDoc}
   * @throws UnsupportedOperationException when called.
   */
  @Override
  public void setVerifyChecksum(boolean verifyChecksum) {
    throwUnsupported();
  }

  /**
   * This method should never be used on {@link DrillFileSystem} since {@link DrillFileSystem} is immutable.
   * {@inheritDoc}
   * @throws UnsupportedOperationException when called.
   */
  @Override
  public void setWriteChecksum(boolean writeChecksum) {
    throwUnsupported();
  }

  @Override
  public FsStatus getStatus() throws IOException {
    return underlyingFs.getStatus();
  }

  @Override
  public FsStatus getStatus(Path p) throws IOException {
    return underlyingFs.getStatus(p);
  }

  @Override
  public void setPermission(Path p, FsPermission permission) throws IOException {
    underlyingFs.setPermission(p, permission);
  }

  @Override
  public void setOwner(Path p, String username, String groupname) throws IOException {
    underlyingFs.setOwner(p, username, groupname);
  }

  @Override
  public void setTimes(Path p, long mtime, long atime) throws IOException {
    underlyingFs.setTimes(p, mtime, atime);
  }

  @Override
  public Path createSnapshot(Path path, String snapshotName) throws IOException {
    return underlyingFs.createSnapshot(path, snapshotName);
  }

  @Override
  public void renameSnapshot(Path path, String snapshotOldName, String snapshotNewName) throws IOException {
    underlyingFs.renameSnapshot(path, snapshotOldName, snapshotNewName);
  }

  @Override
  public void deleteSnapshot(Path path, String snapshotName) throws IOException {
    underlyingFs.deleteSnapshot(path, snapshotName);
  }

  @Override
  public void modifyAclEntries(Path path, List<AclEntry> aclSpec) throws IOException {
    underlyingFs.modifyAclEntries(path, aclSpec);
  }

  @Override
  public void removeAclEntries(Path path, List<AclEntry> aclSpec) throws IOException {
    underlyingFs.removeAclEntries(path, aclSpec);
  }

  @Override
  public void removeDefaultAcl(Path path) throws IOException {
    underlyingFs.removeDefaultAcl(path);
  }

  @Override
  public void removeAcl(Path path) throws IOException {
    underlyingFs.removeAcl(path);
  }

  @Override
  public void setAcl(Path path, List<AclEntry> aclSpec) throws IOException {
    underlyingFs.setAcl(path, aclSpec);
  }

  @Override
  public AclStatus getAclStatus(Path path) throws IOException {
    return underlyingFs.getAclStatus(path);
  }

  @Override
  public Path getWorkingDirectory() {
    return underlyingFs.getWorkingDirectory();
  }

  @Override
  public FSDataOutputStream append(Path f, int bufferSize, Progressable progress) throws IOException {
    return underlyingFs.append(f, bufferSize, progress);
  }

  @Override
  public void concat(Path trg, Path[] psrcs) throws IOException {
    underlyingFs.concat(trg, psrcs);
  }

  @Override
  @Deprecated
  public short getReplication(Path src) throws IOException {
    return underlyingFs.getReplication(src);
  }

  @Override
  public boolean setReplication(Path src, short replication) throws IOException {
    return underlyingFs.setReplication(src, replication);
  }

  @Override
  public boolean mkdirs(Path f, FsPermission permission) throws IOException {
    return underlyingFs.mkdirs(f, permission);
  }

  @Override
  public void copyFromLocalFile(Path src, Path dst) throws IOException {
    underlyingFs.copyFromLocalFile(src, dst);
  }

  @Override
  public void moveFromLocalFile(Path[] srcs, Path dst) throws IOException {
    underlyingFs.moveFromLocalFile(srcs, dst);
  }

  @Override
  public void moveFromLocalFile(Path src, Path dst) throws IOException {
    underlyingFs.moveFromLocalFile(src, dst);
  }

  @Override
  public void copyFromLocalFile(boolean delSrc, Path src, Path dst) throws IOException {
    underlyingFs.copyFromLocalFile(delSrc, src, dst);
  }

  @Override
  public void copyFromLocalFile(boolean delSrc, boolean overwrite, Path[] srcs, Path dst) throws IOException {
    underlyingFs.copyFromLocalFile(delSrc, overwrite, srcs, dst);
  }

  @Override
  public void copyFromLocalFile(boolean delSrc, boolean overwrite, Path src, Path dst) throws IOException {
    underlyingFs.copyFromLocalFile(delSrc, overwrite, src, dst);
  }

  @Override
  public void copyToLocalFile(Path src, Path dst) throws IOException {
    underlyingFs.copyToLocalFile(src, dst);
  }

  @Override
  public void moveToLocalFile(Path src, Path dst) throws IOException {
    underlyingFs.moveToLocalFile(src, dst);
  }

  @Override
  public void copyToLocalFile(boolean delSrc, Path src, Path dst) throws IOException {
    underlyingFs.copyToLocalFile(delSrc, src, dst);
  }

  @Override
  public void copyToLocalFile(boolean delSrc, Path src, Path dst, boolean useRawLocalFileSystem) throws IOException {
    underlyingFs.copyToLocalFile(delSrc, src, dst, useRawLocalFileSystem);
  }

  @Override
  public Path startLocalOutput(Path fsOutputFile, Path tmpLocalFile) throws IOException {
    return underlyingFs.startLocalOutput(fsOutputFile, tmpLocalFile);
  }

  @Override
  public void completeLocalOutput(Path fsOutputFile, Path tmpLocalFile) throws IOException {
    underlyingFs.completeLocalOutput(fsOutputFile, tmpLocalFile);
  }

  @Override
  public void close() throws IOException {
    if (TRACKING_ENABLED) {
      if (openedFiles.size() != 0) {
        final StringBuffer errMsgBuilder = new StringBuffer();

        errMsgBuilder.append(String.format("Not all files opened using this FileSystem are closed. " + "There are" +
            " still [%d] files open.\n", openedFiles.size()));

        for (DebugStackTrace stackTrace : openedFiles.values()) {
          stackTrace.addToStringBuilder(errMsgBuilder);
        }

        final String errMsg = errMsgBuilder.toString();
        logger.error(errMsg);
        throw new IllegalStateException(errMsg);
      }
    }
  }

  @Override
  public long getUsed() throws IOException {
    return underlyingFs.getUsed();
  }

  @Override
  @Deprecated
  public long getBlockSize(Path f) throws IOException {
    return underlyingFs.getBlockSize(f);
  }

  @Override
  @Deprecated
  public long getDefaultBlockSize() {
    return underlyingFs.getDefaultBlockSize();
  }

  @Override
  public long getDefaultBlockSize(Path f) {
    return underlyingFs.getDefaultBlockSize(f);
  }

  @Override
  @Deprecated
  public short getDefaultReplication() {
    return underlyingFs.getDefaultReplication();
  }

  @Override
  public short getDefaultReplication(Path path) {
    return underlyingFs.getDefaultReplication(path);
  }

  @Override
  public boolean mkdirs(Path folderPath) throws IOException {
    if (!underlyingFs.exists(folderPath)) {
      return underlyingFs.mkdirs(folderPath);
    } else if (!underlyingFs.getFileStatus(folderPath).isDir()) {
      throw new IOException("The specified folder path exists and is not a folder.");
    }
    return false;
  }

  @Override
  public FSDataOutputStream create(Path f, FsPermission permission, EnumSet<CreateFlag> flags, int bufferSize,
      short replication, long blockSize, Progressable progress, ChecksumOpt checksumOpt) throws IOException {
    return underlyingFs.create(f, permission, flags, bufferSize, replication, blockSize, progress, checksumOpt);
  }

  @Override
  @Deprecated
  public FSDataOutputStream createNonRecursive(Path f, boolean overwrite, int bufferSize, short replication,
      long blockSize, Progressable progress) throws IOException {
    return underlyingFs.createNonRecursive(f, overwrite, bufferSize, replication, blockSize, progress);
  }

  @Override
  @Deprecated
  public FSDataOutputStream createNonRecursive(Path f, FsPermission permission, boolean overwrite, int bufferSize,
      short replication, long blockSize, Progressable progress) throws IOException {
    return underlyingFs.createNonRecursive(f, permission, overwrite, bufferSize, replication, blockSize, progress);
  }

  @Override
  @Deprecated
  public FSDataOutputStream createNonRecursive(Path f, FsPermission permission, EnumSet<CreateFlag> flags, int bufferSize, short replication, long blockSize, Progressable progress) throws IOException {
    return underlyingFs.createNonRecursive(f, permission, flags, bufferSize, replication, blockSize, progress);
  }

  @Override
  public boolean createNewFile(Path f) throws IOException {
    return underlyingFs.createNewFile(f);
  }

  @Override
  public FSDataOutputStream append(Path f) throws IOException {
    return underlyingFs.append(f);
  }

  @Override
  public FSDataOutputStream append(Path f, int bufferSize) throws IOException {
    return underlyingFs.append(f, bufferSize);
  }

  @Override
  public FSDataOutputStream create(Path f, FsPermission permission, boolean overwrite, int bufferSize, short
      replication, long blockSize, Progressable progress) throws IOException {
    return underlyingFs.create(f, permission, overwrite, bufferSize, replication, blockSize, progress);
  }

  @Override
  public FSDataOutputStream create(Path f, FsPermission permission, EnumSet<CreateFlag> flags, int bufferSize,
      short replication, long blockSize, Progressable progress) throws IOException {
    return underlyingFs.create(f, permission, flags, bufferSize, replication, blockSize, progress);
  }

  @Override
  public FileStatus[] listStatus(Path f) throws FileNotFoundException, IOException {
    return underlyingFs.listStatus(f);
  }

  @Override
  public RemoteIterator<Path> listCorruptFileBlocks(Path path) throws IOException {
    return underlyingFs.listCorruptFileBlocks(path);
  }

  @Override
  public FileStatus[] listStatus(Path f, PathFilter filter) throws FileNotFoundException, IOException {
    return underlyingFs.listStatus(f, filter);
  }

  @Override
  public FileStatus[] listStatus(Path[] files) throws FileNotFoundException, IOException {
    return underlyingFs.listStatus(files);
  }

  @Override
  public FileStatus[] listStatus(Path[] files, PathFilter filter) throws FileNotFoundException, IOException {
    return underlyingFs.listStatus(files, filter);
  }

  @Override
  public FileStatus[] globStatus(Path pathPattern) throws IOException {
    return underlyingFs.globStatus(pathPattern);
  }

  @Override
  public FileStatus[] globStatus(Path pathPattern, PathFilter filter) throws IOException {
    return underlyingFs.globStatus(pathPattern, filter);
  }

  @Override
  public RemoteIterator<LocatedFileStatus> listLocatedStatus(Path f) throws FileNotFoundException, IOException {
    return underlyingFs.listLocatedStatus(f);
  }

  @Override
  public RemoteIterator<LocatedFileStatus> listFiles(Path f, boolean recursive) throws FileNotFoundException, IOException {
    return underlyingFs.listFiles(f, recursive);
  }

  @Override
  public Path getHomeDirectory() {
    return underlyingFs.getHomeDirectory();
  }

  /**
   * This method should never be used on {@link DrillFileSystem} since {@link DrillFileSystem} is immutable.
   * {@inheritDoc}
   * @throws UnsupportedOperationException when called.
   */
  @Override
  public void setWorkingDirectory(Path new_dir) {
    throwUnsupported();
  }

  @Override
  public boolean rename(Path src, Path dst) throws IOException {
    return underlyingFs.rename(src, dst);
  }

  @Override
  @Deprecated
  public boolean delete(Path f) throws IOException {
    return underlyingFs.delete(f);
  }

  @Override
  public boolean delete(Path f, boolean recursive) throws IOException {
    return underlyingFs.delete(f, recursive);
  }

  @Override
  public boolean deleteOnExit(Path f) throws IOException {
    return underlyingFs.deleteOnExit(f);
  }

  @Override
  public boolean cancelDeleteOnExit(Path f) {
    return underlyingFs.cancelDeleteOnExit(f);
  }

  @Override
  public boolean exists(Path f) throws IOException {
    return underlyingFs.exists(f);
  }

  @Override
  public boolean isDirectory(Path f) throws IOException {
    return underlyingFs.isDirectory(f);
  }

  @Override
  public boolean isFile(Path f) throws IOException {
    return underlyingFs.isFile(f);
  }

  @Override
  @Deprecated
  public long getLength(Path f) throws IOException {
    return underlyingFs.getLength(f);
  }

  @Override
  public ContentSummary getContentSummary(Path f) throws IOException {
    return underlyingFs.getContentSummary(f);
  }

  @Override
  public URI getUri() {
    return underlyingFs.getUri();
  }

  @Override
  @LimitedPrivate({"HDFS", "MapReduce"})
  public String getCanonicalServiceName() {
    return underlyingFs.getCanonicalServiceName();
  }

  @Override
  @Deprecated
  public String getName() {
    return underlyingFs.getName();
  }

  @Override
  public Path makeQualified(Path path) {
    return underlyingFs.makeQualified(path);
  }

  @Override
  @Private
  public Token<?> getDelegationToken(String renewer) throws IOException {
    return underlyingFs.getDelegationToken(renewer);
  }

  @Override
  @LimitedPrivate({"HDFS", "MapReduce"})
  public Token<?>[] addDelegationTokens(String renewer, Credentials credentials) throws IOException {
    return underlyingFs.addDelegationTokens(renewer, credentials);
  }

  @Override
  @LimitedPrivate({"HDFS"})
  @VisibleForTesting
  public FileSystem[] getChildFileSystems() {
    return underlyingFs.getChildFileSystems();
  }

  @Override
  public BlockLocation[] getFileBlockLocations(FileStatus file, long start, long len) throws IOException {
    return underlyingFs.getFileBlockLocations(file, start, len);
  }

  @Override
  public BlockLocation[] getFileBlockLocations(Path p, long start, long len) throws IOException {
    return underlyingFs.getFileBlockLocations(p, start, len);
  }

  @Override
  @Deprecated
  public FsServerDefaults getServerDefaults() throws IOException {
    return underlyingFs.getServerDefaults();
  }

  @Override
  public FsServerDefaults getServerDefaults(Path p) throws IOException {
    return underlyingFs.getServerDefaults(p);
  }

  @Override
  public Path resolvePath(Path p) throws IOException {
    return underlyingFs.resolvePath(p);
  }

  @Override
  public boolean truncate(final Path f, final long newLength) throws IOException {
    return underlyingFs.truncate(f, newLength);
  }

  @Override
  public RemoteIterator<FileStatus> listStatusIterator(final Path p) throws FileNotFoundException, IOException {
    return underlyingFs.listStatusIterator(p);
  }

  @Override
  public void access(final Path path, final FsAction mode) throws AccessControlException, FileNotFoundException, IOException {
    underlyingFs.access(path, mode);
  }

  @Override
  public FileChecksum getFileChecksum(final Path f, final long length) throws IOException {
    return underlyingFs.getFileChecksum(f, length);
  }

  @Override
  public void setXAttr(final Path path, final String name, final byte[] value) throws IOException {
    underlyingFs.setXAttr(path, name, value);
  }

  @Override
  public void setXAttr(final Path path, final String name, final byte[] value, final EnumSet<XAttrSetFlag> flag) throws IOException {
    underlyingFs.setXAttr(path, name, value, flag);
  }

  @Override
  public byte[] getXAttr(final Path path, final String name) throws IOException {
    return underlyingFs.getXAttr(path, name);
  }

  @Override
  public Map<String, byte[]> getXAttrs(final Path path) throws IOException {
    return underlyingFs.getXAttrs(path);
  }

  @Override
  public Map<String, byte[]> getXAttrs(final Path path, final List<String> names) throws IOException {
    return underlyingFs.getXAttrs(path, names);
  }

  @Override
  public List<String> listXAttrs(final Path path) throws IOException {
    return underlyingFs.listXAttrs(path);
  }

  @Override
  public void removeXAttr(final Path path, final String name) throws IOException {
    underlyingFs.removeXAttr(path, name);
  }

  /**
   * Returns an InputStream from a Hadoop path. If the data is compressed, this method will return a compressed
   * InputStream depending on the codec.
   * @param path Input file path
   * @return InputStream of opened file path
   * @throws IOException If the file is unreachable, unavailable or otherwise unreadable
   */
  public InputStream openPossiblyCompressedStream(Path path) throws IOException {
    CompressionCodec codec = getCodec(path); // infers from file ext.
    InputStream inputStream = open(path);
    if (codec != null) {
      inputStream = codec.createInputStream(inputStream);
    }
    return inputStream;
  }

  /**
   * Returns the {@link org.apache.hadoop.io.compress.CompressionCodec} for a given file.  This
   * can be used to determine the type of compression (if any) which was used.  Returns null if the
   * file is not compressed.
   * @param path The file of unknown compression
   * @return CompressionCodec used by the file. Null if the file is not compressed.
   */
  public CompressionCodec getCodec(Path path) {
    return codecFactory.getCodec(path);
  }

  @Override
  public void fileOpened(Path path, DrillFSDataInputStream fsDataInputStream) {
    openedFiles.put(fsDataInputStream, new DebugStackTrace(path, Thread.currentThread().getStackTrace()));
  }

  @Override
  public void fileClosed(DrillFSDataInputStream fsDataInputStream) {
    openedFiles.remove(fsDataInputStream);
  }

  public static class DebugStackTrace {
    final private StackTraceElement[] elements;
    final private Path path;

    public DebugStackTrace(Path path, StackTraceElement[] elements) {
      this.path = path;
      this.elements = elements;
    }

    public void addToStringBuilder(StringBuffer sb) {
      sb.append("File '");
      sb.append(path.toString());
      sb.append("' opened at callstack:\n");

      // add all stack elements except the top three as they point to DrillFileSystem.open() and inner stack elements.
      for (int i = 3; i < elements.length; i++) {
        sb.append("\t");
        sb.append(elements[i]);
        sb.append("\n");
      }
      sb.append("\n");
    }
  }
}
