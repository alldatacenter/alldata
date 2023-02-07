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
package org.apache.drill.exec.physical.impl.spill;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.cache.VectorSerializer;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.config.HashAggregate;
import org.apache.drill.exec.physical.config.HashJoinPOP;
import org.apache.drill.exec.physical.config.Sort;
import org.apache.drill.exec.proto.ExecProtos.FragmentHandle;
import org.apache.drill.exec.proto.helper.QueryIdHelper;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import org.apache.drill.shaded.guava.com.google.common.base.Joiner;
import org.apache.drill.shaded.guava.com.google.common.collect.Iterators;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;

/**
 * Generates the set of spill files for this sort session.
 */

public class SpillSet {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SpillSet.class);

  /**
   * Spilling on the Mac using the HDFS file system is very inefficient,
   * affects performance numbers. This interface allows using HDFS in
   * production, but to bypass the HDFS file system when needed.
   */

  private interface FileManager {

    void deleteOnExit(String fragmentSpillDir) throws IOException;

    WritableByteChannel createForWrite(String fileName) throws IOException;

    InputStream openForInput(String fileName) throws IOException;

    void deleteFile(String fileName) throws IOException;

    void deleteDir(String fragmentSpillDir) throws IOException;

    /**
     * Given a manager-specific output stream, return the current write position.
     * Used to report total write bytes.
     *
     * @param channel created by the file manager
     * @return
     */
    long getWriteBytes(WritableByteChannel channel);

    /**
     * Given a manager-specific input stream, return the current read position.
     * Used to report total read bytes.
     *
     * @param inputStream input stream created by the file manager
     * @return
     */
    long getReadBytes(InputStream inputStream);
  }

  /**
   * Normal implementation of spill files using the HDFS file system.
   */

  private static class HadoopFileManager implements FileManager{
    /**
     * The HDFS file system (for local directories, HDFS storage, etc.) used to
     * create the temporary spill files. Allows spill files to be either on local
     * disk, or in a DFS. (The admin can choose to put spill files in DFS when
     * nodes provide insufficient local disk space)
     */

    // The buffer size is calculated as LCM of the Hadoop internal checksum buffer (9 * checksum length), where
    // checksum length is 512 by default, and MapRFS page size that equals to 8 * 1024. The length of the transfer
    // buffer does not affect performance of the write to hdfs or maprfs significantly once buffer length is more
    // than 32 bytes.
    private static final int TRANSFER_SIZE = 9 * 8 * 1024;

    private final byte buffer[];
    private FileSystem fs;

    protected HadoopFileManager(String fsName) {
      buffer = new byte[TRANSFER_SIZE];
      Configuration conf = new Configuration();
      conf.set(FileSystem.FS_DEFAULT_NAME_KEY, fsName);
      try {
        fs = FileSystem.get(conf);
      } catch (IOException e) {
        throw UserException.resourceError(e)
              .message("Failed to get the File System for external sort")
              .build(logger);
      }
    }

    @Override
    public void deleteOnExit(String fragmentSpillDir) throws IOException {
      fs.deleteOnExit(new Path(fragmentSpillDir));
    }

    @Override
    public WritableByteChannel createForWrite(String fileName) throws IOException {
      return new WritableByteChannelImpl(buffer, fs.create(new Path(fileName)));
    }

    @Override
    public InputStream openForInput(String fileName) throws IOException {
      return fs.open(new Path(fileName));
    }

    @Override
    public void deleteFile(String fileName) throws IOException {
      Path path = new Path(fileName);
      if (fs.exists(path)) {
        fs.delete(path, false);
      }
    }

    @Override
    public void deleteDir(String fragmentSpillDir) throws IOException {
      Path path = new Path(fragmentSpillDir);
      if (path != null && fs.exists(path)) {
        if (fs.delete(path, true)) {
            fs.cancelDeleteOnExit(path);
        }
      }
    }

    @Override
    public long getWriteBytes(WritableByteChannel channel) {
      try {
        return ((FSDataOutputStream)((WritableByteChannelImpl)channel).out).getPos();
      } catch (Exception e) {
        // Just used for logging, not worth dealing with the exception.
        return 0;
      }
    }

    @Override
    public long getReadBytes(InputStream inputStream) {
      try {
        return ((FSDataInputStream) inputStream).getPos();
      } catch (IOException e) {
        // Just used for logging, not worth dealing with the exception.
        return 0;
      }
    }
  }

  /**
   * Wrapper around an input stream to collect the total bytes
   * read through the stream for use in reporting performance
   * metrics.
   */

  public static class CountingInputStream extends InputStream
  {
    private InputStream in;
    private long count;

    public CountingInputStream(InputStream in) {
      this.in = in;
    }

    @Override
    public int read() throws IOException {
      int b = in.read();
      if (b != -1) {
        count++;
      }
      return b;
    }

    @Override
    public int read(byte b[]) throws IOException {
      int n = in.read(b);
      if (n != -1) {
        count += n;
      }
      return n;
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
      int n = in.read(b, off, len);
      if (n != -1) {
        count += n;
      }
      return n;
    }

    @Override
    public long skip(long n) throws IOException {
      return in.skip(n);
    }

    @Override
    public void close() throws IOException {
      in.close();
    }

    public long getCount() { return count; }
  }

  /**
   * Wrapper around an output stream to collect the total bytes
   * written through the stream for use in reporting performance
   * metrics.
   */

  public static class CountingOutputStream extends OutputStream {

    private OutputStream out;
    private long count;

    public CountingOutputStream(OutputStream out) {
      this.out = out;
    }

    @Override
    public void write(int b) throws IOException {
      count++;
      out.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
      count += b.length;
      out.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      count += len;
      out.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
      out.flush();
    }

    @Override
    public void close() throws IOException {
      out.close();
    }

    public long getCount() { return count; }
  }

  /**
   * Performance-oriented direct access to the local file system which
   * bypasses HDFS.
   */

  private static class LocalFileManager implements FileManager {

    private File baseDir;

    public LocalFileManager(String fsName) {
      baseDir = new File(fsName.replace(FileSystem.DEFAULT_FS, ""));
    }

    @Override
    public void deleteOnExit(String fragmentSpillDir) throws IOException {
      File dir = new File(baseDir, fragmentSpillDir);
      dir.mkdirs();
      dir.deleteOnExit();
    }

    @Override
    public WritableByteChannel createForWrite(String fileName) throws IOException {
      return FileChannel.open(new File(baseDir, fileName).toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }

     @Override
    public InputStream openForInput(String fileName) throws IOException {
      return new CountingInputStream(
                new BufferedInputStream(
                    new FileInputStream(new File(baseDir, fileName))));
    }

    @Override
    public void deleteFile(String fileName) throws IOException {
      new File(baseDir, fileName).delete();
    }

    @Override
    public void deleteDir(String fragmentSpillDir) throws IOException {
      File spillDir = new File(baseDir, fragmentSpillDir);
      for (File spillFile : spillDir.listFiles()) {
        spillFile.delete(); // IO exception if file delete fails
      }
      spillDir.delete();// IO exception if dir delete fails
    }

    @Override
    public long getWriteBytes(WritableByteChannel channel)
    {
      try {
        return ((FileChannel)channel).position();
      } catch (Exception e) {
        return 0;
      }
    }

    @Override
    public long getReadBytes(InputStream inputStream) {
      return ((CountingInputStream) inputStream).getCount();
    }
  }

  private static class WritableByteChannelImpl implements WritableByteChannel
  {
    private final byte buffer[];
    private OutputStream out;

    WritableByteChannelImpl(byte[] buffer, OutputStream out) {
      this.buffer = buffer;
      this.out = out;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
      int remaining = src.remaining();
      int totalWritten = 0;
      synchronized (buffer) {
        for (int posn = 0; posn < remaining; posn += buffer.length) {
          int len = Math.min(buffer.length, remaining - posn);
          src.get(buffer, 0, len);
          out.write(buffer, 0, len);
          totalWritten += len;
        }
      }
      return totalWritten;
    }

    @Override
    public boolean isOpen()
    {
      return out != null;
    }

    @Override
    public void close() throws IOException {
      out.close();
      out = null;
    }
  }

  private final Iterator<String> dirs;

  /**
   * Set of directories to which this operator should write spill files in a round-robin
   * fashion. The operator requires at least one spill directory, but can
   * support any number. The admin must ensure that sufficient space exists
   * on all directories as this operator does not check space availability
   * before writing to the directories.
   */

  private Set<String> currSpillDirs = Sets.newTreeSet();

  /**
   * The base part of the file name for spill files. Each file has this
   * name plus an appended spill serial number.
   */

  private final String spillDirName;

  private int fileCount = 0;

  private FileManager fileManager;

  private long readBytes;

  private long writeBytes;

  public SpillSet(FragmentContext context, PhysicalOperator popConfig) {
    this(context.getConfig(), context.getHandle(), popConfig);
  }

  public SpillSet(DrillConfig config, FragmentHandle handle, PhysicalOperator popConfig) {
    String operName;

    // Set the spill options from the configuration
    String spillFs;
    List<String> dirList;

    // Set the operator name (used as part of the spill file name),
    // and set oper. specific options (the config file defaults to using the
    // common options; users may override those - per operator)
    if (popConfig instanceof Sort) {
        operName = "Sort";
        spillFs = config.getString(ExecConstants.EXTERNAL_SORT_SPILL_FILESYSTEM);
        dirList = config.getStringList(ExecConstants.EXTERNAL_SORT_SPILL_DIRS);
    } else if (popConfig instanceof HashAggregate) {
        operName = "HashAgg";
        spillFs = config.getString(ExecConstants.HASHAGG_SPILL_FILESYSTEM);
        dirList = config.getStringList(ExecConstants.HASHAGG_SPILL_DIRS);
    } else if (popConfig instanceof HashJoinPOP) {
      operName = "HashJoin";
      spillFs = config.getString(ExecConstants.HASHJOIN_SPILL_FILESYSTEM);
      dirList = config.getStringList(ExecConstants.HASHJOIN_SPILL_DIRS);
    } else {
        // just use the common ones
        operName = "Unknown";
        spillFs = config.getString(ExecConstants.SPILL_FILESYSTEM);
        dirList = config.getStringList(ExecConstants.SPILL_DIRS);
    }

    dirs = Iterators.cycle(dirList);

    // If more than one directory, semi-randomly choose an offset into
    // the list to avoid overloading the first directory in the list.

    if (dirList.size() > 1) {
      int hash = handle.getQueryId().hashCode() +
                 handle.getMajorFragmentId() +
                 handle.getMinorFragmentId() +
                 popConfig.getOperatorId();
      int offset = hash % dirList.size();
      for (int i = 0; i < offset; i++) {
        dirs.next();
      }
    }

    // Use the high-performance local file system if the local file
    // system is selected and impersonation is off. (We use that
    // as a proxy for a non-production Drill setup.)

    boolean impersonationEnabled = config.getBoolean(ExecConstants.IMPERSONATION_ENABLED);
    if (spillFs.startsWith(FileSystem.DEFAULT_FS) && ! impersonationEnabled) {
      fileManager = new LocalFileManager(spillFs);
    } else {
      fileManager = new HadoopFileManager(spillFs);
    }

    spillDirName = String.format("%s_%s_%s-%s-%s",
        QueryIdHelper.getQueryId(handle.getQueryId()),
        operName, handle.getMajorFragmentId(), popConfig.getOperatorId(), handle.getMinorFragmentId());
  }

  public String getNextSpillFile() {
    return getNextSpillFile(null);
  }

  public String getNextSpillFile(String extraName) {

    // Identify the next directory from the round-robin list to
    // the file created from this round of spilling. The directory must
    // must have sufficient space for the output file.

    String spillDir = dirs.next();
    String currSpillPath = Joiner.on("/").join(spillDir, spillDirName);
    currSpillDirs.add(currSpillPath);

    String outputFile = Joiner.on("/").join(currSpillPath, "spill" + ++fileCount);
    if (extraName != null) {
      outputFile += "_" + extraName;
    }

    try {
        fileManager.deleteOnExit(currSpillPath);
    } catch (IOException e) {
        // since this is meant to be used in a batches's spilling, we don't propagate the exception
        logger.warn("Unable to mark spill directory " + currSpillPath + " for deleting on exit", e);
    }
    return outputFile;
  }

  public boolean hasSpilled() {
    return fileCount > 0;
  }

  public int getFileCount() { return fileCount; }

  public InputStream openForInput(String fileName) throws IOException {
    return fileManager.openForInput(fileName);
  }

  public WritableByteChannel openForOutput(String fileName) throws IOException {
    return fileManager.createForWrite(fileName);
  }

  public void delete(String fileName) throws IOException {
    fileManager.deleteFile(fileName);
  }

  public long getWriteBytes() { return writeBytes; }
  public long getReadBytes() { return readBytes; }

  public void close() {
    for (String path : currSpillDirs) {
      try {
        fileManager.deleteDir(path);
      } catch (IOException e) {
          // since this is meant to be used in a batches's cleanup, we don't propagate the exception
          logger.warn("Unable to delete spill directory " + path,  e);
      }
      currSpillDirs.clear(); // in case close() is called again
    }
  }

  public long getPosition(InputStream inputStream) {
    return fileManager.getReadBytes(inputStream);
  }

  public long getPosition(WritableByteChannel channel) {
    return fileManager.getWriteBytes(channel);
  }

  public void tallyReadBytes(long readLength) {
    readBytes += readLength;
  }

  public void tallyWriteBytes(long writeLength) {
    writeBytes += writeLength;
  }

  public VectorSerializer.Writer writer(String fileName) throws IOException {
    return VectorSerializer.writer(openForOutput(fileName));
  }

  public void close(VectorSerializer.Writer writer) throws IOException {
    tallyWriteBytes(writer.getBytesWritten());
    writer.close();
  }
}
