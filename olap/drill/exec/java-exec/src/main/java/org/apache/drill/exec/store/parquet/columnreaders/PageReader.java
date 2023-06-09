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
package org.apache.drill.exec.store.parquet.columnreaders;

import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.store.parquet.DataPageHeaderInfoProvider;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import io.netty.buffer.ByteBufUtil;
import org.apache.drill.exec.util.filereader.BufferedDirectBufInputStream;
import io.netty.buffer.DrillBuf;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.store.parquet.ParquetFormatPlugin;
import org.apache.drill.exec.store.parquet.ParquetReaderStats;
import org.apache.drill.exec.util.filereader.DirectBufInputStream;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.bytes.ByteBufferInputStream;
import org.apache.parquet.bytes.BytesInput;
import org.apache.parquet.bytes.BytesUtils;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.column.Dictionary;
import org.apache.parquet.column.Encoding;
import org.apache.parquet.column.ValuesType;
import org.apache.parquet.column.page.DictionaryPage;
import org.apache.parquet.column.values.ValuesReader;
import org.apache.parquet.column.values.dictionary.DictionaryValuesReader;
import org.apache.parquet.column.values.rle.RunLengthBitPackingHybridDecoder;
import org.apache.parquet.compression.CompressionCodecFactory;
import org.apache.parquet.compression.CompressionCodecFactory.BytesInputDecompressor;
import org.apache.parquet.format.PageHeader;
import org.apache.parquet.format.PageType;
import org.apache.parquet.format.Util;
import org.apache.parquet.format.converter.ParquetMetadataConverter;
import org.apache.parquet.hadoop.metadata.ColumnChunkMetaData;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.io.ParquetDecodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import static org.apache.parquet.column.Encoding.valueOf;

// class to keep track of the read position of variable length columns
class PageReader {
  static final Logger logger = LoggerFactory.getLogger(PageReader.class);

  public static final ParquetMetadataConverter METADATA_CONVERTER = ParquetFormatPlugin.parquetMetadataConverter;

  protected final org.apache.drill.exec.store.parquet.columnreaders.ColumnReader<?> parentColumnReader;
  protected final DirectBufInputStream dataReader;
  //buffer to store bytes of current page
  protected DrillBuf pageData;

  // for variable length data we need to keep track of our current position in the page data
  // as the values and lengths are intermixed, making random access to the length data impossible
  long readyToReadPosInBytes;
  // read position in the current page, stored in the ByteBuf in ParquetRecordReader called bufferWithAllData
  long readPosInBytes;
  // storage space for extra bits at the end of a page if they did not line up with a byte boundary
  // prevents the need to keep the entire last page, as these pageDataByteArray need to be added to the next batch
  //byte extraBits;

  // used for columns where the number of values that will fit in a vector is unknown
  // currently used for variable length
  // TODO - reuse this when compressed vectors are added, where fixed length values will take up a
  // variable amount of space
  // For example: if nulls are stored without extra space left in the data vector
  // (this is currently simplifying random access to the data during processing, but increases the size of the vectors)
  int valuesReadyToRead;

  // the number of values read out of the last page
  int valuesRead;
  int byteLength;
  //int rowGroupIndex;
  IntIterator definitionLevels;
  IntIterator repetitionLevels;
  private ValuesReader valueReader;
  private ValuesReader dictionaryLengthDeterminingReader;
  private ValuesReader dictionaryValueReader;
  Dictionary dictionary;
  PageHeader pageHeader;

  int pageValueCount = -1;

  protected FSDataInputStream inputStream;

  // This needs to be held throughout reading of the entire column chunk
  DrillBuf dictData;

  protected final CompressionCodecFactory codecFactory;
  protected final CompressionCodecName codecName;
  protected final BufferAllocator allocator;
  protected final ColumnDescriptor columnDescriptor;
  protected final ColumnChunkMetaData columnChunkMetaData;
  protected final String fileName;

  protected final ParquetReaderStats stats;
  private final boolean useBufferedReader;
  private final int scanBufferSize;
  private final boolean useFadvise;
  private final boolean enforceTotalSize;

  protected final String debugName;
  private DataPageHeaderInfoProvider dataPageInfo;

