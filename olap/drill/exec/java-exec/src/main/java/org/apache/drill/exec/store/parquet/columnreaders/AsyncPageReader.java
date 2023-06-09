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

import static org.apache.parquet.column.Encoding.valueOf;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import io.netty.buffer.ByteBufUtil;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.ops.OperatorStats;
import org.apache.drill.exec.util.concurrent.ExecutorServiceUtil;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.column.page.DictionaryPage;
import org.apache.parquet.compression.CompressionCodecFactory;
import org.apache.parquet.format.PageHeader;
import org.apache.parquet.format.PageType;
import org.apache.parquet.format.Util;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;

import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;

import io.netty.buffer.DrillBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The AyncPageReader reads one page of data at a time asynchronously from the provided InputStream. The
 * first request to the page reader creates a Future Task (AsyncPageReaderTask) and submits it to the
 * scan thread pool. The result of the Future task (a page) is put into a (blocking) queue and the scan
 * thread starts processing the data as soon as the Future task is complete.
 * This is a simple producer-consumer queue, the AsyncPageReaderTask is the producer and the ParquetScan is
 * the consumer.
 * The AsyncPageReaderTask submits another Future task for reading the next page as soon as it is done,
 * while the results queue is not full. Until the queue is full, therefore, the scan thread pool keeps the
 * disk as busy as possible.
 * In case the disk is slower than the processing, the queue is never filled up after the processing of the
 * pages begins. In this case, the next disk read begins immediately after the previous read is completed
 * and the disk is never idle. The query in this case is effectively bounded by the disk.
 * If, however, the processing is slower than the disk (can happen with SSDs, data being cached by the
 * FileSystem, or if the processing requires complex processing that is necessarily slow) the queue fills
 * up. Once the queue is full, the AsyncPageReaderTask does not submit any new Future tasks. The next Future
 * task is submitted by the *processing* thread as soon as it pulls a page out of the queue. (Note that the
 * invariant here is that there is space for at least one more page in the queue before the Future read task
 * is submitted to the pool). This sequence is important. Not doing so can lead to deadlocks - producer
 * threads may block on putting data into the queue which is full while the consumer threads might be
 * blocked trying to read from a queue that has no data.
 * The first request to the page reader can be either to load a dictionary page or a data page; this leads
 * to the rather odd looking code in the constructor since the parent PageReader calls
 * loadDictionaryIfExists in the constructor.
 * The Future tasks created are kept in a non blocking queue and the Future object is checked for any
 * exceptions that might have occurred during the execution. The queue of Futures is also used to cancel
 * any pending Futures at close (this may happen as a result of a cancel).
 *
 */
class AsyncPageReader extends PageReader {
  static final Logger logger = LoggerFactory.getLogger(AsyncPageReader.class);

  private ExecutorService threadPool;
  private long queueSize;
  private LinkedBlockingQueue<ReadStatus> pageQueue;
  private ConcurrentLinkedQueue<Future<Void>> asyncPageRead;
  private long totalPageValuesRead = 0;
  // Object to use to synchronize access to the page Queue.
  // FindBugs complains if we synchronize on a Concurrent Queue
  private final Object pageQueueSyncronize = new Object();

  AsyncPageReader(ColumnReader<?> parentStatus, FileSystem fs, Path path) throws ExecutionSetupException {
    super(parentStatus, fs, path);
    threadPool = parentColumnReader.parentReader.getOperatorContext().getScanExecutor();
    queueSize = parentColumnReader.parentReader.readQueueSize;
    pageQueue = new LinkedBlockingQueue<>((int) queueSize);
    asyncPageRead = new ConcurrentLinkedQueue<>();
  }

  @Override
  protected void init() throws IOException {
    super.init();
    //Avoid Init if a shutdown is already in progress even if init() is called once
    if (!parentColumnReader.isShuttingDown) {
      asyncPageRead.offer(ExecutorServiceUtil.submit(threadPool, new AsyncPageReaderTask(debugName, pageQueue)));
    }
  }

  /**
   * Reads and stores this column chunk's dictionary page.
   * @throws IOException
   */
  protected void loadDictionary(ReadStatus readStatus) throws IOException {
    assert readStatus.isDictionaryPage();
    assert this.dictionary == null;

    // dictData is not a local because we need to release it later.
    this.dictData = codecName == CompressionCodecName.UNCOMPRESSED
      ? readStatus.getPageData()
      : decompressPageV1(readStatus);

    DictionaryPage page = new DictionaryPage(
      asBytesInput(dictData, 0, pageHeader.uncompressed_page_size),
      pageHeader.uncompressed_page_size,
      pageHeader.dictionary_page_header.num_values,
      valueOf(pageHeader.dictionary_page_header.encoding.name())
    );

    this.dictionary = page.getEncoding().initDictionary(columnDescriptor, page);
  }

