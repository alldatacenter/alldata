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
package org.apache.parquet.hadoop;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;

import org.apache.parquet.bytes.BytesInput;
import org.apache.parquet.bytes.CapacityByteArrayOutputStream;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.column.Encoding;
import org.apache.parquet.column.ParquetProperties;
import org.apache.parquet.column.page.DictionaryPage;
import org.apache.parquet.column.page.PageWriteStore;
import org.apache.parquet.column.page.PageWriter;
import org.apache.parquet.column.statistics.Statistics;
import org.apache.parquet.column.values.bloomfilter.BloomFilter;
import org.apache.parquet.column.values.bloomfilter.BloomFilterWriteStore;
import org.apache.parquet.column.values.bloomfilter.BloomFilterWriter;
import org.apache.parquet.compression.CompressionCodecFactory.BytesInputCompressor;
import org.apache.parquet.crypto.AesCipher;
import org.apache.parquet.crypto.InternalColumnEncryptionSetup;
import org.apache.parquet.crypto.InternalFileEncryptor;
import org.apache.parquet.crypto.ModuleCipherFactory.ModuleType;
import org.apache.parquet.format.BlockCipher;
import org.apache.parquet.format.converter.ParquetMetadataConverter;
import org.apache.parquet.hadoop.metadata.ColumnPath;
import org.apache.parquet.internal.column.columnindex.ColumnIndexBuilder;
import org.apache.parquet.internal.column.columnindex.OffsetIndexBuilder;
import org.apache.parquet.io.ParquetEncodingException;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.bytes.ByteBufferAllocator;
import org.apache.yetus.audience.InterfaceAudience;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@InterfaceAudience.Private
public class ParquetColumnChunkPageWriteStore implements PageWriteStore, BloomFilterWriteStore, Closeable {
  private static final Logger LOG = LoggerFactory.getLogger(ParquetColumnChunkPageWriteStore.class);

  private static ParquetMetadataConverter parquetMetadataConverter = new ParquetMetadataConverter();

  private static final class ColumnChunkPageWriter implements PageWriter, BloomFilterWriter, Closeable {

    private final ColumnDescriptor path;
    private final BytesInputCompressor compressor;
    private final CapacityByteArrayOutputStream tempOutputStream;
    private final CapacityByteArrayOutputStream buf;
    private DictionaryPage dictionaryPage;

    private long uncompressedLength;
    private long compressedLength;
    private long totalValueCount;
    private int pageCount;

    // repetition and definition level encodings are used only for v1 pages and don't change
    private Set<Encoding> rlEncodings = new HashSet<Encoding>();
    private Set<Encoding> dlEncodings = new HashSet<Encoding>();
    private List<Encoding> dataEncodings = new ArrayList<Encoding>();

    private BloomFilter bloomFilter;
    private ColumnIndexBuilder columnIndexBuilder;
    private OffsetIndexBuilder offsetIndexBuilder;
    private Statistics totalStatistics;
    private final ByteBufferAllocator allocator;

    private final CRC32 crc;
    boolean pageWriteChecksumEnabled;

    private final BlockCipher.Encryptor headerBlockEncryptor;
    private final BlockCipher.Encryptor pageBlockEncryptor;
    private final int rowGroupOrdinal;
    private final int columnOrdinal;
    private int pageOrdinal;
    private final byte[] dataPageAAD;
    private final byte[] dataPageHeaderAAD;
    private final byte[] fileAAD;

    private ColumnChunkPageWriter(ColumnDescriptor path,
                                  BytesInputCompressor compressor,
                                  int initialSlabSize,
                                  int maxCapacityHint,
                                  ByteBufferAllocator allocator,
                                  int columnIndexTruncateLength,
                                  boolean pageWriteChecksumEnabled,
                                  BlockCipher.Encryptor headerBlockEncryptor,
                                  BlockCipher.Encryptor pageBlockEncryptor,
                                  byte[] fileAAD,
                                  int rowGroupOrdinal,
                                  int columnOrdinal) {
      this.path = path;
      this.compressor = compressor;
      this.allocator = allocator;
      this.tempOutputStream = new CapacityByteArrayOutputStream(initialSlabSize, maxCapacityHint, allocator);
      this.buf = new CapacityByteArrayOutputStream(initialSlabSize, maxCapacityHint, allocator);
      this.columnIndexBuilder = ColumnIndexBuilder.getBuilder(path.getPrimitiveType(), columnIndexTruncateLength);
      this.offsetIndexBuilder = OffsetIndexBuilder.getBuilder();
      this.pageWriteChecksumEnabled = pageWriteChecksumEnabled;
      this.crc = pageWriteChecksumEnabled ? new CRC32() : null;

      this.headerBlockEncryptor = headerBlockEncryptor;
      this.pageBlockEncryptor = pageBlockEncryptor;
      this.fileAAD = fileAAD;
      this.rowGroupOrdinal = rowGroupOrdinal;
      this.columnOrdinal = columnOrdinal;
      this.pageOrdinal = -1;
      if (null != headerBlockEncryptor) {
        dataPageHeaderAAD = AesCipher.createModuleAAD(fileAAD, ModuleType.DataPageHeader,
                rowGroupOrdinal, columnOrdinal, 0);
      } else {
        dataPageHeaderAAD = null;
      }
      if (null != pageBlockEncryptor) {
        dataPageAAD = AesCipher.createModuleAAD(fileAAD, ModuleType.DataPage,
                rowGroupOrdinal, columnOrdinal, 0);
      } else {
        dataPageAAD = null;
      }
    }