  PageReader(ColumnReader<?> columnReader, FileSystem fs, Path path)
    throws ExecutionSetupException {
    this.parentColumnReader = columnReader;
    this.columnDescriptor = parentColumnReader.getColumnDescriptor();
    this.columnChunkMetaData = columnReader.columnChunkMetaData;
    this.codecFactory = parentColumnReader.parentReader.getCodecFactory();
    this.codecName = parentColumnReader.columnChunkMetaData.getCodec();
    this.allocator = parentColumnReader.parentReader.getOperatorContext().getAllocator();
    this.stats = parentColumnReader.parentReader.parquetReaderStats;
    this.fileName = path.toString();
    debugName = new StringBuilder()
       .append(this.parentColumnReader.parentReader.getFragmentContext().getFragIdString())
       .append(":")
       .append(this.parentColumnReader.parentReader.getOperatorContext().getStats().getId() )
       .append(this.parentColumnReader.columnChunkMetaData.toString() )
       .toString();
    try {
      inputStream  = fs.open(path);
      useBufferedReader  = parentColumnReader.parentReader.useBufferedReader;
      scanBufferSize = parentColumnReader.parentReader.bufferedReadSize;
      useFadvise = parentColumnReader.parentReader.useFadvise;
      enforceTotalSize = parentColumnReader.parentReader.enforceTotalSize;

      if (useBufferedReader) {
        this.dataReader = new BufferedDirectBufInputStream(inputStream, allocator, path.getName(),
            columnChunkMetaData.getStartingPos(), columnChunkMetaData.getTotalSize(), scanBufferSize,
            enforceTotalSize, useFadvise);
      } else {
        this.dataReader = new DirectBufInputStream(inputStream, allocator, path.getName(),
            columnChunkMetaData.getStartingPos(), columnChunkMetaData.getTotalSize(), enforceTotalSize,
            useFadvise);
      }
    } catch (IOException e) {
      throw new ExecutionSetupException("Error opening or reading metadata for parquet file at location: "
          + path.getName(), e);
    }
  }

  protected void throwUserException(Exception e, String msg) throws UserException {
    UserException ex = UserException.dataReadError(e).message(msg)
        .pushContext("Row Group Start: ", columnChunkMetaData.getStartingPos())
        .pushContext("Column: ", this.parentColumnReader.schemaElement.getName())
        .pushContext("File: ", this.fileName).build(logger);
    throw ex;
  }

  protected void init() throws IOException{
    dataReader.init();

    // If getDictionaryPageOffset() was reliable we could read the dictionary page once
    // and for all here.  Instead we must encounter the dictionary page during calls to next()
    // and the code that follows remains commented out.
    /*
    long dictPageOffset = columnChunkMetaData.getDictionaryPageOffset();
    if (dictPageOffset < dataReader.getPos()) {
      return; // this column chunk has no dictionary page
    }

    // advance to the start of the dictionary page
    skip(dictPageOffset - dataReader.getPos());
    nextPageHeader();
    loadDictionary();
     */
  }

  /**
   * Skip over n bytes of column data
   * @param n number of bytes to skip
   * @throws IOException
   */
  protected void skip(long n) throws IOException {
    assert n >= 0;

    while (n > 0) {
      long skipped = dataReader.skip(n);
      if (skipped > 0) {
        n -= skipped;
      } else {
        // no good way to handle this. Guava uses InputStream.available to check
        // if EOF is reached and because available is not reliable,
        // tries to read the rest of the data.
        DrillBuf skipBuf = dataReader.getNext((int) n);
        if (skipBuf != null) {
          skipBuf.release();
        } else {
          throw new EOFException("End of file reached.");
        }
      }
    }
  }

  /**
   * Reads and stores this column chunk's dictionary.
   * @throws IOException
   */
  protected void loadDictionary() throws IOException {
    assert pageHeader.getType() == PageType.DICTIONARY_PAGE;
    assert this.dictionary == null;

    // dictData is not a local because we need to release it later.
    this.dictData = codecName == CompressionCodecName.UNCOMPRESSED
      ? readUncompressedPage()
      : readCompressedPageV1();

    DictionaryPage page = new DictionaryPage(
      asBytesInput(dictData, 0, pageHeader.uncompressed_page_size),
      pageHeader.uncompressed_page_size,
      pageHeader.dictionary_page_header.num_values,
      valueOf(pageHeader.dictionary_page_header.encoding.name())
    );

    this.dictionary = page.getEncoding().initDictionary(columnDescriptor, page);
  }