  /**
   * Reads a compressed v1 data page or a dictionary page, both of which are compressed
   * in their entirety.
   * @return decompressed Parquet page data
   * @throws IOException
   */
  protected DrillBuf decompressPageV1(ReadStatus readStatus) throws IOException {
    Stopwatch timer = Stopwatch.createUnstarted();

    PageHeader pageHeader = readStatus.getPageHeader();
    int inputSize = pageHeader.getCompressed_page_size();
    int outputSize = pageHeader.getUncompressed_page_size();
    // TODO: does reporting this number have the same meaning in an async context?
    long start = dataReader.getPos();
    long timeToRead;

    DrillBuf inputPageData = readStatus.getPageData();
    DrillBuf outputPageData = this.allocator.buffer(outputSize);

    try {
      timer.start();
      CompressionCodecName codecName = columnChunkMetaData.getCodec();
      CompressionCodecFactory.BytesInputDecompressor decomp = codecFactory.getDecompressor(codecName);
      ByteBuffer input = inputPageData.nioBuffer(0, inputSize);
      ByteBuffer output = outputPageData.nioBuffer(0, outputSize);

      decomp.decompress(input, inputSize, output, outputSize);
      outputPageData.writerIndex(outputSize);
      timeToRead = timer.elapsed(TimeUnit.NANOSECONDS);

      if (logger.isTraceEnabled()) {
        logger.trace(
          "Col: {}  readPos: {}  Uncompressed_size: {}  pageData: {}",
          columnChunkMetaData.toString(),
          dataReader.getPos(), // TODO: see comment on earlier call to getPos()
          outputSize,
          ByteBufUtil.hexDump(outputPageData)
        );
      }

      this.updateStats(pageHeader, "Decompress", start, timeToRead, inputSize, outputSize);
    } finally {
      readStatus.setPageData(null);
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
  protected DrillBuf decompressPageV2(ReadStatus readStatus) throws IOException {
    Stopwatch timer = Stopwatch.createUnstarted();

    PageHeader pageHeader = readStatus.getPageHeader();
    int inputSize = pageHeader.getCompressed_page_size();
    int repLevelSize = pageHeader.data_page_header_v2.getRepetition_levels_byte_length();
    int defLevelSize = pageHeader.data_page_header_v2.getDefinition_levels_byte_length();
    int compDataOffset = repLevelSize + defLevelSize;
    int outputSize = pageHeader.uncompressed_page_size;
    // TODO: does reporting this number have the same meaning in an async context?
    long start = dataReader.getPos();
    long timeToRead;

    DrillBuf inputPageData = readStatus.getPageData();
    DrillBuf outputPageData = this.allocator.buffer(outputSize);

    try {
      timer.start();
      // Write out the uncompressed section
      // Note that the following setBytes call to read the repetition and definition level sections
      // advances readerIndex in inputPageData but not writerIndex in outputPageData.
      outputPageData.setBytes(0, inputPageData, compDataOffset);

      // decompress from the start of compressed data to the end of the input buffer
      CompressionCodecName codecName = columnChunkMetaData.getCodec();
      CompressionCodecFactory.BytesInputDecompressor decomp = codecFactory.getDecompressor(codecName);
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
          dataReader.getPos(), // TODO: see comment on earlier call to getPos()
          outputSize,
          ByteBufUtil.hexDump(outputPageData)
        );
      }

      this.updateStats(pageHeader, "Decompress", start, timeToRead, inputSize, outputSize);
    } finally {
      readStatus.setPageData(null);
      if (inputPageData != null) {
        inputPageData.release();
      }
    }

    return outputPageData;
  }