    @Override
    @Deprecated
    public void writePage(BytesInput bytesInput, int valueCount, Statistics<?> statistics, Encoding rlEncoding,
                          Encoding dlEncoding, Encoding valuesEncoding) throws IOException {
      // Setting the builders to the no-op ones so no column/offset indexes will be written for this column chunk
      columnIndexBuilder = ColumnIndexBuilder.getNoOpBuilder();
      offsetIndexBuilder = OffsetIndexBuilder.getNoOpBuilder();

      writePage(bytesInput, valueCount, -1, statistics, rlEncoding, dlEncoding, valuesEncoding);
    }

    @Override
    public void writePage(BytesInput bytes,
                          int valueCount,
                          int rowCount,
                          Statistics statistics,
                          Encoding rlEncoding,
                          Encoding dlEncoding,
                          Encoding valuesEncoding) throws IOException {
      pageOrdinal++;
      long uncompressedSize = bytes.size();
      if (uncompressedSize > Integer.MAX_VALUE) {
        throw new ParquetEncodingException(
                "Cannot write page larger than Integer.MAX_VALUE bytes: " +
                        uncompressedSize);
      }
      BytesInput compressedBytes = compressor.compress(bytes);
      if (null != pageBlockEncryptor) {
        AesCipher.quickUpdatePageAAD(dataPageAAD, pageOrdinal);
        compressedBytes = BytesInput.from(pageBlockEncryptor.encrypt(compressedBytes.toByteArray(), dataPageAAD));
      }
      long compressedSize = compressedBytes.size();
      if (compressedSize > Integer.MAX_VALUE) {
        throw new ParquetEncodingException(
                "Cannot write compressed page larger than Integer.MAX_VALUE bytes: "
                        + compressedSize);
      }
      tempOutputStream.reset();
      if (null != headerBlockEncryptor) {
        AesCipher.quickUpdatePageAAD(dataPageHeaderAAD, pageOrdinal);
      }
      if (pageWriteChecksumEnabled) {
        crc.reset();
        crc.update(compressedBytes.toByteArray());
        parquetMetadataConverter.writeDataPageV1Header(
                (int)uncompressedSize,
                (int)compressedSize,
                valueCount,
                rlEncoding,
                dlEncoding,
                valuesEncoding,
                (int) crc.getValue(),
                tempOutputStream,
                headerBlockEncryptor,
                dataPageHeaderAAD);
      } else {
        parquetMetadataConverter.writeDataPageV1Header(
                (int)uncompressedSize,
                (int)compressedSize,
                valueCount,
                rlEncoding,
                dlEncoding,
                valuesEncoding,
                tempOutputStream,
                headerBlockEncryptor,
                dataPageHeaderAAD);
      }
      this.uncompressedLength += uncompressedSize;
      this.compressedLength += compressedSize;
      this.totalValueCount += valueCount;
      this.pageCount += 1;

      // Copying the statistics if it is not initialized yet so we have the correct typed one
      if (totalStatistics == null) {
        totalStatistics = statistics.copy();
      } else {
        totalStatistics.mergeStatistics(statistics);
      }

      columnIndexBuilder.add(statistics);
      offsetIndexBuilder.add(toIntWithCheck(tempOutputStream.size() + compressedSize), rowCount);

      // by concatenating before writing instead of writing twice,
      // we only allocate one buffer to copy into instead of multiple.
      BytesInput.concat(BytesInput.from(tempOutputStream), compressedBytes).writeAllTo(buf); // is used instead of above
      rlEncodings.add(rlEncoding);
      dlEncodings.add(dlEncoding);
      dataEncodings.add(valuesEncoding);
    }