  /**
   * Reads an uncompressed Parquet page without copying the buffer returned by the backing input stream.
   * @return uncompressed Parquet page data
   * @throws IOException
   */
  protected DrillBuf readUncompressedPage() throws IOException {
    int outputSize = pageHeader.getUncompressed_page_size();
    long start = dataReader.getPos();

    Stopwatch timer = Stopwatch.createStarted();
    DrillBuf outputPageData = dataReader.getNext(outputSize);
    long timeToRead = timer.elapsed(TimeUnit.NANOSECONDS);

    if (logger.isTraceEnabled()) {
        logger.trace(
          "Col: {}  readPos: {}  Uncompressed_size: {}  pageData: {}",
          columnChunkMetaData.toString(),
          dataReader.getPos(),
          outputSize,
          ByteBufUtil.hexDump(outputPageData)
        );
    }

    this.updateStats(pageHeader, "Page Read", start, timeToRead, outputSize, outputSize);

    return outputPageData;
  }

  /**
   * Reads a compressed v1 data page or a dictionary page, both of which are compressed
   * in their entirety.
   * @return decompressed Parquet page data
   * @throws IOException
   */
  protected DrillBuf readCompressedPageV1() throws IOException {
    Stopwatch timer = Stopwatch.createUnstarted();

    int inputSize = pageHeader.getCompressed_page_size();
    int outputSize = pageHeader.getUncompressed_page_size();
    long start = dataReader.getPos();
    long timeToRead;

    DrillBuf inputPageData = null;
    DrillBuf outputPageData = this.allocator.buffer(outputSize);

    try {
      timer.start();
      inputPageData = dataReader.getNext(inputSize);
      timeToRead = timer.elapsed(TimeUnit.NANOSECONDS);
      this.updateStats(pageHeader, "Page Read", start, timeToRead, inputSize, inputSize);
      timer.reset();

      timer.start();
      start = dataReader.getPos();
      CompressionCodecName codecName = columnChunkMetaData.getCodec();
      BytesInputDecompressor decomp = codecFactory.getDecompressor(codecName);
      ByteBuffer input = inputPageData.nioBuffer(0, inputSize);
      ByteBuffer output = outputPageData.nioBuffer(0, outputSize);

      decomp.decompress(input, inputSize, output, outputSize);
      outputPageData.writerIndex(outputSize);
      timeToRead = timer.elapsed(TimeUnit.NANOSECONDS);

      if (logger.isTraceEnabled()) {
        logger.trace(
          "Col: {}  readPos: {}  Uncompressed_size: {}  pageData: {}",
          columnChunkMetaData.toString(),
          dataReader.getPos(),
          outputSize,
          ByteBufUtil.hexDump(outputPageData)
        );
      }

      this.updateStats(pageHeader, "Decompress", start, timeToRead, inputSize, outputSize);
    } finally {
      if (inputPageData != null) {
        inputPageData.release();
      }
    }

    return outputPageData;
  }