  /**
   * Blocks for a page to become available in the queue then takes it and schedules a new page
   * read task if the queue was full.
   * @returns ReadStatus the page taken from the queue
   */
  private ReadStatus nextPageFromQueue() throws InterruptedException, ExecutionException {
    ReadStatus readStatus;
    Stopwatch timer = Stopwatch.createStarted();
    OperatorStats opStats = parentColumnReader.parentReader.getOperatorContext().getStats();
    opStats.startWait();
    try {
      waitForExecutionResult(); // get the result of execution
      synchronized (pageQueueSyncronize) {
        boolean pageQueueFull = pageQueue.remainingCapacity() == 0;
        readStatus = pageQueue.take(); // get the data if no exception has been thrown
        if (readStatus == ReadStatus.EMPTY) {
          throw new DrillRuntimeException("Unexpected end of data");
        }
        //if the queue was full before we took a page out, then there would
        // have been no new read tasks scheduled. In that case, schedule a new read.
        if (!parentColumnReader.isShuttingDown && pageQueueFull) {
          asyncPageRead.offer(ExecutorServiceUtil.submit(threadPool, new AsyncPageReaderTask(debugName, pageQueue)));
        }
      }
    } finally {
      opStats.stopWait();
    }

    long timeBlocked = timer.elapsed(TimeUnit.NANOSECONDS);
    stats.timeDiskScanWait.addAndGet(timeBlocked);
    stats.timeDiskScan.addAndGet(readStatus.getDiskScanTime());
    if (readStatus.isDictionaryPage) {
      stats.numDictPageLoads.incrementAndGet();
      stats.timeDictPageLoads.addAndGet(timeBlocked + readStatus.getDiskScanTime());
    } else {
      stats.numDataPageLoads.incrementAndGet();
      stats.timeDataPageLoads.addAndGet(timeBlocked + readStatus.getDiskScanTime());
    }

    return readStatus;
  }