    @Override
    public void writePageV2(
            int rowCount, int nullCount, int valueCount,
            BytesInput repetitionLevels, BytesInput definitionLevels,
            Encoding dataEncoding, BytesInput data,
            Statistics<?> statistics) throws IOException {
      pageOrdinal++;

      int rlByteLength = toIntWithCheck(repetitionLevels.size());
      int dlByteLength = toIntWithCheck(definitionLevels.size());
      int uncompressedSize = toIntWithCheck(
              data.size() + repetitionLevels.size() + definitionLevels.size()
      );
      // TODO: decide if we compress
      BytesInput compressedData = compressor.compress(data);
      if (null != pageBlockEncryptor) {
        AesCipher.quickUpdatePageAAD(dataPageAAD, pageOrdinal);
        compressedData = BytesInput.from(pageBlockEncryptor.encrypt(compressedData.toByteArray(), dataPageAAD));
      }
      int compressedSize = toIntWithCheck(
              compressedData.size() + repetitionLevels.size() + definitionLevels.size()
      );
      tempOutputStream.reset();
      if (null != headerBlockEncryptor) {
        AesCipher.quickUpdatePageAAD(dataPageHeaderAAD, pageOrdinal);
      }
      parquetMetadataConverter.writeDataPageV2Header(
              uncompressedSize, compressedSize,
              valueCount, nullCount, rowCount,
              dataEncoding,
              rlByteLength,
              dlByteLength,
              tempOutputStream,
              headerBlockEncryptor,
              dataPageHeaderAAD);
      this.uncompressedLength += uncompressedSize;
      this.compressedLength += compressedSize;
      this.totalValueCount += valueCount;
      this.pageCount += 1;

      // Copying the statistics if it is not initialized yet so we have the correct typed one
      if (totalStatistics == null) {
        totalStatistics = statistics.copy();
      } else {
        totalStatistics.mergeStatistics(statistics);
      }

      columnIndexBuilder.add(statistics);
      offsetIndexBuilder.add(toIntWithCheck((long) tempOutputStream.size() + compressedSize), rowCount);

      // by concatenating before writing instead of writing twice,
      // we only allocate one buffer to copy into instead of multiple.
      BytesInput.concat(
              BytesInput.from(tempOutputStream),
              repetitionLevels,
              definitionLevels,
              compressedData).writeAllTo(buf);
      dataEncodings.add(dataEncoding);
    }

    private int toIntWithCheck(long size) {
      if (size > Integer.MAX_VALUE) {
        throw new ParquetEncodingException(
                "Cannot write page larger than " + Integer.MAX_VALUE + " bytes: " +
                        size);
      }
      return (int)size;
    }

    @Override
    public long getMemSize() {
      return buf.size();
    }

    public void writeToFileWriter(ParquetFileWriter writer) throws IOException {
      if (null == headerBlockEncryptor) {
        writer.writeColumnChunk(
                path,
                totalValueCount,
                compressor.getCodecName(),
                dictionaryPage,
                BytesInput.from(buf),
                uncompressedLength,
                compressedLength,
                totalStatistics,
                columnIndexBuilder,
                offsetIndexBuilder,
                bloomFilter,
                rlEncodings,
                dlEncodings,
                dataEncodings);
      } else {
        writer.writeColumnChunk(
                path,
                totalValueCount,
                compressor.getCodecName(),
                dictionaryPage,
                BytesInput.from(buf),
                uncompressedLength,
                compressedLength,
                totalStatistics,
                columnIndexBuilder,
                offsetIndexBuilder,
                bloomFilter,
                rlEncodings,
                dlEncodings,
                dataEncodings,
                headerBlockEncryptor,
                rowGroupOrdinal,
                columnOrdinal,
                fileAAD);
      }
      if (LOG.isDebugEnabled()) {
        LOG.debug(
                String.format(
                        "written %,dB for %s: %,d values, %,dB raw, %,dB comp, %d pages, encodings: %s",
                        buf.size(), path, totalValueCount, uncompressedLength, compressedLength, pageCount, new HashSet<Encoding>(dataEncodings))
                        + (dictionaryPage != null ? String.format(
                        ", dic { %,d entries, %,dB raw, %,dB comp}",
                        dictionaryPage.getDictionarySize(), dictionaryPage.getUncompressedSize(), dictionaryPage.getDictionarySize())
                        : ""));
      }
      rlEncodings.clear();
      dlEncodings.clear();
      dataEncodings.clear();
      pageCount = 0;
      pageOrdinal = -1;
    }

    @Override
    public long allocatedSize() {
      return buf.size();
    }