  /**
   * Reads a compressed v2 data page which excluded the repetition and definition level
   * sections from compression.
   * @return decompressed Parquet page data
   * @throws IOException
   */
  protected DrillBuf readCompressedPageV2() throws IOException {
    Stopwatch timer = Stopwatch.createUnstarted();

    int inputSize = pageHeader.getCompressed_page_size();
    int repLevelSize = pageHeader.data_page_header_v2.getRepetition_levels_byte_length();
    int defLevelSize = pageHeader.data_page_header_v2.getDefinition_levels_byte_length();
    int compDataOffset = repLevelSize + defLevelSize;
    int outputSize = pageHeader.uncompressed_page_size;
    long start = dataReader.getPos();
    long timeToRead;

    DrillBuf inputPageData = null;
    DrillBuf outputPageData = this.allocator.buffer(outputSize);

    try {
      timer.start();
      // Read in both the uncompressed and compressed sections
      inputPageData = dataReader.getNext(inputSize);
      timeToRead = timer.elapsed(TimeUnit.NANOSECONDS);
      this.updateStats(pageHeader, "Page Read", start, timeToRead, inputSize, inputSize);
      timer.reset();

      timer.start();
      start = dataReader.getPos();
      // Write out the uncompressed section
      // Note that the following setBytes call to read the repetition and definition level sections
      // advances readerIndex in inputPageData but not writerIndex in outputPageData.
      outputPageData.setBytes(0, inputPageData, compDataOffset);

      // decompress from the start of compressed data to the end of the input buffer
      CompressionCodecName codecName = columnChunkMetaData.getCodec();
      BytesInputDecompressor decomp = codecFactory.getDecompressor(codecName);
      ByteBuffer input = inputPageData.nioBuffer(compDataOffset, inputSize - compDataOffset);
      ByteBuffer output = outputPageData.nioBuffer(compDataOffset, outputSize - compDataOffset);
      decomp.decompress(
        input,
        inputSize - compDataOffset,
        output,
        outputSize - compDataOffset
      );
      outputPageData.writerIndex(outputSize);
      timeToRead = timer.elapsed(TimeUnit.NANOSECONDS);

    if (logger.isTraceEnabled()) {
      logger.trace(
        "Col: {}  readPos: {}  Uncompressed_size: {}  pageData: {}",
        columnChunkMetaData.toString(),
        dataReader.getPos(),
        outputSize,
        ByteBufUtil.hexDump(outputPageData)
      );
    }

      this.updateStats(pageHeader, "Decompress", start, timeToRead, inputSize, outputSize);
    } finally {
      if (inputPageData != null) {
        inputPageData.release();
      }
    }

    return outputPageData;
  }

  /**
   * Reads the next page header available in the backing input stream.
   * @throws IOException
   */
  protected void readPageHeader() throws IOException {
    long start = dataReader.getPos();
    Stopwatch timer = Stopwatch.createStarted();
    this.pageHeader = Util.readPageHeader(dataReader);
    long timeToRead = timer.elapsed(TimeUnit.NANOSECONDS);
    long pageHeaderBytes = dataReader.getPos() - start;
    this.updateStats(pageHeader, "Page Header", start, timeToRead, pageHeaderBytes, pageHeaderBytes);

    if (logger.isTraceEnabled()) {
      logger.trace(
        "ParquetTrace,{},{},{},{},{},{},{},{}", "Page Header Read", "",
        this.parentColumnReader.parentReader.getHadoopPath(),
        this.columnDescriptor.toString(),
        start,
        0,
        0,
        timeToRead
      );
    }
  }

  /**
   * Inspects the type of the next page and dispatches it for dictionary loading
   * or data decompression accordingly.
   * @throws IOException
   */
  protected void nextInternal() throws IOException {
    readPageHeader();

    if (pageHeader.uncompressed_page_size == 0) {
      logger.info(
        "skipping a {} of size {} because its uncompressed size is 0 bytes.",
        pageHeader.getType(),
        pageHeader.compressed_page_size
      );
      skip(pageHeader.compressed_page_size);
      return;
    }

    switch (pageHeader.getType()) {
      case DICTIONARY_PAGE:
        loadDictionary();
        break;
      case DATA_PAGE:
        pageData = codecName == CompressionCodecName.UNCOMPRESSED
          ? readUncompressedPage()
          : readCompressedPageV1();
        break;
      case DATA_PAGE_V2:
        pageData = codecName == CompressionCodecName.UNCOMPRESSED
          ? readUncompressedPage()
          : readCompressedPageV2();
        break;
      default:
        logger.info("skipping a {} of size {}", pageHeader.getType(), pageHeader.compressed_page_size);
        skip(pageHeader.compressed_page_size);

    }
  }