  /**
   * Inspects the type of the next page and dispatches it for dictionary loading
   * or data decompression accordingly.
   */
  @Override
  protected void nextInternal() throws IOException {
    try {
      ReadStatus readStatus = nextPageFromQueue();
      pageHeader = readStatus.getPageHeader();

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
          loadDictionary(readStatus);
          break;
        case DATA_PAGE:
          pageData = codecName == CompressionCodecName.UNCOMPRESSED
            ? readStatus.getPageData()
            : decompressPageV1(readStatus);
          break;
        case DATA_PAGE_V2:
          pageData = codecName == CompressionCodecName.UNCOMPRESSED
            ? readStatus.getPageData()
            : decompressPageV2(readStatus);
          break;
        default:
          logger.warn("skipping page of type {} of size {}", pageHeader.getType(), pageHeader.compressed_page_size);
          skip(pageHeader.compressed_page_size);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (RuntimeException e) { // Catch this explicitly to satisfy findbugs
      throwUserException(e, "Error reading page data");
    } catch (Exception e) {
      throwUserException(e, "Error reading page data");
    }
  }

  /**
   * Blocking fetch from the page queue.
   */
  private void waitForExecutionResult() throws InterruptedException, ExecutionException {
    // Get the execution result but don't remove the Future object from the "asyncPageRead" queue yet;
    // this will ensure that cleanup will happen properly in case of an exception being thrown
    asyncPageRead.peek().get(); // get the result of execution
    // Alright now remove the Future object
    asyncPageRead.poll();
  }

  @Override
  public void clear() {
    //Cancelling all existing AsyncPageReaderTasks
    while (asyncPageRead != null && !asyncPageRead.isEmpty()) {
      try {
        Future<Void> f = asyncPageRead.poll();
        if(!f.isDone() && !f.isCancelled()){
          f.cancel(true);
        } else {
          f.get(1, TimeUnit.MILLISECONDS);
        }
      } catch (RuntimeException e) {
        // Do Nothing
      } catch (Exception e) {
        // Do nothing.
      }
    }

    //Empty the page queue
    ReadStatus r;
    while (!pageQueue.isEmpty()) {
      r = null;
      try {
        r = pageQueue.poll();
        if (r == ReadStatus.EMPTY) {
          break;
        }
      } catch (Exception e) {
        //Reporting because we shouldn't get this
        logger.error(e.getMessage());
      } finally {
        if (r != null && r.pageData != null) {
          r.pageData.release();
        }
      }
    }
    super.clear();
  }

  /**
   * Wraps up a buffer of page data along with the page header and some metadata
   */
  public static class ReadStatus {
    private PageHeader pageHeader;
    private DrillBuf pageData;
    private boolean isDictionaryPage = false;
    private long bytesRead = 0;
    private long valuesRead = 0;
    private long diskScanTime = 0;

    public static final ReadStatus EMPTY = new ReadStatus();

    public synchronized PageHeader getPageHeader() {
      return pageHeader;
    }

    public synchronized void setPageHeader(PageHeader pageHeader) {
      this.pageHeader = pageHeader;
    }

    public synchronized DrillBuf getPageData() {
      return pageData;
    }

    public synchronized void setPageData(DrillBuf pageData) {
      this.pageData = pageData;
    }

    public synchronized boolean isDictionaryPage() {
      return isDictionaryPage;
    }

    public synchronized void setIsDictionaryPage(boolean isDictionaryPage) {
      this.isDictionaryPage = isDictionaryPage;
    }

    public synchronized long getBytesRead() {
      return bytesRead;
    }

    public synchronized void setBytesRead(long bytesRead) {
      this.bytesRead = bytesRead;
    }

    public synchronized long getValuesRead() {
      return valuesRead;
    }

    public synchronized void setValuesRead(long valuesRead) {
      this.valuesRead = valuesRead;
    }

    public synchronized long getDiskScanTime() {
      return diskScanTime;
    }

    public synchronized void setDiskScanTime(long diskScanTime) {
      this.diskScanTime = diskScanTime;
    }

  }

  private class AsyncPageReaderTask implements Callable<Void> {

    private final AsyncPageReader parent = AsyncPageReader.this;
    private final LinkedBlockingQueue<ReadStatus> queue;
    private final String name;

    public AsyncPageReaderTask(String name, LinkedBlockingQueue<ReadStatus> queue) {
      this.name = name;
      this.queue = queue;
    }

    @Override
    public Void call() throws IOException {
      ReadStatus readStatus = new ReadStatus();
      long bytesRead = 0;
      long valuesRead = 0;
      final long totalValuesRead = parent.totalPageValuesRead;
      Stopwatch timer = Stopwatch.createStarted();

      final long totalValuesCount = parent.columnChunkMetaData.getValueCount();

      // if we are done, just put a marker object in the queue and we are done.
      logger.trace("[{}]: Total Values COUNT {}  Total Values READ {} ", name, totalValuesCount, totalValuesRead);
      if (totalValuesRead >= totalValuesCount) {
        try {
          queue.put(ReadStatus.EMPTY);
          // Some InputStreams (like S3ObjectInputStream) should be closed
          // as soon as possible to make the connection reusable.
          try {
            parent.inputStream.close();
          } catch (IOException e) {
            logger.trace("[{}]: Failure while closing InputStream", name, e);
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          // Do nothing.
        }
        return null;
      }

      DrillBuf pageData = null;
      timer.reset();
      try {
        PageHeader pageHeader = Util.readPageHeader(parent.dataReader);
        int compressedSize = pageHeader.getCompressed_page_size();
        if ( parent.parentColumnReader.isShuttingDown ) { return null; } //Opportunity to skip expensive Parquet processing
        pageData = parent.dataReader.getNext(compressedSize);
        bytesRead = compressedSize;

        synchronized (parent) {
          PageType type = pageHeader.getType() == null ? PageType.DATA_PAGE : pageHeader.getType();
          switch (type) {
            case DICTIONARY_PAGE:
              readStatus.setIsDictionaryPage(true);
              valuesRead += pageHeader.getDictionary_page_header().getNum_values();
              break;
            case DATA_PAGE_V2:
              valuesRead += pageHeader.getData_page_header_v2().getNum_values();
              parent.totalPageValuesRead += valuesRead;
              break;
            case DATA_PAGE:
              valuesRead += pageHeader.getData_page_header().getNum_values();
              parent.totalPageValuesRead += valuesRead;
              break;
            default:
              throw UserException.unsupportedError()
                  .message("Page type is not supported yet: " + type)
                  .build(logger);
          }
          long timeToRead = timer.elapsed(TimeUnit.NANOSECONDS);
          readStatus.setPageHeader(pageHeader);
          readStatus.setPageData(pageData);
          readStatus.setBytesRead(bytesRead);
          readStatus.setValuesRead(valuesRead);
          readStatus.setDiskScanTime(timeToRead);
          assert (totalValuesRead <= totalValuesCount);
        }
        // You do need the synchronized block
        // because you want the check to see if there is remaining capacity in the queue, to be
        // synchronized
        synchronized (parent.pageQueueSyncronize) {
          queue.put(readStatus);
          // if the queue is not full, schedule another read task immediately. If it is then the consumer
          // will schedule a new read task as soon as it removes a page from the queue.
          if (!parentColumnReader.isShuttingDown && queue.remainingCapacity() > 0) {
            asyncPageRead.offer(ExecutorServiceUtil.submit(parent.threadPool, new AsyncPageReaderTask(debugName, queue)));
          }
        }
        // Do nothing.
      } catch (InterruptedException e) {
        if (pageData != null) {
          pageData.release();
        }
        Thread.currentThread().interrupt();
      } catch (Exception e) {
        if (pageData != null) {
          pageData.release();
        }
        parent.throwUserException(e, "Exception occurred while reading from disk.");
      } finally {
        //Nothing to do if isShuttingDown.
    }
      return null;
    }
  }
}
