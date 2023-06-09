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

import java.util.concurrent.atomic.AtomicLong;

import org.apache.drill.exec.ops.OperatorStats;
import org.apache.drill.exec.store.CommonParquetRecordReader.Metric;
import org.apache.hadoop.fs.Path;

public class ParquetReaderStats {

  public AtomicLong numRowgroups = new AtomicLong();
  public AtomicLong rowgroupsPruned = new AtomicLong();

  public AtomicLong numDictPageLoads = new AtomicLong();
  public AtomicLong numDataPageLoads = new AtomicLong();
  public AtomicLong numDataPagesDecoded = new AtomicLong();
  public AtomicLong numDictPagesDecompressed = new AtomicLong();
  public AtomicLong numDataPagesDecompressed = new AtomicLong();

  public AtomicLong totalDictPageReadBytes = new AtomicLong();
  public AtomicLong totalDataPageReadBytes = new AtomicLong();
  public AtomicLong totalDictDecompressedBytes = new AtomicLong();
  public AtomicLong totalDataDecompressedBytes = new AtomicLong();

  public AtomicLong timeDictPageLoads = new AtomicLong();
  public AtomicLong timeDataPageLoads = new AtomicLong();
  public AtomicLong timeDataPageDecode = new AtomicLong();
  public AtomicLong timeDictPageDecode = new AtomicLong();
  public AtomicLong timeDictPagesDecompressed = new AtomicLong();
  public AtomicLong timeDataPagesDecompressed = new AtomicLong();

  public AtomicLong timeDiskScanWait = new AtomicLong();
  public AtomicLong timeDiskScan = new AtomicLong();
  public AtomicLong timeFixedColumnRead = new AtomicLong();
  public AtomicLong timeVarColumnRead = new AtomicLong();
  public AtomicLong timeProcess = new AtomicLong();

  public ParquetReaderStats() {
  }

  public void logStats(org.slf4j.Logger logger, Path hadoopPath) {
    logger.trace(
        "ParquetTrace,Summary,{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{}",
        hadoopPath,
        numRowgroups,
        rowgroupsPruned,
        numDictPageLoads,
        numDataPageLoads,
        numDataPagesDecoded,
        numDictPagesDecompressed,
        numDataPagesDecompressed,
        totalDictPageReadBytes,
        totalDataPageReadBytes,
        totalDictDecompressedBytes,
        totalDataDecompressedBytes,
        timeDictPageLoads,
        timeDataPageLoads,
        timeDataPageDecode,
        timeDictPageDecode,
        timeDictPagesDecompressed,
        timeDataPagesDecompressed,
        timeDiskScanWait,
        timeDiskScan,
        timeFixedColumnRead,
        timeVarColumnRead
    );
  }

  public void update(OperatorStats stats){
    stats.setLongStat(Metric.NUM_ROWGROUPS,
        numRowgroups.longValue());
    stats.setLongStat(Metric.ROWGROUPS_PRUNED,
        rowgroupsPruned.longValue());
    stats.addLongStat(Metric.NUM_DICT_PAGE_LOADS,
        numDictPageLoads.longValue());
    stats.addLongStat(Metric.NUM_DATA_PAGE_lOADS, numDataPageLoads.longValue());
    stats.addLongStat(Metric.NUM_DATA_PAGES_DECODED, numDataPagesDecoded.longValue());
    stats.addLongStat(Metric.NUM_DICT_PAGES_DECOMPRESSED,
        numDictPagesDecompressed.longValue());
    stats.addLongStat(Metric.NUM_DATA_PAGES_DECOMPRESSED,
        numDataPagesDecompressed.longValue());
    stats.addLongStat(Metric.TOTAL_DICT_PAGE_READ_BYTES,
        totalDictPageReadBytes.longValue());
    stats.addLongStat(Metric.TOTAL_DATA_PAGE_READ_BYTES,
        totalDataPageReadBytes.longValue());
    stats.addLongStat(Metric.TOTAL_DICT_DECOMPRESSED_BYTES,
        totalDictDecompressedBytes.longValue());
    stats.addLongStat(Metric.TOTAL_DATA_DECOMPRESSED_BYTES,
        totalDataDecompressedBytes.longValue());
    stats.addLongStat(Metric.TIME_DICT_PAGE_LOADS,
        timeDictPageLoads.longValue());
    stats.addLongStat(Metric.TIME_DATA_PAGE_LOADS,
        timeDataPageLoads.longValue());
    stats.addLongStat(Metric.TIME_DATA_PAGE_DECODE,
        timeDataPageDecode.longValue());
    stats.addLongStat(Metric.TIME_DICT_PAGE_DECODE,
        timeDictPageDecode.longValue());
    stats.addLongStat(Metric.TIME_DICT_PAGES_DECOMPRESSED,
        timeDictPagesDecompressed.longValue());
    stats.addLongStat(Metric.TIME_DATA_PAGES_DECOMPRESSED,
        timeDataPagesDecompressed.longValue());
    stats.addLongStat(Metric.TIME_DISK_SCAN_WAIT,
        timeDiskScanWait.longValue());
    stats.addLongStat(Metric.TIME_DISK_SCAN, timeDiskScan.longValue());
    stats.addLongStat(Metric.TIME_FIXEDCOLUMN_READ, timeFixedColumnRead.longValue());
    stats.addLongStat(Metric.TIME_VARCOLUMN_READ, timeVarColumnRead.longValue());
    stats.addLongStat(Metric.TIME_PROCESS, timeProcess.longValue());
  }
}