  /**
   * Decodes any repetition and definition level data in this page
   * @returns the offset into the page buffer after any levels have been decoded.
   */
  protected int decodeLevels() throws IOException {
    int maxRepLevel = columnDescriptor.getMaxRepetitionLevel();
    int maxDefLevel = columnDescriptor.getMaxDefinitionLevel();
    int dataOffset;

    switch (pageHeader.getType()) {
      case DATA_PAGE:
        ByteBufferInputStream dataStream = ByteBufferInputStream.wrap(pageData.nioBuffer(0, byteLength));

        if (maxRepLevel > 0) {
          Encoding rlEncoding = METADATA_CONVERTER.getEncoding(dataPageInfo.getRepetitionLevelEncoding());
          ValuesReader rlReader = rlEncoding.getValuesReader(columnDescriptor, ValuesType.REPETITION_LEVEL);
          rlReader.initFromPage(pageValueCount, dataStream);
          this.repetitionLevels = new ValuesReaderIntIterator(rlReader);

          // we know that the first value will be a 0, at the end of each list of repeated values we will hit another 0 indicating
          // a new record, although we don't know the length until we hit it (and this is a one way stream of integers) so we
          // read the first zero here to simplify the reading processes, and start reading the first value the same as all
          // of the rest. Effectively we are 'reading' the non-existent value in front of the first allowing direct access to
          // the first list of repetition levels
          this.repetitionLevels.nextInt();
        }

        if (maxDefLevel > 0) {
          Encoding dlEncoding = METADATA_CONVERTER.getEncoding(dataPageInfo.getDefinitionLevelEncoding());
          ValuesReader dlReader = dlEncoding.getValuesReader(columnDescriptor, ValuesType.DEFINITION_LEVEL);
          dlReader.initFromPage(pageValueCount, dataStream);
          this.definitionLevels = new ValuesReaderIntIterator(dlReader);
        }

        dataOffset = (int) dataStream.position();
        break;
      case DATA_PAGE_V2:
        int repLevelLen = pageHeader.data_page_header_v2.repetition_levels_byte_length;
        int defLevelLen = pageHeader.data_page_header_v2.definition_levels_byte_length;

        if (maxRepLevel > 0) {
          this.repetitionLevels = newRLEIterator(
            maxRepLevel,
            BytesInput.from(pageData.nioBuffer(0, repLevelLen))
          );

          // See earlier comment.
          this.repetitionLevels.nextInt();
        }

        if (maxDefLevel > 0) {
          this.definitionLevels = newRLEIterator(
            maxDefLevel,
            BytesInput.from(pageData.nioBuffer(repLevelLen, defLevelLen))
          );
        }

        dataOffset = repLevelLen + defLevelLen;
        break;
      default:
        throw new DrillRuntimeException(String.format(
          "Did not expect to find a page of type %s now.",
          pageHeader.getType()
        ));
    }
    return dataOffset;
  }

  /**
   * Read the next page in the parent column chunk
   *
   * @return true if a page was found to read
   * @throws IOException
   */
  public boolean next() throws IOException {
    this.pageValueCount = -1;
    this.valuesRead = this.valuesReadyToRead = 0;
    this.parentColumnReader.currDefLevel = -1;
    long totalValueCount = columnChunkMetaData.getValueCount();

    if (parentColumnReader.totalValuesRead >= totalValueCount) {
      return false;
    }

    clearDataBufferAndReaders();
    do {
      nextInternal();

      if (pageHeader == null) {
        throw new DrillRuntimeException(String.format(
          "Failed to read another page having read %d of %d values from its " +
          "column chunk.",
          parentColumnReader.totalValuesRead,
          totalValueCount
        ));
      }
    } while (
      // Continue until we hit a non-empty data page
      pageHeader.uncompressed_page_size == 0
      || (pageHeader.getType() != PageType.DATA_PAGE
      && pageHeader.getType() != PageType.DATA_PAGE_V2)
    );

    if (pageData == null) {
      throw new DrillRuntimeException(String.format(
        "Failed to read another page having read %d of %d values from its " +
        "column chunk.",
        parentColumnReader.totalValuesRead,
        totalValueCount
      ));
    }

    dataPageInfo = DataPageHeaderInfoProvider.builder(this.pageHeader);
    this.byteLength = this.pageHeader.uncompressed_page_size;
    this.pageValueCount = dataPageInfo.getNumValues();

    Stopwatch timer = Stopwatch.createStarted();
    // readPosInBytes is used for actually reading the values after we determine how many will fit in the vector
    // readyToReadPosInBytes serves a similar purpose for the vector types where we must count up the values that will
    // fit one record at a time, such as for variable length data. Both operations must start in the same location after the
    // definition and repetition level data which is stored alongside the page data itself
    this.readyToReadPosInBytes = this.readPosInBytes = decodeLevels();

    Encoding valueEncoding = METADATA_CONVERTER.getEncoding(dataPageInfo.getEncoding());
    parentColumnReader.usingDictionary = valueEncoding.usesDictionary();

    long timeDecode = timer.elapsed(TimeUnit.NANOSECONDS);
    stats.numDataPagesDecoded.incrementAndGet();
    stats.timeDataPageDecode.addAndGet(timeDecode);

    return true;
  }

