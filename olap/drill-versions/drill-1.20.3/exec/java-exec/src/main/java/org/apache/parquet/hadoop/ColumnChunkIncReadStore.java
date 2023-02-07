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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.store.parquet.DataPageHeaderInfoProvider;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.bytes.BytesInput;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.column.page.DataPage;
import org.apache.parquet.column.page.DataPageV1;
import org.apache.parquet.column.page.DataPageV2;
import org.apache.parquet.column.page.DictionaryPage;
import org.apache.parquet.column.page.PageReadStore;
import org.apache.parquet.column.page.PageReader;
import org.apache.parquet.column.statistics.Statistics;
import org.apache.parquet.compression.CompressionCodecFactory;
import org.apache.parquet.compression.CompressionCodecFactory.BytesInputDecompressor;
import org.apache.parquet.format.PageHeader;
import org.apache.parquet.format.Util;
import org.apache.parquet.format.converter.ParquetMetadataConverter;
import org.apache.parquet.hadoop.metadata.ColumnChunkMetaData;
import org.apache.parquet.hadoop.util.HadoopStreams;

import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ColumnChunkIncReadStore implements PageReadStore {
  private static final Logger logger = LoggerFactory.getLogger(ColumnChunkIncReadStore.class);

  private static ParquetMetadataConverter METADATA_CONVERTER = new ParquetMetadataConverter();

  private CompressionCodecFactory codecFactory;
  private BufferAllocator allocator;
  private FileSystem fs;
  private Path path;
  private long rowCount;
  private List<FSDataInputStream> streams = new ArrayList<>();

  public ColumnChunkIncReadStore(long rowCount, CompressionCodecFactory codecFactory, BufferAllocator allocator,
      FileSystem fs, Path path) {
    this.codecFactory = codecFactory;
    this.allocator = allocator;
    this.fs = fs;
    this.path = path;
    this.rowCount = rowCount;
  }


  public class ColumnChunkIncPageReader implements PageReader {

    ColumnChunkMetaData metaData;
    ColumnDescriptor columnDescriptor;
    long fileOffset;
    long size;
    private long valueReadSoFar = 0;

    private DictionaryPage dictionaryPage;
    private FSDataInputStream in;
    private BytesInputDecompressor decompressor;

    private ByteBuf lastPage;

    public ColumnChunkIncPageReader(ColumnChunkMetaData metaData, ColumnDescriptor columnDescriptor, FSDataInputStream in) throws IOException {
      this.metaData = metaData;
      this.columnDescriptor = columnDescriptor;
      this.size = metaData.getTotalSize();
      this.fileOffset = metaData.getStartingPos();
      this.in = in;
      this.decompressor = codecFactory.getDecompressor(metaData.getCodec());
    }

    @Override
    public DictionaryPage readDictionaryPage() {
      if (dictionaryPage == null) {
        PageHeader pageHeader = new PageHeader();
        long pos = 0;

        try {
          pos = in.getPos();
          pageHeader = Util.readPageHeader(in);

          if (pageHeader.getDictionary_page_header() == null) {
            in.seek(pos);
            return null;
          }

          BytesInput dictPageBytes = decompressor.decompress(
              BytesInput.from(in, pageHeader.compressed_page_size),
              pageHeader.getUncompressed_page_size()
          );

          dictionaryPage = new DictionaryPage(
              dictPageBytes,
              pageHeader.getDictionary_page_header().getNum_values(),
              METADATA_CONVERTER.getEncoding(pageHeader.dictionary_page_header.encoding)
          );
        } catch (Exception e) {
          throw new DrillRuntimeException("Error reading dictionary page." +
            "\nFile path: " + path.toUri().getPath() +
            "\nRow count: " + rowCount +
            "\nColumn Chunk Metadata: " + metaData +
            "\nPage Header: " + pageHeader +
            "\nFile offset: " + fileOffset +
            "\nSize: " + size +
            "\nValue read so far: " + valueReadSoFar +
            "\nPosition: " + pos, e);
        }
      }
      return dictionaryPage;
    }

    @Override
    public long getTotalValueCount() {
      return metaData.getValueCount();
    }

    @Override
    public DataPage readPage() {
      PageHeader pageHeader = new PageHeader();
      try {
        if (lastPage != null) {
          lastPage.release();
          lastPage = null;
        }
        while(valueReadSoFar < metaData.getValueCount()) {
          pageHeader = Util.readPageHeader(in);
          int compPageSize = pageHeader.getCompressed_page_size();
          int pageSize = pageHeader.getUncompressed_page_size();
          DataPageHeaderInfoProvider pageHeaderInfo;

          switch (pageHeader.type) {
            case DICTIONARY_PAGE:
              if (dictionaryPage == null) {
                BytesInput pageBytes = decompressor.decompress(
                    BytesInput.from(in, compPageSize),
                    pageSize
                );

                dictionaryPage = new DictionaryPage(
                    pageBytes,
                    pageSize,
                    METADATA_CONVERTER.getEncoding(pageHeader.dictionary_page_header.encoding)
                );
              } else {
                in.skip(compPageSize);
              }
              break;
            case DATA_PAGE:
              pageHeaderInfo = DataPageHeaderInfoProvider.builder(pageHeader);
              valueReadSoFar += pageHeaderInfo.getNumValues();
              ByteBuf buf = allocator.buffer(compPageSize);
              lastPage = buf;
              ByteBuffer pageBuf = buf.nioBuffer(0, compPageSize);
              HadoopStreams.wrap(in).readFully(pageBuf);
              pageBuf.flip();

              BytesInput pageBytes = decompressor.decompress(BytesInput.from(pageBuf), pageSize);

              Statistics stats = METADATA_CONVERTER.fromParquetStatistics(
                null,
                pageHeaderInfo.getStatistics(),
                columnDescriptor.getPrimitiveType()
              );

              return new DataPageV1(
                  pageBytes,
                  pageHeaderInfo.getNumValues(),
                  pageSize,
                  stats,
                  METADATA_CONVERTER.getEncoding(pageHeaderInfo.getRepetitionLevelEncoding()),
                  METADATA_CONVERTER.getEncoding(pageHeaderInfo.getDefinitionLevelEncoding()),
                  METADATA_CONVERTER.getEncoding(pageHeaderInfo.getEncoding())
              );
            case DATA_PAGE_V2:
              pageHeaderInfo = DataPageHeaderInfoProvider.builder(pageHeader);
              int repLevelSize = pageHeader.data_page_header_v2.getRepetition_levels_byte_length();
              int defLevelSize = pageHeader.data_page_header_v2.getDefinition_levels_byte_length();
              valueReadSoFar += pageHeaderInfo.getNumValues();

              buf = allocator.buffer(compPageSize);
              lastPage = buf;
              pageBuf = buf.nioBuffer(0, compPageSize);
              HadoopStreams.wrap(in).readFully(pageBuf);
              pageBuf.flip();

              // Note that the repetition and definition levels are stored uncompressed in
              // the v2 page format.
              int pageBufOffset = 0;
              ByteBuffer bb = (ByteBuffer) pageBuf.position(pageBufOffset);
              BytesInput repLevelBytes = BytesInput.from(
                (ByteBuffer) bb.slice().limit(pageBufOffset + repLevelSize)
              );
              pageBufOffset += repLevelSize;

              bb = (ByteBuffer) pageBuf.position(pageBufOffset);
              final BytesInput defLevelBytes = BytesInput.from(
                (ByteBuffer) bb.slice().limit(pageBufOffset + defLevelSize)
              );
              pageBufOffset += defLevelSize;

              // we've now reached the beginning of compressed column data
              bb = (ByteBuffer) pageBuf.position(pageBufOffset);
              final BytesInput colDataBytes = decompressor.decompress(
                BytesInput.from((ByteBuffer) bb.slice()),
                pageSize - repLevelSize - defLevelSize
              );

              stats = METADATA_CONVERTER.fromParquetStatistics(
                null,
                pageHeaderInfo.getStatistics(),
                columnDescriptor.getPrimitiveType()
              );

              return new DataPageV2(
                pageHeader.data_page_header_v2.getNum_rows(),
                pageHeader.data_page_header_v2.getNum_nulls(),
                pageHeaderInfo.getNumValues(),
                repLevelBytes,
                defLevelBytes,
                METADATA_CONVERTER.getEncoding(pageHeaderInfo.getEncoding()),
                colDataBytes,
                pageSize,
                stats,
                pageHeader.data_page_header_v2.isIs_compressed()
              );
            default:
              logger.warn("skipping page of type {} of size {}", pageHeader.getType(), compPageSize);
              in.skip(compPageSize);
              break;
          }
        }
        in.close();
        return null;
      } catch (OutOfMemoryException e) {
        throw e; // throw as it is
      } catch (Exception e) {
        throw new DrillRuntimeException("Error reading page." +
          "\nFile path: " + path.toUri().getPath() +
          "\nRow count: " + rowCount +
          "\nColumn Chunk Metadata: " + metaData +
          "\nPage Header: " + pageHeader +
          "\nFile offset: " + fileOffset +
          "\nSize: " + size +
          "\nValue read so far: " + valueReadSoFar, e);
      }
    }

    void close() {
      decompressor.release();

      if (lastPage != null) {
        lastPage.release();
        lastPage = null;
      }
    }
  }

  private Map<ColumnDescriptor, ColumnChunkIncPageReader> columns = new HashMap<>();

  public void addColumn(ColumnDescriptor descriptor, ColumnChunkMetaData metaData) throws IOException {
    FSDataInputStream in = fs.open(path);
    streams.add(in);
    in.seek(metaData.getStartingPos());
    ColumnChunkIncPageReader reader = new ColumnChunkIncPageReader(metaData, descriptor, in);

    columns.put(descriptor, reader);
  }

  public void close() throws IOException {
    for (FSDataInputStream stream : streams) {
      stream.close();
    }
    for (ColumnChunkIncPageReader reader : columns.values()) {
      reader.close();
    }
  }

  @Override
  public PageReader getPageReader(ColumnDescriptor descriptor) {
    return columns.get(descriptor);
  }

  @Override
  public long getRowCount() {
    return rowCount;
  }

  @Override
  public String toString() {
    return "ColumnChunkIncReadStore[File=" + path.toUri() + "]";
  }
}
