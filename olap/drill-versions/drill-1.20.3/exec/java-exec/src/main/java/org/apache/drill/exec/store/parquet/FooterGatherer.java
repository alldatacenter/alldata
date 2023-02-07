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
package org.apache.drill.exec.store.parquet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.drill.exec.store.TimedCallable;
import org.apache.drill.exec.util.DrillFileSystemUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import org.apache.parquet.bytes.BytesUtils;
import org.apache.parquet.hadoop.Footer;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.ParquetFileWriter;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;
import static org.apache.parquet.format.converter.ParquetMetadataConverter.NO_FILTER;

public class FooterGatherer {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FooterGatherer.class);

  private static final int DEFAULT_READ_SIZE = 64*1024;
  private static final int FOOTER_LENGTH_SIZE = 4;
  private static final int FOOTER_METADATA_SIZE = FOOTER_LENGTH_SIZE + ParquetFileWriter.MAGIC.length;
  private static final int MAGIC_LENGTH = ParquetFileWriter.MAGIC.length;
  private static final int MIN_FILE_SIZE = ParquetFileWriter.MAGIC.length + FOOTER_METADATA_SIZE;

  private static final void readFully(FSDataInputStream stream, long start, byte[] output, int offset, int len) throws IOException{
    int bytesRead = 0;
    while(bytesRead > -1 && bytesRead < len){
      bytesRead += stream.read(start+bytesRead, output, offset + bytesRead, len-bytesRead);
    }
  }

  private static void checkMagicBytes(FileStatus status, byte[] data, int offset) throws IOException {
    for(int i =0, v = offset; i < MAGIC_LENGTH; i++, v++){
      if(ParquetFileWriter.MAGIC[i] != data[v]){
        byte[] magic = ArrayUtils.subarray(data, offset, offset + MAGIC_LENGTH);
        throw new IOException(status.getPath() + " is not a Parquet file. expected magic number at tail " + Arrays.toString(ParquetFileWriter.MAGIC) + " but found " + Arrays.toString(magic));
      }
    }
  }

  /**
   * A function to get a list of footers.
   *
   * @param conf configuration for file system
   * @param statuses list of file statuses
   * @param parallelism parallelism
   * @return a list of footers
   * @throws IOException
   */
  public static List<Footer> getFooters(final Configuration conf, List<FileStatus> statuses, int parallelism) throws IOException {
    final List<TimedCallable<Footer>> readers = new ArrayList<>();
    final List<Footer> foundFooters = new ArrayList<>();
    for (FileStatus status : statuses) {


      if (status.isDirectory()){
        // first we check for summary file.
        FileSystem fs = status.getPath().getFileSystem(conf);

        final Path summaryPath = new Path(status.getPath(), ParquetFileWriter.PARQUET_METADATA_FILE);
        if (fs.exists(summaryPath)){
          FileStatus summaryStatus = fs.getFileStatus(summaryPath);
          foundFooters.addAll(ParquetFileReader.readSummaryFile(conf, summaryStatus));
          continue;
        }

        // else we handle as normal file.
        for (FileStatus inStatus : DrillFileSystemUtil.listFiles(fs, status.getPath(), false)){
          readers.add(new FooterReader(conf, inStatus));
        }
      } else {
        readers.add(new FooterReader(conf, status));
      }

    }
    if(!readers.isEmpty()){
      foundFooters.addAll(TimedCallable.run("Fetch Parquet Footers", logger, readers, parallelism));
    }

    return foundFooters;
  }


  private static class FooterReader extends TimedCallable<Footer> {

    final Configuration conf;
    final FileStatus status;

    public FooterReader(Configuration conf, FileStatus status) {
      super();
      this.conf = conf;
      this.status = status;
    }

    @Override
    protected Footer runInner() throws Exception {
      return readFooter(conf, status);
    }

    @Override
    public String toString() {
      return new ToStringBuilder(this, SHORT_PREFIX_STYLE).append("path", status.getPath()).toString();
    }
  }

  /**
   * An updated footer reader that tries to read the entire footer without knowing the length.
   * This should reduce the amount of seek/read roundtrips in most workloads.
   * @param config configuration for file system
   * @param status file status
   * @return Footer
   * @throws IOException
   */
  public static Footer readFooter(final Configuration config, final FileStatus status) throws IOException {
    final FileSystem fs = status.getPath().getFileSystem(config);
    try(FSDataInputStream file = fs.open(status.getPath())) {

      final long fileLength = status.getLen();
      Preconditions.checkArgument(fileLength >= MIN_FILE_SIZE, "%s is not a Parquet file (too small)", status.getPath());

      int len = (int) Math.min( fileLength, (long) DEFAULT_READ_SIZE);
      byte[] footerBytes = new byte[len];
      readFully(file, fileLength - len, footerBytes, 0, len);

      checkMagicBytes(status, footerBytes, footerBytes.length - ParquetFileWriter.MAGIC.length);
      final int size = BytesUtils.readIntLittleEndian(footerBytes, footerBytes.length - FOOTER_METADATA_SIZE);

      if(size > footerBytes.length - FOOTER_METADATA_SIZE){
        // if the footer is larger than our initial read, we need to read the rest.
        byte[] origFooterBytes = footerBytes;
        int origFooterRead = origFooterBytes.length - FOOTER_METADATA_SIZE;

        footerBytes = new byte[size];

        readFully(file, fileLength - size - FOOTER_METADATA_SIZE, footerBytes, 0, size - origFooterRead);
        System.arraycopy(origFooterBytes, 0, footerBytes, size - origFooterRead, origFooterRead);
      }else{
        int start = footerBytes.length - (size + FOOTER_METADATA_SIZE);
        footerBytes = ArrayUtils.subarray(footerBytes, start, start + size);
      }

      final ByteArrayInputStream from = new ByteArrayInputStream(footerBytes);
      ParquetMetadata metadata = ParquetFormatPlugin.parquetMetadataConverter.readParquetMetadata(from, NO_FILTER);
      Footer footer = new Footer(status.getPath(), metadata);
      return footer;
    }
  }
}