  /**
   * Lazily creates a ValuesReader for when use in the cases when the data type
   * or encoding requires it.
   * @return an existing or new ValuesReader
   */
  public ValuesReader getValueReader() {
    if (valueReader == null) {
      Encoding valueEncoding = METADATA_CONVERTER.getEncoding(dataPageInfo.getEncoding());
      ByteBuffer dataBuffer = pageData.nioBuffer((int)readPosInBytes, byteLength-(int)readPosInBytes);
      this.valueReader = valueEncoding.getValuesReader(columnDescriptor, ValuesType.VALUES);
      try {
        this.valueReader.initFromPage(pageValueCount, ByteBufferInputStream.wrap(dataBuffer));
      } catch (IOException e) {
        throw new DrillRuntimeException("Error initialising a ValuesReader for this page.", e);
      }
    }

    return valueReader;
  }

  /**
   * Lazily creates a dictionary length determining ValuesReader for when use when this column chunk
   * is dictionary encoded.
   * @return an existing or new ValuesReader
   */
  public ValuesReader getDictionaryLengthDeterminingReader() {
    if (dictionaryLengthDeterminingReader == null) {
      ByteBuffer dataBuffer = pageData.nioBuffer((int)readPosInBytes, byteLength-(int)readPosInBytes);
      dictionaryLengthDeterminingReader = new DictionaryValuesReader(dictionary);
      try {
        dictionaryLengthDeterminingReader.initFromPage(pageValueCount, ByteBufferInputStream.wrap(dataBuffer));
      } catch (IOException e) {
        throw new DrillRuntimeException(
          "Error initialising a dictionary length determining ValuesReader for this page.",
          e
        );
      }
    }
    return dictionaryLengthDeterminingReader;
  }

  /**
   * Lazily creates a dictionary ValuesReader for when use when this column chunk is dictionary encoded.
   * @return an existing or new ValuesReader
   */
  public ValuesReader getDictionaryValueReader() {
    if (dictionaryValueReader == null) {
      ByteBuffer dataBuffer = pageData.nioBuffer((int)readPosInBytes, byteLength-(int)readPosInBytes);
      dictionaryValueReader = new DictionaryValuesReader(dictionary);
      try {
        dictionaryValueReader.initFromPage(pageValueCount, ByteBufferInputStream.wrap(dataBuffer));
      } catch (IOException e) {
        throw new DrillRuntimeException(
          "Error initialising a dictionary ValuesReader for this page.",
          e
        );
      }
    }
    return dictionaryValueReader;
  }

  /**
   *
   * @return true if the previous call to next() read a page.
   */
  protected boolean hasPage() {
    return pageValueCount != -1;
  }