    @Override
    public void writeDictionaryPage(DictionaryPage dictionaryPage) throws IOException {
      if (this.dictionaryPage != null) {
        throw new ParquetEncodingException("Only one dictionary page is allowed");
      }
      BytesInput dictionaryBytes = dictionaryPage.getBytes();
      int uncompressedSize = (int)dictionaryBytes.size();
      BytesInput compressedBytes = compressor.compress(dictionaryBytes);
      if (null != pageBlockEncryptor) {
        byte[] dictonaryPageAAD = AesCipher.createModuleAAD(fileAAD, ModuleType.DictionaryPage,
                rowGroupOrdinal, columnOrdinal, -1);
        compressedBytes = BytesInput.from(pageBlockEncryptor.encrypt(compressedBytes.toByteArray(), dictonaryPageAAD));
      }
      this.dictionaryPage = new DictionaryPage(BytesInput.copy(compressedBytes), uncompressedSize,
              dictionaryPage.getDictionarySize(), dictionaryPage.getEncoding());
    }

    @Override
    public String memUsageString(String prefix) {
      return buf.memUsageString(prefix + " ColumnChunkPageWriter");
    }

    @Override
    public void writeBloomFilter(BloomFilter bloomFilter) {
      this.bloomFilter = bloomFilter;
    }

    @Override
    public void close() {
      tempOutputStream.close();
      buf.close();
    }
  }

  private final Map<ColumnDescriptor, ColumnChunkPageWriter> writers = new HashMap<ColumnDescriptor, ColumnChunkPageWriter>();
  private final MessageType schema;

  public ParquetColumnChunkPageWriteStore(BytesInputCompressor compressor, MessageType schema, int initialSlabSize,
                                          int maxCapacityHint, ByteBufferAllocator allocator,
                                          int columnIndexTruncateLength) {
    this(compressor, schema, initialSlabSize, maxCapacityHint, allocator, columnIndexTruncateLength,
            ParquetProperties.DEFAULT_PAGE_WRITE_CHECKSUM_ENABLED);
  }

  public ParquetColumnChunkPageWriteStore(BytesInputCompressor compressor, MessageType schema, int initialSlabSize,
                                          int maxCapacityHint, ByteBufferAllocator allocator,
                                          int columnIndexTruncateLength, boolean pageWriteChecksumEnabled) {
    this.schema = schema;
    for (ColumnDescriptor path : schema.getColumns()) {
      writers.put(path, new ColumnChunkPageWriter(path, compressor, initialSlabSize, maxCapacityHint, allocator, columnIndexTruncateLength,
              pageWriteChecksumEnabled, null, null, null, -1, -1));
    }
  }

  public ParquetColumnChunkPageWriteStore(BytesInputCompressor compressor, MessageType schema, int initialSlabSize,
                                          int maxCapacityHint, ByteBufferAllocator allocator,
                                          int columnIndexTruncateLength, boolean pageWriteChecksumEnabled,
                                          InternalFileEncryptor fileEncryptor, int rowGroupOrdinal) {
    this.schema = schema;
    if (null == fileEncryptor) {
      for (ColumnDescriptor path : schema.getColumns()) {
        writers.put(path, new ColumnChunkPageWriter(path, compressor, initialSlabSize, maxCapacityHint, allocator,
                columnIndexTruncateLength, pageWriteChecksumEnabled, null, null,
                null, -1, -1));
      }
      return;
    }

    // Encrypted file
    int columnOrdinal = -1;
    byte[] fileAAD = fileEncryptor.getFileAAD();
    for (ColumnDescriptor path : schema.getColumns()) {
      columnOrdinal++;
      BlockCipher.Encryptor headerBlockEncryptor = null;
      BlockCipher.Encryptor pageBlockEncryptor = null;
      ColumnPath columnPath = ColumnPath.get(path.getPath());

      InternalColumnEncryptionSetup columnSetup = fileEncryptor.getColumnSetup(columnPath, true, columnOrdinal);
      if (columnSetup.isEncrypted()) {
        headerBlockEncryptor = columnSetup.getMetaDataEncryptor();
        pageBlockEncryptor = columnSetup.getDataEncryptor();
      }

      writers.put(path,  new ColumnChunkPageWriter(path, compressor, initialSlabSize, maxCapacityHint, allocator,
              columnIndexTruncateLength, pageWriteChecksumEnabled, headerBlockEncryptor, pageBlockEncryptor, fileAAD,
              rowGroupOrdinal, columnOrdinal));
    }
  }

  @Override
  public PageWriter getPageWriter(ColumnDescriptor path) {
    return writers.get(path);
  }

  @Override
  public BloomFilterWriter getBloomFilterWriter(ColumnDescriptor path) {
    return writers.get(path);
  }



  public void flushToFileWriter(ParquetFileWriter writer) throws IOException {
    for (ColumnDescriptor path : schema.getColumns()) {
      ColumnChunkPageWriter pageWriter = writers.get(path);
      pageWriter.writeToFileWriter(writer);
    }
  }

  @Override
  public void close() {
    for (ColumnChunkPageWriter pageWriter : writers.values()) {
      pageWriter.close();
    }
  }

}