  protected void updateStats(PageHeader pageHeader, String op, long start, long time, long bytesin, long bytesout) {
    if (logger.isTraceEnabled()) {
      logger.trace("ParquetTrace,{},{},{},{},{},{},{},{}",
        op,
        pageHeader.type == PageType.DICTIONARY_PAGE ? "Dictionary Page" : "Data Page",
        this.parentColumnReader.parentReader.getHadoopPath(),
        this.columnDescriptor.toString(),
        start,
        bytesin,
        bytesout,
        time
      );
    }

    if (pageHeader.type == PageType.DICTIONARY_PAGE) {
      if (bytesin == bytesout) {
        this.stats.timeDictPageLoads.addAndGet(time);
        this.stats.numDictPageLoads.incrementAndGet();
        this.stats.totalDictPageReadBytes.addAndGet(bytesin);
      } else {
        this.stats.timeDictPagesDecompressed.addAndGet(time);
        this.stats.numDictPagesDecompressed.incrementAndGet();
        this.stats.totalDictDecompressedBytes.addAndGet(bytesin);
      }
    } else {
      if (bytesin == bytesout) {
        this.stats.timeDataPageLoads.addAndGet(time);
        this.stats.numDataPageLoads.incrementAndGet();
        this.stats.totalDataPageReadBytes.addAndGet(bytesin);
      } else {
        this.stats.timeDataPagesDecompressed.addAndGet(time);
        this.stats.numDataPagesDecompressed.incrementAndGet();
        this.stats.totalDataDecompressedBytes.addAndGet(bytesin);
      }
    }
  }

  /**
   * Clear buffers and readers between pages of a column chunk
   */
  protected void clearDataBufferAndReaders() {
    if (pageData != null) {
      pageData.release();
      pageData = null;
    }
    this.dictionaryValueReader = this.dictionaryLengthDeterminingReader = this.valueReader = null;
  }

  /**
   * Clear buffers between column chunks
   */
  protected void clearDictionaryBuffer() {
    if (dictData != null) {
      dictData.release();
      dictData = null;
    }
  }

  /**
   * Closes the backing input stream and frees all allocated buffers.
   */
  public void clear() {
    try {
      // data reader also owns the input stream and will close it.
      this.dataReader.close();
    } catch (IOException e) {
      //Swallow the exception which is OK for input streams
      logger.warn("encountered an error when it tried to close its input stream: {}", e);
    }
    // Free all memory, including fixed length types. (Data is being copied for all types not just var length types)
    clearDataBufferAndReaders();
    clearDictionaryBuffer();
  }

  /**
   * Enables Parquet column readers to reset the definition level reader to a specific state.
   * @param skipCount the number of rows to skip (optional)
   *
   * @throws IOException
   */
  void resetDefinitionLevelReader(int skipCount) throws IOException {
    Preconditions.checkState(columnDescriptor.getMaxDefinitionLevel() == 1);
    Preconditions.checkState(pageValueCount > 0);

    decodeLevels();

    // Skip values if requested by caller
    for (int idx = 0; idx < skipCount; ++idx) {
      definitionLevels.nextInt();
    }
  }

  public static BytesInput asBytesInput(DrillBuf buf, int offset, int length) throws IOException {
    return BytesInput.from(buf.nioBuffer(offset, length));
  }

  private IntIterator newRLEIterator(int maxLevel, BytesInput bytes) throws IOException {
    if (maxLevel == 0) {
      return new NullIntIterator();
    }
    return new RLEIntIterator(
      new RunLengthBitPackingHybridDecoder(
        BytesUtils.getWidthFromMaxInt(maxLevel),
        bytes.toInputStream()));
  }

  interface IntIterator {
    int nextInt();
  }

  /**
   * Provides an iterator interface that delegates to a ValuesReader on the
   * repetition or definition levels of a Parquet v1 page.
   */
  static class ValuesReaderIntIterator implements IntIterator {
    ValuesReader delegate;

    public ValuesReaderIntIterator(ValuesReader delegate) {
      super();
      this.delegate = delegate;
    }

    @Override
    public int nextInt() {
      return delegate.readInteger();
    }
  }

  /**
   * Provides an interator interface to the RLE/bitpacked repetition or definition
   * levels of a Parquet v2 page
   */
  private static class RLEIntIterator implements IntIterator {
    RunLengthBitPackingHybridDecoder delegate;

    public RLEIntIterator(RunLengthBitPackingHybridDecoder delegate) {
      this.delegate = delegate;
    }

    @Override
    public int nextInt() {
      try {
        return delegate.readInt();
      } catch (IOException e) {
        throw new ParquetDecodingException(e);
      }
    }
  }

  /**
   * Provides an interator interface to the nonexistent repetition or definition
   * levels of a page that has none.
   */
  private static final class NullIntIterator implements IntIterator {
    @Override
    public int nextInt() {
      return 0;
    }
  }
}
